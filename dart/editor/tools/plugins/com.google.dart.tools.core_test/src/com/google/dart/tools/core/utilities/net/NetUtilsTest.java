/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.tools.core.utilities.net;

import junit.framework.TestCase;

import java.io.IOException;
import java.net.ServerSocket;

public class NetUtilsTest extends TestCase {

  public void test_getUnusedPort() {
    ServerSocket socket = null;
    try {
      socket = new ServerSocket(8080);
      int port = NetUtils.getUnusedPort(8080, 8100);
      assertEquals(8081, port);
    } catch (IOException e) {

    } finally {
      if (socket != null) {
        try {
          socket.close();
        } catch (IOException e) {

        }
      }
    }

  }
}
