/*
 * Copyright (c) 2012, the Dart project authors.
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

package com.google.dart.tools.debug.core.util;

import java.io.IOException;

/**
 * Manages an instance of a ResourceServer.
 * 
 * @see ResourceServer
 */
public class ResourceServerManager {
  public static final int PREFERRED_PORT = 3030;

  private static ResourceServer server;

  public static ResourceServer getServer() throws IOException {
    if (server == null) {
      try {
        // First try and start the server up on our preferred port.
        server = new ResourceServer(PREFERRED_PORT);
      } catch (IOException exception) {
        // Next let it try and auto-bind to an available port.
        server = new ResourceServer();
      }
    }

    return server;
  }

  /**
   * If a server was started, shut it down.
   */
  public static void shutdown() {
    if (server != null) {
      server.shutdown();
      server = null;
    }
  }

}
