// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

void print(var obj) {
  // TODO(ngeoffray): enable when the parser accepts it.
  // obj = obj.toString();
  var hasConsole = JS("bool", @"typeof console == 'object'");
  if (hasConsole) {
    JS("void", @"console.log($0)", obj);
  } else {
    JS("void", @"write($0)", obj);
    JS("void", @"write('\n')");
  }
}

$throw(String msg) {
  var e = JS("Object", @"new Error($0)", msg);
  var hasError = JS("bool", @"typeof Error == 'object'");
  if (hasError) {
    JS("void", @"Error.captureStackTrace($0)", e);
  }
  throw e;
}

guard$num(x) {
  if (JS("bool", @"typeof $0 == 'number'", x)) return x;
  $throw("Num type guard failed.");
}

guard$string(x) {
  if (JS("bool", @"typeof $0 == 'string'", x)) return x;
  $throw("String type guard failed.");
}

guard$array(x) {
  if (JS("bool", @"$0.constructor === Array", x)) return x;
  $throw("String type guard failed.");
}

guard$stringOrArray(x) {
  if (JS("bool", @"typeof $0 == 'string'", x)
      || JS("bool", @"$0.constructor === Array", x)) {
    return x;
  }
  $throw("String or Array type guard failed.");
}

guard$bool(x) {
  if (JS("bool", @"typeof $0 == 'boolean'", x)) return x;
  $throw("Bool type guard failed.");
}

/**
  * Returns true if both arguments are numbers.
  * If only the first argument is a number, throws the given message as
  * exception.
  */
bool checkNumbers(var a, var b, var message) {
  if (JS("bool", @"typeof $0 === 'number'", a)) {
    if (JS("bool", @"typeof $0 === 'number'", b)) {
      return true;
    } else {
      throw message;
    }
  }
  return false;
}


bool isJSArray(var value) {
  return JS("bool", @"$0.constructor === Array", value);
}


add(var a, var b) {
  if (checkNumbers(a, b, "num+ expects a number as second operand.")) {
    return JS("num", @"$0 + $1", a, b);
  } else if (JS("bool", @"typeof $0 === 'string'", a)) {
    if (JS("bool", @"typeof $0 === 'string'", b) ||
        JS("bool", @"typeof $0 === 'number'", b)) {
      return JS("String", @"$0 + $1", a, b);
    }
    throw "Unimplemented String+.";
  }
  throw "Unimplemented user-defined +.";
}

div(var a, var b) {
  if (checkNumbers(a, b, "num/ expects a number as second operand.")) {
    return JS("num", @"$0 / $1", a, b);
  }
  throw "Unimplemented user-defined /.";
}

mul(var a, var b) {
  if (checkNumbers(a, b, "num* expects a number as second operand.")) {
    return JS("num", @"$0 * $1", a, b);
  }
  throw "Unimplemented user-defined *.";
}

sub(var a, var b) {
  if (checkNumbers(a, b, "num- expects a number as second operand.")) {
    return JS("num", @"$0 - $1", a, b);
  }
  throw "Unimplemented user-defined binary -.";
}

mod(var a, var b) {
  if (checkNumbers(a, b, "int% expects an int as second operand.")) {
    // Euclidean Modulo.
    int result = JS("num", @"$0 % $1", a, b);
    if (result == 0) return 0;  // Make sure we don't return -0.0.
    if (result > 0) return result;
    if (b < 0) {
      return result - b;
    } else {
      return result + b;
    }
  }
  throw "Unimplemented user-defined %.";
}

tdiv(var a, var b) {
  if (checkNumbers(a, b, "num~/ expects a number as second operand.")) {
    var tmp = a / b;
    // TODO(ngeoffray): Use tmp.floor and tmp.ceil when
    // we can handle them.
    if (tmp < 0) {
      return JS("num", @"Math.ceil($0)", tmp);
    } else {
      return JS("num", @"Math.floor($0)", tmp);
    }
  }
  throw "Unimplemented user-defined ~/.";
}

eq(var a, var b) {
  if (JS("bool", @"typeof $0 === 'undefined'", a) ||
      JS("bool", @"typeof $0 === 'number'", a) ||
      JS("bool", @"typeof $0 === 'boolean'", a) ||
      JS("bool", @"typeof $0 === 'string'", a)) {
    return JS("bool", @"$0 === $1", a, b);
  }
  // TODO(kasperl): This is not the right implementation if the a has
  // a user-defined == operator.
  return JS("bool", @"$0 === $1", a, b);
}

gt(var a, var b) {
  if (checkNumbers(a, b, "num> expects a number as second operand.")) {
    return JS("bool", @"$0 > $1", a, b);
  }
  throw "Unimplemented user-defined binary >.";
}

ge(var a, var b) {
  if (checkNumbers(a, b, "num>= expects a number as second operand.")) {
    return JS("bool", @"$0 >= $1", a, b);
  }
  throw "Unimplemented user-defined binary >=.";
}

lt(var a, var b) {
  if (checkNumbers(a, b, "num< expects a number as second operand.")) {
    return JS("bool", @"$0 < $1", a, b);
  }
  throw "Unimplemented user-defined binary <.";
}

