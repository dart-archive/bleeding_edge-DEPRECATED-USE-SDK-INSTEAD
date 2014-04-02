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
package com.google.dart.engine.internal.task;

import com.google.dart.engine.context.AnalysisException;

/**
 * The unique instances of the class {@code WaitForAsyncTask} represents a state in which there is
 * no analysis work that can be done until some asynchronous task (such as IO) has completed, but
 * where analysis is not yet complete.
 */
public class WaitForAsyncTask extends AnalysisTask {
  /**
   * The unique instance of this class.
   */
  private static final WaitForAsyncTask UniqueInstance = new WaitForAsyncTask();

  /**
   * Return the unique instance of this class.
   * 
   * @return the unique instance of this class
   */
  public static WaitForAsyncTask getInstance() {
    return UniqueInstance;
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private WaitForAsyncTask() {
    super(null);
  }

  @Override
  public <E> E accept(AnalysisTaskVisitor<E> visitor) throws AnalysisException {
    // There are no results to report.
    return null;
  }

  @Override
  protected String getTaskDescription() {
    return "Waiting for async analysis";
  }

  @Override
  protected void internalPerform() throws AnalysisException {
    // There is no work to be done.
  }
}
