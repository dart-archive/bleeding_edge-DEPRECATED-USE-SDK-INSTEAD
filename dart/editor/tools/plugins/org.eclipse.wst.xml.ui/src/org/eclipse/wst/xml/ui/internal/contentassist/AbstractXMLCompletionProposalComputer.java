/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.contentassist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionContainer;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer;
import org.eclipse.wst.sse.ui.internal.contentassist.ContentAssistUtils;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.eclipse.wst.xml.ui.internal.Logger;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * <p>
 * Implements the framework for making proposals in XML type documents. Deals with all the nastiness
 * needed to figure out where in an XML type document the content assist was invoked and then calls
 * one of many abstract methods depending on the area the content assist was invoked. In this way
 * implementers only have to worry about implementing what to do in each situation while not having
 * to deal with figuring out which situation the content assist was invoked in.
 * </p>
 * 
 * @base org.eclipse.wst.xml.ui.internal.contentassist.AbstractContentAssistProcessor
 * @see DefaultXMLCompletionProposalComputer
 */
public abstract class AbstractXMLCompletionProposalComputer implements ICompletionProposalComputer {
  /**
   * ISSUE: this is a bit of hidden JSP knowledge that was implemented this way for expedency.
   * Should be evolved in future to depend on "nestedContext".
   */
  private class DOMJSPRegionContextsPrivateCopy {
    private static final String JSP_CLOSE = "JSP_CLOSE"; //$NON-NLS-1$
    private static final String JSP_COMMENT_CLOSE = "JSP_COMMENT_CLOSE"; //$NON-NLS-1$

    private static final String JSP_COMMENT_OPEN = "JSP_COMMENT_OPEN"; //$NON-NLS-1$
    private static final String JSP_DECLARATION_OPEN = "JSP_DECLARATION_OPEN"; //$NON-NLS-1$
    private static final String JSP_DIRECTIVE_CLOSE = "JSP_DIRECTIVE_CLOSE"; //$NON-NLS-1$
    private static final String JSP_DIRECTIVE_NAME = "JSP_DIRECTIVE_NAME"; //$NON-NLS-1$

    private static final String JSP_DIRECTIVE_OPEN = "JSP_DIRECTIVE_OPEN"; //$NON-NLS-1$
    private static final String JSP_EXPRESSION_OPEN = "JSP_EXPRESSION_OPEN"; //$NON-NLS-1$

    private static final String JSP_ROOT_TAG_NAME = "JSP_ROOT_TAG_NAME"; //$NON-NLS-1$

    private static final String JSP_SCRIPTLET_OPEN = "JSP_SCRIPTLET_OPEN"; //$NON-NLS-1$
  }

  private String fErrorMessage;
  private ITextViewer fTextViewer;

  public AbstractXMLCompletionProposalComputer() {
    fErrorMessage = null;
    fTextViewer = null;
  }

  /**
   * <p>
   * Return a list of proposed code completions based on the specified location within the document
   * that corresponds to the current cursor position within the text-editor control.
   * </p>
   * 
   * @see org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer#computeCompletionProposals(org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext,
   *      org.eclipse.core.runtime.IProgressMonitor)
   */
  public List computeCompletionProposals(CompletionProposalInvocationContext context,
      IProgressMonitor monitor) {

    ITextViewer textViewer = context.getViewer();
    int documentPosition = context.getInvocationOffset();

    setErrorMessage(null);

    fTextViewer = textViewer;

    IndexedRegion treeNode = ContentAssistUtils.getNodeAt(textViewer, documentPosition);

    Node node = (Node) treeNode;
    while ((node != null) && (node.getNodeType() == Node.TEXT_NODE)
        && (node.getParentNode() != null)) {
      node = node.getParentNode();
    }
    IDOMNode xmlnode = (IDOMNode) node;

    ContentAssistRequest contentAssistRequest = null;

    IStructuredDocumentRegion sdRegion = getStructuredDocumentRegion(documentPosition);
    ITextRegion completionRegion = getCompletionRegion(documentPosition, node);

    String matchString = getMatchString(sdRegion, completionRegion, documentPosition);

    // Handle empty Documents
    if (completionRegion == null) {
      if (((treeNode == null) || (((Node) treeNode).getNodeType() == Node.DOCUMENT_NODE))
          && (completionRegion == null)
          && ((xmlnode == null) || (xmlnode.getChildNodes() == null) || (xmlnode.getChildNodes().getLength() == 0))) {

        IStructuredModel sModel = null;
        try {
          if (textViewer.getDocument() instanceof IStructuredDocument) {
            sModel = StructuredModelManager.getModelManager().getModelForRead(
                (IStructuredDocument) textViewer.getDocument());
          }
          if (sModel != null) {
            IDOMDocument docNode = ((IDOMModel) sModel).getDocument();
            contentAssistRequest = new ContentAssistRequest(docNode, docNode, sdRegion,
                completionRegion, documentPosition, 0, null);
            addEmptyDocumentProposals(contentAssistRequest, context);
          }
        } finally {
          if (sModel != null) {
            sModel.releaseFromRead();
          }
        }
        if (contentAssistRequest == null) {
          Logger.logException(new IllegalStateException("problem getting model")); //$NON-NLS-1$
          return new ArrayList(0);
        }

        ICompletionProposal[] props = contentAssistRequest.getCompletionProposals();
        return (props != null) ? Arrays.asList(props) : new ArrayList(0);
      }
      // MASSIVE ERROR CONDITION
      Logger.logException(new IllegalStateException("completion region was null")); //$NON-NLS-1$
      setErrorMessage(XMLUIMessages.SEVERE_internal_error_occu_UI_);
      contentAssistRequest = new ContentAssistRequest((Node) treeNode, node.getParentNode(),
          sdRegion, completionRegion, documentPosition, 0, ""); //$NON-NLS-1$
      ICompletionProposal[] props = contentAssistRequest.getCompletionProposals();
      return (props != null) ? Arrays.asList(props) : new ArrayList(0);
    }

    // catch documents where no region can be determined
    if ((xmlnode.getNodeType() == Node.DOCUMENT_NODE)
        && ((completionRegion == null) || (xmlnode.getChildNodes() == null) || (xmlnode.getChildNodes().getLength() == 0))) {

      contentAssistRequest = computeStartDocumentProposals(matchString, completionRegion,
          (IDOMNode) treeNode, xmlnode, context);
      ICompletionProposal[] props = contentAssistRequest.getCompletionProposals();
      return (props != null) ? Arrays.asList(props) : new ArrayList(0);
    }

    // compute normal proposals
    contentAssistRequest = computeCompletionProposals(matchString, completionRegion,
        (IDOMNode) treeNode, xmlnode, context);
    if (contentAssistRequest == null) {
      contentAssistRequest = new ContentAssistRequest((Node) treeNode, node.getParentNode(),
          sdRegion, completionRegion, documentPosition, 0, ""); //$NON-NLS-1$
      setErrorMessage(XMLUIMessages.Content_Assist_not_availab_UI_);
    }

    /*
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=123892 Only set this error message if nothing
     * else was already set
     */
    if (contentAssistRequest.getProposals().size() == 0 && getErrorMessage() == null) {
      setErrorMessage(XMLUIMessages.Content_Assist_not_availab_UI_);
    }

    ICompletionProposal[] props = contentAssistRequest.getCompletionProposals();
    return (props != null) ? Arrays.asList(props) : new ArrayList(0);
  }

