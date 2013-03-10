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
package com.google.dart.tools.internal.corext.refactoring.code;

import com.google.dart.engine.services.refactoring.ParameterInfo;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import java.util.List;

/**
 * Interface of "Extract Method Variable" refactoring.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public interface ExtractMethodRefactoring_I {
  RefactoringStatus checkMethodName();

  RefactoringStatus checkParameterNames();

  int getNumberOfDuplicates();

  List<ParameterInfo> getParameters();

  boolean getReplaceAllOccurrences();

  String getSignature(String methodName);

  void setMethodName(String methodName);

  void setReplaceAllOccurrences(boolean replaceAllOccurences);
}
