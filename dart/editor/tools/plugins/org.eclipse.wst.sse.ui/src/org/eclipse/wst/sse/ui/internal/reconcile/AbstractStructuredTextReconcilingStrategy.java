/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.reconcile;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcileResult;
import org.eclipse.jface.text.reconciler.IReconcileStep;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.wst.sse.ui.internal.IReleasable;
import org.eclipse.wst.sse.ui.internal.ITemporaryAnnotation;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.StructuredMarkerAnnotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A base ReconcilingStrategy. Subclasses must implement createReconcileSteps(). This class should
 * not know about IStructuredDocument, only IDocument.
 * 
 * @author pavery
 */
public abstract class AbstractStructuredTextReconcilingStrategy implements IReconcilingStrategy,
    IReconcilingStrategyExtension, IReleasable {

  /** debug flag */
  protected static final boolean DEBUG;
  static {
    String value = Platform.getDebugOption("org.eclipse.wst.sse.ui/debug/reconcilerjob"); //$NON-NLS-1$
    DEBUG = value != null && value.equalsIgnoreCase("true"); //$NON-NLS-1$
  }

  // these limits are safetys for "runaway" validation cases
  // should be used to safeguard potentially dangerous loops or potentially
  // long annotations
  // (since the painter seems to affect performance when painting long
  // annotations)
  public static final int ANNOTATION_LENGTH_LIMIT = 25;
  public static final int ELEMENT_ERROR_LIMIT = 25;

  private IDocument fDocument = null;
  private IProgressMonitor fProgressMonitor = null;
  private ISourceViewer fSourceViewer = null;

  // list of "validator" annotations
  // for gray/un-gray capability
  private HashSet fMarkerAnnotations = null;

  /**
   * Creates a new strategy. The source viewer must be set manually after creation before a
   * reconciler using this constructor will work.
   */
  public AbstractStructuredTextReconcilingStrategy() {
    init();
  }

  /**
   * Creates a new strategy.
   * 
   * @param editor
   */
  public AbstractStructuredTextReconcilingStrategy(ISourceViewer sourceViewer) {
    fSourceViewer = sourceViewer;
    init();
  }

  /**
   * This is where we add results to the annotationModel, doing any special "extra" processing.
   */
  protected void addResultToAnnotationModel(IReconcileResult result) {
    if (!(result instanceof TemporaryAnnotation))
      return;
    // can be null when closing the editor
    if (getAnnotationModel() != null) {
      TemporaryAnnotation tempAnnotation = (TemporaryAnnotation) result;

      StructuredMarkerAnnotation sma = getCorrespondingMarkerAnnotation(tempAnnotation);
      if (sma != null) {
        // un-gray out the marker annotation
        sma.setGrayed(false);
      }

      getAnnotationModel().addAnnotation(tempAnnotation, tempAnnotation.getPosition());
    }
  }

  /**
   * @param object
   * @return if this strategy is responisble for adding this type of key
   */
  protected boolean canHandlePartition(String partition) {
    // String[] haystack = getPartitionTypes();
    // for (int i = 0; i < haystack.length; i++) {
    // if (haystack[i].equals(partition))
    // return true;
    // }
    // return false;
    return false;
  }

  // /**
  // * @param step
  // * @return
  // */
  // protected boolean containsStep(IReconcileStep step) {
  // if (fFirstStep instanceof StructuredReconcileStep)
  // return ((StructuredReconcileStep) fFirstStep).isSiblingStep(step);
  // return false;
  // }

  /**
   * This is where you should create the steps for this strategy
   */
  abstract public void createReconcileSteps();

  /**
   * Remove ALL temporary annotations that this strategy can handle.
   */
  protected TemporaryAnnotation[] getAllAnnotationsToRemove() {
    List removals = new ArrayList();
    IAnnotationModel annotationModel = getAnnotationModel();
    if (annotationModel != null) {
      Iterator i = annotationModel.getAnnotationIterator();
      while (i.hasNext()) {
        Object obj = i.next();
        if (!(obj instanceof ITemporaryAnnotation))
          continue;

        ITemporaryAnnotation annotation = (ITemporaryAnnotation) obj;
        ReconcileAnnotationKey key = (ReconcileAnnotationKey) annotation.getKey();
        // then if this strategy knows how to add/remove this
        // partition type
        if (canHandlePartition(key.getPartitionType()) /*
                                                        * && containsStep(key.getStep())
                                                        */)
          removals.add(annotation);
      }
    }
    return (TemporaryAnnotation[]) removals.toArray(new TemporaryAnnotation[removals.size()]);
  }

  protected IAnnotationModel getAnnotationModel() {
    IAnnotationModel model = null;
    if (fSourceViewer != null) {
      model = fSourceViewer.getAnnotationModel();
    }
    return model;
  }

  protected TemporaryAnnotation[] getAnnotationsToRemove(DirtyRegion dr, List stepsRun) {

    List remove = new ArrayList();
    IAnnotationModel annotationModel = getAnnotationModel();
    // can be null when closing the editor
    if (getAnnotationModel() != null) {

      // clear validator annotations
      getMarkerAnnotations().clear();

      Iterator i = annotationModel.getAnnotationIterator();
      while (i.hasNext()) {

        Object obj = i.next();

        // check if it's a validator marker annotation
        // if it is save it for comparision later (to "gray" icons)
        if (obj instanceof StructuredMarkerAnnotation) {
          StructuredMarkerAnnotation sma = (StructuredMarkerAnnotation) obj;

          if (sma.getAnnotationType() == TemporaryAnnotation.ANNOT_ERROR
              || sma.getAnnotationType() == TemporaryAnnotation.ANNOT_WARNING)
            fMarkerAnnotations.add(sma);
        }

        if (!(obj instanceof TemporaryAnnotation))
          continue;

        TemporaryAnnotation annotation = (TemporaryAnnotation) obj;
        ReconcileAnnotationKey key = (ReconcileAnnotationKey) annotation.getKey();

        // then if this strategy knows how to add/remove this
        // partition type
        if (canHandlePartition(key.getPartitionType()) && stepsRun.contains(key.getStep())) {
          if (key.getScope() == ReconcileAnnotationKey.PARTIAL
              && annotation.getPosition().overlapsWith(dr.getOffset(), dr.getLength())) {
            remove.add(annotation);
          } else if (key.getScope() == ReconcileAnnotationKey.TOTAL) {
            remove.add(annotation);
          }
        }
      }
    }
    return (TemporaryAnnotation[]) remove.toArray(new TemporaryAnnotation[remove.size()]);
  }

  protected abstract boolean containsStep(IReconcileStep step);

  /**
   * Gets partition types from all steps in this strategy.
   * 
   * @return parition types from all steps
   */
  // public String[] getPartitionTypes() {
  // if (fFirstStep instanceof StructuredReconcileStep)
  // return ((StructuredReconcileStep) fFirstStep).getPartitionTypes();
  // return new String[0];
  // }
  public void init() {
    createReconcileSteps();
  }

  /**
   * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#initialReconcile()
   */
  public void initialReconcile() {
    // do nothing
  }

  /**
   * @return
   */
  protected boolean isCanceled() {
    if (DEBUG && (fProgressMonitor != null && fProgressMonitor.isCanceled()))
      System.out.println("** STRATEGY CANCELED **:" + this.getClass().getName()); //$NON-NLS-1$
    return fProgressMonitor != null && fProgressMonitor.isCanceled();
  }

  /**
   * Process the results from the reconcile steps in this strategy.
   * 
   * @param results
   */
  private void process(final IReconcileResult[] results) {
    if (DEBUG)
      System.out.println("[trace reconciler] > STARTING PROCESS METHOD with (" + results.length + ") results"); //$NON-NLS-1$ //$NON-NLS-2$

    if (results == null)
      return;

    for (int i = 0; i < results.length && i < ELEMENT_ERROR_LIMIT && !isCanceled(); i++) {

      if (isCanceled()) {
        if (DEBUG)
          System.out.println("[trace reconciler] >** PROCESS (adding) WAS CANCELLED **"); //$NON-NLS-1$
        return;
      }
      addResultToAnnotationModel(results[i]);
    }

    if (DEBUG) {
      StringBuffer traceString = new StringBuffer();
      for (int j = 0; j < results.length; j++)
        traceString.append("\n (+) :" + results[j] + ":\n"); //$NON-NLS-1$ //$NON-NLS-2$
      System.out.println("[trace reconciler] > PROCESSING (" + results.length + ") results in AbstractStructuredTextReconcilingStrategy " + traceString); //$NON-NLS-1$ //$NON-NLS-2$
    }
  }

  /**
   * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse.jface.text.reconciler.DirtyRegion,
   *      org.eclipse.jface.text.IRegion)
   */
  public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
    // not used
    // we only have validator strategy now

    // // external files may be null
    // if (isCanceled() || fFirstStep == null)
    // return;
    //
    // TemporaryAnnotation[] annotationsToRemove = new
    // TemporaryAnnotation[0];
    // IReconcileResult[] annotationsToAdd = new IReconcileResult[0];
    // StructuredReconcileStep structuredStep = (StructuredReconcileStep)
    // fFirstStep;
    //        
    // annotationsToRemove = getAnnotationsToRemove(dirtyRegion);
    // annotationsToAdd = structuredStep.reconcile(dirtyRegion,
    // subRegion);
    //        
    // smartProcess(annotationsToRemove, annotationsToAdd);
  }

  /**
   * @param partition
   * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse.jface.text.IRegion)
   */
  public void reconcile(IRegion partition) {
    // not used, we use:
    // reconcile(DirtyRegion dirtyRegion, IRegion subRegion)
  }

  /**
   * Calls release() on all the steps in this strategy. Currently done in
   * StructuredRegionProcessor.SourceWidgetDisposeListener#widgetDisposed(...)
   */
  public void release() {
    // release steps (each step calls release on the next)
    // if (fFirstStep != null && fFirstStep instanceof IReleasable)
    // ((IReleasable) fFirstStep).release();
    // we don't to null out the steps, in case
    // it's reconfigured later
  }

  private void removeAnnotations(TemporaryAnnotation[] annotationsToRemove) {

    IAnnotationModel annotationModel = getAnnotationModel();
    // can be null when closing the editor
    if (annotationModel != null) {
      for (int i = 0; i < annotationsToRemove.length; i++) {
        if (isCanceled()) {
          if (DEBUG)
            System.out.println("[trace reconciler] >** REMOVAL WAS CANCELLED **"); //$NON-NLS-1$
          return;
        }
        StructuredMarkerAnnotation sma = getCorrespondingMarkerAnnotation(annotationsToRemove[i]);
        if (sma != null) {
          // gray out the marker annotation
          sma.setGrayed(true);
        }
        // remove the temp one
        annotationModel.removeAnnotation(annotationsToRemove[i]);

      }
    }

    if (DEBUG) {
      StringBuffer traceString = new StringBuffer();
      for (int i = 0; i < annotationsToRemove.length; i++)
        traceString.append("\n (-) :" + annotationsToRemove[i] + ":\n"); //$NON-NLS-1$ //$NON-NLS-2$
      System.out.println("[trace reconciler] > REMOVED (" + annotationsToRemove.length + ") annotations in AbstractStructuredTextReconcilingStrategy :" + traceString); //$NON-NLS-1$ //$NON-NLS-2$
    }
  }

  private StructuredMarkerAnnotation getCorrespondingMarkerAnnotation(
      TemporaryAnnotation tempAnnotation) {

    Iterator it = getMarkerAnnotations().iterator();
    while (it.hasNext()) {
      StructuredMarkerAnnotation markerAnnotation = (StructuredMarkerAnnotation) it.next();
      String message = ""; //$NON-NLS-1$
      try {
        message = (String) markerAnnotation.getMarker().getAttribute(IMarker.MESSAGE);
      } catch (CoreException e) {
        if (DEBUG)
          Logger.logException(e);
      }
      // it would be nice to check line number here...
      if (message != null && message.equals(tempAnnotation.getText()))
        return markerAnnotation;
    }
    return null;
  }

  private void removeAllAnnotations() {
    removeAnnotations(getAllAnnotationsToRemove());
  }

  /**
   * The user needs to manually set the viewer if the default constructor was used.
   * 
   * @param viewer
   */
  public void setViewer(SourceViewer viewer) {
    fSourceViewer = viewer;
  }

  /**
   * Set the document for this strategy.
   * 
   * @param document
   * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#setDocument(org.eclipse.jface.text.IDocument)
   */
  public void setDocument(IDocument document) {

    // remove all old annotations since it's a new document
    removeAllAnnotations();

    if (document == null)
      release();

    // if (getFirstStep() != null)
    // getFirstStep().setInputModel(new DocumentAdapter(document));

    fDocument = document;
  }

  public IDocument getDocument() {
    return fDocument;
  }

  /**
   * @param monitor
   * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#setProgressMonitor(org.eclipse.core.runtime.IProgressMonitor)
   */
  public void setProgressMonitor(IProgressMonitor monitor) {
    // fProgressMonitor = monitor;
    // if (fFirstStep != null)
    // fFirstStep.setProgressMonitor(fProgressMonitor);
  }

  /**
   * Check if the annotation is already there, if it is, no need to remove or add again. This will
   * avoid a lot of "flickering" behavior.
   * 
   * @param annotationsToRemove
   * @param annotationsToAdd
   */
  protected void smartProcess(TemporaryAnnotation[] annotationsToRemove,
      IReconcileResult[] annotationsToAdd) {
//		Comparator comp = getTemporaryAnnotationComparator();
//		List sortedRemovals = Arrays.asList(annotationsToRemove);
//		Collections.sort(sortedRemovals, comp);
//
//		List sortedAdditions = Arrays.asList(annotationsToAdd);
//		Collections.sort(sortedAdditions, comp);
//
//		List filteredRemovals = new ArrayList(sortedRemovals);
//		List filteredAdditions = new ArrayList(sortedAdditions);
//
//		boolean ignore = false;
//		int lastFoundAdded = 0;
//		for (int i = 0; i < sortedRemovals.size(); i++) {
//			TemporaryAnnotation removal = (TemporaryAnnotation) sortedRemovals.get(i);
//			for (int j = lastFoundAdded; j < sortedAdditions.size(); j++) {
//				TemporaryAnnotation addition = (TemporaryAnnotation) sortedAdditions.get(j);
//				// quick position check here
//				if (removal.getPosition().equals(addition.getPosition())) {
//					lastFoundAdded = j;
//					// remove performs TemporaryAnnotation.equals()
//					// which checks text as well
//					filteredAdditions.remove(addition);
//					ignore = true;
//					if (DEBUG)
//						System.out.println(" ~ smart process ignoring: " + removal.getPosition().getOffset()); //$NON-NLS-1$
//					break;
//				}
//			}
//			if (ignore) {
//				filteredRemovals.remove(removal);
//			}
//			ignore = false;
//		}
    if (getAnnotationModel() instanceof IAnnotationModelExtension) {
//			TemporaryAnnotation[] filteredRemovalArray = (TemporaryAnnotation[]) filteredRemovals.toArray(new TemporaryAnnotation[filteredRemovals.size()]);
//			// apply "grey"-ness
//			for (int i = 0; i < filteredRemovalArray.length; i++) {
//				if (isCanceled()) {
//					if (DEBUG)
//						System.out.println("[trace reconciler] >** replacing WAS CANCELLED **"); //$NON-NLS-1$
//					return;
//				}
//				StructuredMarkerAnnotation sma = getCorrespondingMarkerAnnotation(filteredRemovalArray[i]);
//				if (sma != null) {
//					// gray out the marker annotation
//					sma.setGrayed(true);
//				}
//			}
//			Map annotationsToAddMap = new HashMap();
//			for (int i = 0; i < filteredAdditions.size(); i++) {
//				TemporaryAnnotation temporaryAnnotation = (TemporaryAnnotation) filteredAdditions.get(i);
//				annotationsToAddMap.put(temporaryAnnotation, temporaryAnnotation.getPosition());
//			}
//			if (isCanceled()) {
//				if (DEBUG)
//					System.out.println("[trace reconciler] >** PROCESS (replacing) WAS CANCELLED **"); //$NON-NLS-1$
//				return;
//			}
//			/*
//			 * Using the extension means we can't enforce the
//			 * ELEMENT_ERROR_LIMIT limit.
//			 */
//			((IAnnotationModelExtension) getAnnotationModel()).replaceAnnotations(filteredRemovalArray, annotationsToAddMap);

      Map annotationsToAddMap = new HashMap();
      for (int i = 0; i < annotationsToAdd.length; i++) {
        TemporaryAnnotation temporaryAnnotation = (TemporaryAnnotation) annotationsToAdd[i];
        annotationsToAddMap.put(temporaryAnnotation, temporaryAnnotation.getPosition());
      }
      if (isCanceled()) {
        if (DEBUG)
          System.out.println("[trace reconciler] >** PROCESS (replacing) WAS CANCELLED **"); //$NON-NLS-1$
        return;
      }
      ((IAnnotationModelExtension) getAnnotationModel()).replaceAnnotations(annotationsToRemove,
          annotationsToAddMap);
    } else {
//			removeAnnotations((TemporaryAnnotation[]) filteredRemovals.toArray(new TemporaryAnnotation[filteredRemovals.size()]));
//			process((IReconcileResult[]) filteredAdditions.toArray(new IReconcileResult[filteredAdditions.size()]));
      removeAnnotations(annotationsToRemove);
      process(annotationsToAdd);
    }
  }

//	private Comparator getTemporaryAnnotationComparator() {
//		if (fComparator == null) {
//			fComparator = new Comparator() {
//				public int compare(Object arg0, Object arg1) {
//					TemporaryAnnotation ta1 = (TemporaryAnnotation) arg0;
//					TemporaryAnnotation ta2 = (TemporaryAnnotation) arg1;
//					int result = ta1.getPosition().getOffset() - ta2.getPosition().getOffset();
//					if(result != 0)
//						return result;
//					return Collator.getInstance().compare(ta1.getText(), ta2.getText());
//				}
//			};
//		}
//		return fComparator;
//	}

  public HashSet getMarkerAnnotations() {
    if (fMarkerAnnotations == null)
      fMarkerAnnotations = new HashSet();
    return fMarkerAnnotations;
  }
}
