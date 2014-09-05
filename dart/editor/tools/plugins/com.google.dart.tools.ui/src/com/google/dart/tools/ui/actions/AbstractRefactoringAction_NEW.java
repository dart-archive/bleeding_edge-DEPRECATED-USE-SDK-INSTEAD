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

import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

/**
 * Abstract refactoring action.
 */
public abstract class AbstractRefactoringAction_NEW extends AbstractDartSelectionAction_NEW {
  public AbstractRefactoringAction_NEW(DartEditor editor) {
    super(editor);
    setEnabled(SelectionConverter.canOperateOn(editor));
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

  protected void showError(String title, Throwable e) {
    ExceptionHandler.handle(
        e,
        title,
        "Unexpected exception occurred. See the error log for more details.");
  }
}
