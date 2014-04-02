// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of dart2js;

/**
 * [CompilerTask] for loading libraries and setting up the import/export scopes.
 *
 * The library loader uses four different kinds of URIs in different parts of
 * the loading process.
 *
 * ## User URI ##
 *
 * A 'user URI' is a URI provided by the user in code and as the main entry URI
 * at the command line. These generally come in 3 versions:
 *
 *   * A relative URI such as 'foo.dart', '../bar.dart', and 'baz/boz.dart'.
 *
 *   * A dart URI such as 'dart:core' and 'dart:_js_helper'.
 *
 *   * A package URI such as 'package:foo.dart' and 'package:bar/baz.dart'.
 *
 * A user URI can also be absolute, like 'file:///foo.dart' or
 * 'http://example.com/bar.dart', but such URIs cannot necessarily be used for
 * locating source files, since the scheme must be supported by the input
 * provider. The standard input provider for dart2js only supports the 'file'
 * scheme.
 *
 * ## Resolved URI ##
 *
 * A 'resolved URI' is a (user) URI that has been resolved to an absolute URI
 * based on the readable URI (see below) from which it was loaded. A URI with an
 * explicit scheme (such as 'dart:', 'package:' or 'file:') is already resolved.
 * A relative URI like for instance '../foo/bar.dart' is translated into an
 * resolved URI in one of three ways:
 *
 *  * If provided as the main entry URI at the command line, the URI is resolved
 *    relative to the current working directory, say
 *    'file:///current/working/dir/', and the resolved URI is therefore
 *    'file:///current/working/foo/bar.dart'.
 *
 *  * If the relative URI is provided in an import, export or part tag, and the
 *    readable URI of the enclosing compilation unit is a file URI,
 *    'file://some/path/baz.dart', then the resolved URI is
 *    'file://some/foo/bar.dart'.
 *
 *  * If the relative URI is provided in an import, export or part tag, and the
 *    readable URI of the enclosing compilation unit is a package URI,
 *    'package:some/path/baz.dart', then the resolved URI is
 *    'package:some/foo/bar.dart'.
 *
 * The resolved URI thus preserves the scheme through resolution: A readable
 * file URI results in an resolved file URI and a readable package URI results
 * in an resolved package URI. Note that since a dart URI is not a readable URI,
 * import, export or part tags within platform libraries are not interpreted as
 * dart URIs but instead relative to the library source file location.
 *
 * The resolved URI of a library is also used as the canonical URI
 * ([LibraryElement.canonicalUri]) by which we identify which libraries are
 * identical. This means that libraries loaded through the 'package' scheme will
 * resolve to the same library when loaded from within using relative URIs (see
 * for instance the test 'standalone/package/package1_test.dart'). But loading a
 * platform library using a relative URI will _not_ result in the same library
 * as when loaded through the dart URI.
 *
 * ## Readable URI ##
 *
 * A 'readable URI' is an absolute URI whose scheme is either 'package' or
 * something supported by the input provider, normally 'file'. Dart URIs such as
 * 'dart:core' and 'dart:_js_helper' are not readable themselves but are instead
 * resolved into a readable URI using the library root URI provided from the
 * command line and the list of platform libraries found in
 * 'sdk/lib/_internal/libraries.dart'. This is done through the
 * [Compiler.translateResolvedUri] method which checks whether a library by that
 * name exists and in case of internal libraries whether access is granted.
 *
 * ## Resource URI ##
 *
 * A 'resource URI' is an absolute URI with a scheme supported by the input
 * provider. For the standard implementation this means a URI with the 'file'
 * scheme. Readable URIs are converted into resource URIs as part of the
 * [Compiler.readScript] method. In the standard implementation the package URIs
 * are converted to file URIs using the package root URI provided on the
 * command line as base. If the package root URI is
 * 'file:///current/working/dir/' then the package URI 'package:foo/bar.dart'
 * will be resolved to the resource URI
 * 'file:///current/working/dir/foo/bar.dart'.
 *
 * The distinction between readable URI and resource URI is necessary to ensure
 * that these imports
 *
 *     import 'package:foo.dart' as a;
 *     import 'packages/foo.dart' as b;
 *
 * do _not_ resolve to the same library when the package root URI happens to
 * point to the 'packages' folder.
 *
 */
