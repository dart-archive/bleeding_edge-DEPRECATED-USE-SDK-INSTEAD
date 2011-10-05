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
package com.google.dart.tools.ui.dialogs;

import org.eclipse.osgi.util.NLS;

/**
 * 
 */
public class DialogsMessages extends NLS {
  private static final String BUNDLE_NAME = "com.google.dart.tools.ui.dialogs.DialogsMessages"; //$NON-NLS-1$

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, DialogsMessages.class);
  }

  public static String AboutDartDialog_about_image;
  public static String AboutDartDialog_copyright;
  public static String AboutDartDialog_copyright_line2;
  public static String AboutDartDialog_product_label;
  public static String AboutDartDialog_title_text;
  public static String AboutDartDialog_version_string_prefix;
  public static String SortMembersMessageDialog_dialog_title;
  public static String SortMembersMessageDialog_description;
  public static String SortMembersMessageDialog_link_tooltip;
  public static String SortMembersMessageDialog_do_not_sort_fields_label;
  public static String SortMembersMessageDialog_sort_all_label;
  public static String SortMembersMessageDialog_sort_warning_label;

  private DialogsMessages() {
  }
}
