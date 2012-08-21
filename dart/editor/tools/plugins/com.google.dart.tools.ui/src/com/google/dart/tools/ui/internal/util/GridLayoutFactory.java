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

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;

/**
 * GridLayoutFactory provides a convenient shorthand for creating and initializing GridLayout.
 */
public final class GridLayoutFactory {
  public static GridLayoutFactory create(Composite composite) {
    return new GridLayoutFactory(composite, new GridLayout());
  }

  public static GridLayoutFactory modify(Composite composite) {
    Layout layout = composite.getLayout();
    if (layout instanceof GridLayout) {
      return new GridLayoutFactory(composite, (GridLayout) layout);
    }
    return create(composite);
  }

  private final GridLayout layout;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor 
  //
  ////////////////////////////////////////////////////////////////////////////
  private GridLayoutFactory(Composite composite, GridLayout layout) {
    this.layout = layout;
    composite.setLayout(layout);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Sets number of columns in {@link GridLayout}.
   */
  public GridLayoutFactory columns(int numColumns) {
    layout.numColumns = numColumns;
    return this;
  }

  /**
   * Specifies whether all columns in the layout will be forced to have the same width.
   */
  public GridLayoutFactory equalColumns() {
    layout.makeColumnsEqualWidth = true;
    return this;
  }

  /**
   * Sets the vertical margins.
   */
  public GridLayoutFactory marginHeight(int margins) {
    layout.marginHeight = margins;
    return this;
  }

  /**
   * Sets the horizontal/vertical margins.
   */
  public GridLayoutFactory margins(int margins) {
    layout.marginWidth = layout.marginHeight = margins;
    return this;
  }

  /**
   * Sets the horizontal margins.
   */
  public GridLayoutFactory marginWidth(int margins) {
    layout.marginWidth = margins;
    return this;
  }

  /**
   * Sets zero horizontal and vertical margins.
   */
  public GridLayoutFactory noMargins() {
    layout.marginWidth = layout.marginHeight = 0;
    return this;
  }

  /**
   * Sets zero horizontal and vertical spacing.
   */
  public GridLayoutFactory noSpacing() {
    layout.horizontalSpacing = layout.verticalSpacing = 0;
    return this;
  }

  /**
   * Sets horizontal spacing.
   */
  public GridLayoutFactory spacingHorizontal(int spacing) {
    layout.horizontalSpacing = spacing;
    return this;
  }

  /**
   * Sets vertical spacing.
   */
  public GridLayoutFactory spacingVertical(int spacing) {
    layout.verticalSpacing = spacing;
    return this;
  }
}
