/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.editor.selectionactions;

import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.ui.internal.text.IJavaHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IUpdate;

public class StructureSelectHistoryAction extends Action implements IUpdate {
  private DartEditor fEditor;
  private SelectionHistory fHistory;

  public StructureSelectHistoryAction(DartEditor editor, SelectionHistory history) {
    super(SelectionActionMessages.StructureSelectHistory_label);
    setToolTipText(SelectionActionMessages.StructureSelectHistory_tooltip);
    setDescription(SelectionActionMessages.StructureSelectHistory_description);
    Assert.isNotNull(history);
    Assert.isNotNull(editor);
    fHistory = history;
    fEditor = editor;
    update();
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
        IJavaHelpContextIds.STRUCTURED_SELECTION_HISTORY_ACTION);
  }

  @Override
  public void run() {
    SourceRange old = fHistory.getLast();
    if (old != null) {
      try {
        fHistory.ignoreSelectionChanges();
        fEditor.selectAndReveal(old.getOffset(), old.getLength());
      } finally {
        fHistory.listenToSelectionChanges();
      }
    }
  }

  @Override
  public void update() {
    setEnabled(!fHistory.isEmpty());
  }
}
