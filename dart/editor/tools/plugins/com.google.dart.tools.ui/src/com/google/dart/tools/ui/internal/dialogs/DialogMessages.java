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
package com.google.dart.tools.ui.internal.dialogs;

import org.eclipse.osgi.util.NLS;

import java.util.ResourceBundle;

public final class DialogMessages extends NLS {

  private static final String BUNDLE_NAME = "com.google.dart.tools.ui.internal.dialogs.DialogMessages";//$NON-NLS-1$
  private static final ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME);

  public static String OpenFolderDialog_browse;

  public static String OpenFolderDialog_description;
  public static String OpenFolderDialog_dialogMessage;
  public static String OpenFolderDialog_label;
  public static String OpenFolderDialog_message;
  public static String OpenFolderDialog_rubPubMessage;
  public static String OpenFolderDialog_title;

  static {
    NLS.initializeMessages(BUNDLE_NAME, DialogMessages.class);
  }

  public static ResourceBundle getBundle() {
    return bundle;
  }

  private DialogMessages() {

  }

}
