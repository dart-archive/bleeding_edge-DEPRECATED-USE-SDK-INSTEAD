// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Represents a meta-value for code generation.
 */
class Value {
  /** The [Type] of the [Value]. */
  Type type;

  /** The code to generate this value. */
  String code;

  /** Is this a reference to super? */
  bool isSuper;

  /** Is this a pretend first-class type? */
  bool isType;

  /** If we reference this value multiple times, do we need a temp? */
  bool needsTemp;

  Value(this.type, this.code,
        // TODO(sigmund): reorder, so that needsTemp comes first.
        [this.isSuper = false, this.needsTemp = true, this.isType = false]) {
    if (type == null) type = world.varType;
  }

  /** Is this value a constant expression? */
  bool get isConst () => false;

  // TODO(jimhug): These three methods are still a little too similar for me.
  get_(MethodGenerator context, String name, Node node) {
    var member = _resolveMember(context, name, node);
    if (member != null) {
      member = member.get_(context, node, this);
    }
    // member.get_ returns null if no signatures match the given node.
    if (member != null) {
      return member;
    } else {
      return invokeNoSuchMethod(context, 'get:$name', node);
    }
  }

  set_(MethodGenerator context, String name, Node node, Value value,
      [bool isDynamic=false]) {
    var member = _resolveMember(context, name, node);
    if (member != null) {
      member = member.set_(context, node, this, value, isDynamic);
    }
    // member.set_ returns null if no signatures match the given node.
    if (member != null) {
      return member;
    } else {
      return invokeNoSuchMethod(context, 'set:$name', node,
          new Arguments(null, [value]));
    }
  }

  invoke(MethodGenerator context, String name, Node node, Arguments args,
      [bool isDynamic=false]) {
    // TODO(jimhug): The != method is weird - understand it better.
    if (type.isVar && name == '\$ne') {
      if (args.values.length != 1) {
        world.warning('wrong number of arguments for !=', node.span);
      }
      return new Value(null, '\$ne($code, ${args.values[0].code})');
    }

    // TODO(jmesserly): it'd be nice to remove these special cases
    // We could create a $call (and $ne) in world members, and have
    // those guys handle the canInvoke/Invoke logic.

    // Note: this check is a little different than the one in canInvoke, because
    // sometimes we need to call dynamically even if we found the $call method
    // statically.

    if (name == '\$call') {
      if (isType) {
        world.error('must use "new" or "const" to construct a new instance',
            node.span);
      }
      if (type.needsVarCall(args)) {
        return _varCall(context, args);
      }
    }

    var member = _resolveMember(context, name, node);
    if (member == null) {
      return invokeNoSuchMethod(context, name, node, args);
    } else {
      return member.invoke(context, node, this, args, isDynamic);
    }
  }

  bool canInvoke(MethodGenerator context, String name, Arguments args) {
    // TODO(jimhug): The != method is weird - understand it better.
    if (type.isVar && name == '\$ne') {
      return true;
    }

    if (type.isVarOrFunction && name == '\$call') {
      return true;
    }

    var member = _tryResolveMember(context, name);
    return member != null && member.canInvoke(context, args);
  }

  _tryResolveMember(MethodGenerator context, String name) {
    var member = null;
    if (!type.isVar) {
      if (isSuper) {
        member = type.members[name];
      } else {
        member = type.resolveMember(name);
      }
    }
    if (member == null) {
      // TODO(jmesserly): shouldn't look in world except for "var"
      member = context.findMembers(name);
    }
    return member;
  }

