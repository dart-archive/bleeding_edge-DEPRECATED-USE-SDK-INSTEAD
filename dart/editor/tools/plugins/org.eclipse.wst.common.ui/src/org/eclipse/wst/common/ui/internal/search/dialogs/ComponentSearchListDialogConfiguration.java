/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.common.ui.internal.search.dialogs;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.wst.common.ui.internal.Messages;

public class ComponentSearchListDialogConfiguration {
  private String filterLabelText = Messages._UI_LABEL_COMPONENT_NAME;
  private String listLabelText = Messages._UI_LABEL_COMPONENTS;
  private IComponentSearchListProvider searchListProvider;
  private IComponentDescriptionProvider descriptionProvider;
  private INewComponentHandler newComponentHandler;
  private ComponentSearchListDialog dialog;

  public void init(ComponentSearchListDialog dialog) {
    this.dialog = dialog;
  }

  public void createWidgetAtTop(Composite parent) {
  }

  public void createWidgetAboveQualifierBox(Composite parent) {
  }

  public void createWidgetBelowQualifierBox(Composite parent) {
  }

  public void createToolBarItems(ToolBar toolBar) {
  }

  public IComponentDescriptionProvider getDescriptionProvider() {
    return descriptionProvider;
  }

  public void setDescriptionProvider(IComponentDescriptionProvider descriptionProvider) {
    this.descriptionProvider = descriptionProvider;
  }

  public IComponentSearchListProvider getSearchListProvider() {
    return searchListProvider;
  }

  public void setSearchListProvider(IComponentSearchListProvider searchListProvider) {
    this.searchListProvider = searchListProvider;
  }

  public ComponentSearchListDialog getDialog() {
    return dialog;
  }

  public INewComponentHandler getNewComponentHandler() {
    return newComponentHandler;
  }

  public void setNewComponentHandler(INewComponentHandler newComponentHandler) {
    this.newComponentHandler = newComponentHandler;
  }

  public String getFilterLabelText() {
    return filterLabelText;
  }

  public String getListLabelText() {
    return listLabelText;
  }

  public void setFilterLabelText(String filterLabelText) {
    this.filterLabelText = filterLabelText;
  }

  public void setListLabelText(String listLabelText) {
    this.listLabelText = listLabelText;
  }
}
