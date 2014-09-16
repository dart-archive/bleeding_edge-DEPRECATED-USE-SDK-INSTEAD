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

package com.google.dart.tools.ui.internal.refactoring;

import com.google.common.util.concurrent.Uninterruptibles;
import com.google.dart.server.GetRefactoringConsumer;
import com.google.dart.server.generated.types.RefactoringFeedback;
import com.google.dart.server.generated.types.RefactoringOptions;
import com.google.dart.server.generated.types.RefactoringProblem;
import com.google.dart.server.generated.types.SourceChange;
import com.google.dart.tools.core.DartCore;

import static com.google.dart.tools.ui.internal.refactoring.ServiceUtils_NEW.toLTK;
import static com.google.dart.tools.ui.internal.refactoring.ServiceUtils_NEW.toRefactoringStatus;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * LTK wrapper around Analysis Server refactoring.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public abstract class ServerRefactoring extends Refactoring {
  protected static final RefactoringStatus TIMEOUT_STATUS = RefactoringStatus.createFatalErrorStatus("Timeout");

  public static String[] toStringArray(List<String> list) {
    return list.toArray(new String[list.size()]);
  }

  protected final String kind;
  private final String name;
  private final String file;
  private final int offset;
  private final int length;

  protected RefactoringStatus initialStatus;
  protected RefactoringStatus optionsStatus;
  protected RefactoringStatus finalStatus;
  private Change change;

  public ServerRefactoring(String kind, String name, String file, int offset, int length) {
    this.kind = kind;
    this.name = name;
    this.file = file;
    this.offset = offset;
    this.length = length;
  }

  @Override
  public RefactoringStatus checkFinalConditions(IProgressMonitor pm) {
    if (!setOptions(false)) {
      return TIMEOUT_STATUS;
    }
    return finalStatus;
  }

  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) {
    if (!setOptions(true)) {
      return TIMEOUT_STATUS;
    }
    return initialStatus;
  }

  @Override
  public Change createChange(IProgressMonitor pm) {
    boolean timeout = !setOptions(false);
    if (timeout) {
      throw new OperationCanceledException();
    }
    return change;
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

  /**
   * Returns this {@link RefactoringOptions} subclass instance.
   */
  protected abstract RefactoringOptions getOptions();

  /**
   * Sets the received {@link RefactoringFeedback}.
   */
  protected abstract void setFeedback(RefactoringFeedback feedback);

  protected boolean setOptions(boolean validateOnly) {
    final CountDownLatch latch = new CountDownLatch(1);
    RefactoringOptions options = getOptions();
    DartCore.getAnalysisServer().edit_getRefactoring(
        kind,
        file,
        offset,
        length,
        validateOnly,
        options,
        new GetRefactoringConsumer() {
          @Override
          public void computedRefactorings(List<RefactoringProblem> initialProblems,
              List<RefactoringProblem> optionsProblems, List<RefactoringProblem> finalProblems,
              RefactoringFeedback feedback, SourceChange _change, List<String> potentialEdits) {
            if (feedback != null) {
              setFeedback(feedback);
            }
            initialStatus = toRefactoringStatus(initialProblems);
            optionsStatus = toRefactoringStatus(optionsProblems);
            finalStatus = toRefactoringStatus(finalProblems);
            change = toLTK(_change);
            latch.countDown();
          }
        });
    return Uninterruptibles.awaitUninterruptibly(latch, 100, TimeUnit.MILLISECONDS);
  }
}
