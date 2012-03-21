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
package com.google.dart.tools.core.internal.index.impl;

import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.SystemLibraryManager;
import com.google.dart.compiler.UrlLibrarySource;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.LibraryUnit;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.analysis.AnalysisServer;
import com.google.dart.tools.core.index.Attribute;
import com.google.dart.tools.core.index.AttributeCallback;
import com.google.dart.tools.core.index.Element;
import com.google.dart.tools.core.index.Index;
import com.google.dart.tools.core.index.Relationship;
import com.google.dart.tools.core.index.RelationshipCallback;
import com.google.dart.tools.core.index.Resource;
import com.google.dart.tools.core.internal.index.operation.GetAttributeOperation;
import com.google.dart.tools.core.internal.index.operation.GetRelationshipsOperation;
import com.google.dart.tools.core.internal.index.operation.IndexResourceOperation;
import com.google.dart.tools.core.internal.index.operation.OperationProcessor;
import com.google.dart.tools.core.internal.index.operation.OperationQueue;
import com.google.dart.tools.core.internal.index.operation.RemoveResourceOperation;
import com.google.dart.tools.core.internal.index.persistance.IndexReader;
import com.google.dart.tools.core.internal.index.persistance.IndexWriter;
import com.google.dart.tools.core.internal.index.store.IndexStore;
import com.google.dart.tools.core.internal.model.CompilationUnitImpl;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.model.EditorLibraryManager;
import com.google.dart.tools.core.internal.model.ExternalCompilationUnitImpl;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;
import com.google.dart.tools.core.internal.workingcopy.DefaultWorkingCopyOwner;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModel;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;

import org.eclipse.core.resources.ResourcesPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The unique instance of the class <code>InMemoryIndex</code> maintains an in-memory {@link Index
 * index}. The index is expected to be initialized once before it is used in any given session and
 * shut down at the end of the session. The index will be read from disk when it is initialized and
 * written to disk when it is shut down.
 */
public class InMemoryIndex implements Index {
  /**
   * The index store used to hold the data in the index.
   */
  private IndexStore indexStore = new IndexStore();

  /**
   * The queue containing the operations to be processed.
   */
  private OperationQueue queue = new OperationQueue();

  /**
   * The object used to process operations that have been added to the queue.
   */
  private OperationProcessor processor = new OperationProcessor(queue);

  /**
   * A flag indicating whether the content of the index store has been initialized.
   */
  private boolean hasBeenInitialized = false;

  /**
   * The object used to record performance information about the index, or <code>null</code> if
   * performance information is not suppose to be recorded.
   */
  private IndexPerformanceRecorder performanceRecorder;

  /**
   * The unique instance of this class.
   */
  private static final InMemoryIndex UniqueInstance = new InMemoryIndex();

  /**
   * The name of the file containing the index.
   */
  private static final String INDEX_FILE = "index.idx";

  /**
   * The name of the file containing the initial state of the index.
   */
  private static final String INITIAL_INDEX_FILE = "initial_index.idx";

  /**
   * Return the unique instance of this class.
   * 
   * @return the unique instance of this class
   */
  public static InMemoryIndex getInstance() {
    return UniqueInstance;
  }

  private long initIndexingTime = 0L;

  /**
   * Initialize a newly created index.
   */
  private InMemoryIndex() {
    if (DartCoreDebug.PERF_INDEX) {
      performanceRecorder = new IndexPerformanceRecorder();
    }
  }

  /**
   * Asynchronously invoke the given callback with the value of the given attribute that is
   * associated with the given element, or <code>null</code> if there is no value for the attribute.
   * 
   * @param element the element with which the attribute is associated
   * @param attribute the attribute whose value is to be returned
   * @param callback the callback that will be invoked when the attribute value is available
   */
  @Override
  public void getAttribute(Element element, Attribute attribute, AttributeCallback callback) {
    queue.enqueue(new GetAttributeOperation(indexStore, element, attribute, callback));
  }