abstract class LibraryLoader extends CompilerTask {
  LibraryLoader(Compiler compiler) : super(compiler);

  /**
   * Loads the library specified by the [resolvedUri] and returns its
   * [LibraryElement].
   *
   * If the library is not already loaded, the method creates the
   * [LibraryElement] for the library and computes the import/export scope,
   * loading and computing the import/export scopes of all required libraries in
   * the process. The method handles cyclic dependency between libraries.
   *
   * This is the main entry point for [LibraryLoader].
   */
  // TODO(johnniwinther): Remove [canonicalUri] together with
  // [Compiler.scanBuiltinLibrary].
  Future<LibraryElement> loadLibrary(Uri resolvedUri, Node node,
                                     Uri canonicalUri);

  // TODO(johnniwinther): Remove this when patches don't need special parsing.
  Future registerLibraryFromTag(LibraryDependencyHandler handler,
                                LibraryElement library,
                                LibraryDependency tag);

  /**
   * Adds the elements in the export scope of [importedLibrary] to the import
   * scope of [importingLibrary].
   */
  // TODO(johnniwinther): Move handling of 'js_helper' to the library loader
  // to remove this method from the [LibraryLoader] interface.
  void importLibrary(LibraryElement importingLibrary,
                     LibraryElement importedLibrary,
                     Import tag);
}

/**
 * [CombinatorFilter] is a succinct representation of a list of combinators from
 * a library dependency tag.
 */
class CombinatorFilter {
  const CombinatorFilter();

  /**
   * Returns [:true:] if [element] is excluded by this filter.
   */
  bool exclude(Element element) => false;

  /**
   * Creates a filter based on the combinators of [tag].
   */
  factory CombinatorFilter.fromTag(LibraryDependency tag) {
    if (tag == null || tag.combinators == null) {
      return const CombinatorFilter();
    }

    // If the list of combinators contain at least one [:show:] we can create
    // a positive list of elements to include, otherwise we create a negative
    // list of elements to exclude.
    bool show = false;
    Set<String> nameSet;
    for (Combinator combinator in tag.combinators) {
      if (combinator.isShow) {
        show = true;
        var set = new Set<String>();
        for (Identifier identifier in combinator.identifiers) {
          set.add(identifier.source);
        }
        if (nameSet == null) {
          nameSet = set;
        } else {
          nameSet = nameSet.intersection(set);
        }
      }
    }
    if (nameSet == null) {
      nameSet = new Set<String>();
    }
    for (Combinator combinator in tag.combinators) {
      if (combinator.isHide) {
        for (Identifier identifier in combinator.identifiers) {
          if (show) {
            // We have a positive list => Remove hidden elements.
            nameSet.remove(identifier.source);
          } else {
            // We have no positive list => Accumulate hidden elements.
            nameSet.add(identifier.source);
          }
        }
      }
    }
    return show ? new ShowFilter(nameSet) : new HideFilter(nameSet);
  }
}

/**
 * A list of combinators represented as a list of element names to include.
 */
class ShowFilter extends CombinatorFilter {
  final Set<String> includedNames;

  ShowFilter(this.includedNames);

  bool exclude(Element element) => !includedNames.contains(element.name);
}

/**
 * A list of combinators represented as a list of element names to exclude.
 */
class HideFilter extends CombinatorFilter {
  final Set<String> excludedNames;

  HideFilter(this.excludedNames);

  bool exclude(Element element) => excludedNames.contains(element.name);
}

/**
 * Implementation class for [LibraryLoader]. The distinction between
 * [LibraryLoader] and [LibraryLoaderTask] is made to hide internal members from
 * the [LibraryLoader] interface.
 */
class LibraryLoaderTask extends LibraryLoader {
  LibraryLoaderTask(Compiler compiler) : super(compiler);
  String get name => 'LibraryLoader';
  List onLibraryLoadedCallbacks = [];

