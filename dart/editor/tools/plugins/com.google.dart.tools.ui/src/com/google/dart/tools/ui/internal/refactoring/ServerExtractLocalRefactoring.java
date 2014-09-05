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

import com.google.dart.server.generated.types.ExtractLocalVariableFeedback;
import com.google.dart.server.generated.types.ExtractLocalVariableOptions;
import com.google.dart.server.generated.types.RefactoringFeedback;
import com.google.dart.server.generated.types.RefactoringKind;
import com.google.dart.server.generated.types.RefactoringOptions;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import java.util.List;

/**
 * LTK wrapper around Analysis Server 'Extract Local' refactoring.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class ServerExtractLocalRefactoring extends ServerRefactoring {
  private ExtractLocalVariableOptions options = new ExtractLocalVariableOptions("name", true);
  private boolean hasSeveralOccurrences;
  private String[] proposedNames;

  public ServerExtractLocalRefactoring(String file, int offset, int length) {
    super(RefactoringKind.EXTRACT_LOCAL_VARIABLE, "Extract Local", file, offset, length);
  }

  public String[] getProposedNames() {
    return proposedNames;
  }

  public boolean hasSeveralOccurrences() {
    return hasSeveralOccurrences;
  }

  public RefactoringStatus setLocalName(String localName) {
    // TODO(scheglov) add setters
    options = new ExtractLocalVariableOptions(localName, options.extractAll());
    return setOptions(true);
  }

  public void setReplaceAllOccurrences(boolean replaceAllOccurrences) {
    // TODO(scheglov) add setters
    options = new ExtractLocalVariableOptions(options.getName(), replaceAllOccurrences);
  }

  @Override
  protected RefactoringOptions getOptions() {
    return options;
  }

  @Override
  protected void setFeedback(RefactoringFeedback _feedback) {
    ExtractLocalVariableFeedback feedback = (ExtractLocalVariableFeedback) _feedback;
    hasSeveralOccurrences = feedback.getOffsets().length > 1;
    List<String> namesList = feedback.getNames();
    proposedNames = namesList.toArray(new String[namesList.size()]);
  }
}
