// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * A sample GL application.
 */
library simplegl;

import 'gl.dart';

num r;
num g;
num b;

/**
 * Invoked on initial startup.
 */
void setup(GlContext gl) {
  r = 0;
  g = 0;
  b = 0;
}

/**
 * Invoked on each frame render.
 */
void draw(GlContext gl) {
  gl.clearColor(r, g, b, 1.0);
  gl.clear(gl.COLOR_BUFFER_BIT |
      gl.DEPTH_BUFFER_BIT);
  r = r + 0.1;
  if (r > 1) {
    r = 0;
    g = g + 0.1;
  }
  if (g > 1) {
    g = 0;
    b = b + 0.1;
  }
  if (b > 1) {
    b = 0;
  }
}
