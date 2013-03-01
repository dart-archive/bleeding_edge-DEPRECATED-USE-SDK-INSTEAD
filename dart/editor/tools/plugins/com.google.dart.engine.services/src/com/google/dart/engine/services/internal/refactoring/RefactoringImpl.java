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

package com.google.dart.engine.services.internal.refactoring;

import com.google.dart.engine.services.refactoring.NullProgressMonitor;
import com.google.dart.engine.services.refactoring.OperationCanceledException;
import com.google.dart.engine.services.refactoring.ProgressMonitor;
import com.google.dart.engine.services.refactoring.Refactoring;
import com.google.dart.engine.services.refactoring.SubProgressMonitor;
import com.google.dart.engine.services.status.RefactoringStatus;

/**
 * Abstract implementation of {@link Refactoring}.
 */
public abstract class RefactoringImpl implements Refactoring {
  /**
   * @return the given {@link ProgressMonitor} or new {@link NullProgressMonitor} instance.
   */
  protected static ProgressMonitor checkProgressMonitor(ProgressMonitor pm) {
    if (pm != null) {
      return pm;
    }
    return new NullProgressMonitor();
  }

  @Override
  public RefactoringStatus checkAllConditions(ProgressMonitor pm) throws Exception {
    pm = checkProgressMonitor(pm);
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
}
