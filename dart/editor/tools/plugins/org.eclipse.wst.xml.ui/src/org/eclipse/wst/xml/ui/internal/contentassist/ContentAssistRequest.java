/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.contentassist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionCollection;
import org.eclipse.wst.sse.core.utils.StringUtils;
import org.w3c.dom.Node;

public class ContentAssistRequest {
  protected IStructuredDocumentRegion documentRegion = null;
  protected List macros = new ArrayList();
  protected String matchString;
  protected Node node = null;
  protected Node parent = null;
  protected List proposals = new ArrayList();
  protected ITextRegion region = null;
  protected int replacementBeginPosition;
  protected int replacementLength;

  // private Boolean separate = null; // (pa) not used
  // private Boolean sort = null; // (pa) not used
  /**
   * XMLContentAssistRequest constructor comment.
   */
  public ContentAssistRequest(Node node, Node parent, IStructuredDocumentRegion documentRegion,
      ITextRegion completionRegion, int begin, int length, String filter) {
    super();
    setNode(node);
    setParent(parent);
    setDocumentRegion(documentRegion);
    setRegion(completionRegion);
    setMatchString(filter);
    setReplacementBeginPosition(begin);
    setReplacementLength(length);
  }

  public void addMacro(ICompletionProposal newProposal) {
    macros.add(newProposal);
  }

  public void addProposal(ICompletionProposal newProposal) {
    proposals.add(newProposal);
  }

  public ICompletionProposal[] getCompletionProposals() {
    ICompletionProposal results[] = null;
    if ((getProposals().size() > 0) || (getMacros().size() > 0)) {
      List allProposals = new ArrayList();
      if (!shouldSeparate()) {
        allProposals.addAll(getProposals());
        // should be empty, as all macros should have gone into the
        // proposal list
        allProposals.addAll(getMacros());
        allProposals = sortProposals(allProposals);
      } else {
        allProposals.addAll(sortProposals(getProposals()));
        allProposals.addAll(sortProposals(getMacros()));
      }

      results = new ICompletionProposal[allProposals.size()];
      for (int i = 0; i < allProposals.size(); i++) {
        results[i] = (ICompletionProposal) allProposals.get(i);
      }
    }
    return results;
  }

  public IStructuredDocumentRegion getDocumentRegion() {
    return documentRegion;
  }

  /**
   * @return java.util.List
   */
  public java.util.List getMacros() {
    return macros;
  }

  /**
   * @return java.lang.String
   */
  public java.lang.String getMatchString() {
    return matchString;
  }

  /**
   * @return org.w3c.dom.Node
   */
  public org.w3c.dom.Node getNode() {
    return node;
  }

  /**
   * @return org.w3c.dom.Node
   */
  public org.w3c.dom.Node getParent() {
    return parent;
  }

  /**
   * @return java.util.List
   */
  public java.util.List getProposals() {
    return proposals;
  }

  public ITextRegion getRegion() {
    return region;
  }

  /**
   * @return int
   */
  public int getReplacementBeginPosition() {
    return replacementBeginPosition;
  }

  /**
   * @return int
   */
  public int getReplacementLength() {
    return replacementLength;
  }

  public int getStartOffset() {
    if ((getDocumentRegion() != null) && (getRegion() != null)) {
      return ((ITextRegionCollection) getDocumentRegion()).getStartOffset(getRegion());
    }
    return -1;
  }

  public String getText() {
    if ((getDocumentRegion() != null) && (getRegion() != null)) {
      return ((ITextRegionCollection) getDocumentRegion()).getText(getRegion());
    }
    return ""; //$NON-NLS-1$
  }

  public int getTextEndOffset() {
    if ((getDocumentRegion() != null) && (getRegion() != null)) {
      return ((ITextRegionCollection) getDocumentRegion()).getTextEndOffset(getRegion());
    }
    return -1;
  }

  /**
   * @param region
   */
  public void setDocumentRegion(IStructuredDocumentRegion region) {
    documentRegion = region;
  }

  /**
   * @param newMatchString java.lang.String
   */
  public void setMatchString(java.lang.String newMatchString) {
    matchString = newMatchString;
  }

  /**
   * @param newNode org.w3c.dom.Node
   */
  public void setNode(org.w3c.dom.Node newNode) {
    node = newNode;
  }

  /**
   * @param newParent org.w3c.dom.Node
   */
  public void setParent(org.w3c.dom.Node newParent) {
    parent = newParent;
  }

  /**
   * @param newRegion
   */
  public void setRegion(ITextRegion newRegion) {
    region = newRegion;
  }

  /**
   * @param newReplacementBeginPosition int
   */
  public void setReplacementBeginPosition(int newReplacementBeginPosition) {
    replacementBeginPosition = newReplacementBeginPosition;
  }

  public void setReplacementLength(int newReplacementLength) {
    replacementLength = newReplacementLength;
  }

  public boolean shouldSeparate() {
    /*
     * if (separate == null) { PreferenceManager manager = getPreferenceManager(); if(manager ==
     * null) { separate = Boolean.FALSE; } else { Element caSettings =
     * manager.getElement(PreferenceNames.CONTENT_ASSIST); separate = new
     * Boolean(caSettings.getAttribute(PreferenceNames.SEPARATE).equals(PreferenceNames.TRUE)); } }
     * return separate.booleanValue();
     */
    return false;
  }

  protected List sortProposals(List proposalsIn) {
    Collections.sort(proposalsIn, new ProposalComparator());
    return proposalsIn;

  }

  /**
   * @return java.lang.String
   */
  public java.lang.String toString() {
    return "Node: " + getNode() //$NON-NLS-1$
        + "\nParent: " + getParent() //$NON-NLS-1$
        + "\nStructuredDocumentRegion: " + StringUtils.escape(getDocumentRegion().toString()) //$NON-NLS-1$
        + "\nRegion: " + getRegion() //$NON-NLS-1$
        + "\nMatch string: '" + StringUtils.escape(getMatchString()) + "'" //$NON-NLS-2$//$NON-NLS-1$
        + "\nOffsets: [" + getReplacementBeginPosition() + "-" + (getReplacementBeginPosition() + getReplacementLength()) + "]\n"; //$NON-NLS-3$//$NON-NLS-2$//$NON-NLS-1$
  }

}
