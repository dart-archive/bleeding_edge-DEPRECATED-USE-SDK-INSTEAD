package com.google.dart.tools.internal.corext.refactoring.util;

import com.ibm.icu.text.MessageFormat;

/**
 * Helper class to format message strings.
 */
public class Messages {

  public static String format(String message, Object object) {
    return MessageFormat.format(message, new Object[]{object});
  }

  public static String format(String message, Object[] objects) {
    return MessageFormat.format(message, objects);
  }

  private Messages() {
    // Not for instantiation
  }
}
