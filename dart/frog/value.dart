// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Represents a meta-value for code generation.
 */
class Value {
  Type _type;

  /** The javascript code to generate this value. */
  String code;

  /** The source location that created this value for error messages. */
  SourceSpan span;

  /** Is this a reference to super? */
  bool isSuper = false;

  /** Is this a pretend first-class type? */
  bool isType = false;

  /** Is this a final variable? */
  bool isFinal = false;

  /**
   * May value be forced to varType by the forceDynamic option? Note that
   * [isSuper], [isType], and [isConst] imply `!allowDynamic`.
   */
  bool allowDynamic = true;

  /** If we reference this value multiple times, do we need a temp? */
  bool needsTemp;

  // TODO(jmesserly): until reified generics are fixed, treat ParameterType as
  // "var".
  bool get _typeIsVarOrParameterType() => type.isVar || type is ParameterType;

  /** The [Type] of the [Value]. */
  Type get type() {
    if (!options.forceDynamic || !allowDynamic || isType || isSuper ||
        isConst) {
      return _type;
    } else {
      return world.varType;
    }
  }

  Value(this._type, this.code, this.span, [this.needsTemp = true]) {
    if (_type == null) world.internalError('type passed as null', span);
  }

  // TODO(jimhug): Replace with TypeValue.
  Value.type(this._type, this.span)
    : code = null, needsTemp = false, isType = true {
    if (_type == null) world.internalError('type passed as null', span);
  }

  Value union(Value other) {
    // TODO(jimhug): What the hell is right here?
    if (this == other) return this;
    world.internalError('not yet ready for union');
  }

  /** Is this value a constant expression? */
  bool get isConst() => false;

  /** If [isConst], the [EvaluatedValue] that defines this value. */
  EvaluatedValue get constValue() => null;

  // TODO(jimhug): remove once type system works better.
  setField(Member field, Value value, [bool duringInit = false]) { }

  // Nothing to do in general?
  validateInitialized(SourceSpan span) { }

  // TODO(jimhug): Fix these names once get/set are truly pseudo-keywords.
  //   See issue #379.
  Value get_(MethodGenerator context, String name, Node node) {
    final member = _resolveMember(context, name, node);
    if (member != null) {
      return member._get(context, node, this);
    } else {
      return invokeNoSuchMethod(context, 'get:$name', node);
    }
  }

  Value set_(MethodGenerator context, String name, Node node, Value value,
      [bool isDynamic=false]) {

    final member = _resolveMember(context, name, node, isDynamic);
    if (member != null) {
      return member._set(context, node, this, value, isDynamic);
    } else {
      return invokeNoSuchMethod(context, 'set:$name', node,
          new Arguments(null, [value]));
    }
  }

  Value unop(int kind, MethodGenerator context, var node) {
    switch (kind) {
      case TokenKind.NOT:
        // TODO(jimhug): Issue #359 seeks to clarify this behavior.
        var newVal = convertTo(context, world.nonNullBool);
        return new Value(newVal.type, '!${newVal.code}', node.span);
      case TokenKind.ADD:
        world.error('no unary add operator in dart', node.span);
        break;
      case TokenKind.SUB:
        return invoke(context, ':negate', node, Arguments.EMPTY);
      case TokenKind.BIT_NOT:
        return invoke(context, ':bit_not', node, Arguments.EMPTY);
    }
    world.internalError('unimplemented: ${node.op}', node.span);
  }

  Value binop(int kind, Value other, MethodGenerator context, var node) {
    switch (kind) {
      case TokenKind.AND:
      case TokenKind.OR:
        final code = '${code} ${node.op} ${other.code}';
        return new Value(world.nonNullBool, code, node.span);
      // TODO(jimhug): Lot's to resolve here.
      case TokenKind.EQ_STRICT:
        return new Value(world.nonNullBool, '${code} == ${other.code}',
          node.span);
      case TokenKind.NE_STRICT:
        return new Value(world.nonNullBool, '${code} != ${other.code}',
          node.span);
    }

    var name = kind == TokenKind.NE ? ':ne': TokenKind.binaryMethodName(kind);
    return invoke(context, name, node, new Arguments(null, [other]));
  }


  Value invoke(MethodGenerator context, String name, Node node, Arguments args,
      [bool isDynamic=false]) {

    // TODO(jmesserly): it'd be nice to remove these special cases
    // We could create a :call in world members, and have that handle the
    // canInvoke/Invoke logic.

    // Note: this check is a little different than the one in canInvoke, because
    // sometimes we need to call dynamically even if we found the :call method
    // statically.

    if (name == ':call') {
      if (isType) {
        world.error('must use "new" or "const" to construct a new instance',
            node.span);
      }
      if (type.needsVarCall(args)) {
        return _varCall(context, node, args);
      }
    }

    var member = _resolveMember(context, name, node, isDynamic);
    if (member == null) {
      return invokeNoSuchMethod(context, name, node, args);
    } else {
      return member.invoke(context, node, this, args, isDynamic);
    }
  }