  /**
   * Return the object used to process operations that have been added to the queue.
   * 
   * @return the object used to process operations that have been added to the queue
   */
  public OperationProcessor getOperationProcessor() {
    return processor;
  }

  /**
   * Return the number of relationships that are currently recorded in this index.
   * 
   * @return the number of relationships that are currently recorded in this index
   */
  public int getRelationshipCount() {
    synchronized (indexStore) {
      return indexStore.getRelationshipCount();
    }
  }

  /**
   * Asynchronously invoke the given callback with an array containing all of the locations of the
   * elements that have the given relationship with the given element. For example, if the element
   * represents a method and the relationship is the is-referenced-by relationship, then the
   * locations that will be passed into the callback will be all of the places where the method is
   * invoked.
   * 
   * @param element the element that has the relationship with the locations to be returned
   * @param relationship the relationship between the given element and the locations to be returned
   * @param callback the callback that will be invoked when the locations are found
   */
  @Override
  public void getRelationships(Element element, Relationship relationship,
      RelationshipCallback callback) {
    queue.enqueue(new GetRelationshipsOperation(indexStore, element, relationship, callback));
  }

  /**
   * Process the given resource within the context of the given working set in order to record the
   * data and relationships found within the resource.
   * 
   * @param resource the resource containing the elements defined in the compilation unit
   * @param compilationUnit the compilation unit being indexed
   * @param unit the compilation unit to be indexed
   */
  @Override
  public void indexResource(Resource resource, CompilationUnit compilationUnit, DartUnit unit) {
    queue.enqueue(new IndexResourceOperation(indexStore, resource, compilationUnit, unit,
        performanceRecorder));
  }

  /**
   * Initialize this index, assuming that it has not already been initialized.
   */
  public void initializeIndex() {
    synchronized (indexStore) {
      if (hasBeenInitialized) {
        return;
      }
      hasBeenInitialized = true;
      indexStore.clear();
      if (!initializeIndexFrom(getIndexFile())) {
        indexStore.clear();
        if (!initializeIndexFrom(getInitialIndexFile())) {
          indexStore.clear();
          if (!indexBundledLibraries()) {
            indexStore.clear();
            return;
          }
          writeIndexTo(getInitialIndexFile());
        }
        if (!indexUserLibraries()) {
          indexStore.clear();
          initializeIndexFrom(getInitialIndexFile());
        }
      }
    }
  }

  /**
   * Remove from the index all of the information associated with elements or locations in the given
   * resource. This includes relationships between an element in the given resource and any other
   * locations, relationships between any other elements and a location within the given resource,
   * and any values of any attributes defined on elements in the given resource.
   * <p>
   * This method should be invoked when a resource is no longer part of the code base and when the
   * information about the resource is about to be re-generated.
   * 
   * @param resource the resource being removed
   */
  @Override
  public void removeResource(Resource resource) {
    queue.enqueue(new RemoveResourceOperation(indexStore, resource));
  }

  /**
   * Report the number of milliseconds spent indexing since the last time this value was reported
   * and cleared, clearing the value after reporting it.
   */
  public void reportAndResetIndexingTime() {
    if (performanceRecorder != null) {
      DartCore.logInformation("Indexed " + performanceRecorder.getResourceCount()
          + " resources in " + performanceRecorder.getTotalIndexTime() + " ms ["
          + performanceRecorder.getTotalBindingTime() + " ms in binding]");
      performanceRecorder.clear();
    }
  }

  public void shutdown() {
    synchronized (indexStore) {
      if (hasBeenInitialized) {
        writeIndexTo(getIndexFile());
      }
    }
  }

  /**
   * Return the file in which the state of the index is to be stored between sessions.
   * 
   * @return the file in which the state of the index is to be stored
   */
  private File getIndexFile() {
    return new File(DartCore.getPlugin().getStateLocation().toFile(), INDEX_FILE);
  }

//  /**
//   * Return the index file with the given name.
//   * 
//   * @param fileName the name of the index file to be returned
//   * @return the index file with the given name
//   */
//  private File getIndexFile(String fileName) {
//    return new File(DartCore.getPlugin().getStateLocation().toFile(), fileName);
//  }

