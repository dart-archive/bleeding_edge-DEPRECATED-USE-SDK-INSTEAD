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

import java.util.Map;

/**
 * The interface {@code RefactoringCreateConsumer} defines the behavior of objects that consume a
 * create refactoring response.
 * 
 * @coverage dart.server
 */
public interface RefactoringCreateConsumer extends Consumer {
  /**
   * The {@link RefactoringProblem}s that have been computed for the refactoring id.
   * 
   * @param refactoringId the refactoring id, this may be one of the values from
   *          {@link RefactoringKind}
   * @param status the status of the refactoring, the list will be empty if there are no known
   *          problems at this stage
   * @param feedback additional feedback parameters for this kind of refactoring
   */
  public void computed(String refactoringId, RefactoringProblem[] status,
      Map<String, Object> feedback);
}