  bool canInvoke(MethodGenerator context, String name, Arguments args) {
    if (type.isVarOrFunction && name == ':call') {
      return true;
    }

    var member = _resolveMember(context, name, null, isDynamic:true);
    return member != null && member.canInvoke(context, args);
  }

  /**
   * True if this class (or some related class that is not Object) overrides
   * noSuchMethod. If it does we suppress warnings about unknown members.
   */
  // TODO(jmesserly): should we be doing this?
  bool _hasOverriddenNoSuchMethod() {
    if (isSuper) {
      var m = type.getMember('noSuchMethod');
      return m != null && !m.declaringType.isObject;
    } else {
      var m = type.resolveMember('noSuchMethod');
      return m != null && m.members.length > 1;
    }
  }

  _tryResolveMember(MethodGenerator context, String name) {
    if (isSuper) {
      return type.getMember(name);
    } else {
      return type.resolveMember(name);
    }
  }

  // TODO(jimhug): Better type here - currently is union(Member, MemberSet)
  _resolveMember(MethodGenerator context, String name, Node node,
        [bool isDynamic=false]) {

    var member;
    if (!_typeIsVarOrParameterType) {
      member = _tryResolveMember(context, name);

      if (member != null && isType && !member.isStatic) {
        if (!isDynamic) {
          world.error('can not refer to instance member as static', node.span);
        }
        return null;
      }

      if (member == null && !isDynamic && !_hasOverriddenNoSuchMethod()) {
        var typeName = type.name == null ? type.library.name : type.name;
        var message = 'can not resolve "$name" on "${typeName}"';
        if (isType) {
          world.error(message, node.span);
        } else {
          world.warning(message, node.span);
        }
      }
    }

    // Fall back to a dynamic operation for instance members
    if (member == null && !isSuper && !isType) {
      member = context.findMembers(name);
      if (member == null && !isDynamic) {
        var where = 'the world';
        if (name.startsWith('_')) {
          where = 'library "${context.library.name}"';
        }
        world.warning('$name is not defined anywhere in $where.',
           node.span);
      }
    }

    return member;
  }

  checkFirstClass(SourceSpan span) {
    if (isType) {
      world.error('Types are not first class', span);
    }
  }

  /** Generate a call to an unknown function type. */
  Value _varCall(MethodGenerator context, Node node, Arguments args) {
    // TODO(jmesserly): calls to unknown functions will bypass type checks,
    // which normally happen on the caller side, or in the generated stub for
    // dynamic method calls. What should we do?
    var stub = world.functionType.getCallStub(args);
    return stub.invoke(context, node, this, args);
  }

  /** True if convertTo would generate a conversion. */
  bool needsConversion(Type toType) {
    return this != convertTo(null, toType, isDynamic:true);
  }

  /**
   * Assign or convert this value to another type.
   * This is used for converting between function types, inserting type
   * checks when --enable_type_checks is enabled, and wrapping callback
   * functions passed to the dom so we can restore their isolate context.
   */
  Value convertTo(MethodGenerator context, Type toType,
      [bool isDynamic=false]) {

    // Issue type warnings unless we are processing a dynamic operation.
    bool checked = !isDynamic;

    var callMethod = toType.getCallMethod();
    if (callMethod != null) {
      if (checked && !toType.isAssignable(type)) {
        convertWarning(toType);
      }

      return _maybeWrapFunction(toType, callMethod);
    }

    // If we're assigning from a var, pretend it's Object for the purpose of
    // runtime checks.

    // TODO(jmesserly): I'm a little bothered by the fact that we can't call
    // isSubtypeOf directly. If we tracked null literals as the bottom type,
    // and then only allowed Dynamic to be bottom for generic type args, I think
    // we'd get the right behavior from isSubtypeOf.
    Type fromType = type;
    if (type.isVar && (code != 'null' || !toType.isNullable)) {
      fromType = world.objectType;
    }

    // TODO(jmesserly): remove the special case for "num" when our num handling
    // is better.
    bool bothNum = type.isNum && toType.isNum;
    if (fromType.isSubtypeOf(toType) || bothNum) {
      // No checks needed for a widening conversion.
      return this;
    }

    if (checked && !toType.isSubtypeOf(type)) {
      // According to the static types, this conversion can't work.
      convertWarning(toType);
    }

    // Generate a runtime checks if they're turned on, otherwise skip it.
    if (options.enableTypeChecks) {
      if (context == null && isDynamic) {
        // If we're just testing if we need the conversion, not actually doing
        // it we don't need a context. Just return something that is != this.
        // TODO(jmesserly): I don't like using null in this fashion, but it's
        // better than before where we had two code paths that needed to be
        // kept in sync.
        return null;
      }
      return _typeAssert(context, toType, isDynamic);
    } else {
      return this;
    }
  }

