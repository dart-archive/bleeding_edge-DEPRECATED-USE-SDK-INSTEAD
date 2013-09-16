/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.correction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
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

public class XMLQuickAssistProcessor implements IQuickAssistProcessor {

  public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
    return true;
  }

  public boolean canFix(Annotation annotation) {
    return false;
  }

  public ICompletionProposal[] computeQuickAssistProposals(
      IQuickAssistInvocationContext invocationContext) {
    List proposals = new ArrayList();

    getLocalRenameQuickAssistProposal(proposals, invocationContext.getSourceViewer(),
        invocationContext.getOffset());
    getSurroundWithNewElementQuickAssistProposal(proposals, invocationContext.getSourceViewer(),
        invocationContext.getOffset());
    getInsertRequiredAttrs(proposals, invocationContext.getSourceViewer(),
        invocationContext.getOffset());

    return (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);
  }

  public String getErrorMessage() {
    return null;
  }

  private void getInsertRequiredAttrs(List proposals, ISourceViewer viewer, int offset) {
    IDOMNode node = (IDOMNode) getNodeAt(viewer, offset);
    if ((node != null) && (node.getNodeType() == Node.ELEMENT_NODE)) {
      IStructuredDocumentRegion startStructuredDocumentRegion = node.getStartStructuredDocumentRegion();
      if ((startStructuredDocumentRegion != null)
          && startStructuredDocumentRegion.containsOffset(offset)) {
        IDOMNode cursorNode = (IDOMNode) getNodeAt(viewer, offset);
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

  private void getLocalRenameQuickAssistProposal(List proposals, ISourceViewer viewer, int offset) {
    IDOMNode node = (IDOMNode) getNodeAt(viewer, offset);
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

  private ModelQuery getModelQuery(Node node) {
    if (node.getNodeType() == Node.DOCUMENT_NODE) {
      return ModelQueryUtil.getModelQuery((Document) node);
    } else {
      return ModelQueryUtil.getModelQuery(node.getOwnerDocument());
    }
  }

  private List getRequiredAttrs(Node node) {
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

  private void getSurroundWithNewElementQuickAssistProposal(List proposals, ISourceViewer viewer,
      int offset) {
    IDOMNode node = (IDOMNode) getNodeAt(viewer, offset);
    if (node != null) {
      proposals.add(new SurroundWithNewElementQuickAssistProposal());
    }
  }

  /**
   * Returns the closest IndexedRegion for the offset and viewer allowing for differences between
   * viewer offsets and model positions. note: this method returns an IndexedRegion for read only
   * 
   * @param viewer the viewer whose document is used to compute the proposals
   * @param documentOffset an offset within the document for which completions should be computed
   * @return an IndexedRegion
   */
  private IndexedRegion getNodeAt(ITextViewer viewer, int documentOffset) {
    // copied from ContentAssistUtils.getNodeAt()
    if (viewer == null)
      return null;

    IndexedRegion node = null;
    IModelManager mm = StructuredModelManager.getModelManager();
    IStructuredModel model = null;
    if (mm != null)
      model = mm.getExistingModelForRead(viewer.getDocument());
    try {
      if (model != null) {
        int lastOffset = documentOffset;
        node = model.getIndexedRegion(documentOffset);
        while (node == null && lastOffset >= 0) {
          lastOffset--;
          node = model.getIndexedRegion(lastOffset);
        }
      }
    } finally {
      if (model != null)
        model.releaseFromRead();
    }
    return node;
  }
}
