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
 * Instances of the class {@code AnalysisStatus} represent the current state of analysis.
 * 
 * @coverage dart.server
 */
public class AnalysisStatus {
  /**
   * A flag indicating whether analysis is currently being performed.
   */
  private boolean isAnalyzing;

  /**
   * The name of the current target of analysis.
   */
  private String analysisTarget;

  /**
   * Initialize a newly created status object.
   */
  public AnalysisStatus(boolean isAnalyzing, String analysisTarget) {
    this.isAnalyzing = isAnalyzing;
    this.analysisTarget = analysisTarget;
  }

  /**
   * Return the name of the current target of analysis, {@code null} if {@link #isAnalyzing} is
   * {@code false}.
   * 
   * @return the name of the current target of analysis
   */
  public String getAnalysisTarget() {
    return analysisTarget;
  }

  /**
   * Return {@code true} if analysis is currently being performed.
   * 
   * @return {@code true} if analysis is currently being performed
   */
  public boolean isAnalyzing() {
    return isAnalyzing;
  }
}
