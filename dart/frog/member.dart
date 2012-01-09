// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/** A formal parameter to a [Method]. */
class Parameter {
  FormalNode definition;
  Member method;

  String name;
  Type type;
  bool isInitializer = false;

  Value value;

  Parameter(this.definition, this.method);

  resolve() {
    name = definition.name.name;
    if (name.startsWith('this.')) {
      name = name.substring(5);
      isInitializer = true;
    }

    type = method.resolveType(definition.type, false);

    if (definition.value != null) {
      // To match VM, detect cases where value was not actually specified in
      // code and don't signal errors.
      // TODO(jimhug): Clean up after issue #352 is resolved.
      if (!hasDefaultValue) return;

      if (method.name == ':call') {
        // TODO(jimhug): Need simpler way to detect "true" function types vs.
        //   regular methods being used as function types for closures.
        // TODO(sigmund): Disallow non-null default values for native calls?
        if (method.definition.body == null && !method.isNative) {
          world.error('default value not allowed on function type',
              definition.span);
        }
      } else if (method.isAbstract) {
        world.error('default value not allowed on abstract methods',
            definition.span);
      }
    } else if (isInitializer && !method.isConstructor) {
      world.error('initializer parameters only allowed on constructors',
          definition.span);
    }
  }

  genValue(MethodMember method, MethodGenerator context) {
    if (definition.value == null || value != null) return;

    if (context == null) { // interface method
      context = new MethodGenerator(method, null);
    }
    value = definition.value.visit(context);
    if (!value.isConst) {
      world.error('default parameter values must be constant', value.span);
    }
    value = value.convertTo(context, type);
  }

  Parameter copyWithNewType(Member newMethod, Type newType) {
    var ret = new Parameter(definition, newMethod);
    ret.type = newType;
    ret.name = name;
    ret.isInitializer = isInitializer;
    return ret;
  }

  bool get isOptional() => definition != null && definition.value != null;

  /**
   * Gets whether this named parameter has an explicit default value or relies
   * on the implicit `null`.
   */
  bool get hasDefaultValue() =>
    definition.value.span.start != definition.span.start;
}


class Member extends Element {
  final Type declaringType;

  bool isGenerated;
  MethodGenerator generator;

  Member(String name, Type declaringType)
      : isGenerated = false, this.declaringType = declaringType,
        super(name, declaringType);

  abstract bool get isStatic();
  abstract Type get returnType();

  abstract bool get canGet();
  abstract bool get canSet();

  Library get library() => declaringType.library;

  bool get isPrivate() => name.startsWith('_');

  bool get isConstructor() => false;
  bool get isField() => false;
  bool get isMethod() => false;
  bool get isProperty() => false;
  bool get isAbstract() => false;

  bool get isFinal() => false;

  // TODO(jmesserly): these only makes sense on methods, but because of
  // ConcreteMember we need to support them on Member.
  bool get isConst() => false;
  bool get isFactory() => false;

  bool get isOperator() => name.startsWith(':');
  bool get isCallMethod() => name == ':call';

  bool get requiresPropertySyntax() => false;
  bool _providePropertySyntax = false;
  bool get requiresFieldSyntax() => false;
  bool _provideFieldSyntax = false;

  bool get isNative() => false;
  String get constructorName() {
    world.internalError('can not be a constructor', span);
  }

  // Don't display an error here; we'll get a better error later.
  void provideFieldSyntax() {}
  void providePropertySyntax() {}

  Member get initDelegate() {
    world.internalError('cannot have initializers', span);
  }
  void set initDelegate(ctor) {
    world.internalError('cannot have initializers', span);
  }

  Value computeValue() {
    world.internalError('cannot have value', span);
  }

  /**
   * The inferred returnType. Right now this is just used to track
   * non-nullable bools.
   */
  Type get inferredResult() {
    var t = returnType;
    if (t.isBool && (library.isCore || library.isCoreImpl)) {
      // We trust our core libraries not to return null from bools.
      // I hope this trust is well placed!
      return world.nonNullBool;
    }
    return t;
  }

  Definition get definition() => null;

  List<Parameter> get parameters() => [];

  // TODO(jmesserly): isDynamic isn't a great name for this, something better?
  abstract Value _get(MethodGenerator context, Node node, Value target,
      [bool isDynamic]);

  abstract Value _set(MethodGenerator context, Node node, Value target,
      Value value, [bool isDynamic]);

  bool canInvoke(MethodGenerator context, Arguments args) {
    // No source location needed because canInvoke may not produce errors.
    return canGet &&
        new Value(returnType, null, null).canInvoke(context, ':call', args);
  }

  Value invoke(MethodGenerator context, Node node, Value target, Arguments args,
      [bool isDynamic=false]) {
    var newTarget = _get(context, node, target, isDynamic);
    return newTarget.invoke(context, ':call', node, args, isDynamic);
  }

  bool override(Member other) {
    if (isStatic) {
      world.error('static members can not hide parent members',
          span, other.span);
      return false;
    } else if (other.isStatic) {
      world.error('can not override static member', span, other.span);
      return false;
    }
    return true;
  }

  String get generatedFactoryName() {
    assert(this.isFactory);
    String prefix = '${declaringType.jsname}.${constructorName}\$';
    if (name == '') {
      return '${prefix}factory';
    } else {
      return '${prefix}$name\$factory';
    }
  }

  int hashCode() {
    final typeCode = declaringType == null ? 1 : declaringType.hashCode();
    final nameCode = isConstructor ? constructorName.hashCode() :
      name.hashCode();
    return (typeCode << 4) ^ nameCode;
  }

  bool operator ==(other) {
    return other is Member && isConstructor == other.isConstructor &&
        declaringType == other.declaringType && (isConstructor ?
            constructorName == other.constructorName : name == other.name);
  }
}


/**
 * Types are treated as first class members of their library's top type.
 */
// TODO(jmesserly): perhaps Type should extend Member, but that can get
// complicated.
class TypeMember extends Member {
  final DefinedType type;

