// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Stores the actual data on a player's boat grid, the UI representation for its
 * grid and the status of each shot. Acts as a controller handling isolate
 * messages (from the main isolate message and shots from the enemy), and UI
 * events.
 */
// TODO(sigmund): move UI setup out of here, e.g. into a controller class.
class PlayerState extends Actor {

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
  ActorId enemy;

  /** UI representation of this player's grid. */
  ActorId _ownView;

  /** UI representation of the enemy's grid. */
  ActorId _enemyView;

  /** Port used for testing purposes. */
  ActorId _forTest;

  Future<List<ActorId>> viewsFuture;
  
  // This can take no arguments for now (wait for isolate redesign).
  PlayerState() : super() {
    on["setup"] = (int id, Reply r) {
      handleSetup(id, r);
    };
    on["set-enemy"] = (ActorId enemyId) {
      enemy = enemyId;
    };
    // message from the other player indicating it's ready to play.
    on["enemy-is-ready"] = (Reply r) {
      _enemyView.send("set-enemy-ready");
      r.respond([true, ""]);
    };
    // message from the other player indicating a shot.
    on["shoot"] = (int x, int y, Reply r) {
      List res = handleShot(x, y);
      r.respond([true, res]);
    };
    
    /** local action to generate an asynchronous shot at the enemy. */
    on["shoot-enemy"] = (int x, int y) {
      superShot(x, y, _id % 2 == 0);
    };

    /** local action to add a boat in the grid. */
    on["add-boat"] = (Boat boat) {
      boats.placeBoats([boat]);
      assert(enemy != null);
      ready();
    };

    on["query-status"] = (Reply r) {
      r.respond([totalShots, totalHits, totalMisses, boatsSunk]);
    };
  }

  /** Handles a message from the main isolate to setup this player. */
  void handleSetup(int id, Reply r) {
    _id = id;
    boats = new BoatGrid();
    localGrid = new GridState();
    enemyGrid = new GridState();
    totalShots = 0;
    totalHits = 0;
    totalMisses = 0;
    boatsSunk = 0;

    Completer<ActorId> ownV = new Completer<ActorId>();
    Completer<ActorId> enemyV = new Completer<ActorId>();
    create(const PlayerGridViewFactory(), "init", 
        [me, "#p${id}own", localGrid.cells.length, messageback((ActorId pid) {
          ownV.complete(pid);
    })]);

    create(const EnemyGridViewFactory(), "init", 
        [me, "#p${id}enemy", enemyGrid.cells.length, messageback((ActorId pid) {
          enemyV.complete(pid);
    })]);
    viewsFuture = Futures.wait([ownV.future, enemyV.future]);
    viewsFuture.then((List<ActorId> views) {
      _ownView = views[0];
      _enemyView = views[1];
      r.respond([me]);
    });
  }

  /** Handles a shot message from the enemy player. */
  List handleShot(int x, int y) {
    List res = boats.shoot(localGrid, x, y);
    switch (res[0]) {
      case Constants.MISS:
        _ownView.send("add-miss", [x, y]);
        break;
      case Constants.HIT:
        _ownView.send("add-hit", [x, y]);
        break;
      case Constants.SUNK:
        _ownView.send("add-hit", [x, y]);
        break;
    }
    return res;
  }

  /** A single shot on (x, y). */
  void singleShot(int x, int y) {
    if (_canShoot(x, y)) {
      _recordPendingShot(x, y);
      Future<int> res = shoot(x, y); // async shot!
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
      Future<int> firstShot = shoot(x, y);
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

  static final LEFT_DIR = const [-1, 0];
  static final RIGHT_DIR = const [1, 0];
  static final UP_DIR = const [0, -1];
  static final DOWN_DIR = const [0, 1];

  Future<bool> _exploreAllDirections(int x, int y, bool parallel) {
    Completer<bool> superShot = new Completer<bool>();
    if (parallel) {
      final arr = new List<Future<bool>>();
      arr.add(_exploreDirectionHelper(LEFT_DIR, x, y));
      arr.add(_exploreDirectionHelper(RIGHT_DIR, x, y));
      arr.add(_exploreDirectionHelper(UP_DIR, x, y));
      arr.add(_exploreDirectionHelper(DOWN_DIR, x, y));
      Futures.wait(arr).then((arrValues) {
        superShot.complete(true);
      });
    } else {
      _seqExploreDirectionHelper(LEFT_DIR, x, y, superShot,
          _seqExploreDirectionHelper(RIGHT_DIR, x, y, superShot,
            _seqExploreDirectionHelper(UP_DIR, x, y, superShot,
              _seqExploreDirectionHelper(DOWN_DIR, x, y, superShot, null))))
              (false);
    }
    return superShot.future;
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
      Future<int> shot = shoot(x, y);
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
    _enemyView.send("updateStatus", 
        [totalShots, totalHits, totalMisses, boatsSunk]);
    _enemyView.send("addMaybeHit", [x, y]);
    enemyGrid.pending(x, y);
  }

  /** record a cancelled shot in the local enemyGrid state and update the UI. */
  void _recordCancelledShot(int x, int y) {
    totalShots--;
    _enemyView.send("removeMaybeHit", [x, y]);
    _enemyView.send("updateStatus", 
        [totalShots, totalHits, totalMisses, boatsSunk]);
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
        _enemyView.send("add-miss", [x, y]);
        enemyGrid.miss(x, y);
        break;
      case Constants.HIT:
        totalHits++;
        _enemyView.send("add-hit", [x, y]);
        enemyGrid.hit(x, y);
        break;
      case Constants.SUNK:
        totalHits++;
        boatsSunk++;
        _enemyView.send("add-hit", [x, y]);
        enemyGrid.hit(x, y);
        break;
    }
    _enemyView.send("updateStatus", 
        [totalShots, totalHits, totalMisses, boatsSunk]);
  }
  
  Future<int> ready() {
    Completer<int> res = new Completer<int>();
    enemy.send("enemy-is-ready", 
        [messageback((bool success, String error) {
      if (success) {
        res.complete(0);
      } else {
        res.completeException(error);
      }
    })]);
    return res.future;
  }

  Future<int> shoot(int x, int y) {
    Completer<int> res = new Completer<int>();
    ui.setTimeout(messageback(() {
      enemy.send("shoot", 
          [x, y, messageback((bool success, var l) {
        if (success) {
          res.complete(l[0]);
        } else {
          res.completeException(l);
        }
      })]);
    }), 200);
    return res.future;
  }

}
