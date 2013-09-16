/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.contentassist;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionContainer;
import org.eclipse.wst.sse.core.internal.util.ScriptLanguageKeys;
import org.eclipse.wst.sse.ui.internal.contentassist.ContentAssistUtils;
import org.eclipse.wst.sse.ui.internal.contentassist.CustomCompletionProposal;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImageHelper;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * @author pavery
 */
public class XMLContentAssistUtilities extends ContentAssistUtils {

  /**
   * ISSUE: this is a bit of hidden JSP knowledge that was implemented this way for expedency.
   * Should be evolved in future to depend on "nestedContext".
   */
  private class DOMJSPRegionContextsPrivateCopy {
    private static final String JSP_CLOSE = "JSP_CLOSE"; //$NON-NLS-1$
    private static final String JSP_DECLARATION_OPEN = "JSP_DECLARATION_OPEN"; //$NON-NLS-1$
    private static final String JSP_SCRIPTLET_OPEN = "JSP_SCRIPTLET_OPEN"; //$NON-NLS-1$
    private static final String JSP_EXPRESSION_OPEN = "JSP_EXPRESSION_OPEN"; //$NON-NLS-1$

  }

  public static final String CONTENT = "Content"; //$NON-NLS-1$
  public static final String CONTENT_SCRIPT_TYPE = "Content-Script-Type"; //$NON-NLS-1$
  public static final String HEAD = "HEAD"; //$NON-NLS-1$
  public static final String HTML = "HTML"; //$NON-NLS-1$
  public static final String HTTP_EQUIV = "HTTP-EQUIV"; //$NON-NLS-1$
  public static final String META = "META"; //$NON-NLS-1$

  /**
   * A convenience method for getting the closing proposal given the contents (IndexedRegion) of a
   * tag that is started, but possibly not ended
   * 
   * @param viewer the text viewer
   * @param documentPosition the cursor position in the viewer
   * @param indexedNode the contents of the tag that is started but possibly not ended
   * @param parentTagName the tag on which you are checkin for an ending tag
   * @param imagePath content assist image relative path
   * @return ICompletionProposal
   */
  public static ICompletionProposal computeJSPEndTagProposal(ITextViewer viewer,
      int documentPosition, IndexedRegion indexedNode, String parentTagName, String imagePath) {
    ICompletionProposal p = null;

    // check if tag is closed
    boolean hasEndTag = true;
    boolean isJSPTag = false;
    IDOMNode xnode = null;
    String tagName = ""; //$NON-NLS-1$
    if (indexedNode instanceof IDOMNode) {
      xnode = ((IDOMNode) indexedNode);
      // it's ended already...
      if (xnode.getEndStructuredDocumentRegion() != null) {
        return null;
      }
      IDOMNode openNode = null;
      if (!xnode.getNodeName().equalsIgnoreCase(parentTagName)) {
        openNode = (IDOMNode) xnode.getParentNode();
      }
      if (openNode != null) {
        if (openNode instanceof IDOMElement) {
          isJSPTag = ((IDOMElement) openNode).isJSPTag();
        }
        tagName = openNode.getNodeName();
        hasEndTag = (openNode.getEndStructuredDocumentRegion() != null);
      }
    }

    // it's closed, don't add close tag proposal
    if (!hasEndTag && !isJSPTag) {

      // create appropriate close tag text
      String proposedText = "</" + tagName; //$NON-NLS-1$
      String viewerText = viewer.getTextWidget().getText();
      if ((viewerText.length() >= documentPosition) && (viewerText.length() >= 2)
          && (documentPosition >= 2)) {
        String last2chars = viewerText.substring(documentPosition - 2, documentPosition);
        if (last2chars.endsWith("</")) {
          proposedText = tagName;
        } else if (last2chars.endsWith("<")) {
          proposedText = "/" + tagName; //$NON-NLS-1$
        }
      }

      // create proposal
      p = new CustomCompletionProposal(
          proposedText + ">", //$NON-NLS-1$
          documentPosition, 0, proposedText.length() + 1,
          XMLEditorPluginImageHelper.getInstance().getImage(imagePath), NLS.bind(
              XMLUIMessages.End_with_, (new Object[] {proposedText})), null, null,
          XMLRelevanceConstants.R_END_TAG);
    } else if (!hasEndTag && isJSPTag) {

      // create appropriate close tag text
      String proposedText = "%"; //$NON-NLS-1$
      String viewerText = viewer.getTextWidget().getText();

      // TODO (pa) make it smarter to add "%>" or just ">" if % is
      // already there...
      if ((viewerText.length() >= documentPosition) && (viewerText.length() >= 2)) {
        String last2chars = viewerText.substring(documentPosition - 2, documentPosition);
        String lastchar = viewerText.substring(documentPosition - 1, documentPosition);
        if (lastchar.equals("%")) //$NON-NLS-1$
        {
          if (last2chars.endsWith("<%")) {
            proposedText = "%"; //$NON-NLS-1$
          } else {
            proposedText = ""; //$NON-NLS-1$
          }
        }
      }

      // create proposal
      p = new CustomCompletionProposal(
          proposedText + ">", //$NON-NLS-1$
          documentPosition, 0, proposedText.length() + 1,
          XMLEditorPluginImageHelper.getInstance().getImage(imagePath), NLS.bind(
              XMLUIMessages.End_with_, (new Object[] {proposedText})), null, null,
          XMLRelevanceConstants.R_END_TAG);
    }

    return p;
  }

