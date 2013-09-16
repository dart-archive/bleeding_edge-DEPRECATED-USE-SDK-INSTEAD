/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.contentassist;

import java.util.Properties;
import java.util.Vector;

import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;

/**
 * <p>
 * Default implementation of the {@link AbstractXMLCompletionProposalComputer}, defaults are to do
 * nothing
 * </p>
 */
public class DefaultXMLCompletionProposalComputer extends AbstractXMLCompletionProposalComputer {

  /**
   * Default behavior is do to nothing.
   * 
   * @see org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer#sessionEnded()
   */
  public void sessionEnded() {
    //default behavior is to do nothing
  }

  /**
   * Default behavior is do to nothing.
   * 
   * @see org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer#sessionStarted()
   */
  public void sessionStarted() {
    //default behavior is to do nothing
  }

  /**
   * Default behavior is do to nothing.
   * 
   * @see org.eclipse.wst.xml.ui.internal.contentassist.AbstractXMLCompletionProposalComputer#addAttributeNameProposals(org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest,
   *      org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext)
   */
  protected void addAttributeNameProposals(ContentAssistRequest contentAssistRequest,
      CompletionProposalInvocationContext context) {
    //default behavior is to do nothing
  }

  /**
   * Default behavior is do to nothing.
   * 
   * @see org.eclipse.wst.xml.ui.internal.contentassist.AbstractXMLCompletionProposalComputer#addAttributeValueProposals(org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest,
   *      org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext)
   */
  protected void addAttributeValueProposals(ContentAssistRequest contentAssistRequest,
      CompletionProposalInvocationContext context) {
    //default behavior is to do nothing
  }

  /**
   * Default behavior is do to nothing.
   * 
   * @see org.eclipse.wst.xml.ui.internal.contentassist.AbstractXMLCompletionProposalComputer#addCommentProposal(org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest,
   *      org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext)
   */
  protected void addCommentProposal(ContentAssistRequest contentAssistRequest,
      CompletionProposalInvocationContext context) {
    //default behavior is to do nothing
  }

  /**
   * Default behavior is do to nothing.
   * 
   * @see org.eclipse.wst.xml.ui.internal.contentassist.AbstractXMLCompletionProposalComputer#addDocTypeProposal(org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest,
   *      org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext)
   */
  protected void addDocTypeProposal(ContentAssistRequest contentAssistRequest,
      CompletionProposalInvocationContext context) {
    //default behavior is to do nothing
  }

  /**
   * Default behavior is do to nothing.
   * 
   * @see org.eclipse.wst.xml.ui.internal.contentassist.AbstractXMLCompletionProposalComputer#addEmptyDocumentProposals(org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest,
   *      org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext)
   */
  protected void addEmptyDocumentProposals(ContentAssistRequest contentAssistRequest,
      CompletionProposalInvocationContext context) {
    //default behavior is to do nothing
  }

  /**
   * Default behavior is do to nothing.
   * 
   * @see org.eclipse.wst.xml.ui.internal.contentassist.AbstractXMLCompletionProposalComputer#addEndTagNameProposals(org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest,
   *      org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext)
   */
  protected void addEndTagNameProposals(ContentAssistRequest contentAssistRequest,
      CompletionProposalInvocationContext context) {
    //default behavior is to do nothing
  }

  /**
   * Default behavior is do to nothing.
   * 
   * @see org.eclipse.wst.xml.ui.internal.contentassist.AbstractXMLCompletionProposalComputer#addEndTagProposals(org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest,
   *      org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext)
   */
  protected void addEndTagProposals(ContentAssistRequest contentAssistRequest,
      CompletionProposalInvocationContext context) {
    //default behavior is to do nothing
  }

  /**
   * Default behavior is do to nothing.
   * 
   * @see org.eclipse.wst.xml.ui.internal.contentassist.AbstractXMLCompletionProposalComputer#addEntityProposals(org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest,
   *      org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion,
   *      org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode,
   *      org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext)
   */
  protected void addEntityProposals(ContentAssistRequest contentAssistRequest,
      ITextRegion completionRegion, IDOMNode treeNode, CompletionProposalInvocationContext context) {
    //default behavior is to do nothing
  }

  /**
   * Default behavior is do to nothing.
   * 
   * @see org.eclipse.wst.xml.ui.internal.contentassist.AbstractXMLCompletionProposalComputer#addEntityProposals(java.util.Vector,
   *      java.util.Properties, java.lang.String, int,
   *      org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion,
   *      org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion,
   *      org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext)
   */
  protected void addEntityProposals(Vector proposals, Properties map, String key, int nodeOffset,
      IStructuredDocumentRegion sdRegion, ITextRegion completionRegion,
      CompletionProposalInvocationContext context) {
    //default behavior is to do nothing
  }

  /**
   * Default behavior is do to nothing.
   * 
   * @see org.eclipse.wst.xml.ui.internal.contentassist.AbstractXMLCompletionProposalComputer#addPCDATAProposal(java.lang.String,
   *      org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest,
   *      org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext)
   */
  protected void addPCDATAProposal(String nodeName, ContentAssistRequest contentAssistRequest,
      CompletionProposalInvocationContext context) {
    //default behavior is to do nothing
  }

  /**
   * Default behavior is do to nothing.
   * 
   * @see org.eclipse.wst.xml.ui.internal.contentassist.AbstractXMLCompletionProposalComputer#addStartDocumentProposals(org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest,
   *      org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext)
   */
  protected void addStartDocumentProposals(ContentAssistRequest contentAssistRequest,
      CompletionProposalInvocationContext context) {
    //default behavior is to do nothing
  }

  /**
   * Default behavior is do to nothing.
   * 
   * @see org.eclipse.wst.xml.ui.internal.contentassist.AbstractXMLCompletionProposalComputer#addTagCloseProposals(org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest,
   *      org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext)
   */
  protected void addTagCloseProposals(ContentAssistRequest contentAssistRequest,
      CompletionProposalInvocationContext context) {
    //default behavior is to do nothing
  }

  /**
   * Default behavior is do to nothing.
   * 
   * @see org.eclipse.wst.xml.ui.internal.contentassist.AbstractXMLCompletionProposalComputer#addTagInsertionProposals(org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest,
   *      int, org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext)
   */
  protected void addTagInsertionProposals(ContentAssistRequest contentAssistRequest,
      int childPosition, CompletionProposalInvocationContext context) {
    //default behavior is to do nothing
  }

  /**
   * Default behavior is do to nothing.
   * 
   * @see org.eclipse.wst.xml.ui.internal.contentassist.AbstractXMLCompletionProposalComputer#addTagNameProposals(org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest,
   *      int, org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext)
   */
  protected void addTagNameProposals(ContentAssistRequest contentAssistRequest, int childPosition,
      CompletionProposalInvocationContext context) {
    //default behavior is to do nothing
  }
}
