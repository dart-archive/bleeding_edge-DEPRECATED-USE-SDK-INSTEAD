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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.dart.server.GetRefactoringConsumer;
import com.google.dart.server.generated.types.RefactoringFeedback;
import com.google.dart.server.generated.types.RefactoringOptions;
import com.google.dart.server.generated.types.RefactoringProblem;
import com.google.dart.server.generated.types.RequestError;
import com.google.dart.server.generated.types.SourceChange;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.internal.corext.refactoring.base.StringStatusContext;

import static com.google.dart.tools.ui.internal.refactoring.ServiceUtils_NEW.toLTK;
import static com.google.dart.tools.ui.internal.refactoring.ServiceUtils_NEW.toRefactoringStatus;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * LTK wrapper around Analysis Server refactoring.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public abstract class ServerRefactoring extends Refactoring {
  public interface ServerRefactoringListener {
    void requestStateChanged(boolean hasPendingRequests, RefactoringStatus optionsStatus);
  }

  protected static final RefactoringStatus TIMEOUT_STATUS = RefactoringStatus.createFatalErrorStatus("Timeout");

  public static String[] toStringArray(List<String> list) {
    return list.toArray(new String[list.size()]);
  }

  protected final String kind;
  private final String name;
  private final String file;
  private final int offset;
  private final int length;

  protected RefactoringStatus serverErrorStatus;
  protected RefactoringStatus initialStatus;
  protected RefactoringStatus optionsStatus;
  protected RefactoringStatus finalStatus;
  private Change change;
  private final List<String> externalFiles = Lists.newArrayList();

  private int lastId = 0;
  private final Set<Integer> pendingRequestIds = Sets.newHashSet();
  private ServerRefactoringListener listener;

  public ServerRefactoring(String kind, String name, String file, int offset, int length) {
    this.kind = kind;
    this.name = name;
    this.file = file;
    this.offset = offset;
    this.length = length;
  }

  @Override
  public RefactoringStatus checkFinalConditions(IProgressMonitor pm) {
    setOptions(false, pm);
    if (serverErrorStatus != null) {
      return serverErrorStatus;
    }
    // done if already fatal
    if (finalStatus.hasFatalError()) {
      return finalStatus;
    }
    // check for external files
    if (!externalFiles.isEmpty()) {
      finalStatus.addError(
          "The following files are external and cannot be updated",
          new StringStatusContext(null, StringUtils.join(externalFiles, "\n")));
    }
    // done
    return finalStatus;
  }

  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) {
    setOptions(true, pm);
    if (serverErrorStatus != null) {
      return serverErrorStatus;
    }
    return initialStatus;
  }

  @Override
  public Change createChange(IProgressMonitor pm) {
    setOptions(false, pm);
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

  public void setListener(ServerRefactoringListener listener) {
    this.listener = listener;
  }

  /**
   * Returns this {@link RefactoringOptions} subclass instance.
   */
  protected abstract RefactoringOptions getOptions();

  /**
   * Sets the received {@link RefactoringFeedback}.
   */
  protected abstract void setFeedback(RefactoringFeedback feedback);

  protected void setOptions(boolean validateOnly) {
    setOptions(validateOnly, null);
  }

  protected void setOptions(boolean validateOnly, IProgressMonitor pm) {
    // add a new pending request ID
    final int id;
    synchronized (pendingRequestIds) {
      id = ++lastId;
      pendingRequestIds.add(id);
    }
    // do request
    serverErrorStatus = null;
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
            externalFiles.clear();
            initialStatus = toRefactoringStatus(initialProblems);
            optionsStatus = toRefactoringStatus(optionsProblems);
            finalStatus = toRefactoringStatus(finalProblems);
            change = toLTK(_change, externalFiles);
            latch.countDown();
            requestDone(id);
          }

          @Override
          public void onError(RequestError requestError) {
            String message = "Server error: " + requestError.getMessage();
            serverErrorStatus = RefactoringStatus.createFatalErrorStatus(message);
            latch.countDown();
            requestDone(id);
          }

          private void requestDone(final int id) {
            synchronized (pendingRequestIds) {
              pendingRequestIds.remove(id);
              notifyListener();
            }
          }
        });
    // wait for completion
    if (pm != null) {
      while (true) {
        if (pm.isCanceled()) {
          throw new OperationCanceledException();
        }
        boolean done = Uninterruptibles.awaitUninterruptibly(latch, 10, TimeUnit.MILLISECONDS);
        if (done) {
          return;
        }
      }
    } else {
      // Wait a very short time, just in case it it can be done fast,
      // so that we don't have to disable UI and re-enable it 2 milliseconds later.
      Uninterruptibles.awaitUninterruptibly(latch, 10, TimeUnit.MILLISECONDS);
      notifyListener();
    }
  }

  private void notifyListener() {
    if (listener != null) {
      boolean hasPendingRequests = !pendingRequestIds.isEmpty();
      listener.requestStateChanged(hasPendingRequests, optionsStatus);
    }
  }
}
