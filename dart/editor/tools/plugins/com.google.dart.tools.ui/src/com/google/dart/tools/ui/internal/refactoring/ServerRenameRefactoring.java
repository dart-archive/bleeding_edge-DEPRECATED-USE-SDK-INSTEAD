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

import com.google.dart.server.generated.types.RefactoringFeedback;
import com.google.dart.server.generated.types.RefactoringKind;
import com.google.dart.server.generated.types.RefactoringOptions;
import com.google.dart.server.generated.types.RenameFeedback;
import com.google.dart.server.generated.types.RenameOptions;

import org.apache.commons.lang3.text.WordUtils;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * LTK wrapper around Analysis Server 'Rename' refactoring.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class ServerRenameRefactoring extends ServerRefactoring {
  private RenameOptions options;
  private String elementKindName;
  private String oldName;

  public ServerRenameRefactoring(String file, int offset, int length) {
    super(RefactoringKind.RENAME, "Rename", file, offset, length);
  }

  public String getElementKindName() {
    return elementKindName;
  }

  public String getOldName() {
    return oldName;
  }

  public RefactoringStatus setNewName(String newName) {
    options.setNewName(newName);
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
    RenameFeedback feedback = (RenameFeedback) _feedback;
    elementKindName = WordUtils.capitalize(feedback.getElementKindName());
    oldName = feedback.getOldName();
    if (options == null) {
      options = new RenameOptions(oldName);
    }
  }
}
