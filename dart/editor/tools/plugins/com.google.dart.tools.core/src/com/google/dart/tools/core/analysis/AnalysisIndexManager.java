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

import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.UrlLibrarySource;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.index.impl.InMemoryIndex;
import com.google.dart.tools.core.internal.index.util.ResourceFactory;
import com.google.dart.tools.core.internal.model.CompilationUnitImpl;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.model.EditorLibraryManager;
import com.google.dart.tools.core.internal.model.ExternalCompilationUnitImpl;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.internal.workingcopy.DefaultWorkingCopyOwner;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Forwards resolved units to the indexServer for processing
 */
public class AnalysisIndexManager implements AnalysisListener {
  private final InMemoryIndex index;

  public AnalysisIndexManager() {
    index = InMemoryIndex.getInstance();
  }

  public AnalysisIndexManager(InMemoryIndex index) {
    this.index = index;
  }

  @Override
  public void idle(boolean idle) {
    // ignored
  }

  @Override
  public void parsed(AnalysisEvent event) {
  }

  /**
   * Forward all resolved {@link DartUnit}s to the indexer for processing
   */
  @Override
  public void resolved(AnalysisEvent event) {
    updateNewIndex(event);
  }

  private void updateNewIndex(AnalysisEvent event) {
    EditorLibraryManager libraryManager = SystemLibraryManagerProvider.getSystemLibraryManager();
    File libraryFile = event.getLibraryFile();
    HashMap<File, DartUnit> units = event.getUnits();

    // Get the LibrarySource

    DartUnit dartUnit = units.get(libraryFile);
    if (dartUnit == null) {
      DartCore.logError("No compilation unit associated with " + libraryFile);
      return;
    }
    LibrarySource librarySource = dartUnit.getLibrary().getSource();

    // AnalysisServer is entirely based on java.io.File
    // thus we must map file references to system libraries back to dart:<libname> URIs

    URI shortUri = libraryManager.getShortUri(librarySource.getUri());
    if (shortUri != null) {
      librarySource = new UrlLibrarySource(shortUri, libraryManager);
    }

    // Get the DartLibrary

    DartLibraryImpl library;
    IResource resource = ResourceUtil.getResource(libraryFile);
    if (resource == null) {
      library = new DartLibraryImpl(librarySource);
    } else {
      DartElement element = DartCore.create(resource);
      if (element instanceof CompilationUnitImpl) {
        element = ((CompilationUnitImpl) element).getLibrary();
      }
      if (!(element instanceof DartLibrary)) {
        DartCore.logError("Expected library to be associated with " + libraryFile);
        return;
      }
      library = (DartLibraryImpl) element;
    }

    // Index each compilation unit in the library

    for (Entry<File, DartUnit> entry : units.entrySet()) {
      File sourceFile = entry.getKey();
      dartUnit = entry.getValue();

      CompilationUnit compilationUnit;
      IResource res = ResourceUtil.getResource(sourceFile);
      if (res == null) {
        DartSource unitSource = (DartSource) dartUnit.getSourceInfo().getSource();
        String relPath = unitSource.getRelativePath();
        compilationUnit = new ExternalCompilationUnitImpl(library, relPath, unitSource);
      } else {
        compilationUnit = new CompilationUnitImpl(
            library,
            (IFile) res,
            DefaultWorkingCopyOwner.getInstance());
      }

      try {
        index.indexResource(ResourceFactory.getResource(compilationUnit), compilationUnit, dartUnit);
      } catch (Exception exception) {
        DartCore.logError(
            "Could not index \"" + sourceFile + "\" in \"" + libraryFile + "\"",
            exception);
      }
    }
  }
}
