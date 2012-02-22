// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('js_helper');

#import('coreimpl.dart');

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
      checkNull(b);
      throw new IllegalArgumentException(message);
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
    if (b === 0) throw new IntegerDivisionByZeroException();
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
    if (b === 0) throw new IntegerDivisionByZeroException();
    return (a / b).truncate();
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
    if (b < 0) throw new IllegalArgumentException(b);
    return JS("num", @"$0 << $1", a, b);
  }
  return UNINTERCEPTED(a << b);
}

shr(var a, var b) {
  // TODO(floitsch): inputs must be integers.
  if (checkNumbers(a, b, "int>> expects an int as second operand.")) {
    if (b < 0) throw new IllegalArgumentException(b);
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
  checkNull(a);
  if (a is String || isJSArray(a)) {
    if (index is !int) {
      if (index is !num) throw new IllegalArgumentException(index);
      if (index.truncate() !== index) throw new IllegalArgumentException(index);
    }
    if (index < 0 || index >= a.length) {
      throw new IndexOutOfRangeException(index);
    }
    return JS("Object", @"$0[$1]", a, index);
  }
  return UNINTERCEPTED(a[index]);
}

indexSet(var a, var index, var value) {
  checkNull(a);
  if (isJSArray(a)) {
    if (!(index is int)) {
      throw new IllegalArgumentException(index);
    }
    if (index < 0 || index >= a.length) {
      throw new IndexOutOfRangeException(index);
    }
    return JS("Object", @"$0[$1] = $2", a, index, value);
  }
  return UNINTERCEPTED(a[index] = value);
}

builtin$add$1(var receiver, var value) {
  checkNull(receiver);
  if (isJSArray(receiver)) {
    JS("Object", @"$0.push($1)", receiver, value);
    return;
  }
  return UNINTERCEPTED(receiver.add(value));
}

builtin$removeLast$0(var receiver) {
  checkNull(receiver);
  if (isJSArray(receiver)) {
    if (receiver.length === 0) throw new IndexOutOfRangeException(-1);
    return JS("Object", @"$0.pop()", receiver);
  }
  return UNINTERCEPTED(receiver.removeLast());
}

builtin$filter$1(var receiver, var predicate) {
  checkNull(receiver);
  if (isJSArray(receiver)) {
    return JS("Object", @"$0.filter(function(v) { return $1(v) === true; })",
              receiver, predicate);
  }
  return UNINTERCEPTED(receiver.filter(predicate));
}


builtin$get$length(var receiver) {
  checkNull(receiver);
  if (receiver is String || isJSArray(receiver)) {
    return JS("num", @"$0.length", receiver);
  }
  return UNINTERCEPTED(receiver.length);
}

builtin$set$length(receiver, newLength) {
  checkNull(receiver);
  if (isJSArray(receiver)) {
    checkNull(newLength); // TODO(ahe): This is not specified but co19 tests it.
    if (newLength is !int) throw new IllegalArgumentException(newLength);
    if (newLength < 0) throw new IndexOutOfRangeException(newLength);
    JS('void', @"$0.length = $1", receiver, newLength);
  } else {
    UNINTERCEPTED(receiver.length = newLength);
  }
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


builtin$iterator$0(receiver) {
  checkNull(receiver);
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
    if (!hasNext()) throw new NoMoreElementsException();
    var value = JS("Object", @"$0[$1]", list, i);
    i += 1;
    return value;
  }
}

builtin$charCodeAt$1(var receiver, int index) {
  checkNull(receiver);
  if (receiver is String) {
    if (index is !num) throw new IllegalArgumentException(index);
    if (index < 0) throw new IndexOutOfRangeException(index);
    if (index >= receiver.length) throw new IndexOutOfRangeException(index);
    return JS("string", @"$0.charCodeAt($1)", receiver, index);
  } else {
    return UNINTERCEPTED(receiver.charCodeAt(index));
  }
}

builtin$isEmpty$0(receiver) {
  checkNull(receiver);
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

  static List newList(length) {
    if (length == null) return JS("Object", @"new Array()");
    if ((length is !int) || (length < 0)) {
      throw new IllegalArgumentException(length);
    }
    return JS("Object", @"new Array($0)", length);
  }

  static num dateNow() => JS("num", @"Date.now()");

  static num mathFloor(num value) => JS("num", @"Math.floor($0)", value);

  static brokenDownDateToSecondsSinceEpoch(
      int years, int month, int day, int hours, int minutes, int seconds,
      bool isUtc) {
    throw 'Primitives.brokenDownDateToSecondsSinceEpoch is not implemented';
  }

  static int getCurrentMs() {
    throw 'Primitives.getCurrentMs is not implemented';
  }

  static int getYear(int secondsSinceEpoch, bool isUtc) {
    throw 'Primitives.getYear is not implemented';
  }

  static int getMonth(int secondsSinceEpoch, bool isUtc) {
    throw 'Primitives.getMonth is not implemented';
  }

  static int getDay_(int secondsSinceEpoch, bool isUtc);

  static int getHours(int secondsSinceEpoch, bool isUtc) {
    throw 'Primitives.getHours is not implemented';
  }

  static int getMinutes(int secondsSinceEpoch, bool isUtc) {
    throw 'Primitives.getMinutes is not implemented';
  }

  static int getSeconds(int secondsSinceEpoch, bool isUtc) {
    throw 'Primitives.getSeconds is not implemented';
  }

  static String stringFromCharCodes(charCodes) {
    for (var i in charCodes) {
      if (i is !int) throw new IllegalArgumentException(i);
    }
    return JS('String', @'String.fromCharCode.apply($0, $1)', null, charCodes);
  }
}

builtin$compareTo$1(a, b) {
  checkNull(a);
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
    if (b is !String) throw new IllegalArgumentException(b);
    return (a === b) ? 0 : JS('bool', @'$0 < $1', a, b) ? -1 : 1;
  } else {
    return UNINTERCEPTED(a.compareTo(b));
  }
}

