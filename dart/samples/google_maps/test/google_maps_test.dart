// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library google_maps_test;

// Import `web/index.dart` to ensure analyzer tests run on that file. So there
// is no error related to an unused import, reference `web/index.dart`'s
// `main()` within this file's `main()`.
import '../web/index.dart' as google_maps;

/**
 * This test exists to ensure that the google_maps sample compiles without
 * errors.
 */

void main() {
  // Reference the google_maps library so that the import isn't marked
  // as unused.
  google_maps.main;
}
