part of ppw_canvas;

class GameBackgroundElement extends Thing {
  GameBackgroundElement(): super(0, 0) {
    cacheEnabled = true;
  }

  void update() {
    if (size != this._gameElement.size) {
      size = this._gameElement.size;
    }
  }

  void drawOverride(CanvasRenderingContext2D ctx) {
    final rightBgLoc = SquareElement._size * (_gameElement._game.field.width -1) +
        GameElement._edgeOffset;
    final bottomBgLoc = SquareElement._size * (_gameElement._game.field.height -1) +
        GameElement._edgeOffset;

    ctx.save();
    ctx.translate(_gameElement._scaledBoardOffset.x, _gameElement._scaledBoardOffset.y);

    _textureData.drawTextureKeyAt(ctx, 'game_board_corner_top_left.png');

    _textureData.drawTextureKeyAt(ctx, 'game_board_corner_top_right.png',
        new Coordinate(rightBgLoc, 0));

    _textureData.drawTextureKeyAt(ctx, 'game_board_corner_bottom_left.png',
        new Coordinate(0, bottomBgLoc));
    _textureData.drawTextureKeyAt(ctx, 'game_board_corner_bottom_right.png',
        new Coordinate(rightBgLoc, bottomBgLoc));

    for (var i = 1; i < _gameElement._game.field.width - 1; i++) {
      final xLoc = SquareElement._size * i + GameElement._edgeOffset;
      _textureData.drawTextureKeyAt(ctx, 'game_board_side_top.png',
          new Coordinate(xLoc, 0));
      _textureData.drawTextureKeyAt(ctx, 'game_board_side_bottom.png',
          new Coordinate(xLoc, bottomBgLoc));
    }

    for (var i = 1; i < _gameElement._game.field.height - 1; i++) {
      final yLoc = SquareElement._size * i + GameElement._edgeOffset;
      _textureData.drawTextureKeyAt(ctx, 'game_board_side_left.png',
          new Coordinate(0, yLoc));
      _textureData.drawTextureKeyAt(ctx, 'game_board_side_right.png',
          new Coordinate(rightBgLoc, yLoc));
    }

    ctx.restore();

    //
    // start drawing corners
    //

    ctx.save();
    // top left
    ctx.transform(_gameElement._scale, 0, 0, _gameElement._scale, 0, 0);
    _drawCorner(ctx);

    // right flip
    ctx.save();
    ctx.transform(-1, 0, 0, 1, GameElement._backgroundSize.width, 0);
    _drawCorner(ctx);

    // nested bottom, right flip
    ctx.transform(1, 0, 0, -1, 0, GameElement._backgroundSize.height);
    _drawCorner(ctx);

    ctx.restore();

    // bottom left
    ctx.transform(1, 0, 0, -1, 0, GameElement._backgroundSize.height);
    _drawCorner(ctx);

    ctx.restore();

    //
    // end drawing corners
    //
  }

  void _drawCorner(CanvasRenderingContext2D ctx) {
    _textureData.drawTextureKeyAt(ctx, 'background_top_left.png');
    _textureData.drawTextureKeyAt(ctx, 'background_side_left.png',
        new Coordinate(0, GameElement._boardOffset.y));
  }

  GameElement get _gameElement => (parent as CanvasThing).parent;

  TextureData get _textureData => _gameElement._textureData;
}
