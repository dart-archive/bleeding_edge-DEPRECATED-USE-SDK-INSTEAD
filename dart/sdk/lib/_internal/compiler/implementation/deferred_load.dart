// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library deferred_load;

import 'dart2jslib.dart' show
    Compiler,
    CompilerTask,
    Constant,
    ConstructedConstant,
    MessageKind,
    StringConstant,
    invariant;

import 'dart_backend/dart_backend.dart' show
    DartBackend;

import 'elements/elements.dart' show
    Element,
    ClassElement,
    ElementKind,
    Elements,
    FunctionElement,
    LibraryElement,
    MetadataAnnotation,
    ScopeContainerElement,
    PrefixElement,
    ClosureContainer;

import 'util/util.dart' show
    Link;

import 'util/setlet.dart' show
    Setlet;

import 'tree/tree.dart' show
    LibraryTag,
    Node,
    NewExpression,
    Import,
    LiteralString,
    LiteralDartString;

import 'resolution/resolution.dart' show
    TreeElements;

import 'mirrors_used.dart' show
    MirrorUsageAnalyzer,
    MirrorUsageAnalyzerTask,
    MirrorUsage;

/// A "hunk" of the program that will be loaded whenever one of its [imports]
/// are loaded.
///
/// Elements that are only used in one deferred import, is in an OutputUnit with
/// the deferred import as single element in the [imports] set.
///
/// Whenever a deferred Element is shared between several deferred imports it is
/// in an output unit with those imports in the [imports] Set.
///
/// OutputUnits are equal if their [imports] are equal.
class OutputUnit {
  /// The deferred imports that will load this output unit when one of them is
  /// loaded.
  final Setlet<Import> imports = new Setlet<Import>();

  /// A unique name representing this [OutputUnit].
  /// Based on the set of [imports].
  String name;

  /// Returns a name composed of the main output file name and [name].
  String partFileName(Compiler compiler) {
    String outPath = compiler.outputUri != null
        ? compiler.outputUri.path
        : "out";
    String outName = outPath.substring(outPath.lastIndexOf('/') + 1);
    return "${outName}_$name";
  }

  String toString() => "OutputUnit($name)";

  bool operator==(OutputUnit other) {
    return imports.length == other.imports.length &&
        imports.containsAll(other.imports);
  }

  int get hashCode {
    int sum = 0;
    for (Import import in imports) {
      sum = (sum + import.hashCode) & 0x3FFFFFFF;  // Stay in 30 bit range.
    }
    return sum;
  }
}

/// For each deferred import, find elements and constants to be loaded when that
/// import is loaded. Elements that are used by several deferred imports are in
/// shared OutputUnits.
class DeferredLoadTask extends CompilerTask {
  /// The name of this task.
  String get name => 'Deferred Loading';

  /// DeferredLibrary from dart:async
  ClassElement get deferredLibraryClass => compiler.deferredLibraryClass;

  /// A synthetic [Import] representing the loading of the main
  /// program.
  final Import _fakeMainImport = new Import(null, new LiteralString(null,
      new LiteralDartString("main")), null, null, null);

  /// The OutputUnit that will be loaded when the program starts.
  final OutputUnit mainOutputUnit = new OutputUnit();

  /// A set containing (eventually) all output units that will result from the
  /// program.
  final Set<OutputUnit> allOutputUnits = new Set<OutputUnit>();

  /// Will be `true` if the program contains deferred libraries.
  bool splitProgram = false;

  /// A mapping from the name of a [DeferredLibrary] annotation to all dependent
  /// output units.
  final Map<String, Set<OutputUnit>> hunksToLoad =
      new Map<String, Set<OutputUnit>>();
  final Map<Import, String> importDeferName = new Map<Import, String>();

  /// A mapping from elements and constants to their output unit. Query this via
  /// [outputUnitForElement]
  final Map<Element, OutputUnit> _elementToOutputUnit =
      new Map<Element, OutputUnit>();

