// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("dart:coreimpl");

#source("../../corelib/src/implementation/dual_pivot_quicksort.dart");
#source("../../corelib/src/implementation/duration_implementation.dart");
#source("../../corelib/src/implementation/exceptions.dart");
#source("../../corelib/src/implementation/future_implementation.dart");
#source("../../corelib/src/implementation/hash_map_set.dart");
// TODO(jimhug): Re-explore tradeoffs with using builtin JS maps.
#source("../../corelib/src/implementation/linked_hash_map.dart");
#source("../../corelib/src/implementation/maps.dart");
#source("../../corelib/src/implementation/promise_implementation.dart");
#source("../../corelib/src/implementation/queue.dart");
#source("../../corelib/src/implementation/stopwatch_implementation.dart");
#source("../../corelib/src/implementation/splay_tree.dart");

#source("string_buffer.dart");
#source("string_base.dart");
#source("string_implementation.dart");

#source("arrays.dart");
#source("collections.dart");
#source("date_implementation.dart");

#source("isolate.dart");
#source("isolate_serialization.dart");

class ListFactory<E> implements List<E> native "Array" {
  ListFactory([int length]) native;

  // TODO(jmesserly): type parameters aren't working here
  factory ListFactory.from(Iterable other) {
    final list = [];
    for (final e in other) {
      list.add(e);
    }
    return list;
  }

  // TODO(jimhug): Only works for Arrays.
  factory ListFactory.fromList(List other, int startIndex, int endIndex)
    native 'return other.slice(startIndex, endIndex);';

  int length; // all fields on natives are implied native.

  // List<E> members:
  E operator [](int index) native;
  void operator []=(int index, E value) native;
  void add(E value) native "this.push(value);";
  void addLast(E value) native "this.push(value);";
  void addAll(Collection<E> collection) {
    for (E item in collection) add(item);
  }
  void sort(int compare(E a, E b)) native;
  void copyFrom(List<Object> src, int srcStart, int dstStart, int count) native;
  int indexOf(E element, [int start]) native;
  int lastIndexOf(E element, [int start]) native;
  void clear() { length = 0; }

  E removeLast() native "return this.pop();";

  E last() => this[this.length-1];

  List<E> getRange(int start, int length) native
    "return this.slice(start, start + length);";

  void setRange(int start, int length, List<E> from, [int startFrom]) native;
  void removeRange(int start, int length) native "this.splice(start, length);";

  void insertRange(int start, int length, [E initialValue]) native
    """
    // Splice in the values with a minimum of array allocations.
    var args = new Array(length + 2);
    args[0] = start;
    args[1] = 0;
    for (var i = 0; i < length; i++) {
      args[i + 2] = initialValue;
    }
    this.splice.apply(this, args);
    """;

  // Collection<E> members:
  void forEach(void f(E element)) native;
  Collection<E> filter(bool f(E element)) native;
  bool every(bool f(E element)) native;
  bool some(bool f(E element)) native;
  bool isEmpty() => length == 0;

  // Iterable<E> members:
  Iterator<E> iterator() => new ListIterator(this);
}

// Iterator for lists.
class ListIterator<T> implements Iterator<T> {
  ListIterator(List<T> array)
      : _array = array,
        _pos = 0 {
  }

  bool hasNext() {
    return _array.length > _pos;
  }

  T next() {
    // TODO(jmesserly): this check is redundant in a for-in loop
    // Must we do it?
    if (!hasNext()) {
      throw const NoMoreElementsException();
    }
    return _array[_pos++];
  }

  final List<T> _array;
  int _pos;
}

