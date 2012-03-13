// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('js_helper');

#import('coreimpl.dart');

#source('constant_map.dart');
#source('date_helper.dart');
#source('regexp_helper.dart');
#source('string_helper.dart');

/**
 * Returns true if both arguments are numbers.
 *
 * If only the first argument is a number, an
 * [IllegalArgumentException] with the other argument is thrown.
 */
bool checkNumbers(var a, var b) {
  if (a is num) {
    if (b is num) {
      return true;
    } else {
      checkNull(b);
      throw new IllegalArgumentException(b);
    }
  }
  return false;
}


bool isJsArray(var value) {
  return value !== null && JS('bool', @'$0.constructor === Array', value);
}


add(var a, var b) {
  if (checkNumbers(a, b)) {
    return JS('num', @'$0 + $1', a, b);
  } else if (a is String) {
    b = b.toString();
    if (b is String) {
      return JS('String', @'$0 + $1', a, b);
    }
    checkNull(b);
    throw new IllegalArgumentException(b);
  }
  checkNull(a);
  return UNINTERCEPTED(a + b);
}

div(var a, var b) {
  if (checkNumbers(a, b)) {
    return JS('num', @'$0 / $1', a, b);
  }
  checkNull(a);
  return UNINTERCEPTED(a / b);
}

mul(var a, var b) {
  if (checkNumbers(a, b)) {
    return JS('num', @'$0 * $1', a, b);
  }
  checkNull(a);
  return UNINTERCEPTED(a * b);
}

sub(var a, var b) {
  if (checkNumbers(a, b)) {
    return JS('num', @'$0 - $1', a, b);
  }
  checkNull(a);
  return UNINTERCEPTED(a - b);
}

mod(var a, var b) {
  if (checkNumbers(a, b)) {
    // Euclidean Modulo.
    int result = JS('num', @'$0 % $1', a, b);
    if (result == 0) return 0;  // Make sure we don't return -0.0.
    if (result > 0) return result;
    if (b < 0) {
      return result - b;
    } else {
      return result + b;
    }
  }
  checkNull(a);
  return UNINTERCEPTED(a % b);
}

tdiv(var a, var b) {
  if (checkNumbers(a, b)) {
    return (a / b).truncate();
  }
  checkNull(a);
  return UNINTERCEPTED(a ~/ b);
}

eq(var a, var b) {
  if (JS('bool', @'typeof $0 === "object"', a)) {
    if (JS_HAS_EQUALS(a)) {
      return UNINTERCEPTED(a == b) === true;
    } else {
      return JS('bool', @'$0 === $1', a, b);
    }
  }
  // TODO(lrn): is NaN === NaN ? Is -0.0 === 0.0 ?
  return JS('bool', @'$0 === $1', a, b);
}

eqq(var a, var b) {
  return JS('bool', @'$0 === $1', a, b);
}

eqNull(var a) {
  if (JS('bool', @'typeof $0 === "object"', a)) {
    if (JS_HAS_EQUALS(a)) {
      return UNINTERCEPTED(a == null) === true;
    } else {
      return false;
    }
  } else {
    return JS('bool', @'typeof $0 === "undefined"', a);
  }
}

gt(var a, var b) {
  if (checkNumbers(a, b)) {
    return JS('bool', @'$0 > $1', a, b);
  }
  checkNull(a);
  return UNINTERCEPTED(a > b);
}

ge(var a, var b) {
  if (checkNumbers(a, b)) {
    return JS('bool', @'$0 >= $1', a, b);
  }
  checkNull(a);
  return UNINTERCEPTED(a >= b);
}

lt(var a, var b) {
  if (checkNumbers(a, b)) {
    return JS('bool', @'$0 < $1', a, b);
  }
  checkNull(a);
  return UNINTERCEPTED(a < b);
}

le(var a, var b) {
  if (checkNumbers(a, b)) {
    return JS('bool', @'$0 <= $1', a, b);
  }
  checkNull(a);
  return UNINTERCEPTED(a <= b);
}

shl(var a, var b) {
  // TODO(floitsch): inputs must be integers.
  if (checkNumbers(a, b)) {
    if (b < 0) throw new IllegalArgumentException(b);
    return JS('num', @'$0 << $1', a, b);
  }
  checkNull(a);
  return UNINTERCEPTED(a << b);
}

shr(var a, var b) {
  // TODO(floitsch): inputs must be integers.
  if (checkNumbers(a, b)) {
    if (b < 0) throw new IllegalArgumentException(b);
    return JS('num', @'$0 >> $1', a, b);
  }
  checkNull(a);
  return UNINTERCEPTED(a >> b);
}