  /// A mapping from constants to their output unit. Query this via
  /// [outputUnitForConstant]
  final Map<Constant, OutputUnit> _constantToOutputUnit =
      new Map<Constant, OutputUnit>();

  /// All the imports with a [DeferredLibrary] annotation, mapped to the
  /// [LibraryElement] they import.
  /// The main library is included in this set for convenience.
  final Map<Import, LibraryElement> _allDeferredImports =
      new Map<Import, LibraryElement>();

  // For each deferred import we want to know exactly what elements have to
  // be loaded.
  Map<Import, Set<Element>> _importedDeferredBy = null;
  Map<Import, Set<Constant>> _constantsDeferredBy = null;

  Set<Element> _mainElements = new Set<Element>();

  DeferredLoadTask(Compiler compiler) : super(compiler);

  /// Returns the [OutputUnit] where [element] belongs.
  OutputUnit outputUnitForElement(Element element) {
    if (!splitProgram) return mainOutputUnit;

    element = element.implementation;
    while (!_elementToOutputUnit.containsKey(element)) {
      element = element.enclosingElement.implementation;
    }
    return _elementToOutputUnit[element];
  }

  /// Returns the [OutputUnit] where [constant] belongs.
  OutputUnit outputUnitForConstant(Constant constant) {
    if (!splitProgram) return mainOutputUnit;

    return _constantToOutputUnit[constant];
  }

  bool isDeferred(Element element) {
    return outputUnitForElement(element) != mainOutputUnit;
  }

  /// Mark that [import] is part of the [OutputputUnit] for [element].
  ///
  /// [element] can be either a [Constant] or an [Element].
  void _addImportToOutputUnitOfElement(Element element, Import import) {
    // Only one file should be loaded when the program starts, so make
    // sure that only one OutputUnit is created for [fakeMainImport].
    if (import == _fakeMainImport) {
      _elementToOutputUnit[element] = mainOutputUnit;
    }
    _elementToOutputUnit.putIfAbsent(element, () => new OutputUnit())
        .imports.add(import);
  }

  /// Mark that [import] is part of the [OutputputUnit] for [constant].
  ///
  /// [constant] can be either a [Constant] or an [Element].
  void _addImportToOutputUnitOfConstant(Constant constant, Import import) {
    // Only one file should be loaded when the program starts, so make
    // sure that only one OutputUnit is created for [fakeMainImport].
    if (import == _fakeMainImport) {
      _constantToOutputUnit[constant] = mainOutputUnit;
    }
    _constantToOutputUnit.putIfAbsent(constant, () => new OutputUnit())
        .imports.add(import);
  }

  /// Answers whether the [import] has a [DeferredLibrary] annotation.
  bool _isImportDeferred(Import import) {
    return _allDeferredImports.containsKey(import);
  }

  /// Checks whether the [import] has a [DeferredLibrary] annotation and stores
  /// the information in [_allDeferredImports] and on the corresponding
  /// prefixElement.
  void _markIfDeferred(Import import, LibraryElement library) {
    // Check if the import is deferred by a keyword.
    if (import.isDeferred) {
      _allDeferredImports[import] = library.getLibraryFromTag(import);
      return;
    }
    // Check if the import is deferred by a metadata annotation.
    Link<MetadataAnnotation> metadataList = import.metadata;
    if (metadataList == null) return;
    for (MetadataAnnotation metadata in metadataList) {
      metadata.ensureResolved(compiler);
      Element element = metadata.value.computeType(compiler).element;
      if (element == deferredLibraryClass) {
        _allDeferredImports[import] = library.getLibraryFromTag(import);
        // On encountering a deferred library without a prefix we report an
        // error, but continue the compilation to possibly give more
        // information. Therefore it is neccessary to check if there is a prefix
        // here.
        Element maybePrefix = library.find(import.prefix.toString());
        if (maybePrefix != null && maybePrefix.isPrefix()) {
          PrefixElement prefix = maybePrefix;
          prefix.markAsDeferred(import);
        }
      }
    }
  }

