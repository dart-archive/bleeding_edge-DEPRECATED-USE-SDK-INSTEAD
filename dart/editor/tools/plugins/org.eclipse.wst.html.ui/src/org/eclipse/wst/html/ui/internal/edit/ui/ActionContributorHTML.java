/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.edit.ui;

import org.eclipse.wst.xml.ui.internal.actions.ActionContributorXML;

/**
 * Instead, use SourcePageActionContributor for source page contributor of multi page editor. Note
 * that this class is still valid for single page editor.
 */
public class ActionContributorHTML extends ActionContributorXML {
  private static final String[] EDITOR_IDS = {
      "org.eclipse.wst.html.core.htmlsource.source", "org.eclipse.wst.sse.ui.StructuredTextEditor"}; //$NON-NLS-1$ //$NON-NLS-2$

  protected String[] getExtensionIDs() {
    return EDITOR_IDS;
  }
}
