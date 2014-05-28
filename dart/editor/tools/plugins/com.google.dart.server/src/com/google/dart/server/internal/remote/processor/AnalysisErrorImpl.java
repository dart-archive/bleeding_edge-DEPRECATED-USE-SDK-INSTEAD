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

/**
 * An implementation of {@link AnalysisError}.
 * 
 * @coverage dart.server.remote
 */
public class AnalysisErrorImpl implements AnalysisError {
  private final String file;
  private final ErrorCode errorCode;
  private final int offset;
  private final int length;
  private final String message;
  private final String correction;

  public AnalysisErrorImpl(String file, ErrorCode errorCode, int offset, int length,
      String message, String correction) {
    this.file = file;
    this.errorCode = errorCode;
    this.offset = offset;
    this.length = length;
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
  public String getFile() {
    return file;
  }

  @Override
  public int getLength() {
    return length;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public int getOffset() {
    return offset;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[file=");
    builder.append(file);
    builder.append(", errorCode=");
    builder.append(errorCode);
    builder.append(", offset=");
    builder.append(offset);
    builder.append(", length=");
    builder.append(length);
    builder.append(", message=");
    builder.append(message);
    builder.append(", correction=");
    builder.append(correction);
    builder.append("]");
    return builder.toString();
  }
}