  /// Answers whether [element] is explicitly deferred when referred to from
  /// [library].
  bool _isExplicitlyDeferred(Element element, LibraryElement library) {
    Link<Import> imports = _getImports(element, library);
    // If the element is not imported explicitly, it is implicitly imported
    // not deferred.
    if (imports.isEmpty) return false;
    // An element could potentially be loaded by several imports. If all of them
    // is explicitly deferred, we say the element is explicitly deferred.
    // TODO(sigurdm): We might want to give a warning if the imports do not
    // agree.
    return imports.every(_isImportDeferred);
  }

  /// Returns a [Link] of every [Import] that imports [element] into [library].
  Link<Import> _getImports(Element element, LibraryElement library) {
    if (!element.isTopLevel()) {
      element = element.getEnclosingClass();
    }

    return library.getImportsFor(element);
  }

  /// Replaces the imports of [outputUnit] with those in
  /// [replacementImports]. Because mainOutputUnit has a special handling we
  /// create a new outputUnit instead, and update the mapping from the
  /// dependency to its outputUnit.
  void _replaceOutputUnitImports(dynamic dependency,
                                 OutputUnit outputUnit,
                                 Iterable<Import> replacementImports) {
    Map<dynamic, OutputUnit> dependencyToOutputUnit = dependency is Element
        ? _elementToOutputUnit
        : _constantToOutputUnit;
    assert(outputUnit == dependencyToOutputUnit[dependency]);
    if (outputUnit == mainOutputUnit) {
      outputUnit = new OutputUnit();
      dependencyToOutputUnit[dependency] = outputUnit;
    } else {
      outputUnit.imports.clear();
    }
    outputUnit.imports.addAll(replacementImports);
  }

  /// Collects all direct dependencies of [element].
  ///
  /// The collected dependent elements and constants are are added to
  /// [elementDependencies] and [constantDependencies] respectively.
  void _collectDependencies(Element element,
                            Set<Element> elementDependencies,
                            Set<Constant> constantDependencies) {
    TreeElements elements =
        compiler.enqueuer.resolution.getCachedElements(element);
    if (elements == null) return;
    for (Element dependency in elements.allElements) {
      if (Elements.isLocal(dependency) && !dependency.isFunction()) continue;
      if (Elements.isUnresolved(dependency)) continue;
      if (dependency.isStatement()) continue;
      elementDependencies.add(dependency);
    }
    constantDependencies.addAll(elements.allConstants);
    elementDependencies.addAll(elements.otherDependencies);
  }

  /// Finds all elements and constants that [element] depends directly on.
  /// (not the transitive closure.)
  ///
  /// Adds the results to [elements] and [constants].
  void _collectAllElementsAndConstantsResolvedFrom(Element element,
      Set<Element> elements,
      Set<Constant> constants) {
    element = element.implementation;
    for (MetadataAnnotation metadata in element.metadata) {
      if (metadata.value != null) {
        constants.add(metadata.value);
        elements.add(metadata.value.computeType(compiler).element);
      }
    }
    if (element.isClass()) {
      // If we see a class, add everything its instance members refer
      // to.  Static members are not relevant.
      ClassElement cls = element.declaration;
      cls.forEachLocalMember((Element e) {
        if (!e.isInstanceMember()) return;
        _collectDependencies(e.implementation, elements, constants);
      });
      if (cls.implementation != cls) {
        // TODO(ahe): Why doesn't ClassElement.forEachLocalMember do this?
        cls.implementation.forEachLocalMember((Element e) {
          if (!e.isInstanceMember()) return;
          _collectDependencies(e.implementation, elements, constants);
        });
      }
      for (var type in cls.implementation.allSupertypes) {
        elements.add(type.element.implementation);
      }
      elements.add(cls.implementation);
    } else if (Elements.isStaticOrTopLevel(element) ||
               element.isConstructor()) {
      _collectDependencies(element, elements, constants);
    }
    if (element.isGenerativeConstructor()) {
      // When instantiating a class, we record a reference to the
      // constructor, not the class itself.  We must add all the
      // instance members of the constructor's class.
      ClassElement implementation =
          element.getEnclosingClass().implementation;
      _collectAllElementsAndConstantsResolvedFrom(
          implementation, elements, constants);
    }

    // Other elements, in particular instance members, are ignored as
    // they are processed as part of the class.
  }

