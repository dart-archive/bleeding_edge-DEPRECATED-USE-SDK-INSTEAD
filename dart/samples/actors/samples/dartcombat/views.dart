// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/** Base class for all views. */
class View extends Actor {
  String _rootNode;
  View() : super() {
  }
  
  /** Extract the position of a mouse event in a containing 500x500 grid. */
  Future<List<int>> _positionFromEvent(String gridNode, MouseEventWrapper e) {
    final completer = new Completer<List<int>>();
    ui.onRectReady(gridNode, messageback((ClientRect offset) {
      int x = (e.pageX - offset.left) ~/ 50;
      int y = (e.pageY - offset.top) ~/ 50;
      completer.complete([x, y]);
    }));
    return completer.future;
  }

  /** Given a grid node (square or boat) place it at a grid coordinate. */
  void _placeNodeAt(String nodeId, int x, int y) {
    int xoffset = x * 50;
    int yoffset = y * 50;
    ui.setStyles(nodeId, ["top", "left"], 
      [[yoffset.toString() + "px", xoffset.toString() + "px"]]);
  }

  /** Create a div node with a given class name. */
  String _createDiv(String className, String selector) {
    final divId = "ddd${genId()}";
    ui.createIn("div", [divId], selector);
    ui.setAttributes("#${divId}", ["class"], [[className]]);
    return "#${divId}";
  }
}

/** The view displayed to the player for its own grid. */
class PlayerGridView extends View {
  /** Model associated with the player's state. */
  ActorId playerState;

  /** A div element containing the grid. */

  PlayerGridView() : super() {
      
    on["init"] = (ActorId state, String rootNodeId, int localGridCellsLen, 
        Reply rep) {      
      this.playerState = state;
      _rootNode = rootNodeId;
      _render(localGridCellsLen);
      rep.respond([me]);
    };
    
    /** Adds to this view the respresentation of a missed shot. */
    on["add-miss"] = (int x, int y) {
      final divId = _createDiv("icons miss", _rootNode);
      _placeNodeAt(divId, x, y);
    };
    
    /** Adds to this view the respresentation of a shot that hits our boat. */
    on["add-hit"] = (int x, int y) {
      final divId = _createDiv("icons hit-onboat", _rootNode);
      _placeNodeAt(divId, x, y);
    };
  }

  /** Create an initial visual representation of this view. */
  void _render(int localGridCellsLen) {
    String cell = "<div class='icons water'></div>";
    StringBuffer _cells = new StringBuffer();
    for (int i = 0 ; i < localGridCellsLen; i++) {
      _cells.add(cell);
    }
    String cells = _cells.toString();
    String row = "<div class='hbox'>${cells}</div>";
    StringBuffer _rows = new StringBuffer();
    for (int i = 0 ; i < localGridCellsLen; i++) {
      _rows.add(row);
    }
    String rows = _rows.toString();
    String table = "<div class='vbox'>${rows}</div>";
    ui.innerHTML(_rootNode, table);

    // Attaches listeners onto this view.
    create(const PlaceBoatViewFactory(), "attach", [playerState, _rootNode]);
  }
}

/** View used to interactively set a new boat on the board. */
class PlaceBoatView extends View {
  ActorId playerState;

  /** start location where the user first clicked. */
  int _boatStartX;
  int _boatStartY;

  /** last known mouse location. */
  int _boatLastX;
  int _boatLastY;

  /** HTML element rendering the actual boat. */
  String _possibleBoat;

  /** Mouse move-listener to be detached when the boat is placed. */
  String _moveListener;

  PlaceBoatView() : super() {
    on["attach"] = (ActorId state, String rootNodeId) {
      this.playerState = state;
      _rootNode = rootNodeId;
      ui.onMouseDown(_rootNode, new Reply(me, "handleMouseDown"));
      ui.onMouseUp(_rootNode, new Reply(me, "handleMouseUp"));
    };
    
    on["handleMouseDown"] = (var e) {
      handleMouseDown(e);
    };

    on["handleMouseUp"] = (var e) {
      handleMouseUp(e);
    };

    on["handleMouseMove"] = (var e) {
      handleMouseMove(e);
    };
  }

  void handleMouseDown(e) {
    _positionFromEvent(_rootNode, e).then((List<int> pos) {
      _boatStartX = pos[0];
      _boatStartY = pos[1];
      // error case when the mouse was released out of the boat-placing area
      if (_moveListener != null) {
        removeMessage(_moveListener);
        ui.remove(_rootNode, _possibleBoat);
        _moveListener = null;
      }
      
      _possibleBoat = _createDiv("icons boat2", _rootNode);      
      _placeNodeAt(_possibleBoat, _boatStartX, _boatStartY);
      
      Reply handle = messageback(handleMouseMove);
      _moveListener = handle.message;
      ui.onMouseMove(_rootNode, handle);
    });
  }

