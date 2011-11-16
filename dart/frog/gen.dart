// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Top level generator object for writing code and keeping track of
 * dependencies.
 *
 * Should have two compilation models, but only one implemented so far.
 *
 * 1. Do a top-level resolution of all types and their members.
 * 2. Start from main and walk the call-graph compiling members as needed.
 * 2a. That includes compiling overriding methods and calling methods by
 *     selector when invoked on var.
 * 3. Spit out all required code.
 */
class WorldGenerator {
  MethodMember main;
  CodeWriter writer;
  Map<String, GlobalValue> globals;
  CoreJs corejs;
  bool _inheritsGenerated = false;

  WorldGenerator(this.main, this.writer): globals = {}, corejs = new CoreJs();

  run() {
    var metaGen = new MethodGenerator(main, null);
    var mainCall = main.invoke(metaGen, null, null, Arguments.EMPTY);
    main.declaringType.markUsed();

    // TODO(jimhug): Better way to capture hidden control flow.
    world.corelib.types['BadNumberFormatException'].markUsed();
    world.coreimpl.types['NumImplementation'].markUsed();
    world.coreimpl.types['StringImplementation'].markUsed();
    genMethod(
        world.coreimpl.types['StringImplementation'].getMember('contains'));

    // Only include isolate-specific code if isolates are used.
    if (world.corelib.types['Isolate'].isUsed
        || world.coreimpl.types['ReceivePortImpl'].isUsed) {
      corejs.useIsolates = true;
      MethodMember isolateMain =
          world.coreimpl.topType.resolveMember('startAsIsolate').members[0];
      mainCall = isolateMain.invoke(metaGen, null, null,
          new Arguments(null, [main._get(metaGen, main.definition, null)]));
    }

    writeTypes(world.coreimpl);
    writeTypes(world.corelib);

    // Write the main library. This will cause all libraries to be written in
    // the topographic sort order.
    writeTypes(main.declaringType.library);

    _writeGlobals();
    writer.writeln('${mainCall.code};');
  }

  GlobalValue globalForStaticField(FieldMember field, Value fieldValue,
      List<Value> dependencies) {
    var fullname = "${field.declaringType.jsname}.${field.jsname}";
    if (!globals.containsKey(fullname)) {
      globals[fullname] = new GlobalValue.fromStatic(
          field, fieldValue, dependencies);
    }
    return globals[fullname];
  }

  GlobalValue globalForConst(EvaluatedValue exp, List<Value> dependencies) {
    var code = exp.canonicalCode;
    if (!globals.containsKey(code)) {
      globals[code] =
        new GlobalValue.fromConst(globals.length, exp, dependencies);
    }
    return globals[code];
  }

  writeTypes(Library lib) {
    if (lib.isWritten) return;

    // Do this first to be safe in the face of circular refs.
    lib.isWritten = true;

    // Ensure all imports have been written.
    for (var import in lib.imports) {
      writeTypes(import.library);
    }

    // Ensure that our source files have a notion of "order" so we can emit
    // types in the same order source files are imported.
    for (int i = 0; i < lib.sources.length; i++) {
      lib.sources[i].orderInLibrary = i;
    }

    writer.comment('//  ********** Library ${lib.name} **************');
    if (lib.isCore) {
      // Generates the JS natives for dart:core.
      writer.comment('//  ********** Natives dart:core **************');
      corejs.generate(writer);
    }
    for (var file in lib.natives) {
      var filename = basename(file.filename);
      writer.comment('//  ********** Natives $filename **************');
      writer.writeln(file.text);
    }
    lib.topType.markUsed(); // TODO(jimhug): EGREGIOUS HACK

    for (var type in _orderValues(lib.types)) {
      if (type.isUsed && type.isClass) {
        writeType(type);

        if (type.isGeneric) {
          for (var ct in _orderValues(type._concreteTypes)) {
            writeType(ct);
          }
        }
      }
      if (type.isFunction && type.varStubs != null) {
        // Emit stubs on "Function" if needed
        writer.comment('// ********** Code for ${type.jsname} **************');
        _writeDynamicStubs(type);
      }
      // Type check functions for builtin JS types
      if (type.typeCheckCode != null) {
        writer.writeln(type.typeCheckCode);
      }
    }
  }

  genMethod(Member meth, [MethodGenerator enclosingMethod=null]) {
    if (!meth.isGenerated && !meth.isAbstract && meth.definition != null) {
      new MethodGenerator(meth, enclosingMethod).run();
    }
  }

  _maybeIsTest(Type onType, Type checkType) {
    if (!checkType.isTested) return;

    var value = 'false';
    if (onType.isSubtypeOf(checkType)) {
      value = 'function(){return this;}';
    }

    writer.writeln('${onType.jsname}.prototype.is\$${checkType.jsname} = '
        + '$value;');
  }

  writeType(Type type) {
    // TODO(jimhug): Workaround for problems with reified generic Array.
    if (type.name != null && type is ConcreteType &&
        type.library == world.coreimpl &&
        type.name.startsWith('ListFactory')) {
      writer.writeln('${type.jsname} = ${type.genericType.jsname};');
      return;
    }

    var typeName = type.jsname != null ? type.jsname : 'top level';
    writer.comment('// ********** Code for ${typeName} **************');
    if (type.isNativeType && !type.isTop) {
      var nativeName = type.definition.nativeType;
      if (nativeName == '') {
        writer.writeln('function ${type.jsname}() {}');
      } else if (type.jsname != nativeName) {
        writer.writeln('${type.jsname} = ${nativeName};');
      }
    }

    if (type.isTop) {
      // no preludes for top type
    } else if (type.constructors.length == 0) {
      if (!type.isNativeType) {
        // TODO(jimhug): More guards to guarantee staticness
        writer.writeln('function ${type.jsname}() {}');
      }
    } else {
      Member standardConstructor = type.constructors[''];
      if (standardConstructor == null ||
          standardConstructor.generator == null) {
        if (!type.isNativeType) {
          writer.writeln('function ${type.jsname}() {}');
        }
      } else {
        standardConstructor.generator.writeDefinition(writer, null);
      }

      for (var c in type.constructors.getValues()) {
        if (c.generator != null && c != standardConstructor) {
          c.generator.writeDefinition(writer, null);
        }
      }
    }

    if (!type.isTop) {
      if (type is ConcreteType) {
        _ensureInheritsHelper();
        writer.writeln(
            '\$inherits(${type.jsname}, ${type.genericType.jsname});');
      } else if (!type.isNativeType) {
        if (type.parent != null && !type.parent.isObject) {
          _ensureInheritsHelper();
          writer.writeln('\$inherits(${type.jsname}, ${type.parent.jsname});');
        }
      }
    }

    // Concrete types (like List<String>) will have this already defined on
    // their prototype from the generic type (like List)
    if (type is! ConcreteType) {
      _maybeIsTest(type, type);
    }
    if (type.genericType._concreteTypes != null) {
      for (var ct in _orderValues(type.genericType._concreteTypes)) {
        _maybeIsTest(type, ct);
      }
    }

    if (type.interfaces != null) {
      final seen = new Set();
      final worklist = [];
      worklist.addAll(type.interfaces);
      seen.addAll(type.interfaces);
      while (!worklist.isEmpty()) {
        var interface_ = worklist.removeLast();
        _maybeIsTest(type, interface_.genericType);
        if (interface_.genericType._concreteTypes != null) {
          for (var ct in _orderValues(interface_.genericType._concreteTypes)) {
            _maybeIsTest(type, ct);
          }
        }
        for (var other in interface_.interfaces) {
          if (!seen.contains(other)) {
            worklist.addLast(other);
            seen.add(other);
          }
        }
      }
    }

    type.factories.forEach(_writeMethod);

    for (var member in _orderValues(type.members)) {
      if (member is FieldMember) {
        _writeField(member);
      }

      if (member is PropertyMember) {
        _writeProperty(member);
      }

      if (member.isMethod) {
        _writeMethod(member);
      }
    }

    _writeDynamicStubs(type);
  }

  /**
   * Generates the $inherits function when it's first used. Unlike some of the
   * other helpers in [CoreJS], we don't notice that we need this one until
   * we're generating types.
   */
  _ensureInheritsHelper() {
    if (_inheritsGenerated) return;

    _inheritsGenerated = true;
    writer.writeln(@"""
/** Implements extends for Dart classes on JavaScript prototypes. */
function $inherits(child, parent) {
  if (child.prototype.__proto__) {
    child.prototype.__proto__ = parent.prototype;
  } else {
    function tmp() {};
    tmp.prototype = parent.prototype;
    child.prototype = new tmp();
    child.prototype.constructor = child;
  }
}""");
  }

  _writeDynamicStubs(Type type) {
    if (type.varStubs != null) {
      for (var stub in orderValuesByKeys(type.varStubs)) {
        stub.generate(writer);
      }
    }
  }

