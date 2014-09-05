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

import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.ui.IWorkbenchSite;

/**
 * Abstract refactoring action.
 */
public abstract class AbstractRefactoringAction_OLD extends AbstractDartSelectionAction_OLD {
  public AbstractRefactoringAction_OLD(DartEditor editor) {
    super(editor);
    setEnabled(SelectionConverter.canOperateOn(editor));
  }

  public AbstractRefactoringAction_OLD(IWorkbenchSite site) {
    super(site);
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