/**
 * Called by generated code to throw an illegal-argument exception,
 * for example, if a non-integer index is given to an optimized
 * indexed access.
 */
iae(argument) {
  throw new IllegalArgumentException(argument);
}

/**
 * Called by generated code to throw an index-out-of-range exception,
 * for example, if a bounds check fails in an optimized indexed
 * access.
 */
ioore(index) {
  throw new IndexOutOfRangeException(index);
}

builtin$addAll$1(receiver, collection) {
  checkNull(receiver);
  if (!isJSArray(receiver)) return UNINTERCEPTED(receiver.addAll(collection));

  // TODO(ahe): Use for-in when it is implemented correctly.
  var iterator = collection.iterator();
  while (iterator.hasNext()) {
    receiver.add(iterator.next());
  }
}

// TODO(ahe): Investigate why this method causes a compiler crash.
XXX_builtin$addLast$1(receiver, value) {
  checkNull(receiver);
  if (!isJSArray(receiver)) return UNINTERCEPTED(receiver.addLast(value));

  throw @'builtin$addLast$1 is not implemented';
}

builtin$clear$0(receiver) {
  checkNull(receiver);
  if (!isJSArray(receiver)) return UNINTERCEPTED(receiver.clear());
  receiver.length = 0;
}

// TODO(ahe): Investigate why this method causes a compiler crash.
XXX_builtin$forEach$1(receiver, f) {
  checkNull(receiver);
  if (!isJSArray(receiver)) return UNINTERCEPTED(receiver.forEach(f));

  throw @'builtin$forEach$1 is not implemented';
}

builtin$getRange$2(receiver, start, length) {
  checkNull(receiver);
  if (!isJSArray(receiver)) {
    return UNINTERCEPTED(receiver.getRange(start, length));
  }
  if (0 === length) return [];
  checkNull(start); // TODO(ahe): This is not specified but co19 tests it.
  checkNull(length); // TODO(ahe): This is not specified but co19 tests it.
  if (start is !int) throw new IllegalArgumentException(start);
  if (length is !int) throw new IllegalArgumentException(length);
  if (start < 0) throw new IndexOutOfRangeException(start);
  var end = start + length;
  if (end > receiver.length) {
    throw new IndexOutOfRangeException(length);
  }
  if (length < 0) throw new IllegalArgumentException(length);
  return JS("Object", @"$0.slice($1, $2)", receiver, start, end);
}

builtin$indexOf$1(receiver, element) {
  checkNull(receiver);
  if (isJSArray(receiver) || receiver is String) {
    return builtin$indexOf$2(receiver, element, 0);
  }
  return UNINTERCEPTED(receiver.indexOf(element));
}

