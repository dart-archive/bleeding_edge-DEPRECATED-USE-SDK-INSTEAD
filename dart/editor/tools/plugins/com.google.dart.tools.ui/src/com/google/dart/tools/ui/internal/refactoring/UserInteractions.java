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
package com.google.dart.tools.ui.internal.refactoring;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.RefactoringUI;
import org.eclipse.swt.widgets.Shell;

/**
 * @coverage dart.editor.ui.refactoring.ui
 */
public final class UserInteractions {

  /**
   * Substitute for {@link MessageDialog#openInformation(Shell, String, String)}.
   */
  public interface OpenInformation {
    void open(Shell parent, String title, String message);
  }

  /**
   * Displays fatal {@link RefactoringStatus}.
   */
  public interface ShowStatusDialog {
    boolean open(RefactoringStatus status, Shell parent, String windowTitle);
  }

  static {
    reset();
  }

  private static OpenInformation DEFAULT_OpenInformation = new OpenInformation() {
    @Override
    public void open(Shell parent, String title, String message) {
      MessageDialog.openInformation(parent, title, message);
    }
  };

  private static ShowStatusDialog DEFAULT_ShowStatusDialog = new ShowStatusDialog() {
    @Override
    public boolean open(RefactoringStatus status, Shell parent, String windowTitle) {
      Dialog dialog = RefactoringUI.createRefactoringStatusDialog(
          status,
          parent,
          windowTitle,
          false);
      return dialog.open() == IDialogConstants.CANCEL_ID;
    }
  };

  public static OpenInformation openInformation;
  public static ShowStatusDialog showStatusDialog;

  /**
   * Resets all interfaces to their default implementations.
   */
  public static void reset() {
    openInformation = DEFAULT_OpenInformation;
    showStatusDialog = DEFAULT_ShowStatusDialog;
  }
}
