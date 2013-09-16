/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.contentassist;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.wst.html.core.internal.contentmodel.HTMLAttributeDeclaration;
import org.eclipse.wst.html.core.internal.contentmodel.HTMLCMDocument;
import org.eclipse.wst.html.core.internal.contentmodel.HTMLPropertyDeclaration;
import org.eclipse.wst.html.core.internal.document.HTMLDocumentTypeEntry;
import org.eclipse.wst.html.core.internal.document.HTMLDocumentTypeRegistry;
import org.eclipse.wst.html.core.internal.provisional.HTML40Namespace;
import org.eclipse.wst.html.core.internal.provisional.HTMLCMProperties;
import org.eclipse.wst.html.ui.internal.HTMLUIMessages;
import org.eclipse.wst.html.ui.internal.editor.HTMLEditorPluginImageHelper;
import org.eclipse.wst.html.ui.internal.editor.HTMLEditorPluginImages;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.sse.ui.internal.contentassist.ContentAssistUtils;
import org.eclipse.wst.sse.ui.internal.contentassist.CustomCompletionProposal;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDocument;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.contentmodel.basic.CMElementDeclarationImpl;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.eclipse.wst.xml.core.internal.ssemodelquery.ModelQueryAdapter;
import org.eclipse.wst.xml.ui.internal.contentassist.AbstractXMLModelQueryCompletionProposalComputer;
import org.eclipse.wst.xml.ui.internal.contentassist.AttributeContextInformationPresenter;
import org.eclipse.wst.xml.ui.internal.contentassist.AttributeContextInformationProvider;
import org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest;
import org.eclipse.wst.xml.ui.internal.contentassist.XMLContentModelGenerator;
import org.eclipse.wst.xml.ui.internal.contentassist.XMLRelevanceConstants;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImageHelper;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImages;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Node;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * {@link AbstractXMLModelQueryCompletionProposalComputer} for HTML tag proposals
 * </p>
 */
