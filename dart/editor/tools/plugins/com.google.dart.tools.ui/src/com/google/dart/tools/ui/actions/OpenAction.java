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

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.element.Element;
import com.google.dart.tools.internal.corext.refactoring.util.DartElementUtil;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IEditorStatusLine;

/**
 * This action opens a {@link DartEditor} with declaration of {@link Element}.
 */
public class OpenAction extends AbstractDartSelectionAction {
  /**
   * @return {@code true} if given {@link DartSelection} looks valid and we can try to open it.
   */
  private static boolean isValidSelection(DartSelection selection) {
    // if we are already on declaration, we don't need to open anything
    ASTNode node = getSelectionNode(selection);
    if (node instanceof SimpleIdentifier) {
      if (((SimpleIdentifier) node).inDeclarationContext()) {
        return false;
      }
    }
    // interesting elements
    return isInterestingElementSelected(selection);
  }

  private final DartEditor editor;

  public OpenAction(DartEditor editor) {
    super(editor);
    this.editor = editor;
  }

  @Override
  public void selectionChanged(DartSelection selection) {
    setEnabled(isValidSelection(selection));
  }

  @Override
  protected void doRun(DartSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    Element element = getSelectionElement(selection);
    openElement(element);
  }

  @Override
  protected void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    Element element = getSelectionElement(selection);
    openElement(element);
  }

  @Override
  protected void init() {
    setText(ActionMessages.OpenAction_declaration_label);
    setToolTipText(ActionMessages.OpenAction_tooltip);
    setDescription(ActionMessages.OpenAction_description);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, DartHelpContextIds.OPEN_ACTION);
  }

  private void openElement(Element element) {
    // no element - beep
    if (element == null) {
      IEditorStatusLine statusLine = (IEditorStatusLine) editor.getAdapter(IEditorStatusLine.class);
      if (statusLine != null) {
        statusLine.setMessage(true, ActionMessages.OpenAction_error_messageBadSelection, null);
      }
      getShell().getDisplay().beep();
      return;
    }
    // tweak
    element = DartElementUtil.getVariableIfSyntheticAccessor(element);
    // do open
    try {
      DartUI.openInEditor(element);
    } catch (Throwable e) {
      ExceptionHandler.handle(e, getText(), "Exception during open.");
    }
  }
}
