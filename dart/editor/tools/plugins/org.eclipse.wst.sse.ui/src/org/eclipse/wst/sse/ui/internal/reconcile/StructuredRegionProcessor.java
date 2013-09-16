/*******************************************************************************
 * Copyright (c) 2001, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.reconcile;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.model.ModelLifecycleEvent;
import org.eclipse.wst.sse.core.internal.provisional.IModelLifecycleListener;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;

/**
 * An IStructuredModel aware Region Processor. Adds ModelLifecycle listener. ModelLifecycle listener
 * notifies us that some reinitialization needs to take place. Model aware "process()" Implements a
 * IndexedRegion-based "contains()" method using IStructuredModel.
 */
public class StructuredRegionProcessor extends DocumentRegionProcessor {
  class ModelLifecycleListener implements IModelLifecycleListener {
    IStructuredModel changing = null;

    /**
     * @see org.eclipse.wst.sse.core.internal.provisional.IModelLifecycleListener#processPostModelEvent(org.eclipse.wst.sse.core.internal.model.ModelLifecycleEvent)
     */
    public void processPostModelEvent(ModelLifecycleEvent event) {

      // if underlying StructuredDocument changed, need to reconnect it
      // here...
      // ex. file is modified outside the workbench
      if (event.getType() == ModelLifecycleEvent.MODEL_DOCUMENT_CHANGED) {
        if (changing != null && event.getModel() == changing) {
          IStructuredDocument sDoc = event.getModel().getStructuredDocument();

          if (DEBUG) {
            System.out.println("======================================================"); //$NON-NLS-1$
            System.out.println("StructuredRegionProcessor: DOCUMENT MODEL CHANGED TO: "); //$NON-NLS-1$
            System.out.println(sDoc.get());
            System.out.println("======================================================"); //$NON-NLS-1$
          }
          setDocument(sDoc);
        }
        changing = null;
      }
    }

    /**
     * @see org.eclipse.wst.sse.core.internal.provisional.IModelLifecycleListener#processPreModelEvent(org.eclipse.wst.sse.core.internal.model.ModelLifecycleEvent)
     */
    public void processPreModelEvent(ModelLifecycleEvent event) {
      if (fCurrentDoc != null && fCurrentModel != null) {
        if (event.getType() == ModelLifecycleEvent.MODEL_DOCUMENT_CHANGED
            && event.getModel() == fCurrentModel) {
          changing = event.getModel();
          flushDirtyRegionQueue();
          /*
           * note: old annotations are removed via the strategies on
           * AbstractStructuredTextReconcilingStrategy #setDocument(...)
           */
        }
      }
    }
  }

  /**
   * The life cycle listener used to listen to the current documents model even though this class
   * does not hold onto a reference to the model.
   */
  private IModelLifecycleListener fLifeCycleListener = new ModelLifecycleListener();
  /** Used to get the current model on demand so a model does not need to be held by permanently */
  private IDocument fCurrentDoc = null;

