// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class A {
  A();
  A.a()
    : this.b()  /// 01: compile-time error
  ;
  A.b()
    : this.a()  /// 02: compile-time error
  ;
  A.c()
    : this.b()  /// 03: compile-time error
  ;
}

main() {
  new A();
  new A.a();
  new A.b();
  new A.c();
}
