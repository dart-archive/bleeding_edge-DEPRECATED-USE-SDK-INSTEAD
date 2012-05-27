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
package com.google.dart.tools.core.utilities.ast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class LocationSpec {

  /**
   * Parse a set of tests from the given <code>originalSource</code>. The source string has test
   * locations embedded in it, which are identified by '!X' where X is a single character. Each X is
   * matched to positive or negative results in the array of <code>validationStrings</code>.
   * Validation strings contain the name of a prediction with a two character prefix. The first
   * character of the prefix corresponds to an X in the <code>originalSource</code>. The second
   * character is either a '+' or a '-' indicating whether the string is a positive or negative
   * result. If logical not is needed in the source it can be represented by '!!'.
   * 
   * @param originalSource The source for a test that contains test locations
   * @param validationStrings The positive and negative predictions
   * @return a collection of LocationSpec instances
   */
  static Collection<LocationSpec> from(String originalSource, String[] validationStrings) {
    Map<Character, LocationSpec> tests = new HashMap<Character, LocationSpec>();
    String modifiedSource = originalSource;
    while (true) {
      int index = modifiedSource.indexOf('!');
      if (index < 0) {
        break;
      }
      int n = 1; // only delete one char for double-bangs
      char id = modifiedSource.charAt(index + 1);
      if (id != '!') {
        n = 2;
        LocationSpec test = new LocationSpec(id);
        tests.put(id, test);
        test.testLocation = index;
      }
      modifiedSource = modifiedSource.substring(0, index) + modifiedSource.substring(index + n);
    }
    if (modifiedSource.equals(originalSource)) {
      throw new IllegalStateException("No tests in source: " + originalSource);
    }
    for (String result : validationStrings) {
      if (result.length() < 3) {
        throw new IllegalStateException("Invalid location result: " + result);
      }
      char id = result.charAt(0);
      char sign = result.charAt(1);
      String value = result.substring(2);
      LocationSpec test = tests.get(id);
      if (test == null) {
        throw new IllegalStateException("Invalid location result id: " + id + " for: " + result);
      }
      test.source = modifiedSource;
      if (sign == '+') {
        test.positiveResults.add(value);
      } else if (sign == '-') {
        test.negativeResults.add(value);
      } else {
        String err = "Invalid location result sign: " + sign + " for: " + result;
        throw new IllegalStateException(err);
      }
    }
    List<Character> badPoints = new ArrayList<Character>();
    List<Character> badResults = new ArrayList<Character>();
    for (LocationSpec test : tests.values()) {
      if (test.testLocation == -1) {
        badPoints.add(test.id);
      }
      if (test.positiveResults.isEmpty() && test.negativeResults.isEmpty()) {
        badResults.add(test.id);
      }
    }
    if (!(badPoints.isEmpty() && badResults.isEmpty())) {
      StringBuffer err = new StringBuffer();
      if (!badPoints.isEmpty()) {
        err.append("No test location for tests:");
        for (Character ch : badPoints) {
          err.append(' ').append(ch);
        }
        err.append(' ');
      }
      if (!badResults.isEmpty()) {
        err.append("No results for tests:");
        for (Character ch : badResults) {
          err.append(' ').append(ch);
        }
      }
      throw new IllegalStateException(err.toString());
    }
    return tests.values();
  }

  char id;
  int testLocation;
  List<String> positiveResults;
  List<String> negativeResults;
  String source;

  LocationSpec(char id) {
    this.id = id;
    this.testLocation = -1;
    this.positiveResults = new ArrayList<String>();
    this.negativeResults = new ArrayList<String>();
  }
}
