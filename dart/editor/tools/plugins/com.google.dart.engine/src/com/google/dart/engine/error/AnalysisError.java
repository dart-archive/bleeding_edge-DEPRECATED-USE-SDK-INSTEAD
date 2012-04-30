/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.engine.error;

import com.google.dart.engine.source.Source;

/**
 * Instances of the class <code>AnalysisError</code> represent an error discovered during the
 * analysis of some Dart code.
 * 
 * @see AnalysisErrorListener
 */
public class AnalysisError {
  /**
   * The error code associated with the error.
   */
  private ErrorCode errorCode;

  /**
   * The compilation error message.
   */
  private String message;

  /**
   * The source in which the error occurred or <code>null</code> if unknown.
   */
  private Source source;

  /**
   * The character offset from the beginning of the source (zero based) where the error occurred.
   */
  private int offset = 0;

  /**
   * The number of characters from the startPosition to the end of the source which encompasses the
   * compilation error.
   */
  private int length = 0;

  /**
   * The line number in the source (one based) where the error occurred or -1 if it is undefined.
   */
  private int lineNumber = -1;

  /**
   * The column number in the source (one based) where the error occurred or -1 if it is undefined.
   */
  private int columnNumber = -1;

//  /**
//   * Compilation error at the {@link SourceInfo} from specified {@link HasSourceInfo}.
//   * 
//   * @param hasSourceInfo the provider of {@link SourceInfo} where the error occurred
//   * @param errorCode the {@link ErrorCode} to be associated with this error
//   * @param arguments the arguments used to build the error message
//   */
//  public AnalysisError(HasSourceInfo hasSourceInfo, ErrorCode errorCode, Object... arguments) {
//    this(hasSourceInfo.getSourceInfo(), errorCode, arguments);
//  }

  /**
   * Compilation error for the specified {@link Source}, without location.
   * 
   * @param source the {@link Source} for which the exception occurred
   * @param errorCode the {@link ErrorCode} to be associated with this error
   * @param arguments the arguments used to build the error message
   */
  public AnalysisError(Source source, ErrorCode errorCode, Object... arguments) {
    this.source = source;
    this.errorCode = errorCode;
    this.message = String.format(errorCode.getMessage(), arguments);
  }

//  /**
//   * Instantiate a new instance representing a compilation error at the specified location.
//   * 
//   * @param source the source reference
//   * @param location the source range where the error occurred
//   * @param errorCode the error code to be associated with this error
//   * @param arguments the arguments used to build the error message
//   */
//  public AnalysisError(Source source, Location location, ErrorCode errorCode, Object... arguments) {
//    this.source = source;
//    this.errorCode = errorCode;
//    this.message = String.format(errorCode.getMessage(), arguments);
//    if (location != null) {
//      Position begin = location.getBegin();
//      if (begin != null) {
//        offset = begin.getPos();
//        lineNumber = begin.getLine();
//        columnNumber = begin.getCol();
//      }
//      Position end = location.getEnd();
//      if (end != null) {
//        length = end.getPos() - offset;
//        if (length < 0) {
//          length = 0;
//        }
//      }
//    }
//  }

//  /**
//   * Compilation error at the specified {@link SourceInfo}.
//   * 
//   * @param sourceInfo the {@link SourceInfo} where the error occurred
//   * @param errorCode the {@link ErrorCode} to be associated with this error
//   * @param arguments the arguments used to build the error message
//   */
//  public AnalysisError(SourceInfo sourceInfo, ErrorCode errorCode, Object... arguments) {
//    this.source = sourceInfo.getSource();
//    this.lineNumber = sourceInfo.getLine();
//    this.columnNumber = sourceInfo.getColumn();
//    this.offset = sourceInfo.getOffset();
//    this.length = sourceInfo.getLength();
//    this.errorCode = errorCode;
//    this.message = String.format(errorCode.getMessage(), arguments);
//  }

  /**
   * Return the column number in the source (one based) where the error occurred.
   */
  public int getColumnNumber() {
    return columnNumber;
  }

  /**
   * Return the error code associated with the error.
   */
  public ErrorCode getErrorCode() {
    return errorCode;
  }

  /**
   * Return the length of the error location.
   */
  public int getLength() {
    return length;
  }

  /**
   * Return the line number in the source (one based) where the error occurred.
   */
  public int getLineNumber() {
    return lineNumber;
  }

  /**
   * Return the compilation error message.
   */
  public String getMessage() {
    return message;
  }

  /**
   * Return the character offset from the beginning of the source (zero based) where the error
   * occurred.
   */
  public int getOffset() {
    return offset;
  }

  /**
   * Return the source in which the error occurred or <code>null</code> if unknown.
   */
  public Source getSource() {
    return source;
  }

  @Override
  public int hashCode() {
    int hashCode = offset;
    hashCode ^= (message != null) ? message.hashCode() : 0;
    hashCode ^= (source != null) ? source.getFile().hashCode() : 0;
    return hashCode;
  }

  /**
   * Set the source in which the error occurred or <code>null</code> if unknown.
   */
  public void setSource(Source source) {
    this.source = source;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append((source != null) ? source.getFile().getAbsolutePath() : "<unknown source>");
    builder.append("(" + lineNumber + ":" + columnNumber + "): ");
    builder.append(message);
    return builder.toString();
  }
}
