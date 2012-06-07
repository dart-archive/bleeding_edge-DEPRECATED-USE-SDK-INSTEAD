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

import com.google.dart.compiler.SystemLibraryManager;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.EditorLibraryManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Provides analysis of Dart code for Dart editor
 */
public class AnalysisServer {

  private static final String CACHE_FILE_VERSION_TAG = "v1";
  private static final String END_LIBRARIES_TAG = "</end-libraries>";

  private static PerformanceListener performanceListener;

  public static PerformanceListener getPerformanceListener() {
    return performanceListener;
  }

  public static void setPerformanceListener(PerformanceListener performanceListener) {
    AnalysisServer.performanceListener = performanceListener;
  }

  private IdleListener[] idleListeners = new IdleListener[0];

  /**
   * The target (VM, Dartium, JS) against which user libraries are resolved. Targets are immutable
   * and can be accessed on any thread.
   */
  private final EditorLibraryManager libraryManager;

  /**
   * The library files being analyzed by the receiver. Lock against {@link #queue} before accessing
   * this object.
   */
  private final ArrayList<File> libraryFiles = new ArrayList<File>();

  /**
   * The outstanding tasks to be performed. Lock against this object before accessing it.
   */
  private final ArrayList<Task> queue = new ArrayList<Task>();

  /**
   * The index at which the task being performed can insert new tasks. Tracking this allows new
   * tasks to take priority and be first in the queue. Lock against {@link #queue} before accessing
   * this field.
   */
  private int queueIndex = 0;

  /**
   * The background thread on which analysis tasks are performed or <code>null</code> if the
   * background process has not been started yet. Lock against {@link #queue} before accessing this
   * field.
   */
  private Thread backgroundThread;

  /**
   * A context representing what is "saved on disk". Contents of this object should only be accessed
   * on the background thread.
   */
  private final SavedContext savedContext = new SavedContext(this);

  /**
   * A context representing what is "currently being edited". Contents of this object should only be
   * accessed on the background thread.
   */
  private final EditContext editContext = new EditContext(this, savedContext);

  /**
   * <code>true</code> if the background thread should continue executing analysis tasks
   */
  private boolean analyze;

  /**
   * Flag indicating whether the background thread is waiting for more tasks to be queued. Lock
   * against {@link #queue} before accessing this field.
   */
  private boolean isBackgroundThreadIdle = false;

  /**
   * Create a new instance that processes analysis tasks on a background thread
   * 
   * @param libraryManager the target (VM, Dartium, JS) against which user libraries are resolved
   */
  public AnalysisServer(EditorLibraryManager libraryManager) {
    if (libraryManager == null) {
      throw new IllegalArgumentException();
    }
    this.libraryManager = libraryManager;
  }

  public void addIdleListener(IdleListener listener) {
    for (int i = 0; i < idleListeners.length; i++) {
      if (idleListeners[i] == listener) {
        return;
      }
    }
    int oldLen = idleListeners.length;
    IdleListener[] newListeners = new IdleListener[oldLen + 1];
    System.arraycopy(idleListeners, 0, newListeners, 0, oldLen);
    newListeners[oldLen] = listener;
    idleListeners = newListeners;
  }

  /**
   * Analyze the specified library, and keep that analysis current by tracking any changes. Also see
   * {@link Context#resolve(File, ResolveCallback)}.
   * 
   * @param file the library file (not <code>null</code>)
   */
  public void analyze(File file) {
    if (!file.isAbsolute()) {
      throw new IllegalArgumentException("File path must be absolute: " + file);
    }
    synchronized (queue) {
      if (!libraryFiles.contains(file)) {
        libraryFiles.add(file);
        // Append analysis task to the end of the queue so that any user requests take precedence
        queueAnalyzeContext();
      }
    }
  }

  /**
   * Called when a file or directory has been added or removed or file content has been modified.
   * Use {@link #discard(File)} if the file or directory content should no longer be analyzed.
   * 
   * @param file the file or directory (not <code>null</code>)
   */
  public void changed(File file) {
    queueNewTask(new FileChangedTask(this, savedContext, file));
  }

