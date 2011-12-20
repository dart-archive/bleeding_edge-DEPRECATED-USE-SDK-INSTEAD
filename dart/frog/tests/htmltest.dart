// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("dart:html");

void main() {
  var element = new Element.tag('div');
  element.text = "Hello from Dart!";
  document.body.elements.add(element);

  CanvasElement canvas = new Element.tag('canvas');
  canvas.width = 100;
  canvas.height = 100;
  document.body.elements.add(canvas);

  var context = canvas.getContext('2d');
  context.fillRect(10, 10, 20, 20);
}
