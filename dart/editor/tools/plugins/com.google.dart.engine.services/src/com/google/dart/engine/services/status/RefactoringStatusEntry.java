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
package com.google.dart.engine.services.status;

/**
 * An immutable object representing an entry in the list in {@link RefactoringStatus}. A refactoring
 * status entry consists of a severity, a message and a context object.
 */
public class RefactoringStatusEntry {
  private final RefactoringStatusSeverity severity;
  private final String message;
  private final RefactoringStatusContext context;

  public RefactoringStatusEntry(RefactoringStatusSeverity severity, String msg) {
    this(severity, msg, null);
  }

  public RefactoringStatusEntry(RefactoringStatusSeverity severity, String msg,
      RefactoringStatusContext context) {
    this.severity = severity;
    this.message = msg;
    this.context = context;
  }

  /**
   * @return the {@link RefactoringStatusContext} which can be used to show more detailed
   *         information regarding this status entry in the UI. The method may return
   *         <code>null</code> indicating that no context is available.
   */
  public RefactoringStatusContext getContext() {
    return context;
  }

  /**
   * @return the message of the status entry.
   */
  public String getMessage() {
    return message;
  }

  /**
   * @return the severity level.
   */
  public RefactoringStatusSeverity getSeverity() {
    return severity;
  }

  /**
   * Returns whether the entry represents an error or not.
   * 
   * @return <code>true</code> if (severity ==<code>RefactoringStatusSeverity.ERROR</code>).
   */
  public boolean isError() {
    return severity == RefactoringStatusSeverity.ERROR;
  }

  /**
   * Returns whether the entry represents a fatal error or not.
   * 
   * @return <code>true</code> if (severity ==<code>RefactoringStatusSeverity.FATAL</code>)
   */
  public boolean isFatalError() {
    return severity == RefactoringStatusSeverity.FATAL;
  }

  /**
   * Returns whether the entry represents an information or not.
   * 
   * @return <code>true</code> if (severity ==<code>RefactoringStatusSeverity.INFO</code>).
   */
  public boolean isInfo() {
    return severity == RefactoringStatusSeverity.INFO;
  }

  /**
   * Returns whether the entry represents a warning or not.
   * 
   * @return <code>true</code> if (severity ==<code>RefactoringStatusSeverity.WARNING</code>).
   */
  public boolean isWarning() {
    return severity == RefactoringStatusSeverity.WARNING;
  }

  @Override
  public String toString() {
    if (context != null) {
      return severity + ": " + message + "; Context: " + context;
    } else {
      return severity + ": " + message;
    }
  }
}
