#import('../../core/actors-web.dart');
#import('dart:html');

main() {
  var button = document.query("#setup-button");
  var rows = document.query("#rows");
  var cols = document.query("#cols");
  var isols = document.query("#isols");
  var gamePanel = document.query("#game-panel");
  var setupPanel = document.query("#setup-panel");
  button.on.click.add((Event e) {
    var actorManager;
    actorManager = new ActorManager(Math.parseInt(isols.value));
    final numx = Math.parseInt(cols.value);
    final numy = Math.parseInt(rows.value);
    final offset = 160;
    gamePanel.style.setProperty("display", "block", "");
    setupPanel.style.setProperty("display", "none", "");
    actorManager.create(const LifeManagerFactory(), "init", 
        [numx, numy, offset]);
  });
}

class LifeManagerFactory implements ActorFactory {
  const LifeManagerFactory();
  Actor create() => new LifeManager();
}

class LifeManager extends Actor {
  int _numx, _numy;
  List<ActorId> _cells;
  int _readies;
  bool _running;
  int _calculated;
  int _aliveN;

  LifeManager() : super() {
    on["init"] = (int numx, int numy, int offset) {
      _running = false;
      _numx = numx;
      _numy = numy;
      _cells = new List(_numx*_numy);
      for (int i = 0; i < _numy; i++) {
        for (int j = 0; j < _numx; j++) {
          create(const CellFactory(), "init", [_numx, _numy, j, i, offset, me]);
        }
      }
      ui.onclick("#run-button", new Reply(me, "toggle-run"));
      _readies = 0;
      _calculated = 0;
      _aliveN = 0;
    };

    on["toggle-run"] = (var e) {
      if (!_running) {
        _running = true;
        me.send("send-go");
        ui.innerHTML("#run-button", "Stop");
      }
      else {
        _running = false;
        ui.innerHTML("#run-button", "Run");
      }
    };

    on["ready"] = (int indx, ActorId id) {
      _cells[indx] = id;
      _readies++;
      if (_readies == _cells.length) {
        _readies = 0;
        for (ActorId a in _cells) {
          a.send("cells", [_cells]);
        }
      }
    };

    on["calculate"] = (bool isAlive) {
      _calculated++;
      if (isAlive) _aliveN++;
      if (_calculated == _cells.length) {
        ui.innerHTML("#alive", ""+_aliveN);
        _calculated = 0;
        _aliveN = 0;
        for (ActorId a in _cells) {
          a.send("update");
        }
      }
    };

    on["updated"] = () {
      _readies++;
      if (_readies == _cells.length && _running) {
        _readies = 0;
        ui.setTimeout(new Reply(me, "send-go"), 100);
      }
    };

    on["send-go"] = () {
      _readies = 0;
      for (ActorId a in _cells) {
        a.send("go");
      }
    };
  }
}

class Cell extends Actor {
  int _numx, _numy;
  int _cellx, _celly;
  String _cellId;
  bool _alive, _oldState;
  int _rets;
  int _aliveN;
  List<ActorId> _neighbors;
  ActorId _manager;

  void _createCell(String id, int x, int y) {
    ui.createIn("div", [id], "#game-panel");
    ui.setAttributes("#${id}", ["class"], [["movable"]]);
    ui.setStyles("#${id}", ["top", "left"], [["$y", "$x"]]);
    ui.onclick("#${id}", new Reply(me, "toggle"));
  }

  Cell() : super() {
    on["init"] = (int numx, int numy, int cellx, int celly,
                  int offset, ActorId manager) {
      _numx = numx;
      _numy = numy;
      _cellx = cellx;
      _celly = celly;
      _manager = manager;
      _cellId = "c${(_numx*_celly + _cellx)}";
      _alive = false;
      _oldState = _alive;
      _rets = 0;
      _aliveN = 0;
      _createCell(_cellId, 50+_cellx*20, offset+_celly*20);
      reply("ready", [(_numx*_celly + _cellx), me]);
    };

    on["cells"] = (List<ActorId> cells) {
      List<int> _neighborsI = <int>[];
      _neighbors = <ActorId>[];
      for (int i = _celly-1; i <=_celly+1; i++) {
        for (int j = _cellx-1; j <=_cellx+1; j++) {
          if (i >=0 && i < _numy && j>=0 && j < _numx) {
            if (!(i == _celly && j == _cellx))
              _neighborsI.add(_numx*i + j);
          }
        }
      }
      for (int i in _neighborsI) {
        _neighbors.add(cells[i]);
      }
      _manager.send("updated");
    };

    on["get-state"] = (Reply reply) {
      reply.respond([_oldState]);
    };

    on["go"] = () {
      _rets = 0;
      _aliveN = 0;
      for (ActorId x in _neighbors) {
        x.send("get-state", [new Reply(me, "neighbor-state")]);
      }
    };

    on["update"] = () {
      _oldState = _alive;
      _manager.send("updated");
    };

    on["neighbor-state"] = (bool isAlive) {
      _rets++;
      if (isAlive == true) {
        _aliveN++;
      }
      if (_rets == _neighbors.length) {
        if (_alive) {
          if (_aliveN < 2 || _aliveN > 3)
            _alive = false;
        }
        else {
          if (_aliveN == 3)
            _alive = true;
        }
        if (_alive != _oldState) {
          if (_alive) {
            ui.setStyles("#$_cellId", ["background-color"], [["green"]]);
          }
          else {
            ui.setStyles("#$_cellId", ["background-color"], [["yellow"]]);
          }

        }
        _manager.send("calculate", [_alive]);
      }
    };

    on["toggle"] = (var e) {
      String color;
      if (_alive) {
        color = "yellow";
        _alive = false;
      }
      else {
        color = "green";
        _alive = true;
      }
      _oldState = _alive;
      ui.setStyles("#$_cellId", ["background-color"], [[color]]);
    };
  }
}


class CellFactory implements ActorFactory {
  const CellFactory();
  Actor create() => new Cell();
}
