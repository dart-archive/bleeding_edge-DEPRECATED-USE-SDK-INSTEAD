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
package com.google.dart.indexer.workspace.index;

import com.google.dart.indexer.IndexerPlugin;
import com.google.dart.indexer.debug.IndexerDebugOptions;
import com.google.dart.indexer.exceptions.IndexIsStillBuilding;
import com.google.dart.indexer.exceptions.IndexRequestFailed;
import com.google.dart.indexer.exceptions.IndexRequiresFullRebuild;
import com.google.dart.indexer.exceptions.IndexTemporarilyNonOperational;
import com.google.dart.indexer.index.IndexSession;
import com.google.dart.indexer.index.IndexSessionStats;
import com.google.dart.indexer.index.IndexTransaction;
import com.google.dart.indexer.index.configuration.IndexConfigurationInstance;
import com.google.dart.indexer.index.entries.PathAndModStamp;
import com.google.dart.indexer.index.queries.Query;
import com.google.dart.indexer.index.readonly.Index;
import com.google.dart.indexer.storage.inmemory.StorageManager;
import com.google.dart.indexer.utilities.io.PrintStringWriter;
import com.google.dart.indexer.utils.ToStringComparator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/**
 * This class was designed to be testable easily, so it does not interact with workspace events.
 * Thread safety: safe for use from multiple threads. However external synchronization should be
 * employed to prevent accessing the index returned by <code>getIndex</code> while
 * <code>indexPendingFiles</code> method is running.
 */
public class WorkspaceIndexer {
  private final IndexSession session;

  /**
   * Note: the value of this field changes after each index update. To make sure nobody caches the
   * old index, we never give away the value of this variable. (We only pass it to queries, but it's
   * safe to assume that queries will not store and reuse it.)
   */
  private Index index;

  private final IndexingQueue queue = new IndexingQueue();

  private final IndexConfigurationInstance configuration;

  public WorkspaceIndexer(IndexConfigurationInstance configuration) {
    this(configuration, null);
  }

  public WorkspaceIndexer(IndexConfigurationInstance configuration, IFile[] changedFiles) {
    this.configuration = configuration;
    File metadata = new File(getWorkspaceIndexMetadataLocation());
    File targetFile = new File(new File(metadata, "index"), "index.indx");
    StorageManager.setTargetFile(targetFile);
    session = new IndexSession(configuration);
    try {
      index = session.createRegularIndex(metadata);
      if (changedFiles == null) {
        tryResync();
      } else {
        enqueueChangedFiles(changedFiles);
        resyncAllFiles();
      }
    } catch (IndexRequiresFullRebuild exception) {
      try {
        index = session.createNewRegularIndex(metadata);
        enqueueFullRebuild();
      } catch (IndexTemporarilyNonOperational innerException) {
        index = session.createEmptyRegularIndex();
        enqueueFullRebuild();
      }
    }
  }

  public String diskIndexAsString() throws IOException {
    return index.diskIndexAsString();
  }

  public void dispose() {
    session.dispose();
    index = null;
  }

  public void enqueueChangedFiles(IFile[] changedFiles) {
    queue.enqueue(changedFiles);
  }

  public void enqueueFullRebuild() {
    queue.abnormalState(QueueState.NEEDS_REBUILD);
  }

  public void enqueueFullRebuildDueTo(IndexRequiresFullRebuild exception) {
    if (exception.getMessage().indexOf("Initial creation") == -1) {
      IndexerPlugin.getLogger().trace(IndexerDebugOptions.ANOMALIES, exception, "Anomaly");
    }
    if (exception.shouldReportAsError()) {
      IndexerPlugin.getLogger().logError(exception, "Rebuilding the entire index");
    }
    IFile[] filesToIndex = exception.getFilesToIndex();
    if (filesToIndex == null) {
      enqueueFullRebuild();
    } else {
      enqueueFullRebuild(filesToIndex);
    }
  }

