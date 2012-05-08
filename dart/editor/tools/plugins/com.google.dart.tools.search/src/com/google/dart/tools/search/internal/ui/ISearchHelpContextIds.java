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
package com.google.dart.tools.search.internal.ui;

import com.google.dart.tools.search.ui.NewSearchUI;

public interface ISearchHelpContextIds {

  public static final String PREFIX = NewSearchUI.PLUGIN_ID + "."; //$NON-NLS-1$

  public static final String SEARCH_DIALOG = PREFIX + "search_dialog_context"; //$NON-NLS-1$

  public static final String TEXT_SEARCH_PAGE = PREFIX + "text_search_page_context"; //$NON-NLS-1$
  public static final String TYPE_FILTERING_DIALOG = PREFIX + "type_filtering_dialog_context"; //$NON-NLS-1$

  public static final String SEARCH_VIEW = PREFIX + "search_view_context"; //$NON-NLS-1$
  public static final String New_SEARCH_VIEW = PREFIX + "new_search_view_context"; //$NON-NLS-1$

  public static final String REPLACE_DIALOG = PREFIX + "replace_dialog_context"; //$NON-NLS-1$

  public static final String SEARCH_PREFERENCE_PAGE = PREFIX + "search_preference_page_context"; //$NON-NLS-1$

  public static final String SELECT_ALL_ACTION = PREFIX + "select_all_action_context"; //$NON-NLS-1$

  public static final String SEARCH_ACTION = PREFIX + "search_action_context"; //$NON-NLS-1$

  public static final String FILE_SEARCH_ACTION = PREFIX + "file_search_action_context"; //$NON-NLS-1$
}
