// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('js_helper');

$throw(String msg) {
  var e = JS("Object", @"new Error($0)", msg);
  var hasTrace = JS("bool", @"Error.captureStackTrace !== (void 0)");
  if (hasTrace) {
    JS("void", @"Error.captureStackTrace($0)", e);
  }
  throw e;
}

/**
  * Returns true if both arguments are numbers.
  * If only the first argument is a number, throws the given message as
  * exception.
  */
bool checkNumbers(var a, var b, var message) {
  if (a is num) {
    if (b is num) {
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
  } else if (a is String) {
    b = b.toString();
    if (b is String) {
      return JS("String", @"$0 + $1", a, b);
    }
    // The following line is too long, but we can't break it using the +
    // operator, since that's what we are defining here.
    throw "calling toString() on right hand operand of operator + did not return a String";
  }
  return UNINTERCEPTED(a + b);
}

div(var a, var b) {
  if (checkNumbers(a, b, "num/ expects a number as second operand.")) {
    return JS("num", @"$0 / $1", a, b);
  }
  return UNINTERCEPTED(a / b);
}

mul(var a, var b) {
  if (checkNumbers(a, b, "num* expects a number as second operand.")) {
    return JS("num", @"$0 * $1", a, b);
  }
  return UNINTERCEPTED(a * b);
}

sub(var a, var b) {
  if (checkNumbers(a, b, "num- expects a number as second operand.")) {
    return JS("num", @"$0 - $1", a, b);
  }
  return UNINTERCEPTED(a - b);
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
  return UNINTERCEPTED(a % b);
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
  return UNINTERCEPTED(a ~/ b);
}

eq(var a, var b) {
  if (JS("bool", @"typeof $0 === 'object'", a)) {
    if (JS_HAS_EQUALS(a)) {
      return UNINTERCEPTED(a == b) === true;
    } else {
      return JS("bool", @"$0 === $1", a, b);
    }
  }
  // TODO(lrn): is NaN === NaN ? Is -0.0 === 0.0 ?
  return JS("bool", @"$0 === $1", a, b);
}

eqq(var a, var b) {
  return JS("bool", @"$0 === $1", a, b);
}

eqNull(var a) {
  if (JS("bool", @"typeof $0 === 'object'", a)) {
    if (JS_HAS_EQUALS(a)) {
      return UNINTERCEPTED(a == null) === true;
    } else {
      return false;
    }
  } else {
    return JS("bool", @"typeof $0 === 'undefined'", a);
  }
}

gt(var a, var b) {
  if (checkNumbers(a, b, "num> expects a number as second operand.")) {
    return JS("bool", @"$0 > $1", a, b);
  }
  return UNINTERCEPTED(a > b);
}

ge(var a, var b) {
  if (checkNumbers(a, b, "num>= expects a number as second operand.")) {
    return JS("bool", @"$0 >= $1", a, b);
  }
  return UNINTERCEPTED(a >= b);
}

lt(var a, var b) {
  if (checkNumbers(a, b, "num< expects a number as second operand.")) {
    return JS("bool", @"$0 < $1", a, b);
  }
  return UNINTERCEPTED(a < b);
}

le(var a, var b) {
  if (checkNumbers(a, b, "num<= expects a number as second operand.")) {
    return JS("bool", @"$0 <= $1", a, b);
  }
  return UNINTERCEPTED(a <= b);
}

shl(var a, var b) {
  // TODO(floitsch): inputs must be integers.
  if (checkNumbers(a, b, "int<< expects an int as second operand.")) {
    return JS("num", @"$0 << $1", a, b);
  }
  return UNINTERCEPTED(a << b);
}

shr(var a, var b) {
  // TODO(floitsch): inputs must be integers.
  if (checkNumbers(a, b, "int>> expects an int as second operand.")) {
    return JS("num", @"$0 >> $1", a, b);
  }
  return UNINTERCEPTED(a >> b);
}

and(var a, var b) {
  // TODO(floitsch): inputs must be integers.
  if (checkNumbers(a, b, "int& expects an int as second operand.")) {
    return JS("num", @"$0 & $1", a, b);
  }
  return UNINTERCEPTED(a & b);
}

or(var a, var b) {
  // TODO(floitsch): inputs must be integers.
  if (checkNumbers(a, b, "int| expects an int as second operand.")) {
    return JS("num", @"$0 | $1", a, b);
  }
  return UNINTERCEPTED(a | b);
}

xor(var a, var b) {
  // TODO(floitsch): inputs must be integers.
  if (checkNumbers(a, b, "int^ expects an int as second operand.")) {
    return JS("num", @"$0 ^ $1", a, b);
  }
  return UNINTERCEPTED(a ^ b);
}

not(var a) {
  if (JS("bool", @"typeof $0 === 'number'", a)) return JS("num", @"~$0", a);
  return UNINTERCEPTED(~a);
}

neg(var a) {
  if (JS("bool", @"typeof $0 === 'number'", a)) return JS("num", @"-$0", a);
  return UNINTERCEPTED(-a);
}

index(var a, var index) {
  if (a is String || isJSArray(a)) {
    if (!(index is int)) $throw('Illegal argument');
    if (index < 0 || index >= a.length) $throw('Out of bounds');
    return JS("Object", @"$0[$1]", a, index);
  }
  return UNINTERCEPTED(a[index]);
}

indexSet(var a, var index, var value) {
  if (isJSArray(a)) {
    if (!(index is int)) $throw('Illegal argument');
    if (index < 0 || index >= a.length) $throw('Out of bounds');
    return JS("Object", @"$0[$1] = $2", a, index, value);
  }
  return UNINTERCEPTED(a[index] = value);
}

builtin$add$1(var receiver, var value) {
  if (isJSArray(receiver)) {
    JS("Object", @"$0.push($1)", receiver, value);
    return;
  }
  return UNINTERCEPTED(receiver.add(value));
}

builtin$removeLast$0(var receiver) {
  if (isJSArray(receiver)) {
    return JS("Object", @"$0.pop()", receiver);
  }
  return UNINTERCEPTED(receiver.removeLast());
}

builtin$filter$1(var receiver, var predicate) {
  if (isJSArray(receiver)) {
    return JS("Object", @"$0.filter(function(v) { return $1(v) === true; })",
              receiver, predicate);
  }
  return UNINTERCEPTED(receiver.filter(predicate));
}


builtin$get$length(var receiver) {
  if (receiver is String || isJSArray(receiver)) {
    return JS("num", @"$0.length", receiver);
  }
  return UNINTERCEPTED(receiver.length);
}


builtin$toString$0(var value) {
  if (JS("bool", @"typeof $0 == 'object'", value)) {
    if (isJSArray(value)) {
      return "Instance of 'List'";
    }
    return UNINTERCEPTED(value.toString());
  }
  if (JS("bool", @"$0 === 0 && (1 / $0) < 0", value)) {
    return "-0.0";
  }
  if (value === null) return "null";
  if (JS("bool", @"typeof $0 == 'function'", value)) {
    return "Closure";
  }
  return JS("string", @"String($0)", value);
}


builtin$iterator$0(var receiver) {
  if (isJSArray(receiver)) {
    return new ListIterator(receiver);
  }
  return UNINTERCEPTED(receiver.iterator());
}

class ListIterator<T> implements Iterator<T> {
  int i;
  List<T> list;
  ListIterator(List<T> this.list) : i = 0;
  bool hasNext() => i < JS("int", @"$0.length", list);
  T next() {
    var value = JS("Object", @"$0[$1]", list, i);
    i += 1;
    return value;
  }
}

builtin$charCodeAt$1(var receiver, int index) {
  if (receiver is String) {
    return JS("string", @"$0.charCodeAt($1)", receiver, index);
  } else {
    return UNINTERCEPTED(receiver.charCodeAt(index));
  }
}

builtin$isEmpty$0(var receiver) {
  if (receiver is String || isJSArray(receiver)) {
    return JS("bool", @"$0.length === 0", receiver);
  }
  return UNINTERCEPTED(receiver.isEmpty());
}

class Primitives {
  static void printString(String string) {
    var hasConsole = JS("bool", @"typeof console == 'object'");
    if (hasConsole) {
      JS("void", @"console.log($0)", string);
    } else {
      JS("void", @"write($0)", string);
      JS("void", @"write('\n')");
    }
  }

  static String objectToString(Object object) {
    String name = JS('String', @'$0.constructor.name', object);
    if (name === null) {
      name = JS('String', @'$0.match(/^\s*function\s*(\S*)\s*\(/)[1]',
                JS('String', @'$0.constructor.toString()', object));
    }
    return "Instance of '$name'";
  }

  static List newList(int length) {
    if (length == null) return JS("Object", @"new Array()");
    if (!(length is int)) throw "Invalid argument";
    if (length < 0) throw "Negative size";
    return JS("Object", @"new Array($0)", length);
  }

  static num dateNow() => JS("num", @"Date.now()");

  static num mathFloor(num value) => JS("num", @"Math.floor($0)", value);
}

builtin$compareTo$1(a, b) {
  if (checkNumbers(a, b, 'illegal argument')) {
    if (a < b) {
      return -1;
    } else if (a > b) {
      return 1;
    } else if (a == b) {
      if (a == 0) {
        bool aIsNegative = a.isNegative();
        bool bIsNegative = b.isNegative();
        if (aIsNegative == bIsNegative) return 0;
        if (aIsNegative) return -1;
        return 1;
      }
      return 0;
    } else if (a.isNaN()) {
      if (b.isNaN()) {
        return 0;
      }
      return 1;
    } else {
      return -1;
    }
  } else if (a is String) {
    throw 'String.compareTo is not implemented';
  } else {
    return UNINTERCEPTED(a.compareTo(b));
  }
}
