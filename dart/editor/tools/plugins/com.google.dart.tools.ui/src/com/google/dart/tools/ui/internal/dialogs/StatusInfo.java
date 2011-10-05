/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.dialogs;

import com.google.dart.tools.ui.DartUI;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;

/**
 * A settable IStatus. Can be an error, warning, info or ok. For error, info and warning states, a
 * message describes the problem.
 */
public class StatusInfo implements IStatus {

  public static final IStatus OK_STATUS = new StatusInfo();

  private String fStatusMessage;
  private int fSeverity;

  /**
   * Creates a status set to OK (no message)
   */
  public StatusInfo() {
    this(OK, null);
  }

  /**
   * Creates a status .
   * 
   * @param severity The status severity: ERROR, WARNING, INFO and OK.
   * @param message The message of the status. Applies only for ERROR, WARNING and INFO.
   */
  public StatusInfo(int severity, String message) {
    fStatusMessage = message;
    fSeverity = severity;
  }

  /**
   * Returns always an empty array.
   * 
   * @see IStatus#getChildren()
   */
  @Override
  public IStatus[] getChildren() {
    return new IStatus[0];
  }

  /**
   * Returns always the error severity.
   * 
   * @see IStatus#getCode()
   */
  @Override
  public int getCode() {
    return fSeverity;
  }

  /**
   * Returns always <code>null</code>.
   * 
   * @see IStatus#getException()
   */
  @Override
  public Throwable getException() {
    return null;
  }

  /**
   * @see IStatus#getMessage
   */
  @Override
  public String getMessage() {
    return fStatusMessage;
  }

  /*
   * @see IStatus#getPlugin()
   */
  @Override
  public String getPlugin() {
    return DartUI.ID_PLUGIN;
  }

  /*
   * @see IStatus#getSeverity()
   */
  @Override
  public int getSeverity() {
    return fSeverity;
  }

  /**
   * Returns if the status' severity is ERROR.
   */
  public boolean isError() {
    return fSeverity == IStatus.ERROR;
  }

  /**
   * Returns if the status' severity is INFO.
   */
  public boolean isInfo() {
    return fSeverity == IStatus.INFO;
  }

  /**
   * Returns always <code>false</code>.
   * 
   * @see IStatus#isMultiStatus()
   */
  @Override
  public boolean isMultiStatus() {
    return false;
  }

  /**
   * Returns if the status' severity is OK.
   */
  @Override
  public boolean isOK() {
    return fSeverity == IStatus.OK;
  }

  /**
   * Returns if the status' severity is WARNING.
   */
  public boolean isWarning() {
    return fSeverity == IStatus.WARNING;
  }

  /*
   * @see IStatus#matches(int)
   */
  @Override
  public boolean matches(int severityMask) {
    return (fSeverity & severityMask) != 0;
  }

  /**
   * Sets the status to ERROR.
   * 
   * @param errorMessage The error message (can be empty, but not null)
   */
  public void setError(String errorMessage) {
    Assert.isNotNull(errorMessage);
    fStatusMessage = errorMessage;
    fSeverity = IStatus.ERROR;
  }

  /**
   * Sets the status to INFO.
   * 
   * @param infoMessage The info message (can be empty, but not null)
   */
  public void setInfo(String infoMessage) {
    Assert.isNotNull(infoMessage);
    fStatusMessage = infoMessage;
    fSeverity = IStatus.INFO;
  }

  /**
   * Sets the status to OK.
   */
  public void setOK() {
    fStatusMessage = null;
    fSeverity = IStatus.OK;
  }

  /**
   * Sets the status to WARNING.
   * 
   * @param warningMessage The warning message (can be empty, but not null)
   */
  public void setWarning(String warningMessage) {
    Assert.isNotNull(warningMessage);
    fStatusMessage = warningMessage;
    fSeverity = IStatus.WARNING;
  }

  /**
   * Returns a string representation of the status, suitable for debugging purposes only.
   */
  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("StatusInfo "); //$NON-NLS-1$
    if (fSeverity == OK) {
      buf.append("OK"); //$NON-NLS-1$
    } else if (fSeverity == ERROR) {
      buf.append("ERROR"); //$NON-NLS-1$
    } else if (fSeverity == WARNING) {
      buf.append("WARNING"); //$NON-NLS-1$
    } else if (fSeverity == INFO) {
      buf.append("INFO"); //$NON-NLS-1$
    } else {
      buf.append("severity="); //$NON-NLS-1$
      buf.append(fSeverity);
    }
    buf.append(": "); //$NON-NLS-1$
    buf.append(fStatusMessage);
    return buf.toString();
  }
}
