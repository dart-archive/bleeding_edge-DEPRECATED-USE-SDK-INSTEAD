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

package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.analysis.model.Project;

/**
 * Editors implement this interface to participate in {@link DartPrioritySourcesHelper}.
 */
public interface DartPriorityFileEditor {
  /**
   * Answer the analysis context to be used when resolving the source displayed in the editor.
   * 
   * @return the {@link AnalysisContext} corresponding to this editor or {@code null} if none
   */
  AnalysisContext getInputAnalysisContext();

  /**
   * Answer the full path of the file being displayed in this editor.
   * 
   * @return the full path or {@code null} if none
   */
  String getInputFilePath();

  /**
   * Answer the project containing the source being displayed in this editor.
   * 
   * @return the {@link Project} or {@code null} if none
   */
  Project getInputProject();

  /**
   * Answer the source being displayed in this editor.
   * 
   * @return the {@link Source} or {@code null} if none
   */
  Source getInputSource();

  /**
   * @return {@code true} if the editor's content is visible
   */
  boolean isVisible();
}