  /**
   * <p>
   * Returns information about possible contexts based on the specified location within the document
   * that corresponds to the current cursor position within the text viewer.
   * </p>
   * 
   * @see org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer#computeContextInformation(org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext,
   *      org.eclipse.core.runtime.IProgressMonitor)
   */
  public List computeContextInformation(CompletionProposalInvocationContext context,
      IProgressMonitor monitor) {

    //no default context info
    return Collections.EMPTY_LIST;
  }

  /**
   * @see org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer#getErrorMessage()
   */
  public String getErrorMessage() {
    return fErrorMessage;
  }

  /**
   * Add proposals for attribute names
   * 
   * @param contentAssistRequest
   * @param context
   */
  protected abstract void addAttributeNameProposals(ContentAssistRequest contentAssistRequest,
      CompletionProposalInvocationContext context);

  /**
   * Add proposals for attribute values
   * 
   * @param contentAssistRequest
   * @param context
   */
  protected abstract void addAttributeValueProposals(ContentAssistRequest contentAssistRequest,
      CompletionProposalInvocationContext context);

  /**
   * Add comment proposals
   * 
   * @param contentAssistRequest
   * @param context
   */
  protected abstract void addCommentProposal(ContentAssistRequest contentAssistRequest,
      CompletionProposalInvocationContext context);

  /**
   * Add the proposals for a completely empty document
   * 
   * @param contentAssistRequest
   * @param context
   */
  protected abstract void addEmptyDocumentProposals(ContentAssistRequest contentAssistRequest,
      CompletionProposalInvocationContext context);

  /**
   * Add the proposals for the name in an end tag
   * 
   * @param contentAssistRequest
   * @param context
   */
  protected abstract void addEndTagNameProposals(ContentAssistRequest contentAssistRequest,
      CompletionProposalInvocationContext context);

  /**
   * Prompt for end tags to a non-empty Node that hasn't ended Handles these cases: <br>
   * <tagOpen>| <br>
   * <tagOpen>< |<br>
   * <tagOpen></ |
   * 
   * @param contentAssistRequest
   * @param context
   */
  protected abstract void addEndTagProposals(ContentAssistRequest contentAssistRequest,
      CompletionProposalInvocationContext context);

  /**
   * Add entity proposals
   * 
   * @param contentAssistRequest
   * @param completionRegion
   * @param treeNode
   * @param context
   */
  protected abstract void addEntityProposals(ContentAssistRequest contentAssistRequest,
      ITextRegion completionRegion, IDOMNode treeNode, CompletionProposalInvocationContext context);

  /**
   * add entity proposals
   * 
   * @param proposals
   * @param map
   * @param key
   * @param nodeOffset
   * @param sdRegion
   * @param completionRegion
   * @param context
   */
  protected abstract void addEntityProposals(Vector proposals, Properties map, String key,
      int nodeOffset, IStructuredDocumentRegion sdRegion, ITextRegion completionRegion,
      CompletionProposalInvocationContext context);

  /**
   * Add PCData proposals
   * 
   * @param nodeName
   * @param contentAssistRequest
   * @param context
   */
  protected abstract void addPCDATAProposal(String nodeName,
      ContentAssistRequest contentAssistRequest, CompletionProposalInvocationContext context);

  /**
   * Add start document proposals
   * 
   * @param contentAssistRequest
   * @param context
   */
  protected abstract void addStartDocumentProposals(ContentAssistRequest contentAssistRequest,
      CompletionProposalInvocationContext context);

  /**
   * Close an unclosed start tag
   * 
   * @param contentAssistRequest
   * @param context
   */
  protected abstract void addTagCloseProposals(ContentAssistRequest contentAssistRequest,
      CompletionProposalInvocationContext context);

  /**
   * Add tag insertion proposals
   * 
   * @param contentAssistRequest
   * @param childPosition
   * @param context
   */
  protected abstract void addTagInsertionProposals(ContentAssistRequest contentAssistRequest,
      int childPosition, CompletionProposalInvocationContext context);

  /**
   * Add tag name proposals
   * 
   * @param contentAssistRequest
   * @param childPosition
   * @param context
   */
  protected abstract void addTagNameProposals(ContentAssistRequest contentAssistRequest,
      int childPosition, CompletionProposalInvocationContext context);

  /**
   * @param errorMessage the reason why computeProposals was not able to find any completions.
   */
  protected void setErrorMessage(String errorMessage) {
    fErrorMessage = errorMessage;
  }