/** An immutable list. Attempting to modify the list will throw an exception. */
 class ImmutableList<E> extends ListFactory<E> {
   final int _length;

   // TODO(sigmund): remove this when we stop overriding the [length] property
   // in Array.
   int get length() => _length;

   void set length(int length) {
     throw const IllegalAccessException();
   }

   ImmutableList([int length]) : _length = length, super(length);

   factory ImmutableList.from(List other) {
     final list = new ImmutableList(other.length);
     for (int i = 0; i < other.length; i++) {
       // Note: push invokes the setter of [length], which we override. So
       // instead we use the native []= operator that cannot be overriden.
       list._setindex(i, other[i]);
     }
     return list;
   }

   void _setindex(int index, E value) native "return this[index] = value;";

   void operator []=(int index, E value) {
     throw const IllegalAccessException();
   }

   void copyFrom(List src, int srcStart, int dstStart, int count) {
     throw const IllegalAccessException();
   }

   void setRange(int start, int length, List<E> from, [int startFrom = 0]) {
     throw const IllegalAccessException();
   }

   void removeRange(int start, int length) {
     throw const IllegalAccessException();
   }

   void insertRange(int start, int length, [E initialValue = null]) {
     throw const IllegalAccessException();
   }

   void sort(int compare(E a, E b)) {
     throw const IllegalAccessException();
   }

   void add(E element) {
     throw const IllegalAccessException();
   }

   void addLast(E element) {
     throw const IllegalAccessException();
   }

   void addAll(Collection<E> elements) {
     throw const IllegalAccessException();
   }

   void clear() {
     throw const IllegalAccessException();
   }

   E removeLast() {
     throw const IllegalAccessException();
   }


   // The base Array.prototype.toString does not like getting derived arrays,
   // so copy the array if needed.
   // TODO(jmesserly): this is not the right long term fix because it only works
   // for ImmutableList, but all derived types of ListFactory have this problem.
   // We need to implment ListFactory.toString in Dart. However, the
   // mplmentation needs correct handling of cycles (isolate tests depend on
   // this), so it's not trivial.
   String toString() => new List.from(this).toString();
}

/** An immutable map. */
class ImmutableMap<K, V> implements Map<K, V> {
  final Map<K, V> _internal;

  ImmutableMap(List keyValuePairs) : _internal = {} {
    // Note pairs accept repeated values, use the latest definition for the map
    // (see langauge/MapLiteral3Test)
    for (int i = 0; i < keyValuePairs.length; i += 2) {
      _internal[keyValuePairs[i]] = keyValuePairs[i + 1];
    }
  }

  V operator [](K key) => _internal[key];

  bool isEmpty() => _internal.isEmpty();

  int get length() => _internal.length;

  void forEach(void f(K key, V value)) {
    _internal.forEach(f);
  }

  Collection<K> getKeys() => _internal.getKeys();

  Collection<V> getValues() => _internal.getValues();

  bool containsKey(K key) => _internal.containsKey(key);

  bool containsValue(V value) => _internal.containsValue(value);

  void operator []=(K key, V value) {
    throw const IllegalAccessException();
  }

  V putIfAbsent(K key, V ifAbsent()) {
    throw const IllegalAccessException();
  }

  void clear() {
    throw const IllegalAccessException();
  }

  V remove(K key) {
    throw const IllegalAccessException();
  }
}


// TODO(jmesserly): this should wrap real RegExp when we can
// We can't do it yet because we'd need a way to redirect the const
// default constructor.
// TODO(jimhug): One way to resolve this is to make the const constructor
// very special in order for it to generate JS regex literals into the code
// and then treat the constructor as a factory.
class JSSyntaxRegExp implements RegExp {
  final String pattern;
  final bool multiLine;
  final bool ignoreCase;

  const JSSyntaxRegExp(String pattern, [bool multiLine, bool ignoreCase]):
    this._create(pattern,
        (multiLine == true ? 'm' : '') + (ignoreCase == true ? 'i' : ''));

  const JSSyntaxRegExp._create(String pattern, String flags) native
    '''this.re = new RegExp(pattern, flags);
    this.pattern = pattern;
    this.multiLine = this.re.multiline;
    this.ignoreCase = this.re.ignoreCase;''';

  Match firstMatch(String str) {
    List<String> m = _exec(str);
    return m == null ? null
        : new MatchImplementation(pattern, str, _matchStart(m), _lastIndex, m);
  }

  List<String> _exec(String str) native "return this.re.exec(str);";
  int _matchStart(m) native "return m.index;";
  int get _lastIndex() native "return this.re.lastIndex;";

  bool hasMatch(String str) native "return this.re.test(str);";

  String stringMatch(String str) {
    var match = firstMatch(str);
    return match === null ? null : match.group(0);
  }

  Iterable<Match> allMatches(String str) => new _AllMatchesIterable(this, str);
}

class MatchImplementation implements Match {
  const MatchImplementation(
      String this.pattern,
      String this.str,
      int this._start,
      int this._end,
      List<String> this._groups);

