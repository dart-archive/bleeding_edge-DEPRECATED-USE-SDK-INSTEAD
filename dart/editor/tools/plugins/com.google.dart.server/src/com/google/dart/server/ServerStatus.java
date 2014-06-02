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
 * Instances of the class {@code ServerStatus} represent the current state of the server.
 * 
 * @coverage dart.server
 */
public class ServerStatus {
  /**
   * The current status of analysis.
   */
  private AnalysisStatus analysisStatus;

  /**
   * Initialize a newly created status object.
   */
  public ServerStatus() {
    super();
  }

  /**
   * Return the current status of analysis, or {@code null} if the analysis status was not included
   * in the status that was reported.
   * 
   * @return the current status of analysis
   */
  public AnalysisStatus getAnalysisStatus() {
    return analysisStatus;
  }

  /**
   * Set the current status of analysis to the given status.
   * 
   * @param analysisStatus the current status of analysis
   */
  public void setAnalysisStatus(AnalysisStatus analysisStatus) {
    this.analysisStatus = analysisStatus;
  }
}
