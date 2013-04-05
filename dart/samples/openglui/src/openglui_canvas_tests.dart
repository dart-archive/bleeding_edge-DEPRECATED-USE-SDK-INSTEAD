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

void setup(canvasp, int w, int h, int f) {
  if (canvasp == null) {
    log("Allocating canvas");
    canvas = new CanvasElement(width: w, height: h);
    document.body.nodes.add(canvas);
  } else {
    log("Using parent canvas");
    canvas = canvasp;
  }
  canvas.onMouseDown.listen((e) {
    ++testnum;
    isDirty = true;
  });
  canvas.onKeyDown.listen((e) {
    ++testnum;
    isDirty = true;
  });
  ctx = canvas.getContext("2d");
  if (ctx == null) {
    throw "Failed to get 2D context";
  }
  resize(w, h);
  window.requestAnimationFrame(update);
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

void loadImage() {
  initTest("Image loading");
  var imageObj = new ImageElement();
  // Setting src before onLoad is a more interesting test.
  imageObj.src = 'chrome.png';
  imageObj.onLoad.listen((e) {
    ctx.drawImage(e.target, 0, 0, width, height, 0, 0, width, height);
  });
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

void composite() {
  initTest("Composition");
  var num = 0;
  var numPerRow = width ~/ 150;
  var tempCanvas = new CanvasElement(width: width, height:height);
  var tempContext = tempCanvas.getContext("2d");
  log("Width = $width, numPerRow = $numPerRow\n");
  for (var mode in [ 'source-atop', 'source-in',
                     'source-out', 'source-over',
                     'destination-atop', 'destination-in',
                     'destination-out', 'destination-over',
                     'lighter', 'darker',
                     'xor', 'copy']) {
    tempContext.save();
    tempContext.clearRect(0, 0, width, height);
    tempContext.beginPath();
    tempContext.rect(0, 0, 55, 55);
    tempContext.fillStyle = 'blue';
    tempContext.fill();

    tempContext.globalCompositeOperation = mode;
    tempContext.beginPath();
    tempContext.arc(50, 50, 35, 0, 2 * Math.PI, false);
    tempContext.fillStyle = 'red';
    tempContext.fill();
    tempContext.restore();
    tempContext.font = '10pt Verdana';
    tempContext.fillStyle = 'black';
    tempContext.fillText(mode, 0, 100);
    if (num > 0) {
      if ((num % numPerRow) == 0) {
        ctx.translate(-150 * (numPerRow-1), 150);
      } else {
        ctx.translate(150, 0);
      }
    }
    ctx.drawImage(tempCanvas, 0, 0);
    ++num;
  }
}

class Rectangle {
  num x, y, width, height, borderWidth;
}

var startTime = 0;
var myRectangle = null;

void anim() {
  if (myRectangle == null) {
    myRectangle = new Rectangle();
    myRectangle.x = 250;
    myRectangle.y = 70;
    myRectangle.width = 100;
    myRectangle.height = 50;
    myRectangle.borderWidth = 5;
    startTime = (new DateTime.now()).millisecondsSinceEpoch;
  }

  var now = (new DateTime.now()).millisecondsSinceEpoch;
  var time = now - startTime;
  var amplitude = 150;

  // in ms
  var period = 2000;
  var centerX = width / 2 - myRectangle.width / 2;
  var nextX = amplitude * Math.sin(time * 2 * Math.PI / period) + centerX;
  myRectangle.x = nextX;

  // clear
  ctx.clearRect(0, 0, width, height);

  // draw

  ctx.beginPath();
  ctx.rect(myRectangle.x, myRectangle.y, myRectangle.width, myRectangle.height);
  ctx.fillStyle = '#8ED6FF';
  ctx.fill();
  ctx.lineWidth = myRectangle.borderWidth;
  ctx.strokeStyle = 'black';
  ctx.stroke();
}

void linearGradient() {
  initTest("Linear Gradient");
  ctx.rect(0, 0, width, height);
  var grd = ctx.createLinearGradient(0, 0, width, height);
  // light blue
  grd.addColorStop(0, '#8ED6FF');   
  // dark blue
  grd.addColorStop(1, '#004CB3');
  ctx.fillStyle = grd;
  ctx.fill();
}

void radialGradient() {
  initTest("Radial Gradient");
  ctx.rect(0, 0, width, height);
  var grd = ctx.createRadialGradient(238, 50, 10, 238, 50, 300);
  // light blue
  grd.addColorStop(0, '#8ED6FF');
  // dark blue
  grd.addColorStop(1, '#004CB3');
  ctx.fillStyle = grd;
  ctx.fill();
}

int testnum = 0; // Set this to -1 to start with last test.

double x, y, z;

onAccelerometer(double xx, double yy, double zz) {
 x = xx;
 y = yy;
 z = zz;
}

void update(num when) {
  window.requestAnimationFrame(update);
  if (testnum == 0) {
    anim();
    return;
  }
  if (!isDirty) return;
  switch(testnum) {
    case 1:
      helloWorld();
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
    case 21:
      composite();
      break;
    case 22:
      smiley();
      break;
    case 23:
      linearGradient();
      break;
    case 24:
      radialGradient();
      break;
    case 25:
      break; // Skip for now; this is really slow.
      var rayTracer = new RayTracer();
      rayTracer.render(defaultScene(), ctx, width, height);
      break;
    default:
      if (testnum < 0) {
        testnum = 24;
      } else {
        testnum = 0;
      }
      return;
  }

  isDirty = false;
}

// Raytracer adapted from https://gist.github.com/mythz/3817303.

Scene defaultScene() =>
   new Scene(
      [new Plane(new Vector(0.0, 1.0, 0.0), 0.0, Surfaces.checkerboard),
       new Sphere(new Vector(0.0, 1.0, -0.25), 1.0, Surfaces.shiny),
       new Sphere(new Vector(-1.0, 0.5, 1.5), 0.5, Surfaces.shiny)],
      [new Light(new Vector(-2.0, 2.5, 0.0), new Color(0.49, 0.07, 0.07) ),
       new Light(new Vector(1.5, 2.5, 1.5),  new Color(0.07, 0.07, 0.49) ),
       new Light(new Vector(1.5, 2.5, -1.5), new Color(0.07, 0.49, 0.071) ),
       new Light(new Vector(0.0, 3.5, 0.0),  new Color(0.21, 0.21, 0.35) )],
      new Camera(new Vector(3.0, 2.0, 4.0), new Vector(-1.0, 0.5, 0.0))
   );


class Vector {
   num x, y, z;
   Vector(this.x, this.y, this.z);

   operator -(Vector v)  => new Vector(x - v.x, y - v.y, z - v.z);
   operator +(Vector v)  => new Vector(x + v.x, y + v.y, z + v.z);
   static times(num k, Vector v) => new Vector(k * v.x, k * v.y, k * v.z);
   static num dot(Vector v1, Vector v2)  =>
       v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
   static num mag(Vector v)  => Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
   static Vector norm(Vector v) {
     var _mag = mag(v);
     var div = _mag == 0 ? double.INFINITY : 1.0 / _mag;
     return times(div, v);
   }
   static Vector cross(Vector v1, Vector v2) {
     return new Vector(v1.y * v2.z - v1.z * v2.y,
         v1.z * v2.x - v1.x * v2.z,
         v1.x * v2.y - v1.y * v2.x);
   }
}

class Color {
   num r, g, b;
   static final white = new Color(1.0, 1.0, 1.0);
   static final grey = new Color(0.5, 0.5, 0.5);
   static final black = new Color(0.0, 0.0, 0.0);
   static final background = Color.black;
   static final defaultColor = Color.black;

   Color(this.r,this.g,this.b);
   static scale(num k, Color v) => new Color(k * v.r, k * v.g, k * v.b);
   operator +(Color v) => new Color(r + v.r, g + v.g, b + v.b);
   operator *(Color v) => new Color(r * v.r, g * v.g, b * v.b);
   static _intColor(num d) => ((d > 1 ? 1 : d) * 255).toInt();
   static String toDrawingRGB(Color c) =>
       "rgb(${_intColor(c.r)}, ${_intColor(c.g)}, ${_intColor(c.b)})";
}

class Camera {
  Vector pos, forward, right, up;
  Camera (this.pos, Vector lookAt) {
     var down = new Vector(0.0, -1.0, 0.0);
     forward = Vector.norm(lookAt - pos);
     right = Vector.times(1.5, Vector.norm(Vector.cross(forward, down)));
     up = Vector.times(1.5, Vector.norm(Vector.cross(forward, right)));
  }
}

class Ray {
  Vector start, dir;
  Ray([this.start, this.dir]);
}

class Intersection {
  Thing thing;
  Ray ray;
  num dist;
  Intersection(this.thing, this.ray, this.dist);
}

class Light {
  Vector pos;
  Color color;
  Light(this.pos, this.color);
}

abstract class Surface {
  int roughness;
  Color diffuse(Vector pos);
  Color specular(Vector pos);
  num reflect(Vector pos);
}

abstract class Thing {
  Intersection intersect(Ray ray);
  Vector normal(Vector pos);
  Surface surface;
}

class Scene {
  List<Thing> things;
  List<Light> lights;
  Camera camera;
  Scene([this.things,this.lights,this.camera]);
}

class Sphere implements Thing {
  num radius2, radius;
  Vector center;
  Surface surface;

  Sphere (this.center, this.radius, this.surface) {
       this.radius2 = radius * radius;
  }
  normal(Vector pos) => Vector.norm(pos - center);
  intersect(Ray ray) {
    var eo = this.center - ray.start;
    var v = Vector.dot(eo, ray.dir);
    var dist = 0;
    if (v >= 0) {
      var disc = this.radius2 - (Vector.dot(eo, eo) - v * v);
      if (disc >= 0) {
         dist = v - Math.sqrt(disc);
      }
    }
    return dist == 0 ? null : new Intersection(this, ray, dist);
  }
}

class Plane implements Thing {
  Vector norm;
  num offset;
  Surface surface;
  Plane(this.norm, this.offset, this.surface);
  Vector normal(Vector pos) => norm;
  Intersection intersect(Ray ray) {
    var denom = Vector.dot(norm, ray.dir);
    if (denom > 0) {
      return null;
    } else {
      var dist = (Vector.dot(norm, ray.start) + offset) / (-denom);
      return new Intersection(this, ray, dist);
    }
  }
}

class CustomSurface implements Surface {
  Color diffuseColor, specularColor;
  int roughness;
  num reflectPos;
  CustomSurface(this.diffuseColor, this.specularColor,
      this.reflectPos, this.roughness);
  diffuse(pos) => diffuseColor;
  specular(pos) => specularColor;
  reflect(pos) => reflectPos;
}

class CheckerBoardSurface implements Surface {
  int roughness;
  CheckerBoardSurface([this.roughness=150]);
  diffuse(pos) => (pos.z.floor() + pos.x.floor()) % 2 != 0
    ? Color.white
    : Color.black;
  specular(pos) => Color.white;
  reflect(pos)  => (pos.z.floor() + pos.x.floor()) % 2 != 0 ? 0.1 : 0.7;
}

class Surfaces {
  static final shiny = new CustomSurface(Color.white, Color.grey, 0.7, 250);
  static final checkerboard = new CheckerBoardSurface();
}

class RayTracer {
  num _maxDepth = 5;

  Intersection _intersections(Ray ray, Scene scene) {
    var closest = double.INFINITY;
    Intersection closestInter = null;
    for (Thing thing in scene.things) {
      var inter = thing.intersect(ray);
      if (inter != null && inter.dist < closest) {
        closestInter = inter;
        closest = inter.dist;
      }
    }
    return closestInter;
  }

  _testRay(Ray ray, Scene scene) {
    var isect = _intersections(ray, scene);
    return isect != null ? isect.dist : null;
  }

  _traceRay(Ray ray, Scene scene, num depth) {
    var isect = _intersections(ray, scene);
    return isect == null ? Color.background : _shade(isect, scene, depth);
  }

  _shade(Intersection isect, Scene scene, num depth) {
    var d = isect.ray.dir;
    var pos = Vector.times(isect.dist, d) + isect.ray.start;
    var normal = isect.thing.normal(pos);
    var reflectDir = d -
        Vector.times(2, Vector.times(Vector.dot(normal, d), normal));
    var naturalColor = Color.background +
        _getNaturalColor(isect.thing, pos, normal, reflectDir, scene);
    var reflectedColor = (depth >= _maxDepth) ? Color.grey :
         _getReflectionColor(isect.thing, pos, normal, reflectDir,
             scene, depth);
    return naturalColor + reflectedColor;
  }

  _getReflectionColor(Thing thing, Vector pos, Vector normal, Vector rd,
      Scene scene, num depth) =>
          Color.scale(thing.surface.reflect(pos),
              _traceRay(new Ray(pos, rd), scene, depth + 1));

  _getNaturalColor(Thing thing, Vector pos, Vector norm, Vector rd,
      Scene scene) {
    var addLight = (col, light) {
      var ldis = light.pos - pos;
      var livec = Vector.norm(ldis);
      var neatIsect = _testRay(new Ray(pos, livec), scene);
      var isInShadow = neatIsect == null ? false :
          (neatIsect <= Vector.mag(ldis));
      if (isInShadow) {
        return col;
      } else {
        var illum = Vector.dot(livec, norm);
        var lcolor = (illum > 0) ? Color.scale(illum, light.color)
                                 : Color.defaultColor;
        var specular = Vector.dot(livec, Vector.norm(rd));
        var scolor = (specular > 0)
            ? Color.scale(Math.pow(specular, thing.surface.roughness),
                light.color)
            : Color.defaultColor;
        return col + (thing.surface.diffuse(pos)  * lcolor)
            + (thing.surface.specular(pos) * scolor);
      }
    };
    return scene.lights.fold(Color.defaultColor, addLight);
  }

  render(Scene scene, CanvasRenderingContext2D ctx, num screenWidth,
         num screenHeight) {
    var getPoint = (x, y, camera) {
      var recenterX = (x) => (x - (screenWidth / 2.0)) / 2.0 / screenWidth;
      var recenterY = (y) => - (y - (screenHeight / 2.0)) / 2.0 / screenHeight;
      return Vector.norm(camera.forward
          + Vector.times(recenterX(x), camera.right)
          + Vector.times(recenterY(y), camera.up));
    };
    for (int y = 0; y < screenHeight; y++) {
      for (int x = 0; x < screenWidth; x++) {
        var color = _traceRay(new Ray(scene.camera.pos,
            getPoint(x, y, scene.camera) ), scene, 0);
        ctx.fillStyle = Color.toDrawingRGB(color);
        ctx.fillRect(x, y, x + 1, y + 1);
      }
    }
  }
}

