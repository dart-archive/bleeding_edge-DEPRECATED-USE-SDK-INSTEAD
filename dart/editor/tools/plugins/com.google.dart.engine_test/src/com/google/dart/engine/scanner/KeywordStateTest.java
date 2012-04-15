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
package com.google.dart.engine.scanner;

import junit.framework.TestCase;

public class KeywordStateTest extends TestCase {
  public void test_KeywordState() throws Exception {
    //
    // Generate the test data to be scanned.
    //
    Keyword[] keywords = Keyword.values();
    int keywordCount = keywords.length;
    String[] textToTest = new String[keywordCount * 3];
    for (int i = 0; i < keywordCount; i++) {
      String syntax = keywords[i].getSyntax();
      textToTest[i] = syntax;
      textToTest[i + keywordCount] = syntax + "x";
      textToTest[i + keywordCount * 2] = syntax.substring(0, syntax.length() - 1);
    }
    //
    // Scan each of the identifiers.
    //
    KeywordState firstState = KeywordState.KEYWORD_STATE;
    for (int i = 0; i < textToTest.length; i++) {
      String text = textToTest[i];
      int index = 0;
      int length = text.length();
      KeywordState state = firstState;
      while (index < length && state != null) {
        state = state.next(text.charAt(index));
        index++;
      }
      if (i < keywordCount) {
        // keyword
        assertNotNull(state);
        assertNotNull(state.keyword());
        assertEquals(keywords[i], state.keyword());
      } else if (i < keywordCount * 2) {
        // keyword + "x"
        assertNull(state);
      } else {
        // keyword.substring(0, keyword.length() - 1)
        assertNotNull(state);
      }
    }
  }
}