  /**
   * Wraps a function with a conversion, so it can be called directly from
   * Dart or JS code with the proper arity. We avoid the wrapping if the target
   * function has the same arity.
   *
   * Also wraps a callback attached to the dom (e.g. event listeners,
   * setTimeout) so we can restore it's isolate context information. This is
   * needed so that callbacks are executed within the context of the isolate
   * that created them in the first place.
   */
  Value _maybeWrapFunction(Type toType, MethodMember callMethod) {
    int arity = callMethod.parameters.length;
    var myCall = type.getCallMethod();

    Value result = this;
    if (myCall == null || myCall.parameters.length != arity) {
      final stub = world.functionType.getCallStub(new Arguments.bare(arity));
      result = new Value(toType, 'to\$${stub.name}($code)', span);
    }

    if (toType.library.isDom && !type.library.isDom) {
      // TODO(jmesserly): either remove this or make it a more first class
      // feature of our native interop. We shouldn't be checking for the DOM
      // library--any host environment (like node.js) might need this feature
      // for isolates too. But we don't want to wrap every function we send to
      // native code--many callbacks like List.filter are perfectly safe.
      if (arity == 0) {
        world.gen.corejs.useWrap0 = true;
      } else {
        world.gen.corejs.useWrap1 = true;
      }

      result = new Value(toType, '\$wrap_call\$$arity(${result.code})', span);
    }

    return result;
  }

  /**
   * Generates a run time type assertion for the given value. This works like
   * [instanceOf], but it allows null since Dart types are nullable.
   * Also it will throw a TypeError if it gets the wrong type.
   */
  Value _typeAssert(MethodGenerator context, Type toType, bool isDynamic) {
    if (toType is ParameterType) {
      ParameterType p = toType;
      toType = p.extendsType;
    }

    if (toType.isObject || toType.isVar) {
      world.internalError(
          'We thought ${type.name} is not a subtype of ${toType.name}?');
    }

    // Prevent a stack overflow when forceDynamic and type checks are both
    // enabled. forceDynamic would cause the TypeError constructor to type check
    // its arguments, which in turn invokes the TypeError constructor, ad
    // infinitum.
    String throwTypeError(String paramName) => world.withoutForceDynamic(() {
      final typeErrorCtor = world.typeErrorType.getConstructor('_internal');
      world.gen.corejs.ensureTypeNameOf();
      final result = typeErrorCtor.invoke(context, null,
          new Value.type(world.typeErrorType, null),
          new Arguments(null, [
            new Value(world.objectType, paramName, null),
            new Value(world.stringType, '"${toType.name}"', null)]),
          isDynamic);
      world.gen.corejs.useThrow = true;
      return '\$throw(${result.code})';
    });

    // TODO(jmesserly): better assert for integers?
    if (toType.isNum) toType = world.numType;

    // Generate a check like these:
    //   obj && obj.is$TypeName()
    //   $assert_int(obj)
    //
    // We rely on the fact that calling an undefined method produces a JS
    // TypeError. Alternatively we could define fallbacks on Object that throw.
    String check;
    if (toType.isVoid) {
      check = '\$assert_void($code)';
      if (toType.typeCheckCode == null) {
        toType.typeCheckCode = '''
function \$assert_void(x) {
  if (x == null) return null;
  ${throwTypeError("x")}
}''';
      }
    } else if (toType == world.nonNullBool) {
      // This could be made less of a special case
      world.gen.corejs.useNotNullBool = true;
      check = '\$notnull_bool($code)';

    } else if (toType.library.isCore && toType.typeofName != null) {
      check = '\$assert_${toType.name}($code)';

      if (toType.typeCheckCode == null) {
        toType.typeCheckCode = '''
function \$assert_${toType.name}(x) {
  if (x == null || typeof(x) == "${toType.typeofName}") return x;
  ${throwTypeError("x")}
}''';
      }
    } else {
      toType.isChecked = true;

      String checkName = 'assert\$' + toType.jsname;

      // If we track nullability, we could simplify this check.
      var temp = context.getTemp(this);
      check = '(${context.assignTemp(temp, this).code} == null ? null :';
      check += ' ${temp.code}.$checkName())';
      if (this != temp) context.freeTemp(temp);

      // Generate the fallback on Object (that throws a TypeError)
      world.objectType.varStubs.putIfAbsent(checkName,
          () => new VarMethodStub(checkName, null, Arguments.EMPTY,
            throwTypeError('this')));
    }

    return new Value(toType, check, span);
  }

