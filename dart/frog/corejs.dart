// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Generates JS helpers for dart:core. This used to be in a file "core.js".
 * Having them in Dart code means we can easily control which are generated.
 */
// TODO(jmesserly): one idea to make this cleaner: put these as private "native"
// methods somewhere in a library that we import. This would be rather elegant
// because they'd get the right name collision behavior, conversions,
// include-if-used, etc for free. Not sure if it's worth doing that.
class CoreJs {
  // These values track if the helper is actually used. If it is we generate it.
  bool useStackTraceOf = false;
  bool useThrow = false;
  bool useGenStub = false;
  bool useAssert = false;
  bool useNotNullBool = false;
  bool useIndex = false;
  bool useSetIndex = false;

  bool useWrap0 = false;
  bool useWrap1 = false;
  bool useIsolates = false;

  /** An experimental toString implementation. Currently unused. */
  bool useToString = false;

  // These helpers had to switch to a new pattern, because they can be generated
  // after everything else.
  bool _generatedTypeNameOf = false;
  bool _generatedDynamicProto = false;
  bool _generatedInherits = false;

  Map<String, String> _usedOperators;

  CodeWriter writer;

  CoreJs(): _usedOperators = {}, writer = new CodeWriter();

  /**
   * Generates the special operator method, e.g. $add.
   * We want to do $add(x, y) instead of x.$add(y) so it doesn't box.
   * Same idea for the other methods.
   */
  void useOperator(String name) {
    if (_usedOperators[name] != null) return;

    var code;
    switch (name) {
      case ':ne':
        code = _NE_FUNCTION;
        break;

      case ':eq':
        code = _EQ_FUNCTION;
        break;

      case ':bit_not':
        code = _BIT_NOT_FUNCTION;
        break;

      case ':negate':
        code = _NEGATE_FUNCTION;
        break;

      case ':add':
        code = _ADD_FUNCTION;
        break;

      case ':truncdiv':
        useThrow = true;
        code = _TRUNCDIV_FUNCTION;
        break;

      case ':mod':
        code = _MOD_FUNCTION;
        break;

      default:
        // All of the other helpers are generated the same way
        var op = TokenKind.rawOperatorFromMethod(name);
        var jsname = world.toJsIdentifier(name);
        code = _otherOperator(jsname, op);
        break;
    }

    _usedOperators[name] = code;
  }

  // NOTE: some helpers can't be generated when we generate corelib,
  // because we don't discover that we need them until later.
  // Generate on-demand instead
  void ensureDynamicProto() {
    if (_generatedDynamicProto) return;
    _generatedDynamicProto = true;
    ensureTypeNameOf();
    writer.writeln(_DYNAMIC_FUNCTION);
  }

  void ensureTypeNameOf() {
    if (_generatedTypeNameOf) return;
    _generatedTypeNameOf = true;
    writer.writeln(_TYPE_NAME_OF_FUNCTION);
  }

  /** Generates the $inherits function when it's first used. */
  ensureInheritsHelper() {
    if (_generatedInherits) return;
    _generatedInherits = true;
    writer.writeln(_INHERITS_FUNCTION);
  }

  void generate(CodeWriter w) {
    // Write any stuff we had queued up, then replace our writer with the one
    // in WorldGenerator so anything we discover that we need later on will be
    // generated on-demand.
    w.write(writer.text);
    writer = w;

    if (useGenStub) {
      useThrow = true;
      w.writeln(_GENSTUB_FUNCTION);
    }

    if (useStackTraceOf) {
      w.writeln(_STACKTRACEOF_FUNCTION);
    }

    if (useNotNullBool) {
      useThrow = true;
      w.writeln(_NOTNULL_BOOL_FUNCTION);
    }

    if (useThrow) {
      w.writeln(_THROW_FUNCTION);
    }

    if (useToString) {
      w.writeln(_TOSTRING_FUNCTION);
    }

    if (useIndex) {
      w.writeln(_INDEX_OPERATORS);
    }

    if (useSetIndex) {
      w.writeln(_SETINDEX_OPERATORS);
    }

    if (useIsolates) {
      if (useWrap0) {
        w.writeln(_WRAP_CALL0_FUNCTION);
      }
      if (useWrap1) {
        w.writeln(_WRAP_CALL1_FUNCTION);
      }
      w.writeln(_ISOLATE_INIT_CODE);
    } else {
      if (useWrap0) {
        w.writeln(_EMPTY_WRAP_CALL0_FUNCTION);
      }
      if (useWrap1) {
        w.writeln(_EMPTY_WRAP_CALL1_FUNCTION);
      }
    }

    // Write operator helpers
    for (var opImpl in orderValuesByKeys(_usedOperators)) {
      w.writeln(opImpl);
    }
  }
}


