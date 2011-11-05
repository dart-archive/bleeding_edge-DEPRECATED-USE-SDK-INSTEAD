// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// TODO(jimhug): Completeness - see tests/corelib

/** Implements extends for dart classes on javascript prototypes. */
function $inherits(child, parent) {
  if (child.prototype.__proto__) {
    child.prototype.__proto__ = parent.prototype;
  } else {
    function tmp() {};
    tmp.prototype = parent.prototype;
    child.prototype = new tmp();
    child.prototype.constructor = child;
  }
}

// Note: we do $add(x, y) instead of x.$add(y) so it doesn't box.
// Same idea for the other methods.
function $add(x, y) {
  return ((typeof(x) == 'number' && typeof(y) == 'number') ||
          (typeof(x) == 'string' && typeof(y) == 'string'))
    ? x + y : x.$add(y);
}

function $bit_not(x) {
  return (typeof(x) == 'number') ? ~x : x.$bit_not();
}

function $negate(x) {
  return (typeof(x) == 'number') ? -x : x.$negate();
}

function $ne(x, y) {
  if (x == null) return y != null;
  return (typeof(x) == 'number' && typeof(y) == 'number') ||
         (typeof(x) == 'boolean' && typeof(y) == 'boolean') ||
         (typeof(x) == 'string' && typeof(y) == 'string')
    ? x != y : !x.$eq(y);
}

function $eq(x, y) {
  if (x == null) return y == null;
  return (typeof(x) == 'number' && typeof(y) == 'number') ||
         (typeof(x) == 'boolean' && typeof(y) == 'boolean') ||
         (typeof(x) == 'string' && typeof(y) == 'string')
    ? x == y : x.$eq(y);
}

function $truncdiv(x, y) {
  if (typeof(x) == 'number' && typeof(y) == 'number') {
    if (y == 0) throw new IntegerDivisionByZeroException();
    var tmp = x / y;
    return (tmp < 0) ? Math.ceil(tmp) : Math.floor(tmp);
  } else {
    return x.$truncdiv(y);
  }
}


function $bit_or(x, y) {
  return (typeof(x) == 'number' && typeof(y) == 'number')
    ? x | y : x.$bit_or(y);
}
function $bit_xor(x, y) {
  return (typeof(x) == 'number' && typeof(y) == 'number')
    ? x ^ y : x.$bit_xor(y);
}
function $bit_and(x, y) {
  return (typeof(x) == 'number' && typeof(y) == 'number')
    ? x & y : x.$bit_and(y);
}
function $shl(x, y) {
  return (typeof(x) == 'number' && typeof(y) == 'number')
    ? x << y : x.$shl(y);
}
function $sar(x, y) {
  return (typeof(x) == 'number' && typeof(y) == 'number')
    ? x >> y : x.$sar(y);
}
function $shr(x, y) {
  return (typeof(x) == 'number' && typeof(y) == 'number')
    ? x >>> y : x.$shr(y);
}
function $sub(x, y) {
  return (typeof(x) == 'number' && typeof(y) == 'number')
    ? x - y : x.$sub(y);
}
function $mul(x, y) {
  return (typeof(x) == 'number' && typeof(y) == 'number')
    ? x * y : x.$mul(y);
}
function $div(x, y) {
  return (typeof(x) == 'number' && typeof(y) == 'number')
    ? x / y : x.$div(y);
}

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
}

function $lt(x, y) {
  return (typeof(x) == 'number' && typeof(y) == 'number')
    ? x < y : x.$lt(y);
}
function $gt(x, y) {
  return (typeof(x) == 'number' && typeof(y) == 'number')
    ? x > y : x.$gt(y);
}
function $lte(x, y) {
  return (typeof(x) == 'number' && typeof(y) == 'number')
    ? x <= y : x.$lte(y);
}
function $gte(x, y) {
  return (typeof(x) == 'number' && typeof(y) == 'number')
    ? x >= y : x.$gte(y);
}

// TODO(jimhug): Test perf - idea is this speeds up primitives and
//  semi-elegantly handles null - currently unused...
function $toString(o) {
  if (o == null) return 'null';
  var t = typeof(o);
  if (t == 'object') { return o.toString(); }
  else if (t == 'string') { return o; }
  else if (t == 'bool') { return ''+o; }
  else if (t == 'number') { return ''+o; }
  else return o.toString();
}


// If not overridden, $index and $setindex fall back to JS [] and []= accessors
// TODO(jimhug): This fallback could be very confusing in a few cases -
// because of the bizare default [] rules in JS.  We need to revisit this
// to get the right errors - at least in checked mode (once we have that).
Object.prototype.$index = function(i) { return this[i]; }
Object.prototype.$setindex = function(i, value) { return this[i] = value; }