  /// Returns the transitive closure of all libraries that are imported
  /// from root without DeferredLibrary annotations.
  Set<LibraryElement> _nonDeferredReachableLibraries(LibraryElement root) {
    Set<LibraryElement> result = new Set<LibraryElement>();

    void traverseLibrary(LibraryElement library) {
      if (result.contains(library)) return;
      result.add(library);
      // TODO(sigurdm): Make helper getLibraryImportTags when tags is changed to
      // be a List instead of a Link.
      for (LibraryTag tag in library.tags) {
        if (tag is! Import) continue;
        Import import = tag;
        if (!_isImportDeferred(import)) {
          LibraryElement importedLibrary = library.getLibraryFromTag(tag);
          traverseLibrary(importedLibrary);
        }
      }
    }
    traverseLibrary(root);
    return result;
  }

  /// Recursively traverses the graph of dependencies from [element], mapping
  /// deferred imports to each dependency it needs in the sets
  /// [_importedDeferredBy] and [_constantsDeferredBy].
  void _mapDependencies(Element element, Import import) {
    Set<Element> elements = _importedDeferredBy.putIfAbsent(import,
        () => new Set<Element>());
    Set<Constant> constants = _constantsDeferredBy.putIfAbsent(import,
        () => new Set<Constant>());

    if (elements.contains(element)) return;
    // Anything used directly by main will be loaded from the start
    // We do not need to traverse it again.
    if (import != _fakeMainImport && _mainElements.contains(element)) return;

    // Here we modify [_importedDeferredBy].
    elements.add(element);

    Set<Element> dependentElements = new Set<Element>();

    // This call can modify [_importedDeferredBy] and [_constantsDeferredBy].
    _collectAllElementsAndConstantsResolvedFrom(
        element, dependentElements, constants);

    LibraryElement library = element.getLibrary();
    for (Element dependency in dependentElements) {
      if (_isExplicitlyDeferred(dependency, library)) {
        for (Import deferredImport in _getImports(dependency, library)) {
          _mapDependencies(dependency, deferredImport);
        };
      } else {
        _mapDependencies(dependency, import);
      }
    }
  }

