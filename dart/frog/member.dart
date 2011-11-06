// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/** A formal parameter to a [Method]. */
class Parameter {
  FormalNode definition;

  String name;
  Type type;

  Value value;

  Parameter(this.definition);

  resolve(Member method, Type inType) {
    name = definition.name.name;
    type = inType.resolveType(definition.type, false);

    if (method.isStatic && type.hasTypeParams) {
      world.error('using type parameter in static context', definition.span);
    }

    if (definition.value != null) {
      // To match VM, detect cases where value was not actually specified in
      // code and don't signal errors.
      // TODO(jimhug): Clean up after issue #352 is resolved.
      if (definition.value is NullExpression &&
          definition.value.span.start == definition.span.start) {
        return;
      }
      if (method.isAbstract) {
        world.error('default value not allowed on abstract methods',
          definition.span);
      } else if (method.name == '\$call' && method.definition.body == null) {
        // TODO(jimhug): Need simpler way to detect "true" function types vs.
        //   regular methods being used as function types for closures.
        world.error('default value not allowed on function type',
          definition.span);
      }
    }
  }

  genValue(MethodMember method, MethodGenerator context) {
    if (definition.value == null || value != null) return;

    if (context == null) { // interface method
      context = new MethodGenerator(method, null);
    }
    value = definition.value.visit(context);
    value = value.convertTo(context, type, definition.value);
  }

  Parameter copyWithNewType(Type newType) {
    var ret = new Parameter(definition);
    ret.type = newType;
    ret.name = name;
    return ret;
  }

  bool get isOptional() => definition != null && definition.value != null;
}


interface Named {
  String get name();
  Library get library();
  bool get isNative();
  String get jsname();
  set jsname(String name);
}

class Member implements Named {
  final String name;
  final Type declaringType;

  String _jsname;

  bool isGenerated;
  MethodGenerator generator;

  Member(this.name, this.declaringType): isGenerated = false;

  abstract SourceSpan get span();

  abstract bool get isStatic();
  abstract Type get returnType();

  abstract bool get canGet();
  abstract bool get canSet();

  abstract void resolve(Type inType);

  String get jsname() => _jsname == null ? name : _jsname;

  set jsname(String name) => _jsname = name;

  Library get library() => declaringType.library;

  bool get isPrivate() => name.startsWith('_');

  bool get isConstructor() => false;
  bool get isField() => false;
  bool get isMethod() => false;
  bool get isProperty() => false;
  bool get isAbstract() => false;

  bool get prefersPropertySyntax() => true;
  bool get requiresFieldSyntax() => false;

  bool get isNative() => false;
  String get constructorName() =>
      world.internalError('can not be a constructor', span);

  void provideFieldSyntax() => world.internalError('can not be field', span);
  void providePropertySyntax() =>
    world.internalError('can not be property', span);

  Definition get definition() => null;

  List<Parameter> get parameters() => [];

  // TODO(jimhug): Fix these names once get/set are truly pseudo-keywords.
  // TODO(jmesserly): isDynamic isn't a great name for this, something better?
  abstract Value get_(MethodGenerator context, Node node, Value target,
      [bool isDynamic]);

  abstract Value set_(MethodGenerator context, Node node, Value target,
      Value value, [bool isDynamic]);

  bool canInvoke(MethodGenerator context, Arguments args) {
    return canGet && new Value(returnType, null).canInvoke(context, '\$call', args);
  }