  /**
   * Only to be used for content type and IModelLifecycleListener registration. All other uses
   * should "get" the model --getting the model during these specific options can cause lock
   * interruption by org.eclipse.core.internal.jobs.DeadlockDetector.
   */
  private IStructuredModel fCurrentModel = null;

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.wst.sse.ui.internal.reconcile.DirtyRegionProcessor#getOuterRegion(org.eclipse.jface
   * .text.reconciler.DirtyRegion, org.eclipse.jface.text.reconciler.DirtyRegion)
   */
  protected DirtyRegion getOuterRegion(DirtyRegion root, DirtyRegion possible) {
    // first try simple region check if one region contains the other
    DirtyRegion outer = super.getOuterRegion(root, possible);
    if (outer == null && fCurrentDoc != null) {
      IStructuredModel sModel = null;
      try {
        sModel = getStructuredModelForRead(fCurrentDoc);
        if (sModel != null) {
          // now compare nodes
          IndexedRegion rootRegion = sModel.getIndexedRegion(root.getOffset());
          IndexedRegion possRegion = sModel.getIndexedRegion(possible.getOffset());
          if (rootRegion != null && possRegion != null) {
            int rootStart = rootRegion.getStartOffset();
            int possStart = possRegion.getStartOffset();
            // first just check if rootregion starts before
            // possregion
            if (rootStart <= possStart) {
              // check if possregion is inside rootregion
              outer = _getOuterRegion(root, possible, sModel, rootStart, possStart);
            } else {
              // otherwise if rootregion is inside possregion
              outer = _getOuterRegion(possible, root, sModel, possStart, rootStart);
            }
          }
        }

      } finally {
        if (sModel != null) {
          sModel.releaseFromRead();
        }
      }
    }
    return outer;
  }

  /**
   * Assumes that when this method is called, region1's node start offset >= region2's node start
   * offset. Determines if region1 contains region2 or vice versa. Returns region1 if:
   * <ul>
   * <li>region1's node region == region2's node region</li>
   * <li>region1's node region contains region2's node region</li>
   * </ul>
   * Returns region2 if:
   * <ul>
   * <li>region1's node region and region2's node region starts at same offset but region2's node
   * region is longer</li>
   * </ul>
   * Returns null otherwise.
   * 
   * @param region1
   * @param region2
   * @param sModel
   * @param region1NodeStart
   * @param region2NodeStart
   * @return outer dirty region or null if none exists.
   */
  private DirtyRegion _getOuterRegion(DirtyRegion region1, DirtyRegion region2,
      IStructuredModel sModel, int region1NodeStart, int region2NodeStart) {
    DirtyRegion outer = null;
    int region1NodeEnd = -1;
    int region2NodeEnd = -1;
    // then check if region1's end appears after
    // region2's end
    IndexedRegion region1EndNode = sModel.getIndexedRegion(region1.getOffset()
        + region1.getLength());
    if (region1EndNode == null) {
      // if no end, just assume region spans all the
      // way to the end so it includes other region
      outer = region1;
    } else {
      region1NodeEnd = region1EndNode.getEndOffset();
      IndexedRegion region2EndNode = sModel.getIndexedRegion(region2.getOffset()
          + region2.getLength());
      region2NodeEnd = region2EndNode != null ? region2EndNode.getEndOffset()
          : getDocument().getLength();
      if (region1NodeEnd >= region2NodeEnd) {
        // root contains or is equal to possible
        outer = region1;
      } else if (region1NodeStart == region2NodeStart && region2NodeEnd >= region1NodeEnd) {
        // possible contains root because they
        // both start at same place but possible
        // is longer
        outer = region2;
      }
    }
    if (DEBUG) {
      if (outer != null)
        System.out.println("checking if [" + region1NodeStart + ":" + region1NodeEnd + "] contains [" + region2NodeStart + ":" + region2NodeEnd + "] ... " + outer.toString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
      else
        System.out.println("checking if [" + region1NodeStart + ":" + region1NodeEnd + "] contains [" + region2NodeStart + ":" + region2NodeEnd + "] ... NO CONTAIN"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    }
    return outer;
  }

  /**
   * We already know content type from when we created the model, so just use that.
   */
  protected String getContentType(IDocument doc) {
    if (fCurrentModel != null && doc == fCurrentModel.getStructuredDocument()) {
      /*
       * Avoid "get"ting a model if we can, it may require lock acquisition
       */
      return fCurrentModel.getContentTypeIdentifier();
    }

    String contentTypeId = null;
    IStructuredModel sModel = null;
    try {
      sModel = getStructuredModelForRead(doc);
      if (sModel != null) {
        contentTypeId = sModel.getContentTypeIdentifier();
      }
    } finally {
      if (sModel != null)
        sModel.releaseFromRead();
    }
    return contentTypeId;
  }

  /**
   * Remember to release model after use!!
   * 
   * @return
   */
  private IStructuredModel getStructuredModelForRead(IDocument doc) {
    IStructuredModel sModel = null;
    if (doc != null && doc instanceof IStructuredDocument)
      sModel = StructuredModelManager.getModelManager().getModelForRead((IStructuredDocument) doc);
    return sModel;
  }

  protected void process(DirtyRegion dirtyRegion) {
    if (!isInstalled() || isInRewriteSession() || dirtyRegion == null || getDocument() == null)
      return;

    // unhook old lifecycle listener
    if (fCurrentDoc != null) {
      IStructuredModel sModel = null;
      try {
        sModel = getStructuredModelForRead(fCurrentDoc);

        // use structured model to determine area to process
        if (sModel != null) {
          int start = dirtyRegion.getOffset();
          int end = start + dirtyRegion.getLength();
          IndexedRegion irStart = sModel.getIndexedRegion(start);
          IndexedRegion irEnd = sModel.getIndexedRegion(end);

          if (irStart != null) {
            start = Math.min(start, irStart.getStartOffset());
          }
          if (irEnd != null) {
            end = Math.max(end, irEnd.getEndOffset());
          }
          super.process(createDirtyRegion(start, end - start, DirtyRegion.INSERT));
        } else {
          super.process(dirtyRegion);
        }
      } finally {
        if (sModel != null) {
          sModel.releaseFromRead();
        }
      }
    } else {
      super.process(dirtyRegion);
    }
  }

  /**
   * Reinitializes listeners and sets new document onall strategies.
   * 
   * @see org.eclipse.jface.text.reconciler.AbstractReconciler#reconcilerDocumentChanged(IDocument)
   */
  protected void reconcilerDocumentChanged(IDocument newDocument) {
    setDocument(newDocument);
  }

  public void setDocument(IDocument newDocument) {
    /*
     * unhook old lifecycle listener; it is important not to re-get the model at this point as we
     * may be shutting down
     */
    if (fCurrentModel != null) {
      fCurrentModel.removeModelLifecycleListener(fLifeCycleListener);
      fCurrentModel = null;
    }

    // set the new document
    super.setDocument(newDocument);
    fCurrentDoc = newDocument;

    // add new lifecycle listener
    if (fCurrentDoc != null) {
      try {
        fCurrentModel = getStructuredModelForRead(fCurrentDoc);
        if (fCurrentModel != null) {
          fCurrentModel.addModelLifecycleListener(fLifeCycleListener);
        }
      } finally {
        if (fCurrentModel != null) {
          fCurrentModel.releaseFromRead();
        }
      }
    }
  }
}
