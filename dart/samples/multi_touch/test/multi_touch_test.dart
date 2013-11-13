// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library multi_touch_test;

// Import from `web/index.dart` to ensure analyzer tests run on that file.
import '../web/index.dart';

/**
 * This test exists to ensure that the mutli_touch sample compiles without
 * errors.
 */

void main() {
  // Reference to top-level member of index.dart so that the import isn't
  // marked as unused.
  CanvasDrawer;
}