/** Snippet for `$ne`. */
final String _NE_FUNCTION = @"""
function $ne(x, y) {
  if (x == null) return y != null;
  return (typeof(x) == 'number' && typeof(y) == 'number') ||
         (typeof(x) == 'boolean' && typeof(y) == 'boolean') ||
         (typeof(x) == 'string' && typeof(y) == 'string')
    ? x != y : !x.$eq(y);
}""";

/** Snippet for `$eq`. */
final String _EQ_FUNCTION = @"""
function $eq(x, y) {
  if (x == null) return y == null;
  return (typeof(x) == 'number' && typeof(y) == 'number') ||
         (typeof(x) == 'boolean' && typeof(y) == 'boolean') ||
         (typeof(x) == 'string' && typeof(y) == 'string')
    ? x == y : x.$eq(y);
}
// TODO(jimhug): Should this or should it not match equals?
Object.prototype.$eq = function(other) { return this === other; }""";

/** Snippet for `$bit_not`. */
final String _BIT_NOT_FUNCTION = @"""
function $bit_not(x) {
  return (typeof(x) == 'number') ? ~x : x.$bit_not();
}""";

/** Snippet for `$negate`. */
final String _NEGATE_FUNCTION = @"""
function $negate(x) {
  return (typeof(x) == 'number') ? -x : x.$negate();
}""";

/** Snippet for `$add`. This relies on JS's string "+" to match Dart's. */
final String _ADD_FUNCTION = @"""
function $add(x, y) {
  return ((typeof(x) == 'number' && typeof(y) == 'number') ||
          (typeof(x) == 'string'))
    ? x + y : x.$add(y);
}""";

/** Snippet for `$truncdiv`. This uses `$throw`. */
final String _TRUNCDIV_FUNCTION = @"""
function $truncdiv(x, y) {
  if (typeof(x) == 'number' && typeof(y) == 'number') {
    if (y == 0) $throw(new IntegerDivisionByZeroException());
    var tmp = x / y;
    return (tmp < 0) ? Math.ceil(tmp) : Math.floor(tmp);
  } else {
    return x.$truncdiv(y);
  }
}""";

/** Snippet for `$mod`. */
final String _MOD_FUNCTION = @"""
function $mod(x, y) {
  if (typeof(x) == 'number' && typeof(y) == 'number') {
    var result = x % y;
    if (result == 0) {
      return 0;  // Make sure we don't return -0.0.
    } else if (result < 0) {
      if (y < 0) {
        return result - y;
      } else {
        return result + y;
      }
    }
    return result;
  } else {
    return x.$mod(y);
  }
}""";

/** Code snippet for all other operators. */
String _otherOperator(String jsname, String op) {
  return """
function $jsname(x, y) {
  return (typeof(x) == 'number' && typeof(y) == 'number')
    ? x $op y : x.$jsname(y);
}""";
}

/**
 * Snippet for `$dynamic`. Usage:
 *    $dynamic(name).SomeTypeName = ... method ...;
 *    $dynamic(name).Object = ... noSuchMethod ...;
 */
final String _DYNAMIC_FUNCTION = @"""
function $dynamic(name) {
  var f = Object.prototype[name];
  if (f && f.methods) return f.methods;

  var methods = {};
  if (f) methods.Object = f;
  function $dynamicBind() {
    // Find the target method
    var method;
    var proto = Object.getPrototypeOf(this);
    var obj = this;
    do {
      method = methods[obj.$typeNameOf()];
      if (method) break;
      obj = Object.getPrototypeOf(obj);
    } while (obj);

    // Patch the prototype, but don't overwrite an existing stub, like
    // the one on Object.prototype.
    if (!proto.hasOwnProperty(name)) proto[name] = method || methods.Object;

    return method.apply(this, Array.prototype.slice.call(arguments));
  };
  $dynamicBind.methods = methods;
  Object.prototype[name] = $dynamicBind;
  return methods;
}""";

