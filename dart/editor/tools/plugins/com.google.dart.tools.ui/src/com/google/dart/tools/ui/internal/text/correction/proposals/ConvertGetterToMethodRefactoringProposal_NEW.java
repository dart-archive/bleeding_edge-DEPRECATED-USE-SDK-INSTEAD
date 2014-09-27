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
package com.google.dart.tools.ui.internal.text.correction.proposals;

import com.google.dart.tools.ui.actions.ConvertGetterToMethodAction_NEW;
import com.google.dart.tools.ui.internal.text.correction.CorrectionMessages;

/**
 * A quick assist proposal that runs {@link ConvertGetterToMethodAction_NEW}.
 * 
 * @coverage dart.editor.ui.correction
 */
public class ConvertGetterToMethodRefactoringProposal_NEW extends
    AbstractSelectionActionProposal_NEW {
  public ConvertGetterToMethodRefactoringProposal_NEW(ConvertGetterToMethodAction_NEW action) {
    super(action, CorrectionMessages.ConvertGetterToMethodRefactoringProposal_name);
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
