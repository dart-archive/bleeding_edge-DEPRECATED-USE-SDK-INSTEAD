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

package com.google.dart.tools.debug.ui.internal.presentation;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;

/**
 * An EditorPart implementation used to display information about a debugger element that does not
 * have source associated with it.
 * <p>
 * This is roughly modeled after the CommonSourceNotFoundEditor editor.
 */
public class DartSourceNotFoundEditor extends EditorPart implements ILaunchesListener2 {
  public static final String EDITOR_ID = "com.google.dart.tools.debug.ui.internal.presentation.DartSourceNotFoundEditor";

  public DartSourceNotFoundEditor() {

  }

  @Override
  public void createPartControl(Composite parent) {
    FormToolkit toolkit = new FormToolkit(parent.getDisplay());
    Form form = toolkit.createForm(parent);
    form.setText("Source not available");
    toolkit.decorateFormHeading(form);
    GridLayoutFactory.fillDefaults().applyTo(form.getBody());

    String description = "Source not available for " + getDescription() + ".";
    Label label = toolkit.createLabel(form.getBody(), description);
    GridDataFactory.fillDefaults().indent(6, 6).applyTo(label);
  }

  @Override
  public void dispose() {
    if (DebugPlugin.getDefault() != null) {
      DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
    }

    super.dispose();
  }

  @Override
  public void doSave(IProgressMonitor monitor) {

  }

  @Override
  public void doSaveAs() {

  }

  @Override
  public void init(IEditorSite site, IEditorInput input) throws PartInitException {
    setSite(site);
    setInput(input);

    DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);

    setPartName(input.getName());
  }

  @Override
  public boolean isDirty() {
    return false;
  }

  @Override
  public boolean isSaveAsAllowed() {
    return false;
  }

  @Override
  public void launchesAdded(ILaunch[] launches) {

  }

  @Override
  public void launchesChanged(ILaunch[] launches) {

  }

  @Override
  public void launchesRemoved(ILaunch[] launches) {
    for (ILaunch launch : launches) {
      conditionallyRemove(launch);
    }
  }

  @Override
  public void launchesTerminated(ILaunch[] launches) {
    for (ILaunch launch : launches) {
      conditionallyRemove(launch);
    }
  }

  @Override
  public void setFocus() {

  }

  protected void closeEditor() {
    final IEditorPart editor = this;

    Display.getDefault().syncExec(new Runnable() {
      @Override
      public void run() {
        IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

        if (activeWorkbenchWindow != null) {
          IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();

          if (activePage != null) {
            activePage.closeEditor(editor, false);
          }
        }
      }
    });
  }

  protected String getDescription() {
    if (getEditorInput() instanceof DartSourceNotFoundEditorInput) {
      DartSourceNotFoundEditorInput input = (DartSourceNotFoundEditorInput) getEditorInput();

      return input.getDescription();
    } else {
      return null;
    }
  }

  protected ILaunch getLaunch() {
    if (getEditorInput() instanceof DartSourceNotFoundEditorInput) {
      DartSourceNotFoundEditorInput input = (DartSourceNotFoundEditorInput) getEditorInput();

      return input.getLaunch();
    } else {
      return null;
    }
  }

  private void conditionallyRemove(ILaunch launch) {
    if (getLaunch() != null && getLaunch().equals(launch)) {
      closeEditor();
    }
  }

}
