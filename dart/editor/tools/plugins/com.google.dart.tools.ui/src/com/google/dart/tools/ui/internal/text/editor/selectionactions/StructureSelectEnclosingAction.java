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

import com.google.dart.compiler.ast.DartNode;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.SourceReference;
import com.google.dart.tools.ui.internal.text.IJavaHelpContextIds;
import com.google.dart.tools.ui.internal.text.SelectionAnalyzer;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.ui.PlatformUI;

public class StructureSelectEnclosingAction extends StructureSelectionAction {

  /*
   * This constructor is for testing purpose only.
   */
  public StructureSelectEnclosingAction() {
  }

  public StructureSelectEnclosingAction(DartEditor editor, SelectionHistory history) {
    super(SelectionActionMessages.StructureSelectEnclosing_label, editor, history);
    setToolTipText(SelectionActionMessages.StructureSelectEnclosing_tooltip);
    setDescription(SelectionActionMessages.StructureSelectEnclosing_description);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
        IJavaHelpContextIds.STRUCTURED_SELECT_ENCLOSING_ACTION);
  }

  /*
   * @see StructureSelectionAction#internalGetNewSelectionRange(SourceRange, CompilationUnit,
   * SelectionAnalyzer)
   */
  @Override
  SourceRange internalGetNewSelectionRange(SourceRange oldSourceRange, SourceReference sr,
      SelectionAnalyzer selAnalyzer) throws DartModelException {
    DartNode first = selAnalyzer.getFirstSelectedNode();
    if (first == null || first.getParent() == null) {
      return getLastCoveringNodeRange(oldSourceRange, sr, selAnalyzer);
    }

    return getSelectedNodeSourceRange(sr, first.getParent());
  }
}
