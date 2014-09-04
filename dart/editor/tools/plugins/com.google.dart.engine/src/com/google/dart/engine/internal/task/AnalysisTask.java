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
package com.google.dart.engine.internal.task;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.internal.context.InternalAnalysisContext;

/**
 * The abstract class {@code AnalysisTask} defines the behavior of objects used to perform an
 * analysis task.
 */
public abstract class AnalysisTask {
  /**
   * The context in which the task is to be performed.
   */
  private InternalAnalysisContext context;

  /**
   * The exception that was thrown while performing this task, or {@code null} if the task completed
   * successfully.
   */
  private AnalysisException thrownException;

  /**
   * Initialize a newly created task to perform analysis within the given context.
   * 
   * @param context the context in which the task is to be performed
   */
  public AnalysisTask(InternalAnalysisContext context) {
    this.context = context;
  }

  /**
   * Use the given visitor to visit this task.
   * 
   * @param visitor the visitor that should be used to visit this task
   * @return the value returned by the visitor
   * @throws AnalysisException if the visitor throws the exception
   */
  public abstract <E> E accept(AnalysisTaskVisitor<E> visitor) throws AnalysisException;

  /**
   * Return the exception that was thrown while performing this task, or {@code null} if the task
   * completed successfully.
   * 
   * @return the exception that was thrown while performing this task
   */
  public AnalysisException getException() {
    return thrownException;
  }

  /**
   * Perform this analysis task and use the given visitor to visit this task after it has completed.
   * 
   * @param visitor the visitor used to visit this task after it has completed
   * @return the value returned by the visitor
   * @throws AnalysisException if the visitor throws the exception
   */
  public <E> E perform(AnalysisTaskVisitor<E> visitor) throws AnalysisException {
    try {
      safelyPerform();
    } catch (AnalysisException exception) {
      thrownException = exception;
      AnalysisEngine.getInstance().getLogger().logInformation(
          "Task failed: " + getTaskDescription(),
          exception);
    }
    return accept(visitor);
  }

  @Override
  public String toString() {
    return getTaskDescription();
  }

  /**
   * Return the context in which the task is to be performed.
   * 
   * @return the context in which the task is to be performed
   */
  protected InternalAnalysisContext getContext() {
    return context;
  }

  /**
   * Return a textual description of this task.
   * 
   * @return a textual description of this task
   */
  protected abstract String getTaskDescription();

  /**
   * Perform this analysis task, protected by an exception handler.
   * 
   * @throws AnalysisException if an exception occurs while performing the task
   */
  protected abstract void internalPerform() throws AnalysisException;

  /**
   * Perform this analysis task, ensuring that all exceptions are wrapped in an
   * {@link AnalysisException}.
   * 
   * @throws AnalysisException if any exception occurs while performing the task
   */
  private void safelyPerform() throws AnalysisException {
    try {
      internalPerform();
    } catch (AnalysisException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new AnalysisException(exception.toString(), exception);
    }
  }
}
