/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.reconcile;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcileResult;
import org.eclipse.jface.text.reconciler.IReconcileStep;

/**
 * @author pavery
 * @deprecated still working out API (or if anything here should be API) Interface for a step in
 *             StructuredRegionProcessor framework.
 */
public interface IStructuredReconcileStep extends IReconcileStep {
  /**
   * Partitions for which this step can add/remove annotions
   * 
   * @return an array of the partitions for which this step can add/remove annotions
   */
  String[] getPartitionTypes();

  /**
   * Returns the scope for which this step adds annotations.
   * 
   * @return the scope for which this step adds annotations
   */
  //int getScope();

  /**
   * Tells you if the step is equal to this step or any of the sibling steps.
   * 
   * @return
   */
  boolean isSiblingStep(IReconcileStep step);

  /**
   * Adds awareness that the Reconciler is reconciling the entire document this call.
   * 
   * @param dirtyRegion
   * @param subRegion
   * @param refreshAll
   * @return
   */
  IReconcileResult[] reconcile(DirtyRegion dirtyRegion, IRegion subRegion, boolean refreshAll);

  /**
   * Used to reset the state of the Strategy. For example: any flags that need to be reset after a
   * long running operation like processAll().
   */
  void reset();
}
