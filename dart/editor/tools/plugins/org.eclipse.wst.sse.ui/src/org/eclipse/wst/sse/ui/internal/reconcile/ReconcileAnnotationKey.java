/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.reconcile;

import org.eclipse.jface.text.reconciler.IReconcileStep;

/**
 * Implementation of <code>IReconcileAnnotationKey</code> note: clients should use the method
 * StructuredReconcileStep#createKey(String partitionType, int scope)
 * 
 * @author pavery
 */
public class ReconcileAnnotationKey {

  public static final int PARTIAL = 1;
  public static final int TOTAL = 0;

  private String fPartitionType = null;

  private IReconcileStep fReconcileStep = null;
  private int fScope;

  public ReconcileAnnotationKey(IReconcileStep step, String partitionType, int scope) {
    fReconcileStep = step;
    fPartitionType = partitionType;
    fScope = scope;
  }

  public String getPartitionType() {
    return fPartitionType;
  }

  public int getScope() {
    return fScope;
  }

  public IReconcileStep getStep() {
    return fReconcileStep;
  }

  public String toString() {
    return this.getClass() + "\r\nid: " + fPartitionType; //$NON-NLS-1$
  }
}
