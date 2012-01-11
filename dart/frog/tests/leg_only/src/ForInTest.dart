// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Test foreach (aka. for-in) functionality.

testIterator(List expect, Iterable input) {
  int i = 0;
  for (var value in input) {
    Expect.isTrue(i < expect.length);
    Expect.equals(expect[i], value);
    i += 1;
  }
  Expect.equals(expect.length, i);
}

class MyIterable<T> /* implements Iterable<T> */ {
  final List<T> values;
  MyIterable(List<T> values) : this.values = values;
  Iterator iterator() {
    return new MyListIterator(values);
  }
}

class MyListIterator<T> /* implements Iterator<T> */ {
  final List<T> values;
  int index;
  MyListIterator(List<T> values) : this.values = values, index = 0;
  bool hasNext() => index < values.length;
  T next() => values[index++];
}

void main() {
  testIterator([], []);
  testIterator([], new MyIterable([]));
  testIterator([1], [1]);
  testIterator([1], new MyIterable([1]));
  testIterator([1,2,3], [1,2,3]);
  testIterator([1,2,3], new MyIterable([1,2,3]));
  testIterator(["a","b","c"], ["a","b","c"]);
  testIterator(["a","b","c"], new MyIterable(["a","b","c"]));
}
