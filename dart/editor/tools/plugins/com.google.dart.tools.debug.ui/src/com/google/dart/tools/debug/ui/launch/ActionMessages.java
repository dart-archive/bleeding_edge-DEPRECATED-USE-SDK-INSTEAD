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
package com.google.dart.tools.debug.ui.launch;

import org.eclipse.osgi.util.NLS;

public final class ActionMessages extends NLS {

  private static final String BUNDLE_NAME = "com.google.dart.tools.debug.ui.launch.ActionMessages";//$NON-NLS-1$

  public static String OpenInBrowserAction_couldNotOpenFile;
  public static String OpenInBrowserAction_description;
  public static String OpenInBrowserAction_errorMessage;
  public static String OpenInBrowserAction_title;
  public static String OpenInBrowserAction_unableToLaunch;
  public static String OpenInBrowserAction_notInDartLib;
  public static String OpenInBrowserAction_notAnHtmlFile;
  public static String OpenInBrowserAction_noJSFile;
  public static String OpenInBrowserAction_couldNotFindBrowser;
  public static String OpenInBrowserAction_toolTip;

  public static String OpenInBrowserAction_noFileTitle;
  public static String OpenInBrowserAction_noFileMessage;

  public static String OpenInBrowserAction_selectFileTitle;
  public static String OpenInBrowserAction_selectFileMessage;

  public static String OpenInBrowserAction_jobTitle;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, ActionMessages.class);
  }

  private ActionMessages() {
  }

}
