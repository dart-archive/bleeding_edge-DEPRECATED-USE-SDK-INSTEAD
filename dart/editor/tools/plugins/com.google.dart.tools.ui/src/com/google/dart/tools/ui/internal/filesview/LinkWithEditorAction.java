/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.filesview;

import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.ActionMessages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.PartEventAction;
import org.eclipse.ui.ide.FileStoreEditorInput;

import java.net.URI;

/**
 * Action to link Files view contents with the editor.
 */
public class LinkWithEditorAction extends PartEventAction implements ISelectionChangedListener {

  private IWorkbenchPage page;
  private TreeViewer treeViewer;

  public LinkWithEditorAction(IWorkbenchPage page, TreeViewer treeViewer) {
    super(ActionMessages.ToggleLinkingAction_label, IAction.AS_CHECK_BOX);

    this.page = page;
    this.treeViewer = treeViewer;

    setDescription(ActionMessages.ToggleLinkingAction_description);
    setToolTipText(ActionMessages.ToggleLinkingAction_tooltip);
    DartPluginImages.setLocalImageDescriptors(this, "synced.gif"); //$NON-NLS-1$    
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, DartHelpContextIds.LINK_EDITOR_ACTION);

    treeViewer.addSelectionChangedListener(this);
    page.addPartListener(this);
  }

  public void dispose() {
    page.removePartListener(this);
  }

  public boolean getLinkWithEditor() {
    return isChecked();
  }

  @Override
  public void partActivated(IWorkbenchPart part) {
    super.partActivated(part);

    if (!getLinkWithEditor()) {
      return;
    }

    if (part instanceof IEditorPart) {
      IEditorInput input = ((IEditorPart) part).getEditorInput();

      if (input instanceof IFileEditorInput) {
        IFile file = ((IFileEditorInput) input).getFile();

        syncSelectionToEditor(file);
      } else if (input instanceof FileStoreEditorInput) {
        URI uri = ((FileStoreEditorInput) input).getURI();

        try {
          syncSelectionToEditor(EFS.getStore(uri));
        } catch (CoreException e) {

        }
      }
    }
  }

  @Override
  public void run() {
    if (getLinkWithEditor()) {
      syncSelectionToEditor();
    }
  }

  @Override
  public void selectionChanged(SelectionChangedEvent event) {
    if (event.getSelection() instanceof IStructuredSelection) {
      IStructuredSelection sel = (IStructuredSelection) event.getSelection();

      syncEditorToSelection(sel.getFirstElement());
    }
  }

  public void setLinkWithEditor(boolean value) {
    setChecked(value);
  }

  public void syncSelectionToEditor() {
    if (getLinkWithEditor()) {
      IEditorPart part = page.getActiveEditor();

      if (part != null) {
        partActivated(part);
      }
    }
  }

  protected void syncEditorToSelection(Object element) {
    if (!getLinkWithEditor()) {
      return;
    }

    try {
      if (element instanceof IFile) {
        IFile file = (IFile) element;

        for (IEditorReference editor : page.getEditorReferences()) {
          IEditorInput input = editor.getEditorInput();

          if (input instanceof IFileEditorInput) {
            if (file.equals(((IFileEditorInput) input).getFile())) {
              page.bringToTop(editor.getPart(true));
            }
          }
        }
      } else if (element instanceof IFileStore) {
        IFileStore file = (IFileStore) element;

        for (IEditorReference editor : page.getEditorReferences()) {
          IEditorInput input = editor.getEditorInput();

          if (input instanceof FileStoreEditorInput) {
            if (file.toURI().equals(((FileStoreEditorInput) input).getURI())) {
              page.bringToTop(editor.getPart(true));
            }
          }
        }
      }
    } catch (PartInitException exception) {
      DartToolsPlugin.log(exception);
    }
  }

  protected void syncSelectionToEditor(Object file) {
    Object currentSelection = getCurrentSelection();

    if (file != null && !file.equals(currentSelection)) {
      treeViewer.setSelection(new StructuredSelection(file), true);
    }
  }

  private Object getCurrentSelection() {
    ISelection sel = treeViewer.getSelection();

    if (sel instanceof IStructuredSelection) {
      return ((IStructuredSelection) sel).getFirstElement();
    } else {
      return null;
    }
  }

}
