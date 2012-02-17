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

import com.google.dart.tools.ui.internal.view.files.FilesView;
import com.google.dart.tools.ui.wizard.NewDirectoryWizard;
import com.google.dart.tools.ui.wizard.NewDirectoryWizardPage;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.INewWizard;

import java.io.File;

/**
 * Action that opens the {@link NewDirectoryWizard}.
 * 
 * @see NewDirectoryWizard
 * @see NewDirectoryWizardPage
 * @see FilesView
 */
public class NewDirectoryWizardAction extends AbstractOpenWizardAction implements
    ISelectionChangedListener {

  private static final String ACTION_ID = "com.google.dart.tools.ui.directory.new";

  private File directoryFile = null;

  /**
   * Creates an instance of the {@link NewDirectoryWizardAction}.
   */
  public NewDirectoryWizardAction() {
    setText(ActionMessages.NewDirectoryWizardAction_text);
    setDescription(ActionMessages.NewDirectoryWizardAction_description);
    setToolTipText(ActionMessages.NewDirectoryWizardAction_tooltip);
//    setImageDescriptor(DartToolsPlugin.getImageDescriptor("icons/full/dart16/library_new.png"));
    setId(ACTION_ID);
  }

  /**
   * On each selection changed event, call {@link #setEnabled(boolean)} to <code>true</code> iff
   * there is one element selected that is java.io.File that returns <code>true</code> to
   * {@link File#isDirectory()}.
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
          if (file.isDirectory()) {
            directoryFile = file;
            setEnabled(true);
            return;
          }
        }
      }
    }
    setEnabled(false);
  }

  @Override
  protected final INewWizard createWizard() throws CoreException {
    return new NewDirectoryWizard(directoryFile);
  }

}
