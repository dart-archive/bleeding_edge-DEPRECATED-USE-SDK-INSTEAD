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
package com.google.dart.ui.test.util;

import com.google.dart.ui.test.Condition;
import com.google.dart.ui.test.WaitTimedOutException;
import com.google.dart.ui.test.internal.runtime.ConditionHandler;
import com.google.dart.ui.test.matchers.WidgetMatcher;
import com.google.dart.ui.test.runnable.Result;
import com.google.dart.ui.test.runnable.UIThreadRunnable;
import com.google.dart.ui.test.runnable.VoidResult;

import static com.google.dart.ui.test.matchers.WidgetMatchers.and;
import static com.google.dart.ui.test.matchers.WidgetMatchers.ofClass;
import static com.google.dart.ui.test.matchers.WidgetMatchers.withText;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

/**
 * Helper for testing SWT UI from non-UI thread.
 */
public class UiContext2 {
  /**
   * Sends {@link SWT#Selection} event to given {@link Widget}.
   */
  public static void click(final Widget widget) {
    UIThreadRunnable.syncExec(new VoidResult() {
      @Override
      public void run() {
        widget.notifyListeners(SWT.Selection, new Event());
      }
    });
  }

  /**
   * Clicks the {@link Button} with matching text.
   */
  public static void clickButton(Widget start, String text) {
    Button button = findButton(start, text);
    Assert.isNotNull(button, "Cannot find button: " + text);
    click(button);
  }

  /**
   * @return the {@link Button} with matching text.
   */
  public static Button findButton(Widget start, String text) {
    return findWidget(start, and(ofClass(Button.class), withText(text)));
  }

  /**
   * @return the {@link Text} widget that has {@link Label} with given text, may be {@code null}.
   */
  public static Text findTextByLabel(Widget start, String labelText) {
    return (Text) findWidgetByLabel(start, labelText);
  }

  /**
   * @return the matching {@link Widget}, may be {@code null}.
   */
  public static <T extends Widget> T findWidget(final Widget start, final WidgetMatcher matcher) {
    return UIThreadRunnable.syncExec(new Result<T>() {
      @Override
      public T run() {
        return WidgetFinder.findWidget(start, matcher);
      }
    });
  }

  /**
   * @return the {@link Widget} that has {@link Label} with given text, may be {@code null}.
   */
  public static <T extends Widget> T findWidgetByLabel(final Widget start, final String labelText) {
    return UIThreadRunnable.syncExec(new Result<T>() {
      @Override
      public T run() {
        return WidgetFinder.findWidgetByLabel(start, labelText);
      }
    });
  }

  /**
   * @return the {@link Button#getSelection()} result.
   */
  public static boolean getSelection(final Button button) {
    return UIThreadRunnable.syncExec(new Result<Boolean>() {
      @Override
      public Boolean run() {
        return button.getSelection();
      }
    });
  }

  /**
   * @return the text of the given {@link Widget}, may be {@code null} is this type of the
   *         {@link Widget} has no text.
   */
  public static String getText(final Widget widget) {
    return UIThreadRunnable.syncExec(new Result<String>() {
      @Override
      public String run() {
        if (widget instanceof Button) {
          return ((Button) widget).getText();
        }
        if (widget instanceof Label) {
          return ((Label) widget).getText();
        }
        if (widget instanceof StyledText) {
          return ((StyledText) widget).getText();
        }
        if (widget instanceof Text) {
          return ((Text) widget).getText();
        }
        return null;
      }
    });
  }

  /**
   * Asynchronously runs the given {@link IAction}.
   */
  public static void runAction(final IAction action) {
    UIThreadRunnable.asyncExec(new VoidResult() {
      @Override
      public void run() {
        action.run();
      }
    });
  }

  /**
   * Calls {@link Button#setSelection(boolean)}.
   */
  public static void setSelection(final Button button, final boolean selection) {
    UIThreadRunnable.syncExec(new VoidResult() {
      @Override
      public void run() {
        button.setSelection(selection);
        click(button);
      }
    });
  }

  /**
   * Sets text for the given {@link Text} widget.
   */
  public static void setText(final Text textWidget, final String text) {
    UIThreadRunnable.syncExec(new VoidResult() {
      @Override
      public void run() {
        textWidget.setText(text);
      }
    });
  }

  /**
   * Sets the text for {@link Text} widget that has {@link Label} with given text.
   */
  public static void setTextByLabel(Widget start, String labelText, String newText) {
    Text textWidget = findTextByLabel(start, labelText);
    setText(textWidget, newText);
  }

  /**
   * Waits until the given {@link IAction} will enabled.
   */
  public static void waitForActionEnabled(final IAction action) {
    ConditionHandler.DEFAULT.waitFor(new Condition() {
      @Override
      public boolean test() {
        return action.isEnabled();
      }
    });
  }

  /**
   * Waits for the {@link Shell} with matching text.
   */
  public static Shell waitForShell(final String text) throws WaitTimedOutException {
    final Shell foundShell[] = {null};
    ConditionHandler.DEFAULT.waitFor(new Condition() {
      @Override
      public boolean test() {
        UIThreadRunnable.syncExec(new VoidResult() {
          @Override
          public void run() {
            Display display = Display.getCurrent();
            for (Shell shell : display.getShells()) {
              if (text.equals(shell.getText())) {
                foundShell[0] = shell;
              }
            }
          }
        });
        return foundShell[0] != null;
      }

      @Override
      public String toString() {
        return "Shell " + text;
      }
    });
    return foundShell[0];
  }

  /**
   * Waits until the given {@link Shell} is closed.
   */
  public static void waitForShellClosed(final Shell shell) {
    ConditionHandler.DEFAULT.waitFor(new Condition() {
      @Override
      public boolean test() {
        return UIThreadRunnable.syncExec(new Result<Boolean>() {
          @Override
          public Boolean run() {
            return shell.isDisposed();
          }
        });
      }
    });
  }
}
