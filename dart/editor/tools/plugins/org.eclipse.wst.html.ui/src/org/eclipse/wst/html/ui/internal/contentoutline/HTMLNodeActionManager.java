/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.contentoutline;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.wst.html.core.internal.HTMLContentBuilder;
import org.eclipse.wst.html.core.internal.HTMLCorePlugin;
import org.eclipse.wst.html.core.internal.format.HTMLFormatProcessorImpl;
import org.eclipse.wst.html.core.internal.preferences.HTMLCorePreferenceNames;
import org.eclipse.wst.html.core.internal.provisional.HTMLCMProperties;
import org.eclipse.wst.html.core.internal.provisional.contenttype.ContentTypeIdForHTML;
import org.eclipse.wst.sse.core.internal.format.IStructuredFormatProcessor;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.contentmodel.util.DOMContentBuilder;
import org.eclipse.wst.xml.ui.internal.contentoutline.XMLNodeActionManager;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * 
 */
public class HTMLNodeActionManager extends XMLNodeActionManager {
  protected int fTagCase;
  protected int fAttrCase;

  public HTMLNodeActionManager(IStructuredModel model, Viewer viewer) {
    super(model, viewer);
    updateCase();
  }

  /**
   * If neccessary, employ a DOMContentBuilder that understands how to change the case of HTML tags
   * (but NOT taglib derived tags).
   */
  public DOMContentBuilder createDOMContentBuilder(Document document) {
    DOMContentBuilder builder = null;
    String one = fModel.getModelHandler().getAssociatedContentTypeId();
    String modelContentTypeId = fModel.getContentTypeIdentifier();
    if (one != modelContentTypeId) {
      System.out.println("not equal"); //$NON-NLS-1$
    }
    if (modelContentTypeId.equals(ContentTypeIdForHTML.ContentTypeID_HTML))
      builder = new HTMLContentBuilder(document);
    else
      builder = super.createDOMContentBuilder(document);

    return builder;
  }

  private boolean shouldIgnoreCase(CMNode cmnode) {
    if (!cmnode.supports(HTMLCMProperties.SHOULD_IGNORE_CASE))
      return false;
    return ((Boolean) cmnode.getProperty(HTMLCMProperties.SHOULD_IGNORE_CASE)).booleanValue();
  }

  /**
   * Modify the displayed menuitem label to change the case of HTML children but neither XML nor
   * taglib-derived children.
   */
  public String getLabel(Node parent, CMNode cmnode) {
    String result = null;
    // CMNode cmnode = action.getCMNode();
    // don't change the case unless we're certain it is meaningless
    if (shouldIgnoreCase(cmnode)) {
      String name = cmnode.getNodeName();
      if (cmnode.getNodeType() == CMNode.ELEMENT_DECLARATION) {
        if (fTagCase == HTMLCorePreferenceNames.LOWER)
          name = name.toLowerCase();
        else if (fTagCase == HTMLCorePreferenceNames.UPPER)
          name = name.toUpperCase();
        // else do nothing
      } else if (cmnode.getNodeType() == CMNode.ATTRIBUTE_DECLARATION) {
        if (fAttrCase == HTMLCorePreferenceNames.LOWER)
          name = name.toLowerCase();
        else if (fAttrCase == HTMLCorePreferenceNames.UPPER)
          name = name.toUpperCase();
        // else do nothing
      }
      result = name;
    } else {
      result = super.getLabel(parent, cmnode);
    }

    return result;
  }

  /**
   * Another HTML specific detail.
   */
  protected void updateCase() {
    if (fModel != null) {
      String modelContentTypeId = fModel.getContentTypeIdentifier();
      if (modelContentTypeId != null) {
        if (modelContentTypeId.equals(ContentTypeIdForHTML.ContentTypeID_HTML)) {
          Preferences prefs = HTMLCorePlugin.getDefault().getPluginPreferences(); //$NON-NLS-1$
          fTagCase = prefs.getInt(HTMLCorePreferenceNames.TAG_NAME_CASE);
          fAttrCase = prefs.getInt(HTMLCorePreferenceNames.ATTR_NAME_CASE);
        }
      }
    }
  }

  public void reformat(Node newElement, boolean deep) {
    try {
      // tell the model that we are about to make a big model change
      fModel.aboutToChangeModel();

      // format selected node
      IStructuredFormatProcessor formatProcessor = new HTMLFormatProcessorImpl();
      formatProcessor.formatNode(newElement);
    } finally {
      // tell the model that we are done with the big model change
      fModel.changedModel();
    }
  }
}
