/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/

package org.eclipse.wst.sse.ui.internal.provisional.preferences;

import org.eclipse.wst.sse.ui.internal.preferences.EditorPreferenceNames;

/**
 * Common editor preference keys used by the Structured Text Editor.
 * 
 * @plannedfor 1.0
 */
public class CommonEditorPreferenceNames {
  private CommonEditorPreferenceNames() {
    // empty private constructor so users cannot instantiate class
  }

  /**
   * A named preference that controls as-you-type validation
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String EVALUATE_TEMPORARY_PROBLEMS = EditorPreferenceNames.EVALUATE_TEMPORARY_PROBLEMS;
}
