// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Represents a meta-value for code generation.
 */
class Value {
  /** The [Type] of the [Value]. */
  Type type;

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

  /** If we reference this value multiple times, do we need a temp? */
  bool needsTemp;

  // TODO(jmesserly): until reified generics are fixed, treat ParameterType as
  // "var".
  bool get _typeIsVarOrParameterType() => type.isVar || type is ParameterType;

  Value(this.type, this.code, this.span, [this.needsTemp = true]) {
    if (type == null) world.internalError('type passed as null', span);
  }

  // TODO(jimhug): Replace with TypeValue.
  Value.type(this.type, this.span)
    : code = null, needsTemp = false, isType = true {
    if (type == null) world.internalError('type passed as null', span);
  }

  /** Is this value a constant expression? */
  bool get isConst() => false;

  /**
   * A canonicalized form of the code. Two const expressions that result in the
   * same instance should have the same [canonicalCode].
   */
  String get canonicalCode() => null;

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


  Value invoke(MethodGenerator context, String name, Node node, Arguments args,
      [bool isDynamic=false]) {
    // TODO(jmesserly): try to get rid of this code path. We're generating a
    // synthetic != on Object (see DefinedType._createNotEqualMember) already.
    // So it should be pretty easy to make this go away.
    if (_typeIsVarOrParameterType && name == ':ne') {
      if (args.values.length != 1) {
        world.warning('wrong number of arguments for !=', node.span);
      }
      // Ensure the == operator is generated, and get its type
      var eq = invoke(context, ':eq', node, args, isDynamic);
      world.gen.corejs.useOperator(':ne');
      return new Value(eq.type, '\$ne($code, ${args.values[0].code})',
          node.span);
    }

    // TODO(jmesserly): it'd be nice to remove these special cases
    // We could create a :call (and :ne) in world members, and have
    // those guys handle the canInvoke/Invoke logic.

    // Note: this check is a little different than the one in canInvoke, because
    // sometimes we need to call dynamically even if we found the :call method
    // statically.

    if (name == ':call') {
      if (isType) {
        world.error('must use "new" or "const" to construct a new instance',
            node.span);
      }
      if (type.needsVarCall(args)) {
        return _varCall(context, args);
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
    // TODO(jimhug): The != method is weird - understand it better.
    if (_typeIsVarOrParameterType && name == ':ne') {
      return true;
    }

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
  Value _varCall(MethodGenerator context, Arguments args) {
    // TODO(jmesserly): calls to unknown functions will bypass type checks,
    // which normally happen on the caller side, or in the generated stub for
    // dynamic method calls. What should we do?
    var stub = world.functionType.getCallStub(args);
    return new Value(world.varType, '$code.${stub.name}(${args.getCode()})',
      span);
  }

  /** True if convertTo would generate a conversion. */
  // TODO(jmesserly): I don't like how this is coupled to convertTo.
  bool needsConversion(Type toType) {
    var callMethod = toType.getCallMethod();
    if (callMethod != null) {
      int arity = callMethod.parameters.length;
      var myCall = type.getCallMethod();
      if (myCall == null || myCall.parameters.length != arity) {
        return true;
      }
    }
    if (options.enableTypeChecks) {
      Type fromType = type;
      if (type.isVar && (code != 'null' || !toType.isNullable)) {
        fromType = world.objectType;
      }
      bool bothNum = type.isNum && toType.isNum;
      return !(fromType.isSubtypeOf(toType) || bothNum);
    }
    return false;
  }

  /**
   * Assign or convert this value to another type.
   * This is used for converting between function types, inserting type
   * checks when --enable_type_checks is enabled, and wrapping callback
   * functions passed to the dom so we can restore their isolate context.
   */
  // WARNING: this needs to be kept in sync with needsConversion above.
  Value convertTo(MethodGenerator context, Type toType, Node node,
      [bool isDynamic=false]) {

    // Issue type warnings unless we are processing a dynamic operation.
    bool checked = !isDynamic;

    var callMethod = toType.getCallMethod();
    if (callMethod != null) {
      if (checked && !toType.isAssignable(type)) {
        convertWarning(toType, node);
      }

      int arity = callMethod.parameters.length;
      var myCall = type.getCallMethod();
      if (myCall == null || myCall.parameters.length != arity) {
        final stub = world.functionType.getCallStub(new Arguments.bare(arity));
        var val = new Value(toType, 'to\$${stub.name}($code)', node.span);
        // TODO(sigmund): try to remove, see below
        return _isDomCallback(toType) && !_isDomCallback(type) ?
            val._wrapDomCallback(toType, arity) : val;
      } else if (_isDomCallback(toType) && !_isDomCallback(type)) {
        // TODO(sigmund): try to remove, see below
        return _wrapDomCallback(toType, arity);
      }
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
      convertWarning(toType, node);
    }

    // Generate a runtime checks if they're turned on, otherwise skip it.
    if (options.enableTypeChecks) {
      return _typeAssert(context, toType, node, isDynamic);
    } else {
      return this;
    }
  }

  /**
   * Checks whether [toType] is a callback function, and it is defined in the
   * dom library.
   */
  bool _isDomCallback(toType) {
    return (toType.definition is FunctionTypeDefinition
        && toType.library == world.dom);
  }

  /**
   * Wraps a callback attached to the dom (e.g. event listeners, setTimeout) so
   * we can restore it's isolate context information. This is needed so that
   * callbacks are executed within the context of the isolate that created them
   * in the first place.
   */
  // TODO(sigmund): try to remove this specialized logic about isolates
  // and the dom from the compiler, move into the actual dom library if
  // possible.
  Value _wrapDomCallback(Type toType, int arity) {
    if (arity == 0) {
      world.gen.corejs.useWrap0 = true;
    } else {
      world.gen.corejs.useWrap1 = true;
    }
    return new Value(toType, '\$wrap_call\$$arity($code)', span);
  }

  /**
   * Generates a run time type assertion for the given value. This works like
   * [instanceOf], but it allows null since Dart types are nullable.
   * Also it will throw a TypeError if it gets the wrong type.
   */
  Value _typeAssert(MethodGenerator context, Type toType, Node node,
      bool isDynamic) {
    if (toType is ParameterType) {
      ParameterType p = toType;
      toType = p.extendsType;
    }

    if (toType.isObject || toType.isVar) {
      world.internalError(
          'We thought ${type.name} is not a subtype of ${toType.name}?');
    }

    final typeError = world.corelib.types['TypeError'];
    final typeErrorCtor = typeError.getConstructor('_internal');
    world.gen.corejs.ensureTypeNameOf();
    final result = typeErrorCtor.invoke(context, node,
        new Value.type(typeError, null),
        new Arguments(null, [
          new Value(world.objectType, 'this', null),
          new Value(world.stringType, '"${toType.name}"', null)]),
        isDynamic);
    world.gen.corejs.useThrow = true;
    final throwTypeError = '\$throw(${result.code})';

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
  $throwTypeError
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
  $throwTypeError
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
      if (!world.objectType.varStubs.containsKey(checkName)) {
        world.objectType.varStubs[checkName] =
          new VarMethodStub(checkName, null, Arguments.EMPTY, throwTypeError);
      }
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
        return new EvaluatedValue(world.nonNullBool, true, 'true', null);
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

  void convertWarning(Type toType, Node node) {
    // TODO(jmesserly): better error messages for type conversion failures
    world.warning('type "${type.name}" is not assignable to "${toType.name}"',
        node.span);
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
}

// TODO(jmesserly): the subtypes of Value require a lot of type checks and
// downcasts to use; can we make that cleaner? (search for ".dynamic")

/** A value that can has been evaluated statically. */
class EvaluatedValue extends Value {

  var actualValue;

  bool get isConst() => true;

  /**
   * A canonicalized form of the code. Two const expressions that result in the
   * same instance should have the same [canonicalCode].
   */
  String canonicalCode;

  factory EvaluatedValue(Type type, actualValue, String canonicalCode,
      SourceSpan span) {
    return new EvaluatedValue._internal(type, actualValue,
        canonicalCode, span, codeWithComments(canonicalCode, span));
  }

  EvaluatedValue._internal(Type type, this.actualValue, this.canonicalCode,
      SourceSpan span, String code)
    : super(type, code, span, false);

  static String codeWithComments(String canonicalCode, SourceSpan span) {
    return (span != null && span.text != canonicalCode)
        ? '$canonicalCode/*${span.text}*/' : canonicalCode;
  }
}

/** An evaluated constant list expression. */
class ConstListValue extends EvaluatedValue {
  List<EvaluatedValue> values;

  factory ConstListValue(Type type, List<EvaluatedValue> values,
      String actualValue, String canonicalCode, SourceSpan span) {
    return new ConstListValue._internal(type, values, actualValue,
        canonicalCode, span, codeWithComments(canonicalCode, span));
  }

  ConstListValue._internal(type, this.values,
      actualValue, canonicalCode, span, code) :
    super._internal(type, actualValue, canonicalCode, span, code);
}

/** An evaluated constant map expression. */
class ConstMapValue extends EvaluatedValue {
  Map<String, EvaluatedValue> values;

  factory ConstMapValue(Type type, List<EvaluatedValue> keyValuePairs,
      String actualValue, String canonicalCode, SourceSpan span) {
    final values = new Map<String, EvaluatedValue>();
    for (int i = 0; i < keyValuePairs.length; i += 2) {
      values[keyValuePairs[i].actualValue] = keyValuePairs[i + 1];
    }
    return new ConstMapValue._internal(type, values, actualValue,
        canonicalCode, span, codeWithComments(canonicalCode, span));
  }

  ConstMapValue._internal(type, this.values,
      actualValue, canonicalCode, span, code) :
    super._internal(type, actualValue, canonicalCode, span, code);
}

/** An evaluated constant object expression. */
class ConstObjectValue extends EvaluatedValue {
  Map<String, EvaluatedValue> fields;

  factory ConstObjectValue(
      Type type, Map<String, EvaluatedValue> fields,
      String canonicalCode, SourceSpan span) {
    // compute a unique-string form used to index this value in the global const
    // map. This is used to ensure that multiple const object values are
    // equivalent if they have the same type name and values on each field.
    final fieldValues = [];
    for (var f in fields.getKeys()) {
      fieldValues.add('$f = ${fields[f].actualValue}');
    }
    fieldValues.sort((a, b) => a.compareTo(b));
    final actualValue = 'const ${type.jsname} ['
        + Strings.join(fieldValues, ',') + ']';
    return new ConstObjectValue._internal(type, fields, actualValue,
        canonicalCode, span, codeWithComments(canonicalCode, span));
  }

  ConstObjectValue._internal(type, this.fields,
      actualValue, canonicalCode, span, code) :
    super._internal(type, actualValue, canonicalCode, span, code);

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

  /**
   * A canonicalized form of the code. Two const expressions that result in the
   * same instance should have the same [canonicalCode].
   */
  String canonicalCode;

  /** True for either cont expressions or a final static field. */
  bool get isConst() => exp.isConst && (field == null || field.isFinal);

  /** The actual constant value, if [isConst] is true. */
  get actualValue() => exp.dynamic.actualValue;

  /** Other globals that should be defined before this global. */
  List<GlobalValue> dependencies;

  factory GlobalValue.fromStatic(field, Value exp, dependencies) {
    var code = (exp.isConst ? exp.canonicalCode : exp.code);
    var codeWithComment = '$code/*${field.declaringType.name}.${field.name}*/';
    return new GlobalValue(
        exp.type, codeWithComment, field.isFinal, field, null, exp,
        code, exp.span, dependencies.filter((d) => d is GlobalValue));
  }

  factory GlobalValue.fromConst(uniqueId, Value exp, dependencies) {
    var name = "const\$$uniqueId";
    var codeWithComment = "$name/*${exp.span.text}*/";
    return new GlobalValue(
        exp.type, codeWithComment, true, null, name, exp, name,
        exp.span,
        dependencies.filter((d) => d is GlobalValue));
  }

  GlobalValue(Type type, String code, bool isConst,
      this.field, this.name, this.exp, this.canonicalCode,
      SourceSpan span, List<GlobalValue> _dependencies)
      : super(type, code, span, !isConst), dependencies = [] {
    // store transitive-dependencies so sorting algorithm works correctly.
    for (final dep in _dependencies) {
      dependencies.add(dep);
      dependencies.addAll(dep.dependencies);
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
