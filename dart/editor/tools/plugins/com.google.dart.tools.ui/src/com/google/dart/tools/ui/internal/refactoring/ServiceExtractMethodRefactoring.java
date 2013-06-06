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

import com.google.dart.engine.services.refactoring.ExtractMethodRefactoring;
import com.google.dart.engine.services.refactoring.ParameterInfo;
import com.google.dart.engine.services.status.RefactoringStatus;

import static com.google.dart.tools.ui.internal.refactoring.ServiceUtils.toLTK;

import java.util.List;

/**
 * LTK wrapper around Engine Services {@link ExtractMethodRefactoring}.
 */
public class ServiceExtractMethodRefactoring extends ServiceRefactoring {
  private final ExtractMethodRefactoring refactoring;

  public ServiceExtractMethodRefactoring(ExtractMethodRefactoring refactoring) {
    super(refactoring);
    this.refactoring = refactoring;
  }

  public boolean canExtractGetter() {
    return refactoring.canExtractGetter();
  }

  public org.eclipse.ltk.core.refactoring.RefactoringStatus checkMethodName() {
    RefactoringStatus status = refactoring.checkMethodName();
    status = status.escalateErrorToFatal();
    return toLTK(status);
  }

  public org.eclipse.ltk.core.refactoring.RefactoringStatus checkParameterNames() {
    RefactoringStatus status = refactoring.checkParameterNames();
    status = status.escalateErrorToFatal();
    return toLTK(status);
  }

  public boolean getExtractGetter() {
    return refactoring.getExtractGetter();
  }

  public int getNumberOfDuplicates() {
    return refactoring.getNumberOfDuplicates();
  }

  public List<ParameterInfo> getParameters() {
    return refactoring.getParameters();
  }

  public boolean getReplaceAllOccurrences() {
    return refactoring.getReplaceAllOccurrences();
  }

  public String getSignature(String methodName) {
    return refactoring.getSignature(methodName);
  }

  public void setExtractGetter(boolean extractGetter) {
    refactoring.setExtractGetter(extractGetter);
  }

  public void setMethodName(String methodName) {
    refactoring.setMethodName(methodName);
  }

  public void setReplaceAllOccurrences(boolean replaceAllOccurences) {
    refactoring.setReplaceAllOccurrences(replaceAllOccurences);
  }
}
