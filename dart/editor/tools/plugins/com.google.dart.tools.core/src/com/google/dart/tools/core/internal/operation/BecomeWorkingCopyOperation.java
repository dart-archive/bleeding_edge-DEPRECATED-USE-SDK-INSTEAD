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
package com.google.dart.tools.core.internal.operation;

import com.google.dart.tools.core.internal.model.CompilationUnitImpl;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.internal.model.delta.DartElementDeltaImpl;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartElementDelta;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.problem.ProblemRequestor;

import org.eclipse.core.resources.IResource;

/**
 * Instances of the class <code>BecomeWorkingCopyOperation</code> implement an operation that
 * switches a CompilationUnit to working copy mode and signals the working copy addition through a
 * delta.
 */
public class BecomeWorkingCopyOperation extends DartModelOperation {
  private ProblemRequestor problemRequestor;

  /*
   * Creates a BecomeWorkingCopyOperation for the given working copy. perOwnerWorkingCopies map is
   * not null if the working copy is a shared working copy.
   */
  public BecomeWorkingCopyOperation(CompilationUnitImpl workingCopy,
      ProblemRequestor problemRequestor) {
    super(new DartElement[] {workingCopy});
    this.problemRequestor = problemRequestor;
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Override
  protected void executeOperation() throws DartModelException {
    // open the working copy now to ensure contents are that of the current
    // state of this element
    CompilationUnitImpl workingCopy = getWorkingCopy();
    DartModelManager.getInstance().getPerWorkingCopyInfo(workingCopy, true, true, problemRequestor);
    workingCopy.openWhenClosed(workingCopy.createElementInfo(), this.progressMonitor);

    if (!workingCopy.isPrimary()) {
      // report added java delta for a non-primary working copy
      DartElementDeltaImpl delta = new DartElementDeltaImpl(getDartModel());
      delta.added(workingCopy);
      addDelta(delta);
    } else {
      IResource resource = workingCopy.getResource();
      if (resource == null || resource.isAccessible()) {
        // report a F_PRIMARY_WORKING_COPY change delta for a primary working
        // copy
        DartElementDeltaImpl delta = new DartElementDeltaImpl(getDartModel());
        delta.changed(workingCopy, DartElementDelta.F_PRIMARY_WORKING_COPY);
        addDelta(delta);
      } else {
        // report an ADDED delta
        DartElementDeltaImpl delta = new DartElementDeltaImpl(getDartModel());
        delta.added(workingCopy, DartElementDelta.F_PRIMARY_WORKING_COPY);
        addDelta(delta);
      }
    }

    resultElements = new DartElement[] {workingCopy};
  }

  /**
   * Return the working copy that this operation is working on.
   * 
   * @return the working copy that this operation is working on
   */
  protected CompilationUnitImpl getWorkingCopy() {
    return (CompilationUnitImpl) getElementToProcess();
  }
}
