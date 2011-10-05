/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.html;

import com.ibm.icu.text.MessageFormat;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Helper class to get NLSed messages.
 */
class HTMLMessages {

  private static final String RESOURCE_BUNDLE = HTMLMessages.class.getName();

  private static ResourceBundle fgResourceBundle = ResourceBundle.getBundle(RESOURCE_BUNDLE);

  /**
   * Gets a string from the resource bundle and formats it with the given argument.
   * 
   * @param key the string used to get the bundle value, must not be null
   * @param arg the argument used to format the string
   * @return the formatted string
   */
  public static String getFormattedString(String key, Object arg) {
    String format = null;
    try {
      format = fgResourceBundle.getString(key);
    } catch (MissingResourceException e) {
      return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
    }
    if (arg == null) {
      arg = ""; //$NON-NLS-1$
    }
    return MessageFormat.format(format, new Object[] {arg});
  }

  /**
   * Gets a string from the resource bundle and formats it with the given arguments.
   * 
   * @param key the string used to get the bundle value, must not be null
   * @param args the arguments used to format the string
   * @return the formatted string
   */
  public static String getFormattedString(String key, Object[] args) {
    String format = null;
    try {
      format = fgResourceBundle.getString(key);
    } catch (MissingResourceException e) {
      return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
    }
    return MessageFormat.format(format, args);
  }

  /**
   * Gets a string from the resource bundle.
   * 
   * @param key the string used to get the bundle value, must not be null
   * @return the string from the resource bundle
   */
  public static String getString(String key) {
    try {
      return fgResourceBundle.getString(key);
    } catch (MissingResourceException e) {
      return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
    }
  }

  private HTMLMessages() {
  }
}
