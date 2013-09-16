/**
 * Copyright (c) 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Common Public License v1.0 which
 * accompanies this distribution, and is available at http://www.eclipse.org/legal/cpl-v10.html
 * Contributors: IBM - Initial API and implementation
 */
package org.eclipse.wst.common.ui.internal.dialogs;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.wst.common.ui.internal.Messages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A generic save files dialog. The bulk of the code for this dialog was taken from the JDT
 * refactoring support in org.eclipse.jdt.internal.ui.refactoring.RefactoringSaveHelper. This class
 * is a good candidate for reuse amoung components.
 */
public class SaveDirtyFilesDialog extends ListDialog {
  public static boolean saveDirtyFiles() {
    boolean result = true;
    // TODO (cs) add support for save automatically
    Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    IEditorPart[] dirtyEditors = getDirtyEditors();
    if (dirtyEditors.length > 0) {
      result = false;
      SaveDirtyFilesDialog saveDirtyFilesDialog = new SaveDirtyFilesDialog(shell);
      saveDirtyFilesDialog.setInput(Arrays.asList(dirtyEditors));
      // Save all open editors.
      if (saveDirtyFilesDialog.open() == Window.OK) {
        result = true;
        int numDirtyEditors = dirtyEditors.length;
        for (int i = 0; i < numDirtyEditors; i++) {
          dirtyEditors[i].doSave(null);
        }
      }
    }
    return result;
  }

  private static IEditorPart[] getDirtyEditors() {
    List result = new ArrayList(0);
    IWorkbench workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
    for (int i = 0; i < windows.length; i++) {
      IWorkbenchPage[] pages = windows[i].getPages();
      for (int x = 0; x < pages.length; x++) {
        IEditorPart[] editors = pages[x].getDirtyEditors();
        for (int z = 0; z < editors.length; z++) {
          IEditorPart ep = editors[z];
          result.add(ep);
        }
      }
    }
    return (IEditorPart[]) result.toArray(new IEditorPart[result.size()]);
  }

  public SaveDirtyFilesDialog(Shell parent) {
    super(parent);
    setTitle(Messages.SaveFilesDialog_save_all_resources);
    setAddCancelButton(true);
    setLabelProvider(createDialogLabelProvider());
    setMessage(Messages.SaveFilesDialog_must_save);
    setContentProvider(new ListContentProvider());
  }

  protected Control createDialogArea(Composite container) {
    Composite result = (Composite) super.createDialogArea(container);
    // TODO... provide preference that supports 'always save'
    return result;
  }

  private ILabelProvider createDialogLabelProvider() {
    return new LabelProvider() {
      public Image getImage(Object element) {
        return ((IEditorPart) element).getTitleImage();
      }

      public String getText(Object element) {
        return ((IEditorPart) element).getTitle();
      }
    };
  }

  /**
   * A specialized content provider to show a list of editor parts. This class has been copied from
   * org.eclipse.jdt.internal.ui.viewsupport.ListContentProvider This class should be removed once a
   * generic solution is made available.
   */
  private static class ListContentProvider implements IStructuredContentProvider {
    List fContents;

    public ListContentProvider() {
    }

    public Object[] getElements(Object input) {
      if (fContents != null && fContents == input)
        return fContents.toArray();
      return new Object[0];
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      if (newInput instanceof List)
        fContents = (List) newInput;
      else
        fContents = null;
      // we use a fixed set.
    }

    public void dispose() {
    }

    public boolean isDeleted(Object o) {
      return fContents != null && !fContents.contains(o);
    }
  }
}