builtin$indexOf$2(receiver, element, start) {
  checkNull(receiver);
  if (isJSArray(receiver)) {
    if (start is !int) throw new IllegalArgumentException(start);
    var length = JS("num", @"$0.length", receiver);
    return Arrays.indexOf(receiver, element, start, length);
  } else if (receiver is String) {
    checkNull(element);
    if (start is !int) throw new IllegalArgumentException(start);
    if (element is !String) throw new IllegalArgumentException(element);
    if (start < 0) return -1; // TODO(ahe): Is this correct?
    return JS('int', @'$0.indexOf($1, $2)', receiver, element, start);
  }
  return UNINTERCEPTED(receiver.indexOf(element, start));
}

builtin$insertRange$2(receiver, start, length) {
  checkNull(receiver);
  if (isJSArray(receiver)) {
    return builtin$insertRange$3(receiver, start, length, null);
  }
  return UNINTERCEPTED(receiver.insertRange(start, length));
}

builtin$insertRange$3(receiver, start, length, initialValue) {
  checkNull(receiver);
  if (!isJSArray(receiver)) {
    return UNINTERCEPTED(receiver.insertRange(start, length, initialValue));
  }
  return listInsertRange(receiver, start, length, initialValue);
}

listInsertRange(receiver, start, length, initialValue) {
  if (length === 0) {
    return;
  }
  checkNull(start); // TODO(ahe): This is not specified but co19 tests it.
  checkNull(length); // TODO(ahe): This is not specified but co19 tests it.
  if (length is !int) throw new IllegalArgumentException(length);
  if (length < 0) throw new IllegalArgumentException(length);
  if (start is !int) throw new IllegalArgumentException(start);

  var receiverLength = JS("num", @"$0.length", receiver);
  if (start < 0 || start > receiverLength) {
    throw new IndexOutOfRangeException(start);
  }
  receiver.length = receiverLength + length;
  Arrays.copy(receiver,
              start,
              receiver,
              start + length,
              receiverLength - start);
  if (initialValue !== null) {
    for (int i = start; i < start + length; i++) {
      receiver[i] = initialValue;
    }
  }
  receiver.length = receiverLength + length;
}

builtin$last$0(receiver) {
  checkNull(receiver);
  if (!isJSArray(receiver)) {
    return UNINTERCEPTED(receiver.last());
  }
  return receiver[receiver.length - 1];
}

builtin$lastIndexOf$1(receiver, element) {
  checkNull(receiver);
  if (isJSArray(receiver)) {
    var start = JS("num", @"$0.length", receiver);
    return Arrays.lastIndexOf(receiver, element, start);
  } else if (receiver is String) {
    checkNull(element);
    if (element is !String) throw new IllegalArgumentException(element);
    return JS('int', @'$0.lastIndexOf($1)', receiver, element);
  }
  return UNINTERCEPTED(receiver.lastIndexOf(element));
}

builtin$lastIndexOf$2(receiver, element, start) {
  checkNull(receiver);
  if (isJSArray(receiver)) {
    return Arrays.lastIndexOf(receiver, element, start);
  } else if (receiver is String) {
    checkNull(element);
    if (element is !String) throw new IllegalArgumentException(element);
    if (start !== null) {
      if (start is !num) throw new IllegalArgumentException(start);
      if (start < 0) return -1;
      if (start >= receiver.length) start = receiver.length - 1;
    }
    return rawStringLastIndexOf(receiver, element, start);
  }
  return UNINTERCEPTED(receiver.lastIndexOf(element, start));
}

rawStringLastIndexOf(receiver, element, start)
  => JS('int', @'$0.lastIndexOf($1, $2)', receiver, element, start);

builtin$removeRange$2(receiver, start, length) {
  checkNull(receiver);
  if (!isJSArray(receiver)) {
    return UNINTERCEPTED(receiver.removeRange(start, length));
  }
  if (length == 0) {
    return;
  }
  checkNull(start); // TODO(ahe): This is not specified but co19 tests it.
  checkNull(length); // TODO(ahe): This is not specified but co19 tests it.
  if (start is !int) throw new IllegalArgumentException(start);
  if (length is !int) throw new IllegalArgumentException(length);
  if (length < 0) throw new IllegalArgumentException(length);
  var receiverLength = JS("num", @"$0.length", receiver);
  if (start < 0 || start >= receiverLength) {
    throw new IndexOutOfRangeException(start);
  }
  if (start + length > receiverLength) {
    throw new IndexOutOfRangeException(start + length);
  }
  Arrays.copy(receiver,
              start + length,
              receiver,
              start,
              receiverLength - length - start);
  receiver.length = receiverLength - length;
}