  TypeMember(DefinedType type)
      : super(type.name, type.library.topType),
        this.type = type;

  SourceSpan get span() => type.definition.span;

  bool get isStatic() => true;

  // If this really becomes first class, this should return typeof(Type)
  Type get returnType() => world.varType;

  bool canInvoke(MethodGenerator context, Arguments args) => false;
  bool get canGet() => true;
  bool get canSet() => false;

  bool get requiresFieldSyntax() => true;

  Value _get(MethodGenerator context, Node node, Value target,
      [bool isDynamic=false]) {
    return new Value.type(type, node.span);
  }

  Value _set(MethodGenerator context, Node node, Value target, Value value,
      [bool isDynamic=false]) {
    world.error('cannot set type', node.span);
  }

  Value invoke(MethodGenerator context, Node node, Value target, Arguments args,
      [bool isDynamic=false]) {
    world.error('cannot invoke type', node.span);
  }
}

/** Represents a Dart field from source code. */
class FieldMember extends Member {
  final VariableDefinition definition;
  final Expression value;

  Type type;
  Value _computedValue;

  bool isStatic;
  bool isFinal;
  bool isNative;

  // TODO(jimhug): Better notion of fields that need special handling...
  bool get overridesProperty() {
    if (isStatic) return false;

    if (declaringType.parent != null) {
      var p = declaringType.parent.resolveMember(name);
      if (p != null && p.containsProperties) {
        return true;
      }
    }
    return false;
  }

  bool override(Member other) {
    if (!super.override(other)) return false;

      // fields can override properties - but nothing else?
    if (other.isProperty) {
      // TODO(jimhug):
      // other.returnType.ensureAssignableFrom(returnType, null, true);
      return true;
      // TODO(jimhug): Merge in overridesProperty logic here.
    } else {
      world.error('field can not override anything but property',
          span, other.span);
      return false;
    }
  }

  void provideFieldSyntax() {} // Nothing to do.
  void providePropertySyntax() { _providePropertySyntax = true; }

  FieldMember(String name, Type declaringType, this.definition, this.value)
      : super(name, declaringType), isNative = false;

  SourceSpan get span() => definition == null ? null : definition.span;

  Type get returnType() => type;

  bool get canGet() => true;
  bool get canSet() => !isFinal;

  bool get isField() => true;

  resolve() {
    isStatic = declaringType.isTop;
    isFinal = false;
    if (definition.modifiers != null) {
      for (var mod in definition.modifiers) {
        if (mod.kind == TokenKind.STATIC) {
          if (isStatic) {
            world.error('duplicate static modifier', mod.span);
          }
          isStatic = true;
        } else if (mod.kind == TokenKind.FINAL) {
          if (isFinal) {
            world.error('duplicate final modifier', mod.span);
          }
          isFinal = true;
        } else {
          world.error('${mod} modifier not allowed on field', mod.span);
        }
      }
    }
    type = resolveType(definition.type, false);
    if (isStatic && !isFactory && type.hasTypeParams) {
      world.error('using type parameter in static context',
          definition.type.span);
    }

    if (isStatic && isFinal && value == null) {
      world.error('static final field is missing initializer', span);
    }

    library._addMember(this);
  }


  bool _computing = false;
  /** Generates the initial value for this field, if any. Marks it as used. */
  Value computeValue() {
    if (value == null) return null;

    if (_computedValue == null) {
      if (_computing) {
        world.error('circular reference', value.span);
        return null;
      }
      _computing = true;
      var finalMethod = new MethodMember('final_context', declaringType, null);
      finalMethod.isStatic = true;
      var finalGen = new MethodGenerator(finalMethod, null);
      _computedValue = value.visit(finalGen);
      if (!_computedValue.isConst) {
        if (isStatic) {
          world.error(
            'non constant static field must be initialized in functions',
            value.span);
        } else {
          world.error(
              'non constant field must be initialized in constructor',
              value.span);
        }
      }


      if (isStatic) {
        if (isFinal && _computedValue.isConst) {
          ; // keep const as is here
        } else {
          _computedValue = world.gen.globalForStaticField(
              this, _computedValue, [_computedValue]);
        }
      }
      _computing = false;
    }
    return _computedValue;
  }

  Value _get(MethodGenerator context, Node node, Value target,
      [bool isDynamic=false]) {
    if (isNative && returnType != null) {
      returnType.markUsed();
      if (returnType is DefinedType) {
        // TODO(jmesserly): this handles native fields that return types like
        // "List". Is there a better solution for fields? Unlike methods we have
        // no good way to annotate them.
        var defaultType = returnType.genericType.defaultType;
        if (defaultType != null && defaultType.isNative) {
          defaultType.markUsed();
        }
      }
    }

    if (isStatic) {
      // TODO(jmesserly): can we avoid generating the whole type?
      declaringType.markUsed();

      // Make sure to compute the value of all static fields, even if we don't
      // use this value immediately.
      var cv = computeValue();
      if (isFinal) {
        return cv;
      }
      world.gen.hasStatics = true;
      if (declaringType.isTop) {
        if (declaringType.library.isDom) {
          // TODO(jmesserly): this check doesn't look right.
          return new Value(type, '$jsname', node.span);
        } else {
          return new Value(type, '\$globals.$jsname', node.span);
        }
      } else if (declaringType.isNative) {
        if (declaringType.isHiddenNativeType) {
          // TODO: Could warn at parse time.
          world.error('static field of hidden native type is inaccessible',
              node.span);
        }
        return new Value(type, '${declaringType.jsname}.$jsname', node.span);
      } else {
        return new Value(type,
            '\$globals.${declaringType.jsname}_$jsname', node.span);
      }
    }
    return new Value(type, '${target.code}.$jsname', node.span);
  }

  Value _set(MethodGenerator context, Node node, Value target, Value value,
      [bool isDynamic=false]) {
    var lhs = _get(context, node, target, isDynamic);
    value = value.convertTo(context, type, isDynamic);
    return new Value(type, '${lhs.code} = ${value.code}', node.span);
  }
}

