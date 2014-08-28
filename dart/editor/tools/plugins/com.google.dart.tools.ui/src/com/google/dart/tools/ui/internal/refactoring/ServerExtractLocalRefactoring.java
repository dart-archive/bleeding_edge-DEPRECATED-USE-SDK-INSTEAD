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

package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.engine.services.status.RefactoringStatus;

import static com.google.dart.tools.ui.internal.refactoring.ServiceUtils_OLD.toLTK;

/**
 * LTK wrapper around Analysis Server 'Extract Local' refactoring.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class ServerExtractLocalRefactoring extends ServerRefactoring {
  private final boolean hasSeveralOccurrences;
  private final String[] proposedNames;
  private String name;
  private boolean replaceAllOccurrences;

  public ServerExtractLocalRefactoring(String id, RefactoringStatus initialStatus,
      boolean hasSeveralOccurrences, String[] proposedNames) {
    super(id, "Extract Local", initialStatus);
    this.hasSeveralOccurrences = hasSeveralOccurrences;
    this.proposedNames = proposedNames;
  }

  public org.eclipse.ltk.core.refactoring.RefactoringStatus checkLocalName(String localName) {
    this.name = localName;
    RefactoringStatus status = setOptions();
    status = status.escalateErrorToFatal();
    return toLTK(status);
  }

  public String[] getProposedNames() {
    return proposedNames;
  }

  public boolean hasSeveralOccurrences() {
    return hasSeveralOccurrences;
  }

  public void setLocalName(String localName) {
    this.name = localName;
    setOptions();
  }

  public void setReplaceAllOccurrences(boolean replaceAllOccurrences) {
    this.replaceAllOccurrences = replaceAllOccurrences;
    setOptions();
  }

  private RefactoringStatus setOptions() {
    // TODO(scheglov) restore or remove for the new API
//    final RefactoringStatus[] statusPtr = {null};
//    final CountDownLatch latch = new CountDownLatch(1);
//    DartCore.getAnalysisServer().setRefactoringExtractLocalOptions(
//        id,
//        replaceAllOccurrences,
//        name,
//        new RefactoringOptionsValidationConsumer() {
//          @Override
//          public void computed(RefactoringStatus status) {
//            statusPtr[0] = status;
//            latch.countDown();
//          }
//        });
//    if (Uninterruptibles.awaitUninterruptibly(latch, 100, TimeUnit.MILLISECONDS)) {
//      return statusPtr[0];
//    }
    return RefactoringStatus.createFatalErrorStatus("Timeout");
  }
}
