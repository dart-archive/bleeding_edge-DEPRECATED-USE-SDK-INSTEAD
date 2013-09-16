/*******************************************************************************
 * Copyright (c) 2008 Standards for Technology in Automotive Retail and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: David Carver - initial API and
 * implementation, bug 212330
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.wizards;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class NewXMLHandler extends AbstractHandler implements IHandler {

  /**
	 * 
	 */
  public NewXMLHandler() {
    super();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
   */
  public Object execute(ExecutionEvent event) throws ExecutionException {
    IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
    ISelection selection = workbenchWindow.getSelectionService().getSelection();
    Object selectedObject = getSelection(selection);

    if ((selectedObject instanceof IFile) && (selection instanceof IStructuredSelection)) {
      IFile file = (IFile) selectedObject;
      IStructuredSelection structuredSelection = (IStructuredSelection) selection;
      try {
        NewXMLWizard.showDialog(workbenchWindow.getShell(), file, structuredSelection);
      } catch (Exception e) {
        // XMLCorePlugin.getDefault().getLog().log();
      }
    }
    return null;
  }

  public static Object getSelection(ISelection selection) {
    if (selection == null) {
      return null;
    }

    Object result = null;
    if (selection instanceof IStructuredSelection) {
      IStructuredSelection es = (IStructuredSelection) selection;
      Iterator i = es.iterator();
      if (i.hasNext()) {
        result = i.next();
      }
    }
    return result;
  }
}
