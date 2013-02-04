// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
//
// Canvas API tests. Some of these are adapted from:
//
// http://www.html5canvastutorials.com/

library openglui_canvas_tests;

import 'gl.dart';
import 'dart:math' as Math;

var ctx;
var width, height;
bool isDirty = true;
var canvas;

void resize(int w, int h) {
  width = w;
  height = h;
}

void setup(canvasp, int w, int h) {
  if (canvasp == null) {
    canvas = new CanvasElement(width: w, height: h);
  } else {
    canvas = canvasp;
    // called from gl_driver.dart.
    // This is a kludge; we need a clean way of handling events.
    canvas.on.mouseDown.add((e) {
      ++testnum;
    });
  }
  ctx = canvas.getContext("2d");
  if (ctx == null) {
    throw "Failed to get 2D context";
  }
  resize(w, h);
  log("Done setup");
}

resetCanvas() {
  ctx.globalCompositeOperation = "source-over";
  ctx.setTransform(1, 0, 0, 1, 0, 0);
  ctx.fillStyle = "#FFFFFF";
  ctx.clearRect(0, 0, width, height);
  ctx.shadowOffsetX = ctx.shadowOffsetY = ctx.shadowBlur = 0.0;
  ctx.beginPath();
}

initTest(title) {
  resetCanvas();
  ctx.font = "15px Courier";
  ctx.textAlign = 'left';
  ctx.fillStyle = "black";
  ctx.fillText(title, 20, 20);
}

helloWorld() {
  initTest("Hello world");
  ctx.font = "30px Courier";
  ctx.strokeStyle = "#7F7F7F";
  ctx.fillStyle = "#7F7F7F";
  ctx.textAlign = "center";
  ctx.lineWidth = 2;
  ctx.strokeText("Align Center", width / 2, height / 4);
  ctx.textAlign = "left";
  ctx.fillText("Align Left", width / 2, height / 2);
  ctx.textAlign = "right";
  ctx.fillText("Align Right", width / 2, 3 * height / 4);
}

blocks() {
  initTest("fillRect/strokeRect");
  ctx.fillStyle = "#FF0000";
  ctx.fillRect(width / 10, height / 10, width / 2, height / 25);
  ctx.fillStyle = "#00FF00";
  ctx.fillRect(width / 4, height / 5, width / 20, height / 8);
  //ctx.fillStyle = "rgba(0,0,255,0.8)";
  ctx.strokeStyle = "rgba(128,128,128, 0.5)";
  //ctx.fillStyle = "#7F7F7F";
  ctx.strokeRect(width / 5, height / 10, width / 2, height / 8);
}

square(left, top, width, height) {
  ctx.beginPath();
  ctx.moveTo(left, top);
  ctx.lineTo(left + width, top);
  ctx.lineTo(left + width, top + height);
  ctx.lineTo(left, top + height);
  ctx.closePath(); //lineTo(left, top);
}

squares() {
  initTest("fill/stroke paths");
  ctx.strokeStyle = "black";
  ctx.fillStyle = "#FF0000";
  ctx.lineWidth = 4;
  square(width / 10, height / 10, width / 2, height / 25);
  ctx.fill();
  ctx.stroke();
  ctx.fillStyle = "#00FF00";
  square(width / 4, height / 5, width / 20, height / 8);
  ctx.fill();
  ctx.stroke();
  ctx.fillStyle = "rgba(128,128,128, 0.5)";
  square(width / 5, height / 10, width / 2, height / 8);
  ctx.fill();
  ctx.stroke();
}

lineJoin() {
  initTest("Line joins");
  ctx.strokeStyle = "black";
  ctx.fillStyle = "#FF0000";
  ctx.lineWidth = 8;
  ctx.lineJoin = "round";
  square(width / 10, height / 10, width / 2, height / 25);
  ctx.stroke();
  ctx.fillStyle = "#00FF00";
  ctx.lineJoin = "bevel";
  square(width / 4, height / 5, width / 20, height / 8);
  ctx.stroke();
  ctx.fillStyle = "rgba(128,128,128, 0.5)";
  ctx.lineJoin = "miter";
  square(width / 5, height / 10, width / 2, height / 8);
  ctx.stroke();
}

