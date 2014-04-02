// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/// Transfomer that inlines polymer-element definitions from html imports.
library polymer.src.build.import_inliner;

import 'dart:async';
import 'dart:convert';

import 'package:analyzer/src/generated/ast.dart';
import 'package:barback/barback.dart';
import 'package:code_transformers/assets.dart';
import 'package:path/path.dart' as path;
import 'package:html5lib/dom.dart' show
    Document, DocumentFragment, Element, Node;
import 'package:html5lib/dom_parsing.dart' show TreeVisitor;
import 'package:source_maps/refactor.dart' show TextEditTransaction;
import 'package:source_maps/span.dart';

import 'common.dart';

class _HtmlInliner extends PolymerTransformer {
  final TransformOptions options;
  final Transform transform;
  final TransformLogger logger;
  final AssetId docId;
  final seen = new Set<AssetId>();
  final scriptIds = <AssetId>[];

  /// The number of extracted inline Dart scripts. Used as a counter to give
  /// unique-ish filenames.
  int inlineScriptCounter = 0;

  _HtmlInliner(this.options, Transform transform)
      : transform = transform,
        logger = transform.logger,
        docId = transform.primaryInput.id;

  Future apply() {
    seen.add(docId);

    Document document;
    bool changed;

    return readPrimaryAsHtml(transform).then((doc) {
      document = doc;
      // Add the main script's ID, or null if none is present.
      // This will be used by ScriptCompactor.
      changed = _extractScripts(document, docId);
      return _visitImports(document);
    }).then((importsFound) {
      changed = changed || importsFound;

      var output = transform.primaryInput;
      if (changed) output = new Asset.fromString(docId, document.outerHtml);
      transform.addOutput(output);

      // We produce a secondary asset with extra information for later phases.
      transform.addOutput(new Asset.fromString(
          docId.addExtension('.scriptUrls'),
          JSON.encode(scriptIds, toEncodable: (id) => id.serialize())));
    });
  }

  /// Visits imports in [document] and add the imported documents to documents.
  /// Documents are added in the order they appear, transitive imports are added
  /// first.
  ///
  /// Returns `true` if and only if the document was changed and should be
  /// written out.
  Future<bool> _visitImports(Document document) {
    bool changed = false;

    _moveHeadToBody(document);

    // Note: we need to preserve the import order in the generated output.
    return Future.forEach(document.querySelectorAll('link'), (Element tag) {
      var rel = tag.attributes['rel'];
      if (rel != 'import' && rel != 'stylesheet') return null;

      // Note: URL has already been normalized so use docId.
      var href = tag.attributes['href'];
      var id = uriToAssetId(docId, href, transform.logger, tag.sourceSpan,
          errorOnAbsolute: rel != 'stylesheet');

      if (rel == 'import') {
        changed = true;
        if (id == null || !seen.add(id)) {
          tag.remove();
          return null;
        }
        return _inlineImport(id, tag);

      } else if (rel == 'stylesheet') {
        if (id == null) return null;
        changed = true;

        return _inlineStylesheet(id, tag);
      }
    }).then((_) => changed);
  }

  /// To preserve the order of scripts with respect to inlined
  /// link rel=import, we move both of those into the body before we do any
  /// inlining.
  ///
  /// Note: we do this for stylesheets as well to preserve ordering with
  /// respect to eachother, because stylesheets can be pulled in transitively
  /// from imports.
  // TODO(jmesserly): vulcanizer doesn't need this because they inline JS
  // scripts, causing them to be naturally moved as part of the inlining.
  // Should we do the same? Alternatively could we inline head into head and
  // body into body and avoid this whole thing?
  void _moveHeadToBody(Document doc) {
    var insertionPoint = doc.body.firstChild;
    for (var node in doc.head.nodes.toList(growable: false)) {
      if (node is! Element) continue;
      var tag = node.localName;
      var type = node.attributes['type'];
      var rel = node.attributes['rel'];
      if (tag == 'style' || tag == 'script' &&
            (type == null || type == TYPE_JS || type == TYPE_DART) ||
          tag == 'link' && (rel == 'stylesheet' || rel == 'import')) {
        // Move the node into the body, where its contents will be placed.
        doc.body.insertBefore(node, insertionPoint);
      }
    }
  }

