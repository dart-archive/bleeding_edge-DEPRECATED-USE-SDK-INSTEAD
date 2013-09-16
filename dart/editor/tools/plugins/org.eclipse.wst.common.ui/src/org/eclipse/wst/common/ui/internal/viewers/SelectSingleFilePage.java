/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - Initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.common.ui.internal.viewers;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;

// import org.eclipse.ui.help.WorkbenchHelp;

public class SelectSingleFilePage extends WizardPage {
  protected IWorkbench workbench;
  protected SelectSingleFileView selectSingleFileView;

  // parameter isFileMandatory is used to determine if a file must be selected  
  // before being able to proceed to the next page
  public SelectSingleFilePage(IWorkbench workbench, IStructuredSelection selection,
      boolean isFileMandatory) {
    super("SelectSingleFilePage");
    this.workbench = workbench;
    selectSingleFileView = new SelectSingleFileView(selection, isFileMandatory);
  }

  public void setVisible(boolean visible) {
    selectSingleFileView.setVisibleHelper(visible);
    super.setVisible(visible);
  }

  public void createControl(Composite parent) {
    SelectSingleFileView.Listener listener = new SelectSingleFileView.Listener() {
      public void setControlComplete(boolean isComplete) {
        setPageComplete(isComplete);
      }
    };
    selectSingleFileView.setListener(listener);
    Control control = selectSingleFileView.createControl(parent);
//    WorkbenchHelp.setHelp(control, B2BGUIContextIds.BTBG_SELECT_SINGLE_FILE_PAGE);
    setControl(control);
  }

  public void addFilter(ViewerFilter filter) {
    selectSingleFileView.addFilter(filter);
  }

  public void addFilterExtensions(String[] filterExtensions) {
    selectSingleFileView.addFilterExtensions(filterExtensions);
  }

  public void resetFilters() {
    selectSingleFileView.resetFilters();
  }

  public IFile getFile() {
    return selectSingleFileView.getFile();
  }

  /**
   * Returns the selectSingleFileView.
   * 
   * @return SelectSingleFileView
   */
  public TreeViewer getSourceFileViewer() {
    return selectSingleFileView.sourceFileViewer;
  }

  /**
   * Returns the selectSingleFileView.
   * 
   * @return SelectSingleFileView
   */
  public SelectSingleFileView getSelectSingleFileView() {
    return selectSingleFileView;
  }
}
