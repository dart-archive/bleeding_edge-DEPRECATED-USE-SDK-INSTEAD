// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/** Entrypoint to set up the dartcombat sample. */
void setUpGame() {
  setupUI();
  createPlayers();
}

/** Sets up the UI creating the board for each player. */
void setupUI() {
  // Note: we set up the UI programatically to make testing easier.
  var div = new Element.tag("div");
  div.innerHtml = """
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

/** Create and connect players. */
void createPlayers() {
  Player player1 = new Player();
  player1.setup(window, 1);

  Player player2 = new Player();
  player2.setup(window, 2);

  player1.enemy = new FlakyProxy(player2.portToPlayer).sendPort;

  player2.enemy = new FlakyProxy(player1.portToPlayer).sendPort;
}

/**
 * Create and connect players, providing a port for communicating progress to
 * the test.
 */
void createPlayersForTest(SendPort testPort) {
  Player player1 = new Player();
  Player player2 = new Player();

  player1._portForTest = testPort;
  player2._portForTest = testPort;

  player1.setup(window, 1);
  player2.setup(window, 2);

  player1.enemy = player2.portToPlayer;
  player2.enemy = player1.portToPlayer;
}

/**
 * A proxy between ports that randomly drops messages to simulate isolates
 * across the network.
 */
class FlakyProxy {
  ReceivePort proxy;

  SendPort _target;

  SendPort get sendPort => proxy.toSendPort();

  FlakyProxy(this._target) {
    proxy = new ReceivePort();

    proxy.receive((message, SendPort reply) {
      window.setTimeout(() {
        if (randomlyFail()) {
          reply.send(const [false, "There was an error"], null);
        } else {
          _target.send(message, reply);
        }
      }, 200);
    });
  }

  // TODO(sigmund): introduce UI to control flakiness, then do:
  // => Math.random() > 0.9;
  bool randomlyFail() => false;
}
