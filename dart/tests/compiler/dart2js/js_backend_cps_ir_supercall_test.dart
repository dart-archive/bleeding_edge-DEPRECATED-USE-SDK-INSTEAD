// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
// VMOptions=-DUSE_CPS_IR=true

// Tests of interceptors.

library supercall_test;

import 'js_backend_cps_ir.dart';

const List<TestEntry> tests = const [
  const TestEntry.forMethod('function(Sub#m)', """
class Base {
  m(x) {
    print(x+1);
  }
}
class Sub extends Base {
  m(x) => super.m(x+10);
}
main() {
  new Sub().m(100);
}""",
r"""
function(x) {
  var v0, v1;
  v0 = 10;
  v1 = J.getInterceptor(x).$add(x, v0);
  return V.Base.prototype.m$1.call(this, v1);
}"""),

const TestEntry.forMethod('function(Sub#+)', """
class Base {
  m(x) {
    print(x+1000);
  }
  operator+(x) => m(x+10);
}
class Sub extends Base {
  m(x) => super.m(x+100);
  operator+(x) => super + (x+1);
}
main() {
  new Sub() + 10000;
}""",
r"""
function(x) {
  var v0, v1, v2;
  v0 = 1;
  v1 = J.getInterceptor(x).$add(x, v0);
  v2 = this;
  return V.Base.prototype.$add.call(null, v2, v1);
}"""),

const TestEntry.forMethod('function(Sub#m)', """
class Base {
  var field;
}
class Sub extends Base {
  m(x) => x + super.field;
}
main() {
  print(new Sub().m(10));
}""",
r"""
function(x) {
  var v0;
  v0 = this.field;
  return J.getInterceptor(x).$add(x, v0);
}"""),


];

void main() {
  runTests(tests);
}
