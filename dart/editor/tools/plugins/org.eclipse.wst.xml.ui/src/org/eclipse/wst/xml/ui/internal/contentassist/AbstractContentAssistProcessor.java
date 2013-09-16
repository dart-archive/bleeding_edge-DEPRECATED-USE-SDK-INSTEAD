/*******************************************************************************
 * Copyright (c) 2001, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.contentassist;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.encoding.ContentTypeEncodingPreferences;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionContainer;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.sse.core.internal.util.Debug;
import org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer;
import org.eclipse.wst.sse.ui.contentassist.StructuredContentAssistProcessor;
import org.eclipse.wst.sse.ui.internal.IReleasable;
import org.eclipse.wst.sse.ui.internal.contentassist.ContentAssistUtils;
import org.eclipse.wst.sse.ui.internal.contentassist.CustomCompletionProposal;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMContent;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDataType;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDocument;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMEntityDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMGroup;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNamedNodeMap;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNodeList;
import org.eclipse.wst.xml.core.internal.contentmodel.basic.CMNamedNodeMapImpl;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQueryAction;
import org.eclipse.wst.xml.core.internal.contentmodel.util.DOMNamespaceHelper;
import org.eclipse.wst.xml.core.internal.document.AttrImpl;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.core.internal.provisional.contenttype.ContentTypeIdForXML;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.eclipse.wst.xml.ui.internal.Logger;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;
import org.eclipse.wst.xml.ui.internal.editor.CMImageUtil;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImageHelper;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImages;
import org.eclipse.wst.xml.ui.internal.preferences.XMLUIPreferenceNames;
import org.eclipse.wst.xml.ui.internal.taginfo.MarkupTagInfoProvider;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @deprecated This class is no longer used locally and will be removed in the future. Implementers
 *             of this class should now use the
 *             <code>org.eclipse.wst.sse.ui.completionProposal</code> extension point in conjunction
 *             with the {@link ICompletionProposalComputer} interface.
 * @see StructuredContentAssistProcessor
 */
