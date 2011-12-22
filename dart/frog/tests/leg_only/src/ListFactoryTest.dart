// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

main() {
  var a = new List(4);
  Expect.equals(4, a.length);
  a[0] = 42;
  a[1] = 43;
  a[2] = 44;
  a[3] = 45;
  for (int i = 0; i < a.length; i++) {
    Expect.equals(42 + i, a[i]);
  }
}
