/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.core.model;

import com.google.dart.tools.core.internal.model.DartModelStatusImpl;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Instances of the class <code>DartModelException</code> represent a failure in the Dart model.
 * Dart model exceptions contain a Dart-specific status object describing the cause of the
 * exception.
 */
public class DartModelException extends CoreException {
  private static final long serialVersionUID = 1L;

  /**
   * The code exception that caused this model exception, or <code>null</code> if this exception was
   * not caused by a model exception.
   */
  private CoreException nestedCoreException;

  /**
   * Creates a Dart model exception for the given <code>CoreException</code>. Equivalent to
   * <code>DartModelException(exception, DartModelStatusConstants.CORE_EXCEPTION</code> .
   * 
   * @param exception the <code>CoreException</code>
   */
  public DartModelException(CoreException exception) {
    super(exception.getStatus());
    this.nestedCoreException = exception;
  }

  /**
   * Creates a Dart model exception for the given Dart-specific status object.
   * 
   * @param status the Dart-specific status object
   */
  public DartModelException(DartModelStatus status) {
    super(status);
  }

  /**
   * Creates a Dart model exception that wrappers the given <code>Throwable</code>. The exception
   * contains a Dart-specific status object with severity {@link IStatus.ERROR} and the given status
   * code.
   * 
   * @param e the <code>Throwable</code>
   * @param code one of the Dart-specific status codes declared in
   *          <code>DartModelStatusConstants</code>
   */
  public DartModelException(Throwable e, int code) {
    this(new DartModelStatusImpl(code, e));
  }

  /**
   * Return the Dart model status object for this exception.
   * 
   * @return a status object
   */
  public DartModelStatus getDartModelStatus() {
    IStatus status = this.getStatus();
    if (status instanceof DartModelStatus) {
      return (DartModelStatus) status;
    } else {
      // A regular IStatus is created only in the case of a CoreException.
      // See bug 13492 Should handle JavaModelExceptions that contains
      // CoreException more gracefully
      return new DartModelStatusImpl(this.nestedCoreException);
    }
  }

  /**
   * Return the underlying <code>Throwable</code> that caused the failure.
   * 
   * @return the wrapped <code>Throwable</code>, or <code>null</code> if the direct case of the
   *         failure was at the Dart model layer
   */
  public Throwable getException() {
    if (this.nestedCoreException == null) {
      return getStatus().getException();
    } else {
      return this.nestedCoreException;
    }
  }

  /**
   * Return <code>true</code> if this exception indicates that a Dart model element does not exist.
   * Such exceptions have a status with a code of
   * {@link DartModelStatusConstants.ELEMENT_DOES_NOT_EXIST} or
   * {@link DartModelStatusConstants.ELEMENT_NOT_ON_CLASSPATH}. This is a convenience method.
   * 
   * @return <code>true</code> if this exception indicates that a Dart model element does not exist
   */
  public boolean isDoesNotExist() {
    DartModelStatus javaModelStatus = getDartModelStatus();
    return javaModelStatus != null && javaModelStatus.isDoesNotExist();
  }

  /**
   * Print this exception's stack trace to the given print stream.
   * 
   * @param output the print stream
   */
  @Override
  public void printStackTrace(PrintStream output) {
    synchronized (output) {
      super.printStackTrace(output);
      Throwable throwable = getException();
      if (throwable != null) {
        output.print("Caused by: "); //$NON-NLS-1$
        throwable.printStackTrace(output);
      }
    }
  }

  /**
   * Print this exception's stack trace to the given print writer.
   * 
   * @param output the print writer
   */
  @Override
  public void printStackTrace(PrintWriter output) {
    synchronized (output) {
      super.printStackTrace(output);
      Throwable throwable = getException();
      if (throwable != null) {
        output.print("Caused by: "); //$NON-NLS-1$
        throwable.printStackTrace(output);
      }
    }
  }

  /*
   * Return a printable representation of this exception suitable for debugging purposes only.
   */
  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("Dart Model Exception: "); //$NON-NLS-1$
    if (getException() != null) {
      if (getException() instanceof CoreException) {
        CoreException c = (CoreException) getException();
        buffer.append("Core Exception [code "); //$NON-NLS-1$
        buffer.append(c.getStatus().getCode());
        buffer.append("] "); //$NON-NLS-1$
        buffer.append(c.getStatus().getMessage());
      } else {
        buffer.append(getException().toString());
      }
    } else {
      buffer.append(getStatus().toString());
    }
    return buffer.toString();
  }
}
