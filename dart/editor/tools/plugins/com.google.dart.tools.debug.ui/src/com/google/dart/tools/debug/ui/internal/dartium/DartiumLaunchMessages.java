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
package com.google.dart.tools.debug.ui.internal.dartium;

import org.eclipse.osgi.util.NLS;

/**
 *
 */
public class DartiumLaunchMessages extends NLS {
  private static final String BUNDLE_NAME = "com.google.dart.tools.debug.ui.internal.dartium.messages"; //$NON-NLS-1$
  public static String DartiumMainTab_Browse;
  public static String DartiumMainTab_HtmlFileLabel;
  public static String DartiumMainTab_LaunchTarget;
  public static String DartiumMainTab_Message;
  public static String DartiumMainTab_Name;
  public static String DartiumMainTab_NoHtmlFile;
  public static String DartiumMainTab_NoProject;
  public static String DartiumMainTab_NoUrl;
  public static String DartiumMainTab_InvalidURL;
  public static String DartiumMainTab_ProjectLabel;
  public static String DartiumMainTab_SelectHtml;
  public static String DartiumMainTab_SelectProject;
  public static String DartiumMainTab_UrlLabel;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, DartiumLaunchMessages.class);
  }

  private DartiumLaunchMessages() {
  }
}
