// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of dart2js.mirrors;

//------------------------------------------------------------------------------
// Member mirrors implementation.
//------------------------------------------------------------------------------

abstract class Dart2JsMemberMirror extends Dart2JsElementMirror {

  Dart2JsMemberMirror(Dart2JsMirrorSystem system, Element element)
      : super(system, element);

  bool get isStatic => false;
}


class Dart2JsMethodKind {
  static const Dart2JsMethodKind REGULAR = const Dart2JsMethodKind("regular");
  static const Dart2JsMethodKind GENERATIVE =
      const Dart2JsMethodKind("generative");
  static const Dart2JsMethodKind REDIRECTING =
      const Dart2JsMethodKind("redirecting");
  static const Dart2JsMethodKind CONST = const Dart2JsMethodKind("const");
  static const Dart2JsMethodKind FACTORY = const Dart2JsMethodKind("factory");
  static const Dart2JsMethodKind GETTER = const Dart2JsMethodKind("getter");
  static const Dart2JsMethodKind SETTER = const Dart2JsMethodKind("setter");
  static const Dart2JsMethodKind OPERATOR = const Dart2JsMethodKind("operator");

  final String text;

  const Dart2JsMethodKind(this.text);

  String toString() => text;
}


String _getOperatorFromOperatorName(String name) {
  Map<String, String> mapping = const {
    'eq': '==',
    'not': '~',
    'index': '[]',
    'indexSet': '[]=',
    'mul': '*',
    'div': '/',
    'mod': '%',
    'tdiv': '~/',
    'add': '+',
    'sub': '-',
    'shl': '<<',
    'shr': '>>',
    'ge': '>=',
    'gt': '>',
    'le': '<=',
    'lt': '<',
    'and': '&',
    'xor': '^',
    'or': '|',
  };
  String newName = mapping[name];
  if (newName == null) {
    throw new Exception('Unhandled operator name: $name');
  }
  return newName;
}

class Dart2JsMethodMirror extends Dart2JsMemberMirror
    implements MethodMirror {
  final Dart2JsDeclarationMirror owner;
  final String _simpleNameString;
  final Dart2JsMethodKind _kind;

  Dart2JsMethodMirror._internal(Dart2JsDeclarationMirror owner,
      FunctionElement function,
      String this._simpleNameString,
      Dart2JsMethodKind this._kind)
      : this.owner = owner,
        super(owner.mirrorSystem, function);

  factory Dart2JsMethodMirror(Dart2JsDeclarationMirror owner,
                              FunctionElement function) {
    String realName = function.name;
    // TODO(ahe): This method should not be calling
    // Elements.operatorNameToIdentifier.
    String simpleName =
        Elements.operatorNameToIdentifier(function.name);
    Dart2JsMethodKind kind;
    if (function.kind == ElementKind.GETTER) {
      kind = Dart2JsMethodKind.GETTER;
    } else if (function.kind == ElementKind.SETTER) {
      kind = Dart2JsMethodKind.SETTER;
      simpleName = '$simpleName=';
    } else if (function.kind == ElementKind.GENERATIVE_CONSTRUCTOR) {
      // TODO(johnniwinther): Support detection of redirecting constructors.
      if (function.modifiers.isConst()) {
        kind = Dart2JsMethodKind.CONST;
      } else {
        kind = Dart2JsMethodKind.GENERATIVE;
      }
    } else if (function.modifiers.isFactory()) {
      // TODO(johnniwinther): Support detection of redirecting constructors.
      kind = Dart2JsMethodKind.FACTORY;
    } else if (realName == 'unary-') {
      // TODO(johnniwinther): Use a constant for 'unary-'.
      kind = Dart2JsMethodKind.OPERATOR;
      // Simple name is 'unary-'.
      simpleName = 'unary-';
    } else if (simpleName.startsWith('operator\$')) {
      String str = simpleName.substring(9);
      simpleName = 'operator';
      kind = Dart2JsMethodKind.OPERATOR;
      simpleName = _getOperatorFromOperatorName(str);
    } else {
      kind = Dart2JsMethodKind.REGULAR;
    }
    return new Dart2JsMethodMirror._internal(owner, function,
        simpleName, kind);
  }

  FunctionElement get _function => _element;

  bool get isTopLevel => owner is LibraryMirror;

  // TODO(johnniwinther): This seems stale and broken.
  Symbol get constructorName => isConstructor ? simpleName : const Symbol('');

  bool get isConstructor
      => isGenerativeConstructor || isConstConstructor ||
         isFactoryConstructor || isRedirectingConstructor;

  bool get isSynthetic => false;

  bool get isStatic => _function.modifiers.isStatic();

  List<ParameterMirror> get parameters {
    return _parametersFromFunctionSignature(this,
        _function.functionSignature);
  }

  TypeMirror get returnType => owner._getTypeMirror(
      _function.functionSignature.type.returnType);

  bool get isAbstract => _function.isAbstract;

  bool get isRegularMethod => !(isGetter || isSetter || isConstructor);

  bool get isConstConstructor => _kind == Dart2JsMethodKind.CONST;

  bool get isGenerativeConstructor => _kind == Dart2JsMethodKind.GENERATIVE;

  bool get isRedirectingConstructor => _kind == Dart2JsMethodKind.REDIRECTING;

  bool get isFactoryConstructor => _kind == Dart2JsMethodKind.FACTORY;

  bool get isGetter => _kind == Dart2JsMethodKind.GETTER;

  bool get isSetter => _kind == Dart2JsMethodKind.SETTER;

  bool get isOperator => _kind == Dart2JsMethodKind.OPERATOR;

  DeclarationMirror lookupInScope(String name) {
    for (Dart2JsParameterMirror parameter in parameters) {
      if (parameter._element.name == name) {
        return parameter;
      }
    }
    return super.lookupInScope(name);
  }

  // TODO(johnniwinther): Should this really be in the interface of
  // [MethodMirror]?
  String get source => location.sourceText;

  String toString() => 'Mirror on method ${_element.name}';
}