  /// Loads an asset identified by [id], visits its imports and collects its
  /// html imports. Then inlines it into the main document.
  Future _inlineImport(AssetId id, Element link) {
    return readAsHtml(id, transform).then((doc) {
      new _UrlNormalizer(transform, id).visit(doc);
      return _visitImports(doc).then((_) {
        _extractScripts(doc, id);
        _removeScripts(doc);

        // TODO(jmesserly): figure out how this is working in vulcanizer.
        // Do they produce a <body> tag with a <head> and <body> inside?
        var imported = new DocumentFragment();
        imported.nodes..addAll(doc.head.nodes)..addAll(doc.body.nodes);
        link.replaceWith(imported);
      });
    });
  }

  Future _inlineStylesheet(AssetId id, Element link) {
    return transform.readInputAsString(id).then((css) {
      css = new _UrlNormalizer(transform, id).visitCss(css);
      link.replaceWith(new Element.tag('style')..text = css);
    });
  }

  /// Remove scripts from HTML imports, and remember their [AssetId]s for later
  /// use.
  ///
  /// Dartium only allows a single script tag per page, so we can't inline
  /// the script tags. Instead we remove them entirely.
  void _removeScripts(Document doc) {
    for (var script in doc.querySelectorAll('script')) {
      if (script.attributes['type'] == TYPE_DART) {
        script.remove();
        var src = script.attributes['src'];
        scriptIds.add(uriToAssetId(docId, src, logger, script.sourceSpan));

        // only the first script needs to be added.
        // others are already removed by _extractScripts
        return;
      }
    }
  }

  /// Split inline scripts into their own files. We need to do this for dart2js
  /// to be able to compile them.
  ///
  /// This also validates that there weren't any duplicate scripts.
  bool _extractScripts(Document doc, AssetId sourceId) {
    bool changed = false;
    bool first = true;
    for (var script in doc.querySelectorAll('script')) {
      if (script.attributes['type'] != TYPE_DART) continue;

      // only one Dart script per document is supported in Dartium.
      if (!first) {
        // Remove the script. It's invalid to have more than one in Dartium.
        script.remove();
        changed = true;

        // TODO(jmesserly): remove this when we are running linter.
        logger.warning('more than one Dart script per HTML '
            'document is not supported. Script will be ignored.',
            span: script.sourceSpan);
        continue;
      }

      first = false;

      var src = script.attributes['src'];
      if (src != null) continue;

      final filename = path.url.basename(docId.path);
      final count = inlineScriptCounter++;
      var code = script.text;
      // TODO(sigmund): ensure this path is unique (dartbug.com/12618).
      script.attributes['src'] = src = '$filename.$count.dart';
      script.text = '';
      changed = true;

      var newId = docId.addExtension('.$count.dart');
      // TODO(jmesserly): consolidate this check with our other parsing of the
      // Dart code, so we only parse it once.
      if (!_hasLibraryDirective(code)) {
        // Inject a library tag with an appropriate library name.

        // Transform AssetId into a package name. For example:
        //   myPkgName|lib/foo/bar.html -> myPkgName.foo.bar_html
        //   myPkgName|web/foo/bar.html -> myPkgName.web.foo.bar_html
        // This should roughly match the recommended library name conventions.
        var libName = '${path.withoutExtension(sourceId.path)}_'
            '${path.extension(sourceId.path).substring(1)}';
        if (libName.startsWith('lib/')) libName = libName.substring(4);
        libName = libName.replaceAll('/', '.').replaceAll('-', '_');
        libName = '${sourceId.package}.$libName';

        code = "library $libName;\n$code";
      }
      transform.addOutput(new Asset.fromString(newId, code));
    }
    return changed;
  }
}

/// Parse [code] and determine whether it has a library directive.
bool _hasLibraryDirective(String code) =>
    parseCompilationUnit(code).directives.any((d) => d is LibraryDirective);


/// Recursively inlines the contents of HTML imports. Produces as output a
/// single HTML file that inlines the polymer-element definitions, and a text
/// file that contains, in order, the URIs to each library that sourced in a
/// script tag.
///
/// This transformer assumes that all script tags point to external files. To
/// support script tags with inlined code, use this transformer after running
/// [InlineCodeExtractor] on an earlier phase.
class ImportInliner extends Transformer {
  final TransformOptions options;

  ImportInliner(this.options);

  /// Only run on entry point .html files.
  Future<bool> isPrimary(Asset input) =>
      new Future.value(options.isHtmlEntryPoint(input.id));

  Future apply(Transform transform) =>
      new _HtmlInliner(options, transform).apply();
}

const TYPE_DART = 'application/dart';
const TYPE_JS = 'text/javascript';

/// Internally adjusts urls in the html that we are about to inline.
class _UrlNormalizer extends TreeVisitor {
  final Transform transform;