  final Map<Uri, LibraryElement> libraryResourceUriMap =
      new Map<Uri, LibraryElement>();
  final Map<String, LibraryElement> libraryNames =
      new Map<String, LibraryElement>();

  LibraryDependencyHandler currentHandler;

  Future<LibraryElement> loadLibrary(Uri resolvedUri, Node node,
      Uri canonicalUri) {
    return measure(() {
      assert(currentHandler == null);
      // TODO(johnniwinther): Ensure that currentHandler correctly encloses the
      // loading of a library cluster.
      currentHandler = new LibraryDependencyHandler(compiler);
      return createLibrary(currentHandler, null, resolvedUri, node,
          canonicalUri).then((LibraryElement library) {
        return compiler.withCurrentElement(library, () {
          return measure(() {
            currentHandler.computeExports();
            currentHandler = null;
            var workList = onLibraryLoadedCallbacks;
            onLibraryLoadedCallbacks = [];
            return Future.forEach(workList, (f) => f()).then((_) => library);
          });
        });
      });
    });
  }

  /**
   * Processes the library tags in [library].
   *
   * The imported/exported libraries are loaded and processed recursively but
   * the import/export scopes are not set up.
   */
  Future processLibraryTags(LibraryDependencyHandler handler,
                            LibraryElement library) {
    int tagState = TagState.NO_TAG_SEEN;

    /**
     * If [value] is less than [tagState] complain and return
     * [tagState]. Otherwise return the new value for [tagState]
     * (transition function for state machine).
     */
    int checkTag(int value, LibraryTag tag) {
      if (tagState > value) {
        compiler.reportFatalError(
            tag,
            MessageKind.GENERIC, {'text': 'Error: Out of order.'});
        return tagState;
      }
      return TagState.NEXT[value];
    }

    bool importsDartCore = false;
    var libraryDependencies = new LinkBuilder<LibraryDependency>();
    Uri base = library.entryCompilationUnit.script.readableUri;

    // TODO(rnystrom): Remove .toList() here if #11523 is fixed.
    return Future.forEach(library.tags.reverse().toList(), (LibraryTag tag) {
      return compiler.withCurrentElement(library, () {
        if (tag.isImport) {
          Import import = tag;
          tagState = checkTag(TagState.IMPORT_OR_EXPORT, import);
          if (import.uri.dartString.slowToString() == 'dart:core') {
            importsDartCore = true;
          }
          libraryDependencies.addLast(import);
        } else if (tag.isExport) {
          tagState = checkTag(TagState.IMPORT_OR_EXPORT, tag);
          libraryDependencies.addLast(tag);
        } else if (tag.isLibraryName) {
          tagState = checkTag(TagState.LIBRARY, tag);
          if (library.libraryTag != null) {
            compiler.internalError(tag, "Duplicated library declaration.");
          } else {
            library.libraryTag = tag;
          }
        } else if (tag.isPart) {
          Part part = tag;
          StringNode uri = part.uri;
          Uri resolvedUri = base.resolve(uri.dartString.slowToString());
          tagState = checkTag(TagState.SOURCE, part);
          return scanPart(part, resolvedUri, library);
        } else {
          compiler.internalError(tag, "Unhandled library tag.");
        }
      });
    }).then((_) {
      return compiler.withCurrentElement(library, () {
        checkDuplicatedLibraryName(library);
        // Apply patch, if any.
        if (library.isPlatformLibrary) {
          return patchDartLibrary(handler, library, library.canonicalUri.path);
        }
      });
    }).then((_) {
      return compiler.withCurrentElement(library, () {
        // Import dart:core if not already imported.
        if (!importsDartCore && !isDartCore(library.canonicalUri)) {
          return loadCoreLibrary(handler).then((LibraryElement coreLibrary) {
            handler.registerDependency(library, null, coreLibrary);
          });
        }
      });
    }).then((_) {
      // TODO(rnystrom): Remove .toList() here if #11523 is fixed.
      return Future.forEach(libraryDependencies.toLink().toList(), (tag) {
        return compiler.withCurrentElement(library, () {
          return registerLibraryFromTag(handler, library, tag);
        });
      });
    });
  }

