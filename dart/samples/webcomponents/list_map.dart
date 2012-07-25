// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/** 
 * ListMap class so that we have a dictionary usable with non-hashable keys.
 * Note: this class does NOT yet have full Map functionality 
 */
class ListMap<K, V> {

  List<_Pair<K, V>> _list;

  ListMap() 
    : _list = new List<_Pair<K, V>>() { }

  void operator []=(K key, V value) {
    _list.add(new _Pair<K,V>(key, value));
  }

  V operator [](K key) {
    for (var pair in _list) {
      if (pair._key == key)
        return pair._value;
    }
    return null;
  }
}

class _Pair<K, V> {
  K _key;
  V _value;

  _Pair(this._key, this._value);
}