  Value invoke(MethodGenerator context, Node node, Value target, Arguments args,
      [bool isDynamic=false]) {
    var newTarget = get_(context, node, target, isDynamic);
    return newTarget.invoke(context, '\$call', node, args, isDynamic);
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
  Type get returnType() => world.isVar;

  bool canInvoke(MethodGenerator context, Arguments args) => false;
  bool get canGet() => true;
  bool get canSet() => false;

  void resolve(Type inType) {}

  Value get_(MethodGenerator context, Node node, Value target,
      [bool isDynamic=false]) {
    assert(target == null || target.type.isTop);
    // TODO(jmesserly): named args
    return new Value(type, type.jsname, false, false, true);
  }

  Value set_(MethodGenerator context, Node node, Value target, Value value,
      [bool isDynamic=false]) {
    world.error('can not set type', type.definition.span);
  }

  Value invoke(MethodGenerator context, Node node, Value target, Arguments args,
      [bool isDynamic=false]) {
    world.error('can not invoke type', type.definition.span);
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

  bool _providePropertySyntax = false;

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

  bool get prefersPropertySyntax() => false;
  bool get requiresFieldSyntax() => isNative;

  void provideFieldSyntax() {} // Nothing to do.
  void providePropertySyntax() => _providePropertySyntax = true;

  FieldMember(String name, Type declaringType, this.definition, this.value)
      : super(name, declaringType), isNative = false;

  SourceSpan get span() => definition == null ? null : definition.span;

  Type get returnType() => type;

  bool get canGet() => true;
  bool get canSet() => !isFinal;

  bool get isField() => true;

  resolve(Type inType) {
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
    type = inType.resolveType(definition.type, false);
    if (isStatic && type.hasTypeParams) {
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
      var finalMethod =
        new MethodMember('final_context', declaringType, null);
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
        _computedValue = world.gen.globalForStaticField(
            this, _computedValue, [_computedValue]);
      }
      _computing = false;
    }
    return _computedValue;
  }

  Value get_(MethodGenerator context, Node node, Value target,
      [bool isDynamic=false]) {
    if (!isDynamic) {
      declaringType.markUsed();
    }
    if (isStatic) {
      // Make sure to compute the value of all static fields, even if we don't
      // use this value immediately.
      var cv = computeValue();
      if (isFinal) {
        return cv;
      }
      if (declaringType.isTop) {
        return new Value(type, '$jsname');
      } else {
        return new Value(type, '${declaringType.jsname}.$jsname');
      }
    } else if (target.isConst && isFinal) {
      // take advantage of consts and retrieve the value directly if possible
      var constTarget = target is GlobalValue ? target.exp : target;
      if (constTarget is ConstObjectValue) {
        return constTarget.fields[name];
      } else if (constTarget.type == world.stringType && name == 'length') {
        return new Value(type, '${constTarget.actualValue.length}');
      }
    }
    return new Value(type, '${target.code}.$jsname');
  }

  Value set_(MethodGenerator context, Node node, Value target, Value value,
      [bool isDynamic=false]) {
    var lhs = get_(context, node, target, isDynamic);
    value = value.convertTo(context, type, node, isDynamic);
    return new Value(type, '${lhs.code} = ${value.code}');
  }
}

class PropertyMember extends Member {
  MethodMember getter;
  MethodMember setter;

  bool _provideFieldSyntax = false;

  // TODO(jimhug): What is the right span for this beast?
  SourceSpan get span() => getter != null ? getter.span : null;

  bool get canGet() => getter != null;
  bool get canSet() => setter != null;

  bool get prefersPropertySyntax() => true;
  bool get requiresFieldSyntax() => false;

