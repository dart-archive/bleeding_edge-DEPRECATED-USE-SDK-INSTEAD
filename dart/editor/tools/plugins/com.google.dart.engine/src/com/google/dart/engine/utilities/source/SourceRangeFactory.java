/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.utilities.source;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.scanner.Token;

import java.util.List;

/**
 * Factory for creating instances of {@link SourceRange} using {@link ASTNode}s, {@link Token}s and
 * values.
 * 
 * @coverage dart.engine.utilities
 */
public class SourceRangeFactory {
  /**
   * @return the name {@link SourceRange} of the given {@link Element};
   */
  public static SourceRange rangeElementName(Element element) {
    return rangeStartLength(element.getNameOffset(), element.getName().length());
  }

  /**
   * @return the {@link SourceRange} which start at end of "a" and ends at end of "b".
   */
  public static SourceRange rangeEndEnd(ASTNode a, ASTNode b) {
    int start = a.getEnd();
    int end = b.getEnd();
    return rangeStartEnd(start, end);
  }

  /**
   * @return the {@link SourceRange} which start at end of "a" and ends at "end".
   */
  public static SourceRange rangeEndEnd(ASTNode a, int end) {
    int start = a.getEnd();
    return rangeStartEnd(start, end);
  }

  /**
   * @return the {@link SourceRange} which start at end of "a" and ends at end of "b".
   */
  public static SourceRange rangeEndEnd(ASTNode a, SourceRange b) {
    int start = a.getEnd();
    int end = b.getEnd();
    return rangeStartEnd(start, end);
  }

  /**
   * @return the {@link SourceRange} which start at end of "a" and ends at end of "b".
   */
  public static SourceRange rangeEndEnd(SourceRange a, ASTNode b) {
    int start = a.getEnd();
    int end = b.getEnd();
    return rangeStartEnd(start, end);
  }

  /**
   * @return the {@link SourceRange} which start at end of "a" and ends at end of "b".
   */
  public static SourceRange rangeEndEnd(SourceRange a, SourceRange b) {
    int start = a.getEnd();
    int end = b.getEnd();
    return rangeStartEnd(start, end);
  }

  /**
   * @return the {@link SourceRange} which start at the end of "startInfo" and has specified length.
   */
  public static SourceRange rangeEndLength(ASTNode a, int length) {
    int start = a.getEnd();
    return new SourceRange(start, length);
  }

  public static SourceRange rangeEndLength(SourceRange a, int length) {
    int start = a.getOffset() + a.getLength();
    return rangeStartLength(start, length);
  }

  /**
   * @return the {@link SourceRange} which start at the end of "a" and ends at the start of "b".
   */
  public static SourceRange rangeEndStart(ASTNode a, ASTNode b) {
    int start = a.getEnd();
    int end = b.getOffset();
    return rangeStartEnd(start, end);
  }

  /**
   * @return the {@link SourceRange} which start at the end of "a" and ends "b".
   */
  public static SourceRange rangeEndStart(ASTNode a, int b) {
    int start = a.getEnd();
    return rangeStartEnd(start, b);
  }

  /**
   * @return the {@link SourceRange} which start at end of "a" and ends at start of "b".
   */
  public static SourceRange rangeEndStart(ASTNode a, Token b) {
    int start = a.getEnd();
    int end = b.getOffset();
    return rangeStartEnd(start, end);
  }

  /**
   * @return the {@link SourceRange} which start at the end of "a" and ends at the start of "b".
   */
  public static SourceRange rangeEndStart(SourceRange a, SourceRange b) {
    int start = a.getEnd();
    int end = b.getOffset();
    return rangeStartEnd(start, end);
  }

  /**
   * @return the {@link SourceRange} of "a" with offset from given "base".
   */
  public static SourceRange rangeFromBase(ASTNode a, int base) {
    int start = a.getOffset() - base;
    int length = a.getLength();
    return rangeStartLength(start, length);
  }

  /**
   * @return the {@link SourceRange} of "a" with offset from given "base".
   */
  public static SourceRange rangeFromBase(ASTNode a, SourceRange base) {
    return rangeFromBase(a, base.getOffset());
  }

  /**
   * @return the {@link SourceRange} "a" with offset from given "base".
   */
  public static SourceRange rangeFromBase(SourceRange a, SourceRange base) {
    int start = a.getOffset() - base.getOffset();
    int length = a.getLength();
    return rangeStartLength(start, length);
  }

  /**
   * @return the {@link SourceRange} for given {@link ASTNode}, or {@code null} if {@code null} was
   *         given.
   */
  public static SourceRange rangeNode(ASTNode node) {
    if (node != null) {
      return new SourceRange(node.getOffset(), node.getLength());
    }
    return null;
  }

