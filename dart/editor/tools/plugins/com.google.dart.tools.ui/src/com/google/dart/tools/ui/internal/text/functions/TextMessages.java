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
package com.google.dart.tools.ui.internal.text.functions;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 */
final class TextMessages extends NLS {

  private static final String BUNDLE_NAME = TextMessages.class.getName();

  public static String JavaOutlineInformationControl_SortByDefiningTypeAction_label;

  public static String JavaOutlineInformationControl_SortByDefiningTypeAction_tooltip;
  public static String JavaOutlineInformationControl_SortByDefiningTypeAction_description;
  public static String JavaOutlineInformationControl_LexicalSortingAction_label;
  public static String JavaOutlineInformationControl_LexicalSortingAction_tooltip;
  public static String JavaOutlineInformationControl_LexicalSortingAction_description;
  public static String JavaOutlineInformationControl_GoIntoTopLevelType_label;
  public static String JavaOutlineInformationControl_GoIntoTopLevelType_tooltip;
  public static String JavaOutlineInformationControl_GoIntoTopLevelType_description;
  static {
    NLS.initializeMessages(BUNDLE_NAME, TextMessages.class);
  }

  private TextMessages() {
    // Do not instantiate
  }
}
