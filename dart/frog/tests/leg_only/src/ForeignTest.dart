// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

foreign1(var a, var b) {
  return JS("num", @"$0 + $1", a, b);
}

// TODO(floitsch): don't do a JS side-effect once we have static variables.
// Throws, if called twice.
callOnce() {
  // We can't easily throw in a JS foreign. Simply access an undefined global in
  // case of failure.
  JS("bool", @"(typeof _FOREIGN_TEST_GLOBAL_ !== 'undefined') ? _FOREIGN_TEST_FAIL_ : 'ok'");
  JS("void", @"_FOREIGN_TEST_GLOBAL_ = 'defined'");
  return 499;
}

foreign2() {
  var t = callOnce();
  return JS("num", @"$0 + $0", t);
}

void main() {
  Expect.equals(9, foreign1(4, 5));
  Expect.equals(998, foreign2());
}
