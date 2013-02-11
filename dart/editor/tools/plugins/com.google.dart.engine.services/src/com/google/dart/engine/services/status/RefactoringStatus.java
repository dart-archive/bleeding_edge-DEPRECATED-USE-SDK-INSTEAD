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

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Outcome of a condition checking operation.
 */
public class RefactoringStatus {
  /**
   * @return the {@link Enum} value with maximal ordinal.
   */
  private static <T extends Enum<T>> T max(T a, T b) {
    if (b.ordinal() > a.ordinal()) {
      return b;
    }
    return a;
  }

  private RefactoringStatusSeverity severity = RefactoringStatusSeverity.OK;
  private final List<RefactoringStatusEntry> entries = Lists.newArrayList();

  /**
   * Adds a <code>ERROR</code> entry filled with the given message to this status.
   */
  public void addError(String msg) {
    addError(msg, null);
  }

  /**
   * Adds a <code>ERROR</code> entry filled with the given message and status to this status.
   */
  public void addError(String msg, RefactoringStatusContext context) {
    addEntry(new RefactoringStatusEntry(RefactoringStatusSeverity.ERROR, msg, context));
  }

  /**
   * Adds a <code>FATAL</code> entry filled with the given message to this status.
   */
  public void addFatalError(String msg) {
    addFatalError(msg, null);
  }

  /**
   * Adds a <code>FATAL</code> entry filled with the given message and status to this status.
   */
  public void addFatalError(String msg, RefactoringStatusContext context) {
    addEntry(new RefactoringStatusEntry(RefactoringStatusSeverity.FATAL, msg, context));
  }

  /**
   * Adds a <code>WARNING</code> entry filled with the given message to this status.
   */
  public void addWarning(String msg) {
    addWarning(msg, null);
  }

  /**
   * Adds a <code>WARNING</code> entry filled with the given message and status to this status.
   */
  public void addWarning(String msg, RefactoringStatusContext context) {
    addEntry(new RefactoringStatusEntry(RefactoringStatusSeverity.WARNING, msg, context));
  }

  /**
   * @return the RefactoringStatusEntry with the highest severity, or <code>null</code> if no
   *         entries are present.
   */
  public RefactoringStatusEntry getEntryWithHighestSeverity() {
    if (entries.isEmpty()) {
      return null;
    }
    RefactoringStatusEntry result = entries.get(0);
    for (RefactoringStatusEntry entry : entries) {
      if (result.getSeverity().ordinal() < entry.getSeverity().ordinal()) {
        result = entry;
      }
    }
    return result;
  }

  /**
   * @return the message from the {@link RefactoringStatusEntry} with highest severity; may be
   *         <code>null</code> if not entries are present.
   */
  public String getMessage() {
    RefactoringStatusEntry entry = getEntryWithHighestSeverity();
    if (entry == null) {
      return null;
    }
    return entry.getMessage();
  }

  /**
   * @return <code>true</code> if the current severity is <code>
   *  FATAL</code> or <code>ERROR</code>.
   */
  public boolean hasError() {
    return severity == RefactoringStatusSeverity.FATAL
        || severity == RefactoringStatusSeverity.ERROR;
  }

  /**
   * @return <code>true</code> if the current severity is <code>FATAL</code>.
   */
  public boolean hasFatalError() {
    return severity == RefactoringStatusSeverity.FATAL;
  }

  /**
   * @return <code>true</code> if the current severity is <code>
   *  FATAL</code>, <code>ERROR</code>, <code>WARNING</code> or <code>INFO</code>.
   */
  public boolean hasInfo() {
    return severity == RefactoringStatusSeverity.FATAL
        || severity == RefactoringStatusSeverity.ERROR
        || severity == RefactoringStatusSeverity.WARNING
        || severity == RefactoringStatusSeverity.INFO;
  }

  /**
   * @return <code>true</code> if the current severity is <code>
   *  FATAL</code>, <code>ERROR</code> or <code>WARNING</code>.
   */
  public boolean hasWarning() {
    return severity == RefactoringStatusSeverity.FATAL
        || severity == RefactoringStatusSeverity.ERROR
        || severity == RefactoringStatusSeverity.WARNING;
  }

  /**
   * @return <code>true</code> if the severity is <code>OK</code>.
   */
  public boolean isOK() {
    return severity == RefactoringStatusSeverity.OK;
  }

  @Override
  public String toString() {
    StringBuffer buff = new StringBuffer();
    buff.append("<").append(severity.name());
    if (!isOK()) {
      buff.append("\n");
      for (RefactoringStatusEntry entry : entries) {
        buff.append("\t").append(entry).append("\n");
      }
    }
    buff.append(">");
    return buff.toString();
  }

  /**
   * Adds given {@link RefactoringStatusEntry} and updates {@link #severity}.
   */
  private void addEntry(RefactoringStatusEntry entry) {
    entries.add(entry);
    severity = max(severity, entry.getSeverity());
  }
}
