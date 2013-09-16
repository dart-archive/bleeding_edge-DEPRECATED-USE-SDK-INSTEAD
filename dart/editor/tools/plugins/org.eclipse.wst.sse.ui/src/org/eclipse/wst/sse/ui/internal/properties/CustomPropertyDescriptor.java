/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.properties;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.PropertyDescriptor;

public class CustomPropertyDescriptor extends PropertyDescriptor {

  protected Class cellEditorClass = null;

  /**
   * Returns a property descriptor with a unique name and a display name.
   * 
   * @param id the id for the property
   * @param displayName the name to display for the property
   */
  public CustomPropertyDescriptor(Object id, String newDisplayName, Class editorClass) {
    super(id, newDisplayName);
    setDescription((String) id);
    setCellEditorClass(editorClass);
  }

  /**
   * Returns a property descriptor with a unique name and a display name.
   * 
   * @param uniqueName the unique name of the property
   * @param displayName the name to display for the property
   */
  public CustomPropertyDescriptor(String uniqueName, String newDisplayName) {
    this(uniqueName, newDisplayName, TextCellEditor.class);
  }

  public CellEditor createPropertyEditor(Composite parent) {
    return getPropertyEditor(parent);
  }

  public Class getCellEditorClass() {
    return cellEditorClass;
  }

  /**
   * Returns the editor used to edit the property.
   * 
   * @return an editor for the property
   */
  protected CellEditor getPropertyEditor(Composite parent) {
    if (getCellEditorClass() == null)
      return null;

    java.lang.reflect.Constructor constructor = null;
    try {
      constructor = getCellEditorClass().getDeclaredConstructor(new Class[] {Composite.class});
    } catch (NoSuchMethodException nsme) {
      return new TextCellEditor(parent);
    }
    if (constructor != null) {
      try {
        return (CellEditor) constructor.newInstance(new Object[] {parent});
      } catch (InstantiationException ie) {
      } catch (java.lang.reflect.InvocationTargetException ite) {
      } catch (IllegalAccessException iae) {
      }
    }
    return new TextCellEditor(parent);
  }

  public void setCellEditorClass(Class newCellEditorClass) {
    cellEditorClass = newCellEditorClass;
  }
}
