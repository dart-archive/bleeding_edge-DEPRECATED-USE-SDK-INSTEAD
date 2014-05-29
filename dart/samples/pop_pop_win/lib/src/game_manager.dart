// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
library pop_pop_win.game_manager;

import 'dart:async';

import 'game_storage.dart';
import 'game.dart';

abstract class GameManager {
  final int _width, _height, _bombCount;
  final GameStorage _gameStorage = new GameStorage();

  Game _game;
  StreamSubscription _updatedEventId;
  StreamSubscription _gameStateChangedId;
  Timer _clockTimer;

  GameManager(this._width, this._height, this._bombCount) {
    newGame();
  }

  Game get game => _game;

  Stream get bestTimeUpdated => _gameStorage.bestTimeUpdated;

  Future<int> get bestTimeMilliseconds =>
      _gameStorage.getBestTimeMilliseconds(_width, _height, _bombCount);

  void newGame() {
    if (_updatedEventId != null) {
      assert(_game != null);
      assert(_gameStateChangedId != null);
      _updatedEventId.cancel();
      _gameStateChangedId.cancel();
      _gameStateChanged(GameState.reset);
    }
    final f = new Field(_bombCount, _width, _height);
    _game = new Game(f);
    _updatedEventId = _game.updated.listen((_) => gameUpdated());
    _gameStateChangedId = _game.stateChanged.listen(_gameStateChanged);
  }

  void gameUpdated() {}

  void resetScores() {
    _gameStorage.reset();
  }

  void _click(int x, int y, bool alt) {
    final ss = _game.getSquareState(x, y);

    if (alt) {
      if (ss == SquareState.hidden) {
        _game.setFlag(x, y, true);
      } else if (ss == SquareState.flagged) {
        _game.setFlag(x, y, false);
      } else if (ss == SquareState.revealed) {
        _game.reveal(x, y);
      }
    } else {
      if (ss == SquareState.hidden) {
        _game.reveal(x, y);
      }
    }
  }

  void updateClock() {
    if (_clockTimer == null && _game.state == GameState.started) {
      _clockTimer = new Timer(const Duration(seconds: 1), updateClock);
    } else if (_clockTimer != null && _game.state != GameState.started) {
      _clockTimer.cancel();
      _clockTimer = null;
    }
  }

  void onNewBestTime(int value) {}

  void onGameStateChanged(GameState value) {}

  bool get _canClick {
    return _game.state == GameState.reset || _game.state == GameState.started;
  }

  void _gameStateChanged(GameState newState) {
    _gameStorage.recordState(newState);
    if (newState == GameState.won) {
      _gameStorage.updateBestTime(_game).then((bool newBestTime) {
        if (newBestTime) {
          bestTimeMilliseconds.then((int val) {
            onNewBestTime(val);
          });
        }
      });
    }
    updateClock();
    onGameStateChanged(newState);
  }
}
