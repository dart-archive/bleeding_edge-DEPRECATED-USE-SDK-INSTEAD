// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * An HTML-based prototype implementation for GL.
 */

// TODO(vsm): Move this out of the common directory.
library gl_html;

import 'dart:html';
export 'dart:html' show WebGLRenderingContext;
export 'dart:html' show Float32Array;
export 'dart:html' show ImageElement;
export 'dart:html' show CanvasElement;

log(message) => window.console.log(message);

getDisplayCanvas(resize) {
  final canvas = new CanvasElement(width: window.innerWidth, height: window.innerHeight);
  document.body.nodes.add(canvas);

  window.onResize.listen((e) {
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
    resize(canvas.width, canvas.height);
  });
  return canvas;
}

animate(draw) {
  var render;
  render = (n) {
    draw();
    window.requestAnimationFrame(render);
  };
  render(0);
}

