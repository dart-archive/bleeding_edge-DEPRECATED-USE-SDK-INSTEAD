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

import com.google.common.collect.Lists;
import com.google.dart.tools.ui.EventSender;

import static com.google.dart.ui.test.matchers.WidgetMatchers.and;
import static com.google.dart.ui.test.matchers.WidgetMatchers.ofClass;
import static com.google.dart.ui.test.matchers.WidgetMatchers.withText;
import static com.google.dart.ui.test.util.WidgetFinder.findWidget;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import java.util.LinkedList;

/**
 * Helper for testing SWT UI.
 */
public class UiContext {
  /**
   * @return the text of the given {@link Widget}, may be {@code null} is this type of the
   *         {@link Widget} has no text.
   */
  public static String getText(Widget widget) {
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

  /**
   * Runs the events loop for the given number of milliseconds.
   */
  public static void runEventLoop(int ms) {
    long start = System.currentTimeMillis();
    do {
      while (Display.getDefault().readAndDispatch()) {
        // do nothing
      }
      Thread.yield();
    } while (System.currentTimeMillis() - start < ms);
  }

  /**
   * Runs the events loop one time. Usually just to update UI, such as repaint.
   */
  public static void runEventLoopOnce() {
    while (Display.getDefault().readAndDispatch()) {
      // do nothing
    }
  }

  private final Display display;
  private final LinkedList<Shell> shells = Lists.newLinkedList();

  private Shell shell;

  public UiContext() {
    this.display = Display.getCurrent();
  }

  /**
   * Sends {@link SWT#Selection} event to given {@link Widget}.
   */
  public void click(Widget widget) {
    widget.notifyListeners(SWT.Selection, new Event());
  }

  /**
   * Sends {@link SWT#Selection} event to the given {@link Button}.
   */
  public void clickButton(Button button) {
    click(button);
  }

  /**
   * Sends {@link SWT#Selection} event to the {@link Button} with given text.
   */
  public void clickButton(String text) {
    Button button = findButton(text);
    Assert.isNotNull(button, "Can not find button with text |" + text + "|");
    click(button);
  }

  /**
   * Clicks the {@link Table} cell.
   */
  public void clickTableItem(Table table, int itemIndex, int columnIndex) {
    TableItem item = table.getItem(itemIndex);
    Rectangle cellBounds = item.getTextBounds(columnIndex);
    int x = cellBounds.x + 1;
    int y = cellBounds.y + 1;
    new EventSender(table).click(x, y, 1);
  }

  /**
   * @return the {@link Button} with the given text.
   */
  public Button findButton(String text) {
    return findWidget(getShell(), and(ofClass(Button.class), withText(text)));
  }

  /**
   * @return the {@link Shell} with the given text, may be {@code null}.
   */
  public Shell findShell(String text) {
    for (Shell shell : display.getShells()) {
      if (text.equals(shell.getText())) {
        return shell;
      }
    }
    return null;
  }

  /**
   * @return the {@link Table} with the given text.
   */
  public Table findTable() {
    return findWidget(getShell(), ofClass(Table.class));
  }

  /**
   * @return the {@link Text} widget that has {@link Label} with given text, may be {@code null}.
   */
  public Text findTextByLabel(String labelText) {
    return (Text) findWidgetByLabel(labelText);
  }

  /**
   * @return the {@link Widget} that has {@link Label} with given text, may be {@code null}.
   */
  public Widget findWidgetByLabel(String labelText) {
    return WidgetFinder.findWidgetByLabel(getShell(), labelText);
  }

  /**
   * @return the {@link Shell} to use as start to searching {@link Widget}'s.
   */
  public Shell getShell() {
    if (this.shell != null) {
      if (this.shell.isDisposed()) {
        this.shell = null;
      } else {
        return this.shell;
      }
    }
    return display.getShells()[0];
  }

  /**
   * Sends {@link SWT#Selection} event to given {@link Button}.
   */
  public void setButtonSelection(Button button, boolean selection) {
    button.setSelection(selection);
    click(button);
  }

  /**
   * Sends {@link SWT#Selection} event to given {@link Button}.
   */
  public void setButtonSelection(String text, boolean selection) {
    Button button = findButton(text);
    Assert.isNotNull(button, "Can not find button with text |" + text + "|");
    setButtonSelection(button, selection);
  }

  /**
   * Specifies that it is expected that current {@link Shell} was closed, so we return to previous.
   */
  public void shellClosed() {
    shell = shells.removeFirst();
  }

  /**
   * Activates the specified {@link Shell}.
   */
  public void useShell(Shell shell) {
    shells.addFirst(this.shell);
    this.shell = shell;
  }
}
