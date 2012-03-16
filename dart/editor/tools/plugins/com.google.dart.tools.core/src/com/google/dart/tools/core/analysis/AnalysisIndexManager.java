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
package com.google.dart.tools.core.analysis;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.indexer.standard.StandardDriver;
import com.google.dart.indexer.workspace.driver.WorkspaceIndexingDriver;
import com.google.dart.indexer.workspace.index.IndexingTarget;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.internal.index.impl.InMemoryIndex;
import com.google.dart.tools.core.internal.index.util.ResourceFactory;
import com.google.dart.tools.core.internal.indexer.task.CompilationUnitIndexingTarget;
import com.google.dart.tools.core.internal.model.CompilationUnitImpl;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;

import org.eclipse.core.resources.IResource;

import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Forwards resolved units to the indexServer for processing
 */
public class AnalysisIndexManager implements AnalysisListener {
  private final InMemoryIndex index;

  private final WorkspaceIndexingDriver indexServer;

  public AnalysisIndexManager() {
    if (DartCoreDebug.NEW_INDEXER) {
      index = InMemoryIndex.getInstance();
      indexServer = null;
    } else {
      index = null;
      indexServer = StandardDriver.getInstance();
    }
  }

  @Override
  public void parsed(AnalysisEvent event) {
  }

  /**
   * Forward all resolved {@link DartUnit}s to the indexer for processing
   */
  @Override
  public void resolved(AnalysisEvent event) {
    File libraryFile = event.getLibraryFile();
    HashMap<File, DartUnit> units = event.getUnits();
    IResource[] resources = ResourceUtil.getResources(libraryFile);
    if (resources == null || resources.length != 1) {
      DartCore.logError("Could not find compilation unit corresponding to " + libraryFile + " ("
          + (resources == null ? "no" : resources.length) + " files found)");
      return;
    }
    DartElement element = DartCore.create(resources[0]);
    if (element instanceof CompilationUnitImpl) {
      element = ((CompilationUnitImpl) element).getLibrary();
    }
    if (!(element instanceof DartLibrary)) {
      DartCore.logError("Expected library to be associated with " + libraryFile);
      return;
    }
    DartLibrary library = (DartLibrary) element;
    Set<Entry<File, DartUnit>> entries = units.entrySet();
    if (DartCoreDebug.NEW_INDEXER) {
      for (Entry<File, DartUnit> entry : entries) {
        File sourceFile = entry.getKey();
        CompilationUnit compilationUnit = library.getCompilationUnit(sourceFile.toURI());
        if (compilationUnit == null) {
          DartCore.logError(
              "Expected unit associated with \"" + sourceFile + "\" in \"" + libraryFile + "\"");
        } else {
          DartUnit dartUnit = entry.getValue();
          try {
            index.indexResource(
                ResourceFactory.getResource(compilationUnit), compilationUnit, dartUnit);
          } catch (Exception exception) {
            DartCore.logError(
                "Could not index \"" + sourceFile + "\" in \"" + libraryFile + "\"", exception);
          }
        }
      }
    } else {
      IndexingTarget[] indexTargets = new IndexingTarget[entries.size()];
      int index = 0;
      for (Entry<File, DartUnit> entry : entries) {
        File sourceFile = entry.getKey();
        CompilationUnit compilationUnit = library.getCompilationUnit(sourceFile.toURI());
        if (compilationUnit == null) {
          DartCore.logError(
              "Expected unit associated with " + sourceFile + "\n  in " + libraryFile);
          continue;
        }
        DartUnit dartUnit = entry.getValue();
        indexTargets[index++] = new CompilationUnitIndexingTarget(compilationUnit, dartUnit);
      }
      indexServer.enqueueTargets(indexTargets);
    }
  }
}
