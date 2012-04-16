package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;

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
  private final Control m_control;
  private int m_stateMask;
  private int m_dragButton;
  private int m_lastDragX;
  private int m_lastDragY;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Constructor of create event emulator for given <code>control</code>.
   */
  public EventSender(Control control) {
    m_control = control;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // High-Level events emulate
  //
  ////////////////////////////////////////////////////////////////////////////
  public void setStateMask(int stateMask) {
    m_stateMask = stateMask;
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
   * Emulate mouse enter to given location <code>(x, y)</code>.
   */
  public void mouseEnter(int x, int y) {
    Event event = createEvent(x, y, 0);
    m_control.notifyListeners(SWT.MouseEnter, event);
  }

  /**
   * Emulate mouse click use given location and <code>button</code>.
   */
  public void click(Point location, int button) {
    click(location.x, location.y, button);
  }

  /**
   * Emulate mouse click use given location <code>(x, y)</code> and <code>button</code>.
   */
  public void click(int x, int y, int button) {
    Event event = createEvent(x, y, button);
    m_control.notifyListeners(SWT.MouseDown, event);
    updateStateMask(event, button);
    m_control.notifyListeners(SWT.MouseUp, event);
  }

  /**
   * Emulates mouse click in last location.
   */
  public void click(int button) {
    click(m_lastDragX, m_lastDragY, button);
  }

  /**
   * Emulates mouse click using button <code>1</code> in last location.
   */
  public void click() {
    click(1);
  }

  /**
   * Emulate mouse double click use given location <code>(x, y)</code> and <code>button</code>.
   */
  public void doubleClick(int x, int y, int button) {
    Event event = createEvent(x, y, button);
    m_control.notifyListeners(SWT.MouseDown, event);
    updateStateMask(event, button);
    m_control.notifyListeners(SWT.MouseUp, event);
    event.stateMask = m_stateMask;
    m_control.notifyListeners(SWT.MouseDown, event);
    m_control.notifyListeners(SWT.MouseDoubleClick, event);
    updateStateMask(event, button);
    m_control.notifyListeners(SWT.MouseUp, event);
  }

  /**
   * Emulate mouse move to given location <code>(x, y)</code>.
   */
  public EventSender moveTo(int x, int y) {
    saveLastMouseLocation(x, y);
    // send event
    Event event = createEvent(x, y, 0);
    m_control.notifyListeners(SWT.MouseMove, event);
    // process "async" runnables
    waitEventLoop(0);
    return this;
  }

  /**
   * Start emulate operation drag use given location <code>(x, y)</code> and <code>button</code>.
   */
  public void startDrag(int x, int y, int button) {
    saveLastMouseLocation(x, y);
    m_dragButton = button;
    //
    Event event = createEvent(x, y, button);
    m_control.notifyListeners(SWT.MouseDown, event);
  }

//  /**
//   * Emulate mouse drag to given location.
//   */
//  public void dragTo(org.eclipse.wb.draw2d.geometry.Point location) {
//    dragTo(location.x, location.y);
//  }

  /**
   * Emulate mouse drag to given location <code>(x, y)</code>.
   */
  public void dragTo(int x, int y) {
    saveLastMouseLocation(x, y);
    // send event
    Event event = createEvent(x, y, m_dragButton);
    updateStateMask(event, m_dragButton);
    m_control.notifyListeners(SWT.MouseMove, event);
    // process "async" runnables
    waitEventLoop(0);
  }

  /**
   * Ending emulate operation mouse drag.
   */
  public void endDrag() {
    Event event = createEvent(m_lastDragX, m_lastDragY, m_dragButton);
    updateStateMask(event, m_dragButton);
    m_control.notifyListeners(SWT.MouseUp, event);
    m_dragButton = 0;
    m_lastDragX = 0;
    m_lastDragY = 0;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // High level keyboard
  //
  ////////////////////////////////////////////////////////////////////////////
  public void keyDown(int key) {
    keyDown(key, (char) key);
  }

  public void keyUp(int key) {
    keyUp(key, (char) key);
  }

  public void keyDown(int key, char c) {
    Event event = createKeyEvent(key, c);
    m_control.notifyListeners(SWT.KeyDown, event);
  }

  public void keyUp(int key, char c) {
    Event event = createKeyEvent(key, c);
    m_control.notifyListeners(SWT.KeyUp, event);
  }

  public Event createKeyEvent(int key, char c) {
    Event event = new Event();
    event.widget = m_control;
    event.stateMask = m_stateMask;
    event.keyCode = key;
    event.character = c;
    return event;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // UI utils
  //
  ////////////////////////////////////////////////////////////////////////////
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

  ////////////////////////////////////////////////////////////////////////////
  //
  // Scrolling
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Scrolls vertical {@link ScrollBar} and sends {@link SWT#Selection} event.
   */
  public void setVerticalBarSelection(int selection) {
    ScrollBar verticalBar = ((Scrollable) m_control).getVerticalBar();
    verticalBar.setSelection(selection);
    verticalBar.notifyListeners(SWT.Selection, new Event());
  }

  /**
   * Sends {@link SWT#MouseHover} event with given location.
   */
  public void mouseHover(Point location) {
    mouseEnter(location.x, location.y);
  }

  /**
   * Sends {@link SWT#MouseHover} event with given location.
   */
  public void mouseHover(int x, int y) {
    Event event = createEvent(x, y, 0);
    m_control.notifyListeners(SWT.MouseHover, event);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Keyboard
  //
  ////////////////////////////////////////////////////////////////////////////
  public static void sendText(String text) {
    char[] charArray = text.toCharArray();
    for (int i = 0; i < charArray.length; i++) {
      char c = charArray[i];
      sendCharacter(c);
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

  ////////////////////////////////////////////////////////////////////////////
  //
  // Utils
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Remembers this mouse location as last.
   */
  private void saveLastMouseLocation(int x, int y) {
    m_lastDragX = x;
    m_lastDragY = y;
  }

  private Event createEvent(int x, int y, int button) {
    Event event = new Event();
    event.widget = m_control;
    event.stateMask = m_stateMask;
    event.button = button;
    event.x = x;
    event.y = y;
    return event;
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

  ////////////////////////////////////////////////////////////////////////////
  //
  // Low-level events
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Posts low-level {@link SWT.MouseMove} event with coordinates relative to control.
   */
  public void postMouseMove(Point p) {
    postMouseMove(p.x, p.y);
  }

  /**
   * Posts low-level {@link SWT.MouseMove} event with coordinates relative to control.
   */
  public void postMouseMove(int x, int y) {
    Display display = Display.getCurrent();
    Point p = display.map(m_control, null, x, y);
    postMouseMoveAbs(p.x, p.y);
  }

  public void postMouseMove(int x, int y, int button) {
    Display display = Display.getCurrent();
    Point p = display.map(m_control, null, x, y);
    // prepare event
    Event event;
    {
      event = new Event();
      event.type = SWT.MouseMove;
      event.x = p.x;
      event.y = p.y;
      event.button = button;
    }
    // post event
    Display.getCurrent().post(event);
  }

  /**
   * Posts low-level {@link SWT.MouseMove} event with absolute coordinates.
   */
  public static void postMouseMoveAbs(Point p) {
    postMouseMoveAbs(p.x, p.y);
  }

  /**
   * Posts low-level {@link SWT.MouseMove} event with absolute coordinates.
   */
  public static void postMouseMoveAbs(int x, int y) {
    // prepare event
    Event event;
    {
      event = new Event();
      event.type = SWT.MouseMove;
      event.x = x;
      event.y = y;
    }
    // post event
    Display.getCurrent().post(event);
  }

  /**
   * Posts low-level {@link SWT.MouseDown} event.
   */
  public static void postMouseDown(int button) {
    postMouseDownUp(SWT.MouseDown, button);
  }

  /**
   * Posts low-level {@link SWT.MouseUp} event.
   */
  public static void postMouseUp(int button) {
    postMouseDownUp(SWT.MouseUp, button);
  }

  /**
   * Posts low-level {@link SWT.MouseDown} or {@link SWT.MouseUp} event.
   */
  private static void postMouseDownUp(int type, int button) {
    // prepare event
    final Event event;
    {
      event = new Event();
      event.type = type;
      event.button = button;
    }
    // post event
    Display.getCurrent().post(event);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Low-level keyboard events
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Posts low-level {@link SWT.KeyDown} or {@link SWT.KeyUp} event.
   */
  private static void postKeyEvent(int type, int keyCode, char character) {
    // prepare event
    final Event event;
    {
      event = new Event();
      event.type = type;
      event.keyCode = keyCode;
      event.character = character;
    }
    // post event
    Display.getCurrent().post(event);
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
}
