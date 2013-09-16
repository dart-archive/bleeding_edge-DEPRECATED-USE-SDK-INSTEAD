/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.taginfo;

import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDataType;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDocumentation;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNodeList;
import org.eclipse.wst.xml.core.internal.contentmodel.util.CMDescriptionBuilder;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;

/**
 * Provides basic tag information such as element/attribute name, data type, and tag
 * info/documentation for CMNodes. Uses HTML to enhance presentation.
 */
public class MarkupTagInfoProvider {
  protected final static String BOLD_END = "</b>"; //$NON-NLS-1$
  protected final static String BOLD_START = "<b>"; //$NON-NLS-1$
  protected final static String HEADING_END = "</h5>"; //$NON-NLS-1$
  protected final static String HEADING_START = "<h5>"; //$NON-NLS-1$
  protected final static String LIST_BEGIN = "<ul>"; //$NON-NLS-1$
  protected final static String LIST_ELEMENT = "<li>"; //$NON-NLS-1$
  protected final static String NEW_LINE = "<dl>"; //$NON-NLS-1$
  protected final static String PARAGRAPH_END = "</p>"; //$NON-NLS-1$
  protected final static String PARAGRAPH_START = "<p>"; //$NON-NLS-1$
  protected final static String SPACE = " "; //$NON-NLS-1$

  /**
   * Returns basic tag information for display given a CMNode
   * 
   * @return String
   */
  public String getInfo(CMNode node) {
    if (node == null) {
      return null;
    }
    StringBuffer sb = new StringBuffer();
    // we assume that if there is tagInfo present, only display tagInfo
    printTagInfo(sb, node);

    // no tagInfo present, so try to display tag description
    if (sb.length() == 0) {
      printDescription(sb, node);
    }

    // no tag description present either, so display default info
    if (sb.length() == 0) {
      printDefaultInfo(node, sb);
    }

    return sb.toString();
  }

  /**
   * Adds dataType's data type information, including enumerated type values to string buffer, sb
   */
  protected void printDataTypeInfo(StringBuffer sb, CMDataType dataType) {
    String dataTypeName = dataType.getNodeName();
    if ((dataTypeName != null) && (dataTypeName.length() > 0)) {
      sb.append(PARAGRAPH_START + BOLD_START + XMLUIMessages.Data_Type____4 + SPACE + BOLD_END);
      sb.append(dataTypeName);
      sb.append(PARAGRAPH_END);
    }
    String defaultValue = dataType.getImpliedValue();
    if (defaultValue != null) {
      sb.append(PARAGRAPH_START + BOLD_START + XMLUIMessages.Default_Value____6 + SPACE + BOLD_END);
      sb.append(defaultValue);
      sb.append(PARAGRAPH_END);
    }
    String[] enumeratedValue = dataType.getEnumeratedValues();
    if ((enumeratedValue != null) && (enumeratedValue.length > 0)) {
      sb.append(PARAGRAPH_START + BOLD_START + XMLUIMessages.Enumerated_Values____5 + SPACE
          + BOLD_END);
      sb.append(LIST_BEGIN);
      for (int i = 0; i < enumeratedValue.length; i++) {
        sb.append(LIST_ELEMENT + enumeratedValue[i]);
      }
      sb.append(PARAGRAPH_END);
    }
  }

  /**
   * Adds the default info (element name, content model, data type) of CMNode to the string buffer,
   * sb
   */
  protected void printDefaultInfo(CMNode node, StringBuffer sb) {
    {

      if (node.getNodeType() == CMNode.ELEMENT_DECLARATION) {
        CMElementDeclaration ed = (CMElementDeclaration) node;
        sb.append(PARAGRAPH_START + BOLD_START + XMLUIMessages.Element____1 + SPACE + BOLD_END);
        sb.append(node.getNodeName());
        sb.append(PARAGRAPH_END);
        printDocumentation(sb, node);
        if (ed.getContentType() == CMElementDeclaration.PCDATA) {
          CMDataType dataType = ed.getDataType();
          if (dataType != null) {
            printDataTypeInfo(sb, dataType);
          }
        } else {
          CMDescriptionBuilder builder = new CMDescriptionBuilder();
          String description = builder.buildDescription(node);
          if ((description != null) && (description.length() > 0)) {
            sb.append(PARAGRAPH_START + BOLD_START + XMLUIMessages.Content_Model____2 + SPACE
                + BOLD_END);
            sb.append(description + PARAGRAPH_END);
          }
        }
      } else if (node.getNodeType() == CMNode.ATTRIBUTE_DECLARATION) {
        CMAttributeDeclaration ad = (CMAttributeDeclaration) node;
        sb.append(PARAGRAPH_START + BOLD_START + XMLUIMessages.Attribute____3 + SPACE + BOLD_END);
        sb.append(node.getNodeName());
        sb.append(PARAGRAPH_END);
        printDocumentation(sb, node);
        CMDataType dataType = ad.getAttrType();
        if (dataType != null) {
          printDataTypeInfo(sb, dataType);
        }
      } else if (node.getNodeType() == CMNode.DATA_TYPE) {
        sb.append(PARAGRAPH_START + BOLD_START + XMLUIMessages.Data_Type____4 + SPACE + BOLD_END);
        sb.append(node.getNodeName());
        sb.append(PARAGRAPH_END);
        printDocumentation(sb, node);
      }
    }
  }

  /**
   * Adds the description property of the CMNode to the string buffer, sb
   */
  protected void printDescription(StringBuffer sb, CMNode node) {
    String tagInfo = (String) node.getProperty("description"); //$NON-NLS-1$
    if (tagInfo != null) {
      sb.append(PARAGRAPH_START + tagInfo.trim() + PARAGRAPH_END);
    }
  }

  /**
   * Adds the tag documentation property of the CMNode to the string buffer, sb
   */
  protected void printDocumentation(StringBuffer sb, CMNode node) {
    CMNodeList nodeList = (CMNodeList) node.getProperty("documentation"); //$NON-NLS-1$
    if ((nodeList != null) && (nodeList.getLength() > 0)) {
      for (int i = 0; i < nodeList.getLength(); i++) {
        CMDocumentation documentation = (CMDocumentation) nodeList.item(i);
        String doc = documentation.getValue();
        if (doc != null) {
          sb.append(PARAGRAPH_START + doc.trim() + PARAGRAPH_END);
        }
      }
      sb.append(NEW_LINE);
    }
  }

  /**
   * Adds the tag info property of the CMNode to the string buffer, sb
   */
  protected void printTagInfo(StringBuffer sb, CMNode node) {
    String tagInfo = (String) node.getProperty("tagInfo"); //$NON-NLS-1$
    if (tagInfo != null) {
      sb.append(PARAGRAPH_START + tagInfo.trim() + PARAGRAPH_END);
    }
  }
}
