// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// TODO(sigmund): separate isolates from dart:core
// #library('dart:isolate');

/**
 * [Isolate2] provides APIs to spawn, communicate, and stop an isolate. An
 * isolate can be spawned by simply creating a new instance of [Isolate2]. The
 * [Isolate2] instance exposes a port to communicate with the isolate and
 * methods to control its behavior remotely.
 */
 // TODO(sigmund): rename to Isolate once we delete the old implementation
interface Isolate2 default IsolateFactory {

  /**
   * Create and spawn an isolate that shares the same code as the current
   * isolate, but that starts from [topLevelFunction]. The [topLevelFunction]
   * argument must be a static method closure that takes exactly one
   * argument of type [ReceivePort]. It is illegal to pass a function closure
   * that captures values in scope.
   *
   * When an child isolate is spawned, a new [ReceivePort] is created for it.
   * This port is passed to [topLevelFunction]. A [SendPort] derived from
   * such port is sent to the spawner isolate, which is accessible in
   * [Isolate2.sendPort] field of this instance.
   */
  Isolate2.fromCode(Function topLevelFunction);

  /**
   * Create and spawn an isolate whose code is available at [uri].
   * The code in [uri] must have an method called [: isolateMain :], which takes
   * exactly one argument of type [ReceivePort].
   * Like with [Isolate2.fromCode], a [ReceivePort] is created in the child
   * isolate, and a [SendPort] to it is stored in [Isolate2.sendPort].
   */
  Isolate2.fromUri(String uri);

  /** Port used to communicate with this isolate. */
  SendPort sendPort;

  /** Stop this isolate. */
  void stop();
}

