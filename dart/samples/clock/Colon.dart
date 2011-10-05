class Colon {
  Element root;

  Colon(int pos) {
    root = CountDownClock.window.document.createElement('div');
    Util.abs(root);
    Util.pos(root, pos, 0);

    HTMLImageElement dot = CountDownClock.window.document.createElement('img');
    root.appendChild(dot);
    Util.abs(dot);
    Util.pos(dot, 0, 2 * CountDownClock.BALL_HEIGHT);
    dot.src = Balls.PNGS[4];

    dot = CountDownClock.window.document.createElement('img');
    root.appendChild(dot);
    Util.abs(dot);
    Util.pos(dot, 0, 4 * CountDownClock.BALL_HEIGHT);
    dot.src = Balls.PNGS[4];
  }
}
