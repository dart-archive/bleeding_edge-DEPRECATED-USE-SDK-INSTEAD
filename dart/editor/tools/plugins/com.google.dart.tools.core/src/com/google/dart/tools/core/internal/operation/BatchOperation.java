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

import com.google.dart.tools.core.internal.model.DartModelStatusImpl;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModelStatus;

import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;

/**
 * Instances of the class <code>BatchOperation</code> represent an operation created as a result of
 * a call to DartCore.run(IWorkspaceRunnable, IProgressMonitor) that encapsulates a user defined
 * IWorkspaceRunnable.
 */
public class BatchOperation extends DartModelOperation {
  private IWorkspaceRunnable runnable;

  public BatchOperation(IWorkspaceRunnable runnable) {
    this.runnable = runnable;
  }

  @Override
  protected boolean canModifyRoots() {
    // anything in the workspace runnable can modify the roots
    return true;
  }

  @Override
  protected void executeOperation() throws DartModelException {
    try {
      runnable.run(this.progressMonitor);
    } catch (DartModelException exception) {
      throw exception;
    } catch (CoreException exception) {
      if (exception.getStatus().getCode() == IResourceStatus.OPERATION_FAILED) {
        Throwable cause = exception.getStatus().getException();
        if (cause instanceof DartModelException) {
          throw (DartModelException) cause;
        }
      }
      throw new DartModelException(exception);
    }
  }

  @Override
  protected DartModelStatus verify() {
    // cannot verify user defined operation
    return DartModelStatusImpl.VERIFIED_OK;
  }
}
