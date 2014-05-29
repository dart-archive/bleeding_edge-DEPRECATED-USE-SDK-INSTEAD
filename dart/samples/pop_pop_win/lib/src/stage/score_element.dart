// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
library pop_pop_win.stage.score_element;

import 'package:stagexl/stagexl.dart';

import 'package:pop_pop_win/src/game.dart';
import 'game_element.dart';

class ScoreElement extends TextField implements Animatable {

  int bestTime;

  ScoreElement(this.bestTime) {
    defaultTextFormat = new TextFormat('Slackey, cursive', 28, Color.Black,
        leading: 1);
    autoSize = TextFieldAutoSize.LEFT;
    x = 1400;
    y = 20;
  }

  bool advanceTime(num time) {
      var time = (game.duration == null) ?
        '0' : (game.duration.inMilliseconds / 1000).toStringAsFixed(1);
      text = 'Bombs Left: ${game.bombsLeft}\nTime: $time';
      if (bestTime > 0) {
        text = text + '\nRecord: ${(bestTime/1000).toStringAsFixed(1)}';
      }
      return true;
  }

  Game get game => (parent as GameElement).manager.game;
}
