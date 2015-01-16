// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library operation.queue;

import 'dart:collection';

import 'package:analysis_server/src/analysis_server.dart';
import 'package:analysis_server/src/operation/operation.dart';


/**
 * A queue of operations in an [AnalysisServer].
 */
class ServerOperationQueue {
  final List<Queue<ServerOperation>> _queues = <Queue<ServerOperation>>[];

  ServerOperationQueue() {
    for (int i = 0; i < ServerOperationPriority.COUNT; i++) {
      var queue = new DoubleLinkedQueue<ServerOperation>();
      _queues.add(queue);
    }
  }

  /**
   * Returns `true` if there are no queued [ServerOperation]s.
   */
  bool get isEmpty {
    return _queues.every((queue) => queue.isEmpty);
  }

  /**
   * Adds the given operation to this queue. The exact position in the queue
   * depends on the priority of the given operation relative to the priorities
   * of the other operations in the queue.
   */
  void add(ServerOperation operation) {
    int queueIndex = operation.priority.ordinal;
    Queue<ServerOperation> queue = _queues[queueIndex];
    queue.addLast(operation);
  }

  /**
   * Removes all elements in the queue.
   */
  void clear() {
    for (Queue<ServerOperation> queue in _queues) {
      queue.clear();
    }
  }

  /**
   * Returns the next operation to perform or `null` if empty.
   */
  ServerOperation take() {
    for (Queue<ServerOperation> queue in _queues) {
      if (!queue.isEmpty) {
        return queue.removeFirst();
      }
    }
    return null;
  }
}