  _resolveMember(MethodGenerator context, String name, Node node) {
    var member = _tryResolveMember(context, name);
    if (member == null) {
      // TODO(jmesserly): we suppress warnings if someone has overridden
      // noSuchMethod, and we know it will call their version. Is that right?
      if (_tryResolveMember(context, 'noSuchMethod').members.length > 1) {
        return null;
      }

      var typeName = type.name == null ? type.library.name : type.name;
      var message = 'can not resolve "$name" on "${typeName}"';
      if (isType) {
        world.error(message, node.span);
      } else {
        world.warning(message, node.span);
      }
      // TODO(jmesserly): isn't this condition always true if we got here?
      if (context.findMembers(name) == null) {
        world.warning('$name is not defined anywhere in the world.', node.span);
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
    var stub = world.functionType.getCallStub(args);
    return new Value(null, '$code.${stub.name}(${args.getCode()})');
  }

  /**
   * Assign or convert this value to another type.
   * This is used for converting between function types, and inserting type
   * checks when --enable_type_checks is enabled.
   */
  Value convertTo(MethodGenerator context, Type toType, Node node,
      [bool isDynamic=false]) {

    // Check types if enabled, unless this is a dynamic operation
    bool checked = options.enableTypeChecks && !isDynamic;

    var callMethod = toType.getCallMethod();
    if (callMethod != null) {
      if (checked && !toType.isAssignable(type)) {
        convertWarning(toType, node);
      }

      int arity = callMethod.parameters.length;
      var myCall = type.getCallMethod();
      if (myCall == null || myCall.parameters.length != arity) {
        final stub = world.functionType.getCallStub(new Arguments.bare(arity));
        return new Value(toType, 'to\$${stub.name}($code)');
      }
    }

    // Don't add runtime asserts unless we have type checks turned on.
    if (!options.enableTypeChecks) {
      return this;
    }

    if (type.isSubtypeOf(toType)) {
      return this; // widening conversion
    } else if (checked && !toType.isSubtypeOf(type)) {
      // According to the static types, this conversion can't work.
      convertWarning(toType, node);
    }

    return _typeAssert(context, toType, node);
  }

  /**
   * Generates a run time type assertion for the given value. This works like
   * [instanceOf], but it allows null since Dart types are nullable.
   * Also it will throw a TypeError if it gets the wrong type.
   */
  // TODO(jmesserly): this generated code is too verbose.
  Value _typeAssert(MethodGenerator context, Type toType, Node node) {
    if (toType is ParameterType) {
      ParameterType p = toType;
      toType = p.extendsType;
    }

    // TODO(jmesserly): I don't like the duplication with instanceOf
    var temp = context.getTemp(this);
    String testCode;
    if (toType.library.isCore && toType.typeofName != null) {
      testCode = "typeof(${temp.code}) == '${toType.typeofName}'";
    } else if (toType.isClass && toType is !ConcreteType) {
      toType.markUsed();
      testCode = '${temp.code} instanceof ${toType.jsname}';
    } else {
      toType.isTested = true;
      testCode = '${temp.code}.is\$${toType.jsname}';
    }
    testCode = '(${context.assignTemp(temp, this).code} == null || $testCode)';
    var test = new Value(world.boolType, testCode);

    var err = world.corelib.types['TypeError'];
    world.gen.genMethod(err.members['toString']);
    var args = new Arguments(null, [temp,
        new Value(world.stringType, '"${toType.name}"')]);
    var typeErr = err.getConstructor('').invoke(context, node, null, args);

    var result = new Value(toType, '(${test.code} ? ${temp.code} : '
        + '\$throw(${typeErr.code}))');
    if (temp != this) context.freeTemp(temp);
    return result;
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
      return new EvaluatedValue(world.boolType, true, 'true', null);
    }

    if (toType is ParameterType) {
      return new EvaluatedValue(world.boolType, true, 'true', null);
    }

    String testCode = null;
    if (toType.library.isCore) {
      var typeofName = toType.typeofName;
      if (typeofName != null) {
        testCode = "(typeof($code) ${isTrue ? '==' : '!='} '$typeofName')";
      }
    }
    if (toType.isClass && toType is !ConcreteType) {
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

      testCode = '(${context.assignTemp(temp, this).code} &&';
      testCode += ' ${temp.code}.is\$${toType.jsname})';
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
    }
    return new Value(world.boolType, testCode);
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
        new Value(world.stringType, '"$name"'),
        new Value(world.listType, '[$pos]')];

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
    return _tryResolveMember(context, 'noSuchMethod').invoke(
        context, node, this, new Arguments(null, noSuchArgs));
  }

  Value invokeSpecial(String name, Arguments args, Type returnType) {
    assert(name.startsWith('\$'));
    assert(!args.hasNames);
    // TODO(jimhug): We need to do this a little bit more like get and set on
    // properties.  We should check the set of members for something
    // like "requiresNativeIndexer" and "requiresDartIndexer" to
    // decide on a strategy.

    var argsString = args.getCode();
    // Most operator calls need to be emitted as function calls, so we don't
    // box numbers accidentally. Indexing is the exception.
    if (name == '\$index' || name == '\$setindex') {
      return new Value(returnType, '$code.$name($argsString)');
    } else {
      if (argsString.length > 0) argsString = ', $argsString';
      return new Value(returnType, '$name($code$argsString)');
    }
  }
}

