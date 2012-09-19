// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class Util {
  static void abs(Element elem) {
    elem.style.position = 'absolute';
  }

  static void rel(Element elem) {
    elem.style.position = 'relative';
  }

  static void pos(Element elem, double x, double y) {
    elem.style.left = "${x.toRadixString(10)}PX";
    elem.style.top = "${y.toRadixString(10)}PX";
  }

  static void posSize(Element elem, double l, double t, double r, double b) {
    pos(elem, l, t);
    elem.style.right = "${r.toRadixString(10)}PX";
    elem.style.bottom = "${b.toRadixString(10)}PX";
  }

  static int bounds(Element elem) {
    return elem.getBoundingClientRect();
  }

  static void opacity(Element elem, double opacity) {
    elem.style.opacity = opacity;
  }

  static double currentTimeMillis() {
    return new Date.now().millisecondsSinceEpoch.toDouble();
  }

  static double clientWidth() {
    return document.documentElement.clientWidth;
  }

  static double clientHeight() {
    return document.documentElement.clientHeight;
  }
}

class Numbers {
  static final PIXELS = [
     [
       [ 1, 1, 1, 1 ],
       [ 1, 0, 0, 1 ],
       [ 1, 0, 0, 1 ],
       [ 1, 0, 0, 1 ],
       [ 1, 0, 0, 1 ],
       [ 1, 0, 0, 1 ],
       [ 1, 1, 1, 1 ]
    ], [
       [ 0, 0, 0, 1 ],
       [ 0, 0, 0, 1 ],
       [ 0, 0, 0, 1 ],
       [ 0, 0, 0, 1 ],
       [ 0, 0, 0, 1 ],
       [ 0, 0, 0, 1 ],
       [ 0, 0, 0, 1 ]
    ], [
       [ 1, 1, 1, 1 ],
       [ 0, 0, 0, 1 ],
       [ 0, 0, 0, 1 ],
       [ 1, 1, 1, 1 ],
       [ 1, 0, 0, 0 ],
       [ 1, 0, 0, 0 ],
       [ 1, 1, 1, 1 ]
    ], [
       [ 1, 1, 1, 1 ],
       [ 0, 0, 0, 1 ],
       [ 0, 0, 0, 1 ],
       [ 1, 1, 1, 1 ],
       [ 0, 0, 0, 1 ],
       [ 0, 0, 0, 1 ],
       [ 1, 1, 1, 1 ]
    ], [
       [ 1, 0, 0, 1 ],
       [ 1, 0, 0, 1 ],
       [ 1, 0, 0, 1 ],
       [ 1, 1, 1, 1 ],
       [ 0, 0, 0, 1 ],
       [ 0, 0, 0, 1 ],
       [ 0, 0, 0, 1 ]
    ], [
       [ 1, 1, 1, 1 ],
       [ 1, 0, 0, 0 ],
       [ 1, 0, 0, 0 ],
       [ 1, 1, 1, 1 ],
       [ 0, 0, 0, 1 ],
       [ 0, 0, 0, 1 ],
       [ 1, 1, 1, 1 ]
    ], [
       [ 1, 1, 1, 1 ],
       [ 1, 0, 0, 0 ],
       [ 1, 0, 0, 0 ],
       [ 1, 1, 1, 1 ],
       [ 1, 0, 0, 1 ],
       [ 1, 0, 0, 1 ],
       [ 1, 1, 1, 1 ]
    ], [
       [ 1, 1, 1, 1 ],
       [ 0, 0, 0, 1 ],
       [ 0, 0, 0, 1 ],
       [ 0, 0, 0, 1 ],
       [ 0, 0, 0, 1 ],
       [ 0, 0, 0, 1 ],
       [ 0, 0, 0, 1 ]
    ], [
       [ 1, 1, 1, 1 ],
       [ 1, 0, 0, 1 ],
       [ 1, 0, 0, 1 ],
       [ 1, 1, 1, 1 ],
       [ 1, 0, 0, 1 ],
       [ 1, 0, 0, 1 ],
       [ 1, 1, 1, 1 ]
    ], [
       [ 1, 1, 1, 1 ],
       [ 1, 0, 0, 1 ],
       [ 1, 0, 0, 1 ],
       [ 1, 1, 1, 1 ],
       [ 0, 0, 0, 1 ],
       [ 0, 0, 0, 1 ],
       [ 1, 1, 1, 1 ]
    ]
  ];
}

