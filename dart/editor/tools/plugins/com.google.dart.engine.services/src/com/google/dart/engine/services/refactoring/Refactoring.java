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

package com.google.dart.engine.services.refactoring;

import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.status.RefactoringStatus;

/**
 * Abstract refactoring operation.
 */
public abstract class Refactoring {
  /**
   * Checks all conditions - {@link #checkInitialConditions(ProgressMonitor)} and
   * {@link #checkFinalConditions(ProgressMonitor)} to decide if refactoring can be performed.
   */
  public RefactoringStatus checkAllConditions(ProgressMonitor pm) throws Exception {
    pm.beginTask("", 2);
    RefactoringStatus result = new RefactoringStatus();
    result.merge(checkInitialConditions(new SubProgressMonitor(pm, 1)));
    if (!result.hasFatalError()) {
      if (pm.isCanceled()) {
        throw new OperationCanceledException();
      }
      result.merge(checkFinalConditions(new SubProgressMonitor(pm, 1)));
    }
    pm.done();
    return result;
  }

  /**
   * Validates environment to check if refactoring can be performed.
   */
  public abstract RefactoringStatus checkFinalConditions(ProgressMonitor pm) throws Exception;

  /**
   * Validates arguments to check if refactoring can be performed. This check is usually quick
   * because it is used often as arguments change, may be by human.
   */
  public abstract RefactoringStatus checkInitialConditions(ProgressMonitor pm) throws Exception;

  /**
   * @return the {@link Change} to apply to perform this refactoring.
   */
  public abstract Change createChange(ProgressMonitor pm) throws Exception;

  /**
   * @return the human readable name of this {@link Refactoring}.
   */
  public abstract String getRefactoringName();
}