  /**
   * Return the file containing the initial state of the index. This file should be considered to be
   * read-only and should be loaded only if the normal index file does not yet exist. The initial
   * state of the index includes all of the information from the bundled libraries, but does not
   * include any information from loaded libraries.
   * 
   * @return the file containing the initial state of the index
   */
  private File getInitialIndexFile() {
    //DartCore.getPlugin().getBundle().getResource(INITIAL_INDEX_FILE).openStream();
    return new File(DartCore.getPlugin().getStateLocation().toFile(), INITIAL_INDEX_FILE);
  }

  /**
   * Initialize this index with information from the bundled libraries.
   * 
   * @return <code>true</code> if the bundled libraries were successfully indexed
   */
  private boolean indexBundledLibraries() {
    long startTime = System.currentTimeMillis();
    boolean librariesIndexed;
    if (DartCoreDebug.ANALYSIS_SERVER) {
      librariesIndexed = indexBundledLibraries2();
    } else {
      librariesIndexed = indexBundledLibraries1();
    }
    if (DartCoreDebug.PERF_INDEX) {
      long endTime = System.currentTimeMillis();
      DartCore.logInformation("Initializing the index with information from bundled libraries took "
          + (endTime - startTime) + " ms (" + initIndexingTime + " ms indexing)");
    }
    return librariesIndexed;
  }

  /**
   * Initialize this index with information from the bundled libraries using
   * {@link DartCompilerUtilities#resolveLibrary(DartLibraryImpl, java.util.Collection, java.util.Collection)}
   * 
   * @return <code>true</code> if the bundled libraries were successfully indexed
   */
  private boolean indexBundledLibraries1() {
    boolean librariesIndexed = true;
    EditorLibraryManager libraryManager = SystemLibraryManagerProvider.getSystemLibraryManager();
    ArrayList<String> librarySpecs = new ArrayList<String>(libraryManager.getAllLibrarySpecs());
    if (librarySpecs.remove("dart:html")) {
      librarySpecs.add(0, "dart:html");
    }
    ArrayList<DartUnit> contributedUnits = new ArrayList<DartUnit>();
    ArrayList<DartCompilationError> parseErrors = new ArrayList<DartCompilationError>();
    HashSet<URI> initializedLibraries = new HashSet<URI>();
    for (String urlSpec : librarySpecs) {
      try {
        URI libraryUri = new URI(urlSpec);
        UrlLibrarySource librarySource = new UrlLibrarySource(libraryUri, libraryManager);
        LibraryUnit libraryUnit = DartCompilerUtilities.resolveLibrary(librarySource,
            contributedUnits, parseErrors);
        indexBundledLibrary(libraryUnit, initializedLibraries);
      } catch (URISyntaxException exception) {
        librariesIndexed = false;
        DartCore.logError("Invalid URI returned from the system library manager: \"" + urlSpec
            + "\"", exception);
      } catch (DartModelException exception) {
        librariesIndexed = false;
        DartCore.logError("Could not resolve bundled library: \"" + urlSpec + "\"", exception);
      } catch (Exception exception) {
        librariesIndexed = false;
        DartCore.logError("Could not index bundled libraries", exception);
      }
    }
    return librariesIndexed;
  }