  void checkDuplicatedLibraryName(LibraryElement library) {
    Uri resourceUri = library.entryCompilationUnit.script.resourceUri;
    LibraryName tag = library.libraryTag;
    LibraryElement existing =
        libraryResourceUriMap.putIfAbsent(resourceUri, () => library);
    if (!identical(existing, library)) {
      if (tag != null) {
        compiler.withCurrentElement(library, () {
          compiler.reportWarning(tag.name,
              MessageKind.DUPLICATED_LIBRARY_RESOURCE,
              {'libraryName': tag.name,
               'resourceUri': resourceUri,
               'canonicalUri1': library.canonicalUri,
               'canonicalUri2': existing.canonicalUri});
        });
      } else {
        compiler.reportHint(library,
            MessageKind.DUPLICATED_RESOURCE,
            {'resourceUri': resourceUri,
             'canonicalUri1': library.canonicalUri,
             'canonicalUri2': existing.canonicalUri});
      }
    } else if (tag != null) {
      String name = library.getLibraryOrScriptName();
      existing = libraryNames.putIfAbsent(name, () => library);
      if (!identical(existing, library)) {
        compiler.withCurrentElement(library, () {
          compiler.reportWarning(tag.name,
              MessageKind.DUPLICATED_LIBRARY_NAME,
              {'libraryName': name});
        });
        compiler.withCurrentElement(existing, () {
          compiler.reportWarning(existing.libraryTag.name,
              MessageKind.DUPLICATED_LIBRARY_NAME,
              {'libraryName': name});
        });
      }
    }
  }

  bool isDartCore(Uri uri) => uri.scheme == "dart" && uri.path == "core";

  /**
   * Lazily loads and returns the [LibraryElement] for the dart:core library.
   */
  Future<LibraryElement> loadCoreLibrary(LibraryDependencyHandler handler) {
    if (compiler.coreLibrary != null) {
      return new Future.value(compiler.coreLibrary);
    }

    Uri coreUri = new Uri(scheme: 'dart', path: 'core');
    return createLibrary(handler, null, coreUri, null, coreUri)
        .then((LibraryElement library) {
      compiler.coreLibrary = library;
      return library;
    });
  }

  Future patchDartLibrary(LibraryDependencyHandler handler,
                        LibraryElement library, String dartLibraryPath) {
    if (library.isPatched) return new Future.value();
    Uri patchUri = compiler.resolvePatchUri(dartLibraryPath);
    if (patchUri == null) return new Future.value();

    return compiler.patchParser.patchLibrary(handler, patchUri, library);
  }

  /**
   * Handle a part tag in the scope of [library]. The [resolvedUri] given is
   * used as is, any URI resolution should be done beforehand.
   */
  Future scanPart(Part part, Uri resolvedUri, LibraryElement library) {
    if (!resolvedUri.isAbsolute) throw new ArgumentError(resolvedUri);
    Uri readableUri = compiler.translateResolvedUri(library, resolvedUri, part);
    if (readableUri == null) return new Future.value();
    return compiler.withCurrentElement(library, () {
      return compiler.readScript(part, readableUri).
          then((Script sourceScript) {
            if (sourceScript == null) return;

            CompilationUnitElement unit =
                new CompilationUnitElementX(sourceScript, library);
            compiler.withCurrentElement(unit, () {
              compiler.scanner.scan(unit);
              if (unit.partTag == null) {
                compiler.reportError(unit, MessageKind.MISSING_PART_OF_TAG);
              }
            });
          });
    });
  }

  /**
   * Handle an import/export tag by loading the referenced library and
   * registering its dependency in [handler] for the computation of the import/
   * export scope.
   */
  Future registerLibraryFromTag(LibraryDependencyHandler handler,
                                LibraryElement library,
                                LibraryDependency tag) {
    Uri base = library.entryCompilationUnit.script.readableUri;
    Uri resolvedUri = base.resolve(tag.uri.dartString.slowToString());
    return createLibrary(handler, library, resolvedUri, tag.uri, resolvedUri)
        .then((LibraryElement loadedLibrary) {
          if (loadedLibrary == null) return;
          compiler.withCurrentElement(library, () {
            handler.registerDependency(library, tag, loadedLibrary);
          });
        });
  }