  public void execute(Query query) throws IndexTemporarilyNonOperational, IndexRequiresFullRebuild,
      IndexIsStillBuilding {
    if (IndexerPlugin.getLogger().isTracing(IndexerDebugOptions.STORE_CONTENTS_BEFORE_EACH_QUERY)) {
      PrintStringWriter writer = new PrintStringWriter();
      writer.println("============================================================");
      writer.println(index.getUnderlyingStorage().toString());
      writer.println("============================================================");
      IndexerPlugin.getLogger().trace(IndexerDebugOptions.STORE_CONTENTS_BEFORE_EACH_QUERY,
          writer.toString());
    }
    if (!isReadyToExecuteQuery(query)) {
      throw new IndexIsStillBuilding();
    }
    try {
      query.executeUsing(index);
    } catch (IndexRequiresFullRebuild e) {
      enqueueFullRebuildDueTo(e);
      throw e;
    }
  }

  public IndexSessionStats gatherStatistics() {
    return session.gatherStatistics();
  }

  /**
   * @return list of files which may be indexed with errors
   */
  public IPath[] getFilesWithErrors() {
    return index.getFilesWithErrors();
  }

  /**
   * Utility method used to update monitor state by an IndexingJob
   * 
   * @return
   */
  public int getQueueSize() {
    return queue.getQueueSize();
  }

  public int handleSpecialStates() {
    QueueState state = queue.pollState();
    if (state == QueueState.NEEDS_RESYNC) {
      try {
        resyncAllFiles();
      } catch (IndexRequiresFullRebuild e) {
        enqueueFullRebuildDueTo(e);
      }
      state = queue.pollState();
    }
    if (state == QueueState.NEEDS_REBUILD) {
      Collection<IFile> files = collectAllExistingFiles();
      enqueueFullRebuild(files.toArray(new IFile[files.size()]));
    }
    return queue.getQueueSize();
  }

  /**
   * @return true if index may be inconsistent
   */
  public boolean hasErrors() {
    return index.hasErrors();
  }

  public boolean indexPendingFiles(long stopProcessingAt) throws IndexTemporarilyNonOperational {
    return indexPendingFiles(stopProcessingAt, new NullProgressMonitor());
  }

  /**
   * After a successful execution of this method the new version of the index will be available for
   * querying. If this method fails with an exception, all future queries will use an empty index.
   * 
   * @param stopProcessingAt stop processing when <code>System.currentTimeMillis()</code> becomes
   *          greater than this value; <code>-1</code> for no limit.
   * @param monitor
   * @return <code>true</code> if there is (possibly) more work to do, <code>false</code> if the
   *         indexing queue is empty right now (so no more work to do).
   * @throws IndexTemporarilyNonOperational
   */
  public boolean indexPendingFiles(long stopProcessingAt, IProgressMonitor monitor)
      throws IndexTemporarilyNonOperational {
    synchronized (this) {
      handleSpecialStates();
    }

    IndexTransaction transaction = session.createTransaction(index);
    LinkedList<IFile> dequeued = new LinkedList<IFile>();
    try {
      boolean didSomething = false;
      try {
        didSomething = doIndexPendingFiles(transaction, dequeued, stopProcessingAt, monitor);
      } finally {
        transaction.close();
      }
      File metadata = new File(getWorkspaceIndexMetadataLocation());
      synchronized (this) {
        index = session.createRegularIndex(metadata);
        this.notifyAll();
      }
      return didSomething;
    } catch (IndexRequiresFullRebuild e) {
      synchronized (this) {
        index = session.createEmptyRegularIndex();
        enqueueFullRebuildDueTo(e);
      }
      return true;
    } catch (IndexTemporarilyNonOperational e) {
      synchronized (this) {
        index = session.createEmptyRegularIndex();
        // no changes have been committed, so re-enqueue all dequeued files
        while (!dequeued.isEmpty()) {
          queue.reenqueue(dequeued.removeLast());
        }
      }
      throw e;
    } catch (IndexRequestFailed e) {
      throw new AssertionError("Unreachable catch: " + e);
    } catch (RuntimeException e) {
      // Oops! Something is seriously wrong here.
      e.printStackTrace(System.err);
      synchronized (this) {
        index = session.createEmptyRegularIndex();
      }
      throw new IndexTemporarilyNonOperational(e);
    } catch (Error e) {
      if (e instanceof ThreadDeath) {
        throw e;
      }
      // Oops! Something is seriously wrong here.
      e.printStackTrace(System.err);
      synchronized (this) {
        index = session.createEmptyRegularIndex();
      }
      throw new IndexTemporarilyNonOperational(e);
    }

  }