grid() {
  initTest("line strokes");
  ctx.lineWidth = 1;
  for (var x = 0.5; x < width; x += 10) {
    ctx.moveTo(x, 0);
    ctx.lineTo(x, height);
  }
  for (var y = 0.5; y < height; y += 10) {
    ctx.moveTo(0, y);
    ctx.lineTo(width, y);
  }
  ctx.strokeStyle = "#eee";
  ctx.stroke();
}

line(x1, y1, x2, y2) {
  ctx.beginPath();
  ctx.moveTo(x1, y1);
  ctx.lineTo(x2, y2);
}

strokeLines() {
  initTest("line caps");
  ctx.lineWidth = height / 40;
  ctx.strokeStyle = '#0000ff';
  // butt line cap (top line)
  line(width / 4, height / 4, 3 * width / 4, height / 4);
  ctx.lineCap = 'butt';
  ctx.stroke();

  // round line cap (middle line)
  line(width / 4, height / 2, 3 * width / 4, height / 2);
  ctx.lineCap = 'round';
  ctx.stroke();

  // square line cap (bottom line)
  line(width / 4,  3 * height / 4, 3 * width / 4, 3 * height / 4);
  ctx.lineCap = 'square';
  ctx.stroke();
}

colors() {
  initTest("Colors");
  var colors = [
    "maroon",
    "red",
    "orange",
    "yellow",
    "olive",
    "purple",
    "fuschia",
    "white",
    "lime",
    "green",
    "navy",
    "blue",
    "aqua",
    "teal",
    "silver",
    "gray",
    "black"
  ];

  var i = 1;
  var yinc = height / (2 * colors.length + 1);
  
  ctx.textAlign = "center";
  ctx.font = "${yinc}px Courier";
  
  for (var color in colors) {
    ctx.fillStyle = color;
    ctx.fillRect(width / 4, i * 2 * yinc, width / 2, 3 * yinc / 2);
    ctx.fillStyle = (color == "gray") ? "white" : "gray";
    ctx.fillText(color, width / 2, i * 2 * yinc + 7 * yinc / 8);
    ++i;
  }
}

smiley() {
  initTest("arcs");
  ctx.translate(width / 2 - 80, height / 2 - 75);

  ctx.beginPath();
  ctx.arc(100,80,75,0,Math.PI*2,true);
  ctx.fillStyle = "rgb(255,255,204)";
  ctx.fill();
  ctx.stroke();

  ctx.fillStyle = "black";
  ctx.beginPath();
  ctx.arc(80,55,8,0,Math.PI*2,true);
  ctx.fill();

  ctx.beginPath();
  ctx.arc(120,55,8,0,Math.PI*2,true);
  ctx.fill();

  ctx.beginPath();
  ctx.arc(100,85,10,4,Math.PI*2,true);
  ctx.stroke();

  ctx.beginPath();
  ctx.arc(100,95,30,0,Math.PI,false);
  ctx.stroke();

  ctx.setTransform(1, 0, 0, 1, 0, 0);
}

rot(txt1, [txt2, sx = 1.0, sy = 1.0]) {
  if (txt2 == null) txt2 = txt1;
  ctx.font = "50px sans serif";
  ctx.translate(width / 2, height / 2);
  ctx.textAlign = "right";
  ctx.fillStyle = "red";
  ctx.fillText(txt1, 0, 0);
  ctx.rotate(Math.PI / 2);
  ctx.scale(sx, sy);
  ctx.fillStyle = "green";
  ctx.fillText(txt2, 0, 0);
  ctx.scale(sx, sy);
  ctx.rotate(Math.PI / 2);
  ctx.fillStyle = "blue";
  ctx.fillText(txt1, 0, 0);
  ctx.scale(sx, sy);
  ctx.rotate(Math.PI / 2);
  ctx.fillStyle = "yellow";
  ctx.fillText(txt2, 0, 0);
  ctx.setTransform(1, 0, 0, 1, 0, 0);
}

rotate() {
  initTest("Rotation");
  rot("Dart");
}

alpha() {
  initTest("Global alpha");
  ctx.fillStyle = "gray";
  ctx.fillRect(0, 0, width, height);
  grid();
  ctx.globalAlpha = 0.5;
  rot("Global", "Alpha");
  ctx.globalAlpha = 1.0;
}

scale() {
  initTest("Scale");
  rot("Scale", "Test", 0.8, 0.5);
}