/** Snippet for `$typeNameOf`. */
// TODO(sigmund): find a way to make this work on all browsers, including
// checking the typeName on prototype objects (so we can fix dynamic
// dispatching on $varMethod).
final String _TYPE_NAME_OF_FUNCTION = @"""
Object.prototype.$typeNameOf = function() {
  if ((typeof(window) != 'undefined' && window.constructor.name == 'DOMWindow')
      || typeof(process) != 'undefined') { // fast-path for Chrome and Node
    return this.constructor.name;
  }
  var str = Object.prototype.toString.call(this);
  str = str.substring(8, str.length - 1);
  if (str == 'Window') str = 'DOMWindow';
  return str;
}""";

/** Snippet for `$inherits`. */
final String _INHERITS_FUNCTION = @"""
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
}""";

/** Snippet for `$genStub`. */
final String _GENSTUB_FUNCTION = @"""
/**
 * Generates a dynamic call stub for a function.
 * Our goal is to create a stub method like this on-the-fly:
 *   function($0, $1, capture) { return this($0, $1, true, capture); }
 *
 * This stub then replaces the dynamic one on Function, with one that is
 * specialized for that particular function, taking into account its default
 * arguments.
 */
Function.prototype.$genStub = function(argsLength, names) {
  // Fast path: if no named arguments and arg count matches
  if (this.length == argsLength && !names) {
    return this;
  }

  function $throwArgMismatch() {
    // TODO(jmesserly): better error message
    $throw(new ClosureArgumentMismatchException());
  }

  var paramsNamed = this.$optional ? (this.$optional.length / 2) : 0;
  var paramsBare = this.length - paramsNamed;
  var argsNamed = names ? names.length : 0;
  var argsBare = argsLength - argsNamed;

  // Check we got the right number of arguments
  if (argsBare < paramsBare || argsLength > this.length ||
      argsNamed > paramsNamed) {
    return $throwArgMismatch;
  }

  // First, fill in all of the default values
  var p = new Array(paramsBare);
  if (paramsNamed) {
    p = p.concat(this.$optional.slice(paramsNamed));
  }
  // Fill in positional args
  var a = new Array(argsLength);
  for (var i = 0; i < argsBare; i++) {
    p[i] = a[i] = '$' + i;
  }
  // Then overwrite with supplied values for optional args
  var lastParameterIndex;
  var namesInOrder = true;
  for (var i = 0; i < argsNamed; i++) {
    var name = names[i];
    a[i + argsBare] = name;
    var j = this.$optional.indexOf(name);
    if (j < 0 || j >= paramsNamed) {
      return $throwArgMismatch;
    } else if (lastParameterIndex && lastParameterIndex > j) {
      namesInOrder = false;
    }
    p[j + paramsBare] = name;
    lastParameterIndex = j;
  }

  if (this.length == argsLength && namesInOrder) {
    // Fast path #2: named arguments, but they're in order.
    return this;
  }

  // Note: using Function instead of 'eval' to get a clean scope.
  // TODO(jmesserly): evaluate the performance of these stubs.
  var f = 'function(' + a.join(',') + '){return $f(' + p.join(',') + ');}';
  return new Function('$f', 'return ' + f + '').call(null, this);
}""";

/** Snippet for `$stackTraceOf`. */
final String _STACKTRACEOF_FUNCTION = @"""
function $stackTraceOf(e) {
  // TODO(jmesserly): we shouldn't be relying on the e.stack property.
  // Need to mangle it.
  return  (e && e.stack) ? e.stack : null;
}""";

/**
 * Snippet for `$notnull_bool`. This pattern chosen because IE9 does really
 * badly with typeof, and it's still decent on other browsers.
 */
final String _NOTNULL_BOOL_FUNCTION = @"""
function $notnull_bool(test) {
  if (test === true || test === false) return test;
  $throw(new TypeError(test, 'bool'));
}""";

/** Snippet for `$throw`. */
final String _THROW_FUNCTION = @"""
function $throw(e) {
  // If e is not a value, we can use V8's captureStackTrace utility method.
  // TODO(jmesserly): capture the stack trace on other JS engines.
  if (e && (typeof e == 'object') && Error.captureStackTrace) {
    // TODO(jmesserly): this will clobber the e.stack property
    Error.captureStackTrace(e, $throw);
  }
  throw e;
}""";