  /**
   * Test to see if value is an instance of this type.
   *
   * - If a primitive type, then uses the JavaScript typeof.
   * - If it's a non-generic class, use instanceof.
   * - Otherwise add a fake member to test for.  This value is generated
   *   as a function so that it can be called for a runtime failure.
   */
  Value instanceOf(MethodGenerator context, Type toType, SourceSpan span,
      [bool isTrue=true, bool forceCheck=false]) {
    // TODO(jimhug): Optimize away tests that will always pass unless
    //    forceCheck is true.

    if (toType.isVar) {
      world.error('can not resolve type', span);
    }

    String testCode = null;
    if (toType.isVar || toType.isObject || toType is ParameterType) {
      // Note: everything is an Object, including null.
      if (needsTemp) {
        return new Value(world.nonNullBool, '($code, true)', span);
      } else {
        // TODO(jimhug): Mark non-const?
        return Value.fromBool(true, span);
      }
    }

    if (toType.library.isCore) {
      var typeofName = toType.typeofName;
      if (typeofName != null) {
        testCode = "(typeof($code) ${isTrue ? '==' : '!='} '$typeofName')";
      }
    }
    if (toType.isClass && toType is !ConcreteType
        && !toType.isHiddenNativeType) {
      toType.markUsed();
      testCode = '($code instanceof ${toType.jsname})';
      if (!isTrue) {
        testCode = '!' + testCode;
      }
    }
    if (testCode == null) {
      toType.isTested = true;

      // If we track nullability, we could simplify this check.
      var temp = context.getTemp(this);

      String checkName = 'is\$${toType.jsname}';
      testCode = '(${context.assignTemp(temp, this).code} &&';
      testCode += ' ${temp.code}.$checkName())';
      if (isTrue) {
        // Add !! to convert to boolean.
        // TODO(jimhug): only do this if needed
        testCode = '!!' + testCode;
      } else {
        // The single ! here nicely converts undefined to false and function
        // to true.
        testCode = '!' + testCode;
      }
      if (this != temp) context.freeTemp(temp);

      // Generate the fallback on Object (that returns false)
      if (!world.objectType.varStubs.containsKey(checkName)) {
        world.objectType.varStubs[checkName] =
          new VarMethodStub(checkName, null, Arguments.EMPTY, 'return false');
      }
    }
    return new Value(world.nonNullBool, testCode, span);
  }

  void convertWarning(Type toType) {
    // TODO(jmesserly): better error messages for type conversion failures
    world.warning('type "${type.name}" is not assignable to "${toType.name}"',
        span);
  }

  Value invokeNoSuchMethod(MethodGenerator context, String name, Node node,
      [Arguments args]) {
    var pos = '';
    if (args != null) {
      var argsCode = [];
      for (int i = 0; i < args.length; i++) {
        argsCode.add(args.values[i].code);
      }
      pos = Strings.join(argsCode, ", "); // don't remove trailing nulls
    }
    final noSuchArgs = [
        new Value(world.stringType, '"$name"', node.span),
        new Value(world.listType, '[$pos]', node.span)];

    // TODO(jmesserly): should be passing names but that breaks tests. Oh well.
    /*if (args != null && args.hasNames) {
      var names = [];
      for (int i = args.bareCount; i < args.length; i++) {
        names.add('"${args.getName(i)}", ${args.values[i].code}');
      }
      noSuchArgs.add(new Value(world.gen.useMapFactory(),
          '\$map(${Strings.join(names, ", ")})'));
    }*/

    // Finally, invoke noSuchMethod
    return _resolveMember(context, 'noSuchMethod', node).invoke(
        context, node, this, new Arguments(null, noSuchArgs));
  }


  static Value fromBool(bool value, SourceSpan span) {
    return new BoolValue(value, true, span);
  }

  static Value fromInt(int value, SourceSpan span) {
    return new IntValue(value, true, span);
  }

  static Value fromDouble(double value, SourceSpan span) {
    return new DoubleValue(value, true, span);
  }

  static Value fromString(String value, SourceSpan span) {
    return new StringValue(value, true, span);
  }

  static Value fromNull(SourceSpan span) {
    return new NullValue(true, span);
  }
}


// TODO(jimhug): rename to PrimitiveValue and refactor further
class EvaluatedValue extends Value implements Hashable {
  /** Is this value treated as const by dart language? */
  bool isConst;

  EvaluatedValue(this.isConst, Type type, SourceSpan span):
    super(type, '@@@', span, false);

  String get code() {
    world.internalError('Should not be getting code from raw EvaluatedValue',
      span);
  }


  EvaluatedValue get constValue() => this;

  // TODO(jimhug): Using computed code here without caching is major fear.
  int hashCode() => code.hashCode();

  bool operator ==(var other) {
    return other is EvaluatedValue && other.type == this.type &&
      other.code == this.code;
  }
}


class NullValue extends EvaluatedValue {
  NullValue(bool isConst, SourceSpan span):
    super(isConst, world.varType, span);

