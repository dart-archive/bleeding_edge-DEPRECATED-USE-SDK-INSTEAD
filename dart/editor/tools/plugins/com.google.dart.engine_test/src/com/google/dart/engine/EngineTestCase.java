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
package com.google.dart.engine;

import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.StringToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.utilities.io.PrintStringWriter;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import java.util.List;
import java.util.Map;

/**
 * The class {@code EngineTestCase} defines utility methods for making assertions.
 */
public class EngineTestCase extends TestCase {

  private static final int PRINT_RANGE = 6;

  /**
   * Assert that the tokens in the actual stream of tokens have the same types and lexemes as the
   * tokens in the expected stream of tokens. Note that this does not assert anything about the
   * offsets of the tokens (although the lengths will be equal).
   * 
   * @param expectedStream the head of the stream of tokens that were expected
   * @param actualStream the head of the stream of tokens that were actually found
   * @throws AssertionFailedError if the two streams of tokens are not the same
   */
  public static void assertAllMatch(Token expectedStream, Token actualStream) {
    Token left = expectedStream;
    Token right = actualStream;
    while (left.getType() != TokenType.EOF && right.getType() != TokenType.EOF) {
      assertMatches(left, right);
      left = left.getNext();
      right = right.getNext();
    }
  }

  /**
   * Assert that the array of actual values contain exactly the same values as those in the array of
   * expected value, with the exception that the order of the elements is not required to be the
   * same.
   * 
   * @param expectedValues the values that are expected to be found
   * @param actualValues the actual values that are being compared against the expected values
   */
  public static void assertEqualsIgnoreOrder(Object[] expectedValues, Object[] actualValues) {
    assertNotNull(actualValues);
    int expectedLength = expectedValues.length;
    assertEquals(expectedLength, actualValues.length);
    boolean[] found = new boolean[expectedLength];
    for (int i = 0; i < expectedLength; i++) {
      found[i] = false;
    }
    for (Object actualValue : actualValues) {
      boolean wasExpected = false;
      for (int i = 0; i < expectedLength; i++) {
        if (!found[i] && expectedValues[i].equals(actualValue)) {
          found[i] = true;
          wasExpected = true;
          break;
        }
      }
      if (!wasExpected) {
        fail("The actual value " + actualValue + " was not expected");
      }
    }
  }

  /**
   * Assert that a given String is equal to an expected value.
   * 
   * @param expected the expected String value
   * @param actualValues the actual String value
   */
  public static void assertEqualString(String expected, String actual) {

    if (actual == null || expected == null) {
      if (actual == expected) {
        return;
      }
      if (actual == null) {
        Assert.assertTrue("Content not as expected: is 'null' expected: " + expected, false);
      } else {
        Assert.assertTrue("Content not as expected: expected 'null' is: " + actual, false);
      }
    }

    int diffPos = getDiffPos(actual, expected);

    if (diffPos != -1) {

      int diffAhead = Math.max(0, diffPos - PRINT_RANGE);
      int diffAfter = Math.min(actual.length(), diffPos + PRINT_RANGE);

      String diffStr = actual.substring(diffAhead, diffPos) + '^'
          + actual.substring(diffPos, diffAfter);

      // use detailed message
      String message = "Content not as expected: is\n" + actual + "\nDiffers at pos " + diffPos + ": " + diffStr + "\nexpected:\n" + expected; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

      assertEquals(message, expected, actual);
    }

  }

  /**
   * Assert that the given object is an instance of the expected class.
   * 
   * @param expectedClass the class that the object is expected to be an instance of
   * @param object the object being tested
   * @return the object that was being tested
   * @throws Exception if the object is not an instance of the expected class
   */
  @SuppressWarnings("unchecked")
  public static <E> E assertInstanceOf(Class<E> expectedClass, Object object) {
    if (!expectedClass.isInstance(object)) {
      fail("Expected instance of " + expectedClass.getName() + ", found "
          + (object == null ? "null" : object.getClass().getName()));
    }
    return (E) object;
  }

  /**
   * Assert that the given array is non-{@code null} and has the expected number of elements.
   * 
   * @param expectedLength the expected number of elements
   * @param array the array being tested
   * @throws AssertionFailedError if the array is {@code null} or does not have the expected number
   *           of elements
   */
  public static void assertLength(int expectedLength, Object[] array) {
    if (array == null) {
      fail("Expected array of length " + expectedLength + "; found null");
    } else if (array.length != expectedLength) {
      fail("Expected array of length " + expectedLength + "; contained " + array.length
          + " elements");
    }
  }

  /**
   * Assert that the actual token has the same type and lexeme as the expected token. Note that this
   * does not assert anything about the offsets of the tokens (although the lengths will be equal).
   * 
   * @param expectedToken the token that was expected
   * @param actualToken the token that was found
   * @throws AssertionFailedError if the two tokens are not the same
   */
  public static void assertMatches(Token expectedToken, Token actualToken) {
    assertEquals(expectedToken.getType(), actualToken.getType());
    if (expectedToken instanceof KeywordToken) {
      assertInstanceOf(KeywordToken.class, actualToken);
      assertEquals(
          ((KeywordToken) expectedToken).getKeyword(),
          ((KeywordToken) actualToken).getKeyword());
    } else if (expectedToken instanceof StringToken) {
      assertInstanceOf(StringToken.class, actualToken);
      assertEquals(
          ((StringToken) expectedToken).getLexeme(),
          ((StringToken) actualToken).getLexeme());
    }
  }

  /**
   * Assert that the given list is non-{@code null} and has the expected number of elements.
   * 
   * @param expectedSize the expected number of elements
   * @param list the list being tested
   * @throws AssertionFailedError if the list is {@code null} or does not have the expected number
   *           of elements
   */
  public static void assertSize(int expectedSize, List<?> list) {
    if (list == null) {
      fail("Expected list of size " + expectedSize + "; found null");
    } else if (list.size() != expectedSize) {
      fail("Expected list of size " + expectedSize + "; contained " + list.size() + " elements");
    }
  }

  /**
   * Assert that the given map is non-{@code null} and has the expected number of elements.
   * 
   * @param expectedSize the expected number of elements
   * @param map the map being tested
   * @throws AssertionFailedError if the map is {@code null} or does not have the expected number of
   *           elements
   */
  public static void assertSize(int expectedSize, Map<?, ?> map) {
    if (map == null) {
      fail("Expected map of size " + expectedSize + "; found null");
    } else if (map.size() != expectedSize) {
      fail("Expected map of size " + expectedSize + "; contained " + map.size() + " elements");
    }
  }

  /**
   * Convert the given array of lines into a single source string.
   * 
   * @param lines the lines to be merged into a single source string
   * @return the source string composed of the given lines
   */
  public static String createSource(String... lines) {
    PrintStringWriter writer = new PrintStringWriter();
    for (String line : lines) {
      writer.println(line);
    }
    return writer.toString();
  }

  /**
   * Calculate the offset where the given strings differ.
   * 
   * @param str1 the first String to compare
   * @param str2 the second String to compare
   * @return the offset at which the strings differ (or <code>-1</code> if they do not)
   */
  private static int getDiffPos(String str1, String str2) {

    int len1 = Math.min(str1.length(), str2.length());

    int diffPos = -1;
    for (int i = 0; i < len1; i++) {
      if (str1.charAt(i) != str2.charAt(i)) {
        diffPos = i;
        break;
      }
    }

    if (diffPos == -1 && str1.length() != str2.length()) {
      diffPos = len1;
    }

    return diffPos;
  }
}
