// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
library pop_pop_win.stage.board_element;

import 'package:bot/bot.dart' show Array2d;
import 'package:stagexl/stagexl.dart';

import 'package:pop_pop_win/src/game.dart';
import 'game_element.dart';
import 'square_element.dart';

class BoardElement extends Sprite {
  Array2d<SquareElement> _elements;

  BoardElement(GameElement gameElement) {
    addTo(gameElement);

    _elements = new Array2d<SquareElement>(game.field.width, game.field.height);

    num scaledSize = SquareElement.SIZE * _boardScale;
    for (int i = 0; i < _elements.length; i++) {
      var coords = _elements.getCoordinate(i);
      var se = new SquareElement(coords.item1, coords.item2)
          ..x = coords.item1 * scaledSize
          ..y = coords.item2 * scaledSize
          ..scaleX = _boardScale
          ..scaleY = _boardScale
          ..addTo(this);

      _elements[i] = se;
      se.updateState();
    }

  }

  GameElement get gameElement => parent;
  num get _boardScale => gameElement.boardScale;
  num get _boardSize => gameElement.boardSize;
  Array2d<SquareElement> get squares => _elements;
  Game get game => gameElement.game;
  Stage get _stage => gameElement.manager.stage;

  TextureAtlas get opaqueAtlas =>
      gameElement.resourceManager.getTextureAtlas('opaque');
}
