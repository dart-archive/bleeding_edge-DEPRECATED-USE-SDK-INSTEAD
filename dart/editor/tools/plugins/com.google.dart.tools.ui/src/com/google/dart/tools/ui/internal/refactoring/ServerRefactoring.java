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
import com.google.dart.engine.services.status.RefactoringStatus;

import static com.google.dart.tools.ui.internal.refactoring.ServiceUtils_OLD.toLTK;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * LTK wrapper around Analysis Server refactoring.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class ServerRefactoring extends org.eclipse.ltk.core.refactoring.Refactoring {
  protected final String id;
  private final String name;
  private final RefactoringStatus initialStatus;
  private RefactoringStatus finalStatus;
  private Change change;

  public ServerRefactoring(String id, String name, RefactoringStatus initialStatus) {
    this.id = id;
    this.name = name;
    this.initialStatus = initialStatus;
  }

  @Override
  public org.eclipse.ltk.core.refactoring.RefactoringStatus checkFinalConditions(IProgressMonitor pm)
      throws CoreException, OperationCanceledException {
    pm.beginTask("Checking final conditions", IProgressMonitor.UNKNOWN);
    // TODO(scheglov) restore or remove for the new API
//    final CountDownLatch latch = new CountDownLatch(1);
//    DartCore.getAnalysisServer().applyRefactoring(id, new RefactoringApplyConsumer() {
//      @Override
//      public void computed(RefactoringStatus _status, Change _change) {
//        finalStatus = _status;
//        change = _change;
//        latch.countDown();
//      }
//    });
//    while (true) {
//      if (pm.isCanceled()) {
//        throw new OperationCanceledException();
//      }
//      if (Uninterruptibles.awaitUninterruptibly(latch, 10, TimeUnit.MILLISECONDS)) {
//        pm.done();
//        break;
//      }
//    }
//    return toLTK(finalStatus);
    return new org.eclipse.ltk.core.refactoring.RefactoringStatus();
  }

  @Override
  public org.eclipse.ltk.core.refactoring.RefactoringStatus checkInitialConditions(
      IProgressMonitor pm) throws CoreException, OperationCanceledException {
    return toLTK(initialStatus);
  }

  @Override
  public org.eclipse.ltk.core.refactoring.Change createChange(IProgressMonitor pm) {
    return toLTK(change);
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * @return {@code true} if the {@link Change} created by refactoring may be unsafe, so we want
   *         user to review the change to ensure that he understand it.
   */
  public boolean requiresPreview() {
    return false;
  }
}
