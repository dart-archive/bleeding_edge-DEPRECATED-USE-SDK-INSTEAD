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
package com.google.dart.tools.ui.actions;

import com.google.dart.tools.ui.instrumentation.UIInstrumentation;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Superclass to support adding instrumentation to jobs. All jobs should subclass this and override
 * doRun with the functionality of the job and making appropriate calls to the instrumentation
 * argument.
 */
public abstract class InstrumentedJob extends Job {

  public InstrumentedJob(String name) {
    super(name);
  }

  @Override
  public final IStatus run(IProgressMonitor monitor) {
    IStatus result;
    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder(this.getClass());
    try {
      result = doRun(monitor, instrumentation);
      instrumentation.metric("Run", "Completed");
      return result;
    } catch (RuntimeException e) {
      instrumentation.record(e);
      throw e;
    } finally {
      instrumentation.log();
    }
  }

  /**
   * Implementing classes should override this method to perform the action for the class
   * 
   * @param instrumentation
   * @param event The event passed with the event, may be null
   * @param instrumentation The instrumentation logger, will not be null
   */
  protected abstract IStatus doRun(IProgressMonitor monitor,
      UIInstrumentationBuilder instrumentation);
}