  /**
   * <p>
   * This does all the magic of figuring out where in the XML type document the content assist was
   * invoked and then calling the corresponding method to add the correct proposals
   * </p>
   * <p>
   * <b>NOTE: </b>if overriding be sure to make super call back to this method otherwise you will
   * loose all of the proposals generated by this method
   * </p>
   * 
   * @param matchString
   * @param completionRegion
   * @param treeNode
   * @param xmlnode
   * @param context
   * @return {@link ContentAssistRequest} that now has all the proposals in it
   */
  protected ContentAssistRequest computeCompletionProposals(String matchString,
      ITextRegion completionRegion, IDOMNode treeNode, IDOMNode xmlnode,
      CompletionProposalInvocationContext context) {

    int documentPosition = context.getInvocationOffset();

    ContentAssistRequest contentAssistRequest = null;
    String regionType = completionRegion.getType();
    IStructuredDocumentRegion sdRegion = getStructuredDocumentRegion(documentPosition);

    // Handle the most common and best supported cases
    if ((xmlnode.getNodeType() == Node.ELEMENT_NODE)
        || (xmlnode.getNodeType() == Node.DOCUMENT_NODE)) {
      if (regionType == DOMRegionContext.XML_TAG_OPEN) {
        contentAssistRequest = computeTagOpenProposals(matchString, completionRegion, treeNode,
            xmlnode, context);
      } else if (regionType == DOMRegionContext.XML_TAG_NAME) {
        contentAssistRequest = computeTagNameProposals(matchString, completionRegion, treeNode,
            xmlnode, context);
      } else if (regionType == DOMRegionContext.XML_TAG_ATTRIBUTE_NAME) {
        contentAssistRequest = computeAttributeProposals(matchString, completionRegion, treeNode,
            xmlnode, context);
      } else if (regionType == DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS) {
        contentAssistRequest = computeEqualsProposals(matchString, completionRegion, treeNode,
            xmlnode, context);
      } else if ((regionType == DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE)
          && (documentPosition == sdRegion.getTextEndOffset())
          && (sdRegion.getText(completionRegion).endsWith("\"") || sdRegion.getText(completionRegion).endsWith("\'"))) { //$NON-NLS-1$ //$NON-NLS-2$ 
        // this is for when the cursor is at the end of the closing
        // quote for an attribute..
        IDOMNode actualNode = (IDOMNode) xmlnode.getModel().getIndexedRegion(
            sdRegion.getStartOffset(completionRegion));
        contentAssistRequest = new ContentAssistRequest(actualNode, actualNode, sdRegion,
            completionRegion, documentPosition, 0, matchString);
        addTagCloseProposals(contentAssistRequest, context);
      } else if (regionType == DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE) {
        contentAssistRequest = computeAttributeValueProposals(matchString, completionRegion,
            treeNode, xmlnode, context);
      } else if ((regionType == DOMRegionContext.XML_TAG_CLOSE)
          || (regionType == DOMRegionContext.XML_EMPTY_TAG_CLOSE)
          || (regionType.equals(DOMJSPRegionContextsPrivateCopy.JSP_DIRECTIVE_CLOSE))) {

        contentAssistRequest = computeTagCloseProposals(matchString, completionRegion, treeNode,
            xmlnode, context);
      } else if (regionType == DOMRegionContext.XML_END_TAG_OPEN) {
        contentAssistRequest = computeEndTagOpenProposals(matchString, completionRegion, treeNode,
            xmlnode, context);
      } else if ((regionType == DOMRegionContext.XML_CONTENT)
          || (regionType == DOMRegionContext.XML_CHAR_REFERENCE)
          || (regionType == DOMRegionContext.XML_ENTITY_REFERENCE)
          || (regionType == DOMRegionContext.XML_PE_REFERENCE)) {

        contentAssistRequest = computeContentProposals(matchString, completionRegion, treeNode,
            xmlnode, context);
      }

      // These ITextRegion types begin DOM Elements as well and although
      // internally harder to assist,
      // text insertions directly before them can be made
      else if ((documentPosition == sdRegion.getStartOffset(completionRegion))
          && (regionType.equals(DOMJSPRegionContextsPrivateCopy.JSP_COMMENT_OPEN)
              || regionType.equals(DOMJSPRegionContextsPrivateCopy.JSP_DECLARATION_OPEN)
              || regionType.equals(DOMJSPRegionContextsPrivateCopy.JSP_DIRECTIVE_OPEN)
              || regionType.equals(DOMJSPRegionContextsPrivateCopy.JSP_EXPRESSION_OPEN)
              || regionType.equals(DOMJSPRegionContextsPrivateCopy.JSP_SCRIPTLET_OPEN)
              || (regionType == DOMRegionContext.XML_DECLARATION_OPEN)
              || (regionType == DOMRegionContext.XML_PI_OPEN)
              || (regionType == DOMRegionContext.XML_COMMENT_OPEN) || (regionType == DOMRegionContext.XML_CDATA_OPEN))) {

        contentAssistRequest = new ContentAssistRequest(treeNode, xmlnode.getParentNode(),
            sdRegion, completionRegion, documentPosition, 0, matchString);
        addTagInsertionProposals(contentAssistRequest, getElementPosition(treeNode), context);
        addStartDocumentProposals(contentAssistRequest, context);
      }
    }
    // Not a Document or Element? (odd cases go here for now)
    else if (isCloseRegion(completionRegion)) {
      contentAssistRequest = new ContentAssistRequest(treeNode, xmlnode.getParentNode(), sdRegion,
          completionRegion, sdRegion.getStartOffset(completionRegion)
              + completionRegion.getLength(), 0, matchString);
      addStartDocumentProposals(contentAssistRequest, context);
      if (documentPosition >= sdRegion.getTextEndOffset(completionRegion)) {
        addTagInsertionProposals(contentAssistRequest, getElementPosition(treeNode) + 1, context);
      }
    } else if ((documentPosition == sdRegion.getStartOffset(completionRegion))
        && (regionType.equals(DOMJSPRegionContextsPrivateCopy.JSP_COMMENT_OPEN)
            || regionType.equals(DOMJSPRegionContextsPrivateCopy.JSP_DECLARATION_OPEN)
            || regionType.equals(DOMJSPRegionContextsPrivateCopy.JSP_DIRECTIVE_OPEN)
            || regionType.equals(DOMJSPRegionContextsPrivateCopy.JSP_EXPRESSION_OPEN)
            || regionType.equals(DOMJSPRegionContextsPrivateCopy.JSP_SCRIPTLET_OPEN)
            || (regionType == DOMRegionContext.XML_DECLARATION_OPEN)
            || (regionType == DOMRegionContext.XML_PI_OPEN)
            || (regionType == DOMRegionContext.XML_COMMENT_OPEN) || (regionType == DOMRegionContext.XML_CDATA_OPEN))) {

      contentAssistRequest = new ContentAssistRequest(treeNode, xmlnode.getParentNode(), sdRegion,
          completionRegion, documentPosition, 0, matchString);
      addTagInsertionProposals(contentAssistRequest, getElementPosition(treeNode), context);
      addStartDocumentProposals(contentAssistRequest, context);
    }
    return contentAssistRequest;
  }

