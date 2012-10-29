// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * A simple GL library prototype that hides the underlying implementation.
 */
library gl;

abstract class GlContext {
  int get COLOR_BUFFER_BIT;
  int get DEPTH_BUFFER_BIT;

  void clearColor(num red, num green, num blue, num alpha);
  void clear(int mask);
}
