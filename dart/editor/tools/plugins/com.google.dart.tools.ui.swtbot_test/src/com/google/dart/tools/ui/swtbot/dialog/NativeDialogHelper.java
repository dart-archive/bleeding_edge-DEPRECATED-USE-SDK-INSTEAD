/*
 * a * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.ui.swtbot.dialog;

import com.google.dart.tools.ui.swtbot.Performance;
import com.google.dart.tools.ui.swtbot.conditions.NativeShellClosed;
import com.google.dart.tools.ui.swtbot.conditions.NativeShellShowing;
import com.google.dart.tools.ui.swtbot.util.SWTBotUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;

/**
 * Helper for driving native dialogs
 */
class NativeDialogHelper {

  protected final SWTWorkbenchBot bot;
  protected final Display display;

  private static final int MODIFIER_MASK = SWT.ALT | SWT.SHIFT | SWT.CTRL | SWT.COMMAND;
  private static final int[] MODIFIERS = new int[] {SWT.ALT, SWT.SHIFT, SWT.CTRL, SWT.COMMAND};

  public NativeDialogHelper(SWTWorkbenchBot bot) {
    this.bot = bot;
    this.display = bot.getDisplay();
  }

  /**
   * Ensure that the native shell opened by this helper has been closed
   */
  protected void ensureNativeShellClosed() {
    int count = 0;
    while (SWTBotUtil.activeShell(bot) == null && count < 5) {
      System.out.println("Active shell is null or not found... attempting to close native dialog");
      typeKeyCode(SWT.ESC);
      bot.sleep(500);
      count++;
    }
  }

  /**
   * Post key click events (key down followed by key up) for the specified character. To type an
   * entire string, see {@link #typeText(String)}. If you want to queue events for characters with
   * accelerators such as {@link WT#CTRL} | 's', call {@link #typeKeyCode(int)} rather than this
   * method.
   * 
   * @param ch the character without accelerators
   */
  protected void typeChar(final char ch) {
    boolean shift = needsShift(ch);
    char lowerCase = ch;
    if (shift) {
      postKeyCodeDown(SWT.SHIFT);
      if (ch >= 'A' && ch <= 'Z') {
        lowerCase = Character.toLowerCase(ch);
      } else if (ch == '_') {
        lowerCase = '-';
      } else {
        throw new RuntimeException("Unknown lower case char for " + ch + " (" + ((int) ch) + ")");
      }
    }
    postCharDown(lowerCase);
    postCharUp(lowerCase);
    if (shift) {
      postKeyCodeUp(SWT.SHIFT);
    }
  }

  /**
   * Post key click events (key down followed by key up) for the specified keyCode. All uppercase
   * characters (e.g. 'T') are converted to lower case (e.g. 't') thus <code>keyCode('T')</code> is
   * equivalent to <code>keyCode('t')</code>. Also see {@link #typeChar(char)} and
   * {@link #typeText(String)}
   * 
   * @param keyCode the code for the key to be posted such as {@link SWT#HOME}, {@link SWT#CTRL} |
   *          't', {@link SWT#SHIFT} | {@link SWT#END}
   */
  protected void typeKeyCode(final int keyCode) {
    postModifierKeysDown(keyCode);
    int unmodified = keyCode - (keyCode & MODIFIER_MASK);

    // Key code characters have the SWT.KEYCODE_BIT bit set
    // whereas unicode characters do not
    if ((unmodified & SWT.KEYCODE_BIT) != 0) {
      postKeyCodeDown(unmodified);
      postKeyCodeUp(unmodified);
    } else {
      char ch = (char) unmodified;
      if (Character.isLetter(ch)) {
        ch = Character.toLowerCase(ch);
      }
      postCharDown(ch);
      postCharUp(ch);
    }

    postModifierKeysUp(keyCode);
  }

  /**
   * Post key click events (key down followed by key up) for the specified text
   */
  protected void typeText(String text) {
    if (text != null) {
      for (int i = 0; i < text.length(); i++) {
        typeChar(text.charAt(i));
      }
    }
  }

  /**
   * Wait for the active shell to be non-null, and assume that the native dialog has closed
   */
  protected void waitForNativeShellClosed() {
    bot.waitUntil(new NativeShellClosed(), Performance.DEFAULT_TIMEOUT_MS);
    bot.sleep(10);
  }