  /**
   * <p>
   * Similar to
   * {@link #computeCompletionProposals(CompletionProposalInvocationContext, IProgressMonitor)} only
   * specificly for attribute proposals
   * </p>
   * <p>
   * Implementers should not override this method, it is made available to implementers so that if
   * they override
   * {@link #computeCompletionProposals(String, ITextRegion, IDOMNode, IDOMNode, CompletionProposalInvocationContext)}
   * they can call this method if needed
   * </p>
   * 
   * @param matchString
   * @param completionRegion
   * @param nodeAtOffset
   * @param node
   * @param context
   * @return
   */
  protected final ContentAssistRequest computeAttributeProposals(String matchString,
      ITextRegion completionRegion, IDOMNode nodeAtOffset, IDOMNode node,
      CompletionProposalInvocationContext context) {

    int documentPosition = context.getInvocationOffset();
    ITextViewer viewer = context.getViewer();
    ContentAssistRequest contentAssistRequest = null;
    IStructuredDocumentRegion sdRegion = getStructuredDocumentRegion(documentPosition);
    // if the attribute name is selected, replace it instead of creating a new attribute
    if (documentPosition <= sdRegion.getStartOffset(completionRegion)
        && (viewer != null && viewer.getSelectedRange().y != (sdRegion.getEndOffset(completionRegion) - sdRegion.getStartOffset(completionRegion)))) {
      // setup to insert new attributes
      contentAssistRequest = new ContentAssistRequest(nodeAtOffset, node, sdRegion,
          completionRegion, documentPosition, 0, matchString);
    } else {
      // Setup to replace an existing attribute name
      contentAssistRequest = new ContentAssistRequest(nodeAtOffset, node, sdRegion,
          completionRegion, sdRegion.getStartOffset(completionRegion),
          completionRegion.getTextLength(), matchString);
    }
    addAttributeNameProposals(contentAssistRequest, context);
    contentAssistRequest.setReplacementBeginPosition(documentPosition);
    contentAssistRequest.setReplacementLength(0);
    if ((node.getFirstStructuredDocumentRegion() != null)
        && (!node.getFirstStructuredDocumentRegion().isEnded())) {
      addTagCloseProposals(contentAssistRequest, context);
    }
    return contentAssistRequest;
  }

  /**
   * <p>
   * this is the position the cursor should be in after the proposal is applied
   * </p>
   * 
   * @param proposedText
   * @return the position the cursor should be in after the proposal is applied
   */
  protected static int getCursorPositionForProposedText(String proposedText) {
    int cursorAdjustment;
    cursorAdjustment = proposedText.indexOf("\"\"") + 1; //$NON-NLS-1$
    // otherwise, after the first tag
    if (cursorAdjustment == 0) {
      cursorAdjustment = proposedText.indexOf('>') + 1;
    }
    if (cursorAdjustment == 0) {
      cursorAdjustment = proposedText.length() + 1;
    }

    return cursorAdjustment;
  }

  /**
   * <p>
   * helpful utility method for determining if one string starts with another one. This is case
   * insensitive. If either are null then result is <code>true</code>
   * </p>
   * 
   * @param aString the string to check to see if it starts with the given prefix
   * @param prefix check that the given string starts with this prefix
   * @return <code>true</code> if the given string starts with the given prefix, <code>false</code>
   *         otherwise
   */
  protected static boolean beginsWith(String aString, String prefix) {
    if ((aString == null) || (prefix == null)) {
      return true;
    }
    return aString.toLowerCase().startsWith(prefix.toLowerCase());
  }

  private ContentAssistRequest computeAttributeValueProposals(String matchString,
      ITextRegion completionRegion, IDOMNode nodeAtOffset, IDOMNode node,
      CompletionProposalInvocationContext context) {

    int documentPosition = context.getInvocationOffset();

    ContentAssistRequest contentAssistRequest = null;
    IStructuredDocumentRegion sdRegion = getStructuredDocumentRegion(documentPosition);
    if ((documentPosition > sdRegion.getStartOffset(completionRegion)
        + completionRegion.getTextLength())
        && (sdRegion.getStartOffset(completionRegion) + completionRegion.getTextLength() != sdRegion.getStartOffset(completionRegion)
            + completionRegion.getLength())) {
      // setup to add a new attribute at the documentPosition
      IDOMNode actualNode = (IDOMNode) node.getModel().getIndexedRegion(
          sdRegion.getStartOffset(completionRegion));
      contentAssistRequest = new ContentAssistRequest(actualNode, actualNode, sdRegion,
          completionRegion, documentPosition, 0, matchString);
      addAttributeNameProposals(contentAssistRequest, context);
      if ((actualNode.getFirstStructuredDocumentRegion() != null)
          && !actualNode.getFirstStructuredDocumentRegion().isEnded()) {
        addTagCloseProposals(contentAssistRequest, context);
      }
    } else {
      // setup to replace the existing value
      if (!nodeAtOffset.getFirstStructuredDocumentRegion().isEnded()
          && (documentPosition < sdRegion.getStartOffset(completionRegion))) {
        // if the IStructuredDocumentRegion isn't closed and the
        // cursor is in front of the value, add
        contentAssistRequest = new ContentAssistRequest(nodeAtOffset, node, sdRegion,
            completionRegion, documentPosition, 0, matchString);
        addAttributeNameProposals(contentAssistRequest, context);
      } else {
        int replaceLength = completionRegion.getTextLength();

        //if container region, be sure replace length is only the attribute value region not the entire container
        if (completionRegion instanceof ITextRegionContainer) {
          ITextRegion openRegion = ((ITextRegionContainer) completionRegion).getFirstRegion();
          ITextRegion closeRegion = ((ITextRegionContainer) completionRegion).getLastRegion();

          /*
           * check to see if the container is opened the same way its closed. Such as: <img src=' '
           * But not: <img src='
           * 
           * </body> </html> In the latter case we only want to replace the opening text of the
           * container Admittedly crude test, but effective.
           */
          if (openRegion.getType() != closeRegion.getType()) {
            replaceLength = openRegion.getTextLength();
          }
        }

        contentAssistRequest = new ContentAssistRequest(nodeAtOffset, node, sdRegion,
            completionRegion, sdRegion.getStartOffset(completionRegion), replaceLength, matchString);

        addAttributeValueProposals(contentAssistRequest, context);
      }
    }
    return contentAssistRequest;
  }

