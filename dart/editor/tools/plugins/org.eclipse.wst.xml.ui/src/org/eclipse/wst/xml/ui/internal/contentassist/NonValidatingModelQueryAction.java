/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.contentassist;

import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;

public class NonValidatingModelQueryAction implements
    org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQueryAction {

  protected CMNode cmnode = null;
  protected int endIndex = 0;
  protected int kind = INSERT;
  protected int startIndex = 0;
  protected Object userData = null;

  /**
   * NonValidatingModelQueryAction constructor comment.
   */
  protected NonValidatingModelQueryAction() {
    super();
  }

  /**
   * NonValidatingModelQueryAction constructor comment.
   */
  public NonValidatingModelQueryAction(CMNode newChild, int newKind, int newStart, int newEnd,
      Object newUserData) {
    super();
    cmnode = newChild;
    kind = newKind;
    startIndex = newStart;
    endIndex = newEnd;
    userData = newUserData;
  }

  /**
   * getCMNode method comment.
   */
  public org.eclipse.wst.xml.core.internal.contentmodel.CMNode getCMNode() {
    return cmnode;
  }

  /**
   * @return int
   */
  public int getEndIndex() {
    return endIndex;
  }

  /**
   * @return int
   */
  public int getKind() {
    return kind;
  }

  /**
   * getParent method comment.
   */
  public org.w3c.dom.Node getParent() {
    return null;
  }

  /**
   * @return int
   */
  public int getStartIndex() {
    return startIndex;
  }

  /**
   * getUserData method comment.
   */
  public Object getUserData() {
    return null;
  }

  /**
   * performAction method comment.
   */
  public void performAction() {
  }

  /**
   * setCMNode method comment.
   */
  protected void setCMNode(org.eclipse.wst.xml.core.internal.contentmodel.CMNode newNode) {
    cmnode = newNode;
  }

  /**
   * @param newEndIndex int
   */
  protected void setEndIndex(int newEndIndex) {
    endIndex = newEndIndex;
  }

  /**
   * @param newKind int
   */
  protected void setKind(int newKind) {
    kind = newKind;
  }

  /**
   * @param newStartIndex int
   */
  protected void setStartIndex(int newStartIndex) {
    startIndex = newStartIndex;
  }

  /**
   * setUserData method comment.
   */
  public void setUserData(Object object) {
  }

  /**
   * @return java.lang.String
   */
  public String toString() {
    String actionName = null;
    switch (kind) {
      case INSERT:
        actionName = "INSERT";//$NON-NLS-1$
        break;
      case REMOVE:
        actionName = "REMOVE";//$NON-NLS-1$
        break;
      case REPLACE:
        actionName = "REPLACE";//$NON-NLS-1$
        break;
      default:
        actionName = "UNKNOWN ACTION ";//$NON-NLS-1$
    }
    String nodeName = (cmnode != null) ? getCMNode().getNodeName() : "(unknown)";//$NON-NLS-1$
    return actionName + "=" + nodeName + "(" + startIndex + "..." + endIndex + ")";//$NON-NLS-4$//$NON-NLS-3$//$NON-NLS-2$//$NON-NLS-1$
  }
}
