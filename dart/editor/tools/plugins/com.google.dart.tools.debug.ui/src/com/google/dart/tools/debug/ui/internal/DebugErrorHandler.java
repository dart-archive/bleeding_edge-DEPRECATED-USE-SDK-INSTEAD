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
package com.google.dart.tools.debug.ui.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Handles displaying errors to the user.
 */
public class DebugErrorHandler {

  /**
   * Display an error dialog to the user.
   * 
   * @param shell the parent shell - can be null
   * @param title the dialog's title
   * @param message the message to show to the user
   * @param status the error status - can be null
   */
  public static void errorDialog(Shell shell, String title, String message, IStatus status) {
    // if the 'message' resource string and the IStatus' message are the same,
    // don't show both in the dialog
    if (status != null && message.equals(status.getMessage())) {
      message = null;
    }
    ErrorDialog.openError(shell, title, message, status);
  }

  /**
   * Display an error dialog to the user.
   * 
   * @param shell the parent shell - can be null
   * @param title the dialog's title
   * @param message the message to show to the user
   * @param exception the exception to display to the user
   */
  public static void errorDialog(Shell shell, String title, String message, Throwable exception) {
    IStatus status;

    if (shell == null) {
      shell = Display.getCurrent().getActiveShell();
    }

    if (exception instanceof CoreException) {
      status = ((CoreException) exception).getStatus();
      // if the 'message' resource string and the IStatus' message are the same,
      // don't show both in the dialog
      if (status != null && message.equals(status.getMessage())) {
        message = null;
      }
    } else {
      status = new Status(
          IStatus.ERROR,
          DartDebugUIPlugin.PLUGIN_ID,
          IDebugUIConstants.INTERNAL_ERROR,
          "Error within Debug UI: ", exception); //$NON-NLS-1$
    }

    ErrorDialog.openError(shell, title, message, status);
  }

  private DebugErrorHandler() {

  }

}
