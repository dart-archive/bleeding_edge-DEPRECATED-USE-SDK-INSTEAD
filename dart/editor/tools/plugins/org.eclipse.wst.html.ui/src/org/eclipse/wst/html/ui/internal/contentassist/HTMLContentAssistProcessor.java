/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.contentassist;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.wst.css.ui.internal.contentassist.CSSContentAssistProcessor;
import org.eclipse.wst.html.core.internal.contentmodel.HTMLCMDocument;
import org.eclipse.wst.html.core.internal.provisional.HTML40Namespace;
import org.eclipse.wst.html.core.internal.provisional.HTMLCMProperties;
import org.eclipse.wst.html.core.text.IHTMLPartitions;
import org.eclipse.wst.html.ui.StructuredTextViewerConfigurationHTML;
import org.eclipse.wst.html.ui.internal.HTMLUIPlugin;
import org.eclipse.wst.html.ui.internal.editor.HTMLEditorPluginImageHelper;
import org.eclipse.wst.html.ui.internal.editor.HTMLEditorPluginImages;
import org.eclipse.wst.html.ui.internal.preferences.HTMLUIPreferenceNames;
import org.eclipse.wst.html.ui.internal.templates.TemplateContextTypeIdsHTML;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapterFactory;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.sse.ui.internal.IReleasable;
import org.eclipse.wst.sse.ui.internal.contentassist.ContentAssistUtils;
import org.eclipse.wst.sse.ui.internal.contentassist.CustomCompletionProposal;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDocument;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.eclipse.wst.xml.core.internal.ssemodelquery.ModelQueryAdapter;
import org.eclipse.wst.xml.ui.internal.contentassist.AbstractContentAssistProcessor;
import org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest;
import org.eclipse.wst.xml.ui.internal.contentassist.XMLContentModelGenerator;
import org.eclipse.wst.xml.ui.internal.contentassist.XMLRelevanceConstants;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @deprecated This class is no longer used locally and will be removed in the future
 * @see HTMLStructuredContentAssistProcessor
 */