  get actualValue() => null;

  String get code() => 'null';

  Value binop(int kind, var other, MethodGenerator context, var node) {
    if (other is! NullValue) return super.binop(kind, other, context, node);

    final c = isConst && other.isConst;
    final s = node.span;
    switch (kind) {
      case TokenKind.EQ_STRICT:
      case TokenKind.EQ:
        return new BoolValue(true, c, s);
      case TokenKind.NE_STRICT:
      case TokenKind.NE:
        return new BoolValue(false, c, s);
    }

    return super.binop(kind, other, context, node);
  }
}

class BoolValue extends EvaluatedValue {
  bool actualValue;

  BoolValue(this.actualValue, bool isConst, SourceSpan span):
    super(isConst, world.nonNullBool, span);

  String get code() => actualValue ? 'true' : 'false';

  Value unop(int kind, MethodGenerator context, var node) {
    switch (kind) {
      case TokenKind.NOT:
        return new BoolValue(!actualValue, isConst, node.span);
    }
    return super.unop(kind, context, node);
  }

  Value binop(int kind, var other, MethodGenerator context, var node) {
    if (other is! BoolValue) return super.binop(kind, other, context, node);

    final c = isConst && other.isConst;
    final s = node.span;
    bool x = actualValue, y = other.actualValue;
    switch (kind) {
      case TokenKind.EQ_STRICT:
      case TokenKind.EQ:
        return new BoolValue(x == y, c, s);
      case TokenKind.NE_STRICT:
      case TokenKind.NE:
        return new BoolValue(x != y, c, s);
      case TokenKind.AND:
        return new BoolValue(x && y, c, s);
      case TokenKind.OR:
        return new BoolValue(x || y, c, s);
    }

    return super.binop(kind, other, context, node);
  }
}

class IntValue extends EvaluatedValue {
  int actualValue;

  IntValue(this.actualValue, bool isConst, SourceSpan span):
    super(isConst, world.intType, span);

  // TODO(jimhug): Only add parens when needed.
  String get code() => '(${actualValue})';

  Value unop(int kind, MethodGenerator context, var node) {
    switch (kind) {
      case TokenKind.ADD:
        // This is allowed on numeric constants only
        return new IntValue(actualValue, isConst, span);
      case TokenKind.SUB:
        return new IntValue(-actualValue, isConst, span);
      case TokenKind.BIT_NOT:
        return new IntValue(~actualValue, isConst, span);
    }
    return super.unop(kind, context, node);
  }


  Value binop(int kind, var other, MethodGenerator context, var node) {
    final c = isConst && other.isConst;
    final s = node.span;
    if (other is IntValue) {
      int x = actualValue;
      int y = other.actualValue;
      switch (kind) {
        case TokenKind.EQ_STRICT:
        case TokenKind.EQ:
          return new BoolValue(x == y, c, s);
        case TokenKind.NE_STRICT:
        case TokenKind.NE:
          return new BoolValue(x != y, c, s);

        case TokenKind.BIT_OR:
          return new IntValue(x | y, c, s);
        case TokenKind.BIT_XOR:
          return new IntValue(x ^ y, c, s);
        case TokenKind.BIT_AND:
          return new IntValue(x & y, c, s);
        case TokenKind.SHL:
          return new IntValue(x << y, c, s);
        case TokenKind.SAR:
          return new IntValue(x >> y, c, s);
        case TokenKind.SHR:
          return new IntValue(x >>> y, c, s);
        case TokenKind.ADD:
          return new IntValue(x + y, c, s);
        case TokenKind.SUB:
          return new IntValue(x - y, c, s);
        case TokenKind.MUL:
          return new IntValue(x * y, c, s);
        case TokenKind.DIV:
          return new DoubleValue(x / y, c, s);
        case TokenKind.TRUNCDIV:
          return new IntValue(x ~/ y, c, s);
        case TokenKind.MOD:
          return new IntValue(x % y, c, s);
        case TokenKind.LT:
          return new BoolValue(x < y, c, s);
        case TokenKind.GT:
          return new BoolValue(x > y, c, s);
        case TokenKind.LTE:
          return new BoolValue(x <= y, c, s);
        case TokenKind.GTE:
          return new BoolValue(x >= y, c, s);
      }
    } else if (other is DoubleValue) {
      int x = actualValue;
      double y = other.actualValue;
      switch (kind) {
        case TokenKind.EQ_STRICT:
        case TokenKind.EQ:
          return new BoolValue(x == y, c, s);
        case TokenKind.NE_STRICT:
        case TokenKind.NE:
          return new BoolValue(x != y, c, s);

        case TokenKind.ADD:
          return new DoubleValue(x + y, c, s);
        case TokenKind.SUB:
          return new DoubleValue(x - y, c, s);
        case TokenKind.MUL:
          return new DoubleValue(x * y, c, s);
        case TokenKind.DIV:
          return new DoubleValue(x / y, c, s);
        case TokenKind.TRUNCDIV:
          // TODO(jimhug): I expected int, but corelib says double here...
          return new DoubleValue(x ~/ y, c, s);
        case TokenKind.MOD:
          return new DoubleValue(x % y, c, s);
        case TokenKind.LT:
          return new BoolValue(x < y, c, s);
        case TokenKind.GT:
          return new BoolValue(x > y, c, s);
        case TokenKind.LTE:
          return new BoolValue(x <= y, c, s);
        case TokenKind.GTE:
          return new BoolValue(x >= y, c, s);
      }
    }

    return super.binop(kind, other, context, node);
  }
}

