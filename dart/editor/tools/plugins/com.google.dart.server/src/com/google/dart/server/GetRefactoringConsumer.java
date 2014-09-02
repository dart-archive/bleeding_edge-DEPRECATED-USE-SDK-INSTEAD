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

import java.util.List;

/**
 * The interface {@code GetRefactoringConsumer} defines the behavior of objects that get the changes
 * required to perform a refactoring.
 * 
 * @coverage dart.server
 */
public interface GetRefactoringConsumer extends Consumer {
  /**
   * The changes required to perform a refactoring.
   * <p>
   * TODO (jwren) can feedback be Map<String, Object>?, fill in javadoc below.
   * 
   * @param problems The status of the refactoring. The array will be empty if there are no known
   *          problems
   * @param feedback
   * @param change
   * @param potentialEdits
   */
  public void computedRefactorings(List<RefactoringProblem> problems, Object feedback,
      SourceChange change, List<String> potentialEdits);
}
