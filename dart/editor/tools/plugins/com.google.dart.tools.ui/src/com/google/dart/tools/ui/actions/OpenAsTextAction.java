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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.SelectionListenerAction;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.IDE;

import java.util.List;

/**
 * Action opens the selected file in a text editor
 */
public class OpenAsTextAction extends SelectionListenerAction {

  private static final String ACTION_ID = "com.google.dart.tools.ui.actions.openAsText"; //$NON-NLS-1$
  private IWorkbenchPage page;

  public OpenAsTextAction(IWorkbenchPage page) {
    super(ActionMessages.OpenAsTextAction_title);
    setId(ACTION_ID);
    setToolTipText(ActionMessages.OpenAsTextAction_toolTip);
    setDescription(ActionMessages.OpenAsTextAction_description);
    this.page = page;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void run() {

    List<IResource> selectedResources = getSelectedResources();
    if (selectedResources.get(0) instanceof IFile) {
      IFile file = (IFile) selectedResources.get(0);
      try {
        IDE.openEditor(page, file, EditorsUI.DEFAULT_TEXT_EDITOR_ID);
      } catch (PartInitException e) {

      }

    }
  }

}
