/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.wizards;

import java.util.Iterator;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

// TODO cs... rename this class NewXMLActionDelegate
// we need to re-add pre-validation using the validation framework API's
// since we also need to add validation to the 'New XML' case, this
// prevalidation
// function should really go into the NewWizard somewhere
//
public class XMLImportActionDelegate implements IActionDelegate {
  /**
   * Checks the current selection and runs the separate browser to show the content of the Readme
   * file. This code shows how to launch separate browsers that are not VA/Base desktop parts.
   * 
   * @param action the action that was performed
   */
  public void run(IAction action) {
    IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
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
  }

  /**
   * unused
   */
  public void selectionChanged(IAction action, ISelection selection) {
    // unused
  }

  // scammed from WindowUtility
  //
  public static Object getSelection(ISelection selection) {
    if (selection == null) {
      return null;
    } // end of if ()

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
