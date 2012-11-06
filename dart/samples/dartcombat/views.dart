// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/** Base class for all views. */
class View {
  Document doc;
  View(this.doc) {}
}

/** The view displayed to the player for its own grid. */
class PlayerGridView extends View {
  /** Model associated with the player's state. */
  PlayerState state;

  /** A div element containing the grid. */
  Element _rootNode;

  PlayerGridView(
      PlayerState this.state, Element rootNode)
      : super(rootNode.document), _rootNode = rootNode {
    render();
  }

  /** Create an initial visual representation of this view. */
  void render() {
    String cell = "<div class='icons water'></div>";
    StringBuffer _cells = new StringBuffer();
    for (int i = 0 ; i < state.localGrid.cells.length; i++) {
      _cells.add(cell);
    }
    String cells = _cells.toString();
    String row = "<div class='hbox'>${cells}</div>";
    StringBuffer _rows = new StringBuffer();
    for (int i = 0 ; i < state.localGrid.cells.length; i++) {
      _rows.add(row);
    }
    String rows = _rows.toString();
    String table = "<div class='vbox'>${rows}</div>";
    _rootNode.innerHTML = table;

    // Attaches listeners onto this view.
    new PlaceBoatView(state, _rootNode).attach();
  }

  /** Adds to this view the respresentation of a missed shot. */
  void addMiss(int x, int y) {
    Element node = ViewUtil.createDiv("icons miss");
    ViewUtil.placeNodeAt(node, x, y);
    _rootNode.nodes.add(node);
  }

  /** Adds to this view the respresentation of a shot that hits our boat. */
  void addHit(int x, int y) {
    Element node = ViewUtil.createDiv("icons hit-onboat");
    ViewUtil.placeNodeAt(node, x, y);
    _rootNode.nodes.add(node);
  }
}

/** View used to interactively set a new boat on the board. */
class PlaceBoatView extends View {
  PlayerState state;

  /** root of the grid. */
  Element _rootNode;

  /** start location where the user first clicked. */
  int _boatStartX;
  int _boatStartY;

  /** last known mouse location. */
  int _boatLastX;
  int _boatLastY;

  /** HTML element rendering the actual boat. */
  Element _possibleBoat;

  /** Mouse move-listener to be detached when the boat is placed. */
  Function _moveListener;

  PlaceBoatView(
      PlayerState this.state, Element rootNode)
      : super(rootNode.document), _rootNode = rootNode {}

  void attach() {
    _rootNode.on.mouseDown.add(handleMouseDown);
    _rootNode.on.mouseUp.add(handleMouseUp);
  }

  void handleMouseDown(e) {
    e.preventDefault();
    ViewUtil.positionFromEvent(_rootNode, e).then((List<int> pos) {
      _boatStartX = pos[0];
      _boatStartY = pos[1];
      // error case when the mouse was released out of the boat-placing area
      if (_moveListener != null) {
        _rootNode.on.mouseMove.remove(_moveListener, false);
        _possibleBoat.remove();
        _moveListener = null;
      }
      _possibleBoat = ViewUtil.createDiv("icons boat2");
      ViewUtil.placeNodeAt(_possibleBoat, _boatStartX, _boatStartY);
      _rootNode.nodes.add(_possibleBoat);
      _moveListener = handleMouseMove;
      _rootNode.on.mouseMove.add(_moveListener);
    });
  }

  void handleMouseMove(e) {
    e.preventDefault();
    ViewUtil.positionFromEvent(_rootNode, e).then((List<int> pos) {
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

      _possibleBoat.attributes["class"] = "icons boat${boatSize} boatdir-${dir}";
    });
  }

  /** Handle end of positioning of a boat. */
  void handleMouseUp(e) {
    _rootNode.on.mouseMove.remove(_moveListener, false);
    _moveListener = null;
    ViewUtil.positionFromEvent(_rootNode, e).then((List<int> pos) {
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

      state.addBoat(boat);
    });
  }
}

/** The view displayed to the player for its enemy's grid. */
class EnemyGridView extends View {
  PlayerState state;
  bool _enemyReady;
  Element _rootNode;
  ShootingStatusView statusBar;

