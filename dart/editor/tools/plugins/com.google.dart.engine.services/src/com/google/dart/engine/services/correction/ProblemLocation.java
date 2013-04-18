/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.engine.services.correction;

import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.source.Source;

/**
 * Information about problem, another presentation of {@link AnalysisError}.
 */
public final class ProblemLocation {
  private final ErrorCode errorCode;
  private final int offset;
  private final int length;
  private final String message;

  public ProblemLocation(ErrorCode errorCode, int offset, int length, String message) {
    this.errorCode = errorCode;
    this.offset = offset;
    this.length = length;
    this.message = message;
  }

  /**
   * @return the {@link ErrorCode} of the problem. Not {@code null}.
   */
  public ErrorCode getErrorCode() {
    return errorCode;
  }

  /**
   * @return the length of the {@link Source} part with the problem.
   */
  public int getLength() {
    return length;
  }

  /**
   * @return the message of the problem which is displayed for user. Not {@code null}.
   */
  public String getMessage() {
    return message;
  }

  /**
   * @return the offset of the problem.
   */
  public int getOffset() {
    return offset;
  }
}