class PropertyMember extends Member {
  MethodMember getter;
  MethodMember setter;

  Member _overriddenField;

  // TODO(jimhug): What is the right span for this beast?
  SourceSpan get span() => getter != null ? getter.span : null;

  bool get canGet() => getter != null;
  bool get canSet() => setter != null;

  // If the property is just a declaration in an interface, continue to allow
  // field syntax in the generated code.
  bool get requiresPropertySyntax() => declaringType.isClass;

  void provideFieldSyntax() { _provideFieldSyntax = true; }
  void providePropertySyntax() {
    // when overriding native fields, we still provide a field syntax to ensure
    // that native functions will find the appropriate property implementation.
    // TODO(sigmund): should check for this transitively...
    if (_overriddenField != null && _overriddenField.isNative) {
      provideFieldSyntax();
    }
  }

  // TODO(jimhug): Union of getter and setters sucks!
  bool get isStatic() => getter == null ? setter.isStatic : getter.isStatic;

  bool get isProperty() => true;

  Type get returnType() {
    return getter == null ? setter.returnType : getter.returnType;
  }

  PropertyMember(String name, Type declaringType): super(name, declaringType);

  bool override(Member other) {
    if (!super.override(other)) return false;

    // properties can override other properties and fields
    if (other.isProperty || other.isField) {
      // TODO(jimhug):
      // other.returnType.ensureAssignableFrom(returnType, null, true);
      if (other.isProperty) addFromParent(other);
      else _overriddenField = other;
      return true;
    } else {
      world.error('property can only override field or property',
          span, other.span);
      return false;
    }
  }

  Value _get(MethodGenerator context, Node node, Value target,
      [bool isDynamic=false]) {
    if (getter == null) {
      if (_overriddenField != null) {
        return _overriddenField._get(context, node, target, isDynamic);
      }
      return target.invokeNoSuchMethod(context, 'get:$name', node);
    }
    return getter.invoke(context, node, target, Arguments.EMPTY);
  }

  Value _set(MethodGenerator context, Node node, Value target, Value value,
      [bool isDynamic=false]) {
    if (setter == null) {
      if (_overriddenField != null) {
        return _overriddenField._set(context, node, target, value, isDynamic);
      }
      return target.invokeNoSuchMethod(context, 'set:$name', node,
        new Arguments(null, [value]));
    }
    return setter.invoke(context, node, target, new Arguments(null, [value]),
        isDynamic);
  }

  addFromParent(Member parentMember) {
    // TODO(jimhug): Egregious Hack!
    PropertyMember parent;
    if (parentMember is ConcreteMember) {
      ConcreteMember c = parentMember;
      parent = c.baseMember;
    } else {
      parent = parentMember;
    }

    if (getter == null) getter = parent.getter;
    if (setter == null) setter = parent.setter;
  }

  resolve() {
    if (getter != null) {
      getter.resolve();
      if (getter.parameters.length != 0) {
        world.error('getter methods should take no arguments',
            getter.definition.span);
      }
      if (getter.returnType.isVoid) {
        world.warning('getter methods should not be void',
            getter.definition.returnType.span);
      }
    }
    if (setter != null) {
      setter.resolve();
      if (setter.parameters.length != 1) {
        world.error('setter methods should take a single argument',
            setter.definition.span);
      }
      // Not issue warning if setter is implicitly dynamic (returnType == null),
      // but do if it is explicit (returnType.isVar)
      if (!setter.returnType.isVoid && setter.definition.returnType != null) {
        world.warning('setter methods should be void',
            setter.definition.returnType.span);
      }
    }

    library._addMember(this);
  }
}


class ConcreteMember extends Member {
  final Member baseMember;
  Type returnType;
  List<Parameter> parameters;

  ConcreteMember(String name, ConcreteType declaringType, this.baseMember)
      : super(name, declaringType) {
    parameters = [];
    returnType = baseMember.returnType.resolveTypeParams(declaringType);
    // TODO(jimhug): Optimize not creating new array if no new param types.
    for (var p in baseMember.parameters) {
      var newType = p.type.resolveTypeParams(declaringType);
      if (newType != p.type) {
        parameters.add(p.copyWithNewType(this, newType));
      } else {
        parameters.add(p);
      }
    }
  }

  SourceSpan get span() => baseMember.span;

  bool get isStatic() => baseMember.isStatic;
  bool get isAbstract() => baseMember.isAbstract;
  bool get isConst() => baseMember.isConst;
  bool get isFactory() => baseMember.isFactory;
  bool get isFinal() => baseMember.isFinal;
  bool get isNative() => baseMember.isNative;

  String get jsname() => baseMember.jsname;
  set jsname(String name) =>
    world.internalError('bad set of jsname on ConcreteMember');


  bool get canGet() => baseMember.canGet;
  bool get canSet() => baseMember.canSet;
  bool canInvoke(MethodGenerator context, Arguments args) =>
    baseMember.canInvoke(context, args);

  bool get isField() => baseMember.isField;
  bool get isMethod() => baseMember.isMethod;
  bool get isProperty() => baseMember.isProperty;

  bool get requiresPropertySyntax() => baseMember.requiresPropertySyntax;
  bool get requiresFieldSyntax() => baseMember.requiresFieldSyntax;

  void provideFieldSyntax() => baseMember.provideFieldSyntax();
  void providePropertySyntax() => baseMember.providePropertySyntax();

  bool get isConstructor() => name == declaringType.name;

  String get constructorName() => baseMember.constructorName;

  Definition get definition() => baseMember.definition;

  // TODO(sigmund): this is EGREGIOUS
  Member get initDelegate() => baseMember.initDelegate;
  void set initDelegate(ctor) { baseMember.initDelegate = ctor; }

  Type resolveType(TypeReference node, bool isRequired) {
    var type = baseMember.resolveType(node, isRequired);
    return type.resolveTypeParams(declaringType);
  }

