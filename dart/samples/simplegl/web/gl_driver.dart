// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library gl_driver;

import 'dart:html';
import 'gl.dart';

/**
 * A driver to run GL applications in the browser.
 */
void glMain(setup, resize, draw) {
  // Setup a Canvas for GL to run inside.
  final canvas = new CanvasElement(width: window.innerWidth, height: window.innerHeight);
  document.body.nodes.add(canvas);

  gl = canvas.getContext('experimental-webgl');

  // The first 'setup' entry point is called once.
  setup();
  resize(canvas.width, canvas.height);

  // The second 'render' entry point is called each time the canvas should
  // be re-drawn.
  var render;
  render = (n) {
    draw();
    window.requestAnimationFrame(render);
  };
  render(0);

  window.on.resize.add((e) {
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
    resize(canvas.width, canvas.height);
  });
}