  /**
   * Create (or reuse) a library element for the library specified by the
   * [resolvedUri].
   *
   * If a new library is created, the [handler] is notified.
   */
  // TODO(johnniwinther): Remove [canonicalUri] and make [resolvedUri] the
  // canonical uri when [Compiler.scanBuiltinLibrary] is removed.
  Future<LibraryElement> createLibrary(LibraryDependencyHandler handler,
                                       LibraryElement importingLibrary,
                                       Uri resolvedUri,
                                       Node node,
                                       Uri canonicalUri) {
    // TODO(johnniwinther): Create erroneous library elements for missing
    // libraries.
    Uri readableUri =
        compiler.translateResolvedUri(importingLibrary, resolvedUri, node);
    if (readableUri == null) return new Future.value();
    LibraryElement library;
    if (canonicalUri != null) {
      library = compiler.libraries[canonicalUri.toString()];
    }
    if (library != null) {
      return new Future.value(library);
    }
    return compiler.withCurrentElement(importingLibrary, () {
      return compiler.readScript(node, readableUri)
          .then((Script script) {
            if (script == null) return null;
            LibraryElement element = new LibraryElementX(script, canonicalUri);
            compiler.withCurrentElement(element, () {
              handler.registerNewLibrary(element);
              native.maybeEnableNative(compiler, element);
              if (canonicalUri != null) {
                compiler.libraries[canonicalUri.toString()] = element;
              }
              compiler.scanner.scanLibrary(element);
            });
            return processLibraryTags(handler, element).then((_) {
              compiler.withCurrentElement(element, () {
                handler.registerLibraryExports(element);
                onLibraryLoadedCallbacks.add(
                    () => compiler.onLibraryLoaded(element, resolvedUri));
              });
              return element;
            });
          });
    });
  }

  // TODO(johnniwinther): Remove this method when 'js_helper' is handled by
  // [LibraryLoaderTask].
  void importLibrary(LibraryElement importingLibrary,
                     LibraryElement importedLibrary,
                     Import tag) {
    new ImportLink(tag, importedLibrary).importLibrary(compiler,
                                                       importingLibrary);
  }
}


/**
 * The fields of this class models a state machine for checking script
 * tags come in the correct order.
 */
class TagState {
  static const int NO_TAG_SEEN = 0;
  static const int LIBRARY = 1;
  static const int IMPORT_OR_EXPORT = 2;
  static const int SOURCE = 3;
  static const int RESOURCE = 4;

  /** Next state. */
  static const List<int> NEXT =
      const <int>[NO_TAG_SEEN,
                  IMPORT_OR_EXPORT, // Only one library tag is allowed.
                  IMPORT_OR_EXPORT,
                  SOURCE,
                  RESOURCE];
}

/**
 * An [import] tag and the [importedLibrary] imported through [import].
 */
class ImportLink {
  final Import import;
  final LibraryElement importedLibrary;

  ImportLink(this.import, this.importedLibrary);

  /**
   * Imports the library into the [importingLibrary].
   */
  void importLibrary(Compiler compiler, LibraryElement importingLibrary) {
    assert(invariant(importingLibrary,
                     importedLibrary.exportsHandled,
                     message: 'Exports not handled on $importedLibrary'));
    var combinatorFilter = new CombinatorFilter.fromTag(import);
    if (import != null && import.prefix != null) {
      String prefix = import.prefix.source;
      Element existingElement = importingLibrary.find(prefix);
      PrefixElement prefixElement;
      if (existingElement == null || !existingElement.isPrefix()) {
        prefixElement = new PrefixElementX(prefix,
            importingLibrary.entryCompilationUnit, import.getBeginToken());
      } else {
        prefixElement = existingElement;
      }
      importingLibrary.addToScope(prefixElement, compiler);
      importedLibrary.forEachExport((Element element) {
        if (combinatorFilter.exclude(element)) return;
        prefixElement.addImport(element, import, compiler);
      });
      if (import.isDeferred) {
        prefixElement.addImport(
            new DeferredLoaderGetterElementX(prefixElement),
            import, compiler);
        // TODO(sigurdm): When we remove support for the annotation based
        // syntax the [PrefixElement] constructor should receive this
        // information.
        prefixElement.markAsDeferred(import);
      }
    } else {
      importedLibrary.forEachExport((Element element) {
        compiler.withCurrentElement(importingLibrary, () {
          if (combinatorFilter.exclude(element)) return;
          importingLibrary.addImport(element, import, compiler);
        });
      });
    }
  }
}

