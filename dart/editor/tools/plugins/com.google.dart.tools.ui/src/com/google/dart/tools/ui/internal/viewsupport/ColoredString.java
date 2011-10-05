/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.viewsupport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ColoredString {

  public static class Style {
    private final String fForegroundColorName;

    public Style(String foregroundColorName) {
      fForegroundColorName = foregroundColorName;
    }

    public String getForegroundColorName() {
      return fForegroundColorName;
    }
  }

  public static class StyleRange {
    public int offset;
    public int length;
    public Style style;

    public StyleRange(int offset, int length, Style style) {
      this.offset = offset;
      this.length = length;
      this.style = style;
    }
  }

  public static final Style DEFAULT_STYLE = null;
  private final StringBuffer fBuffer;

  private ArrayList<StyleRange> fRanges;

  private static final List<StyleRange> EMPTY_LIST = new ArrayList<ColoredString.StyleRange>();

  public ColoredString() {
    fBuffer = new StringBuffer();
    fRanges = null;
  }

  public ColoredString(String text) {
    this(text, ColoredString.DEFAULT_STYLE);
  }

  public ColoredString(String text, Style style) {
    this();
    append(text, style);
  }

  public ColoredString append(char ch) {
    return append(String.valueOf(ch), DEFAULT_STYLE);
  }

  public ColoredString append(ColoredString string) {
    int offset = fBuffer.length();
    fBuffer.append(string.getString());
    for (Iterator<StyleRange> iterator = string.getRanges(); iterator.hasNext();) {
      StyleRange curr = iterator.next();
      addRange(new StyleRange(offset + curr.offset, curr.length, curr.style));
    }
    return this;
  }

  public ColoredString append(String text) {
    return append(text, DEFAULT_STYLE);
  }

  public ColoredString append(String text, Style style) {
    if (text.length() == 0) {
      return this;
    }

    int offset = fBuffer.length();
    fBuffer.append(text);
    if (style != null) {
      int nRanges = getNumberOfRanges();
      if (nRanges > 0) {
        StyleRange last = getRange(nRanges - 1);
        if (last.offset + last.length == offset && style.equals(last.style)) {
          last.length += text.length();
          return this;
        }
      }
      addRange(new StyleRange(offset, text.length(), style));
    }
    return this;
  }

  public void colorize(int offset, int length, Style style) {
    if (offset < 0 || offset + length > fBuffer.length()) {
      throw new IllegalArgumentException(
          "Invalid offset (" + offset + ") or length (" + length + ")"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
    }

    int insertPos = 0;
    int nRanges = getNumberOfRanges();
    for (int i = 0; i < nRanges; i++) {
      StyleRange curr = getRange(i);
      if (curr.offset + curr.length <= offset) {
        insertPos = i + 1;
      }
    }
    if (insertPos < nRanges) {
      StyleRange curr = getRange(insertPos);
      if (curr.offset > offset + length) {
        throw new IllegalArgumentException("Overlapping ranges"); //$NON-NLS-1$
      }
    }
    addRange(insertPos, new StyleRange(offset, length, style));
  }

  public Iterator<StyleRange> getRanges() {
    if (!hasRanges()) {
      return EMPTY_LIST.iterator();
    }
    return getRangesList().iterator();
  }

  public String getString() {
    return fBuffer.toString();
  }

  public int length() {
    return fBuffer.length();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return fBuffer.toString();
  }

  private void addRange(int index, StyleRange range) {
    getRangesList().add(index, range);
  }

  private void addRange(StyleRange range) {
    getRangesList().add(range);
  }

  private int getNumberOfRanges() {
    return fRanges == null ? 0 : fRanges.size();
  }

  private StyleRange getRange(int index) {
    if (fRanges != null) {
      return fRanges.get(index);
    }
    throw new IndexOutOfBoundsException();
  }

  private List<StyleRange> getRangesList() {
    if (fRanges == null) {
      fRanges = new ArrayList<StyleRange>(2);
    }
    return fRanges;
  }

  private boolean hasRanges() {
    return fRanges != null && !fRanges.isEmpty();
  }
}
