/*
 * Copyright (c) 2015, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.server.internal.remote;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

import java.util.LinkedList;

/**
 * A {@link RequestSink} that enqueues requests only it is open.
 * 
 * @coverage dart.server.remote
 */
public class BlockRequestSink implements RequestSink {
  /**
   * The base {@link RequestSink}
   */
  private final RequestSink base;

  /**
   * A queue of requests.
   */
  private final LinkedList<JsonObject> queue = Lists.newLinkedList();

  private boolean blocked = false;

  public BlockRequestSink(RequestSink base) {
    this.base = base;
  }

  @Override
  public void add(JsonObject request) {
    synchronized (queue) {
      if (blocked) {
        queue.add(request);
      } else {
        base.add(request);
      }
    }
  }

  /**
   * Block this sink and starts queuing requests.
   */
  public void block() {
    if (blocked) {
      throw new IllegalStateException("The lock is already blocked.");
    }
    synchronized (queue) {
      blocked = true;
    }
  }

  @Override
  public void close() {
    base.close();
  }

  /**
   * Unblock this sink and send all the queued requests.
   */
  public void unblock() {
    if (!blocked) {
      throw new IllegalStateException("The lock is already unblocked.");
    }
    synchronized (queue) {
      blocked = false;
      for (JsonObject request : queue) {
        add(request);
      }
      queue.clear();
    }
  }
}
