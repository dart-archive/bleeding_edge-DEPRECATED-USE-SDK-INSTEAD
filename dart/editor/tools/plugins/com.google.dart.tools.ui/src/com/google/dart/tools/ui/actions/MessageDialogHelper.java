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

package com.google.dart.tools.ui.actions;

import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * A helper class to wrap calls to MessageDialog.
 */
public class MessageDialogHelper {

  /**
   * Convenience method to open a standard information dialog. If this application is running
   * headlessly, log an error instead.
   * 
   * @param parent the parent shell of the dialog, or <code>null</code> if none
   * @param title the dialog's title, or <code>null</code> if none
   * @param message the message
   */
  public static void openInformation(Shell shell, String title, String message) {
    if (ErrorDialog.AUTOMATED_MODE) {
      DartToolsPlugin.logErrorMessage(title + ": " + message);
    } else {
      MessageDialog.openInformation(shell, title, message);
    }
  }

}
