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
package com.google.dart.ui.test.util;

import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Scrollable;

/**
 * Event emulator used for send different mouse events to given SWT control.
 */
public class EventSender {
  /**
   * Posts low-level {@link SWT.MouseDown} event.
   */
  public static void postMouseDown(int button) {
    postMouseDownUp(SWT.MouseDown, button);
  }

  /**
   * Posts low-level {@link SWT.MouseMove} event with absolute coordinates.
   */
  public static void postMouseMoveAbs(int x, int y) {
    Event event = new Event();
    event.type = SWT.MouseMove;
    event.x = x;
    event.y = y;
    postEvent(event);
  }

  /**
   * Posts low-level {@link SWT.MouseMove} event with absolute coordinates.
   */
  public static void postMouseMoveAbs(Point p) {
    postMouseMoveAbs(p.x, p.y);
  }

  /**
   * Posts low-level {@link SWT.MouseUp} event.
   */
  public static void postMouseUp(int button) {
    postMouseDownUp(SWT.MouseUp, button);
  }

  public static void sendCharacter(boolean shift, char character) {
    if (shift) {
      postKeyEvent(SWT.KeyDown, SWT.SHIFT, (char) 0);
    }
    postKeyEvent(SWT.KeyDown, 0, character);
    postKeyEvent(SWT.KeyUp, 0, character);
    if (shift) {
      postKeyEvent(SWT.KeyUp, SWT.SHIFT, (char) 0);
    }
  }

  public static void sendCharacter(char character) {
    sendCharacter(Character.isUpperCase(character), character);
  }

  public static void sendKey(int key) {
    postKeyEvent(SWT.KeyDown, key, (char) 0);
    postKeyEvent(SWT.KeyUp, key, (char) 0);
  }

  /**
   * Sends "key" event with modifiers.
   */
  public static void sendKey(int modifiers, int key) {
    postModifiers(SWT.KeyDown, modifiers);
    try {
      postKeyEvent(SWT.KeyDown, key, (char) 0);
      postKeyEvent(SWT.KeyUp, key, (char) 0);
    } finally {
      postModifiers(SWT.KeyUp, modifiers);
    }
  }

  public static void sendText(String text) {
    char[] charArray = text.toCharArray();
    for (int i = 0; i < charArray.length; i++) {
      char c = charArray[i];
      sendCharacter(c);
    }
  }

  /**
   * Waits given number of milliseconds and runs events loop every 1 millisecond.<br>
   * At least one events loop will be executed.
   */
  public static void waitEventLoop(int time) {
    try {
      long start = System.currentTimeMillis();
      do {
        Thread.sleep(0);
        while (Display.getCurrent().readAndDispatch()) {
          // do nothing
        }
      } while (System.currentTimeMillis() - start < time);
    } catch (Throwable e) {
      throw ExecutionUtils.propagate(e);
    }
  }

  /**
   * Check that the current {@link Thread} is the UI thread and posts {@link Event} into the
   * {@link Display}.
   */
  private static void postEvent(Event event) {
    Display display = Display.getCurrent();
    Assert.isNotNull(display, "Events can only be sent from the UI thread");
    display.post(event);
  }

  /**
   * Posts low-level {@link SWT.KeyDown} or {@link SWT.KeyUp} event.
   */
  private static void postKeyEvent(int type, int keyCode, char character) {
    Event event = new Event();
    event.type = type;
    event.keyCode = keyCode;
    event.character = character;
    postEvent(event);
  }

  /**
   * Posts modifiers up/down event.
   */
  private static void postModifiers(int event, int modifiers) {
    if ((modifiers & SWT.SHIFT) != 0) {
      postKeyEvent(event, SWT.SHIFT, (char) 0);
    }
    if ((modifiers & SWT.CTRL) != 0) {
      postKeyEvent(event, SWT.CTRL, (char) 0);
    }
    if ((modifiers & SWT.ALT) != 0) {
      postKeyEvent(event, SWT.CTRL, (char) 0);
    }
  }

  /**
   * Posts low-level {@link SWT.MouseDown} or {@link SWT.MouseUp} event.
   */
  private static void postMouseDownUp(int type, int button) {
    Event event = new Event();
    event.type = type;
    event.button = button;
    postEvent(event);
  }

  private static final void updateStateMask(Event event, int button) {
    switch (button) {
      case 1:
        event.stateMask |= SWT.BUTTON1;
        break;
      case 2:
        event.stateMask |= SWT.BUTTON2;
        break;
      case 3:
        event.stateMask |= SWT.BUTTON3;
        break;
      case 4:
        event.stateMask |= SWT.BUTTON4;
        break;
      case 5:
        event.stateMask |= SWT.BUTTON5;
        break;
    }
  }

  private final Control control;

  private int stateMask;
  private int dragButton;
  private int lastDragX;
  private int lastDragY;

  /**
   * Constructor to create event emulator for given <code>control</code>.
   */
  public EventSender(Control control) {
    this.control = control;
  }

  /**
   * Emulates mouse click using button <code>1</code> in last location.
   */
  public void click() {
    click(1);
  }

  /**
   * Emulates mouse click in last location.
   */
  public void click(int button) {
    click(lastDragX, lastDragY, button);
  }

  /**
   * Emulate mouse click use given location <code>(x, y)</code> and <code>button</code>.
   */
  public void click(int x, int y, int button) {
    Event event = createEvent(x, y, button);
    control.notifyListeners(SWT.MouseDown, event);
    updateStateMask(event, button);
    control.notifyListeners(SWT.MouseUp, event);
  }

