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

package com.google.dart.engine.services.refactoring;

/**
 * {@link ProgressMonitor} which represents part of bigger {@link ProgressMonitor}.
 */
public class SubProgressMonitor implements ProgressMonitor {
  private final ProgressMonitor parent;
  private final int parentTicks;
  private int nestedBeginTasks;
  private double scale;
  private int sentToParent;
  private boolean usedUp;
  private boolean hasSubTask;

  public SubProgressMonitor(ProgressMonitor parent, int parentTicks) {
    this.parent = parent;
    this.parentTicks = parentTicks;
  }

  @Override
  public void beginTask(String name, int totalWork) {
    nestedBeginTasks++;
    // ignore nested begin task calls
    if (nestedBeginTasks > 1) {
      return;
    }
    // prepare scale to convert this PM to parent PM
    scale = totalWork <= 0 ? 0 : (double) parentTicks / (double) totalWork;
    sentToParent = 0;
    usedUp = false;
  }

  @Override
  public void done() {
    // ignore if already done
    if (nestedBeginTasks == 0 || --nestedBeginTasks > 0) {
      return;
    }
    // send any remaining ticks
    double remaining = parentTicks - sentToParent;
    if (remaining > 0) {
      parent.internalWorked(remaining);
    }
    // clear the sub task if there was one
    if (hasSubTask) {
      parent.subTask("");
    }
  }

  @Override
  public void internalWorked(double work) {
    // may be ignore
    if (usedUp || nestedBeginTasks != 1) {
      return;
    }
    // send to parent
    double parentWork = work > 0.0d ? scale * work : 0.0d;
    parent.internalWorked(parentWork);
    sentToParent += parentWork;
    // may be used up 
    if (sentToParent >= parentTicks) {
      usedUp = true;
    }
  }

  @Override
  public boolean isCanceled() {
    return parent.isCanceled();
  }

  @Override
  public void setCanceled() {
    parent.setCanceled();
  }

  @Override
  public void subTask(String name) {
    hasSubTask = true;
    parent.subTask(name);
  }

  @Override
  public void worked(int work) {
    internalWorked(work);
  }
}
