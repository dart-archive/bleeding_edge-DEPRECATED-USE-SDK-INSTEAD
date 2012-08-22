// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('spirodraw');

#import('dart:html');
#import('dart:math', prefix: 'Math');

#source("ColorPicker.dart");

void main() {
  new Spirodraw().go();
}

class Spirodraw {
  static double PI2 = Math.PI * 2;
  Document doc;
  // Scale factor used to scale wheel radius from 1-10 to pixels
  int RUnits, rUnits, dUnits;
  // Fixed radius, wheel radius, pen distance in pixels
  double R, r, d; 
  InputElement fixedRadiusSlider, wheelRadiusSlider, 
    penRadiusSlider, penWidthSlider, speedSlider;
  SelectElement inOrOut;
  LabelElement numTurns;
  DivElement mainDiv;
  num lastX, lastY;
  int height, width, xc, yc;
  int maxTurns;
  CanvasElement frontCanvas, backCanvas;
  CanvasRenderingContext2D front, back;
  CanvasElement paletteElement; 
  ColorPicker colorPicker;
  String penColor = "red";
  int penWidth;
  double rad = 0.0;
  double stepSize;
  bool animationEnabled = true;
  int numPoints;
  double speed;
  bool run;
  
  Spirodraw() {
    doc = window.document;
    inOrOut = doc.query("#in_out");
    fixedRadiusSlider = doc.query("#fixed_radius");
    wheelRadiusSlider = doc.query("#wheel_radius");
    penRadiusSlider = doc.query("#pen_radius");
    penWidthSlider = doc.query("#pen_width");
    speedSlider = doc.query("#speed");
    numTurns = doc.query("#num_turns");
    mainDiv = doc.query("#main");
    frontCanvas = doc.query("#canvas");
    front = frontCanvas.context2d;
    backCanvas = new Element.tag("canvas");
    back = backCanvas.context2d;
    paletteElement = doc.query("#palette");
    window.on.resize.add((event) => onResize(), true);
    initControlPanel();
  }

  void go() {
    onResize();
  }
  
  void onResize() {
    height = window.innerHeight;
    width = window.innerWidth - 270;
    yc = height~/2;
    xc = width~/2;
    frontCanvas.height = height;
    frontCanvas.width = width;
    backCanvas.height = height;
    backCanvas.width = width;
    clear();
  }
  
  void initControlPanel() {
    inOrOut.on.change.add((event) => refresh(), true);
    fixedRadiusSlider.on.change.add((event) => refresh(), true);
    wheelRadiusSlider.on.change.add((event) => refresh(), true);
    speedSlider.on.change.add((event) => onSpeedChange(), true);
    penRadiusSlider.on.change.add((event) => refresh(), true);
    penWidthSlider.on.change.add((event) => onPenWidthChange(), true);
    colorPicker = new ColorPicker(paletteElement);
    colorPicker.addListener((String color) => onColorChange(color));
    doc.query("#start").on.click.add((event) => start(), true);
    doc.query("#stop").on.click.add((event) => stop(), true);
    doc.query("#clear").on.click.add((event) => clear(), true);
    doc.query("#lucky").on.click.add((event) => lucky(), true);
  }
  
  void onColorChange(String color) {
    penColor = color;
    drawFrame(rad);
  }

  void onSpeedChange() {
    speed = speedSlider.valueAsNumber;
    stepSize = calcStepSize();
  }
  
  void onPenWidthChange() {
    penWidth = penWidthSlider.valueAsNumber.toInt();
    drawFrame(rad);
  }
  
  void refresh() {
    stop();
    // Reset
    lastX = lastY = 0;
    // Compute fixed radius
    // based on starting diameter == min / 2, fixed radius == 10 units
    int min = Math.min(height, width);
    double pixelsPerUnit = min / 40;
    RUnits = fixedRadiusSlider.valueAsNumber.toInt();
    R = RUnits * pixelsPerUnit;
    // Scale inner radius and pen distance in units of fixed radius
    rUnits = wheelRadiusSlider.valueAsNumber.toInt();
    r = rUnits * R/RUnits * Math.parseInt(inOrOut.value);
    dUnits = penRadiusSlider.valueAsNumber.toInt();
    d = dUnits * R/RUnits;
    numPoints = calcNumPoints();
    maxTurns = calcTurns();
    onSpeedChange();
    numTurns.text = "0 / ${maxTurns}";
    penWidth = penWidthSlider.valueAsNumber.toInt();
    drawFrame(0.0);
  }

  int calcNumPoints() {
    if ((dUnits==0) || (rUnits==0))
      // Empirically, treat it like an oval
      return 2;
    int gcf_ = gcf(RUnits, rUnits);
    int n = RUnits ~/ gcf_;
    int d_ = rUnits ~/ gcf_;
    if (n % 2 == 1)
      // odd
      return n;
    else if (d_ %2 == 1)
      return n;
    else
      return n~/2;
  }

