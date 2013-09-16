/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.contentassist;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;

/**
 * <p>
 * A {@link IContextInformationValidator} for structured documents
 * </p>
 */
public class ContextInformationValidator implements IContextInformationValidator {

  /** the installed document position */
  private int fDocumentPosition;

  /** the installed document */
  private IStructuredDocument fDocument;

  /** the installed region start */
  private IndexedRegion fRegion;

  /**
   * <p>
   * Default constructor
   * </p>
   */
  public ContextInformationValidator() {
    fDocumentPosition = -1;
    fDocument = null;
  }

  /**
   * @see org.eclipse.jface.text.contentassist.IContextInformationValidator#install(org.eclipse.jface.text.contentassist.IContextInformation,
   *      org.eclipse.jface.text.ITextViewer, int)
   */
  public void install(IContextInformation info, ITextViewer viewer, int documentPosition) {
    fDocumentPosition = documentPosition;
    fDocument = (IStructuredDocument) viewer.getDocument();
  }

  /**
   * @see org.eclipse.jface.text.contentassist.IContextInformationValidator#isContextInformationValid(int)
   */
  public boolean isContextInformationValid(int documentPosition) {
    /*
     * determine whether or not this context info should still be showing... if cursor still within
     * the same region then it's valid
     */
    boolean result = false;

    calculateInstalledIndexedRegion();

    if (fRegion != null) {
      int start = fRegion.getStartOffset();
      int end = fRegion.getEndOffset();
      result = (documentPosition < end) && (documentPosition > start + 1);
    }
    return result;
  }

  /**
   * @return {@link IndexedRegion} that this validator was installed on
   */
  private void calculateInstalledIndexedRegion() {
    if (fRegion == null) {
      IStructuredModel model = null;
      try {
        model = StructuredModelManager.getModelManager().getModelForRead(fDocument);
        if (model != null) {
          fRegion = model.getIndexedRegion(fDocumentPosition);
        }
      } finally {
        if (model != null)
          model.releaseFromRead();
      }
    }
  }
}