// TODO(jmesserly): do perf analysis, figure out if this is worth it and what
// the cost of $index $setindex is on all browsers
Array.prototype.$index = function(i) { return this[i]; }
Array.prototype.$setindex = function(i, value) { return this[i] = value; }
String.prototype.$index = function(i) { return this[i]; }


// TODO(jimhug): Should this or should it not match equals?
Object.prototype.$eq = function(other) { return this === other; }

function $map(items) {
  var ret = new HashMapImplementation();
  for (var i=0; i < items.length;) {
    ret.$setindex(items[i++], items[i++]);
  }
  return ret;
}

function $assert(test, text, url, line, column) {
  if (typeof test == 'function') test = test();
  if (!test) $throw(new AssertError(text, url, line, column));
}

function $notnull_bool(test) {
  if (test == null || typeof(test) != 'boolean') {
    $throw(new TypeError('must be "true" or "false"'));
  }
  return test === true;
}

function $throw(e) {
  // If e is not a value, we can use V8's captureStackTrace utility method.
  // TODO(jmesserly): capture the stack trace on other JS engines.
  if (e && (typeof e == "object") && Error.captureStackTrace) {
    // TODO(jmesserly): this will clobber the e.stack property
    Error.captureStackTrace(e, $throw);
  }
  throw e;
}

function $stackTraceOf(e) {
  // TODO(jmesserly): we shouldn't be relying on the e.stack property.
  // Need to mangle it.
  return e.stack ? e.stack : null;
}

// Translate a JavaScript exception to a Dart exception
// TODO(jmesserly): cross browser support. This is Chrome specific.
function $toDartException(e) {
  if (e instanceof TypeError) {
    switch(e.type) {
    case "property_not_function":
    case "called_non_callable":
      if (e.arguments[0] == "undefined") {
        return new NullPointerException();
      }
      return new ObjectNotClosureException();
    case "non_object_property_call":
    case "non_object_property_load":
      return new NullPointerException();
    case "undefined_method":
      if (e.arguments[0] == "call" || e.arguments[0] == "apply") {
        return new ObjectNotClosureException();
      }
      // TODO(jmesserly): can this ever happen?
      return new NoSuchMethodException("", e.arguments[0], []);
    }
  } else if (e instanceof RangeError) {
    if (e.message.indexOf('call stack') >= 0) {
      return new StackOverflowException();
    }
  }
  return e;
}

// TODO(jimhug): I can't figure out the quoting rules to put this in
//   StringImplementation correctly <frown>.
function $regexpAllFromString(s) {
  return new RegExp(s.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, "\\$&"), 'g');
}

/* TODO(jimhug): Add array bounds checking in checked mode
function $inlineArrayIndexCheck(array, index) {
  if (index >= 0 && index < array.length) {
    return index;
  }
  native__ArrayJsUtil__throwIndexOutOfRangeException(index);
}
*/

/**
 * Generates a dynamic call stub for a function.
 * Our goal is to create a stub method like this on-the-fly:
 *   function($0, $1, capture) { this($0, $1, true, capture); }
 *
 * This stub then replaces the dynamic one on Function, with one that is
 * specialized for that particular function, taking into account its default
 * arguments.
 */
Function.prototype.$genStub = function(argsLength, names) {
  // TODO(jmesserly): only emit $genStub if actually needed

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
    var j = this.$optional.indexOf(name, 0);
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

  // Note: using Function instead of "eval" to get a clean scope.
  // TODO(jmesserly): evaluate the performance of these stubs.
  var f = 'function(' + a.join(',') + '){return $f(' + p.join(',') + ');}';
  return new Function('$f', 'return ' + f + '').call(null, this);
}

function $varMethod(name, methods) {
  Object.prototype[name] = function() {
    $patchMethod(this, name, methods);
    this[name].apply(this, Array.prototype.slice.call(arguments));
  };
}

Object.prototype.get$typeName = function() {
  // TODO(vsm): how can we make this go down the fast path for Chrome?
  //(for Chrome: return this.constructor.name;)
  var str = Object.prototype.toString.call(this);
  return str.substring(8, str.length - 1);
}

function $patchMethod(obj, name, methods) {
  // Get the prototype to patch.
  // Don't overwrite an existing stub, like the one on Object.prototype
  var proto = Object.getPrototypeOf(obj);
  if (!proto || proto.hasOwnProperty(name)) proto = obj;
  var method;
  while (obj && !(method = methods[obj.get$typeName()])) {
    obj = Object.getPrototypeOf(obj);
  }
  Object.defineProperty(proto, name, {value: method || methods['Object']});
}
