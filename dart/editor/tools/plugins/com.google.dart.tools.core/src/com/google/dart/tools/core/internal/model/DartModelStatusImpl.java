/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.model;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartModelStatus;
import com.google.dart.tools.core.model.DartModelStatusConstants;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Instances of the class <code>DartModelStatusImpl</code> implement a Dart model status that can be
 * used when creating exceptions.
 * 
 * @coverage dart.tools.core.model
 */
public class DartModelStatusImpl extends Status implements DartModelStatus,
    DartModelStatusConstants {
  /**
   * An array containing the children of this status object.
   */
  private IStatus[] children = NO_CHILDREN;

  /**
   * An empty array of status objects used when a status has no children.
   */
  private final static IStatus[] NO_CHILDREN = new IStatus[0];

  /**
   * Singleton OK object
   */
  public static final DartModelStatusImpl VERIFIED_OK = new DartModelStatusImpl(OK, OK);

  /**
   * Constructs a Dart model status with no corresponding elements.
   */
  public DartModelStatusImpl(int severity, int code) {
    super(severity, DartCore.PLUGIN_ID, code, "DartModelStatus", null); //$NON-NLS-1$
  }

  /**
   * Constructs a Dart model status with no corresponding elements.
   */
  public DartModelStatusImpl(int severity, int code, String message) {
    super(severity, DartCore.PLUGIN_ID, code, message, null); //$NON-NLS-1$
  }

  public DartModelStatusImpl(int code, String message) {
    super(Status.ERROR, DartCore.PLUGIN_ID, code, message, null);
  }

  public DartModelStatusImpl(int code, Throwable exception) {
    super(Status.ERROR, DartCore.PLUGIN_ID, code, exception.getMessage(), exception);
  }

  public DartModelStatusImpl(String message, Throwable exception) {
    super(Status.ERROR, DartCore.PLUGIN_ID, message, exception);
  }

  public DartModelStatusImpl(Throwable exception) {
    super(Status.ERROR, DartCore.PLUGIN_ID, CORE_EXCEPTION, exception.getMessage(), exception);
  }

  // public DartModelStatusImpl(int severity, String pluginId, int code,
  // String message, Throwable exception) {
  // super(severity, pluginId, code, message, exception);
  // }

  // public DartModelStatusImpl(int severity, String pluginId, String message) {
  // super(severity, pluginId, message);
  // }

  // public DartModelStatusImpl(int severity, String pluginId, String message,
  // Throwable exception) {
  // super(severity, pluginId, message, exception);
  // }

  @Override
  public IStatus[] getChildren() {
    return children;
  }

  @Override
  public int getSeverity() {
    if (this.children == NO_CHILDREN) {
      return super.getSeverity();
    }
    int severity = -1;
    int count = children.length;
    for (int i = 0; i < count; i++) {
      int childrenSeverity = children[i].getSeverity();
      if (childrenSeverity > severity) {
        severity = childrenSeverity;
      }
    }
    return severity;
  }

  @Override
  public boolean isDoesNotExist() {
    return getCode() == ELEMENT_DOES_NOT_EXIST;
  }

  @Override
  public boolean isMultiStatus() {
    return this.children != NO_CHILDREN;
  }

  @Override
  public boolean isOK() {
    return getCode() == OK;
  }

  @Override
  public boolean matches(int mask) {
    if (!isMultiStatus()) {
      return matches(this, mask);
    } else {
      for (int i = 0, max = this.children.length; i < max; i++) {
        if (matches((DartModelStatusImpl) this.children[i], mask)) {
          return true;
        }
      }
      return false;
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Dart Model Status ["); //$NON-NLS-1$
    builder.append(getMessage());
    builder.append("]"); //$NON-NLS-1$
    return builder.toString();
  }

  private int getBits() {
    int severity = 1 << (getCode() % 100 / 33);
    int category = 1 << ((getCode() / 100) + 3);
    return severity | category;
  }

  /**
   * Helper for matches(int).
   */
  private boolean matches(DartModelStatusImpl status, int mask) {
    int severityMask = mask & 0x7;
    int categoryMask = mask & ~0x7;
    int bits = status.getBits();
    return ((severityMask == 0) || (bits & severityMask) != 0)
        && ((categoryMask == 0) || (bits & categoryMask) != 0);
  }
}