  Value computeValue() => baseMember.computeValue();

  // TODO(jimhug): Add support for type params.
  bool override(Member other) => baseMember.override(other);

  Value _get(MethodGenerator context, Node node, Value target,
      [bool isDynamic=false]) {
    Value ret = baseMember._get(context, node, target, isDynamic);
    return new Value(inferredResult, ret.code, node.span);
  }

  Value _set(MethodGenerator context, Node node, Value target, Value value,
      [bool isDynamic=false]) {
    // TODO(jimhug): Check arg types in context of concrete type.
    Value ret = baseMember._set(context, node, target, value, isDynamic);
    return new Value(returnType, ret.code, node.span);
  }

  _evalConstConstructor(ObjectValue newObject, Arguments args) {
    // TODO(jimhug): Concrete type probably matters somehow here
    return baseMember.dynamic._evalConstConstructor(newObject, args);
  }


  Value invoke(MethodGenerator context, Node node, Value target, Arguments args,
      [bool isDynamic=false]) {
    // TODO(jimhug): Check arg types in context of concrete type.
    Value ret = baseMember.invoke(context, node, target, args, isDynamic);
    var code = ret.code;
    if (isConstructor) {
      // TODO(jimhug): Egregious hack - won't live through the year.
      code = code.replaceFirst(
          declaringType.genericType.jsname, declaringType.jsname);
    }
    if (baseMember is MethodMember) {
      declaringType.genMethod(this);
    }
    return new Value(inferredResult, code, node.span);
  }
}


/** Represents a Dart method or top-level function. */
class MethodMember extends Member {
  FunctionDefinition definition;
  Type returnType;
  List<Parameter> parameters;

  Type _functionType;
  bool isStatic = false;
  bool isAbstract = false;

  // Note: these two modifiers are only legal on constructors
  bool isConst = false;
  bool isFactory = false;

  /** True if this is a function defined inside another method. */
  bool isLambda = false;

  /**
   * True if we should provide info on optional parameters for use by runtime
   * dispatch.
   */
  bool _provideOptionalParamInfo = false;

  /*
   * When this is a constructor, contains any other constructor called during
   * initialization (if any).
   */
  Member initDelegate;

  MethodMember(String name, Type declaringType, this.definition)
      : super(name, declaringType);

  bool get isConstructor() => name == declaringType.name;
  bool get isMethod() => !isConstructor;

  bool get isNative() => definition.nativeBody != null;

  bool get canGet() => true;
  bool get canSet() => false;

  bool get requiresPropertySyntax() => true;

  SourceSpan get span() => definition == null ? null : definition.span;

  String get constructorName() {
    var returnType = definition.returnType;
    if (returnType == null) return '';
    if (returnType is GenericTypeReference) {
      return '';
    }

    // TODO(jmesserly): make this easier?
    if (returnType.names != null) {
      return returnType.names[0].name;
    } else if (returnType.name != null) {
      return returnType.name.name;
    }
    world.internalError('no valid constructor name', definition.span);
  }

  Type get functionType() {
    if (_functionType == null) {
      _functionType =
          library.getOrAddFunctionType(declaringType, name, definition);
      // TODO(jimhug): Better resolution checks.
      if (parameters == null) {
        resolve();
      }
    }
    return _functionType;
  }

  bool override(Member other) {
    if (!super.override(other)) return false;

    // methods can only override other methods
    if (other.isMethod) {
      // TODO(jimhug):
      // other.returnType.ensureAssignableFrom(returnType, null, true);
      // TODO(jimhug): Check for further parameter compatibility.
      return true;
    } else {
      world.error('method can only override methods', span, other.span);
      return false;
    }
  }

  bool canInvoke(MethodGenerator context, Arguments args) {
    int bareCount = args.bareCount;

    if (bareCount > parameters.length) return false;

    if (bareCount == parameters.length) {
      if (bareCount != args.length) return false;
    } else {
      if (!parameters[bareCount].isOptional) return false;

      for (int i = bareCount; i < args.length; i++) {
        if (indexOfParameter(args.getName(i)) < 0) {
          return false;
        }
      }
    }

    return true;
  }

  // TODO(jmesserly): might need to make this faster
  /** Gets the index of an optional parameter. */
  int indexOfParameter(String name) {
    for (int i = 0; i < parameters.length; i++) {
      final p = parameters[i];
      if (p.isOptional && p.name == name) {
        return i;
      }
    }
    return -1;
  }

  void provideFieldSyntax() { _provideFieldSyntax = true; }
  void providePropertySyntax() { _providePropertySyntax = true; }

  Value _set(MethodGenerator context, Node node, Value target, Value value,
      [bool isDynamic=false]) {
    world.error('cannot set method', node.span);
  }

  Value _get(MethodGenerator context, Node node, Value target,
      [bool isDynamic=false]) {
    // TODO(jimhug): Would prefer to invoke!
    declaringType.genMethod(this);
    _provideOptionalParamInfo = true;
    if (isStatic) {
      // ensure the type is generated.
      // TODO(sigmund): can we avoid generating the entire type, but only what
      // we need?
      declaringType.markUsed();
      var type = declaringType.isTop ? '' : '${declaringType.jsname}.';
      return new Value(functionType, '$type$jsname', node.span);
    }
    _providePropertySyntax = true;
    return new Value(functionType, '${target.code}.get\$$jsname()', node.span);
  }

  /**
   * Checks if the named arguments are in their natural or 'home' positions,
   * i.e. they may be passed directly without inserting, deleting or moving the
   * arguments to correspond with the parameters.
   */
  bool namesInHomePositions(Arguments args) {
    if (!args.hasNames) return true;

    for (int i = args.bareCount; i < args.values.length; i++) {
      if (i >= parameters.length) {
        return false;
      }
      if (args.getName(i) != parameters[i].name) {
        return false;
      }
    }
    return true;
  }

