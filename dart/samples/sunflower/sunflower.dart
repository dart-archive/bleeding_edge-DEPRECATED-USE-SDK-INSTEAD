// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('sunflower');

#import('dart:html');
#import('dart:math');

final SEED_RADIUS = 2;
final SCALE_FACTOR = 4;
final TAU = PI * 2;

final MAX_D = 300;
final ORANGE = "orange";

num centerX, centerY;
int seeds = 0;

var PHI;

main() {
  PHI = (sqrt(5) + 1) / 2;

  CanvasElement canvas = query("#canvas");
  centerX = centerY = MAX_D / 2;
  var context = canvas.context2d;

  InputElement slider = query("#slider");
  slider.on.change.add((Event e) {
    seeds = parseInt(slider.value);
    drawFrame(context);
  }, true);

  seeds = parseInt(slider.value);

  drawFrame(context);
}

/**
 * Draw the complete figure for the current number of seeds.
 */
void drawFrame(CanvasRenderingContext2D context) {
  context.clearRect(0, 0, MAX_D, MAX_D);

  for (var i = 0; i < seeds; i++) {
    var theta = i * TAU / PHI;
    var r = sqrt(i) * SCALE_FACTOR;
    var x = centerX + r * cos(theta);
    var y = centerY - r * sin(theta);

    drawSeed(context, x, y);
  }

  displaySeedCount(seeds);
}

/**
 * Draw a small circle representing a seed centered at (x,y).
 */
void drawSeed(CanvasRenderingContext2D context, num x, num y) {
  context.beginPath();
  context.lineWidth = 2;
  context.fillStyle = ORANGE;
  context.strokeStyle = ORANGE;
  context.arc(x, y, SEED_RADIUS, 0, TAU, false);
  context.fill();
  context.closePath();
  context.stroke();
}

void displaySeedCount(num seedCount) {
  query("#notes").text = "${seedCount} seeds";
}
