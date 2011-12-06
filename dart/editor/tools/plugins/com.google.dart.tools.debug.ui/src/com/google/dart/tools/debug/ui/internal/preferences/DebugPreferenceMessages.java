/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.debug.ui.internal.preferences;

import org.eclipse.osgi.util.NLS;

/**
 *
 */
public class DebugPreferenceMessages extends NLS {
  private static final String BUNDLE_NAME = "com.google.dart.tools.debug.ui.internal.preferences.messages"; //$NON-NLS-1$
  public static String DebugPreferencePage_Browse;
  public static String DebugPreferencePage_BrowserLocation;
  public static String DebugPreferencePage_BrowserTitle;
  public static String DebugPreferencePage_DefaultBrowserText;
  public static String DebugPreferencePage_DialogMessage;
  public static String DebugPreferencePage_DialogTitle;
  public static String DebugPreferencePage_SelectBrowserText;
  public static String DebugPreferencePage_VMExecutableLocation;
  public static String DebugPreferencePage_VMPath;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, DebugPreferenceMessages.class);
  }

  private DebugPreferenceMessages() {
  }
}
