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
package com.google.dart.server;

/**
 * The interface {@code AnalysisStatus} defines the behavior of objects that indicate the current
 * state of analysis.
 * 
 * @coverage dart.server
 */
public interface AnalysisStatus {
  /**
   * Returns the name of the current target of analysis, {@code null} if {@link #isAnalyzing} is
   * {@code false}.
   */
  String getAnalysisTarget();

  /**
   * Returns {@code true} if analysis is currently being performed.
   */
  boolean isAnalyzing();
}
