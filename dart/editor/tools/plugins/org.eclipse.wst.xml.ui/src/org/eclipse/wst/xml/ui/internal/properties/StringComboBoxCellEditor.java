/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.properties;

import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.sse.ui.internal.Logger;

/**
 * An extended ComboBoxCellEditor that selects and returns Strings
 */

public class StringComboBoxCellEditor extends ComboBoxCellEditor {
  private boolean fSettingValue = false;

  /**
	 * 
	 */
  public StringComboBoxCellEditor() {
    super();
  }

  /**
   * @param parent
   * @param items
   */
  public StringComboBoxCellEditor(Composite parent, String[] items) {
    super(parent, items);
  }

  /**
   * @param parent
   * @param items
   * @param style
   */
  public StringComboBoxCellEditor(Composite parent, String[] items, int style) {
    super(parent, items, style);
  }

  protected Object doGetValue() {
    // otherwise limits to set of valid values
    Object index = super.doGetValue();
    int selection = -1;
    if (index instanceof Integer) {
      selection = ((Integer) index).intValue();
    }
    if (selection >= 0) {
      return getItems()[selection];
    } else if (getControl() instanceof CCombo) {
      // retrieve the actual text as the list of valid items doesn't
      // contain the value
      return ((CCombo) getControl()).getText();
    }
    return null;
  }

  protected void doSetValue(Object value) {
    if (fSettingValue) {
      return;
    }
    fSettingValue = true;
    if (value instanceof Integer) {
      super.doSetValue(value);
    } else {
      String stringValue = value.toString();
      int selection = -1;
      for (int i = 0; i < getItems().length; i++) {
        if (getItems()[i].equals(stringValue)) {
          selection = i;
        }
      }
      if (selection >= 0) {
        super.doSetValue(new Integer(selection));
      } else {
        super.doSetValue(new Integer(-1));
        if ((getControl() instanceof CCombo)
            && !stringValue.equals(((CCombo) getControl()).getText())) {
          // update the Text widget
          ((CCombo) getControl()).setText(stringValue);
        }
      }
    }
    fSettingValue = false;
  }

  public void setItems(String[] newItems) {
    if ((getControl() == null) || getControl().isDisposed()) {
      Logger.log(Logger.ERROR, "Attempted to update item list for disposed cell editor"); //$NON-NLS-1$
      return;
    }

    // keep selection if possible
    Object previousSelectedValue = getValue();
    super.setItems(newItems);
    if ((previousSelectedValue != null) && (getControl() instanceof CCombo)) {
      for (int i = 0; i < newItems.length; i++) {
        if (newItems[i].equals(previousSelectedValue)) {
          setValue(previousSelectedValue);
        }
      }
    }
  }
}
