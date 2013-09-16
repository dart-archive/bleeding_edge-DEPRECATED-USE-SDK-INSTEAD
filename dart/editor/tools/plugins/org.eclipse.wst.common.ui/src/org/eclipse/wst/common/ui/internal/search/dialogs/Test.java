/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.common.ui.internal.search.dialogs;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.ActionDelegate;
import org.eclipse.wst.common.core.search.scope.SearchScope;

public class Test extends ActionDelegate {
  public void run(IAction action) {
    try {
      ComponentSearchListDialogConfiguration configuration = new ComponentSearchListDialogConfiguration();
      configuration.setListLabelText("List:");
      configuration.setFilterLabelText("Filter:");
      configuration.setSearchListProvider(searchListProvider);
      configuration.setDescriptionProvider(new BaseComponentDescriptionProvider("foo"));
      Shell shell = Display.getCurrent().getActiveShell();
      ComponentSearchListDialog dialog = new ComponentSearchListDialog(shell, "test", configuration);
      dialog.setBlockOnOpen(true);
      dialog.create();
      dialog.open();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  class BaseComponentDescriptionProvider extends LabelProvider implements
      IComponentDescriptionProvider {
    String prefix;

    BaseComponentDescriptionProvider(String prefix) {
      this.prefix = prefix;
    }

    public IFile getFile(Object component) {
      return null;
    }

    public ILabelProvider getLabelProvider() {
      return this;
    }

    public String getName(Object component) {
      String string = (String) component;
      return string.substring(prefix.length() + 1);
    }

    public String getQualifier(Object component) {
      return prefix;
    }

    public String getText(Object element) {
      return getName(element);
    }

    public boolean isApplicable(Object component) {
      if (component instanceof String) {
        return ((String) component).startsWith(prefix);
      }
      return false;
    }

    public Image getFileIcon(Object component) {
      return null;
    }
  }

  IComponentSearchListProvider searchListProvider = new IComponentSearchListProvider() {
    public void populateComponentList(IComponentList list, SearchScope scope, IProgressMonitor pm) {
      list.add("foo:" + "Hello!");
      list.add("foo:" + "Hey!");
      list.add("foo:" + "How-are-you?");
      list.add("foo:" + "What-is-that-smell?");
    }
  };
}
