/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.sse.ui.reconcile;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;

/**
 * A course listener for source viewer "reconciling" of a document
 */
public interface ISourceReconcilingListener {

  /**
   * Called before reconciling is started.
   */
  void aboutToBeReconciled();

  /**
   * Called after reconciling has been finished.
   * 
   * @param document the text document or <code>null</code> if reconciliation has been cancelled
   * @param model the annotation model that was changed or <code>null</code> if reconciliation has
   *          been cancelled
   * @param forced <code>true</code> iff this reconciliation was forced
   * @param progressMonitor the progress monitor
   */
  void reconciled(IDocument document, IAnnotationModel model, boolean forced,
      IProgressMonitor progressMonitor);
}
