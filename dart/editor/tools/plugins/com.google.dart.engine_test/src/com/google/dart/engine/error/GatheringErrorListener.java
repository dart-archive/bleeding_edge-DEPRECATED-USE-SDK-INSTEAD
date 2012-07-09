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
   * A list containing the errors that were collected.
   */
  private List<AnalysisError> errors = new ArrayList<AnalysisError>();

  /**
   * Initialize a newly created error listener to collect errors.
   */
  public GatheringErrorListener() {
    super();
  }

  /**
   * Assert that one error has been gathered and that it has the given given severity.
   * 
   * @param expectedSeverity the severity of the error that should have been gathered
   * @throws AssertionFailedError if there is not exactly one error with the given severity
   */
  public void assertError(ErrorSeverity expectedSeverity) {
    int actualCount = errors.size();
    if (actualCount != 1) {
      Assert.fail("Expected 1 error, found " + actualCount);
    }
    ErrorSeverity actualSeverity = errors.get(0).getErrorCode().getErrorSeverity();
    if (actualSeverity != expectedSeverity) {
      Assert.fail("Expected one " + expectedSeverity + ", found a " + actualSeverity);
    }
  }

  /**
   * Assert that the number of errors that have been gathered matches the number of errors that are
   * given and that they have the expected error codes. The order in which the errors were gathered
   * is ignored.
   * 
   * @param errorCodes the error codes of the errors that should have been gathered
   * @throws AssertionFailedError if a different number of errors have been gathered than were
   *           expected
   */
  public void assertErrors(ErrorCode[] errorCodes) {
    StringBuilder builder = new StringBuilder();
    //
    // Compute the expected number of each type of error.
    //
    HashMap<ErrorCode, Integer> expectedCounts = new HashMap<ErrorCode, Integer>();
    for (ErrorCode code : errorCodes) {
      Integer count = expectedCounts.get(code);
      if (count == null) {
        count = Integer.valueOf(1);
      } else {
        count = Integer.valueOf(count.intValue() + 1);
      }
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
      ArrayList<AnalysisError> list = errorsByCode.get(code);
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
    if (builder.length() > 0) {
      Assert.fail(builder.toString());
    }
  }

  /**
   * Assert that the number of errors that have been gathered matches the number of severities that
   * are given and that there are the same number of errors and warnings as specified by the
   * argument. The order in which the errors were gathered is ignored.
   * 
   * @param severities the severities of the errors that should have been gathered
   * @throws AssertionFailedError if a different number of errors have been gathered than were
   *           expected
   */
  public void assertErrors(ErrorSeverity[] severities) {
    int expectedErrorCount = 0;
    int expectedWarningCount = 0;
    for (ErrorSeverity severity : severities) {
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
    if (expectedErrorCount != actualErrorCount) {
      Assert.fail("Expected " + expectedErrorCount + " errors, found " + actualErrorCount);
    }
    if (expectedWarningCount != actualWarningCount) {
      Assert.fail("Expected " + expectedWarningCount + " warnings, found " + actualWarningCount);
    }
  }

  /**
   * Assert that no errors have been gathered.
   * 
   * @throws AssertionFailedError if any errors have been gathered
   */
  public void assertNoErrors() {
    Assert.assertEquals(0, errors.size());
  }

  /**
   * Return the errors that were collected.
   * 
   * @return the errors that were collected
   */
  public List<AnalysisError> getErrors() {
    return errors;
  }

  @Override
  public void onError(AnalysisError error) {
    errors.add(error);
  }
}
