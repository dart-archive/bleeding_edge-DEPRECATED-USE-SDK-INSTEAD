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
package com.google.dart.tools.ui.internal.text;

import org.eclipse.core.runtime.Assert;

/**
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public class Selection {

  /** Flag indicating that the AST node somehow intersects with the selection. */
  public static final int INTERSECTS = 0;

  /** Flag that indicates that an AST node appears before the selected nodes. */
  public static final int BEFORE = 1;

  /** Flag indicating that an AST node is covered by the selection. */
  public static final int SELECTED = 2;

  /** Flag indicating that an AST nodes appears after the selected nodes. */
  public static final int AFTER = 3;

  /**
   * Creates a new selection from the given start and end.
   * 
   * @param s the start offset of the selection (inclusive)
   * @param e the end offset of the selection (inclusive)
   * @return the created selection object
   */
  public static Selection createFromStartEnd(int s, int e) {
    Assert.isTrue(s >= 0 && e >= s);
    Selection result = new Selection();
    result.fStart = s;
    result.fLength = e - s + 1;
    result.fExclusiveEnd = result.fStart + result.fLength;
    return result;
  }

  /**
   * Creates a new selection from the given start and length.
   * 
   * @param s the start offset of the selection (inclusive)
   * @param l the length of the selection
   * @return the created selection object
   */
  public static Selection createFromStartLength(int s, int l) {
    Assert.isTrue(s >= 0 && l >= 0);
    Selection result = new Selection();
    result.fStart = s;
    result.fLength = l;
    result.fExclusiveEnd = s + l;
    return result;
  }

  private int fStart;

  private int fLength;

  private int fExclusiveEnd;

  protected Selection() {
  }

  public boolean covers(int position) {
    return fStart <= position && position < fStart + fLength;
  }

  // cover* methods do a closed interval check.

  public int getExclusiveEnd() {
    return fExclusiveEnd;
  }

  public int getInclusiveEnd() {
    return fExclusiveEnd - 1;
  }

  public int getLength() {
    return fLength;
  }

  public int getOffset() {
    return fStart;
  }

  @Override
  public String toString() {
    return "<start == " + fStart + ", length == " + fLength + "/>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
  }
}
