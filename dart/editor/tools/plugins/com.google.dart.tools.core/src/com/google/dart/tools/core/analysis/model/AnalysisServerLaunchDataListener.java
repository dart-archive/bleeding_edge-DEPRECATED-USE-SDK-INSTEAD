/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.tools.core.analysis.model;

/**
 * Used by {@link AnalysisServerData} to notify clients about new "execution.launchData" info.
 * 
 * @coverage dart.tools.core.model
 */
public interface AnalysisServerLaunchDataListener {
  /**
   * New launch data has been computed.
   * 
   * @param file the file for which launch data is being provided
   * @param kind the kind of the executable file, or {@code null} for non-Dart files
   * @param referencedFiles a list of the Dart files that are referenced by the file, or
   *          {@code null} for non-HTML files
   */
  void computedLaunchData(String file, String kind, String[] referencedFiles);
}
