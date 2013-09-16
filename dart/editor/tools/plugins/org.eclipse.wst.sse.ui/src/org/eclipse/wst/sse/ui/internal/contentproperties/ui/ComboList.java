/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.contentproperties.ui;

import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.sse.ui.internal.Logger;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * @deprecated People should manage their own combo/list
 */
public class ComboList {
  private Combo combo;

  private List list;

  public ComboList(Composite parent, int style) {
    combo = new Combo(parent, style);
    list = new ArrayList();
  }

  /*
   * following methods is original method of Combo class.
   */
  public void add(String key) {
    checkError();
    combo.add(key);
    list.add(key);
  }

  public void add(String key, int index) {
    checkError();
    combo.add(key, index);
    list.add(index, key);
  }

  public void add(String key, String value) {
    checkError();
    combo.add(key);
    list.add(value);
  }

  public void add(String key, String value, int index) {
    checkError();
    combo.add(key, index);
    list.add(index, value);
  }

  public void addFocusListener(FocusListener listener) {
    combo.addFocusListener(listener);
  }

  public void addModifyListener(org.eclipse.swt.events.ModifyListener listener) {
    combo.addModifyListener(listener);
  }

  public void addSelectionListener(org.eclipse.swt.events.SelectionListener listener) {
    combo.addSelectionListener(listener);

  }

  private void checkError() {
    if (!isConsistency()) {
      Logger.log(
          Logger.WARNING,
          "Difference between the number of keys[" + combo.getItemCount() + "] and the number of values[" + list.size() + "] in ComboList"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
  }

  public org.eclipse.swt.graphics.Point computeSize(int wHint, int hHint) {
    return combo.computeSize(wHint, hHint);
  }

  public Map createHashtable() {
    checkError();
    Map m = new Hashtable();
    for (int i = 0; i < list.size(); i++)
      m.put(combo.getItem(i), list.get(i));
    return m;
  }

  public void deselect(int i) {
    combo.deselect(i);
  }

  public void deselectAll() {
    combo.deselectAll();
  }

  public boolean equals(Object obj) {
    return combo.equals(obj);
  }

  private void exchangePosition(int i, int j) {
    String tmpKey = getItem(i);
    Object tmpValue = list.remove(i);

    combo.setItem(i, getItem(j));
    list.add(i, list.remove(j - 1));

    combo.setItem(j, tmpKey);
    list.add(j, tmpValue);
  }

  public boolean existsAsKey(String str) {
    if (combo.indexOf(str) >= 0)
      return true;
    else
      return false;
  }

  public boolean existsAsValue(String str) {
    if (list.indexOf(str) >= 0)
      return true;
    else
      return false;
  }

  public boolean getEnabled() {
    return combo.getEnabled();
  }

  public String getItem(int index) {
    return combo.getItem(index);
  }

  public int getItemCount() {
    return combo.getItemCount();
  }

  public String getKey(String value) {
    if (value == null)
      return null;
    int index = -1;
    checkError();
    for (int i = 0; i < list.size(); i++) {
      if (!list.get(i).equals(value))
        continue;
      index = i;
      break;
    }
    if (index != -1)
      return combo.getItem(index);
    else
      return null;
  }

  public Object getLayoutData() {
    return combo.getLayoutData();
  }

  public String getSelectedValue() {
    checkError();
    int index = getSelectionIndex();
    if (index < 0)
      return null;
    return getValue(getItem(index));
  }

  public int getSelectionIndex() {
    return combo.getSelectionIndex();
  }

  public String getText() {
    return combo.getText();
  }

  public String getValue(String key) {
    if (key == null)
      return null;
    int index = -1;
    checkError();
    for (int i = 0; i < combo.getItemCount(); i++) {
      if (!combo.getItem(i).equals(key))
        continue;
      index = i;
      break;
    }
    if (index != -1)
      return (String) list.get(index);
    else
      return null;
  }

  public int indexOf(String str) {
    return combo.indexOf(str);
  }

  public boolean isConsistency() {

    if (list.size() == combo.getItemCount())
      return true;
    else
      return false;
  }

  public void remove(int index) {
    checkError();
    combo.remove(index);
    list.remove(index);
  }

  public void remove(String str) {
    checkError();
    combo.remove(str);
    list.remove(str);
  }

  public void select(int index) {
    combo.select(index);
  }

  public void setEnabled(boolean enabled) {
    combo.setEnabled(enabled);
  }

  public boolean setFocus() {
    return combo.setFocus();
  }

  public void setItem(int index, String str) {
    checkError();
    combo.setItem(index, str);
    list.remove(index);
    list.add(index, str);
  }

  public void setItem(String[] strArray) {
    checkError();
    combo.setItems(strArray);
    for (int i = 0; i < strArray.length; i++)
      list.add(strArray[i]);
  }

  public void setLayout(org.eclipse.swt.widgets.Layout lo) {
    combo.setLayout(lo);
  }

  public void setLayoutData(Object layoutData) {
    combo.setLayoutData(layoutData);
  }

  public void setSelection(Point point) {
    combo.setSelection(point);
  }

  public void setText(String str) {
    combo.setText(str);
  }

  public void sortByKey(int offset) {
    if (offset < 0 || offset > this.combo.getItemCount() - 1)
      return;
    checkError();
    //
    for (int i = offset; i < combo.getItemCount() - 1; i++) {
      for (int j = i + 1; j < combo.getItemCount(); j++) {
        if (getItem(i).compareTo(getItem(j)) > 0) {
          exchangePosition(i, j);
        }
      }
    }
  }

}