/**
 * The combinator filter computed from an export tag and the library dependency
 * node for the library that declared the export tag. This represents an edge in
 * the library dependency graph.
 */
class ExportLink {
  final Export export;
  final CombinatorFilter combinatorFilter;
  final LibraryDependencyNode exportNode;

  ExportLink(Export export, LibraryDependencyNode this.exportNode)
      : this.export = export,
        this.combinatorFilter = new CombinatorFilter.fromTag(export);

  /**
   * Exports [element] to the dependent library unless [element] is filtered by
   * the export combinators. Returns [:true:] if the set pending exports of the
   * dependent library was modified.
   */
  bool exportElement(Element element) {
    if (combinatorFilter.exclude(element)) return false;
    return exportNode.addElementToPendingExports(element, export);
  }
}

/**
 * A node in the library dependency graph.
 *
 * This class is used to collect the library dependencies expressed through
 * import and export tags, and as the work-list entry in computations of library
 * exports performed in [LibraryDependencyHandler.computeExports].
 */
class LibraryDependencyNode {
  final LibraryElement library;

  // TODO(ahe): Remove [hashCodeCounter] and [hashCode] when
  // VM implementation of Object.hashCode is not slow.
  final int hashCode = ++hashCodeCounter;
  static int hashCodeCounter = 0;


  /**
   * A linked list of the import tags that import [library] mapped to the
   * corresponding libraries. This is used to propagate exports into imports
   * after the export scopes have been computed.
   */
  Link<ImportLink> imports = const Link<ImportLink>();

  /**
   * A linked list of the export tags the dependent upon this node library.
   * This is used to propagate exports during the computation of export scopes.
   */
  Link<ExportLink> dependencies = const Link<ExportLink>();

  /**
   * The export scope for [library] which is gradually computed by the work-list
   * computation in [LibraryDependencyHandler.computeExports].
   */
  Map<String, Element> exportScope =
      new Map<String, Element>();

  /// Map from exported elements to the export directives that exported them.
  Map<Element, Link<Export>> exporters = new Map<Element, Link<Export>>();

  /**
   * The set of exported elements that need to be propageted to dependent
   * libraries as part of the work-list computation performed in
   * [LibraryDependencyHandler.computeExports]. Each export element is mapped
   * to a list of exports directives that export it.
   */
  Map<Element, Link<Export>> pendingExportMap =
      new Map<Element, Link<Export>>();

  LibraryDependencyNode(LibraryElement this.library);

  /**
   * Registers that the library of this node imports [importLibrary] through the
   * [import] tag.
   */
  void registerImportDependency(Import import,
                                LibraryElement importedLibrary) {
    imports = imports.prepend(new ImportLink(import, importedLibrary));
  }

  /**
   * Registers that the library of this node is exported by
   * [exportingLibraryNode] through the [export] tag.
   */
  void registerExportDependency(Export export,
                                LibraryDependencyNode exportingLibraryNode) {
    dependencies =
        dependencies.prepend(new ExportLink(export, exportingLibraryNode));
  }

  /**
   * Registers all non-private locally declared members of the library of this
   * node to be exported. This forms the basis for the work-list computation of
   * the export scopes performed in [LibraryDependencyHandler.computeExports].
   */
  void registerInitialExports() {
    for (Element element in library.getNonPrivateElementsInScope()) {
      pendingExportMap[element] = const Link<Export>();
    }
  }

  void registerHandledExports(LibraryElement exportedLibraryElement,
                              Export export,
                              CombinatorFilter filter) {
    assert(invariant(library, exportedLibraryElement.exportsHandled));
    for (Element exportedElement in exportedLibraryElement.exports) {
      if (!filter.exclude(exportedElement)) {
        Link<Export> exports =
            pendingExportMap.putIfAbsent(exportedElement,
                                         () => const Link<Export>());
        pendingExportMap[exportedElement] = exports.prepend(export);
      }
    }
  }