  _writeStaticField(FieldMember field) {
    // Final static fields must be constants which will be folded and inlined.
    if (field.isFinal) return;

    var fullname = "${field.declaringType.jsname}.${field.jsname}";
    if (globals.containsKey(fullname)) {
      var value = globals[fullname];
      if (field.declaringType.isTop && !field.isNative) {
        writer.writeln('var ${field.jsname} = ${value.exp.code};');
      } else {
        writer.writeln(
          '${field.declaringType.jsname}.${field.jsname} = ${value.exp.code};');
      }
    }
    // No need to write code for a static class field with no initial value.
  }

  _writeField(FieldMember field) {
    // Generate declarations for static top-level fields with no value.
    if (field.declaringType.isTop && !field.isNative && field.value == null) {
      writer.writeln('var ${field.jsname};');
    }

    // generate code for instance fields
    if (field._providePropertySyntax) {
      writer.writeln(
        '${field.declaringType.jsname}.prototype.get\$${field.jsname} = ' +
        'function() { return this.${field.jsname}; };');
      if (!field.isFinal) {
        writer.writeln(
          '${field.declaringType.jsname}.prototype.set\$${field.jsname} = ' +
          'function(value) { return this.${field.jsname} = value; };');
      }
    }

    // TODO(jimhug): Currently choose not to initialize fields on objects, but
    //    instead to rely on uninitialized === null in our generated code.
    //    Investigate the perf pros and cons of this.
  }

  _writeProperty(PropertyMember property) {
    if (property.getter != null) _writeMethod(property.getter);
    if (property.setter != null) _writeMethod(property.setter);

    if (property._provideFieldSyntax) {
      writer.enterBlock('Object.defineProperty(' +
        '${property.declaringType.jsname}.prototype, "${property.jsname}", {');
      if (property.getter != null) {
        writer.write(
          'get: ${property.declaringType.jsname}.prototype.${property.getter.jsname}');
        // The shenanigan below is to make IE happy -- IE 9 doesn't like a
        // trailing comma on the last element in a list.
        writer.writeln(property.setter == null ? '' : ',');
      }
      if (property.setter != null) {
        writer.writeln(
          'set: ${property.declaringType.jsname}.prototype.${property.setter.jsname}');
      }
      writer.exitBlock('});');
    }
  }

  _writeMethod(Member method) {
    if (method.generator != null) {
      method.generator.writeDefinition(writer, null);
    }
  }

  _writeGlobals() {
    if (globals.length > 0) {
      writer.comment('//  ********** Globals **************');
    }
    var list = globals.getValues();
    list.sort((a, b) => a.compareTo(b));
    for (var global in list) {
      if (global.field != null) {
        _writeStaticField(global.field);
      } else {
        writer.writeln('var ${global.name} = ${global.exp.code};');
      }
    }
  }

  /** Order a list of values in a Map by SourceSpan, then by name. */
  List _orderValues(Map map) {
    // TODO(jmesserly): should we copy the list?
    // Right now, the Maps are returning a copy already.
    List values = map.getValues();
    values.sort(_compareMembers);
    return values;
  }

  int _compareMembers(x, y) {
    if (x.span != null && y.span != null) {
      // First compare by source span.
      int spans = x.span.compareTo(y.span);
      if (spans != 0) return spans;
    }
    if (x.span == null) return 1;
    if (y.span == null) return -1;

    // If that fails, compare by name.
    return x.name.compareTo(y.name);
  }

  /** Marks that a map literal is used, e.g. a call to $map. */
  Type useMapFactory() {
    corejs.useMap = true;
    var factType = world.coreimpl.types['HashMapImplementation'];
    var m = factType.resolveMember('\$setindex');
    genMethod(m.members[0]); // TODO(jimhug): Clean up initializing.
    var c = factType.getConstructor('');
    genMethod(c);
    return factType;
  }
}


class BlockScope {
  MethodGenerator enclosingMethod;
  BlockScope parent;
  Map<String, Value> _vars; // TODO(jimhug): Using a list may improve perf.

  /**
   * Variables in this method that have been captured by lambdas.
   * Don't reuse the names in child blocks.
   */
  Set<String> _closedOver;

  /** If we are in a catch block, this is the exception variable to rethrow. */
  Value rethrow;

  /**
   * True if the block is reentrant while the current method is executing.
   * This is only used for the blocks within loops.
   */
  bool reentrant;

  BlockScope(this.enclosingMethod, this.parent, [this.reentrant = false])
    : _vars = {} {

    if (isMethodScope) {
      _closedOver = new Set<String>();
    } else {
      // Blocks within a reentrant block are also reentrant.
      reentrant = reentrant || parent.reentrant;
    }
  }

  /** True if this is the top level scope of the method. */
  bool get isMethodScope() {
    return parent == null || parent.enclosingMethod != enclosingMethod;
  }

  /**
   * Gets the method scope associated with this block scope (possibly itself).
   */
  BlockScope get methodScope() {
    var s = this;
    while (!s.isMethodScope) s = s.parent;
    return s;
  }

  lookup(String name) {
    var ret = _vars[name];
    if (ret != null) return ret;

    for (var s = parent; s != null; s = s.parent) {
      ret = s._vars[name];
      if (ret != null) {
        // If this variable is from a different method, it means we closed over
        // it in the child lambda. Time for some bookeeping!
        if (s.enclosingMethod != enclosingMethod) {
          // Make sure the parent method doesn't reuse this variable to mean
          // something else.
          s.methodScope._closedOver.add(ret.code);

          // If the scope we found this variable in is reentrant, remember the
          // variable. The lambda we're in will capture it with Function.bind.
          if (enclosingMethod.captures != null && s.reentrant) {
            enclosingMethod.captures.add(ret.code);
          }
        }

        return ret;
      }
    }
  }

  /**
   * Returns true if we can't use this name because we would be shadowing
   * another name in the JS that we might need to access later.
   */
  bool _isDefinedInParent(String name) {
    if (isMethodScope && _closedOver.contains(name)) return true;

    for (var s = parent; s != null; s = s.parent) {
      if (s._vars.containsKey(name)) return true;
      // Don't reuse a name that's been closed over
      if (s.isMethodScope && s._closedOver.contains(name)) return true;
    }

    // Ensure that we don't shadow another name that would've been accessible,
    // like to level names.
    // (This lookup might report errors, which is a bit strange.
    // But probably harmless since we have to pay for the lookup anyway.)
    final type = enclosingMethod.method.declaringType;
    if (type.library.lookup(name, null) != null) return true;

    // Nobody else needs this name. It's safe to reuse.
    return false;
  }


  Value create(String name, Type type, SourceSpan span,
      [bool isParameter = false]) {

    var jsName = world.toJsIdentifier(name);
    if (_vars.containsKey(name)) {
      world.error('duplicate name "$name"', span);
    }

    // Make sure variables don't shadow any names we might need to access.
    if (!isParameter) {
      int index = 0;
      while (_isDefinedInParent(jsName)) {
        jsName = '$name${index++}';
      }
    }

    var ret = new Value(type, jsName, span, false);
    _vars[name] = ret;
    return ret;
  }

  Value declareParameter(Parameter p) {
    return create(p.name, p.type, p.definition.span, isParameter:true);
  }

  /** Declares a variable in the current scope for this identifier. */
  Value declare(DeclaredIdentifier id) {
    var type = enclosingMethod.method.resolveType(id.type, false);
    return create(id.name.name, type, id.span);
  }

  /**
   * Finds the first lexically enclosing catch block, if any, and returns its
   * exception variable.
   */
  Value getRethrow() {
    var scope = this;
    while (scope.rethrow == null && scope.parent != null) {
      scope = scope.parent;
    }
    return scope.rethrow;
  }
}


/**
 * A naive code generator for Dart.
 */
class MethodGenerator implements TreeVisitor {
  Member method;
  CodeWriter writer;
  BlockScope _scope;
  MethodGenerator enclosingMethod;
  bool needsThis;
  List<String> _paramCode;

  // TODO(jmesserly): if we knew temps were always used like a stack, we could
  // reduce the overhead here.
  List<String> _freeTemps;
  Set<String> _usedTemps;

  /**
   * The set of variables that this lambda closes that need to capture
   * with Function.prototype.bind. This is any variable that lives inside a
   * reentrant block scope (e.g. loop bodies).
   *
   * This field is null if we don't need to track this.
   */
  Set<String> captures;

  MethodGenerator(this.method, this.enclosingMethod)
      : writer = new CodeWriter(), needsThis = false {
    if (enclosingMethod != null) {
      _scope = new BlockScope(this, enclosingMethod._scope);
      captures = new Set();
    } else {
      _scope = new BlockScope(this, null);
    }
    // For named lambdas, add the name to this scope so we can call it
    // recursively.
    if (enclosingMethod != null && method.name != '') {
      MethodMember m = method; // lambdas must be MethodMembers
      _scope.create(m.name, m.functionType, m.definition.span);
    }
    _usedTemps = new Set();
    _freeTemps = [];
  }