  /**
   * Initialize this index with information from the bundled libraries using the
   * {@link AnalysisServer}.
   * 
   * @return <code>true</code> if the bundled libraries were successfully indexed
   */
  private boolean indexBundledLibraries2() {
    AnalysisServer server = SystemLibraryManagerProvider.getDefaultAnalysisServer();
    CountDownLatch latch = server.analyzeBundledLibraries();
    boolean librariesIndexed;
    try {
      librariesIndexed = latch.await(30, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      librariesIndexed = false;
    }
    return librariesIndexed;
  }

  /**
   * Initialize this index with information from the given bundled library.
   * 
   * @param libraryUnit the library to be used to initialize the index
   * @param initializedLibraries the URI's of libraries that have already been used to initialize
   *          the index
   */
  private void indexBundledLibrary(LibraryUnit libraryUnit, HashSet<URI> initializedLibraries) {
    LibrarySource librarySource = libraryUnit.getSource();
    URI libraryUri = librarySource.getUri();
    if (initializedLibraries.contains(libraryUri)) {
      return;
    }
    initializedLibraries.add(libraryUri);
    DartLibraryImpl library = new DartLibraryImpl(librarySource);
    for (DartUnit ast : libraryUnit.getUnits()) {
      DartSource unitSource = (DartSource) ast.getSourceInfo().getSource();
      URI unitUri = unitSource.getUri();
      Resource resource = new Resource(unitUri.toString());
      String relativePath = unitSource.getRelativePath();
      CompilationUnit compilationUnit = new ExternalCompilationUnitImpl(library, relativePath,
          librarySource.getSourceFor(relativePath));
      long startTime = System.currentTimeMillis();
      indexResource(resource, compilationUnit, ast);
      long endTime = System.currentTimeMillis();
      initIndexingTime += endTime - startTime;
    }
    for (LibraryUnit importedLibrary : libraryUnit.getImports()) {
      indexBundledLibrary(importedLibrary, initializedLibraries);
    }
  }

  /**
   * Initialize this index with information from the user libraries.
   */
  private boolean indexUserLibraries() {
    boolean librariesIndexed = true;
    try {
      ArrayList<DartCompilationError> parseErrors = new ArrayList<DartCompilationError>();
      HashSet<URI> initializedLibraries = new HashSet<URI>();
      DartModel model = DartCore.create(ResourcesPlugin.getWorkspace().getRoot());
      for (DartProject project : model.getDartProjects()) {
        for (DartLibrary library : project.getDartLibraries()) {
          LibraryUnit libraryUnit = DartCompilerUtilities.resolveLibrary((DartLibraryImpl) library,
              true, parseErrors);
          // If AnalysisServer is active, then indexer receives resolved units via listener
          if (!DartCoreDebug.ANALYSIS_SERVER) {
            indexUserLibrary(libraryUnit, initializedLibraries);
          }
        }
      }
    } catch (Exception exception) {
      librariesIndexed = false;
      DartCore.logError("Could not index user libraries", exception);
    }
    return librariesIndexed;
  }

  /**
   * Initialize this index with information from the given user library.
   * 
   * @param libraryUnit the library to be used to initialize the index
   * @param initializedLibraries the URI's of libraries that have already been used to initialize
   *          the index
   */
  private void indexUserLibrary(LibraryUnit libraryUnit, HashSet<URI> initializedLibraries) {
    LibrarySource librarySource = libraryUnit.getSource();
    URI libraryUri = librarySource.getUri();
    if (SystemLibraryManager.isDartUri(libraryUri) || initializedLibraries.contains(libraryUri)) {
      return;
    }
    initializedLibraries.add(libraryUri);
    DartLibraryImpl library = new DartLibraryImpl(librarySource);
    for (DartUnit ast : libraryUnit.getUnits()) {
      DartSource unitSource = (DartSource) ast.getSourceInfo().getSource();
      URI unitUri = unitSource.getUri();
      Resource resource = new Resource(unitUri.toString());
      CompilationUnit compilationUnit = new CompilationUnitImpl(library, unitUri,
          DefaultWorkingCopyOwner.getInstance());
      // library.getCompilationUnit(unitUri);
      long startTime = System.currentTimeMillis();
      indexResource(resource, compilationUnit, ast);
      long endTime = System.currentTimeMillis();
      initIndexingTime += endTime - startTime;
    }
    for (LibraryUnit importedLibrary : libraryUnit.getImports()) {
      // library.getImportedLibrary(importedLibrary.getSource().getUri());
      indexUserLibrary(importedLibrary, initializedLibraries);
    }
  }

  /**
   * Initialize this index from the given file.
   * 
   * @param indexFile the file from which this index is to be initialized
   * @return <code>true</code> if the index was correctly initialized
   */
  private boolean initializeIndexFrom(File indexFile) {
    if (indexFile.exists()) {
      try {
        readIndexFrom(indexFile);
        return true;
      } catch (IOException exception) {
        DartCore.logError("Could not read index file: \"" + indexFile.getAbsolutePath() + "\"",
            exception);
      }
      try {
        indexFile.delete();
      } catch (Exception exception) {
        DartCore.logError("Could not delete corrupt index file: \"" + indexFile.getAbsolutePath()
            + "\"", exception);
      }
    }
    return false;
  }

  /**
   * Read the contents of this index from the given input stream.
   * 
   * @param input the input stream from which this index will be read
   * @throws IOException if the index could not be read from the given input stream
   */
  private void readIndex(ObjectInputStream input) throws IOException {
    IndexReader reader = indexStore.createIndexReader();
    reader.readIndex(input);
  }

  /**
   * Read the contents of this index from the given file.
   * 
   * @param indexFile the file from which this index will be read
   * @throws IOException if the index could not be read from the given file
   */
  private void readIndexFrom(File indexFile) throws IOException {
    ObjectInputStream input = null;
    try {
      input = new ObjectInputStream(new FileInputStream(indexFile));
      long startTime = System.currentTimeMillis();
      readIndex(input);
      if (DartCoreDebug.PERF_INDEX) {
        long endTime = System.currentTimeMillis();
        DartCore.logInformation("Reading the index took " + (endTime - startTime) + " ms");
      }
    } finally {
      if (input != null) {
        try {
          input.close();
        } catch (IOException exception) {
          DartCore.logError(
              "Could not close index file after read: \"" + indexFile.getAbsolutePath() + "\"",
              exception);
        }
      }
    }
  }

  /**
   * Write the contents of this index to the given output stream.
   * 
   * @param output the output stream to which this index will be written
   * @throws IOException if the index could not be written to the given output stream
   */
  private void writeIndex(ObjectOutputStream output) throws IOException {
    IndexWriter writer = indexStore.createIndexWriter();
    writer.writeIndex(output);
  }

  /**
   * Write the contents of this index to the given file.
   * 
   * @param indexFile the file to which this index will be written
   * @throws IOException if the index could not be written to the given file
   */
  private void writeIndexTo(File indexFile) {
    boolean successfullyWritten = true;
    ObjectOutputStream output = null;
    try {
      output = new ObjectOutputStream(new FileOutputStream(indexFile));
      long startTime = System.currentTimeMillis();
      writeIndex(output);
      if (DartCoreDebug.PERF_INDEX) {
        long endTime = System.currentTimeMillis();
        DartCore.logInformation("Writing the index took " + (endTime - startTime) + " ms");
      }
    } catch (IOException exception) {
      successfullyWritten = false;
      DartCore.logError("Could not write index file: \"" + indexFile.getAbsolutePath() + "\"",
          exception);
    } finally {
      if (output != null) {
        try {
          output.flush();
        } catch (IOException exception) {
          successfullyWritten = false;
          DartCore.logError(
              "Could not flush index file after write: \"" + indexFile.getAbsolutePath() + "\"",
              exception);
        }
        try {
          output.close();
        } catch (IOException exception) {
          successfullyWritten = false;
          DartCore.logError(
              "Could not close index file after write: \"" + indexFile.getAbsolutePath() + "\"",
              exception);
        }
      }
    }
    if (!successfullyWritten) {
      if (!indexFile.delete()) {
        DartCore.logError("Could not delete corrupted index file: \"" + indexFile.getAbsolutePath()
            + "\"");
      }
    }
  }
}
