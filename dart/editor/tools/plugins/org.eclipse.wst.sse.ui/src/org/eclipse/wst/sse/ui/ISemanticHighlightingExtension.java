/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui;

import org.eclipse.jface.text.Position;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;

/**
 * Extends {@link org.eclipse.wst.sse.ui.ISemanticHighlighting} allowing clients to define
 * consumability with respect to indexed regions containing the structured document region.
 * 
 * @since 3.2
 */
public interface ISemanticHighlightingExtension {

  /**
   * Returns an array of positions iff the semantic highlighting consumes any part of the structured
   * document region
   * <p>
   * NOTE: Implementors are not allowed to keep a reference on the either regions or on any object
   * retrieved from the regions.
   * </p>
   * 
   * @param documentRegion the structured document region
   * @param indexedRegion the indexed region that contains the <code>documentRegion</code>
   * @return an array of positions to consume iff the semantic highlighting consumes any part of the
   *         structured document region, otherwise null
   */
  public Position[] consumes(IStructuredDocumentRegion documentRegion, IndexedRegion indexedRegion);
}
