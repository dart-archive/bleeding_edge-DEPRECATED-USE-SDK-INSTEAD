/*******************************************************************************
 * Copyright (c) 2001, 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.properties;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;
import org.eclipse.wst.sse.ui.internal.ShowViewAction;
import org.eclipse.wst.sse.ui.internal.editor.EditorPluginImageHelper;
import org.eclipse.wst.sse.ui.internal.editor.EditorPluginImages;
import org.eclipse.wst.sse.ui.internal.editor.IHelpContextIds;

/**
 * Surfaces the Properties view
 */
public class ShowPropertiesAction extends ShowViewAction {
  private final static String VIEW_ID = "org.eclipse.ui.views.PropertySheet"; //$NON-NLS-1$
  private ISelectionProvider fSelectionProvider;
  private IWorkbenchPart fPart;

  public ShowPropertiesAction() {
    super(
        SSEUIMessages.ShowPropertiesAction_0,
        EditorPluginImageHelper.getInstance().getImageDescriptor(EditorPluginImages.IMG_OBJ_PROP_PS)); //$NON-NLS-1$
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
        IHelpContextIds.CONTMNU_PROPERTIES_HELPID);
  }

  public ShowPropertiesAction(IWorkbenchPart part, ISelectionProvider provider) {
    this();
    fSelectionProvider = provider;
    fPart = part;
  }

  protected String getViewID() {
    return VIEW_ID;
  }

  public void run() {
    super.run();

    if (fSelectionProvider != null) {
      IWorkbenchWindow window = SSEUIPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
      IWorkbenchPage page = window.getActivePage();
      if (page != null) {
        IViewPart findView = page.findView(getViewID());
        if (findView instanceof ISelectionListener && fPart != null) {
          ((ISelectionListener) findView).selectionChanged(fPart, fSelectionProvider.getSelection());
        } else {
          findView.getViewSite().getSelectionProvider().setSelection(
              fSelectionProvider.getSelection());
        }
      }
    }
  }
}
