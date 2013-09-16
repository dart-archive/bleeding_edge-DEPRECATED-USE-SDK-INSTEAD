/*******************************************************************************
 * Copyright (c) 2001, 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.actions;

/*******************************************************************************
 * Copyright (c) 2001, 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/

/**
 * Defines the definitions ids for editor actions.
 */
public interface ActionDefinitionIds {
  // TODO: Can these be better defined with a "plugin prefix" to be more
  // portable?
  public final static String ADD_BLOCK_COMMENT = "org.eclipse.wst.sse.ui.add.block.comment";//$NON-NLS-1$

  public final static String CLEANUP_DOCUMENT = "org.eclipse.wst.sse.ui.cleanup.document";//$NON-NLS-1$
  public final static String COMMENT = "org.eclipse.wst.sse.ui.comment";//$NON-NLS-1$
  public final static String EDIT_BREAKPOINTS = "org.eclipse.wst.sse.ui.breakpoints.edit";//$NON-NLS-1$
  public final static String FIND_OCCURRENCES = "org.eclipse.wst.sse.ui.search.find.occurrences";//$NON-NLS-1$
  public final static String FORMAT_ACTIVE_ELEMENTS = "org.eclipse.wst.sse.ui.format.active.elements";//$NON-NLS-1$
  public final static String FORMAT_DOCUMENT = "org.eclipse.wst.sse.ui.format.document";//$NON-NLS-1$
  public final static String MANAGE_BREAKPOINTS = "org.eclipse.wst.sse.ui.breakpoints.manage";//$NON-NLS-1$
  public final static String OPEN_FILE = "org.eclipse.wst.sse.ui.open.file.from.source";//$NON-NLS-1$
  public final static String REMOVE_BLOCK_COMMENT = "org.eclipse.wst.sse.ui.remove.block.comment";//$NON-NLS-1$
  public final static String STRUCTURE_SELECT_ENCLOSING = "org.eclipse.wst.sse.ui.structure.select.enclosing";//$NON-NLS-1$
  public final static String STRUCTURE_SELECT_HISTORY = "org.eclipse.wst.sse.ui.structure.select.last";//$NON-NLS-1$
  public final static String STRUCTURE_SELECT_NEXT = "org.eclipse.wst.sse.ui.structure.select.next";//$NON-NLS-1$
  public final static String STRUCTURE_SELECT_PREVIOUS = "org.eclipse.wst.sse.ui.structure.select.previous";//$NON-NLS-1$
  public final static String TOGGLE_COMMENT = "org.eclipse.wst.sse.ui.toggle.comment";//$NON-NLS-1$
  public final static String TOGGLE_BREAKPOINTS = "org.eclipse.wst.sse.ui.breakpoints.toggle";//$NON-NLS-1$
  public final static String UNCOMMENT = "org.eclipse.wst.sse.ui.uncomment";//$NON-NLS-1$
  public final static String SHOW_OUTLINE = "org.eclipse.wst.sse.ui.quick_outline";//$NON-NLS-1$

  // registered command IDs, so we pick up the declared key bindings
  public static final String GOTO_MATCHING_BRACKET = "org.eclipse.wst.sse.ui.goto.matching.bracket"; //$NON-NLS-1$
}
