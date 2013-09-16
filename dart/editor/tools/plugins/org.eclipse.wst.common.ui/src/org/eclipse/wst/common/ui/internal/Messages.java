/*******************************************************************************
 * Copyright (c) 2001, 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.common.ui.internal;

import org.eclipse.osgi.util.NLS;

public class Messages {
  private static final String BUNDLE_NAME = "org.eclipse.wst.common.ui.internal.CommonUIMessages"; //$NON-NLS-1$

  static {
    NLS.initializeMessages(BUNDLE_NAME, Messages.class); //$NON-NLS-1$
  }

//	 SelectSingleFilePage and SelectMultiFilePage
  public static String _UI_LABEL_SOURCE_FILES;
  public static String _UI_LABEL_SELECTED_FILES;

  public static String _UI_POPUP_EXPAND_ALL;
  public static String _UI_POPUP_COLLAPSE_ALL;

  public static String _UI_IMPORT_BUTTON;
  public static String _UI_IMPORT_BUTTON_TOOL_TIP;

//	 SelectMultiFilePage
  public static String _UI_ADD_BUTTON_TOOL_TIP;
  public static String _UI_REMOVE_BUTTON_TOOL_TIP;
  public static String _UI_REMOVE_ALL_BUTTON_TOOL_TIP;

  public static String _UI_ADD_BUTTON;
  public static String _UI_REMOVE_BUTTON;
  public static String _UI_REMOVE_ALL_BUTTON;

//	 SelectJavaProjectView
  public static String _UI_LABEL_CHOOSE_FOLDER;

//	 SelectJavaProjectDialog
  public static String _UI_LABEL_FOLDER_SELECTION;

//	 TextViewerOperationAction
  public static String _UI_MENU_COPY;
  public static String _UI_MENU_CUT;
  public static String _UI_MENU_DELETE;
  public static String _UI_MENU_PASTE;
  public static String _UI_MENU_PREFIX;
  public static String _UI_MENU_REDO;
  public static String _UI_MENU_SELECT_ALL;
  public static String _UI_MENU_SHIFT_LEFT;
  public static String _UI_MENU_SHIFT_RIGHT;
  public static String _UI_MENU_STRIP_PREFIX;
  public static String _UI_MENU_UNDO;

//	 SourceViewerGotoLineAction
  public static String _UI_MENU_GOTO_LINE;
  public static String _UI_GOTO_LINE_DIALOG_TITLE;
  public static String _UI_GOTO_LINE_DIALOG_TEXT;

  public static String _UI_FILE_CHANGED_TITLE;
  public static String _UI_FILE_DELETED_SAVE_CHANGES;
  public static String _UI_FILE_DELETED_EDITOR_CLOSED;
  public static String _UI_FILE_CHANGED_LOAD_CHANGES;
  public static String _UI_SAVE_BUTTON;
  public static String _UI_CLOSE_BUTTON;

//	 XSL Prefererence
  public static String _UI_XSLT_SELECT;
  public static String _UI_XSLT_STYLESHEET;
  public static String _UI_XSLT_TRANSFORM;

//	 XSL Debug Prefererence
  public static String _UI_XSL_DEBUG_SELECT_LAUNCHER;
  public static String _UI_XSL_DEBUG_LOCAL;
  public static String _UI_XSL_DEBUG_REMOTE;
  public static String _UI_XSL_TILE_EDITOR;
  public static String _UI_XSL_DEBUG_AND_TRANSFORM;
  public static String _UI_XSL_CONTEXT_URI;
  public static String _UI_XSL_CONTEXT;

  public static String _UI_OVERRIDE_FILE;
  public static String _UI_JAVA_EXIST_FILE1;
  public static String _UI_JAVA_EXIST_FILE2;

//	 some options strings common to several plugins
  public static String _UI_ERROR_CREATING_FILE_TITLE;
  public static String _UI_ERROR_CREATING_FILE_SHORT_DESC;
  public static String _UI_ERROR_CREATING_FILE_LONG_DESC;
  public static String _UI_PARENT_FOLDER_IS_READ_ONLY;
  public static String _UI_UNKNOWN_ERROR_WITH_HINT;
  public static String _UI_UNKNOWN_ERROR;

//	 usage - this label is followed by two radio button options for the file location
  public static String _UI_LABEL_INCLUDE_URL_FILE;
  public static String _UI_RADIO_FILE;
  public static String _UI_RADIO_URL;

  public static String _UI_LABEL_COMPONENTS;
  public static String _UI_LABEL_QUALIFIER;

//	- component selection dialogs 
  public static String _UI_LABEL_COMPONENT_NAME;
  public static String _UI_LABEL_MATCHING_COMPONENTS;
  public static String _UI_LABEL_SPECIFIED_FILE;
  public static String _UI_LABEL_ENCLOSING_PROJECT;
  public static String _UI_LABEL_WORKSPACE;
  public static String _UI_LABEL_CURRENT_RESOURCE;
  public static String _UI_LABEL_SEARCH_SCOPE;
  public static String _UI_LABEL_NARROW_SEARCH_SCOPE_RESOURCE;
  public static String _UI_LABEL_AVAILABLE_TYPES;
  public static String _UI_LABEL_WORKING_SETS;

  public static String _UI_LABEL_New;
  public static String _UI_LABEL_DECLARATION_LOCATION;
  public static String _UI_LABEL_CHOOSE;

  /*
   * ====================================================================================== ! ! Here
   * is the list of Error string that have message IDs - make sure they are unique ! Range for
   * b2bgui messageIDs: IWAX1201 - IWAX1400 !
   * !======================================================================================
   */
  public static String _ERROR_THE_CONTAINER_NAME;

  public static String _ERROR_LOCAL_LOCATION;
  public static String _ERROR_NOT_JAVA_PROJECT;

//	 NOTE TO TRANSLATOR: this error message text is followed by a message from another plugin
  public static String _ERROR_INVALID_JAVA_PACKAGE;

//	 NOTE TO TRANSLATOR: (_ERROR_BAD_FILENAME_EXTENSION + string + _UI_LABEL_OR + string) or (_ERROR_BAD_FILENAME_EXTENSION)
  public static String _ERROR_BAD_FILENAME_EXTENSION;
  public static String _ERROR_FILE_ALREADY_EXISTS;

  public static String _ERROR_CONTAINER_NOT_JAVA_BUILDPATH;
  public static String _ERROR_USE_PROJECT_JAVA_SOURCE_FOLDER;

  public static String _UI_ERROR;

  public static String _WARN_INVALID_JAVA_PACKAGE;

  public static String _ERROR_ROOT_ELEMENT;
  public static String _ERROR_SAVING_FILE;

//	 File Validator
  public static String _UI_ERROR_VALIDATE_FAIL_TITLE;
  public static String _UI_ERROR_VALIDATE_FAIL_MESSAGE;

//	 PropertyDirtyChangeListener and PropertyResourceChangeListener
  public static String _UI_ERROR_VALIDATE_EDIT_FAIL_ONE_FILE;

  public static String SaveFilesDialog_save_all_resources;
  public static String SaveFilesDialog_must_save;
}