  /// Adds extra dependencies coming from mirror usage.
  ///
  /// The elements are added with [_mapDependencies].
  void _addMirrorElements() {
    MirrorUsageAnalyzerTask mirrorTask = compiler.mirrorUsageAnalyzerTask;
    // For each import we record all mirrors-used elements from all the
    // libraries reached directly from that import.
    for (Import deferredImport in _allDeferredImports.keys) {
      LibraryElement deferredLibrary = _allDeferredImports[deferredImport];
      for (LibraryElement library in
          _nonDeferredReachableLibraries(deferredLibrary)) {
        // TODO(sigurdm): The metadata should go to the right output unit.
        // For now they all go to the main output unit.
        for (MetadataAnnotation metadata in library.metadata) {
          if (metadata.value != null) {
            _mapDependencies(metadata.value.computeType(compiler).element,
                _fakeMainImport);
          }
        }
        for (LibraryTag tag in library.tags) {
          for (MetadataAnnotation metadata in tag.metadata) {
            if (metadata.value != null) {
              _mapDependencies(metadata.value.computeType(compiler).element,
                  _fakeMainImport);
            }
          }
        }

        if (mirrorTask.librariesWithUsage.contains(library)) {

          Map<LibraryElement, List<MirrorUsage>> mirrorsResult =
              mirrorTask.analyzer.collectMirrorsUsedAnnotation();

          // If there is a MirrorsUsed annotation we add only the needed
          // things to the output units for the library.
          List<MirrorUsage> mirrorUsages = mirrorsResult[library];
          if (mirrorUsages == null) continue;
          for (MirrorUsage usage in mirrorUsages) {
            if (usage.targets != null) {
              for (Element dependency in usage.targets) {
                _mapDependencies(dependency, deferredImport);
              }
            }
            if (usage.metaTargets != null) {
              for (Element dependency in usage.metaTargets) {
                _mapDependencies(dependency, deferredImport);
              }
            }
          }
        } else {
          // If there is no MirrorsUsed annotation we add _everything_ to
          // the output units for the library.

          // TODO(sigurdm): This is too expensive.
          // Plan: If mirrors are used without MirrorsUsed, create an
          // "EverythingElse" library that contains all elements that are
          // not referred by main or deferred libraries that don't contain
          // mirrors (without MirrorsUsed).
          //
          // So basically we want:
          //   mainImport
          //   deferredA
          //   deferredB
          //   deferredCwithMirrorsUsed
          //   deferredEverythingElse
          //
          // Where deferredEverythingElse will be loaded for *all* libraries
          // that contain a mirror usage without MirrorsUsed.
          //   When loading the deferredEverythingElse also load all other
          //   deferred libraries at the same time.
          bool usesMirrors = false;
          for (LibraryTag tag in library.tags) {
            if (tag is! Import) continue;
            if (library.getLibraryFromTag(tag) == compiler.mirrorsLibrary) {
              usesMirrors = true;
              break;
            }
          }
          if (usesMirrors) {
            for (Link link in compiler.enqueuer.allElementsByName.values) {
              for (Element dependency in link) {
                _mapDependencies(dependency, deferredImport);
              }
            }
          }
        }
      }
    }
  }

  /// Goes through [allConstants] and adjusts their outputUnits.
  void _adjustConstantsOutputUnit(Set<Constant> allConstants) {
    // A constant has three dependencies:
    // 1- the libraries it is used in.
    // 2- its class.
    // 3- its arguments.
    // The constant should only be loaded if all three dependencies are
    // loaded.
    // TODO(floitsch): only load constants when all three dependencies are
    // satisfied.
    //
    // So far we only looked at where the constants were used. For now, we
    // use a simplified approach to fix this (partially): if the current
    // library is not deferred, only look at the class (2). Otherwise store
    // the constant in the current (deferred) library.
    for (Constant constant in allConstants) {
      // If the constant is not a "constructed" constant, it can stay where
      // it is.
      if (!constant.isConstructedObject) continue;
      OutputUnit constantUnit = _constantToOutputUnit[constant];
      Setlet<Import> constantImports = constantUnit.imports;
      ConstructedConstant constructed = constant;
      Element classElement = constructed.type.element;
      OutputUnit classUnit = _elementToOutputUnit[classElement];
      // This happens with classes that are only used as annotations.
      // TODO(sigurdm): Find out if we can use a specific check for this.
      if (classUnit == null) continue;
      Setlet<Import> classImports = classUnit.imports;
      // The class exists in the main-unit. Just leave the constant where it
      // is. We know that the constructor will be available.
      if (classImports.length == 1 && classImports.single == _fakeMainImport) {
        continue;
      }
      // The class is loaded for all imports in the classImport-set.
      // If the constant's imports are included in the class' set, we can
      // keep the constant unit as is.
      // If the constant is used otherwise, we need to make sure that the
      // class is available before constructing the constant.
      if (classImports.containsAll(constantImports)) continue;
      // We could now just copy the OutputUnit from the class to the output
      // unit of the constant, but we prefer separate instances.
      // Replace the imports of the constant to match the ones of the class.
      _replaceOutputUnitImports(constant, constantUnit, classImports);
    }
  }

