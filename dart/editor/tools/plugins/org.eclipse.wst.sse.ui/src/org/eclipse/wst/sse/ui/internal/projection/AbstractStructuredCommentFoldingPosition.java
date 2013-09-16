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
 * Represents a folding position for an XML comment
 */
public abstract class AbstractStructuredCommentFoldingPosition extends Position implements
    IProjectionPosition {

  /**
   * Default constructor
   * 
   * @param offset the offset of the folding position
   * @param length the length of the folidng position
   */
  public AbstractStructuredCommentFoldingPosition(int offset, int length) {
    super(offset, length);
  }

  /**
   * @see org.eclipse.jface.text.source.projection.IProjectionPosition#computeCaptionOffset(org.eclipse.jface.text.IDocument)
   */
  public int computeCaptionOffset(IDocument document) throws BadLocationException {
    return findFirstContent(document.get(offset, length));
  }

  /**
   * @see org.eclipse.jface.text.source.projection.IProjectionPosition#computeProjectionRegions(org.eclipse.jface.text.IDocument)
   */
  public IRegion[] computeProjectionRegions(IDocument document) throws BadLocationException {
    //get the content of the comment
    String content = document.get(offset, length);
    int contentStart = findFirstContent(content);

    //find the start line of the comment
    //find the end line of the comment
    //find the first line of text in the comment
    int startLineNum = document.getLineOfOffset(getStartOffset());
    IRegion startLine = document.getLineInformation(startLineNum);
    int endLineNum = document.getLineOfOffset(getEndOffset()) + 1;
    IRegion endLine = document.getLineInformation(endLineNum);
    int captionLineNum = document.getLineOfOffset(getStartOffset() + contentStart);

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

    //fold before the first line of text in the comment
    IRegion preRegion = null;
    IRegion postRegion = null;
    if (startLineNum < captionLineNum) {
      IRegion captionLine = document.getLineInformation(captionLineNum);
      preRegion = new Region(foldOffset, captionLine.getOffset() - foldOffset);
    }

    //fold after the first line of text in the comment
    if (captionLineNum < endLineNum) {
      int postRegionOffset = document.getLineOffset(captionLineNum + 1);
      postRegion = new Region(postRegionOffset, foldEndOffset - postRegionOffset);
    }

    IRegion[] regions = null;
    if (preRegion != null && postRegion != null) {
      regions = new IRegion[] {preRegion, postRegion};
    } else if (preRegion != null) {
      regions = new IRegion[] {preRegion};
    } else if (postRegion != null) {
      regions = new IRegion[] {postRegion};
    }

    return regions;
  }

  /**
   * Finds the offset of the first identifier part within <code>content</code>. Returns 0 if none is
   * found.
   * 
   * @param content the content to search
   * @param prefixEnd the end of the prefix
   * @return the first index of a unicode identifier part, or zero if none can be found
   */
  private int findFirstContent(final CharSequence content) {
    int lenght = content.length();
    for (int i = 0; i < lenght; i++) {
      if (Character.isUnicodeIdentifierPart(content.charAt(i)))
        return i;
    }
    return 0;
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
