// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class Ball {
  static final double GRAVITY = 400.0;
  static final double RESTITUTION = 0.8;
  static final double MIN_VELOCITY = 100.0;
  static final double INIT_VELOCITY = 800.0;
  static final double RADIUS = 14.0;
  static final double BORDER = 55.0;

  static double randomVelocity() {
    return (Math.random() - 0.5) * INIT_VELOCITY;
  }

  Element root;
  HTMLImageElement elem;
  int x, y;
  double vx, vy;
  double ax, ay;
  double age;

  Ball(Element root, int x, int y, int color) {
    this.root = root;
    this.elem = window.document.createElement('img');
    elem.src = Balls.PNGS[color];
    Util.abs(elem);
    Util.pos(elem, x, y);
    root.appendChild(elem);

    ax = 0.0;
    ay = GRAVITY;

    vx = randomVelocity();
    vy = randomVelocity();

    this.x = x;
    this.y = y;
  }

  // return false => remove me
  bool tick(double delta) {
    // Update velocity and position.
    vx += ax * delta;
    vy += ay * delta;

    x += vx * delta;
    y += vy * delta;

    // Handle falling off the edge.
    if ((x < RADIUS) || (x > Util.clientWidth())) {
      root.removeChild(elem);
      return false;
    }

    // Handle ground collisions.
    if (y >= Util.clientHeight() - RADIUS - BORDER) {
      y = Util.clientHeight() - RADIUS - BORDER;
      vy *= -RESTITUTION;
    }

    // Position the element.
    Util.pos(elem, x - RADIUS, y - RADIUS);
    return true;
  }
}
