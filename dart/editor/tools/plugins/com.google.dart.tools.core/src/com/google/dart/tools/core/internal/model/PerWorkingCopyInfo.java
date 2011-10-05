/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.model;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.problem.Problem;
import com.google.dart.tools.core.problem.ProblemRequestor;

/**
 * Instances of the class <code>PerWorkingCopyInfo</code> implement a problem requester that accepts
 * problems associated with a single working copy.
 */
public class PerWorkingCopyInfo implements ProblemRequestor {
  private int useCount = 0;

  private ProblemRequestor problemRequestor;

  private CompilationUnitImpl workingCopy;

  public PerWorkingCopyInfo(CompilationUnitImpl workingCopy, ProblemRequestor problemRequestor) {
    this.workingCopy = workingCopy;
    this.problemRequestor = problemRequestor;
  }

  @Override
  public void acceptProblem(Problem problem) {
    ProblemRequestor requestor = getProblemRequestor();
    if (requestor == null) {
      return;
    }
    requestor.acceptProblem(problem);
  }

  @Override
  public void beginReporting() {
    ProblemRequestor requestor = getProblemRequestor();
    if (requestor == null) {
      return;
    }
    requestor.beginReporting();
  }

  /**
   * Decrement the use count for the working copy.
   */
  public void decrementUseCount() {
    useCount--;
  }

  @Override
  public void endReporting() {
    ProblemRequestor requestor = getProblemRequestor();
    if (requestor == null) {
      return;
    }
    requestor.endReporting();
  }

  public ProblemRequestor getProblemRequestor() {
    if (problemRequestor == null && workingCopy.getOwner() != null) {
      DartCore.notYetImplemented();
      // return
      // workingCopy.getOwner().getProblemRequestor(workingCopy);
    }
    return problemRequestor;
  }

  /**
   * Return the number of users of the working copy.
   * 
   * @return the number of users of the working copy
   */
  public int getUseCount() {
    return useCount;
  }

  public CompilationUnit getWorkingCopy() {
    return workingCopy;
  }

  /**
   * Increment the use count for the working copy.
   */
  public void incrementUseCount() {
    useCount++;
  }

  @Override
  public boolean isActive() {
    ProblemRequestor requestor = getProblemRequestor();
    return requestor != null && requestor.isActive();
  }

  /**
   * Set the number of users of the working copy to the given count.
   * 
   * @param count the new number of users of the working copy
   */
  public void setUseCount(int count) {
    useCount = count;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Info for "); //$NON-NLS-1$
    builder.append(workingCopy.toStringWithAncestors());
    builder.append("\nUse count = "); //$NON-NLS-1$
    builder.append(useCount);
    builder.append("\nProblem requestor:\n  "); //$NON-NLS-1$
    builder.append(problemRequestor);
    if (problemRequestor == null) {
      ProblemRequestor requestor = getProblemRequestor();
      builder.append("\nOwner problem requestor:\n  "); //$NON-NLS-1$
      builder.append(requestor);
    }
    return builder.toString();
  }
}
