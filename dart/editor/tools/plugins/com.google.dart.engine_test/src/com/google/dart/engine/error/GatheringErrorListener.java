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
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.engine.utilities.source.LineInfo;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Instances of the class {@code GatheringErrorListener} implement an error listener that collects
 * all of the errors passed to it for later examination.
 */
public class GatheringErrorListener implements AnalysisErrorListener {
  /**
   * The source being parsed.
   */
  private String rawSource;

  /**
   * The source being parsed after inserting a marker at the beginning and end of the range of the
   * most recent error.
   */
  @SuppressWarnings("unused")
  private String markedSource;

  /**
   * A list containing the errors that were collected.
   */
  private List<AnalysisError> errors = new ArrayList<AnalysisError>();

  /**
   * A table mapping sources to the line information for the source.
   */
  private HashMap<Source, LineInfo> lineInfoMap = new HashMap<Source, LineInfo>();

  /**
   * An empty array of errors used when no errors are expected.
   */
  private static final AnalysisError[] NO_ERRORS = new AnalysisError[0];

  /**
   * Initialize a newly created error listener to collect errors.
   */
  public GatheringErrorListener() {
    super();
  }

  /**
   * Initialize a newly created error listener to collect errors.
   */
  public GatheringErrorListener(String rawSource) {
    this.rawSource = rawSource;
    this.markedSource = rawSource;
  }

  /**
   * Assert that the number of errors that have been gathered matches the number of errors that are
   * given and that they have the expected error codes and locations. The order in which the errors
   * were gathered is ignored.
   * 
   * @param errorCodes the errors that should have been gathered
   * @throws AssertionFailedError if a different number of errors have been gathered than were
   *           expected or if they do not have the same codes and locations
   */
  public void assertErrors(AnalysisError... expectedErrors) {
    if (errors.size() != expectedErrors.length) {
      fail(expectedErrors);
    }
    List<AnalysisError> remainingErrors = new ArrayList<AnalysisError>();
    for (AnalysisError error : expectedErrors) {
      remainingErrors.add(error);
    }
    for (AnalysisError error : errors) {
      if (!foundAndRemoved(remainingErrors, error)) {
        fail(expectedErrors);
      }
    }
  }

  /**
   * Assert that the number of errors that have been gathered matches the number of errors that are
   * given and that they have the expected error codes. The order in which the errors were gathered
   * is ignored.
   * 
   * @param expectedErrorCodes the error codes of the errors that should have been gathered
   * @throws AssertionFailedError if a different number of errors have been gathered than were
   *           expected
   */
  public void assertErrors(ErrorCode... expectedErrorCodes) {
    StringBuilder builder = new StringBuilder();
    //
    // Compute the expected number of each type of error.
    //
    HashMap<ErrorCode, Integer> expectedCounts = new HashMap<ErrorCode, Integer>();
    for (ErrorCode code : expectedErrorCodes) {
      Integer count = expectedCounts.get(code);
      if (count == null) {
        count = Integer.valueOf(1);
      } else {
        count = Integer.valueOf(count.intValue() + 1);
      }
      expectedCounts.put(code, count);
    }
    //
    // Compute the actual number of each type of error.
    //
    HashMap<ErrorCode, ArrayList<AnalysisError>> errorsByCode = new HashMap<ErrorCode, ArrayList<AnalysisError>>();
    for (AnalysisError error : errors) {
      ErrorCode code = error.getErrorCode();
      ArrayList<AnalysisError> list = errorsByCode.get(code);
      if (list == null) {
        list = new ArrayList<AnalysisError>();
        errorsByCode.put(code, list);
      }
      list.add(error);
    }
    //
    // Compare the expected and actual number of each type of error.
    //
    for (Map.Entry<ErrorCode, Integer> entry : expectedCounts.entrySet()) {
      ErrorCode code = entry.getKey();
      int expectedCount = entry.getValue().intValue();
      int actualCount;
      ArrayList<AnalysisError> list = errorsByCode.remove(code);
      if (list == null) {
        actualCount = 0;
      } else {
        actualCount = list.size();
      }
      if (actualCount != expectedCount) {
        if (builder.length() == 0) {
          builder.append("Expected ");
        } else {
          builder.append("; ");
        }
        builder.append(expectedCount);
        builder.append(" errors of type ");
        builder.append(code);
        builder.append(", found ");
        builder.append(actualCount);
      }
    }
    //
    // Check that there are no more errors in the actual-errors map, otherwise, record message.
    //
    for (Map.Entry<ErrorCode, ArrayList<AnalysisError>> entry : errorsByCode.entrySet()) {
      ErrorCode code = entry.getKey();
      ArrayList<AnalysisError> actualErrors = entry.getValue();
      int actualCount = actualErrors.size();
      if (builder.length() == 0) {
        builder.append("Expected ");
      } else {
        builder.append("; ");
      }
      builder.append("0 errors of type ");
      builder.append(code);
      builder.append(", found ");
      builder.append(actualCount);
      builder.append(" (");
      for (int i = 0; i < actualErrors.size(); i++) {
        AnalysisError error = actualErrors.get(i);
        if (i > 0) {
          builder.append(", ");
        }
        builder.append(error.getOffset());
      }
      builder.append(")");
    }
    if (builder.length() > 0) {
      Assert.fail(builder.toString());
    }
  }

