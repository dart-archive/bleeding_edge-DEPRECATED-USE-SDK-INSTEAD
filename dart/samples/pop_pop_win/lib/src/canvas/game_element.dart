part of pop_pop_win.canvas;

class GameElement extends ParentThing {
  static const _edgeOffset = 32;
  static const _backgroundSize = const Size(2048, 1536);
  static const _backgroundEdgeOffset = 256;
  static const _backgroundHoleSize = 16 * SquareElement._size + 2 * _edgeOffset;
  static const _boardOffset = const Vector(352, 96);
  static const _popExplodeAnimationOffset = const Vector(-88, -88);
  static const _popAnimationHitFrame = 12;

  static const _dartAnimationOffset =
      const Vector(-512 + 0.5 * SquareElement._size,
          -388 + 0.5 * SquareElement._size);

  final CanvasThing _canvas = new CanvasThing(0, 0);
  final GameBackgroundElement _background = new GameBackgroundElement();
  final BoardElement _boardElement = new BoardElement();
  final ScoreElement _scoreElement;
  final NewGameElement _newGameElement = new NewGameElement();
  final GameTitleElement _titleElement = new GameTitleElement();
  final TextureAnimationThing _popAnimationLayer, _dartAnimationLayer;
  final TextureData _textureData;
  final EventHandle _targetChanged = new EventHandle();

  int _targetX, _targetY;
  double _scale;
  Vector _scaledBoardOffset;
  Box _scaledInnerBox;
  SquareElement _mouseDownElement, _lastHoldUnfreeze;
  Timer _mouseDownTimer;

  Game _game;

  GameElement(TextureData textureData)
      : _textureData = textureData,
        _popAnimationLayer = new TextureAnimationThing(0, 0, textureData),
        _dartAnimationLayer = new TextureAnimationThing(0, 0, textureData),
        _scoreElement = new ScoreElement(),
        super(100, 100) {
    _canvas.registerParent(this);
    _canvas.add(_background);
    _canvas.add(_boardElement);
    _canvas.add(_newGameElement);
    _canvas.add(_scoreElement);
    _canvas.add(_popAnimationLayer);
    _canvas.add(_titleElement);
    _canvas.add(_dartAnimationLayer);

    _newGameElement.clicked.listen((args) => GameAudio.click());

    MouseManager.setClickable(_titleElement, true);
    MouseManager.getClickStream(_titleElement).listen((args) =>
        _titleClickedEventHandle.add(EventArgs.empty));
  }

  Stream<EventArgs> get newGameClick => _newGameElement.clicked;

  Game get game => _game;

  void set game(Game value) {
    _game = value;
    if (value == null) {
      size = const Size(100, 100);
    } else {
      _updateSize(value.field.width, value.field.height);
    }
  }

  bool get canRevealTarget =>
      _targetX != null && _game.canReveal(_targetX, _targetY);

  bool get canFlagTarget =>
      _targetX != null && _game.canToggleFlag(_targetX, _targetY);

  void setGameManager(GameManager manager) {
    _scoreElement.setGameManager(manager);
  }

  void revealTarget() {
    if (_targetX != null) {
      game.reveal(_targetX, _targetY);
      _target(null, null);
    }
  }

  void toggleTargetFlag() {
    if (_targetX != null) {
      final success = _toggleFlag(_targetX, _targetY);
      if (success) {
        _target(null, null);
      }
    }
  }

  Stream get targetChanged => _targetChanged.stream;

  int get visualChildCount => 1;

  Thing getVisualChild(int index) {
    assert(index == 0);
    return _canvas;
  }

  void update() {
    super.update();
    final offset = _scaledBoardOffset +
        const Coordinate(_edgeOffset, _edgeOffset);

    _canvas.setTopLeft(_boardElement, offset);
    _canvas.setTopLeft(_popAnimationLayer, offset);
    _canvas.setTopLeft(_dartAnimationLayer, offset);

    // score offset
    // end of the board - score width
    final x = _scale * (40 + _backgroundSize.width - _boardOffset.x - _scoreElement.width);

    _canvas.setTopLeft(_scoreElement, new Coordinate(x, 0));
    _canvas.getChildTransform(_scoreElement).scale(_scale, _scale);

    final newGameTopLeft = new Coordinate(
        (_boardOffset.x + _newGameElement.width * 0.2) * _scale, 0);
    _canvas.setTopLeft(_newGameElement, newGameTopLeft);
    _canvas.getChildTransform(_newGameElement).scale(_scale, _scale);

    final titleMultiplier = 1.7;
    final titleTopLeft = new Coordinate(_scale * 0.5 * (_backgroundSize.width -
        _titleElement.width * titleMultiplier), 0);
    _canvas.setTopLeft(_titleElement, titleTopLeft);
    _canvas.getChildTransform(_titleElement)
      .scale(titleMultiplier * _scale, titleMultiplier * _scale);
  }