  private ContentAssistRequest computeContentProposals(String matchString,
      ITextRegion completionRegion, IDOMNode nodeAtOffset, IDOMNode node,
      CompletionProposalInvocationContext context) {

    int documentPosition = context.getInvocationOffset();
    ContentAssistRequest contentAssistRequest = null;

    // setup to add children at the content node's position
    contentAssistRequest = new ContentAssistRequest(nodeAtOffset, node,
        getStructuredDocumentRegion(documentPosition), completionRegion, documentPosition, 0,
        matchString);
    if ((node != null) && (node.getNodeType() == Node.DOCUMENT_NODE)
        && (((Document) node).getDoctype() == null)) {
      addStartDocumentProposals(contentAssistRequest, context);
    }
    addTagInsertionProposals(contentAssistRequest, getElementPosition(nodeAtOffset), context);
    if (node.getNodeType() != Node.DOCUMENT_NODE) {
      addEndTagProposals(contentAssistRequest, context);
    }
    // entities?
    addEntityProposals(contentAssistRequest, completionRegion, node, context);
    // addEntityProposals(contentAssistRequest);
    return contentAssistRequest;
  }

  private ContentAssistRequest computeEndTagOpenProposals(String matchString,
      ITextRegion completionRegion, IDOMNode nodeAtOffset, IDOMNode node,
      CompletionProposalInvocationContext context) {

    int documentPosition = context.getInvocationOffset();
    ContentAssistRequest contentAssistRequest = null;
    IStructuredDocumentRegion sdRegion = getStructuredDocumentRegion(documentPosition);
    int completionRegionStart = sdRegion.getStartOffset(completionRegion);
    int completionRegionLength = completionRegion.getLength();
    IStructuredDocumentRegion sdRegionAtCompletionOffset = node.getStructuredDocument().getRegionAtCharacterOffset(
        completionRegionStart + completionRegionLength);
    ITextRegion regionAtEndOfCompletion = sdRegionAtCompletionOffset.getRegionAtCharacterOffset(completionRegionStart
        + completionRegionLength);

    if ((documentPosition != completionRegionStart) && (regionAtEndOfCompletion != null)
        && (regionAtEndOfCompletion.getType() == DOMRegionContext.XML_TAG_NAME)) {
      ITextRegion nameRegion = regionAtEndOfCompletion;
      contentAssistRequest = new ContentAssistRequest(nodeAtOffset, nodeAtOffset.getParentNode(),
          sdRegion, completionRegion, sdRegion.getStartOffset(nameRegion),
          nameRegion.getTextLength(), matchString);
    } else {
      if (nodeAtOffset.getFirstStructuredDocumentRegion() == sdRegion) {
        // abnormal case, this unmatched end tag will be a sibling
        contentAssistRequest = new ContentAssistRequest(nodeAtOffset, nodeAtOffset.getParentNode(),
            sdRegion, completionRegion, documentPosition, 0, matchString);
      } else {
        // normal case, this end tag is the parent
        contentAssistRequest = new ContentAssistRequest(nodeAtOffset, nodeAtOffset, sdRegion,
            completionRegion, documentPosition, 0, matchString);
      }
    }
    // if (documentPosition >= sdRegion.getStartOffset(completionRegion) +
    // completionRegion.getTextLength())
    addEndTagProposals(contentAssistRequest, context);
    // else
    if (completionRegionStart == documentPosition) {
      // positioned at start of end tag
      addTagInsertionProposals(contentAssistRequest, node.getChildNodes().getLength(), context);
    }
    return contentAssistRequest;
  }

  private ContentAssistRequest computeEqualsProposals(String matchString,
      ITextRegion completionRegion, IDOMNode nodeAtOffset, IDOMNode node,
      CompletionProposalInvocationContext context) {

    int documentPosition = context.getInvocationOffset();
    ContentAssistRequest contentAssistRequest = null;
    IStructuredDocumentRegion sdRegion = getStructuredDocumentRegion(documentPosition);
    ITextRegion valueRegion = node.getStartStructuredDocumentRegion().getRegionAtCharacterOffset(
        sdRegion.getStartOffset(completionRegion) + completionRegion.getLength());
    if ((valueRegion != null)
        && (valueRegion.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE)
        && (sdRegion.getStartOffset(valueRegion) <= documentPosition)) {
      // replace the adjacent attribute value
      contentAssistRequest = new ContentAssistRequest(nodeAtOffset, node, sdRegion, valueRegion,
          sdRegion.getStartOffset(valueRegion), valueRegion.getTextLength(), matchString);
    } else {
      // append an attribute value after the '='
      contentAssistRequest = new ContentAssistRequest(nodeAtOffset, node, sdRegion,
          completionRegion, documentPosition, 0, matchString);
    }
    addAttributeValueProposals(contentAssistRequest, context);
    return contentAssistRequest;
  }

  private ContentAssistRequest computeStartDocumentProposals(String matchString,
      ITextRegion completionRegion, IDOMNode nodeAtOffset, IDOMNode node,
      CompletionProposalInvocationContext context) {

    int documentPosition = context.getInvocationOffset();

    // setup for a non-empty document, but one that hasn't been formally
    // started
    ContentAssistRequest contentAssistRequest = null;
    contentAssistRequest = new ContentAssistRequest(nodeAtOffset, node,
        getStructuredDocumentRegion(documentPosition), completionRegion, documentPosition, 0,
        matchString);
    addStartDocumentProposals(contentAssistRequest, context);
    return contentAssistRequest;
  }