  /**
   * Assert that the number of errors that have been gathered matches the number of severities that
   * are given and that there are the same number of errors and warnings as specified by the
   * argument. The order in which the errors were gathered is ignored.
   * 
   * @param expectedSeverities the severities of the errors that should have been gathered
   * @throws AssertionFailedError if a different number of errors have been gathered than were
   *           expected
   */
  public void assertErrors(ErrorSeverity... expectedSeverities) {
    int expectedErrorCount = 0;
    int expectedWarningCount = 0;
    for (ErrorSeverity severity : expectedSeverities) {
      if (severity == ErrorSeverity.ERROR) {
        expectedErrorCount++;
      } else {
        expectedWarningCount++;
      }
    }
    int actualErrorCount = 0;
    int actualWarningCount = 0;
    for (AnalysisError error : errors) {
      if (error.getErrorCode().getErrorSeverity() == ErrorSeverity.ERROR) {
        actualErrorCount++;
      } else {
        actualWarningCount++;
      }
    }
    if (expectedErrorCount != actualErrorCount || expectedWarningCount != actualWarningCount) {
      Assert.fail("Expected " + expectedErrorCount + " errors and " + expectedWarningCount
          + " warnings, found " + actualErrorCount + " errors and " + actualWarningCount
          + " warnings");
    }
  }

  /**
   * Assert that no errors have been gathered.
   * 
   * @throws AssertionFailedError if any errors have been gathered
   */
  public void assertNoErrors() {
    assertErrors(NO_ERRORS);
  }

  /**
   * Return the errors that were collected.
   * 
   * @return the errors that were collected
   */
  public List<AnalysisError> getErrors() {
    return errors;
  }

