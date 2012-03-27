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
package com.google.dart.tools.ui.internal.util;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUIMessages;
import com.google.dart.tools.ui.internal.text.DartStatusConstants;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;

/**
 * The default exception handler shows an error dialog when one of its handle methods is called. If
 * the passed exception is a <code>CoreException</code> an error dialog pops up showing the
 * exception's status information. For a <code>InvocationTargetException</code> a normal message
 * dialog pops up showing the exception's message. Additionally the exception is written to the
 * platform log.
 */
public class ExceptionHandler {

  private static ExceptionHandler fgInstance = new ExceptionHandler();

  /**
   * Handles the given <code>CoreException</code>.
   * 
   * @param e the <code>CoreException</code> to be handled
   * @param parent the dialog window's parent shell
   * @param title the dialog window's window title
   * @param message message to be displayed by the dialog window
   */
  public static void handle(CoreException e, Shell parent, String title, String message) {
    fgInstance.perform(e, parent, title, message);
  }

  /**
   * Handles the given <code>CoreException</code>. The workbench shell is used as a parent for the
   * dialog window.
   * 
   * @param e the <code>CoreException</code> to be handled
   * @param title the dialog window's window title
   * @param message message to be displayed by the dialog window
   */
  public static void handle(CoreException e, String title, String message) {
    handle(e, DartToolsPlugin.getActiveWorkbenchShell(), title, message);
  }

  /**
   * Handles the given <code>InvocationTargetException</code>.
   * 
   * @param e the <code>InvocationTargetException</code> to be handled
   * @param parent the dialog window's parent shell
   * @param title the dialog window's window title
   * @param message message to be displayed by the dialog window
   */
  public static void handle(InvocationTargetException e, Shell parent, String title, String message) {
    fgInstance.perform(e, parent, title, message);
  }

  /**
   * Handles the given <code>InvocationTargetException</code>. The workbench shell is used as a
   * parent for the dialog window.
   * 
   * @param e the <code>InvocationTargetException</code> to be handled
   * @param title the dialog window's window title
   * @param message message to be displayed by the dialog window
   */
  public static void handle(InvocationTargetException e, String title, String message) {
    handle(e, DartToolsPlugin.getActiveWorkbenchShell(), title, message);
  }

  /**
   * Handles the given <code>IStatus</code>. The workbench shell is used as a parent for the dialog
   * window.
   * 
   * @param status the <code>IStatus</code> to be handled
   * @param title the dialog window's window title
   * @param message message to be displayed by the dialog window
   */
  public static void handle(IStatus status, String title, String message) {
    fgInstance.perform(status, DartToolsPlugin.getActiveWorkbenchShell(), title, message);
  }

  /**
   * Logs the given exception using the platform's logging mechanism. The exception is logged as an
   * error with the error code <code>JavaStatusConstants.INTERNAL_ERROR</code>.
   */
  public static void log(Throwable t, String message) {
    DartToolsPlugin.log(new Status(IStatus.ERROR, DartToolsPlugin.getPluginId(),
        DartStatusConstants.INTERNAL_ERROR, message, t));
  }

  protected void perform(CoreException e, Shell shell, String title, String message) {
    DartToolsPlugin.log(e);
    IStatus status = e.getStatus();
    if (status != null) {
      ErrorDialog.openError(shell, title, message, status);
    } else {
      displayMessageDialog(e, e.getMessage(), shell, title, message);
    }
  }

  protected void perform(InvocationTargetException e, Shell shell, String title, String message) {
    Throwable target = e.getTargetException();
    if (target instanceof CoreException) {
      perform((CoreException) target, shell, title, message);
    } else {
      DartToolsPlugin.log(e);
      if (e.getMessage() != null && e.getMessage().length() > 0) {
        displayMessageDialog(e, e.getMessage(), shell, title, message);
      } else {
        displayMessageDialog(e, target.getMessage(), shell, title, message);
      }
    }
  }

  protected void perform(IStatus status, Shell shell, String title, String message) {
    DartToolsPlugin.log(status);
    ErrorDialog.openError(shell, title, message, status);
  }

  private void displayMessageDialog(Throwable t, String exceptionMessage, Shell shell,
      String title, String message) {
    StringWriter msg = new StringWriter();
    if (message != null) {
      msg.write(message);
      msg.write("\n\n"); //$NON-NLS-1$
    }
    if (exceptionMessage == null || exceptionMessage.length() == 0) {
      msg.write(DartUIMessages.ExceptionDialog_seeErrorLogMessage);
    } else {
      msg.write(exceptionMessage);
    }
    MessageDialog.openError(shell, title, msg.toString());
  }
}
