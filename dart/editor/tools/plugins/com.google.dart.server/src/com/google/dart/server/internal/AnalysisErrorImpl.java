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

package com.google.dart.server.internal;

import com.google.dart.server.AnalysisError;
import com.google.dart.server.ErrorSeverity;
import com.google.dart.server.Location;
import com.google.dart.server.utilities.general.ObjectUtilities;

/**
 * An implementation of {@link AnalysisError}.
 * 
 * @coverage dart.server
 */
public class AnalysisErrorImpl implements AnalysisError {
  private final ErrorSeverity errorSeverity;
  private final String errorType;
  private final Location location;
  private final String message;
  private final String correction;

  public AnalysisErrorImpl(ErrorSeverity errorSeverity, String errorType, Location location,
      String message, String correction) {
    this.errorSeverity = errorSeverity;
    this.errorType = errorType;
    this.location = location;
    this.message = message;
    this.correction = correction;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || !(o instanceof AnalysisErrorImpl)) {
      return false;
    }
    AnalysisErrorImpl other = (AnalysisErrorImpl) o;
    return other.errorSeverity == errorSeverity
        && ObjectUtilities.equals(other.errorType, errorType)
        && ObjectUtilities.equals(other.location, location)
        && ObjectUtilities.equals(other.message, message)
        && ObjectUtilities.equals(other.correction, correction);
  }

  @Override
  public String getCorrection() {
    return correction;
  }

  @Override
  public ErrorSeverity getErrorSeverity() {
    return errorSeverity;
  }

  @Override
  public String getErrorType() {
    return errorType;
  }

  @Override
  public Location getLocation() {
    return location;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[location=");
    builder.append(location.toString());
    builder.append(", message=");
    builder.append(message);
    builder.append(", correction=");
    builder.append(correction);
    builder.append("]");
    return builder.toString();
  }
}
