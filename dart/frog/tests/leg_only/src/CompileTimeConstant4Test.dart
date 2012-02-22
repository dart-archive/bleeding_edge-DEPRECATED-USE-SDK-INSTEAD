// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

final x = "foo";
final y = "foo";
final g1 = x + "bar";
final g2 = x
  + null   /// 01: compile-time error
;
final g3 = x
  + 499   /// 02: compile-time error
;
final g4 = x
  + 3.3   /// 03: compile-time error
;
final g5 = x
  + true   /// 04: compile-time error
;
final g6 = x
  + false   /// 05: compile-time error
;
final g7 = "foo"
  + x[0];   /// 06: compile-time error
;
final g8 = 1
  + x.length  /// 07: compile-time error
;
final g9 = x == y;

use(x) => x;

main() {
  Expect.equals("foobar", g1);
  Expect.isTrue(g9);
  use(g2);
  use(g3);
  use(g4);
  use(g5);
  use(g6);
  use(g7);
  use(g8);
}
