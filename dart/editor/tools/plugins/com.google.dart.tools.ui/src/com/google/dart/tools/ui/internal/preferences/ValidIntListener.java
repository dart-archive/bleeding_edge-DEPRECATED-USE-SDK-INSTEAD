package com.google.dart.tools.ui.internal.preferences;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * Listener that only allows digits to be entered into a text field
 */
class ValidIntListener implements Listener {
  @Override
  public void handleEvent(Event e) {
    String txt = e.text;
    // Allow for delete
    if (txt.isEmpty()) {
      return;
    }
    try {
      // Only allow digits
      int num = Integer.parseInt(txt);
      if (num >= 0) {
        return;
      }
    } catch (NumberFormatException nfe) {
      // Error
    }

    e.doit = false;
    return;
  }
}
