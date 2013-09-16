/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.contentassist;

public class SimpleCMElementDeclaration implements
    org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration {

  String fNodeName;

  /**
   * SimpleCMELementDeclaration constructor comment.
   */
  public SimpleCMElementDeclaration() {
    super();
  }

  public SimpleCMElementDeclaration(String nodeName) {
    super();
    setNodeName(nodeName);
  }

  /**
   * getAttributes method
   * 
   * @return CMNamedNodeMap Returns CMNamedNodeMap of AttributeDeclaration
   */
  public org.eclipse.wst.xml.core.internal.contentmodel.CMNamedNodeMap getAttributes() {
    return null;
  }

  /**
   * getCMContent method
   * 
   * @return CMContent Returns the root node of this element's content model. This can be an
   *         CMElementDeclaration or a CMGroup
   */
  public org.eclipse.wst.xml.core.internal.contentmodel.CMContent getContent() {
    return null;
  }

  /**
   * getContentType method
   * 
   * @return int Returns one of : ANY, EMPTY, ELEMENT, MIXED, PCDATA, CDATA.
   */
  public int getContentType() {
    return 0;
  }

  /**
   * getDataType method
   * 
   * @return java.lang.String
   */
  public org.eclipse.wst.xml.core.internal.contentmodel.CMDataType getDataType() {
    return null;
  }

  /**
   * getElementName method
   * 
   * @return java.lang.String
   */
  public String getElementName() {
    return null;
  }

  /**
   * getLocalElements method
   * 
   * @return CMNamedNodeMap Returns a list of locally defined elements.
   */
  public org.eclipse.wst.xml.core.internal.contentmodel.CMNamedNodeMap getLocalElements() {
    return null;
  }

  /**
   * getMaxOccur method
   * 
   * @return int If -1, it's UNBOUNDED.
   */
  public int getMaxOccur() {
    return 0;
  }

  /**
   * getMinOccur method
   * 
   * @return int If 0, it's OPTIONAL. If 1, it's REQUIRED.
   */
  public int getMinOccur() {
    return 0;
  }

  /**
   * @return java.lang.String
   */
  public java.lang.String getNodeName() {
    return fNodeName;
  }

  /**
   * getNodeType method
   * 
   * @return int Returns one of :
   */
  public int getNodeType() {
    return 0;
  }

  /**
   * getProperty method
   * 
   * @return java.lang.Object Returns the object property desciped by the propertyName
   */
  public Object getProperty(String propertyName) {
    return null;
  }

  /**
   * @param newNodeName java.lang.String
   */
  public void setNodeName(java.lang.String newNodeName) {
    fNodeName = newNodeName;
  }

  /**
   * supports method
   * 
   * @return boolean Returns true if the CMNode supports a specified property
   */
  public boolean supports(String propertyName) {
    return false;
  }
}