  /// Computes a unique string for the name field for each outputUnit.
  ///
  /// Also sets up the [hunksToLoad] mapping.
  void _assignNamesToOutputUnits(Set<OutputUnit> allOutputUnits) {
    Set<String> usedImportNames = new Set<String>();

    // Returns suggestedName if it is not in usedNames. Otherwise concatenates
    // the smallest number that makes it not appear in usedNames.
    // Adds the result to usedNames.
    String makeUnique(String suggestedName, Set<String> usedNames) {
      String result = suggestedName;
      if (usedNames.contains(suggestedName)) {
        int counter = 0;
        while (usedNames.contains(result)) {
          counter++;
          result = "$suggestedName$counter";
        }
      }
      usedNames.add(result);
      return result;
    }

    // Finds the first argument to the [DeferredLibrary] annotation
    void computeImportDeferName(Import import) {
      String result;
      if (import == _fakeMainImport) {
        result = "main";
      } else if (import.isDeferred) {
        result = import.prefix.toString();
      } else {
        Link<MetadataAnnotation> metadatas = import.metadata;
        assert(metadatas != null);
        for (MetadataAnnotation metadata in metadatas) {
          metadata.ensureResolved(compiler);
          Element element = metadata.value.computeType(compiler).element;
          if (metadata.value.computeType(compiler).element ==
              deferredLibraryClass) {
            ConstructedConstant constant = metadata.value;
            StringConstant s = constant.fields[0];
            result = s.value.slowToString();
            break;
          }
        }
      }
      assert(result != null);
      importDeferName[import] = makeUnique(result, usedImportNames);;
    }

    Set<String> usedOutputUnitNames = new Set<String>();
    Map<OutputUnit, String> generatedNames = new Map<OutputUnit, String>();

    void computeOutputUnitName(OutputUnit outputUnit) {
      if (generatedNames[outputUnit] != null) return;
      String suggestedName = outputUnit.imports.map((import) {
        return importDeferName[import];
      }).join('_');
      outputUnit.name = makeUnique(suggestedName, usedOutputUnitNames);
      generatedNames[outputUnit] = outputUnit.name;
    }

    for (Import import in _allDeferredImports.keys) {
      computeImportDeferName(import);
    }

    for (OutputUnit outputUnit in allOutputUnits) {
      computeOutputUnitName(outputUnit);
    }

    // For each deferred import we find out which outputUnits to load.
    for (Import import in _allDeferredImports.keys) {
      if (import == _fakeMainImport) continue;
      hunksToLoad[importDeferName[import]] = new Set<OutputUnit>();
      for (OutputUnit outputUnit in allOutputUnits) {
        if (outputUnit == mainOutputUnit) continue;
        if (outputUnit.imports.contains(import)) {
          hunksToLoad[importDeferName[import]].add(outputUnit);
        }
      }
    }
  }