builtin$setRange$3(receiver, start, length, from) {
  checkNull(receiver);
  if (isJSArray(receiver)) {
    return builtin$setRange$4(receiver, start, length, from, 0);
  }
  return UNINTERCEPTED(receiver.setRange(start, length, from));
}

builtin$setRange$4(receiver, start, length, from, startFrom) {
  checkNull(receiver);
  if (!isJSArray(receiver)) {
    return UNINTERCEPTED(receiver.setRange(start, length, from, startFrom));
  }

  if (length === 0) return;
  checkNull(start); // TODO(ahe): This is not specified but co19 tests it.
  checkNull(length); // TODO(ahe): This is not specified but co19 tests it.
  checkNull(from); // TODO(ahe): This is not specified but co19 tests it.
  checkNull(startFrom); // TODO(ahe): This is not specified but co19 tests it.
  if (start is !int) throw new IllegalArgumentException(start);
  if (length is !int) throw new IllegalArgumentException(length);
  if (startFrom is !int) throw new IllegalArgumentException(startFrom);
  if (length < 0) throw new IllegalArgumentException(length);
  if (start < 0) throw new IndexOutOfRangeException(start);
  if (start + length > receiver.length) {
    throw new IndexOutOfRangeException(start + length);
  }

  Arrays.copy(from, startFrom, receiver, start, length);
}

builtin$some$1(receiver, f) {
  checkNull(receiver);
  if (!isJSArray(receiver)) return UNINTERCEPTED(receiver.some(f));

  return Collections.some(receiver, f);
}

builtin$sort$1(receiver, compare) {
  checkNull(receiver);
  if (!isJSArray(receiver)) return UNINTERCEPTED(receiver.sort(compare));

  DualPivotQuicksort.sort(receiver, compare);
}

checkNull(object) {
  if (object === null) throw new NullPointerException();
}

checkNum(value) {
  checkNull(value);
  if (value is !num) throw new IllegalArgumentException(value);
  return value;
}

builtin$isNegative$0(receiver) {
  checkNull(receiver);
  if (receiver is num) {
    return (receiver === 0) ? (1 / receiver) < 0 : receiver < 0;
  } else {
    return UNINTERCEPTED(receiver.isNegative());
  }
}

builtin$isNaN$0(receiver) {
  checkNull(receiver);
  if (receiver is num) {
    return JS("bool", @"isNaN($0)", receiver);
  } else {
    return UNINTERCEPTED(receiver.isNegative());
  }
}

builtin$remainder$1(a, b) {
  checkNull(a);
  if (checkNumbers(a, b, "num.remainder expects a number as second operand.")) {
    if (b === 0) throw new IntegerDivisionByZeroException();
    return JS("num", @"$0 % $1", a, b);
  } else {
    return UNINTERCEPTED(a.remainder(b));
  }
}

builtin$abs$0(receiver) {
  checkNull(receiver);
  if (receiver is !num) return UNINTERCEPTED(receiver.abs());

  return JS("num", @"Math.abs($0)", receiver);
}

builtin$toInt$0(receiver) {
  checkNull(receiver);
  if (receiver is !num) return UNINTERCEPTED(receiver.toInt());

  if (receiver.isNaN()) throw new BadNumberFormatException("NaN");

  if (receiver.isInfinite()) throw new BadNumberFormatException("Infinity");

  var truncated = receiver.truncate();
  return JS("bool", @"$0 == -0.0", truncated) ? 0 : truncated;
}

builtin$ceil$0(receiver) {
  checkNull(receiver);
  if (receiver is !num) return UNINTERCEPTED(receiver.ceil());

  return JS("num", @"Math.ceil($0)", receiver);
}

builtin$floor$0(receiver) {
  checkNull(receiver);
  if (receiver is !num) return UNINTERCEPTED(receiver.floor());

  return JS("num", @"Math.floor($0)", receiver);
}

builtin$isInfinite$0(receiver) {
  checkNull(receiver);
  if (receiver is !num) return UNINTERCEPTED(receiver.isInfinite());

  return JS("bool", @"$0 == Infinity", receiver)
    || JS("bool", @"$0 == -Infinity", receiver);
}

builtin$negate$0(receiver) {
  checkNull(receiver);
  if (receiver is !num) return UNINTERCEPTED(receiver.negate());

  return JS("num", @"-$0", receiver);
}