  /**
   * Stop analyzing the specified library or all libraries in the specified directory tree.
   * 
   * @param file the library file (not <code>null</code>)
   */
  public void discard(File file) {

    // If this is a dart file, then discard the library

    if (file.isFile() || (!file.exists() && DartCore.isDartLikeFileName(file.getName()))) {
      synchronized (queue) {
        libraryFiles.remove(file);
        queueNewTask(new DiscardLibraryTask(this, savedContext, file));
      }
      return;
    }

    // Otherwise, discard all libraries in the specified directory tree

    String prefix = file.getAbsolutePath() + File.separator;
    synchronized (queue) {
      Iterator<File> iter = libraryFiles.iterator();
      while (iter.hasNext()) {
        File libraryFile = iter.next();
        if (libraryFile.getPath().startsWith(prefix)) {
          iter.remove();
          queueNewTask(new DiscardLibraryTask(this, savedContext, libraryFile));
        }
      }
    }
  }

  /**
   * Answer the context containing analysis of Dart source currently being edited
   */
  public EditContext getEditContext() {
    return editContext;
  }

  /**
   * Answer the context containing analysis of Dart source on disk
   */
  public SavedContext getSavedContext() {
    return savedContext;
  }

  /**
   * Answer <code>true</code> if the receiver does not have any queued tasks and the receiver's
   * background thread is waiting for new tasks to be queued.
   */
  public boolean isIdle() {
    synchronized (queue) {
      return isBackgroundThreadIdle && queue.isEmpty();
    }
  }

  /**
   * Reload the cached information from the previous session. This method must be called before
   * {@link #start()} has been called when the server is not yet running.
   * 
   * @return <code>true</code> if the cached information was successfully loaded, else
   *         <code>false</code>
   */
  public boolean readCache() {
    try {
      return readCache(getAnalysisStateFile());
    } catch (IOException e) {
      DartCore.logError("Failed to read analysis cache: " + getAnalysisStateFile(), e);
      reanalyze();
      return false;
    }
  }

  /**
   * Called when all cached information should be discarded and all libraries reanalyzed
   */
  public void reanalyze() {
    queueNewTask(new EverythingChangedTask(this, savedContext));
  }

  public void removeIdleListener(IdleListener listener) {
    int oldLen = idleListeners.length;
    for (int i = 0; i < oldLen; i++) {
      if (idleListeners[i] == listener) {
        IdleListener[] newListeners = new IdleListener[oldLen - 1];
        System.arraycopy(idleListeners, 0, newListeners, 0, i);
        System.arraycopy(idleListeners, i + 1, newListeners, i, oldLen - 1 - i);
        return;
      }
    }
  }

  /**
   * Scan the specified file or recursively scan the specified directory for libraries to analyze.
   * If the fullScan parameter is <code>false</code> and the scan takes too long or includes too
   * many bytes of Dart source code, then the scan will stop and the specified folder marked so that
   * it will not be analyzed.
   * 
   * @param file the file or directory of files to scan (not <code>null</code>)
   * @param fullScan <code>true</code> if the scan should recurse infinitely deep and for however
   *          long the scan takes or <code>false</code> if the scan should stop once the time or
   *          size threshold has been reached.
   */
  public void scan(File file, boolean fullScan) {
    queueNewTask(new LibraryScanTask(this, savedContext, file, fullScan));
  }