public class HTMLTagsCompletionProposalComputer extends
    AbstractXMLModelQueryCompletionProposalComputer {

  /** <code>true</code> if the document the proposal request is on is XHTML */
  protected boolean isXHTML = false;

  /** the context information validator for this computer */
  private IContextInformationValidator fContextInformationValidator;

  /**
   * <p>
   * Default constructor
   * </p>
   */
  public HTMLTagsCompletionProposalComputer() {
    this.fContextInformationValidator = null;
  }

  /**
   * <p>
   * Determine if the document is XHTML or not, then compute the proposals
   * </p>
   * 
   * @see org.eclipse.wst.xml.ui.internal.contentassist.AbstractXMLCompletionProposalComputer#computeCompletionProposals(org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext,
   *      org.eclipse.core.runtime.IProgressMonitor)
   */
  public List computeCompletionProposals(CompletionProposalInvocationContext context,
      IProgressMonitor monitor) {

    //determine if the content is XHTML or not
    IndexedRegion treeNode = ContentAssistUtils.getNodeAt(context.getViewer(),
        context.getInvocationOffset());
    IDOMNode node = (IDOMNode) treeNode;
    boolean isXHTMLNode = isXHTMLNode(node);
    if (this.isXHTML != isXHTMLNode) {
      this.isXHTML = isXHTMLNode;
    }

    //compute the completion proposals
    return super.computeCompletionProposals(context, monitor);
  }

  /**
   * @see org.eclipse.wst.xml.ui.internal.contentassist.AbstractXMLCompletionProposalComputer#computeContextInformation(org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext,
   *      org.eclipse.core.runtime.IProgressMonitor)
   */
  public List computeContextInformation(CompletionProposalInvocationContext context,
      IProgressMonitor monitor) {

    AttributeContextInformationProvider attributeInfoProvider = new AttributeContextInformationProvider(
        (IStructuredDocument) context.getDocument(),
        (AttributeContextInformationPresenter) getContextInformationValidator());
    return Arrays.asList(attributeInfoProvider.getAttributeInformation(context.getInvocationOffset()));
  }

  /**
   * <p>
   * Dependent on if the document is XHTML or not
   * </p>
   * 
   * @see org.eclipse.wst.xml.ui.internal.contentassist.AbstractXMLModelQueryCompletionProposalComputer#getContentGenerator()
   */
  protected XMLContentModelGenerator getContentGenerator() {
    if (isXHTML) {
      return XHTMLMinimalContentModelGenerator.getInstance();
    } else {
      return HTMLMinimalContentModelGenerator.getInstance();
    }
  }

  /**
   * <p>
   * Filter out all {@link CMNode}s except those specific to HTML documents
   * </p>
   * 
   * @see org.eclipse.wst.xml.ui.internal.contentassist.AbstractXMLModelQueryCompletionProposalComputer#validModelQueryNode(org.eclipse.wst.xml.core.internal.contentmodel.CMNode)
   */
  protected boolean validModelQueryNode(CMNode node) {
    boolean isValid = false;
    Object cmdoc = node.getProperty("CMDocument"); //$NON-NLS-1$
    if (cmdoc instanceof CMNode) {
      String name = ((CMNode) cmdoc).getNodeName();
      isValid = name != null && name.endsWith(".dtd") && name.indexOf("html") != -1; //$NON-NLS-1$ //$NON-NLS-2$
    } else if (node.supports(HTMLAttributeDeclaration.IS_HTML)) {
      Boolean isHTML = (Boolean) node.getProperty(HTMLAttributeDeclaration.IS_HTML);
      isValid = isHTML == null || isHTML.booleanValue();
    } else if (node instanceof HTMLPropertyDeclaration) {
      HTMLPropertyDeclaration propDec = (HTMLPropertyDeclaration) node;
      isValid = !propDec.isJSP();
    } else if (node instanceof CMAttributeDeclaration || node instanceof CMElementDeclarationImpl) {
      isValid = true;
    } else if (node instanceof CMElementDeclaration) {
      Boolean isXHTML = ((Boolean) node.getProperty(HTMLCMProperties.IS_XHTML));
      isValid = isXHTML != null && isXHTML.booleanValue();
    }

    // Do not propose obsolete tags, regardless
    if (isValid && node.supports(HTMLCMProperties.IS_OBSOLETE)) {
      Boolean isObsolete = ((Boolean) node.getProperty(HTMLCMProperties.IS_OBSOLETE));
      isValid = !(isObsolete != null && isObsolete.booleanValue());
    }

    return isValid;
  }

  /**
   * @see org.eclipse.wst.xml.ui.internal.contentassist.AbstractXMLModelQueryCompletionProposalComputer#addEmptyDocumentProposals(org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest,
   *      org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext)
   */
  protected void addEmptyDocumentProposals(ContentAssistRequest contentAssistRequest,
      CompletionProposalInvocationContext context) {

    addHTMLTagProposal(contentAssistRequest, context);
  }

  /**
   * @see org.eclipse.wst.xml.ui.internal.contentassist.AbstractXMLModelQueryCompletionProposalComputer#addStartDocumentProposals(org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest,
   *      org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext)
   */
  protected void addStartDocumentProposals(ContentAssistRequest contentAssistRequest,
      CompletionProposalInvocationContext context) {

    //determine if XMLPI is first element
    Node aNode = contentAssistRequest.getNode();
    Document owningDocument = aNode.getOwnerDocument();
    Node first = owningDocument.getFirstChild();
    boolean xmlpiIsFirstElement = ((first != null) && (first.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE));

    //if there is an XMLPI then XHTML doctype, else HTML doctype
    if (xmlpiIsFirstElement && (owningDocument.getDoctype() == null)
        && isCursorAfterXMLPI(contentAssistRequest)) {

      addDocTypeProposal(contentAssistRequest, true);
    } else {
      addDocTypeProposal(contentAssistRequest, false);
    }
  }

  /**
   * @param contentAssistRequest
   * @param isXHTML
   */
  private void addDocTypeProposal(ContentAssistRequest contentAssistRequest, boolean isXHTML) {
    // if a DocumentElement exists, use that for the root Element name
    String rootname = "unspecified"; //$NON-NLS-1$
    if (contentAssistRequest.getNode().getOwnerDocument().getDocumentElement() != null) {
      rootname = contentAssistRequest.getNode().getOwnerDocument().getDocumentElement().getNodeName();
    }

    //decide which entry to use
    HTMLDocumentTypeEntry entry;
    if (isXHTML) {
      entry = HTMLDocumentTypeRegistry.getInstance().getXHTMLDefaultEntry();
    } else {
      entry = HTMLDocumentTypeRegistry.getInstance().getDefaultEntry();
    }

    //create the content assist string and proposal
    String proposedText = "<!DOCTYPE " + rootname + " PUBLIC \"" + //$NON-NLS-1$ //$NON-NLS-2$
        entry.getPublicId() + "\" \"" + entry.getSystemId() + "\">"; //$NON-NLS-1$ //$NON-NLS-2$
    ICompletionProposal proposal = new CustomCompletionProposal(proposedText,
        contentAssistRequest.getReplacementBeginPosition(),
        contentAssistRequest.getReplacementLength(), 10,
        XMLEditorPluginImageHelper.getInstance().getImage(XMLEditorPluginImages.IMG_OBJ_DOCTYPE),
        entry.getDisplayName() + " " + HTMLUIMessages.Expandable_label_document_type, //$NON-NLS-1$ 
        null, null, XMLRelevanceConstants.R_DOCTYPE);
    contentAssistRequest.addProposal(proposal);
  }

  /**
   * <p>
   * adds HTML tag proposal for empty document
   * </p>
   * 
   * @param contentAssistRequest request to add proposal too
   * @param context context of the completion request
   */
  private void addHTMLTagProposal(ContentAssistRequest contentAssistRequest,
      CompletionProposalInvocationContext context) {
    IStructuredModel model = null;
    try {
      if (context.getDocument() instanceof IStructuredDocument) {
        model = StructuredModelManager.getModelManager().getModelForRead(
            (IStructuredDocument) context.getDocument());
      }
      if (model != null) {
        IDOMDocument doc = ((IDOMModel) model).getDocument();

        ModelQuery mq = ModelQueryUtil.getModelQuery(doc);
        if (mq != null) {

          // XHTML requires lowercase tagname for lookup
          CMDocument correspondingCMDocument = mq.getCorrespondingCMDocument(doc);
          if (correspondingCMDocument != null) {
            CMElementDeclaration htmlDecl = (CMElementDeclaration) correspondingCMDocument.getElements().getNamedItem(
                HTML40Namespace.ElementName.HTML.toLowerCase());
            if (htmlDecl != null) {
              StringBuffer proposedTextBuffer = new StringBuffer();
              getContentGenerator().generateTag(doc, htmlDecl, proposedTextBuffer);

              String proposedText = proposedTextBuffer.toString();
              String requiredName = getContentGenerator().getRequiredName(doc, htmlDecl);

              IStructuredDocumentRegion region = contentAssistRequest.getDocumentRegion();
              if (region != null) {
                if (region.getFirstRegion() != null
                    && region.getFirstRegion().getType().equals(DOMRegionContext.XML_TAG_OPEN)) {
                  //in order to differentiate between content assist on 
                  //completely empty document and the one with xml open tag
                  proposedText = proposedText.substring(1);
                }
              }
              if (!beginsWith(proposedText, contentAssistRequest.getMatchString())) {
                return;
              }
              int cursorAdjustment = getCursorPositionForProposedText(proposedText);
              CustomCompletionProposal proposal = new CustomCompletionProposal(proposedText,
                  contentAssistRequest.getReplacementBeginPosition(),
                  contentAssistRequest.getReplacementLength(), cursorAdjustment,
                  HTMLEditorPluginImageHelper.getInstance().getImage(
                      HTMLEditorPluginImages.IMG_OBJ_TAG_GENERIC), requiredName, null, null,
                  XMLRelevanceConstants.R_TAG_NAME);
              contentAssistRequest.addProposal(proposal);
            }
          }
        }
      }
    } finally {
      if (model != null)
        model.releaseFromRead();
    }
  }

  /**
   * Determine if this Document is an XHTML Document. Operates solely off of the Document Type
   * declaration
   */
  private static boolean isXHTMLNode(Node node) {
    if (node == null) {
      return false;
    }

    Document doc = null;
    if (node.getNodeType() != Node.DOCUMENT_NODE)
      doc = node.getOwnerDocument();
    else
      doc = ((Document) node);

    if (doc instanceof IDOMDocument) {
      return ((IDOMDocument) doc).isXMLType();
    }

    if (doc instanceof INodeNotifier) {
      ModelQueryAdapter adapter = (ModelQueryAdapter) ((INodeNotifier) doc).getAdapterFor(ModelQueryAdapter.class);
      CMDocument cmdoc = null;
      if (adapter != null && adapter.getModelQuery() != null)
        cmdoc = adapter.getModelQuery().getCorrespondingCMDocument(doc);
      if (cmdoc != null) {
        // treat as XHTML unless we've got the in-code HTML content
        // model
        if (cmdoc instanceof HTMLCMDocument)
          return false;
        if (cmdoc.supports(HTMLCMProperties.IS_XHTML))
          return Boolean.TRUE.equals(cmdoc.getProperty(HTMLCMProperties.IS_XHTML));
      }
    }
    // this should never be reached
    DocumentType docType = doc.getDoctype();
    return docType != null && docType.getPublicId() != null
        && docType.getPublicId().indexOf("-//W3C//DTD XHTML ") == 0; //$NON-NLS-1$
  }

  /**
   * Returns a validator used to determine when displayed context information should be dismissed.
   * May only return <code>null</code> if the processor is incapable of computing context
   * information. a context information validator, or <code>null</code> if the processor is
   * incapable of computing context information
   */
  private IContextInformationValidator getContextInformationValidator() {
    if (fContextInformationValidator == null) {
      fContextInformationValidator = new AttributeContextInformationPresenter();
    }
    return fContextInformationValidator;
  }
}
