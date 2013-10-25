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
import com.google.dart.engine.utilities.general.ObjectUtilities;

import java.util.Comparator;

/**
 * Instances of the class {@code AnalysisError} represent an error discovered during the analysis of
 * some Dart code.
 * 
 * @see AnalysisErrorListener
 * @coverage dart.engine.error
 */
public class AnalysisError {
  /**
   * An empty array of errors used when no errors are expected.
   */
  public static final AnalysisError[] NO_ERRORS = new AnalysisError[0];

  /**
   * A {@link Comparator} that sorts by the name of the file that the {@link AnalysisError} was
   * found.
   */
  public static final Comparator<AnalysisError> FILE_COMPARATOR = new Comparator<AnalysisError>() {
    @Override
    public int compare(AnalysisError o1, AnalysisError o2) {
      return o1.getSource().getShortName().compareTo(o2.getSource().getShortName());
    }
  };

  /**
   * A {@link Comparator} that sorts error codes first by their severity (errors first, warnings
   * second), and then by the the error code type.
   */
  public static final Comparator<AnalysisError> ERROR_CODE_COMPARATOR = new Comparator<AnalysisError>() {
    @Override
    public int compare(AnalysisError o1, AnalysisError o2) {
      ErrorCode errorCode1 = o1.getErrorCode();
      ErrorCode errorCode2 = o2.getErrorCode();
      ErrorSeverity errorSeverity1 = errorCode1.getErrorSeverity();
      ErrorSeverity errorSeverity2 = errorCode2.getErrorSeverity();
      ErrorType errorType1 = errorCode1.getType();
      ErrorType errorType2 = errorCode2.getType();
      if (errorSeverity1.equals(errorSeverity2)) {
        return errorType1.compareTo(errorType2);
      } else {
        return errorSeverity2.compareTo(errorSeverity1);
      }
    }
  };

  /**
   * The error code associated with the error.
   */
  private ErrorCode errorCode;

  /**
   * The localized error message.
   */
  private String message;

  /**
   * The correction to be displayed for this error, or {@code null} if there is no correction
   * information for this error.
   */
  private String correction;

  /**
   * The source in which the error occurred, or {@code null} if unknown.
   */
  private Source source;

  /**
   * The character offset from the beginning of the source (zero based) where the error occurred.
   */
  private int offset = 0;

  /**
   * The number of characters from the offset to the end of the source which encompasses the
   * compilation error.
   */
  private int length = 0;

  /**
   * A flag indicating whether this error can be shown to be a non-issue because of the result of
   * type propagation.
   */
  private boolean isStaticOnly = false;

  /**
   * Initialize a newly created analysis error for the specified source. The error has no location
   * information.
   * 
   * @param source the source for which the exception occurred
   * @param errorCode the error code to be associated with this error
   * @param arguments the arguments used to build the error message
   */
  public AnalysisError(Source source, ErrorCode errorCode, Object... arguments) {
    this.source = source;
    this.errorCode = errorCode;
    this.message = String.format(errorCode.getMessage(), arguments);
  }

  /**
   * Initialize a newly created analysis error for the specified source at the given location.
   * 
   * @param source the source for which the exception occurred
   * @param offset the offset of the location of the error
   * @param length the length of the location of the error
   * @param errorCode the error code to be associated with this error
   * @param arguments the arguments used to build the error message
   */
  public AnalysisError(Source source, int offset, int length, ErrorCode errorCode,
      Object... arguments) {
    this.source = source;
    this.offset = offset;
    this.length = length;
    this.errorCode = errorCode;
    this.message = String.format(errorCode.getMessage(), arguments);
    String correctionTemplate = errorCode.getCorrection();
    if (correctionTemplate != null) {
      this.correction = String.format(correctionTemplate, arguments);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    // prepare other AnalysisError
    if (!(obj instanceof AnalysisError)) {
      return false;
    }
    AnalysisError other = (AnalysisError) obj;
    // Quick checks.
    if (errorCode != other.errorCode) {
      return false;
    }
    if (offset != other.offset || length != other.length) {
      return false;
    }
    if (isStaticOnly != other.isStaticOnly) {
      return false;
    }
    // Deep checks.
    if (!ObjectUtilities.equals(message, other.message)) {
      return false;
    }
    if (!ObjectUtilities.equals(source, other.source)) {
      return false;
    }
    // OK
    return true;
  }

  /**
   * Return the correction to be displayed for this error, or {@code null} if there is no correction
   * information for this error. The correction should indicate how the user can fix the error.
   * 
   * @return the template used to create the correction to be displayed for this error
   */
  public String getCorrection() {
    return correction;
  }

  /**
   * Return the error code associated with the error.
   * 
   * @return the error code associated with the error
   */
  public ErrorCode getErrorCode() {
    return errorCode;
  }

  /**
   * Return the number of characters from the offset to the end of the source which encompasses the
   * compilation error.
   * 
   * @return the length of the error location
   */
  public int getLength() {
    return length;
  }

  /**
   * Return the message to be displayed for this error. The message should indicate what is wrong
   * and why it is wrong.
   * 
   * @return the message to be displayed for this error
   */
  public String getMessage() {
    return message;
  }

  /**
   * Return the character offset from the beginning of the source (zero based) where the error
   * occurred.
   * 
   * @return the offset to the start of the error location
   */
  public int getOffset() {
    return offset;
  }

  /**
   * Return the value of the given property, or {@code null} if the given property is not defined
   * for this error.
   * 
   * @param property the property whose value is to be returned
   * @return the value of the given property
   */
  public Object getProperty(ErrorProperty property) {
    return null;
  }

  /**
   * Return the source in which the error occurred, or {@code null} if unknown.
   * 
   * @return the source in which the error occurred
   */
  public Source getSource() {
    return source;
  }

  @Override
  public int hashCode() {
    int hashCode = offset;
    hashCode ^= (message != null) ? message.hashCode() : 0;
    hashCode ^= (source != null) ? source.hashCode() : 0;
    return hashCode;
  }

  /**
   * Return {@code true} if this error can be shown to be a non-issue because of the result of type
   * propagation.
   * 
   * @return {@code true} if this error can be shown to be a non-issue
   */
  public boolean isStaticOnly() {
    return isStaticOnly;
  }

  /**
   * Set whether this error can be shown to be a non-issue because of the result of type propagation
   * to the given value.
   * 
   * @param isStaticOnly {@code true} if this error can be shown to be a non-issue
   */
  public void setIsStaticOnly(boolean isStaticOnly) {
    this.isStaticOnly = isStaticOnly;
  }

  /**
   * Set the source in which the error occurred to the given source.
   * 
   * @param source the source in which the error occurred
   */
  public void setSource(Source source) {
    this.source = source;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append((source != null) ? source.getFullName() : "<unknown source>");
    builder.append("(");
    builder.append(offset);
    builder.append("..");
    builder.append(offset + length - 1);
    builder.append("): ");
    //builder.append("(" + lineNumber + ":" + columnNumber + "): ");
    builder.append(message);
    return builder.toString();
  }
}