le(var a, var b) {
  if (checkNumbers(a, b, "num<= expects a number as second operand.")) {
    return JS("bool", @"$0 <= $1", a, b);
  }
  throw "Unimplemented user-defined binary <=.";
}

shl(var a, var b) {
  // TODO(floitsch): inputs must be integers.
  if (checkNumbers(a, b, "int<< expects an int as second operand.")) {
    return JS("num", @"$0 << $1", a, b);
  }
  throw "Unimplemented user-defined binary <<.";
}

shr(var a, var b) {
  // TODO(floitsch): inputs must be integers.
  if (checkNumbers(a, b, "int>> expects an int as second operand.")) {
    return JS("num", @"$0 >> $1", a, b);
  }
  throw "Unimplemented user-defined binary >>.";
}

and(var a, var b) {
  // TODO(floitsch): inputs must be integers.
  if (checkNumbers(a, b, "int& expects an int as second operand.")) {
    return JS("num", @"$0 & $1", a, b);
  }
  throw "Unimplemented user-defined binary &.";
}

or(var a, var b) {
  // TODO(floitsch): inputs must be integers.
  if (checkNumbers(a, b, "int| expects an int as second operand.")) {
    return JS("num", @"$0 | $1", a, b);
  }
  throw "Unimplemented user-defined binary |.";
}

xor(var a, var b) {
  // TODO(floitsch): inputs must be integers.
  if (checkNumbers(a, b, "int^ expects an int as second operand.")) {
    return JS("num", @"$0 ^ $1", a, b);
  }
  throw "Unimplemented user-defined binary ^.";
}

not(var a) {
  if (JS("bool", @"typeof $0 === 'number'", a)) return JS("num", @"~$0", a);
  throw "Unimplemented user-defined ~.";
}

neg(var a) {
  if (JS("bool", @"typeof $0 === 'number'", a)) return JS("num", @"-$0", a);
  throw "Unimplemented user-defined unary-.";
}

index(var a, var index) {
  if (isJSArray(a)) return JS("Object", @"$0[$1]", a, index);
  throw "Unimplemented user-defined [].";
}

indexSet(var a, var index, var value) {
  if (isJSArray(a)) {
    return JS("Object", @"$0[$1] = $2", a, index, value);
  }
  throw "Unimplemented user-defined []=.";
}

builtin$add$1(var receiver, var value) {
  if (isJSArray(receiver)) {
    JS("Object", @"$0.push($1)", receiver, value);
    return;
  }
  throw "Unimplemented user-defined add().";
}

builtin$removeLast$0(var receiver) {
  if (isJSArray(receiver)) {
    return JS("Object", @"$0.pop()", receiver);
  }
  throw "Unimplemented user-defined removeLast().";
}

builtin$filter$1(var receiver, var predicate) {
  if (isJSArray(receiver)) {
    return JS("Object", @"$0.filter(function(v) { return $1(v) === true; })",
              receiver, predicate);
  }
  throw "Unimplemented user-defined filter().";
}


builtin$get$length(var receiver) {
  if (JS("bool", @"typeof $0 === 'string'", receiver) || isJSArray(receiver)) {
    return JS("num", @"$0.length", receiver);
  }
  throw "Unimplemented user-defined length.";
}

bool isInt(var v) {
  return JS("bool", @"($0 | 0) === $1", v, v);
}

class int {}
class double {}
class String {}
class bool {}
class Object {}
class List<T> {
  static void _checkConstructorInput(n) {
    // TODO(ngeoffray): Inline once we support optional parameters or
    // bailout.
    if (!isInt(n)) throw "Invalid argument";
    if (n < 0) throw "Negative size";
  }

  factory List(n) {
    // TODO(ngeoffray): Adjust to optional parameters.
    if (JS("bool", @"$0 === (void 0)", n)) return JS("Object", @"new Array()");
    _checkConstructorInput(n);
    return JS("Object", @"new Array($0)", n);
  }
}

class Expect {
  static void equals(var expected, var actual) {
    if (expected == actual) return;
    _fail('Expect.equals(expected: <' + expected.toString() +
          '>, actual:<' + actual.toString() + '> fails.');
  }

  static void fail(String message) {
    _fail("Expect.fail('" + message + "')");
  }

  static void _fail(String message) {
    throw message;
  }
}

class Stopwatch {
  double startMs;
  double elapsedMs;

  Stopwatch() {
    elapsedMs = 0.0;
  }

  void start() {
    if (startMs == null) {
      startMs = JS("num", @"Date.now()");
    }
  }

  void stop() {
    if (startMs == null) return;
    elapsedMs += JS("num", @"Date.now() - $0", startMs);
    startMs = null;
  }

  void reset() {
    elapsedMs = 0.0;
    if (startMs == null) return;
    startMs = JS("num", @"Date.now()");
  }

  int elapsed() {
    return elapsedInMs();
  }

  int elapsedInUs() {
    return elapsedInMs() * 1000;
  }

  int elapsedInMs() {
    if (startMs == null) {
      return JS("num", @"Math.floor($0)", elapsedMs);
    } else {
      return
        JS("num", @"Math.floor($0 + (Date.now() - $1))", elapsedMs, startMs);
    }
  }

  int frequency() {
    return 1000;
  }
}