  final String pattern;
  final String str;
  final int _start;
  final int _end;
  final List<String> _groups;

  int start() => _start;
  int end() => _end;
  String group(int group) => _groups[group];
  String operator [](int group) => _groups[group];
  int groupCount() => _groups.length;

  List<String> groups(List<int> groups) {
    List<String> out = [];
    groups.forEach((int group) => out.add(_groups[group]));
    return out;
  }
}

class _AllMatchesIterable implements Iterable<Match> {
  final JSSyntaxRegExp _re;
  final String _str;

  const _AllMatchesIterable(this._re, this._str);

  Iterator<Match> iterator() => new _AllMatchesIterator(_re, _str);
}

class _AllMatchesIterator implements Iterator<Match> {
  final RegExp _re;
  final String _str;
  Match _next;
  bool _done;

  _AllMatchesIterator(RegExp re, String this._str)
    : _done = false,
      _re = new JSSyntaxRegExp._create(re.pattern,
        // Create a new RegExp with the "global" flag set so we can use it to
        // iterate over multiple matches. Note that this will make the RegExp
        // disobey the normal API.
        'g' + (re.multiLine ? 'm' : '') + (re.ignoreCase ? 'i' : ''));

  Match next() {
    if (!hasNext()) {
      throw const NoMoreElementsException();
    }

    // _next is set by #hasNext
    var next = _next;
    _next = null;
    return next;
  }

  bool hasNext() {
    if (_done) {
      return false;
    } else if (_next != null) {
      return true;
    }

    _next = _re.firstMatch(_str);
    if (_next == null) {
      _done = true;
      return false;
    } else {
      return true;
    }
  }
}


class NumImplementation implements int, double native "Number" {
  // TODO(jimhug): Move these out of methods to avoid boxing when not needed.
  num remainder(num other) native "return this % other;";

  bool isEven() native "return ((this & 1) == 0);";
  bool isOdd() native "return ((this & 1) == 1);";
  bool isNaN() native "return isNaN(this);";
  bool isNegative() native
    "return this == 0 ? (1 / this) < 0 : this < 0;";
  bool isInfinite() native
    "return (this == Infinity) || (this == -Infinity);";

  num abs() native "return Math.abs(this);";
  num round() native "return Math.round(this);";
  num floor() native "return Math.floor(this);";
  num ceil() native "return Math.ceil(this);";
  num truncate() native
    "return (this < 0) ? Math.ceil(this) : Math.floor(this);";

  int hashCode() native "return this & 0xFFFFFFF;";

  // If truncated is -0.0 return +0. The test will also trigger for positive
  // 0s but that's not a problem.
  int toInt() native
  '''if (isNaN(this)) throw new BadNumberFormatException("NaN");
  if ((this == Infinity) || (this == -Infinity)) {
    throw new BadNumberFormatException("Infinity");
  }
  var truncated = (this < 0) ? Math.ceil(this) : Math.floor(this);

  if (truncated == -0.0) return 0;
  return truncated;''';

  double toDouble() native "return this + 0;";

  String toStringAsFixed(int fractionDigits) native
    "return this.toFixed(fractionDigits);";
  String toStringAsExponential(int fractionDigits) native
    "return this.toExponential(fractionDigits)";
  String toStringAsPrecision(int precision) native
    "return this.toPrecision(precision)";
  String toRadixString(int radix) native "return this.toString(radix)";

  // CompareTo has to give a complete order, including -0/+0, NaN and
  // Infinities.
  // Order is: -Inf < .. < -0.0 < 0.0 .. < +inf < NaN.
  int compareTo(NumImplementation other) {
    // Don't use the 'this' object (which is a JS Number object), but get the
    // primitive JS number by invoking toDouble().
    num thisValue = toDouble();
    // Remember that NaN return false for any comparison.
    if (thisValue < other) {
      return -1;
    } else if (thisValue > other) {
      return 1;
    } else if (thisValue == other) {
      if (thisValue == 0) {
        bool thisIsNegative = isNegative();
        bool otherIsNegative = other.isNegative();
        if (thisIsNegative == otherIsNegative) return 0;
        if (thisIsNegative) return -1;
        return 1;
      }
      return 0;
    } else if (isNaN()) {
      if (other.isNaN()) {
        return 0;
      }
      return 1;
    } else {
      return -1;
    }
  }
}
