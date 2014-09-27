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

import com.google.dart.tools.ui.actions.ConvertGetterToMethodAction_OLD;
import com.google.dart.tools.ui.internal.text.correction.CorrectionMessages;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;

/**
 * A quick assist proposal that runs {@link ConvertGetterToMethodAction_OLD}.
 * 
 * @coverage dart.editor.ui.correction
 */
public class ConvertGetterToMethodRefactoringProposal_OLD extends AbstractSelectionActionProposal_OLD {
  public ConvertGetterToMethodRefactoringProposal_OLD(ConvertGetterToMethodAction_OLD action,
      DartSelection selection) {
    super(action, CorrectionMessages.ConvertGetterToMethodRefactoringProposal_name, selection);
  }

  @Override
  public String getAdditionalProposalInfo() {
    return CorrectionMessages.ConvertGetterToMethodRefactoringProposal_additionalInfo;
  }

  @Override
  public int getRelevance() {
    return 9;
  }
}
