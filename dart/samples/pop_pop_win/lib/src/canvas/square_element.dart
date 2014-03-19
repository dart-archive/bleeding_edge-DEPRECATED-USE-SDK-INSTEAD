part of ppw_canvas;

class SquareElement extends Thing {
  static const int _size = 80;

  static const List<String> _balloonBits = const['balloon_pieces_a.png',
                                                 'balloon_pieces_b.png',
                                                 'balloon_pieces_c.png',
                                                 'balloon_pieces_d.png'];

  static const List<String> _numberMap = const["game_board_center",
                                               "number_one", "number_two",
                                               "number_three", "number_four",
                                               "number_five", "number_six",
                                               "number_seven", "number_eight"];

  final int x, y;

  SquareState _lastDrawingState;

  SquareElement(this.x, this.y): super(_size, _size) {
    MouseManager.setClickable(this, true);
  }

  void update() {
    if (_lastDrawingState != _squareState) {
      _lastDrawingState = _squareState;
      invalidateDraw();
    }
  }

  void drawOverride(CanvasRenderingContext2D ctx) {
    var textureName;
    switch (_lastDrawingState) {
      case SquareState.hidden:
        textureName = _getHiddenTexture();
        break;
      case SquareState.flagged:
        textureName = 'balloon_tagged_frozen.png';
        break;
      case SquareState.revealed:
        final prefix = _numberMap[_adjacentCount];
        textureName = '$prefix.png';
        break;
      case SquareState.bomb:
        textureName = 'crater_b.png';
        break;
      case SquareState.safe:
        textureName = 'balloon_tagged_bomb.png';
        break;
    }

    _textureData.drawTextureKeyAt(ctx, textureName);
  }

  String toString() => 'Square at [$x, $y]';

  String _getHiddenTexture() {
    assert(_lastDrawingState == SquareState.hidden);
    if (_game.state == GameState.lost) {
      final index = (x + y) % _balloonBits.length;
      return _balloonBits[index];
    } else {
      return 'balloon.png';
    }
  }

  SquareState get _squareState => _game.getSquareState(x, y);

  int get _adjacentCount => _game.field.getAdjacentCount(x, y);

  BoardElement get _board {
    final BoardElement p = this.parent;
    return p;
  }

  TextureData get _textureData => _board._textureData;

  Game get _game => _board._game;
}
