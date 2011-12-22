// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

void expectEquals(var expected, var actual) {
  if (expected == actual) {
  } else {
    print("Actual does not match expected");
    throw actual;
  }
}

void main() {
  var prev = -1;
  for (int i = 0; i < 2; i++) {
    var x = i;
    if (prev != -1) {
      expectEquals(0, prev);
    }
    prev = x;
  }
}
