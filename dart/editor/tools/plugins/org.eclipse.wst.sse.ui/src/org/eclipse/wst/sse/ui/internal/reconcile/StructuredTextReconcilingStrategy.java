/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.reconcile;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;

/**
 * Uses IStructuredModel nodes to check "overlaps()" when getting annotations to remove.
 */
public abstract class StructuredTextReconcilingStrategy extends
    AbstractStructuredTextReconcilingStrategy {

  public StructuredTextReconcilingStrategy(ISourceViewer sourceViewer) {
    super(sourceViewer);
  }

  /**
   * Checks if this position overlaps any of the StructuredDocument regions' correstponding
   * IndexedRegion.
   * 
   * @param pos
   * @param dr
   * @return true if the position overlaps any of the regions, otherwise false.
   */
  protected boolean overlaps(Position pos, IStructuredDocumentRegion[] sdRegions) {
    int start = -1;
    int end = -1;
    for (int i = 0; i < sdRegions.length; i++) {
      if (!sdRegions[i].isDeleted()) {
        IndexedRegion corresponding = getCorrespondingNode(sdRegions[i]);
        if (corresponding != null) {
          if (start == -1 || start > corresponding.getStartOffset())
            start = corresponding.getStartOffset();
          if (end == -1 || end < corresponding.getEndOffset())
            end = corresponding.getEndOffset();
        }
      }
    }
    return pos.overlapsWith(start, end - start);
  }

  /**
   * Returns the corresponding node for the StructuredDocumentRegion.
   * 
   * @param sdRegion
   * @return the corresponding node for sdRegion
   */
  protected IndexedRegion getCorrespondingNode(IStructuredDocumentRegion sdRegion) {
    IStructuredModel sModel = StructuredModelManager.getModelManager().getExistingModelForRead(
        getDocument());
    IndexedRegion indexedRegion = null;
    try {
      if (sModel != null)
        indexedRegion = sModel.getIndexedRegion(sdRegion.getStart());
    } finally {
      if (sModel != null)
        sModel.releaseFromRead();
    }
    return indexedRegion;
  }

}
