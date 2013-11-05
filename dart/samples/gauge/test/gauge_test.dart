// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library gauge_test;

// Import from `web/gauge.dart` to ensure analyzer tests run on that file.
import '../web/gauge.dart';

/**
 * This test exists to ensure that the gauge sample compiles without
 * errors.
 */

void main() {
  // Reference to top-level member of gauge.dart so that the import isn't
  // marked as unused.
  Gauge;
}
