/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.projection;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.projection.IProjectionPosition;

/**
 * Represents a single folding position in an <code>IStructuredDocument</code>
 */
public abstract class AbstractStructuredFoldingPosition extends Position implements
    IProjectionPosition {

  /**
   * Default constructor
   * 
   * @param offset the offset of the folding position
   * @param length the length of the folidng position
   */
  public AbstractStructuredFoldingPosition(int offset, int length) {
    super(offset, length);
  }

  /**
   * @see org.eclipse.jface.text.source.projection.IProjectionPosition#computeCaptionOffset(org.eclipse.jface.text.IDocument)
   */
  public int computeCaptionOffset(IDocument document) throws BadLocationException {

    return 0;
  }

  /**
   * @see org.eclipse.jface.text.source.projection.IProjectionPosition#computeProjectionRegions(org.eclipse.jface.text.IDocument)
   */
  public IRegion[] computeProjectionRegions(IDocument document) throws BadLocationException {

    int startLineNum = document.getLineOfOffset(getStartOffset()) + 1;
    IRegion startLine = document.getLineInformation(startLineNum);
    int endLineNum = document.getLineOfOffset(getEndOffset()) + 1;
    IRegion endLine = document.getLineInformation(endLineNum);

    int foldOffset;
    int foldEndOffset;

    synchronized (this) {
      foldOffset = startLine.getOffset();
      if (foldOffset < offset) {
        offset = foldOffset;
      }

      foldEndOffset = endLine.getOffset();

      if ((foldEndOffset - offset) > length) {
        length = foldEndOffset - offset;
      }
    }

    return new IRegion[] {new Region(foldOffset, foldEndOffset - foldOffset)};
  }

  /**
   * @return the start offset of the folding position
   */
  protected abstract int getStartOffset();

  /**
   * @return the end offset of the folding position
   */
  protected abstract int getEndOffset();
}
