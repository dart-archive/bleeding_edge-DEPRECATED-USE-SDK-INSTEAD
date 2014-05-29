// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
library pop_pop_win.stage.square_element;

import 'package:stagexl/stagexl.dart';

import 'package:pop_pop_win/src/game.dart';
import 'board_element.dart';
import 'game_element.dart';

class SquareElement extends Sprite {
  static const int SIZE = 80;

  static const List<String> _balloonBits = const [
      'balloon_pieces_a',
      'balloon_pieces_b',
      'balloon_pieces_c',
      'balloon_pieces_d'
  ];

  static const List<String> _numberMap = const [
      "game_board_center",
      "number_one", "number_two",
      "number_three", "number_four",
      "number_five", "number_six",
      "number_seven", "number_eight"
  ];

  final int x, y;
  final Bitmap _bitmap = new Bitmap(new BitmapData(SIZE, SIZE, true,
      Color.Transparent));

  SquareElement(this.x, this.y) {
    addChild(_bitmap);

    onMouseClick.listen(_onClick);
    onMouseRightClick.listen(_onClick);

    useHandCursor = true;
  }

  void updateState() {
    var textureName;
    switch (squareState) {
      case SquareState.hidden:
        textureName = _getHiddenTexture();
        break;
      case SquareState.flagged:
        textureName = 'balloon_tagged_frozen';
        break;
      case SquareState.revealed:
        textureName = _numberMap[_adjacentCount];
        break;
      case SquareState.bomb:
        textureName = 'crater_b';
        break;
      case SquareState.safe:
        textureName = 'balloon_tagged_bomb';
        break;
    }

    useHandCursor = !_game.gameEnded && (squareState == SquareState.hidden ||
        squareState == SquareState.flagged);

    _bitmap.bitmapData
        ..clear()
        ..drawPixels(_opaqueAtlas.getBitmapData(textureName),
            new Rectangle(0, 0, SIZE, SIZE), new Point(0, 0));
  }

  void _onClick(MouseEvent e) {
    if (!_game.gameEnded) {
      bool alt = (e.type == MouseEvent.RIGHT_CLICK) || e.shiftKey;
      _gameElement.click(x, y, alt);
    }
  }

  String toString() => 'Square at [$x, $y]';

  String _getHiddenTexture() {
    assert(squareState == SquareState.hidden);
    if (_game.state == GameState.lost) {
      useHandCursor = false;
      final index = (x + y) % _balloonBits.length;
      return _balloonBits[index];
    } else {
      useHandCursor = true;
      return 'balloon';
    }
  }

  SquareState get squareState => _game.getSquareState(x, y);

  int get _adjacentCount => _game.field.getAdjacentCount(x, y);

  BoardElement get _board {
    final BoardElement p = this.parent;
    return p;
  }

  GameElement get _gameElement => _board.gameElement;

  TextureAtlas get _opaqueAtlas => _board.opaqueAtlas;

  Game get _game => _board.game;
}
