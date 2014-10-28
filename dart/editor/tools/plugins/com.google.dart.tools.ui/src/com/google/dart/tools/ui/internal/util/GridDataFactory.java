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
package com.google.dart.tools.ui.internal.util;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Control;

/**
 * This class provides a convenient shorthand for creating and initializing GridData.
 */
public final class GridDataFactory {
  /**
   * Creates new {@link GridDataFactory} with new {@link GridData}.
   */
  public static GridDataFactory create(Control control) {
    return new GridDataFactory(control, new GridData());
  }

  /**
   * Creates new {@link GridDataFactory} for modifying {@link GridData} already installed in
   * control.
   */
  public static GridDataFactory modify(Control control) {
    GridData gridData;
    {
      Object existingLayoutData = control.getLayoutData();
      if (existingLayoutData instanceof GridData) {
        gridData = (GridData) existingLayoutData;
      } else {
        gridData = new GridData();
      }
    }
    return new GridDataFactory(control, gridData);
  }

  private final PixelConverter pixelConverter;

  private final GridData data;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  private GridDataFactory(Control control, GridData gridData) {
    this.pixelConverter = new PixelConverter(control);
    this.data = gridData;
    if (control.getLayoutData() != data) {
      control.setLayoutData(data);
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Alignment
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Sets the alignment of the control within its cell.
   * 
   * @param hAlign horizontal alignment. One of SWT.BEGINNING, SWT.CENTER, SWT.END, or SWT.FILL.
   * @param vAlign vertical alignment. One of SWT.BEGINNING, SWT.CENTER, SWT.END, or SWT.FILL.
   * @return this
   */
  public GridDataFactory align(int hAlign, int vAlign) {
    data.horizontalAlignment = hAlign;
    data.verticalAlignment = vAlign;
    return this;
  }

  /**
   * Sets the horizontal alignment of the control within its cell.
   * 
   * @param hAlign horizontal alignment. One of SWT.BEGINNING, SWT.CENTER, SWT.END, or SWT.FILL.
   * @return this
   */
  public GridDataFactory alignHorizontal(int hAlign) {
    data.horizontalAlignment = hAlign;
    return this;
  }

  /**
   * Sets the horizontal alignment of the control to GridData.CENTER
   * 
   * @return this
   */
  public GridDataFactory alignHorizontalCenter() {
    return alignHorizontal(GridData.CENTER);
  }

  /**
   * Sets the horizontal alignment of the control to GridData.FILL
   * 
   * @return this
   */
  public GridDataFactory alignHorizontalFill() {
    return alignHorizontal(GridData.FILL);
  }

  /**
   * Sets the horizontal alignment of the control to GridData.BEGINNING
   * 
   * @return this
   */
  public GridDataFactory alignHorizontalLeft() {
    return alignHorizontal(GridData.BEGINNING);
  }

  /**
   * Sets the horizontal alignment of the control to GridData.END
   * 
   * @return this
   */
  public GridDataFactory alignHorizontalRight() {
    return alignHorizontal(GridData.END);
  }

  /**
   * Sets the vertical alignment of the control within its cell.
   * 
   * @param vAlign vertical alignment. One of SWT.BEGINNING, SWT.CENTER, SWT.END, or SWT.FILL.
   * @return this
   */
  public GridDataFactory alignVertical(int vAlign) {
    data.verticalAlignment = vAlign;
    return this;
  }

  /**
   * Sets the vertical alignment of the control to GridData.END
   * 
   * @return this
   */
  public GridDataFactory alignVerticalBottom() {
    return alignVertical(GridData.END);
  }

  /**
   * Sets the vertical alignment of the control to GridData.FILL
   * 
   * @return this
   */
  public GridDataFactory alignVerticalFill() {
    return alignVertical(GridData.FILL);
  }

  /**
   * Sets the vertical alignment of the control to GridData.CENTER
   * 
   * @return this
   */
  public GridDataFactory alignVerticalMiddle() {
    return alignVertical(GridData.CENTER);
  }

  /**
   * Sets the vertical alignment of the control to GridData.BEGINNING
   * 
   * @return this
   */
  public GridDataFactory alignVerticalTop() {
    return alignVertical(GridData.BEGINNING);
  }

  /**
   * Returns the number of pixels corresponding to the height of the given number of characters.
   * 
   * @param chars the number of characters
   * @return the number of pixels
   */
  public int convertHeightInCharsToPixels(int chars) {
    return pixelConverter.convertHeightInCharsToPixels(chars);
  }

  /**
   * Returns the number of pixels corresponding to the width of the given number of characters.
   * 
   * @param chars the number of characters
   * @return the number of pixels
   */
  public int convertWidthInCharsToPixels(int chars) {
    return pixelConverter.convertWidthInCharsToPixels(chars);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Exclude
  //
  ////////////////////////////////////////////////////////////////////////////
  public GridDataFactory exclude(boolean value) {
    data.exclude = value;
    return this;
  }

  /**
   * Sets the horizontal and vertical alignment to GridData.FILL.
   */
  public GridDataFactory fill() {
    return align(GridData.FILL, GridData.FILL);
  }

  /**
   * Sets the horizontal alignment of the control to GridData.FILL
   * 
   * @return this
   */
  public GridDataFactory fillHorizontal() {
    return alignHorizontalFill();
  }

  /**
   * Sets the vertical alignment of the control to GridData.FILL
   * 
   * @return this
   */
  public GridDataFactory fillVertical() {
    return alignVerticalFill();
  }

  public GridDataFactory grab() {
    return grab(true, true);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Grab
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Determines whether extra horizontal or vertical space should be allocated to this control's
   * column when the layout resizes. If any control in the column is set to grab horizontal then the
   * whole column will grab horizontal space. If any control in the row is set to grab vertical then
   * the whole row will grab vertical space.
   * 
   * @param horizontal true if the control's column should grow horizontally
   * @param vertical true if the control's row should grow vertically
   * @return this
   */
  public GridDataFactory grab(boolean horizontal, boolean vertical) {
    data.grabExcessHorizontalSpace = horizontal;
    data.grabExcessVerticalSpace = vertical;
    return this;
  }

  public GridDataFactory grabHorizontal() {
    data.grabExcessHorizontalSpace = true;
    return this;
  }

  public GridDataFactory grabVertical() {
    data.grabExcessVerticalSpace = true;
    return this;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Hint
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Sets the width and height hints. The width and height hints override the control's preferred
   * size. If either hint is set to SWT.DEFAULT, the control's preferred size is used.
   * 
   * @param xHint horizontal hint (pixels), or SWT.DEFAULT to use the control's preferred size
   * @param yHint vertical hint (pixels), or SWT.DEFAULT to use the control's preferred size
   * @return this
   */
  public GridDataFactory hint(int xHint, int yHint) {
    data.widthHint = xHint;
    data.heightHint = yHint;
    return this;
  }

  /**
   * Sets the width and height hints. The width and height hints override the control's preferred
   * size. If either hint is set to SWT.DEFAULT, the control's preferred size is used.
   * 
   * @param hint size (pixels) to be used instead of the control's preferred size. If the x or y
   *          values are set to SWT.DEFAULT, the control's computeSize() method will be used to
   *          obtain that dimension of the preferred size.
   * @return this
   */
  public GridDataFactory hint(Point hint) {
    data.widthHint = hint.x;
    data.heightHint = hint.y;
    return this;
  }

  /**
   * Sets hint in chars.
   */
  public GridDataFactory hintChars(int xHintInChars, int yHintInChars) {
    hintWidthChars(xHintInChars);
    hintHeightChars(yHintInChars);
    return this;
  }

  /**
   * Sets the height hint.
   * 
   * @return this
   */
  public GridDataFactory hintHeight(int yHint) {
    data.heightHint = yHint;
    return this;
  }

  /**
   * Increments vertical hint on given value.
   * 
   * @return this
   */
  public GridDataFactory hintHeightAdd(int increment) {
    return hintHeight(data.heightHint + increment);
  }

  /**
   * Sets the height hint in chars.
   * 
   * @return this
   */
  public GridDataFactory hintHeightChars(int hintInChars) {
    return hintHeight(convertHeightInCharsToPixels(hintInChars));
  }

  /**
   * Sets the width hint.
   * 
   * @return this
   */
  public GridDataFactory hintWidth(int xHint) {
    data.widthHint = xHint;
    return this;
  }

  /**
   * Increments horizontal hint on given value.
   * 
   * @return this
   */
  public GridDataFactory hintWidthAdd(int increment) {
    return hintHeight(data.widthHint + increment);
  }

  /**
   * Sets the width hint in chars.
   * 
   * @return this
   */
  public GridDataFactory hintWidthChars(int hintInChars) {
    return hintWidth(convertWidthInCharsToPixels(hintInChars));
  }

  /**
   * Sets the width hint in dialog units.
   * 
   * @return this
   */
  public GridDataFactory hintWidthUnits(int hintInDLU) {
    return hintWidth(pixelConverter.convertHorizontalDLUsToPixels(hintInDLU));
  }

  /**
   * Sets the width hint to the minimum of current hint and given <code>otherHint</code>.
   * 
   * @return this
   */
  public GridDataFactory hintWithMin(int otherHint) {
    data.widthHint = Math.min(data.widthHint, otherHint);
    return this;
  }

  /**
   * Sets the indent of the control within the cell in pixels.
   */
  public GridDataFactory indent(int hIndent, int vIndent) {
    data.horizontalIndent = hIndent;
    data.verticalIndent = vIndent;
    return this;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Indent
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Sets the indent of the control within the cell in pixels.
   */
  public GridDataFactory indentHorizontal(int hIndent) {
    data.horizontalIndent = hIndent;
    return this;
  }

  /**
   * Sets the indent of the control within the cell in characters.
   */
  public GridDataFactory indentHorizontalChars(int hIndent) {
    data.horizontalIndent = convertWidthInCharsToPixels(hIndent);
    return this;
  }

  /**
   * Sets the indent of the control within the cell in pixels.
   */
  public GridDataFactory indentVertical(int vIndent) {
    data.verticalIndent = vIndent;
    return this;
  }

  public GridDataFactory minHeight(int minimumHeight) {
    data.minimumHeight = minimumHeight;
    return this;
  }

  public GridDataFactory minHeightChars(int heightInChars) {
    return minHeight(convertHeightInCharsToPixels(heightInChars));
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Minimum size
  //
  ////////////////////////////////////////////////////////////////////////////
  public GridDataFactory minWidth(int minimumWidth) {
    data.minimumWidth = minimumWidth;
    return this;
  }

  public GridDataFactory minWidthChars(int widthInChars) {
    return minWidth(convertWidthInCharsToPixels(widthInChars));
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Span
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Sets the GridData span. The span controls how many cells are filled by the control.
   * 
   * @param hSpan number of columns spanned by the control
   * @param vSpan number of rows spanned by the control
   * @return this
   */
  public GridDataFactory span(int hSpan, int vSpan) {
    data.horizontalSpan = hSpan;
    data.verticalSpan = vSpan;
    return this;
  }

  /**
   * Sets the GridData span. The span controls how many cells are filled by the control.
   * 
   * @param hSpan number of columns spanned by the control
   * @return this
   */
  public GridDataFactory spanHorizontal(int hSpan) {
    data.horizontalSpan = hSpan;
    return this;
  }

  /**
   * Sets the GridData span. The span controls how many cells are filled by the control.
   * 
   * @param vSpan number of rows spanned by the control
   * @return this
   */
  public GridDataFactory spanVertical(int vSpan) {
    data.verticalSpan = vSpan;
    return this;
  }
}
