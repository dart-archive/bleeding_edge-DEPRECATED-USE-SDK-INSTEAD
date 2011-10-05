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
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.SpecificContentAssistExecutor;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * 
 * 
 */
public final class DartContentAssistHandler extends AbstractHandler {
  private final SpecificContentAssistExecutor fExecutor = new SpecificContentAssistExecutor(
      CompletionProposalComputerRegistry.getDefault());

  public DartContentAssistHandler() {
  }

  /*
   * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands. ExecutionEvent)
   */
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    ITextEditor editor = getActiveEditor();
    if (editor == null) {
      return null;
    }

    String categoryId = event.getParameter("com.google.dart.tools.ui.specific_content_assist.category_id"); //$NON-NLS-1$
    if (categoryId == null) {
      return null;
    }

    fExecutor.invokeContentAssist(editor, categoryId);

    return null;
  }

  private ITextEditor getActiveEditor() {
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (window != null) {
      IWorkbenchPage page = window.getActivePage();
      if (page != null) {
        IEditorPart editor = page.getActiveEditor();
        if (editor instanceof ITextEditor) {
          return (DartEditor) editor;
        }
      }
    }
    return null;
  }

}
