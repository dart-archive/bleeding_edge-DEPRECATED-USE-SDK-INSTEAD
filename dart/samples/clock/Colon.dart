// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class Colon {
  Element root;

  Colon(int pos) {
    root = window.document.createElement('div');
    Util.abs(root);
    Util.pos(root, pos, 0);

    HTMLImageElement dot = window.document.createElement('img');
    root.appendChild(dot);
    Util.abs(dot);
    Util.pos(dot, 0, 2 * CountDownClock.BALL_HEIGHT);
    dot.src = Balls.PNGS[4];

    dot = window.document.createElement('img');
    root.appendChild(dot);
    Util.abs(dot);
    Util.pos(dot, 0, 4 * CountDownClock.BALL_HEIGHT);
    dot.src = Balls.PNGS[4];
  }
}
