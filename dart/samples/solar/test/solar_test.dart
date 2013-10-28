// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library solar_test;

import '../web/solar.dart' as solar;

/**
 * This test exists to ensure that the solar sample compiles without errors.
 */
void main() {
  // Reference the solar library so that the import isn't marked as unused.
  solar.PlanetaryBody p = null;
  print(p);
}
