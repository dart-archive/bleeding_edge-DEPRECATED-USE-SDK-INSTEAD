// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library dart2js.js_emitter.program_builder;

import 'js_emitter.dart' show computeMixinClass;
import 'model.dart';

import '../common.dart';
import '../js/js.dart' as js;

import '../js_backend/js_backend.dart' show
    Namer,
    JavaScriptBackend,
    JavaScriptConstantCompiler;

import '../closure.dart' show ClosureFieldElement;

import 'js_emitter.dart' as emitterTask show
    CodeEmitterTask,
    Emitter,
    InterceptorStubGenerator,
    TypeTestGenerator,
    TypeTestProperties;

import '../universe/universe.dart' show Universe;
import '../deferred_load.dart' show DeferredLoadTask, OutputUnit;

part 'registry.dart';

class ProgramBuilder {
  final Compiler _compiler;
  final Namer namer;
  final emitterTask.CodeEmitterTask _task;

  final Registry _registry;

  ProgramBuilder(Compiler compiler,
                 this.namer,
                 this._task)
      : this._compiler = compiler,
        this._registry = new Registry(compiler);

  JavaScriptBackend get backend => _compiler.backend;
  Universe get universe => _compiler.codegenWorld;

  /// Mapping from [ClassElement] to constructed [Class]. We need this to
  /// update the superclass in the [Class].
  final Map<ClassElement, Class> _classes = <ClassElement, Class>{};

  /// Mapping from [OutputUnit] to constructed [Fragment]. We need this to
  /// generate the deferredLoadingMap (to know which hunks to load).
  final Map<OutputUnit, Fragment> _outputs = <OutputUnit, Fragment>{};

  /// Mapping from [ConstantValue] to constructed [Constant]. We need this to
  /// update field-initializers to point to the ConstantModel.
  final Map<ConstantValue, Constant> _constants = <ConstantValue, Constant>{};

  Program buildProgram() {
    // Note: In rare cases (mostly tests) output units can be empty. This
    // happens when the deferred code is dead-code eliminated but we still need
    // to check that the library has been loaded.
    _compiler.deferredLoadTask.allOutputUnits.forEach(
        _registry.registerOutputUnit);
    _task.outputClassLists.forEach(_registry.registerElements);
    _task.outputStaticLists.forEach(_registry.registerElements);
    _task.outputConstantLists.forEach(_registerConstants);
    _task.outputStaticNonFinalFieldLists.forEach(_registry.registerElements);

    // TODO(kasperl): There's code that implicitly needs access to the special
    // $ holder so we have to register that. Can we track if we have to?
    _registry.registerHolder(r'$');

    MainFragment mainOutput = _buildMainOutput(_registry.mainLibrariesMap);
    Iterable<Fragment> deferredOutputs = _registry.deferredLibrariesMap
        .map((librariesMap) => _buildDeferredOutput(mainOutput, librariesMap));

    List<Fragment> outputs = new List<Fragment>(_registry.librariesMapCount);
    outputs[0] = mainOutput;
    outputs.setAll(1, deferredOutputs);

    Program result =
        new Program(outputs, _task.outputContainsConstantList, _buildLoadMap());

    // Resolve the superclass references after we've processed all the classes.
    _classes.forEach((ClassElement element, Class c) {
      if (element.superclass != null) {
        c.setSuperclass(_classes[element.superclass]);
      }
      if (element.isMixinApplication) {
        MixinApplication mixinApplication = c;
        mixinApplication.setMixinClass(_classes[computeMixinClass(element)]);
      }
    });

    _markEagerClasses();

    return result;
  }

  void _markEagerClasses() {
    _markEagerInterceptorClasses();
  }

  /// Builds a map from loadId to outputs-to-load.
  Map<String, List<Fragment>> _buildLoadMap() {
    List<OutputUnit> convertHunks(List<OutputUnit> hunks) {
      return hunks.map((OutputUnit unit) => _outputs[unit])
          .toList(growable: false);
    }

    Map<String, List<Fragment>> loadMap = <String, List<Fragment>>{};
    _compiler.deferredLoadTask.hunksToLoad
        .forEach((String loadId, List<OutputUnit> outputUnits) {
      loadMap[loadId] = outputUnits
          .map((OutputUnit unit) => _outputs[unit])
          .toList(growable: false);
    });
    return loadMap;
  }