  /**
   * @return the {@link SourceRange} which starts at the start of first "first" and ends at the end
   *         of "last" element.
   */
  public static SourceRange rangeNodes(List<? extends ASTNode> nodes) {
    if (nodes.isEmpty()) {
      return rangeStartLength(0, 0);
    }
    ASTNode first = nodes.get(0);
    ASTNode last = nodes.get(nodes.size() - 1);
    return rangeStartEnd(first, last);
  }

  /**
   * @return the {@link SourceRange} which start at start of "a" and ends at end of "b".
   */
  public static SourceRange rangeStartEnd(ASTNode a, ASTNode b) {
    int start = a.getOffset();
    int end = b.getEnd();
    return rangeStartEnd(start, end);
  }

  /**
   * @return the {@link SourceRange} which start at start of "a" and ends at "end".
   */
  public static SourceRange rangeStartEnd(ASTNode a, int end) {
    int start = a.getOffset();
    return rangeStartEnd(start, end);
  }

  /**
   * @return the {@link SourceRange} which start at "start" and ends at end of "b".
   */
  public static SourceRange rangeStartEnd(int start, ASTNode b) {
    int end = b.getEnd();
    return new SourceRange(start, end - start);
  }

  public static SourceRange rangeStartEnd(int start, int end) {
    return new SourceRange(start, end - start);
  }

  /**
   * @return the {@link SourceRange} which start at start of "a" and ends at end of "b".
   */
  public static SourceRange rangeStartEnd(SourceRange a, ASTNode b) {
    int start = a.getOffset();
    int end = b.getEnd();
    return new SourceRange(start, end - start);
  }

  /**
   * @return the {@link SourceRange} which start at start of "a" and ends at "end".
   */
  public static SourceRange rangeStartEnd(SourceRange a, int end) {
    int start = a.getOffset();
    return rangeStartEnd(start, end);
  }

  /**
   * @return the {@link SourceRange} which start at start of "a" and ends at end of "b".
   */
  public static SourceRange rangeStartEnd(SourceRange a, SourceRange b) {
    int start = a.getOffset();
    int end = b.getEnd();
    return rangeStartEnd(start, end);
  }

  /**
   * @return the {@link SourceRange} which start at start of "a" and ends at end of "b".
   */
  public static SourceRange rangeStartEnd(Token a, ASTNode b) {
    int start = a.getOffset();
    int end = b.getEnd();
    return rangeStartEnd(start, end);
  }

  public static SourceRange rangeStartEnd(Token a, Token b) {
    int start = a.getOffset();
    int end = b.getEnd();
    return rangeStartEnd(start, end);
  }

  /**
   * @return the {@link SourceRange} which start at start of "a" and has given length.
   */
  public static SourceRange rangeStartLength(ASTNode a, int length) {
    int start = a.getOffset();
    return new SourceRange(start, length);
  }

  public static SourceRange rangeStartLength(int start, int length) {
    return new SourceRange(start, length);
  }

  public static SourceRange rangeStartLength(SourceRange a, int length) {
    return rangeStartLength(a.getOffset(), length);
  }

  /**
   * @return the {@link SourceRange} which start at start of "a" and ends at start of "b".
   */
  public static SourceRange rangeStartStart(ASTNode a, ASTNode b) {
    int start = a.getOffset();
    int end = b.getOffset();
    return rangeStartEnd(start, end);
  }

  /**
   * @return the {@link SourceRange} which start "start" and ends at start of "b".
   */
  public static SourceRange rangeStartStart(int start, ASTNode b) {
    int end = b.getOffset();
    return rangeStartEnd(start, end);
  }

  /**
   * @return the {@link SourceRange} which start at start of "a" and ends at start of "b".
   */
  public static SourceRange rangeStartStart(SourceRange a, ASTNode b) {
    int start = a.getOffset();
    int end = b.getOffset();
    return rangeStartEnd(start, end);
  }

  /**
   * @return the {@link SourceRange} which start at start of "a" and ends at start of "b".
   */
  public static SourceRange rangeStartStart(SourceRange a, SourceRange b) {
    int start = a.getOffset();
    int end = b.getOffset();
    return rangeStartEnd(start, end);
  }

  public static SourceRange rangeToken(Token token) {
    return rangeStartLength(token.getOffset(), token.getLength());
  }

  /**
   * Given {@link SourceRange} created relative to "base", return absolute {@link SourceRange}.
   */
  public static SourceRange rangeWithBase(ASTNode base, SourceRange r) {
    return rangeWithBase(base.getOffset(), r);
  }

  /**
   * Given {@link SourceRange} created relative to "base", return absolute {@link SourceRange}.
   */
  public static SourceRange rangeWithBase(int base, SourceRange r) {
    int start = base + r.getOffset();
    int length = r.getLength();
    return rangeStartLength(start, length);
  }

  /**
   * Given {@link SourceRange} created relative to "base", return absolute {@link SourceRange}.
   */
  public static SourceRange rangeWithBase(SourceRange base, SourceRange r) {
    return rangeWithBase(base.getOffset(), r);
  }

}
