/*******************************************************************************
 * Copyright (c) 2001, 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.contentassist;

import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDataType;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.w3c.dom.Node;

public class XMLContentModelGenerator extends AbstractContentModelGenerator {

  /**
   * ISSUE: this is a bit of hidden JSP knowledge that was implemented this way for expedency.
   * Should be evolved in future to depend on "nestedContext".
   */
  private class DOMJSPRegionContextsPrivateCopy {
    private static final String JSP_DIRECTIVE_OPEN = "JSP_DIRECTIVE_OPEN"; //$NON-NLS-1$
  }

  /**
   * XMLContentModelGenerator constructor comment.
   */
  public XMLContentModelGenerator() {
    super();
  }

  public void generateAttribute(CMAttributeDeclaration attrDecl, StringBuffer buffer) {
    if ((attrDecl == null) || (buffer == null)) {
      return;
    }
    int usage = attrDecl.getUsage();
    if (usage == CMAttributeDeclaration.REQUIRED) {
      buffer.append(" "); //$NON-NLS-1$
      generateRequiredAttribute(null, attrDecl, buffer); // todo pass
      // ownerNode as
      // 1st param
    }
    return;
  }

  protected void generateEndTag(String tagName, Node parentNode, CMElementDeclaration elementDecl,
      StringBuffer buffer) {
    if (elementDecl == null) {
      return;
    }
    if (elementDecl.getContentType() != CMElementDeclaration.EMPTY) {
      buffer.append("</" + tagName + ">");//$NON-NLS-2$//$NON-NLS-1$
    }
    return;
  }

  public void generateRequiredAttribute(Node ownerNode, CMAttributeDeclaration attrDecl,
      StringBuffer buffer) {
    if ((attrDecl == null) || (buffer == null)) {
      return;
    }

    // attribute name
    String attributeName = getRequiredName(ownerNode, attrDecl);
    CMDataType attrType = attrDecl.getAttrType();
    String defaultValue = null;
    // = sign
    buffer.append(attributeName + "="); //$NON-NLS-1$
    // attribute value
    if (attrType != null) {
      // insert any value that is implied
      if ((attrType.getImpliedValueKind() != CMDataType.IMPLIED_VALUE_NONE)
          && (attrType.getImpliedValue() != null)) {
        defaultValue = attrType.getImpliedValue();
      }
      // otherwise, if an enumerated list of values exists, use the
      // first value
      else if ((attrType.getEnumeratedValues() != null)
          && (attrType.getEnumeratedValues().length > 0)) {
        defaultValue = attrType.getEnumeratedValues()[0];
      }
    }

    char attrQuote = '\"';
    // Found a double quote, wrap the attribute in single quotes
    if (defaultValue != null && defaultValue.indexOf(attrQuote) >= 0) {
      attrQuote = '\'';
    }

    buffer.append(attrQuote);
    buffer.append(((defaultValue != null) ? defaultValue : "")); //$NON-NLS-1$
    buffer.append(attrQuote);
    return;
  }

  protected void generateStartTag(String tagName, Node parentNode,
      CMElementDeclaration elementDecl, StringBuffer buffer) {
    if ((elementDecl == null) || (buffer == null)) {
      return;
    }
    buffer.append("<" + tagName);//$NON-NLS-1$
    generateAttributes(elementDecl, buffer);
    buffer.append(getStartTagClose(parentNode, elementDecl));
    return;
  }

  public int getMinimalStartTagLength(Node node, CMElementDeclaration elementDecl) {
    if (elementDecl == null) {
      return 0;
    }
    if (requiresAttributes(elementDecl)) {
      return getRequiredName(node, elementDecl).length() + 2; // < +
      // name +
      // space
    } else {
      return 1 + getRequiredName(node, elementDecl).length()
          + getStartTagClose(node, elementDecl).length(); // < +
      // name
      // +
      // appropriate
      // close
    }
  }

  protected String getOtherClose(Node notATagNode) {
    if (notATagNode instanceof IDOMNode) {
      IStructuredDocumentRegion node = ((IDOMNode) notATagNode).getStartStructuredDocumentRegion();
      if ((node != null)
          && (node.getNumberOfRegions() > 1)
          && node.getRegions().get(0).getType().equals(
              DOMJSPRegionContextsPrivateCopy.JSP_DIRECTIVE_OPEN)) {
        return "%>"; //$NON-NLS-1$
      }
    }
    return null;
  }

  public String getStartTagClose(Node parentNode, CMElementDeclaration elementDecl) {
    String other = getOtherClose(parentNode);
    if (other != null) {
      return other;
    }
    if (elementDecl == null) {
      return ">";//$NON-NLS-1$
    }
    if (elementDecl.getContentType() == CMElementDeclaration.EMPTY) {
      return "/>"; //$NON-NLS-1$
    }
    return ">"; //$NON-NLS-1$
  }
}