  /**
   * Registers the compute export scope with the node library.
   */
  void registerExports() {
    library.setExports(exportScope.values.toList());
  }

  /**
   * Registers the imports of the node library.
   */
  void registerImports(Compiler compiler) {
    for (ImportLink link in imports) {
      link.importLibrary(compiler, library);
    }
  }

  /**
   * Copies and clears pending export set for this node.
   */
  Map<Element, Link<Export>> pullPendingExports() {
    Map<Element, Link<Export>> pendingExports =
        new Map<Element, Link<Export>>.from(pendingExportMap);
    pendingExportMap.clear();
    return pendingExports;
  }

  /**
   * Adds [element] to the export scope for this node. If the [element] name
   * is a duplicate, an error element is inserted into the export scope.
   */
  Element addElementToExportScope(Compiler compiler, Element element,
                                  Link<Export> exports) {
    String name = element.name;

    void reportDuplicateExport(Element duplicate,
                               Link<Export> duplicateExports,
                               {bool reportError: true}) {
      assert(invariant(library, !duplicateExports.isEmpty,
          message: "No export for $duplicate from ${duplicate.getLibrary()} "
                   "in $library."));
      compiler.withCurrentElement(library, () {
        for (Export export in duplicateExports) {
          if (reportError) {
            compiler.reportError(export,
                MessageKind.DUPLICATE_EXPORT, {'name': name});
            reportError = false;
          } else {
            compiler.reportInfo(export,
                MessageKind.DUPLICATE_EXPORT_CONT, {'name': name});
          }
        }
      });
    }

    void reportDuplicateExportDecl(Element duplicate,
                                   Link<Export> duplicateExports) {
      assert(invariant(library, !duplicateExports.isEmpty,
          message: "No export for $duplicate from ${duplicate.getLibrary()} "
                   "in $library."));
      compiler.reportInfo(duplicate, MessageKind.DUPLICATE_EXPORT_DECL,
          {'name': name, 'uriString': duplicateExports.head.uri});
    }

    Element existingElement = exportScope[name];
    if (existingElement != null && existingElement != element) {
      if (existingElement.isErroneous()) {
        reportDuplicateExport(element, exports);
        reportDuplicateExportDecl(element, exports);
        element = existingElement;
      } else if (existingElement.getLibrary() == library) {
        // Do nothing. [existingElement] hides [element].
      } else if (element.getLibrary() == library) {
        // [element] hides [existingElement].
        exportScope[name] = element;
        exporters[element] = exports;
      } else {
        // Declared elements hide exported elements.
        Link<Export> existingExports = exporters[existingElement];
        reportDuplicateExport(existingElement, existingExports);
        reportDuplicateExport(element, exports, reportError: false);
        reportDuplicateExportDecl(existingElement, existingExports);
        reportDuplicateExportDecl(element, exports);
        element = exportScope[name] = new ErroneousElementX(
            MessageKind.DUPLICATE_EXPORT, {'name': name}, name, library);
      }
    } else {
      exportScope[name] = element;
      exporters[element] = exports;
    }
    return element;
  }

  /**
   * Propagates the exported [element] to all library nodes that depend upon
   * this node. If the propagation updated any pending exports, [:true:] is
   * returned.
   */
  bool propagateElement(Element element) {
    bool change = false;
    for (ExportLink link in dependencies) {
      if (link.exportElement(element)) {
        change = true;
      }
    }
    return change;
  }

  /**
   * Adds [element] to the pending exports of this node and returns [:true:] if
   * the pending export set was modified. The combinators of [export] are used
   * to filter the element.
   */
  bool addElementToPendingExports(Element element, Export export) {
    bool changed = false;
    if (!identical(exportScope[element.name], element)) {
      Link<Export> exports = pendingExportMap.putIfAbsent(element, () {
        changed = true;
        return const Link<Export>();
      });
      pendingExportMap[element] = exports.prepend(export);
    }
    return changed;
  }
}

