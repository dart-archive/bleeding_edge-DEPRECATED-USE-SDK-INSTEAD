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
package com.google.dart.tools.ui.internal.text.editor.selectionactions;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 */
final class SelectionActionMessages extends NLS {

  private static final String BUNDLE_NAME = SelectionActionMessages.class.getName();

  public static String StructureSelect_error_title;

  public static String StructureSelect_error_message;
  public static String StructureSelectNext_label;
  public static String StructureSelectNext_tooltip;
  public static String StructureSelectNext_description;
  public static String StructureSelectPrevious_label;
  public static String StructureSelectPrevious_tooltip;
  public static String StructureSelectPrevious_description;
  public static String StructureSelectEnclosing_label;
  public static String StructureSelectEnclosing_tooltip;
  public static String StructureSelectEnclosing_description;
  public static String StructureSelectHistory_label;
  public static String StructureSelectHistory_tooltip;
  public static String StructureSelectHistory_description;
  public static String GotoNextMember_label;

  public static String GotoPreviousMember_label;
  static {
    NLS.initializeMessages(BUNDLE_NAME, SelectionActionMessages.class);
  }

  private SelectionActionMessages() {
    // Do not instantiate
  }
}