  /**
   * Return {@code true} if an error with the given error code has been gathered.
   * 
   * @param errorCode the error code being searched for
   * @return {@code true} if an error with the given error code has been gathered
   */
  public boolean hasError(ErrorCode errorCode) {
    for (AnalysisError error : errors) {
      if (error.getErrorCode() == errorCode) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void onError(AnalysisError error) {
    if (rawSource != null) {
      int left = error.getOffset();
      int right = left + error.getLength() - 1;
      markedSource = rawSource.substring(0, left) + "^" + rawSource.substring(left, right) + "^"
          + rawSource.substring(right);
    }
    errors.add(error);
  }

  /**
   * Set the line information associated with the given source to the given information.
   * 
   * @param source the source with which the line information is associated
   * @param lineStarts the line start information to be associated with the source
   */
  public void setLineInfo(Source source, int[] lineStarts) {
    lineInfoMap.put(source, new LineInfo(lineStarts));
  }

  /**
   * Set the line information associated with the given source to the given information.
   * 
   * @param source the source with which the line information is associated
   * @param lineInfo the line information to be associated with the source
   */
  public void setLineInfo(Source source, LineInfo lineInfo) {
    lineInfoMap.put(source, lineInfo);
  }

  /**
   * Return {@code true} if the two errors are equivalent.
   * 
   * @param firstError the first error being compared
   * @param secondError the second error being compared
   * @return {@code true} if the two errors are equivalent
   */
  private boolean equals(AnalysisError firstError, AnalysisError secondError) {
    return firstError.getErrorCode() == secondError.getErrorCode()
        && firstError.getOffset() == secondError.getOffset()
        && firstError.getLength() == secondError.getLength()
        && equals(firstError.getSource(), secondError.getSource());
  }

  /**
   * Return {@code true} if the two sources are equivalent.
   * 
   * @param firstSource the first source being compared
   * @param secondSource the second source being compared
   * @return {@code true} if the two sources are equivalent
   */
  private boolean equals(Source firstSource, Source secondSource) {
    if (firstSource == null) {
      return secondSource == null;
    } else if (secondSource == null) {
      return false;
    }
    return firstSource.equals(secondSource);
  }

  /**
   * Assert that the number of errors that have been gathered matches the number of errors that are
   * given and that they have the expected error codes. The order in which the errors were gathered
   * is ignored.
   * 
   * @param errorCodes the errors that should have been gathered
   * @throws AssertionFailedError with
   */
  private void fail(AnalysisError[] expectedErrors) {
    PrintStringWriter writer = new PrintStringWriter();
    writer.print("Expected ");
    writer.print(expectedErrors.length);
    writer.print(" errors:");
    for (AnalysisError error : expectedErrors) {
      Source source = error.getSource();
      LineInfo lineInfo = lineInfoMap.get(source);
      writer.println();
      if (lineInfo == null) {
        int offset = error.getOffset();
        writer.printf(
            "  %s %s (%d..%d)",
            source == null ? "" : source.getShortName(),
            error.getErrorCode(),
            offset,
            offset + error.getLength());
      } else {
        LineInfo.Location location = lineInfo.getLocation(error.getOffset());
        writer.printf(
            "  %s %s (%d, %d/%d)",
            source == null ? "" : source.getShortName(),
            error.getErrorCode(),
            location.getLineNumber(),
            location.getColumnNumber(),
            error.getLength());
      }
    }
    writer.println();
    writer.print("found ");
    writer.print(errors.size());
    writer.print(" errors:");
    for (AnalysisError error : errors) {
      Source source = error.getSource();
      LineInfo lineInfo = lineInfoMap.get(source);
      writer.println();
      if (lineInfo == null) {
        int offset = error.getOffset();
        writer.printf(
            "  %s %s (%d..%d): %s",
            source == null ? "" : source.getShortName(),
            error.getErrorCode(),
            offset,
            offset + error.getLength(),
            error.getMessage());
      } else {
        LineInfo.Location location = lineInfo.getLocation(error.getOffset());
        writer.printf(
            "  %s %s (%d, %d/%d): %s",
            source == null ? "" : source.getShortName(),
            error.getErrorCode(),
            location.getLineNumber(),
            location.getColumnNumber(),
            error.getLength(),
            error.getMessage());
      }
    }
    Assert.fail(writer.toString());
  }

  /**
   * Search through the given list of errors for an error that is equal to the target error. If one
   * is found, remove it from the list and return {@code true}, otherwise return {@code false}
   * without modifying the list.
   * 
   * @param errors the errors through which we are searching
   * @param targetError the error being searched for
   * @return {@code true} if the error is found and removed from the list
   */
  private boolean foundAndRemoved(List<AnalysisError> errors, AnalysisError targetError) {
    for (AnalysisError error : errors) {
      if (equals(error, targetError)) {
        errors.remove(error);
        return true;
      }
    }
    return true;
  }
}
