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

import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.refactoring.ProgressMonitor;
import com.google.dart.engine.services.refactoring.Refactoring;
import com.google.dart.engine.services.status.RefactoringStatus;

import static com.google.dart.tools.ui.internal.refactoring.ServiceUtils.createCoreException;
import static com.google.dart.tools.ui.internal.refactoring.ServiceUtils.toLTK;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * LTK wrapper around Engine Services {@link Refactoring}.
 */
public class ServiceRefactoring extends org.eclipse.ltk.core.refactoring.Refactoring {
  private final Refactoring refactoring;

  public ServiceRefactoring(Refactoring refactoring) {
    this.refactoring = refactoring;
  }

  @Override
  public org.eclipse.ltk.core.refactoring.RefactoringStatus checkFinalConditions(IProgressMonitor pm)
      throws CoreException, OperationCanceledException {
    try {
      ProgressMonitor spm = new ServiceProgressMonitor(pm);
      RefactoringStatus status = refactoring.checkFinalConditions(spm);
      return toLTK(status);
    } catch (Throwable e) {
      return toLTK(e);
    }
  }

  @Override
  public org.eclipse.ltk.core.refactoring.RefactoringStatus checkInitialConditions(
      IProgressMonitor pm) throws CoreException, OperationCanceledException {
    try {
      ProgressMonitor spm = new ServiceProgressMonitor(pm);
      RefactoringStatus status = refactoring.checkInitialConditions(spm);
      return toLTK(status);
    } catch (Throwable e) {
      return toLTK(e);
    }
  }

  @Override
  public org.eclipse.ltk.core.refactoring.Change createChange(IProgressMonitor pm)
      throws CoreException, OperationCanceledException {
    try {
      ProgressMonitor spm = new ServiceProgressMonitor(pm);
      Change change = refactoring.createChange(spm);
      return toLTK(change);
    } catch (Throwable e) {
      throw createCoreException(e);
    }
  }

  @Override
  public String getName() {
    return refactoring.getRefactoringName();
  }
}