curves() {
  initTest("Curves");
  ctx.beginPath();
  ctx.moveTo(188, 150);
  ctx.quadraticCurveTo(288, 0, 388, 150);
  ctx.lineWidth = 2;
  ctx.strokeStyle = 'red';
  ctx.stroke();
  ctx.beginPath();
  ctx.moveTo(188, 130);
  ctx.bezierCurveTo(140, 10, 388, 10, 388, 170);
  ctx.lineWidth = 5;
  ctx.strokeStyle = 'blue';
  ctx.stroke();
  var x1, x2, y1, y2, ex, ey;
  ctx.beginPath();
  x1 = width / 4;
  y1 = height / 2;
  x2 = width / 2;
  y2 = height / 4;
  ex = 3 * width / 4;
  ey = y1;
  ctx.moveTo(x1, x2);
  ctx.quadraticCurveTo(x2, y2, ex, ey);
  ctx.lineWidth = 10;
  ctx.strokeStyle = 'green';
  ctx.stroke();

  ctx.beginPath();
  ctx.moveTo(188, 130);
  x1 = width / 8;
  x2 = 7 * width / 8;
  y1 = height / 50;
  y2 = y1;
  ex = x2;
  ey = height / 2;
  ctx.bezierCurveTo(x1, y1, x2, y2, ex, ey);
  ctx.lineWidth = 7;
  ctx.strokeStyle = 'black';
  ctx.stroke();

  // Draw a cloud
  ctx.beginPath();
  var wscale = width / 578;
  var hscale = height / 800;
  ctx.translate(0, height / 2);
  ctx.moveTo(170 * wscale, 80 * hscale);
  ctx.bezierCurveTo(130 * wscale, 100 * hscale,
                    130 * wscale, 150 * hscale,
                    230 * wscale, 150 * hscale);
  ctx.bezierCurveTo(250 * wscale, 180 * hscale,
                    320 * wscale, 180 * hscale,
                    340 * wscale, 150 * hscale);
  ctx.bezierCurveTo(420 * wscale, 150 * hscale,
                    420 * wscale, 120 * hscale,
                    390 * wscale, 100 * hscale);
  ctx.bezierCurveTo(430 * wscale, 40 * hscale,
                    370 * wscale, 30 * hscale,
                    340 * wscale, 50 * hscale);
  ctx.bezierCurveTo(320 * wscale, 5 * hscale,
                    250 * wscale, 20 * hscale,
                    250 * wscale, 50 * hscale);
  ctx.bezierCurveTo(200 * wscale, 5 * hscale,
                    150 * wscale, 20 * hscale,
                    170 * wscale, 80 * hscale);
  ctx.closePath();
  ctx.lineWidth = 5;
  ctx.fillStyle = 'gray';
  ctx.fill();
}

void shadows() {
  initTest("Shadows");
  ctx.shadowBlur=20;
  ctx.shadowColor="black";
  ctx.fillStyle="red";
  var w = width / 2;
  if (w > height / 2) w = height / 2;
  ctx.fillRect(width / 2 - w / 2, height / 4 - w / 2, w, w);
  ctx.shadowOffsetX = 10;
  ctx.shadowOffsetY = 10;
  ctx.shadowColor="green";
  ctx.fillRect(width / 2 - w / 2, 3 * height / 4 - w / 2, w, w);
}

void lineJoins() {
  initTest("Line joins");
  ctx.lineWidth=10;
  ctx.lineJoin="miter";
  ctx.moveTo(width / 2 - 25, height / 4 - 10);
  ctx.lineTo(width /2 + 25, height / 4);
  ctx.lineTo(width / 2 - 25, height / 4 + 10);
  ctx.stroke();
  ctx.lineJoin="round";
  ctx.moveTo(width / 2 - 25, height / 2 - 10);
  ctx.lineTo(width /2 + 25, height / 2);
  ctx.lineTo(width / 2 - 25, height / 2 + 10);
  ctx.stroke();
  ctx.lineJoin="bevel";
  ctx.moveTo(width / 2 - 25, 3 * height / 4 - 10);
  ctx.lineTo(width /2 + 25, 3 * height / 4);
  ctx.lineTo(width / 2 - 25, 3 * height / 4 + 10);
  ctx.stroke();
}

