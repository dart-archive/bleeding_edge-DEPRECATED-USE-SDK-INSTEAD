// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import('dart:dom');

#source('Ball.dart');
#source('Balls.dart');
#source('ClockNumber.dart');
#source('ClockNumbers.dart');
#source('Colon.dart');
#source('Util.dart');

void main() {
  new CountDownClock();
}

class CountDownClock {

  static final int NUMBER_SPACING = 19;
  static final int BALL_WIDTH = 19;
  static final int BALL_HEIGHT = 19;

  List<ClockNumber> hours, minutes, seconds;
  Balls balls;

  CountDownClock() :
      hours = new List<ClockNumber>(2),
      minutes = new List<ClockNumber>(2),
      seconds = new List<ClockNumber>(2),
      balls = new Balls() {
    createNumbers();
    updateTime();
    window.setInterval(f() => updateTime(), 1000);

    balls.tick();
    window.setInterval(f() => balls.tick(), 50);
  }

  void updateTime() {
    int time = Util.currentTimeMillis() ~/ 1000;
    time %= 86400;
    setDigits(pad2(time ~/ 3600), hours);
    time %= 3600;
    setDigits(pad2((time ~/ 60)), minutes);
    time %= 60;
    setDigits(pad2(time), seconds);
  }

  void setDigits(String digits, List<ClockNumber> numbers) {
    for (int i = 0; i < numbers.length; ++i) {
      int digit = digits.charCodeAt(i) - '0'.charCodeAt(0);
      numbers[i].setPixels(ClockNumbers.PIXELS[digit]);
    }
  }

  String pad3(int num) {
    if (num < 10) {
      return "00${num}";
    }
    if (num < 100) {
      return "0${num}";
    }
    return "${num}";
  }

  String pad2(int num) {
    if (num < 10) {
      return "0${num}";
    }
    return "${num}";
  }

  void createNumbers() {
    HTMLDivElement root = window.document.createElement('div');
    Util.rel(root);
    root.style.setProperty("textAlign", 'center');
    window.document.getElementById("canvas-content").appendChild(root);

    int x = 0;

    for (int i = 0; i < hours.length; ++i) {
      hours[i] = new ClockNumber(this, x, 2);
      root.appendChild(hours[i].root);
      Util.pos(hours[i].root, x, 0);
      x += BALL_WIDTH * ClockNumber.WIDTH + NUMBER_SPACING;
    }

    root.appendChild(new Colon(x).root);
    x += BALL_WIDTH + NUMBER_SPACING;

    for (int i = 0; i < minutes.length; ++i) {
      minutes[i] = new ClockNumber(this, x, 5);
      root.appendChild(minutes[i].root);
      Util.pos(minutes[i].root, x, 0);
      x += BALL_WIDTH * ClockNumber.WIDTH + NUMBER_SPACING;
    }

    root.appendChild(new Colon(x).root);
    x += BALL_WIDTH + NUMBER_SPACING;

    for (int i = 0; i < seconds.length; ++i) {
      seconds[i] = new ClockNumber(this, x, 1);
      root.appendChild(seconds[i].root);
      Util.pos(seconds[i].root, x, 0);
      x += BALL_WIDTH * ClockNumber.WIDTH + NUMBER_SPACING;
    }
  }
}