/** A value that can has been evaluated statically. */
class EvaluatedValue extends Value {

  var actualValue;

  bool get isConst() => true;

  /**
   * A canonicalized form of the code. Two const expressions that result in the
   * same instance should have the same [canonicalCode].
   */
  String canonicalCode;

  /** Original span where this evaluated expression came from. */
  SourceSpan original;

  factory EvaluatedValue(type, actualValue, canonicalCode, original) {
    return new EvaluatedValue._internal(type, actualValue,
        canonicalCode, original, codeWithComments(canonicalCode, original));
  }

  EvaluatedValue._internal(
      type, this.actualValue, this.canonicalCode, this.original, code)
      : super(type, code, false, false, false);

  static String codeWithComments(String canonicalCode, SourceSpan original) {
    return (original != null && original.text != canonicalCode)
        ? '$canonicalCode/*${original.text}*/' : canonicalCode;
  }
}

/** An evaluated constant list expression. */
class ConstListValue extends EvaluatedValue {
  List<EvaluatedValue> values;

  factory ConstListValue(Type type, List<EvaluatedValue> values,
      String actualValue, String canonicalCode, SourceSpan original) {
    return new ConstListValue._internal(type, values, actualValue,
        canonicalCode, original, codeWithComments(canonicalCode, original));
  }

  ConstListValue._internal(type, this.values,
      actualValue, canonicalCode, original, code) :
    super._internal(type, actualValue, canonicalCode, original, code);
}

/** An evaluated constant map expression. */
class ConstMapValue extends EvaluatedValue {
  Map<String, EvaluatedValue> values;

  factory ConstMapValue(Type type, List<EvaluatedValue> keyValuePairs,
      String actualValue, String canonicalCode, SourceSpan original) {
    final values = new Map<String, EvaluatedValue>();
    for (int i = 0; i < keyValuePairs.length; i += 2) {
      values[keyValuePairs[i].actualValue] = keyValuePairs[i + 1];
    }
    return new ConstMapValue._internal(type, values, actualValue,
        canonicalCode, original, codeWithComments(canonicalCode, original));
  }

  ConstMapValue._internal(type, this.values,
      actualValue, canonicalCode, original, code) :
    super._internal(type, actualValue, canonicalCode, original, code);
}

/** An evaluated constant object expression. */
class ConstObjectValue extends EvaluatedValue {
  Map<String, EvaluatedValue> fields;

  factory ConstObjectValue(
      Type type, Map<String, EvaluatedValue> fields,
      String canonicalCode, SourceSpan original) {
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
        canonicalCode, original, codeWithComments(canonicalCode, original));
  }

  ConstObjectValue._internal(type, this.fields,
      actualValue, canonicalCode, original, code) :
    super._internal(type, actualValue, canonicalCode, original, code);

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

  /** Original span where this value came from. */
  SourceSpan original;

  /** True for either cont expressions or a final static field. */
  bool get isConst() => exp.isConst && (field == null || field.isFinal);

  /** The actual constant value, if [isConst] is true. */
  get actualValue() => exp.dynamic.actualValue;

  /** Other globals that should be defined before this global. */
  List<GlobalValue> dependencies;

  factory GlobalValue.fromStatic(field, exp, dependencies) {
    var code = (exp.isConst ? exp.canonicalCode : exp.code);
    var codeWithComment = '$code/*${field.declaringType.name}.${field.name}*/';
    return new GlobalValue(
        exp.type, codeWithComment, field.isFinal, field, null, exp,
        code, null, dependencies.filter((d) => d is GlobalValue));
  }

  factory GlobalValue.fromConst(uniqueId, exp, dependencies) {
    var name = "const\$$uniqueId";
    var codeWithComment = "$name/*${exp.original.text}*/";
    return new GlobalValue(
        exp.type, codeWithComment, true, null, name, exp, name,
        exp.original,
        dependencies.filter((d) => d is GlobalValue));
  }

  GlobalValue(type, code, isConst,
      this.field, this.name, this.exp, this.canonicalCode,
      this.original, this.dependencies)
      : super(type, code, false, !isConst, false);

  int compareTo(GlobalValue other) {
    // order by dependencies, o.w. by name
    if (other == this) {
      return 0;
    } else if (dependencies.indexOf(other, 0) >= 0) {
      return 1;
    } else if (other.dependencies.indexOf(this, 0) >= 0) {
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
