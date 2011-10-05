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
import com.google.dart.tools.core.internal.model.DartProjectImpl;
import com.google.dart.tools.core.internal.model.ExternalDartProject;
import com.google.dart.tools.core.internal.model.delta.DartElementDeltaImpl;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartElementDelta;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;

import org.eclipse.core.resources.IResource;

/**
 * Instances of the class <code>DiscardWorkingCopyOperation</code> implement an operation that
 * discards a working copy (decrement its use count and remove its working copy info if the use
 * count is 0) and signal its removal through a delta.
 */
public class DiscardWorkingCopyOperation extends DartModelOperation {
  public DiscardWorkingCopyOperation(DartElement workingCopy) {
    super(new DartElement[] {workingCopy});
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Override
  protected void executeOperation() throws DartModelException {
    CompilationUnitImpl workingCopy = getWorkingCopy();

    DartModelManager manager = DartModelManager.getInstance();
    int useCount = manager.discardPerWorkingCopyInfo(workingCopy);
    if (useCount == 0) {
      DartProject dartProject = workingCopy.getDartProject();
      if (ExternalDartProject.EXTERNAL_PROJECT_NAME.equals(dartProject.getElementName())) {
        manager.removePerProjectInfo((DartProjectImpl) dartProject, true);
        manager.containerRemove(dartProject);
      }
      if (!workingCopy.isPrimary()) {
        // report removed Dart delta for a non-primary working copy
        DartElementDeltaImpl delta = new DartElementDeltaImpl(getDartModel());
        delta.removed(workingCopy);
        addDelta(delta);
        removeReconcileDelta(workingCopy);
      } else {
        IResource resource = workingCopy.getResource();
        if (resource == null || resource.isAccessible()) {
          // report a F_PRIMARY_WORKING_COPY change delta for a primary working
          // copy
          DartElementDeltaImpl delta = new DartElementDeltaImpl(getDartModel());
          delta.changed(workingCopy, DartElementDelta.F_PRIMARY_WORKING_COPY);
          addDelta(delta);
        } else {
          // report a REMOVED delta
          DartElementDeltaImpl delta = new DartElementDeltaImpl(getDartModel());
          delta.removed(workingCopy, DartElementDelta.F_PRIMARY_WORKING_COPY);
          addDelta(delta);
        }
      }
    }
  }

  /**
   * Return the working copy this operation is working on.
   * 
   * @return the working copy this operation is working on
   */
  protected CompilationUnitImpl getWorkingCopy() {
    return (CompilationUnitImpl) getElementToProcess();
  }
}
