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
package com.google.dart.tools.ui.internal.text.correction.proposals;

import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.internal.refactoring.actions.RenameDartElementAction_OLD;
import com.google.dart.tools.ui.internal.text.correction.CorrectionMessages;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;

import org.eclipse.swt.graphics.Image;

/**
 * A quick assist proposal that starts the Rename refactoring.
 * 
 * @coverage dart.editor.ui.correction
 */
public class RenameRefactoringProposal extends AbstractSelectionActionProposal {
  public RenameRefactoringProposal(RenameDartElementAction_OLD action, DartSelection selection) {
    super(action, CorrectionMessages.RenameRefactoringProposal_name, selection);
  }

  @Override
  public String getAdditionalProposalInfo() {
    return CorrectionMessages.RenameRefactoringProposal_additionalInfo;
  }

  @Override
  public Image getImage() {
    return DartPluginImages.get(DartPluginImages.IMG_CORRECTION_LINKED_RENAME);
  }

  @Override
  public int getRelevance() {
    return 8;
  }
}
