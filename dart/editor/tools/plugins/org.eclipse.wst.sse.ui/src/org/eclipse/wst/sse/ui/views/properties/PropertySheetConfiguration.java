/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.views.properties;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.IPropertySourceProvider;

/**
 * Configuration class for Property Sheet Pages. Not finalized.
 * 
 * @since 1.0
 */
public abstract class PropertySheetConfiguration {
  /**
   * Create new instance of PropertySheetConfiguration
   */
  public PropertySheetConfiguration() {
    // Must have empty constructor to createExecutableExtension
    super();
  }

  /**
   * Adds contribution menu items to the given menuManager, toolbarManager, statusLineManager.
   * 
   * @param menuManager the local menu manager of the property sheet
   * @param toolBarManager the local toolbar manager of the property sheet
   * @param statusLineManager the status line manager of the property sheet
   */
  public void addContributions(IMenuManager menuManager, IToolBarManager toolBarManager,
      IStatusLineManager statusLineManager) {
    // do nothing
  }

  /**
   * Allows for filteration of selection before being sent to the viewer.
   * 
   * @param selectingPart may be null
   * @param selection model selection
   * @return the (filtered) selection to be sent to the viewer
   */
  public ISelection getInputSelection(IWorkbenchPart selectingPart, ISelection selection) {
    ISelection preferredSelection = selection;
    if (selection instanceof IStructuredSelection) {
      // don't support more than one selected node
      if (((IStructuredSelection) selection).size() > 1)
        preferredSelection = StructuredSelection.EMPTY;
    }
    return preferredSelection;
  }

  /**
   * Returns the correct IPropertySourceProvider.
   * 
   * @param page the page to be configured by this configuration
   * @return the IPropertySourceProvider for the given page
   */
  public abstract IPropertySourceProvider getPropertySourceProvider(IPropertySheetPage page);

  /**
   * Removes contribution menu items from the given menuManager, toolbarManager, statusLineManager.
   * 
   * @param menuManager the local menu manager of the property sheet
   * @param toolBarManager the local toolbar manager of the property sheet
   * @param statusLineManager the status line manager of the property sheet
   */
  public void removeContributions(IMenuManager menuManager, IToolBarManager toolBarManager,
      IStatusLineManager statusLineManager) {
    // do nothing
  }

  /**
   * General hook for resource releasing and listener removal when configurations change.
   */
  public void unconfigure() {
  }
}
