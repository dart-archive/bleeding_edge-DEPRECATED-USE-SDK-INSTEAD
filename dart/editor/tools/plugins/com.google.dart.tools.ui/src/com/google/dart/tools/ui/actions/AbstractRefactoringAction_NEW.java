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

import com.google.dart.server.GetAvailableRefactoringsConsumer;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.viewers.SelectionChangedEvent;

import java.util.List;

/**
 * Abstract refactoring action.
 */
public abstract class AbstractRefactoringAction_NEW extends AbstractDartSelectionAction_NEW {
  private final String kind;

  public AbstractRefactoringAction_NEW(DartEditor editor, String kind) {
    super(editor);
    this.kind = kind;
    setEnabled(SelectionConverter.canOperateOn(editor));
  }

  @Override
  public void selectionChanged(SelectionChangedEvent event) {
    super.selectionChanged(event);
    setEnabled(false);
    if (file != null) {
      DartCore.getAnalysisServer().edit_getAvailableRefactorings(
          file,
          selectionOffset,
          selectionLength,
          new GetAvailableRefactoringsConsumer() {
            @Override
            public void computedRefactoringKinds(List<String> refactoringKinds) {
              boolean res = refactoringKinds.contains(kind);
              setEnabled(res);
            }
          });
    }
  }

  protected boolean canOperateOn() {
    if (editor == null) {
      return false;
    }
    if (!editor.isEditable()) {
      return false;
    }
    return true;
  }
}
