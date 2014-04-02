part of pop_pop_win.canvas;

class GameRoot extends GameManager {
  final Stage _stage;
  final CanvasElement _canvas;
  final GameElement _gameElement;
  final AffineTransform _gameElementTx;

  bool _frameRequested = false;

  factory GameRoot(int width, int height, int bombCount,
      CanvasElement canvasElement, TextureData textureData) {

    final rootElement = new GameElement(textureData);
    final stage = new Stage(canvasElement, rootElement);
    new MouseManager(stage);

    return new GameRoot._internal(width, height, bombCount,
        canvasElement, stage, rootElement);
  }

  GameRoot._internal(int width, int height, int bombCount,
      this._canvas, this._stage, GameElement gameElement) :
      this._gameElement = gameElement,
      _gameElementTx = gameElement.addTransform(),
      super(width, height, bombCount) {

    _gameElement.setGameManager(this);
    _stage.invalidated.listen(_stageInvalidated);

    _gameElement.newGameClick.listen((args) => newGame());

    MouseManager.getMouseMoveStream(_gameElement).listen(_mouseMoveHandler);
    MouseManager.getMouseOutStream(_stage).listen(_mouseOutHandler);

    window.onResize.listen((args) => _updateCanvasSize());
    _updateCanvasSize();
  }

  void newGame() {
    super.newGame();
    _gameElement.game = super.game;
    _requestFrame();
  }

  bool get canRevealTarget => _gameElement.canRevealTarget;

  bool get canFlagTarget => _gameElement.canFlagTarget;

  void revealTarget() => _gameElement.revealTarget();

  void toggleTargetFlag() => _gameElement.toggleTargetFlag();

  Stream get targetChanged => _gameElement.targetChanged;

  void onGameStateChanged(GameState newState) {
    switch (newState) {
      case GameState.won:
        GameAudio.win();
        break;
    }
  }

  void _updateCanvasSize() {
    final windowSize = new Size(window.innerWidth, window.innerHeight);

    _canvas.width = min(windowSize.width, _gameElement.width).toInt();
    _canvas.height = min(windowSize.height, _gameElement.height).toInt();

    // now move it!
    final delta = new Vector(windowSize.width - _canvas.width,
        windowSize.height - _canvas.height).scale(0.5);

    _canvas.style.left = "${delta.x.toInt()}px";

    // add a little padding at the top (up to 20) if there's room
    // but don't try to vertically center
    final topDelta = min(delta.y, 20).toInt();
    _canvas.style.top = "${topDelta}px";

    _requestFrame();
  }

  void _requestFrame() {
    if (!_frameRequested) {
      _frameRequested = true;
      window.requestAnimationFrame(_onFrame);
    }
  }

  void _onFrame(double time) {
    final boardInnerBox = _gameElement._scaledInnerBox;
    final xScale = _stage.size.width / boardInnerBox.width;
    final yScale = _stage.size.height / boardInnerBox.height;

    final prettyScale = min(1, min(xScale, yScale));

    final newDimensions = _gameElement.size * prettyScale;

    final delta = new Vector(_stage.size.width - newDimensions.width,
        min(40, _stage.size.height - newDimensions.height))
      .scale(0.5)
      .scale(1/prettyScale);

    _gameElementTx.setToScale(prettyScale, prettyScale);
    _gameElementTx.translate(delta.x, delta.y);

    var updated = _stage.draw();
    _frameRequested = false;
    if (updated) {
      _requestFrame();
    }
  }

  void updateClock() {
    _requestFrame();
    super.updateClock();
  }

  void gameUpdated(args) {
    _requestFrame();
  }

  void _stageInvalidated(args) {
    _requestFrame();
  }

  void _mouseMoveHandler(ThingMouseEventArgs args) {
    bool showPointer = false;
    if (!game.gameEnded && args.thing is SquareElement) {
      final SquareElement se = args.thing;
      showPointer = game.canReveal(se.x, se.y);
    } else if (args.thing is NewGameElement) {
      showPointer = true;
    } else if (args.thing is GameTitleElement) {
      showPointer = true;
    }
    _updateCursor(showPointer);
  }

  void _mouseOutHandler(args) {
    _updateCursor(false);
  }

  void _updateCursor(bool showFinger) {
    _canvas.style.cursor = showFinger ? 'pointer' : 'inherit';
  }
}