  bool namesInOrder(Arguments args) {
    if (!args.hasNames) return true;

    int lastParameter = null;
    for (int i = args.bareCount; i < parameters.length; i++) {
      var p = args.getIndexOfName(parameters[i].name);
      // Only worry about parameters that needTemps. Otherwise it's fine to
      // reorder.
      if (p >= 0 && args.values[p].needsTemp) {
        if (lastParameter != null && lastParameter > p) {
          return false;
        }
        lastParameter = p;
      }
    }
    return true;
  }

  /** Returns true if any of the arguments will need conversion. */
  // TODO(jmesserly): I don't like how this is coupled to invoke
  bool needsArgumentConversion(Arguments args) {
    int bareCount = args.bareCount;
    for (int i = 0; i < bareCount; i++) {
      var arg = args.values[i];
      if (arg.needsConversion(parameters[i].type)) {
        return true;
      }
    }

    if (bareCount < parameters.length) {
      genParameterValues();
      for (int i = bareCount; i < parameters.length; i++) {
        var arg = args.getValue(parameters[i].name);
        if (arg != null && arg.needsConversion(parameters[i].type)) {
          return true;
        }
      }
    }

    return false;
  }

  static String _argCountMsg(int actual, int expected, [bool atLeast=false]) {
    return 'wrong number of positional arguments, expected ' +
        '${atLeast ? "at least " : ""}$expected but found $actual';
  }

  Value _argError(MethodGenerator context, Node node, Value target,
      Arguments args, String msg, int argIndex) {
    SourceSpan span;
    if ((args.nodes == null) || (argIndex >= args.nodes.length)) {
      span = node.span;
    } else {
      span = args.nodes[argIndex].span;
    }
    if (isStatic || isConstructor) {
      world.error(msg, span);
    } else {
      world.warning(msg, span);
    }
    return target.invokeNoSuchMethod(context, name, node, args);
  }

  genParameterValues() {
    // Pure lazy?
    for (var p in parameters) p.genValue(this, generator);
  }

  /**
   * Invokes this method on the given [target] with the given [args].
   * [node] provides a [SourceSpan] for any error messages.
   */
  Value invoke(MethodGenerator context, Node node, Value target,
      Arguments args, [bool isDynamic=false]) {
    // TODO(jimhug): Fix this hack for ensuring a method is resolved.
    if (parameters == null) {
      world.info('surprised to need to resolve: ${declaringType.name}.$name');
      resolve();
    }

    declaringType.genMethod(this);

    if (isStatic || isFactory) {
      // TODO(sigmund): can we avoid generating the entire type, but only what
      // we need?
      declaringType.markUsed();
    }

    // TODO(jmesserly): get rid of this in favor of using the native method
    // "bodies" to tell the compiler about valid return types.
    if (isNative && returnType != null) returnType.markUsed();

    if (!namesInOrder(args)) {
      // Names aren't in order. For now, use a var call because it's an
      // easy way to get the right eval order for out of order arguments.
      return context.findMembers(name).invokeOnVar(context, node, target, args);
    }

    var argsCode = [];
    if (!target.isType && (isConstructor || target.isSuper)) {
      argsCode.add('this');
    }

    int bareCount = args.bareCount;
    for (int i = 0; i < bareCount; i++) {
      var arg = args.values[i];
      if (i >= parameters.length) {
        var msg = _argCountMsg(args.length, parameters.length);
        return _argError(context, node, target, args, msg, i);
      }
      arg = arg.convertTo(context, parameters[i].type, isDynamic);
      argsCode.add(arg.code);
    }

    int namedArgsUsed = 0;
    if (bareCount < parameters.length) {
      genParameterValues();

      for (int i = bareCount; i < parameters.length; i++) {
        var arg = args.getValue(parameters[i].name);
        if (arg == null) {
          arg = parameters[i].value;
        } else {
          arg = arg.convertTo(context, parameters[i].type, isDynamic);
          namedArgsUsed++;
        }

        if (arg == null || !parameters[i].isOptional) {
          var msg = _argCountMsg(Math.min(i, args.length), i + 1, atLeast:true);
          return _argError(context, node, target, args, msg, i);
        } else {
          argsCode.add(arg.code);
        }
      }
      Arguments.removeTrailingNulls(argsCode);
    }

    if (namedArgsUsed < args.nameCount) {
      // Find the unused argument name
      var seen = new Set<String>();
      for (int i = bareCount; i < args.length; i++) {
        var name = args.getName(i);
        if (seen.contains(name)) {
          return _argError(context, node, target, args,
              'duplicate argument "$name"', i);
        }
        seen.add(name);
        int p = indexOfParameter(name);
        if (p < 0) {
          return _argError(context, node, target, args,
              'method does not have optional parameter "$name"', i);
        } else if (p < bareCount) {
          return _argError(context, node, target, args,
              'argument "$name" passed as positional and named',
              // Given that the named was mentioned explicitly, highlight the
              // positional location instead:
              p);
        }
      }
      world.internalError('wrong named arguments calling $name', node.span);
    }

    var argsString = Strings.join(argsCode, ', ');

    if (isConstructor) {
      return _invokeConstructor(context, node, target, args, argsString);
    }

    if (target.isSuper) {
      return new Value(inferredResult,
          '${declaringType.jsname}.prototype.$jsname.call($argsString)',
          node.span);
    }

    if (isOperator) {
      return _invokeBuiltin(context, node, target, args, argsCode, isDynamic);
    }

    if (isFactory) {
      assert(target.isType);
      return new Value(target.type, '$generatedFactoryName($argsString)',
          node !== null ? node.span : null);
    }

    if (isStatic) {
      if (declaringType.isTop) {
        return new Value(inferredResult,
            '$jsname($argsString)', node !== null ? node.span : null);
      }
      return new Value(inferredResult,
          '${declaringType.jsname}.$jsname($argsString)', node.span);
    }

    // TODO(jmesserly): factor this better
    if (name == 'get:typeName' && declaringType.library.isDom) {
      world.gen.corejs.ensureTypeNameOf();
    }

    var code = '${target.code}.$jsname($argsString)';
    return new Value(inferredResult, code, node.span);
  }

