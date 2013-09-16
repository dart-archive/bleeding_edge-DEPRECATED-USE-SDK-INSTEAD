/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.projection;

import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.ui.internal.projection.AbstractStructuredFoldingPosition;
import org.w3c.dom.css.CSSStyleRule;

/**
 * An {@link AbstractStructuredFoldingPosition} used to cover CSS regions
 */
public class CSSRuleFoldingPosition extends AbstractStructuredFoldingPosition {

  /**
   * the region that will be folded
   */
  private IndexedRegion fRegion;

  /**
   * Creates a folding position that covers {@link IndexedRegion}s in a CSS document
   * 
   * @param region the {@link IndexedRegion} that this folding position covers
   */
  public CSSRuleFoldingPosition(IndexedRegion region) {
    super(region.getStartOffset(), region.getLength());
    this.fRegion = region;
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.projection.AbstractStructuredFoldingPosition#getStartOffset()
   */
  protected int getStartOffset() {
    int startOffset = fRegion.getStartOffset();

    //so that multi-line CSS selector text does not get folded
    if (this.fRegion instanceof CSSStyleRule) {
      CSSStyleRule rule = (CSSStyleRule) this.fRegion;
      startOffset += rule.getSelectorText().length();
    }

    return startOffset;
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.projection.AbstractStructuredFoldingPosition#getEndOffset()
   */
  protected int getEndOffset() {
    return fRegion.getEndOffset();
  }

}
