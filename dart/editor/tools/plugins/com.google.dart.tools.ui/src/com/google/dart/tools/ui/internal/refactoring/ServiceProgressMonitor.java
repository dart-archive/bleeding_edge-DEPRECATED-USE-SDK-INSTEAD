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

import com.google.dart.engine.services.refactoring.ProgressMonitor;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Engine Services {@link ProgressMonitor} implementation that reports to Eclipse
 * {@link IProgressMonitor}.
 */
public class ServiceProgressMonitor implements ProgressMonitor {
  private final IProgressMonitor pm;

  public ServiceProgressMonitor(IProgressMonitor pm) {
    this.pm = pm;
  }

  @Override
  public void beginTask(String name, int totalWork) {
    pm.beginTask(name, totalWork);
  }

  @Override
  public void done() {
    pm.done();
  }

  @Override
  public void internalWorked(double work) {
    pm.internalWorked(work);
  }

  @Override
  public boolean isCanceled() {
    return pm.isCanceled();
  }

  @Override
  public void setCanceled() {
    pm.setCanceled(true);
  }

  @Override
  public void subTask(String name) {
    pm.subTask(name);
  }

  @Override
  public void worked(int work) {
    pm.worked(work);
  }
}
