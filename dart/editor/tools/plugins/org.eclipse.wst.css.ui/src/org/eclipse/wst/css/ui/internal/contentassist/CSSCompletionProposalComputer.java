/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.contentassist;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.wst.css.core.internal.Logger;
import org.eclipse.wst.css.core.internal.provisional.adapters.ICSSModelAdapter;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSDocument;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSModel;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSNode;
import org.eclipse.wst.html.core.internal.htmlcss.StyleAdapterFactory;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapter;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer;
import org.eclipse.wst.sse.ui.internal.contentassist.ContentAssistUtils;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.ui.internal.contentassist.XMLContentAssistUtilities;
import org.eclipse.wst.xml.ui.internal.util.SharedXMLEditorPluginImageHelper;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * Completion computer for CSS
 * </p>
 */
public class CSSCompletionProposalComputer implements ICompletionProposalComputer {
  /**
   * @see org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer#computeCompletionProposals(org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext,
   *      org.eclipse.core.runtime.IProgressMonitor)
   */
  public List computeCompletionProposals(CompletionProposalInvocationContext context,
      IProgressMonitor monitor) {
    ITextViewer viewer = context.getViewer();
    int documentPosition = context.getInvocationOffset();

    IndexedRegion indexedNode = ContentAssistUtils.getNodeAt(viewer, documentPosition);
    IDOMNode xNode = null;
    IDOMNode parent = null;
    CSSProposalArranger arranger = null;

    // If there is a selected region, we'll need to replace the text
    ITextSelection selection = (ITextSelection) viewer.getSelectionProvider().getSelection();

    // bail if we couldn't get an indexed node
    // if(indexedNode == null) return new ICompletionProposal[0];
    if (indexedNode instanceof IDOMNode) {
      xNode = (IDOMNode) indexedNode;
      parent = (IDOMNode) xNode.getParentNode();
    }
    // need to get in here if there in the no 0 region <style>|</style>
    // case
    if ((xNode != null) && xNode.getNodeName().equalsIgnoreCase(HTML40Namespace.ElementName.STYLE)) {
      // now we know the cursor is in a <style> tag w/out region
      IStructuredModel cssModel = getCSSModel(xNode);
      if (cssModel != null) {
        // adjust offsets for embedded style
        int offset = documentPosition;
        int pos = 0;
        IndexedRegion keyIndexedNode = cssModel.getIndexedRegion(pos);
        if (keyIndexedNode == null) {
          keyIndexedNode = (IndexedRegion) ((ICSSModel) cssModel).getDocument();
        }
        arranger = new CSSProposalArranger(pos, (ICSSNode) keyIndexedNode, offset,
            selection.getLength(), (char) 0);
      }
    } else if ((parent != null)
        && parent.getNodeName().equalsIgnoreCase(HTML40Namespace.ElementName.STYLE)) {
      // now we know the cursor is in a <style> tag with a region
      // use the parent because that will be the <style> tag
      IStructuredModel cssModel = getCSSModel(parent);
      if (cssModel != null) {
        // adjust offsets for embedded style
        int offset = indexedNode.getStartOffset();
        int pos = documentPosition - offset;
        IndexedRegion keyIndexedNode = cssModel.getIndexedRegion(pos);
        if (keyIndexedNode == null) {
          keyIndexedNode = (IndexedRegion) ((ICSSModel) cssModel).getDocument();
        }
        arranger = new CSSProposalArranger(pos, (ICSSNode) keyIndexedNode, offset,
            selection.getLength(), (char) 0);
      }
    } else if (indexedNode instanceof IDOMNode) {
      IDOMNode domNode = ((IDOMNode) indexedNode);
      // get model for node w/ style attribute
      IStructuredModel cssModel = getCSSModel(domNode);
      if (cssModel != null) {
        // adjust offsets for embedded style
        int textRegionStartOffset = getTextRegionStartOffset(domNode, documentPosition);
        int pos = documentPosition - textRegionStartOffset;

        char quote = (char) 0;
        try {
          quote = context.getDocument().get(textRegionStartOffset, 1).charAt(0);
        } catch (BadLocationException e) {
          Logger.logException("error getting quote character", e);
        }

        //get css indexed region
        IndexedRegion cssIndexedNode = cssModel.getIndexedRegion(pos);
        if (cssIndexedNode == null) {
          cssIndexedNode = (IndexedRegion) ((ICSSModel) cssModel).getDocument();
        }
        if (cssIndexedNode instanceof ICSSNode) {
          // inline style for a tag, not embedded
          arranger = new CSSProposalArranger(pos, (ICSSNode) cssIndexedNode, textRegionStartOffset,
              selection.getLength(), quote);
        }
      }
    } else if (indexedNode instanceof ICSSNode) {
      // when editing external CSS using CSS Designer, ICSSNode is passed.
      ICSSDocument cssdoc = ((ICSSNode) indexedNode).getOwnerDocument();
      if (cssdoc != null) {
        IStructuredModel cssModel = cssdoc.getModel();
        if (cssModel != null) {
          IndexedRegion keyIndexedNode = cssModel.getIndexedRegion(documentPosition);
          if (keyIndexedNode == null) {
            keyIndexedNode = (IndexedRegion) ((ICSSModel) cssModel).getDocument();
          }
          if (keyIndexedNode instanceof ICSSNode) {
            // inline style for a tag, not embedded
            arranger = new CSSProposalArranger(documentPosition, (ICSSNode) keyIndexedNode, 0,
                selection.getLength(), (char) 0);
          }
        }
      }
    } else if ((indexedNode == null) && ContentAssistUtils.isViewerEmpty(viewer)) {
      // the top of empty CSS Document
      IStructuredModel cssModel = null;
      try {
        cssModel = StructuredModelManager.getModelManager().getExistingModelForRead(
            viewer.getDocument());
        if (cssModel instanceof ICSSModel) {
          IndexedRegion keyIndexedNode = cssModel.getIndexedRegion(documentPosition);
          if (keyIndexedNode == null) {
            keyIndexedNode = (IndexedRegion) ((ICSSModel) cssModel).getDocument();
          }
          if (keyIndexedNode instanceof ICSSNode) {
            // inline style for a tag, not embedded
            arranger = new CSSProposalArranger(documentPosition, (ICSSNode) keyIndexedNode, 0,
                (char) 0);
          }
        }
      } finally {
        if (cssModel != null)
          cssModel.releaseFromRead();
      }
    }

    ICompletionProposal[] proposals = new ICompletionProposal[0];
    if (arranger != null) {
      proposals = arranger.getProposals();

      ICompletionProposal[] newfileproposals = new ICompletionProposal[0];
      ICompletionProposal[] anyproposals = new ICompletionProposal[0];

      // add end tag if parent is not closed
      ICompletionProposal endTag = XMLContentAssistUtilities.computeXMLEndTagProposal(viewer,
          documentPosition, indexedNode, HTML40Namespace.ElementName.STYLE,
          SharedXMLEditorPluginImageHelper.IMG_OBJ_TAG_GENERIC);

      // add the additional proposals
      int additionalLength = newfileproposals.length + anyproposals.length;
      additionalLength = (endTag != null) ? ++additionalLength : additionalLength;
      if (additionalLength > 0) {
        ICompletionProposal[] plusOnes = new ICompletionProposal[proposals.length
            + additionalLength];
        int appendPos = proposals.length;
        // add end tag proposal
        if (endTag != null) {
          System.arraycopy(proposals, 0, plusOnes, 1, proposals.length);
          plusOnes[0] = endTag;
          ++appendPos;
        } else {
          System.arraycopy(proposals, 0, plusOnes, 0, proposals.length);
        }
        // add items in newfileproposals
        for (int i = 0; i < newfileproposals.length; ++i) {
          plusOnes[appendPos + i] = newfileproposals[i];
        }
        // add items in anyproposals
        appendPos = appendPos + newfileproposals.length;
        for (int i = 0; i < anyproposals.length; ++i) {
          plusOnes[appendPos + i] = anyproposals[i];
        }
        proposals = plusOnes;
      }
    }
    return Arrays.asList(proposals);
  }

