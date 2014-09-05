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

import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.refactoring.actions.RenameDartElementAction_OLD;
import com.google.dart.tools.ui.internal.refactoring.actions.RenameResourceAction;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

/**
 * Renames a Dart element or workbench resource.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class RenameAction_OLD extends AbstractDartSelectionAction_OLD {
  private final RenameDartElementAction_OLD renameElement;
  private final RenameResourceAction renameResource;

  public RenameAction_OLD(DartEditor editor) {
    super(editor);
    renameElement = new RenameDartElementAction_OLD(editor);
    renameResource = new RenameResourceAction(editor.getSite());
  }

  public RenameAction_OLD(IWorkbenchSite site) {
    super(site);
    renameElement = new RenameDartElementAction_OLD(site);
    renameElement.setText(getText());
    renameResource = new RenameResourceAction(site);
    renameResource.setText(getText());
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, DartHelpContextIds.RENAME_ACTION);
  }

  @Override
  public void selectionChanged(SelectionChangedEvent event) {
    renameElement.selectionChanged(event);
    renameResource.selectionChanged(event);
    setEnabled(computeEnabledState());
  }

  @Override
  public void update(ISelection selection) {
    renameElement.update(selection);
    renameResource.update(selection);
    setEnabled(computeEnabledState());
  }

  @Override
  protected void doRun(Event event, UIInstrumentationBuilder instrumentation) {
    if (renameElement.isEnabled()) {
      renameElement.doRun(event, instrumentation);
      return;
    }
    if (renameResource.isEnabled()) {
      renameResource.doRun(event, instrumentation);
      return;
    }
  }

  @Override
  protected void init() {
    setText(RefactoringMessages.RenameAction_text);
  }

  private boolean computeEnabledState() {
    return renameElement.isEnabled() || renameResource.isEnabled();
  }
}