  MainFragment _buildMainOutput(LibrariesMap librariesMap) {
    // Construct the main output from the libraries and the registered holders.
    MainFragment result = new MainFragment(
        librariesMap.outputUnit,
        "",  // The empty string is the name for the main output file.
        backend.emitter.staticFunctionAccess(_compiler.mainFunction),
        _buildLibraries(librariesMap),
        _buildStaticNonFinalFields(librariesMap),
        _buildStaticLazilyInitializedFields(librariesMap),
        _buildConstants(librariesMap),
        _registry.holders.toList(growable: false));
    _outputs[librariesMap.outputUnit] = result;
    return result;
  }

  DeferredFragment _buildDeferredOutput(MainFragment mainOutput,
                                      LibrariesMap librariesMap) {
    DeferredFragment result = new DeferredFragment(
        librariesMap.outputUnit,
        backend.deferredPartFileName(librariesMap.name, addExtension: false),
                                     librariesMap.name,
        mainOutput,
        _buildLibraries(librariesMap),
        _buildStaticNonFinalFields(librariesMap),
        _buildStaticLazilyInitializedFields(librariesMap),
        _buildConstants(librariesMap));
    _outputs[librariesMap.outputUnit] = result;
    return result;
  }

  List<Constant> _buildConstants(LibrariesMap librariesMap) {
    List<ConstantValue> constantValues =
        _task.outputConstantLists[librariesMap.outputUnit];
    if (constantValues == null) return const <Constant>[];
    return constantValues.map((ConstantValue value) => _constants[value])
        .toList(growable: false);
  }

  List<StaticField> _buildStaticNonFinalFields(LibrariesMap librariesMap) {
    // TODO(floitsch): handle static non-final fields correctly with deferred
    // libraries.
    if (librariesMap != _registry.mainLibrariesMap) {
      return const <StaticField>[];
    }
    Iterable<VariableElement> staticNonFinalFields =
        backend.constants.getStaticNonFinalFieldsForEmission();
    return Elements.sortedByPosition(staticNonFinalFields)
        .map(_buildStaticField)
        .toList(growable: false);
  }

  StaticField _buildStaticField(Element element) {
    JavaScriptConstantCompiler handler = backend.constants;
    ConstantValue initialValue = handler.getInitialValueFor(element).value;
    js.Expression code = _task.emitter.constantReference(initialValue);
    String name = namer.getNameOfGlobalField(element);
    bool isFinal = false;
    bool isLazy = false;
    return new StaticField(element,
                           name, _registry.registerHolder(r'$'), code,
                           isFinal, isLazy);
  }

  List<StaticField> _buildStaticLazilyInitializedFields(
      LibrariesMap librariesMap) {
    // TODO(floitsch): lazy fields should just be in their respective
    // libraries.
    if (librariesMap != _registry.mainLibrariesMap) {
      return const <StaticField>[];
    }

    JavaScriptConstantCompiler handler = backend.constants;
    List<VariableElement> lazyFields =
        handler.getLazilyInitializedFieldsForEmission();
    return Elements.sortedByPosition(lazyFields)
        .map(_buildLazyField)
        .where((field) => field != null)  // Happens when the field was unused.
        .toList(growable: false);
  }

  StaticField _buildLazyField(Element element) {
    JavaScriptConstantCompiler handler = backend.constants;
    js.Expression code = backend.generatedCode[element];
    // The code is null if we ended up not needing the lazily
    // initialized field after all because of constant folding
    // before code generation.
    if (code == null) return null;

    String name = namer.getNameOfGlobalField(element);
    bool isFinal = element.isFinal;
    bool isLazy = true;
    return new StaticField(element,
                           name, _registry.registerHolder(r'$'), code,
                           isFinal, isLazy);
  }

  List<Library> _buildLibraries(LibrariesMap librariesMap) {
    List<Library> libraries = new List<Library>(librariesMap.length);
    int count = 0;
    librariesMap.forEach((LibraryElement library, List<Element> elements) {
      libraries[count++] = _buildLibrary(library, elements);
    });
    return libraries;
  }

