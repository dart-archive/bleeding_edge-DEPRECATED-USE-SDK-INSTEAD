// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * An HTML-based prototype implementation for GL.
 */
library gl_html;

import 'dart:html';
import 'gl.dart';

class HtmlGlContext extends GlContext {
  WebGLRenderingContext _context;
  HtmlGlContext(this._context);

  int get COLOR_BUFFER_BIT => WebGLRenderingContext.COLOR_BUFFER_BIT;
  int get DEPTH_BUFFER_BIT => WebGLRenderingContext.DEPTH_BUFFER_BIT;

  void clearColor(num red, num green, num blue, num alpha) =>
      _context.clearColor(red, green, blue, alpha);
  void clear(int mask) =>
      _context.clear(mask);
}
