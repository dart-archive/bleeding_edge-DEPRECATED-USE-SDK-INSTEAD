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
  bool useThrow = false;
  bool useNotNullBool = false;
  bool useIndex = false;
  bool useSetIndex = false;

  bool useWrap0 = false;
  bool useWrap1 = false;
  bool useIsolates = false;

  // These helpers had to switch to a new pattern, because they can be generated
  // after everything else.
  bool _generatedTypeNameOf = false;
  bool _generatedDynamicProto = false;
  bool _generatedInherits = false;
  bool _generatedDefProp = false;


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
        ensureDefProp();
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
        // TODO(jimhug): Only do this once!
        world.gen.markTypeUsed(
          world.corelib.types['IntegerDivisionByZeroException']);
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
    ensureDefProp();
    writer.writeln(_DYNAMIC_FUNCTION);
  }

  void ensureTypeNameOf() {
    if (_generatedTypeNameOf) return;
    _generatedTypeNameOf = true;
    ensureDefProp();
    writer.writeln(_TYPE_NAME_OF_FUNCTION);
  }

  /** Generates the $inherits function when it's first used. */
  void ensureInheritsHelper() {
    if (_generatedInherits) return;
    _generatedInherits = true;
    writer.writeln(_INHERITS_FUNCTION);
  }

  /** Generates the $defProp function when it's first used. */
  void ensureDefProp() {
    if (_generatedDefProp) return;
    _generatedDefProp = true;
    writer.writeln(_DEF_PROP_FUNCTION);
  }

  void generate(CodeWriter w) {
    // Write any stuff we had queued up, then replace our writer with the one
    // in WorldGenerator so anything we discover that we need later on will be
    // generated on-demand.
    w.write(writer.text);
    writer = w;

    if (useNotNullBool) {
      useThrow = true;
      w.writeln(_NOTNULL_BOOL_FUNCTION);
    }

    if (useThrow) {
      w.writeln(_THROW_FUNCTION);
    }

    if (useIndex) {
      ensureDefProp();
      w.writeln(options.disableBoundsChecks ?
        _INDEX_OPERATORS : _CHECKED_INDEX_OPERATORS);
    }

    if (useSetIndex) {
      ensureDefProp();
      w.writeln(options.disableBoundsChecks ?
        _SETINDEX_OPERATORS : _CHECKED_SETINDEX_OPERATORS);
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

    if (world.dom != null) {
      ensureTypeNameOf();
      ensureDefProp();
      // TODO(jmesserly): we need to find a way to avoid conflicts with other
      // generated "typeName" fields. Ideally we wouldn't be patching 'Object'
      // here.
      w.writeln('\$defProp(Object.prototype, "get\$typeName", ' +
                'Object.prototype.\$typeNameOf);');
    }
    // This just turns console into a no-op if it is not defined so that IE
    // doesn't throw an error if the debug console isn't open.
    w.writeln(_IE_LOG_PATCH);
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
$defProp(Object.prototype, '$eq', function(other) {
  return this === other;
});""";

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
    var obj = this;
    var tag = obj.$typeNameOf();
    var method = methods[tag];
    if (!method) {
      var table = $dynamicMetadata;
      for (var i = 0; i < table.length; i++) {
        var entry = table[i];
        if (entry.map.hasOwnProperty(tag)) {
          method = methods[entry.tag];
          if (method) break;
        }
      }
    }
    method = method || methods.Object;
    var proto = Object.getPrototypeOf(obj);
    if (!proto.hasOwnProperty(name)) {
      $defProp(proto, name, method);
    }

    return method.apply(this, Array.prototype.slice.call(arguments));
  };
  $dynamicBind.methods = methods;
  $defProp(Object.prototype, name, $dynamicBind);
  return methods;
}
if (typeof $dynamicMetadata == 'undefined') $dynamicMetadata = [];

function $dynamicSetMetadata(inputTable) {
  // TODO: Deal with light isolates.
  var table = [];
  for (var i = 0; i < inputTable.length; i++) {
    var tag = inputTable[i][0];
    var tags = inputTable[i][1];
    var map = {};
    var tagNames = tags.split('|');
    for (var j = 0; j < tagNames.length; j++) {
      map[tagNames[j]] = true;
    }
    table.push({tag: tag, tags: tags, map: map});
  }
  $dynamicMetadata = table;
}
""";

/** 
 * IE throws an error if you try to log to the console but you don't have the
 * console window open. This just turns console into a no-op if it is not
 * defined so that the JS in IE doesn't crash.
 */
final String _IE_LOG_PATCH = @"""
if(typeof console === "undefined") {
    console = { log: function() { } };
}
""";

/** Snippet for `$typeNameOf`. */
final String _TYPE_NAME_OF_FUNCTION = @"""
$defProp(Object.prototype, '$typeNameOf', function() {
  if ((typeof(window) != 'undefined' && window.constructor.name == 'DOMWindow')
      || typeof(process) != 'undefined') { // fast-path for Chrome and Node
    return this.constructor.name;
  }
  var str = Object.prototype.toString.call(this);
  str = str.substring(8, str.length - 1);
  if (str == 'Window') {
    str = 'DOMWindow';
  } else if (str == 'Document') {
    str = 'HTMLDocument';
  }
  return str;
});""";

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

/** Snippet for `$defProp`. */
final String _DEF_PROP_FUNCTION = @"""
function $defProp(obj, prop, value) {
  Object.defineProperty(obj, prop,
      {value: value, enumerable: false, writable: true, configurable: true});
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
$defProp(Object.prototype, '$index', function(i) {
  var proto = Object.getPrototypeOf(this);
  if (proto !== Object) {
    proto.$index = function(i) { return this[i]; }
  }
  return this[i];
});
$defProp(Array.prototype, '$index', function(i) { 
  return this[i]; 
});
$defProp(String.prototype, '$index', function(i) { 
  return this[i]; 
});""";

final String _CHECKED_INDEX_OPERATORS = @"""
$defProp(Object.prototype, '$index', function(i) {
  var proto = Object.getPrototypeOf(this);
  if (proto !== Object) {
    proto.$index = function(i) { return this[i]; }
  }
  return this[i];
});
$defProp(Array.prototype, '$index', function(index) {
  var i = index | 0;
  if (i !== index) {
    throw new IllegalArgumentException('index is not int');
  } else if (i < 0 || i >= this.length) {
    throw new IndexOutOfRangeException(index);
  }
  return this[i];
});
$defProp(String.prototype, '$index', function(i) {
  return this[i];
});""";



/** Snippet for `$setindex` in Object, Array, and String. */
final String _SETINDEX_OPERATORS = @"""
$defProp(Object.prototype, '$setindex', function(i, value) {
  var proto = Object.getPrototypeOf(this);
  if (proto !== Object) {
    proto.$setindex = function(i, value) { return this[i] = value; }
  }
  return this[i] = value;
});
$defProp(Array.prototype, '$setindex',
    function(i, value) { return this[i] = value; });""";

final String _CHECKED_SETINDEX_OPERATORS = @"""
$defProp(Object.prototype, '$setindex', function(i, value) {
  var proto = Object.getPrototypeOf(this);
  if (proto !== Object) {
    proto.$setindex = function(i, value) { return this[i] = value; }
  }
  return this[i] = value;
});
$defProp(Array.prototype, '$setindex', function(index, value) {
  var i = index | 0;
  if (i !== index) {
    throw new IllegalArgumentException('index is not int');
  } else if (i < 0 || i >= this.length) {
    throw new IndexOutOfRangeException(index);
  }
  return this[i] = value;
});""";

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
