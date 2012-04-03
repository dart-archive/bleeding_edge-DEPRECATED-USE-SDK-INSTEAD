// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class Colon {
  Element root;

  Colon(double pos) {
    root = new Element.tag('div');
    Util.abs(root);
    Util.pos(root, pos, 0.0);

    ImageElement dot = new Element.tag('img');
    root.nodes.add(dot);
    Util.abs(dot);
    Util.pos(dot, 0.0, 2.0 * CountDownClock.BALL_HEIGHT);
    dot.src = Balls.PNGS[4];

    dot = new Element.tag('img');
    root.nodes.add(dot);
    Util.abs(dot);
    Util.pos(dot, 0.0, 4.0 * CountDownClock.BALL_HEIGHT);
    dot.src = Balls.PNGS[4];
  }
}
