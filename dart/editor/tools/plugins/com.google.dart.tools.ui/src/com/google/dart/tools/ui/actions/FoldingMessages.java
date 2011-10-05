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
package com.google.dart.tools.ui.actions;

import com.ibm.icu.text.MessageFormat;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Class that gives access to the folding messages resource bundle.
 */
public class FoldingMessages {

  private static final String BUNDLE_NAME = "com.google.dart.tools.ui.actions.FoldingMessages"; //$NON-NLS-1$

  private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

  /**
   * Returns the formatted resource string associated with the given key in the resource bundle.
   * <code>MessageFormat</code> is used to format the message. If there isn't any value under the
   * given key, the key is returned.
   * 
   * @param key the resource key
   * @param arg the message argument
   * @return the string
   */
  public static String getFormattedString(String key, Object arg) {
    return getFormattedString(key, new Object[] {arg});
  }

  /**
   * Returns the formatted resource string associated with the given key in the resource bundle.
   * <code>MessageFormat</code> is used to format the message. If there isn't any value under the
   * given key, the key is returned.
   * 
   * @param key the resource key
   * @param args the message arguments
   * @return the string
   */
  public static String getFormattedString(String key, Object[] args) {
    return MessageFormat.format(getString(key), args);
  }

  /**
   * Returns the resource bundle managed by the receiver.
   * 
   * @return the resource bundle
   */
  public static ResourceBundle getResourceBundle() {
    return RESOURCE_BUNDLE;
  }

  /**
   * Returns the resource string associated with the given key in the resource bundle. If there
   * isn't any value under the given key, the key is returned.
   * 
   * @param key the resource key
   * @return the string
   */
  public static String getString(String key) {
    try {
      return RESOURCE_BUNDLE.getString(key);
    } catch (MissingResourceException e) {
      return '!' + key + '!';
    }
  }

  private FoldingMessages() {
    // no instance
  }
}
