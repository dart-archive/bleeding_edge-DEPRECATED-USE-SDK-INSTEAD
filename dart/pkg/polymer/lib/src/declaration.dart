// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of polymer;

/// *Warning* this class is experimental and subject to change.
///
/// The data associated with a polymer-element declaration, if it is backed
/// by a Dart class instead of a JavaScript prototype.
class PolymerDeclaration {
  /// The polymer-element for this declaration.
  final HtmlElement element;

  /// The Dart type corresponding to this custom element declaration.
  final Type type;

  /// If we extend another custom element, this points to the super declaration.
  final PolymerDeclaration superDeclaration;

  /// The name of the custom element.
  final String name;

  /// Map of publish properties. Can be a field or a property getter, but if
  /// this map contains a getter, is because it also has a corresponding setter.
  ///
  /// Note: technically these are always single properties, so we could use a
  /// Symbol instead of a PropertyPath. However there are lookups between this
  /// map and [_observe] so it is easier to just track paths.
  Map<PropertyPath, smoke.Declaration> _publish;

  /// The names of published properties for this polymer-element.
  Iterable<String> get publishedProperties =>
      _publish != null ? _publish.keys.map((p) => '$p') : const [];

  /// Same as [_publish] but with lower case names.
  Map<String, smoke.Declaration> _publishLC;

  Map<PropertyPath, List<Symbol>> _observe;

  Map<String, Object> _instanceAttributes;

  List<Element> _sheets;
  List<Element> get sheets => _sheets;

  List<Element> _styles;
  List<Element> get styles => _styles;

  DocumentFragment get templateContent {
    final template = element.querySelector('template');
    return template != null ? templateBind(template).content : null;
  }

  /// Maps event names and their associated method in the element class.
  final Map<String, String> _eventDelegates = {};

  /// Expected events per element node.
  // TODO(sigmund): investigate whether we need more than 1 set of local events
  // per element (why does the js implementation stores 1 per template node?)
  Expando<Set<String>> _templateDelegates;

  String get extendee => superDeclaration != null ?
      superDeclaration.name : null;

  // Dart note: since polymer-element is handled in JS now, we have a simplified
  // flow for registering. We don't need to wait for the supertype or the code
  // to be noticed.
  PolymerDeclaration(this.element, this.name, this.type, this.superDeclaration);

  void register() {
    // build prototype combining extendee, Polymer base, and named api
    buildType();

    // back reference declaration element
    // TODO(sjmiles): replace `element` with `elementElement` or `declaration`
    _declarations[name] = this;

    // more declarative features
    desugar();
    // register our custom element
    registerType(name);

    // NOTE: skip in Dart because we don't have mutable global scope.
    // reference constructor in a global named by 'constructor' attribute
    // publishConstructor();
  }

  /// Gets the Dart type registered for this name, and sets up declarative
  /// features. Fills in the [type] and [supertype] fields.
  ///
  /// *Note*: unlike the JavaScript version, we do not have to metaprogram the
  /// prototype, which simplifies this method.
  void buildType() {
    // transcribe `attributes` declarations onto own prototype's `publish`
    publishAttributes(superDeclaration);

    publishProperties();

    inferObservers();

    // desugar compound observer syntax, e.g. @ObserveProperty('a b c')
    explodeObservers();

    // Skip the rest in Dart:
    // chain various meta-data objects to inherited versions
    // chain custom api to inherited
    // build side-chained lists to optimize iterations
    // inherit publishing meta-data
    // x-platform fixup
  }

