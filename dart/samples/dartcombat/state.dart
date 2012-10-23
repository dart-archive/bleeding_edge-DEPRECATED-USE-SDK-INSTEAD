// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

void spawnPlayer() {
  PlayerState state = new PlayerState();
  port.receive(state.dispatch);
}

/**
 * Stores the actual data on a player's boat grid, the UI representation for its
 * grid and the status of each shot. Acts as a controller handling isolate
 * messages (from the main isolate message and shots from the enemy), and UI
 * events.
 */
// TODO(sigmund): move UI setup out of here, e.g. into a controller class.
class PlayerState {

  /** internal id for this player. */
  int _id;

  /** Set up of boats on the board. */
  BoatGrid boats;

  /** State of shots taken by the enemy on this player's board. */
  GridState localGrid;

  /** State of shots this player has taken on the enemy. */
  GridState enemyGrid;

  /** Total shots made. */
  int totalShots;

  /** Total hits. */
  int totalHits;

  /** Total misses. */
  int totalMisses;

  /** Total boats that we have sunk. */
  int boatsSunk;

  /** Interface to interact with the enemy. */
  Enemy enemy;

  /** UI representation of this player's grid. */
  PlayerGridView _ownView;

  /** UI representation of the enemy's grid. */
  EnemyGridView _enemyView;

  /** Port used for testing purposes. */
  SendPort _portForTest;

  /** dispatches all messages that are expected in this isolate. */
  void dispatch(var message, SendPort replyTo) {
    int action = message['action'];
    List args = message['args'];
    switch (action) {
      // message from the main program, setting this as player 1 or 2
      case MessageIds.SETUP:
        handleSetup(args[0]);
        break;
      // message from the main program, giving port to talk with other player
      case MessageIds.SET_ENEMY:
        enemy = new Enemy(args[0]);
        break;
      // message from the other player indicating it's ready to play.
      case MessageIds.ENEMY_IS_READY:
        _enemyView.setEnemyReady();
        replyTo.send([true], null);
        break;
      // message from the other player indicating a shot.
      case MessageIds.SHOOT:
        List res = handleShot(args[0], args[1]);
        replyTo.send([true, res], null);
        break;
      // message from the unit test (used to make tests deterministic)
      case MessageIds.SET_PORT_FOR_TEST:
        _portForTest = args[0];
        replyTo.send([true], null);
        break;
      default:
        break;
    }
  }

  /** Handles a message from the main isolate to setup this player. */
  void handleSetup(int id) {
    _id = id;
    boats = new BoatGrid();
    localGrid = new GridState();
    enemyGrid = new GridState();
    totalShots = 0;
    totalHits = 0;
    totalMisses = 0;
    boatsSunk = 0;

    _ownView = new PlayerGridView(this, document.query("#p${id}own"));
    _enemyView = new EnemyGridView(this, document.query("#p${id}enemy"));
    if (_portForTest != null) {
      _portForTest.call(["_TEST:handleSetup", id]);
    }
  }

  /** Handles a shot message from the enemy player. */
  List handleShot(int x, int y) {
    List res = boats.shoot(localGrid, x, y);
    switch (res[0]) {
      case Constants.MISS:
        _ownView.addMiss(x, y);
        break;
      case Constants.HIT:
        _ownView.addHit(x, y);
        break;
      case Constants.SUNK:
        _ownView.addHit(x, y);
        break;
    }
    if (_portForTest != null) {
      _portForTest.call(["_TEST:handleShot", _id, res[0], x, y]);
    }
    return res;
  }

  /** local action to add a boat in the grid. */
  void addBoat(Boat boat) {
    boats.placeBoats([boat]);
    assert(enemy != null);
    enemy.ready();
  }

  /** local action to generate an asynchronous shot at the enemy. */
  void shoot(int x, int y) {
    superShot(x, y, _id % 2 == 0);
  }

  /** A single shot on (x, y). */
  void singleShot(int x, int y) {
    if (_canShoot(x, y)) {
      _recordPendingShot(x, y);
      Future<int> res = enemy.shoot(x, y); // async shot!
      res.then((int result) {
        _recordShotResult(result, x, y);
      });
      res.handleException((String error) {
        _recordFailedShot(x, y);
        return true;
      });
    }
  }

  /**
   * Takes 1 shot, if it's a hit, it then shoots to each of the 4 cardinal
   * directions until a boat is sunk. When [parallel] all directions are
   * explored in parallel.
   */
  void superShot(int x, int y, bool parallel) {
    if (_canShoot(x, y)) {
      _recordPendingShot(x, y);
      Future<int> firstShot = enemy.shoot(x, y);
      firstShot.then((int res) {
        _recordShotResult(res, x, y);
        if (res == Constants.HIT) {
          // no miss, but no sunk, search around
          _exploreAllDirections(x, y, parallel);
        }
      });
      firstShot.handleException((String error) {
        _recordFailedShot(x, y);
        return true;
      });
    }
  }

