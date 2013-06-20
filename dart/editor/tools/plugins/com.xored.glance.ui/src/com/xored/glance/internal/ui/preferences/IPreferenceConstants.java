/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and Implementation (Yuri Strot)
 *******************************************************************************/
package com.xored.glance.internal.ui.preferences;

/**
 * TODO(messick) Move strings to a property file for eventual localization.
 * 
 * @author Yuri Strot
 */
public interface IPreferenceConstants {

  final String PREFERENCE_PAGE_ID = "com.xored.glance.ui.preference";

  final String SEARCH_PREFIX = "search.";
  final String PANEL_PREFIX = "panel.";
  final String HISTORY = "searchHistory";

  final String PANEL_DIRECTIONS = PANEL_PREFIX + "directions";
  final String PANEL_CLOSE = PANEL_PREFIX + "close";
  final String PANEL_TEXT_SIZE = PANEL_PREFIX + "textSize";
  final String PANEL_STATUS_LINE = PANEL_PREFIX + "statusLine";
  final String PANEL_LINK = PANEL_PREFIX + "link";
  final String PANEL_STARTUP = PANEL_PREFIX + "startup";
  final String PANEL_AUTO_INDEXING = PANEL_PREFIX + "autoIndexing";
  final String PANEL_MAX_INDEXING_DEPTH = PANEL_PREFIX + "maxIndexingDepth";

  final String SEARCH_CASE_SENSITIVE = SEARCH_PREFIX + "caseSensitive";
  final String SEARCH_WORD_PREFIX = SEARCH_PREFIX + "wordPrefix";
  final String SEARCH_REGEXP = SEARCH_PREFIX + "regexp";
  final String SEARCH_CAMEL_CASE = SEARCH_PREFIX + "camelCase";
  final String SEARCH_INCREMENTAL = PANEL_PREFIX + "incremental";

  final String LABEL_CASE_SENSITIVE = "Case Sensitive";
  final String LABEL_WORD_PREFIX = "Word Prefix";
  final String LABEL_REGEXP = "Regular Expressions";
  final String LABEL_CAMEL_CASE = "Camel Case";
  final String LABEL_CLEAR_HISTORY = "Clear Search History";

  final String COLOR_HIGHLIGHT = "glanceColorBackground";
  final String COLOR_SELECTION = "glanceSelectedColorBackground";

}
