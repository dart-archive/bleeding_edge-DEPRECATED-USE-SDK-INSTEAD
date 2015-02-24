// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library services.src.index.store.collection;

import 'dart:collection';
import 'dart:typed_data' show Uint32List;

import 'package:analyzer/src/generated/utilities_general.dart';


/**
 * A hash map with `List<int>` keys and [int] values.
 */
class IntArrayToIntMap {
  final Map<Uint32List, int> map = new HashMap<Uint32List, int>(
      equals: _intArrayEquals,
      hashCode: _intArrayHashCode);

  /**
   * Returns the value for the given [key] or null if [key] is not in the map.
   */
  int operator [](List<int> key) {
    Uint32List typedKey = _getTypedKey(key);
    return map[typedKey];
  }

  /**
   * Associates the [key] with the given [value].
   *
   * If the key was already in the map, its associated value is changed.
   * Otherwise the key-value pair is added to the map.
   */
  void operator []=(List<int> key, int value) {
    Uint32List typedKey = _getTypedKey(key);
    map[typedKey] = value;
  }

  /**
   * Returns an [Uint32List] version of the given `List<int>` key.
   */
  static Uint32List _getTypedKey(List<int> key) {
    if (key is Uint32List) {
      return key;
    }
    return new Uint32List.fromList(key);
  }

  static bool _intArrayEquals(List<int> a, List<int> b) {
    int length = a.length;
    if (length != b.length) {
      return false;
    }
    for (int i = 0; i < length; i++) {
      if (a[i] != b[i]) {
        return false;
      }
    }
    return true;
  }

  static int _intArrayHashCode(List<int> key) {
    return key.fold(0, JenkinsSmiHash.combine);
  }
}


/**
 * A table mapping [int] keys to sets of [int]s.
 */
class IntToIntSetMap {
  final Map<int, Uint32List> _map = new HashMap<int, Uint32List>();

  /**
   * The number of key-value pairs in the map.
   */
  int get length => _map.length;

  /**
   * Adds the [value] to the set associated with the given [value].
   */
  void add(int key, int value) {
    Uint32List values = _map[key];
    if (values == null) {
      values = new Uint32List(1);
      values[0] = value;
      _map[key] = values;
    }
    if (values.indexOf(value) == -1) {
      int length = values.length;
      Uint32List newSet = new Uint32List(length + 1);
      newSet.setRange(0, length, values);
      newSet[length] = value;
      _map[key] = newSet;
    }
  }

  /**
   * Removes all pairs from the map.
   */
  void clear() {
    _map.clear();
  }

  /**
   * Returns the set of [int]s for the given [key] or an empty list if [key] is
   * not in the map.
   */
  List<int> get(int key) {
    List<int> values = _map[key];
    if (values == null) {
      values = <int>[];
    }
    return values;
  }
}
