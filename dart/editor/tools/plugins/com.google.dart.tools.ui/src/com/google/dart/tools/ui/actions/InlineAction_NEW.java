/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * {@link Action} to inline local variable, method, function.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class InlineAction_NEW extends AbstractRefactoringAction_NEW {
  private final InlineLocalAction_NEW inlineLocal;
  private final InlineMethodAction_NEW inlineMethod;

  private IPropertyChangeListener enabledListener = new IPropertyChangeListener() {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (IAction.ENABLED.equals(event.getProperty())) {
        setEnabled(inlineLocal.isEnabled() || inlineMethod.isEnabled());
      }
    }
  };

  public InlineAction_NEW(DartEditor editor) {
    super(editor);
    inlineLocal = new InlineLocalAction_NEW(editor);
    inlineMethod = new InlineMethodAction_NEW(editor);
    inlineLocal.addPropertyChangeListener(enabledListener);
    inlineMethod.addPropertyChangeListener(enabledListener);
  }

  @Override
  public void run() {
    if (!waitReadyForRefactoring()) {
      return;
    }
    // try to inline
    Shell shell = getShell();
    if (inlineLocal.isEnabled()) {
      inlineLocal.run();
      return;
    }
    if (inlineMethod.isEnabled()) {
      inlineMethod.run();
      return;
    }
    // complain
    MessageDialogHelper.openInformation(
        shell,
        RefactoringMessages.InlineAction_dialog_title,
        RefactoringMessages.InlineAction_select);
  }

  @Override
  public void selectionChanged(ISelection selection) {
    super.selectionChanged(selection);
    inlineLocal.selectionChanged(selection);
    inlineMethod.selectionChanged(selection);
  }

  @Override
  protected void init() {
    setText(RefactoringMessages.InlineAction_Inline);
    {
      String id = DartEditorActionDefinitionIds.INLINE;
      setId(id);
      setActionDefinitionId(id);
    }
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, DartHelpContextIds.INLINE_ACTION);
  }
}
