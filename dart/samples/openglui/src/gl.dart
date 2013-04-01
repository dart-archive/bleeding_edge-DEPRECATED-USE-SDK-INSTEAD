// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * An HTML-based prototype implementation for GL.
 */

// TODO(vsm): Move this out of the common directory.
library gl_html;

import 'dart:html' as html;
export 'dart:html' show WebGLRenderingContext;
export 'dart:html' show Float32Array;
export 'dart:html' show ImageElement;
export 'dart:html' show CanvasElement;
export 'dart:html' show AudioElement;
export 'dart:html' show KeyboardEvent;
export 'dart:html' show MouseEvent;
export 'dart:html' show HtmlDocument;
export 'dart:html' show CanvasRenderingContext2D;
export 'dart:html' show Rect;

get document => html.document;
get window => html.window;
get sfx_extension => 'mp3';

log(message) => window.console.log(message);
glSwapBuffers() {}

getDisplayCanvas(resize) {
  var canvas = document.query('#canvas');
  if (canvas == null) {
    canvas = new html.CanvasElement(
        width: window.innerWidth, height: window.innerHeight);
    document.body.nodes.add(canvas);
  }
  return canvas;
}


