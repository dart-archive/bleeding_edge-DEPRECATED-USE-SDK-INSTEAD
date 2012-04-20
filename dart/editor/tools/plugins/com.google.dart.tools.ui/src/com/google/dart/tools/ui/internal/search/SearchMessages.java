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
package com.google.dart.tools.ui.internal.search;

import org.eclipse.osgi.util.NLS;

/**
 * Search messages.
 */
public final class SearchMessages extends NLS {

  private static final String BUNDLE_NAME = "com.google.dart.tools.ui.internal.search.SearchMessages";//$NON-NLS-1$

  public static String SearchLabelProvider_exact_singular;
  public static String SearchLabelProvider_exact_noCount;
  public static String SearchLabelProvider_exact_and_potential_plural;
  public static String SearchLabelProvider_potential_singular;
  public static String SearchLabelProvider_potential_noCount;
  public static String SearchLabelProvider_potential_plural;
  public static String SearchLabelProvider_exact_plural;
  public static String Search_Error_search_title;
  public static String Search_Error_search_message;
  public static String Search_Error_search_notsuccessful_message;
  public static String Search_Error_search_notsuccessful_title;
  public static String Search_Error_openEditor_title;
  public static String Search_Error_openEditor_message;
  public static String Search_Error_codeResolve;
  public static String SearchElementSelectionDialog_title;
  public static String SearchElementSelectionDialog_message;
  public static String Search_FindReferencesAction_label;
  public static String Search_FindReferencesAction_tooltip;

  public static String DartElementAction_typeSelectionDialog_title;
  public static String DartElementAction_typeSelectionDialog_message;
  public static String DartElementAction_error_open_message;
  public static String DartElementAction_operationUnavailable_title;
  public static String DartElementAction_operationUnavailable_generic;
  public static String DartElementAction_operationUnavailable_field;
  public static String DartElementAction_operationUnavailable_interface;

  public static String DartSearchResultPage_open_editor_error_title;
  public static String DartSearchResultPage_open_editor_error_message;
  public static String DartSearchResultPage_filtered_message;
  public static String DartSearchResultPage_error_marker;
  public static String DartSearchResultPage_filteredWithCount_message;

  public static String DartSearchQuery_error_element_does_not_exist;
  public static String DartSearchQuery_error_participant_estimate;
  public static String DartSearchQuery_error_participant_search;
  public static String DartSearchQuery_error_unsupported_pattern;
  public static String DartSearchQuery_label;
  public static String DartSearchQuery_task_label;

  public static String DartSearchQuery_status_ok_message;

  public static String FindAction_unresolvable_selection;

  public static String MatchFilter_PotentialFilter_name;
  public static String MatchFilter_PotentialFilter_actionLabel;
  public static String MatchFilter_PotentialFilter_description;

  static {
    NLS.initializeMessages(BUNDLE_NAME, SearchMessages.class);
  }

  private SearchMessages() {
    // Do not instantiate
  }
}