  /**
   * Start the background analysis process if it has not already been started
   */
  public void start() {
    synchronized (queue) {
      if (analyze) {
        return;
      }
      analyze = true;
      backgroundThread = new Thread(new Runnable() {

        @Override
        public void run() {
          try {

            while (analyze) {

              // Get a task from the queue or null if the queue is empty
              // and determine if the thread has changed idle state
              Task task = null;
              boolean notify = false;
              synchronized (queue) {
                if (queue.size() > 0) {
                  queueIndex = 0;
                  task = queue.remove(0);
                  if (isBackgroundThreadIdle) {
                    isBackgroundThreadIdle = false;
                    notify = true;
                  }
                } else {
                  if (!isBackgroundThreadIdle) {
                    isBackgroundThreadIdle = true;
                    notify = true;
                  }
                }
              }

              // Notify others if the receiver's idle state has changed
              if (notify) {
                notifyIdle(task == null);
              }

              // Perform the task or wait for a new task to be added to the queue
              if (task != null) {
                try {
                  task.perform();
                } catch (Throwable e) {
                  DartCore.logError("Analysis Task Exception", e);
                }
              } else {
                synchronized (queue) {
                  if (analyze && queue.isEmpty()) {
                    try {
                      queue.wait();
                    } catch (InterruptedException e) {
                      //$FALL-THROUGH$
                    }
                  }
                }
              }

            }

            // Ensure #stop method is unblocked and notify others that idle state has changed
            boolean notify = false;
            synchronized (queue) {
              if (!isBackgroundThreadIdle) {
                isBackgroundThreadIdle = true;
                notify = true;
                queue.notifyAll();
              }
            }
            if (notify) {
              notifyIdle(true);
            }

          } catch (Throwable e) {
            DartCore.logError("Analysis Server Exception", e);
          }
        }
      }, getClass().getSimpleName());
      backgroundThread.start();
    }
  }

  /**
   * Signal the background analysis thread to stop and block until it does or 5 seconds have passed.
   */
  public void stop() {
    synchronized (queue) {
      analyze = false;
      queue.clear();
      queue.notifyAll();
      long end = System.currentTimeMillis() + 5000;
      while (!isBackgroundThreadIdle) {
        long delta = end - System.currentTimeMillis();
        if (delta <= 0) {
          DartCore.logError("Gave up waiting for " + getClass().getSimpleName() + " to stop");
          break;
        }
        try {
          queue.wait(delta);
        } catch (InterruptedException e) {
          //$FALL-THROUGH$
        }
      }
    }
  }

  /**
   * Write the cached information to the file used to store analysis state between sessions. This
   * method must be called after {@link #stop()} has been called when the server is not running.
   * 
   * @return <code>true</code> if successful, else false
   */
  public boolean writeCache() {
    try {
      writeCache(getAnalysisStateFile());
      return true;
    } catch (IOException e) {
      DartCore.logError("Failed to write analysis cache: " + getAnalysisStateFile(), e);
      return false;
    }
  }

  IdleListener[] getIdleListeners() {
    return idleListeners;
  }

  EditorLibraryManager getLibraryManager() {
    return libraryManager;
  }

  /**
   * Answer the library files identified by {@link #analyze(File)}
   * 
   * @return an array of files (not <code>null</code>, contains no <code>null</code>s)
   */
  File[] getTrackedLibraryFiles() {
    synchronized (queue) {
      return libraryFiles.toArray(new File[libraryFiles.size()]);
    }
  }

  /**
   * Ensure that all libraries have been analyzed by adding an instance of
   * {@link AnalyzeContextTask} to the end of the queue if it has not already been added.
   */
  void queueAnalyzeContext() {
    synchronized (queue) {
      int index = queue.size() - 1;
      if (index >= 0) {
        Task lastTask = queue.get(index);
        if (lastTask instanceof AnalyzeContextTask) {
          return;
        }
      } else {
        index = 0;
      }
      queue.add(index, new AnalyzeContextTask(this, savedContext));
      queue.notifyAll();
    }
  }

  /**
   * Add a priority task to the front of the queue. Should *not* be called by the current task being
   * performed... use {@link #queueSubTask(Task)} instead.
   */
  void queueNewTask(Task task) {
    if (analyze) {
      synchronized (queue) {
        int index = 0;
        if (!task.isPriority()) {
          while (index < queue.size() && queue.get(index).isPriority()) {
            index++;
          }
        }
        queue.add(index, task);
        queueIndex++;
        queue.notifyAll();
      }
    }
  }

  /**
   * Used by the current task being performed to add subtasks in a way that will not reduce the
   * priority of new tasks that have been queued while the current task is executing
   */
  void queueSubTask(Task subtask) {
    if (analyze) {
      if (Thread.currentThread() != backgroundThread) {
        throw new IllegalStateException();
      }
      synchronized (queue) {
        queue.add(queueIndex, subtask);
        queueIndex++;
      }
    }
  }