  // Note that a library-element may have multiple [Library]s, if it is split
  // into multiple output units.
  Library _buildLibrary(LibraryElement library, List<Element> elements) {
    String uri = library.canonicalUri.toString();

    List<StaticMethod> statics = elements
        .where((e) => e is FunctionElement)
        .map(_buildStaticMethod)
        .toList();

    if (library == backend.interceptorsLibrary) {
      statics.addAll(_generateGetInterceptorMethods());
      statics.addAll(_generateOneShotInterceptors());
    }

    List<Class> classes = elements
        .where((e) => e is ClassElement)
        .map(_buildClass)
        .toList(growable: false);

    return new Library(library, uri, statics, classes);
  }

  Class _buildClass(ClassElement element) {
    List<Method> methods = [];
    List<InstanceField> fields = [];

    void visitMember(ClassElement enclosing, Element member) {
      assert(invariant(element, member.isDeclaration));
      assert(invariant(element, element == enclosing));

      if (Elements.isNonAbstractInstanceMember(member)) {
        js.Expression code = backend.generatedCode[member];
        // TODO(kasperl): Figure out under which conditions code is null.
        if (code != null) methods.add(_buildMethod(member, code));
      } else if (member.isField && !member.isStatic) {
        fields.add(_buildInstanceField(member, enclosing));
      }
    }

    ClassElement implementation = element.implementation;

    // MixinApplications run through the members of their mixin. Here, we are
    // only interested in direct members.
    if (!element.isMixinApplication) {
      implementation.forEachMember(visitMember, includeBackendMembers: true);
    }

    emitterTask.TypeTestGenerator generator =
        new emitterTask.TypeTestGenerator(_compiler, _task, namer);
    emitterTask.TypeTestProperties typeTests =
        generator.generateIsTests(element);

    // At this point a mixin application must not have any methods or fields.
    // Type-tests might be added to mixin applications, too.
    assert(!element.isMixinApplication || methods.isEmpty);
    assert(!element.isMixinApplication || fields.isEmpty);

    // TODO(floitsch): we should not add the code here, but have a list of
    // is/as classes in the Class object.
    // The individual emitters should then call the type test generator to
    // generate the code.
    typeTests.properties.forEach((String name, js.Node code) {
      methods.add(_buildStubMethod(name, code));
    });

    String name = namer.getNameOfClass(element);
    String holderName = namer.globalObjectFor(element);
    Holder holder = _registry.registerHolder(holderName);
    bool onlyForRti = _task.typeTestRegistry.rtiNeededClasses.contains(element);
    bool isInstantiated =
        _compiler.codegenWorld.directlyInstantiatedClasses.contains(element);

    Class result;
    if (element.isMixinApplication) {
      result = new MixinApplication(element,
                                    name, holder, methods, fields,
                                    isDirectlyInstantiated: isInstantiated,
                                    onlyForRti: onlyForRti);
    } else {
      result = new Class(element,
                         name, holder, methods, fields,
                         isDirectlyInstantiated: isInstantiated,
                         onlyForRti: onlyForRti);
    }
    _classes[element] = result;
    return result;
  }

  Method _buildMethod(FunctionElement element, js.Expression code) {
    String name = namer.getNameOfInstanceMember(element);
    // TODO(floitsch): compute `needsTearOff`.
    return new Method(element, name, code, needsTearOff: false);
  }

  Method _buildStubMethod(String name, js.Expression code) {
    // TODO(floitsch): compute `needsTearOff`.
    return new StubMethod(name, code, needsTearOff: false);
  }

  // The getInterceptor methods directly access the prototype of classes.
  // We must evaluate these classes eagerly so that the prototype is
  // accessible.
  void _markEagerInterceptorClasses() {
    Map<String, Set<ClassElement>> specializedGetInterceptors =
        backend.specializedGetInterceptors;
    for (Set<ClassElement> classes in specializedGetInterceptors.values) {
      for (ClassElement element in classes) {
        Class cls = _classes[element];
        if (cls != null) cls.isEager = true;
      }
    }
  }

