// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("dart:html");

void main() {
  var element = document.createElement('div');
  element.innerHTML = "Hello from Dart!";
  document.body.nodes.add(element);

  CanvasElement canvas = document.createElement('canvas');
  canvas.width = 100;
  canvas.height = 100;
  document.body.nodes.add(canvas);

  var context = canvas.getContext('2d');
  context.fillRect(10, 10, 20, 20);
}
