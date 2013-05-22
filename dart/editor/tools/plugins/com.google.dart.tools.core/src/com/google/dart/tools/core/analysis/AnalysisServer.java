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
   * Called when all cached information should be discarded and all libraries reanalyzed. No
   * {@link AnalysisListener#discarded(AnalysisEvent)} events are sent when the information is
   * discarded.
   */
  void reanalyze();

  /**
   * Scan the specified file or recursively scan the specified directory for libraries to analyze.
   * 
   * @param file the file or directory of files to scan (not <code>null</code>).
   * @param milliseconds the number of milliseconds to wait for the scan to complete.
   * @return <code>true</code> if the scan completed in the specified amount of time.
   */
  boolean scan(File file, long milliseconds);
}