  Library get library() => method.library;

  // TODO(jimhug): Where does this really belong?
  MemberSet findMembers(String name) {
    return library._findMembers(name);
  }

  bool get isClosure() => (enclosingMethod != null);

  bool get isStatic() => method.isStatic;

  Value getTemp(Value value) {
    return value.needsTemp ? forceTemp(value) : value;
  }

  Value forceTemp(Value value) {
    String name;
    if (_freeTemps.length > 0) {
      name = _freeTemps.removeLast();
    } else {
      name = '\$' + _usedTemps.length;
    }
    _usedTemps.add(name);
    return new Value(value.type, name, value.span, /*needsTemp:*/false);
  }

  Value assignTemp(Value tmp, Value v) {
    if (tmp == v) {
      return v;
    } else {
      // TODO(jmesserly): we should mark this returned value with the temp
      // somehow, so getTemp will reuse it instead of allocating a new one.
      return new Value(v.type, '(${tmp.code} = ${v.code})', v.span);
    }
  }

  void freeTemp(Value value) {
    if (_usedTemps.remove(value.code)) {
      _freeTemps.add(value.code);
    } else {
      world.internalError(
        'tried to free unused value or non-temp "${value.code}"');
    }
  }

  run() {
    if (method.isGenerated) return;

    // This avoids any attempts to infer across recursion.
    method.isGenerated = true;
    method.generator = this;

    if (method.definition.body is NativeStatement) {
      if (method.definition.body.body == null) {
        method.generator = null;
      } else {
        _paramCode = map(method.parameters, (p) => p.name);
        writer.write(method.definition.body.body);
      }
    } else {
      writeBody();
    }
  }


  writeDefinition(CodeWriter defWriter, LambdaExpression lambda/*=null*/) {
    // To implement block scope: capture any variables we need to.
    var paramCode = _paramCode;
    var names = null;
    if (captures != null && captures.length > 0) {
      names = new List.from(captures);
      names.sort((x, y) => x.compareTo(y));
      // Prepend these as extra parameters. We'll bind them below.
      paramCode = new List.from(names);
      paramCode.addAll(_paramCode);
    }

    String _params = '(${Strings.join(_paramCode, ", ")})';
    String params = '(${Strings.join(paramCode, ", ")})';
    // TODO(jmesserly): many of these are similar, it'd be nice to clean up.
    if (method.declaringType.isTop && !isClosure) {
      defWriter.enterBlock('function ${method.jsname}$params {');
    } else if (isClosure) {
      if (method.name == '') {
        defWriter.enterBlock('(function $params {');
      } else if (names != null) {
        if (lambda == null) {
          defWriter.enterBlock('var ${method.jsname} = (function$params {');
        } else {
          defWriter.enterBlock('(function ${method.jsname}$params {');
        }
      } else {
        defWriter.enterBlock('function ${method.jsname}$params {');
      }
    } else if (method.isConstructor) {
      if (method.constructorName == '') {
        defWriter.enterBlock('function ${method.declaringType.jsname}$params {');
      } else {
        defWriter.enterBlock('${method.declaringType.jsname}.${method.constructorName}\$ctor = function$params {');
      }
    } else if (method.isFactory) {
      defWriter.enterBlock('${method.generatedFactoryName} = function$_params {');
    } else if (method.isStatic) {
      defWriter.enterBlock('${method.declaringType.jsname}.${method.jsname} = function$_params {');
    } else {
      defWriter.enterBlock('${method.declaringType.jsname}.prototype.'
        + '${method.jsname} = function$_params {');
    }

    if (needsThis) {
      defWriter.writeln('var \$this = this; // closure support');
    }

    if (_usedTemps.length > 0 || _freeTemps.length > 0) {
      assert(_usedTemps.length == 0); // all temps should be freed.
      _freeTemps.addAll(_usedTemps);
      _freeTemps.sort((x, y) => x.compareTo(y));
      defWriter.writeln('var ${Strings.join(_freeTemps, ", ")};');
    }

    // TODO(jimhug): Lot's of string translation here - perf bottleneck?
    defWriter.writeln(writer.text);

    if (names != null) {
      // TODO(jmesserly): bind isn't implemented in Safari.
      defWriter.exitBlock('}).bind(null, ${Strings.join(names, ", ")})');
    } else if (isClosure && method.name == '') {
      defWriter.exitBlock('})');
    } else {
      defWriter.exitBlock('}');
    }
    if (method.isConstructor && method.constructorName != '') {
      defWriter.writeln(
        '${method.declaringType.jsname}.${method.constructorName}\$ctor.prototype = ' +
        '${method.declaringType.jsname}.prototype;');
    }

    _provideOptionalParamInfo(defWriter);

    if (method is MethodMember) {
      MethodMember m = method;
      if (m._providePropertySyntax) {
        defWriter.enterBlock('${m.declaringType.jsname}.prototype'
            + '.get\$${m.jsname} = function() {');
        // TODO(jimhug): Bind not available in older Safari, need fallback?
        defWriter.writeln('return ${m.declaringType.jsname}.prototype.'
            + '${m.jsname}.bind(this);');
        defWriter.exitBlock('}');

        if (m._provideFieldSyntax) {
          world.internalError('bound m accessed with field syntax');
        }
      }
    }
  }

  /**
   * Generates information about the default/named arguments into the JS code.
   * Only methods that are passed as bound methods to "var" need this. It is
   * generated to support run time stub creation.
   */
  _provideOptionalParamInfo(CodeWriter defWriter) {
    if (method is MethodMember) {
      MethodMember meth = method;
      if (meth._provideOptionalParamInfo) {
        var optNames = [];
        var optValues = [];
        meth.genParameterValues();
        for (var param in meth.parameters) {
          if (param.isOptional) {
            optNames.add(param.name);
            optValues.add(_escapeString(param.value.code));
          }
        }
        if (optNames.length > 0) {
          // TODO(jmesserly): the logic for how to refer to
          // static/instance/top-level members is duplicated all over the place.
          // Badly needs cleanup.
          var start = '';
          if (meth.isStatic) {
            if (!meth.declaringType.isTop) {
              start = meth.declaringType.jsname + '.';
            }
          } else {
            start = meth.declaringType.jsname + '.prototype.';
          }

          optNames.addAll(optValues);
          var optional = "['" + Strings.join(optNames, "', '") + "']";
          defWriter.writeln('${start}${meth.jsname}.\$optional = $optional');
        }
      }
    }
  }

  writeBody() {
    var initializers = null;
    var initializedFields = null; // to check that final fields are initialized
    if (method.isConstructor) {
      initializers = [];
      initializedFields = new Set();
      for (var f in world.gen._orderValues(method.declaringType.getAllMembers())) {
        if (f is FieldMember && !f.isStatic) {
          var cv = f.computeValue();
          if (cv != null) {
            initializers.add('this.${f.jsname} = ${cv.code}');
            initializedFields.add(f.name);
          }
        }
      }
    }

    // Collects parameters for writing signature in the future.
    _paramCode = [];
    for (var p in method.parameters) {
      if (initializers != null && p.isInitializer) {
        var field = method.declaringType.getMember(p.name);
        if (field == null) {
          world.error('bad this parameter - no matching field',
            p.definition.span);
        }
        if (!field.isField) {
          world.error('"this.${p.name}" does not refer to a field',
            p.definition.span);
        }
        var paramValue = new Value(field.returnType, p.name,
          p.definition.span, false);
        _paramCode.add(paramValue.code);

        initializers.add('this.${field.jsname} = ${paramValue.code};');
        initializedFields.add(p.name);
      } else {
        var paramValue = _scope.declareParameter(p);
        _paramCode.add(paramValue.code);
      }
    }

    var body = method.definition.body;

    if (body == null && !method.isConstructor) {
      world.error('unexpected empty body for ${method.name}',
        method.definition.span);
    }

    if (initializers != null) {
      for (var i in initializers) {
        writer.writeln(i);
      }
      final declaredInitializers = method.definition.initializers;
      if (declaredInitializers != null) {
        var initializerCall = null;
        for (var init in declaredInitializers) {
          // TODO(jimhug): Lot's of correctness to verify here.
          if (init is CallExpression) {
            if (initializerCall != null) {
              world.error('only one initializer redirecting call is allowed',
                  init.span);
            }
            initializerCall = init;
          } else if (init is BinaryExpression
              && TokenKind.kindFromAssign(init.op.kind) == 0) {

            var left = init.x;
            if (!(left is DotExpression && left.self is ThisExpression
                || left is VarExpression)) {
              world.error('invalid left side of initializer', left.span);
              continue;
            }

            var f = method.declaringType.getMember(left.name.name);
            if (f == null) {
              world.error('bad initializer - no matching field', left.span);
              continue;
            } else if (!f.isField) {
              world.error('"${left.name.name}" does not refer to a field',
                  left.span);
              continue;
            }

            initializedFields.add(f.name);
            writer.writeln('this.${f.jsname} = ${visitValue(init.y).code};');
          } else {
            world.error('invalid initializer', init.span);
          }
        }

        if (initializerCall != null) {
          var target = _writeInitializerCall(initializerCall);
          if (!target.isSuper) {
            // when calling another constructor on the same class
            // no other initialization is allowed
            if (initializers.length > 0) {
              for (var p in method.parameters) {
                if (p.isInitializer) {
                  world.error(
                      'no initialization allowed on redirecting constructors',
                      p.definition.span);
                  break;
                }
              }
            }
            if (declaredInitializers.length > 1) {
              var init = declaredInitializers[0] == initializerCall
                  ? declaredInitializers[1] : declaredInitializers[0];
              world.error(
                  'no initialization allowed on redirecting constructors',
                  init.span);
            }
            initializedFields = null;
          }
          // TODO(sigmund): check for initialization cycles
        } else {
          // TODO(jimhug): Is it an error not to have an initializerCall?
        }
      }
      writer.comment('// Initializers done');
    }

    // check that initialization was correct
    if (initializedFields != null) {
      for (var name in method.declaringType.members.getKeys()) {
        var member = method.declaringType.members[name];
        if (member is FieldMember && member.isFinal && !member.isStatic
            && !initializedFields.contains(name)) {
          world.error('Field "${name}" is final and was not initialized',
              method.definition.span);
        }
      }
    }

    visitStatementsInBlock(body);
  }