  /**
   * Emulate mouse click use given location and <code>button</code>.
   */
  public void click(Point location, int button) {
    click(location.x, location.y, button);
  }

  public Event createKeyEvent(int key, char c) {
    Event event = new Event();
    event.widget = control;
    event.stateMask = stateMask;
    event.keyCode = key;
    event.character = c;
    return event;
  }

  /**
   * Emulates CTRL key down.
   */
  public void ctrlDown() {
    setStateMask(SWT.CTRL);
  }

  /**
   * Emulates CTRL key up.
   */
  public void ctrlUp() {
    setStateMask(SWT.NONE);
  }

  /**
   * Emulate mouse double click use given location <code>(x, y)</code> and <code>button</code>.
   */
  public void doubleClick(int x, int y, int button) {
    Event event = createEvent(x, y, button);
    control.notifyListeners(SWT.MouseDown, event);
    updateStateMask(event, button);
    control.notifyListeners(SWT.MouseUp, event);
    event.stateMask = stateMask;
    control.notifyListeners(SWT.MouseDown, event);
    control.notifyListeners(SWT.MouseDoubleClick, event);
    updateStateMask(event, button);
    control.notifyListeners(SWT.MouseUp, event);
  }

  /**
   * Emulate mouse drag to given location <code>(x, y)</code>.
   */
  public void dragTo(int x, int y) {
    saveLastMouseLocation(x, y);
    // send event
    Event event = createEvent(x, y, dragButton);
    updateStateMask(event, dragButton);
    control.notifyListeners(SWT.MouseMove, event);
    // process "async" runnables
    waitEventLoop(0);
  }

  /**
   * Ending emulate operation mouse drag.
   */
  public void endDrag() {
    Event event = createEvent(lastDragX, lastDragY, dragButton);
    updateStateMask(event, dragButton);
    control.notifyListeners(SWT.MouseUp, event);
    dragButton = 0;
    lastDragX = 0;
    lastDragY = 0;
  }

  public void keyDown(int key) {
    keyDown(key, (char) key);
  }

  public void keyDown(int key, char c) {
    Event event = createKeyEvent(key, c);
    control.notifyListeners(SWT.KeyDown, event);
  }

  public void keyUp(int key) {
    keyUp(key, (char) key);
  }

  public void keyUp(int key, char c) {
    Event event = createKeyEvent(key, c);
    control.notifyListeners(SWT.KeyUp, event);
  }

  /**
   * Emulate mouse enter to given location <code>(x, y)</code>.
   */
  public void mouseEnter(int x, int y) {
    Event event = createEvent(x, y, 0);
    control.notifyListeners(SWT.MouseEnter, event);
  }

  /**
   * Sends {@link SWT#MouseHover} event with given location.
   */
  public void mouseHover(int x, int y) {
    Event event = createEvent(x, y, 0);
    control.notifyListeners(SWT.MouseHover, event);
  }

  /**
   * Sends {@link SWT#MouseHover} event with given location.
   */
  public void mouseHover(Point location) {
    mouseEnter(location.x, location.y);
  }

  /**
   * Emulate mouse move to given location <code>(x, y)</code>.
   */
  public EventSender moveTo(int x, int y) {
    saveLastMouseLocation(x, y);
    // send event
    Event event = createEvent(x, y, 0);
    control.notifyListeners(SWT.MouseMove, event);
    // process "async" runnables
    waitEventLoop(0);
    return this;
  }

  /**
   * Posts low-level {@link SWT.MouseMove} event with coordinates relative to control.
   */
  public void postMouseMove(int x, int y) {
    Display display = Display.getCurrent();
    Point p = display.map(control, null, x, y);
    postMouseMoveAbs(p.x, p.y);
  }

  public void postMouseMove(int x, int y, int button) {
    Display display = Display.getCurrent();
    Point p = display.map(control, null, x, y);
    // post event
    Event event = new Event();
    event.type = SWT.MouseMove;
    event.x = p.x;
    event.y = p.y;
    event.button = button;
    postEvent(event);
  }

  /**
   * Posts low-level {@link SWT.MouseMove} event with coordinates relative to control.
   */
  public void postMouseMove(Point p) {
    postMouseMove(p.x, p.y);
  }

  public void setStateMask(int stateMask) {
    this.stateMask = stateMask;
  }

  /**
   * Scrolls vertical {@link ScrollBar} and sends {@link SWT#Selection} event.
   */
  public void setVerticalBarSelection(int selection) {
    ScrollBar verticalBar = ((Scrollable) control).getVerticalBar();
    verticalBar.setSelection(selection);
    verticalBar.notifyListeners(SWT.Selection, new Event());
  }

  /**
   * Start emulate operation drag use given location <code>(x, y)</code> and <code>button</code>.
   */
  public void startDrag(int x, int y, int button) {
    saveLastMouseLocation(x, y);
    dragButton = button;
    //
    Event event = createEvent(x, y, button);
    control.notifyListeners(SWT.MouseDown, event);
  }

  private Event createEvent(int x, int y, int button) {
    Event event = new Event();
    event.widget = control;
    event.stateMask = stateMask;
    event.button = button;
    event.x = x;
    event.y = y;
    return event;
  }

  /**
   * Remembers this mouse location as last.
   */
  private void saveLastMouseLocation(int x, int y) {
    lastDragX = x;
    lastDragY = y;
  }
}
