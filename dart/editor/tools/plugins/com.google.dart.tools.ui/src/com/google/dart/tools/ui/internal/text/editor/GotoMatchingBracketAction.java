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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.ui.internal.text.IJavaHelpContextIds;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;

public class GotoMatchingBracketAction extends Action {

  public final static String GOTO_MATCHING_BRACKET = "GotoMatchingBracket"; //$NON-NLS-1$

  private final DartEditor fEditor;

  public GotoMatchingBracketAction(DartEditor editor) {
    super(DartEditorMessages.GotoMatchingBracket_label);
    Assert.isNotNull(editor);
    fEditor = editor;
    setEnabled(true);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
        IJavaHelpContextIds.GOTO_MATCHING_BRACKET_ACTION);
  }

  @Override
  public void run() {
    fEditor.gotoMatchingBracket();
  }
}
