// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/** Entrypoint to set up the dartcombat sample. */
void setUpGame() {
  setupUI();
  new ActorManager(3).create(const GameFactory(), "create-players");
}

/** Sets up the UI creating the board for each player. */
void setupUI() {
  // Note: we set up the UI programatically to make testing easier.
  var div = new Element.tag("div");
  div.innerHTML = """
    <div class='hbox'>
      <div class='vbox'>
        Player 1 board:
        <div class='own' id='p1own'></div>
        Known of enemy's board:
        <div class='enemy' id='p1enemy'></div>
      </div>
      <div style='width:20%'></div>
      <div class='vbox'>
        Player 2 board:
        <div class='own' id='p2own'></div>
        Known of enemy's board:
        <div class='enemy' id='p2enemy'></div>
      </div>
    </div>
  """;
  document.body.nodes.add(div);
}

class Game extends Actor {
  Future<List<ActorId>> playersFuture;
  
  Game() : super() {
    on["create-players"] = () {
      Completer<ActorId> p1 = new Completer<ActorId>();
      Completer<ActorId> p2 = new Completer<ActorId>();
      create(const PlayerFactory(), "init", [1, messageback((ActorId pid) {
        p1.complete(pid);
      })]);
      create(const PlayerFactory(), "init", [2, messageback((ActorId pid) {
        p2.complete(pid);
      })]);
      playersFuture = Futures.wait([p1.future, p2.future]);
      playersFuture.then((List<ActorId> players) {
        players[0].send("set-enemy", [players[1]]); 
        players[1].send("set-enemy", [players[0]]);
      });
    };
  }
}

class GameFactory implements ActorFactory {
  const GameFactory();
  Actor create() => new Game();
}
class PlayerFactory implements ActorFactory {
  const PlayerFactory();
  Actor create() => new Player();
}
class PlayerStateFactory implements ActorFactory {
  const PlayerStateFactory();
  Actor create() => new PlayerState();
}
class PlayerGridViewFactory implements ActorFactory {
  const PlayerGridViewFactory();
  Actor create() => new PlayerGridView();
}
class PlaceBoatViewFactory implements ActorFactory {
  const PlaceBoatViewFactory();
  Actor create() => new PlaceBoatView();
}
class EnemyGridViewFactory implements ActorFactory {
  const EnemyGridViewFactory();
  Actor create() => new EnemyGridView();
}
