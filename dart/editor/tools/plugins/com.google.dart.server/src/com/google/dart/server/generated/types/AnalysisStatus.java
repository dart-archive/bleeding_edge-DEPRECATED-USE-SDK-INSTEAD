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
 *
 * This file has been automatically generated.  Please do not edit it manually.
 * To regenerate the file, use the script "pkg/analysis_server/spec/generate_files".
 */
package com.google.dart.server.generated.types;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.google.dart.server.utilities.general.ObjectUtilities;
import org.apache.commons.lang3.StringUtils;

/**
 * An indication of the current state of analysis.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class AnalysisStatus {

  /**
   * An empty array of {@link AnalysisStatus}s.
   */
  public static final AnalysisStatus[] EMPTY_ARRAY = new AnalysisStatus[0];

  /**
   * True if analysis is currently being performed.
   */
  private final Boolean analyzing;

  /**
   * The name of the current target of analysis. This field is omitted if analyzing is false.
   */
  private final String analysisTarget;

  /**
   * Constructor for {@link AnalysisStatus}.
   */
  public AnalysisStatus(Boolean analyzing, String analysisTarget) {
    this.analyzing = analyzing;
    this.analysisTarget = analysisTarget;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof AnalysisStatus) {
      AnalysisStatus other = (AnalysisStatus) obj;
      return
        ObjectUtilities.equals(other.analyzing, analyzing) &&
        ObjectUtilities.equals(other.analysisTarget, analysisTarget);
    }
    return false;
  }

  /**
   * The name of the current target of analysis. This field is omitted if analyzing is false.
   */
  public String getAnalysisTarget() {
    return analysisTarget;
  }

  /**
   * True if analysis is currently being performed.
   */
  public Boolean getAnalyzing() {
    return analyzing;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("analyzing=");
    builder.append(analyzing + ", ");
    builder.append("analysisTarget=");
    builder.append(analysisTarget);
    builder.append("]");
    return builder.toString();
  }

}
