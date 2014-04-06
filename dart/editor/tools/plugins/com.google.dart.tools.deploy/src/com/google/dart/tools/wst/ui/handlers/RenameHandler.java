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

package com.google.dart.tools.wst.ui.handlers;

import com.google.dart.engine.element.Element;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.tools.internal.corext.refactoring.RefactoringExecutionStarter;
import com.google.dart.tools.wst.ui.HtmlReconcilerHook;
import com.google.dart.tools.wst.ui.HtmlReconcilerManager;
import com.google.dart.tools.wst.ui.hyperlink.ElementHyperlinkDetector;
import com.google.dart.tools.wst.ui.hyperlink.ElementRegion;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.sse.ui.internal.StructuredTextViewer;

/**
 * Rename refactoring handler for HTML editor.
 */
public class RenameHandler extends AbstractHandler {
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
    ITextEditor editor = (ITextEditor) HandlerUtil.getActiveEditor(event);
    executeWith(window, editor);
    return null;
  }

  private void executeWith(IWorkbenchWindow window, ITextEditor editor) {
    if (window == null) {
      return;
    }
    if (!(editor instanceof StructuredTextEditor)) {
      return;
    }
    // prepare HtmlUnit
    StructuredTextEditor structuredEditor = (StructuredTextEditor) editor;
    StructuredTextViewer textViewer = structuredEditor.getTextViewer();
    HtmlUnit htmlUnit = getHtmlUnit(textViewer);
    // find Element
    int offset = textViewer.getSelectedRange().x;
    ElementRegion elementRegion = ElementHyperlinkDetector.getElementRegion(htmlUnit, offset);
    // start rename refactoring
    if (elementRegion != null) {
      Element element = elementRegion.element;
      RefactoringExecutionStarter.startRenameRefactoring(element);
    }
  }

  private HtmlUnit getHtmlUnit(ITextViewer textViewer) {
    IDocument document = textViewer.getDocument();
    HtmlReconcilerHook reconciler = HtmlReconcilerManager.getInstance().reconcilerFor(document);
    return reconciler.getResolvedUnit();
  }
}
