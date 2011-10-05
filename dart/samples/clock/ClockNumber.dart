class ClockNumber {
  static final int WIDTH = 4;
  static final int HEIGHT = 7;

  CountDownClock app;
  Element root;
  Array<Array<HTMLImageElement>> imgs;
  Array<Array<int>> pixels;
  int ballColor;

  ClockNumber(CountDownClock app, int pos, int ballColor) {
    this.imgs = new Array<Array<HTMLImageElement>>(HEIGHT);
    this.app = app;
    this.ballColor = ballColor;

    root = CountDownClock.window.document.createElement('div');
    Util.abs(root);
    Util.pos(root, pos, 0);

    // HACK(jgw): Need a better way to initialize multi-dimensional arrays.
    for (int y = 0; y < HEIGHT; ++y) {
      imgs[y] = new Array<HTMLImageElement>(WIDTH);
    }

    for (int y = 0; y < HEIGHT; ++y) {
      for (int x = 0; x < WIDTH; ++x) {
        imgs[y][x] = CountDownClock.window.document.createElement('img');
        root.appendChild(imgs[y][x]);
        Util.abs(imgs[y][x]);
        Util.pos(imgs[y][x], x * CountDownClock.BALL_WIDTH, y * CountDownClock.BALL_HEIGHT);
      }
    }
  }

  void setPixels(Array<Array<int>> px) {
    for (int y = 0; y < HEIGHT; ++y) {
      for (int x = 0; x < WIDTH; ++x) {
        HTMLImageElement img = imgs[y][x];

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