class DoubleValue extends EvaluatedValue {
  double actualValue;

  DoubleValue(this.actualValue, bool isConst, SourceSpan span):
    super(isConst, world.doubleType, span);

  String get code() => '(${actualValue})';

  Value unop(int kind, MethodGenerator context, var node) {
    switch (kind) {
      case TokenKind.ADD:
        // This is allowed on numeric constants only
        return new DoubleValue(actualValue, isConst, span);
      case TokenKind.SUB:
        return new DoubleValue(-actualValue, isConst, span);
    }
    return super.unop(kind, context, node);
  }

  Value binop(int kind, var other, MethodGenerator context, var node) {
    final c = isConst && other.isConst;
    final s = node.span;
    if (other is DoubleValue) {
      double x = actualValue;
      double y = other.actualValue;
      switch (kind) {
        case TokenKind.EQ_STRICT:
        case TokenKind.EQ:
          return new BoolValue(x == y, c, s);
        case TokenKind.NE_STRICT:
        case TokenKind.NE:
          return new BoolValue(x != y, c, s);

        case TokenKind.ADD:
          return new DoubleValue(x + y, c, s);
        case TokenKind.SUB:
          return new DoubleValue(x - y, c, s);
        case TokenKind.MUL:
          return new DoubleValue(x * y, c, s);
        case TokenKind.DIV:
          return new DoubleValue(x / y, c, s);
        case TokenKind.TRUNCDIV:
          // TODO(jimhug): I expected int, but corelib says double here...
          return new DoubleValue(x ~/ y, c, s);
        case TokenKind.MOD:
          return new DoubleValue(x % y, c, s);
        case TokenKind.LT:
          return new BoolValue(x < y, c, s);
        case TokenKind.GT:
          return new BoolValue(x > y, c, s);
        case TokenKind.LTE:
          return new BoolValue(x <= y, c, s);
        case TokenKind.GTE:
          return new BoolValue(x >= y, c, s);
      }
    } else if (other is IntValue) {
      double x = actualValue;
      int y = other.actualValue;
      switch (kind) {
        case TokenKind.EQ_STRICT:
        case TokenKind.EQ:
          return new BoolValue(x == y, c, s);
        case TokenKind.NE_STRICT:
        case TokenKind.NE:
          return new BoolValue(x != y, c, s);

        case TokenKind.ADD:
          return new DoubleValue(x + y, c, s);
        case TokenKind.SUB:
          return new DoubleValue(x - y, c, s);
        case TokenKind.MUL:
          return new DoubleValue(x * y, c, s);
        case TokenKind.DIV:
          return new DoubleValue(x / y, c, s);
        case TokenKind.TRUNCDIV:
          // TODO(jimhug): I expected int, but corelib says double here...
          return new DoubleValue(x ~/ y, c, s);
        case TokenKind.MOD:
          return new DoubleValue(x % y, c, s);
        case TokenKind.LT:
          return new BoolValue(x < y, c, s);
        case TokenKind.GT:
          return new BoolValue(x > y, c, s);
        case TokenKind.LTE:
          return new BoolValue(x <= y, c, s);
        case TokenKind.GTE:
          return new BoolValue(x >= y, c, s);
      }
    }

    return super.binop(kind, other, context, node);
  }
}

class StringValue extends EvaluatedValue {
  String actualValue;

  StringValue(this.actualValue, bool isConst, SourceSpan span):
    super(isConst, world.stringType, span);

  Value binop(int kind, var other, MethodGenerator context, var node) {
    if (other is! StringValue) return super.binop(kind, other, context, node);

    final c = isConst && other.isConst;
    final s = node.span;
    String x = actualValue, y = other.actualValue;
    switch (kind) {
      case TokenKind.EQ_STRICT:
      case TokenKind.EQ:
        return new BoolValue(x == y, c, s);
      case TokenKind.NE_STRICT:
      case TokenKind.NE:
        return new BoolValue(x != y, c, s);
      case TokenKind.ADD:
        return new StringValue(x + y, c, s);
    }

    return super.binop(kind, other, context, node);
  }