  /**
   * A convenience method for getting the closing proposal given the contents (IndexedRegion) of a
   * tag that is started, but possibly not ended
   * 
   * @param viewer the text viewer
   * @param documentPosition the cursor position in the viewer
   * @param indexedNode the contents of the tag that is started but possibly not ended
   * @param parentTagName the tag on which you are checkin for an ending tag
   * @param imagePath content assist relative image path
   * @return ICompletionProposal
   */
  public static ICompletionProposal computeXMLEndTagProposal(ITextViewer viewer,
      int documentPosition, IndexedRegion indexedNode, String parentTagName, String imagePath) {
    ICompletionProposal p = null;

    // check if tag is closed
    boolean hasEndTag = true;
    IDOMNode xnode = null;
    String tagName = ""; //$NON-NLS-1$
    if (indexedNode instanceof IDOMNode) {
      xnode = ((IDOMNode) indexedNode);
      // it's ended already...
      if (xnode.getEndStructuredDocumentRegion() != null) {
        return null;
      }
      IDOMNode styleNode = null;
      if (!xnode.getNodeName().equalsIgnoreCase(parentTagName)) {
        styleNode = (IDOMNode) xnode.getParentNode();
      }
      if (styleNode != null) {
        tagName = styleNode.getNodeName();
        hasEndTag = (styleNode.getEndStructuredDocumentRegion() != null);
      }
    }

    // it's closed, don't add close tag proposal
    if (!hasEndTag) {

      // create appropriate close tag text
      String proposedText = "</" + tagName; //$NON-NLS-1$
      String viewerText = viewer.getTextWidget().getText();
      if ((viewerText.length() >= documentPosition) && (viewerText.length() >= 2)
          && (documentPosition >= 2)) {
        String last2chars = viewerText.substring(documentPosition - 2, documentPosition);
        if (last2chars.endsWith("</")) {
          proposedText = tagName;
        } else if (last2chars.endsWith("<")) {
          proposedText = "/" + tagName; //$NON-NLS-1$
        }
      }

      // create proposal
      p = new CustomCompletionProposal(
          proposedText + ">", //$NON-NLS-1$
          documentPosition, 0, proposedText.length() + 1,
          XMLEditorPluginImageHelper.getInstance().getImage(imagePath), NLS.bind(
              XMLUIMessages.End_with_, (new Object[] {proposedText})), null, null,
          XMLRelevanceConstants.R_END_TAG);
    }
    return p;
  }

