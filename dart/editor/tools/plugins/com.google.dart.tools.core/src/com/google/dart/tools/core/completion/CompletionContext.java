/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.completion;

import java.security.Signature;

/**
 * Instances of the class <code>CompletionContext</code> implement the context in which the
 * completion occurs.
 * 
 * @coverage dart.tools.core.completion
 */
public class CompletionContext {
  /**
   * The completed token is the first token of a member declaration.<br>
   * e.g.
   * 
   * <pre>
   * public class X {
   *   Foo| // completion occurs at |
   * }
   * </pre>
   * 
   * @see #getTokenLocation()
   */
  public static final int TL_MEMBER_START = 1;

  /**
   * The completed token is the first token of a statement.<br>
   * e.g.
   * 
   * <pre>
   * public class X {
   *   public void bar() {
   *     Foo| // completion occurs at |
   *   }
   * }
   * </pre>
   * 
   * @see #getTokenLocation()
   */
  public static final int TL_STATEMENT_START = 2;

  /**
   * The completion token is unknown.
   */
  public static final int TOKEN_KIND_UNKNOWN = 0;

  /**
   * The completion token is a name.
   */
  public static final int TOKEN_KIND_NAME = 1;

  /**
   * The completion token is a string literal. The string literal ends quote can be not present the
   * source. <code>"foo"</code> or <code>"foo</code>.
   */
  public static final int TOKEN_KIND_STRING_LITERAL = 2;

  /**
   * Return keys of expected types of a potential completion proposal at the completion position.
   * It's not mandatory to a completion proposal to respect this expectation.
   * 
   * @return keys of expected types of a potential completion proposal at the completion position or
   *         <code>null</code> if there is no expected types.
   * @see org.eclipse.jdt.core.dom.ASTParser#createASTs(ICompilationUnit[], String[],
   *      org.eclipse.jdt.core.dom.ASTRequestor, org.eclipse.core.runtime.IProgressMonitor)
   */
  public char[][] getExpectedTypesKeys() {
    return null; // default overridden by concrete implementation
  }

  /**
   * Return signatures of expected types of a potential completion proposal at the completion
   * position. It's not mandatory to a completion proposal to respect this expectation.
   * 
   * @return signatures expected types of a potential completion proposal at the completion position
   *         or <code>null</code> if there is no expected types.
   * @see Signature
   */
  public char[][] getExpectedTypesSignatures() {
    return null; // default overridden by concrete implementation
  }

  /**
   * Return the offset position in the source file buffer after which code assist is requested.
   * 
   * @return offset position in the source file buffer
   */
  public int getOffset() {
    return -1; // default overridden by concrete implementation
  }

  /**
   * Return the completed token. This token is either the identifier or Dart language keyword or the
   * string literal under, immediately preceding, the original request offset. If the original
   * request offset is not within or immediately after an identifier or keyword or a string literal
   * then the returned value is <code>null</code>.
   * 
   * @return completed token or <code>null</code>
   */
  public char[] getToken() {
    return null; // default overridden by concrete implementation
  }

  /**
   * Return the character index of the end (exclusive) of the subrange in the source file buffer
   * containing the relevant token. When there is no relevant token, the range is empty (
   * <code>getTokenEnd() == getTokenStart() - 1</code>).
   * 
   * @return character index of token end position (exclusive)
   */
  // TODO (david) https://bugs.eclipse.org/bugs/show_bug.cgi?id=132558
  public int getTokenEnd() {
    return -1; // default overridden by concrete implementation
  }

  /**
   * Return the kind of completion token being proposed.
   * <p>
   * The set of different kinds of completion token is expected to change over time. It is strongly
   * recommended that clients do <b>not</b> assume that the kind is one of the ones they know about,
   * and code defensively for the possibility of unexpected future growth.
   * </p>
   * 
   * @return the kind; one of the kind constants declared on this class whose name starts with
   *         <code>TOKEN_KIND</code>, or possibly a kind unknown to the caller
   * @since 3.2
   */
  public int getTokenKind() {
    return -1; // default overridden by concrete implementation
  }

  /**
   * Return the location of completion token being proposed. The returned location is a bit mask
   * which can contain some values of the constants declared on this class whose name starts with
   * <code>TL</code>, or possibly values unknown to the caller.
   * <p>
   * The set of different location values is expected to change over time. It is strongly
   * recommended that clients do <b>not</b> assume that the location contains only known value, and
   * code defensively for the possibility of unexpected future growth.
   * 
   * @return the location
   */
  public int getTokenLocation() {
    return -1; // default overridden by concrete implementation
  }

  /**
   * Return the character index of the start of the subrange in the source file buffer containing
   * the relevant token being completed. This token is either the identifier or Dart language
   * keyword under, or immediately preceding, the original request offset. If the original request
   * offset is not within or immediately after an identifier or keyword, then the position returned
   * is original request offset and the token range is empty.
   * 
   * @return character index of token start position (inclusive)
   */
  public int getTokenStart() {
    return -1; // default overridden by concrete implementation
  }

  /**
   * Return <code>true</code> if this completion context is an extended context. Some methods of
   * this context can be used only if this context is an extended context but an extended context
   * consumes more memory.
   * 
   * @return <code>true</code> if this completion context is an extended context
   */
  public boolean isExtended() {
    return false; // default overridden by concrete implementation
  }

  /**
   * Return <code>true</code> if completion takes place in a javadoc comment or not.
   * 
   * @return true if completion takes place in a javadoc comment
   */
  @Deprecated
  public boolean isInJavadoc() {
    return false; // default overridden by concrete implementation
  }

  /**
   * Return <code>true</code> if completion takes place in a formal reference of a javadoc tag or
   * not. Tags with formal reference are:
   * <ul>
   * <li>&#64;see</li>
   * <li>&#64;throws</li>
   * <li>&#64;exception</li>
   * <li>{&#64;link Object}</li>
   * <li>{&#64;linkplain Object}</li>
   * <li>{&#64;value} when compiler compliance is set at leats to 1.5</li>
   * </ul>
   * 
   * @return true if completion takes place in formal reference of a javadoc tag
   */
  @Deprecated
  public boolean isInJavadocFormalReference() {
    return false; // default overridden by concrete implementation
  }

  /**
   * Return <code>true</code> if completion takes place in text area of a javadoc comment or not.
   * 
   * @return true if completion takes place in a text area of a javadoc comment
   */
  @Deprecated
  public boolean isInJavadocText() {
    return false; // default overridden by concrete implementation
  }
}
