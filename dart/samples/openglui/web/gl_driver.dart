// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library gl_driver;

import '../src/gl.dart';

/**
 * A driver to run GL applications in the browser.
 */

void glMain(setup, resize) {
  // Setup a Canvas for GL to run inside.
  final canvas = getDisplayCanvas(resize);
  setup(canvas, canvas.width, canvas.height, 2);
}

