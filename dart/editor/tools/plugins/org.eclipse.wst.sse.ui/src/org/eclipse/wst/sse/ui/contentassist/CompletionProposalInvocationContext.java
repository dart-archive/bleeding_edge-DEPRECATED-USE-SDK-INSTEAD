/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.contentassist;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.wst.sse.ui.internal.Logger;

/**
 * <p>
 * Helpful class for passing around information about the invocation context of a content assist
 * request
 * </p>
 * 
 * @base org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext
 */
public class CompletionProposalInvocationContext {
  /** the viewer that was used to invoke content assist */
  private final ITextViewer fViewer;

  /** the character offset where content assist was invoked in the viewer */
  private final int fInvocationOffset;

  /** the partition type where content assist was invoked */
  private String fPartitionType;

  /**
   * <p>
   * Create a new context
   * </p>
   * 
   * @param viewer {@link ITextViewer} that was used to invoke content assist
   * @param invocationOffset character offset where content assist was invoked in the viewer
   */
  public CompletionProposalInvocationContext(ITextViewer viewer, int invocationOffset) {
    fViewer = viewer;
    fInvocationOffset = invocationOffset;
    fPartitionType = null;
  }

  /**
   * @return {@link IDocument} that content assist was invoked on
   */
  public IDocument getDocument() {
    return fViewer.getDocument();
  }

  /**
   * @return {@link ITextViewer} that was used to invoke content assist
   */
  public ITextViewer getViewer() {
    return fViewer;
  }

  /**
   * @return character offset where content assist was invoked in the viewer
   */
  public int getInvocationOffset() {
    return fInvocationOffset;
  }

  /**
   * @return the partition type where content assist was invoked
   */
  public String getInvocationPartitionType() {
    if (fPartitionType == null) {
      fPartitionType = ""; //$NON-NLS-1$
      try {
        fPartitionType = this.getDocument().getPartition(this.fInvocationOffset).getType();
      } catch (BadLocationException e) {
        // should never happen, nothing we can do about it if it does
        Logger.logException(
            "Could not get the partition type at content assist invocation offset", e); //$NON-NLS-1$
      }
    }
    return fPartitionType;
  }

  /**
   * <p>
   * Invocation contexts are equal if they describe the same context and are of the same type. This
   * implementation checks for <code>null</code> values and class equality. Subclasses should extend
   * this method by adding checks for their context relevant fields (but not necessarily cached
   * values).
   * </p>
   * <p>
   * Example:
   * 
   * <pre>
	 * class MyContext extends ContentAssistInvocationContext {
	 * 	private final Object fState;
	 * 	private Object fCachedInfo;
	 *
	 * 	...
	 *
	 * 	public boolean equals(Object obj) {
	 * 		if (!super.equals(obj))
	 * 			return false;
	 * 		MyContext other= (MyContext) obj;
	 * 		return fState.equals(other.fState);
	 * 	}
	 * }
	 * </pre>
   * </p>
   * <p>
   * Subclasses should also extend {@link Object#hashCode()}.
   * </p>
   * 
   * @param obj {@inheritDoc}
   * @return {@inheritDoc}
   */
  public boolean equals(Object obj) {
    boolean equal = false;
    if (obj instanceof CompletionProposalInvocationContext) {
      CompletionProposalInvocationContext other = (CompletionProposalInvocationContext) obj;
      equal = (fViewer == null && other.fViewer == null || fViewer != null
          && fViewer.equals(other.fViewer))
          && fInvocationOffset == other.fInvocationOffset;
    }
    return equal;
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return 23459213 << 5 | (fViewer == null ? 0 : fViewer.hashCode() << 3) | fInvocationOffset;
  }
}
