/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.contentassist;

import org.eclipse.core.runtime.Platform;
import org.eclipse.wst.html.core.internal.HTMLCorePlugin;
import org.eclipse.wst.html.core.internal.contentmodel.HTMLElementDeclaration;
import org.eclipse.wst.html.core.internal.preferences.HTMLCorePreferenceNames;
import org.eclipse.wst.html.core.internal.provisional.HTMLCMProperties;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.ui.internal.contentassist.XMLContentModelGenerator;
import org.w3c.dom.Node;

import java.util.Locale;

public class HTMLMinimalContentModelGenerator extends XMLContentModelGenerator {

  private static HTMLMinimalContentModelGenerator instance = null;
  protected int fTagCase;
  protected int fAttrCase;

  /**
   * HTMLMinimalContentModelGenerator constructor comment.
   */
  private HTMLMinimalContentModelGenerator() {
    super();
  }

  private void init() {
    String qualifier = HTMLCorePlugin.getDefault().getBundle().getSymbolicName();
    fTagCase = Platform.getPreferencesService().getInt(qualifier,
        HTMLCorePreferenceNames.TAG_NAME_CASE, 0, null);
    fAttrCase = Platform.getPreferencesService().getInt(qualifier,
        HTMLCorePreferenceNames.ATTR_NAME_CASE, 0, null);
  }

  protected void generateEndTag(String tagName, Node parentNode, CMElementDeclaration elementDecl,
      StringBuffer buffer) {
    if (elementDecl == null)
      return;
    if (elementDecl instanceof HTMLElementDeclaration) {
      if (((Boolean) elementDecl.getProperty(HTMLCMProperties.IS_JSP)).booleanValue()) {
        if (elementDecl.getContentType() == CMElementDeclaration.EMPTY)
          return;
      } else {
        String ommission = (String) elementDecl.getProperty(HTMLCMProperties.OMIT_TYPE);
        if (ommission.equals(HTMLCMProperties.Values.OMIT_END)
            || ommission.equals(HTMLCMProperties.Values.OMIT_END_DEFAULT)
            || ommission.equals(HTMLCMProperties.Values.OMIT_END_MUST)) {
          return;
        }
      }
    }

    if (elementDecl.getContentType() == CMElementDeclaration.EMPTY)
      return;
    buffer.append("</" + tagName + ">"); //$NON-NLS-2$//$NON-NLS-1$
    return;
  }

  private boolean shouldIgnoreCase(CMNode cmnode) {
    if (!cmnode.supports(HTMLCMProperties.SHOULD_IGNORE_CASE))
      return false;
    return ((Boolean) cmnode.getProperty(HTMLCMProperties.SHOULD_IGNORE_CASE)).booleanValue();
  }

  public String getRequiredName(Node ownerNode, CMNode cmnode) {
    String name = super.getRequiredName(ownerNode, cmnode);
    // don't change the case unless we're certain it is meaningless
    if (shouldIgnoreCase(cmnode)) {
      int caseVal = -1;
      if (cmnode.getNodeType() == CMNode.ELEMENT_DECLARATION)
        caseVal = fTagCase;
      else if (cmnode.getNodeType() == CMNode.ATTRIBUTE_DECLARATION)
        caseVal = fAttrCase;
      switch (caseVal) {
        case HTMLCorePreferenceNames.LOWER: {
          name = name.toLowerCase(Locale.US);
        }
          break;
        case HTMLCorePreferenceNames.UPPER: {
          name = name.toUpperCase(Locale.US);
        }
          break;
      }
    }
    return name;
  }

  public String getStartTagClose(Node parentNode, CMElementDeclaration elementDecl) {
    String other = getOtherClose(parentNode);
    if (other != null)
      return other;
    if (elementDecl == null)
      return ">"; //$NON-NLS-1$
    if (elementDecl instanceof HTMLElementDeclaration) {
      if (((Boolean) elementDecl.getProperty(HTMLCMProperties.IS_JSP)).booleanValue()) {
        if (elementDecl.getContentType() == CMElementDeclaration.EMPTY)
          return "/>"; //$NON-NLS-1$
      } else {
        String ommission = (String) elementDecl.getProperty(HTMLCMProperties.OMIT_TYPE);
        if (ommission.equals(HTMLCMProperties.Values.OMIT_END)
            || ommission.equals(HTMLCMProperties.Values.OMIT_END_DEFAULT)
            || ommission.equals(HTMLCMProperties.Values.OMIT_END_MUST)) {
          return ">"; //$NON-NLS-1$
        }
      }
    }

    //if not an html element and empty, assume start tag needs to be closed
    else if (elementDecl.getContentType() == CMElementDeclaration.EMPTY) {
      return "/>"; //$NON-NLS-1$
    }

    return ">"; //$NON-NLS-1$
  }

  /**
   * Gets the instance.
   * 
   * @return Returns a HTMLMinimalContentModelGenerator
   */
  public synchronized static HTMLMinimalContentModelGenerator getInstance() {
    if (instance == null)
      instance = new HTMLMinimalContentModelGenerator();
    instance.init();
    return instance;
  }

}
