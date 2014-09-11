/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.ui.actions;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

import java.io.IOException;

/**
 * {@link Action} to open folder with selected {@link IResource} and reveal it.
 */
public class ShowInFinderAction extends InstrumentedSelectionDispatchAction {
  public static final String ID = "com.google.dart.tools.ui.file.showInFinder";

  private static ShowInFinderAction INSTANCE;

  public static ShowInFinderAction getInstance(IWorkbenchWindow window) {
    if (INSTANCE == null) {
      Assert.isNotNull(window);
      INSTANCE = new ShowInFinderAction(window);
    }
    return INSTANCE;
  }

  private final IWorkbenchWindow window;

  private ShowInFinderAction(IWorkbenchWindow window) {
    super(window);
    this.window = window;
    setId(ID);
    setActionDefinitionId(ID);
    if (SystemUtils.IS_OS_LINUX) {
      setText("Show in File Manager");
    }
    if (SystemUtils.IS_OS_MAC) {
      setText("Show in Finder");
    }
    if (SystemUtils.IS_OS_WINDOWS) {
      setText("Show in Explorer");
    }
  }

  @Override
  public ISelection getSelection() {
    IWorkbenchPage page = window.getActivePage();
    IWorkbenchPart activePart = page.getActivePart();
    // editor is active
    if (activePart instanceof IEditorPart) {
      IEditorPart editorPart = (IEditorPart) activePart;
      IEditorInput editorInput = editorPart.getEditorInput();
      if (editorInput instanceof IFileEditorInput) {
        IFileEditorInput fileEditorInput = (IFileEditorInput) editorInput;
        IFile file = fileEditorInput.getFile();
        return new StructuredSelection(file);
      }
    }
    // view is active
    return window.getSelectionService().getSelection();
  }

  @Override
  public void selectionChanged(DartSelection selection) {
    super.selectionChanged(selection);
  }

  public void updateEnablement() {

    boolean enabled = false;

    ISelection selection = getSelection();
    if (selection instanceof StructuredSelection) {
      StructuredSelection structuredSelection = (StructuredSelection) selection;
      if (!structuredSelection.isEmpty()
          && structuredSelection.getFirstElement() instanceof IResource) {
        enabled = true;
      }
    }

    setEnabled(enabled);
  }

  @Override
  protected void doRun(Event event, UIInstrumentationBuilder instrumentation) {
    ISelection selection = getSelection();
    if (selection instanceof StructuredSelection) {
      StructuredSelection structuredSelection = (StructuredSelection) selection;
      if (structuredSelection.isEmpty()) {
        return;
      }
      Object element = structuredSelection.getFirstElement();
      if (!(element instanceof IResource)) {
        return;
      }
      IResource resource = (IResource) element;
      instrumentation.data("path", resource.getLocation().toOSString());
      String path = resource.getLocation().toOSString();
      try {
        if (SystemUtils.IS_OS_LINUX) {
          try {
            new ProcessBuilder("/usr/bin/nautilus", path).start();
          } catch (IOException e) {
            // no Nautilus, try generic xdg-open
            if (resource instanceof IFile) {
              path = resource.getParent().getLocation().toOSString();
            }
            new ProcessBuilder("/usr/bin/xdg-open", path).start();
          }
          return;
        }
        if (SystemUtils.IS_OS_MAC) {
          new ProcessBuilder("/usr/bin/open", "-R", path).start();
          return;
        }
        if (SystemUtils.IS_OS_WINDOWS) {
          new ProcessBuilder("Explorer.exe", "/select,", path).start();
          return;
        }
      } catch (IOException e) {
        instrumentation.record(e);
        ErrorDialog.openError(
            window.getShell(),
            null,
            null,
            DartToolsPlugin.createErrorStatus("Cannot " + getText(), e));
      }
    }
  }
}
