// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Don't convert !(a op b) til (a neg-op b) when a or b might be NaN.
main() {
  Expect.isFalse(double.NAN >= 0);
  Expect.isTrue(!(double.NAN < 0));

  Expect.isFalse(double.NAN <= 0);
  Expect.isTrue(!(double.NAN > 0));

  Expect.isFalse(double.NAN < 0);
  Expect.isTrue(!(double.NAN >= 0));

  Expect.isFalse(double.NAN > 0);
  Expect.isTrue(!(double.NAN <= 0));

  Expect.isFalse(double.NAN == 0);
  Expect.isTrue(!(double.NAN != 0));

  Expect.isFalse(double.NAN != 0);
  Expect.isTrue(!(double.NAN == 0));

  Expect.isFalse(double.NAN === 0);
  Expect.isFalse(!(double.NAN !== 0));

  Expect.isFalse(double.NAN !== 0);
  Expect.isFalse(!(double.NAN === 0));

  Expect.isFalse(0 >= double.NAN);
  Expect.isTrue(!(0 < double.NAN));

  Expect.isFalse(0 <= double.NAN);
  Expect.isTrue(!(0 > double.NAN));

  Expect.isFalse(0 < double.NAN);
  Expect.isTrue(!(0 >= double.NAN));

  Expect.isFalse(0 > double.NAN);
  Expect.isTrue(!(0 <= double.NAN));

  Expect.isFalse(0 == double.NAN);
  Expect.isTrue(!(0 != double.NAN));

  Expect.isFalse(0 != double.NAN);
  Expect.isTrue(!(0 == double.NAN));

  Expect.isFalse(0 === double.NAN);
  Expect.isFalse(!(0 !== double.NAN));

  Expect.isFalse(0 !== double.NAN);
  Expect.isFalse(!(0 === double.NAN));
}
