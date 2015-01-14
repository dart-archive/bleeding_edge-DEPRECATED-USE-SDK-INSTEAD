// Copyright (c) 2015, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Error in class finalization triggered via mirror in a static initializer.
// Simply check that we do not crash.

library mirror_in_static_init_test;

import 'dart:mirrors';

abstract class C {
  int _a;
  C([this._a: 0]);
}

final int staticField = () {
  var lib = currentMirrorSystem().findLibrary(#mirror_in_static_init_test);
  var lst = List.from(lib.declarations.values);
  return 42;
}();

main() {
  return staticField;
}
