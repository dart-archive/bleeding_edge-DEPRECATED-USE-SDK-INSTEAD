// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import('dart:html');

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
  static final double BALL_WIDTH = 19.0;
  static final double BALL_HEIGHT = 19.0;

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
    Date now = new Date.now();
    
    setDigits(pad2(now.hours), hours);
    setDigits(pad2(now.minutes), minutes);
    setDigits(pad2(now.seconds), seconds);
  }

  void setDigits(String digits, List<ClockNumber> numbers) {
    for (int i = 0; i < numbers.length; ++i) {
      int digit = digits.charCodeAt(i) - '0'.charCodeAt(0);
      numbers[i].setPixels(ClockNumbers.PIXELS[digit]);
    }
  }

  String pad3(int number) {
    if (number < 10) {
      return "00${number}";
    }
    if (number < 100) {
      return "0${number}";
    }
    return "${number}";
  }

  String pad2(int number) {
    if (number < 10) {
      return "0${number}";
    }
    return "${number}";
  }

  void createNumbers() {
    DivElement root = new Element.tag('div');
    Util.rel(root);
    root.style.textAlign = 'center';
    document.query("#canvas-content").nodes.add(root);

    double x = 0.0;

    for (int i = 0; i < hours.length; ++i) {
      hours[i] = new ClockNumber(this, x, 2);
      root.nodes.add(hours[i].root);
      Util.pos(hours[i].root, x.toDouble(), 0.0);
      x += BALL_WIDTH * ClockNumber.WIDTH + NUMBER_SPACING;
    }

    root.nodes.add(new Colon(x).root);
    x += BALL_WIDTH + NUMBER_SPACING;

    for (int i = 0; i < minutes.length; ++i) {
      minutes[i] = new ClockNumber(this, x, 5);
      root.nodes.add(minutes[i].root);
      Util.pos(minutes[i].root, x.toDouble(), 0.0);
      x += BALL_WIDTH * ClockNumber.WIDTH + NUMBER_SPACING;
    }

    root.nodes.add(new Colon(x).root);
    x += BALL_WIDTH + NUMBER_SPACING;

    for (int i = 0; i < seconds.length; ++i) {
      seconds[i] = new ClockNumber(this, x, 1);
      root.nodes.add(seconds[i].root);
      Util.pos(seconds[i].root, x.toDouble(), 0.0);
      x += BALL_WIDTH * ClockNumber.WIDTH + NUMBER_SPACING;
    }
  }
}