  static const LEFT_DIR = const [-1, 0];
  static const RIGHT_DIR = const [1, 0];
  static const UP_DIR = const [0, -1];
  static const DOWN_DIR = const [0, 1];

  Future<bool> _exploreAllDirections(int x, int y, bool parallel) {
    Completer<bool> superShot_ = new Completer<bool>();
    if (parallel) {
      final arr = new List<Future<bool>>();
      arr.add(_exploreDirectionHelper(LEFT_DIR, x, y));
      arr.add(_exploreDirectionHelper(RIGHT_DIR, x, y));
      arr.add(_exploreDirectionHelper(UP_DIR, x, y));
      arr.add(_exploreDirectionHelper(DOWN_DIR, x, y));
      Futures.wait(arr).then((arrValues) {
        superShot_.complete(true);
      });
    } else {
      _seqExploreDirectionHelper(LEFT_DIR, x, y, superShot_,
          _seqExploreDirectionHelper(RIGHT_DIR, x, y, superShot_,
            _seqExploreDirectionHelper(UP_DIR, x, y, superShot_,
              _seqExploreDirectionHelper(DOWN_DIR, x, y, superShot_, null))))(false);
    }
    return superShot_.future;
  }
  Function _seqExploreDirectionHelper(List<int> dir, int x, int y,
      Completer<bool> seq, void _next(bool res)) {
    return (bool res) {
      if (res) {
        seq.complete(true);
      } else {
        _exploreDirectionHelper(dir, x, y).then(
            (_next != null) ? _next : (void _(v) {seq.complete(false);}));
      }
    };
  }

  Future<bool> _exploreDirectionHelper(List<int> dir, int x, int y) {
    Completer<bool> sunk = new Completer<bool>();
    _followDir(x + dir[0], y + dir[1], dir[0], dir[1], sunk);
    return sunk.future;
  }

  void _followDir(int x, int y, int incX, int incY, Completer<bool> sunk) {
    if (_canShoot(x, y)) {
      _recordPendingShot(x, y);
      Future<int> shot = enemy.shoot(x, y);
      shot.then((int res) {
        _recordShotResult(res, x, y);
        switch (res) {
          case Constants.HIT:
            if (!sunk.future.isComplete) {
              _followDir(x + incX, y + incY, incX, incY, sunk);
            }
            break;
          case Constants.SUNK:
            sunk.complete(true);
            break;
          case Constants.MISS:
            sunk.complete(false);
            break;
        }
      });
      shot.handleException((String error) {
        _recordFailedShot(x, y);
        sunk.completeException(error);
        return true;
      });
      // We don't actually chain sunk.cancel with shot.cancel because individual
      // shots can't be cancelled.
    } else {
      sunk.complete(false);
    }
  }

  /** checks that a shot is in range and has not been done before. */
  bool _canShoot(int x, int y) {
    return _inRange(x, y) && enemyGrid.valueAt(x, y) == null;
  }

  /** checks that a shot is in range. */
  bool _inRange(int x, int y) {
    return x >= 0 && y >=0 && x < Constants.SIZE && y < Constants.SIZE;
  }

  /** register a pending shot in the local enemyGrid state and update the UI. */
  void _recordPendingShot(int x, int y) {
    totalShots++;
    _enemyView.statusBar.updateStatus();
    _enemyView.addMaybeHit(x, y);
    enemyGrid.pending(x, y);
  }

  /** record a cancelled shot in the local enemyGrid state and update the UI. */
  void _recordCancelledShot(int x, int y) {
    totalShots--;
    _enemyView.removeMaybeHit(x, y);
    _enemyView.statusBar.updateStatus();
    enemyGrid.clear(x, y);
  }

  /** record a failing shot in the local enemyGrid state and update the UI. */
  void _recordFailedShot(int x, int y) {
    _recordCancelledShot(x, y);
  }

  /** register the result of a shot and update the UI. */
  void _recordShotResult(int shotResult, int x, int y) {
    switch(shotResult) {
      case Constants.MISS:
        totalMisses++;
        _enemyView.addMiss(x, y);
        enemyGrid.miss(x, y);
        break;
      case Constants.HIT:
        totalHits++;
        _enemyView.addHit(x, y);
        enemyGrid.hit(x, y);
        break;
      case Constants.SUNK:
        totalHits++;
        boatsSunk++;
        _enemyView.addHit(x, y);
        enemyGrid.hit(x, y);
        break;
    }
    _enemyView.statusBar.updateStatus();
  }
}