void saveRestore() {
  initTest("Save/restore state");
  ctx.font = "30px courier";
  ctx.fillStyle = "red";
  ctx.strokeStyle = "black";
  ctx.shadowBlur = 5;
  ctx.shadowColor = "green";
  ctx.lineWidth = 1;
  ctx.textAlign = "left";
  ctx.rotate(Math.PI / 30);
  ctx.fillText("State 1", width /2, height / 6);
  ctx.strokeText("State 1", width /2, height / 6);
  ctx.save();

  ctx.font = "40px sans serif";
  ctx.fillStyle = "blue";
  ctx.strokeStyle = "orange";
  ctx.shadowBlur = 8;
  ctx.shadowOffsetX = 5;
  ctx.shadowColor = "black";
  ctx.lineWidth = 2;
  ctx.textAlign = "right";
  ctx.rotate(Math.PI / 30);
  ctx.fillText("State 2", width /2, 2 * height / 6);
  ctx.strokeText("State 2", width /2, 2 * height / 6);
  ctx.save();

  ctx.font = "50px times roman";
  ctx.fillStyle = "yellow";
  ctx.strokeStyle = "gray";
  ctx.shadowBlur = 8;
  ctx.shadowOffsetX = 5;
  ctx.shadowColor = "red";
  ctx.lineWidth = 3;
  ctx.textAlign = "center";
  ctx.rotate(-Math.PI / 15);
  ctx.fillText("State 3", width /2, 3 * height / 6);
  ctx.strokeText("State 3", width /2, 3 * height / 6);

  ctx.restore();
  ctx.fillText("State 2", width /2, 4 * height / 6);
  ctx.strokeText("State 2", width /2, 4 * height / 6);

  ctx.restore();
  ctx.fillText("State 1", width /2, 5 * height / 6);
  ctx.strokeText("State 1", width /2, 5 * height / 6);
}

void mirror() {
  initTest("Mirror");
  // translate context to center of canvas
  ctx.translate(width / 2, height / 2);

  // flip context horizontally
  ctx.scale(-1, 1);

  ctx.font = '30pt Calibri';
  ctx.textAlign = 'center';
  ctx.fillStyle = 'blue';
  ctx.fillText('Magic Mirror', 0, 0);
}

void oval() {
  initTest("Path - pop state - stroke");
  var centerX = 0;
  var centerY = 0;
  var radius = 50;

  // save state
  ctx.save();

  // translate context
  ctx.translate(width / 2, height / 2);

  // scale context horizontally
  ctx.scale(2, 1);

  // draw circle which will be stretched into an oval
  ctx.beginPath();
  ctx.arc(centerX, centerY, radius, 0, 2 * Math.PI, false);

  // restore to original state
  ctx.restore();

  // apply styling
  ctx.fillStyle = '#8ED6FF';
  ctx.fill();
  ctx.lineWidth = 5;
  ctx.strokeStyle = 'black';
  ctx.stroke();
}

void lineDash() {
  initTest("Line dash");
  ctx.setLineDash([ 5, 8, 3 ]);
  ctx.strokeStyle = "#FF0000";
  ctx.strokeRect(width / 10, height / 10, width / 2, height / 25);
  ctx.lineDashOffset = 1;
  ctx.strokeStyle = "#00FF00";
  ctx.strokeRect(width / 4, height / 5, width / 20, height / 8);
  ctx.setLineDash([]);
  ctx.strokeStyle = "#0000FF";
  ctx.strokeStyle = "rgba(128,128,128, 0.5)";
  ctx.strokeRect(width / 5, height / 10, width / 2, height / 8);
  log("Width = $width");
}

// TODO(gram) - apparently a canvas can have only one op in its lifetime,
// so this test won't work until we can use off-screen canvases.
void compositeOp() {
  initTest("Composition");
  var num = 0;
  ctx.font = '10pt Verdana';
  var numPerRow = width ~/ 95;
  log("Width = $width, numPerRow = $numPerRow\n");
  for (var mode in [ 'source-atop', 'source-in',
                     'source-out', 'source-over',
                     'destination-atop', 'destination-in',
                     'destination-out', 'destination-over',
                     'lighter', 'darker',
                     'xor', 'copy']) {
    var col = num % numPerRow;
    var row = num ~/ numPerRow;
    ctx.globalCompositeOperation = mode;
    ctx.translate(95 * col, 100 * row);
    log("Doing $mode at row $row, col $col\n");
    ctx.beginPath();
    ctx.rect(0, 0, 55, 55);
    ctx.fillStyle = 'blue';
    ctx.fill();
    ctx.beginPath();
    ctx.arc(50, 50, 35, 0, 2 * Math.PI, false);
    ctx.fillStyle = 'red';
    ctx.fill();
    ctx.fillStyle = 'black';
    ctx.fillText(mode, 0, 100);
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ++num;
  }
}

