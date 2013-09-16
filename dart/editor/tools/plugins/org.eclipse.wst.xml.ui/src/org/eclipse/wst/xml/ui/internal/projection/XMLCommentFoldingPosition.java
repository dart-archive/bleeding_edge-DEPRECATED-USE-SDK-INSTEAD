/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.projection;

import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.ui.internal.projection.AbstractStructuredCommentFoldingPosition;

/**
 *
 */
public class XMLCommentFoldingPosition extends AbstractStructuredCommentFoldingPosition {

  /**
   * The region covering an XML comment
   */
  private IStructuredDocumentRegion fRegion;

  /**
   * Create a folding position to cover a XML comment region
   * 
   * @param region
   */
  public XMLCommentFoldingPosition(IStructuredDocumentRegion region) {
    //can't use region.getLength here because doesn't work in DTD docs for some reason
    super(region.getStartOffset(), region.getEndOffset() - region.getStartOffset());
    this.fRegion = region;
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.projection.AbstractStructuredCommentFoldingPosition#getEndOffset()
   */
  protected int getEndOffset() {
    return fRegion.getEndOffset();
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.projection.AbstractStructuredCommentFoldingPosition#getStartOffset()
   */
  protected int getStartOffset() {
    return fRegion.getStartOffset();
  }

}
