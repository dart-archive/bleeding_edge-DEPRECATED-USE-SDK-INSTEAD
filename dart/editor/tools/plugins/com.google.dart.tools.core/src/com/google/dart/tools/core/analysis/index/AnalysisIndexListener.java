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
package com.google.dart.tools.core.analysis.index;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.AnalysisEvent;
import com.google.dart.tools.core.analysis.AnalysisListener;
import com.google.dart.tools.core.analysis.TaskListener;
import com.google.dart.tools.core.internal.index.impl.InMemoryIndex;

import java.io.File;
import java.util.Map.Entry;

/**
 * Forwards resolved units to the indexServer for processing
 */
class AnalysisIndexListener implements AnalysisListener, TaskListener {
  private final InMemoryIndex index;

  public AnalysisIndexListener() {
    index = InMemoryIndex.getInstance();
  }

  public AnalysisIndexListener(InMemoryIndex index) {
    this.index = index;
  }

  @Override
  public void discarded(AnalysisEvent event) {
    File libraryFile = event.getLibraryFile();
    index.removeResource(libraryFile, libraryFile);
    for (File sourceFile : event.getFiles()) {
      index.removeResource(libraryFile, sourceFile);
    }
  }

  @Override
  public void idle(boolean idle) {
    index.setProcessQueries(idle);
  }

  @Override
  public void parsed(AnalysisEvent event) {
    // ignored
  }

  @Override
  public void processing(int toBeProcessed) {
    // ignored
  }

  /**
   * Forward all resolved {@link DartUnit}s to the indexer for processing
   */
  @Override
  public void resolved(AnalysisEvent event) {
    File libraryFile = event.getLibraryFile();
    for (Entry<File, DartUnit> entry : event.getUnits().entrySet()) {
      File sourceFile = entry.getKey();
      DartUnit dartUnit = entry.getValue();
      try {
        index.indexResource(libraryFile, sourceFile, dartUnit);
      } catch (Exception exception) {
        DartCore.logError(
            "Could not index \"" + sourceFile + "\" in \"" + libraryFile + "\"",
            exception);
      }
    }
  }
}
