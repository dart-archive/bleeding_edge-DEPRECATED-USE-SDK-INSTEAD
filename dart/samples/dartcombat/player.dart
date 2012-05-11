// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/** A local player (API known to the main isolate when creating players). */
interface Player default PlayerImpl {

  Player();

  final Future<SendPort> portToPlayer;

  void setup(Window window, int player);

  void set enemy(SendPort portToEnemy);

  void set _portForTest(SendPort testPort);
}

/** A remote enemy (API visible to a player to communicate with the enemy). */
interface Enemy default EnemyImpl {
  Enemy(SendPort port);

  /** tell the enemy that we are ready, receive confirmation asynchronously. */
  Future<int> ready();

  /** shoot asynchronously. */
  Future<int> shoot(int x, int y);
}


/**
 * A default implementation for player that sends messages to an isolate, which
 * contains the actual player state.
 */
class PlayerImpl implements Player {
  final Future<SendPort> portToPlayer;

  PlayerImpl() : portToPlayer = new PlayerState().spawn();

  void setup(Window window, int player) {
    portToPlayer.then((SendPort port) => port.call(
        { "action" : MessageIds.SETUP,
          "args" : [player] }));
  }

  void set enemy(SendPort portToEnemy) {
    portToPlayer.then((port) => port.call(
        { "action" : MessageIds.SET_ENEMY,
          "args" : [portToEnemy]}));
  }

  void set _portForTest(SendPort testPort) {
    portToPlayer.then((port) => port.call(
        { "action" : MessageIds.SET_PORT_FOR_TEST,
          "args" : [testPort]}));
  }
}

/**
 * A default implementation for an enemy that sends messages to an isolate,
 * which contains the actual enemy state.
 */
class EnemyImpl implements Enemy {
  SendPort portToEnemy;

  EnemyImpl(this.portToEnemy) {}

  Future<int> ready() {
    Completer<int> res = new Completer<int>();
    return portToEnemy.call({ "action" : MessageIds.ENEMY_IS_READY })
      .transform((message) {
         if (!message[0]) throw message[1];
         return 0;
       });
  }

  Future<int> shoot(int x, int y) {
    Completer<int> res = new Completer<int>();
    return portToEnemy.call({ "action" : MessageIds.SHOOT, "args" : [x, y] })
      .transform((message) {
         if (!message[0]) throw message[1];
         return message[1][0];
       });
  }
}

/** Collection of message IDs used to communicate with player isolates. */
class MessageIds {
  /** message to set up a new player. */
  static final SETUP = 1;

  /** message to initialize the enemy of a player. */
  static final SET_ENEMY = 2;

  /** message indicating that the enemy is ready to play. */
  static final ENEMY_IS_READY = 3;

  /** message describing a shoot action. */
  static final SHOOT = 4;

  /** message to set up a test port, used to make tests non-flaky. */
  static final SET_PORT_FOR_TEST = 5;
}