  /**
   * @see org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer#computeContextInformation(org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext,
   *      org.eclipse.core.runtime.IProgressMonitor)
   */
  public List computeContextInformation(CompletionProposalInvocationContext context,
      IProgressMonitor monitor) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * @see org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer#getErrorMessage()
   */
  public String getErrorMessage() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * @see org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer#sessionStarted()
   */
  public void sessionStarted() {
    //default is to do nothing
  }

  /**
   * @see org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer#sessionEnded()
   */
  public void sessionEnded() {
    //default is to do nothing
  }

  /**
   * Returns the CSSmodel for a given XML node.
   * 
   * @param element
   * @return IStructuredModel
   */
  private static IStructuredModel getCSSModel(IDOMNode element) {
    if (element == null) {
      return null;
    }
    INodeAdapter adapter = StyleAdapterFactory.getInstance().adapt(element);
    if ((adapter == null) || !(adapter instanceof ICSSModelAdapter)) {
      return null;
    }
    ICSSModelAdapter modelAdapter = (ICSSModelAdapter) adapter;
    return modelAdapter.getModel();
  }

  /**
   * <p>
   * Get the start offset of the text region containing the given document position
   * </p>
   * 
   * @param domNode {@link IDOMNode} containing the document position
   * @param documentPosition the document relative position to get the start offset of the
   *          containing {@link ITextRegion} for
   * @return start offset of the {@link ITextRegion} containing the given document position
   */
  private static int getTextRegionStartOffset(IDOMNode domNode, int documentPosition) {
    IStructuredDocumentRegion structRegion = domNode.getFirstStructuredDocumentRegion();
    return structRegion.getStartOffset(structRegion.getRegionAtCharacterOffset(documentPosition));
  }
}
