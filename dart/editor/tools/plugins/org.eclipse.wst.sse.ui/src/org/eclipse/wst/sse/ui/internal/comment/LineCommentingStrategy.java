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
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;

/**
 * <p>
 * Represents a Line Comment commenting strategy
 * </p>
 */
public class LineCommentingStrategy extends CommentingStrategy {
  /** the prefix of the line comment associated with this strategy */
  private String fPrefix;

  /**
   * @param prefix the prefix of the line comment associated with this strategy
   */
  public LineCommentingStrategy(String prefix) {
    super();
    this.fPrefix = prefix;
  }

  /**
   * <p>
   * Assumes that the given offset is at the begining of a line and adds the line comment prefix
   * there
   * </p>
   * 
   * @see org.eclipse.wst.sse.ui.internal.comment.CommentingStrategy#apply(org.eclipse.wst.sse.core.internal.provisional.IStructuredModel,
   *      int, int)
   */
  public void apply(IStructuredDocument document, int offset, int length)
      throws BadLocationException {

    document.replace(offset, 0, this.fPrefix + " ");
  }

  /**
   * <p>
   * Assumes that the given offset is at the beginning of a line that is commented and removes the
   * comment prefix from the beginning of the line, leading whitespace on the line will not prevent
   * this method from finishing correctly
   * </p>
   * 
   * @see org.eclipse.wst.sse.ui.internal.comment.CommentingStrategy#remove(org.eclipse.wst.sse.core.internal.provisional.IStructuredModel,
   *      int, int, boolean)
   */
  public void remove(IStructuredDocument document, int offset, int length, boolean removeEnclosing)
      throws BadLocationException {
    String content = document.get(offset, length);
    int innerOffset = content.indexOf(this.fPrefix);
    if (innerOffset > 0) {
      offset += innerOffset;
    }

    uncomment(document, offset, this.fPrefix, -1, null);
  }

  /**
   * <p>
   * A region is already commented if it begins with the the associated prefix ignoring any leading
   * whitespace
   * </p>
   * 
   * @see org.eclipse.wst.sse.ui.internal.comment.CommentingStrategy#alreadyCommenting(org.eclipse.jface.text.IDocument,
   *      org.eclipse.jface.text.IRegion)
   */
  public boolean alreadyCommenting(IStructuredDocument document, IRegion region)
      throws BadLocationException {

    String regionContent = document.get(region.getOffset(), region.getLength()).trim();
    return regionContent.startsWith(this.fPrefix);
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.comment.CommentingStrategy#clone()
   */
  public Object clone() {
    return new LineCommentingStrategy(this.fPrefix);
  }
}