  private ContentAssistRequest computeTagCloseProposals(String matchString,
      ITextRegion completionRegion, IDOMNode nodeAtOffset, IDOMNode node,
      CompletionProposalInvocationContext context) {

    int documentPosition = context.getInvocationOffset();
    ContentAssistRequest contentAssistRequest = null;
    IStructuredDocumentRegion sdRegion = getStructuredDocumentRegion(documentPosition);

    if ((node.getNodeType() == Node.DOCUMENT_NODE) || (documentPosition >= sdRegion.getEndOffset())) {
      // this is a content request as the documentPosition is AFTER the
      // end of the closing region
      if ((node == nodeAtOffset) && (node.getParentNode() != null)) {
        node = (IDOMNode) node.getParentNode();
      }
      contentAssistRequest = new ContentAssistRequest(nodeAtOffset, node, sdRegion,
          completionRegion, documentPosition, 0, matchString);
      addTagInsertionProposals(contentAssistRequest, getElementPosition(nodeAtOffset), context);
      if ((node.getNodeType() != Node.DOCUMENT_NODE)
          && (node.getEndStructuredDocumentRegion() == null)) {
        addEndTagProposals(contentAssistRequest, context);
      }
    } else {
      // at the start of the tag's close or within it
      ITextRegion closeRegion = sdRegion.getLastRegion();
      boolean insideTag = !sdRegion.isEnded()
          || (documentPosition <= sdRegion.getStartOffset(closeRegion));
      if (insideTag) {
        // this is a request for completions within a tag
        contentAssistRequest = new ContentAssistRequest(nodeAtOffset, node, sdRegion,
            completionRegion, documentPosition, 0, matchString);
        if ((node.getNodeType() != Node.DOCUMENT_NODE)
            && (node.getEndStructuredDocumentRegion() != null)) {
          addTagCloseProposals(contentAssistRequest, context);
        }
        if (sdRegion == nodeAtOffset.getFirstStructuredDocumentRegion()) {
          contentAssistRequest.setReplacementBeginPosition(documentPosition);
          contentAssistRequest.setReplacementLength(0);
          addAttributeNameProposals(contentAssistRequest, context);
        }
      }
    }
    return contentAssistRequest;
  }

  private ContentAssistRequest computeTagNameProposals(String matchString,
      ITextRegion completionRegion, IDOMNode nodeAtOffset, IDOMNode node,
      CompletionProposalInvocationContext context) {

    int documentPosition = context.getInvocationOffset();
    ContentAssistRequest contentAssistRequest = null;
    IStructuredDocumentRegion sdRegion = getStructuredDocumentRegion(documentPosition);

    if (sdRegion != nodeAtOffset.getFirstStructuredDocumentRegion()) {
      // completing the *first* tag in "<tagname1 |<tagname2"
      IDOMNode actualNode = (IDOMNode) node.getModel().getIndexedRegion(
          sdRegion.getStartOffset(completionRegion));
      if (actualNode != null) {
        if (actualNode.getFirstStructuredDocumentRegion() == sdRegion) {
          // start tag
          if (documentPosition > sdRegion.getStartOffset(completionRegion)
              + completionRegion.getLength()) {
            // it's attributes
            contentAssistRequest = new ContentAssistRequest(actualNode, actualNode, sdRegion,
                completionRegion, documentPosition - matchString.length(), matchString.length(),
                matchString);
            if (node.getStructuredDocument().getRegionAtCharacterOffset(
                sdRegion.getStartOffset(completionRegion) - 1).getRegionAtCharacterOffset(
                sdRegion.getStartOffset(completionRegion) - 1).getType() == DOMRegionContext.XML_TAG_OPEN) {
              addAttributeNameProposals(contentAssistRequest, context);
            }
            addTagCloseProposals(contentAssistRequest, context);
          } else {
            // it's name
            contentAssistRequest = new ContentAssistRequest(actualNode, actualNode.getParentNode(),
                sdRegion, completionRegion, documentPosition - matchString.length(),
                matchString.length(), matchString);
            addTagNameProposals(contentAssistRequest, getElementPosition(actualNode), context);
          }
        } else {
          if (documentPosition >= sdRegion.getStartOffset(completionRegion)
              + completionRegion.getLength()) {
            // insert name
            contentAssistRequest = new ContentAssistRequest(actualNode, actualNode.getParentNode(),
                sdRegion, completionRegion, documentPosition, 0, matchString);
          } else {
            // replace name
            contentAssistRequest = new ContentAssistRequest(actualNode, actualNode.getParentNode(),
                sdRegion, completionRegion, sdRegion.getStartOffset(completionRegion),
                completionRegion.getTextLength(), matchString);
          }
          addEndTagNameProposals(contentAssistRequest, context);
        }
      }
    } else {
      if (documentPosition > sdRegion.getStartOffset(completionRegion)
          + completionRegion.getTextLength()) {
        // unclosed tag with only a name; should prompt for attributes
        // and a close instead
        contentAssistRequest = new ContentAssistRequest(nodeAtOffset, node, sdRegion,
            completionRegion, documentPosition - matchString.length(), matchString.length(),
            matchString);
        addAttributeNameProposals(contentAssistRequest, context);
        addTagCloseProposals(contentAssistRequest, context);
      } else {
        if (sdRegion.getRegions().get(0).getType() != DOMRegionContext.XML_END_TAG_OPEN) {
          int replaceLength = documentPosition - sdRegion.getStartOffset(completionRegion);
          contentAssistRequest = new ContentAssistRequest(node, node.getParentNode(), sdRegion,
              completionRegion, sdRegion.getStartOffset(completionRegion), replaceLength,
              matchString);
          addTagNameProposals(contentAssistRequest, getElementPosition(nodeAtOffset), context);
        } else {
          IDOMNode actualNode = (IDOMNode) node.getModel().getIndexedRegion(documentPosition);
          if (actualNode != null) {
            if (documentPosition >= sdRegion.getStartOffset(completionRegion)
                + completionRegion.getTextLength()) {
              contentAssistRequest = new ContentAssistRequest(actualNode,
                  actualNode.getParentNode(), sdRegion, completionRegion, documentPosition, 0,
                  matchString);
            } else {
              contentAssistRequest = new ContentAssistRequest(actualNode,
                  actualNode.getParentNode(), sdRegion, completionRegion,
                  sdRegion.getStartOffset(completionRegion), completionRegion.getTextLength(),
                  matchString);
            }
            addEndTagNameProposals(contentAssistRequest, context);
          }
        }
      }
    }
    return contentAssistRequest;
  }

