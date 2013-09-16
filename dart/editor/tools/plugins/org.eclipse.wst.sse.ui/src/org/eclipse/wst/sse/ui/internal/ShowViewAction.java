/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

/**
 * Surfaces a View.
 * 
 * @author Nitin Dahyabhai
 */
public abstract class ShowViewAction extends Action {
  /**
	 *  
	 */
  public ShowViewAction() {
    super();
  }

  /**
   * @param text
   */
  public ShowViewAction(String text) {
    super(text);
  }

  /**
   * @param text
   * @param image
   */
  public ShowViewAction(String text, ImageDescriptor image) {
    super(text, image);
  }

  /**
   * @param text
   * @param style
   */
  public ShowViewAction(String text, int style) {
    super(text, style);
  }

  /**
   * @return
   */
  protected abstract String getViewID();

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.Action#run()
   */
  public void run() {
    super.run();
    showView();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.Action#runWithEvent(org.eclipse.swt.widgets.Event)
   */
  public void runWithEvent(Event event) {
    super.runWithEvent(event);
    showView();
  }

  /**
	 *  
	 */
  private void showView() {
    IWorkbenchWindow window = SSEUIPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
    IWorkbenchPage page = window.getActivePage();
    if (page != null) {
      try {
        page.showView(getViewID());
      } catch (PartInitException e) {
        ErrorDialog.openError(window.getShell(), SSEUIMessages.ShowView_errorTitle, //$NON-NLS-1$
            e.getMessage(), e.getStatus());
      }
    }
  }
}
