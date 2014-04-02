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
package com.google.dart.tools.core.utilities.general;

import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.utilities.source.SourceRange;

/**
 * @coverage dart.tools.core.utilities
 */
public class SourceRangeFactory {
  /**
   * @return the {@link SourceRange} which start at end of "a" and ends at end of "b".
   */
  public static SourceRange forEndEnd(SourceRange a, SourceRange b) {
    int start = a.getEnd();
    int end = b.getEnd();
    return forStartEnd(start, end);
  }

  public static SourceRange forEndLength(SourceRange a, int length) {
    int start = a.getOffset() + a.getLength();
    return forStartLength(start, length);
  }

  /**
   * @return the {@link SourceRange} which start at the end of "a" and ends at the start of "b".
   */
  public static SourceRange forEndStart(SourceRange a, SourceRange b) {
    int start = a.getEnd();
    int end = b.getOffset();
    return forStartEnd(start, end);
  }

  public static SourceRange forStartEnd(int start, int end) {
    return new SourceRange(start, end - start);
  }

  /**
   * @return the {@link SourceRange} which start at start of "a" and ends at "end".
   */
  public static SourceRange forStartEnd(SourceRange a, int end) {
    int start = a.getOffset();
    return forStartEnd(start, end);
  }

  /**
   * @return the {@link SourceRange} which start at start of "a" and ends at end of "b".
   */
  public static SourceRange forStartEnd(SourceRange a, SourceRange b) {
    int start = a.getOffset();
    int end = b.getEnd();
    return forStartEnd(start, end);
  }

  public static SourceRange forStartEnd(Token a, Token b) {
    int start = a.getOffset();
    int end = b.getEnd();
    return forStartEnd(start, end);
  }

  public static SourceRange forStartLength(int start, int length) {
    return new SourceRange(start, length);
  }

  public static SourceRange forStartLength(SourceRange a, int length) {
    return forStartLength(a.getOffset(), length);
  }

  public static SourceRange forToken(Token token) {
    return forStartLength(token.getOffset(), token.getLength());
  }

  /**
   * @return the {@link SourceRange} "a" with offset from given "base".
   */
  public static SourceRange fromBase(SourceRange a, SourceRange base) {
    int start = a.getOffset() - base.getOffset();
    int length = a.getLength();
    return forStartLength(start, length);
  }

  /**
   * Given {@link SourceRange} created relative to "base", return absolute {@link SourceRange}.
   */
  public static SourceRange withBase(int base, SourceRange r) {
    int start = base + r.getOffset();
    int length = r.getLength();
    return forStartLength(start, length);
  }

  /**
   * Given {@link SourceRange} created relative to "base", return absolute {@link SourceRange}.
   */
  public static SourceRange withBase(SourceRange base, SourceRange r) {
    return withBase(base.getOffset(), r);
  }

}