  void handleMouseMove(e) {
    _positionFromEvent(_rootNode, e).then((List<int> pos) {
      if (_boatLastX == pos[0] && _boatLastY == pos[1]) {
        return;
      }
      _boatLastX = pos[0];
      _boatLastY = pos[1];
      int deltaX = _boatLastX - _boatStartX;
      int deltaY = _boatLastY - _boatStartY;

      String dir;
      bool flip = false;
      int boatSize = 2;
      if (deltaX.abs() >= deltaY.abs()) {
        dir = deltaX < 0 ? "right" : "left";
        boatSize = Math.max(2, Math.min(5, deltaX.abs() + 1));
      } else {
        dir = deltaY < 0 ? "up" : "down";
        boatSize = Math.max(2, Math.min(5, deltaY.abs() + 1));
      }

      ui.setAttributes(_possibleBoat, 
          ["class"], [["icons boat${boatSize} boatdir-${dir}"]]);
    });
  }

  /** Handle end of positioning of a boat. */
  void handleMouseUp(e) {
    removeMessage(_moveListener);
    _moveListener = null;
    _positionFromEvent(_rootNode, e).then((List<int> pos) {
      int _boatEndX = pos[0];
      int _boatEndY = pos[1];

      int deltaX = _boatEndX - _boatStartX;
      int deltaY = _boatEndY - _boatStartY;
      Boat boat;

      if (deltaX.abs() >= deltaY.abs()) {
        int boatSize = Math.max(2, Math.min(5, deltaX.abs() + 1));
        boat = new Boat(deltaX < 0 ? (_boatStartX - boatSize + 1) : _boatStartX,
            _boatStartY, true, boatSize);
      } else {
        int boatSize = Math.max(2, Math.min(5, deltaY.abs() + 1));
        boat = new Boat(_boatStartX,
            deltaY < 0 ? (_boatStartY - boatSize + 1) : _boatStartY,
            false, boatSize);
      }

      playerState.send("add-boat", [boat]);
    });
  }
}

/** The view displayed to the player for its enemy's grid. */
class EnemyGridView extends View {
  ActorId playerState;
  bool _enemyReady;
  String statusBar;

  EnemyGridView() : super() {
    _enemyReady = false;
    on["init"] = (ActorId state, String rootNodeId, int enemyGridCellsLen, 
        Reply rep) {
      _rootNode = rootNodeId;
      playerState = state;
      String cell = "<div class='icons water'></div>";
      StringBuffer _cells = new StringBuffer();
      for (int i = 0 ; i < enemyGridCellsLen; i++) {
        _cells.add(cell);
      }
      String cells = _cells.toString();
      String row = "<div class='hbox'>${cells}</div>";
      StringBuffer _rows = new StringBuffer();
      for (int i = 0 ; i < enemyGridCellsLen; i++) {
        _rows.add(row);
      }
      String rows = _rows.toString();
      String table = "<div class='vbox'>${rows}</div>";
      ui.innerHTML(_rootNode,
          "${table}<div class='notready'>ENEMY IS NOT READY</div>");
      statusBar = _createDiv("shooting-status", _rootNode);
      playerState.send("query-status", [new Reply(me, "update-status")]);
      ui.onclick(_rootNode, new Reply(me, "handle-click"), false);
      rep.respond([me]);
    };

    /** Update the view to indicate we sunk another enemy's boat. */
    on["update-status"] = (var total, var hit, var miss, var sunk) {
      final accounted = hit + miss;
      ui.innerHTML(statusBar,
          "${total} &lt;= ${accounted} (${hit} + ${miss}); ${sunk}");
    };
    
    /** Interpret clicks as a shooting action. */
    on["handle-click"] = (MouseEventWrapper e) {
      _positionFromEvent(_rootNode, e).then((List<int> pos) {
        playerState.send("shoot-enemy", [pos[0], pos[1]]);
      });
    };
    
    /** Update the view to indicate the enemy is ready. */
    on["set-enemy-ready"] = () {
      if (!_enemyReady) {
        _enemyReady = true;
        ui.remove(_rootNode, ".notready");
      }
    };
    
    /** Update the view to indicate a shot that hit an enemy's boat. */
    on["add-hit"] = (int x, int y) {
      final node = _createDiv("icons hit", _rootNode);
      _placeNodeAt(node, x, y);
    };

    /** Update the view to indicate a shot that missed an enemy's boat. */
    on["add-miss"] = (int x, int y) {
      final node = _createDiv("icons miss", _rootNode);
      _placeNodeAt(node, x, y);
    };
  }
}
