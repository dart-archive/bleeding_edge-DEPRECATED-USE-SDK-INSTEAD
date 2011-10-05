/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.indexer.standard;

import com.google.dart.indexer.workspace.driver.WorkspaceIndexingDriver;

public class StandardDriver {
  private static WorkspaceIndexingDriver DRIVER = StandardIndexerFactory.createDriver();

  private static boolean testsRunning;

  public static synchronized WorkspaceIndexingDriver getInstance() {
    if (testsRunning) {
      throw new IllegalStateException("Standard workspace driver creation prohibited when testing");
    }
    if (DRIVER == null) {
      DRIVER = StandardIndexerFactory.createDriver();
    }
    return DRIVER;
  }

  public static synchronized void shutdown() {
    if (DRIVER != null) {
      DRIVER.shutdown();
    }
    DRIVER = null;
  }

  public static void shutdownForTests() {
    testsRunning = true;
    if (DRIVER != null) {
      if (!DRIVER.isShutdown()) {
        DRIVER.shutdown();
      }
      DRIVER = null;
    }
  }
}
