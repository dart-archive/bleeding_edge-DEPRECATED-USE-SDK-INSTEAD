// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("dart:dom_deprecated");

void main() {
  var element = document.createElement('div');
  element.innerHTML = "Hello from Dart!";
  document.body.appendChild(element);

  HTMLCanvasElement canvas = document.createElement('canvas');
  canvas.setAttribute('width', '100');
  canvas.setAttribute('height', '100');
  document.body.appendChild(canvas);

  var context = canvas.getContext('2d');
  context.fillRect(10, 10, 20, 20);
}