and(var a, var b) {
  // TODO(floitsch): inputs must be integers.
  if (checkNumbers(a, b)) {
    return JS('num', @'$0 & $1', a, b);
  }
  checkNull(a);
  return UNINTERCEPTED(a & b);
}

or(var a, var b) {
  // TODO(floitsch): inputs must be integers.
  if (checkNumbers(a, b)) {
    return JS('num', @'$0 | $1', a, b);
  }
  checkNull(a);
  return UNINTERCEPTED(a | b);
}

xor(var a, var b) {
  // TODO(floitsch): inputs must be integers.
  if (checkNumbers(a, b)) {
    return JS('num', @'$0 ^ $1', a, b);
  }
  checkNull(a);
  return UNINTERCEPTED(a ^ b);
}

not(var a) {
  if (JS('bool', @'typeof $0 === "number"', a)) return JS('num', @'~$0', a);
  checkNull(a);
  return UNINTERCEPTED(~a);
}

neg(var a) {
  if (JS('bool', @'typeof $0 === "number"', a)) return JS('num', @'-$0', a);
  checkNull(a);
  return UNINTERCEPTED(-a);
}

index(var a, var index) {
  checkNull(a);
  if (a is String || isJsArray(a)) {
    if (index is !int) {
      if (index is !num) throw new IllegalArgumentException(index);
      if (index.truncate() !== index) throw new IllegalArgumentException(index);
    }
    if (index < 0 || index >= a.length) {
      throw new IndexOutOfRangeException(index);
    }
    return JS('Object', @'$0[$1]', a, index);
  }
  checkNull(a);
  return UNINTERCEPTED(a[index]);
}

indexSet(var a, var index, var value) {
  checkNull(a);
  if (isJsArray(a)) {
    if (!(index is int)) {
      throw new IllegalArgumentException(index);
    }
    if (index < 0 || index >= a.length) {
      throw new IndexOutOfRangeException(index);
    }
    checkMutable(a, 'indexed set');
    return JS('Object', @'$0[$1] = $2', a, index, value);
  }
  checkNull(a);
  UNINTERCEPTED(a[index] = value);
  return value;
}

checkMutable(list, reason) {
  if (JS('bool', @'!!($0.immutable$list)', list)) {
    throw new UnsupportedOperationException(reason);
  }
}

builtin$add$1(var receiver, var value) {
  checkNull(receiver);
  if (isJsArray(receiver)) {
    checkGrowable(receiver, 'add');
    JS('Object', @'$0.push($1)', receiver, value);
    return;
  }
  return UNINTERCEPTED(receiver.add(value));
}

builtin$removeLast$0(var receiver) {
  checkNull(receiver);
  if (isJsArray(receiver)) {
    checkGrowable(receiver, 'removeLast');
    if (receiver.length === 0) throw new IndexOutOfRangeException(-1);
    return JS('Object', @'$0.pop()', receiver);
  }
  return UNINTERCEPTED(receiver.removeLast());
}

builtin$filter$1(var receiver, var predicate) {
  checkNull(receiver);
  if (!isJsArray(receiver)) return UNINTERCEPTED(receiver.filter(predicate));

  return Collections.filter(receiver, [], predicate);
}


builtin$get$length(var receiver) {
  checkNull(receiver);
  if (receiver is String || isJsArray(receiver)) {
    return JS('num', @'$0.length', receiver);
  }
  return UNINTERCEPTED(receiver.length);
}

builtin$set$length(receiver, newLength) {
  checkNull(receiver);
  if (isJsArray(receiver)) {
    checkNull(newLength); // TODO(ahe): This is not specified but co19 tests it.
    if (newLength is !int) throw new IllegalArgumentException(newLength);
    if (newLength < 0) throw new IndexOutOfRangeException(newLength);
    checkGrowable(receiver, 'set length');
    JS('void', @'$0.length = $1', receiver, newLength);
  } else {
    UNINTERCEPTED(receiver.length = newLength);
  }
  return newLength;
}

checkGrowable(list, reason) {
  if (JS('bool', @'!!($0.fixed$length)', list)) {
    throw new UnsupportedOperationException(reason);
  }
}

builtin$toString$0(var value) {
  if (JS('bool', @'typeof $0 == "object"', value)) {
    if (isJsArray(value)) {
      return Collections.collectionToString(value);
    } else {
      return UNINTERCEPTED(value.toString());
    }
  }
  if (JS('bool', @'$0 === 0 && (1 / $0) < 0', value)) {
    return '-0.0';
  }
  if (value === null) return 'null';
  if (JS('bool', @'typeof $0 == "function"', value)) {
    return 'Closure';
  }
  return JS('string', @'String($0)', value);
}