  public boolean isReadyToExecuteQuery(Query query) {
    if (queue.isPendingResyncOrRebuild()) {
      return false;
    }
    IProject project = query.getContainingProject();
    if (project == null) {
      return queue.getQueueSize() == 0;
    }
    Set<IProject> projects = computeRelatedProjects(project);
    return !queue.hasQueuedFilesIn(projects);
  }

  public void prioritizeQuery(Query query) {
    IProject containingProject = query.getContainingProject();
    if (containingProject != null) {
      Set<IProject> projects = computeRelatedProjects(containingProject);
      for (Iterator<IProject> iterator = projects.iterator(); iterator.hasNext();) {
        IProject project = iterator.next();
        queue.prioritizeProject(project);
      }
    }
  }

  public void rebuildIndex() {
    File metadata = new File(getWorkspaceIndexMetadataLocation());
    try {
      index = session.createNewRegularIndex(metadata);
      enqueueFullRebuild();
    } catch (IndexTemporarilyNonOperational exception) {
      index = session.createEmptyRegularIndex();
      enqueueFullRebuild();
    }
  }

  public void runConsistencyCheck(IProgressMonitor monitor) {
    session.runConsistencyCheck(monitor);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " with " + queue;
  }

  public void tryResync() {
    queue.abnormalState(QueueState.NEEDS_RESYNC);
  }

  public void unprioritizeQuery(Query query) {
    IProject containingProject = query.getContainingProject();
    if (containingProject != null) {
      Set<IProject> projects = computeRelatedProjects(containingProject);
      for (Iterator<IProject> iterator = projects.iterator(); iterator.hasNext();) {
        IProject project = iterator.next();
        queue.unprioritizeProject(project);
      }
    }
  }

  private void addReferencedProjects(IProject project, Set<IProject> set) {
    if (!project.isAccessible()) {
      return;
    }
    try {
      if (!set.add(project)) {
        return;
      }
      IProject[] projects = project.getReferencedProjects();
      for (int i = 0; i < projects.length; i++) {
        addReferencedProjects(projects[i], set);
      }
    } catch (CoreException exception) {
      IndexerPlugin.getLogger().logError(exception);
    }
  }

  private void addReferencingProjects(IProject project, Set<IProject> set) {
    if (!project.isAccessible()) {
      return;
    }
    if (!set.add(project)) {
      return;
    }
    IProject[] projects = project.getReferencingProjects();
    for (int i = 0; i < projects.length; i++) {
      addReferencedProjects(projects[i], set);
    }
  }

  private Collection<IFile> collectAllExistingFiles() {
    final Collection<IFile> existingFiles = new ArrayList<IFile>();
    try {
      ResourcesPlugin.getWorkspace().getRoot().accept(
          new WorkspaceFilesCollector(configuration, existingFiles));
    } catch (CoreException e) {
      // cannot happen (not even if some file is out of sync)
      IndexerPlugin.getLogger().logError(e);
    }
    return existingFiles;
  }

  private Set<IProject> computeRelatedProjects(IProject project) {
    Set<IProject> projects = new HashSet<IProject>();
    addReferencedProjects(project, projects);
    addReferencingProjects(project, projects);
    return projects;
  }

