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
package com.google.dart.tools.ui.actions;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.directoryset.DirectorySetManager;
import com.google.dart.tools.ui.internal.view.files.FilesView;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.IWorkbenchSite;

import java.io.File;

/**
 * Action that opens the new application wizard.
 * 
 * @see ShowDirectoryWizardAction
 * @see FilesView
 */
public class HideDirectoryAction extends SelectionDispatchAction implements
    ISelectionChangedListener {

  private static final String ACTION_ID = "com.google.dart.tools.ui.directory.hide";

  /**
   * Creates an instance of the <code>HideDirectoryAction</code>.
   */
  public HideDirectoryAction(IWorkbenchSite site) {
    super(site);
    setText(ActionMessages.HideDirectoryAction_text);
    setDescription(ActionMessages.HideDirectoryAction_description);
    setToolTipText(ActionMessages.HideDirectoryAction_tooltip);
//    setImageDescriptor(DartToolsPlugin.getImageDescriptor("icons/full/dart16/library_new.png"));
    setId(ACTION_ID);
    setEnabled(false);
  }

  @Override
  public void run(IStructuredSelection selection) {
    if (isEnabled()) {
      String pathToRemove = ((File) (selection.toArray()[0])).getAbsolutePath();
      DartCore.getDirectorySetManager().removePath(pathToRemove);
    }
  }

  /**
   * On each selection changed event, call {@link #setEnabled(boolean)} to <code>true</code> iff
   * there is one element selected that is a top level directory in the Files view.
   * 
   * @see DirectorySetManager
   */
  @Override
  public void selectionChanged(SelectionChangedEvent event) {
    // if the selection is a structured selection (aka, from the Files view)
    if (event.getSelection() instanceof IStructuredSelection) {
      IStructuredSelection selection = (IStructuredSelection) event.getSelection();
      // if there is one element selected
      if (selection.size() == 1) {
        Object firstElt = selection.getFirstElement();
        // if that element is a java.io.File element
        if (firstElt instanceof File) {
          File file = (File) firstElt;
          setEnabled(file.isDirectory()
              && DartCore.getDirectorySetManager().hasPath(file.getAbsolutePath()));
          return;
        }
      }
    }
    setEnabled(false);
  }
}
