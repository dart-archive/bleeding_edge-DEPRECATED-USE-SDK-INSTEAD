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
  bool useTypeNameOf = false;
  bool useStackTraceOf = false;
  bool useToDartException = false;
  bool useThrow = false;
  bool useVarMethod = false;
  bool useGenStub = false;
  bool useMap = false;
  bool useAssert = false;
  bool useNotNullBool = false;
  bool useIndex = false;
  bool useSetIndex = false;

  /** An experimental toString implementation. Currently unused. */
  bool useToString = false;

  Map<String, String> _usedOperators;

  CoreJs(): _usedOperators = {};

  /**
   * Generates the special operator method, e.g. $add.
   * We want to do $add(x, y) instead of x.$add(y) so it doesn't box.
   * Same idea for the other methods.
   */
  void useOperator(String name) {
    if (_usedOperators[name] != null) return;

    var code;
    switch (name) {
      case '\$ne':
        code = @"""
function $ne(x, y) {
  if (x == null) return y != null;
  return (typeof(x) == 'number' && typeof(y) == 'number') ||
         (typeof(x) == 'boolean' && typeof(y) == 'boolean') ||
         (typeof(x) == 'string' && typeof(y) == 'string')
    ? x != y : !x.$eq(y);
}""";
        break;

      case '\$eq':
        code = @"""
function $eq(x, y) {
  if (x == null) return y == null;
  return (typeof(x) == 'number' && typeof(y) == 'number') ||
         (typeof(x) == 'boolean' && typeof(y) == 'boolean') ||
         (typeof(x) == 'string' && typeof(y) == 'string')
    ? x == y : x.$eq(y);
}
// TODO(jimhug): Should this or should it not match equals?
Object.prototype.$eq = function(other) { return this === other; }""";
        break;

      case '\$bit_not':
        code = @"""
function $bit_not(x) {
  return (typeof(x) == 'number') ? ~x : x.$bit_not();
}""";
        break;

      case '\$negate':
        code = @"""
function $negate(x) {
  return (typeof(x) == 'number') ? -x : x.$negate();
}""";
        break;

      case '\$add':
        code = @"""
function $add(x, y) {
  return ((typeof(x) == 'number' && typeof(y) == 'number') ||
          (typeof(x) == 'string' && typeof(y) == 'string'))
    ? x + y : x.$add(y);
}""";
        break;

      case '\$truncdiv':
        useThrow = true;
        code = @"""
function $truncdiv(x, y) {
  if (typeof(x) == 'number' && typeof(y) == 'number') {
    if (y == 0) $throw(new IntegerDivisionByZeroException());
    var tmp = x / y;
    return (tmp < 0) ? Math.ceil(tmp) : Math.floor(tmp);
  } else {
    return x.$truncdiv(y);
  }
}""";
        break;

      case '\$mod':
        code = @"""
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
        break;

      default:
        // All of the other helpers are generated the same way
        var op = TokenKind.rawOperatorFromMethod(name);
        code = """
function ${name}(x, y) {
  return (typeof(x) == 'number' && typeof(y) == 'number')
    ? x ${op} y : x.${name}(y);
}""";
        break;
    }

    _usedOperators[name] = code;
  }

  void generate(CodeWriter w) {
    if (useVarMethod) {
      useTypeNameOf = true;
      w.writeln(@"""
function $varMethod(name, methods) {
  Object.prototype[name] = function() {
    $patchMethod(this, name, methods);
    this[name].apply(this, Array.prototype.slice.call(arguments));
  };
}
function $patchMethod(obj, name, methods) {
  // Get the prototype to patch.
  // Don't overwrite an existing stub, like the one on Object.prototype
  var proto = Object.getPrototypeOf(obj);
  if (!proto || proto.hasOwnProperty(name)) proto = obj;
  var method;
  while (obj && !(method = methods[obj.$typeNameOf()])) {
    obj = Object.getPrototypeOf(obj);
  }
  obj[name] = method || methods['Object'];
}""");
    }

    if (useGenStub) {
      useThrow = true;
      w.writeln(@"""
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

  // Note: using Function instead of 'eval' to get a clean scope.
  // TODO(jmesserly): evaluate the performance of these stubs.
  var f = 'function(' + a.join(',') + '){return $f(' + p.join(',') + ');}';
  return new Function('$f', 'return ' + f + '').call(null, this);
}""");
    }

    if (useStackTraceOf) {
      w.writeln(@"""
function $stackTraceOf(e) {
  // TODO(jmesserly): we shouldn't be relying on the e.stack property.
  // Need to mangle it.
  return e.stack ? e.stack : null;
}""");
    }

    if (useToDartException) {
      w.writeln(@"""
// Translate a JavaScript exception to a Dart exception
// TODO(jmesserly): cross browser support. This is Chrome specific.
function $toDartException(e) {
  var res = e;
  if (e instanceof TypeError) {
    switch(e.type) {
      case 'property_not_function':
      case 'called_non_callable':
        if (e.arguments[0] == null) {
          res = new NullPointerException();
        } else {
          res = new ObjectNotClosureException();
        }
        break;
      case 'non_object_property_call':
      case 'non_object_property_load':
        res = new NullPointerException();
        break;
      case 'undefined_method':
        if (e.arguments[0] == 'call' || e.arguments[0] == 'apply') {
          res = new ObjectNotClosureException();
        } else {
          // TODO(jmesserly): can this ever happen?
          res = new NoSuchMethodException('', e.arguments[0], []);
        }
        break;
    }
  } else if (e instanceof RangeError) {
    if (e.message.indexOf('call stack') >= 0) {
      res = new StackOverflowException();
    }
  }
  // TODO(jmesserly): setting the stack property is not a long term solution.
  // Also it causes the exception to print as if it were a TypeError or
  // RangeError, instead of using the proper toString.
  res.stack = e.stack;
  return res;
}""");
    }

    if (useNotNullBool) {
      useThrow = true;
      // Some testing showed that this patterned fared well across browsers.
      w.writeln(@"""
function $notnull_bool(test) {
  return typeof(test) == 'boolean' ? test : test.is$bool();
}""");
    }

    if (useAssert) {
      useThrow = true;
      w.writeln(@"""
function $assert(test, text, url, line, column) {
  if (typeof test == 'function') test = test();
  if (!test) $throw(new AssertError(text, url, line, column));
}""");
    }

    if (useThrow) {
      w.writeln(@"""
function $throw(e) {
  // If e is not a value, we can use V8's captureStackTrace utility method.
  // TODO(jmesserly): capture the stack trace on other JS engines.
  if (e && (typeof e == 'object') && Error.captureStackTrace) {
    // TODO(jmesserly): this will clobber the e.stack property
    Error.captureStackTrace(e, $throw);
  }
  throw e;
}""");
    }

    if (useMap) {
      w.writeln(@"""
function $map(items) {
  var ret = new HashMapImplementation();
  for (var i=0; i < items.length;) {
    ret.$setindex(items[i++], items[i++]);
  }
  return ret;
}""");
    }

    if (useToString) {
      // TODO(jimhug): Test perf - idea is this speeds up primitives and
      //  semi-elegantly handles null - currently unused...
      w.writeln(@"""
function $toString(o) {
  if (o == null) return 'null';
  var t = typeof(o);
  if (t == 'object') { return o.toString(); }
  else if (t == 'string') { return o; }
  else if (t == 'bool') { return ''+o; }
  else if (t == 'number') { return ''+o; }
  else return o.toString();
}""");
    }

    if (useTypeNameOf) {
      // TODO(sigmund): find a way to make this work on all browsers, including
      // checking the typeName on prototype objects (so we can fix dynamic
      // dispatching on $varMethod).
      w.writeln(@"""
Object.prototype.$typeNameOf = function() {
  if (window.constructor.name == 'DOMWindow') { // fast-path for Chrome
    return this.constructor.name;
  }
  var str = Object.prototype.toString.call(this);
  return str.substring(8, str.length - 1);
}""");
    }

    if (useIndex) {
      // If not overridden, $index and $setindex fall back to JS [] and []=
      // accessors
      // TODO(jimhug): This fallback could be very confusing in a few cases -
      // because of the bizare default [] rules in JS.  We need to revisit this
      // to get the right errors - at least in checked mode (once we have that).
      // TODO(jmesserly): do perf analysis, figure out if this is worth it and
      // what the cost of $index $setindex is on all browsers
      w.writeln(@"""
Object.prototype.$index = function(i) { return this[i]; }
Array.prototype.$index = function(i) { return this[i]; }
String.prototype.$index = function(i) { return this[i]; }""");
    }

    if (useSetIndex) {
      /* TODO(jimhug): Add array bounds checking in checked mode
      function $inlineArrayIndexCheck(array, index) {
        if (index >= 0 && index < array.length) {
          return index;
        }
        native__ArrayJsUtil__throwIndexOutOfRangeException(index);
      }*/
      w.writeln(@"""
Object.prototype.$setindex = function(i, value) { return this[i] = value; }
Array.prototype.$setindex = function(i, value) { return this[i] = value; }""");
    }

    // Write operator helpers
    for (var opImpl in orderValuesByKeys(_usedOperators)) {
      w.writeln(opImpl);
    }
  }
}
