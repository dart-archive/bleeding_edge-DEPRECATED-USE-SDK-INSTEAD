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
package com.google.dart.tools.core.index;

import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.UrlLibrarySource;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.LibraryUnit;
import com.google.dart.indexer.exceptions.IndexRequestFailed;
import com.google.dart.indexer.index.updating.LayerUpdater;
import com.google.dart.indexer.index.updating.LocationUpdater;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.index.contributor.IndexContributor;
import com.google.dart.tools.core.internal.index.persistance.IndexReader;
import com.google.dart.tools.core.internal.index.persistance.IndexWriter;
import com.google.dart.tools.core.internal.index.store.IndexStore;
import com.google.dart.tools.core.internal.indexer.contributor.DartContributor;
import com.google.dart.tools.core.internal.indexer.contributor.ElementsByCategoryContributor;
import com.google.dart.tools.core.internal.indexer.contributor.FieldAccessContributor;
import com.google.dart.tools.core.internal.indexer.contributor.MethodInvocationContributor;
import com.google.dart.tools.core.internal.indexer.contributor.MethodOverrideContributor;
import com.google.dart.tools.core.internal.indexer.contributor.TypeHierarchyContributor;
import com.google.dart.tools.core.internal.indexer.contributor.TypeReferencesContributor;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;

import junit.framework.TestCase;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;

public class TimingTest extends TestCase {
  private static class MockIndexStore extends IndexStore {
    @Override
    public IndexReader createIndexReader() {
      return null;
    }

    @Override
    public IndexWriter createIndexWriter() {
      return null;
    }

    @Override
    public String getAttribute(Element element, Attribute attribute) {
      return null;
    }

    @Override
    public Location[] getRelationships(Element element, Relationship relationship) {
      return null;
    }

    @Override
    public void recordAttribute(Element element, Attribute attribute, String value) {
    }

    @Override
    public void recordRelationship(Resource contributor, Element element,
        Relationship relationship, Location location) {
    }

    @Override
    public void regenerateResource(Resource resource) {
    }

    @Override
    public void removeResource(Resource resource) {
    }
  }

  private static class MockLayerUpdater implements LayerUpdater {
    @Override
    public LocationUpdater startLocation(com.google.dart.indexer.locations.Location location)
        throws IndexRequestFailed {
      return new MockLocationUpdater();
    }
  }

  private static class MockLocationUpdater implements LocationUpdater {
    @Override
    public com.google.dart.indexer.locations.Location getSourceLocation() {
      return null;
    }

    @Override
    public void hasReferenceTo(com.google.dart.indexer.locations.Location location)
        throws IndexRequestFailed {
    }
  }

  public void test_timing() throws Exception {
    final File libraryFile = new File(
        "/Users/brianwilkerson/src/dart-public/dart/frog/leg/leg.dart");
    ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        DartCore.openLibrary(libraryFile, monitor);
      }
    }, null);

    URI libraryUri = libraryFile.toURI();
    UrlLibrarySource librarySource = new UrlLibrarySource(libraryUri);
    ArrayList<DartUnit> contributedUnits = new ArrayList<DartUnit>();
    ArrayList<DartCompilationError> parseErrors = new ArrayList<DartCompilationError>();
    LibraryUnit libraryUnit = DartCompilerUtilities.resolveLibrary(librarySource, contributedUnits,
        parseErrors);

    HashSet<URI> initializedLibraries = new HashSet<URI>();
    long oldTime = oldContributor(new MockLayerUpdater(), libraryUnit, initializedLibraries);

    initializedLibraries = new HashSet<URI>();
    long newTime = newContributor(new MockIndexStore(), libraryUnit, initializedLibraries);

    DartCore.logInformation("old: " + oldTime + " ms; new: " + newTime + " ms");
  }

  /**
   * Run a new contributor over each of the compilation units in the given library unit and all
   * imported libraries (other than system defined libraries).
   * 
   * @param index the index to which the contributor will contribute
   * @param libraryUnit the library to be contributed to the index
   * @param initializedLibraries the URI's of libraries that have already been contributed to the
   *          index
   * @return the number of milliseconds required to contribute all of the units
   */
  private long newContributor(IndexStore index, LibraryUnit libraryUnit,
      HashSet<URI> initializedLibraries) {
    long totalTime = 0L;
    LibrarySource librarySource = libraryUnit.getSource();
    URI libraryUri = librarySource.getUri();
    if (initializedLibraries.contains(libraryUri)) {
      return totalTime;
    }
    initializedLibraries.add(libraryUri);
    DartLibraryImpl library = new DartLibraryImpl(librarySource);
    for (DartUnit ast : libraryUnit.getUnits()) {
      DartSource unitSource = ast.getSource();
      URI unitUri = unitSource.getUri();
      CompilationUnit compilationUnit = library.getCompilationUnit(unitUri);
      IndexContributor contributor = new IndexContributor(index, compilationUnit);
      long startTime = System.currentTimeMillis();
      ast.accept(contributor);
      long endTime = System.currentTimeMillis();
      totalTime += (endTime - startTime);
    }
    for (LibraryUnit importedLibrary : libraryUnit.getImports()) {
      if (!importedLibrary.getSource().getUri().getScheme().equals("dart")) {
        totalTime += newContributor(index, importedLibrary, initializedLibraries);
      }
    }
    return totalTime;
  }

  /**
   * Run a new contributor over each of the compilation units in the given library unit and all
   * imported libraries (other than system defined libraries).
   * 
   * @param index the index to which the contributor will contribute
   * @param libraryUnit the library to be contributed to the index
   * @param initializedLibraries the URI's of libraries that have already been contributed to the
   *          index
   * @return the number of milliseconds required to contribute all of the units
   */
  private long oldContributor(LayerUpdater layerUpdater, LibraryUnit libraryUnit,
      HashSet<URI> initializedLibraries) {
    long totalTime = 0L;
    LibrarySource librarySource = libraryUnit.getSource();
    URI libraryUri = librarySource.getUri();
    if (initializedLibraries.contains(libraryUri)) {
      return totalTime;
    }
    initializedLibraries.add(libraryUri);
    DartLibraryImpl library = new DartLibraryImpl(librarySource);
    for (DartUnit ast : libraryUnit.getUnits()) {
      DartSource unitSource = ast.getSource();
      URI unitUri = unitSource.getUri();
      CompilationUnit compilationUnit = library.getCompilationUnit(unitUri);
      DartContributor[] contributors = {
          new ElementsByCategoryContributor(), new FieldAccessContributor(),
          new MethodInvocationContributor(), new MethodOverrideContributor(),
          new TypeHierarchyContributor(), new TypeReferencesContributor(),};
      for (DartContributor contributor : contributors) {
        long startTime = System.currentTimeMillis();
        contributor.initialize(compilationUnit, layerUpdater);
        ast.accept(contributor);
        long endTime = System.currentTimeMillis();
        totalTime += (endTime - startTime);
      }
    }
    for (LibraryUnit importedLibrary : libraryUnit.getImports()) {
      if (!importedLibrary.getSource().getUri().getScheme().equals("dart")) {
        totalTime += oldContributor(layerUpdater, importedLibrary, initializedLibraries);
      }
    }
    return totalTime;
  }
}