  void drawOverride(CanvasRenderingContext2D ctx) {
    // draw children via super
    super.drawOverride(ctx);

    // draw target element
    _drawTarget(ctx);
  }

  void _drawTarget(CanvasRenderingContext2D ctx) {
    assert((_targetX == null) == (_targetY == null));
    if (_targetX != null) {
      final halfSize = SquareElement._size * 0.5;
      var targetLoc = new Vector(_targetX, _targetY);
      targetLoc = targetLoc.scale(SquareElement._size);

      ctx.fillStyle = 'rgba(255, 0, 0, 0.5)';
      CanvasUtil.centeredCircle(ctx,
          targetLoc.x + halfSize, targetLoc.y + halfSize, halfSize);
      ctx.fill();
    }
  }

  void _startPopAnimation(Coordinate start, [Iterable<Coordinate> reveals = null]) {
    if(reveals == null) {
      assert(game.state == GameState.lost);
      reveals = new NumberEnumerable.fromRange(0, game.field.length)
          .map((i) {
            final t = game.field.getCoordinate(i);
            final c = new Coordinate(t.item1, t.item2);
            return new Tuple(c, game.getSquareState(c.x, c.y));
          })
          .where((t2) => t2.item2 == SquareState.bomb || t2.item2 == SquareState.hidden)
          .map((t2) => t2.item1)
          .toList();
    }

    final values = reveals.map((c) {
      final initialOffset = new Vector(SquareElement._size * c.x,
          SquareElement._size * c.y);
      final squareOffset = _popExplodeAnimationOffset + initialOffset;

      var delay = _popAnimationHitFrame + ((c - start).magnitude * 4).toInt();
      delay += rnd.nextInt(10);

      return [c, initialOffset, squareOffset, delay];
    }).toList();

    values.sort((a, b) {
      final int da = a[3];
      final int db = b[3];
      return da.compareTo(db);
    });

    for (final v in values) {
      final Coordinate c = v[0];
      final Vector initialOffset = v[1];
      final Vector squareOffset = v[2];
      final int delay = v[3];

      final ss = game.getSquareState(c.x, c.y);

      String texturePrefix;
      int frameCount;

      switch (ss) {
        case SquareState.revealed:
        case SquareState.hidden:
          texturePrefix = 'balloon_pop';
          frameCount = 28;
          break;
        case SquareState.bomb:
          texturePrefix = 'balloon_explode';
          frameCount = 24;
          break;
        default:
          throw 'not supported';
      }

      final request = new TextureAnimationRequest(texturePrefix, frameCount, squareOffset,
          delay: delay, initialFrame: 'balloon.png', initialFrameOffset: initialOffset);

      switch (ss) {
        case SquareState.revealed:
        case SquareState.hidden:
          request.started.listen((args) => GameAudio.pop());
          break;
        case SquareState.bomb:
          request.started.listen((args) => GameAudio.bomb());
          break;
      }

      _popAnimationLayer.add(request);
    }
  }

  void _startDartAnimation(List<Coordinate> points) {
    assert(points.length >= 1);
    GameAudio.throwDart();
    for(final point in points) {
      final squareOffset = _dartAnimationOffset +
          new Vector(SquareElement._size * point.x, SquareElement._size * point.y);

      _dartAnimationLayer.add(new TextureAnimationRequest('dart_fly_shadow', 54, squareOffset));
      _dartAnimationLayer.add(new TextureAnimationRequest('dart_fly', 54, squareOffset));
    }
  }

  void wireSquareMouseEvent(Thing square) {
    MouseManager.getClickStream(square).listen(_squareClicked);
    MouseManager.getMouseDownStream(square).listen(_squareMouseDown);
    MouseManager.getMouseUpStream(square).listen(_squareMouseUp);
    MouseManager.getMouseMoveStream(square).listen(_squareMouseMove);
  }