  Iterable<StaticMethod> _generateGetInterceptorMethods() {
    emitterTask.InterceptorStubGenerator stubGenerator =
        new emitterTask.InterceptorStubGenerator(_compiler, namer, backend);

    String holderName = namer.globalObjectFor(backend.interceptorsLibrary);
    Holder holder = _registry.registerHolder(holderName);

    Map<String, Set<ClassElement>> specializedGetInterceptors =
        backend.specializedGetInterceptors;
    List<String> names = specializedGetInterceptors.keys.toList()..sort();
    return names.map((String name) {
      Set<ClassElement> classes = specializedGetInterceptors[name];
      js.Expression code = stubGenerator.generateGetInterceptorMethod(classes);
      // TODO(floitsch): compute `needsTearOff`.
      return new StaticStubMethod(name, holder, code, needsTearOff: false);
    });
  }

  bool _fieldNeedsGetter(VariableElement field) {
    assert(field.isField);
    if (_fieldAccessNeverThrows(field)) return false;
    return backend.shouldRetainGetter(field)
        || _compiler.codegenWorld.hasInvokedGetter(field, _compiler.world);
  }

  bool _fieldNeedsSetter(VariableElement field) {
    assert(field.isField);
    if (_fieldAccessNeverThrows(field)) return false;
    return (!field.isFinal && !field.isConst)
        && (backend.shouldRetainSetter(field)
            || _compiler.codegenWorld.hasInvokedSetter(field, _compiler.world));
  }

  // We never access a field in a closure (a captured variable) without knowing
  // that it is there.  Therefore we don't need to use a getter (that will throw
  // if the getter method is missing), but can always access the field directly.
  bool _fieldAccessNeverThrows(VariableElement field) {
    return field is ClosureFieldElement;
  }

  InstanceField _buildInstanceField(VariableElement field,
                                    ClassElement holder) {
    assert(invariant(field, field.isDeclaration));
    String name = namer.fieldPropertyName(field);

    int getterFlags = 0;
    if (_fieldNeedsGetter(field)) {
      bool isIntercepted = backend.fieldHasInterceptedGetter(field);
      if (isIntercepted) {
        getterFlags += 2;
        if (backend.isInterceptorClass(holder)) {
          getterFlags += 1;
        }
      } else {
        getterFlags = 1;
      }
    }

    int setterFlags = 0;
    if (_fieldNeedsSetter(field)) {
      bool isIntercepted = backend.fieldHasInterceptedSetter(field);
      if (isIntercepted) {
        setterFlags += 2;
        if (backend.isInterceptorClass(holder)) {
          setterFlags += 1;
        }
      } else {
        setterFlags = 1;
      }
    }

    return new InstanceField(field, name, getterFlags, setterFlags);
  }

  Iterable<StaticMethod> _generateOneShotInterceptors() {
    emitterTask.InterceptorStubGenerator stubGenerator =
        new emitterTask.InterceptorStubGenerator(_compiler, namer, backend);

    String holderName = namer.globalObjectFor(backend.interceptorsLibrary);
    Holder holder = _registry.registerHolder(holderName);

    List<String> names = backend.oneShotInterceptors.keys.toList()..sort();
    return names.map((String name) {
      js.Expression code = stubGenerator.generateOneShotInterceptor(name);
      return new StaticStubMethod(name, holder, code, needsTearOff: false);
    });
  }

  StaticMethod _buildStaticMethod(FunctionElement element) {
    String name = namer.getNameOfMember(element);
    String holder = namer.globalObjectFor(element);
    js.Expression code = backend.generatedCode[element];
    bool needsTearOff =
        universe.staticFunctionsNeedingGetter.contains(element);
    // TODO(floitsch): add tear-off name: namer.getStaticClosureName(element).
    return new StaticMethod(element,
                            name, _registry.registerHolder(holder), code,
                            needsTearOff: needsTearOff);
  }

  void _registerConstants(OutputUnit outputUnit,
                          Iterable<ConstantValue> constantValues) {
    // `constantValues` is null if an outputUnit doesn't contain any constants.
    if (constantValues == null) return;
    for (ConstantValue constantValue in constantValues) {
      _registry.registerConstant(outputUnit, constantValue);
      assert(!_constants.containsKey(constantValue));
      String name = namer.constantName(constantValue);
      String constantObject = namer.globalObjectForConstant(constantValue);
      Holder holder = _registry.registerHolder(constantObject);
      Constant constant = new Constant(name, holder, constantValue);
      _constants[constantValue] = constant;
    }
  }
}
