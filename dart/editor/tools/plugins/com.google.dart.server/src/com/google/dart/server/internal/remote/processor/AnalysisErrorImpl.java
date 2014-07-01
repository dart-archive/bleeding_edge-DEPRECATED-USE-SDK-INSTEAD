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

package com.google.dart.server.internal.remote.processor;

import com.google.dart.engine.error.ErrorCode;
import com.google.dart.server.AnalysisError;
import com.google.dart.server.ErrorSeverity;
import com.google.dart.server.ErrorType;
import com.google.dart.server.Location;

/**
 * An implementation of {@link AnalysisError}.
 * 
 * @coverage dart.server.remote
 */
public class AnalysisErrorImpl implements AnalysisError {
  private final ErrorCode errorCode;
  private final ErrorSeverity errorSeverity;
  private final ErrorType errorType;
  private final Location location;
  private final String message;
  private final String correction;

  public AnalysisErrorImpl(ErrorCode errorCode, ErrorSeverity errorSeverity, ErrorType errorType,
      Location location, String message, String correction) {
    this.errorCode = errorCode;
    this.errorSeverity = errorSeverity;
    this.errorType = errorType;
    this.location = location;
    this.message = message;
    this.correction = correction;
  }

  @Override
  public String getCorrection() {
    return correction;
  }

  @Override
  public ErrorCode getErrorCode() {
    return errorCode;
  }

  @Override
  public ErrorSeverity getErrorSeverity() {
    return errorSeverity;
  }

  @Override
  public ErrorType getErrorType() {
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
    builder.append("[errorCode=");
    builder.append(errorCode);
    builder.append(", location=");
    builder.append(location.toString());
    builder.append(", message=");
    builder.append(message);
    builder.append(", correction=");
    builder.append(correction);
    builder.append("]");
    return builder.toString();
  }
}
