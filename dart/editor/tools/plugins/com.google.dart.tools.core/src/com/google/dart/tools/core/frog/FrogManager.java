/*
 * Copyright 2012 Dart project authors.
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

import com.google.dart.tools.core.model.DartSdk;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;

/**
 * Manages instances of {@link FrogServer}
 */
public class FrogManager {
  public static final String LOCALHOST_ADDRESS = "127.0.0.1";
  public static final int AUTO_BIND_PORT = 0;
  public static final int DEFAULT_PORT = 1236;

  private static final Object lock = new Object();

  private static FrogProcess frogProcess;
  private static FrogServer server;

  /**
   * Answer the current server, instantiating a new one if necessary.
   * 
   * @return the server (not <code>null</code>)
   */
  public static FrogServer getServer() throws UnknownHostException, IOException {
    synchronized (lock) {
      if (frogProcess == null) {
        frogProcess = new FrogProcess();
        frogProcess.startProcess();
      }

      if (server == null) {
        server = new FrogServer(LOCALHOST_ADDRESS, frogProcess.getPort());
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
        try {
          server.shutdown();
        } catch (IOException ioe) {
          // We were not able to send the shutdown message - kill the process.
          if (frogProcess != null) {
            frogProcess.killProcess();
          }
        }

        server = null;
      }

      frogProcess = null;
    }
  }

  /**
   * Return the path to the dart executable, or null if the path has not been set.
   * 
   * @return the path to the dart executable, or null if the path has not been set
   */
  protected static String getDartVmExecutablePath() {
    DartSdk sdk = DartSdk.getInstance();

    if (sdk != null) {
      File vm = sdk.getVmExecutable();

      if (vm != null) {
        return vm.getPath();
      }
    }

    return null;
  }

}