/** Snippet for `$toString`. */
// TODO(jimhug): Test perf - idea is this speeds up primitives and
//  semi-elegantly handles null - currently unused...
final String _TOSTRING_FUNCTION = @"""
function $toString(o) {
  if (o == null) return 'null';
  var t = typeof(o);
  if (t == 'object') { return o.toString(); }
  else if (t == 'string') { return o; }
  else if (t == 'bool') { return ''+o; }
  else if (t == 'number') { return ''+o; }
  else return o.toString();
}""";

/**
 * Snippet for `$index` in Object, Array, and String.  If not overridden,
 * `$index` and `$setindex` fall back to JS [] and []= accessors.
 */
// TODO(jimhug): This fallback could be very confusing in a few cases -
// because of the bizare default [] rules in JS.  We need to revisit this
// to get the right errors - at least in checked mode (once we have that).
// TODO(jmesserly): do perf analysis, figure out if this is worth it and
// what the cost of $index $setindex is on all browsers

// Performance of Object.prototype methods can go down because there are
// so many of them. Instead, first time we hit it, put it on the derived
// prototype. TODO(jmesserly): make this go away by handling index more
// like a normal method.
final String _INDEX_OPERATORS = @"""
Object.prototype.$index = function(i) {
  var proto = Object.getPrototypeOf(this);
  if (proto !== Object) {
    proto.$index = function(i) { return this[i]; }
  }
  return this[i];
}
Array.prototype.$index = function(i) { return this[i]; }
String.prototype.$index = function(i) { return this[i]; }""";

/** Snippet for `$setindex` in Object, Array, and String. */
// TODO(jimhug): Add array bounds checking in checked mode
/*
function $inlineArrayIndexCheck(array, index) {
  if (index >= 0 && index < array.length) {
    return index;
  }
  native__ArrayJsUtil__throwIndexOutOfRangeException(index);
}*/
final String _SETINDEX_OPERATORS = @"""
Object.prototype.$setindex = function(i, value) {
  var proto = Object.getPrototypeOf(this);
  if (proto !== Object) {
    proto.$setindex = function(i, value) { return this[i] = value; }
  }
  return this[i] = value;
}
Array.prototype.$setindex = function(i, value) { return this[i] = value; }""";

/** Snippet for `$wrap_call$0`. */
final String _WRAP_CALL0_FUNCTION = @"""
// Wrap a 0-arg dom-callback to bind it with the current isolate:
function $wrap_call$0(fn) { return fn && fn.wrap$call$0(); }
Function.prototype.wrap$call$0 = function() {
  var isolateContext = $globalState.currentContext;
  var self = this;
  this.wrap$0 = function() {
    isolateContext.eval(self);
    $globalState.topEventLoop.run();
  };
  this.wrap$call$0 = function() { return this.wrap$0; };
  return this.wrap$0;
}""";

/** Snippet for `$wrap_call$0`, in case it was not necessary. */
final String _EMPTY_WRAP_CALL0_FUNCTION =
    @"function $wrap_call$0(fn) { return fn; }";

/** Snippet for `$wrap_call$1`. */
final String _WRAP_CALL1_FUNCTION = @"""
// Wrap a 1-arg dom-callback to bind it with the current isolate:
function $wrap_call$1(fn) { return fn && fn.wrap$call$1(); }
Function.prototype.wrap$call$1 = function() {
  var isolateContext = $globalState.currentContext;
  var self = this;
  this.wrap$1 = function(arg) {
    isolateContext.eval(function() { self(arg); });
    $globalState.topEventLoop.run();
  };
  this.wrap$call$1 = function() { return this.wrap$1; };
  return this.wrap$1;
}""";

/** Snippet for `$wrap_call$1`, in case it was not necessary. */
final String _EMPTY_WRAP_CALL1_FUNCTION =
    @"function $wrap_call$1(fn) { return fn; }";

/** Snippet that initializes the isolates state. */
final String _ISOLATE_INIT_CODE = @"""
var $globalThis = this;
var $globals = null;
var $globalState = null;""";
