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

import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.refactoring.actions.RenameDartElementAction_OLD;
import com.google.dart.tools.ui.internal.refactoring.actions.RenameResourceAction;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

/**
 * Renames a Dart element or workbench resource.
 */
public class RenameAction_OLD extends InstrumentedSelectionDispatchAction {
  private RenameDartElementAction_OLD fRenameDartElement;
  private final RenameResourceAction fRenameResource;

  public RenameAction_OLD(DartEditor editor) {
    this(editor.getEditorSite());
    fRenameDartElement = new RenameDartElementAction_OLD(editor);
  }

  public RenameAction_OLD(IWorkbenchSite site) {
    super(site);
    setText(RefactoringMessages.RenameAction_text);
    fRenameDartElement = new RenameDartElementAction_OLD(site);
    fRenameDartElement.setText(getText());
    fRenameResource = new RenameResourceAction(site);
    fRenameResource.setText(getText());
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, DartHelpContextIds.RENAME_ACTION);
  }

  @Override
  public void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    boolean renameRan = false;

    if (fRenameDartElement.isEnabled()) {
      fRenameDartElement.doRun(selection, event, instrumentation);
      renameRan = true;
      instrumentation.metric("Rename Ran", "DartElement");
    }

    if (fRenameResource != null && fRenameResource.isEnabled()) {
      fRenameResource.doRun(selection, event, instrumentation);
      renameRan = true;
      instrumentation.metric("Rename Ran", "Resource");
    }

    if (!renameRan) {
      instrumentation.metric("Problem", "Rename didn't run");
    }
  }

  @Override
  public void doRun(ITextSelection selection, Event event, UIInstrumentationBuilder instrumentation) {
    if (fRenameDartElement.isEnabled()) {
      fRenameDartElement.doRun(selection, event, instrumentation);
    } else {
      instrumentation.metric("Problem", "RenameDartElement not enabled");
    }

  }

  @Override
  public void selectionChanged(SelectionChangedEvent event) {
    fRenameDartElement.selectionChanged(event);
    if (fRenameResource != null) {
      fRenameResource.selectionChanged(event);
    }
    setEnabled(computeEnabledState());
  }

  @Override
  public void update(ISelection selection) {
    fRenameDartElement.update(selection);

    if (fRenameResource != null) {
      fRenameResource.update(selection);
    }

    setEnabled(computeEnabledState());
  }

  private boolean computeEnabledState() {
    if (fRenameResource != null) {
      return fRenameDartElement.isEnabled() || fRenameResource.isEnabled();
    } else {
      return fRenameDartElement.isEnabled();
    }
  }
}
