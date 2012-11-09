// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library swipe;

import 'dart:html';
import 'dart:math';

Element target;

var figureWidth;

double anglePos = 0.0;

var timeoutHandle;

void main() {
  target = query('#target');

  initialize3D();

  // Handle touch events.
  int touchStartX;

  target.on.touchStart.add((TouchEvent event) {
    event.preventDefault();

    if (event.touches.length > 0) {
      touchStartX = event.touches[0].pageX;
    }
  });

  target.on.touchMove.add((TouchEvent event) {
    event.preventDefault();

    if (touchStartX != null && event.touches.length > 0) {
      int newTouchX = event.touches[0].pageX;

      if (newTouchX > touchStartX) {
        spinFigure(target, (newTouchX - touchStartX) ~/ 20 + 1);
        touchStartX = null;
      } else if (newTouchX < touchStartX) {
        spinFigure(target, (newTouchX - touchStartX) ~/ 20 - 1);
        touchStartX = null;
      }
    }
  });

  target.on.touchEnd.add((TouchEvent event) {
    event.preventDefault();

    touchStartX = null;
  });

  // Handle key events.
  document.on.keyDown.add((KeyboardEvent event) {
    switch (event.keyIdentifier) {
      case KeyName.LEFT:
        startSpin(target, -1);
        break;
      case KeyName.RIGHT:
        startSpin(target, 1);
        break;
    }
  });

  document.on.keyUp.add((event) => stopSpin());
}

void initialize3D() {
  target.classes.add("transformable");

  num childCount = target.elements.length;

  window.requestLayoutFrame(() {
    num width = query("#target").clientWidth;
    figureWidth = (width / 2) ~/ tan(PI / childCount);

    target.style.transform = "translateZ(-${figureWidth}px)";

    num radius = (figureWidth * 1.2).round();
    query('#container2').style.width = "${radius}px";

    for (int i = 0; i < childCount; i++) {
      var panel = target.elements[i];

      panel.classes.add("transformable");

      panel.style.transform =
          "rotateY(${i * (360 / childCount)}deg) translateZ(${radius}px)";
    }

    spinFigure(target, -1);
  });
}

void spinFigure(Element figure, int direction) {
  num childCount = target.elements.length;

  anglePos += (360.0 / childCount) * direction;

  figure.style.transform =
      'translateZ(-${figureWidth}px) rotateY(${anglePos}deg)';
}

/**
 * Start an indefinite spin in the given direction.
 */
void startSpin(Element figure, int direction) {
  // If we're not already spinning -
  if (timeoutHandle == null) {
    spinFigure(figure, direction);

    timeoutHandle = window.setInterval(
        () => spinFigure(figure, direction), 100);
  }
}

/**
 * Stop any spin that may be in progress.
 */
void stopSpin() {
  if (timeoutHandle != null) {
    window.clearInterval(timeoutHandle);
    timeoutHandle = null;
  }
}
