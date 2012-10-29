// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import 'dart:html';
import 'gl_html.dart';
import 'simplegl.dart';

/**
 * A driver to run GL applications in the browser.
 */
void main() {
  // Setup a Canvas for GL to run inside.
  final canvas = new CanvasElement(width: 480, height: 800);
  document.body.nodes.add(canvas);
  final context =
      new HtmlGlContext(canvas.getContext('experimental-webgl'));

  // The first 'setup' entry point is called once.
  setup(context);

  // The second 'render' entry point is called each time the canvas should
  // be re-drawn.
  var render;
  render = (n) {
    draw(context);
    window.requestAnimationFrame(render);
  };
  render(context);
}
