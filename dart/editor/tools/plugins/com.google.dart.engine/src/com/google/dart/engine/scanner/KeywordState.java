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

import java.util.Arrays;

/**
 * Instances of the abstract class <code>KeywordState</code> represent a state in a state machine
 * used to scan keywords.
 */
public final class KeywordState {
  /**
   * An empty transition table used by leaf states.
   */
  private static final KeywordState[] EMPTY_TABLE = new KeywordState[26];

  /**
   * The initial state in the state machine.
   */
  public static final KeywordState KEYWORD_STATE = createKeywordStateTable();

  /**
   * Create the next state in the state machine where we have already recognized the subset of
   * strings in the given array of strings starting at the given offset and having the given length.
   * All of these strings have a common prefix and the next character is at the given start index.
   * 
   * @param start the index of the character in the strings used to transition to a new state
   * @param strings an array containing all of the strings that will be recognized by the state
   *          machine
   * @param offset the offset of the first string in the array that has the prefix that is assumed
   *          to have been recognized by the time we reach the state being built
   * @param length the number of strings in the array that pass through the state being built
   * @return the state that was created
   */
  private static KeywordState computeKeywordStateTable(int start, String[] strings, int offset,
      int length) {
    KeywordState[] result = new KeywordState[26];
    assert length != 0;
    char chunk = '\0';
    int chunkStart = -1;
    boolean isLeaf = false;
    for (int i = offset; i < offset + length; i++) {
      if (strings[i].length() == start) {
        isLeaf = true;
      }
      if (strings[i].length() > start) {
        char c = strings[i].charAt(start);
        if (chunk != c) {
          if (chunkStart != -1) {
            result[chunk - 'a'] = computeKeywordStateTable(start + 1, strings, chunkStart, i
                - chunkStart);
          }
          chunkStart = i;
          chunk = c;
        }
      }
    }
    if (chunkStart != -1) {
      assert result[chunk - 'a'] == null;
      result[chunk - 'a'] = computeKeywordStateTable(start + 1, strings, chunkStart, offset
          + length - chunkStart);
    } else {
      assert length == 1;
      return new KeywordState(EMPTY_TABLE, strings[offset]);
    }
    if (isLeaf) {
      return new KeywordState(result, strings[offset]);
    } else {
      return new KeywordState(result, null);
    }
  }

  /**
   * Create the initial state in the state machine.
   * 
   * @return the state that was created
   */
  private static KeywordState createKeywordStateTable() {
    Keyword[] values = Keyword.values();
    String[] strings = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      strings[i] = values[i].getSyntax();
    }
    Arrays.sort(strings);
    return computeKeywordStateTable(0, strings, 0, strings.length);
  }

  /**
   * A table mapping characters to the states to which those characters will transition. (The index
   * into the array is the offset from the character <code>'a'</code> to the transitioning
   * character.)
   */
  private final KeywordState[] table;

  /**
   * The keyword that is recognized by this state, or <code>null</code> if this state is not a
   * terminal state.
   */
  private final Keyword keyword;

  /**
   * Initialize a newly created state to have the given transitions and to recognize the keyword
   * with the given syntax.
   * 
   * @param table a table mapping characters to the states to which those characters will transition
   * @param syntax the syntax of the keyword that is recognized by the state
   */
  private KeywordState(KeywordState[] table, String syntax) {
    this.table = table;
    this.keyword = (syntax == null) ? null : Keyword.keywords.get(syntax);
  }

  /**
   * Return the keyword that was recognized by this state, or <code>null</code> if this state does
   * not recognized a keyword.
   * 
   * @return the keyword that was matched by reaching this state
   */
  public Keyword keyword() {
    return keyword;
  }

  /**
   * Return the state that follows this state on a transition of the given character, or
   * <code>null</code> if there is no valid state reachable from this state with such a transition.
   * 
   * @param c the character used to transition from this state to another state
   * @return the state that follows this state on a transition of the given character
   */
  public KeywordState next(char c) {
    return table[c - 'a'];
  }
}
