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

import com.google.dart.server.generated.types.SourceChange;

/**
 * The interface {@code RefactoringApplyConsumer} defines the behavior of objects that consume an
 * apply refactoring response.
 * 
 * @coverage dart.server
 */
public interface RefactoringApplyConsumer extends Consumer {
  /**
   * The {@link RefactoringProblem}s and {@link SourceChange} that have been computed.
   * 
   * @param status the status of the refactoring, the list will be empty if there are no known
   *          problems at this stage
   * @param change the changes that are to be applied to affect the refactoring
   */
  public void computed(RefactoringProblem[] problems, SourceChange sourceChange);
}
