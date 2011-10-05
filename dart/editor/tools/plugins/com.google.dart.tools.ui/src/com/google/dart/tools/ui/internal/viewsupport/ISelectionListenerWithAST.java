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
package com.google.dart.tools.ui.internal.viewsupport;

import com.google.dart.compiler.ast.DartUnit;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorPart;

/**
 * Listener to be informed on text selection changes in an editor (post selection), including the
 * corresponding AST. The AST is shared and must not be modified. Listeners can be registered in a
 * <code>SelectionListenerWithASTManager</code>.
 */
public interface ISelectionListenerWithAST {

  /**
   * Called when a selection has changed. The method is called in a post selection event in an
   * background thread.
   * 
   * @param part The editor part in which the selection change has occurred.
   * @param selection The new text selection
   * @param astRoot The AST tree corresponding to the editor's input. This AST is shared and must
   *          not be modified.
   */
  void selectionChanged(IEditorPart part, ITextSelection selection, DartUnit astRoot);

}
