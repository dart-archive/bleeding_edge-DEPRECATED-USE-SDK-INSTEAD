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
package com.google.dart.tools.ui.omni;

import org.eclipse.osgi.util.NLS;

public class OmniBoxMessages extends NLS {

  private static final String BUNDLE_NAME = "com.google.dart.tools.ui.omni.messages"; //$NON-NLS-1$
  public static String OmniBox_Commands;
  public static String OmniBox_Editors;
  public static String OmniBox_Menus;
  public static String OmniBox_New;
  public static String OmniBox_Preferences;
  public static String OmniBox_Previous;
  public static String OmniBox_Views;
  public static String OmniBox_PressKeyToShowAllMatches;
  public static String OmniBox_PressKeyToShowInitialMatches;
  public static String OmniBox_StartTypingToFindMatches;
  public static String OmniBox_Providers;
  public static String OmniBox_Types;
  public static String OmniBox_Files;
  public static String OmniBoxControlContribution_control_tooltip;
  public static String TextSearchElement_occurences;
  public static String TextSearchProvider_label;
  public static String TextSearch_taskName;

  static {
    NLS.initializeMessages(BUNDLE_NAME, OmniBoxMessages.class);
  }

  private OmniBoxMessages() {
  }
}