  void provideFieldSyntax() => _provideFieldSyntax = true;
  void providePropertySyntax() {}// Nothing to do.

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
      return true;
    } else {
      world.error('property can only override field or property',
        span, other.span);
      return false;
    }
  }

  Value get_(MethodGenerator context, Node node, Value target,
      [bool isDynamic=false]) {
    if (getter == null) {
      return target.invokeNoSuchMethod(context, 'get:$name', node);
    }
    return getter.invoke(context, node, target, Arguments.EMPTY);
  }

  Value set_(MethodGenerator context, Node node, Value target, Value value,
      [bool isDynamic=false]) {
    return setter.invoke(context, node, target, new Arguments(null, [value]),
      isDynamic);
  }

  addFromParent(Member parentMember) {
    // TODO(jimhug): Egregious Hack!
    if (parentMember is ConcreteMember) {
      parentMember = parentMember.baseMember;
    }

    if (getter == null) getter = parentMember.getter;
    if (setter == null) setter = parentMember.setter;
  }

  resolve(Type inType) {
    if (getter != null) getter.resolve(inType);
    if (setter != null) setter.resolve(inType);

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
    // TODO(jimhug): Optimize not creating new array if new param types.
    for (var p in baseMember.parameters) {
      var newType = p.type.resolveTypeParams(declaringType);
      if (newType != p.type) {
        parameters.add(p.copyWithNewType(newType));
      } else {
        parameters.add(p);
      }
    }
  }

  SourceSpan get span() => baseMember.span;

  bool get isStatic() => baseMember.isStatic;
  bool get isAbstract() => baseMember.isAbstract;
  bool get isConst() => baseMember.isConst;

  String get jsname() => baseMember.jsname;
  set jsname(String name) =>
    world.internalError('bad set of jsname on ConcreteMember');

  bool get isFactory() => baseMember.isFactory;

  bool get canGet() => baseMember.canGet;
  bool get canSet() => baseMember.canSet;
  bool canInvoke(MethodGenerator context, Arguments args) =>
    baseMember.canInvoke(context, args);

  bool get isField() => baseMember.isField;
  bool get isMethod() => baseMember.isMethod;
  bool get isProperty() => baseMember.isProperty;

  bool get prefersPropertySyntax() => baseMember.prefersPropertySyntax;
  bool get requiresFieldSyntax() => baseMember.requiresFieldSyntax;

  void provideFieldSyntax() => baseMember.provideFieldSyntax();
  void providePropertySyntax() => baseMember.providePropertySyntax();

  bool get isConstructor() => name == declaringType.name;

  String get constructorName() => baseMember.constructorName;

  Definition get definition() => baseMember.definition;

  // TODO(sigmund): this is EGREGIOUS
  Definition get initDelegate() => baseMember.initDelegate;
  Definition set initDelegate(ctor) { baseMember.initDelegate = ctor; }

  Type resolveType(TypeReference node, bool isRequired) {
    var type = baseMember.resolveType(node, isRequired);
    return type.resolveTypeParams(declaringType);
  }

  // TODO(jimhug): Add support for type params.
  bool override(Member other) => baseMember.override(other);

  Value get_(MethodGenerator context, Node node, Value target,
      [bool isDynamic=false]) {
    Value ret = baseMember.get_(context, node, target, isDynamic);
    return new Value(returnType, ret.code);
  }

  Value set_(MethodGenerator context, Node node, Value target, Value value,
      [bool isDynamic=false]) {
    // TODO(jimhug): Check arg types in context of concrete type.
    Value ret = baseMember.set_(context, node, target, value, isDynamic);
    return new Value(returnType, ret.code);
  }

  Value invoke(MethodGenerator context, Node node, Value target, Arguments args,
      [bool isDynamic=false]) {
    // TODO(jimhug): Check arg types in context of concrete type.
    Value ret = baseMember.invoke(context, node, target, args, isDynamic);
    var code = ret.code;
    if (isConstructor) {
      // TODO(jimhug): Egregious hack - won't live through the weekend.
      code = code.replaceFirst(
          declaringType.genericType.jsname, declaringType.jsname);
    }
    declaringType.genMethod(this);
    return new Value(returnType, code);
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

  bool _providePropertySyntax = false;
  bool _provideFieldSyntax = false;

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

  bool get isNative() => definition.body is NativeStatement;

  bool get canGet() => false; // TODO(jimhug): get bound method support.
  bool get canSet() => false;

  SourceSpan get span() => definition == null ? null : definition.span;

  String get constructorName() {
    if (definition.returnType == null) return '';

    // TODO(jmesserly): make this easier?
    if (definition.returnType.names != null) {
      return definition.returnType.names[0].name;
    } else if (definition.returnType.name != null) {
      return definition.returnType.name.name;
    }
    world.internalError('no valid constructor name', definition.span);
  }

  Type get functionType() {
    if (_functionType == null) {
      _functionType = library.getOrAddFunctionType(name,
        definition, declaringType);
      // TODO(jimhug): Better resolution checks.
      if (parameters == null) {
        resolve(declaringType);
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

  Type resolveType(TypeReference node, bool isRequired) {
    var type = declaringType.resolveType(node, isRequired);
    if (isStatic && type.hasTypeParams) {
      // TODO(jimhug): Is this really so hard?
      world.error('using type parameter in static context',
        node.span);
    }
    return type;
  }

  bool get prefersPropertySyntax() => true;
  bool get requiresFieldSyntax() => false;

  void provideFieldSyntax() => _provideFieldSyntax = true;
  void providePropertySyntax() => _providePropertySyntax = true;

  Value set_(MethodGenerator context, Node, Value target, Value value,
      [bool isDynamic=false]) {
    world.error('can not set method', definition.span);
  }

  Value get_(MethodGenerator context, Node node, Value target,
      [bool isDynamic=false]) {
    // TODO(jimhug): Would prefer to invoke!
    declaringType.genMethod(this);
    _provideOptionalParamInfo = true;
    if (isStatic) {
      var type = declaringType.isTop ? '' : '${declaringType.jsname}.';
      return new Value(functionType, '$type$jsname');
    }
    _providePropertySyntax = true;
    return new Value(functionType, '${target.code}.get\$$jsname()');
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
        return false;
      }
    }

    if (bareCount < parameters.length) {
      genParameterValues();
      for (int i = bareCount; i < parameters.length; i++) {
        var arg = args.getValue(parameters[i].name);
        if (arg != null && arg.needsConversion(parameters[i].type)) {
          return false;
        }
      }
    }

    return true;
  }

  static String _argCountMsg(int actual, int expected, [bool atLeast=false]) {
    // TODO(jimhug): better messages with default named args.
    return 'wrong number of arguments, expected ' +
        '${atLeast ? "at least " : ""}$expected but found $actual';
  }

  Value _argError(MethodGenerator context, Node node, Value target,
      Arguments args, String msg) {
    if (isStatic || isConstructor) {
      world.error(msg, node.span);
    } else {
      world.warning(msg, node.span);
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
      this.resolve(declaringType);
    }

    declaringType.genMethod(this);

    if (isStatic || isFactory) {
      declaringType.markUsed();
    }

    if (!namesInOrder(args)) {
      // Names aren't in order. For now, use a var call because it's an
      // easy way to get the right eval order for out of order arguments.
      return context.findMembers(name).invokeOnVar(context, node, target, args);
    }

    var argsCode = [];
    if (target != null && (isConstructor || target.isSuper)) {
      argsCode.add('this');
    }

    int bareCount = args.bareCount;
    for (int i = 0; i < bareCount; i++) {
      var arg = args.values[i];
      if (i >= parameters.length) {
        // TODO(jimhug): better error location
        var msg = _argCountMsg(args.length, parameters.length);
        return _argError(context, node, target, args, msg);
      }
      arg = arg.convertTo(context, parameters[i].type, node, isDynamic);
      if (isConst && arg.isConst) {
        argsCode.add(arg.canonicalCode);
      } else {
        argsCode.add(arg.code);
      }
    }

    if (bareCount < parameters.length) {
      genParameterValues();

      int namedArgsUsed = 0;
      for (int i = bareCount; i < parameters.length; i++) {
        var arg = args.getValue(parameters[i].name);
        if (arg == null) {
          arg = parameters[i].value;
        } else {
          arg = arg.convertTo(context, parameters[i].type, node, isDynamic);
          namedArgsUsed++;
        }

        if (arg == null || !parameters[i].isOptional) {
          // TODO(jimhug): better error location
          var msg = _argCountMsg(Math.min(i, args.length), i + 1, atLeast:true);
          return _argError(context, node, target, args, msg);
        } else {
          argsCode.add(isConst && arg.isConst
              ? arg.canonicalCode : arg.code);
        }
      }

      if (namedArgsUsed < args.nameCount) {
        // TODO(jmesserly): better error location
        // Find the unused argument name
        var seen = new Set<String>();
        for (int i = bareCount; i < args.length; i++) {
          var name = args.getName(i);
          if (seen.contains(name)) {
            return _argError(context, node, target, args,
                'duplicate argument "$name"');
          }
          seen.add(name);
          int p = indexOfParameter(name);
          if (p < 0) {
            return _argError(context, node, target, args,
                'method does not have optional parameter "$name"');
          } else if (p < bareCount) {
            return _argError(context, node, target, args,
                'argument "$name" passed as positional and named');
          }
        }
        world.internalError('wrong named arguments calling $name', node.span);
      }

      Arguments.removeTrailingNulls(argsCode);
    }

    var argsString = Strings.join(argsCode, ', ');

    if (isConstructor) {
      return _invokeConstructor(context, node, target, args, argsString);
    }

    if (name.startsWith('\$')) {
      return _invokeBuiltin(context, node, target, args, argsCode);
    }

    // TODO(jmesserly): can target ever be null in super call?
    if (target != null && target.isSuper) {
      return new Value(returnType,
          '${declaringType.jsname}.prototype.$jsname.call($argsString)');
    }

    if (isFactory) {
      return new Value(returnType, '$generatedFactoryName($argsString)');
    }

    if (isStatic) {
      if (declaringType.isTop) {
        // TODO(jimhug): Explore moving libraries into their own namespaces
        return new Value(returnType, '$jsname($argsString)');
      }
      return new Value(returnType,
        '${declaringType.jsname}.$jsname($argsString)');
    }

    var code = '${target.code}.$jsname($argsString)';
    // optimize expressions which we know statically their value.
    if (target.isConst) {
      if (target is GlobalValue) {
        target = target.exp;
      }
      if (name == 'get\$length') {
        if (target is ConstListValue || target is ConstMapValue) {
          code = '${target.values.length}';
        }
      } else if (name == 'isEmpty') {
        if (target is ConstListValue || target is ConstMapValue) {
          code = '${target.values.isEmpty()}';
        }
      }
    }

    return new Value(returnType, code);
  }

  Value _invokeConstructor(MethodGenerator context, Node node, Value target,
      Arguments args, argsString) {
    declaringType.markUsed();

    if (target != null) {
      // initializer call to another constructor
      var code = (constructorName != '')
          ? '${declaringType.jsname}.${constructorName}\$ctor.call($argsString)'
          : '${declaringType.jsname}.call($argsString)';
      return new Value(declaringType, code);
    } else {
      var code = (constructorName != '')
          ? 'new ${declaringType.jsname}.${constructorName}\$ctor($argsString)'
          : 'new ${declaringType.jsname}($argsString)';
      if (isConst && node.isConst) {
        return _invokeConstConstructor(node, code, target, args);
      } else {
        return new Value(declaringType, code);
      }
    }
  }

  /**
   * Special handling for const constructors so that so that:
   * [: const B() === const B.a(0, 1) === const B.b(0) :]
   * where: [:
   *   class A {
   *     final int x;
   *     const A(this.x);
   *   }
   *
   *   class B {
   *     final int y;
   *     const B() : y = 0, super(1);
   *     const B.a(this.y, x) : super(x);
   *     const B.b(v) : this.a(v, 1);
   *   }
   * :]
   */
  Value _invokeConstConstructor(
      Node node, String code, Value target, Arguments args) {
    // Statically compute the actual value for every field in the const object.
    final fields = new Map<String, EvaluatedValue>();

    // First deduce the value for fields initialized with the 'this.x' syntax.
    for (int i = 0; i < parameters.length; i++) {
      var param = parameters[i].name;
      if (param.startsWith('this.')) {
        final fname = param.substring(5);
        var value = null;
        if (i < args.length) {
          value = args.values[i];
        } else { // named arguments
          value = args.getValue(parameters[i].name);
          if (value == null) {
            value = parameters[i].value;
          }
        }
        fields[fname] = value;
      }
    }

    // Then evaluate initializer expressions.
    if (definition.initializers != null) {
      // Introduce a temporary scope to evaluate initializers, which defines the
      // value for any formal argument as it's constant expression value.
      generator._pushBlock();
      for (int j = 0; j < definition.formals.length; j++) {
        var name = definition.formals[j].name.name;
        var value = null;
        if (j < args.length) {
          value = args.values[j];
        } else { // named arguments
          value = args.getValue(parameters[j].name);
          if (value == null) {
            value = parameters[j].value;
          }
        }
        generator._scope._vars[name] = value;
      }

      for (var init in definition.initializers) {
        if (init is CallExpression) {
          // Construct arguments to delegate and invoke it.
          var delegateArgs = generator._makeArgs(init.arguments);
          var value = initDelegate.invoke(
              generator, node, target, delegateArgs);
          if (init.target is ThisExpression) {
            // Redirection: Use directly the delegate result. E.g. so that
            //   const B.b(0) === const B.a(0, 1)
            return value;
          } else {
            // Super-call: embed the value the super class fields.
            if (value is GlobalValue) {
              value = value.exp;
            }
            for (var fname in value.fields.getKeys()) {
              fields[fname] = value.fields[fname];
            }
          }
        } else {
          // Normal field initializer assignment.
          var fname = init.x.name.name;
          var val = generator.visitValue(init.y);
          fields[fname] = val;
        }
      }

      generator._popBlock();
    }

    // Add default values only if they weren't overriden in the constructor.
    for (var f in declaringType.members.getValues()) {
      if (f is FieldMember && !f.isStatic && f.value != null
          && !fields.containsKey(f.name)) {
        fields[f.name] = f.computeValue();
      }
    }

    return world.gen.globalForConst(
        new ConstObjectValue(declaringType, fields, code, node.span),
        args.values);
  }


  Value _invokeBuiltin(MethodGenerator context, Node node, Value target,
      Arguments args, argsCode) {
    var allConst = target.isConst && args.values.every((arg) => arg.isConst);
    // TODO(jimhug): Handle super calls on special methods.
    // Handle some fast paths for Number, String, List and DOM.
    if (declaringType.isNum) {
      // TODO(jimhug): This fails in bad ways when argsCode[1] is not num.
      // TODO(jimhug): What about null?
      if (!allConst) {
        var code;
        if (name == '\$negate') {
          code = '-${target.code}';
        } else if (name == '\$bit_not') {
          code = '~${target.code}';
        } else if (name == '\$truncdiv') {
          code = '$name(${target.code}, ${argsCode[0]})';
        } else if (name == '\$mod') {
          code = '$name(${target.code}, ${argsCode[0]})';
        } else {
          var op = TokenKind.rawOperatorFromMethod(name);
          code = '${target.code} $op ${argsCode[0]}';
        }

        return new Value(returnType, code);
      } else {
        var value;
        num val0, val1, ival0, ival1;
        val0 = target.dynamic.actualValue;
        ival0 = val0.toInt();
        if (args.values.length > 0) {
          val1 = args.values[0].dynamic.actualValue;
          ival1 = val1.toInt();
        }
        switch (name) {
          case '\$negate': value = -val0; break;
          case '\$add': value = val0 + val1; break;
          case '\$sub': value = val0 - val1; break;
          case '\$mul': value = val0 * val1; break;
          case '\$div': value = val0 / val1; break;
          case '\$truncdiv': value = val0 ~/ val1; break;
          case '\$mod': value = val0 % val1; break;
          case '\$eq': value = val0 == val1; break;
          case '\$lt': value = val0 < val1; break;
          case '\$gt': value = val0 > val1; break;
          case '\$lte': value = val0 <= val1; break;
          case '\$gte': value = val0 >= val1; break;
          case '\$ne': value = val0 != val1; break;

          // Note: unfortunatelly bit operations fail on doubles in dartvm
          case '\$bit_not': value = (~ival0).toDouble(); break;
          case '\$bit_or': value = (ival0 | ival1).toDouble(); break;
          case '\$bit_xor': value = (ival0 ^ ival1).toDouble(); break;
          case '\$bit_and': value = (ival0 & ival1).toDouble(); break;
          case '\$shl': value = (ival0 << ival1).toDouble(); break;
          case '\$sar': value = (ival0 >> ival1).toDouble(); break;
          case '\$shr': value = (ival0 >>> ival1).toDouble(); break;
        }
        return new EvaluatedValue(returnType, value, "$value", node.span);
      }
    } else if (declaringType.isString) {
      if (name == '\$index') {
        // Note: this could technically propagate constness, but that's not
        // specified explicitly and the VM doesn't do that.
        return new Value(declaringType, '${target.code}[${argsCode[0]}]');
      } else if (name == '\$add') {
        if (allConst) {
          var val0 = target.dynamic.actualValue;
          val0 = val0.substring(1, val0.length - 1);
          var val1 = args.values[0].dynamic.actualValue;
          if (args.values[0].type.isString) {
            val1 = val1.substring(1, val1.length - 1);
          }
          var value = '${val0}${val1}';
          value = '"' + value.replaceAll('"', '\\"') + '"';
          return new EvaluatedValue(world.stringType, value, value, node.span);
        }

        // Ensure we generate toString on the right side
        args.values[0].invoke(context, 'toString', node, Arguments.EMPTY);
        return new Value(declaringType, '${target.code} + ${argsCode[0]}');
      }
    } else if (declaringType.isNativeType) {
      if (name == '\$index') {
        // Note: this could technically propagate constness, but that's not
        // specified explicitly and the VM doesn't do that.
        return new Value(null, '${target.code}[${argsCode[0]}]');
      } else if (name == '\$setindex') {
        return new Value(null,
          '${target.code}[${argsCode[0]}] = ${argsCode[1]}');
      }
    }

    // TODO(jimhug): Optimize null on lhs as well.
    if (name == '\$eq' || name == '\$ne') {
      final op = name == '\$eq' ? '==' : '!=';
      if (allConst) {
        var val0 = target.dynamic.actualValue;
        var val1 = args.values[0].dynamic.actualValue;
        var newVal = name == '\$eq' ? val0 == val1 : val0 != val1;
        return new EvaluatedValue(world.boolType,
            newVal, "$newVal", node.span);
      }
      // Optimize test when null is on the rhs.
      if (argsCode[0] == 'null') {
        return new Value(returnType, '${target.code} $op null');
      } else if (target.type.isNum || target.type.isString) {
        // TODO(jimhug): Maybe check rhs.
        return new Value(returnType, '${target.code} $op ${argsCode[0]}');
      }
      return new Value(returnType,
        '$name(${target.code}, ${argsCode[0]})');
    }

    if (name == '\$call') {
      declaringType.markUsed();
      return new Value(returnType,
        '${target.code}(${Strings.join(argsCode, ", ")})');
    }

    return target.invokeSpecial(jsname, args, returnType);
  }


  resolve(Type inType) {
    // TODO(jimhug): cut-and-paste-and-edit from Field.resolve
    isStatic = inType.isTop;
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
          isConst = true;
        } else if (mod.kind == TokenKind.FACTORY) {
          if (isFactory) {
            world.error('duplicate factory modifier', mod.span);
          }
          isFactory = true;
        } else if (mod.kind == TokenKind.ABSTRACT) {
          if (isAbstract) {
            if (declaringType.isClass) {
              world.error('duplicate abstract modifier', mod.span);
            } else {
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

    if (isAbstract) {
      if (definition.body != null &&
          declaringType.definition is! FunctionTypeDefinition) {
        // TODO(jimhug): Creating function types for concrete methods is
        //   steadily feeling uglier...
        world.error('abstract method can not have a body',
          definition.body.span);
      }
      if (isStatic &&
          declaringType.definition is! FunctionTypeDefinition) {
        world.error('static method can not be abstract', definition.span);
      }
    } else {
      if (definition.body == null && !isConstructor) {
        world.error('method needs a body', span);
      }
    }

    if (isConstructor) {
      returnType = declaringType;
    } else {
      // TODO(jimhug): Unify this check and the below with method's
      //   resolveType method - requires cleaning up inType stuff.
      returnType = inType.resolveType(definition.returnType, false);

      if (isStatic && returnType.hasTypeParams) {
        world.error('using type parameter in static context',
          definition.returnType.span);
      }
    }
    parameters = [];
    for (var formal in definition.formals) {
      var param = new Parameter(formal);
      param.resolve(this, inType);
      parameters.add(param);
    }

    if (!isLambda) {
      library._addMember(this);
    }
  }
}


class MemberSet {
  final String name;
  final List<Member> members;
  final String jsname;

  MemberSet(Member member):
    name = member.name, members = [member], jsname = member.jsname;

  toString() => '$name:${members.length}';

  // TODO(jimhug): Still working towards the right logic for conflicts...
  bool get containsProperties() => members.some((m) => m is PropertyMember);
  bool get containsMethods() => members.some((m) => m is MethodMember);

  void add(Member member) => members.add(member);

  // TODO(jimhug): Always false, or is this needed?
  bool get isStatic() => members.length == 1 && members[0].isStatic;

  bool canInvoke(MethodGenerator context, Arguments args) =>
    members.some((m) => m.canInvoke(context, args));

  Value _makeError(Node node, Value target, String action) {
    if (!target.type.isVar) {
      world.warning('could not find applicable $action for "$name"', node.span);
    }
    return new Value(null, '${target.code}.$jsname() /*no applicable $action*/');
  }

  bool _treatAsField;
  bool get treatAsField() {
    if (_treatAsField == null) {
      _treatAsField = true;
      for (var member in members) {
        if (member.requiresFieldSyntax) {
          _treatAsField = true;
          break;
        }
        if (member.prefersPropertySyntax) {
          _treatAsField = false;
        }
      }
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

  Value get_(MethodGenerator context, Node node, Value target,
      [bool isDynamic=false]) {
    if (members.length == 1) {
      return members[0].get_(context, node, target, isDynamic);
    }
    final targets = members.filter((m) => m.canGet);
    if (targets.length == 1) {
      return targets[0].get_(context, node, target, isDynamic);
    }

    Value returnValue = null;
    for (var member in targets) {
      final value = member.get_(context, node, target, isDynamic:true);
      returnValue = _tryUnion(returnValue, value, node);
    }
    if (returnValue == null) {
      return _makeError(node, target, 'getter');
    }
    if (returnValue.code == null) {
      if (treatAsField) {
        return new Value(returnValue.type, '${target.code}.$jsname');
      } else {
        return new Value(returnValue.type, '${target.code}.get\$$jsname()');
      }
    }
    return returnValue;
  }

  Value set_(MethodGenerator context, Node node, Value target, Value value,
      [bool isDynamic=false]) {
    if (members.length == 1) {
      return members[0].set_(context, node, target, value, isDynamic);
    }
    final targets = members.filter((m) => m.canSet);
    if (targets.length == 1) {
      return targets[0].set_(context, node, target, value, isDynamic);
    }

    Value returnValue = null;
    for (var member in targets) {
      final res = member.set_(context, node, target, value, isDynamic:true);
      returnValue = _tryUnion(returnValue, res, node);
    }
    if (returnValue == null) {
      return _makeError(node, target, 'setter');
    }
    if (returnValue.code == null) {
      if (treatAsField) {
        return new Value(returnValue.type,
          '${target.code}.$jsname = ${value.code}');
      } else {
        return new Value(returnValue.type,
          '${target.code}.set\$$jsname(${value.code})');
      }
    }
    return returnValue;
  }

  Value invoke(MethodGenerator context, Node node, Value target,
      Arguments args, [bool isDynamic=false]) {
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
      returnValue = _tryUnion(returnValue, res, node);
    }

    if (returnValue == null) {
      return _makeError(node, target, 'method');
    }

    // If we fail to unify the resulting code, implement as a var call.
    if (returnValue.code == null) {
      if (name.startsWith('\$')) {
        return target.invokeSpecial(name, args, returnValue.type);
      } else {
        return invokeOnVar(context, node, target, args);
      }
    }

    return returnValue;
  }

  Value invokeOnVar(MethodGenerator context, Node node, Value target,
      Arguments args) {
    return getVarMember(context, node, args).invoke(context, node, target, args);
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
        return new Value(type, x.code,
            x.isSuper && y.isSuper,
            x.needsTemp || y.needsTemp,
            x.isType && y.isType);
      }
    } else {
      return new Value(type, null);
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
      final returnType = reduce(map(targets, (t) => t.returnType), Type.union);
      stub = new VarMethodSet(stubName, targets, args, returnType);
      world.objectType.varStubs[stubName] = stub;
    }
    return stub;
  }
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
