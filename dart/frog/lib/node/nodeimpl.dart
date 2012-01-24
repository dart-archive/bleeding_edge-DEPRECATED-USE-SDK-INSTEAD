// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Utilities used by the node library.

#library('nodeimpl');
#import('dart:coreimpl');

var require(String module) native;

// Helpers for read-only collections of native objects

// Takes a JavaScript object, returns a dart object
// Used to wrap list elements and map values

typedef var NativeValueWrapper(var v);

// TODO: Implement more of the List<E> protocol.

class NativeListBase<E> implements List<E>{
  var _list;
  NativeListBase(this._list);
  int get length() => _length(_list);
  static int _length(var list) native "return list.length;";
  abstract E operator[](int index);

  // List<E> protocol

  _throwUnsupported() {
    throw new UnsupportedOperationException('not extendable');
  }
  void add(E value) => _throwUnsupported();
  void addAll(Collection<E> collection) => _throwUnsupported();
  void addLast(E value) => _throwUnsupported();
  void clear() => _throwUnsupported();
  List<E> getRange(int start, int length) => _throwUnsupported();
  int indexOf(E element, [int start])
      => FixedLists.indexOf(this, element, start);
  void insertRange(int start, int length, [E initialValue])
      => _throwUnsupported();
  int last()
      => FixedLists.last(this);
  int lastIndexOf(E element, [int start])
      => FixedLists.lastIndexOf(this, element, start);
  int removeLast() {_throwUnsupported(); return 0; }
  void removeRange(int start, int length) => _throwUnsupported();
  void setRange(int start, int length, List<E> from, [int startFrom])
      => _throwUnsupported();
  void sort(int compare(E a, E b))
      => DualPivotQuicksort.sort(this, compare);

  // Collection<E> members:
  void forEach(void f(E element)) => FixedLists.forEach(this, f);
  Collection<E> filter(bool f(E element))
    => _throwUnsupported();
  bool every(bool f(E element)) => FixedLists.every(this, f);
  bool some(bool f(E element)) => FixedLists.some(this, f);
  bool isEmpty() => FixedLists.isEmpty(this);

  // Iterable<int> members:
  Iterator<int> iterator() => new ListIterator(this);
}

class NativeList<E> extends NativeListBase<E> {
  NativeValueWrapper _ctor;
  NativeList(var list, this._ctor) : super(list);
  E operator[](int index) => _ctor(_at(_list, index));
  static _at(var list, int index) native "return list[index];";
}

// TODO: Implement more of the Map<E> protocol.


class NativeMapBase<V> implements Map<String, V> {
  var _map;
  NativeMapBase(this._map);
  int get length() => _length(_map);
  int _length(var map) native "return map.length;";

  // Map<String, V>

  bool isEmpty() => length == 0;

  abstract V operator[](String key);

  Collection<String> getKeys() {
    List<String> keys = new List<String>();
    _forEachKey(_map, (String key) => keys.add(key));
    return keys;
  }

  Collection<V> getValues() {
    List<V> values = new List<V>();
    _forEachKey(_map, (String key) => values.add(this[key]));
    return values;
  }

  void forEach(void f(String key, V value)) =>
      _forEachKey(_map, (String k) => f(k, this[k]));

  static void _forEachKey(var map, void f(String key))
    native """
      for (var i in map) {
        if (map.hasOwnProperty(i)) {
          f(i);
        }
      }
    """;
}

class NativeMap<V> extends NativeMapBase<V> {
  NativeValueWrapper _ctor;
  NativeMap(var map, this._ctor) : super(map);
  V operator[](String key) => _ctor(_at(_map, key));
  var _at(var map, String key) native "return map[key];";
}

class NativeMapPrimitiveValue<V> extends NativeMapBase<V> {
  NativeMapPrimitiveValue(map) : super(map);
  V operator[](String key) => _at(_map, key);
  var _at(var map, String key) native "return map[key];";
}

_nativeProperty(object, String key)
  native "return object.hasOwnProperty(key) ? object[key] : null;";

String nativeGetStringProperty(object, String key)
  => _nativeProperty(object, key);

bool nativeGetBoolProperty(var object, String key)
  => _nativeProperty(object, key);

int nativeGetIntProperty(var object, String key)
  => _nativeProperty(object, key);

double nativeGetDoubleProperty(var object, String key)
  => _nativeProperty(object, key);


// Fixed size List utilities

class FixedLists {
  static void getRangeCheck(int srcLength, int start, int length) {
    if (length < 0) {
      throw new IllegalArgumentException();
    }
    int end = start + length;
    if (start < 0 || start >= srcLength) {
      throw new IndexOutOfRangeException(start);
    }
    if (end < 0 || end > srcLength) {
      throw new IndexOutOfRangeException(end);
    }
  }

  static int indexOf(List list, var element, int start) {
    if (start == null) {
      start = 0;
    }
    int end = list.length;
    for (int i = start; i < end; i++) {
      if (list[i] == element) {
        return i;
      }
    }
    return -1;
  }

  static int lastIndexOf(List list, var element, int start) {
    int end = start == null ? list.length : start;
    for (int i = end-1; i >= 0; i--) {
      if (list[i] == element) {
        return i;
      }
    }
    return -1;
  }

  static num last(List list) {
    int end = list.length;
    if (end <= 0) {
      throw new IndexOutOfRangeException(0);
    }
    return list[end-1];
  }

  static void forEach(List list, void f(var element)) {
    int len = list.length;
    for (int i = 0; i < len; i++) {
      f(list[i]);
    }
  }

  static Collection filter(List list, bool f(element), List ctor(int length)) {
    // Assume computation is more expensive than allocation, so use a
    // temporary growable list to store the filtered results.
    // (The alternative would be to make two passes through the list.)
    List filtered = [];
    int len = list.length;
    for (int i = 0; i < len; i++) {
      var e = list[i];
      if (f(e)) {
        filtered.add(e);
      }
    }
    int filteredLength = filtered.length;
    List result = ctor(filteredLength);
    for (int i = 0; i < len; i++) {
      result[i] = filtered[i];
    }
    return result;
  }

  static bool every(List list, bool f(int element)) {
    int len = list.length;
    for (int i = 0; i < len; i++) {
      if (! f(list[i])) {
        return false;
      }
    }
    return true;
  }

  static Collection map(List list, f(element), List result) {
    int len = list.length;
    for (int i = 0; i < len; i++) {
      result[i] = f(list[i]);
    }
    return result;
  }


  static bool some(List list, bool f(num element)) {
    int len = list.length;
    for (int i = 0; i < len; i++) {
      if (f(list[i])) {
        return true;
      }
    }
    return false;
  }

  static bool isEmpty(List list) => list.length == 0;
}