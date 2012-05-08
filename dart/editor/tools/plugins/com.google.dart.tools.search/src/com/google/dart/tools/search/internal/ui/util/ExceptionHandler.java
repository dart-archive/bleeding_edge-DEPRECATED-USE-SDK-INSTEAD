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
package com.google.dart.tools.search.internal.ui.util;

import com.google.dart.tools.search.internal.ui.SearchMessages;
import com.google.dart.tools.search.internal.ui.SearchPlugin;
import com.google.dart.tools.search.ui.NewSearchUI;

import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;

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
   * Logs the given exception using the platform's logging mechanism. The exception is logged as an
   * error with the error code <code>JavaStatusConstants.INTERNAL_ERROR</code>.
   * 
   * @param t The exception to log
   * @param message The message to be used for teh status
   */
  public static void log(Throwable t, String message) {
    SearchPlugin.log(new Status(IStatus.ERROR, NewSearchUI.PLUGIN_ID, IStatus.ERROR, message, t));
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
    handle(e, SearchPlugin.getActiveWorkbenchShell(), title, message);
  }

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
   * Handles the given <code>InvocationTargetException</code>. The workbench shell is used as a
   * parent for the dialog window.
   * 
   * @param e the <code>InvocationTargetException</code> to be handled
   * @param title the dialog window's window title
   * @param message message to be displayed by the dialog window
   */
  public static void handle(InvocationTargetException e, String title, String message) {
    handle(e, SearchPlugin.getActiveWorkbenchShell(), title, message);
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

  //---- Hooks for subclasses to control exception handling ------------------------------------

  protected void perform(CoreException e, Shell shell, String title, String message) {
    SearchPlugin.log(e);
    IStatus status = e.getStatus();
    if (status != null) {
      ErrorDialog.openError(shell, title, message, status);
    } else {
      displayMessageDialog(e.getMessage(), shell, title, message);
    }
  }

  protected void perform(InvocationTargetException e, Shell shell, String title, String message) {
    Throwable target = e.getTargetException();
    if (target instanceof CoreException) {
      perform((CoreException) target, shell, title, message);
    } else {
      SearchPlugin.log(e);
      if (e.getMessage() != null && e.getMessage().length() > 0) {
        displayMessageDialog(e.getMessage(), shell, title, message);
      } else {
        displayMessageDialog(target.getMessage(), shell, title, message);
      }
    }
  }

  //---- Helper methods -----------------------------------------------------------------------

  public static void displayMessageDialog(Throwable t, Shell shell, String title, String message) {
    fgInstance.displayMessageDialog(t.getMessage(), shell, title, message);
  }

  public static void displayMessageDialog(Throwable t, String title, String message) {
    displayMessageDialog(t, SearchPlugin.getActiveWorkbenchShell(), title, message);
  }

  private void displayMessageDialog(String exceptionMessage, Shell shell, String title,
      String message) {
    StringWriter msg = new StringWriter();
    if (message != null) {
      msg.write(message);
      msg.write("\n\n"); //$NON-NLS-1$
    }
    if (exceptionMessage == null || exceptionMessage.length() == 0)
      msg.write(SearchMessages.ExceptionDialog_seeErrorLogMessage);
    else
      msg.write(exceptionMessage);
    MessageDialog.openError(shell, title, msg.toString());
  }
}
