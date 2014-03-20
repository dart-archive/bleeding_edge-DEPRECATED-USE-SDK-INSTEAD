part of pop_pop_win.canvas;

// NOTE: setGameManager must be called immediately after construction

class ScoreElement extends Thing {
  static const _bombsLeftStr = "BOMBS LEFT:";
  static const _valueOffset = 15;

  GameManager _gameManager;

  String _clockStr, _bombsStr, _bestTimeStr;
  num _textSize;

  ScoreElement(): super(400, 96);

  void setGameManager(GameManager manager) {
    assert(_gameManager == null);
    assert(manager != null);
    _gameManager = manager;
    invalidateDraw();
  }

  void update() {
    final newBombStr = _game.bombsLeft.toString();
    if (newBombStr != _bombsStr) {
      _bombsStr = newBombStr;
      invalidateDraw();
    }

    var newClockStr = '';
    if (_game.duration != null) {
      newClockStr = toRecordString(_game.duration.inMilliseconds);
    }
    if (newClockStr != _clockStr) {
      _clockStr = newClockStr;
      invalidateDraw();
    }

    var bestTimeStr = null;
    _gameManager.bestTimeMilliseconds.then((int val) {
      if (val != null) {
        bestTimeStr = toRecordString(val);
      }
      if (_bestTimeStr != bestTimeStr) {
        _bestTimeStr = bestTimeStr;
        invalidateDraw();
      }
    });

    super.update();
  }


  void drawOverride(CanvasRenderingContext2D ctx) {
    final rowHeight = (0.33 * height);
    final fontHeight = (rowHeight * 0.9).toInt();
    ctx.font = '${fontHeight}px Slackey';
    ctx.textBaseline = 'middle';

    final textSize = _getTextSize(ctx);

    ctx.fillStyle = 'black';
    ctx.textAlign = 'right';
    ctx.fillText(_bombsLeftStr, textSize, 0.5 * rowHeight);
    ctx.textAlign = 'left';
    ctx.fillText(_bombsStr, textSize + _valueOffset, 0.5 * rowHeight);
    ctx.textAlign = 'right';
    ctx.fillText('TIME:', textSize, 1.5 * rowHeight);
    ctx.textAlign = 'left';
    ctx.fillText(_clockStr, textSize + _valueOffset, 1.5 * rowHeight);

    if (_bestTimeStr != null) {
      ctx.textAlign = 'right';
      ctx.fillText('RECORD:', textSize, 2.5 * rowHeight);
      ctx.textAlign = 'left';
      ctx.fillText(_bestTimeStr, textSize + _valueOffset, 2.5 * rowHeight);
    }
  }

  static String toRecordString(num milliseconds) {
    return (milliseconds * 0.001).toStringAsFixed(1);
  }

  num _getTextSize(CanvasRenderingContext2D ctx) {
    if (_textSize == null) {
      final mets = ctx.measureText(_bombsLeftStr);
      _textSize = mets.width;
    }
    return _textSize;
  }

  Game get _game => _gameManager.game;
}
