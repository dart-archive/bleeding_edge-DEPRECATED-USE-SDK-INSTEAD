/*******************************************************************************
 * Copyright (c) 2001, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.correction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.ui.internal.StructuredTextViewer;
import org.eclipse.wst.sse.ui.internal.contentassist.ContentAssistUtils;
import org.eclipse.wst.sse.ui.internal.correction.IQuickAssistProcessor;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNamedNodeMap;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.contentmodel.basic.CMNamedNodeMapImpl;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * @deprecated since 2.0 RC0 Use org.eclipse.jface.text.quickassist.IQuickAssistProcessor and
 *             XMLQuickAssistProcessor
 */
public class QuickAssistProcessorXML implements IQuickAssistProcessor {
  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.wst.sse.ui.correction.IQuickAssistProcessor#canAssist(org.eclipse.wst.sse.core.
   * text.IStructuredDocument, int)
   */
  public boolean canAssist(StructuredTextViewer viewer, int offset) {
    return true;
  }

  /**
   * @param proposals
   * @param viewer
   * @param offset
   */
  protected void getInsertRequiredAttrs(ArrayList proposals, StructuredTextViewer viewer, int offset) {
    IDOMNode node = (IDOMNode) ContentAssistUtils.getNodeAt(viewer, offset);
    if ((node != null) && (node.getNodeType() == Node.ELEMENT_NODE)) {
      IStructuredDocumentRegion startStructuredDocumentRegion = node.getStartStructuredDocumentRegion();
      if ((startStructuredDocumentRegion != null)
          && startStructuredDocumentRegion.containsOffset(offset)) {
        IDOMNode cursorNode = (IDOMNode) ContentAssistUtils.getNodeAt(viewer, offset);
        List requiredAttrs = getRequiredAttrs(cursorNode);
        if (requiredAttrs.size() > 0) {
          NamedNodeMap currentAttrs = node.getAttributes();
          List insertAttrs = new ArrayList();
          if (currentAttrs.getLength() == 0) {
            insertAttrs.addAll(requiredAttrs);
          } else {
            for (int i = 0; i < requiredAttrs.size(); i++) {
              String requiredAttrName = ((CMAttributeDeclaration) requiredAttrs.get(i)).getAttrName();
              boolean found = false;
              for (int j = 0; j < currentAttrs.getLength(); j++) {
                String currentAttrName = currentAttrs.item(j).getNodeName();
                if (requiredAttrName.compareToIgnoreCase(currentAttrName) == 0) {
                  found = true;
                  break;
                }
              }
              if (!found) {
                insertAttrs.add(requiredAttrs.get(i));
              }
            }
          }
          if (insertAttrs.size() > 0) {
            proposals.add(new InsertRequiredAttrsQuickAssistProposal(insertAttrs));
          }
        }
      }
    }
  }

  /**
   * @param proposals
   * @param viewer
   * @param offset
   */
  protected void getLocalRenameQuickAssistProposal(ArrayList proposals,
      StructuredTextViewer viewer, int offset) {
    IDOMNode node = (IDOMNode) ContentAssistUtils.getNodeAt(viewer, offset);
    IStructuredDocumentRegion startStructuredDocumentRegion = node == null ? null
        : node.getStartStructuredDocumentRegion();
    IStructuredDocumentRegion endStructuredDocumentRegion = node == null ? null
        : node.getEndStructuredDocumentRegion();

    ITextRegion region = null;
    int regionTextEndOffset = 0;
    if ((startStructuredDocumentRegion != null)
        && startStructuredDocumentRegion.containsOffset(offset)) {
      region = startStructuredDocumentRegion.getRegionAtCharacterOffset(offset);
      regionTextEndOffset = startStructuredDocumentRegion.getTextEndOffset(region);
    } else if ((endStructuredDocumentRegion != null)
        && endStructuredDocumentRegion.containsOffset(offset)) {
      region = endStructuredDocumentRegion.getRegionAtCharacterOffset(offset);
      regionTextEndOffset = endStructuredDocumentRegion.getTextEndOffset(region);
    }

    if ((region != null)
        && ((region.getType() == DOMRegionContext.XML_TAG_NAME) || (region.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_NAME))
        && (offset <= regionTextEndOffset)) {
      proposals.add(new RenameInFileQuickAssistProposal());
    }
  }

  protected ModelQuery getModelQuery(Node node) {
    if (node.getNodeType() == Node.DOCUMENT_NODE) {
      return ModelQueryUtil.getModelQuery((Document) node);
    } else {
      return ModelQueryUtil.getModelQuery(node.getOwnerDocument());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.wst.sse.ui.correction.IQuickAssistProcessor#getProposals(org.eclipse.wst.sse.core
   * .text.IStructuredDocument, int)
   */
  public ICompletionProposal[] getProposals(StructuredTextViewer viewer, int offset)
      throws CoreException {
    ArrayList proposals = new ArrayList();

    getLocalRenameQuickAssistProposal(proposals, viewer, offset);
    getSurroundWithNewElementQuickAssistProposal(proposals, viewer, offset);
    getInsertRequiredAttrs(proposals, viewer, offset);

    return (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);
  }

  protected List getRequiredAttrs(Node node) {
    List result = new ArrayList();

    ModelQuery modelQuery = getModelQuery(node);
    if (modelQuery != null) {
      CMElementDeclaration elementDecl = modelQuery.getCMElementDeclaration((Element) node);
      if (elementDecl != null) {
        CMNamedNodeMap attrMap = elementDecl.getAttributes();

        CMNamedNodeMapImpl allAttributes = new CMNamedNodeMapImpl(attrMap);
        List nodes = ModelQueryUtil.getModelQuery(node.getOwnerDocument()).getAvailableContent(
            (Element) node, elementDecl, ModelQuery.INCLUDE_ATTRIBUTES);
        for (int k = 0; k < nodes.size(); k++) {
          CMNode cmnode = (CMNode) nodes.get(k);
          if (cmnode.getNodeType() == CMNode.ATTRIBUTE_DECLARATION) {
            allAttributes.put(cmnode);
          }
        }
        attrMap = allAttributes;

        Iterator it = attrMap.iterator();
        CMAttributeDeclaration attr = null;
        while (it.hasNext()) {
          attr = (CMAttributeDeclaration) it.next();
          if (attr.getUsage() == CMAttributeDeclaration.REQUIRED) {
            result.add(attr);
          }
        }
      }
    }

    return result;
  }

  /**
   * @param proposals
   * @param viewer
   * @param offset
   */
  protected void getSurroundWithNewElementQuickAssistProposal(ArrayList proposals,
      StructuredTextViewer viewer, int offset) {
    IDOMNode node = (IDOMNode) ContentAssistUtils.getNodeAt(viewer, offset);
    if (node != null) {
      proposals.add(new SurroundWithNewElementQuickAssistProposal());
    }
  }
}
