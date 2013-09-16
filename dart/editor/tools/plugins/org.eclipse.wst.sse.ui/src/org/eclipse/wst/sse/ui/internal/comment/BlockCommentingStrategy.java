/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.comment;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.ui.internal.Logger;

import java.util.List;

/**
 * <p>
 * Represents a Block Comment commenting strategy
 * </p>
 */
public class BlockCommentingStrategy extends CommentingStrategy {
  /** the prefix of the block comment associated with this strategy */
  private String fPrefix;

  /** the suffix of the block comment associated with this strategy */
  private String fSuffix;

  /**
   * @param prefix the prefix of the block comment associated with this strategy
   * @param suffix the suffix of the block comment associated with this strategy
   */
  public BlockCommentingStrategy(String prefix, String suffix) {
    super();
    this.fPrefix = prefix;
    this.fSuffix = suffix;
  }

  /**
   * <p>
   * When applying a block comment it also removes any block comments associated with this strategy
   * that would now be enclosed by the new block comment
   * </p>
   * 
   * @see org.eclipse.wst.sse.ui.internal.comment.CommentingStrategy#apply(org.eclipse.wst.sse.core.internal.provisional.IStructuredModel,
   *      int, int)
   */
  public void apply(IStructuredDocument document, int offset, int length)
      throws BadLocationException {
    int commentPrefixOffset = offset;
    int commentSuffixOffset = commentPrefixOffset + length;

    try {
      document.replace(commentSuffixOffset, 0, " " + this.fSuffix); //$NON-NLS-1$
      this.remove(document, commentPrefixOffset + this.fPrefix.length(), length, false);
      document.replace(commentPrefixOffset, 0, this.fPrefix + " "); //$NON-NLS-1$
    } catch (BadLocationException e) {
      Logger.log(Logger.WARNING_DEBUG, e.getMessage(), e);
    }
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.comment.CommentingStrategy#remove(org.eclipse.jface.text.IDocument,
   *      int, int)
   */
  public void remove(IStructuredDocument document, int offset, int length, boolean removeEnclosing)
      throws BadLocationException {
    IRegion region = new Region(offset, length);
    ITypedRegion[] typedRegions = document.computePartitioning(region.getOffset(),
        region.getLength());
    List commentRegions = this.getAssociatedCommentedRegions(typedRegions);

    //remove in reverse order as to not effect offset of other regions
    for (int i = commentRegions.size() - 1; i >= 0; --i) {
      try {
        //get the comment region
        ITypedRegion typedRegion = (ITypedRegion) commentRegions.get(i);
        IRegion commentRegion = new Region(typedRegion.getOffset(), typedRegion.getLength());

        /*
         * because of the nature of structured regions the comment region could actually be a sub
         * region that needs to be drilled down too
         */
        if (!this.alreadyCommenting(document, commentRegion)) {
          IStructuredDocumentRegion structuredRegion = document.getRegionAtCharacterOffset(commentRegion.getOffset());

          commentRegion = new Region(structuredRegion.getStartOffset(),
              structuredRegion.getLength());

          if (!this.alreadyCommenting(document, commentRegion)) {
            ITextRegion enclosedRegion = structuredRegion.getRegionAtCharacterOffset(typedRegion.getOffset());
            int enclosedOffset = structuredRegion.getStartOffset(enclosedRegion);
            commentRegion = new Region(enclosedOffset,
                structuredRegion.getTextEndOffset(enclosedRegion) - enclosedOffset);
          }
        }

        //at this point should have found the comment region, if not there is an issue
        if (this.alreadyCommenting(document, commentRegion)) {
          String regionContent = document.get(commentRegion.getOffset(), commentRegion.getLength());

          //if found the comment prefix and suffix then uncomment, otherwise log error
          int commentPrefixOffset = commentRegion.getOffset() + regionContent.indexOf(this.fPrefix);
          int commentSuffixOffset = commentRegion.getOffset();
          commentSuffixOffset += regionContent.lastIndexOf(this.fSuffix);

          //remove comment block depending on if its an enclosing comment block and weather that is allowed
          if (removeEnclosing
              || (commentPrefixOffset >= offset && commentSuffixOffset <= offset + length)) {
            uncomment(document, commentPrefixOffset, this.fPrefix, commentSuffixOffset,
                this.fSuffix);
          }
        } else {
          Logger.log(Logger.ERROR,
              "BlockCommentingStrategy#remove could not find the commenting region to remove"); //$NON-NLS-1$
        }
      } catch (BadLocationException e) {
        Logger.logException(
            "This should only ever happen if something has gone wrong with the partitioning", e); //$NON-NLS-1$
      }
    }
  }

  /**
   * <p>
   * A region is already commented by this strategy if it starts with the strategy's associated
   * prefix and ends with its associated suffix, ignoring any trailing or leading whitespace
   * </p>
   * 
   * @see org.eclipse.wst.sse.ui.internal.comment.CommentingStrategy#alreadyCommenting(org.eclipse.jface.text.IDocument,
   *      org.eclipse.jface.text.IRegion)
   */
  public boolean alreadyCommenting(IStructuredDocument document, IRegion region)
      throws BadLocationException {
    String regionContent = document.get(region.getOffset(), region.getLength()).trim();
    return regionContent.startsWith(this.fPrefix) && regionContent.endsWith(this.fSuffix);
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.comment.CommentingStrategy#clone()
   */
  public Object clone() {
    return new BlockCommentingStrategy(this.fPrefix, this.fSuffix);
  }
}
