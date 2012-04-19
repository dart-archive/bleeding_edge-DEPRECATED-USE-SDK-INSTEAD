// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class Balls {
  static final double RADIUS2 = Ball.RADIUS * Ball.RADIUS;

  // TODO: "static const Array<String> PNGS" doesn't parse
  static final List<String> PNGS = const ["images/ball-d9d9d9.png",
      "images/ball-009a49.png", "images/ball-13acfa.png",
      "images/ball-265897.png", "images/ball-b6b4b5.png",
      "images/ball-c0000b.png", "images/ball-c9c9c9.png"];

  DivElement root;
  int lastTime;
  List<Ball> balls;

  Balls() :
      lastTime = Util.currentTimeMillis(),
      balls = new List<Ball>() {
    root = new Element.tag('div');
    document.body.nodes.add(root);
    //root.style.zIndex = 100;
    Util.abs(root);
    Util.posSize(root, 0.0, 0.0, 0.0, 0.0);
  }

  void tick() {
    int now = Util.currentTimeMillis();
    double delta = Math.min((now - lastTime) / 1000.0, 0.1);
    lastTime = now;
    // incrementally move each ball, removing balls that are offscreen
    balls = balls.filter((ball) => ball.tick(delta));
    collideBalls(delta);
  }

  void collideBalls(double delta) {
    // TODO: Make this nasty O(n^2) stuff better.
    balls.forEach((b0) {
      balls.forEach((b1) {

        // See if the two balls are intersecting.
        double dx = (b0.x - b1.x).abs();
        double dy = (b0.y - b1.y).abs();
        double d2 = dx * dx + dy * dy;
        if (d2 < RADIUS2) {
          // Make sure they're actually on a collision path
          // (not intersecting while moving apart).
          // This keeps balls that end up intersecting from getting stuck
          // without all the complexity of keeping them strictly separated.
          if (newDistanceSquared(delta, b0, b1) > d2) {
            return;
          }

          // They've collided. Normalize the collision vector.
          double d = Math.sqrt(d2);
          if (d == 0) {
            // TODO: move balls apart.
            return;
          }
          dx /= d;
          dy /= d;

          // Calculate the impact velocity and speed along the collision vector.
          double impactx = b0.vx - b1.vx;
          double impacty = b0.vy - b1.vy;
          double impactSpeed = impactx * dx + impacty * dy;

          // Bump.
          b0.vx -= dx * impactSpeed;
          b0.vy -= dy * impactSpeed;
          b1.vx += dx * impactSpeed;
          b1.vy += dy * impactSpeed;
        }
      });
    });
  }

  double newDistanceSquared(double delta, Ball b0, Ball b1) {
    double nb0x = b0.x + b0.vx * delta;
    double nb0y = b0.y + b0.vy * delta;
    double nb1x = b1.x + b1.vx * delta;
    double nb1y = b1.y + b1.vy * delta;
    double ndx = (nb0x - nb1x).abs();
    double ndy = (nb0y - nb1y).abs();
    double nd2 = ndx * ndx + ndy * ndy;
    return nd2;
  }

  void add(double x, double y, int color) {
    balls.add(new Ball(root, x, y, color));
  }
}