  /**
   * Wait for the original shell to loose focus, and assume that when that happens the native shell
   * then has focus.
   */
  protected void waitForNativeShellShowing() {
    bot.waitUntil(new NativeShellShowing());
    bot.sleep(1000);
  }

  /**
   * Determine if this key requires a shift to dispatch the keyStroke.
   * 
   * @param keyCode - the key in question
   * @return true if a shift event is required.
   */
  private boolean needsShift(char keyCode) {

    if (keyCode >= 62 && keyCode <= 90) {
      return true;
    }
    if (keyCode >= 123 && keyCode <= 126) {
      return true;
    }
    if (keyCode >= 33 && keyCode <= 43 && keyCode != 39) {
      return true;
    }
    if (keyCode >= 94 && keyCode <= 95) {
      return true;
    }
    if (keyCode == 58 || keyCode == 60 || keyCode == 62) {
      return true;
    }

    return false;
  }

  /**
   * Post key down event for the specified character
   * 
   * @param ch the character
   */
  private void postCharDown(final char ch) {
    Event event;
    event = new Event();
    event.type = SWT.KeyDown;
    event.character = ch;
    postEvent(event);
  }

  /**
   * Post key up event for the specified character
   * 
   * @param ch the character
   */
  private void postCharUp(final char ch) {
    Event event;
    event = new Event();
    event.type = SWT.KeyUp;
    event.character = ch;
    postEvent(event);
  }

  /**
   * Post the specified event to the OS event queue then sleep for 1/100 second to give the UI
   * thread a chance to get control and process the event. Sometimes the test framework drives the
   * UI too fast, and the OS event buffer gets full. In this situation, the #post(...) method
   * returns false, we back off, sleep for 1/4 second, then try again.
   * 
   * @param event the event (not <code>null</code>)
   */
  private void postEvent(final Event event) {
    final boolean[] success = new boolean[1];
    int count = 0;
    while (true) {
      display.syncExec(new Runnable() {
        @Override
        public void run() {
//          event.time = System.currentTimeMillis();
          success[0] = display.post(event);
        }
      });
      if (success[0]) {
        break;
      }
      if (++count == 5) {
        throw new RuntimeException("Failed to post event: " + event);
      }
      try {
        Thread.sleep(250);
      } catch (InterruptedException e) {
        //$FALL-THROUGH$
      }
    }
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      //$FALL-THROUGH$
    }
  }

  /**
   * Post key down event for the specified keyCode
   * 
   * @param keyCode the code for the key down to be queued such as {@link WT#HOME}, {@link WT#CTRL},
   *          {@link WT#SHIFT}, {@link WT#END}
   */
  private void postKeyCodeDown(int keyCode) {
    Event event;
    event = new Event();
    event.type = SWT.KeyDown;
    event.keyCode = keyCode;
    postEvent(event);
  }

  /**
   * Post key up event for the specified keyCode
   * 
   * @param keyCode the code for the key down to be queued such as {@link WT#HOME}, {@link WT#CTRL},
   *          {@link WT#SHIFT}, {@link WT#END}
   */
  private void postKeyCodeUp(int keyCode) {
    Event event;
    event = new Event();
    event.type = SWT.KeyUp;
    event.keyCode = keyCode;
    postEvent(event);
  }

  /**
   * Examine the accelerator bits to determine if any modifier keys (Shift, Alt, Control, Command)
   * are specified and post zero or more key down events for those modifier keys.
   * 
   * @param accelerator the accelerator that may specify zero or more modifier keys<br/>
   *          ({@link WT#SHIFT} , {@link WT#CTRL}, ...)
   */
  private void postModifierKeysDown(int accelerator) {
    for (int i = 0; i < MODIFIERS.length; i++) {
      int mod = MODIFIERS[i];
      if ((accelerator & mod) == mod) {
        postKeyCodeDown(mod);
      }
    }
  }

  /**
   * Examine the accelerator bits to determine if any modifier keys (Shift, Alt, Control, Command)
   * are specified and post zero or more key up events for those modifier keys.
   * 
   * @param accelerator the accelerator that may specify zero or more modifier keys<br/>
   *          ({@link WT#SHIFT} , {@link WT#CTRL}, ...)
   */
  private void postModifierKeysUp(int accelerator) {
    for (int i = MODIFIERS.length - 1; i >= 0; i--) {
      int mod = MODIFIERS[i];
      if ((accelerator & mod) == mod) {
        postKeyCodeUp(mod);
      }
    }
  }

}
