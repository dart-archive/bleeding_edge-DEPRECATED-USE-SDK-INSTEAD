// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
library pop_pop_win.stage.game_root;

import 'package:stagexl/stagexl.dart';

import 'package:pop_pop_win/src/audio.dart';
import 'package:pop_pop_win/src/game.dart';
import 'package:pop_pop_win/src/game_manager.dart';
import 'game_element.dart';

class GameRoot extends GameManager {
  final Stage stage;
  final ResourceManager resourceManager;
  GameElement _gameElement;

  GameRoot(int width, int height, int bombCount,
      this.stage, this.resourceManager) : super(width, height, bombCount) {

    var opa = resourceManager.getTextureAtlas('opaque');
    var sta = resourceManager.getTextureAtlas('static');

    _gameElement = new GameElement(this)
        ..alpha = 0;

    stage..addChild(_gameElement)
        ..juggler.tween(_gameElement, .5).animate.alpha.to(1);
  }

  void onGameStateChanged(GameState newState) {
    if (newState == GameState.won) {
      _gameElement.boardElement.squares.forEach((se) => se.updateState());
      if (game.duration.inMilliseconds < _gameElement.scoreElement.bestTime
          || _gameElement.scoreElement.bestTime == 0) {
        _gameElement.scoreElement.bestTime = game.duration.inMilliseconds;
      }
      GameAudio.win();
    }
  }

  void newGame() {
    super.newGame();
    if (_gameElement != null) {
      _gameElement.boardElement.squares.forEach((se) => se.updateState());
    }
  }
}