builtin$round$0(receiver) {
  checkNull(receiver);
  if (receiver is !num) return UNINTERCEPTED(receiver.round());

  return JS("num", @"Math.round($0)", receiver);
}

builtin$toDouble$0(receiver) {
  checkNull(receiver);
  if (receiver is !num) return UNINTERCEPTED(receiver.toDouble());

  // TODO(ahe): Just return receiver?
  return JS("double", @"$0 + 0", receiver);
}

builtin$truncate$0(receiver) {
  checkNull(receiver);
  if (receiver is !num) return UNINTERCEPTED(receiver.truncate());

  return receiver < 0 ? receiver.ceil() : receiver.floor();
}

builtin$toStringAsFixed$1(receiver, fractionDigits) {
  checkNull(receiver);
  if (receiver is !num) {
    return UNINTERCEPTED(receiver.toStringAsFixed(fractionDigits));
  }
  checkNum(fractionDigits);

  return JS("String", @"$0.toFixed($1)", receiver, fractionDigits);
}

builtin$toStringAsExponential$1(receiver, fractionDigits) {
  checkNull(receiver);
  if (receiver is !num) {
    return UNINTERCEPTED(receiver.toStringAsExponential(fractionDigits));
  }
  checkNum(fractionDigits);

  return JS("String", @"$0.toExponential($1)", receiver, fractionDigits);
}

builtin$toStringAsPrecision$1(receiver, fractionDigits) {
  checkNull(receiver);
  if (receiver is !num) {
    return UNINTERCEPTED(receiver.toStringAsPrecision(fractionDigits));
  }
  checkNum(fractionDigits);

  return JS("String", @"$0.toPrecision($1)", receiver, fractionDigits);
}

builtin$toRadixString$1(receiver, radix) {
  checkNull(receiver);
  if (receiver is !num) {
    return UNINTERCEPTED(receiver.toRadixString(radix));
  }
  checkNum(radix);

  return JS("String", @"$0.toString($1)", receiver, radix);
}

builtin$allMatches$1(receiver, str) {
  checkNull(receiver);
  if (receiver is !String) return UNINTERCEPTED(receiver.allMatches(str));

  throw 'String.allMatches is not implemented';
}

builtin$concat$1(receiver, other) {
  checkNull(receiver);
  if (receiver is !String) return UNINTERCEPTED(receiver.concat(other));

  if (other is !String) throw new IllegalArgumentException(other);
  return JS('String', @'$0.concat($1)', receiver, other);
}

builtin$contains$2(receiver, other, startIndex) {
  checkNull(receiver);
  if (receiver is !String) {
    return UNINTERCEPTED(receiver.contains(other, startIndex));
  }
  checkNull(other);
  if (other is !String) {
    throw 'String.contains with non-String is not implemented';
  }
  if ((startIndex !== null) || (startIndex is !num)) {
    throw new IllegalArgumentException(startIndex);
  }
  return receiver.indexOf(other, startIndex) >= 0;
}

builtin$endsWith$1(receiver, other) {
  checkNull(receiver);
  if (receiver is !String) return UNINTERCEPTED(receiver.endsWith(other));

  checkNull(other);
  if (other is !String) throw new IllegalArgumentException(other);

  int receiverLength = receiver.length;
  int otherLength = other.length;
  if (otherLength > receiverLength) return false;
  return other == receiver.substring(receiverLength - otherLength);
}

builtin$replaceAll$2(receiver, from, to) {
  checkNull(receiver);
  if (receiver is !String) return UNINTERCEPTED(receiver.replaceAll(from, to));

  throw 'String.replaceAll is not implemented';
}

builtin$replaceFirst$2(receiver, from, to) {
  checkNull(receiver);
  if (receiver is !String) {
    return UNINTERCEPTED(receiver.replaceFirst(from, to));
  }
  if (from is !String) throw new IllegalArgumentException(from);
  if (from is !String) throw new IllegalArgumentException(from);

  return JS('String', @'$0.replace($1, $2)', receiver, from, to);
}

builtin$split$1(receiver, pattern) {
  checkNull(receiver);
  if (receiver is !String) return UNINTERCEPTED(receiver.split(pattern));
  checkNull(pattern);
  if (pattern is !String) throw new IllegalArgumentException(pattern);

  return JS('List', @'$0.split($1)', receiver, pattern);
}

builtin$splitChars$0(receiver) {
  checkNull(receiver);
  if (receiver is !String) return UNINTERCEPTED(receiver.splitChars());

  return JS('List', @'$0.split("")', receiver);
}

