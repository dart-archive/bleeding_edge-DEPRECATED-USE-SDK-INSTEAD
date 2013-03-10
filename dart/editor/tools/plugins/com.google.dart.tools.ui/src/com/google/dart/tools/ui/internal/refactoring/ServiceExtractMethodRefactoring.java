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
import com.google.dart.tools.internal.corext.refactoring.code.ExtractMethodRefactoring_I;

import static com.google.dart.tools.ui.internal.refactoring.ServiceUtils.toLTK;

import java.util.List;

/**
 * LTK wrapper around Engine Services {@link ExtractMethodRefactoring}.
 */
public class ServiceExtractMethodRefactoring extends ServiceRefactoring implements
    ExtractMethodRefactoring_I {
  private final ExtractMethodRefactoring refactoring;

  public ServiceExtractMethodRefactoring(ExtractMethodRefactoring refactoring) {
    super(refactoring);
    this.refactoring = refactoring;
  }

  @Override
  public org.eclipse.ltk.core.refactoring.RefactoringStatus checkMethodName() {
    RefactoringStatus status = refactoring.checkMethodName();
    status = status.escalateErrorToFatal();
    return toLTK(status);
  }

  @Override
  public org.eclipse.ltk.core.refactoring.RefactoringStatus checkParameterNames() {
    RefactoringStatus status = refactoring.checkParameterNames();
    status = status.escalateErrorToFatal();
    return toLTK(status);
  }

  @Override
  public int getNumberOfDuplicates() {
    return refactoring.getNumberOfDuplicates();
  }

  @Override
  public List<ParameterInfo> getParameters() {
    return refactoring.getParameters();
  }

  @Override
  public boolean getReplaceAllOccurrences() {
    return refactoring.getReplaceAllOccurrences();
  }

  @Override
  public String getSignature(String methodName) {
    return refactoring.getSignature(methodName);
  }

  @Override
  public void setMethodName(String methodName) {
    refactoring.setMethodName(methodName);
  }

  @Override
  public void setReplaceAllOccurrences(boolean replaceAllOccurences) {
    refactoring.setReplaceAllOccurrences(replaceAllOccurences);
  }

//  @Override
//  public org.eclipse.ltk.core.refactoring.RefactoringStatus checkLocalName(String localName) {
//    RefactoringStatus status = refactoring.checkLocalName(localName);
//    status = status.escalateErrorToFatal();
//    return toLTK(status);
//  }
//
//  @Override
//  public String[] guessNames() {
//    return refactoring.guessNames();
//  }
//
//  @Override
//  public boolean replaceAllOccurrences() {
//    return refactoring.replaceAllOccurrences();
//  }
//
//  @Override
//  public void setLocalName(String localName) {
//    refactoring.setLocalName(localName);
//  }
//
//  @Override
//  public void setReplaceAllOccurrences(boolean replaceAllOccurrences) {
//    refactoring.setReplaceAllOccurrences(replaceAllOccurrences);
//  }
}
