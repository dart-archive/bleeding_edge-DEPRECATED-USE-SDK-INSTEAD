/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui.internal.text.editor.selectionactions;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.services.util.SelectionAnalyzer;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.ui.PlatformUI;

public class StructureSelectEnclosingAction extends StructureSelectionAction {

  public StructureSelectEnclosingAction(DartEditor editor, SelectionHistory history) {
    super(SelectionActionMessages.StructureSelectEnclosing_label, editor, history);
    setToolTipText(SelectionActionMessages.StructureSelectEnclosing_tooltip);
    setDescription(SelectionActionMessages.StructureSelectEnclosing_description);
    try {
      PlatformUI.getWorkbench().getHelpSystem().setHelp(
          this,
          DartHelpContextIds.STRUCTURED_SELECT_ENCLOSING_ACTION);
    } catch (IllegalStateException ex) {
      // ignore workbench-not-created error thrown during testing
    }
  }

  @Override
  SourceRange internalGetNewSelectionRange(SourceRange oldSourceRange, AstNode node,
      SelectionAnalyzer selAnalyzer) {
    AstNode first = selAnalyzer.getFirstSelectedNode();
    if (first == null || first.getParent() == null) {
      return getLastCoveringNodeRange(oldSourceRange, node, selAnalyzer);
    }
    return getSelectedNodeSourceRange(node, first.getParent());
  }
}