  Value _invokeConstructor(MethodGenerator context, Node node,
      Value target, Arguments args, argsString) {
    declaringType.markUsed();

    String ctor = constructorName;
    if (ctor != '') ctor = '.${ctor}\$ctor';

    final span = node != null ? node.span : target.span;
    if (!target.isType) {
      // initializer call to another constructor
      var code = '${declaringType.nativeName}${ctor}.call($argsString)';
      return new Value(target.type, code, span);
    } else {
      // Start of abstract interpretation to replace const hacks goes here
      // TODO(jmesserly): using the "node" here feels really hacky
      if (isConst && node is NewExpression && node.dynamic.isConst) {
        // TODO(jimhug): Embedding JSSyntaxRegExp works around an annoying
        //   issue with tracking native constructors for const objects.
        if (isNative || declaringType.name == 'JSSyntaxRegExp') {
          // check that all args are const?
          var code = 'new ${declaringType.nativeName}${ctor}($argsString)';
          return world.gen.globalForConst(new Value(target.type, code, span),
            [args.values]);
        }
        var newType = declaringType;
        var newObject = new ObjectValue(true, newType, span);
        newObject.initFields();
        _evalConstConstructor(newObject, args);
        return world.gen.globalForConst(newObject, [args.values]);
      } else {
        var code = 'new ${declaringType.nativeName}${ctor}($argsString)';
        return new Value(target.type, code, span);
      }
    }
  }

  _evalConstConstructor(Value newObject, Arguments args) {
    declaringType.markUsed();
    var generator = new MethodGenerator(this, null);
    generator.evalBody(newObject, args);
  }

  Value _invokeBuiltin(MethodGenerator context, Node node, Value target,
      Arguments args, argsCode, bool isDynamic) {
    // Handle some fast paths for Number, String, List and DOM.
    if (declaringType.isNum) {
      // TODO(jimhug): This fails in bad ways when argsCode[1] is not num.
      // TODO(jimhug): What about null?
      var code;
      if (name == ':negate') {
        code = '-${target.code}';
      } else if (name == ':bit_not') {
        code = '~${target.code}';
      } else if (name == ':truncdiv' || name == ':mod') {
        world.gen.corejs.useOperator(name);
        code = '$jsname(${target.code}, ${argsCode[0]})';
      } else {
        var op = TokenKind.rawOperatorFromMethod(name);
        code = '${target.code} $op ${argsCode[0]}';
      }

      return new Value(inferredResult, code, node.span);
    } else if (declaringType.isString) {
      if (name == ':index') {
        return new Value(declaringType, '${target.code}[${argsCode[0]}]',
          node.span);
      } else if (name == ':add') {
        return new Value(declaringType, '${target.code} + ${argsCode[0]}',
          node.span);
      }
    } else if (declaringType.isNative) {
      if (name == ':index') {
        return
            new Value(returnType, '${target.code}[${argsCode[0]}]', node.span);
      } else if (name == ':setindex') {
        return new Value(returnType,
            '${target.code}[${argsCode[0]}] = ${argsCode[1]}', node.span);
      }
    }

    // TODO(jimhug): Optimize null on lhs as well.
    if (name == ':eq' || name == ':ne') {
      final op = name == ':eq' ? '==' : '!=';

      if (name == ':ne') {
        // Ensure == is generated.
        target.invoke(context, ':eq', node, args, isDynamic);
      }

      // Optimize test when null is on the rhs.
      if (argsCode[0] == 'null') {
        return new Value(inferredResult, '${target.code} $op null', node.span);
      } else if (target.type.isNum || target.type.isString) {
        // TODO(jimhug): Maybe check rhs.
        return new Value(inferredResult, '${target.code} $op ${argsCode[0]}',
            node.span);
      }
      world.gen.corejs.useOperator(name);
      // TODO(jimhug): Should be able to use faster path sometimes here!
      return new Value(inferredResult,
          '$jsname(${target.code}, ${argsCode[0]})', node.span);
    }

    if (isCallMethod) {
      declaringType.markUsed();
      return new Value(inferredResult,
          '${target.code}(${Strings.join(argsCode, ", ")})', node.span);
    }

    if (name == ':index') {
      world.gen.corejs.useIndex = true;
    } else if (name == ':setindex') {
      world.gen.corejs.useSetIndex = true;
    }

    // Fall back to normal method invocation.
    var argsString = Strings.join(argsCode, ', ');
    return new Value(inferredResult, '${target.code}.$jsname($argsString)',
        node.span);
  }

  resolve() {
    // TODO(jimhug): work through side-by-side with spec
    isStatic = declaringType.isTop;
    isConst = false;
    isFactory = false;
    isAbstract = !declaringType.isClass;
    if (definition.modifiers != null) {
      for (var mod in definition.modifiers) {
        if (mod.kind == TokenKind.STATIC) {
          if (isStatic) {
            world.error('duplicate static modifier', mod.span);
          }
          isStatic = true;
        } else if (isConstructor && mod.kind == TokenKind.CONST) {
          if (isConst) {
            world.error('duplicate const modifier', mod.span);
          }
          if (isFactory) {
            world.error('const factory not allowed', mod.span);
          }
          isConst = true;
        } else if (mod.kind == TokenKind.FACTORY) {
          if (isFactory) {
            world.error('duplicate factory modifier', mod.span);
          }
          if (isConst) {
            world.error('const factory not allowed', mod.span);
          }
          if (isStatic) {
            world.error('static factory not allowed', mod.span);
          }
          isFactory = true;
        } else if (mod.kind == TokenKind.ABSTRACT) {
          if (isAbstract) {
            if (declaringType.isClass) {
              world.error('duplicate abstract modifier', mod.span);
            } else if (!isCallMethod) {
              world.error('abstract modifier not allowed on interface members',
                mod.span);
            }
          }
          isAbstract = true;
        } else {
          world.error('${mod} modifier not allowed on method', mod.span);
        }
      }
    }

    if (isFactory) {
      isStatic = true;
    }

    // TODO(jimhug): need a better annotation for being an operator method
    if (isOperator && isStatic && !isCallMethod) {
      world.error('operator method may not be static "${name}"', span);
    }

    if (isAbstract) {
      if (definition.body != null &&
          declaringType.definition is! FunctionTypeDefinition) {
        // TODO(jimhug): Creating function types for concrete methods is
        //   steadily feeling uglier...
        world.error('abstract method can not have a body', span);
      }
      if (isStatic &&
          declaringType.definition is! FunctionTypeDefinition) {
        world.error('static method can not be abstract', span);
      }
    } else {
      if (definition.body == null && !isConstructor && !isNative) {
        world.error('method needs a body', span);
      }
    }

    if (isConstructor && !isFactory) {
      returnType = declaringType;
    } else {
      returnType = resolveType(definition.returnType, false);
    }
    parameters = [];
    for (var formal in definition.formals) {
      // TODO(jimhug): Clean up construction of Parameters.
      var param = new Parameter(formal, this);
      param.resolve();
      parameters.add(param);
    }

    if (!isLambda) {
      library._addMember(this);
    }
  }