  /**
   * Calls another constructor (super, super.name, this, this.name).  Returns
   * the value of the target expression.
   */
  Value _writeInitializerCall(CallExpression node) {
    String contructorName = '';
    var targetExp = node.target;
    if (targetExp is DotExpression) {
      DotExpression dot = targetExp;
      targetExp = dot.self;
      contructorName = dot.name.name;
    }

    var target = null;
    if (targetExp is SuperExpression) {
      target = _makeSuperValue(targetExp);
    } else if (targetExp is ThisExpression) {
      target = _makeThisValue(targetExp);
    } else {
      world.error('bad call in initializers', node.span);
    }

    var m = target.type.getConstructor(contructorName);
    method.initDelegate = m;
    // check no cycles in in initialization:
    var other = m;
    while (other != null) {
      if (other == method) {
        world.error('initialization cycle', node.span);
        break;
      }
      other = other.initDelegate;
    }

    // move this all to happen when new first constuctor is walked.
    world.gen.genMethod(m);
    var value = m.invoke(this, node, target, _makeArgs(node.arguments));
    if (target.type != world.objectType) {
      // No need to actually call Object's empty super constructor.
      writer.writeln('${value.code};');
    }
    return target;
  }

  _makeArgs(List<ArgumentNode> arguments) {
    var args = [];
    bool seenLabel = false;
    for (var arg in arguments) {
      if (arg.label != null) {
        seenLabel = true;
      } else if (seenLabel) {
        // TODO(jimhug): Move this into parser?
        world.error('bare argument can not follow named arguments', arg.span);
      }
      args.add(visitValue(arg.value));
    }

    return new Arguments(arguments, args);
  }

  /**
   * Escapes a string so it can be inserted into JS code as a double-quoted
   * JS string.
   */
  static String _escapeString(String text) {
    // TODO(jimhug): Use a regex for performance here.
    return text.replaceAll('\\', '\\\\').replaceAll('"', '\\"').replaceAll(
        '\n', '\\n').replaceAll('\r', '\\r');
  }

  /** Visits [body] without creating a new block for a [BlockStatement]. */
  bool visitStatementsInBlock(Statement body) {
    if (body is BlockStatement) {
      BlockStatement block = body;
      for (var stmt in block.body) {
        stmt.visit(this);
      }
    } else {
      if (body != null) body.visit(this);
    }
    return false;
  }

  _pushBlock([bool reentrant = false]) {
    _scope = new BlockScope(this, _scope, reentrant);
  }

  _popBlock() {
    _scope = _scope.parent;
  }

  MethodMember _makeLambdaMethod(String name, FunctionDefinition func) {
    var meth = new MethodMember(name, method.declaringType, func);
    meth.isLambda = true;
    meth.resolve(method.declaringType);
    world.gen.genMethod(meth, this);
    return meth;
  }

  visitBool(Expression node) {
    // Boolean conversions in if/while/do/for/conditions require non-null bool.

    // TODO(jmesserly): why do we have this rule? It seems inconsistent with
    // the rest of the type system, and just causes bogus asserts unless all
    // bools are initialized to false.
    return visitValue(node).convertTo(this, world.nonNullBool, node);
  }

  visitValue(Expression node) {
    if (node == null) return null;

    var value = node.visit(this);
    value.checkFirstClass(node.span);
    return value;
  }

  visitTypedValue(Expression node, Type expectedType) {
    return visitValue(node).convertTo(this, expectedType, node);
  }

  visitVoid(Expression node) {
    // TODO(jmesserly): should we generalize this?
    if (node is PostfixExpression) {
      var value = visitPostfixExpression(node, /*isVoid:*/true);
      value.checkFirstClass(node.span);
      return value;
    }
    // TODO(jimhug): Some level of warnings for non-void things here?
    return visitValue(node);
  }

  // ******************* Statements *******************

  bool visitDietStatement(DietStatement node) {
    var parser = new Parser(node.span.file, startOffset: node.span.start);
    visitStatementsInBlock(parser.block());
    return false;
  }

  bool visitVariableDefinition(VariableDefinition node) {
    var isFinal = false;
    // TODO(jimhug): Clean this up and share modifier parsing somewhere.
    if (node.modifiers != null && node.modifiers[0].kind == TokenKind.FINAL) {
      isFinal = true;
    }
    writer.write('var ');
    var type = method.resolveType(node.type, false);
    for (int i=0; i < node.names.length; i++) {
      var thisType = type;
      if (i > 0) {
        writer.write(', ');
      }
      final name = node.names[i].name;
      var value = visitValue(node.values[i]);
      if (isFinal) {
        if (value == null) {
          world.error('no value specified for final variable', node.span);
        } else {
          // TODO(jimhug): Mark inferred types as special for correct errors.
          if (thisType.isVar) thisType = value.type;
        }
      }

      var val = _scope.create(name, thisType, node.names[i].span);

      if (value == null) {
        writer.write('${val.code}');
      } else {
        value = value.convertTo(this, type, node.values[i]);
        writer.write('${val.code} = ${value.code}');
      }
    }
    writer.writeln(';');
    return false;

  }

  bool visitFunctionDefinition(FunctionDefinition node) {
    var name = world.toJsIdentifier(node.name.name);

    var meth = _makeLambdaMethod(name, node);

    // TODO(jimhug): Pass js name into writeDefinition?
    var funcValue =
      _scope.create(name, meth.functionType, method.definition.span);
    meth.generator.writeDefinition(writer, null);
    return false;
  }

  /**
   * Returns true indicating that normal control-flow is interrupted by
   * this statement. (This could be a return, break, throw, or continue.)
   */
  bool visitReturnStatement(ReturnStatement node) {
    if (node.value == null) {
      // This is essentially "return null".
      // It can't issue a warning because every type is nullable.
      writer.writeln('return;');
    } else {
      if (method.isConstructor) {
        world.error('return of value not allowed from constructor', node.span);
      }
      var value = visitTypedValue(node.value, method.returnType);
      writer.writeln('return ${value.code};');
    }
    return true;
  }

  bool visitThrowStatement(ThrowStatement node) {
    // Dart allows throwing anything, just like JS
    if (node.value != null) {
      var value = visitValue(node.value);
      // Ensure that we generate a toString() method for things that we throw
      value.invoke(this, 'toString', node, Arguments.EMPTY);
      writer.writeln('\$throw(${value.code});');
      world.gen.corejs.useThrow = true;
    } else {
      var rethrow = _scope.getRethrow();
      if (rethrow == null) {
        world.error('rethrow outside of catch', node.span);
      } else {
        // Use a normal throw instead of $throw so we don't capture a new stack
        writer.writeln('throw ${rethrow.code};');
      }
    }
    return true;
  }

  bool visitAssertStatement(AssertStatement node) {
    // be sure to walk test for static checking even is asserts disabled
    var test = visitValue(node.test); // TODO(jimhug): check bool or callable.
    if (options.enableAsserts) {
      var err = world.corelib.types['AssertError'];
      world.gen.genMethod(err.getConstructor(''));
      world.gen.genMethod(err.members['toString']);
      var span = node.test.span;

      // TODO(jmesserly): do we need to include path/line/column here?
      // It should be captured in the stack trace.
      var line = span.file.getLine(span.start);
      var column = span.file.getColumn(line, span.start);
      writer.writeln('\$assert(${test.code}, "${_escapeString(span.text)}",' +
        ' "${basename(span.file.filename)}", ${line + 1}, ${column + 1});');
      world.gen.corejs.useAssert = true;
    }
    return false;
  }

