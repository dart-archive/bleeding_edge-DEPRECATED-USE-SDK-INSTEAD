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
 * A list of fixes associated with a specific error
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class ErrorFixes {

  /**
   * The error with which the fixes are associated.
   */
  private final AnalysisError error;

  /**
   * The fixes associated with the error.
   */
  private final List<SourceChange> fixes;

  /**
   * Constructor for {@link ErrorFixes}.
   */
  public ErrorFixes(AnalysisError error, List<SourceChange> fixes) {
    this.error = error;
    this.fixes = fixes;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ErrorFixes) {
      ErrorFixes other = (ErrorFixes) obj;
      return
        ObjectUtilities.equals(other.error, error) &&
        ObjectUtilities.equals(other.fixes, fixes);
    }
    return false;
  }

  /**
   * The error with which the fixes are associated.
   */
  public AnalysisError getError() {
    return error;
  }

  /**
   * The fixes associated with the error.
   */
  public List<SourceChange> getFixes() {
    return fixes;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("error=");
    builder.append(error.toString() + ", ");
    builder.append("fixes=");
    builder.append(StringUtils.join(fixes, ", ") + ", ");
    builder.append("]");
    return builder.toString();
  }

}