  /// Implement various declarative features.
  void desugar() {
    // compile list of attributes to copy to instances
    accumulateInstanceAttributes();
    // parse on-* delegates declared on `this` element
    parseHostEvents();
    // install external stylesheets as if they are inline
    installSheets();

    adjustShadowElement();

    // TODO(sorvell): install a helper method this.resolvePath to aid in
    // setting resource paths. e.g.
    // this.$.image.src = this.resolvePath('images/foo.png')
    // Potentially remove when spec bug is addressed.
    // https://www.w3.org/Bugs/Public/show_bug.cgi?id=21407
    // TODO(jmesserly): resolvePath not ported, see first comment in this class.

    // under ShadowDOMPolyfill, transforms to approximate missing CSS features
    _shimShadowDomStyling(templateContent, name, extendee);

    // TODO(jmesserly): this feels unnatrual in Dart. Since we have convenient
    // lazy static initialization, can we get by without it?
    if (smoke.hasStaticMethod(type, #registerCallback)) {
      smoke.invoke(type, #registerCallback, [this]);
    }
  }

  // TODO(sorvell): remove when spec addressed:
  // https://www.w3.org/Bugs/Public/show_bug.cgi?id=22460
  // make <shadow></shadow> be <shadow><content></content></shadow>
  void adjustShadowElement() {
    // TODO(sorvell): avoid under SD polyfill until this bug is addressed:
    // https://github.com/Polymer/ShadowDOM/issues/297
    if (!_hasShadowDomPolyfill) {
      final content = templateContent;
      if (content == null) return;

      for (var s in content.querySelectorAll('shadow')) {
        if (s.nodes.isEmpty) s.append(new ContentElement());
      }
    }
  }

  void registerType(String name) {
    var baseTag;
    var decl = this;
    while (decl != null) {
      baseTag = decl.element.attributes['extends'];
      decl = decl.superDeclaration;
    }
    document.register(name, type, extendsTag: baseTag);
  }

  void publishAttributes(PolymerDeclaration superDecl) {
    // get properties to publish
    if (superDecl != null && superDecl._publish != null) {
      // Dart note: even though we walk the type hierarchy in
      // _getPublishedProperties, this will additionally include any names
      // published via the `attributes` attribute.
      _publish = new Map.from(superDecl._publish);
    }

    _publish = _getPublishedProperties(type, _publish);

    // merge names from 'attributes' attribute
    var attrs = element.attributes['attributes'];
    if (attrs != null) {
      // names='a b c' or names='a,b,c'
      // record each name for publishing
      for (var attr in attrs.split(_ATTRIBUTES_REGEX)) {
        // remove excess ws
        attr = attr.trim();

        // do not override explicit entries
        if (attr == '') continue;

        var property = smoke.nameToSymbol(attr);
        var path = new PropertyPath([property]);
        if (_publish != null && _publish.containsKey(path)) {
          continue;
        }

        var decl = smoke.getDeclaration(type, property);
        if (decl == null || decl.isMethod || decl.isFinal) {
          window.console.warn('property for attribute $attr of polymer-element '
              'name=$name not found.');
          continue;
        }
        if (_publish == null) _publish = {};
        _publish[path] = decl;
      }
    }

    // NOTE: the following is not possible in Dart; fields must be declared.
    // install 'attributes' as properties on the prototype,
    // but don't override
  }

  void accumulateInstanceAttributes() {
    // inherit instance attributes
    _instanceAttributes = new Map<String, Object>();
    if (superDeclaration != null) {
      _instanceAttributes.addAll(superDeclaration._instanceAttributes);
    }

    // merge attributes from element
    element.attributes.forEach((name, value) {
      if (isInstanceAttribute(name)) {
        _instanceAttributes[name] = value;
      }
    });
  }

  static bool isInstanceAttribute(name) {
    // do not clone these attributes onto instances
    final blackList = const {
        'name': 1, 'extends': 1, 'constructor': 1, 'noscript': 1,
        'attributes': 1};

    return !blackList.containsKey(name) && !name.startsWith('on-');
  }

  /// Extracts events from the element tag attributes.
  void parseHostEvents() {
    addAttributeDelegates(_eventDelegates);
  }

  void addAttributeDelegates(Map<String, String> delegates) {
    element.attributes.forEach((name, value) {
      if (_hasEventPrefix(name)) {
        var start = value.indexOf('{{');
        var end = value.lastIndexOf('}}');
        if (start >= 0 && end >= 0) {
          delegates[_removeEventPrefix(name)] =
              value.substring(start + 2, end).trim();
        }
      }
    });
  }

  String urlToPath(String url) {
    if (url == null) return '';
    return (url.split('/')..removeLast()..add('')).join('/');
  }

  /// Install external stylesheets loaded in <element> elements into the
  /// element's template.
  void installSheets() {
    cacheSheets();
    cacheStyles();
    installLocalSheets();
    installGlobalStyles();
  }

  void cacheSheets() {
    _sheets = findNodes(_SHEET_SELECTOR);
    for (var s in sheets) s.remove();
  }

  void cacheStyles() {
    _styles = findNodes('$_STYLE_SELECTOR[$_SCOPE_ATTR]');
    for (var s in styles) s.remove();
  }

  /// Takes external stylesheets loaded in an `<element>` element and moves
  /// their content into a style element inside the `<element>`'s template.
  /// The sheet is then removed from the `<element>`. This is done only so
  /// that if the element is loaded in the main document, the sheet does
  /// not become active.
  /// Note, ignores sheets with the attribute 'polymer-scope'.
  void installLocalSheets() {
    var sheets = this.sheets.where(
        (s) => !s.attributes.containsKey(_SCOPE_ATTR));
    var content = templateContent;
    if (content != null) {
      var cssText = new StringBuffer();
      for (var sheet in sheets) {
        cssText..write(_cssTextFromSheet(sheet))..write('\n');
      }
      if (cssText.length > 0) {
        content.insertBefore(
            new StyleElement()..text = '$cssText',
            content.firstChild);
      }
    }
  }

  List<Element> findNodes(String selector, [bool matcher(Element e)]) {
    var nodes = element.querySelectorAll(selector).toList();
    var content = templateContent;
    if (content != null) {
      nodes = nodes..addAll(content.querySelectorAll(selector));
    }
    if (matcher != null) return nodes.where(matcher).toList();
    return nodes;
  }

  /// Promotes external stylesheets and style elements with the attribute
  /// polymer-scope='global' into global scope.
  /// This is particularly useful for defining @keyframe rules which
  /// currently do not function in scoped or shadow style elements.
  /// (See wkb.ug/72462)
  // TODO(sorvell): remove when wkb.ug/72462 is addressed.
  void installGlobalStyles() {
    var style = styleForScope(_STYLE_GLOBAL_SCOPE);
    Polymer.applyStyleToScope(style, document.head);
  }

  String cssTextForScope(String scopeDescriptor) {
    var cssText = new StringBuffer();
    // handle stylesheets
    var selector = '[$_SCOPE_ATTR=$scopeDescriptor]';
    matcher(s) => s.matches(selector);

    for (var sheet in sheets.where(matcher)) {
      cssText..write(_cssTextFromSheet(sheet))..write('\n\n');
    }
    // handle cached style elements
    for (var style in styles.where(matcher)) {
      cssText..write(style.text)..write('\n\n');
    }
    return cssText.toString();
  }

  StyleElement styleForScope(String scopeDescriptor) {
    var cssText = cssTextForScope(scopeDescriptor);
    return cssTextToScopeStyle(cssText, scopeDescriptor);
  }

  StyleElement cssTextToScopeStyle(String cssText, String scopeDescriptor) {
    if (cssText == '') return null;

    return new StyleElement()
        ..text = cssText
        ..attributes[_STYLE_SCOPE_ATTRIBUTE] = '$name-$scopeDescriptor';
  }

  /// Fetch a list of all *Changed methods so we can observe the associated
  /// properties.
  void inferObservers() {
    for (var decl in smoke.query(type, _changedMethodQueryOptions)) {
      // TODO(jmesserly): now that we have a better system, should we
      // deprecate *Changed methods?
      if (_observe == null) _observe = new HashMap();
      var name = smoke.symbolToName(decl.name);
      name = name.substring(0, name.length - 7);
      _observe[new PropertyPath(name)] = [decl.name];
    }
  }

  /// Fetch a list of all methods annotated with [ObserveProperty] so we can
  /// observe the associated properties.
  void explodeObservers() {
    var options = const smoke.QueryOptions(includeFields: false,
        includeProperties: false, includeMethods: true, includeInherited: true,
        includeUpTo: HtmlElement, withAnnotations: const [ObserveProperty]);
    for (var decl in smoke.query(type, options)) {
      for (var meta in decl.annotations) {
        if (meta is! ObserveProperty) continue;
        if (_observe == null) _observe = new HashMap();
        for (String name in meta.names) {
          _observe.putIfAbsent(new PropertyPath(name), () => []).add(decl.name);
        }
      }
    }
  }

  void publishProperties() {
    // Dart note: _publish was already populated by publishAttributes
    if (_publish != null) _publishLC = _lowerCaseMap(_publish);
  }

  Map<String, dynamic> _lowerCaseMap(Map<PropertyPath, dynamic> properties) {
    final map = new Map<String, dynamic>();
    properties.forEach((PropertyPath path, value) {
      map['$path'.toLowerCase()] = value;
    });
    return map;
  }
}

/// maps tag names to prototypes
final Map _typesByName = new Map<String, Type>();

Type _getRegisteredType(String name) => _typesByName[name];

/// track document.register'ed tag names and their declarations
final Map _declarations = new Map<String, PolymerDeclaration>();

bool _isRegistered(String name) => _declarations.containsKey(name);
PolymerDeclaration _getDeclaration(String name) => _declarations[name];

Map<PropertyPath, smoke.Declaration> _getPublishedProperties(
    Type type, Map<PropertyPath, smoke.Declaration> props) {
  var options = const smoke.QueryOptions(includeInherited: true,
      includeUpTo: HtmlElement, withAnnotations: const [PublishedProperty]);
  for (var decl in smoke.query(type, options)) {
    if (decl.isFinal) continue;
    if (props == null) props = {};
    props[new PropertyPath([decl.name])] = decl;
  }
  return props;
}

/// Attribute prefix used for declarative event handlers.
const _EVENT_PREFIX = 'on-';

/// Whether an attribute declares an event.
bool _hasEventPrefix(String attr) => attr.startsWith(_EVENT_PREFIX);

String _removeEventPrefix(String name) => name.substring(_EVENT_PREFIX.length);

/// Using Polymer's platform/src/ShadowCSS.js passing the style tag's content.
void _shimShadowDomStyling(DocumentFragment template, String name,
    String extendee) {
  if (template == null || !_hasShadowDomPolyfill) return;

  var platform = js.context['Platform'];
  if (platform == null) return;
  var shadowCss = platform['ShadowCSS'];
  if (shadowCss == null) return;
  shadowCss.callMethod('shimStyling', [template, name, extendee]);
}

final bool _hasShadowDomPolyfill = js.context.hasProperty('ShadowDOMPolyfill');

const _STYLE_SELECTOR = 'style';
const _SHEET_SELECTOR = '[rel=stylesheet]';
const _STYLE_GLOBAL_SCOPE = 'global';
const _SCOPE_ATTR = 'polymer-scope';
const _STYLE_SCOPE_ATTRIBUTE = 'element';
const _STYLE_CONTROLLER_SCOPE = 'controller';

String _cssTextFromSheet(LinkElement sheet) {
  if (sheet == null) return '';

  // In deploy mode we should never do a sync XHR; link rel=stylesheet will
  // be inlined into a <style> tag by ImportInliner.
  if (loader.deployMode) return '';

  // TODO(jmesserly): sometimes the href property is wrong after deployment.
  var href = sheet.href;
  if (href == '') href = sheet.attributes["href"];

  // TODO(jmesserly): it seems like polymer-js is always polyfilling
  // HTMLImports, because their code depends on "__resource" to work, so I
  // don't see how it can work with native HTML Imports. We use a sync-XHR
  // under the assumption that the file is likely to have been already
  // downloaded and cached by HTML Imports.
  try {
    return (new HttpRequest()
        ..open('GET', href, async: false)
        ..send())
        .responseText;
  } on DomException catch (e, t) {
    _sheetLog.fine('failed to XHR stylesheet text href="$href" error: '
        '$e, trace: $t');
    return '';
  }
}

final Logger _sheetLog = new Logger('polymer.stylesheet');


final smoke.QueryOptions _changedMethodQueryOptions = new smoke.QueryOptions(
    includeFields: false, includeProperties: false, includeMethods: true,
    includeInherited: true, includeUpTo: HtmlElement,
    matches: _isObserverMethod);

bool _isObserverMethod(Symbol symbol) {
  String name = smoke.symbolToName(symbol);
  if (name == null) return false;
  return name.endsWith('Changed') && name != 'attributeChanged';
}

// TODO(jmesserly): is this list complete?
final _eventTranslations = const {
  // TODO(jmesserly): these three Polymer.js translations won't work in Dart,
  // because we strip the webkit prefix (below). Reconcile.
  'webkitanimationstart': 'webkitAnimationStart',
  'webkitanimationend': 'webkitAnimationEnd',
  'webkittransitionend': 'webkitTransitionEnd',

  'domfocusout': 'DOMFocusOut',
  'domfocusin': 'DOMFocusIn',
  'dommousescroll': 'DOMMouseScroll',

  // TODO(jmesserly): Dart specific renames. Reconcile with Polymer.js
  'animationend': 'webkitAnimationEnd',
  'animationiteration': 'webkitAnimationIteration',
  'animationstart': 'webkitAnimationStart',
  'doubleclick': 'dblclick',
  'fullscreenchange': 'webkitfullscreenchange',
  'fullscreenerror': 'webkitfullscreenerror',
  'keyadded': 'webkitkeyadded',
  'keyerror': 'webkitkeyerror',
  'keymessage': 'webkitkeymessage',
  'needkey': 'webkitneedkey',
  'speechchange': 'webkitSpeechChange',
};

final _reverseEventTranslations = () {
  final map = new Map<String, String>();
  _eventTranslations.forEach((onName, eventType) {
    map[eventType] = onName;
  });
  return map;
}();

// Dart note: we need this function because we have additional renames JS does
// not have. The JS renames are simply case differences, whereas we have ones
// like doubleclick -> dblclick and stripping the webkit prefix.
String _eventNameFromType(String eventType) {
  final result = _reverseEventTranslations[eventType];
  return result != null ? result : eventType;
}

final _ATTRIBUTES_REGEX = new RegExp(r'\s|,');