void loadImage() {
  initTest("Image loading");
  var imageObj = new ImageElement();
  imageObj.on.load.add((e) {
    ctx.drawImage(imageObj, 69, 50);
  });
  imageObj.src = 'http://www.html5canvastutorials.com/demos/assets/darth-vader.bmp';
}

void clip() {
  initTest("Clipping");
  var x = width / 2;
  var y = height / 2;
  var radius = height / 4;
  var offset = 2 * radius / 3;

  ctx.save();
  ctx.beginPath();
  ctx.arc(x, y, radius, 0, 2 * Math.PI, false);
  ctx.clip();

  // draw blue circle inside clipping region
  ctx.beginPath();
  ctx.arc(x - offset, y - offset, radius, 0, 2 * Math.PI, false);
  ctx.fillStyle = 'blue';
  ctx.fill();

  // draw yellow circle inside clipping region
  ctx.beginPath();
  ctx.arc(x + offset, y, radius, 0, 2 * Math.PI, false);
  ctx.fillStyle = 'yellow';
  ctx.fill();

  // draw red circle inside clipping region
  ctx.beginPath();
  ctx.arc(x, y + offset, radius, 0, 2 * Math.PI, false);
  ctx.fillStyle = 'red';
  ctx.fill();

  // Restore the canvas context to its original state
  // before we defined the clipping region
  ctx.restore();
  ctx.beginPath();
  ctx.arc(x, y, radius, 0, 2 * Math.PI, false);
  ctx.lineWidth = 10;
  ctx.strokeStyle = 'blue';
  ctx.stroke();
}

void shear() {
  initTest("Transform");
  var rectWidth = width / 4;
  var rectHeight = height / 8;

  // shear matrix:
  //  1  sx  0
  //  sy  1  0
  //  0  0  1

  var sx = 0.75;
  // .75 horizontal shear
  var sy = 0;
  // no vertical shear

  // translate context to center of canvas
  ctx.translate(width / 2, height / 2);

  // apply custom transform
  ctx.transform(1, sy, sx, 1, 0, 0);

  ctx.fillStyle = 'blue';
  ctx.fillRect(-rectWidth / 2, rectHeight / -2, rectWidth, rectHeight);
}

int numtests = 21;
int testnum = numtests - 1; // Start with latest.

void update() {
  if (!isDirty) return;
  switch(testnum) {
    case 0:
      helloWorld();
      break;
    case 1:
      smiley();
      break;
    case 2:
      blocks();
      break;
    case 3:
      squares();
      break;
    case 4:
      grid();
      break;
    case 5:
      strokeLines();
      break;
    case 6:
      lineJoin();
      break;
    case 7:
      colors();
      break;
    case 8:
      rotate();
      break;
    case 9:
      alpha();
      break;
    case 10:
      scale();
      break;
    case 11:
      curves();
      break;
    case 12:
      shadows();
      break;
    case 13:
      lineJoins();
      break;
    case 14:
      saveRestore();
      break;
    case 15:
      mirror();
      break;
    case 16:
      oval();
      break;
    case 17:
      lineDash();
      break;
    case 18:
      loadImage();
      break;
    case 19:
      clip();
      break;
    case 20:
      shear();
      break;
    default:
      testnum = 0;
      update();
      break;
  }
  isDirty = false;
}

/*
TODO(gram): below are the current entry points for input events for the
native code version. We need to integrate these somehow with the WebGL
version:
*/
onMotionDown(num when, num x, num y) {
  ++testnum;
  isDirty = true;
  log("Marking dirty");
}
onMotionUp(num when, num x, num y) {}
onMotionMove(num when, num x, num y) {}
onMotionCancel(num when, num x, num y) {}
onMotionOutside(num when, num x, num y) {}
onMotionPointerDown(num when, num x, num y) {
  ++testnum;
  isDirty = true;
  log("Marking dirty");
}

onMotionPointerUp(num when, num x, num y) {}

onKeyDown(num when, int flags, int keycode, int metastate, int repeat) {
  ++testnum;
  isDirty = true;
  log("Marking dirty");
}

onKeyUp(num when, int flags, int keycode, int metastate, int repeat) {}

onKeyMultiple(num when, int flags, int keycode, int metastate, int repeat) {
}


