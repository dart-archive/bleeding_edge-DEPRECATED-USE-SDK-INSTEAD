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
package com.google.dart.tools.internal.corext;

import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.common.HasSourceInfo;
import com.google.dart.engine.scanner.Token;
import com.google.dart.tools.core.internal.model.SourceRangeImpl;
import com.google.dart.tools.core.model.SourceRange;

import java.util.List;

public class SourceRangeFactory {

  public static SourceRange create(DartCompilationError error) {
    return new SourceRangeImpl(error.getStartPosition(), error.getLength());
  }

  public static SourceRange create(HasSourceInfo hasSourceInfo) {
    return new SourceRangeImpl(hasSourceInfo);
  }

  /**
   * @return the {@link SourceRange} which starts at the start of first "first" and ends at the end
   *         of "last" element.
   */
  public static SourceRange create(List<? extends HasSourceInfo> elements) {
    if (elements.isEmpty()) {
      return forStartLength(0, 0);
    }
    HasSourceInfo first = elements.get(0);
    HasSourceInfo last = elements.get(elements.size() - 1);
    return forStartEnd(first, last);
  }

  /**
   * @return the {@link SourceRange} which start at end of "a" and ends at end of "b".
   */
  public static SourceRange forEndEnd(HasSourceInfo a, HasSourceInfo b) {
    int start = a.getSourceInfo().getEnd();
    int end = b.getSourceInfo().getEnd();
    return forStartEnd(start, end);
  }

  /**
   * @return the {@link SourceRange} which start at the end of "startInfo" and has specified length.
   */
  public static SourceRange forEndLength(HasSourceInfo startInfo, int length) {
    int start = startInfo.getSourceInfo().getEnd();
    return new SourceRangeImpl(start, length);
  }

  /**
   * @return the {@link SourceRange} which start at the end of "a" and ends at the start of "b".
   */
  public static SourceRange forEndStart(HasSourceInfo a, HasSourceInfo b) {
    int start = a.getSourceInfo().getEnd();
    int end = b.getSourceInfo().getOffset();
    return forStartEnd(start, end);
  }

  /**
   * @return the {@link SourceRange} which start at the end of "a" and ends "b".
   */
  public static SourceRange forEndStart(HasSourceInfo a, int end) {
    int start = a.getSourceInfo().getEnd();
    return forStartEnd(start, end);
  }

  /**
   * @return the {@link SourceRange} which start at start of "a" and ends at end of "b".
   */
  public static SourceRange forStartEnd(HasSourceInfo a, HasSourceInfo b) {
    int start = a.getSourceInfo().getOffset();
    int end = b.getSourceInfo().getEnd();
    return forStartEnd(start, end);
  }

  /**
   * @return the {@link SourceRange} which start at "start" and ends at end of "b".
   */
  public static SourceRange forStartEnd(int start, HasSourceInfo b) {
    int end = b.getSourceInfo().getEnd();
    return new SourceRangeImpl(start, end - start);
  }

  public static SourceRange forStartEnd(int start, int end) {
    return new SourceRangeImpl(start, end - start);
  }

  /**
   * @return the {@link SourceRange} which start at start of "a" and has given length.
   */
  public static SourceRange forStartLength(HasSourceInfo a, int length) {
    int start = a.getSourceInfo().getOffset();
    return new SourceRangeImpl(start, length);
  }

  public static SourceRange forStartLength(int start, int length) {
    return new SourceRangeImpl(start, length);
  }

  public static SourceRange forStartLength(SourceRange a, int length) {
    return forStartLength(a.getOffset(), length);
  }

  /**
   * @return the {@link SourceRange} which start at start of "a" and ends at start of "b".
   */
  public static SourceRange forStartStart(HasSourceInfo a, HasSourceInfo b) {
    int start = a.getSourceInfo().getOffset();
    int end = b.getSourceInfo().getOffset();
    return forStartEnd(start, end);
  }

  /**
   * @return the {@link SourceRange} which start "start" and ends at start of "b".
   */
  public static SourceRange forStartStart(int start, HasSourceInfo b) {
    int end = b.getSourceInfo().getOffset();
    return forStartEnd(start, end);
  }

  public static SourceRange forToken(Token token) {
    return forStartLength(token.getOffset(), token.getLength());
  }

  /**
   * @return the {@link SourceRange} of "a" with offset from given "base".
   */
  public static SourceRange fromBase(HasSourceInfo a, int base) {
    int start = a.getSourceInfo().getOffset() - base;
    int length = a.getSourceInfo().getLength();
    return forStartLength(start, length);
  }

  /**
   * @return the {@link SourceRange} of "a" with offset from given "base".
   */
  public static SourceRange fromBase(HasSourceInfo a, SourceRange base) {
    return fromBase(a, base.getOffset());
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
  public static SourceRange withBase(HasSourceInfo base, SourceRange r) {
    return withBase(base.getSourceInfo().getOffset(), r);
  }

  /**
   * Given {@link SourceRange} created relative to "base", return absolute {@link SourceRange}.
   */
  public static SourceRange withBase(int base, SourceRange r) {
    int start = base + r.getOffset();
    int length = r.getLength();
    return forStartLength(start, length);
  }

}
