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

import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.PackageLibraryManager;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.analysis.AnalysisServer;
import com.google.dart.tools.core.analysis.SavedContext;
import com.google.dart.tools.core.index.Attribute;
import com.google.dart.tools.core.index.AttributeCallback;
import com.google.dart.tools.core.index.Element;
import com.google.dart.tools.core.index.Index;
import com.google.dart.tools.core.index.NotifyCallback;
import com.google.dart.tools.core.index.Relationship;
import com.google.dart.tools.core.index.RelationshipCallback;
import com.google.dart.tools.core.index.Resource;
import com.google.dart.tools.core.internal.index.operation.ClearIndexOperation;
import com.google.dart.tools.core.internal.index.operation.GetAttributeOperation;
import com.google.dart.tools.core.internal.index.operation.GetRelationshipsOperation;
import com.google.dart.tools.core.internal.index.operation.IndexOperation;
import com.google.dart.tools.core.internal.index.operation.IndexResourceOperation;
import com.google.dart.tools.core.internal.index.operation.OperationProcessor;
import com.google.dart.tools.core.internal.index.operation.OperationQueue;
import com.google.dart.tools.core.internal.index.operation.RemoveResourceOperation;
import com.google.dart.tools.core.internal.index.persistance.IndexReader;
import com.google.dart.tools.core.internal.index.persistance.IndexWriter;
import com.google.dart.tools.core.internal.index.store.IndexStore;
import com.google.dart.tools.core.internal.index.util.ResourceFactory;
import com.google.dart.tools.core.internal.model.CompilationUnitImpl;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.model.ExternalCompilationUnitImpl;
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.internal.workingcopy.DefaultWorkingCopyOwner;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModel;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.DartSdkManager;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

/**
 * The unique instance of the class <code>InMemoryIndex</code> maintains an in-memory {@link Index
 * index}. The index is expected to be initialized once before it is used in any given session and
 * shut down at the end of the session. The index will be read from disk when it is initialized and
 * written to disk when it is shut down.
 */
public class InMemoryIndex implements Index {

  /**
   * Answer the file generated at build time that holds the index for the SDK
   * 
   * @return the file (not {@code null})
   */
  public static File getSdkIndexFile() {
    Location location = Platform.getConfigurationLocation();
    if (location != null) {
      URL configURL = location.getURL();
      if (configURL != null && configURL.getProtocol().startsWith("file")) {
        File pluginConfigDir = new File(configURL.getFile(), DartCore.PLUGIN_ID);
        return new File(pluginConfigDir, INDEX_FILE);
      }
    }
    // TODO (danrubel): Remove old location once new location is confirmed
    return DartSdkManager.getManager().getSdk().getLibraryIndexFile();
  }

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
  public static final String INDEX_FILE = "index.idx";

  /**
   * The name of the file containing the initial state of the index.
   */
  //private static final String INITIAL_INDEX_FILE = "initial_index.idx";

  /**
   * Return the unique instance of this class.
   * 
   * @return the unique instance of this class
   */
  public static InMemoryIndex getInstance() {
    return UniqueInstance;
  }