/**
 * Helper class used for computing the possibly cyclic import/export scopes of
 * a set of libraries.
 *
 * This class is used by [ScannerTask.scanLibrary] to collect all newly loaded
 * libraries and to compute their import/export scopes through a fixed-point
 * algorithm.
 */
class LibraryDependencyHandler {
  final Compiler compiler;

  /**
   * Newly loaded libraries and their corresponding node in the library
   * dependency graph. Libraries that have already been fully loaded are not
   * part of the dependency graph of this handler since their export scopes have
   * already been computed.
   */
  Map<LibraryElement,LibraryDependencyNode> nodeMap =
      new Map<LibraryElement,LibraryDependencyNode>();

  LibraryDependencyHandler(Compiler this.compiler);

  /**
   * Performs a fixed-point computation on the export scopes of all registered
   * libraries and creates the import/export of the libraries based on the
   * fixed-point.
   */
  void computeExports() {
    bool changed = true;
    while (changed) {
      changed = false;
      Map<LibraryDependencyNode, Map<Element, Link<Export>>> tasks =
          new Map<LibraryDependencyNode, Map<Element, Link<Export>>>();

      // Locally defined elements take precedence over exported
      // elements.  So we must propagate local elements first.  We
      // ensure this by pulling the pending exports before
      // propagating.  This enforces that we handle exports
      // breadth-first, with locally defined elements being level 0.
      nodeMap.forEach((_, LibraryDependencyNode node) {
        Map<Element, Link<Export>> pendingExports = node.pullPendingExports();
        tasks[node] = pendingExports;
      });
      tasks.forEach((LibraryDependencyNode node,
                     Map<Element, Link<Export>> pendingExports) {
        pendingExports.forEach((Element element, Link<Export> exports) {
          element = node.addElementToExportScope(compiler, element, exports);
          if (node.propagateElement(element)) {
            changed = true;
          }
        });
      });
    }

    // Setup export scopes. These have to be set before computing the import
    // scopes to avoid accessing uncomputed export scopes during handling of
    // imports.
    nodeMap.forEach((LibraryElement library, LibraryDependencyNode node) {
      node.registerExports();
    });

    // Setup import scopes.
    nodeMap.forEach((LibraryElement library, LibraryDependencyNode node) {
      node.registerImports(compiler);
    });
  }

  /**
   * Registers that [library] depends on [loadedLibrary] through [tag].
   */
  void registerDependency(LibraryElement library,
                          LibraryDependency tag,
                          LibraryElement loadedLibrary) {
    if (tag != null) {
      library.recordResolvedTag(tag, loadedLibrary);
    }
    if (tag is Export) {
      // [loadedLibrary] is exported by [library].
      LibraryDependencyNode exportingNode = nodeMap[library];
      if (loadedLibrary.exportsHandled) {
        // Export scope already computed on [loadedLibrary].
        var combinatorFilter = new CombinatorFilter.fromTag(tag);
        exportingNode.registerHandledExports(
            loadedLibrary, tag, combinatorFilter);
        return;
      }
      LibraryDependencyNode exportedNode = nodeMap[loadedLibrary];
      assert(invariant(loadedLibrary, exportedNode != null,
          message: "$loadedLibrary has not been registered"));
      assert(invariant(library, exportingNode != null,
          message: "$library has not been registered"));
      exportedNode.registerExportDependency(tag, exportingNode);
    } else if (tag == null || tag is Import) {
      // [loadedLibrary] is imported by [library].
      LibraryDependencyNode importingNode = nodeMap[library];
      assert(invariant(library, importingNode != null,
          message: "$library has not been registered"));
      importingNode.registerImportDependency(tag, loadedLibrary);
    }
  }

  /**
   * Registers [library] for the processing of its import/export scope.
   */
  void registerNewLibrary(LibraryElement library) {
    nodeMap[library] = new LibraryDependencyNode(library);
  }

  /**
   * Registers all top-level entities of [library] as starting point for the
   * fixed-point computation of the import/export scopes.
   */
  void registerLibraryExports(LibraryElement library) {
    nodeMap[library].registerInitialExports();
  }
}