  private static String getMetaScriptType(Document doc) {
    // Can not just do a Document.getElementsByTagName(String) as this
    // needs
    // to be relatively fast.
    List metas = new ArrayList();
    // check for META tags under the Document
    Node html = null;
    Node head = null;
    Node child = null;
    // ----------------------------------------------------------------------
    // (pa) 20021217
    // cmvc defect 235554
    // performance enhancement: using child.getNextSibling() rather than
    // nodeList(item) for O(n) vs. O(n*n)
    // ----------------------------------------------------------------------

    for (child = doc.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      if (child.getNodeName().equalsIgnoreCase(META)) {
        metas.add(child);
      } else if (child.getNodeName().equalsIgnoreCase(HTML)) {
        html = child;
      }
    }
    // NodeList children = doc.getChildNodes();
    // for(int i = 0; i < children.getLength(); i++) {
    // child = children.item(i);
    // if(child.getNodeType() != Node.ELEMENT_NODE)
    // continue;
    // if(child.getNodeName().equalsIgnoreCase(META))
    // metas.add(child);
    // else if(child.getNodeName().equalsIgnoreCase(HTML))
    // html = child;
    // }

    // check for META tags under HEAD
    if (html != null) {
      for (child = html.getFirstChild(); (child != null) && (head == null); child = child.getNextSibling()) {
        if (child.getNodeType() != Node.ELEMENT_NODE) {
          continue;
        }
        if (child.getNodeName().equalsIgnoreCase(HEAD)) {
          head = child;
        }
      }
      // children = html.getChildNodes();
      // for(int i = 0; i < children.getLength() && head == null; i++) {
      // child = children.item(i);
      // if(child.getNodeType() != Node.ELEMENT_NODE)
      // continue;
      // if(child.getNodeName().equalsIgnoreCase(HEAD))
      // head = child;
      // }
    }

    if (head != null) {
      for (head.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child.getNodeType() != Node.ELEMENT_NODE) {
          continue;
        }
        if (child.getNodeName().equalsIgnoreCase(META)) {
          metas.add(child);
        }
      }
      // children = head.getChildNodes();
      // for(int i = 0 ; i < children.getLength(); i++) {
      // child = children.item(i);
      // if(child.getNodeType() != Node.ELEMENT_NODE)
      // continue;
      // if(child.getNodeName().equalsIgnoreCase(META))
      // metas.add(child);
      // }
    }

