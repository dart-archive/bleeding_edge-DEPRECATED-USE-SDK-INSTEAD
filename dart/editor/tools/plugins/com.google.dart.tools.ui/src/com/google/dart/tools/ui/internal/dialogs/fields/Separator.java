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
package com.google.dart.tools.ui.internal.dialogs.fields;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * Dialog field describing a separator.
 */
public class Separator extends DialogField {

  protected static GridData gridDataForSeperator(int span, int height) {
    GridData gd = new GridData();
    gd.horizontalAlignment = GridData.FILL;
    gd.verticalAlignment = GridData.BEGINNING;
    gd.heightHint = height;
    gd.horizontalSpan = span;
    return gd;
  }

  private Label fSeparator;

  private int fStyle;

  public Separator() {
    this(SWT.NONE);
  }

  /**
   * @param style of the separator. See <code>Label</code> for possible styles.
   */
  public Separator(int style) {
    super();
    fStyle = style;
  }

  /*
   * @see DialogField#doFillIntoGrid
   */
  @Override
  public Control[] doFillIntoGrid(Composite parent, int nColumns) {
    return doFillIntoGrid(parent, nColumns, 4);
  }

  /**
   * Creates the separator and fills it in a MGridLayout.
   * 
   * @param height The height of the separator
   */
  public Control[] doFillIntoGrid(Composite parent, int nColumns, int height) {
    assertEnoughColumns(nColumns);

    Control separator = getSeparator(parent);
    separator.setLayoutData(gridDataForSeperator(nColumns, height));

    return new Control[] {separator};
  }

  /*
   * @see DialogField#getNumberOfControls
   */
  @Override
  public int getNumberOfControls() {
    return 1;
  }

  /**
   * Creates or returns the created separator.
   * 
   * @param parent The parent composite or <code>null</code> if the widget has already been created.
   */
  public Control getSeparator(Composite parent) {
    if (fSeparator == null) {
      assertCompositeNotNull(parent);
      fSeparator = new Label(parent, fStyle);
    }
    return fSeparator;
  }

}