class Dart2JsFieldMirror extends Dart2JsMemberMirror implements VariableMirror {
  final Dart2JsDeclarationMirror owner;
  VariableElement _variable;

  Dart2JsFieldMirror(Dart2JsDeclarationMirror owner,
                     VariableElement variable)
      : this.owner = owner,
        this._variable = variable,
        super(owner.mirrorSystem, variable);

  bool get isTopLevel => owner is LibraryMirror;

  bool get isStatic => _variable.modifiers.isStatic();

  bool get isFinal => _variable.modifiers.isFinal();

  bool get isConst => _variable.modifiers.isConst();

  TypeMirror get type => owner._getTypeMirror(_variable.type);


}

class Dart2JsParameterMirror extends Dart2JsMemberMirror
    implements ParameterMirror {
  final Dart2JsDeclarationMirror owner;
  final bool isOptional;
  final bool isNamed;

  factory Dart2JsParameterMirror(Dart2JsDeclarationMirror owner,
                                 ParameterElement element,
                                 {bool isOptional: false,
                                  bool isNamed: false}) {
    if (element is FieldParameterElement) {
      return new Dart2JsFieldParameterMirror(
          owner, element, isOptional, isNamed);
    }
    return new Dart2JsParameterMirror._normal(
        owner, element, isOptional, isNamed);
  }

  Dart2JsParameterMirror._normal(Dart2JsDeclarationMirror owner,
                                 ParameterElement element,
                                 this.isOptional,
                                 this.isNamed)
    : this.owner = owner,
      super(owner.mirrorSystem, element);

  ParameterElement get _element => super._element;

  TypeMirror get type => owner._getTypeMirror(_element.type,
                                              _element.functionSignature);


  bool get isFinal => false;

  bool get isConst => false;

  InstanceMirror get defaultValue {
    if (hasDefaultValue) {
      Constant constant = mirrorSystem.compiler.constantHandler
          .getConstantForVariable(_element);
      assert(invariant(_element, constant != null,
          message: "Missing constant for parameter "
                   "$_element with default value."));
      return _convertConstantToInstanceMirror(mirrorSystem, constant);
    }
    return null;
  }

  bool get hasDefaultValue {
    return _element.initializer != null;
  }

  bool get isInitializingFormal => false;

  VariableMirror get initializedField => null;
}

class Dart2JsFieldParameterMirror extends Dart2JsParameterMirror {

  Dart2JsFieldParameterMirror(Dart2JsDeclarationMirror method,
                              FieldParameterElement element,
                              bool isOptional,
                              bool isNamed)
      : super._normal(method, element, isOptional, isNamed);

  FieldParameterElement get _fieldParameterElement => _element;

  bool get isInitializingFormal => true;

  VariableMirror get initializedField => new Dart2JsFieldMirror(
      owner.owner, _fieldParameterElement.fieldElement);
}