  /**
   * Remove any tasks related to analysis that do not have callbacks. The assumption is that
   * analysis tasks with explicit callbacks are related to user requests and should be preserved.
   * This should only be called from the background thread.
   */
  void removeAllBackgroundAnalysisTasks() {
    if (Thread.currentThread() != backgroundThread) {
      throw new IllegalStateException();
    }
    synchronized (queue) {
      Iterator<Task> iter = queue.iterator();
      while (iter.hasNext()) {
        if (iter.next().isBackgroundAnalysis()) {
          iter.remove();
        }
      }
    }
  }

  /**
   * Resolve the specified path to a file.
   * 
   * @return the file or <code>null</code> if it could not be resolved
   */
  File resolvePath(URI base, String relPath) {
    if (relPath == null) {
      return null;
    }
    if (SystemLibraryManager.isDartSpec(relPath) || SystemLibraryManager.isPackageSpec(relPath)) {
      URI relativeUri;
      try {
        relativeUri = new URI(relPath);
      } catch (URISyntaxException e) {
        DartCore.logError("Failed to create URI: " + relPath, e);
        return null;
      }
      URI resolveUri = libraryManager.resolveDartUri(relativeUri);
      if (resolveUri == null) {
        return null;
      }
      return new File(resolveUri.getPath());
    }
    File file = new File(relPath);
    if (file.isAbsolute()) {
      return file;
    }
    try {
      String path = base.resolve(new URI(null, null, relPath, null)).normalize().getPath();
      if (path != null) {
        return new File(path);
      }
    } catch (URISyntaxException e) {
      //$FALL-THROUGH$
    }
    return null;
  }

  private File getAnalysisStateFile() {
    return new File(DartCore.getPlugin().getStateLocation().toFile(), "analysis.cache");
  }

  /**
   * TESTING: Answer <code>true</code> if the receiver has cached information about the specified
   * library.
   * 
   * @param file the library file (not <code>null</code>)
   */
  @SuppressWarnings("unused")
  private boolean isLibraryCached(File file) {
    synchronized (queue) {
      return savedContext.getCachedLibrary(file) != null;
    }
  }

  private void notifyIdle(boolean idle) {
    for (IdleListener listener : getIdleListeners()) {
      try {
        listener.idle(idle);
      } catch (Throwable e) {
        DartCore.logError("Exception during idle notification", e);
      }
    }
  }

  /**
   * Reload the cached information from the specified file. This method must be called before
   * {@link #start()} has been called when the server is not yet running.
   * 
   * @return <code>true</code> if the cached information was successfully loaded, else
   *         <code>false</code>
   */
  private boolean readCache(File cacheFile) throws IOException {
    if (analyze) {
      throw new IllegalStateException();
    }
    if (cacheFile == null || !cacheFile.isFile()) {
      return false;
    }
    LineNumberReader reader;
    try {
      reader = new LineNumberReader(new BufferedReader(new FileReader(cacheFile)));
    } catch (FileNotFoundException e) {
      DartCore.logError("Failed to open analysis cache: " + cacheFile);
      return false;
    }
    try {
      if (!CACHE_FILE_VERSION_TAG.equals(reader.readLine())) {
        return false;
      }
      while (true) {
        String path = reader.readLine();
        if (path == null) {
          throw new IOException("Expected " + END_LIBRARIES_TAG + " but found EOF");
        }
        if (path.equals(END_LIBRARIES_TAG)) {
          break;
        }
        libraryFiles.add(new File(path));
      }
      savedContext.readCache(reader);
      queueAnalyzeContext();
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        DartCore.logError("Failed to close analysis cache: " + cacheFile);
      }
    }
    return true;
  }

  /**
   * Write the cached information to the specified file. This method must be called after
   * {@link #stop()} has been called when the server is not running.
   */
  private void writeCache(File cacheFile) throws IOException {
    if (analyze) {
      throw new IllegalStateException();
    }
    PrintWriter writer = new PrintWriter(cacheFile);
    try {
      writer.println(CACHE_FILE_VERSION_TAG);
      for (File libFile : libraryFiles) {
        writer.println(libFile.getPath());
      }
      writer.println(END_LIBRARIES_TAG);
      savedContext.writeCache(writer);
    } finally {
      writer.close();
    }
  }
}