  private ContentAssistRequest computeTagOpenProposals(String matchString,
      ITextRegion completionRegion, IDOMNode nodeAtOffset, IDOMNode node,
      CompletionProposalInvocationContext context) {

    int documentPosition = context.getInvocationOffset();
    ContentAssistRequest contentAssistRequest = null;
    IStructuredDocumentRegion sdRegion = getStructuredDocumentRegion(documentPosition);
    if (sdRegion != nodeAtOffset.getFirstStructuredDocumentRegion()
        || sdRegion.getPrevious() != null
        && sdRegion.getPrevious().getLastRegion().getType() == DOMRegionContext.XML_TAG_OPEN) {
      // completing the *first* XML_TAG_OPEN in "<<tagname"
      IDOMNode actualNode = (IDOMNode) node.getModel().getIndexedRegion(
          sdRegion.getStartOffset(completionRegion));
      if (actualNode != null) {
        if (sdRegion.getFirstRegion().getType() == DOMRegionContext.XML_END_TAG_OPEN) {
          contentAssistRequest = new ContentAssistRequest(actualNode, actualNode, sdRegion,
              completionRegion, documentPosition, 0, matchString);
          if (actualNode.hasChildNodes())
            addTagNameProposals(contentAssistRequest,
                getElementPosition(actualNode.getLastChild()), context);
          else
            addTagNameProposals(contentAssistRequest, 0, context);
        } else {
          contentAssistRequest = new ContentAssistRequest(actualNode, actualNode.getParentNode(),
              sdRegion, completionRegion, documentPosition, 0, matchString);
          addTagNameProposals(contentAssistRequest, getElementPosition(actualNode), context);
        }
        addEndTagProposals(contentAssistRequest, context); // (pa) 220850
      }
    } else {
      if (documentPosition == sdRegion.getStartOffset(completionRegion)) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
          // at the start of an existing tag, right before the '<'
          contentAssistRequest = new ContentAssistRequest(nodeAtOffset, node.getParentNode(),
              sdRegion, completionRegion, documentPosition, 0, matchString);
          addTagInsertionProposals(contentAssistRequest, getElementPosition(nodeAtOffset), context);
          addEndTagProposals(contentAssistRequest, context);
        } else if (node.getNodeType() == Node.DOCUMENT_NODE) {
          // at the opening of the VERY first tag with a '<'
          contentAssistRequest = new ContentAssistRequest(nodeAtOffset, node.getParentNode(),
              sdRegion, completionRegion, sdRegion.getStartOffset(completionRegion),
              completionRegion.getTextLength(), matchString);
          addStartDocumentProposals(contentAssistRequest, context);
        }
      } else {
        // within the white space
        ITextRegion name = getNameRegion(node.getStartStructuredDocumentRegion());
        // (pa) ITextRegion refactor
        // if (name != null && name.containsOffset(documentPosition))
        // {
        if ((name != null)
            && ((sdRegion.getStartOffset(name) <= documentPosition) && (sdRegion.getEndOffset(name) >= documentPosition))
            && (sdRegion.getLastRegion().getType() == DOMRegionContext.XML_TAG_CLOSE || sdRegion.getLastRegion().getType() == DOMRegionContext.XML_EMPTY_TAG_CLOSE)) {

          // replace the existing name
          contentAssistRequest = new ContentAssistRequest(node, node.getParentNode(), sdRegion,
              completionRegion, sdRegion.getStartOffset(name), name.getTextLength(), matchString);
        } else {
          // insert a valid new name, or possibly an end tag
          contentAssistRequest = new ContentAssistRequest(nodeAtOffset,
              ((Node) nodeAtOffset).getParentNode(), sdRegion, completionRegion, documentPosition,
              0, matchString);
          addEndTagProposals(contentAssistRequest, context);
          contentAssistRequest.setReplacementBeginPosition(documentPosition);
          contentAssistRequest.setReplacementLength(0);
        }
        addTagNameProposals(contentAssistRequest, getElementPosition(nodeAtOffset), context);
      }
    }
    return contentAssistRequest;
  }

  private ITextRegion getCompletionRegion(int offset, IStructuredDocumentRegion sdRegion) {
    ITextRegion region = sdRegion.getRegionAtCharacterOffset(offset);
    if (region == null) {
      return null;
    }

    if (sdRegion.getStartOffset(region) == offset) {
      // The offset is at the beginning of the region
      if ((sdRegion.getStartOffset(region) == sdRegion.getStartOffset())
          && (sdRegion.getPrevious() != null) && (!sdRegion.getPrevious().isEnded())) {
        // Is the region also the start of the node? If so, the
        // previous IStructuredDocumentRegion is
        // where to look for a useful region.
        region = sdRegion.getPrevious().getRegionAtCharacterOffset(offset - 1);
      } else {
        // Is there no separating whitespace from the previous region?
        // If not,
        // then that region is the important one
        ITextRegion previousRegion = sdRegion.getRegionAtCharacterOffset(offset - 1);
        if ((previousRegion != null) && (previousRegion != region)
            && (previousRegion.getTextLength() == previousRegion.getLength())) {
          region = previousRegion;
        }
      }
    } else {
      // The offset is NOT at the beginning of the region
      if ((region.getType() != DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS)
          && (offset > sdRegion.getStartOffset(region) + region.getTextLength())) { //attached XML_TAG_ATTRIBUTE_EQUALS filter due to #bug219992
        // Is the offset within the whitespace after the text in this
        // region?
        // If so, use the next region
        ITextRegion nextRegion = sdRegion.getRegionAtCharacterOffset(sdRegion.getStartOffset(region)
            + region.getLength());
        if (nextRegion != null) {
          region = nextRegion;
        }
      } else {
        // Is the offset within the important text for this region?
        // If so, then we've already got the right one.
      }
    }

    // valid WHITE_SPACE region handler (#179924)
    if ((region != null) && (region.getType() == DOMRegionContext.WHITE_SPACE)) {
      ITextRegion previousRegion = sdRegion.getRegionAtCharacterOffset(sdRegion.getStartOffset(region) - 1);
      if (previousRegion != null) {
        region = previousRegion;
      }
    }

    return region;
  }

  /**
   * Return the region whose content's require completion. This is something of a misnomer as
   * sometimes the user wants to be prompted for contents of a non-existant ITextRegion, such as for
   * enumerated attribute values following an '=' sign.
   */
  private ITextRegion getCompletionRegion(int documentPosition, Node domnode) {
    if (domnode == null) {
      return null;
    }

    ITextRegion region = null;
    int offset = documentPosition;
    IStructuredDocumentRegion flatNode = null;
    IDOMNode node = (IDOMNode) domnode;

    if (node.getNodeType() == Node.DOCUMENT_NODE) {
      if (node.getStructuredDocument().getLength() == 0) {
        return null;
      }
      ITextRegion result = node.getStructuredDocument().getRegionAtCharacterOffset(offset).getRegionAtCharacterOffset(
          offset);
      while (result == null) {
        offset--;
        result = node.getStructuredDocument().getRegionAtCharacterOffset(offset).getRegionAtCharacterOffset(
            offset);
      }
      return result;
    }

    IStructuredDocumentRegion startTag = node.getStartStructuredDocumentRegion();
    IStructuredDocumentRegion endTag = node.getEndStructuredDocumentRegion();

    // Determine if the offset is within the start
    // IStructuredDocumentRegion, end IStructuredDocumentRegion, or
    // somewhere within the Node's XML content.
    if ((startTag != null) && (startTag.getStartOffset() <= offset)
        && (offset < startTag.getStartOffset() + startTag.getLength())) {
      flatNode = startTag;
    } else if ((endTag != null) && (endTag.getStartOffset() <= offset)
        && (offset < endTag.getStartOffset() + endTag.getLength())) {
      flatNode = endTag;
    }

    if (flatNode != null) {
      // the offset is definitely within the start or end tag, continue
      // on and find the region
      region = getCompletionRegion(offset, flatNode);
    } else {
      // the docPosition is neither within the start nor the end, so it
      // must be content
      flatNode = node.getStructuredDocument().getRegionAtCharacterOffset(offset);
      // (pa) ITextRegion refactor
      // if (flatNode.contains(documentPosition)) {
      if ((flatNode.getStartOffset() <= documentPosition)
          && (flatNode.getEndOffset() >= documentPosition)) {
        // we're interesting in completing/extending the previous
        // IStructuredDocumentRegion if the current
        // IStructuredDocumentRegion isn't plain content or if it's
        // preceded by an orphan '<'
        if ((offset == flatNode.getStartOffset())
            && (flatNode.getPrevious() != null)
            && (((flatNode.getRegionAtCharacterOffset(documentPosition) != null) && (flatNode.getRegionAtCharacterOffset(
                documentPosition).getType() != DOMRegionContext.XML_CONTENT))
                || (flatNode.getPrevious().getLastRegion().getType() == DOMRegionContext.XML_TAG_OPEN) || (flatNode.getPrevious().getLastRegion().getType() == DOMRegionContext.XML_END_TAG_OPEN))) {

          // Is the region also the start of the node? If so, the
          // previous IStructuredDocumentRegion is
          // where to look for a useful region.
          region = flatNode.getPrevious().getLastRegion();
        } else if (flatNode.getEndOffset() == documentPosition) {
          region = flatNode.getLastRegion();
        } else {
          region = flatNode.getFirstRegion();
        }
      } else {
        // catch end of document positions where the docPosition isn't
        // in a IStructuredDocumentRegion
        region = flatNode.getLastRegion();
      }
    }

    return region;
  }

  private int getElementPosition(Node child) {
    Node parent = child.getParentNode();
    if (parent == null) {
      return 0;
    }

    NodeList children = parent.getChildNodes();
    if (children == null) {
      return 0;
    }
    int count = 0;

    for (int i = 0; i < children.getLength(); i++) {
      if (children.item(i) == child) {
        return count;
      } else {
        // if (children.item(i).getNodeType() == Node.ELEMENT_NODE)
        count++;
      }
    }
    return 0;
  }

  private String getMatchString(IStructuredDocumentRegion parent, ITextRegion aRegion, int offset) {
    if ((aRegion == null) || isCloseRegion(aRegion)) {
      return ""; //$NON-NLS-1$
    }
    String matchString = null;
    String regionType = aRegion.getType();
    if ((regionType == DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS)
        || (regionType == DOMRegionContext.XML_TAG_OPEN)
        || (offset > parent.getStartOffset(aRegion) + aRegion.getTextLength())) {
      matchString = ""; //$NON-NLS-1$
    } else if (regionType == DOMRegionContext.XML_CONTENT) {
      matchString = ""; //$NON-NLS-1$
    } else {
      if ((parent.getText(aRegion).length() > 0) && (parent.getStartOffset(aRegion) < offset)) {
        matchString = parent.getText(aRegion).substring(0, offset - parent.getStartOffset(aRegion));
      } else {
        matchString = ""; //$NON-NLS-1$
      }
    }
    return matchString;
  }

  private ITextRegion getNameRegion(IStructuredDocumentRegion flatNode) {
    if (flatNode == null) {
      return null;
    }
    Iterator regionList = flatNode.getRegions().iterator();
    while (regionList.hasNext()) {
      ITextRegion region = (ITextRegion) regionList.next();
      if (isNameRegion(region)) {
        return region;
      }
    }
    return null;
  }

  private boolean isCloseRegion(ITextRegion region) {
    String type = region.getType();
    return ((type == DOMRegionContext.XML_PI_CLOSE) || (type == DOMRegionContext.XML_TAG_CLOSE)
        || (type == DOMRegionContext.XML_EMPTY_TAG_CLOSE)
        || (type == DOMRegionContext.XML_CDATA_CLOSE)
        || (type == DOMRegionContext.XML_COMMENT_CLOSE)
        || (type == DOMRegionContext.XML_ATTLIST_DECL_CLOSE)
        || (type == DOMRegionContext.XML_ELEMENT_DECL_CLOSE)
        || (type == DOMRegionContext.XML_DOCTYPE_DECLARATION_CLOSE)
        || (type == DOMJSPRegionContextsPrivateCopy.JSP_CLOSE)
        || (type == DOMJSPRegionContextsPrivateCopy.JSP_COMMENT_CLOSE)
        || (type.equals(DOMJSPRegionContextsPrivateCopy.JSP_DIRECTIVE_CLOSE)) || (type == DOMRegionContext.XML_DECLARATION_CLOSE));
  }

  private boolean isNameRegion(ITextRegion region) {
    String type = region.getType();
    return ((type == DOMRegionContext.XML_TAG_NAME)
        || (type == DOMJSPRegionContextsPrivateCopy.JSP_DIRECTIVE_NAME)
        || (type == DOMRegionContext.XML_ELEMENT_DECL_NAME)
        || (type == DOMRegionContext.XML_DOCTYPE_NAME)
        || (type == DOMRegionContext.XML_ATTLIST_DECL_NAME)
        || (type == DOMJSPRegionContextsPrivateCopy.JSP_ROOT_TAG_NAME) || type.equals(DOMJSPRegionContextsPrivateCopy.JSP_DIRECTIVE_NAME));
  }

  /**
   * StructuredTextViewer must be set before using this.
   */
  private IStructuredDocumentRegion getStructuredDocumentRegion(int pos) {
    return ContentAssistUtils.getStructuredDocumentRegion(fTextViewer, pos);
  }
}
