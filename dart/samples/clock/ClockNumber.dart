// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class ClockNumber {
  static final int WIDTH = 4;
  static final int HEIGHT = 7;

  CountDownClock app;
  Element root;
  List<List<ImageElement>> imgs;
  List<List<int>> pixels;
  int ballColor;

  ClockNumber(CountDownClock this.app, double pos, int this.ballColor) {
    imgs = new List<List<ImageElement>>(HEIGHT);

    root = new Element.tag('div');
    Util.abs(root);
    Util.pos(root, pos, 0.0);

    // HACK(jgw): Need a better way to initialize multi-dimensional arrays.
    for (int y = 0; y < HEIGHT; ++y) {
      imgs[y] = new List<ImageElement>(WIDTH);
    }

    for (int y = 0; y < HEIGHT; ++y) {
      for (int x = 0; x < WIDTH; ++x) {
        imgs[y][x] = new Element.tag('img');
        root.nodes.add(imgs[y][x]);
        Util.abs(imgs[y][x]);
        Util.pos(imgs[y][x], x * CountDownClock.BALL_WIDTH, y * CountDownClock.BALL_HEIGHT);
      }
    }
  }

  void setPixels(List<List<int>> px) {
    for (int y = 0; y < HEIGHT; ++y) {
      for (int x = 0; x < WIDTH; ++x) {
        ImageElement img = imgs[y][x];

        if (pixels != null) {
          if ((pixels[y][x] != 0) && (px[y][x] == 0)) {
            int absx = Util.bounds(img).left.toInt();
            int absy = Util.bounds(img).top.toInt();
            app.balls.add(absx.toDouble(), absy.toDouble(), ballColor);
          }
        }

        img.src = px[y][x] != 0 ? Balls.PNGS[ballColor] : Balls.PNGS[6];
      }
    }
    pixels = px;
  }
}