builtin$iterator$0(receiver) {
  checkNull(receiver);
  if (isJsArray(receiver)) {
    return new ListIterator(receiver);
  }
  return UNINTERCEPTED(receiver.iterator());
}

class ListIterator<T> implements Iterator<T> {
  int i;
  List<T> list;
  ListIterator(List<T> this.list) : i = 0;
  bool hasNext() => i < JS('int', @'$0.length', list);
  T next() {
    if (!hasNext()) throw new NoMoreElementsException();
    var value = JS('Object', @'$0[$1]', list, i);
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
    return JS('int', @'$0.charCodeAt($1)', receiver, index);
  } else {
    return UNINTERCEPTED(receiver.charCodeAt(index));
  }
}

builtin$isEmpty$0(receiver) {
  checkNull(receiver);
  if (receiver is String || isJsArray(receiver)) {
    return JS('bool', @'$0.length === 0', receiver);
  }
  return UNINTERCEPTED(receiver.isEmpty());
}

class Primitives {
  static void printString(String string) {
    var hasConsole = JS('bool', @'typeof console == "object"');
    if (hasConsole) {
      JS('void', @'console.log($0)', string);
    } else {
      JS('void', @'write($0)', string);
      JS('void', @'write("\n")');
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
    if (length === null) return JS('Object', @'new Array()');
    if ((length is !int) || (length < 0)) {
      throw new IllegalArgumentException(length);
    }
    var result = JS('Object', @'new Array($0)', length);
    JS('void', @'$0.fixed$length = $1', result, true);
    return result;
  }

  static num dateNow() => JS('num', @'Date.now()');

  static String stringFromCharCodes(charCodes) {
    for (var i in charCodes) {
      if (i is !int) throw new IllegalArgumentException(i);
    }
    return JS('String', @'String.fromCharCode.apply($0, $1)', null, charCodes);
  }

  static valueFromDecomposedDate(years, month, day, hours, minutes, seconds,
                                 milliseconds, isUtc) {
    checkInt(years);
    checkInt(month);
    if (month < 1 || 12 < month) throw new IllegalArgumentException(month);
    checkInt(day);
    if (day < 1 || 31 < day) throw new IllegalArgumentException(day);
    checkInt(hours);
    if (hours < 0 || 24 < hours) throw new IllegalArgumentException(hours);
    checkInt(minutes);
    if (minutes < 0 || 59 < minutes) {
      throw new IllegalArgumentException(minutes);
    }
    checkInt(seconds);
    if (seconds < 0 || 59 < seconds) {
      // TODO(ahe): Leap seconds?
      throw new IllegalArgumentException(seconds);
    }
    checkInt(milliseconds);
    if (milliseconds < 0 || 999 < milliseconds) {
      throw new IllegalArgumentException(milliseconds);
    }
    checkBool(isUtc);
    var jsMonth = month - 1;
    var value;
    if (isUtc) {
      value = JS('num', @'Date.UTC($0, $1, $2, $3, $4, $5, $6)',
                 years, jsMonth, day, hours, minutes, seconds, milliseconds);
    } else {
      value = JS('num', @'new Date($0, $1, $2, $3, $4, $5, $6).valueOf()',
                 years, jsMonth, day, hours, minutes, seconds, milliseconds);
    }
    if (value.isNaN()) throw new IllegalArgumentException('');
    if (years <= 0 || years < 100) return patchUpY2K(value, years, isUtc);
    return value;
  }

  static patchUpY2K(value, years, isUtc) {
    var date = JS('Object', @'new Date($0)', value);
    if (isUtc) {
      JS('num', @'$0.setUTCFullYear($1)', date, years);
    } else {
      JS('num', @'$0.setFullYear($1)', date, years);
    }
    return JS('num', @'$0.valueOf()', date);
  }

  // Lazily keep a JS Date stored in the JS object.
  static lazyAsJsDate(receiver) {
    if (JS('bool', @'$0.date === (void 0)', receiver)) {
      JS('void', @'$0.date = new Date($1)', receiver, receiver.value);
    }
    return JS('Date', @'$0.date', receiver);
  }

  static getYear(receiver) {
    return (receiver.timeZone.isUtc)
      ? JS('int', @'$0.getUTCFullYear()', lazyAsJsDate(receiver))
      : JS('int', @'$0.getFullYear()', lazyAsJsDate(receiver));
  }

  static getMonth(receiver) {
    return (receiver.timeZone.isUtc)
      ? JS('int', @'$0.getUTCMonth()', lazyAsJsDate(receiver)) + 1
      : JS('int', @'$0.getMonth()', lazyAsJsDate(receiver)) + 1;
  }

  static getDay(receiver) {
    return (receiver.timeZone.isUtc)
      ? JS('int', @'$0.getUTCDate()', lazyAsJsDate(receiver))
      : JS('int', @'$0.getDate()', lazyAsJsDate(receiver));
  }

  static getHours(receiver) {
    return (receiver.timeZone.isUtc)
      ? JS('int', @'$0.getUTCHours()', lazyAsJsDate(receiver))
      : JS('int', @'$0.getHours()', lazyAsJsDate(receiver));
  }

  static getMinutes(receiver) {
    return (receiver.timeZone.isUtc)
      ? JS('int', @'$0.getUTCMinutes()', lazyAsJsDate(receiver))
      : JS('int', @'$0.getMinutes()', lazyAsJsDate(receiver));
  }

  static getSeconds(receiver) {
    return (receiver.timeZone.isUtc)
      ? JS('int', @'$0.getUTCSeconds()', lazyAsJsDate(receiver))
      : JS('int', @'$0.getSeconds()', lazyAsJsDate(receiver));
  }

  static getMilliseconds(receiver) {
    return (receiver.timeZone.isUtc)
      ? JS('int', @'$0.getUTCMilliseconds()', lazyAsJsDate(receiver))
      : JS('int', @'$0.getMilliseconds()', lazyAsJsDate(receiver));
  }

  static getWeekday(receiver) {
    return (receiver.timeZone.isUtc)
      ? JS('int', @'$0.getUTCDay()', lazyAsJsDate(receiver))
      : JS('int', @'$0.getDay()', lazyAsJsDate(receiver));
  }

  static valueFromDateString(str) {
    checkNull(str);
    if (str is !String) throw new IllegalArgumentException(str);
    var value = JS('num', @'Date.parse($0)', str);
    if (value.isNaN()) throw new IllegalArgumentException(str);
    return value;
  }
}

builtin$compareTo$1(a, b) {
  checkNull(a);
  if (checkNumbers(a, b)) {
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
    return JS('bool', @'$0 == $1', a, b) ? 0
      : JS('bool', @'$0 < $1', a, b) ? -1 : 1;
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
  if (!isJsArray(receiver)) return UNINTERCEPTED(receiver.addAll(collection));

  // TODO(ahe): Use for-in when it is implemented correctly.
  var iterator = collection.iterator();
  while (iterator.hasNext()) {
    receiver.add(iterator.next());
  }
}

builtin$addLast$1(receiver, value) {
  checkNull(receiver);
  if (!isJsArray(receiver)) return UNINTERCEPTED(receiver.addLast(value));

  checkGrowable(receiver, 'addLast');
  JS('Object', @'$0.push($1)', receiver, value);
}

builtin$clear$0(receiver) {
  checkNull(receiver);
  if (!isJsArray(receiver)) return UNINTERCEPTED(receiver.clear());
  receiver.length = 0;
}

builtin$forEach$1(receiver, f) {
  checkNull(receiver);
  if (!isJsArray(receiver)) return UNINTERCEPTED(receiver.forEach(f));


  var length = JS('num', @'$0.length', receiver);
  if (length > 0 && f === null) throw new ObjectNotClosureException(); // Sigh.
  for (var i = 0; i < length; i++) {
    f(JS('Object', @'$0[$1]', receiver, i));
  }
}

builtin$getRange$2(receiver, start, length) {
  checkNull(receiver);
  if (!isJsArray(receiver)) {
    return UNINTERCEPTED(receiver.getRange(start, length));
  }
  if (0 === length) return [];
  checkNull(start); // TODO(ahe): This is not specified but co19 tests it.
  checkNull(length); // TODO(ahe): This is not specified but co19 tests it.
  if (start is !int) throw new IllegalArgumentException(start);
  if (length is !int) throw new IllegalArgumentException(length);
  if (start < 0) throw new IndexOutOfRangeException(start);
  if (length < 0) throw new IllegalArgumentException(length);
  var end = start + length;
  if (end > receiver.length) {
    throw new IndexOutOfRangeException(length);
  }
  if (length < 0) throw new IllegalArgumentException(length);
  return JS('Object', @'$0.slice($1, $2)', receiver, start, end);
}

builtin$indexOf$1(receiver, element) {
  checkNull(receiver);
  if (isJsArray(receiver) || receiver is String) {
    return builtin$indexOf$2(receiver, element, 0);
  }
  return UNINTERCEPTED(receiver.indexOf(element));
}

builtin$indexOf$2(receiver, element, start) {
  checkNull(receiver);
  if (isJsArray(receiver)) {
    if (start is !int) throw new IllegalArgumentException(start);
    var length = JS('num', @'$0.length', receiver);
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
  if (isJsArray(receiver)) {
    return builtin$insertRange$3(receiver, start, length, null);
  }
  return UNINTERCEPTED(receiver.insertRange(start, length));
}

builtin$insertRange$3(receiver, start, length, initialValue) {
  checkNull(receiver);
  if (!isJsArray(receiver)) {
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

  var receiverLength = JS('num', @'$0.length', receiver);
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
  if (!isJsArray(receiver)) {
    return UNINTERCEPTED(receiver.last());
  }
  return receiver[receiver.length - 1];
}

builtin$lastIndexOf$1(receiver, element) {
  checkNull(receiver);
  if (isJsArray(receiver)) {
    var start = JS('num', @'$0.length', receiver);
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
  if (isJsArray(receiver)) {
    return Arrays.lastIndexOf(receiver, element, start);
  } else if (receiver is String) {
    checkNull(element);
    if (element is !String) throw new IllegalArgumentException(element);
    if (start !== null) {
      if (start is !num) throw new IllegalArgumentException(start);
      if (start < 0) return -1;
      if (start >= receiver.length) start = receiver.length - 1;
    }
    return stringLastIndexOfUnchecked(receiver, element, start);
  }
  return UNINTERCEPTED(receiver.lastIndexOf(element, start));
}

stringLastIndexOfUnchecked(receiver, element, start)
  => JS('int', @'$0.lastIndexOf($1, $2)', receiver, element, start);

builtin$removeRange$2(receiver, start, length) {
  checkNull(receiver);
  if (!isJsArray(receiver)) {
    return UNINTERCEPTED(receiver.removeRange(start, length));
  }
  checkGrowable(receiver, 'removeRange');
  if (length == 0) {
    return;
  }
  checkNull(start); // TODO(ahe): This is not specified but co19 tests it.
  checkNull(length); // TODO(ahe): This is not specified but co19 tests it.
  if (start is !int) throw new IllegalArgumentException(start);
  if (length is !int) throw new IllegalArgumentException(length);
  if (length < 0) throw new IllegalArgumentException(length);
  var receiverLength = JS('num', @'$0.length', receiver);
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
  if (isJsArray(receiver)) {
    return builtin$setRange$4(receiver, start, length, from, 0);
  }
  return UNINTERCEPTED(receiver.setRange(start, length, from));
}

builtin$setRange$4(receiver, start, length, from, startFrom) {
  checkNull(receiver);
  if (!isJsArray(receiver)) {
    return UNINTERCEPTED(receiver.setRange(start, length, from, startFrom));
  }

  checkMutable(receiver, 'indexed set');
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
  if (!isJsArray(receiver)) return UNINTERCEPTED(receiver.some(f));

  return Collections.some(receiver, f);
}

builtin$every$1(receiver, f) {
  if (!isJsArray(receiver)) return UNINTERCEPTED(receiver.every(f));

  return Collections.every(receiver, f);
}

builtin$sort$1(receiver, compare) {
  checkNull(receiver);
  if (!isJsArray(receiver)) return UNINTERCEPTED(receiver.sort(compare));

  DualPivotQuicksort.sort(receiver, compare);
}

checkNull(object) {
  if (object === null) throw new NullPointerException();
  return object;
}

checkNum(value) {
  checkNull(value);
  if (value is !num) throw new IllegalArgumentException(value);
  return value;
}

checkInt(value) {
  checkNull(value);
  if (value is !int) throw new IllegalArgumentException(value);
  return value;
}

checkBool(value) {
  checkNull(value);
  if (value is !bool) throw new IllegalArgumentException(value);
  return value;
}

checkString(value) {
  checkNull(value);
  if (value is !String) throw new IllegalArgumentException(value);
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
    return JS('bool', @'isNaN($0)', receiver);
  } else {
    return UNINTERCEPTED(receiver.isNegative());
  }
}

builtin$remainder$1(a, b) {
  checkNull(a);
  if (checkNumbers(a, b)) {
    return JS('num', @'$0 % $1', a, b);
  } else {
    return UNINTERCEPTED(a.remainder(b));
  }
}

builtin$abs$0(receiver) {
  checkNull(receiver);
  if (receiver is !num) return UNINTERCEPTED(receiver.abs());

  return JS('num', @'Math.abs($0)', receiver);
}

builtin$toInt$0(receiver) {
  checkNull(receiver);
  if (receiver is !num) return UNINTERCEPTED(receiver.toInt());

  if (receiver.isNaN()) throw new BadNumberFormatException('NaN');

  if (receiver.isInfinite()) throw new BadNumberFormatException('Infinity');

  var truncated = receiver.truncate();
  return JS('bool', @'$0 == -0.0', truncated) ? 0 : truncated;
}

builtin$ceil$0(receiver) {
  checkNull(receiver);
  if (receiver is !num) return UNINTERCEPTED(receiver.ceil());

  return JS('num', @'Math.ceil($0)', receiver);
}

builtin$floor$0(receiver) {
  checkNull(receiver);
  if (receiver is !num) return UNINTERCEPTED(receiver.floor());

  return JS('num', @'Math.floor($0)', receiver);
}

builtin$isInfinite$0(receiver) {
  checkNull(receiver);
  if (receiver is !num) return UNINTERCEPTED(receiver.isInfinite());

  return JS('bool', @'$0 == Infinity', receiver)
    || JS('bool', @'$0 == -Infinity', receiver);
}

builtin$negate$0(receiver) {
  checkNull(receiver);
  if (receiver is !num) return UNINTERCEPTED(receiver.negate());

  return JS('num', @'-$0', receiver);
}

builtin$round$0(receiver) {
  checkNull(receiver);
  if (receiver is !num) return UNINTERCEPTED(receiver.round());

  if (JS('bool', @'$0 < 0', receiver)) {
    return JS('num', @'-Math.round(-$0)', receiver);
  } else {
    return JS('num', @'Math.round($0)', receiver);
  }
}

builtin$toDouble$0(receiver) {
  checkNull(receiver);
  if (receiver is !num) return UNINTERCEPTED(receiver.toDouble());

  // TODO(ahe): Just return receiver?
  return JS('double', @'$0 + 0', receiver);
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

  String result = JS('String', @'$0.toFixed($1)', receiver, fractionDigits);
  if (receiver == 0 && receiver.isNegative()) return "-$result";
  return result;
}

builtin$toStringAsExponential$1(receiver, fractionDigits) {
  checkNull(receiver);
  if (receiver is !num) {
    return UNINTERCEPTED(receiver.toStringAsExponential(fractionDigits));
  }
  if (fractionDigits !== null) checkNum(fractionDigits);

  String result = JS('String', @'$0.toExponential($1)',
                     receiver, fractionDigits);
  if (receiver == 0 && receiver.isNegative()) return "-$result";
  return result;
}

builtin$toStringAsPrecision$1(receiver, fractionDigits) {
  checkNull(receiver);
  if (receiver is !num) {
    return UNINTERCEPTED(receiver.toStringAsPrecision(fractionDigits));
  }
  checkNum(fractionDigits);

  String result = JS('String', @'$0.toPrecision($1)',
                     receiver, fractionDigits);
  if (receiver == 0 && receiver.isNegative()) return "-$result";
  return result;
}

builtin$toRadixString$1(receiver, radix) {
  checkNull(receiver);
  if (receiver is !num) {
    return UNINTERCEPTED(receiver.toRadixString(radix));
  }
  checkNum(radix);

  return JS('String', @'$0.toString($1)', receiver, radix);
}

builtin$allMatches$1(receiver, str) {
  checkNull(receiver);
  if (receiver is !String) return UNINTERCEPTED(receiver.allMatches(str));
  checkString(str);
  return allMatchesInStringUnchecked(receiver, str);
}

builtin$concat$1(receiver, other) {
  checkNull(receiver);
  if (receiver is !String) return UNINTERCEPTED(receiver.concat(other));

  if (other is !String) throw new IllegalArgumentException(other);
  return JS('String', @'$0.concat($1)', receiver, other);
}

builtin$contains$1(receiver, other) {
  if (receiver is !String) {
    checkNull(receiver);
    return UNINTERCEPTED(receiver.contains(other));
  }
  return builtin$contains$2(receiver, other, 0);
}

builtin$contains$2(receiver, other, startIndex) {
  checkNull(receiver);
  if (receiver is !String) {
    return UNINTERCEPTED(receiver.contains(other, startIndex));
  }
  checkNull(other);
  return stringContainsUnchecked(receiver, other, startIndex);
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

  checkNull(from);
  checkString(to);
  return stringReplaceAllUnchecked(receiver, from, to);
}

builtin$replaceFirst$2(receiver, from, to) {
  checkNull(receiver);
  if (receiver is !String) {
    return UNINTERCEPTED(receiver.replaceFirst(from, to));
  }
  checkNull(from);
  checkString(to);
  return stringReplaceFirstUnchecked(receiver, from, to);
}

builtin$split$1(receiver, pattern) {
  checkNull(receiver);
  if (receiver is !String) return UNINTERCEPTED(receiver.split(pattern));
  checkNull(pattern);
  return stringSplitUnchecked(receiver, pattern);
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
  return JS('bool', @'$0 == $1', other,
            JS('String', @'$0.substring(0, $1)', receiver, length));
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
    if (!JS('bool',
            @'/^\s*[+-]?(?:0[xX][abcdefABCDEF0-9]+|\d+)\s*$/.test($0)',
            str)) {
      throw new BadNumberFormatException(str);
    }
    var trimmed = str.trim();
    var base = 10;;
    if ((trimmed.length > 2 && (trimmed[1] == 'x' || trimmed[1] == 'X')) ||
        (trimmed.length > 3 && (trimmed[2] == 'x' || trimmed[2] == 'X'))) {
      base = 16;
    }
    var ret = JS('num', @'parseInt($0, $1)', trimmed, base);
    if (ret.isNaN()) throw new BadNumberFormatException(str);
    return ret;
  }

  static double parseDouble(String str) {
    checkNull(str);
    if (str is !String) throw new IllegalArgumentException();
    var ret = JS('num', @'parseFloat($0)', str);
    if (ret == 0 && (str.startsWith("0x") || str.startsWith("0X"))) {
      // TODO(ahe): This is unspecified, but tested by co19.
      ret = JS('num', @'parseInt($0)', str);
    }
    if (ret.isNaN() && str != 'NaN' && str != '-NaN') {
      throw new BadNumberFormatException(str);
    }
    return ret;
  }

  static double sqrt(num value)
    => JS('double', @'Math.sqrt($0)', checkNum(value));

  static double sin(num value)
    => JS('double', @'Math.sin($0)', checkNum(value));

  static double cos(num value)
    => JS('double', @'Math.cos($0)', checkNum(value));

  static double tan(num value)
    => JS('double', @'Math.tan($0)', checkNum(value));

  static double acos(num value)
    => JS('double', @'Math.acos($0)', checkNum(value));

  static double asin(num value)
    => JS('double', @'Math.asin($0)', checkNum(value));

  static double atan(num value)
    => JS('double', @'Math.atan($0)', checkNum(value));

  static double atan2(num a, num b)
    => JS('double', @'Math.atan2($0, $1)', checkNum(a), checkNum(b));

  static double exp(num value)
    => JS('double', @'Math.exp($0)', checkNum(value));

  static double log(num value)
    => JS('double', @'Math.log($0)', checkNum(value));

  static num pow(num value, num exponent) {
    checkNum(value);
    checkNum(exponent);
    return JS('num', @'Math.pow($0, $1)', value, exponent);
  }

  static double random() => JS('double', @'Math.random()');
}

/**
 * This is the [Jenkins hash function][1] but using masking to keep
 * values in SMI range. This was inspired by jmesserly's work in
 * Frog.
 *
 * [1]: http://en.wikipedia.org/wiki/Jenkins_hash_function
 */
builtin$hashCode$0(receiver) {
  // TODO(ahe): This method shouldn't have to use JS. Update when our
  // optimizations are smarter.
  if (receiver is num) return JS('int', @'$0 & 0x1FFFFFFF', receiver);
  if (receiver is !String) return UNINTERCEPTED(receiver.hashCode());
  int hash = 0;
  int length = JS('int', @'$0.length', receiver);
  for (int i = 0; i < length; i++) {
    hash = 0x1fffffff & (hash + JS('int', @'$0.charCodeAt($1)', receiver, i));
    hash = 0x1fffffff & (hash + JS('int', @'$0 << $1', 0x0007ffff & hash, 10));
    hash ^= hash >> 6;
  }
  hash = 0x1fffffff & (hash + JS('int', @'$0 << $1', 0x03ffffff & hash, 3));
  hash ^= hash >> 11;
  return 0x1fffffff & (hash + JS('int', @'$0 << $1', 0x00003fff & hash, 15));
}

// TODO(ahe): Dynamic may be overridden.
builtin$get$dynamic(receiver) => receiver;

/**
 * Called by generated code to capture the stacktrace before throwing
 * an exception.
 */
captureStackTrace(ex) {
  var jsError = JS('Object', @'new Error()');
  JS('void', @'$0.dartException = $1', jsError, ex);
  JS('void', @'''$0.toString = $1''', jsError, toStringWrapper);
  return jsError;
}

/**
 * This method is installed as JavaScript toString method on exception
 * objects in [captureStackTrace]. So JavaScript 'this' binds to an
 * instance of JavaScript Error to which we have added a property
 * 'dartException' which holds a Dart object.
 */
toStringWrapper() => JS('Object', @'this.dartException').toString();

builtin$charCodes$0(receiver) {
  checkNull(receiver);
  if (receiver is !String) return UNINTERCEPTED(receiver.charCodes());
  int len = receiver.length;
  List<int> result = new List<int>(len);
  for (int i = 0; i < len; i++) {
    result[i] = receiver.charCodeAt(i);
  }
  return result;
}

makeLiteralListConst(list) {
  JS('bool', @'$0.immutable$list = $1', list, true);
  JS('bool', @'$0.fixed$length = $1', list, true);
  return list;
}

/**
 * Called from catch blocks in generated code to extract the Dart
 * exception from the thrown value. The thrown value may have been
 * created by [captureStackTrace] or it may be a 'native' JS
 * exception.
 *
 * Some native exceptions are mapped to new Dart instances, others are
 * returned unmodified.
 */
unwrapException(ex) {
  // Note that we are checking if the object has the property. If it
  // has, it could be set null if the thrown value is null.
  if (JS('bool', @'"dartException" in $0', ex)) {
    return JS('Object', @'$0.dartException', ex);
  } else if (JS('bool', @'$0 instanceof TypeError', ex)) {
    // TODO(ahe): ex.type is Chrome specific.
    var type = JS('String', @'$0.type', ex);
    var jsArguments = JS('Object', @'$0.arguments', ex);
    var name = jsArguments[0];
    if (type == 'property_not_function' ||
        type == 'called_non_callable' ||
        type == 'non_object_property_call' ||
        type == 'non_object_property_load') {
      if (name !== null && name.startsWith(@'$call$')) {
        return new ObjectNotClosureException();
      } else {
        return new NullPointerException();
      }
    } else if (type == 'undefined_method') {
      if (name is String && name.startsWith(@'$call$')) {
        return new ObjectNotClosureException();
      } else {
        return new NoSuchMethodException('', name, []);
      }
    }
  } else if (JS('bool', @'$0 instanceof RangeError', ex)) {
    var message = JS('String', @'$0.message', ex);
    if (message.contains('call stack')) {
      return new StackOverflowException();
    }
  }
  return ex;
}

/**
 * Called by generated code to fetch the stack trace from an
 * exception.
 */
StackTrace getTraceFromException(exception) {
  return new StackTrace(JS("var", @"$0.stack", exception));
}

class StackTrace {
  var stack;
  StackTrace(this.stack);
  String toString() => stack != null ? stack : '';
}


/**
 * Called by generated code to build a map literal. [keyValuePairs] is
 * a list of key, value, key, value, ..., etc.
 */
makeLiteralMap(List keyValuePairs) {
  Iterator iterator = keyValuePairs.iterator();
  Map result = new LinkedHashMap();
  while (iterator.hasNext()) {
    String key = iterator.next();
    var value = iterator.next();
    result[key] = value;
  }
  return result;
}

/**
 * Called by generated code to convert a Dart closure to a JS
 * closure when the Dart closure is passed to the DOM.
 */
convertDartClosureToJS(closure) {
  if (closure === null) return null;
  var function = JS('var', @'$0.$identity', closure);
  if (JS('bool', @'!!$0', function)) return function;
  function = JS("var", @"""function() {
    var dartClosure = $0;
    switch (arguments.length) {
      case 0: return $1(dartClosure);
      case 1: return $2(dartClosure, arguments[0]);
      case 2: return $3(dartClosure, arguments[0], arguments[1]);
      default:
        throw new Error('Unsupported number of arguments for wrapped closure');
    }
  }""",
  closure,
  callClosure0,
  callClosure1,
  callClosure2);
  JS('void', @'$0.$identity = $1', closure, function);
  return function;
}

/**
 * Helper methods when converting a Dart closure to a JS closure.
 */
callClosure0(closure) => closure();
callClosure1(closure, arg1) => closure(arg1);
callClosure2(closure, arg1, arg2) => closure(arg1, arg2);

/**
 * Super class for Dart closures.
 */
class Closure implements Function {
  String toString() => "Closure";
}

bool jsHasOwnProperty(var jsObject, String property) {
  return JS('bool', @'$0.hasOwnProperty($1)', jsObject, property);
}

jsPropertyAccess(var jsObject, String property) {
  return JS('var', @'$0[$1]', jsObject, property);
}

/**
 * Called at the end of unaborted switch cases to get the singleton
 * FallThroughError exception that will be thrown.
 */
getFallThroughError() => const FallThroughError();
