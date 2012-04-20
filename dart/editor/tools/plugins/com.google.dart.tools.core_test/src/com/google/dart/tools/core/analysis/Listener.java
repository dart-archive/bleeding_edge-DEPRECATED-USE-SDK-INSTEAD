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
package com.google.dart.tools.core.analysis;

import com.google.dart.tools.core.internal.model.EditorLibraryManager;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;

class Listener implements AnalysisListener {
  private static final String LINE_SEPARATOR = System.getProperty("line.separator");

  private final AnalysisServer server;
  private final Object waitForIdleLock = new Object();
  private final HashSet<String> parsed = new HashSet<String>();
  private final HashSet<String> resolved = new HashSet<String>();
  private final StringBuilder duplicates = new StringBuilder();

  public Listener(AnalysisServer server) {
    this.server = server;
    server.addAnalysisListener(this);
  }

  @Override
  public void idle(boolean idle) {
    if (idle) {
      synchronized (waitForIdleLock) {
        waitForIdleLock.notifyAll();
      }
    }
  }

  @Override
  public void parsed(AnalysisEvent event) {
    for (File file : event.getFiles()) {
      parsed.add(file.getPath());
    }
  }

  @Override
  public void resolved(AnalysisEvent event) {
    String libPath = event.getLibraryFile().getPath();
    if (!resolved.add(libPath)) {
      if (duplicates.length() == 0) {
        duplicates.append("Duplicate library resolutions:");
      }
      duplicates.append(LINE_SEPARATOR);
      duplicates.append(libPath);
    }
  }

  void assertBundledLibrariesResolved() throws Exception {
    ArrayList<String> notResolved = new ArrayList<String>();
    EditorLibraryManager libraryManager = SystemLibraryManagerProvider.getAnyLibraryManager();
    ArrayList<String> librarySpecs = new ArrayList<String>(libraryManager.getAllLibrarySpecs());
    for (String urlSpec : librarySpecs) {
      URI libraryUri = new URI(urlSpec);
      File libraryFile = new File(libraryManager.resolveDartUri(libraryUri));
      String libraryPath = libraryFile.getPath();
      if (!resolved.contains(libraryPath)) {
        notResolved.add(libraryPath);
      }
    }
    if (notResolved.size() > 0) {
      AnalysisServerTest.fail("Expected these libraries to be resolved: " + notResolved);
    }
  }

  void assertNoDuplicates() {
    if (duplicates.length() > 0) {
      AnalysisServerTest.fail(duplicates.toString());
    }
  }

  HashSet<String> getParsed() {
    return parsed;
  }

  HashSet<String> getResolved() {
    return resolved;
  }

  void reset() {
    resolved.clear();
    duplicates.setLength(0);
  }

  /**
   * Wait for up to the specified number of milliseconds for the analysis server associated with the
   * receiver to be idle. If the specified number is less than or equal to zero, then this method
   * returns immediately
   * 
   * @param millis the number of milliseconds to wait
   * @return <code>true</code> if the server is idle
   */
  boolean waitForIdle(long millis) {
    long endTime = System.currentTimeMillis() + millis;
    synchronized (waitForIdleLock) {
      while (!server.isIdle()) {
        long delta = endTime - System.currentTimeMillis();
        if (delta <= 0) {
          return false;
        }
        try {
          waitForIdleLock.wait(delta);
        } catch (InterruptedException e) {
          //$FALL-THROUGH$
        }
      }
    }
    return true;
  }
}
