/*******************************************************************************
 * Copyright (c) 2001, 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.actions;

public interface StructuredTextEditorActionConstants {
  public final static String ACTION_NAME_ADD_BLOCK_COMMENT = "AddBlockComment";//$NON-NLS-1$
  public final static String ACTION_NAME_CLEANUP_DOCUMENT = "CleanupDocument";//$NON-NLS-1$
  public final static String ACTION_NAME_COMMENT = "Comment";//$NON-NLS-1$
  public final static String ACTION_NAME_CONTENTASSIST_CONTEXT_INFORMATION = "ContentAssistContextInformation";//$NON-NLS-1$
  public final static String ACTION_NAME_CONTENTASSIST_PROPOSALS = "ContentAssistProposals";//$NON-NLS-1$
  public final static String ACTION_NAME_FIND_OCCURRENCES = "FindOccurrences"; //$NON-NLS-1$
  public final static String ACTION_NAME_FORMAT_ACTIVE_ELEMENTS = "FormatActiveElements";//$NON-NLS-1$
  public final static String ACTION_NAME_FORMAT_DOCUMENT = "FormatDocument";//$NON-NLS-1$
  public final static String ACTION_NAME_MANAGE_BREAKPOINTS = "ManageBreakpoints";//$NON-NLS-1$
  public final static String ACTION_NAME_OPEN_FILE = "OpenFileFromSource";//$NON-NLS-1$
  public final static String ACTION_NAME_REMOVE_BLOCK_COMMENT = "RemoveBlockComment";//$NON-NLS-1$
  public final static String ACTION_NAME_STRUCTURE_SELECT_ENCLOSING = "StructureSelectEnclosing";//$NON-NLS-1$
  public final static String ACTION_NAME_STRUCTURE_SELECT_HISTORY = "StructureSelectHistory";//$NON-NLS-1$
  public final static String ACTION_NAME_STRUCTURE_SELECT_NEXT = "StructureSelectNext";//$NON-NLS-1$
  public final static String ACTION_NAME_STRUCTURE_SELECT_PREVIOUS = "StructureSelectPrevious";//$NON-NLS-1$
  public final static String ACTION_NAME_TOGGLE_COMMENT = "ToggleComment";//$NON-NLS-1$
  public final static String ACTION_NAME_UNCOMMENT = "Uncomment";//$NON-NLS-1$
  public final static String ACTION_NAME_GOTO_MATCHING_BRACKET = "GotoMatchingBracket";//$NON-NLS-1$
  public final static String ACTION_NAME_SHOW_OUTLINE = "ShowQuickOutline"; //$NON-NLS-1$

  /**
   * @deprecated use UNDERSCORE instead
   */
  public final static String DOT = ".";//$NON-NLS-1$
  public final static String UNDERSCORE = "_"; //$NON-NLS-1$

  public final static String GROUP_NAME_MENU_ADDITIONS = "MenuAdditions";//$NON-NLS-1$
  public final static String GROUP_NAME_TOOLBAR_ADDITIONS = "ToolbarAdditions";//$NON-NLS-1$

  public final static String STATUS_CATEGORY_OFFSET = "Offset";//$NON-NLS-1$
}
