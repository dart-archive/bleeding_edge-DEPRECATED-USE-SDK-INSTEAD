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
  ImageElement elem;
  double x, y;
  double vx, vy;
  double ax, ay;
  double age;

  Ball(Element this.root, double this.x, double this.y, int color) {
    elem = new Element.tag('img');
    elem.src = Balls.PNGS[color];
    Util.abs(elem);
    Util.pos(elem, x.toDouble(), y.toDouble());
    root.nodes.add(elem);

    ax = 0.0;
    ay = GRAVITY;

    vx = randomVelocity();
    vy = randomVelocity();
  }

  // return false => remove me
  bool tick(double delta) {
    // Update velocity and position.
    vx += ax * delta;
    vy += ay * delta;

    x += (vx * delta).toInt();
    y += (vy * delta).toInt();

    // Handle falling off the edge.
    if ((x < RADIUS) || (x > Util.clientWidth())) {
      elem.remove();
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
