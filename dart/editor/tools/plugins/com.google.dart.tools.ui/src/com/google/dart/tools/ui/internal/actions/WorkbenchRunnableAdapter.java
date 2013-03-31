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
package com.google.dart.tools.ui.internal.actions;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.IThreadListener;

import java.lang.reflect.InvocationTargetException;

/**
 * An {@link IRunnableWithProgress} that adapts an {@link IWorkspaceRunnable} so that is can be
 * executed inside an {@link IRunnableContext}. The runnable is run as an
 * {@linkplain JavaCore#run(IWorkspaceRunnable, ISchedulingRule, IProgressMonitor) atomic Java model
 * operation}.
 * <p>
 * {@link OperationCanceledException}s thrown by the adapted runnable are caught and re-thrown as
 * {@link InterruptedException}s.
 */
public class WorkbenchRunnableAdapter implements IRunnableWithProgress, IThreadListener {

  private IWorkspaceRunnable fWorkspaceRunnable;
  private ISchedulingRule fRule;
  private boolean fTransfer;

  /**
   * Runs a workspace runnable with the given lock or <code>null</code> to run with no lock at all.
   * 
   * @param runnable the workspace runnable
   * @param rule the scheduling rule
   */
  public WorkbenchRunnableAdapter(IWorkspaceRunnable runnable, ISchedulingRule rule) {
    this(runnable, rule, true);
  }

  /**
   * Runs a workspace runnable with the given lock or <code>null</code> to run with no lock at all.
   * 
   * @param runnable the workspace runnable
   * @param rule the scheduling rule
   * @param transfer <code>true</code> if the rule is to be transfered to the model context thread.
   *          Otherwise <code>false</code>
   */
  public WorkbenchRunnableAdapter(IWorkspaceRunnable runnable, ISchedulingRule rule,
      boolean transfer) {
    Assert.isNotNull(runnable);
    Assert.isNotNull(rule);
    fWorkspaceRunnable = runnable;
    fRule = rule;
    fTransfer = transfer;
  }

  public ISchedulingRule getSchedulingRule() {
    return fRule;
  }

  @Override
  public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
    try {
      ResourcesPlugin.getWorkspace().run(
          fWorkspaceRunnable,
          fRule,
          IWorkspace.AVOID_UPDATE,
          monitor);
    } catch (OperationCanceledException e) {
      throw new InterruptedException(e.getMessage());
    } catch (CoreException e) {
      throw new InvocationTargetException(e);
    }
  }

  @Override
  public void threadChange(Thread thread) {
    if (fTransfer) {
      Job.getJobManager().transferRule(fRule, thread);
    }
  }
}
