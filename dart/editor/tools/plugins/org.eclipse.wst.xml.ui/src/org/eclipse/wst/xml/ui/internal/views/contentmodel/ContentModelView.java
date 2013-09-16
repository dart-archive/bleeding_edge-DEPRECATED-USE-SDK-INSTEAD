/*******************************************************************************
 * Copyright (c) 2010 Standards for Technology in Automotive Retail and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: David Carver - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.xml.ui.internal.views.contentmodel;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.State;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.ViewPart;
import org.w3c.dom.Element;

public class ContentModelView extends ViewPart implements ISelectionListener {
  private TreeViewer tv;

  public void init(IViewSite site, IMemento memento) throws PartInitException {
    super.init(site, memento);
    getSite().getPage().addPostSelectionListener(this);
  }

  public void dispose() {
    getSite().getPage().removePostSelectionListener(this);
    super.dispose();
  }

  public void createPartControl(Composite parent) {
    Tree tree = new Tree(parent, SWT.NONE);
    tv = new TreeViewer(tree);
    tv.setContentProvider(new BaseWorkbenchContentProvider());
    tv.setLabelProvider(new WorkbenchLabelProvider());
  }

  public void setFocus() {

  }

  private IStructuredSelection currentSelection;

  public void selectionChanged(IWorkbenchPart part, ISelection selection) {

    IEditorPart edPart = getSite().getPage().getActiveEditor();
    if (part.equals(edPart)) {
      if (selection instanceof IStructuredSelection) {
        currentSelection = (IStructuredSelection) selection;
        if (!selection.isEmpty() && (currentSelection.getFirstElement() instanceof Element)) {
          if (isLinkedWithEditor() && !currentSelection.getFirstElement().equals(tv.getInput())) {
            tv.setInput(currentSelection.getFirstElement());
          }
        }
      }
    }

  }

  private boolean isLinkedWithEditor() {
    ICommandService service = (ICommandService) PlatformUI.getWorkbench().getService(
        ICommandService.class);
    Command command = service.getCommand("org.eclipse.wst.xml.ui.cmnd.contentmodel.sych"); //$NON-NLS-1$
    State state = command.getState("org.eclipse.ui.commands.toggleState"); //$NON-NLS-1$

    return ((Boolean) state.getValue()).booleanValue();
  }

}
