// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/** A boat in the grid. */
class Boat {
  final int startX;
  final int startY;
  final bool horizontal;
  final int length;
  int hitCount = 0;

  Boat(this.startX, this.startY, this.horizontal, this.length) {}

  bool get sunk() => length == hitCount;
}

/** Represents a grid configuration. */
class BoatGrid {
  List<List<Boat>> boatMap;

  BoatGrid() : boatMap = new List(Constants.SIZE) {
    for (int i = 0; i < Constants.SIZE; i++) {
      boatMap[i] = new List(10);
    }
  }

  void placeBoats(List<Boat> boats) {
    for (int b = 0; b < boats.length; b++) {
      Boat boat = boats[b];
      for (int i = 0; i < boat.length; i++)  {
        int x = boat.startX + (boat.horizontal ? i : 0);
        int y = boat.startY + (boat.horizontal ? 0 : i);
        boatMap[x][y] = boat;
      }
    }
  }

  List<int> shoot(GridState state, int x, int y) {
    assert(x >= 0);
    assert(y >= 0);
    assert(x < Constants.SIZE);
    assert(y < Constants.SIZE);
    assert(state.valueAt(x, y) == null); // repeated shot
    Boat b = boatMap[x][y];
    if (b == null) {
      state.miss(x, y);
      return const [Constants.MISS];
    } else {
      state.hit(x, y);
      b.hitCount++;
      return b.sunk ? [Constants.SUNK, b.length] : const [Constants.HIT];
    }
  }
}

/** Represents the current state of a boat grid. */
class GridState {
  List<List<int>> cells;

  GridState()
      : cells = new List(Constants.SIZE) {
    for (int i = 0; i < Constants.SIZE; i++) {
      cells[i] = new List(10);
    }
  }

  int valueAt(int x, int y) => cells[x][y];

  void miss(int x, int y) {
    cells[x][y] = Constants.MISS;
  }

  void hit(int x, int y) {
    cells[x][y] = Constants.HIT;
  }

  void pending(int x, int y) {
    cells[x][y] = Constants.PENDING;
  }

  void clear(int x, int y) {
    cells[x][y] = null;
  }
}


/** Static constants used by the game. */
class Constants {
  static const SIZE = 10;
  static const MISS = 1;
  static const HIT = 2;
  static const SUNK = 3;
  static const PENDING = 4;

  Constants() {}
}
