package com.google.dart.ui.test.helpers2;

import static com.google.dart.ui.test.util.UiContext2.clickButton;

import org.eclipse.swt.widgets.Shell;

/**
 * Helper for testing dialogs.
 */
public class DialogHelper {
  protected final Shell shell;

  public DialogHelper(Shell shell) {
    this.shell = shell;
  }

  public void closeCancel() {
    clickButton(shell, "Cancel");
  }

  public void closeOK() {
    clickButton(shell, "OK");
  }
}
