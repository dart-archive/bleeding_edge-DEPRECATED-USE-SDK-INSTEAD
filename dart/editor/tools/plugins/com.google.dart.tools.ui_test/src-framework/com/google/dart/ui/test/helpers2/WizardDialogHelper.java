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

package com.google.dart.ui.test.helpers2;

import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;
import com.google.dart.ui.test.runnable.UIThreadRunnable;
import com.google.dart.ui.test.runnable.VoidResult;

import static com.google.dart.ui.test.util.UiContext2.getText;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import static org.junit.Assert.assertEquals;

/**
 * Helper for testing wizard dialogs.
 */
public class WizardDialogHelper extends DialogHelper {
  private Label messageTextWidget;

  public WizardDialogHelper(final Shell shell) {
    super(shell);
    UIThreadRunnable.syncExec(new VoidResult() {
      @Override
      public void run() {
        Dialog dialog = (Dialog) shell.getData();
        Object messageBox = ReflectionUtils.getFieldObject(dialog, "fMessageBox");
        messageTextWidget = ReflectionUtils.getFieldObject(messageBox, "fText");
      }
    });
  }

  public void assertMessage(String expected) {
    assertEquals(expected, getMessage());
  }

  public void assertNoMessage() {
    assertMessage("");
  }

  public String getMessage() {
    return getText(messageTextWidget);
  }
}