  private boolean doIndexPendingFiles(IndexTransaction transaction, LinkedList<IFile> dequeued,
      long stopProcessingAt, IProgressMonitor monitor) throws IndexRequestFailed {
    boolean didSomething = false;
    int filesIndexedAfterCheckpoint = 0;
    int filesIndexed = 0;
    do {
      if (monitor.isCanceled()) {
        return true;
      }
      IFile file = queue.dequeue();
      if (file == null) {
        break;
      }
      dequeued.addLast(file);
      didSomething = true;
      try {
        synchronized (this) {
          indexFile(file, transaction, false);
        }
      } catch (FileIndexingFailed e) {
        IndexerPlugin.getLogger().logError(e);
        transaction.addErrorFile(file, e);
      }
      monitor.worked(1);
      monitor.subTask(queue.size() + " files left to index");
      ++filesIndexed;
      if (++filesIndexedAfterCheckpoint == 20) {
        IndexerPlugin.getLogger().trace(IndexerDebugOptions.MISCELLANEOUS,
            filesIndexed + " files total.");
        // session.getStorage().checkpoint();
        filesIndexedAfterCheckpoint = 0;
      }
      IProject project = file.getProject();
      if (!queue.hasQueuedFilesIn(project)) {
        synchronized (this) {
          this.notifyAll();
        }
      }
    } while (stopProcessingAt == -1 || System.currentTimeMillis() < stopProcessingAt);
    IFile[] filesWithErrors = transaction.getFilesWithErrors();
    if (filesWithErrors.length > 0) {
      monitor.subTask(filesWithErrors.length + " files to reindex");
      for (int a = 0; a < filesWithErrors.length; a++) {
        try {
          synchronized (this) {
            indexFile(filesWithErrors[a], transaction, true);
          }
        } catch (FileIndexingFailed e) {
          IndexerPlugin.getLogger().logError(e);
        }
      }
    }
    return didSomething;
  }

  private void enqueueFullRebuild(IFile[] filesToIndex) {
    session.destroyIndex();
    queue.replaceWith(filesToIndex);
  }

  private PathAndModStamp[] findAllIndexedFiles(final HashSet<IFile> unprocessedExistingFiles)
      throws IndexRequiresFullRebuild {
    PathAndModStamp[] indexedFiles;
    try {
      indexedFiles = index.loadAllFileHeaders();

    } catch (IOException e) {
      IFile[] filesToIndex = unprocessedExistingFiles.toArray(new IFile[unprocessedExistingFiles.size()]);
      throw new IndexRequiresFullRebuild(e, filesToIndex);
    }
    return indexedFiles;
  }

  private String getWorkspaceIndexMetadataLocation() {
    return IndexerPlugin.getDefault().getStateLocation().toFile().getAbsolutePath();
  }

  private void indexFile(IFile file, IndexTransaction transaction, boolean isRetry)
      throws IndexRequestFailed, IndexRequiresFullRebuild, IndexTemporarilyNonOperational,
      FileIndexingFailed {
    // IndexerPlugin.getLogger().trace(IndexerDebugOptions.MISCELLANEOUS,
    // "WorkspaceIndexer.indexFile(" + file + ")");
    try {
      if (file.exists()) {
        transaction.indexFile(file);
      } else {
        IFile[] affectedFiles = transaction.removeFile(file);
        queue.enqueue(affectedFiles);
      }
    } catch (RuntimeException e) {
      throw new FileIndexingFailed(file, e, isRetry);
    } catch (Error e) {
      throw new FileIndexingFailed(file, e, isRetry);
    }
  }

  private void resyncAllFiles() throws IndexRequiresFullRebuild {
    final HashSet<IFile> unprocessedExistingFiles = new HashSet<IFile>(collectAllExistingFiles());
    PathAndModStamp[] indexedFiles = findAllIndexedFiles(unprocessedExistingFiles);
    PathAndModStamp[] findAllIndexedFiles = session.getStorage().readFileNamesAndStamps(
        unprocessedExistingFiles);
    Arrays.sort(indexedFiles, ToStringComparator.getInstance());
    Arrays.sort(findAllIndexedFiles, ToStringComparator.getInstance());
    boolean equals = Arrays.equals(indexedFiles, findAllIndexedFiles);
    if (!equals) {
      indexedFiles = findAllIndexedFiles;
    }
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    for (int i = 0; i < indexedFiles.length; i++) {
      PathAndModStamp info = indexedFiles[i];
      IFile file = root.getFile(new Path(info.getPath()));
      if (!unprocessedExistingFiles.contains(file)) {
        queue.deletedFile(file);
      } else {
        unprocessedExistingFiles.remove(file);
        if (file.getModificationStamp() != info.getModificationStamp()) {
          queue.changedFile(file);
        }
      }
    }
    for (Iterator<IFile> iterator = unprocessedExistingFiles.iterator(); iterator.hasNext();) {
      IFile file = iterator.next();
      queue.addedFile(file);
    }
  }
}
