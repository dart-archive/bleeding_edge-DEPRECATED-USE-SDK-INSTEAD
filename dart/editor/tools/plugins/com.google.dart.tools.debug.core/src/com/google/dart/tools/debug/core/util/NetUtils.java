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
import java.net.ServerSocket;

/**
 * A collection of static networking utilities.
 */
public class NetUtils {

  /**
   * Find and return an unused server socket port. Attempt to use preferredPort; if that is not
   * available then we return any unused port.
   * 
   * @param preferredPort
   * @return
   */
  public static int findUnusedPort(int preferredPort) {
    try {
      ServerSocket ss = new ServerSocket(preferredPort);

      ss.close();

      return preferredPort;
    } catch (IOException ioe) {

    }

    try {
      // Bind to any free port.
      ServerSocket ss = new ServerSocket(0);

      int port = ss.getLocalPort();

      ss.close();

      return port;
    } catch (IOException ioe) {

    }

    return -1;
  }

  private NetUtils() {

  }

}
