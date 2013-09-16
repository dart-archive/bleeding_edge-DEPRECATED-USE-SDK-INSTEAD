/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSModel;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSNode;
import org.eclipse.wst.css.core.internal.util.CSSPathService;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;

/**
 */
public class CSSTextPropertyDescriptor extends TextPropertyDescriptor {
  private final ICSSNode fNode;

  /**
   * CSSTextPropertyDescriptor constructor comment.
   * 
   * @param id java.lang.String
   * @param displayName java.lang.String
   */
  public CSSTextPropertyDescriptor(String id, String displayName, ICSSNode node) {
    super(id, displayName);
    this.fNode = node;
  }

  public CSSTextPropertyDescriptor(String id, String displayName, ICSSNode node, String category) {
    super(id, displayName);
    this.fNode = node;
    setCategory(category);
  }

  /**
   * @return org.eclipse.jface.viewers.CellEditor
   * @param parent org.eclipse.swt.widgets.Composite
   */
  public CellEditor createPropertyEditor(Composite parent) {
    ICSSModel model = fNode.getOwnerDocument().getModel();
    if (model == null)
      return null;
    if (model.getStyleSheetType() == ICSSModel.EXTERNAL && findEditor(model) == null)
      return null;
    // check whether IFile is readonly to prohibit editing before
    // validateEdit()
    IStructuredModel structuredModel = model;
    if (model.getStyleSheetType() != ICSSModel.EXTERNAL) {
      structuredModel = ((IDOMNode) model.getOwnerDOMNode()).getModel();
      if (structuredModel == null)
        return null;
    }
    IFile file = CSSPathService.location2File(structuredModel.getBaseLocation());
    if (file == null || file.isReadOnly())
      return null;

    return super.createPropertyEditor(parent);
  }

  private static IEditorPart findEditor(ICSSModel model) {
    IWorkbench workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
    for (int i = 0; i < windows.length; i++) {
      IWorkbenchPage[] pages = windows[i].getPages();
      for (int j = 0; j < pages.length; j++) {
        IEditorReference[] editors = pages[j].getEditorReferences();
        for (int k = 0; k < editors.length; k++) {
          IEditorPart editPart = editors[k].getEditor(false);
          if (editPart != null) {
            IEditorInput editorInput = editPart.getEditorInput();
            if (editorInput instanceof IFileEditorInput) {
              IFile file = ((IFileEditorInput) editorInput).getFile();
              if (file != null) {
                //TODO Urgent needs to be fixed
                // I think we need 'equals' (or 'equivalent'
                // on model) for cases like this
                if (StructuredModelManager.getModelManager().calculateId(file).equals(model.getId())) {
                  return editPart;
                }
              }
            }
          }
        }
      }
    }
    return null;
  }
}
