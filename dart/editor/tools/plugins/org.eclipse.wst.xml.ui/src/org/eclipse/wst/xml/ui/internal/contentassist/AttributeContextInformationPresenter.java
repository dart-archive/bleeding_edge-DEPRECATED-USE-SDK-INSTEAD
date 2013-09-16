/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.contentassist;

import java.util.HashMap;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationPresenter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.sse.ui.internal.contentassist.ContextInformationValidator;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;

/**
 * Responsible for the presentation of the context info popup. This includes text style, and when
 * the window should close.
 * 
 * @author pavery
 */
public class AttributeContextInformationPresenter extends ContextInformationValidator implements
    IContextInformationPresenter {

  private IContextInformation fInfo = null;
  private ContextInfoModelUtil fModelUtil = null;

  public AttributeContextInformationPresenter() {
    super();
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.contentassist.ContextInformationValidator#install(org.eclipse.jface.text.contentassist.IContextInformation,
   *      org.eclipse.jface.text.ITextViewer, int)
   */
  public void install(IContextInformation info, ITextViewer viewer, int documentPosition) {
    super.install(info, viewer, documentPosition);
    this.fInfo = info;
    this.fModelUtil = new ContextInfoModelUtil((IStructuredDocument) viewer.getDocument());
  }

  /**
   * @see org.eclipse.jface.text.contentassist.IContextInformationPresenter#updatePresentation(int,
   *      org.eclipse.jface.text.TextPresentation)
   */
  public boolean updatePresentation(int documentPosition, TextPresentation presentation) {
    presentation.clear();

    if (!(fInfo instanceof AttributeContextInformation)) {
      return false;
    }

    // iterate existing attributes from current node
    IDOMNode xmlNode = fModelUtil.getXMLNode(documentPosition);
    if (xmlNode == null)
      return false;

    IStructuredDocumentRegion sdRegion = xmlNode.getFirstStructuredDocumentRegion();
    ITextRegionList regions = sdRegion.getRegions();
    ITextRegion r = null;
    String attrName = ""; //$NON-NLS-1$
    Object temp = null;
    Position p = null;
    HashMap map = ((AttributeContextInformation) fInfo).getAttr2RangeMap();

    // so we can add ranges in order
    StyleRange[] sorted = new StyleRange[fInfo.getInformationDisplayString().length()];
    for (int i = 0; i < regions.size(); i++) {
      r = regions.get(i);
      if (r.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_NAME) {
        attrName = sdRegion.getText(r);
        temp = map.get(attrName);
        if (temp != null) {
          p = (Position) temp;
          sorted[p.offset] = new StyleRange(p.offset, p.length, null, null, SWT.BOLD);
        }
      }
    }
    // style ranges need to be added in order
    StyleRange sr = null;
    for (int i = 0; i < sorted.length; i++) {
      sr = sorted[i];
      if (sr != null) {
        presentation.addStyleRange(sr);
      }
    }
    return true;
  }
}
