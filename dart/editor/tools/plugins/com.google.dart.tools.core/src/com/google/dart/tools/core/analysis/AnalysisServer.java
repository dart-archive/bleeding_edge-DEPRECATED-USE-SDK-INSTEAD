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

import java.io.File;

/**
 * Provides analysis of Dart code for Dart editor
 */
public interface AnalysisServer {

  /**
   * Add an object to be notified when there are no tasks queued (or analyzing is <code>false</code>
   * ) and no tasks being performed.
   * 
   * @param listener the object to be notified
   */
  void addIdleListener(IdleListener listener);

  /**
   * Analyze the specified library, and keep that analysis current by tracking any changes. Also see
   * {@link Context#resolve(File, ResolveCallback)}.
   * 
   * @param libraryFile the library file (not <code>null</code>)
   */
  void analyze(File libraryFile);

  /**
   * Called when file content has been modified or anything in the "packages" directory has changed.
   * Use {@link #discard(File)} if the file or directory content should no longer be analyzed.
   * 
   * @param file the file or directory (not <code>null</code>)
   */
  void changed(File file);

  /**
   * Stop analyzing the specified library or all libraries in the specified directory tree.
   * 
   * @param file the library file (not <code>null</code>)
   */
  void discard(File file);

  /**
   * Answer analysis statistics.
   */
  String getAnalysisStatus(String message);

  /**
   * Answer the context containing analysis of Dart source currently being edited
   */
  EditContext getEditContext();

  /**
   * Answer the context containing analysis of Dart source on disk
   */
  SavedContext getSavedContext();

  /**
   * Answer the library files identified by {@link #analyze(File)}
   * 
   * @return an array of files (not <code>null</code>, contains no <code>null</code>s)
   */
  File[] getTrackedLibraryFiles();

  /**
   * Answer <code>true</code> if the receiver does not have any queued tasks and the receiver's
   * background thread is waiting for new tasks to be queued.
   */
  boolean isIdle();

  /**
   * TESTING: Answer <code>true</code> if information about the specified library is cached
   * 
   * @param file the library file (not <code>null</code>)
   */
  boolean isLibraryCached(File file);

  /**
   * TESTING: Answer <code>true</code> if specified library has been resolved
   * 
   * @param file the library file (not <code>null</code>)
   */
  boolean isLibraryResolved(File file);

  /**
   * Ensure that all libraries have been analyzed by adding an instance of
   * {@link AnalyzeContextTask} to the end of the queue if it has not already been added.
   */
  void queueAnalyzeContext();

  /**
   * Queue sub task to analyze the specified library.
   */
  void queueAnalyzeSubTask(File libraryFile);

  /**
   * Queue sub task to analyze the specified library if it is not already analyzed.
   */
  void queueAnalyzeSubTaskIfNew(File libraryFile);

  /**
   * Add a priority task to the front of the queue. Should *not* be called by the current task being
   * performed... use {@link #queueSubTask(Task)} instead.
   */
  void queueNewTask(Task task);

  /**
   * Used by the current task being performed to add subtasks in a way that will not reduce the
   * priority of new tasks that have been queued while the current task is executing
   */
  void queueSubTask(Task subtask);

  /**
   * Reload the cached information from the previous session. This method must be called before
   * {@link #start()} has been called when the server is not yet running.
   * 
   * @return <code>true</code> if the cached information was successfully loaded, else
   *         <code>false</code>
   */
  boolean readCache();

  /**
   * Called when all cached information should be discarded and all libraries reanalyzed. No
   * {@link AnalysisListener#discarded(AnalysisEvent)} events are sent when the information is
   * discarded.
   */
  void reanalyze();

  /**
   * Remove any tasks related to analysis of the specified file or directory and that do not have
   * callbacks. The assumption is that analysis tasks with explicit callbacks are related to user
   * requests and should be preserved. This should only be called from the background thread.
   * 
   * @param discarded the file or directory tree being affected (not <code>null</code>)
   */
  void removeBackgroundTasks(File discarded);

  void removeIdleListener(IdleListener listener);

  /**
   * Scan the specified file or recursively scan the specified directory for libraries to analyze.
   * 
   * @param file the file or directory of files to scan (not <code>null</code>).
   * @param milliseconds the number of milliseconds to wait for the scan to complete.
   * @return <code>true</code> if the scan completed in the specified amount of time.
   */
  boolean scan(File file, long milliseconds);

  /**
   * Scan the specified file or recursively scan the specified directory for libraries to analyze.
   * The callback is used to report progress and check if the operation has been canceled.
   * 
   * @param file the file or directory of files to scan (not <code>null</code>)
   * @param callback for reporting progress and canceling the operation.
   */
  void scan(File file, ScanCallback callback);

  /**
   * Start the background analysis process if it has not already been started
   */
  void start();

  /**
   * Start processing low priority tasks when there are no requests
   */
  void startIdleTaskProcessing();

  /**
   * Signal the background analysis thread to stop and wait for up to 5 seconds for it to do so.
   */
  void stop();

  /**
   * Wait up to the specified number of milliseconds for the receiver to be idle. If the specified
   * number is less than or equal to zero, then this method returns immediately.
   * 
   * @param milliseconds the number of milliseconds to wait for idle
   * @return <code>true</code> if the receiver is idle
   */
  boolean waitForIdle(long milliseconds);

  /**
   * Write the cached information to the file used to store analysis state between sessions. This
   * method must be called after {@link #stop()} has been called when the server is not running.
   * 
   * @return <code>true</code> if successful, else false
   */
  boolean writeCache();
}
