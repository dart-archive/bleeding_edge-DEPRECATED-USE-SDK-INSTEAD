part of ppw_canvas;

class GameTitleElement extends Thing {

  GameTitleElement() : super(318, 96);

  void drawOverride(CanvasRenderingContext2D ctx) {
    _textureData.drawTextureKeyAt(ctx, 'logo_win.png');
  }

  GameElement get _gameElement => (parent as CanvasThing).parent;

  TextureData get _textureData => _gameElement._textureData;

  Game get _game => _gameElement._game;

  void _mouseDirectlyOver(args) {
    invalidateDraw();
  }
}
