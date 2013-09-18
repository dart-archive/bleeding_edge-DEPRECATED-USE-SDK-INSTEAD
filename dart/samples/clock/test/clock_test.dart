// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library clock_test;

import '../web/clock.dart' as clock;

// @static-clean

/**
 * This test exists to ensure that the clock sample compiles without errors.
 */
void main() {
  // Reference the clock library so that the import isn't marked as unused.
  clock.CountDownClock c = null;
  print(c);
}