  bool visitBreakStatement(BreakStatement node) {
    // TODO(jimhug): Lots of flow error checking here and below.
    if (node.label == null) {
      writer.writeln('break;');
    } else {
      writer.writeln('break ${node.label.name};');
    }
    return true;
  }

  bool visitContinueStatement(ContinueStatement node) {
    if (node.label == null) {
      writer.writeln('continue;');
    } else {
      writer.writeln('continue ${node.label.name};');
    }
    return true;
  }

  bool visitIfStatement(IfStatement node) {
    var test = visitBool(node.test);
    writer.write('if (${test.code}) ');
    var exit1 = node.trueBranch.visit(this);
    if (node.falseBranch != null) {
      writer.write('else ');
      if (node.falseBranch.visit(this) && exit1) {
        return true;
      }
    }
    return false;
  }

  bool visitWhileStatement(WhileStatement node) {
    var test = visitBool(node.test);
    writer.write('while (${test.code}) ');
    _pushBlock(/*reentrant:*/true);
    node.body.visit(this);
    _popBlock();
    return false;
  }

  bool visitDoStatement(DoStatement node) {
    writer.write('do ');
    _pushBlock(/*reentrant:*/true);
    node.body.visit(this);
    _popBlock();
    var test = visitBool(node.test);
    writer.writeln('while (${test.code})');
    return false;
  }

  bool visitForStatement(ForStatement node) {
    _pushBlock();
    writer.write('for (');
    if (node.init != null) node.init.visit(this);
    else writer.write(';');
    if (node.test != null) {
      var test = visitBool(node.test);
      writer.write(' ${test.code}; ');
    } else {
      writer.write('; ');
    }

    bool needsComma = false;
    for (var s in node.step) {
      if (needsComma) writer.write(', ');
      var sv = visitVoid(s);
      writer.write(sv.code);
      needsComma = true;
    }
    writer.write(') ');
    _pushBlock(/*reentrant:*/true);
    node.body.visit(this);
    _popBlock();
    _popBlock();
    return false;
  }

  bool visitForInStatement(ForInStatement node) {
    // TODO(jimhug): visitValue and other cleanups here.
    var itemType = method.resolveType(node.item.type, false);
    var itemName = node.item.name.name;
    var list = node.list.visit(this);
    _pushBlock(/*reentrant:*/true);
    // TODO(jimhug): Check that itemType matches list members...
    var item = _scope.create(itemName, itemType, node.item.name.span);
    Value listVar = list;
    if (list.needsTemp) {
      listVar = _scope.create('\$list', list.type, null);
      writer.writeln('var ${listVar.code} = ${list.code};');
    }

    // Special path for list for readability and perf optimization.
    if (list.type.isList) {
      var tmpi = _scope.create('\$i', world.numType, null);
      writer.enterBlock('for (var ${tmpi.code} = 0;' +
          '${tmpi.code} < ${listVar.code}.length; ${tmpi.code}++) {');
      var value = listVar.invoke(this, '\$index', node.list,
          new Arguments(null, [tmpi]));
      writer.writeln('var ${item.code} = ${value.code};');
    } else {
      _pushBlock();
      var iterator = list.invoke(this, 'iterator', node.list, Arguments.EMPTY);
      var tmpi = _scope.create('\$i', iterator.type, null);

      var hasNext = tmpi.invoke(this, 'hasNext', node.list, Arguments.EMPTY);
      var next = tmpi.invoke(this, 'next', node.list, Arguments.EMPTY);

      writer.enterBlock(
        'for (var ${tmpi.code} = ${iterator.code}; ${hasNext.code}; ) {');
      writer.writeln('var ${item.code} = ${next.code};');
    }

    visitStatementsInBlock(node.body);
    writer.exitBlock('}');
    _popBlock();
    return false;
  }

  void _genToDartException(String ex, Node node) {
    var types = const [
      'NullPointerException', 'ObjectNotClosureException',
      'NoSuchMethodException', 'StackOverflowException'];
    // TODO(jimhug): This is an egregious hack to get some toStrings called.
    var target = new Value(world.varType, 'this', node.span);
    for (var name in types) {
      world.corelib.types[name].markUsed();
      world.corelib.types[name].members['toString'].invoke(
          this, node, target, Arguments.EMPTY);
    }
    writer.writeln('$ex = \$toDartException($ex);');
    world.gen.corejs.useToDartException = true;
  }

  bool visitTryStatement(TryStatement node) {
    writer.enterBlock('try {');
    _pushBlock();
    visitStatementsInBlock(node.body);
    _popBlock();

    if (node.catches.length == 1) {
      // Handle a single catch. We can generate simple code here compared to the
      // multiple catch, such as no extra temp or if-else-if chain.
      var catch_ = node.catches[0];
      _pushBlock();
      var ex = _scope.declare(catch_.exception);
      _scope.rethrow = ex;
      writer.nextBlock('} catch (${ex.code}) {');
      if (catch_.trace != null) {
        var trace = _scope.declare(catch_.trace);
        writer.writeln('var ${trace.code} = \$stackTraceOf(${ex.code});');
        world.gen.corejs.useStackTraceOf = true;
      }
      _genToDartException(ex.code, node);

      if (!ex.type.isVar) {
        var test = ex.instanceOf(this, ex.type, catch_.exception.span,
            isTrue:false, forceCheck:true);
        writer.writeln('if (${test.code}) throw ${ex.code};');
      }
      visitStatementsInBlock(node.catches[0].body);
      _popBlock();
    } else if (node.catches.length > 0) {
      // Handle more than one catch
      _pushBlock();
      var ex = _scope.create('\$ex', world.varType, null);
      _scope.rethrow = ex;
      writer.nextBlock('} catch (${ex.code}) {');
      var trace = null;
      if (node.catches.some((c) => c.trace != null)) {
        trace = _scope.create('\$trace', world.varType, null);
        writer.writeln('var ${trace.code} = \$stackTraceOf(${ex.code});');
        world.gen.corejs.useStackTraceOf = true;
      }
      _genToDartException(ex.code, node);

      // We need a rethrow unless we encounter a "var" catch
      bool needsRethrow = true;

      for (int i = 0; i < node.catches.length; i++) {
        var catch_ = node.catches[i];

        _pushBlock();
        var tmp = _scope.declare(catch_.exception);
        if (!tmp.type.isVar) {
          var test = ex.instanceOf(this, tmp.type, catch_.exception.span,
              isTrue:true, forceCheck:true);
          if (i == 0) {
            writer.enterBlock('if (${test.code}) {');
          } else {
            writer.nextBlock('} else if (${test.code}) {');
          }
        } else if (i > 0) {
          writer.nextBlock('} else {');
        }

        writer.writeln('var ${tmp.code} = ${ex.code};');
        if (catch_.trace != null) {
          // TODO(jmesserly): ensure this is the right type
          var tmptrace = _scope.declare(catch_.trace);
          writer.writeln('var ${tmptrace.code} = ${trace.code};');
        }

        visitStatementsInBlock(catch_.body);
        _popBlock();

        if (tmp.type.isVar) {
          // We matched this for sure; no need to keep going
          if (i + 1 < node.catches.length) {
            world.warning('Unreachable catch clause', node.catches[i + 1]);
          }
          if (i > 0) {
            // Close the else block
            writer.exitBlock('}');
          }
          needsRethrow = false;
          break;
        }
      }

      if (needsRethrow) {
        // If we didn't have a "catch (var e)", generate a rethrow
        writer.nextBlock('} else {');
        writer.writeln('throw ${ex.code};');
        writer.exitBlock('}');
      }

      _popBlock();
    }

    if (node.finallyBlock != null) {
      writer.nextBlock('} finally {');
      _pushBlock();
      visitStatementsInBlock(node.finallyBlock);
      _popBlock();
    }

    // Close the try-catch-finally
    writer.exitBlock('}');
    // TODO(efortuna): This could be more precise by combining all the different
    // paths here.  -i.e. if there is a finally block with a return at the end
    // then this can return true, similarly if all blocks have a return at the
    // end then the same holds.
    return false;
  }