  /** Overriden to ensure that type arguments aren't used in static methods. */
  Type resolveType(TypeReference node, bool typeErrors) {
    Type t = super.resolveType(node, typeErrors);
    if (isStatic && !isFactory && t is ParameterType) {
      world.error('using type parameter in static context.', node.span);
    }
    return t;
  }
}


class MemberSet {
  final String name;
  final List<Member> members;
  final String jsname;
  final bool isVar;

  MemberSet(Member member, [bool isVar=false]):
    name = member.name, members = [member], jsname = member.jsname,
    isVar = isVar;

  toString() => '$name:${members.length}';

  // TODO(jimhug): Still working towards the right logic for conflicts...
  bool get containsProperties() => members.some((m) => m is PropertyMember);
  bool get containsMethods() => members.some((m) => m is MethodMember);


  void add(Member member) => members.add(member);

  // TODO(jimhug): Always false, or is this needed?
  bool get isStatic() => members.length == 1 && members[0].isStatic;
  bool get isOperator() => members[0].isOperator;

  bool canInvoke(MethodGenerator context, Arguments args) =>
    members.some((m) => m.canInvoke(context, args));

  Value _makeError(Node node, Value target, String action) {
    if (!target.type.isVar) {
      world.warning('could not find applicable $action for "$name"', node.span);
    }
    return new Value(world.varType,
        '${target.code}.$jsname() /*no applicable $action*/', node.span);
  }

  bool _treatAsField;
  bool get treatAsField() {
    if (_treatAsField == null) {
      // If this is the global MemberSet from world, always bind dynamically.
      // Note: we need this for proper noSuchMethod and REPL behavior.
      _treatAsField = !isVar && (members.some((m) => m.requiresFieldSyntax)
          || members.every((m) => !m.requiresPropertySyntax));

      for (var member in members) {
        if (_treatAsField) {
          member.provideFieldSyntax();
        } else {
          member.providePropertySyntax();
        }
      }
    }
    return _treatAsField;
  }

  Value _get(MethodGenerator context, Node node, Value target,
      [bool isDynamic=false]) {
    // If this is the global MemberSet from world, always bind dynamically.
    // Note: we need this for proper noSuchMethod and REPL behavior.
    Value returnValue;
    final targets = members.filter((m) => m.canGet);
    if (isVar) {
      targets.forEach((m) => m._get(context, node, target, isDynamic: true));
      returnValue = new Value(_foldTypes(targets), null, node.span);
    } else {
      if (members.length == 1) {
        return members[0]._get(context, node, target, isDynamic);
      } else if (targets.length == 1) {
        return targets[0]._get(context, node, target, isDynamic);
      }

      for (var member in targets) {
        final value = member._get(context, node, target, isDynamic:true);
        returnValue = _tryUnion(returnValue, value, node);
      }
      if (returnValue == null) {
        return _makeError(node, target, 'getter');
      }
    }

    if (returnValue.code == null) {
      if (treatAsField) {
        return new Value(returnValue.type, '${target.code}.$jsname',
            node.span);
      } else {
        return new Value(returnValue.type, '${target.code}.get\$$jsname()',
            node.span);
      }
    }
    return returnValue;
  }

  Value _set(MethodGenerator context, Node node, Value target, Value value,
      [bool isDynamic=false]) {
    // If this is the global MemberSet from world, always bind dynamically.
    // Note: we need this for proper noSuchMethod and REPL behavior.
    Value returnValue;
    final targets = members.filter((m) => m.canSet);
    if (isVar) {
      targets.forEach((m) =>
          m._set(context, node, target, value, isDynamic: true));
      returnValue = new Value(_foldTypes(targets), null, node.span);
    } else {
      if (members.length == 1) {
        return members[0]._set(context, node, target, value, isDynamic);
      } else if (targets.length == 1) {
        return targets[0]._set(context, node, target, value, isDynamic);
      }

      for (var member in targets) {
        final res = member._set(context, node, target, value, isDynamic:true);
        returnValue = _tryUnion(returnValue, res, node);
      }
      if (returnValue == null) {
        return _makeError(node, target, 'setter');
      }
    }

    if (returnValue.code == null) {
      if (treatAsField) {
        return new Value(returnValue.type,
          '${target.code}.$jsname = ${value.code}', node.span);
      } else {
        return new Value(returnValue.type,
          '${target.code}.set\$$jsname(${value.code})', node.span);
      }
    }
    return returnValue;
  }

