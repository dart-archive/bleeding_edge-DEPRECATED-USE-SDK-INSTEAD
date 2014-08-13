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
 * An indication of an error, warning, or hint that was produced by the analysis.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class AnalysisError {

  /**
   * The correction message to be displayed for this error. The correction message should indicate
   * how the user can fix the error. The field is omitted if there is no correction message
   * associated with the error code.
   */
  private final String correction;

  /**
   * The location associated with the error.
   */
  private final Location location;

  /**
   * The message to be displayed for this error. The message should indicate what is wrong with the
   * code and why it is wrong.
   */
  private final String message;

  /**
   * The severity of the error.
   */
  private final String severity;

  /**
   * The type of the error.
   */
  private final String type;

  /**
   * Constructor for {@link AnalysisError}.
   */
  public AnalysisError(String severity, String type, Location location, String message, String correction) {
    this.severity = severity;
    this.type = type;
    this.location = location;
    this.message = message;
    this.correction = correction;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof AnalysisError) {
      AnalysisError other = (AnalysisError) obj;
      return
        ObjectUtilities.equals(other.severity, severity) &&
        ObjectUtilities.equals(other.type, type) &&
        ObjectUtilities.equals(other.location, location) &&
        ObjectUtilities.equals(other.message, message) &&
        ObjectUtilities.equals(other.correction, correction);
    }
    return false;
  }

  /**
   * The correction message to be displayed for this error. The correction message should indicate
   * how the user can fix the error. The field is omitted if there is no correction message
   * associated with the error code.
   */
  public String getCorrection() {
    return correction;
  }

  /**
   * The location associated with the error.
   */
  public Location getLocation() {
    return location;
  }

  /**
   * The message to be displayed for this error. The message should indicate what is wrong with the
   * code and why it is wrong.
   */
  public String getMessage() {
    return message;
  }

  /**
   * The severity of the error.
   */
  public String getSeverity() {
    return severity;
  }

  /**
   * The type of the error.
   */
  public String getType() {
    return type;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("severity=");
    builder.append(severity.toString() + ", ");
    builder.append("type=");
    builder.append(type.toString() + ", ");
    builder.append("location=");
    builder.append(location.toString() + ", ");
    builder.append("message=");
    builder.append(message.toString() + ", ");
    builder.append("correction=");
    builder.append(correction.toString() + ", ");
    builder.append("]");
    return builder.toString();
  }

}