  bool visitSwitchStatement(SwitchStatement node) {
    var test = visitValue(node.test);
    writer.enterBlock('switch (${test.code}) {');
    for (var case_ in node.cases) {
      if (case_.label != null) {
        world.error('unimplemented: labeled case statement', case_.span);
      }
      _pushBlock();
      for (int i=0; i < case_.cases.length; i++) {
        var expr = case_.cases[i];
        if (expr == null) {
          // Default can only be the last case.
          if (i < case_.cases.length - 1) {
            world.error('default clause must be the last case', case_.span);
          }
          writer.writeln('default:');
        } else {
          var value = visitValue(expr);
          writer.writeln('case ${value.code}:');
        }
      }
      writer.enterBlock('');
      bool caseExits = _visitAllStatements(case_.statements, false);

      if (case_ != node.cases[node.cases.length - 1] && !caseExits) {
        var span = case_.statements[case_.statements.length - 1].span;
        writer.writeln('\$throw(new FallThroughError());');
        world.gen.corejs.useThrow = true;
      }
      writer.exitBlock('');
      _popBlock();
    }
    writer.exitBlock('}');
    // TODO(efortuna): When we are passing more information back about
    // control flow by returning something other than bool, return true for the
    // cases where every branch of the switch statement ends with a return
    // statement.
    return false;
  }

  bool _visitAllStatements(statementList, exits) {
    for (int i = 0; i < statementList.length; i++) {
      var stmt = statementList[i];
      exits = stmt.visit(this);
      //TODO(efortuna): fix this so you only get one error if you have "return;
      //a; b; c;"
      if (stmt != statementList[statementList.length - 1] && exits) {
        world.warning('unreachable code', statementList[i + 1].span);
      }
    }
    return exits;
  }

  bool visitBlockStatement(BlockStatement node) {
    _pushBlock();
    writer.enterBlock('{');
    var exits = _visitAllStatements(node.body, false);
    writer.exitBlock('}');
    _popBlock();
    return exits;
  }

  bool visitLabeledStatement(LabeledStatement node) {
    writer.writeln('${node.name.name}:');
    node.body.visit(this);
    return false;
  }

  bool visitExpressionStatement(ExpressionStatement node) {
    if (node.body is VarExpression || node.body is ThisExpression) {
      // TODO(jmesserly): this is a "warning" but not a "type warning",
      // Is that okay? We have a similar issue around unreachable code warnings.
      world.warning('variable used as statement', node.span);
    }
    var value = visitVoid(node.body);
    writer.writeln('${value.code};');
    return false;
  }

  bool visitEmptyStatement(EmptyStatement node) {
    writer.writeln(';');
    return false;
  }

  _checkNonStatic(Node node) {
    if (isStatic) {
      world.warning('not allowed in static method', node.span);
    }
  }

  _makeSuperValue(Node node) {
    var parentType = method.declaringType.parent;
    _checkNonStatic(node);
    if (parentType == null) {
      world.error('no super class', node.span);
    }
    // TODO(jimhug): Replace with SuperValue.
    var ret = new Value(parentType, 'this', node.span, false);
    ret.isSuper = true;
    return ret;
  }

  _getOutermostMethod() {
    var result = this;
    while (result.enclosingMethod != null) {
      result = result.enclosingMethod;
    }
    return result;
  }


  // TODO(jimhug): Share code better with _makeThisValue.
  String _makeThisCode() {
    if (enclosingMethod != null) {
      _getOutermostMethod().needsThis = true;
      return '\$this';
    } else {
      return 'this';
    }
  }

  /**
   * Creates a reference to the enclosing type ('this') that can be used within
   * closures.
   */
  Value _makeThisValue(Node node) {
    if (enclosingMethod != null) {
      var outermostMethod = _getOutermostMethod();
      outermostMethod._checkNonStatic(node);
      outermostMethod.needsThis = true;
      return new Value(outermostMethod.method.declaringType, '\$this',
        node != null ? node.span : null, /*needsTemp:*/false);
    } else {
      _checkNonStatic(node);
      return new Value(method.declaringType, 'this', node != null ? node.span : null,
        /*needsTemp:*/false);
    }
  }

  // ******************* Expressions *******************
  visitLambdaExpression(LambdaExpression node) {
    var name = '';
    if (node.func.name != null) {
      name = world.toJsIdentifier(node.func.name.name);
    }

    var meth = _makeLambdaMethod(name, node.func);

    var w = new CodeWriter();
    meth.generator.writeDefinition(w, node);

    return new Value(meth.functionType, w.text, node.span);
  }

  visitCallExpression(CallExpression node) {
    var target;
    var position = node.target;
    var name = '\$call';
    if (node.target is DotExpression) {
      DotExpression dot = node.target;
      target = dot.self.visit(this);
      name = dot.name.name;
      position = dot.name;
    } else if (node.target is VarExpression) {
      VarExpression varExpr = node.target;
      name = varExpr.name.name;
     // First check in block scopes.
      target = _scope.lookup(name);
      if (target != null) {
        return target.invoke(this, '\$call', node, _makeArgs(node.arguments));
      }

      target = _makeThisOrType(varExpr.span);
      return target.invoke(this, name, node, _makeArgs(node.arguments));
    } else {
      target = node.target.visit(this);
    }

    return target.invoke(this, name, position, _makeArgs(node.arguments));
  }

  visitIndexExpression(IndexExpression node) {
    var target = visitValue(node.target);
    var index = visitValue(node.index);
    return target.invoke(this, '\$index', node, new Arguments(null, [index]));
  }

  visitBinaryExpression(BinaryExpression node) {
    final kind = node.op.kind;
    // TODO(jimhug): Ensure these have same semantics as JS!
    if (kind == TokenKind.AND || kind == TokenKind.OR) {
      var x = visitTypedValue(node.x, world.nonNullBool);
      var y = visitTypedValue(node.y, world.nonNullBool);
      final code = '${x.code} ${node.op} ${y.code}';
      if (x.isConst && y.isConst) {
        var value = (kind == TokenKind.AND)
            ? x.actualValue && y.actualValue : x.actualValue || y.actualValue;
        return new EvaluatedValue(world.nonNullBool, value, '$value',
            node.span);
      }
      return new Value(world.nonNullBool, code, node.span);
    } else if (kind == TokenKind.EQ_STRICT || kind == TokenKind.NE_STRICT) {
      var x = visitValue(node.x);
      var y = visitValue(node.y);
      if (x.isConst && y.isConst) {
        var value = kind == TokenKind.EQ_STRICT
            // Note: it is ok to use == and not === here since all of these
            // constant comparisons are applied to doubles, bool, or strings.
            // We need it for the compile-time evaluator because
            // (9).toDouble() === 9.0 is false in dartvm.
            ? x.actualValue == y.actualValue : x.actualValue != y.actualValue;
        return new EvaluatedValue(world.nonNullBool, value, "$value",
            node.span);
      }
      if (x.code == 'null' || y.code == 'null') {
        // Switching to == ensures that null and undefined are interchangable.
        final op = node.op.toString().substring(0,2);
        return new Value(world.nonNullBool, '${x.code} $op ${y.code}',
            node.span);
      } else {
        // TODO(jimhug): Resolve issue with undefined and null here.
        return new Value(world.nonNullBool, '${x.code} ${node.op} ${y.code}',
          node.span);
      }
    }

    final assignKind = TokenKind.kindFromAssign(node.op.kind);
    if (assignKind == -1) {
      final x = visitValue(node.x);
      final y = visitValue(node.y);
      var name = TokenKind.binaryMethodName(node.op.kind);
      if (node.op.kind == TokenKind.NE) {
        name = '\$ne';
      }
      if (name == null) {
        world.internalError('unimplemented binary op ${node.op}', node.span);
        return;
      }
      return x.invoke(this, name, node, new Arguments(null, [y]));
    } else {
      return _visitAssign(assignKind, node.x, node.y, node, null);
    }
  }

  /**
   * Visits an assignment expression.
   * Note: captureOriginal can optionally capture the original value of the
   * left side. This is used by postfix expressions to ensure they return the
   * original value, before it has been modified.
   */
  _visitAssign(int kind, Expression xn, Expression yn, Node position,
      Value captureOriginal(Value right)) {

    if (captureOriginal == null) {
      captureOriginal = (x) => x;
    }

    // TODO(jimhug): The usual battle with making assign impl not look ugly.
    if (xn is VarExpression) {
      return _visitVarAssign(kind, xn, yn, position, captureOriginal);
    } else if (xn is IndexExpression) {
      return _visitIndexAssign(kind, xn, yn, position, captureOriginal);
    } else if (xn is DotExpression) {
      return _visitDotAssign(kind, xn, yn, position, captureOriginal);
    } else {
      world.error('illegal lhs', position.span);
    }
  }

