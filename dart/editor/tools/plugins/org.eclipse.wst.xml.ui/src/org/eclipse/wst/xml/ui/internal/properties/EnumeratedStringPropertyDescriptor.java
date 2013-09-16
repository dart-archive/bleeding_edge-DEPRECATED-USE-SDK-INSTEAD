/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.properties;

import java.util.Arrays;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.PropertyDescriptor;

/**
 * This class should be used for properties which require a combo box cell editor and whose values
 * consist of a list of enumerated strings.
 */
public class EnumeratedStringPropertyDescriptor extends PropertyDescriptor {
  protected StringComboBoxCellEditor fEditor;
  protected Composite fParent;

  /**
   * The enumerated possible values for the described property
   */
  protected String fValues[] = null;

  public EnumeratedStringPropertyDescriptor(Object id, String newDisplayName, String[] valuesArray) {
    super(id, newDisplayName);
    setDescription((String) id);
    fValues = valuesArray;
  }

  /**
   * Creates and returns a new cell editor for editing this property. Returns <code>null</code> if
   * the property is not editable.
   * 
   * @param parent the parent widget for the cell editor
   * @return the cell editor for this property, or <code>null</code> if this property cannot be
   *         edited
   */
  public CellEditor createPropertyEditor(Composite parent) {
    // Check to see if we already have a Cell Editor with a valid Control
    // under the given parent.
    // If any of that's not true, create and return a new Cell Editor
    if ((fEditor == null) || (fEditor.getControl() == null) || fEditor.getControl().isDisposed()
        || (parent != fParent)) {
      fEditor = new StringComboBoxCellEditor(parent, fValues);
    }
    fParent = parent;
    return fEditor;
  }

  public void updateValues(String newValues[]) {
    if (Arrays.equals(fValues, newValues)) {
      return;
    }
    fValues = newValues;
    if (fEditor != null) {
      fEditor.setItems(newValues);
    }
  }
}
