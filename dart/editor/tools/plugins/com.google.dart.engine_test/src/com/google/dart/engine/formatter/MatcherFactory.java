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
package com.google.dart.engine.formatter;

import com.google.dart.engine.formatter.edit.Edit;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

/**
 * A factory for matchers.
 */
public class MatcherFactory {

  private static class EditMatcher extends BaseMatcher<Edit> {

    private final Edit expected;

    public EditMatcher(Edit expected) {
      this.expected = expected;
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(expected.toString());
    }

    @Override
    public boolean matches(Object item) {
      if (item instanceof Edit) {
        Edit actual = (Edit) item;
        if (expected.length != actual.length) {
          return false;
        }
        if (expected.offset != actual.offset) {
          return false;
        }
        return expected.replacement.equals(actual);
      }
      return false;
    }

  }

  /**
   * Create a matcher to match the given expected edit.
   * 
   * @param expected the expected edit
   * @return the resulting matcher
   */
  public static Matcher<Edit> matches(Edit expected) {
    return new EditMatcher(expected);
  }

}
