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
package com.google.dart.tools.ui.internal.util;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Caret;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Widget;

/**
 * Utility class to simplify access to some SWT resources.
 */
public class SWTUtil {

  private static final int COMBO_VISIBLE_ITEM_COUNT = 30;

  /**
   * Returns a width hint for a button control.
   */
  public static int getButtonWidthHint(Button button) {
    button.setFont(JFaceResources.getDialogFont());
    @SuppressWarnings("deprecation")
    PixelConverter converter = new PixelConverter(button);
    int widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    return Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
  }

  /**
   * Returns the shell for the given widget. If the widget doesn't represent a SWT object that
   * manage a shell, <code>null</code> is returned.
   * 
   * @return the shell for the given widget
   */
  public static Shell getShell(Widget widget) {
    if (widget instanceof Control) {
      return ((Control) widget).getShell();
    }
    if (widget instanceof Caret) {
      return ((Caret) widget).getParent().getShell();
    }
    if (widget instanceof DragSource) {
      return ((DragSource) widget).getControl().getShell();
    }
    if (widget instanceof DropTarget) {
      return ((DropTarget) widget).getControl().getShell();
    }
    if (widget instanceof Menu) {
      return ((Menu) widget).getParent().getShell();
    }
    if (widget instanceof ScrollBar) {
      return ((ScrollBar) widget).getParent().getShell();
    }

    return null;
  }

  /**
   * Returns the standard display to be used. The method first checks, if the thread calling this
   * method has an associated disaply. If so, this display is returned. Otherwise the method returns
   * the default display.
   */
  public static Display getStandardDisplay() {
    Display display;
    display = Display.getCurrent();
    if (display == null) {
      display = Display.getDefault();
    }
    return display;
  }

  public static int getTableHeightHint(Table table, int rows) {
    if (table.getFont().equals(JFaceResources.getDefaultFont())) {
      table.setFont(JFaceResources.getDialogFont());
    }
    int result = table.getItemHeight() * rows + table.getHeaderHeight();
    if (table.getLinesVisible()) {
      result += table.getGridLineWidth() * (rows - 1);
    }
    return result;
  }

  /**
   * Sets width and height hint for the button control. <b>Note:</b> This is a NOP if the button's
   * layout data is not an instance of <code>GridData</code> .
   * 
   * @param button the button for which to set the dimension hint
   */
  public static void setButtonDimensionHint(Button button) {
    Assert.isNotNull(button);
    Object gd = button.getLayoutData();
    if (gd instanceof GridData) {
      ((GridData) gd).widthHint = getButtonWidthHint(button);
      ((GridData) gd).horizontalAlignment = GridData.FILL;
    }
  }

  /**
   * Sets the default visible item count for {@link Combo}s. Workaround for
   * https://bugs.eclipse.org/bugs/show_bug.cgi?id=7845 .
   * 
   * @param combo the combo
   * @see Combo#setVisibleItemCount(int)
   * @see #COMBO_VISIBLE_ITEM_COUNT
   * @since 3.5
   */
  public static void setDefaultVisibleItemCount(Combo combo) {
    combo.setVisibleItemCount(COMBO_VISIBLE_ITEM_COUNT);
  }

}
