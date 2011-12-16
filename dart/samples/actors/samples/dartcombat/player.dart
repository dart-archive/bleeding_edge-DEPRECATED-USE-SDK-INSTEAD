// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/** A local player (API known to the main isolate when creating players). */
interface Player default PlayerImpl {
  Player();
}

/**
 * A default implementation for player that sends messages to an isolate, which
 * contains the actual player state.
 */
class PlayerImpl extends Actor implements Player {
  Future<ActorId> playerStateFuture;

  PlayerImpl() : super() {
    on["init"] = (int player, Reply rep) {
      Completer<ActorId> c = new Completer<ActorId>();
      create(const PlayerStateFactory(), "setup", 
          [player, messageback((ActorId state) {
            c.complete(state);
      })]);
      playerStateFuture = c.future;
      playerStateFuture.then((ActorId playerState) {
        rep.respond([playerState]);
      });
    };
    
    on["set-enemy"] = (ActorId enemy) {
      playerStateFuture.then((ActorId playerState) {
        playerState.send("set-enemy", [enemy]);
      });
    };
  }
}