    return getMetaScriptType(metas);
  }

  private static String getMetaScriptType(List metaNodeList) {
    Node meta = null;
    NamedNodeMap attributes = null;
    boolean httpEquiv = false;
    String contentScriptType = null;

    for (int i = metaNodeList.size() - 1; i >= 0; i--) {
      meta = (Node) metaNodeList.get(i);
      attributes = meta.getAttributes();
      httpEquiv = false;
      contentScriptType = null;
      for (int j = 0; j < attributes.getLength(); j++) {
        if (attributes.item(j).getNodeName().equalsIgnoreCase(HTTP_EQUIV)) {
          httpEquiv = attributes.item(j).getNodeValue().equalsIgnoreCase(CONTENT_SCRIPT_TYPE);
        } else if (attributes.item(j).getNodeName().equalsIgnoreCase(CONTENT)) {
          contentScriptType = attributes.item(j).getNodeValue();
        }
      }
      if (httpEquiv && (contentScriptType != null)) {
        return contentScriptType;
      }
    }
    return null;
  }

  /**
   * Returns the scripting language the scriptNode is in Currently returns javascript unless some
   * unknown type or language is specified. Then the unknown type/language is returned
   * 
   * @param scriptNode
   */
  public static String getScriptLanguage(Node scriptNode) {
    Node attr = null;

    boolean specified = false;
    // try to find a scripting adapter for 'type'
    if ((scriptNode == null) || (scriptNode.getAttributes() == null)) {
      return null;
    }

    attr = scriptNode.getAttributes().getNamedItem("type");//$NON-NLS-1$
    if (attr != null) {
      specified = true;
      String type = attr.getNodeValue();
      return lookupScriptType(type);
    }
    // now try to find a scripting adapter for 'language' (deprecated by
    // HTML specifications)
    attr = scriptNode.getAttributes().getNamedItem("language");//$NON-NLS-1$
    if (attr != null) {
      specified = true;
      String language = attr.getNodeValue();
      return lookupScriptLanguage(language);
    }
    // check if one is specified by a META tag at the root level or inside
    // of HEAD
    String type = null;
    if (!specified) {
      type = getMetaScriptType(scriptNode.getOwnerDocument());
    }
    if (type != null) {
      specified = true;
      return lookupScriptType(type);
    }
    // return default
    if (!specified) {
      return ScriptLanguageKeys.JAVASCRIPT;
    }
    return null;
  }

  /**
   * Tells you if the flatnode is the %> delimiter ISSUE: this is a bit of hidden JSP knowledge that
   * was implemented this way for expedency. Should be evolved in future to depend on
   * "nestedContext".
   */
  public static boolean isJSPCloseDelimiter(IStructuredDocumentRegion fn) {
    if (fn == null) {
      return false;
    }
    return isJSPCloseDelimiter(fn.getType());
  }

  /**
   * ISSUE: this is a bit of hidden JSP knowledge that was implemented this way for expedency.
   * Should be evolved in future to depend on "nestedContext".
   */
  public static boolean isJSPCloseDelimiter(String type) {
    if (type == null) {
      return false;
    }
    return (type.equals(DOMJSPRegionContextsPrivateCopy.JSP_CLOSE) || type.equals(DOMRegionContext.XML_TAG_CLOSE));
  }

  /**
   * Tells you if the flatnode is the JSP region <%%>, <%=%>, <%!%> ISSUE: this is a bit of hidden
   * JSP knowledge that was implemented this way for expedency. Should be evolved in future to
   * depend on "nestedContext".
   */
  public static boolean isJSPDelimiter(IStructuredDocumentRegion fn) {
    boolean isDelimiter = false;
    String type = fn.getType();
    if (type != null) {
      isDelimiter = isJSPDelimiter(type);
    }
    return isDelimiter;
  }

  /**
   * ISSUE: this is a bit of hidden JSP knowledge that was implemented this way for expedency.
   * Should be evolved in future to depend on "nestedContext".
   */
  public static boolean isJSPDelimiter(String type) {
    if (type == null) {
      return false;
    }
    return (isJSPOpenDelimiter(type) || isJSPCloseDelimiter(type));
  }

  /**
   * Tells you if the flatnode is <%, <%=, or <%! ISSUE: this is a bit of hidden JSP knowledge that
   * was implemented this way for expedency. Should be evolved in future to depend on
   * "nestedContext".
   */
  public static boolean isJSPOpenDelimiter(IStructuredDocumentRegion fn) {
    if (fn == null) {
      return false;
    }
    return isJSPOpenDelimiter(fn.getType());
  }

  /**
   * ISSUE: this is a bit of hidden JSP knowledge that was implemented this way for expedency.
   * Should be evolved in future to depend on "nestedContext".
   */
  public static boolean isJSPOpenDelimiter(String type) {
    if (type == null) {
      return false;
    }
    return (type.equals(DOMJSPRegionContextsPrivateCopy.JSP_SCRIPTLET_OPEN)
        || type.equals(DOMJSPRegionContextsPrivateCopy.JSP_DECLARATION_OPEN) || type.equals(DOMJSPRegionContextsPrivateCopy.JSP_EXPRESSION_OPEN));
  }

  /**
   * Tells you if the flatnode is the <jsp:scriptlet>, <jsp:expression>, or <jsp:declaration>tag
   * ISSUE: this is a bit of hidden JSP knowledge that was implemented this way for expedency.
   * Should be evolved in future to depend on "nestedContext".
   */
  public static boolean isXMLJSPDelimiter(IStructuredDocumentRegion fn) {
    boolean isDelimiter = false;
    if ((fn != null) && (fn instanceof ITextRegionContainer)) {
      Object[] regions = ((ITextRegionContainer) fn).getRegions().toArray();
      ITextRegion temp = null;
      String regionText = ""; //$NON-NLS-1$
      for (int i = 0; i < regions.length; i++) {
        temp = (ITextRegion) regions[i];
        if (temp.getType() == DOMRegionContext.XML_TAG_NAME) {
          regionText = fn.getText(temp);
          if (regionText.equalsIgnoreCase("jsp:scriptlet")
              || regionText.equalsIgnoreCase("jsp:expression")
              || regionText.equalsIgnoreCase("jsp:declaration")) {
            isDelimiter = true;
          }
        }
      }
    }
    return isDelimiter;
  }

  /**
   * Returns "javascript" if language attribute is some form of javascript, "java" if language
   * attribute is some form of java. Otherwise, just returns type.
   * 
   * @param language
   */
  public static String lookupScriptLanguage(String language) {
    for (int i = 0; i < ScriptLanguageKeys.JAVASCRIPT_LANGUAGE_KEYS.length; i++) {
      if (ScriptLanguageKeys.JAVASCRIPT_LANGUAGE_KEYS[i].equalsIgnoreCase(language)) {
        return ScriptLanguageKeys.JAVASCRIPT;
      }
    }
    for (int i = 0; i < ScriptLanguageKeys.JAVA_LANGUAGE_KEYS.length; i++) {
      if (ScriptLanguageKeys.JAVA_LANGUAGE_KEYS[i].equalsIgnoreCase(language)) {
        return ScriptLanguageKeys.JAVA;
      }
    }
    return language;
  }

  /**
   * Returns "javascript" if type (used in <script type="xxx"> is actually javascript type.
   * Otherwise, just returns type
   * 
   * @param type
   */
  public static String lookupScriptType(String type) {
    for (int i = 0; i < ScriptLanguageKeys.JAVASCRIPT_MIME_TYPE_KEYS.length; i++) {
      if (ScriptLanguageKeys.JAVASCRIPT_MIME_TYPE_KEYS[i].equalsIgnoreCase(type)) {
        return ScriptLanguageKeys.JAVASCRIPT;
      }
    }
    return type;
  }
}