public class HTMLContentAssistProcessor extends AbstractContentAssistProcessor implements
    IPropertyChangeListener {
  private INodeAdapterFactory factoryForCSS = null;
  protected IPreferenceStore fPreferenceStore = null;
  protected boolean isXHTML = false;
  private HTMLTemplateCompletionProcessor fTemplateProcessor = null;
  private IContentAssistProcessor fJSContentAssistProcessor = null;
  private List fTemplateContexts = new ArrayList();

  public HTMLContentAssistProcessor() {

    super();
  }

  protected void addAttributeNameProposals(ContentAssistRequest contentAssistRequest) {
    addTemplates(contentAssistRequest, TemplateContextTypeIdsHTML.ATTRIBUTE);
    super.addAttributeNameProposals(contentAssistRequest);
  }

  protected void addAttributeValueProposals(ContentAssistRequest contentAssistRequest) {
    addTemplates(contentAssistRequest, TemplateContextTypeIdsHTML.ATTRIBUTE_VALUE);
    super.addAttributeValueProposals(contentAssistRequest);
  }

  /**
   * Add the proposals for a completely empty document
   */
  protected void addEmptyDocumentProposals(ContentAssistRequest contentAssistRequest) {
    addTemplates(contentAssistRequest, TemplateContextTypeIdsHTML.NEW);
  }

  protected void addPCDATAProposal(String nodeName, ContentAssistRequest contentAssistRequest) {
    if (isXHTML)
      super.addPCDATAProposal(nodeName, contentAssistRequest);
  }

  protected void addStartDocumentProposals(ContentAssistRequest contentAssistRequest) {
    if (isXHTML)
      addEmptyDocumentProposals(contentAssistRequest);
  }

  protected void addTagInsertionProposals(ContentAssistRequest contentAssistRequest,
      int childPosition) {
    addTemplates(contentAssistRequest, TemplateContextTypeIdsHTML.TAG);
    super.addTagInsertionProposals(contentAssistRequest, childPosition);
  }

  /**
   * Adds templates to the list of proposals
   * 
   * @param contentAssistRequest
   * @param context
   */
  private void addTemplates(ContentAssistRequest contentAssistRequest, String context) {
    addTemplates(contentAssistRequest, context, contentAssistRequest.getReplacementBeginPosition());
  }

  /**
   * Adds templates to the list of proposals
   * 
   * @param contentAssistRequest
   * @param context
   * @param startOffset
   */
  private void addTemplates(ContentAssistRequest contentAssistRequest, String context,
      int startOffset) {
    if (contentAssistRequest == null)
      return;

    // if already adding template proposals for a certain context type, do
    // not add again
    if (!fTemplateContexts.contains(context)) {
      fTemplateContexts.add(context);
      boolean useProposalList = !contentAssistRequest.shouldSeparate();

      if (getTemplateCompletionProcessor() != null) {
        getTemplateCompletionProcessor().setContextType(context);
        ICompletionProposal[] proposals = getTemplateCompletionProcessor().computeCompletionProposals(
            fTextViewer, startOffset);
        for (int i = 0; i < proposals.length; ++i) {
          if (useProposalList)
            contentAssistRequest.addProposal(proposals[i]);
          else
            contentAssistRequest.addMacro(proposals[i]);
        }
      }
    }
  }

  protected boolean beginsWith(String aString, String prefix) {
    if (aString == null || prefix == null || prefix.length() == 0)
      return true;
    int minimumLength = Math.min(prefix.length(), aString.length());
    String beginning = aString.substring(0, minimumLength);
    return beginning.equalsIgnoreCase(prefix);
  }

  protected ContentAssistRequest computeCompletionProposals(int documentPosition,
      String matchString, ITextRegion completionRegion, IDOMNode treeNode, IDOMNode xmlnode) {
    ContentAssistRequest request = super.computeCompletionProposals(documentPosition, matchString,
        completionRegion, treeNode, xmlnode);
    // bug115927 use original document position for all/any region templates
    addTemplates(request, TemplateContextTypeIdsHTML.ALL, documentPosition);
    return request;
  }

  /**
   * Return a list of proposed code completions based on the specified location within the document
   * that corresponds to the current cursor position within the text-editor control.
   * 
   * @param documentPosition a location within the document
   * @return an array of code-assist items
   */
  public ICompletionProposal[] computeCompletionProposals(ITextViewer textViewer,
      int documentPosition) {
    fTemplateContexts.clear();

    IndexedRegion treeNode = ContentAssistUtils.getNodeAt(textViewer, documentPosition);
    IDOMNode node = (IDOMNode) treeNode;
    setErrorMessage(null);

    // check if it's in a comment node
    IStructuredDocument structuredDocument = (IStructuredDocument) textViewer.getDocument();
    IStructuredDocumentRegion fn = structuredDocument.getRegionAtCharacterOffset(documentPosition);
    if (fn != null && fn.getType() == DOMRegionContext.XML_COMMENT_TEXT
        && documentPosition != fn.getStartOffset()) {
      return new ICompletionProposal[0];
    }

    // CMVC 242695
    // if it's a </script> tag, bounce back to JS ca processor...
    if (fn != null && fn.getType() == DOMRegionContext.XML_TAG_NAME
        && documentPosition == fn.getStartOffset()) {
      ITextRegionList v = fn.getRegions();
      if (v.size() > 1) {
        // determine that it's a close tag
        if ((v.get(0)).getType() == DOMRegionContext.XML_END_TAG_OPEN) {
          Iterator it = v.iterator();
          ITextRegion region = null;
          // search for script tag name
          while (it.hasNext()) {
            region = (ITextRegion) it.next();
            if (fn.getText(region).equalsIgnoreCase("script")) { //$NON-NLS-1$
              IContentAssistProcessor jsProcessor = getJSContentAssistProcessor();
              if (jsProcessor != null) {
                return jsProcessor.computeCompletionProposals(textViewer, documentPosition);
              }
              return new ICompletionProposal[0];
            }
          }
        }
      }
    }

    isXHTML = getXHTML(node);

    fGenerator = null; // force reload of content generator

    // handle blank HTML document case
    if (treeNode == null || isViewerEmpty(textViewer)) {
      // cursor is at the EOF
      ICompletionProposal htmlTagProposal = getHTMLTagProposal(textViewer, documentPosition);
      ICompletionProposal[] superResults = super.computeCompletionProposals(textViewer,
          documentPosition);
      if (superResults != null && superResults.length > 0 && htmlTagProposal != null) {
        ICompletionProposal[] blankHTMLDocResults = new ICompletionProposal[superResults.length + 1];
        blankHTMLDocResults[0] = htmlTagProposal;
        System.arraycopy(superResults, 0, blankHTMLDocResults, 1, superResults.length);
        return blankHTMLDocResults;
      }
    }

    if (node != null && node.getNodeType() == Node.ELEMENT_NODE) {

      // check embedded CSS proposals at the beginning of the STYLE end
      // tag
      Element element = (Element) node;
      String tagName = element.getTagName();
      if (tagName != null && tagName.equalsIgnoreCase(HTML40Namespace.ATTR_NAME_STYLE)) {//$NON-NLS-1$
        IStructuredDocumentRegion endStructuredDocumentRegion = node.getEndStructuredDocumentRegion();
        if (endStructuredDocumentRegion != null
            && endStructuredDocumentRegion.getStartOffset() == documentPosition) {
          IStructuredDocumentRegion startStructuredDocumentRegion = node.getStartStructuredDocumentRegion();
          if (startStructuredDocumentRegion != null) {
            int offset = startStructuredDocumentRegion.getEndOffset();
            int pos = documentPosition - offset;
            ICompletionProposal[] proposals = getCSSProposals(textViewer, pos, node, offset,
                (char) 0);
            if (proposals != null)
              return proposals;
          }
        }
      }

      // check inline CSS proposals
      // need to find attr region from sd region
      IStructuredDocumentRegion sdRegion = ContentAssistUtils.getStructuredDocumentRegion(
          textViewer, documentPosition);
      Iterator regions = sdRegion.getRegions().iterator();
      ITextRegion styleNameRegion = null;
      ITextRegion styleValueRegion = null;
      while (regions.hasNext()) {
        styleNameRegion = (ITextRegion) regions.next();
        if (styleNameRegion.getType().equals(DOMRegionContext.XML_TAG_ATTRIBUTE_NAME)
            && sdRegion.getText(styleNameRegion).equalsIgnoreCase(HTML40Namespace.ATTR_NAME_STYLE)) { //$NON-NLS-1$
          // the next region should be "="
          if (regions.hasNext()) {
            regions.next(); // skip the "="
            // next region should be attr value region
            if (regions.hasNext()) {
              styleValueRegion = (ITextRegion) regions.next();
              break;
            }
          }
        }
      }

      if (styleValueRegion != null) {
        int offset = sdRegion.getStartOffset(styleValueRegion);
        int end = sdRegion.getTextEndOffset(styleValueRegion);
        if (documentPosition >= offset && documentPosition <= end) {
          boolean askCSS = true;
          char quote = (char) 0;
          String text = sdRegion.getText(styleValueRegion);
          int length = (text != null ? text.length() : 0);
          if (length > 0) {
            char firstChar = text.charAt(0);
            if (firstChar == '"' || firstChar == '\'') {
              if (documentPosition == offset) {
                // before quote
                askCSS = false;
              } else {
                offset++;
                quote = firstChar;
              }
            }
            if (documentPosition == end) {
              if (length > 1 && text.charAt(length - 1) == quote) {
                // after quote
                askCSS = false;
              }
            }
          }
          if (askCSS) {
            int pos = documentPosition - offset;
            ICompletionProposal[] proposals = getCSSProposals(textViewer, pos, node, offset, quote);
            if (proposals != null)
              return proposals;
          }
        }
      }
    }

    return super.computeCompletionProposals(textViewer, documentPosition);
  }

  /**
   * Returns true if there is no text or it's all white space, otherwise returns false
   * 
   * @param treeNode
   * @param textViewer
   * @return boolean
   */
  private boolean isViewerEmpty(ITextViewer textViewer) {
    boolean isEmpty = false;
    String text = textViewer.getTextWidget().getText();
    if (text == null || (text != null && text.trim().equals(""))) //$NON-NLS-1$
      isEmpty = true;
    return isEmpty;
  }

  /**
   * @return ICompletionProposal
   */
  private ICompletionProposal getHTMLTagProposal(ITextViewer viewer, int documentPosition) {
    IModelManager mm = StructuredModelManager.getModelManager();
    IStructuredModel model = null;
    ICompletionProposal result = null;
    try {
      if (mm != null) {
        model = mm.getExistingModelForRead(viewer.getDocument());

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

                CustomCompletionProposal proposal = new CustomCompletionProposal(proposedText,
                    documentPosition,
                    /* start pos */
                    0, /* replace length */
                    requiredName.length() + 2, /*
                                                * cursor position after (relavtive to start)
                                                */
                    HTMLEditorPluginImageHelper.getInstance().getImage(
                        HTMLEditorPluginImages.IMG_OBJ_TAG_GENERIC), requiredName, null, null,
                    XMLRelevanceConstants.R_TAG_NAME);
                result = proposal;
              }
            }
          }
        }
      }
    } finally {
      if (model != null)
        model.releaseFromRead();
    }
    return result;
  }

  /**
   * @see AbstractContentAssistProcessor#getContentGenerator()
   */
  public XMLContentModelGenerator getContentGenerator() {
    if (fGenerator == null) {
      if (isXHTML)
        fGenerator = XHTMLMinimalContentModelGenerator.getInstance();
      else
        fGenerator = HTMLMinimalContentModelGenerator.getInstance();
    }
    return fGenerator;
  }

  protected ICompletionProposal[] getCSSProposals(ITextViewer viewer, int pos, IDOMNode element,
      int offset, char quote) {

    CSSContentAssistProcessor cssProcessor = new CSSContentAssistProcessor();
    cssProcessor.setDocumentOffset(offset);
    cssProcessor.setQuoteCharOfStyleAttribute(quote);

    return cssProcessor.computeCompletionProposals(viewer, pos);
  }

  protected String getEmptyTagCloseString() {
    if (isXHTML)
      return " />"; //$NON-NLS-1$
    return ">"; //$NON-NLS-1$
  }

  private IContentAssistProcessor getJSContentAssistProcessor() {
    if (fJSContentAssistProcessor == null) {
      fJSContentAssistProcessor = new StructuredTextViewerConfigurationHTML().getContentAssistant(
          null).getContentAssistProcessor(IHTMLPartitions.SCRIPT);
    }
    return fJSContentAssistProcessor;
  }

  private HTMLTemplateCompletionProcessor getTemplateCompletionProcessor() {
    if (fTemplateProcessor == null) {
      fTemplateProcessor = new HTMLTemplateCompletionProcessor();
    }
    return fTemplateProcessor;
  }

  /**
   * Determine if this Document is an XHTML Document. Oprates solely off of the Document Type
   * declaration
   */
  protected boolean getXHTML(Node node) {
    if (node == null)
      return false;

    Document doc = null;
    if (node.getNodeType() != Node.DOCUMENT_NODE)
      doc = node.getOwnerDocument();
    else
      doc = ((Document) node);

    if (doc instanceof IDOMDocument)
      return ((IDOMDocument) doc).isXMLType();

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

  protected void init() {
    getPreferenceStore().addPropertyChangeListener(this);
    reinit();
  }

  protected void reinit() {
    String key = HTMLUIPreferenceNames.AUTO_PROPOSE;
    boolean doAuto = getPreferenceStore().getBoolean(key);
    if (doAuto) {
      key = HTMLUIPreferenceNames.AUTO_PROPOSE_CODE;
      completionProposalAutoActivationCharacters = getPreferenceStore().getString(key).toCharArray();
    } else {
      completionProposalAutoActivationCharacters = null;
    }
  }

  public void release() {
    if (factoryForCSS != null) {
      factoryForCSS.release();
    }
    if (fJSContentAssistProcessor instanceof IReleasable) {
      ((IReleasable) fJSContentAssistProcessor).release();
    }
    getPreferenceStore().removePropertyChangeListener(this);
    super.release();
  }

  protected boolean stringsEqual(String a, String b) {
    return a.equalsIgnoreCase(b);
  }

  public void propertyChange(PropertyChangeEvent event) {
    String property = event.getProperty();

    if (property.compareTo(HTMLUIPreferenceNames.AUTO_PROPOSE) == 0
        || property.compareTo(HTMLUIPreferenceNames.AUTO_PROPOSE_CODE) == 0) {
      reinit();
    }
  }

  protected IPreferenceStore getPreferenceStore() {
    if (fPreferenceStore == null)
      fPreferenceStore = HTMLUIPlugin.getDefault().getPreferenceStore();

    return fPreferenceStore;
  }

  public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentPosition,
      IndexedRegion indexedNode, ITextRegion region) {
    return computeCompletionProposals(viewer, documentPosition);
  }
}