  void _squareClicked(ThingMouseEventArgs args) {
    if (!_game.gameEnded && _lastHoldUnfreeze == null) {
      final SquareElement se = args.thing;
      _click(se.x, se.y, args.shiftKey);
    }
  }

  void _squareMouseDown(ThingMouseEventArgs args) {
    _lastHoldUnfreeze = null;
    if (_mouseDownTimer != null) {
      _mouseDownTimer.cancel();
    }
    final SquareElement se = args.thing;
    _mouseDownElement = se;
    _mouseDownTimer = new Timer(const Duration(seconds: 1), _mouseDownTimeoutHandle);
  }

  void _squareMouseMove(ThingMouseEventArgs args) {
    final SquareElement se = args.thing;
    if (_mouseDownElement != se) {
      _clearTimeout();
    }
  }

  void _squareMouseUp(ThingMouseEventArgs args) {
    final SquareElement se = args.thing;
    _clearTimeout();
  }

  void _mouseDownTimeoutHandle() {
    assert(_mouseDownTimer != null);
    assert(_mouseDownElement != null);
    _click(_mouseDownElement.x, _mouseDownElement.y, true);
    _lastHoldUnfreeze = _mouseDownElement;
    _clearTimeout();
  }

  void _clearTimeout() {
    if (_mouseDownTimer != null) {
      _mouseDownTimer.cancel();
      _mouseDownTimer = null;
    }
    _mouseDownElement = null;
  }

  void _target(int x, int y) {
    _targetX = x;
    _targetY = y;
    _targetChanged.add(null);
    invalidateDraw();
  }

  bool _toggleFlag(int x, int y) {
    assert(!game.gameEnded);
    final ss = game.getSquareState(x, y);
    if (ss == SquareState.hidden) {
      game.setFlag(x, y, true);
      GameAudio.flag();
      return true;
    } else if (ss == SquareState.flagged) {
      game.setFlag(x, y, false);
      GameAudio.unflag();
      return true;
    }
    return false;
  }

  void _click(int x, int y, bool alt) {
    assert(!game.gameEnded);
    final ss = game.getSquareState(x, y);

    List<Coordinate> reveals = null;

    if (alt) {
      if (ss == SquareState.hidden || ss == SquareState.flagged) {
        _toggleFlag(x, y);
      } else if (ss == SquareState.revealed) {
        if (game.canReveal(x, y)) {
          // get adjacent ballons
          final adjHidden = game.field.getAdjacentIndices(x, y)
              .map((i) {
                final t = game.field.getCoordinate(i);
                return new Coordinate(t.item1, t.item2);
              })
              .where((t) => game.getSquareState(t.x, t.y) == SquareState.hidden)
              .toList();

          assert(adjHidden.length > 0);

          _startDartAnimation(adjHidden);
          reveals = game.reveal(x, y);
        }
      }
    } else {
      if (ss == SquareState.hidden) {
        _startDartAnimation([new Coordinate(x, y)]);
        reveals = game.reveal(x, y);
      }
    }

    if (reveals != null && reveals.length > 0) {
      assert(game.state != GameState.lost);
      if (!alt) {
        // if it was a normal click, the first item should be the clicked item
        var first = reveals[0];
        assert(first.x == x);
        assert(first.y == y);
      }
      _startPopAnimation(new Coordinate(x, y), reveals);
    } else if (game.state == GameState.lost) {
      _startPopAnimation(new Coordinate(x, y));
    }
  }

  void _updateSize(int w, int h) {
    final sizeX = _getScale(w, _backgroundSize.width, _backgroundHoleSize);
    final sizeY = _getScale(h, _backgroundSize.height, _backgroundHoleSize);

    _canvas.size = size = new Size(sizeX, sizeY);

    // NOTE: width wins here. Need to do work to make left and right sides
    //       scale nicely when not a square
    _scale = sizeX / _backgroundSize.width;
    _scaledBoardOffset = _boardOffset.scale(_scale);

    _scaledInnerBox = new Box(_backgroundEdgeOffset * _scale, 0,
        sizeX - 2 * _backgroundEdgeOffset * _scale, sizeY);
  }

  static num _getScale(int count, num fullSize, num holeSize) {
    final k = count * SquareElement._size + 2 * _edgeOffset;

    return k * fullSize / holeSize;
  }
}
