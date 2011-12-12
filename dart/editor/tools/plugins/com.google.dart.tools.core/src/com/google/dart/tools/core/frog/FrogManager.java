/*
 * Copyright 2011 Dart project authors.
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
package com.google.dart.tools.core.frog;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * Manages instances of {@link FrogServer}
 */
public class FrogManager {
  private static final Object lock = new Object();
  private static FrogServer server;

  /**
   * Answer the current server, instantiating a new one if necessary.
   * 
   * @return the server (not <code>null</code>)
   */
  public static FrogServer getServer() throws UnknownHostException, IOException {
    synchronized (lock) {
      if (server == null) {
        server = new FrogServer();
      }
    }
    return server;
  }

  /**
   * Dispose of the current server if there is one
   */
  public static void shutdown() {
    synchronized (lock) {
      if (server != null) {
        server.shutdown();
        server = null;
      }
    }
  }
}