  EnemyGridView(
      PlayerState this.state, Element rootNode)
      : super(rootNode.document),
        _enemyReady = false,
        _rootNode = rootNode {

    String cell = "<div class='icons water'></div>";
    StringBuffer _cells = new StringBuffer();
    for (int i = 0 ; i < state.enemyGrid.cells.length; i++) {
      _cells.add(cell);
    }
    String cells = _cells.toString();
    String row = "<div class='hbox'>${cells}</div>";
    StringBuffer _rows = new StringBuffer();
    for (int i = 0 ; i < state.enemyGrid.cells.length; i++) {
      _rows.add(row);
    }
    String rows = _rows.toString();
    String table = "<div class='vbox'>${rows}</div>";
    _rootNode.innerHTML =
        "${table}<div class='notready'>ENEMY IS NOT READY</div>";
    statusBar = new ShootingStatusView(state, doc);
    _rootNode.nodes.add(statusBar._rootNode);
    _rootNode.on.click.add((Event e) {
          MouseEvent mouseEvent = e;
          handleClick(mouseEvent);
        }, false);
  }

  /** Interpret clicks as a shooting action. */
  void handleClick(MouseEvent e) {
    ViewUtil.positionFromEvent(_rootNode, e).then((List<int> pos) {
      state.shoot(pos[0], pos[1]);
    });
  }

  /** Update the view to indicate the enemy is ready. */
  void setEnemyReady() {
    if (!_enemyReady) {
      _enemyReady = true;
      _rootNode.query(".notready").remove();
    }
  }


  /** Update the view to indicate a shot that hit an enemy's boat. */
  void addHit(int x, int y) {
    Element node = ViewUtil.createDiv("icons hit");
    ViewUtil.placeNodeAt(node, x, y);
    _rootNode.nodes.add(node);
  }

  /** Update the view to indicate a shot that missed an enemy's boat. */
  void addMiss(int x, int y) {
    Element node = ViewUtil.createDiv("icons miss");
    ViewUtil.placeNodeAt(node, x, y);
    _rootNode.nodes.add(node);
  }

  /** Update the view to indicate a shot is in progress. */
  void addMaybeHit(int x, int y) {
    Element node = ViewUtil.createDiv("icons maybe-hit");
    ViewUtil.placeNodeAt(node, x, y);
    _rootNode.nodes.add(node);
  }

  /**
   * Remove the icon indicating that a shot is in progress (only called when
   * shots failed due to network errors).
   */
  void removeMaybeHit(int x, int y) {
    for (Element node in _rootNode.queryAll(".maybe-hit")) {
      int xoffset = x * 50;
      int yoffset = y * 50;
      if (node.style.getPropertyValue("top") == "${yoffset}px"
          && node.style.getPropertyValue("left") == "${xoffset}px") {
        node.remove();
        return;
      }
    }
  }
}

class ShootingStatusView extends View {
  PlayerState state;
  Element _rootNode;

  ShootingStatusView(this.state, Document doc)
      : super(doc) {
    _rootNode = ViewUtil.createDiv("shooting-status");
    updateStatus();
  }

  /** Update the view to indicate we sunk another enemy's boat. */
  void updateStatus() {
    final total = state.totalShots;
    final hit = state.totalHits;
    final miss = state.totalMisses;
    final accounted = hit + miss;
    final sunk = state.boatsSunk;
    _rootNode.innerHTML =
        "${total} &lt;= ${accounted} (${hit} + ${miss}); ${sunk}";
  }
}

/** Utility methods used by the views above. */
class ViewUtil {

  /** Extract the position of a mouse event in a containing 500x500 grid. */
  static Future<List<int>> positionFromEvent(Element gridNode, MouseEvent e) {
    final completer = new Completer<List<int>>();
    gridNode.rect.then((ElementRect rect) {
      int x = (e.pageX - rect.offset.left) ~/ 50;
      int y = (e.pageY - rect.offset.top) ~/ 50;
      completer.complete([x, y]);
    });
    return completer.future;
  }

  /** Given a grid node (square or boat) place it at a grid coordinate. */
  static void placeNodeAt(Element node, int x, int y) {
    int xoffset = x * 50;
    int yoffset = y * 50;
    node.style.setProperty("top", "${yoffset}px");
    node.style.setProperty("left", "${xoffset}px");
  }

  /** Create a div node with a given class name. */
  static Element createDiv(String className) {
    Element node = new Element.tag("div");
    node.attributes["class"] = className;
    return node;
  }
}
