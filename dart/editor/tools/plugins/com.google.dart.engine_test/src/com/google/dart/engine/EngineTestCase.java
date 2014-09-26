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

import com.google.common.base.Objects;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.StringToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.utilities.io.PrintStringWriter;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
   * Assert that the given collection is non-{@code null} and has the expected number of elements.
   * 
   * @param expectedSize the expected number of elements
   * @param c the collection being tested
   * @throws AssertionFailedError if the list is {@code null} or does not have the expected number
   *           of elements
   */
  public static void assertCollectionSize(int expectedSize, Collection<?> c) {
    if (c == null) {
      fail("Expected collection of size " + expectedSize + "; found null");
    } else if (c.size() != expectedSize) {
      fail("Expected collection of size " + expectedSize + "; contained " + c.size() + " elements");
    }
  }

  /**
   * Assert that the given array is non-{@code null} and contains the expected elements. The
   * elements can appear in any order.
   * 
   * @param array the array being tested
   * @param expectedElements the expected elements
   * @throws AssertionFailedError if the array is {@code null} or does not contain the expected
   *           elements
   */
  public static void assertContains(Object[] array, Object... expectedElements) {
    int expectedSize = expectedElements.length;
    if (array == null) {
      fail("Expected array of length " + expectedSize + "; found null");
    }
    if (array.length != expectedSize) {
      fail("Expected array of length " + expectedSize + "; contained " + array.length + " elements");
    }
    boolean[] found = new boolean[expectedSize];
    for (int i = 0; i < expectedSize; i++) {
      privateAssertContains(array, found, expectedElements[i]);
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
   * @param actual the actual String value
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

    int diffPos = getDiffPos(expected, actual);

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
   * Assert that the given array is non-{@code null} and has exactly expected elements.
   * 
   * @param array the array being tested
   * @param expectedElements the expected elements
   * @throws AssertionFailedError if the array is {@code null} or does not have the expected
   *           elements
   */
  public static void assertExactElementsInArray(Object array[], Object... expectedElements) {
    int expectedSize = expectedElements.length;
    if (array == null) {
      fail("Expected array of size " + expectedSize + "; found null");
    }
    if (array.length != expectedSize) {
      fail("Expected array of size " + expectedSize + "; contained " + array.length + " elements");
    }
    for (int i = 0; i < expectedSize; i++) {
      Object element = array[i];
      Object expectedElement = expectedElements[i];
      if (!Objects.equal(element, expectedElement)) {
        fail("Expected " + expectedElement + " at [" + i + "]; found " + element);
      }
    }
  }

  /**
   * Assert that the given list is non-{@code null} and has exactly expected elements.
   * 
   * @param list the list being tested
   * @param expectedElements the expected elements
   * @throws AssertionFailedError if the list is {@code null} or does not have the expected elements
   */
  public static void assertExactElementsInList(List<?> list, Object... expectedElements) {
    int expectedSize = expectedElements.length;
    if (list == null) {
      fail("Expected list of size " + expectedSize + "; found null");
    }
    if (list.size() != expectedSize) {
      fail("Expected list of size " + expectedSize + "; contained " + list.size() + " elements");
    }
    for (int i = 0; i < expectedSize; i++) {
      Object element = list.get(i);
      Object expectedElement = expectedElements[i];
      if (!Objects.equal(element, expectedElement)) {
        fail("Expected " + expectedElement + " at [" + i + "]; found " + element);
      }
    }
  }

  /**
   * Assert that the given list is non-{@code null} and has exactly expected elements.
   * 
   * @param set the list being tested
   * @param expectedElements the expected elements
   * @throws AssertionFailedError if the list is {@code null} or does not have the expected elements
   */
  public static void assertExactElementsInSet(Set<?> set, Object... expectedElements) {
    int expectedSize = expectedElements.length;
    if (set == null) {
      fail("Expected list of size " + expectedSize + "; found null");
    }
    if (set.size() != expectedSize) {
      fail("Expected list of size " + expectedSize + "; contained " + set.size() + " elements");
    }
    for (int i = 0; i < expectedSize; i++) {
      Object expectedElement = expectedElements[i];
      if (!set.contains(expectedElement)) {
        fail("Expected " + expectedElement + " in set" + set);
      }
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
  public static void assertSizeOfList(int expectedSize, List<?> list) {
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
  public static void assertSizeOfMap(int expectedSize, Map<?, ?> map) {
    if (map == null) {
      fail("Expected map of size " + expectedSize + "; found null");
    } else if (map.size() != expectedSize) {
      fail("Expected map of size " + expectedSize + "; contained " + map.size() + " elements");
    }
  }

  /**
   * Assert that the given set is non-{@code null} and has the expected number of elements.
   * 
   * @param expectedSize the expected number of elements
   * @param set the set being tested
   * @throws AssertionFailedError if the set is {@code null} or does not have the expected number of
   *           elements
   */
  public static void assertSizeOfSet(int expectedSize, Set<?> set) {
    if (set == null) {
      fail("Expected set of size " + expectedSize + "; found null");
    } else if (set.size() != expectedSize) {
      fail("Expected set of size " + expectedSize + "; contained " + set.size() + " elements");
    }
  }

  /**
   * Convert the given array of lines into a single source string.
   * 
   * @param lines the lines to be merged into a single source string
   * @return the source string composed of the given lines
   */
  public static String createSource(String... lines) {
    @SuppressWarnings("resource")
    PrintStringWriter writer = new PrintStringWriter();
    for (String line : lines) {
      writer.println(line);
    }
    return writer.toString();
  }

  /**
   * @return the {@link AstNode} with requested type at offset of the "prefix".
   */
  public static <T extends AstNode> T findNode(AstNode root, String code, String prefix,
      Class<T> clazz) {
    int offset = code.indexOf(prefix);
    if (offset == -1) {
      throw new IllegalArgumentException("Not found '" + prefix + "'.");
    }
    AstNode node = new NodeLocator(offset).searchWithin(root);
    return node.getAncestor(clazz);
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

  private static void privateAssertContains(Object[] array, boolean[] found, Object element) {
    if (element == null) {
      for (int i = 0; i < array.length; i++) {
        if (!found[i]) {
          if (array[i] == null) {
            found[i] = true;
            return;
          }
        }
      }
      fail("Does not contain null");
    } else {
      for (int i = 0; i < array.length; i++) {
        if (!found[i]) {
          if (element.equals(array[i])) {
            found[i] = true;
            return;
          }
        }
      }
      fail("Does not contain " + element);
    }
  }

  /**
   * Assert that the given collection has the same number of elements as the number of specified
   * names, and that for each specified name, a corresponding element can be found in the given
   * collection with that name.
   * 
   * @param elements the elements
   * @param names the names
   */
  protected void assertNamedElements(Element[] elements, String... names) {
    for (String elemName : names) {
      boolean found = false;
      for (Element elem : elements) {
        if (elem.getName().equals(elemName)) {
          found = true;
          break;
        }
      }
      if (!found) {
        StringBuilder msg = new StringBuilder();
        msg.append("Expected element named: ");
        msg.append(elemName);
        msg.append("\n  but found: ");
        for (Element elem : elements) {
          msg.append(elem.getName());
          msg.append(", ");
        }
        fail(msg.toString());
      }
    }
    assertLength(names.length, elements);
  }

  protected AnalysisContextImpl createAnalysisContext() {
    AnalysisContextImpl context = new AnalysisContextImpl();
    context.setSourceFactory(new SourceFactory());
    return context;
  }

  /**
   * Return the getter in the given type with the given name. Inherited getters are ignored.
   * 
   * @param type the type in which the getter is declared
   * @param getterName the name of the getter to be returned
   * @return the property accessor element representing the getter with the given name
   */
  protected PropertyAccessorElement getGetter(InterfaceType type, String getterName) {
    for (PropertyAccessorElement accessor : type.getElement().getAccessors()) {
      if (accessor.isGetter() && accessor.getName().equals(getterName)) {
        return accessor;
      }
    }
    fail("Could not find getter named " + getterName + " in " + type.getDisplayName());
    return null;
  }

  /**
   * Return the method in the given type with the given name. Inherited methods are ignored.
   * 
   * @param type the type in which the method is declared
   * @param methodName the name of the method to be returned
   * @return the method element representing the method with the given name
   */
  protected MethodElement getMethod(InterfaceType type, String methodName) {
    for (MethodElement method : type.getElement().getMethods()) {
      if (method.getName().equals(methodName)) {
        return method;
      }
    }
    fail("Could not find method named " + methodName + " in " + type.getDisplayName());
    return null;
  }
}
