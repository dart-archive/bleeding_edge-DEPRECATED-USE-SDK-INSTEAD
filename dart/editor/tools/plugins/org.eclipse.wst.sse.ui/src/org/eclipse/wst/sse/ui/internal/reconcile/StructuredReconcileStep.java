/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.reconcile;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.reconciler.AbstractReconcileStep;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilableModel;
import org.eclipse.jface.text.reconciler.IReconcileResult;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredPartitioning;
import org.eclipse.wst.sse.core.text.IStructuredPartitions;
import org.eclipse.wst.sse.ui.internal.IReleasable;
import org.eclipse.wst.sse.ui.internal.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * ReconcileStep that knows about the annotation that it adds to the AnnotationModel. It knows how
 * to create an annotation key (for smart removal later) It knows the partition types on which it
 * can operate. It knows the scope on which it operates (for short circuiting) It knows if the
 * Reconciler is reconciling the entire document. Clients must subclass this class.
 * 
 * @author pavery
 */
public abstract class StructuredReconcileStep extends AbstractReconcileStep implements IReleasable {

  /** debug flag */
  protected static final boolean DEBUG;
  static {
    String value = Platform.getDebugOption("org.eclipse.wst.sse.ui/debug/reconcilerjob"); //$NON-NLS-1$
    DEBUG = value != null && value.equalsIgnoreCase("true"); //$NON-NLS-1$
  }

  protected final IReconcileResult[] EMPTY_RECONCILE_RESULT_SET = new IReconcileResult[0];

  /**
   * It's possible for a partial step to get called on the same area twice (as w/ a full document
   * reconcile) this list keeps track of area already covered. Should be reset() after the "batch"
   * of reconciling is finished.
   */
  private HashSet fPartitionTypes = null;

  public StructuredReconcileStep() {
    super();
    fPartitionTypes = new HashSet();
  }

  public ReconcileAnnotationKey createKey(IStructuredDocumentRegion sdRegion, int scope) {

    ITypedRegion tr = getPartition(sdRegion);
    String partitionType = (tr != null) ? tr.getType() : IStructuredPartitions.UNKNOWN_PARTITION;
    return createKey(partitionType, scope);
  }

  /**
   * @param sdRegion
   * @return
   */
  protected ITypedRegion getPartition(IStructuredDocumentRegion sdRegion) {
    ITypedRegion tr = null;
    if (!sdRegion.isDeleted())
      tr = getPartition(sdRegion.getParentDocument(), sdRegion.getStartOffset());
    return tr;
  }

  private ITypedRegion getPartition(IDocument doc, int offset) {
    ITypedRegion tr = null;
    // not sure why document would ever be null, but put in this 
    // guard for 
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=86069
    if (doc != null) {
      try {
        tr = TextUtilities.getPartition(doc,
            IStructuredPartitioning.DEFAULT_STRUCTURED_PARTITIONING, offset, false);
      } catch (BadLocationException e) {
        if (DEBUG)
          Logger.logException("problem getting partition at: " + offset, e); //$NON-NLS-1$
      }
    }
    return tr;
  }

  /**
   * Clients should use this method to create annotation keys as it registers the key for removal
   * later.
   * 
   * @param partitionType
   * @param scope
   * @return
   */
  public ReconcileAnnotationKey createKey(String partitionType, int scope) {
    fPartitionTypes.add(partitionType);
    return new ReconcileAnnotationKey(this, partitionType, scope);
  }

  protected IDocument getDocument() {
    IDocument doc = null;
    IReconcilableModel rModel = getModel();
    if (rModel instanceof DocumentAdapter) {
      doc = ((DocumentAdapter) rModel).getDocument();
    }
    return doc;
  }

  public IReconcilableModel getModel() {
    return getInputModel();
  }

  public String getPartitionType(IDocument doc, int offset) {
    ITypedRegion tr = getPartition(doc, offset);
    return (tr != null) ? tr.getType() : IStructuredPartitions.UNKNOWN_PARTITION;
  }

  public String[] getPartitionTypes() {
    // using hash set to automatically get rid of dupes
    HashSet tempResults = new HashSet();
    // add these partition types
    tempResults.addAll(fPartitionTypes);
    return (String[]) tempResults.toArray(new String[tempResults.size()]);
  }

  protected IStructuredDocument getStructuredDocument() {
    IStructuredDocument sDoc = null;
    IDocument doc = getDocument();
    if (doc instanceof IStructuredDocument)
      sDoc = (IStructuredDocument) getDocument();
    return sDoc;
  }

  /**
   * Removes duplicates.
   * 
   * @param results1
   * @param results2
   * @return
   */
  protected IReconcileResult[] merge(IReconcileResult[] results1, IReconcileResult[] results2) {
    if (results1 == null)
      return results2;
    if (results2 == null)
      return results1;

    List results = new ArrayList();
    results.addAll(Arrays.asList(results1));
    for (int i = 0; i < results2.length; i++) {
      results.add(results2[i]);
    }
    return (IReconcileResult[]) results.toArray(new IReconcileResult[results.size()]);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.text.reconciler.AbstractReconcileStep#reconcileModel(org.eclipse.jface.text
   * .reconciler.DirtyRegion, org.eclipse.jface.text.IRegion)
   */
  protected IReconcileResult[] reconcileModel(DirtyRegion dirtyRegion, IRegion subRegion) {
    return EMPTY_RECONCILE_RESULT_SET;
  }

  /**
   * Release resources used by the step here as needed. Be sure to call super.release() when you
   * override this method as to propagate the release through all steps.
   */
  public void release() {
    // 
  }
}
