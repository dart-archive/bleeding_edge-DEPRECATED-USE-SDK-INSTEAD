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

/**
 * The interface {@code RefactoringSetOptionsConsumer} defines the behavior of objects that consume
 * a set refactoring options response.
 * 
 * @coverage dart.server
 */
public interface RefactoringSetOptionsConsumer extends Consumer {
  /**
   * The refactoring kinds that have been computed for file location.
   * 
   * @param refactoringProblems the status of the refactoring. The list will be empty if there are
   *          no known problems at this stage
   */
  public void computedStatus(RefactoringProblem[] refactoringProblems);
}