builtin$startsWith$1(receiver, other) {
  checkNull(receiver);
  if (receiver is !String) return UNINTERCEPTED(receiver.startsWith(other));
  checkNull(other);
  if (other is !String) throw new IllegalArgumentException(other);

  int length = other.length;
  if (length > receiver.length) return false;
  return other === JS('String', @'$0.substring(0, $1)', receiver, length);
}

builtin$substring$1(receiver, startIndex) {
  checkNull(receiver);
  if (receiver is !String) return UNINTERCEPTED(receiver.substring(startIndex));

  return builtin$substring$2(receiver, startIndex, null);
}

builtin$substring$2(receiver, startIndex, endIndex) {
  checkNull(receiver);
  if (receiver is !String) {
    return UNINTERCEPTED(receiver.substring(startIndex, endIndex));
  }
  checkNum(startIndex);
  var length = receiver.length;
  if (endIndex === null) endIndex = length;
  checkNum(endIndex);
  if (startIndex < 0 ) throw new IndexOutOfRangeException(startIndex);
  if (startIndex > endIndex) throw new IndexOutOfRangeException(startIndex);
  if (endIndex > length) throw new IndexOutOfRangeException(endIndex);
  return substringUnchecked(receiver, startIndex, endIndex);
}

substringUnchecked(receiver, startIndex, endIndex)
  => JS('String', @'$0.substring($1, $2)', receiver, startIndex, endIndex);


builtin$toLowerCase$0(receiver) {
  checkNull(receiver);
  if (receiver is !String) return UNINTERCEPTED(receiver.toLowerCase());

  return JS('String', @'$0.toLowerCase()', receiver);
}

builtin$toUpperCase$0(receiver) {
  checkNull(receiver);
  if (receiver is !String) return UNINTERCEPTED(receiver.toUpperCase());

  return JS('String', @'$0.toUpperCase()', receiver);
}

builtin$trim$0(receiver) {
  checkNull(receiver);
  if (receiver is !String) return UNINTERCEPTED(receiver.trim());

  return JS('String', @'$0.trim()', receiver);
}

class MathNatives {
  static int parseInt(str) {
    checkNull(str);
    if (str is !String) throw new IllegalArgumentException(str);
    var trimmed = str.trim();
    if (!JS('bool', @'/^(0[xX])?[+-]?[0-9]+$/.test($0)', trimmed)) {
      throw new BadNumberFormatException(str);
    }
    var ret = JS("num", @"parseInt($0, 10)", str);
    if (ret.isNaN()) throw new BadNumberFormatException(str);
    return ret;
  }

  static double parseDouble(String str) {
    checkNull(str);
    if (str is !String) throw new IllegalArgumentException();
    var ret = JS("num", @"parseFloat($0)", str);
    if (ret.isNaN() && str != 'NaN') throw new BadNumberFormatException(str);
    return ret;
  }

  static double sqrt(num value)
    => JS("double", @"Math.sqrt($0)", checkNum(value));

  static double sin(num value)
    => JS("double", @"Math.sin($0)", checkNum(value));

  static double cos(num value)
    => JS("double", @"Math.cos($0)", checkNum(value));

  static double tan(num value)
    => JS("double", @"Math.tan($0)", checkNum(value));

  static double acos(num value)
    => JS("double", @"Math.acos($0)", checkNum(value));

  static double asin(num value)
    => JS("double", @"Math.asin($0)", checkNum(value));

  static double atan(num value)
    => JS("double", @"Math.atan($0)", checkNum(value));

  static double atan2(num a, num b)
    => JS("double", @"Math.atan2($0, $1)", checkNum(a), checkNum(b));

  static double exp(num value)
    => JS("double", @"Math.exp($0)", checkNum(value));

  static double log(num value)
    => JS("double", @"Math.log($0)", checkNum(value));

  static num pow(num value, num exponent) {
    checkNum(value);
    checkNum(exponent);
    return JS("num", @"Math.pow($0, $1)", value, exponent);
  }

  static double random() => JS("double", @"Math.random()");
}

builtin$hashCode$0(receiver) {
  if (receiver is num) return receiver & 0x1FFFFFFF;
  if (receiver is String) {
    throw 'String.hashCode is not implemented';
  }
  if (isJSArray(receiver)) {
    throw 'List.hashCode is not implemented';
  }
  return UNINTERCEPTED(receiver.hashCode());
}