  // TODO return optimum step size in radians
  double calcStepSize() => speed / 100 * maxTurns / numPoints;

  void drawFrame(double theta) {
    if (animationEnabled) {
      front.clearRect(0, 0, width, height);
      front.drawImage(backCanvas, 0, 0);
      drawFixed();
    }
    drawWheel(theta);
  }

  bool animate(int time) {
    if (run && rad <= maxTurns * PI2) {
      rad+=stepSize;
      drawFrame(rad);
      int nTurns = (rad / PI2).toInt();
      numTurns.text = '${nTurns}/$maxTurns';
      window.webkitRequestAnimationFrame(animate);
    } else {
      stop();
    }
  }
  
  void start() {
    refresh();
    rad = 0.0;
    run = true;
    window.webkitRequestAnimationFrame(animate);
  }

  int calcTurns() {
    // compute ratio of wheel radius to big R then find LCM
    if ((dUnits==0) || (rUnits==0))
      return 1;
    int ru = rUnits.abs();
    int wrUnits = RUnits + rUnits;
    int g = gcf (wrUnits, ru);
    return (ru ~/ g).toInt();
  }

  void stop() {
    run = false;
    // Show drawing only
    front.clearRect(0, 0, width, height);
    front.drawImage(backCanvas, 0, 0);
    // Reset angle
    rad = 0.0;
  }

  void clear() {
    stop();
    back.clearRect(0, 0, width, height);
    refresh();
  }

  /**
   * Choose random settings for wheel and pen, but
   * leave fixed radius alone as it often changes
   * things too much.
   */
  void lucky() {
    var rand = new Math.Random();
    wheelRadiusSlider.valueAsNumber = rand.nextDouble() * 9;
    penRadiusSlider.valueAsNumber = rand.nextDouble() * 9;
    penWidthSlider.valueAsNumber = 1 + rand.nextDouble() * 9;
    colorPicker.selectedColor = colorPicker.getHexString(rand.nextDouble() * 215);
    start();
  }

  void drawFixed() {
    if (animationEnabled) {
      front.beginPath();
      front.setLineWidth(2);
      front.strokeStyle = "gray";
      front.arc(xc, yc, R, 0, PI2, true);
      front.closePath();
      front.stroke();
    }
  }

  /**
   * Draw the wheel with its center at angle theta
   * with respect to the fixed wheel
   * 
   * @param theta
   */
  void drawWheel(double theta) {
    double wx = xc + ((R + r) * Math.cos(theta));
    double wy = yc - ((R + r) * Math.sin(theta));
    if (animationEnabled) {
      if (rUnits>0) {
        // Draw ring
        front.beginPath();
        front.arc(wx, wy, r.abs(), 0, PI2, true);
        front.closePath();
        front.stroke();
        // Draw center
        front.setLineWidth(1);
        front.beginPath();
        front.arc(wx, wy, 3, 0, PI2, true);
        front.fillStyle = "black";
        front.fill();
        front.closePath();
        front.stroke();
      }
    }
    drawTip(wx, wy, theta);
  }

  /**
   * Draw a rotating line that shows the wheel rolling and leaves
   * the pen trace
   * 
   * @param wx X coordinate of wheel center
   * @param wy Y coordinate of wheel center
   * @param theta Angle of wheel center with respect to fixed circle
   */
  void drawTip(double wx, double wy, double theta) {
    // Calc wheel rotation angle
    double rot = (r==0) ? theta : theta * (R+r) / r;
    // Find tip of line
    double tx = wx + d * Math.cos(rot);
    double ty = wy - d * Math.sin(rot);
    if (animationEnabled) {
      front.beginPath();
      front.fillStyle = penColor;
      front.arc(tx, ty, penWidth/2+2, 0, PI2, true);
      front.fill();
      front.moveTo(wx, wy);
      front.strokeStyle = "black";
      front.lineTo(tx, ty);
      front.closePath();
      front.stroke();
    }
    drawSegmentTo(tx, ty);
  }

  void drawSegmentTo(double tx, double ty) {
    if (lastX > 0) {
      back.beginPath();
      back.strokeStyle = penColor;
      back.setLineWidth(penWidth);
      back.moveTo(lastX, lastY);
      back.lineTo(tx, ty);
      back.closePath();
      back.stroke();
    }
    lastX = tx;
    lastY = ty;
  }
  
}

int gcf(int n, int d) {
  if (n==d)
    return n;
  int max = Math.max(n, d);
  for (int i = max ~/ 2; i > 1; i--)
    if ((n % i == 0) && (d % i == 0))
      return i;
  return 1;
}