  // This is expensive and we may want to cache its value if called often
  String get code() {
    // TODO(jimhug): This could be much more efficient
    StringBuffer buf = new StringBuffer();
    buf.add('"');
    for (int i=0; i < actualValue.length; i++) {
      var ch = actualValue.charCodeAt(i);
      switch (ch) {
        case 9/*'\t'*/: buf.add(@'\t'); break;
        case 10/*'\n'*/: buf.add(@'\n'); break;
        case 13/*'\r'*/: buf.add(@'\r'); break;
        case 34/*"*/: buf.add(@'\"'); break;
        case 92/*\*/: buf.add(@'\\'); break;
        default:
          if (ch >= 32 && ch <= 126) {
            buf.add(actualValue[i]);
          } else {
            final hex = ch.toRadixString(16);
            switch (hex.length) {
              case 1: buf.add(@'\x0'); buf.add(hex); break;
              case 2: buf.add(@'\x'); buf.add(hex); break;
              case 3: buf.add(@'\u0'); buf.add(hex); break;
              case 4: buf.add(@'\u'); buf.add(hex); break;
              default:
                world.internalError(
                  'unicode values greater than 2 bytes not implemented');
                break;
            }
          }
          break;
      }
    }
    buf.add('"');
    return buf.toString();
  }
}


class ListValue extends EvaluatedValue {
  List<Value> values;

  ListValue(this.values, bool isConst, Type type, SourceSpan span):
    super(isConst, type, span);

  String get code() {
    final buf = new StringBuffer();
    buf.add('[');
    for (var i = 0; i < values.length; i++) {
      if (i > 0) buf.add(', ');
      buf.add(values[i].code);
    }
    buf.add(']');
    var listCode = buf.toString();

    if (!isConst) return listCode;

    var v = new Value(world.listType, listCode, span);
    final immutableListCtor = world.immutableListType.getConstructor('from');
    final result = immutableListCtor.invoke(null, null,
        new Value.type(v.type, span), new Arguments(null, [v]));
    return result.code;
  }

  Value binop(int kind, var other, MethodGenerator context, var node) {
    // TODO(jimhug): Support int/double better
    if (other is! ListValue) return super.binop(kind, other, context, node);

    switch (kind) {
      case TokenKind.EQ_STRICT:
        return new BoolValue(type == other.type && code == other.code,
          isConst && other.isConst, node.span);
      case TokenKind.NE_STRICT:
        return new BoolValue(type != other.type || code != other.code,
          isConst && other.isConst, node.span);
    }

    return super.binop(kind, other, context, node);
  }

  GlobalValue getGlobalValue() {
    assert(isConst);

    return world.gen.globalForConst(this, values);
  }
}


class MapValue extends EvaluatedValue {
  List<Value> values;

  MapValue(this.values, bool isConst, Type type, SourceSpan span):
    super(isConst, type, span);

  String get code() {
    // Cache?
    var items = new ListValue(values, false, world.listType, span);
    var tp = world.corelib.topType;
    Member f = isConst ? tp.getMember('_constMap') : tp.getMember('_map');
    // TODO(jimhug): Clean up invoke signature
    var value = f.invoke(null, null, new Value.type(tp, null),
      new Arguments(null, [items]));
    return value.code;
  }

  GlobalValue getGlobalValue() {
    assert(isConst);

    return world.gen.globalForConst(this, values);
  }

  Value binop(int kind, var other, MethodGenerator context, var node) {
    if (other is! MapValue) return super.binop(kind, other, context, node);

    switch (kind) {
      case TokenKind.EQ_STRICT:
        return new BoolValue(type == other.type && code == other.code,
          isConst && other.isConst, node.span);
      case TokenKind.NE_STRICT:
        return new BoolValue(type != other.type || code != other.code,
          isConst && other.isConst, node.span);
    }

    return super.binop(kind, other, context, node);
  }
}


class ObjectValue extends EvaluatedValue {
  Map<FieldMember, Value> fields;
  bool seenNativeInitializer = false;

  String _code;

  ObjectValue(bool isConst, Type type, SourceSpan span):
    fields = {}, super(isConst, type, span);

  String get code() {
    if (_code === null) validateInitialized(null);
    return _code;
  }

  initFields() {
    var allMembers = world.gen._orderValues(type.getAllMembers());
    for (var f in allMembers) {
      if (f.isField && !f.isStatic && f.declaringType.isClass) {
        fields[f] = f.computeValue();
      }
    }
  }

  setField(Member field, Value value, [bool duringInit = false]) {
    var currentValue = fields[field];
    if (isConst && !value.isConst) {
      world.error('used of non-const value in const intializer', value.span);
    }

    if (currentValue === null) {
      fields[field] = value;
      if (field.isFinal && !duringInit) {
        world.error('can not initialize final fields outside of initializer',
          value.span);
      }
    } else {
      // TODO(jimhug): Clarify spec on reinitializing fields with defaults.
      if (field.isFinal && field.computeValue() === null) {
        world.error('reassignment of field not allowed', value.span,
          field.span);
      } else {
        fields[field] = value; //currentValue.union(value);
      }
    }
  }

