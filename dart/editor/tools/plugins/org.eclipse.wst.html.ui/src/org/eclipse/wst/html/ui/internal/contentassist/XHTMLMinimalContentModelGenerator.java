/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.contentassist;

import org.eclipse.wst.html.core.internal.contentmodel.HTMLElementDeclaration;
import org.eclipse.wst.html.core.internal.provisional.HTMLCMProperties;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.ui.internal.contentassist.XMLContentModelGenerator;
import org.w3c.dom.Node;

public class XHTMLMinimalContentModelGenerator extends XMLContentModelGenerator {

  private static XHTMLMinimalContentModelGenerator instance = null;

  private XHTMLMinimalContentModelGenerator() {
    super();
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

  public String getStartTagClose(Node parentNode, CMElementDeclaration elementDecl) {
    String other = getOtherClose(parentNode);
    if (other != null)
      return other;
    if (elementDecl == null)
      return ">"; //$NON-NLS-1$
    // EMPTY tag, do a self-close
    if (elementDecl.getContentType() == CMElementDeclaration.EMPTY) {
      // if it's a JSP element, don't add the space since the JSP container doesn't/shouldn't care
      if (elementDecl instanceof HTMLElementDeclaration
          && (((Boolean) elementDecl.getProperty(HTMLCMProperties.IS_JSP)).booleanValue()))
        // if it's not JSP, conform to XHTML guidelines and add the space
        return "/>"; //$NON-NLS-1$
      else
        return " />"; //$NON-NLS-1$
    }
    // not defined as EMPTY, but should be treated as such anyway
    else if (elementDecl instanceof HTMLElementDeclaration) {
      String ommission = (String) elementDecl.getProperty(HTMLCMProperties.OMIT_TYPE);
      if (ommission.equals(HTMLCMProperties.Values.OMIT_END)
          || ommission.equals(HTMLCMProperties.Values.OMIT_END_DEFAULT)
          || ommission.equals(HTMLCMProperties.Values.OMIT_END_MUST)) {
        return " />"; //$NON-NLS-1$
      }
    }

    return ">"; //$NON-NLS-1$
  }

  public synchronized static XHTMLMinimalContentModelGenerator getInstance() {
    if (instance == null)
      instance = new XHTMLMinimalContentModelGenerator();
    return instance;
  }
}
