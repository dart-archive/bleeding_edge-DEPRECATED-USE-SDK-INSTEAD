/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.projection;

import org.eclipse.jface.text.Position;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.ui.internal.projection.AbstractStructuredFoldingStrategy;
import org.eclipse.wst.xml.core.internal.document.CommentImpl;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMText;

/**
 * A folding strategy for XML type structured documents. See AbstractStructuredFoldingStrategy for
 * more details.
 */
public class XMLFoldingStrategy extends AbstractStructuredFoldingStrategy {

  /**
   * Create an instance of the folding strategy. Be sure to set the viewer and document after
   * creation.
   */
  public XMLFoldingStrategy() {
    super();
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.projection.AbstractFoldingStrategy#calcNewFoldPosition(org.eclipse.wst.sse.core.internal.provisional.IndexedRegion)
   */
  protected Position calcNewFoldPosition(IndexedRegion indexedRegion) {
    Position retPos = null;

    //only want to fold regions of the valid type and with a valid range
    if (indexedRegion.getStartOffset() >= 0 && indexedRegion.getLength() >= 0) {
      IDOMNode node = (IDOMNode) indexedRegion;
      IStructuredDocumentRegion startRegion = node.getStartStructuredDocumentRegion();
      IStructuredDocumentRegion endRegion = node.getEndStructuredDocumentRegion();

      //if the node has an endRegion (end tag) then folding region is
      //	between the start and end tag
      //else if the region is a comment
      //else if the region is only an open tag or an open/close tag then don't fold it
      if (startRegion != null && endRegion != null) {
        if (endRegion.getEndOffset() >= startRegion.getStartOffset())
          retPos = new XMLElementFoldingPosition(startRegion, endRegion);
      } else if (startRegion != null && indexedRegion instanceof CommentImpl) {
        retPos = new XMLCommentFoldingPosition(startRegion);
      }
    }

    return retPos;
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.projection.AbstractFoldingStrategy#indexedRegionValidType(org.eclipse.wst.sse.core.internal.provisional.IndexedRegion)
   */
  protected boolean indexedRegionValidType(IndexedRegion indexedRegion) {
    return (indexedRegion instanceof IDOMNode) && !(indexedRegion instanceof IDOMText);
  }
}