  // TODO(jmesserly): it'd be nice if we didn't have to deal directly with
  // MemberSets here and in visitVarExpression.
  _visitVarAssign(int kind, VarExpression xn, Expression yn, Node position,
      Value captureOriginal(Value right)) {
    final name = xn.name.name;

    // First check in block scopes.
    var x = _scope.lookup(name);
    var y = visitValue(yn);

    if (x == null) {
      // Look for a setter in the class
      var members = method.declaringType.resolveMember(name);
      if (members != null) {
        x = _makeThisOrType(position.span);
        if (kind == 0) {
          return x.set_(this, name, position, y);
        } else if (!members.treatAsField || members.containsMethods) {
          var right = x.get_(this, name, position);
          right = captureOriginal(right);
          y = right.invoke(this, TokenKind.binaryMethodName(kind),
              position, new Arguments(null, [y]));
          return x.set_(this, name, position, y);
        } else {
          x = x.get_(this, name, position);
        }
      } else {
        // Look for a top-level setter
        final member = library.lookup(name, xn.name.span);
        if (member == null) {
          world.warning('can not resolve ${name}', xn.span);
          return _makeMissingValue(name);
        }
        members = new MemberSet(member);
        // If we can't treat it as a field, generate a setter call.
        // Also make sure we dont't try to set a method.
        if (!members.treatAsField || members.containsMethods) {
          if (kind != 0) {
            var right = members._get(this, position, x);
            right = captureOriginal(right);
            y = right.invoke(this, TokenKind.binaryMethodName(kind),
                position, new Arguments(null, [y]));
          }
          return members._set(this, position, x, y);
        } else {
          x = members._get(this, position, x);
        }
      }

      // Otherwise treat it as a field.
      // This makes for nicer code in the $op= case
    }

    // TODO(jimhug): Needs checks for final and other rules to enforce.
    y = y.convertTo(this, x.type, yn);

    if (kind == 0) {
      x = captureOriginal(x);
      return new Value(y.type, '${x.code} = ${y.code}', position.span);
    } else if (x.type.isNum && y.type.isNum && (kind != TokenKind.TRUNCDIV)) {
      // Process everything but ~/ , which has no equivalent JS operator
      x = captureOriginal(x);
      // Very localized optimization for numbers!
      final op = TokenKind.kindToString(kind);
      return new Value(y.type, '${x.code} $op= ${y.code}', position.span);
    } else {
      var right = x;
      right = captureOriginal(right);
      y = right.invoke(this, TokenKind.binaryMethodName(kind),
        position, new Arguments(null, [y]));
      return new Value(y.type, '${x.code} = ${y.code}', position.span);
    }
  }

  _visitIndexAssign(int kind, IndexExpression xn, Expression yn, Node position,
      Value captureOriginal(Value right)) {
    var target = visitValue(xn.target);
    var index = visitValue(xn.index);
    var y = visitValue(yn);

    var tmptarget = target;
    var tmpindex = index;
    if (kind != 0) {
      tmptarget = getTemp(target);
      tmpindex = getTemp(index);
      index = assignTemp(tmpindex, index);
      var right = tmptarget.invoke(this, '\$index',
          position, new Arguments(null, [tmpindex]));
      right = captureOriginal(right);
      y = right.invoke(this, TokenKind.binaryMethodName(kind),
          position, new Arguments(null, [y]));
    }
    var ret = assignTemp(tmptarget, target).invoke(this, '\$setindex',
        position, new Arguments(null, [index, y]));
    if (tmptarget != target) freeTemp(tmptarget);
    if (tmpindex != index) freeTemp(tmpindex);
    return ret;
  }

  _visitDotAssign(int kind, DotExpression xn, Expression yn, Node position,
      Value captureOriginal(Value right)) {

    // This is not visitValue because types are assignable through .
    var target = xn.self.visit(this);

    var y = visitValue(yn);
    var tmptarget = target;
    if (kind != 0) {
      tmptarget = getTemp(target);
      var right = tmptarget.get_(this, xn.name.name, xn.name);
      right = captureOriginal(right);
      y = right.invoke(this, TokenKind.binaryMethodName(kind),
          position, new Arguments(null, [y]));
    }
    var ret = assignTemp(tmptarget, target).set_(
        this, xn.name.name, xn.name, y);
    if (tmptarget != target) freeTemp(tmptarget);
    return ret;
  }

  visitUnaryExpression(UnaryExpression node) {
    var value = visitValue(node.self);
    switch (node.op.kind) {
      case TokenKind.INCR:
      case TokenKind.DECR:
        if (value.type.isNum) {
          return new Value(value.type, '${node.op}${value.code}', node.span);
        } else {
          // ++x becomes x += 1
          // --x becomes x -= 1
          // TODO(jimhug): Confirm that --x becomes x -= 1 as it is in VM.
          var kind = (TokenKind.INCR == node.op.kind ?
              TokenKind.ADD : TokenKind.SUB);
          var operand = new LiteralExpression(1,
            new TypeReference(node.span, world.numType), '1', node.span);

          return _visitAssign(kind, node.self, operand, node, null);
        }
      case TokenKind.NOT:
        // TODO(jimhug): Issue #359 seeks to clarify this behavior.
        if (value.type.isBool && value.isConst) {
          var newVal = !value.actualValue;
          return new EvaluatedValue(value.type, newVal, '${newVal}', node.span);
        } else {
          var newVal = value.convertTo(this, world.nonNullBool, node);
          return new Value(newVal.type, '!${newVal.code}', node.span);
        }

      case TokenKind.ADD:
        // TODO(jimhug): Issue #359 seeks to clarify this behavior.
        return value.convertTo(this, world.numType, node);

      case TokenKind.SUB:
      case TokenKind.BIT_NOT:
        if (node.op.kind == TokenKind.BIT_NOT) {
          return value.invoke(this, '\$bit_not', node, Arguments.EMPTY);
        } else if (node.op.kind == TokenKind.SUB) {
          return value.invoke(this, '\$negate', node, Arguments.EMPTY);
        } else {
          world.internalError('unimplemented: unary ${node.op}',
            node.span);
        }
      default:
        world.internalError('unimplemented: ${node.op}', node.span);
    }
  }

  visitPostfixExpression(PostfixExpression node, [bool isVoid = false]) {
    var value = visitValue(node.body);
    if (value.type.isNum) {
      return new Value(value.type, '${value.code}${node.op}', node.span);
    }

    // x++ is equivalent to (t = x, x = t + 1, t), where we capture all temps
    // needed to evaluate x so we're not evaluating multiple times. Likewise,
    // x-- is equivalent to (t = x, x = t - 1, t).
    var kind = (TokenKind.INCR == node.op.kind) ? TokenKind.ADD : TokenKind.SUB;
    var operand = new LiteralExpression(1,
      new TypeReference(node.span, world.numType), '1', node.span);

    // Use _visitAssign to do most of the work, but save the right side in a
    // temporary variable if needed.
    // TODO(jmesserly): I don't like passing function args like this, but the
    // alternative is duplicating most of the _visitAssign logic. Needs cleanup.
    var tmpleft = null, left = null;
    var ret = _visitAssign(kind, node.body, operand, node, (l) {
      if (isVoid) {
        // No need for a temp if we're throwing away the result.
        return l;
      } else {
        // We always need a temp to capture the old value
        left = l;
        tmpleft = forceTemp(l);
        return assignTemp(tmpleft, left);
      }
    });

    if (tmpleft != null) {
      ret = new Value(ret.type, "(${ret.code}, ${tmpleft.code})", node.span);
    }
    if (tmpleft != left) {
      freeTemp(tmpleft);
    }
    return ret;
  }

  visitNewExpression(NewExpression node) {
    var typeRef = node.type;

    var constructorName = '';
    if (node.name != null) {
      constructorName = node.name.name;
    }

    // Named constructors and library prefixes, oh my!
    // At last, we can collapse the ambiguous wave function...
    if (constructorName == '' && typeRef is !GenericTypeReference &&
        typeRef.names != null) {

      // Pull off the last name from the type, guess it's the constructor name.
      var names = new List.from(typeRef.names);
      constructorName = names.removeLast().name;
      if (names.length == 0) names = null;

      typeRef = new NameTypeReference(
          typeRef.isFinal, typeRef.name, names, typeRef.span);
    }

    var type = method.resolveType(typeRef, true);
    if (type.isTop) {
      type = type.library.findTypeByName(constructorName);
      constructorName = '';
    }

    var m = type.getConstructor(constructorName);
    if (m == null) {
      var name = type.jsname;
      if (type.isVar) {
        name = typeRef.name.name;
      }
      world.error('no matching constructor for $name', node.span);
      return _makeMissingValue(name);
    }

    if (node.isConst) {
      if (!m.isConst) {
        world.error('can\'t use const on a non-const constructor', node.span);
      }
      for (var arg in node.arguments) {
        if (!visitValue(arg.value).isConst) {
          world.error('const constructor expects const arguments', arg.span);
        }
      }
    }
    return m.invoke(this, node, null, _makeArgs(node.arguments));
  }

