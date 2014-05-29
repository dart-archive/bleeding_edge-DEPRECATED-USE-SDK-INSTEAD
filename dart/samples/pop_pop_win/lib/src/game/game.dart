// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
part of pop_pop_win.game;

class Game {
  final Field field;
  final Array2d<SquareState> _states;
  final StreamController _updatedEvent = new StreamController();
  final StreamController<GameState> _gameStateEvent =
      new StreamController<GameState>();

  GameState _state;
  int _bombsLeft;
  int _revealsLeft;
  DateTime _startTime;
  DateTime _endTime;

  Game(Field field)
      : this.field = field,
        _state = GameState.reset,
        _states = new Array2d<SquareState>(field.width, field.height,
          SquareState.hidden) {
    assert(field != null);
    _bombsLeft = field.bombCount;
    _revealsLeft = field.length - field.bombCount;
  }

  int get bombsLeft => _bombsLeft;

  int get revealsLeft => _revealsLeft;

  GameState get state => _state;

  Stream get updated => _updatedEvent.stream;

  Stream get stateChanged => _gameStateEvent.stream;

  SquareState getSquareState(int x, int y) => _states.get(x, y);

  bool get gameEnded => _state == GameState.won || _state == GameState.lost;

  Duration get duration {
    if (_startTime == null) {
      assert(state == GameState.reset);
      return null;
    } else {
      assert((state == GameState.started) == (_endTime == null));
      final end = (_endTime == null) ? new DateTime.now() : _endTime;
      return end.difference(_startTime);
    }
  }

  bool canToggleFlag(int x, int y) {
    final currentSS = _states.get(x, y);
    return currentSS == SquareState.hidden || currentSS == SquareState.flagged;
  }

  void setFlag(int x, int y, bool value) {
    _ensureStarted();
    assert(value != null);

    final currentSS = _states.get(x, y);
    if (value) {
      require(currentSS == SquareState.hidden);
      _states.set(x, y, SquareState.flagged);
      _bombsLeft--;
    } else {
      require(currentSS == SquareState.flagged);
      _states.set(x, y, SquareState.hidden);
      _bombsLeft++;
    }
    _update();
  }

  bool canReveal(int x, int y) {
    final currentSS = _states.get(x, y);
    if (currentSS == SquareState.hidden) {
      return true;
    } else if (_canChord(x, y)) {
      return true;
    }
    return false;
  }

  List<Point> reveal(int x, int y) {
    _ensureStarted();
    require(canReveal(x, y), "Item cannot be revealed.");
    final currentSS = _states.get(x, y);

    List<Point> reveals;

    // normal reveal
    if (currentSS == SquareState.hidden) {
      if (field.get(x, y)) {
        _setLost();
        reveals = <Point>[];
      } else {
        reveals = _doReveal(x, y);
      }
    } else if (_canChord(x, y)) {
      reveals = _doChord(x, y);
    }
    _update();

    if (_state == GameState.lost) {
      return null;
    } else {
      return reveals;
    }
  }

  String toBoardString() {
    final buffer = new StringBuffer();
    for (var y = -2; y < field.height; y++) {
      if (y > -2) {
        buffer.write('\n');
      }
      for (var x = -2; x < field.width; x++) {
        var char = null;
        if (y == -2) {
          if (x == -2) {
            char = ' ';
          } else if (x == -1) {
            char = '|';
          } else {
            char = (x % 10).toString();
          }
        } else if (y == -1) {
          if (x == -1) {
            char = '+';
          } else {
            char = '-';
          }
        } else {
          if (x == -2) {
            char = (y % 10).toString();
          } else if (x == -1) {
            char = '|';
          } else {
            switch (getSquareState(x, y)) {
              case SquareState.flagged:
                char = '\u2611';
                break;
              case SquareState.revealed:
                var count = field.getAdjacentCount(x, y);
                char = count.toString();
                break;
              case SquareState.hidden:
                char = '?';
                break;
            }
          }
        }
        assert(char != null);
        buffer.write(char);
      }
    }
    return buffer.toString();
  }

