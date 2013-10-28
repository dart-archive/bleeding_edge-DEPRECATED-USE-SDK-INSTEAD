// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library sunflower_test;

import '../web/sunflower.dart' as sunflower;

/**
 * This test exists to ensure that the sunflower sample compiles without errors.
 */
void main() {
  // Reference the sunflower library so that the import isn't marked as unused.
  String s = sunflower.ORANGE;
  s = null;
  print(s);
}
