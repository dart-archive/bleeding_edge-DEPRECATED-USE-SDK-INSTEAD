/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.projection;

import org.eclipse.jface.text.Position;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSStyleRule;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.ui.internal.projection.AbstractStructuredFoldingStrategy;

import java.util.Iterator;
import java.util.List;

/**
 * A folding strategy for CSS structured documents. See AbstractStructuredFoldingStrategy for more
 * details.
 */
public class CSSFoldingStrategy extends AbstractStructuredFoldingStrategy {

  /**
   * Create an instance of the folding strategy. Be sure to set the viewer and document after
   * creation.
   */
  public CSSFoldingStrategy() {
    super();
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.projection.AbstractFoldingStrategy#calcNewFoldPosition(org.eclipse.wst.sse.core.internal.provisional.IndexedRegion)
   */
  protected Position calcNewFoldPosition(IndexedRegion indexedRegion) {
    Position newPos = null;

    //only want to fold regions with a valid range
    if (indexedRegion.getStartOffset() >= 0 && indexedRegion.getLength() >= 0) {
      newPos = new CSSRuleFoldingPosition(indexedRegion);
    }
    return newPos;
  }

  /**
   * The same as the AbstractFoldingStrategy implementation except for the new position is
   * calculated on the given IndexRegion and not the annotations stored IndexedRegion because of the
   * way CSS regions are replaced when they are updated. (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.ui.internal.projection.AbstractStructuredFoldingStrategy#updateAnnotations(java.util.Iterator,
   *      org.eclipse.wst.sse.core.internal.provisional.IndexedRegion, java.util.List,
   *      java.util.List)
   */
  protected void updateAnnotations(Iterator existingAnnotationsIter, IndexedRegion dirtyRegion,
      List modifications, List deletions) {

    while (existingAnnotationsIter.hasNext()) {
      Object obj = existingAnnotationsIter.next();
      if (obj instanceof FoldingAnnotation) {
        FoldingAnnotation annotation = (FoldingAnnotation) obj;

        //NOTE can not use the annotations stored region because the CSSStructuredDocument
        //	replaces the old region with a new one when it is modified
        Position newPos = calcNewFoldPosition(annotation.getRegion());
        if (newPos != null) {
          Position oldPos = fProjectionAnnotationModel.getPosition(annotation);
          if (!newPos.equals(oldPos)) {
            oldPos.setOffset(newPos.offset);
            oldPos.setLength(newPos.length);
            modifications.add(annotation);
          }
        } else {
          deletions.add(annotation);
        }
      }
    }
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.projection.AbstractFoldingStrategy#indexedRegionValidType(org.eclipse.wst.sse.core.internal.provisional.IndexedRegion)
   */
  protected boolean indexedRegionValidType(IndexedRegion indexedRegion) {
    return (indexedRegion instanceof ICSSStyleRule);
  }
}
