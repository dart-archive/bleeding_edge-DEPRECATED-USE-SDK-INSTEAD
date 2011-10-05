// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('sunflower');

#import('dart:dom');

#resource('sunflower.css');

main() {
  new Sunflower();
}

class Sunflower {
  
  Sunflower() {
    var doc = window.document;
    
    HTMLCanvasElement canvas = doc.getElementById("canvas");
    xc = yc = MAX_D / 2;
    ctx = canvas.getContext("2d");
    
    HTMLInputElement slider = doc.getElementById("slider");
    slider.onchange = (EventListener) {
      seeds = Math.parseInt(slider.value);
      drawFrame();
    };
    
    drawFrame();
  }
  
  // Draw the complete figure for the current number of seeds.
  void drawFrame() {
    ctx.clearRect(0, 0, MAX_D, MAX_D);
    for (var i=0; i<seeds; i++) {
      var theta = i * PI2 / PHI;
      var r = Math.sqrt(i) * SCALE_FACTOR;
      var x = xc + r * Math.cos(theta);
      var y = yc - r * Math.sin(theta);
      drawSeed(x,y);
    }
  }
  
  // Draw a small circle representing a seed centered at (x,y).
  void drawSeed(num x, num y) {
    ctx.beginPath();
    ctx.setLineWidth(2);
    ctx.setFillColor(ORANGE);
    ctx.setStrokeColor(ORANGE);
    ctx.arc(x, y, SEED_RADIUS, 0, PI2, false);
    ctx.fill();
    ctx.closePath();
    ctx.stroke();
  }

  CanvasRenderingContext2D ctx;
  num xc, yc;
  num seeds = 0;

  static final SEED_RADIUS = 2;
  static final SCALE_FACTOR = 4;
  static final PI2 = Math.PI * 2;
  static final PHI = (Math.sqrt(5)+1) / 2;
  static final MAX_D = 300;
  static final String ORANGE = "orange";

}