  public static InMemoryIndex newInstanceForTesting() {
    return new InMemoryIndex();
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
   * Remove all of the information from this index and re-initialize it to have information about
   * the bundled libraries.
   */
  public void clear() {
    queue.enqueue(new ClearIndexOperation(indexStore, new Runnable() {
      @Override
      public void run() {
        initializeBundledLibraries();
      }
    }));
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
   */
  public void indexResource(File libraryFile, File sourceFile, DartUnit dartUnit)
      throws DartModelException {

    // Get the LibrarySource

    LibrarySource librarySource = dartUnit.getLibrary().getSource();

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

    // Get the CompilationUnit

    DartSource unitSource = (DartSource) dartUnit.getSourceInfo().getSource();
    CompilationUnit compilationUnit;
    IResource res = ResourceUtil.getResource(sourceFile);
    if (res != null) {
      DefaultWorkingCopyOwner workingCopy = DefaultWorkingCopyOwner.getInstance();
      compilationUnit = new CompilationUnitImpl(library, (IFile) res, workingCopy);
    } else {
      String relPath = unitSource.getRelativePath();
      compilationUnit = new ExternalCompilationUnitImpl(library, relPath, unitSource);
    }

    URI unitUri = unitSource.getUri();
    Resource indexerResource;
    if (PackageLibraryManager.isDartUri(unitUri)) {
      indexerResource = new Resource(ResourceFactory.composeResourceId(
          librarySource.getUri().toString(),
          unitUri.toString()));
    } else if (PackageLibraryManager.isPackageUri(unitUri)) {
      indexerResource = new Resource(ResourceFactory.composeResourceId(
          libraryFile.toURI().toString(),
          sourceFile.toURI().toString()));
    } else {
      indexerResource = ResourceFactory.getResource(compilationUnit);
    }

    // Queue the resource to be indexed

    indexResource(indexerResource, libraryFile, compilationUnit, dartUnit);
  }

  /**
   * Process the given resource within the context of the given working set in order to record the
   * data and relationships found within the resource.
   * 
   * @param resource the resource containing the elements defined in the compilation unit
   * @param libraryFile the library file defining the library containing the compilation unit to be
   *          indexed or <code>null</code> if the library is not on disk
   * @param compilationUnit the compilation unit being indexed
   * @param unit the compilation unit to be indexed
   */
  @Override
  public void indexResource(Resource resource, File libraryFile, CompilationUnit compilationUnit,
      DartUnit unit) {
    queue.enqueue(new IndexResourceOperation(
        indexStore,
        resource,
        libraryFile,
        compilationUnit,
        unit,
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
        if (DartCoreDebug.TRACE_INDEX_STATISTICS) {
          logIndexStats("Clearing index after failing to read from index file");
        }
        indexStore.clear();
        if (!initializeBundledLibraries()) {
          if (DartCoreDebug.TRACE_INDEX_STATISTICS) {
            logIndexStats("Failed to initialize bundled libraries");
          }
          return;
        }
        if (!indexUserLibraries()) {
          if (DartCoreDebug.TRACE_INDEX_STATISTICS) {
            logIndexStats("Clearing index after failing to index user libraries");
          }
          indexStore.clear();
          initializeBundledLibraries();
        }
      }
      if (DartCoreDebug.TRACE_INDEX_STATISTICS) {
        logIndexStats("After initializing the index");
      }
    }
  }

  /**
   * Write index statistics to the log.
   */
  public void logIndexStats(String message) {
    int relationshipCount;
    int attributeCount;
    int elementCount;
    int resourceCount;
    synchronized (indexStore) {
      relationshipCount = indexStore.getRelationshipCount();
      attributeCount = indexStore.getAttributeCount();
      elementCount = indexStore.getElementCount();
      resourceCount = indexStore.getResourceCount();
    }
    DartCore.logInformation(message + ": " + relationshipCount + " relationships and "
        + attributeCount + " attributes in " + elementCount + " elements in " + resourceCount
        + " resources");
  }

  /**
   * Asynchronously invoke the given {@link NotifyCallback} when the operation is performed in the
   * queue.
   * 
   * @param callback the callback that will be invoked when the operation is reached in the queue
   */
  public void notify(final NotifyCallback callback) {
    notify(callback, false);
  }

  /**
   * Asynchronously invoke the given {@link NotifyCallback} when the index is has finished indexing
   * everything on its queue and is processing queries.
   * 
   * @param callback the callback that will be invoked when the operation is reached in the queue
   */
  public void notifyWhenReadyForQueries(final NotifyCallback callback) {
    notify(callback, true);
  }

  /**
   * Remove from the index all of the information associated with elements or locations in the given
   * resource. This includes relationships between an element in the given resource and any other
   * locations, relationships between any other elements and a location within the given resource,
   * and any values of any attributes defined on elements in the given resource.
   * <p>
   * This method should be invoked when a resource is no longer part of the code base and when the
   * information about the resource is about to be re-generated.
   */
  public void removeResource(File libraryFile, File sourceFile) {
    String resourceId = ResourceFactory.composeResourceId(
        libraryFile.toURI().toString(),
        sourceFile.toURI().toString());
    removeResource(new Resource(resourceId));
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

  /**
   * Set whether the index should process query requests.
   * 
   * @param processQueries <code>true</code> if the index should process incomming query requests or
   *          <code>false</code> if query requests should be queued but not processed until this
   *          method is called with a value of <code>true</code>.
   */
  public void setProcessQueries(boolean processQueries) {
    queue.setProcessQueries(processQueries);
  }

  public void shutdown() {
    synchronized (indexStore) {
      if (DartCoreDebug.TRACE_INDEX_STATISTICS) {
        logIndexStats("In shutdown, before writing the index");
      }
      if (hasBeenInitialized) {
        if (hasPendingClear()) {
          try {
            if (DartCoreDebug.TRACE_INDEX_STATISTICS) {
              DartCore.logInformation("In shutdown, deleting the index file");
            }
            getIndexFile().delete();
          } catch (Exception exception) {
            DartCore.logError("Could not delete the index file", exception);
          }
        } else {
          writeIndexTo(getIndexFile());
          if (DartCoreDebug.TRACE_INDEX_STATISTICS) {
            logIndexStats("In shutdown, after writing the index");
          }
        }
      }
    }
  }

  /**
   * Write the current index to the SDK directory.
   */
  public void writeIndexToSdk() {
    writeIndexTo(getSdkIndexFile());
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
    // return new File(DartCore.getPlugin().getStateLocation().toFile(), INITIAL_INDEX_FILE);
    DartSdkManager sdkManager = DartSdkManager.getManager();
    if (sdkManager.hasSdk()) {
      return getSdkIndexFile();
    }
    return null;
  }

  /**
   * Return <code>true</code> if there is an operation on the queue to clear the index. We can
   * effectively do so by deleting the index file from disk.
   * 
   * @return <code>true</code> if there is an operation on the queue to clear the index
   */
  private boolean hasPendingClear() {
    for (IndexOperation operation : queue.getOperations()) {
      if (operation instanceof ClearIndexOperation) {
        return true;
      }
    }
    return false;
  }

  /**
   * Initialize this index with information from the bundled libraries.
   * 
   * @return <code>true</code> if the bundled libraries were successfully indexed
   */
  private boolean indexBundledLibraries() {
    boolean librariesIndexed = true;
    long startTime = System.currentTimeMillis();
    PackageLibraryManager libraryManager = PackageLibraryManagerProvider.getPackageLibraryManager();
    ArrayList<String> librarySpecs = new ArrayList<String>(libraryManager.getAllLibrarySpecs());
    if (librarySpecs.remove("dart:html")) {
      librarySpecs.add("dart:html");
    }
    AnalysisServer analysisServer = PackageLibraryManagerProvider.getDefaultAnalysisServer();
    analysisServer.reanalyze();
    SavedContext savedContext = analysisServer.getSavedContext();
    for (String urlSpec : librarySpecs) {
      try {
        URI libraryUri = new URI(urlSpec);
        File libraryFile = new File(libraryManager.resolveDartUri(libraryUri));
        savedContext.resolve(libraryFile, null);
      } catch (URISyntaxException exception) {
        librariesIndexed = false;
        DartCore.logError("Invalid URI returned from the system library manager: \"" + urlSpec
            + "\"", exception);
      } catch (Exception exception) {
        librariesIndexed = false;
        DartCore.logError("Could not index bundled libraries", exception);
      }
    }
    if (DartCoreDebug.PERF_INDEX) {
      long endTime = System.currentTimeMillis();
      DartCore.logInformation("Initializing the index with information from bundled libraries took "
          + (endTime - startTime) + " ms (" + initIndexingTime + " ms indexing)");
    }
    return librariesIndexed;
  }

  /**
   * Initialize this index with information from the user libraries.
   */
  private boolean indexUserLibraries() {
    boolean librariesIndexed = true;
    try {
      AnalysisServer analysisServer = PackageLibraryManagerProvider.getDefaultAnalysisServer();
      SavedContext savedContext = analysisServer.getSavedContext();
      DartModel model = DartCore.create(ResourcesPlugin.getWorkspace().getRoot());
      for (DartProject project : model.getDartProjects()) {
        for (DartLibrary library : project.getDartLibraries()) {
          CompilationUnit compilationUnit = library.getDefiningCompilationUnit();
          if (compilationUnit == null) {
            continue;
          }
          IResource libraryResource = compilationUnit.getResource();
          if (libraryResource == null) {
            continue;
          }
          IPath libraryLocation = libraryResource.getLocation();
          if (libraryLocation == null) {
            continue;
          }
          File libraryFile = libraryLocation.toFile();
          savedContext.resolve(libraryFile, null);
        }
      }
    } catch (Exception exception) {
      librariesIndexed = false;
      DartCore.logError("Could not index user libraries", exception);
    }
    return librariesIndexed;
  }

  /**
   * Initialize this index to contain information about the bundled libraries. The index store is
   * expected to have been cleared before invoking this method.
   * 
   * @return {@code true} if the bundled libraries were successfully indexed
   */
  private boolean initializeBundledLibraries() {
    synchronized (indexStore) {
      hasBeenInitialized = true;
      if (!initializeIndexFrom(getInitialIndexFile())) {
        if (DartCoreDebug.TRACE_INDEX_STATISTICS) {
          logIndexStats("Clearing index after failing to read from initial index file");
        }
        indexStore.clear();
        if (!indexBundledLibraries()) {
          if (DartCoreDebug.TRACE_INDEX_STATISTICS) {
            logIndexStats("Clearing index after failing to index bundled libraries");
          }
          indexStore.clear();
          return false;
        }
        // TODO(brianwilkerson) Restore the following line once we figure out how to know that the
        // bundled libraries (and only the bundled libraries) have been indexed.
//        writeIndexTo(getInitialIndexFile());
      }
    }
    return true;
  }

  /**
   * Initialize this index from the given file.
   * 
   * @param indexFile the file from which this index is to be initialized
   * @return <code>true</code> if the index was correctly initialized
   */
  private boolean initializeIndexFrom(File indexFile) {
    if (indexFile == null) {
      if (DartCoreDebug.TRACE_INDEX_STATISTICS) {
        DartCore.logInformation("Index file was null");
      }
      return false;
    } else if (!indexFile.exists()) {
      if (DartCoreDebug.TRACE_INDEX_STATISTICS) {
        DartCore.logInformation("Index file " + indexFile.getAbsolutePath() + " does not exist");
      }
      return false;
    }
    if (DartCoreDebug.TRACE_INDEX_STATISTICS) {
      DartCore.logInformation("About to initialize the index from file "
          + indexFile.getAbsolutePath() + " (size = " + indexFile.getTotalSpace() + " bytes)");
    }
    try {
      boolean wasRead = readIndexFrom(indexFile);
      if (DartCoreDebug.TRACE_INDEX_STATISTICS) {
        logIndexStats("After initializing the index from file " + indexFile.getAbsolutePath());
      }
      synchronized (indexStore) {
        return wasRead && indexStore.getResourceCount() > 0;
      }
    } catch (Exception exception) {
      DartCore.logError("Could not read index file " + indexFile.getAbsolutePath(), exception);
    }
    if (DartCoreDebug.TRACE_INDEX_STATISTICS) {
      logIndexStats("Deleting corrupted index file " + indexFile.getAbsolutePath());
    }
    try {
      indexFile.delete();
    } catch (Exception exception) {
      DartCore.logError(
          "Could not delete corrupt index file " + indexFile.getAbsolutePath(),
          exception);
    }
    return false;
  }

  private void notify(final NotifyCallback callback, final boolean isQuery) {
    queue.enqueue(new IndexOperation() {
      @Override
      public boolean isQuery() {
        return isQuery;
      }

      @Override
      public void performOperation() {
        callback.done();
      }

      @Override
      public boolean removeWhenResourceRemoved(Resource resource) {
        return false;
      }
    });
  }

  /**
   * Read the contents of this index from the given input stream.
   * 
   * @param input the input stream from which this index will be read
   * @return {@code true} if the file was correctly read
   * @throws IOException if the index could not be read from the given input stream
   */
  private boolean readIndex(ObjectInputStream input) throws IOException {
    IndexReader reader = indexStore.createIndexReader();
    return reader.readIndex(input);
  }

  /**
   * Read the contents of this index from the given file.
   * 
   * @param indexFile the file from which this index will be read
   * @return {@code true} if the file was correctly read
   * @throws IOException if the index could not be read from the given file
   */
  private boolean readIndexFrom(File indexFile) throws IOException {
    ObjectInputStream input = null;
    try {
      input = new ObjectInputStream(new FileInputStream(indexFile));
      long startTime = System.currentTimeMillis();
      boolean wasRead = readIndex(input);
      if (DartCoreDebug.PERF_INDEX) {
        long endTime = System.currentTimeMillis();
        DartCore.logInformation("Reading the index took " + (endTime - startTime) + " ms");
      }
      return wasRead;
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
      DartCore.logError(
          "Could not write index file: \"" + indexFile.getAbsolutePath() + "\"",
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