  void onResolutionComplete(FunctionElement main) {
    if (!splitProgram) {
      allOutputUnits.add(mainOutputUnit);
      return;
    }
    if (main == null) return;
    LibraryElement mainLibrary = main.getLibrary();
    _importedDeferredBy = new Map<Import, Set<Element>>();
    _constantsDeferredBy = new Map<Import, Set<Constant>>();
    _importedDeferredBy[_fakeMainImport] = _mainElements;

    measureElement(mainLibrary, () {

      // Starting from main, traverse the program and find all dependencies.
      _mapDependencies(compiler.mainFunction, _fakeMainImport);

      // Also add "global" dependencies to the main OutputUnit.  These are
      // things that the backend need but cannot associate with a particular
      // element, for example, startRootIsolate.  This set also contains
      // elements for which we lack precise information.
      for (Element element in compiler.globalDependencies.otherDependencies) {
        _mapDependencies(element, _fakeMainImport);
      }

      // Now check to see if we have to add more elements due to mirrors.
      if (compiler.mirrorsLibrary != null) {
        _addMirrorElements();
      }

      Set<Constant> allConstants = new Set<Constant>();
      // Reverse the mapping. For each element record an OutputUnit collecting
      // all deferred imports using this element. Same for constants.
      for (Import import in _importedDeferredBy.keys) {
        for (Element element in _importedDeferredBy[import]) {
          _addImportToOutputUnitOfElement(element, import);
        }
        for (Constant constant in _constantsDeferredBy[import]) {
          allConstants.add(constant);
          _addImportToOutputUnitOfConstant(constant, import);
        }
      }

      // Release maps;
      _importedDeferredBy = null;
      _constantsDeferredBy = null;

      _adjustConstantsOutputUnit(allConstants);

      // Find all the output units we have used.
      // Also generate a unique name for each OutputUnit.
      for (OutputUnit outputUnit in _elementToOutputUnit.values) {
        allOutputUnits.add(outputUnit);
      }
      for (OutputUnit outputUnit in _constantToOutputUnit.values) {
        allOutputUnits.add(outputUnit);
      }

      _assignNamesToOutputUnits(allOutputUnits);
    });
  }

  void ensureMetadataResolved(Compiler compiler) {
    _allDeferredImports[_fakeMainImport] = compiler.mainApp;
    var lastDeferred;
    // When detecting duplicate prefixes of deferred libraries there are 4
    // cases of duplicate prefixes:
    // 1.
    // import "lib.dart" deferred as a;
    // import "lib2.dart" deferred as a;
    // 2.
    // import "lib.dart" deferred as a;
    // import "lib2.dart" as a;
    // 3.
    // import "lib.dart" as a;
    // import "lib2.dart" deferred as a;
    // 4.
    // import "lib.dart" as a;
    // import "lib2.dart" as a;
    // We must be able to signal error for case 1, 2, 3, but accept case 4.

    // The prefixes that have been used by any imports in this library.
    Setlet<String> usedPrefixes = new Setlet<String>();
    // The last deferred import we saw with a given prefix (if any).
    Map<String, Import> prefixDeferredImport = new Map<String, Import>();
    for (LibraryElement library in compiler.libraries.values) {
      compiler.withCurrentElement(library, () {
        prefixDeferredImport.clear();
        usedPrefixes.clear();
        // TODO(sigurdm): Make helper getLibraryImportTags when tags is a List
        // instead of a Link.
        for (LibraryTag tag in library.tags) {
          if (tag is! Import) continue;
          Import import = tag;
          _markIfDeferred(import, library);
          String prefix = (import.prefix != null)
              ? import.prefix.toString()
              : null;
          // The last import we saw with the same prefix.
          Import previousDeferredImport = prefixDeferredImport[prefix];
          bool isDeferred = _isImportDeferred(import);
          if (isDeferred) {
            if (prefix == null) {
              compiler.reportError(import,
                  MessageKind.DEFERRED_LIBRARY_WITHOUT_PREFIX);
            } else {
              prefixDeferredImport[prefix] = import;
            }
            splitProgram = true;
            lastDeferred = import;
          }
          if (prefix != null) {
            if (previousDeferredImport != null ||
                (isDeferred && usedPrefixes.contains(prefix))) {
              Import failingImport = (previousDeferredImport != null)
                  ? previousDeferredImport
                  : import;
              compiler.reportError(failingImport.prefix,
                  MessageKind.DEFERRED_LIBRARY_DUPLICATE_PREFIX);
            }
            usedPrefixes.add(prefix);
          }
        }
      });
    }
    if (splitProgram && compiler.backend is DartBackend) {
      // TODO(sigurdm): Implement deferred loading for dart2dart.
      splitProgram = false;
      compiler.reportInfo(
          lastDeferred,
          MessageKind.DEFERRED_LIBRARY_DART_2_DART);
    }
  }
}