  /// Asset where the original content (and original url) was found.
  final AssetId sourceId;

  _UrlNormalizer(this.transform, this.sourceId);

  visitElement(Element node) {
    node.attributes.forEach((name, value) {
      if (_urlAttributes.contains(name)) {
        if (value != '' && !value.trim().startsWith('{{')) {
          node.attributes[name] = _newUrl(value, node.sourceSpan);
        }
      }
    });
    if (node.localName == 'style') {
      node.text = visitCss(node.text);
    } else if (node.localName == 'script' &&
        node.attributes['type'] == TYPE_DART) {
      // TODO(jmesserly): we might need to visit JS too to handle ES Harmony
      // modules.
      node.text = visitInlineDart(node.text);
    }
    super.visitElement(node);
  }

  static final _URL = new RegExp(r'url\(([^)]*)\)', multiLine: true);
  static final _QUOTE = new RegExp('["\']', multiLine: true);

  /// Visit the CSS text and replace any relative URLs so we can inline it.
  // Ported from:
  // https://github.com/Polymer/vulcanize/blob/c14f63696797cda18dc3d372b78aa3378acc691f/lib/vulcan.js#L149
  // TODO(jmesserly): use csslib here instead? Parsing with RegEx is sadness.
  // Maybe it's reliable enough for finding URLs in CSS? I'm not sure.
  String visitCss(String cssText) {
    var url = spanUrlFor(sourceId, transform);
    var src = new SourceFile.text(url, cssText);
    return cssText.replaceAllMapped(_URL, (match) {
      // Extract the URL, without any surrounding quotes.
      var span = src.span(match.start, match.end);
      var href = match[1].replaceAll(_QUOTE, '');
      href = _newUrl(href, span);
      return 'url($href)';
    });
  }

  String visitInlineDart(String code) {
    var unit = parseCompilationUnit(code);
    var file = new SourceFile.text(spanUrlFor(sourceId, transform), code);
    var output = new TextEditTransaction(code, file);

    for (Directive directive in unit.directives) {
      if (directive is UriBasedDirective) {
        var uri = directive.uri.stringValue;
        var span = _getSpan(file, directive.uri);

        var id = uriToAssetId(sourceId, uri, transform.logger, span,
            errorOnAbsolute: false);
        if (id == null) continue;

        var primaryId = transform.primaryInput.id;
        var newUri = assetUrlFor(id, primaryId, transform.logger);
        if (newUri != uri) {
          output.edit(span.start.offset, span.end.offset, "'$newUri'");
        }
      }
    }

    if (!output.hasEdits) return code;

    // TODO(sigmund): emit source maps when barback supports it (see
    // dartbug.com/12340)
    return (output.commit()..build(file.url)).text;
  }

  String _newUrl(String href, Span span) {
    var uri = Uri.parse(href);
    if (uri.isAbsolute) return href;
    if (!uri.scheme.isEmpty) return href;
    if (!uri.host.isEmpty) return href;
    if (uri.path.isEmpty) return href;  // Implies standalone ? or # in URI.
    if (path.isAbsolute(href)) return href;

    var id = uriToAssetId(sourceId, href, transform.logger, span);
    if (id == null) return href;
    var primaryId = transform.primaryInput.id;

    if (id.path.startsWith('lib/')) {
      return 'packages/${id.package}/${id.path.substring(4)}';
    }

    if (id.path.startsWith('asset/')) {
      return 'assets/${id.package}/${id.path.substring(6)}';
    }

    if (primaryId.package != id.package) {
      // Techincally we shouldn't get there
      transform.logger.error("don't know how to include $id from $primaryId",
          span: span);
      return href;
    }

    var builder = path.url;
    return builder.relative(builder.join('/', id.path),
        from: builder.join('/', builder.dirname(primaryId.path)));
  }
}

/// HTML attributes that expect a URL value.
/// <http://dev.w3.org/html5/spec/section-index.html#attributes-1>
///
/// Every one of these attributes is a URL in every context where it is used in
/// the DOM. The comments show every DOM element where an attribute can be used.
const _urlAttributes = const [
  'action',     // in form
  'background', // in body
  'cite',       // in blockquote, del, ins, q
  'data',       // in object
  'formaction', // in button, input
  'href',       // in a, area, link, base, command
  'icon',       // in command
  'manifest',   // in html
  'poster',     // in video
  'src',        // in audio, embed, iframe, img, input, script, source, track,
                //    video
];

_getSpan(SourceFile file, AstNode node) => file.span(node.offset, node.end);
