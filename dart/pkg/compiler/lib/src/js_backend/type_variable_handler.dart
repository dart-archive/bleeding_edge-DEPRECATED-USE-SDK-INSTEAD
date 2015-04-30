// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of js_backend;

/**
 * Handles construction of TypeVariable constants needed at runtime.
 */
class TypeVariableHandler {
  final Compiler _compiler;
  FunctionElement _typeVariableConstructor;
  CompileTimeConstantEvaluator _evaluator;

  /**
   * Set to 'true' on first encounter of a class with type variables.
   */
  bool _seenClassesWithTypeVariables = false;

  /**
   *  Maps a class element to a list with indices that point to type variables
   *  constants for each of the class' type variables.
   */
  Map<ClassElement, List<int>> _typeVariables =
      new Map<ClassElement, List<int>>();

  /**
   *  Maps a TypeVariableType to the index pointing to the constant representing
   *  the corresponding type variable at runtime.
   */
  Map<TypeVariableElement, int> _typeVariableConstants =
      new Map<TypeVariableElement, int>();

  TypeVariableHandler(this._compiler);

  ClassElement get _typeVariableClass => _backend.typeVariableClass;
  CodeEmitterTask get _task => _backend.emitter;
  MetadataCollector get _metadataCollector => _task.metadataCollector;
  JavaScriptBackend get _backend => _compiler.backend;

  void registerClassWithTypeVariables(ClassElement cls, Enqueuer enqueuer,
                                      Registry registry) {
    if (enqueuer.isResolutionQueue) {
      // On first encounter, we have to ensure that the support classes get
      // resolved.
      if (!_seenClassesWithTypeVariables) {
        _backend.enqueueClass(
            enqueuer, _typeVariableClass, registry);
        _typeVariableClass.ensureResolved(_compiler);
        Link constructors = _typeVariableClass.constructors;
        if (constructors.isEmpty && constructors.tail.isEmpty) {
          _compiler.internalError(_typeVariableClass,
              "Class '$_typeVariableClass' should only have one constructor");
        }
        _typeVariableConstructor = _typeVariableClass.constructors.head;
        _backend.enqueueInResolution(_typeVariableConstructor, registry);
        enqueuer.registerInstantiatedType(_typeVariableClass.rawType,
                                          registry);
        enqueuer.registerStaticUse(_backend.getCreateRuntimeType());
        _seenClassesWithTypeVariables = true;
      }
    } else {
      if (_backend.isAccessibleByReflection(cls)) {
        _processTypeVariablesOf(cls);
      }
    }
  }

  void _processTypeVariablesOf(ClassElement cls) {
    InterfaceType typeVariableType = _typeVariableClass.thisType;
    List<int> constants = <int>[];

    for (TypeVariableType currentTypeVariable in cls.typeVariables) {
      TypeVariableElement typeVariableElement = currentTypeVariable.element;

      AstConstant wrapConstant(ConstantExpression constant) {
        return new AstConstant(typeVariableElement,
                               typeVariableElement.node,
                               constant);
      }

      ConstantExpression name = new StringConstantExpression(
          currentTypeVariable.name,
          _backend.constantSystem.createString(
              new DartString.literal(currentTypeVariable.name)));
      int boundIndex = _metadataCollector.reifyType(typeVariableElement.bound);
      ConstantExpression bound = new IntConstantExpression(
          boundIndex,
          _backend.constantSystem.createInt(boundIndex));
      ConstantExpression type = _backend.constants.createTypeConstant(cls);
      List<AstConstant> arguments =
          [wrapConstant(type), wrapConstant(name), wrapConstant(bound)];

      // TODO(johnniwinther): Support a less front-end specific creation of
      // constructed constants.
      AstConstant constant =
          CompileTimeConstantEvaluator.makeConstructedConstant(
              _compiler,
              _backend.constants,
              typeVariableElement,
              typeVariableElement.node,
              typeVariableType,
              _typeVariableConstructor,
              const CallStructure.unnamed(3),
              arguments,
              arguments);
      ConstantValue value = constant.value;
      _backend.registerCompileTimeConstant(value, _compiler.globalDependencies);
      _backend.constants.addCompileTimeConstantForEmission(value);
      constants.add(
          _reifyTypeVariableConstant(value, currentTypeVariable.element));
    }
    _typeVariables[cls] = constants;
  }

  /**
   * Adds [c] to [emitter.metadataCollector] and returns the index pointing to
   * the entry.
   *
   * If the corresponding type variable has already been encountered an
   * entry in the list has already been reserved and the constant is added
   * there, otherwise a new entry for [c] is created.
   */
  int _reifyTypeVariableConstant(ConstantValue c, TypeVariableElement variable) {
    String name = jsAst.prettyPrint(_task.constantReference(c),
                                    _compiler).getText();
    int index;
    if (_typeVariableConstants.containsKey(variable)) {
      index = _typeVariableConstants[variable];
      _metadataCollector.globalMetadata[index] = name;
    } else {
      index = _metadataCollector.addGlobalMetadata(name);
      _typeVariableConstants[variable] = index;
    }
    return index;
  }

  /**
   * Returns the index pointing to the constant in [emitter.metadataCollector]
   * representing this type variable.
   *
   * If the constant has not yet been constructed, an entry is  allocated in
   * the global metadata list and the index pointing to this entry is returned.
   * When the corresponding constant is constructed later,
   * [reifyTypeVariableConstant] will be called and the constant will be added
   * on the allocated entry.
   */
  int reifyTypeVariable(TypeVariableElement variable) {
    if (_typeVariableConstants.containsKey(variable)) {
      return _typeVariableConstants[variable];
    }

    // TODO(15613): Remove quotes.
    _metadataCollector.globalMetadata.add('"Placeholder for ${variable}"');
    return _typeVariableConstants[variable] =
        _metadataCollector.globalMetadata.length - 1;
  }

  List<int> typeVariablesOf(ClassElement classElement) {
    List<int> result = _typeVariables[classElement];
    if (result == null) {
      result = const <int>[];
    }
    return result;
  }
}