  validateInitialized(SourceSpan span) {
    var buf = new StringBuffer();
    buf.add('Object.create(');
    buf.add('${type.jsname}.prototype, ');

    buf.add('{');
    for (var field in fields.getKeys()) {
      buf.add(field.name);
      buf.add(': ');
      buf.add('{"value": ');
      if (fields[field] === null) {
        world.error('Required field "${field.name}" was not initialized',
          span, field.span);
        buf.add('null');
      } else {
        buf.add(fields[field].code);
      }
      buf.add(', writeable: false}, ');
    }
    buf.add('})');
    _code = buf.toString();
  }

  Value binop(int kind, var other, MethodGenerator context, var node) {
    if (other is! ObjectValue) return super.binop(kind, other, context, node);

    switch (kind) {
      case TokenKind.EQ_STRICT:
      case TokenKind.EQ:
        return new BoolValue(type == other.type && code == other.code,
          isConst && other.isConst, node.span);
      case TokenKind.NE_STRICT:
      case TokenKind.NE:
        return new BoolValue(type != other.type || code != other.code,
          isConst && other.isConst, node.span);
    }

    return super.binop(kind, other, context, node);
  }

}


/**
 * A global value in the generated code, which corresponds to either a static
 * field or a memoized const expressions.
 */
class GlobalValue extends Value implements Comparable {
  /** Static field definition (null for constant exp). */
  FieldMember field;

  /**
   * When [this] represents a constant expression, the global variable name
   * generated for it.
   */
  String name;

  /** The value of the field or constant expression to declare. */
  Value exp;

  /** True for either cont expressions or a final static field. */
  bool isConst;

  /** The actual constant value, when [isConst] is true. */
  get actualValue() => exp.dynamic.actualValue;

  /** If [isConst], the [EvaluatedValue] that defines this value. */
  EvaluatedValue get constValue() => isConst ? exp.constValue : null;

  /** Other globals that should be defined before this global. */
  List<GlobalValue> dependencies;

  GlobalValue(Type type, String code, bool isConst,
      this.field, this.name, this.exp,
      SourceSpan span, List<Value> _dependencies)
      : super(type, code, span, !isConst),
        dependencies = [], isConst = isConst {
    // store transitive-dependencies so sorting algorithm works correctly.
    for (var dep in _dependencies) {
      if (dep is GlobalValue) {
        dependencies.add(dep);
        dependencies.addAll(dep.dependencies);
      }
    }
  }

  int compareTo(GlobalValue other) {
    // order by dependencies, o.w. by name
    if (other == this) {
      return 0;
    } else if (dependencies.indexOf(other) >= 0) {
      return 1;
    } else if (other.dependencies.indexOf(this) >= 0) {
      return -1;
    } else if (dependencies.length > other.dependencies.length) {
      return 1;
    } else if (dependencies.length < other.dependencies.length) {
      return -1;
    } else if (name == null && other.name != null) {
      return 1;
    } else if (name != null && other.name == null) {
      return -1;
    } else if (name != null) {
      return name.compareTo(other.name);
    } else {
      return field.name.compareTo(other.field.name);
    }
  }
}

/**
 * Represents the hidden or implicit value in a bare reference like 'a'.
 * This could be this, the current type, or the current library for purposes
 * of resolving members.
 */
class BareValue extends Value {
  MethodGenerator home;

  BareValue(this.home, MethodGenerator outermost, SourceSpan span)
      : super(outermost.method.declaringType, null, span, false) {
    isType = outermost.isStatic;
  }

  // Bare values should never be treated as though they have a var type. If we
  // do end up calling an instance method that should be forced dynamic, we can
  // handle that in `_tryResolveMember`.
  Type get type() => _type;

  // TODO(jimhug): Lazy initialization here is weird!
  _ensureCode() {
    if (code != null) return;
    if (isType) {
      code = type.jsname;
    } else {
      code = home._makeThisCode();
    }
  }

  _tryResolveMember(MethodGenerator context, String name) {
    assert(context == home);

    // First look for members directly defined on my type.
    var member = type.resolveMember(name);
    if (member != null) {
      if (options.forceDynamic && !member.isStatic) {
        member = context.findMembers(name);
      }
      _ensureCode();
      return member;
    }

    // Then look for members in my library.
    member = home.library.lookup(name, span);
    if (member != null) {
      return member;
    }

    _ensureCode();
    return null;
  }
}

String _escapeForComment(String text) {
  return text.replaceAll('/*', '/ *').replaceAll('*/', '* /');
}
