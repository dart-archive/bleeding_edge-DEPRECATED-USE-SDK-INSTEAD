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

import com.google.dart.engine.services.status.RefactoringStatus;

/**
 * The interface {@code RefactoringExtractLocalConsumer} defines the behavior of objects that
 * consume initial information about "Extract Local" refactoring.
 * 
 * @coverage dart.server
 */
public interface RefactoringExtractLocalConsumer extends Consumer {
  /**
   * An "Extract Local" refactoring has been created.
   * 
   * @param refactoringId the identifier associated with the refactoring
   * @param status the status of the refactoring
   * @param hasSeveralOccurrences is {@code true} if there are more than one occurrence
   * @param proposedNames proposed variable names (may be empty, but not null)
   */
  public void computed(String refactoringId, RefactoringStatus status,
      boolean hasSeveralOccurrences, String[] proposedNames);
}
