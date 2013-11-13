// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// This sample is mostly a port of the mutli-touch example found at:
// https://github.com/paulirish/demo/blob/master/js/multitouch.js

// The author information in the original is reproduced:

    // canvasDrawr originally from Mike Taylr  http://miketaylr.com/
    // Tim Branyen massaged it: http://timbranyen.com/
    // and i did too. with multi touch.
    // and boris fixed some touch identifier stuff to be more specific.

library multi_touch;

import 'dart:html';
import 'dart:math' show Random;

class CanvasDrawer {
  static const List<String> colors = const [
      "red", "green", "yellow", "blue", "magenta", "orangered"];
  CanvasElement canvas;
  CanvasRenderingContext2D ctxt;
  Rectangle offset;
  Map<int, Map> lines = {};

  CanvasDrawer(this.canvas) {
    ctxt = canvas.getContext('2d');
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;

    ctxt.lineWidth = (new Random().nextDouble() * 10).ceil();
    ctxt.lineCap = "round";
    offset = canvas.offset;

    canvas.onTouchStart.listen(preDraw);
    canvas.onTouchMove.listen(draw);
  }

  preDraw(TouchEvent event) {
    event.preventDefault();
    event.touches.forEach((touch) {
      lines[touch.identifier] = {
          'x'    : touch.page.x - offset.left,
          'y'    : touch.page.y - offset.top,
          'color' : colors[new Random().nextInt(colors.length)]
      };
    });
  }

  draw(TouchEvent event) {
    event.preventDefault();
    event.touches.forEach((touch) {
      var _id = touch.identifier;
      var ret = move(
          _id,
          touch.page.x - offset.left - lines[_id]['x'],
          touch.page.y - offset.top - lines[_id]['y']);
      lines[_id]['x'] = ret['x'];
      lines[_id]['y'] = ret['y'];
    });
  }

  move(_id, changeX, changeY) {
    ctxt.strokeStyle = lines[_id]['color'];
    ctxt.beginPath();
    ctxt.moveTo(lines[_id]['x'], lines[_id]['y']);
    ctxt.lineTo(lines[_id]['x'] + changeX, lines[_id]['y'] + changeY);
    ctxt.stroke();
    ctxt.closePath();
    return { 'x': lines[_id]['x'] + changeX, 'y': lines[_id]['y'] + changeY };
  }
}

void main() {
  var canvas = querySelector('#example');
  CanvasDrawer canvasDrawer = new CanvasDrawer(canvas);
}
