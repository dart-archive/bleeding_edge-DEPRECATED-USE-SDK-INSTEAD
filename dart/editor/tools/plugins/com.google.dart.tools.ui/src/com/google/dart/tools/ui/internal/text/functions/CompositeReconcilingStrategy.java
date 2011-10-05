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
package com.google.dart.tools.ui.internal.text.functions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;

/**
 * A reconciling strategy consisting of a sequence of internal reconciling strategies. By default,
 * all requests are passed on to the contained strategies.
 */
public class CompositeReconcilingStrategy implements IReconcilingStrategy,
    IReconcilingStrategyExtension {

  /** The list of internal reconciling strategies. */
  private IReconcilingStrategy[] fStrategies;

  /**
   * Creates a new, empty composite reconciling strategy.
   */
  public CompositeReconcilingStrategy() {
  }

  /**
   * Returns the previously set stratgies or <code>null</code>.
   * 
   * @return the contained strategies or <code>null</code>
   */
  public IReconcilingStrategy[] getReconcilingStrategies() {
    return fStrategies;
  }

  /*
   * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension# initialReconcile()
   */
  @Override
  public void initialReconcile() {
    if (fStrategies == null) {
      return;
    }

    for (int i = 0; i < fStrategies.length; i++) {
      if (fStrategies[i] instanceof IReconcilingStrategyExtension) {
        IReconcilingStrategyExtension extension = (IReconcilingStrategyExtension) fStrategies[i];
        extension.initialReconcile();
      }
    }
  }

  /*
   * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse
   * .jface.text.reconciler.DirtyRegion, org.eclipse.jface.text.IRegion)
   */
  @Override
  public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
    if (fStrategies == null) {
      return;
    }

    for (int i = 0; i < fStrategies.length; i++) {
      fStrategies[i].reconcile(dirtyRegion, subRegion);
    }
  }

  /*
   * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse
   * .jface.text.IRegion)
   */
  @Override
  public void reconcile(IRegion partition) {
    if (fStrategies == null) {
      return;
    }

    for (int i = 0; i < fStrategies.length; i++) {
      fStrategies[i].reconcile(partition);
    }
  }

  /*
   * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#setDocument(org.
   * eclipse.jface.text.IDocument)
   */
  @Override
  public void setDocument(IDocument document) {
    if (fStrategies == null) {
      return;
    }

    for (int i = 0; i < fStrategies.length; i++) {
      fStrategies[i].setDocument(document);
    }
  }

  /*
   * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#
   * setProgressMonitor(org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public void setProgressMonitor(IProgressMonitor monitor) {
    if (fStrategies == null) {
      return;
    }

    for (int i = 0; i < fStrategies.length; i++) {
      if (fStrategies[i] instanceof IReconcilingStrategyExtension) {
        IReconcilingStrategyExtension extension = (IReconcilingStrategyExtension) fStrategies[i];
        extension.setProgressMonitor(monitor);
      }
    }
  }

  /**
   * Sets the reconciling strategies for this composite strategy.
   * 
   * @param strategies the strategies to be set or <code>null</code>
   */
  public void setReconcilingStrategies(IReconcilingStrategy[] strategies) {
    fStrategies = strategies;
  }
}
