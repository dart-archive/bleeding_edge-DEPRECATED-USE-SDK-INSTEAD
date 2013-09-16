/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.openon;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * Interface for Open On... navigation
 * 
 * @author amywu
 */
public interface IOpenOn {
  /**
   * Returns the entire region relevant to the current offset where an openable source region is
   * found. null if offset does not contain an openable source.
   * 
   * @param document IDocument
   * @param offset int
   * @return IRegion entire region of openable source
   */
  public IRegion getOpenOnRegion(IDocument document, int offset);

  /**
   * Opens the file/source relevant to region if possible.
   * 
   * @param viewer ITextViewer
   * @param region Region to examine
   */
  public void openOn(IDocument document, IRegion region);
}
