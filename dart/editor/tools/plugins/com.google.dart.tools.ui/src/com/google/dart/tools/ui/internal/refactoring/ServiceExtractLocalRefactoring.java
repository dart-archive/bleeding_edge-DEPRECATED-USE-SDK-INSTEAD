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

import com.google.dart.engine.services.refactoring.ExtractLocalRefactoring;
import com.google.dart.engine.services.status.RefactoringStatus;

import static com.google.dart.tools.ui.internal.refactoring.ServiceUtils_OLD.toLTK;

/**
 * LTK wrapper around Engine Services {@link ExtractLocalRefactoring}.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class ServiceExtractLocalRefactoring extends ServiceRefactoring {
  private final ExtractLocalRefactoring refactoring;

  public ServiceExtractLocalRefactoring(ExtractLocalRefactoring refactoring) {
    super(refactoring);
    this.refactoring = refactoring;
  }

  public org.eclipse.ltk.core.refactoring.RefactoringStatus checkLocalName(String localName) {
    RefactoringStatus status = refactoring.checkLocalName(localName);
    status = status.escalateErrorToFatal();
    return toLTK(status);
  }

  public String[] guessNames() {
    return refactoring.guessNames();
  }

  public boolean hasSeveralOccurrences() {
    return refactoring.hasSeveralOccurrences();
  }

  public void setLocalName(String localName) {
    refactoring.setLocalName(localName);
  }

  public void setReplaceAllOccurrences(boolean replaceAllOccurrences) {
    refactoring.setReplaceAllOccurrences(replaceAllOccurrences);
  }
}