  bool _canChord(int x, int y) {
    final currentSS = _states.get(x, y);
    if (currentSS == SquareState.revealed) {
      // might be a 'chord' reveal
      final adjCount = field.getAdjacentCount(x, y);
      if (adjCount > 0) {
        final adjHidden = _getAdjacentCount(x, y, SquareState.hidden);
        if (adjHidden > 0) {
          final adjFlags = _getAdjacentCount(x, y, SquareState.flagged);
          if (adjFlags == adjCount) {
            return true;
          }
        }
      }
    }
    return false;
  }

  List<Point> _doChord(int x, int y) {
    // this does not repeat a bunch of validations that have already happened
    // be careful
    final currentSS = _states.get(x, y);
    assert(currentSS == SquareState.revealed);

    final flagged = new List<int>();
    final hidden = new List<int>();
    final adjCount = field.getAdjacentCount(x, y);
    assert(adjCount > 0);

    bool failed = false;

    for (final i in field.getAdjacentIndices(x, y)) {
      if (_states[i] == SquareState.hidden) {
        hidden.add(i);
        if (field[i]) {
          failed = true;
        }
      } else if (_states[i] == SquareState.flagged) {
        flagged.add(i);
      }
    }

    // for now we assume counts have been checked
    assert(flagged.length == adjCount);

    var reveals = <Point>[];

    // if any of the hidden are bombs, we've failed
    if (failed) {
      _setLost();
    } else {
      for (final i in hidden) {
        final c = field.getCoordinate(i);
        if (canReveal(c.item1, c.item2)) {
          reveals.addAll(reveal(c.item1, c.item2));
        }
      }
    }

    return reveals;
  }

  List<Point> _doReveal(int x, int y) {
    assert(_states.get(x, y) == SquareState.hidden);
    _states.set(x, y, SquareState.revealed);
    _revealsLeft--;
    assert(_revealsLeft >= 0);
    var reveals = [new Point(x, y)];
    if (_revealsLeft == 0) {
      _setWon();
    } else if (field.getAdjacentCount(x, y) == 0) {
      for (final i in field.getAdjacentIndices(x, y)) {
        if (_states[i] == SquareState.hidden) {
          final c = field.getCoordinate(i);
          reveals.addAll(_doReveal(c.item1, c.item2));
          assert(state == GameState.started || state == GameState.won);
        }
      }
    }
    return reveals;
  }

  void _setWon() {
    assert(state == GameState.started);
    for (int i = 0; i < field.length; i++) {
      if (field[i]) {
        _states[i] = SquareState.safe;
      }
    }
    _setState(GameState.won);
  }

  void _setLost() {
    assert(state == GameState.started);
    for (int i = 0; i < field.length; i++) {
      if (field[i]) {
        _states[i] = SquareState.bomb;
      }
    }
    _setState(GameState.lost);
  }

  void _update() => _updatedEvent.add(null);

  void _setState(GameState value) {
    assert(value != null);
    assert(_state != null);
    assert((_state == GameState.reset) == (_startTime == null));
    if (_state != value) {
      _state = value;
      if (_state == GameState.started) {
        _startTime = new DateTime.now();
      } else if (gameEnded) {
        _endTime = new DateTime.now();
      }
      _gameStateEvent.add(_state);
    }
  }

  void _ensureStarted() {
    if (state == GameState.reset) {
      assert(_startTime == null);
      _setState(GameState.started);
    }
    assert(state == GameState.started);
    assert(_startTime != null);
  }

  int _getAdjacentCount(int x, int y, SquareState state) {
    int val = 0;
    for (final i in field.getAdjacentIndices(x, y)) {
      if (_states[i] == state) {
        val++;
      }
    }
    return val;
  }
}
