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
package com.google.dart.server;

import com.google.dart.engine.services.refactoring.Parameter;
import com.google.dart.engine.services.status.RefactoringStatus;

/**
 * The interface {@code RefactoringExtractMethodConsumer} defines the behavior of objects that
 * consume initial information about "Extract Method" refactoring.
 * 
 * @coverage dart.server
 */
public interface RefactoringExtractMethodConsumer extends Consumer {
  /**
   * An "Extract Method" refactoring has been created.
   * 
   * @param refactoringId the identifier associated with the refactoring
   * @param status the status of the refactoring
   * @param numOccurrences the number of occurrences of the selected expressions or statements
   * @param canExtractGetter is {@code true} if the code can be extracted as a getter
   * @param parameters the parameters of the method being extracted
   */
  public void computed(String refactoringId, RefactoringStatus status, int numOccurrences,
      boolean canExtractGetter, Parameter[] parameters);
}
