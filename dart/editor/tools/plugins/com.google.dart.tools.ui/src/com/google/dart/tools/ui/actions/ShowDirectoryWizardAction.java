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
import com.google.dart.tools.ui.wizard.ShowDirectoryWizard;
import com.google.dart.tools.ui.wizard.ShowDirectoryWizardPage;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.INewWizard;

/**
 * Action that opens the {@link ShowDirectoryWizard}.
 * 
 * @see ShowDirectoryWizard
 * @see ShowDirectoryWizardPage
 * @see HideDirectoryAction
 * @see FilesView
 */
public class ShowDirectoryWizardAction extends AbstractOpenWizardAction implements
    ISelectionChangedListener {

  private static final String ACTION_ID = "com.google.dart.tools.ui.directory.show";

  /**
   * Creates an instance of the {@link ShowDirectoryWizardAction}.
   */
  public ShowDirectoryWizardAction() {
    setText(ActionMessages.ShowDirectoryWizardAction_text);
    setDescription(ActionMessages.ShowDirectoryWizardAction_description);
    setToolTipText(ActionMessages.ShowDirectoryWizardAction_tooltip);
//    setImageDescriptor(DartToolsPlugin.getImageDescriptor("icons/full/dart16/library_new.png"));
    setId(ACTION_ID);
  }

  /**
   * Do nothing on each selection change, this action is always enabled.
   */
  @Override
  public void selectionChanged(SelectionChangedEvent event) {
  }

  @Override
  protected final INewWizard createWizard() throws CoreException {
    return new ShowDirectoryWizard();
  }

}
