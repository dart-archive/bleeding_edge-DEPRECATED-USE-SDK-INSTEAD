/*
 * Copyright 2013 Dart project authors.
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

import com.google.dart.engine.context.AnalysisContext;

/**
 * Used by {@link ProjectManager} to notify others as analysis is process and when it is complete.
 * 
 * @coverage dart.tools.core.model
 */
public interface AnalysisListener {

  /**
   * Called when analysis for a particular {@link AnalysisContext} is complete.
   * 
   * @param event contains information about the completed analysis
   */
  void complete(AnalysisEvent event);

  /**
   * Called when a compilation unit has been resolved.
   * 
   * @param event contains information about the compilation unit that was resolved
   */
  void resolved(ResolvedEvent event);
}