abstract public class AbstractContentAssistProcessor implements IContentAssistProcessor,
    IReleasable {
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

  protected static final String INTERNALERROR = XMLUIMessages.SEVERE_internal_error_occu_UI_;
  protected static final String UNKNOWN_ATTR = XMLUIMessages.No_known_attribute__UI_;
  protected static final String UNKNOWN_CONTEXT = XMLUIMessages.Content_Assist_not_availab_UI_;
  protected char completionProposalAutoActivationCharacters[] = null;
  protected char contextInformationAutoActivationCharacters[] = null;
  private AttributeContextInformationPresenter fContextInformationPresenter = null;

  protected String fErrorMessage = null;
  protected XMLContentModelGenerator fGenerator;
  // protected IResource resource = null;
  protected MarkupTagInfoProvider fInfoProvider = null;
  protected ITextViewer fTextViewer = null;

  private final boolean showValues = true;

  public AbstractContentAssistProcessor() {
    init();
  }

  protected void addAttributeNameProposals(ContentAssistRequest contentAssistRequest) {
    IDOMNode node = (IDOMNode) contentAssistRequest.getNode();
    IStructuredDocumentRegion sdRegion = contentAssistRequest.getDocumentRegion();
    // retrieve the list of attributes
    CMElementDeclaration elementDecl = getCMElementDeclaration(node);
    if (elementDecl != null) {
      CMNamedNodeMap attributes = elementDecl.getAttributes();

      CMNamedNodeMapImpl allAttributes = new CMNamedNodeMapImpl(attributes);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        List nodes = ModelQueryUtil.getModelQuery(node.getOwnerDocument()).getAvailableContent(
            (Element) node, elementDecl, ModelQuery.INCLUDE_ATTRIBUTES);
        for (int k = 0; k < nodes.size(); k++) {
          CMNode cmnode = (CMNode) nodes.get(k);
          if (cmnode.getNodeType() == CMNode.ATTRIBUTE_DECLARATION) {
            allAttributes.put(cmnode);
          }
        }
      }
      attributes = allAttributes;

      String matchString = contentAssistRequest.getMatchString();

      // check whether an attribute really exists for the replacement
      // offsets AND if it possesses a value
      boolean attrAtLocationHasValue = false;
      NamedNodeMap attrs = node.getAttributes();
      for (int i = 0; i < attrs.getLength(); i++) {
        AttrImpl existingAttr = (AttrImpl) attrs.item(i);
        ITextRegion name = existingAttr.getNameRegion();

        if ((sdRegion.getStartOffset(name) <= contentAssistRequest.getReplacementBeginPosition())
            && (sdRegion.getStartOffset(name) + name.getLength() >= contentAssistRequest.getReplacementBeginPosition()
                + contentAssistRequest.getReplacementLength())
            && (existingAttr.getValueRegion() != null)) {
          attrAtLocationHasValue = true;
          break;
        }
      }

      // only add proposals for the attributes whose names begin with
      // the matchstring
      if (attributes != null) {
        for (int i = 0; i < attributes.getLength(); i++) {
          CMAttributeDeclaration attrDecl = (CMAttributeDeclaration) attributes.item(i);

          int isRequired = 0;
          if (attrDecl.getUsage() == CMAttributeDeclaration.REQUIRED) {
            isRequired = XMLRelevanceConstants.R_REQUIRED;
          }

          boolean showAttribute = true;
          showAttribute = showAttribute
              && beginsWith(getRequiredName(node, attrDecl), matchString.trim());
          AttrImpl attr = (AttrImpl) node.getAttributes().getNamedItem(
              getRequiredName(node, attrDecl));
          ITextRegion nameRegion = attr != null ? attr.getNameRegion() : null;
          // nameRegion.getEndOffset() + 1 is required to allow for
          // matches against the full name of an existing Attr
          showAttribute = showAttribute
              && (attr == null || nameRegion == null || (nameRegion != null
                  && (sdRegion.getStartOffset(nameRegion) <= contentAssistRequest.getReplacementBeginPosition()) && (sdRegion.getStartOffset(nameRegion)
                  + nameRegion.getLength() >= contentAssistRequest.getReplacementBeginPosition()
                  + contentAssistRequest.getReplacementLength())));
          if (showAttribute) {
            Image attrImage = CMImageUtil.getImage(attrDecl);
            if (attrImage == null) {
              if (isRequired > 0) {
                attrImage = XMLEditorPluginImageHelper.getInstance().getImage(
                    XMLEditorPluginImages.IMG_OBJ_ATT_REQ_OBJ);
              } else {
                attrImage = XMLEditorPluginImageHelper.getInstance().getImage(
                    XMLEditorPluginImages.IMG_OBJ_ATTRIBUTE);
              }
            }

            String proposedText = null;
            String proposedInfo = getAdditionalInfo(elementDecl, attrDecl);
            CustomCompletionProposal proposal = null;
            // attribute is at this location and already exists
            if (attrAtLocationHasValue) {
              // only propose the name
              proposedText = getRequiredName(node, attrDecl);
              proposal = new CustomCompletionProposal(proposedText,
                  contentAssistRequest.getReplacementBeginPosition(),
                  contentAssistRequest.getReplacementLength(), proposedText.length(), attrImage,
                  proposedText, null, proposedInfo, XMLRelevanceConstants.R_XML_ATTRIBUTE_NAME
                      + isRequired, true);
            }
            // no attribute exists or is elsewhere, generate
            // minimally
            else {
              Attr existingAttrNode = (Attr) node.getAttributes().getNamedItem(
                  getRequiredName(node, attrDecl));
              String value = null;
              if (existingAttrNode != null && existingAttrNode.getSpecified()) {
                value = existingAttrNode.getNodeValue();
              }
              if ((value != null) && (value.length() > 0)) {
                proposedText = getRequiredName(node, attrDecl);
              } else {
                proposedText = getRequiredText(node, attrDecl);
              }
              proposal = new CustomCompletionProposal(proposedText,
                  contentAssistRequest.getReplacementBeginPosition(),
                  contentAssistRequest.getReplacementLength(), attrDecl.getNodeName().length() + 2,
                  attrImage,
                  // if the value isn't empty (no empty set of
                  // quotes), show it
                  // BUG 203494, content strings may have "", but not be empty
                  // An empty string is when there's no content between double quotes
                  // and there is no single quote that may be encasing a double quote
                  (showValues && (proposedText.lastIndexOf('\"') - proposedText.indexOf('\"') == 1 && proposedText.indexOf('\'') == -1))
                      ? getRequiredName(node, attrDecl) : proposedText, null, proposedInfo,
                  XMLRelevanceConstants.R_XML_ATTRIBUTE_NAME + isRequired);
            }
            contentAssistRequest.addProposal(proposal);
          }
        }
      }
    } else {
      setErrorMessage(NLS.bind(XMLUIMessages.Element__is_unknown,
          (new Object[] {node.getNodeName()})));
    }
  }

  protected void addAttributeValueProposals(ContentAssistRequest contentAssistRequest) {

    IDOMNode node = (IDOMNode) contentAssistRequest.getNode();

    // Find the attribute region and name for which this position should
    // have a value proposed
    IStructuredDocumentRegion open = node.getFirstStructuredDocumentRegion();
    ITextRegionList openRegions = open.getRegions();
    int i = openRegions.indexOf(contentAssistRequest.getRegion());
    if (i < 0) {
      return;
    }
    ITextRegion nameRegion = null;
    while (i >= 0) {
      nameRegion = openRegions.get(i--);
      if (nameRegion.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_NAME) {
        break;
      }
    }

    // the name region is REQUIRED to do anything useful
    if (nameRegion != null) {
      // Retrieve the declaration
      CMElementDeclaration elementDecl = getCMElementDeclaration(node);

      // String attributeName = nameRegion.getText();
      String attributeName = open.getText(nameRegion);

      CMAttributeDeclaration attrDecl = null;

      // No CMElementDeclaration means no attribute metadata, but
      // retrieve the
      // declaration for the attribute otherwise
      if (elementDecl != null) {
        CMNamedNodeMap attributes = elementDecl.getAttributes();

        CMNamedNodeMapImpl allAttributes = new CMNamedNodeMapImpl(attributes) {
          private Map caseInsensitive;

          private Map getCaseInsensitiveMap() {
            if (caseInsensitive == null)
              caseInsensitive = new HashMap();
            return caseInsensitive;
          }

          public CMNode getNamedItem(String name) {
            CMNode node = super.getNamedItem(name);
            if (node == null) {
              node = (CMNode) getCaseInsensitiveMap().get(name.toLowerCase(Locale.US));
            }
            return node;
          }

          public void put(CMNode cmNode) {
            super.put(cmNode);
            getCaseInsensitiveMap().put(cmNode.getNodeName().toLowerCase(Locale.US), cmNode);
          }
        };
        if (node.getNodeType() == Node.ELEMENT_NODE) {
          List nodes = ModelQueryUtil.getModelQuery(node.getOwnerDocument()).getAvailableContent(
              (Element) node, elementDecl, ModelQuery.INCLUDE_ATTRIBUTES);
          for (int k = 0; k < nodes.size(); k++) {
            CMNode cmnode = (CMNode) nodes.get(k);
            if (cmnode.getNodeType() == CMNode.ATTRIBUTE_DECLARATION) {
              allAttributes.put(cmnode);
            }
          }
        }
        attributes = allAttributes;

        String noprefixName = DOMNamespaceHelper.getUnprefixedName(attributeName);
        if (attributes != null) {
          attrDecl = (CMAttributeDeclaration) attributes.getNamedItem(noprefixName);
          if (attrDecl == null) {
            attrDecl = (CMAttributeDeclaration) attributes.getNamedItem(attributeName);
          }
        }
        if (attrDecl == null) {
          setErrorMessage(UNKNOWN_ATTR, attributeName);
        }
      }

      String currentValue = node.getAttributes().getNamedItem(attributeName).getNodeValue();
      String proposedInfo = null;
      Image image = CMImageUtil.getImage(attrDecl);
      if (image == null) {
        if ((attrDecl != null) && (attrDecl.getUsage() == CMAttributeDeclaration.REQUIRED)) {
          image = XMLEditorPluginImageHelper.getInstance().getImage(
              XMLEditorPluginImages.IMG_OBJ_ATT_REQ_OBJ);
        } else {
          image = XMLEditorPluginImageHelper.getInstance().getImage(
              XMLEditorPluginImages.IMG_OBJ_ATTRIBUTE);
        }
      }

      if ((attrDecl != null) && (attrDecl.getAttrType() != null)) {
        // attribute is known, prompt with values from the declaration
        proposedInfo = getAdditionalInfo(elementDecl, attrDecl);
        List possibleValues = getPossibleDataTypeValues(node, attrDecl);
        String defaultValue = attrDecl.getAttrType().getImpliedValue();
        if (possibleValues.size() > 0 || defaultValue != null) {
          // ENUMERATED VALUES
          String matchString = contentAssistRequest.getMatchString();
          if (matchString == null) {
            matchString = ""; //$NON-NLS-1$
          }
          if ((matchString.length() > 0)
              && (matchString.startsWith("\"") || matchString.startsWith("'"))) {
            matchString = matchString.substring(1);
          }
          boolean currentValid = false;

          // d210858, if the region's a container, don't suggest the
          // enumerated values as they probably won't help
          boolean existingComplicatedValue = (contentAssistRequest.getRegion() != null)
              && (contentAssistRequest.getRegion() instanceof ITextRegionContainer);
          if (!existingComplicatedValue) {
            int rOffset = contentAssistRequest.getReplacementBeginPosition();
            int rLength = contentAssistRequest.getReplacementLength();
            for (Iterator j = possibleValues.iterator(); j.hasNext();) {
              String possibleValue = (String) j.next();
              if (!possibleValue.equals(defaultValue)) {
                currentValid = currentValid || possibleValue.equals(currentValue);
                if ((matchString.length() == 0) || possibleValue.startsWith(matchString)) {
                  String rString = "\"" + possibleValue + "\""; //$NON-NLS-2$//$NON-NLS-1$
                  CustomCompletionProposal proposal = new CustomCompletionProposal(rString,
                      rOffset, rLength, possibleValue.length() + 1,
                      XMLEditorPluginImageHelper.getInstance().getImage(
                          XMLEditorPluginImages.IMG_OBJ_ENUM), rString, null, proposedInfo,
                      XMLRelevanceConstants.R_XML_ATTRIBUTE_VALUE);
                  contentAssistRequest.addProposal(proposal);
                }
              }
            }
            if (defaultValue != null
                && ((matchString.length() == 0) || defaultValue.startsWith(matchString))) {
              String rString = "\"" + defaultValue + "\""; //$NON-NLS-2$//$NON-NLS-1$
              CustomCompletionProposal proposal = new CustomCompletionProposal(rString, rOffset,
                  rLength, defaultValue.length() + 1,
                  XMLEditorPluginImageHelper.getInstance().getImage(
                      XMLEditorPluginImages.IMG_OBJ_DEFAULT), rString, null, proposedInfo,
                  XMLRelevanceConstants.R_XML_ATTRIBUTE_VALUE);
              contentAssistRequest.addProposal(proposal);
            }
          }
        } else if (((attrDecl.getUsage() == CMAttributeDeclaration.FIXED) || (attrDecl.getAttrType().getImpliedValueKind() == CMDataType.IMPLIED_VALUE_FIXED))
            && (attrDecl.getAttrType().getImpliedValue() != null)) {
          // FIXED values
          String value = attrDecl.getAttrType().getImpliedValue();
          if ((value != null) && (value.length() > 0)) {
            String rValue = "\"" + value + "\"";//$NON-NLS-2$//$NON-NLS-1$
            CustomCompletionProposal proposal = new CustomCompletionProposal(rValue,
                contentAssistRequest.getReplacementBeginPosition(),
                contentAssistRequest.getReplacementLength(), rValue.length() + 1, image, rValue,
                null, proposedInfo, XMLRelevanceConstants.R_XML_ATTRIBUTE_VALUE);
            contentAssistRequest.addProposal(proposal);
            if ((currentValue.length() > 0) && !value.equals(currentValue)) {
              rValue = "\"" + currentValue + "\""; //$NON-NLS-2$//$NON-NLS-1$
              proposal = new CustomCompletionProposal(rValue,
                  contentAssistRequest.getReplacementBeginPosition(),
                  contentAssistRequest.getReplacementLength(), rValue.length() + 1, image, rValue,
                  null, proposedInfo, XMLRelevanceConstants.R_XML_ATTRIBUTE_VALUE);
              contentAssistRequest.addProposal(proposal);
            }
          }
        }
      } else {
        // unknown attribute, so supply nice empty values
        proposedInfo = getAdditionalInfo(null, elementDecl);
        CustomCompletionProposal proposal = null;
        if ((currentValue != null) && (currentValue.length() > 0)) {
          String rValue = "\"" + currentValue + "\""; //$NON-NLS-2$//$NON-NLS-1$
          proposal = new CustomCompletionProposal(rValue,
              contentAssistRequest.getReplacementBeginPosition(),
              contentAssistRequest.getReplacementLength(), 1, image, rValue, null, proposedInfo,
              XMLRelevanceConstants.R_XML_ATTRIBUTE_VALUE);
          contentAssistRequest.addProposal(proposal);
        }
      }
    } else {
      setErrorMessage(UNKNOWN_CONTEXT);
    }
  }

  protected void addCommentProposal(ContentAssistRequest contentAssistRequest) {
    contentAssistRequest.addProposal(new CustomCompletionProposal(
        "<!--  -->", //$NON-NLS-1$
        contentAssistRequest.getReplacementBeginPosition(),
        contentAssistRequest.getReplacementLength(), 5,
        XMLEditorPluginImageHelper.getInstance().getImage(XMLEditorPluginImages.IMG_OBJ_COMMENT),
        NLS.bind(XMLUIMessages.Comment__, (new Object[] {" <!--  -->"})), //$NON-NLS-1$
        null, null, XMLRelevanceConstants.R_COMMENT));
  }

  /**
   * Add all of the element declarations int the CMContent object into one big list.
   */
  protected void addContent(List contentList, CMContent content) {
    if (content == null) {
      return;
    }
    if (content instanceof CMGroup) {
      CMNodeList children = ((CMGroup) content).getChildNodes();
      if (children == null) {
        return;
      }
      for (int i = 0; i < children.getLength(); i++) {
        CMNode child = children.item(i);
        if (child.getNodeType() == CMNode.ELEMENT_DECLARATION) {
          contentList.add(child);
        } else {
          if (child.getNodeType() == CMNode.GROUP) {
            addContent(contentList, (CMContent) child);
          } else {
            throw new IllegalArgumentException("Unknown content child: " + child); //$NON-NLS-1$
          }
        }
      }
    } else {
      contentList.add(content);
    }
  }

  protected void addDocTypeProposal(ContentAssistRequest contentAssistRequest) {
    // if a DocumentElement exists, use that for the root Element name
    String rootname = "unspecified"; //$NON-NLS-1$
    if (contentAssistRequest.getNode().getOwnerDocument().getDocumentElement() != null) {
      rootname = contentAssistRequest.getNode().getOwnerDocument().getDocumentElement().getNodeName();
    }

    String proposedText = "<!DOCTYPE " + rootname + " PUBLIC \"//UNKNOWN/\" \"unknown.dtd\">"; //$NON-NLS-1$ //$NON-NLS-2$
    ICompletionProposal proposal = new CustomCompletionProposal(proposedText,
        contentAssistRequest.getReplacementBeginPosition(),
        contentAssistRequest.getReplacementLength(), 10,
        XMLEditorPluginImageHelper.getInstance().getImage(XMLEditorPluginImages.IMG_OBJ_DOCTYPE),
        "<!DOCTYPE ... >", //$NON-NLS-1$
        null, null, XMLRelevanceConstants.R_DOCTYPE);
    // TODO provide special documentation on doc type
    contentAssistRequest.addProposal(proposal);
  }

  /**
   * Add the proposals for a completely empty document
   */
  protected void addEmptyDocumentProposals(ContentAssistRequest contentAssistRequest) {
    // nothing
  }

  /**
   * Add the proposals for the name in an end tag
   */
  protected void addEndTagNameProposals(ContentAssistRequest contentAssistRequest) {

    if (contentAssistRequest.getStartOffset() + contentAssistRequest.getRegion().getTextLength() < contentAssistRequest.getReplacementBeginPosition()) {
      CustomCompletionProposal proposal = new CustomCompletionProposal(
          ">", //$NON-NLS-1$
          contentAssistRequest.getReplacementBeginPosition(),
          contentAssistRequest.getReplacementLength(), 1,
          XMLEditorPluginImageHelper.getInstance().getImage(
              XMLEditorPluginImages.IMG_OBJ_TAG_GENERIC), NLS.bind(XMLUIMessages.Close_with__,
              (new Object[] {" '>'"})), //$NON-NLS-1$
          null, null, XMLRelevanceConstants.R_END_TAG_NAME);
      contentAssistRequest.addProposal(proposal);
    } else {
      IDOMNode node = (IDOMNode) contentAssistRequest.getNode();
      ModelQuery modelQuery = ModelQueryUtil.getModelQuery(node.getOwnerDocument());
      Node aNode = contentAssistRequest.getNode();
      String matchString = contentAssistRequest.getMatchString();
      if (matchString.startsWith("</")) {
        matchString = matchString.substring(2);
      }
      while (aNode != null) {
        if (aNode.getNodeType() == Node.ELEMENT_NODE) {
          if (aNode.getNodeName().startsWith(matchString)) {
            IDOMNode aXMLNode = (IDOMNode) aNode;
            CMElementDeclaration ed = modelQuery.getCMElementDeclaration((Element) aNode);
            if ((aXMLNode.getEndStructuredDocumentRegion() == null)
                && ((ed == null) || (ed.getContentType() != CMElementDeclaration.EMPTY))) {
              String replacementText = aNode.getNodeName();
              String displayText = replacementText;
              String proposedInfo = (ed != null) ? getAdditionalInfo(null, ed) : null;
              if (!contentAssistRequest.getDocumentRegion().isEnded()) {
                replacementText += ">"; //$NON-NLS-1$
              }
              CustomCompletionProposal proposal = null;
              // double check to see if the region acted upon is
              // a tag name; replace it if so
              Image image = CMImageUtil.getImage(ed);
              if (image == null) {
                image = XMLEditorPluginImageHelper.getInstance().getImage(
                    XMLEditorPluginImages.IMG_OBJ_TAG_GENERIC);
              }
              if (contentAssistRequest.getRegion().getType() == DOMRegionContext.XML_TAG_NAME) {
                proposal = new CustomCompletionProposal(replacementText,
                    contentAssistRequest.getStartOffset(),
                    contentAssistRequest.getRegion().getTextLength(), replacementText.length(),
                    image, displayText, null, proposedInfo, XMLRelevanceConstants.R_END_TAG_NAME);
              } else {
                proposal = new CustomCompletionProposal(replacementText,
                    contentAssistRequest.getReplacementBeginPosition(),
                    contentAssistRequest.getReplacementLength(), replacementText.length(), image,
                    NLS.bind(XMLUIMessages.Close_with__, (new Object[] {"'" + displayText + "'"})), //$NON-NLS-1$ //$NON-NLS-2$
                    null, proposedInfo, XMLRelevanceConstants.R_END_TAG_NAME);
              }
              contentAssistRequest.addProposal(proposal);
            }
          }
        }
        aNode = aNode.getParentNode();
      }
    }
  }

  /**
   * Prompt for end tags to a non-empty Node that hasn't ended Handles these cases: <br>
   * <tagOpen>| <br>
   * <tagOpen>< |<br>
   * <tagOpen></ |
   * 
   * @param contentAssistRequest
   */
  protected void addEndTagProposals(ContentAssistRequest contentAssistRequest) {
    IDOMNode node = (IDOMNode) contentAssistRequest.getParent();

    if (isCommentNode(node)) {
      // loop and find non comment node parent
      while ((node != null) && isCommentNode(node)) {
        node = (IDOMNode) node.getParentNode();
      }
    }

    // node is already closed
    if (node.isClosed()) {
      // loop and find non comment unclose node parent
      while ((node != null) && node.isClosed()) {
        node = (IDOMNode) node.getParentNode();
      }
    }
    // there were no unclosed tags
    if (node == null) {
      return;
    }

    // data to create a CustomCompletionProposal
    String replaceText = node.getNodeName() + ">"; //$NON-NLS-1$
    int replaceBegin = contentAssistRequest.getReplacementBeginPosition();
    int replaceLength = contentAssistRequest.getReplacementLength();
    int cursorOffset = node.getNodeName().length() + 1;
    String displayString = ""; //$NON-NLS-1$
    String proposedInfo = ""; //$NON-NLS-1$
    Image image = XMLEditorPluginImageHelper.getInstance().getImage(
        XMLEditorPluginImages.IMG_OBJ_TAG_GENERIC);

    setErrorMessage(null);
    boolean addProposal = false;

    if (node.getNodeType() == Node.ELEMENT_NODE) {
      // ////////////////////////////////////////////////////////////////////////////////////
      IStructuredDocument sDoc = (IStructuredDocument) fTextViewer.getDocument();
      IStructuredDocumentRegion xmlEndTagOpen = sDoc.getRegionAtCharacterOffset(contentAssistRequest.getReplacementBeginPosition());
      // skip backward to "<", "</", or the (unclosed) start tag, null
      // if not found
      String type = ""; //$NON-NLS-1$
      while ((xmlEndTagOpen != null)
          && ((type = xmlEndTagOpen.getType()) != DOMRegionContext.XML_END_TAG_OPEN)
          && (type != DOMRegionContext.XML_TAG_CLOSE) && !needsEndTag(xmlEndTagOpen)
          && (type != DOMRegionContext.XML_TAG_OPEN)) {
        xmlEndTagOpen = xmlEndTagOpen.getPrevious();
      }

      if (xmlEndTagOpen == null) {
        return;
      }

      node = (IDOMNode) node.getModel().getIndexedRegion(xmlEndTagOpen.getStartOffset());
      node = (IDOMNode) node.getParentNode();

      if (isStartTag(xmlEndTagOpen)) {
        // this is the case for a start tag w/out end tag
        // eg:
        // <p>
        // <% String test = "test"; %>
        // |
        if (needsEndTag(xmlEndTagOpen)) {
          String tagName = getTagName(xmlEndTagOpen);
          xmlEndTagOpen.getTextEndOffset();
          replaceLength = 0;
          replaceText = "</" + tagName + ">"; //$NON-NLS-1$ //$NON-NLS-2$ $NON-NLS-2$
          // replaceText = "</" + node.getNodeName() + ">";
          // //$NON-NLS-1$ $NON-NLS-2$
          cursorOffset = tagName.length() + 3;
          displayString = NLS.bind(XMLUIMessages.End_with__, (new Object[] {tagName}));
          addProposal = true;
        }
      } else if (type == DOMRegionContext.XML_END_TAG_OPEN) {
        // this is the case for: <tag> </ |
        // possibly <tag> </ |<anotherTag>
        // should only be replacing white space...
        replaceLength = (replaceBegin > xmlEndTagOpen.getTextEndOffset()) ? replaceBegin
            - xmlEndTagOpen.getTextEndOffset() : 0;
        replaceText = node.getNodeName() + ">"; //$NON-NLS-1$
        cursorOffset = replaceText.length();
        replaceBegin = xmlEndTagOpen.getTextEndOffset();
        displayString = NLS.bind(XMLUIMessages.End_with_, (new Object[] {node.getNodeName()}));
        addProposal = true;
      } else if (type == DOMRegionContext.XML_TAG_OPEN) {
        // this is the case for: <tag> < |
        replaceText = "/" + node.getNodeName() + ">"; //$NON-NLS-1$ //$NON-NLS-2$ $NON-NLS-2$
        cursorOffset = replaceText.length();
        // replaceText = "/" + node.getNodeName() + ">"; //$NON-NLS-1$
        // $NON-NLS-2$
        // should only be replacing white space...
        replaceLength = (replaceBegin > xmlEndTagOpen.getTextEndOffset()) ? replaceBegin
            - xmlEndTagOpen.getTextEndOffset() : 0;
        replaceBegin = xmlEndTagOpen.getTextEndOffset();
        displayString = NLS.bind(XMLUIMessages.End_with_, (new Object[] {"/" + node.getNodeName()})); //$NON-NLS-1$
        addProposal = true;
      }
    }
    // ////////////////////////////////////////////////////////////////////////////////////
    // sometimes the node is not null, but
    // getNodeValue() is null, put in a null check
    else if ((node.getNodeValue() != null) && (node.getNodeValue().indexOf("</") != -1)) { //$NON-NLS-1$
      // the case where "</" is started, but the nodes comes in as a
      // text node (instead of element)
      // like this: <tag> </|
      Node parent = node.getParentNode();
      if ((parent != null) && (parent.getNodeType() != Node.DOCUMENT_NODE)) {
        replaceText = parent.getNodeName() + ">"; //$NON-NLS-1$
        cursorOffset = replaceText.length();
        displayString = NLS.bind(XMLUIMessages.End_with__, (new Object[] {parent.getNodeName()}));
        setErrorMessage(null);
        addProposal = true;
      }
    }
    // ////////////////////////////////////////////////////////////////////////////////////
    else if (node.getNodeType() == Node.DOCUMENT_NODE) {
      setErrorMessage(UNKNOWN_CONTEXT);
    }
    if (addProposal == true) {
      CustomCompletionProposal proposal = new CustomCompletionProposal(replaceText, replaceBegin,
          replaceLength, cursorOffset, image, displayString, null, proposedInfo,
          XMLRelevanceConstants.R_END_TAG);
      contentAssistRequest.addProposal(proposal);
    }
  }

  protected void addEntityProposals(ContentAssistRequest contentAssistRequest,
      int documentPosition, ITextRegion completionRegion, IDOMNode treeNode) {
    ICompletionProposal[] eps = computeEntityReferenceProposals(documentPosition, completionRegion,
        treeNode);
    for (int i = 0; (eps != null) && (i < eps.length); i++) {
      contentAssistRequest.addProposal(eps[i]);
    }
  }

  protected void addEntityProposals(Vector proposals, Properties map, String key, int nodeOffset,
      IStructuredDocumentRegion sdRegion, ITextRegion completionRegion) {
    if (map == null) {
      return;
    }
    String entityName = ""; //$NON-NLS-1$
    String entityValue = ""; //$NON-NLS-1$
    Image entityIcon = XMLEditorPluginImageHelper.getInstance().getImage(
        XMLEditorPluginImages.IMG_OBJ_ENTITY_REFERENCE);
    String replacementText = ""; //$NON-NLS-1$
    String displayString = ""; //$NON-NLS-1$
    Enumeration keys = map.keys();

    while ((keys != null) && keys.hasMoreElements()) {
      entityName = (String) keys.nextElement();
      entityValue = map.getProperty(entityName);
      // filter based on partial entity string...
      if (entityName.toLowerCase().startsWith(key.toLowerCase()) || key.trim().equals("")) //$NON-NLS-1$
      {
        // figure out selection...if text is selected, add it to
        // selection length
        int selectionLength = nodeOffset;
        if (fTextViewer != null) {
          selectionLength += fTextViewer.getSelectedRange().y;
        }
        // create a new proposal for entity string...
        replacementText = "&" + entityName + ";"; //$NON-NLS-1$ //$NON-NLS-2$ 
        displayString = "&" + entityName + "; (" + entityValue + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        ICompletionProposal cp = new CustomCompletionProposal(replacementText,
            sdRegion.getStartOffset(completionRegion), selectionLength, replacementText.length(),
            entityIcon, displayString, null, null, XMLRelevanceConstants.R_ENTITY);
        if (cp != null) {
          proposals.add(cp);
        }
      }
    }
  }

  protected void addPCDATAProposal(String nodeName, ContentAssistRequest contentAssistRequest) {
    CustomCompletionProposal proposal = new CustomCompletionProposal(
        "<![CDATA[]]>", //$NON-NLS-1$
        contentAssistRequest.getReplacementBeginPosition(),
        contentAssistRequest.getReplacementLength(), 9,
        XMLEditorPluginImageHelper.getInstance().getImage(
            XMLEditorPluginImages.IMG_OBJ_CDATASECTION), "CDATA Section", //$NON-NLS-1$
        null, null, XMLRelevanceConstants.R_CDATA);
    contentAssistRequest.addProposal(proposal);

    proposal = new CustomCompletionProposal(nodeName,
        contentAssistRequest.getReplacementBeginPosition(),
        contentAssistRequest.getReplacementLength(), nodeName.length(),
        XMLEditorPluginImageHelper.getInstance().getImage(XMLEditorPluginImages.IMG_OBJ_TXTEXT),
        "#PCDATA", //$NON-NLS-1$
        null, null, XMLRelevanceConstants.R_CDATA);
    contentAssistRequest.addProposal(proposal);
  }

  protected void addStartDocumentProposals(ContentAssistRequest contentAssistRequest) {
    Node aNode = contentAssistRequest.getNode();
    boolean xmlpiFound = false;
    Document owningDocument = aNode.getOwnerDocument();
    // ==> // int xmlpiNodePosition = -1;

    // make sure xmlpi is root element
    // don't want doctype proposal if XMLPI isn't first element...
    Node first = owningDocument.getFirstChild();
    boolean xmlpiIsFirstElement = ((first != null) && (first.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE));
    boolean insertDoctype = xmlpiIsFirstElement;

    for (Node child = owningDocument.getFirstChild(); child != null; child = child.getNextSibling()) {
      boolean xmlpi = ((child.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) && child.getNodeName().equals(
          "xml")); //$NON-NLS-1$
      xmlpiFound = xmlpiFound || xmlpi;
      if (xmlpiFound) {
        if (child instanceof IDOMNode) {
          // ==> // int xmlpiNodePosition =
          // ((XMLNode)child).getEndOffset();
        }
        // skip white space and text
        while (((child = child.getNextSibling()) != null)
            && (child.getNodeType() == Node.TEXT_NODE)) {
          // just skipping
        }
        // check if theres a node inbetween XMLPI and cursor position
        if ((child != null) && (child instanceof IDOMNode)) {
          if ((contentAssistRequest.getReplacementBeginPosition() >= ((IDOMNode) child).getEndOffset())
              || !xmlpiIsFirstElement) {
            insertDoctype = false;
          }
        }
        break;
      }
    }

    if (xmlpiFound && (owningDocument.getDoctype() == null)
        && isCursorAfterXMLPI(contentAssistRequest) && insertDoctype) {
      addDocTypeProposal(contentAssistRequest);
    }
  }

  /**
   * Close an unclosed start tag
   */
  protected void addTagCloseProposals(ContentAssistRequest contentAssistRequest) {
    IDOMNode node = (IDOMNode) contentAssistRequest.getParent();
    if (node.getNodeType() == Node.ELEMENT_NODE) {

      CMElementDeclaration elementDecl = getCMElementDeclaration(node);
      String proposedInfo = (elementDecl != null) ? getAdditionalInfo(null, elementDecl) : null;
      int contentType = (elementDecl != null) ? elementDecl.getContentType()
          : CMElementDeclaration.ANY;
      // if it's XML and content doesn't HAVE to be element, add "/>"
      // proposal.
      boolean endWithSlashBracket = (getXML(node) && (contentType != CMElementDeclaration.ELEMENT));

      Image image = CMImageUtil.getImage(elementDecl);
      if (image == null) {
        image = XMLEditorPluginImageHelper.getInstance().getImage(
            XMLEditorPluginImages.IMG_OBJ_TAG_GENERIC);
      }

      // is the start tag ended properly?
      if ((contentAssistRequest.getDocumentRegion() == node.getFirstStructuredDocumentRegion())
          && !(node.getFirstStructuredDocumentRegion()).isEnded()) {
        setErrorMessage(null);
        // Is this supposed to be an empty tag? Note that if we can't
        // tell, we assume it's not.
        if ((elementDecl != null) && (elementDecl.getContentType() == CMElementDeclaration.EMPTY)) {
          // prompt with a self-closing end character if needed
          CustomCompletionProposal proposal = new CustomCompletionProposal(
              getContentGenerator().getStartTagClose(node, elementDecl),
              contentAssistRequest.getReplacementBeginPosition(),
              // this is one of the few times to ignore the length --
              // always insert
              // contentAssistRequest.getReplacementLength(),
              0, getContentGenerator().getStartTagClose(node, elementDecl).length(), image,
              NLS.bind(XMLUIMessages.Close_with___,
                  (new Object[] {getContentGenerator().getStartTagClose(node, elementDecl)})),
              null, proposedInfo, XMLRelevanceConstants.R_CLOSE_TAG);
          contentAssistRequest.addProposal(proposal);
        } else {
          // prompt with a close for the start tag
          CustomCompletionProposal proposal = new CustomCompletionProposal(">", //$NON-NLS-1$
              contentAssistRequest.getReplacementBeginPosition(),
              // this is one of the few times to ignore the
              // length -- always insert
              // contentAssistRequest.getReplacementLength(),
              0, 1, image, NLS.bind(XMLUIMessages.Close_with__, (new Object[] {" '>'"})), //$NON-NLS-1$
              null, proposedInfo, XMLRelevanceConstants.R_CLOSE_TAG);
          contentAssistRequest.addProposal(proposal);

          // prompt with the closer for the start tag and an end tag
          // if one is not present
          if (node.getEndStructuredDocumentRegion() == null) {
            // make sure tag name is actually what it thinks it
            // is...(eg. <%@ vs. <jsp:directive)
            IStructuredDocumentRegion sdr = contentAssistRequest.getDocumentRegion();
            String openingTagText = (sdr != null) ? sdr.getFullText() : ""; //$NON-NLS-1$
            if ((openingTagText != null) && (openingTagText.indexOf(node.getNodeName()) != -1)) {
              proposal = new CustomCompletionProposal(
                  "></" + node.getNodeName() + ">", //$NON-NLS-2$//$NON-NLS-1$
                  contentAssistRequest.getReplacementBeginPosition(),
                  // this is one of the few times to
                  // ignore the length -- always insert
                  // contentAssistRequest.getReplacementLength(),
                  0, 1, image, NLS.bind(XMLUIMessages.Close_with____,
                      (new Object[] {node.getNodeName()})), null, proposedInfo,
                  XMLRelevanceConstants.R_CLOSE_TAG);
              contentAssistRequest.addProposal(proposal);
            }
          }
          // prompt with slash bracket "/>" incase if it's a self
          // ending tag
          if (endWithSlashBracket) {
            proposal = new CustomCompletionProposal("/>", //$NON-NLS-1$
                contentAssistRequest.getReplacementBeginPosition(),
                // this is one of the few times to ignore
                // the length -- always insert
                // contentAssistRequest.getReplacementLength(),
                0, 2, image, NLS.bind(XMLUIMessages.Close_with__, (new Object[] {" \"/>\""})), //$NON-NLS-1$
                null, proposedInfo, XMLRelevanceConstants.R_CLOSE_TAG + 1); // +1
            // to
            // bring
            // to
            // top
            // of
            // list
            contentAssistRequest.addProposal(proposal);
          }
        }
      } else if ((contentAssistRequest.getDocumentRegion() == node.getLastStructuredDocumentRegion())
          && !node.getLastStructuredDocumentRegion().isEnded()) {
        setErrorMessage(null);
        // prompt with a closing end character for the end tag
        CustomCompletionProposal proposal = new CustomCompletionProposal(">", //$NON-NLS-1$
            contentAssistRequest.getReplacementBeginPosition(),
            // this is one of the few times to ignore the
            // length -- always insert
            // contentAssistRequest.getReplacementLength(),
            0, 1, image, NLS.bind(XMLUIMessages.Close_with__, (new Object[] {" '>'"})), //$NON-NLS-1$
            null, proposedInfo, XMLRelevanceConstants.R_CLOSE_TAG);
        contentAssistRequest.addProposal(proposal);
      }
    } else if (node.getNodeType() == Node.DOCUMENT_NODE) {
      setErrorMessage(UNKNOWN_CONTEXT);
    }
  }

  protected void addTagInsertionProposals(ContentAssistRequest contentAssistRequest,
      int childPosition) {
    List cmnodes = null;
    Node parent = contentAssistRequest.getParent();
    String error = null;

    // (nsd) This is only valid at the document element level
    // only valid if it's XML (check added 2/17/2004)
    if ((parent != null) && (parent.getNodeType() == Node.DOCUMENT_NODE)
        && ((IDOMDocument) parent).isXMLType() && !isCursorAfterXMLPI(contentAssistRequest)) {
      return;
    }
    // only want proposals if cursor is after doctype...
    if (!isCursorAfterDoctype(contentAssistRequest)) {
      return;
    }

    // fix for meta-info comment nodes.. they currently "hide" other
    // proposals because the don't
    // have a content model (so can't propose any children..)
    if ((parent != null) && (parent instanceof IDOMNode) && isCommentNode((IDOMNode) parent)) {
      // loop and find non comment node?
      while ((parent != null) && isCommentNode((IDOMNode) parent)) {
        parent = parent.getParentNode();
      }
    }

    if (parent.getNodeType() == Node.ELEMENT_NODE) {
      CMElementDeclaration parentDecl = getCMElementDeclaration(parent);
      if (parentDecl != null) {
        // XSD-specific ability - no filtering
        CMDataType childType = parentDecl.getDataType();
        if (childType != null) {
          String[] childStrings = childType.getEnumeratedValues();
          String defaultValue = childType.getImpliedValue();
          if (childStrings != null || defaultValue != null) {
            // the content string is the sole valid child...so
            // replace the rest
            int begin = contentAssistRequest.getReplacementBeginPosition();
            int length = contentAssistRequest.getReplacementLength();
            if (parent instanceof IDOMNode) {
              if (((IDOMNode) parent).getLastStructuredDocumentRegion() != ((IDOMNode) parent).getFirstStructuredDocumentRegion()) {
                begin = ((IDOMNode) parent).getFirstStructuredDocumentRegion().getEndOffset();
                length = ((IDOMNode) parent).getLastStructuredDocumentRegion().getStartOffset()
                    - begin;
              }
            }
            String proposedInfo = getAdditionalInfo(parentDecl, childType);
            for (int i = 0; i < childStrings.length; i++) {
              if (!childStrings[i].equals(defaultValue)) {
                CustomCompletionProposal textProposal = new CustomCompletionProposal(
                    childStrings[i], begin, length, childStrings[i].length(),
                    XMLEditorPluginImageHelper.getInstance().getImage(
                        XMLEditorPluginImages.IMG_OBJ_ENUM), childStrings[i], null, proposedInfo,
                    XMLRelevanceConstants.R_TAG_INSERTION);
                contentAssistRequest.addProposal(textProposal);
              }
            }
            if (defaultValue != null) {
              CustomCompletionProposal textProposal = new CustomCompletionProposal(defaultValue,
                  begin, length, defaultValue.length(),
                  XMLEditorPluginImageHelper.getInstance().getImage(
                      XMLEditorPluginImages.IMG_OBJ_DEFAULT), defaultValue, null, proposedInfo,
                  XMLRelevanceConstants.R_TAG_INSERTION);
              contentAssistRequest.addProposal(textProposal);
            }
          }
        }
      }
      if ((parentDecl != null) && (parentDecl.getContentType() == CMElementDeclaration.PCDATA)) {
        addPCDATAProposal(parentDecl.getNodeName(), contentAssistRequest);
      } else {
        // retrieve the list of all possible children within this
        // parent context
        cmnodes = getAvailableChildElementDeclarations((Element) parent, childPosition,
            ModelQueryAction.INSERT);

        // retrieve the list of the possible children within this
        // parent context and at this index
        List strictCMNodeSuggestions = null;
        if (XMLUIPreferenceNames.SUGGESTION_STRATEGY_VALUE_STRICT.equals(XMLUIPlugin.getInstance().getPreferenceStore().getString(
            XMLUIPreferenceNames.SUGGESTION_STRATEGY))) {
          strictCMNodeSuggestions = getValidChildElementDeclarations((Element) parent,
              childPosition, ModelQueryAction.INSERT);
        }
        Iterator nodeIterator = cmnodes.iterator();
        if (!nodeIterator.hasNext()) {
          if (getCMElementDeclaration(parent) != null) {
            error = NLS.bind(XMLUIMessages._Has_no_available_child,
                (new Object[] {parent.getNodeName()}));
          } else {
            error = NLS.bind(XMLUIMessages.Element__is_unknown,
                (new Object[] {parent.getNodeName()}));
          }
        }
        String matchString = contentAssistRequest.getMatchString();
        // chop off any leading <'s and whitespace from the
        // matchstring
        while ((matchString.length() > 0)
            && (Character.isWhitespace(matchString.charAt(0)) || beginsWith(matchString, "<"))) {
          //$NON-NLS-1$
          matchString = matchString.substring(1);
        }
        while (nodeIterator.hasNext()) {
          Object o = nodeIterator.next();
          if (o instanceof CMElementDeclaration) {
            CMElementDeclaration elementDecl = (CMElementDeclaration) o;
            // only add proposals for the child element's that
            // begin with the matchstring
            String tagname = getRequiredName(parent, elementDecl);
            boolean isStrictCMNodeSuggestion = strictCMNodeSuggestions != null
                ? strictCMNodeSuggestions.contains(elementDecl) : false;

            Image image = CMImageUtil.getImage(elementDecl);

            if (image == null) {
              if (strictCMNodeSuggestions != null) {
                image = isStrictCMNodeSuggestion
                    ? XMLEditorPluginImageHelper.getInstance().getImage(
                        XMLEditorPluginImages.IMG_OBJ_TAG_GENERIC_EMPHASIZED)
                    : XMLEditorPluginImageHelper.getInstance().getImage(
                        XMLEditorPluginImages.IMG_OBJ_TAG_GENERIC_DEEMPHASIZED);
              } else {
                image = XMLEditorPluginImageHelper.getInstance().getImage(
                    XMLEditorPluginImages.IMG_OBJ_TAG_GENERIC);
              }

            }

            // int markupAdjustment =
            // getContentGenerator().getMinimalStartTagLength(parent,
            // elementDecl);
            if (beginsWith(tagname, matchString)) {
              String proposedText = getRequiredText(parent, elementDecl);

              // https://bugs.eclipse.org/bugs/show_bug.cgi?id=89811
              // place cursor in first empty quotes
              int markupAdjustment = getCursorPositionForProposedText(proposedText);

              String proposedInfo = getAdditionalInfo(parentDecl, elementDecl);
              int relevance = isStrictCMNodeSuggestion
                  ? XMLRelevanceConstants.R_STRICTLY_VALID_TAG_INSERTION
                  : XMLRelevanceConstants.R_TAG_INSERTION;
              CustomCompletionProposal proposal = new CustomCompletionProposal(proposedText,
                  contentAssistRequest.getReplacementBeginPosition(),
                  contentAssistRequest.getReplacementLength(), markupAdjustment, image, tagname,
                  null, proposedInfo, relevance);
              contentAssistRequest.addProposal(proposal);
            }
          }
        }
        if (contentAssistRequest.getProposals().size() == 0) {
          if (error != null) {
            setErrorMessage(error);
          } else if ((contentAssistRequest.getMatchString() != null)
              && (contentAssistRequest.getMatchString().length() > 0)) {
            setErrorMessage(NLS.bind(XMLUIMessages.No_known_child_tag,
                (new Object[] {parent.getNodeName(), contentAssistRequest.getMatchString()})));
            //$NON-NLS-1$ = "No known child tag names of <{0}> begin with \"{1}\"."
          } else {
            setErrorMessage(NLS.bind(XMLUIMessages.__Has_no_known_child,
                (new Object[] {parent.getNodeName()})));
          }
        }
      }
    } else if (parent.getNodeType() == Node.DOCUMENT_NODE) {
      // Can only prompt with elements if the cursor position is past
      // the XML processing
      // instruction and DOCTYPE declaration
      boolean xmlpiFound = false;
      boolean doctypeFound = false;
      int minimumOffset = -1;

      for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {

        boolean xmlpi = ((child.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) && child.getNodeName().equals(
            "xml")); //$NON-NLS-1$
        boolean doctype = child.getNodeType() == Node.DOCUMENT_TYPE_NODE;
        if (xmlpi || (doctype && (minimumOffset < 0))) {
          minimumOffset = ((IDOMNode) child).getFirstStructuredDocumentRegion().getStartOffset()
              + ((IDOMNode) child).getFirstStructuredDocumentRegion().getTextLength();
        }
        xmlpiFound = xmlpiFound || xmlpi;
        doctypeFound = doctypeFound || doctype;
      }

      if (contentAssistRequest.getReplacementBeginPosition() >= minimumOffset) {
        List childDecls = getAvailableRootChildren((Document) parent, childPosition);
        for (int i = 0; i < childDecls.size(); i++) {
          CMElementDeclaration ed = (CMElementDeclaration) childDecls.get(i);
          if (ed != null) {
            Image image = CMImageUtil.getImage(ed);
            if (image == null) {
              image = XMLEditorPluginImageHelper.getInstance().getImage(
                  XMLEditorPluginImages.IMG_OBJ_TAG_GENERIC);
            }
            String proposedText = getRequiredText(parent, ed);
            String tagname = getRequiredName(parent, ed);
            // account for the &lt; and &gt;
            int markupAdjustment = getContentGenerator().getMinimalStartTagLength(parent, ed);
            String proposedInfo = getAdditionalInfo(null, ed);
            CustomCompletionProposal proposal = new CustomCompletionProposal(proposedText,
                contentAssistRequest.getReplacementBeginPosition(),
                contentAssistRequest.getReplacementLength(), markupAdjustment, image, tagname,
                null, proposedInfo, XMLRelevanceConstants.R_TAG_INSERTION);
            contentAssistRequest.addProposal(proposal);
          }
        }
      }
    }
  }

  protected void addTagNameProposals(ContentAssistRequest contentAssistRequest, int childPosition) {
    List cmnodes = null;
    Node parent = contentAssistRequest.getParent();
    IDOMNode node = (IDOMNode) contentAssistRequest.getNode();
    String error = null;
    String matchString = contentAssistRequest.getMatchString();
    if (parent.getNodeType() == Node.ELEMENT_NODE) {
      // retrieve the list of children
      // validActions = getAvailableChildrenAtIndex((Element) parent,
      // childPosition);
      cmnodes = getAvailableChildElementDeclarations((Element) parent, childPosition,
          ModelQueryAction.INSERT);
      Iterator nodeIterator = cmnodes.iterator();
      // chop off any leading <'s and whitespace from the matchstring
      while ((matchString.length() > 0)
          && (Character.isWhitespace(matchString.charAt(0)) || beginsWith(matchString, "<"))) {
        //$NON-NLS-1$
        matchString = matchString.substring(1);
      }
      if (!nodeIterator.hasNext()) {
        error = NLS.bind(XMLUIMessages.__Has_no_known_child, (new Object[] {parent.getNodeName()}));
      }
      while (nodeIterator.hasNext()) {
        CMNode elementDecl = (CMNode) nodeIterator.next();
        if (elementDecl != null) {
          // only add proposals for the child element's that begin
          // with the matchstring
          String proposedText = null;
          int cursorAdjustment = 0;

          // do a check to see if partial attributes of partial tag
          // names are in list
          if (((node != null) && (node.getAttributes() != null)
              && (node.getAttributes().getLength() > 0) && attributeInList(node, parent,
                elementDecl))
              || ((node.getNodeType() != Node.TEXT_NODE) && node.getFirstStructuredDocumentRegion().isEnded())) {

            proposedText = getRequiredName(parent, elementDecl);
            cursorAdjustment = proposedText.length();
          } else {
            proposedText = getRequiredName(parent, elementDecl);
            cursorAdjustment = proposedText.length();
            if (elementDecl instanceof CMElementDeclaration) {
              CMElementDeclaration ed = (CMElementDeclaration) elementDecl;
              // https://bugs.eclipse.org/bugs/show_bug.cgi?id=89811
              StringBuffer sb = new StringBuffer();
              getContentGenerator().generateTag(parent, ed, sb);
              // since it's a name proposal, assume '<' is
              // already there
              // only return the rest of the tag
              proposedText = sb.toString().substring(1);
              cursorAdjustment = getCursorPositionForProposedText(proposedText);

              // cursorAdjustment = proposedText.length() +
              // 1;
              // proposedText += "></" +
              // getRequiredName(parent, elementDecl) + ">";
              // //$NON-NLS-2$//$NON-NLS-1$
            }
          }
          if (beginsWith(proposedText, matchString)) {
            Image image = CMImageUtil.getImage(elementDecl);
            if (image == null) {
              image = XMLEditorPluginImageHelper.getInstance().getImage(
                  XMLEditorPluginImages.IMG_OBJ_TAG_GENERIC);
            }
            String proposedInfo = getAdditionalInfo(getCMElementDeclaration(parent), elementDecl);
            CustomCompletionProposal proposal = new CustomCompletionProposal(proposedText,
                contentAssistRequest.getReplacementBeginPosition(),
                contentAssistRequest.getReplacementLength(), cursorAdjustment, image,
                getRequiredName(parent, elementDecl), null, proposedInfo,
                XMLRelevanceConstants.R_TAG_NAME);
            contentAssistRequest.addProposal(proposal);
          }
        }
      }
      if (contentAssistRequest.getProposals().size() == 0) {
        if (error != null) {
          setErrorMessage(error);
        } else if ((contentAssistRequest.getMatchString() != null)
            && (contentAssistRequest.getMatchString().length() > 0)) {
          setErrorMessage(NLS.bind(XMLUIMessages.No_known_child_tag_names,
              (new Object[] {parent.getNodeName(), contentAssistRequest.getMatchString()})));
          //$NON-NLS-1$ = "No known child tag names of <{0}> begin with \"{1}\""
        } else {
          setErrorMessage(NLS.bind(XMLUIMessages.__Has_no_known_child,
              (new Object[] {parent.getNodeName()})));
        }
      }
    } else if (parent.getNodeType() == Node.DOCUMENT_NODE) {
      List childElements = getAvailableRootChildren((Document) parent, childPosition);
      for (int i = 0; i < childElements.size(); i++) {
        CMNode ed = (CMNode) childElements.get(i);
        if (ed == null) {
          continue;
        }
        String proposedText = null;
        int cursorAdjustment = 0;
        if (ed instanceof CMElementDeclaration) {
          // proposedText = getRequiredName(parent, ed);
          StringBuffer sb = new StringBuffer();
          getContentGenerator().generateTag(parent, (CMElementDeclaration) ed, sb);
          // tag starts w/ '<', but we want to compare to name
          proposedText = sb.toString().substring(1);

          if (!beginsWith(proposedText, matchString)) {
            continue;
          }

          cursorAdjustment = getCursorPositionForProposedText(proposedText);

          String proposedInfo = getAdditionalInfo(null, ed);
          Image image = CMImageUtil.getImage(ed);
          if (image == null) {
            image = XMLEditorPluginImageHelper.getInstance().getImage(
                XMLEditorPluginImages.IMG_OBJ_TAG_GENERIC);
          }
          CustomCompletionProposal proposal = new CustomCompletionProposal(proposedText,
              contentAssistRequest.getReplacementBeginPosition(),
              contentAssistRequest.getReplacementLength(), cursorAdjustment, image,
              getRequiredName(parent, ed), null, proposedInfo, XMLRelevanceConstants.R_TAG_NAME);
          contentAssistRequest.addProposal(proposal);
        }
      }
    }
  }

  /**
   * this is the position the cursor should be in after the proposal is applied
   * 
   * @param proposedText
   * @return the position the cursor should be in after the proposal is applied
   */
  private int getCursorPositionForProposedText(String proposedText) {
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
   * @deprecated XML proposal is added via xml declaration template instead
   */
  protected void addXMLProposal(ContentAssistRequest contentAssistRequest) {
    String proposedText = "<?xml version=\"1.0\" encoding=\"" + ContentTypeEncodingPreferences.getUserPreferredCharsetName(ContentTypeIdForXML.ContentTypeID_XML) + "\"?>"; //$NON-NLS-2$//$NON-NLS-1$
    ICompletionProposal proposal = new CustomCompletionProposal(proposedText,
        contentAssistRequest.getReplacementBeginPosition(),
        contentAssistRequest.getReplacementLength(), proposedText.length(),
        XMLEditorPluginImageHelper.getInstance().getImage(
            XMLEditorPluginImages.IMG_OBJ_PROCESSINGINSTRUCTION), proposedText, null, null,
        XMLRelevanceConstants.R_XML_DECLARATION);
    // TODO add special XML proposal info
    contentAssistRequest.addProposal(proposal);
  }

  /**
   * This method determines if any of the attributes in the proposed XMLNode node, are possible
   * values of attributes from possible Elements at this point in the document according to the
   * Content Model.
   * 
   * @param node the element with attributes that you would like to test if are possible for
   *          possible Elements at this point
   * @param cmnode possible element at this point in the document (depending on what 'node' is) true
   *          if any attributes of 'node' match any possible attributes from 'cmnodes' list.
   */
  protected boolean attributeInList(IDOMNode node, Node parent, CMNode cmnode) {
    if ((node == null) || (parent == null) || (cmnode == null)) {
      return false;
    }
    String elementMatchString = node.getNodeName();
    String cmnodeName = getRequiredName(parent, cmnode);// cmnode.getNodeName();
    if (node instanceof Element) {
      NamedNodeMap map = ((Element) node).getAttributes();
      String attrMatchString = ""; //$NON-NLS-1$
      CMNamedNodeMap cmattrMap = null;
      // iterate attribute possibilities for partially started node
      for (int i = 0; (map != null) && (i < map.getLength()); i++) {
        attrMatchString = map.item(i).getNodeName();
        // filter on whatever user typed for element name already
        if (beginsWith(cmnodeName, elementMatchString)) {
          if (cmnode.getNodeType() == CMNode.ELEMENT_DECLARATION) {
            cmattrMap = ((CMElementDeclaration) cmnode).getAttributes();

            CMNamedNodeMapImpl allAttributes = new CMNamedNodeMapImpl(cmattrMap);
            List nodes = ModelQueryUtil.getModelQuery(node.getOwnerDocument()).getAvailableContent(
                (Element) node, (CMElementDeclaration) cmnode, ModelQuery.INCLUDE_ATTRIBUTES);
            for (int k = 0; k < nodes.size(); k++) {
              CMNode adnode = (CMNode) nodes.get(k);
              if (adnode.getNodeType() == CMNode.ATTRIBUTE_DECLARATION) {
                allAttributes.put(adnode);
              }
            }
            cmattrMap = allAttributes;

            // iterate possible attributes from a cmnode in
            // proposal list
            for (int k = 0; (cmattrMap != null) && (k < cmattrMap.getLength()); k++) {
              // check if name matches
              if (cmattrMap.item(k).getNodeName().equals(attrMatchString)) {
                return true;
              }
            }
          }
        }
      }
    }
    return false;
  }

  protected boolean beginsWith(String aString, String prefix) {
    if ((aString == null) || (prefix == null)) {
      return true;
    }
    // (pa) matching independent of case to be consistant with Java
    // editor CA
    return aString.toLowerCase().startsWith(prefix.toLowerCase());
  }

  protected ContentAssistRequest computeAttributeProposals(int documentPosition,
      String matchString, ITextRegion completionRegion, IDOMNode nodeAtOffset, IDOMNode node) {
    ContentAssistRequest contentAssistRequest = null;
    IStructuredDocumentRegion sdRegion = getStructuredDocumentRegion(documentPosition);
    if (documentPosition < sdRegion.getStartOffset(completionRegion)) {
      // setup to insert new attributes
      contentAssistRequest = newContentAssistRequest(nodeAtOffset, node, sdRegion,
          completionRegion, documentPosition, 0, matchString);
    } else {
      // Setup to replace an existing attribute name
      contentAssistRequest = newContentAssistRequest(nodeAtOffset, node, sdRegion,
          completionRegion, sdRegion.getStartOffset(completionRegion),
          completionRegion.getTextLength(), matchString);
    }
    addAttributeNameProposals(contentAssistRequest);
    contentAssistRequest.setReplacementBeginPosition(documentPosition);
    contentAssistRequest.setReplacementLength(0);
    if ((node.getFirstStructuredDocumentRegion() != null)
        && (!node.getFirstStructuredDocumentRegion().isEnded())) {
      addTagCloseProposals(contentAssistRequest);
    }
    return contentAssistRequest;
  }

  protected ContentAssistRequest computeAttributeValueProposals(int documentPosition,
      String matchString, ITextRegion completionRegion, IDOMNode nodeAtOffset, IDOMNode node) {
    ContentAssistRequest contentAssistRequest = null;
    IStructuredDocumentRegion sdRegion = getStructuredDocumentRegion(documentPosition);
    if ((documentPosition > sdRegion.getStartOffset(completionRegion)
        + completionRegion.getTextLength())
        && (sdRegion.getStartOffset(completionRegion) + completionRegion.getTextLength() != sdRegion.getStartOffset(completionRegion)
            + completionRegion.getLength())) {
      // setup to add a new attribute at the documentPosition
      IDOMNode actualNode = (IDOMNode) node.getModel().getIndexedRegion(
          sdRegion.getStartOffset(completionRegion));
      contentAssistRequest = newContentAssistRequest(actualNode, actualNode, sdRegion,
          completionRegion, documentPosition, 0, matchString);
      addAttributeNameProposals(contentAssistRequest);
      if ((actualNode.getFirstStructuredDocumentRegion() != null)
          && !actualNode.getFirstStructuredDocumentRegion().isEnded()) {
        addTagCloseProposals(contentAssistRequest);
      }
    } else {
      // setup to replace the existing value
      if (!nodeAtOffset.getFirstStructuredDocumentRegion().isEnded()
          && (documentPosition < sdRegion.getStartOffset(completionRegion))) {
        // if the IStructuredDocumentRegion isn't closed and the
        // cursor is in front of the value, add
        contentAssistRequest = newContentAssistRequest(nodeAtOffset, node, sdRegion,
            completionRegion, documentPosition, 0, matchString);
        addAttributeNameProposals(contentAssistRequest);
      } else {
        contentAssistRequest = newContentAssistRequest(nodeAtOffset, node, sdRegion,
            completionRegion, sdRegion.getStartOffset(completionRegion),
            completionRegion.getTextLength(), matchString);
        addAttributeValueProposals(contentAssistRequest);
      }
    }
    return contentAssistRequest;
  }

  protected ContentAssistRequest computeCompletionProposals(int documentPosition,
      String matchString, ITextRegion completionRegion, IDOMNode treeNode, IDOMNode xmlnode) {
    ContentAssistRequest contentAssistRequest = null;
    String regionType = completionRegion.getType();
    IStructuredDocumentRegion sdRegion = getStructuredDocumentRegion(documentPosition);

    // Handle the most common and best supported cases
    if ((xmlnode.getNodeType() == Node.ELEMENT_NODE)
        || (xmlnode.getNodeType() == Node.DOCUMENT_NODE)) {
      if (regionType == DOMRegionContext.XML_TAG_OPEN) {
        contentAssistRequest = computeTagOpenProposals(documentPosition, matchString,
            completionRegion, treeNode, xmlnode);
      } else if (regionType == DOMRegionContext.XML_TAG_NAME) {
        contentAssistRequest = computeTagNameProposals(documentPosition, matchString,
            completionRegion, treeNode, xmlnode);
      } else if (regionType == DOMRegionContext.XML_TAG_ATTRIBUTE_NAME) {
        contentAssistRequest = computeAttributeProposals(documentPosition, matchString,
            completionRegion, treeNode, xmlnode);
      } else if (regionType == DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS) {
        contentAssistRequest = computeEqualsProposals(documentPosition, matchString,
            completionRegion, treeNode, xmlnode);
      } else if ((regionType == DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE)
          && (documentPosition == sdRegion.getTextEndOffset())
          && (sdRegion.getText(completionRegion).endsWith("\"") || sdRegion.getText(completionRegion).endsWith("\'"))) //$NON-NLS-1$ //$NON-NLS-2$
      {
        // this is for when the cursor is at the end of the closing
        // quote for an attribute..
        IDOMNode actualNode = (IDOMNode) xmlnode.getModel().getIndexedRegion(
            sdRegion.getStartOffset(completionRegion));
        contentAssistRequest = newContentAssistRequest(actualNode, actualNode, sdRegion,
            completionRegion, documentPosition, 0, matchString);
        addTagCloseProposals(contentAssistRequest);
      } else if (regionType == DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE) {
        contentAssistRequest = computeAttributeValueProposals(documentPosition, matchString,
            completionRegion, treeNode, xmlnode);
      } else if ((regionType == DOMRegionContext.XML_TAG_CLOSE)
          || (regionType == DOMRegionContext.XML_EMPTY_TAG_CLOSE)
          || (regionType.equals(DOMJSPRegionContextsPrivateCopy.JSP_DIRECTIVE_CLOSE))) {
        contentAssistRequest = computeTagCloseProposals(documentPosition, matchString,
            completionRegion, treeNode, xmlnode);
      } else if (regionType == DOMRegionContext.XML_END_TAG_OPEN) {
        contentAssistRequest = computeEndTagOpenProposals(documentPosition, matchString,
            completionRegion, treeNode, xmlnode);
      } else if ((regionType == DOMRegionContext.XML_CONTENT)
          || (regionType == DOMRegionContext.XML_CHAR_REFERENCE)
          || (regionType == DOMRegionContext.XML_ENTITY_REFERENCE)
          || (regionType == DOMRegionContext.XML_PE_REFERENCE)) {
        contentAssistRequest = computeContentProposals(documentPosition, matchString,
            completionRegion, treeNode, xmlnode);
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
        contentAssistRequest = newContentAssistRequest(treeNode, xmlnode.getParentNode(), sdRegion,
            completionRegion, documentPosition, 0, matchString);
        addTagInsertionProposals(contentAssistRequest, getElementPositionForModelQuery(treeNode));
        addStartDocumentProposals(contentAssistRequest);
      }
    }
    // Not a Document or Element? (odd cases go here for now)
    else if (isCloseRegion(completionRegion)) {
      contentAssistRequest = newContentAssistRequest(treeNode, xmlnode.getParentNode(), sdRegion,
          completionRegion,
          sdRegion.getStartOffset(completionRegion) + completionRegion.getLength(), 0, matchString);
      addStartDocumentProposals(contentAssistRequest);
      if (documentPosition >= sdRegion.getTextEndOffset(completionRegion)) {
        addTagInsertionProposals(contentAssistRequest,
            getElementPositionForModelQuery(treeNode) + 1);
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
      contentAssistRequest = newContentAssistRequest(treeNode, xmlnode.getParentNode(), sdRegion,
          completionRegion, documentPosition, 0, matchString);
      addTagInsertionProposals(contentAssistRequest, getElementPositionForModelQuery(treeNode));
      addStartDocumentProposals(contentAssistRequest);
    }
    return contentAssistRequest;
  }

  /**
   * CONTENT ASSIST STARTS HERE Return a list of proposed code completions based on the specified
   * location within the document that corresponds to the current cursor position within the
   * text-editor control.
   * 
   * @param textViewer
   * @param documentPosition - the cursor location within the document an array of
   *          ICompletionProposals
   */
  public ICompletionProposal[] computeCompletionProposals(ITextViewer textViewer,
      int documentPosition) {

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
        IStructuredModel sModel = StructuredModelManager.getModelManager().getExistingModelForRead(
            textViewer.getDocument());
        try {
          if (sModel != null) {
            IDOMDocument docNode = ((IDOMModel) sModel).getDocument();
            contentAssistRequest = newContentAssistRequest(docNode, docNode, sdRegion,
                completionRegion, documentPosition, 0, null);
            addEmptyDocumentProposals(contentAssistRequest);
          }
        } finally {
          if (sModel != null) {
            sModel.releaseFromRead();
          }
        }
        if (contentAssistRequest == null) {
          Logger.logException(new IllegalStateException("problem getting model")); //$NON-NLS-1$
          return new ICompletionProposal[0];
        }
        return contentAssistRequest.getCompletionProposals();
      }
      // MASSIVE ERROR CONDITION
      Logger.logException(new IllegalStateException("completion region was null")); //$NON-NLS-1$
      setErrorMessage(INTERNALERROR);
      contentAssistRequest = newContentAssistRequest((Node) treeNode, node.getParentNode(),
          sdRegion, completionRegion, documentPosition, 0, ""); //$NON-NLS-1$
      return contentAssistRequest.getCompletionProposals();
    }

    // catch documents where no region can be determined
    if ((xmlnode.getNodeType() == Node.DOCUMENT_NODE)
        && ((completionRegion == null) || (xmlnode.getChildNodes() == null) || (xmlnode.getChildNodes().getLength() == 0))) {
      contentAssistRequest = computeStartDocumentProposals(documentPosition, matchString,
          completionRegion, (IDOMNode) treeNode, xmlnode);
      return contentAssistRequest.getCompletionProposals();
    }

    // compute normal proposals
    contentAssistRequest = computeCompletionProposals(documentPosition, matchString,
        completionRegion, (IDOMNode) treeNode, xmlnode);
    if (contentAssistRequest == null) {
      contentAssistRequest = newContentAssistRequest((Node) treeNode, node.getParentNode(),
          sdRegion, completionRegion, documentPosition, 0, ""); //$NON-NLS-1$
      if (Debug.displayWarnings) {
        System.out.println(UNKNOWN_CONTEXT
            + " " + completionRegion.getType() + "@" + documentPosition); //$NON-NLS-2$//$NON-NLS-1$
      }
      setErrorMessage(UNKNOWN_CONTEXT);
    }

    /*
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=123892 Only set this error message if nothing
     * else was already set
     */
    if (contentAssistRequest.getProposals().size() == 0 && getErrorMessage() == null) {
      setErrorMessage(UNKNOWN_CONTEXT);
    }

    return contentAssistRequest.getCompletionProposals();
  }

  protected ContentAssistRequest computeContentProposals(int documentPosition, String matchString,
      ITextRegion completionRegion, IDOMNode nodeAtOffset, IDOMNode node) {
    ContentAssistRequest contentAssistRequest = null;

    // setup to add children at the content node's position
    contentAssistRequest = newContentAssistRequest(nodeAtOffset, node,
        getStructuredDocumentRegion(documentPosition), completionRegion, documentPosition, 0,
        matchString);
    if ((node != null) && (node.getNodeType() == Node.DOCUMENT_NODE)
        && (((Document) node).getDoctype() == null)) {
      addStartDocumentProposals(contentAssistRequest);
    }
    addTagInsertionProposals(contentAssistRequest, getElementPositionForModelQuery(nodeAtOffset));
    if (node.getNodeType() != Node.DOCUMENT_NODE) {
      addEndTagProposals(contentAssistRequest);
    }
    // entities?
    addEntityProposals(contentAssistRequest, documentPosition, completionRegion, node);
    // addEntityProposals(contentAssistRequest);
    return contentAssistRequest;
  }

  /**
   * Returns information about possible contexts based on the specified location within the document
   * that corresponds to the current cursor position within the text viewer.
   * 
   * @param viewer the viewer whose document is used to compute the possible contexts an array of
   *          context information objects or <code>null</code> if no context could be found
   */
  public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
    return new IContextInformation[0];
  }

  protected ContentAssistRequest computeEndTagOpenProposals(int documentPosition,
      String matchString, ITextRegion completionRegion, IDOMNode nodeAtOffset, IDOMNode node) {
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
      contentAssistRequest = newContentAssistRequest(nodeAtOffset, nodeAtOffset.getParentNode(),
          sdRegion, completionRegion, sdRegion.getStartOffset(nameRegion),
          nameRegion.getTextLength(), matchString);
    } else {
      if (nodeAtOffset.getFirstStructuredDocumentRegion() == sdRegion) {
        // abnormal case, this unmatched end tag will be a sibling
        contentAssistRequest = newContentAssistRequest(nodeAtOffset, nodeAtOffset.getParentNode(),
            sdRegion, completionRegion, documentPosition, 0, matchString);
      } else {
        // normal case, this end tag is the parent
        contentAssistRequest = newContentAssistRequest(nodeAtOffset, nodeAtOffset, sdRegion,
            completionRegion, documentPosition, 0, matchString);
      }
    }
    // if (documentPosition >= sdRegion.getStartOffset(completionRegion) +
    // completionRegion.getTextLength())
    addEndTagProposals(contentAssistRequest);
    // else
    if (completionRegionStart == documentPosition) {
      // positioned at start of end tag
      addTagInsertionProposals(contentAssistRequest, node.getChildNodes().getLength());
    }
    return contentAssistRequest;
  }

  /**
   * return all possible EntityReferenceProposals (according to current position in doc)
   */
  protected ICompletionProposal[] computeEntityReferenceProposals(int documentPosition,
      ITextRegion completionRegion, IDOMNode treeNode) {
    // only handle XML content for now
    Vector proposals = new Vector(); // ICompletionProposals
    IStructuredDocumentRegion sdRegion = getStructuredDocumentRegion(documentPosition);
    if ((completionRegion != null) && (completionRegion.getType() == DOMRegionContext.XML_CONTENT)) {
      int nodeOffset = documentPosition - sdRegion.getStartOffset(completionRegion);
      String regionText = sdRegion.getFullText(completionRegion);

      // if directly to the right of a &, region will be null, need to
      // move to
      // the previous region...there might be a better way to do this
      if ((regionText != null) && regionText.trim().equals("") && (documentPosition > 0)) { //$NON-NLS-1$
        IStructuredDocumentRegion prev = treeNode.getStructuredDocument().getRegionAtCharacterOffset(
            documentPosition - 1);
        if ((prev != null) && prev.getText().equals("&")) { //$NON-NLS-1$
          // https://bugs.eclipse.org/bugs/show_bug.cgi?id=206680
          // examine previous region
          sdRegion = prev;
          completionRegion = prev.getLastRegion();
          regionText = prev.getFullText();
          nodeOffset = 1;
        }
      }

      // string must start w/ &
      if ((regionText != null) && regionText.startsWith("&")) { //$NON-NLS-1$						 		
        String key = (nodeOffset > 0) ? regionText.substring(1, nodeOffset) : ""; //$NON-NLS-1$

        // get entity proposals, passing in the appropriate start
        // string
        ModelQuery mq = ModelQueryUtil.getModelQuery(((Node) treeNode).getOwnerDocument());
        if (mq != null) {
          CMDocument xmlDoc = mq.getCorrespondingCMDocument(treeNode);
          CMNamedNodeMap cmmap = null;
          Properties entities = null;
          if (xmlDoc != null) {
            cmmap = xmlDoc.getEntities();
          }
          if (cmmap != null) {
            entities = mapToProperties(cmmap);
          } else // 224787 in absence of content model, just use
          // minimal 5 entities
          {
            entities = new Properties();
            entities.put("quot", "\""); //$NON-NLS-1$ //$NON-NLS-2$
            entities.put("apos", "'"); //$NON-NLS-1$ //$NON-NLS-2$
            entities.put("amp", "&"); //$NON-NLS-1$ //$NON-NLS-2$
            entities.put("lt", "<"); //$NON-NLS-1$ //$NON-NLS-2$
            entities.put("gt", ">"); //$NON-NLS-1$ //$NON-NLS-2$	
            entities.put("nbsp", " "); //$NON-NLS-1$ //$NON-NLS-2$									
          }
          addEntityProposals(proposals, entities, key, nodeOffset, sdRegion, completionRegion);
        }
      }
    }
    return (ICompletionProposal[]) ((proposals.size() > 0)
        ? proposals.toArray(new ICompletionProposal[proposals.size()]) : null);
  }

  protected ContentAssistRequest computeEqualsProposals(int documentPosition, String matchString,
      ITextRegion completionRegion, IDOMNode nodeAtOffset, IDOMNode node) {
    ContentAssistRequest contentAssistRequest = null;
    IStructuredDocumentRegion sdRegion = getStructuredDocumentRegion(documentPosition);
    ITextRegion valueRegion = node.getStartStructuredDocumentRegion().getRegionAtCharacterOffset(
        sdRegion.getStartOffset(completionRegion) + completionRegion.getLength());
    if ((valueRegion != null)
        && (valueRegion.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE)
        && (sdRegion.getStartOffset(valueRegion) <= documentPosition)) {
      // replace the adjacent attribute value
      contentAssistRequest = newContentAssistRequest(nodeAtOffset, node, sdRegion, valueRegion,
          sdRegion.getStartOffset(valueRegion), valueRegion.getTextLength(), matchString);
    } else {
      // append an attribute value after the '='
      contentAssistRequest = newContentAssistRequest(nodeAtOffset, node, sdRegion,
          completionRegion, documentPosition, 0, matchString);
    }
    addAttributeValueProposals(contentAssistRequest);
    return contentAssistRequest;
  }

  protected ContentAssistRequest computeStartDocumentProposals(int documentPosition,
      String matchString, ITextRegion completionRegion, IDOMNode nodeAtOffset, IDOMNode node) {
    // setup for a non-empty document, but one that hasn't been formally
    // started
    ContentAssistRequest contentAssistRequest = null;
    contentAssistRequest = newContentAssistRequest(nodeAtOffset, node,
        getStructuredDocumentRegion(documentPosition), completionRegion, documentPosition, 0,
        matchString);
    addStartDocumentProposals(contentAssistRequest);
    return contentAssistRequest;
  }

  protected ContentAssistRequest computeTagCloseProposals(int documentPosition, String matchString,
      ITextRegion completionRegion, IDOMNode nodeAtOffset, IDOMNode node) {
    ContentAssistRequest contentAssistRequest = null;
    IStructuredDocumentRegion sdRegion = getStructuredDocumentRegion(documentPosition);

    if ((node.getNodeType() == Node.DOCUMENT_NODE) || (documentPosition >= sdRegion.getEndOffset())) {
      // this is a content request as the documentPosition is AFTER the
      // end of the closing region
      if ((node == nodeAtOffset) && (node.getParentNode() != null)) {
        node = (IDOMNode) node.getParentNode();
      }
      contentAssistRequest = newContentAssistRequest(nodeAtOffset, node, sdRegion,
          completionRegion, documentPosition, 0, matchString);
      addTagInsertionProposals(contentAssistRequest, getElementPositionForModelQuery(nodeAtOffset));
      if ((node.getNodeType() != Node.DOCUMENT_NODE)
          && (node.getEndStructuredDocumentRegion() == null)) {
        addEndTagProposals(contentAssistRequest);
      }
    } else {
      // at the start of the tag's close or within it
      ITextRegion closeRegion = sdRegion.getLastRegion();
      boolean insideTag = !sdRegion.isEnded()
          || (documentPosition <= sdRegion.getStartOffset(closeRegion));
      if (insideTag) {
        // this is a request for completions within a tag
        contentAssistRequest = newContentAssistRequest(nodeAtOffset, node, sdRegion,
            completionRegion, documentPosition, 0, matchString);
        if ((node.getNodeType() != Node.DOCUMENT_NODE)
            && (node.getEndStructuredDocumentRegion() != null)) {
          addTagCloseProposals(contentAssistRequest);
        }
        if (sdRegion == nodeAtOffset.getFirstStructuredDocumentRegion()) {
          contentAssistRequest.setReplacementBeginPosition(documentPosition);
          contentAssistRequest.setReplacementLength(0);
          addAttributeNameProposals(contentAssistRequest);
        }
      }
    }
    return contentAssistRequest;
  }

  protected ContentAssistRequest computeTagNameProposals(int documentPosition, String matchString,
      ITextRegion completionRegion, IDOMNode nodeAtOffset, IDOMNode node) {
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
            contentAssistRequest = newContentAssistRequest(actualNode, actualNode, sdRegion,
                completionRegion, documentPosition - matchString.length(), matchString.length(),
                matchString);
            if (node.getStructuredDocument().getRegionAtCharacterOffset(
                sdRegion.getStartOffset(completionRegion) - 1).getRegionAtCharacterOffset(
                sdRegion.getStartOffset(completionRegion) - 1).getType() == DOMRegionContext.XML_TAG_OPEN) {
              addAttributeNameProposals(contentAssistRequest);
            }
            addTagCloseProposals(contentAssistRequest);
          } else {
            // it's name
            contentAssistRequest = newContentAssistRequest(actualNode, actualNode.getParentNode(),
                sdRegion, completionRegion, documentPosition - matchString.length(),
                matchString.length(), matchString);
            addTagNameProposals(contentAssistRequest, getElementPositionForModelQuery(actualNode));
          }
        } else {
          if (documentPosition >= sdRegion.getStartOffset(completionRegion)
              + completionRegion.getLength()) {
            // insert name
            contentAssistRequest = newContentAssistRequest(actualNode, actualNode.getParentNode(),
                sdRegion, completionRegion, documentPosition, 0, matchString);
          } else {
            // replace name
            contentAssistRequest = newContentAssistRequest(actualNode, actualNode.getParentNode(),
                sdRegion, completionRegion, sdRegion.getStartOffset(completionRegion),
                completionRegion.getTextLength(), matchString);
          }
          addEndTagNameProposals(contentAssistRequest);
        }
      }
    } else {
      if (documentPosition > sdRegion.getStartOffset(completionRegion)
          + completionRegion.getTextLength()) {
        // unclosed tag with only a name; should prompt for attributes
        // and a close instead
        contentAssistRequest = newContentAssistRequest(nodeAtOffset, node, sdRegion,
            completionRegion, documentPosition - matchString.length(), matchString.length(),
            matchString);
        addAttributeNameProposals(contentAssistRequest);
        addTagCloseProposals(contentAssistRequest);
      } else {
        if (sdRegion.getRegions().get(0).getType() != DOMRegionContext.XML_END_TAG_OPEN) {
          int replaceLength = documentPosition - sdRegion.getStartOffset(completionRegion);
          contentAssistRequest = newContentAssistRequest(node, node.getParentNode(), sdRegion,
              completionRegion, sdRegion.getStartOffset(completionRegion), replaceLength,
              matchString);
          addTagNameProposals(contentAssistRequest, getElementPositionForModelQuery(nodeAtOffset));
        } else {
          IDOMNode actualNode = (IDOMNode) node.getModel().getIndexedRegion(documentPosition);
          if (actualNode != null) {
            if (documentPosition >= sdRegion.getStartOffset(completionRegion)
                + completionRegion.getTextLength()) {
              contentAssistRequest = newContentAssistRequest(actualNode,
                  actualNode.getParentNode(), sdRegion, completionRegion, documentPosition, 0,
                  matchString);
            } else {
              contentAssistRequest = newContentAssistRequest(actualNode,
                  actualNode.getParentNode(), sdRegion, completionRegion,
                  sdRegion.getStartOffset(completionRegion), completionRegion.getTextLength(),
                  matchString);
            }
            addEndTagNameProposals(contentAssistRequest);
          }
        }
      }
    }
    return contentAssistRequest;
  }

  protected ContentAssistRequest computeTagOpenProposals(int documentPosition, String matchString,
      ITextRegion completionRegion, IDOMNode nodeAtOffset, IDOMNode node) {
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
          contentAssistRequest = newContentAssistRequest(actualNode, actualNode, sdRegion,
              completionRegion, documentPosition, 0, matchString);
          if (actualNode.hasChildNodes())
            addTagNameProposals(contentAssistRequest,
                getElementPositionForModelQuery(actualNode.getLastChild()));
          else
            addTagNameProposals(contentAssistRequest, 0);
        } else {
          contentAssistRequest = newContentAssistRequest(actualNode, actualNode.getParentNode(),
              sdRegion, completionRegion, documentPosition, 0, matchString);
          addTagNameProposals(contentAssistRequest, getElementPositionForModelQuery(actualNode));
        }
        addEndTagProposals(contentAssistRequest); // (pa) 220850
      }
    } else {
      if (documentPosition == sdRegion.getStartOffset(completionRegion)) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
          // at the start of an existing tag, right before the '<'
          contentAssistRequest = newContentAssistRequest(nodeAtOffset, node.getParentNode(),
              sdRegion, completionRegion, documentPosition, 0, matchString);
          addTagInsertionProposals(contentAssistRequest,
              getElementPositionForModelQuery(nodeAtOffset));
          addEndTagProposals(contentAssistRequest);
        } else if (node.getNodeType() == Node.DOCUMENT_NODE) {
          // at the opening of the VERY first tag with a '<'
          contentAssistRequest = newContentAssistRequest(nodeAtOffset, node.getParentNode(),
              sdRegion, completionRegion, sdRegion.getStartOffset(completionRegion),
              completionRegion.getTextLength(), matchString);
          addStartDocumentProposals(contentAssistRequest);
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
          contentAssistRequest = newContentAssistRequest(node, node.getParentNode(), sdRegion,
              completionRegion, sdRegion.getStartOffset(name), name.getTextLength(), matchString);
        } else {
          // insert a valid new name, or possibly an end tag
          contentAssistRequest = newContentAssistRequest(nodeAtOffset,
              ((Node) nodeAtOffset).getParentNode(), sdRegion, completionRegion, documentPosition,
              0, matchString);
          addEndTagProposals(contentAssistRequest);
          contentAssistRequest.setReplacementBeginPosition(documentPosition);
          contentAssistRequest.setReplacementLength(0);
        }
        addTagNameProposals(contentAssistRequest, getElementPositionForModelQuery(nodeAtOffset));
      }
    }
    return contentAssistRequest;
  }

  /**
   * Retreives cmnode's documentation to display in the completion proposal's additional info. If no
   * documentation exists for cmnode, try displaying parentOrOwner's documentation String any
   * documentation information to display for cmnode. <code>null</code> if there is nothing to
   * display.
   */
  protected String getAdditionalInfo(CMNode parentOrOwner, CMNode cmnode) {
    String addlInfo = null;

    if (cmnode == null) {
      if (Debug.displayWarnings) {
        new IllegalArgumentException("Null declaration!").printStackTrace(); //$NON-NLS-1$
      }
      return null;
    }

    addlInfo = getInfoProvider().getInfo(cmnode);
    if ((addlInfo == null) && (parentOrOwner != null)) {
      addlInfo = getInfoProvider().getInfo(parentOrOwner);
    }
    return addlInfo;
  }

  // returns a list of ModelQueryActions
  protected List getAvailableChildrenAtIndex(Element parent, int index, int validityChecking) {
    List list = new ArrayList();
    CMElementDeclaration parentDecl = getCMElementDeclaration(parent);
    if (parentDecl != null) {
      ModelQuery modelQuery = ModelQueryUtil.getModelQuery(parent.getOwnerDocument());
      // taken from ActionManagers
      // int editMode = modelQuery.getEditMode();
      int editMode = ModelQuery.EDIT_MODE_UNCONSTRAINED;
      int ic = (editMode == ModelQuery.EDIT_MODE_CONSTRAINED_STRICT)
          ? ModelQuery.INCLUDE_CHILD_NODES | ModelQuery.INCLUDE_SEQUENCE_GROUPS
          : ModelQuery.INCLUDE_CHILD_NODES;
      modelQuery.getInsertActions(parent, parentDecl, index, ic, validityChecking, list);
    }
    return list;
  }

  // returns a list of CMElementDeclarations
  protected List getAvailableRootChildren(Document document, int childIndex) {
    List list = null;

    // extract the valid 'root' node name from the DocumentType Node
    DocumentType docType = document.getDoctype();
    String rootName = null;
    if (docType != null) {
      rootName = docType.getNodeName();
    }
    if (rootName == null) {
      return new ArrayList(0);
    }

    for (Node child = document.getFirstChild(); child != null; child = child.getNextSibling()) {
      // make sure the "root" Element isn't already present
      // is it required to be an Element?
      if ((child.getNodeType() == Node.ELEMENT_NODE) && stringsEqual(child.getNodeName(), rootName)) {
        // if the node is missing either the start or end tag, don't
        // count it as present
        if ((child instanceof IDOMNode)
            && ((((IDOMNode) child).getStartStructuredDocumentRegion() == null) || (((IDOMNode) child).getEndStructuredDocumentRegion() == null))) {
          continue;
        }
        if (Debug.displayInfo) {
          System.out.println(rootName + " already present!"); //$NON-NLS-1$
        }
        setErrorMessage(NLS.bind(XMLUIMessages.The_document_element__, (new Object[] {rootName})));
        return new ArrayList(0);
      }
    }

    list = new ArrayList(1);
    ModelQuery modelQuery = ModelQueryUtil.getModelQuery(document);
    if (modelQuery != null) {
      CMDocument cmdoc = modelQuery.getCorrespondingCMDocument(document);
      if (cmdoc != null) {
        if (rootName != null) {
          CMElementDeclaration rootDecl = (CMElementDeclaration) cmdoc.getElements().getNamedItem(
              rootName);
          if (rootDecl != null) {
            list.add(rootDecl);
          } else {
            // supply the given document name anyway, even if it
            // is an error
            list.add(new SimpleCMElementDeclaration(rootName));
            if (Debug.displayInfo || Debug.displayWarnings) {
              System.out.println("No definition found for " + rootName + " in " + docType.getPublicId() + "/" + docType.getSystemId()); //$NON-NLS-3$//$NON-NLS-2$//$NON-NLS-1$
            }
            String location = "" + (docType.getPublicId() != null ? docType.getPublicId() + "/" : "") + (docType.getSystemId() != null ? docType.getSystemId() : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            //$NON-NLS-4$//$NON-NLS-3$//$NON-NLS-2$//$NON-NLS-1$
            //$NON-NLS-4$//$NON-NLS-3$//$NON-NLS-2$//$NON-NLS-1$
            if (location.length() > 0) {
              setErrorMessage(NLS.bind(XMLUIMessages.No_definition_for_in, (new Object[] {
                  rootName, location})));
            } else {
              setErrorMessage(NLS.bind(XMLUIMessages.No_definition_for, (new Object[] {rootName})));
            }
          }
        }
      } else {
        if (Debug.displayInfo || Debug.displayWarnings) {
          System.out.println("No content model found."); //$NON-NLS-1$
        }
        //$NON-NLS-1$
        //$NON-NLS-1$
        String location = "" + (docType.getPublicId() != null ? docType.getPublicId() + "/" : "") + (docType.getSystemId() != null ? docType.getSystemId() : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        //$NON-NLS-4$//$NON-NLS-3$//$NON-NLS-2$//$NON-NLS-1$
        //$NON-NLS-4$//$NON-NLS-3$//$NON-NLS-2$//$NON-NLS-1$
        if (location.length() > 0) {
          setErrorMessage(NLS.bind(XMLUIMessages.No_content_model_for, (new Object[] {location})));
        } else {
          setErrorMessage(XMLUIMessages.No_content_model_found_UI_);
        }
      }
    }

    return list;
  }

  protected CMElementDeclaration getCMElementDeclaration(Node node) {
    CMElementDeclaration result = null;
    if (node.getNodeType() == Node.ELEMENT_NODE) {
      ModelQuery modelQuery = ModelQueryUtil.getModelQuery(node.getOwnerDocument());
      if (modelQuery != null) {
        result = modelQuery.getCMElementDeclaration((Element) node);
      }
    }
    return result;
  }

  /**
   * Returns the characters which when entered by the user should automatically trigger the
   * presentation of possible completions. the auto activation characters for completion proposal or
   * <code>null</code> if no auto activation is desired
   */
  public char[] getCompletionProposalAutoActivationCharacters() {
    return completionProposalAutoActivationCharacters;
  }

  protected ITextRegion getCompletionRegion(int offset, IStructuredDocumentRegion sdRegion) {
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
      if (offset > sdRegion.getStartOffset(region) + region.getTextLength()) {
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
  protected ITextRegion getCompletionRegion(int documentPosition, Node domnode) {
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

  /**
   * Provided by default. Subclasses may override with their own implementations.
   * 
   * @see AbstractContentAssistProcessor#getContentGenerator()
   */
  public XMLContentModelGenerator getContentGenerator() {
    if (fGenerator == null) {
      fGenerator = new XMLContentModelGenerator();
    }
    return fGenerator;
  }

  /**
   * Returns the characters which when entered by the user should automatically trigger the
   * presentation of context information. the auto activation characters for presenting context
   * information or <code>null</code> if no auto activation is desired
   */
  public char[] getContextInformationAutoActivationCharacters() {
    return contextInformationAutoActivationCharacters;
  }

  /**
   * Returns a validator used to determine when displayed context information should be dismissed.
   * May only return <code>null</code> if the processor is incapable of computing context
   * information. a context information validator, or <code>null</code> if the processor is
   * incapable of computing context information
   */
  public IContextInformationValidator getContextInformationValidator() {
    if (fContextInformationPresenter == null) {
      fContextInformationPresenter = new AttributeContextInformationPresenter();
    }
    return fContextInformationPresenter;
  }

  protected int getElementPosition(Node child) {
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

  private int getElementPositionForModelQuery(Node child) {
    return getElementPosition(child);
    // return -1;
  }

  /**
   * Return the reason why computeProposals was not able to find any completions. an error message
   * or null if no error occurred
   */
  public String getErrorMessage() {
    return fErrorMessage;
  }

  /**
   * @param iResource
   */
  // public void initialize(IResource iResource) {
  // this.resource = iResource;
  // }
  /**
   * Gets the infoProvider. fInfoProvider and if fInfoProvider was <code>null</code> create a new
   * instance
   */
  public MarkupTagInfoProvider getInfoProvider() {
    if (fInfoProvider == null) {
      fInfoProvider = new MarkupTagInfoProvider();
    }
    return fInfoProvider;
  }

  protected String getMatchString(IStructuredDocumentRegion parent, ITextRegion aRegion, int offset) {
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

  protected ITextRegion getNameRegion(IStructuredDocumentRegion flatNode) {
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

  /**
   * Retrieves all of the possible valid values for this attribute declaration
   */
  protected List getPossibleDataTypeValues(Node node, CMAttributeDeclaration ad) {
    List list = null;
    if (node.getNodeType() == Node.ELEMENT_NODE) {
      Element element = (Element) node;
      String[] dataTypeValues = null;
      // The ModelQuery may not be available if the corresponding
      // adapter
      // is absent
      ModelQuery modelQuery = ModelQueryUtil.getModelQuery(element.getOwnerDocument());
      if (modelQuery != null) {
        dataTypeValues = modelQuery.getPossibleDataTypeValues(element, ad);
      } else {
        if (ad.getAttrType() != null) {
          dataTypeValues = ad.getAttrType().getEnumeratedValues();
        }
      }
      if (dataTypeValues != null) {
        list = new ArrayList(dataTypeValues.length);
        for (int i = 0; i < dataTypeValues.length; i++) {
          list.add(dataTypeValues[i]);
        }
      }
    }
    if (list == null) {
      list = new ArrayList(0);
    }
    return list;
  }

  protected String getRequiredName(Node parentOrOwner, CMNode cmnode) {
    if ((cmnode == null) || (parentOrOwner == null)) {
      if (Debug.displayWarnings) {
        new IllegalArgumentException("Null declaration!").printStackTrace(); //$NON-NLS-1$
      }
      return ""; //$NON-NLS-1$
    }
    return getContentGenerator().getRequiredName(parentOrOwner, cmnode);
  }

  protected String getRequiredText(Node parentOrOwner, CMAttributeDeclaration attrDecl) {
    if (attrDecl == null) {
      if (Debug.displayWarnings) {
        new IllegalArgumentException("Null attribute declaration!").printStackTrace(); //$NON-NLS-1$
      }
      return ""; //$NON-NLS-1$
    }
    StringBuffer buff = new StringBuffer();
    getContentGenerator().generateRequiredAttribute(parentOrOwner, attrDecl, buff);
    return buff.toString();
  }

  protected String getRequiredText(Node parentOrOwner, CMElementDeclaration elementDecl) {
    if (elementDecl == null) {
      if (Debug.displayWarnings) {
        new IllegalArgumentException("Null attribute declaration!").printStackTrace(); //$NON-NLS-1$
      }
      return ""; //$NON-NLS-1$
    }
    StringBuffer buff = new StringBuffer();
    getContentGenerator().generateTag(parentOrOwner, elementDecl, buff);
    return buff.toString();
  }

  /**
   * StructuredTextViewer must be set before using this.
   */
  public IStructuredDocumentRegion getStructuredDocumentRegion(int pos) {
    // (pa) ITextRegion refactor defect 245190
    // return
    // (IStructuredDocumentRegion)ContentAssistUtils.getNodeAt((StructuredTextViewer)fTextViewer,
    // pos);
    return ContentAssistUtils.getStructuredDocumentRegion(fTextViewer, pos);
  }

  private String getTagName(IStructuredDocumentRegion sdRegion) {
    ITextRegionList regions = sdRegion.getRegions();
    ITextRegion region = null;
    String name = ""; //$NON-NLS-1$
    for (int i = 0; i < regions.size(); i++) {
      region = regions.get(i);
      if (region.getType() == DOMRegionContext.XML_TAG_NAME) {
        name = sdRegion.getText(region);
        break;
      }
    }
    return name;
  }

  // returns a list of CMNodes that are available within this parent context
  // Given the grammar shown below and a snippet of XML code (where the '|'
  // indicated the cursor position)
  // the list would return all of the element declarations that are
  // potential child elements of Foo.
  //
  // grammar : Foo -> (A, B, C)
  // snippet : <Foo><A>|
  // result : {A, B, C}
  // 
  // TODO cs... do we need to pass in the 'kindOfAction'? Seems to me we
  // could assume it's always an insert.
  protected List getAvailableChildElementDeclarations(Element parent, int childPosition,
      int kindOfAction) {
    List modelQueryActions = getAvailableChildrenAtIndex(parent, childPosition,
        ModelQuery.VALIDITY_NONE);
    Iterator iterator = modelQueryActions.iterator();
    List cmnodes = new Vector();
    while (iterator.hasNext()) {
      ModelQueryAction action = (ModelQueryAction) iterator.next();
      if ((childPosition < 0)
          || (((action.getStartIndex() <= childPosition) && (childPosition <= action.getEndIndex())) && (action.getKind() == kindOfAction))) {
        CMNode actionCMNode = action.getCMNode();
        if ((actionCMNode != null) && !cmnodes.contains(actionCMNode)) {
          cmnodes.add(actionCMNode);
        }
      }
    }
    return cmnodes;
  }

  // returns a list of CMNodes that can be validly inserted at this
  // childPosition
  // Given the grammar shown below and a snippet of XML code (where the '|'
  // indicated the cursor position)
  // the list would return only the element declarations can be inserted
  // while maintaing validity of the content.
  //
  // grammar : Foo -> (A, B, C)
  // snippet : <Foo><A>|
  // result : {B}
  //    
  protected List getValidChildElementDeclarations(Element parent, int childPosition,
      int kindOfAction) {
    List modelQueryActions = getAvailableChildrenAtIndex(parent, childPosition,
        ModelQuery.VALIDITY_STRICT);
    Iterator iterator = modelQueryActions.iterator();
    List cmnodes = new Vector();
    while (iterator.hasNext()) {
      ModelQueryAction action = (ModelQueryAction) iterator.next();
      if ((childPosition < 0)
          || (((action.getStartIndex() <= childPosition) && (childPosition <= action.getEndIndex())) && (action.getKind() == kindOfAction))) {
        CMNode actionCMNode = action.getCMNode();
        if ((actionCMNode != null) && !cmnodes.contains(actionCMNode)) {
          cmnodes.add(actionCMNode);
        }
      }
    }
    return cmnodes;
  }

  /**
   * Similar to the call in HTMLContentAssistProcessor. Pass in a node, it tells you if the document
   * is XML type.
   * 
   * @param node
   */
  protected boolean getXML(Node node) {
    if (node == null) {
      return false;
    }

    Document doc = null;
    doc = (node.getNodeType() != Node.DOCUMENT_NODE) ? node.getOwnerDocument() : ((Document) node);

    return (doc instanceof IDOMDocument) && ((IDOMDocument) doc).isXMLType();
  }

  // Initialize local settings
  protected void init() {
    // implement in subclasses
  }

  protected boolean isCloseRegion(ITextRegion region) {
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

  /*
   * This is to determine if a tag is a special meta-info comment tag that shows up as an ELEMENT
   */
  private boolean isCommentNode(IDOMNode node) {
    return ((node != null) && (node instanceof IDOMElement) && ((IDOMElement) node).isCommentTag());
  }

  /**
   * Checks if cursor position is after doctype tag...
   * 
   * @param car
   */
  protected boolean isCursorAfterDoctype(ContentAssistRequest car) {
    Node aNode = car.getNode();
    Document parent = aNode.getOwnerDocument();
    int xmldoctypeNodePosition = -1;
    boolean isAfterDoctype = true;

    if (parent == null) {
      return true; // blank document case
    }

    for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof IDOMNode) {
        if (child.getNodeType() == Node.DOCUMENT_TYPE_NODE) {
          xmldoctypeNodePosition = ((IDOMNode) child).getEndOffset();
          isAfterDoctype = (car.getReplacementBeginPosition() >= xmldoctypeNodePosition);
          break;
        }
      }
    }
    return isAfterDoctype;
  }

  /**
   * This method can check if the cursor is after the XMLPI
   * 
   * @param car
   */
  protected boolean isCursorAfterXMLPI(ContentAssistRequest car) {
    Node aNode = car.getNode();
    boolean xmlpiFound = false;
    Document parent = aNode.getOwnerDocument();
    int xmlpiNodePosition = -1;
    boolean isAfterXMLPI = false;

    if (parent == null) {
      return true; // blank document case
    }

    for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      boolean xmlpi = ((child.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) && child.getNodeName().equals(
          "xml")); //$NON-NLS-1$
      xmlpiFound = xmlpiFound || xmlpi;
      if (xmlpiFound) {
        if (child instanceof IDOMNode) {
          xmlpiNodePosition = ((IDOMNode) child).getEndOffset();
          isAfterXMLPI = (car.getReplacementBeginPosition() >= xmlpiNodePosition);
        }
        break;
      }
    }
    return isAfterXMLPI;
  }

  protected boolean isNameRegion(ITextRegion region) {
    String type = region.getType();
    return ((type == DOMRegionContext.XML_TAG_NAME)
        || (type == DOMJSPRegionContextsPrivateCopy.JSP_DIRECTIVE_NAME)
        || (type == DOMRegionContext.XML_ELEMENT_DECL_NAME)
        || (type == DOMRegionContext.XML_DOCTYPE_NAME)
        || (type == DOMRegionContext.XML_ATTLIST_DECL_NAME)
        || (type == DOMJSPRegionContextsPrivateCopy.JSP_ROOT_TAG_NAME) || type.equals(DOMJSPRegionContextsPrivateCopy.JSP_DIRECTIVE_NAME));
  }

  protected boolean isQuote(String string) {
    String trimmed = string.trim();
    if (trimmed.length() > 0) {
      return (trimmed.charAt(0) == '\'') || (trimmed.charAt(0) == '"');
    }
    return false;
  }

  private boolean isSelfClosed(IStructuredDocumentRegion startTag) {
    ITextRegionList regions = startTag.getRegions();
    return regions.get(regions.size() - 1).getType() == DOMRegionContext.XML_EMPTY_TAG_CLOSE;
  }

  private boolean isStartTag(IStructuredDocumentRegion sdRegion) {
    boolean result = false;
    if (sdRegion.getRegions().size() > 0) {
      ITextRegion r = sdRegion.getRegions().get(0);
      result = (r.getType() == DOMRegionContext.XML_TAG_OPEN) && sdRegion.isEnded();
    }
    return result;
  }

  protected Properties mapToProperties(CMNamedNodeMap map) {
    Properties p = new Properties();
    for (int i = 0; i < map.getLength(); i++) {
      CMEntityDeclaration decl = (CMEntityDeclaration) map.item(i);
      p.put(decl.getName(), decl.getValue());
    }
    return p;
  }

  /**
   * Gets the corresponding XMLNode, and checks if it's closed.
   * 
   * @param startTag
   */
  private boolean needsEndTag(IStructuredDocumentRegion startTag) {
    boolean result = false;
    IStructuredModel sModel = StructuredModelManager.getModelManager().getExistingModelForRead(
        fTextViewer.getDocument());
    try {
      if (sModel != null) {
        IDOMNode xmlNode = (IDOMNode) sModel.getIndexedRegion(startTag.getStart());
        if (!isStartTag(startTag)) {
          result = false;
        } else if (isSelfClosed(startTag)) {
          result = false;
        } else if (!xmlNode.isContainer()) {
          result = false;
        } else {
          result = xmlNode.getEndStructuredDocumentRegion() == null;
        }
      }
    } finally {
      if (sModel != null) {
        sModel.releaseFromRead();
      }
    }
    return result;
  }

  protected ContentAssistRequest newContentAssistRequest(Node node, Node possibleParent,
      IStructuredDocumentRegion documentRegion, ITextRegion completionRegion, int begin,
      int length, String filter) {
    return new ContentAssistRequest(node, possibleParent, documentRegion, completionRegion, begin,
        length, filter);
  }

  public void release() {
    fGenerator = null;
  }

  /**
   * Set the reason why computeProposals was not able to find any completions.
   */
  public void setErrorMessage(String errorMessage) {
    fErrorMessage = errorMessage;
  }

  /**
   * Set the reason why computeProposals was not able to find any completions.
   */
  protected void setErrorMessage(String errorMessage, String append) {
    setErrorMessage(errorMessage + append);
  }

  /**
   * Set the reason why computeProposals was not able to find any completions.
   */
  protected void setErrorMessage(String errorMessage, String prepend, String append) {
    setErrorMessage(prepend + errorMessage + append);
  }

  protected boolean stringsEqual(String a, String b) {
    // (pa) 221190 matching independent of case to be consistant with Java
    // editor CA
    return a.equalsIgnoreCase(b);
  }
}