class Ball {
  static final double GRAVITY = 400;
  static final double RESTITUTION = 0.8;
  static final double MIN_VELOCITY = 100;
  static final double INIT_VELOCITY = 800;
  static final double RADIUS = 14;
  static final double BORDER = 55;

  static double randomVelocity() {
    return (Math.random() - 0.5) * INIT_VELOCITY;
  }

  Element root;
  ImageElement elem;
  double x, y;
  double vx, vy;
  double ax, ay;
  double age;

  Ball(Element root, double x, double y, int color) {
    this.root = root;
    this.elem = document.createElement('img');
    elem.src = Balls.PNGS[color];
    Util.abs(elem);
    Util.pos(elem, x, y);
    root.appendChild(elem);

    ax = 0;
    ay = GRAVITY;

    vx = randomVelocity();
    vy = randomVelocity();

    this.x = x;
    this.y = y;
  }

  // return false => remove me
  boolean tick(double delta) {
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

class Balls {
  static final double RADIUS2 = Ball.RADIUS * Ball.RADIUS;

  // TODO: "static const Array<String> PNGS" doesn't parse
  static final Array PNGS = [
      "images/ball-d9d9d9.png", "images/ball-009a49.png", "images/ball-13acfa.png",
      "images/ball-265897.png", "images/ball-b6b4b5.png", "images/ball-c0000b.png",
      "images/ball-c9c9c9.png"
  ];

  Element root;
  double lastTime;
  var balls;

  Balls():
    lastTime(Util.currentTimeMillis()),
    balls(new Array<Ball>())
  {
    root = document.createElement('div');
    document.body.appendChild(root);
    root.style.zIndex = 100;
    Util.abs(root);
    Util.posSize(root, 0, 0, 0, 0);
  }

  void tick() {
    double now = Util.currentTimeMillis();
    double delta = (now - lastTime) / 1000;
    if (delta > 0.1) {
      delta = 0.1;
    }
    lastTime = now;

    for (int i = 0; i < balls.length; ++i) {
      Ball ball = balls[i];
      if (!ball.tick(delta)) {
        balls.splice(i, 1);
        --i;
      }
    }

    collideBalls(delta);
  }

  void collideBalls(double delta) {
    // TODO: Make this nasty O(n^2) stuff better.
    for (int i = 0; i < balls.length; ++i) {
      for (int j = i + 1; j < balls.length; ++j) {
        Ball b0 = balls[i];
        Ball b1 = balls[j];

        // See if the two balls are intersecting.
        double dx = Math.abs(b0.x - b1.x);
        double dy = Math.abs(b0.y - b1.y);
        double d2 = dx * dx + dy * dy;
        if (d2 < RADIUS2) {
          // Make sure they're actually on a collision path (not intersecting while moving apart).
          // This keeps balls that end up intersecting from getting stuck without all the
          // complexity of keeping them strictly separated.
          if (newDistanceSquared(delta, b0, b1) > d2) {
            continue;
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
      }
    }
  }

  double newDistanceSquared(double delta, Ball b0, Ball b1) {
    double nb0x = b0.x + b0.vx * delta;
    double nb0y = b0.y + b0.vy * delta;
    double nb1x = b1.x + b1.vx * delta;
    double nb1y = b1.y + b1.vy * delta;
    double ndx = Math.abs(nb0x - nb1x);
    double ndy = Math.abs(nb0y - nb1y);
    double nd2 = ndx * ndx + ndy * ndy;
    return nd2;
  }

  void add(double x, double y, int color) {
    balls.add(new Ball(root, x, y, color));
  }
}

class Number {
  static final int WIDTH = 4;
  static final int HEIGHT = 7;

  Clock app;
  Element root;
  Array<Array<ImageElement>> imgs;
  Array<Array<int>> pixels;
  int ballColor;

  Number(Clock app, int pos, int ballColor) {
    this.imgs = new Array<Array<ImageElement>>();
    this.app = app;
    this.ballColor = ballColor;

    root = document.createElement('div');
    Util.abs(root);
    Util.pos(root, pos, 0);

    // HACK(jgw): Need a better way to initialize multi-dimensional arrays.
    for (int y = 0; y < HEIGHT; ++y) {
      imgs[y] = new Array<ImageElement>();
    }

    for (int y = 0; y < HEIGHT; ++y) {
      for (int x = 0; x < WIDTH; ++x) {
        imgs[y][x] = document.createElement('img');
        root.appendChild(imgs[y][x]);
        Util.abs(imgs[y][x]);
        Util.pos(imgs[y][x], x * Clock.BALL_WIDTH, y * Clock.BALL_HEIGHT);
      }
    }
  }

  void setPixels(Array<Array<int>> px) {
    for (int y = 0; y < HEIGHT; ++y) {
      for (int x = 0; x < WIDTH; ++x) {
        ImageElement img = imgs[y][x];

        if (pixels != null) {
          if ((pixels[y][x] != 0) && (px[y][x] == 0)) {
            int absx = Util.bounds(img).left;
            int absy = Util.bounds(img).top;
            app.balls.add(absx, absy, ballColor);
          }
        }

        img.src = px[y][x] != 0 ? Balls.PNGS[ballColor] : Balls.PNGS[6];
      }
    }
    pixels = px;
  }
}

class Colon {
  Element root;

  Colon(int pos) {
    root = document.createElement('div');
    Util.abs(root);
    Util.pos(root, pos, 0);

    ImageElement dot = document.createElement('img');
    root.appendChild(dot);
    Util.abs(dot);
    Util.pos(dot, 0, 2 * Clock.BALL_HEIGHT);
    dot.src = Balls.PNGS[4];

    dot = document.createElement('img');
    root.appendChild(dot);
    Util.abs(dot);
    Util.pos(dot, 0, 4 * Clock.BALL_HEIGHT);
    dot.src = Balls.PNGS[4];
  }
}

class Clock {
  static void main() {
    new Clock();
  }

  static final int NUMBER_SPACING = 19;
  static final int BALL_WIDTH = 19;
  static final int BALL_HEIGHT = 19;

  Array<Number> hours;
  Array<Number> minutes;
  Array<Number> seconds;
  Balls balls;

  Clock():
    hours(new Array<Number>(2)),
    minutes(new Array<Number>(2)),
    seconds(new Array<Number>(2)),
    balls(new Balls())
  {
    createNumbers();
    updateTime();
    window.setInterval(function() {
      // TODO: is 'this' bound correctly yet?
      updateTime();
    }, 1000);

    balls.tick();
    window.setInterval(function() {
      // TODO: is 'this' bound correctly yet?
      this.balls.tick();
    }, 50);
  }

  void updateTime() {
    int time = Util.currentTimeMillis() / 1000;
    time %= 86400;
    setDigits(pad2(time / 3600), hours);
    time %= 3600;
    setDigits(pad2((time / 60)), minutes);
    time %= 60;
    setDigits(pad2(time), seconds);
  }

  void setDigits(String digits, Array<Number> numbers) {
    for (int i = 0; i < numbers.length; ++i) {
      int digit = digits.charAt(i)[0] - '0'[0];
      numbers[i].setPixels(Numbers.PIXELS[digit]);
    }
  }

  String pad3(int num) {
    if (num < 10) {
      return "00$num";
    }
    if (num < 100) {
      return "0$num";
    }
    return "$num";
  }

  String pad2(int num) {
    if (num < 10) {
      return "0$num";
    }
    return "$num";
  }

  void createNumbers() {
    DivElement root = document.createElement('div');
    Util.rel(root);
    root.style.textAlign = 'center';
    document.getElementById("canvas-content").appendChild(root);

    int x = 0;

    for (int i = 0; i < hours.length; ++i) {
      hours[i] = new Number(this, x, 2);
      root.appendChild(hours[i].root);
      Util.pos(hours[i].root, x, 0);
      x += BALL_WIDTH * Number.WIDTH + NUMBER_SPACING;
    }

    root.appendChild(new Colon(x).root);
    x += BALL_WIDTH + NUMBER_SPACING;

    for (int i = 0; i < minutes.length; ++i) {
      minutes[i] = new Number(this, x, 5);
      root.appendChild(minutes[i].root);
      Util.pos(minutes[i].root, x, 0);
      x += BALL_WIDTH * Number.WIDTH + NUMBER_SPACING;
    }

    root.appendChild(new Colon(x).root);
    x += BALL_WIDTH + NUMBER_SPACING;

    for (int i = 0; i < seconds.length; ++i) {
      seconds[i] = new Number(this, x, 1);
      root.appendChild(seconds[i].root);
      Util.pos(seconds[i].root, x, 0);
      x += BALL_WIDTH * Number.WIDTH + NUMBER_SPACING;
    }
  }
}
