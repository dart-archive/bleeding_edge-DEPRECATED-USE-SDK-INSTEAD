/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.common.ui.internal.search;

import org.eclipse.osgi.util.NLS;

public final class SearchMessages extends NLS {

  private static final String BUNDLE_NAME = "org.eclipse.wst.common.ui.internal.search.SearchMessages";//$NON-NLS-1$

  private SearchMessages() {
    // Do not instantiate
  }

  public static String group_references;
  public static String Search_FindDeclarationAction_label;
  public static String Search_FindDeclarationsInProjectAction_label;
  public static String Search_FindDeclarationsInWorkingSetAction_label;
  public static String FileSearchPage_limited_format;
  public static String FileSearchPage_error_marker;
  public static String FileSearchPage_sort_by_label;
  public static String OpenWithMenu_label;
  public static String FileSearchQuery_label;
  public static String FileSearchQuery_pluralPattern;
  public static String FileSearchQuery_singularLabel;
  public static String FileSearchQuery_singularLabel_fileNameSearch;
  public static String FileSearchQuery_pluralPattern_fileNameSearch;
  public static String TextSearchEngine_statusMessage;
  public static String FileLabelProvider_count_format;
  public static String FileLabelProvider_removed_resource_label;

  static {
    NLS.initializeMessages(BUNDLE_NAME, SearchMessages.class);
  }
}
