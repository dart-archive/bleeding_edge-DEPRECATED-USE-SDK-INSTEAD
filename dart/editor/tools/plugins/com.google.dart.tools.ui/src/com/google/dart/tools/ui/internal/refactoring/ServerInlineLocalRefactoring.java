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

package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.server.generated.types.InlineLocalVariableFeedback;
import com.google.dart.server.generated.types.RefactoringFeedback;
import com.google.dart.server.generated.types.RefactoringKind;
import com.google.dart.server.generated.types.RefactoringOptions;

/**
 * LTK wrapper around Analysis Server 'Inline Local' refactoring.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class ServerInlineLocalRefactoring extends ServerRefactoring {
  private String variableName;
  private int occurrences;

  public ServerInlineLocalRefactoring(String file, int offset, int length) {
    super(RefactoringKind.INLINE_LOCAL_VARIABLE, "Inline Local", file, offset, length);
  }

  public int getOccurrences() {
    return occurrences;
  }

  public String getVariableName() {
    return variableName;
  }

  @Override
  protected RefactoringOptions getOptions() {
    return null;
  }

  @Override
  protected void setFeedback(RefactoringFeedback _feedback) {
    InlineLocalVariableFeedback feedback = (InlineLocalVariableFeedback) _feedback;
    variableName = feedback.getName();
    occurrences = feedback.getOccurrences();
  }
}
