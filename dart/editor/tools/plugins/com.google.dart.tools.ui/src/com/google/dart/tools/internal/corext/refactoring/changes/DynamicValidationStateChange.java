/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.internal.corext.refactoring.changes;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class DynamicValidationStateChange extends CompositeChange implements
    WorkspaceTracker.Listener {

  private boolean listenerRegistered = false;
  private RefactoringStatus validationState = null;
  private long timeStamp;
  private ISchedulingRule schedulingRule;

  // 30 minutes
  private static final long LIFE_TIME = 30 * 60 * 1000;

  public DynamicValidationStateChange(Change change) {
    super(change.getName());
    add(change);
    markAsSynthetic();
    schedulingRule = ResourcesPlugin.getWorkspace().getRoot();
  }

  public DynamicValidationStateChange(String name) {
    super(name);
    markAsSynthetic();
    schedulingRule = ResourcesPlugin.getWorkspace().getRoot();
  }

  public DynamicValidationStateChange(String name, Change[] changes) {
    super(name, changes);
    markAsSynthetic();
    schedulingRule = ResourcesPlugin.getWorkspace().getRoot();
  }

  @Override
  public void dispose() {
    if (listenerRegistered) {
      WorkspaceTracker.INSTANCE.removeListener(this);
      listenerRegistered = false;
    }
    super.dispose();
  }

  public ISchedulingRule getSchedulingRule() {
    return schedulingRule;
  }

  @Override
  public void initializeValidationData(IProgressMonitor pm) {
    super.initializeValidationData(pm);
    WorkspaceTracker.INSTANCE.addListener(this);
    listenerRegistered = true;
    timeStamp = System.currentTimeMillis();
  }

  @Override
  public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
    if (validationState == null) {
      return super.isValid(pm);
    }
    return validationState;
  }

  @Override
  public Change perform(IProgressMonitor pm) throws CoreException {
    final Change[] result = new Change[1];
    IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        result[0] = DynamicValidationStateChange.super.perform(monitor);
      }
    };
    DartCore.run(runnable, schedulingRule, pm);
    return result[0];
  }

  public void setSchedulingRule(ISchedulingRule schedulingRule) {
    this.schedulingRule = schedulingRule;
  }

  @Override
  public void workspaceChanged() {
    long currentTime = System.currentTimeMillis();
    if (currentTime - timeStamp < LIFE_TIME) {
      return;
    }
    validationState = RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.DynamicValidationStateChange_workspace_changed);
    // remove listener from workspace tracker
    WorkspaceTracker.INSTANCE.removeListener(this);
    listenerRegistered = false;
    // clear up the children to not hang onto too much memory
    Change[] children = clear();
    for (int i = 0; i < children.length; i++) {
      final Change change = children[i];
      SafeRunner.run(new ISafeRunnable() {
        @Override
        public void handleException(Throwable exception) {
          DartToolsPlugin.log(exception);
        }

        @Override
        public void run() throws Exception {
          change.dispose();
        }
      });
    }
  }

  @Override
  protected Change createUndoChange(Change[] childUndos) {
    DynamicValidationStateChange result = new DynamicValidationStateChange(getName());
    for (int i = 0; i < childUndos.length; i++) {
      result.add(childUndos[i]);
    }
    return result;
  }
}