  visitListExpression(ListExpression node) {
    // TODO(jimhug): Use node.type or other type inference here.
    var argsCode = [];
    var argValues = [];
    for (var item in node.values) {
      var arg = visitValue(item);
      argValues.add(arg);
      if (node.isConst) {
        if (!arg.isConst) {
          world.error('const list can only contain const values', item.span);
          argsCode.add(arg.code);
        } else {
          argsCode.add(arg.canonicalCode);
        }
      } else {
        argsCode.add(arg.code);
      }
    }

    world.coreimpl.types['ListFactory'].markUsed();

    final code = '[${Strings.join(argsCode, ", ")}]';
    var value = new Value(world.listType, code, node.span);
    if (node.isConst) {
      final immutableList = world.coreimpl.types['ImmutableList'];
      final immutableListCtor = immutableList.getConstructor('from');
      final result = immutableListCtor.invoke(
          this, node, null, new Arguments(null, [value]));
      value = world.gen.globalForConst(
          new ConstListValue(immutableList, argValues, 'const $code',
              result.code, node.span),
          argValues);
    }
    return value;
  }


  visitMapExpression(MapExpression node) {
    // Ensure that Map factory is intialized - pseudo-call needed members.
    var mapImplType = world.gen.useMapFactory();

    var argValues = [];
    var argsCode = [];
    for (int i = 0; i < node.items.length; i += 2) {
      // TODO(jimhug): Use node.type or other type inference here.
      // TODO(jimhug): Would be nice to allow arbitrary keys here.
      var key = visitTypedValue(node.items[i], world.stringType);
      final valueItem = node.items[i+1];
      var value = visitValue(valueItem);
      argValues.add(key);
      argValues.add(value);

      if (node.isConst) {
        if (!key.isConst || !value.isConst) {
          world.error('const map can only contain const values',
              valueItem.span);
          argsCode.add(key.code);
          argsCode.add(value.code);
        } else {
          argsCode.add(key.canonicalCode);
          argsCode.add(value.canonicalCode);
        }
      } else {
        argsCode.add(key.code);
        argsCode.add(value.code);
      }
    }
    var argList = '[${Strings.join(argsCode, ", ")}]';
    final code = '\$map($argList)';
    if (node.isConst) {
      final immutableMap = world.coreimpl.types['ImmutableMap'];
      final immutableMapCtor = immutableMap.getConstructor('');
      final argsValue = new Value(world.listType, argList, node.span);
      final result = immutableMapCtor.invoke(
          this, node, null, new Arguments(null, [argsValue]));
      final value = new ConstMapValue(
          immutableMap, argValues, code, result.code, node.span);
      return world.gen.globalForConst(value, argValues);
    }
    return new Value(mapImplType, code, node.span);
  }

  visitConditionalExpression(ConditionalExpression node) {
    var test = visitBool(node.test);
    var trueBranch = visitValue(node.trueBranch);
    var falseBranch = visitValue(node.falseBranch);

    var code = '${test.code} ? ${trueBranch.code} : ${falseBranch.code}';
    return new Value(Type.union(trueBranch.type, falseBranch.type), code,
      node.span);
  }

  visitIsExpression(IsExpression node) {
    var value = visitValue(node.x);
    var type = method.resolveType(node.type, false);
    return value.instanceOf(this, type, node.span, node.isTrue);
  }

  visitParenExpression(ParenExpression node) {
    var body = visitValue(node.body);
    if (body.isConst) {
      return new EvaluatedValue(body.type, body.actualValue,
          '(${body.canonicalCode})', node.span);
    }
    return new Value(body.type, '(${body.code})', node.span);
  }

  visitDotExpression(DotExpression node) {
    // Types are legal targets of .
    var target = node.self.visit(this);
    return target.get_(this, node.name.name, node.name);
  }

  visitVarExpression(VarExpression node) {
    final name = node.name.name;

    // First check in block scopes.
    var ret = _scope.lookup(name);
    if (ret != null) return ret;

    return _makeThisOrType(node.span).get_(this, name, node);
  }

  _makeMissingValue(String name) {
    // TODO(jimhug): Probably goes away to be fully replaced by noSuchMethod
    return new Value(world.varType, '$name()/*NotFound*/', null);
  }

  _makeThisOrType(SourceSpan span) {
    return new BareValue(this, _getOutermostMethod(), span);
  }

  visitThisExpression(ThisExpression node) {
    return _makeThisValue(node);
  }

  visitSuperExpression(SuperExpression node) {
    return _makeSuperValue(node);
  }

  visitNullExpression(NullExpression node) {
    // TODO(jimhug): should be passing node.span
    // TODO(jimhug): Can we do better than var for the type?
    return new EvaluatedValue(world.varType, null, 'null', null);
  }

  visitLiteralExpression(LiteralExpression node) {
    // All Literal types are filled in at parse time, so no need to resolve.
    var type = node.type.type;
    assert(type != null);

    if (node.value is List) {
      var items = [];
      for (var item in node.value) {
        var val = visitValue(item);
        val.invoke(this, 'toString', item, Arguments.EMPTY);

        // TODO(jimhug): Ensure this solves all precedence problems.
        var code = val.code;
        if (item is BinaryExpression || item is ConditionalExpression) {
          code = '(${code})';
        }
        items.add(code);
      }
      return new Value(type, '(${Strings.join(items, " + ")})', node.span);
    }

    var text = node.text;
    // TODO(jimhug): Confirm that only strings need possible translation
    if (type.isString) {
      if (text.startsWith('@')) {
        text = _escapeString(parseStringLiteral(text));
        text = '"$text"';
      } else if (isMultilineString(text)) {
        // convert multi-line strings into single-line
        text = parseStringLiteral(text);
        // TODO(jimhug): What about \r?
        text = text.replaceAll('\n', '\\n');
        text = text.replaceAll('"', '\\"');
        text = '"$text"';
      }
      if (text !== node.text) {
        node.value = text;
        node.text = text;
      }
    }

    // TODO(jimhug): Should pass node.span - but that breaks something...
    return new EvaluatedValue(type, node.value, node.text, null);
  }
}


// TODO(jmesserly): move this into its own file?
class Arguments {
  static Arguments _empty;
  static Arguments get EMPTY() {
    if (_empty == null) {
      _empty = new Arguments(null, []);
    }
    return _empty;
  }

  List<Value> values;
  List<ArgumentNode> nodes;
  int _bareCount;

  Arguments(this.nodes, this.values);

  /** Constructs a bare list of arguments. */
  factory Arguments.bare(int arity) {
    var values = [];
    for (int i = 0; i < arity; i++) {
      // TODO(jimhug): Need a firm rule about null SourceSpans are allowed.
      values.add(new Value(world.varType, '\$$i', null, /*needsTemp:*/false));
    }
    return new Arguments(null, values);
  }

  int get nameCount() => length - bareCount;
  bool get hasNames() => bareCount < length;

  int get length() => values.length;

  String getName(int i) => nodes[i].label.name;

  int getIndexOfName(String name) {
    for (int i = bareCount; i < length; i++) {
      if (getName(i) == name) {
        return i;
      }
    }
    return -1;
  }

  Value getValue(String name) {
    int i = getIndexOfName(name);
    return i >= 0 ? values[i] : null;
  }

  int get bareCount() {
    if (_bareCount == null) {
      _bareCount = length;
      if (nodes != null) {
        for (int i=0; i < nodes.length; i++) {
          if (nodes[i].label != null) {
            _bareCount = i;
            break;
          }
        }
      }
    }
    return _bareCount;
  }

  String getCode() {
    var argsCode = [];
    for (int i = 0; i < length; i++) {
      argsCode.add(values[i].code);
    }
    removeTrailingNulls(argsCode);
    return Strings.join(argsCode, ", ");
  }

  List<String> getBareCodes() {
    var result = [];
    for (int i = 0; i < bareCount; i++) {
      result.add(values[i].code);
    }
    return result;
  }

  List<String> getNamedCodes() {
    var result = [];
    for (int i = bareCount; i < length; i++) {
      result.add(values[i].code);
    }
    return result;
  }

  static removeTrailingNulls(List<Value> argsCode) {
    // We simplify calls with null defaults by relying on JS and our
    // choice to make undefined === null for Dart generated code. This helps
    // and ensures correct defaults values for native calls.
    while (argsCode.length > 0 && argsCode.last() == 'null') {
      argsCode.removeLast();
    }
  }

  /** Gets the named arguments. */
  List<String> getNames() {
    var names = [];
    for (int i = bareCount; i < length; i++) {
      names.add(getName(i));
    }
    return names;
  }

  /** Gets the argument names used in a call stub; uses $0 $1 for bare args. */
  Arguments toCallStubArgs() {
    var result = [];
    for (int i = 0; i < bareCount; i++) {
      result.add(new Value(world.varType, '\$$i', null, /*needsTemp:*/false));
    }
    for (int i = bareCount; i < length; i++) {
      var name = getName(i);
      if (name == null) name = '\$$i';
      result.add(new Value(world.varType, name, null, /*needsTemp:*/false));
    }
    return new Arguments(nodes, result);
  }
}
