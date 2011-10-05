/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.text.dart;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;

/**
 * Describes the context of an invocation of content assist in a text viewer. The context knows the
 * document, the invocation offset and can lazily compute the identifier prefix preceding the
 * invocation offset. It may know the viewer.
 * <p>
 * Subclasses may add information to their environment. For example, source code editors may provide
 * specific context information such as an AST.
 * </p>
 * <p>
 * Clients may instantiate and subclass.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public class ContentAssistInvocationContext {

  /* state */
  private final ITextViewer fViewer;
  private final IDocument fDocument;
  private final int fOffset;

  /* cached additional info */
  private CharSequence fPrefix;

  /**
   * Creates a new context for the given document and offset.
   * 
   * @param document the document that content assist is invoked in
   * @param offset the offset into the document where content assist is invoked at
   */
  public ContentAssistInvocationContext(IDocument document, int offset) {
    Assert.isNotNull(document);
    Assert.isTrue(offset >= 0);
    fViewer = null;
    fDocument = document;
    fOffset = offset;
  }

  /**
   * Equivalent to {@linkplain #ContentAssistInvocationContext(ITextViewer, int)
   * ContentAssistInvocationContext(viewer, viewer.getSelectedRange().x)}.
   * 
   * @param viewer the text viewer that content assist is invoked in
   */
  public ContentAssistInvocationContext(ITextViewer viewer) {
    this(viewer, viewer.getSelectedRange().x);
  }

  /**
   * Creates a new context for the given viewer and offset.
   * 
   * @param viewer the text viewer that content assist is invoked in
   * @param offset the offset into the viewer's document where content assist is invoked at
   */
  public ContentAssistInvocationContext(ITextViewer viewer, int offset) {
    Assert.isNotNull(viewer);
    fViewer = viewer;
    fDocument = null;
    fOffset = offset;
  }

  /**
   * Creates a new context with no viewer or invocation offset set.
   */
  protected ContentAssistInvocationContext() {
    fDocument = null;
    fViewer = null;
    fOffset = -1;
  }

  /**
   * Computes the identifier (as specified by {@link Character#isJavaIdentifierPart(char)}) that
   * immediately precedes the invocation offset.
   * 
   * @return the prefix preceding the content assist invocation offset, <code>null</code> if there
   *         is no document
   * @throws BadLocationException if accessing the document fails
   */
  public CharSequence computeIdentifierPrefix() throws BadLocationException {
    if (fPrefix == null) {
      IDocument document = getDocument();
      if (document == null) {
        return null;
      }
      int end = getInvocationOffset();
      int start = end;
      while (--start >= 0) {
        if (!Character.isJavaIdentifierPart(document.getChar(start))) {
          break;
        }
      }
      start++;
      fPrefix = document.get(start, end - start);
    }

    return fPrefix;
  }

  /**
   * Invocation contexts are equal if they describe the same context and are of the same type. This
   * implementation checks for <code>null</code> values and class equality. Subclasses should extend
   * this method by adding checks for their context relevant fields (but not necessarily cached
   * values).
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
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!getClass().equals(obj.getClass())) {
      return false;
    }
    ContentAssistInvocationContext other = (ContentAssistInvocationContext) obj;
    return (fViewer == null && other.fViewer == null || fViewer != null
        && fViewer.equals(other.fViewer))
        && fOffset == other.fOffset
        && (fDocument == null && other.fDocument == null || fDocument != null
            && fDocument.equals(other.fDocument));
  }

  /**
   * Returns the document that content assist is invoked on, or <code>null</code> if not known.
   * 
   * @return the document or <code>null</code>
   */
  public IDocument getDocument() {
    if (fDocument == null) {
      if (fViewer == null) {
        return null;
      }
      return fViewer.getDocument();
    }
    return fDocument;
  }

  /**
   * Returns the invocation offset.
   * 
   * @return the invocation offset
   */
  public final int getInvocationOffset() {
    return fOffset;
  }

  /**
   * Returns the viewer, <code>null</code> if not available.
   * 
   * @return the viewer, possibly <code>null</code>
   */
  public final ITextViewer getViewer() {
    return fViewer;
  }

  /*
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return 23459213 << 5 | (fViewer == null ? 0 : fViewer.hashCode() << 3) | fOffset;
  }
}