  Value invoke(MethodGenerator context, Node node, Value target,
      Arguments args, [bool isDynamic=false]) {
    // If this is the global MemberSet from world, always bind dynamically.
    // Note: we need this for proper noSuchMethod and REPL behavior.
    if (isVar && !isOperator) {
      return invokeOnVar(context, node, target, args);
    }

    if (members.length == 1) {
      return members[0].invoke(context, node, target, args, isDynamic);
    }
    final targets = members.filter((m) => m.canInvoke(context, args));
    if (targets.length == 1) {
      return targets[0].invoke(context, node, target, args, isDynamic);
    }

    Value returnValue = null;
    for (var member in targets) {
      final res = member.invoke(context, node, target, args, isDynamic:true);
      // TODO(jmesserly): If the code has different type checks, it will fail to
      // unify and go through a dynamic stub. Good so far. However, we'll end
      // up with a bogus unused temp generated (usually "var $0"). We need a way
      // to throw away temps when we throw away the code.
      returnValue = _tryUnion(returnValue, res, node);
    }

    if (returnValue == null) {
      return _makeError(node, target, 'method');
    }

    if (returnValue.code == null) {
      if (name == ':call') {
        // TODO(jmesserly): reconcile this with similar code in Value
        return target._varCall(context, node, args);
      } else if (isOperator) {
        // TODO(jmesserly): make operators less special.
        return invokeSpecial(target, args, returnValue.type);
      } else {
        return invokeOnVar(context, node, target, args);
      }
    }

    return returnValue;
  }

  Value invokeSpecial(Value target, Arguments args, Type returnType) {
    assert(name.startsWith(':'));
    assert(!args.hasNames);
    // TODO(jimhug): We need to do this a little bit more like get and set on
    // properties.  We should check the set of members for something
    // like "requiresNativeIndexer" and "requiresDartIndexer" to
    // decide on a strategy.

    var argsString = args.getCode();
    // Most operator calls need to be emitted as function calls, so we don't
    // box numbers accidentally. Indexing is the exception.
    if (name == ':index' || name == ':setindex') {
      return new Value(returnType, '${target.code}.$jsname($argsString)',
          target.span);
    } else {
      if (argsString.length > 0) argsString = ', $argsString';
      world.gen.corejs.useOperator(name);
      return new Value(returnType, '$jsname(${target.code}$argsString)',
          target.span);
    }
  }

  Value invokeOnVar(MethodGenerator context, Node node, Value target,
      Arguments args) {
    var member = getVarMember(context, node, args);
    return member.invoke(context, node, target, args);
  }

  Value _union(Value x, Value y, Node node) {
    var result = _tryUnion(x, y, node);
    if (result.code == null) {
      world.internalError('mismatched code for $name (${x.code}, ${y.code})',
          node.span);
    }
    return result;
  }

  Value _tryUnion(Value x, Value y, Node node) {
    if (x == null) return y;
    var type = Type.union(x.type, y.type);
    if (x.code == y.code) {
      if (type == x.type) {
        return x;
      } else if (x.isConst || y.isConst) {
        world.internalError("unexpected: union of const values ");
      } else {
        // TODO(jimhug): This is icky - but this whole class needs cleanup.
        var ret = new Value(type, x.code, node.span);
        ret.isSuper = x.isSuper && y.isSuper;
        ret.needsTemp = x.needsTemp || y.needsTemp;
        ret.isType = x.isType && y.isType;
        return ret;
      }
    } else {
      return new Value(type, null, node.span);
    }
  }

  dumpAllMembers() {
    for (var member in members) {
      world.warning('hard-multi $name on ${member.declaringType.name}',
          member.span);
    }
  }

  VarMember getVarMember(MethodGenerator context, Node node, Arguments args) {
    if (world.objectType.varStubs == null) {
      world.objectType.varStubs = {};
    }

    var stubName = _getCallStubName(name, args);
    var stub = world.objectType.varStubs[stubName];
    if (stub == null) {
      // Ensure that we're making stub with all possible members of this name.
      // We need this canonicalization step because only one VarMemberSet can
      // live on Object.prototype
      // TODO(jmesserly): this is ugly--we're throwing away type information!
      // The right solution is twofold:
      //   1. put stubs on a more precise type when possible
      //   2. merge VarMemberSets together if necessary
      final mset = context.findMembers(name).members;

      final targets = mset.filter((m) => m.canInvoke(context, args));
      stub = new VarMethodSet(name, stubName, targets, args,
          _foldTypes(targets));
      world.objectType.varStubs[stubName] = stub;
    }
    return stub;
  }

  Type _foldTypes(List<Member> targets) =>
    reduce(map(targets, (t) => t.returnType), Type.union, world.varType);
}

/**
 * A [FactoryMap] maps type names to a list of factory constructors.
 * The constructors list is actually a map that maps factory names to
 * [MethodMember]. The reason why we need both indirections are:
 * 1) A class can define factory methods for multiple interfaces.
 * 2) A factory constructor can have a name.
 *
 * For example:
 *
 * [:
 * interface I factory A {
 *   I();
 *   I.foo();
 * }
 *
 * interface I2 factory A {
 *   I2();
 * }
 *
 * class A {
 *   factory I() { ... }     // Member1
 *   factory I.foo() { ... } // Member2
 *   factory I2() { ... }    // Member3
 *   factory A() { ... }     // Member4
 * }
 * :]
 *
 * The [:factories:] field of A will be a [FactoryMap] that looks
 * like:
 * { "I"  : { "": Member1, "foo": Member2 },
 *   "I2" : { "": Member3 },
 *   "A"  : { "", Member4 }
 * }
 */
class FactoryMap {
  Map<String, Map<String, Member>> factories;

  FactoryMap() : factories = {};

  // Returns the factories defined for [type].
  Map<String, Member> getFactoriesFor(String typeName) {
    var ret = factories[typeName];
    if (ret == null) {
      ret = {};
      factories[typeName] = ret;
    }
    return ret;
  }

  void addFactory(String typeName, String name, Member member) {
    getFactoriesFor(typeName)[name] = member;
  }

  Member getFactory(String typeName, String name) {
    return getFactoriesFor(typeName)[name];
  }

  void forEach(void f(Member member)) {
    factories.forEach((_, Map constructors) {
      constructors.forEach((_, Member member) {
        f(member);
      });
    });
  }
}
