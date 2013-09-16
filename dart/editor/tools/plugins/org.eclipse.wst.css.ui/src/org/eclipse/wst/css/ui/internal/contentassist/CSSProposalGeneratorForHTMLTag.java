/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.contentassist;

import org.eclipse.wst.css.core.internal.CSSCorePlugin;
import org.eclipse.wst.css.core.internal.preferences.CSSCorePreferenceNames;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSModel;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSStyleRule;
import org.eclipse.wst.css.ui.internal.image.CSSImageType;
import org.eclipse.wst.html.core.internal.contentmodel.HTMLCMDocumentFactory;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDocument;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNamedNodeMap;
import org.eclipse.wst.xml.core.internal.document.DocumentTypeAdapter;
import org.eclipse.wst.xml.core.internal.provisional.contentmodel.CMDocType;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

class CSSProposalGeneratorForHTMLTag extends CSSProposalGenerator {

  /**
   * CSSStyleRuleProposalGenerator constructor comment.
   */
  CSSProposalGeneratorForHTMLTag(CSSContentAssistContext context) {
    super(context);

    if (fHTMLTags == null) {
      fHTMLTags = setupHTMLTags();
    }
  }

  /**
   * getCandidates method comment.
   */
  protected Iterator getCandidates() {
    List candidates = new ArrayList();

    if (checkLeadingColon()) {
      return candidates.iterator();
    }

    boolean bLowerCase = CSSCorePlugin.getDefault().getPluginPreferences().getInt(
        CSSCorePreferenceNames.CASE_SELECTOR) == CSSCorePreferenceNames.LOWER;
    // XHTML requires lower case
    if (fContext.getModel().getStyleSheetType() == ICSSModel.EMBEDDED) {
      Node domNode = fContext.getModel().getOwnerDOMNode();
      if (domNode != null && !(domNode instanceof Document)) {
        domNode = domNode.getOwnerDocument();
        if (domNode instanceof IDOMDocument) {
          DocumentTypeAdapter adapter = (DocumentTypeAdapter) ((IDOMDocument) domNode).getAdapterFor(DocumentTypeAdapter.class);
          if (adapter != null)
            bLowerCase = (adapter.getTagNameCase() == DocumentTypeAdapter.LOWER_CASE);
        }
      }
    }

    int length = fHTMLTags.length;
    for (int i = 0; i < length; i++) {
      String tagText = fHTMLTags[i];
      if (bLowerCase) {
        tagText = tagText.toLowerCase();
      }
      if (!isMatch(tagText)) {
        continue;
      }

      int cursorPos = 0;
      StringBuffer buf = new StringBuffer();
      buf.append(tagText);
      cursorPos += tagText.length();
      boolean inRule = (fContext.getTargetNode() instanceof ICSSStyleRule);
      if (!inRule || fContext.getTextToReplace().length() == 0) {
        buf.append(" ");//$NON-NLS-1$
        cursorPos += 1;
      }
      if (!inRule) {
        StringAndOffset sao = generateBraces();
        buf.append(sao.fString);
        cursorPos += sao.fOffset;
      }

      CSSCACandidate item = new CSSCACandidate();
      item.setReplacementString(buf.toString());
      item.setCursorPosition(cursorPos);
      item.setDisplayString(tagText);
      item.setImageType(CSSImageType.SELECTOR_TAG);
      candidates.add(item);
    }

    return candidates.iterator();
  }

  /**
	 *  
	 */
  private static String[] setupHTMLTags() {
    CMDocument cmdoc = HTMLCMDocumentFactory.getCMDocument(CMDocType.HTML_DOC_TYPE);
    CMNamedNodeMap elements = cmdoc.getElements();
    Vector names = new Vector();
    int nElements = elements.getLength();
    for (int i = 0; i < nElements; i++) {
      CMElementDeclaration edec = (CMElementDeclaration) elements.item(i);
      if (isAttrDefined(edec, HTML40Namespace.ATTR_NAME_STYLE)) {
        names.add(edec.getElementName());
      }
    }
    Collections.sort(names);
    String[] tags = new String[names.size()];
    Iterator iNames = names.iterator();
    for (int i = 0; iNames.hasNext(); i++) {
      tags[i] = (String) iNames.next();
    }
    return tags;
  }

  /**
	 *  
	 */
  private static boolean isAttrDefined(CMElementDeclaration edec, String attrName) {
    if (edec == null) {
      return false;
    }
    CMNamedNodeMap attrs = edec.getAttributes();
    if (attrs == null) {
      return false;
    }
    for (int i = 0; i < attrs.getLength(); i++) {
      CMAttributeDeclaration attr = (CMAttributeDeclaration) attrs.item(i);
      if (attr == null) {
        continue;
      }
      if (attr.getAttrName().equalsIgnoreCase(attrName)) {
        return true;
      }
    }
    return false;
  }

  private static String[] fHTMLTags = null;
}
