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

import com.google.dart.server.generated.types.MoveFileOptions;
import com.google.dart.server.generated.types.RefactoringFeedback;
import com.google.dart.server.generated.types.RefactoringKind;
import com.google.dart.server.generated.types.RefactoringOptions;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * LTK wrapper around Analysis Server 'Move File' refactoring.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class ServerMoveFileRefactoring extends ServerRefactoring {
  private MoveFileOptions options = new MoveFileOptions(null);

  public ServerMoveFileRefactoring(String file) {
    super(RefactoringKind.MOVE_FILE, "Move File", file, 0, 0);
  }

  public RefactoringStatus setNewFile(String newFile) {
    options.setNewFile(newFile);
    if (!setOptions(true)) {
      return TIMEOUT_STATUS;
    }
    return optionsStatus;
  }

  @Override
  protected RefactoringOptions getOptions() {
    return options;
  }

  @Override
  protected void setFeedback(RefactoringFeedback _feedback) {
  }
}
