part of ppw_canvas;

class NewGameElement extends Thing {
  final EventHandle<EventArgs> _clickedEvent =
      new EventHandle<EventArgs>();

  NewGameElement() : super(294, 92) {
    MouseManager.setClickable(this, true);
    MouseManager.getClickStream(this).listen((args) =>
        _clickedEvent.add(EventArgs.empty));
    Mouse.isMouseDirectlyOverProperty.getStream(this).listen(_mouseDirectlyOver);
  }

  Stream<EventArgs> get clicked => _clickedEvent.stream;

  void drawOverride(CanvasRenderingContext2D ctx) {
    final texture = Mouse.isMouseDirectlyOver(this) ?
        'button_new_game_clicked.png' : 'button_new_game.png';
    _textureData.drawTextureKeyAt(ctx, texture);
  }

  GameElement get _gameElement => (parent as CanvasThing).parent;

  TextureData get _textureData => _gameElement._textureData;

  Game get _game => _gameElement._game;

  void _mouseDirectlyOver(args) {
    invalidateDraw();
  }
}
