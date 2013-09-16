/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.views.contentoutline;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.wst.html.ui.internal.HTMLUIPlugin;
import org.eclipse.wst.html.ui.internal.contentoutline.HTMLNodeActionManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.ui.internal.contentoutline.XMLNodeActionManager;
import org.eclipse.wst.xml.ui.views.contentoutline.XMLContentOutlineConfiguration;

/**
 * Configuration for outline view page which shows HTML content.
 * 
 * @see org.eclipse.wst.sse.ui.views.contentoutline.ContentOutlineConfiguration
 * @since 1.0
 */
public class HTMLContentOutlineConfiguration extends XMLContentOutlineConfiguration {
  private static final String OUTLINE_FILTER_PREF = "org.eclipse.wst.html.ui.OutlinePage"; //$NON-NLS-1$

  /**
   * Create new instance of HTMLContentOutlineConfiguration
   */
  public HTMLContentOutlineConfiguration() {
    // Must have empty constructor to createExecutableExtension
    super();
  }

  protected XMLNodeActionManager createNodeActionManager(TreeViewer treeViewer) {
    return new HTMLNodeActionManager((IStructuredModel) treeViewer.getInput(), treeViewer);
  }

  protected IPreferenceStore getPreferenceStore() {
    return HTMLUIPlugin.getDefault().getPreferenceStore();
  }

  protected String getOutlineFilterTarget() {
    return OUTLINE_FILTER_PREF;
  }
}
