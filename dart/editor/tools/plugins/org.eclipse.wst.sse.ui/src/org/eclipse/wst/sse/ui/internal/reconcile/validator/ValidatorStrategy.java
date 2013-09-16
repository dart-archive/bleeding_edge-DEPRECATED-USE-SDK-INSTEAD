/*******************************************************************************
 * Copyright (c) 2001, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.reconcile.validator;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcileResult;
import org.eclipse.jface.text.reconciler.IReconcileStep;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.wst.sse.core.utils.StringUtils;
import org.eclipse.wst.sse.ui.internal.IReleasable;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.reconcile.DocumentAdapter;
import org.eclipse.wst.sse.ui.internal.reconcile.ReconcileAnnotationKey;
import org.eclipse.wst.sse.ui.internal.reconcile.StructuredTextReconcilingStrategy;
import org.eclipse.wst.sse.ui.internal.reconcile.TemporaryAnnotation;
import org.eclipse.wst.validation.ValidationFramework;
import org.eclipse.wst.validation.Validator;
import org.eclipse.wst.validation.internal.provisional.core.IValidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Special validator strategy. Runs validator steps contributed via the
 * <code>org.eclipse.wst.sse.ui.extensions.sourcevalidation</code> extension point
 * 
 * @author pavery
 */
public class ValidatorStrategy extends StructuredTextReconcilingStrategy {

  private static final boolean DEBUG_VALIDATION_CAPABLE_BUT_DISABLED = Boolean.valueOf(
      Platform.getDebugOption("org.eclipse.wst.sse.ui/debug/reconcilerValidatorEnablement")).booleanValue();
  private static final boolean DEBUG_VALIDATION_UNSUPPORTED = Boolean.valueOf(
      Platform.getDebugOption("org.eclipse.wst.sse.ui/debug/reconcilerValidatorSupported")).booleanValue();
  private static final Object NO_FILE = new Object();

  private String[] fContentTypeIds = null;
  private List fMetaData = null;
  /** validator id (as declared in ext point) -> ReconcileStepForValidator * */
  private HashMap fVidToVStepMap = null;

  /*
   * List of ValidatorMetaDatas of total scope validators that have been run since beginProcessing()
   * was called.
   */
  private List fTotalScopeValidatorsAlreadyRun = new ArrayList();

  /*
   * Whether the Validation Framework has indicated that validation is suspended for the current
   * resource
   */
  private boolean fValidatorsSuspended = false;
  private Object fFile;
  private boolean fIsCancelled = false;

  public ValidatorStrategy(ISourceViewer sourceViewer, String contentType) {
    super(sourceViewer);
    fMetaData = new ArrayList();
    fContentTypeIds = calculateParentContentTypeIds(contentType);
    fVidToVStepMap = new HashMap();
  }

  public void addValidatorMetaData(ValidatorMetaData vmd) {
    fMetaData.add(vmd);
  }

  public void beginProcessing() {
    fTotalScopeValidatorsAlreadyRun.clear();
  }

  /**
   * The content type passed in should be the most specific one. TODO: This exact method is also in
   * ValidatorMetaData. Should be in a common place.
   * 
   * @param contentType
   * @return
   */
  private String[] calculateParentContentTypeIds(String contentTypeId) {

    Set parentTypes = new HashSet();

    IContentTypeManager ctManager = Platform.getContentTypeManager();
    IContentType ct = ctManager.getContentType(contentTypeId);
    String id = contentTypeId;

    while (ct != null && id != null) {

      parentTypes.add(id);
      ct = ctManager.getContentType(id);
      if (ct != null) {
        IContentType baseType = ct.getBaseType();
        id = (baseType != null) ? baseType.getId() : null;
      }
    }
    return (String[]) parentTypes.toArray(new String[parentTypes.size()]);
  }

  protected boolean canHandlePartition(String partitionType) {
    ValidatorMetaData vmd = null;
    for (int i = 0; i < fMetaData.size(); i++) {
      vmd = (ValidatorMetaData) fMetaData.get(i);
      if (vmd.canHandlePartitionType(getContentTypeIds(), partitionType))
        return true;
    }
    return false;
  }

  protected boolean containsStep(IReconcileStep step) {
    return fVidToVStepMap.containsValue(step);
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.provisional.reconcile.AbstractStructuredTextReconcilingStrategy#createReconcileSteps()
   */
  public void createReconcileSteps() {
    // do nothing, steps are created
  }

  public void endProcessing() {
    fTotalScopeValidatorsAlreadyRun.clear();
  }

  /**
   * All content types on which this ValidatorStrategy can run
   * 
   * @return
   */
  public String[] getContentTypeIds() {
    return fContentTypeIds;
  }

  /**
   * @param tr Partition of the region to reconcile.
   * @param dr Dirty region representation of the typed region
   */
  public void reconcile(ITypedRegion tr, DirtyRegion dr) {
    /*
     * Abort if no workspace file is known (new validation framework does not support that scenario)
     * or no validators have been specified
     */
    if (isCanceled() || fMetaData.isEmpty() || fValidatorsSuspended)
      return;

    IDocument doc = getDocument();
    // for external files, this can be null
    if (doc == null)
      return;

    String partitionType = tr.getType();

    ValidatorMetaData vmd = null;
    List annotationsToAdd = new ArrayList();
    List stepsRanOnThisDirtyRegion = new ArrayList(1);

    /*
     * Keep track of the disabled validators by source id for the V2 validators.
     */
    Set disabledValsBySourceId = new HashSet(20);

    /*
     * Keep track of the disabled validators by class id for the v1 validators.
     */
    Set disabledValsByClass = new HashSet(20);
    IFile file = getFile();
    if (file != null) {
      if (!file.isAccessible())
        return;

      Collection disabledValidators = null;
      try {
        /*
         * Take extra care when calling this external code, as it can indirectly cause bundles to
         * start
         */
        disabledValidators = ValidationFramework.getDefault().getDisabledValidatorsFor(file);
      } catch (Exception e) {
        Logger.logException(e);
      }

      if (disabledValidators != null) {
        for (Iterator it = disabledValidators.iterator(); it.hasNext();) {
          Validator v = (Validator) it.next();
          Validator.V1 v1 = null;
          try {
            v1 = v.asV1Validator();
          } catch (Exception e) {
            Logger.logException(e);
          }
          if (v1 != null)
            disabledValsByClass.add(v1.getId());
          // not a V1 validator
          else if (v.getSourceId() != null) {
            //could be more then one sourceid per batch validator
            String[] sourceIDs = StringUtils.unpack(v.getSourceId());
            disabledValsBySourceId.addAll(Arrays.asList(sourceIDs));
          }
        }
      }
    }

    /*
     * Loop through all of the relevant validator meta data to find supporting validators for this
     * partition type. Don't check this.canHandlePartition() before-hand since it just loops through
     * and calls vmd.canHandlePartitionType()...which we're already doing here anyway to find the
     * right vmd.
     */
    for (int i = 0; i < fMetaData.size() && !isCanceled(); i++) {
      vmd = (ValidatorMetaData) fMetaData.get(i);
      if (vmd.canHandlePartitionType(getContentTypeIds(), partitionType)) {
        /*
         * Check if validator is enabled according to validation preferences before attempting to
         * create/use it
         */
        if (!disabledValsBySourceId.contains(vmd.getValidatorId())
            && !disabledValsByClass.contains(vmd.getValidatorClass())) {
          if (DEBUG_VALIDATION_UNSUPPORTED) {
            Logger.log(Logger.INFO, "Source validator " + vmd.getValidatorId()
                + " handling (content types:[" + StringUtils.pack(getContentTypeIds())
                + "] partition type:" + partitionType);
          }
          int validatorScope = vmd.getValidatorScope();
          ReconcileStepForValidator validatorStep = null;
          // get step for partition type
          Object o = fVidToVStepMap.get(vmd.getValidatorId());
          if (o != null) {
            validatorStep = (ReconcileStepForValidator) o;
          } else {
            // if doesn't exist, create one
            IValidator validator = vmd.createValidator();

            validatorStep = new ReconcileStepForValidator(validator, validatorScope);
            validatorStep.setInputModel(new DocumentAdapter(doc));

            fVidToVStepMap.put(vmd.getValidatorId(), validatorStep);
          }

          if (!fTotalScopeValidatorsAlreadyRun.contains(vmd) && !fIsCancelled) {
            annotationsToAdd.addAll(Arrays.asList(validatorStep.reconcile(dr, dr)));
            stepsRanOnThisDirtyRegion.add(validatorStep);

            if (validatorScope == ReconcileAnnotationKey.TOTAL) {
              // mark this validator as "run"
              fTotalScopeValidatorsAlreadyRun.add(vmd);
            }
          }
        } else if (DEBUG_VALIDATION_CAPABLE_BUT_DISABLED) {
          String message = "Source validator able (id:" + vmd.getValidatorId() + " class:"
              + vmd.getValidatorClass() + " but skipped because it was reported as disabled";
          Logger.log(Logger.INFO, message);
        }
      } else if (DEBUG_VALIDATION_UNSUPPORTED) {
        Logger.log(Logger.INFO, "Source validator " + vmd.getValidatorId()
            + " can not handle (content types:[" + StringUtils.pack(getContentTypeIds())
            + "] partition type:" + partitionType);
      }
    }

    TemporaryAnnotation[] annotationsToRemove = getAnnotationsToRemove(dr,
        stepsRanOnThisDirtyRegion);
    if (annotationsToRemove.length + annotationsToAdd.size() > 0 && !fIsCancelled)
      smartProcess(
          annotationsToRemove,
          (IReconcileResult[]) annotationsToAdd.toArray(new IReconcileResult[annotationsToAdd.size()]));
  }

  public void release() {
    super.release();
    fIsCancelled = true;
    Iterator it = fVidToVStepMap.values().iterator();
    IReconcileStep step = null;
    while (it.hasNext()) {
      step = (IReconcileStep) it.next();
      if (step instanceof IReleasable)
        ((IReleasable) step).release();
    }
    fFile = null;
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.reconcile.AbstractStructuredTextReconcilingStrategy#setDocument(org.eclipse.jface.text.IDocument)
   */
  public void setDocument(IDocument document) {
    super.setDocument(document);

    fFile = null;

    try {
      fValidatorsSuspended = false;
      if (document != null) {
        IFile file = getFile();
        if (file != null) {
          // Validation is suspended for this resource, do nothing
          fValidatorsSuspended = !file.isAccessible()
              || ValidationFramework.getDefault().isSuspended(file.getProject())
              || ValidationFramework.getDefault().getProjectSettings(file.getProject()).getSuspend();
        }
      }
    } catch (Exception e) {
      fValidatorsSuspended = true;
      Logger.logException(e);
    }

    // validator steps are in "fVIdToVStepMap" (as opposed to fFirstStep >
    // next step etc...)
    Iterator it = fVidToVStepMap.values().iterator();
    IReconcileStep step = null;
    while (it.hasNext()) {
      step = (IReconcileStep) it.next();
      step.setInputModel(new DocumentAdapter(document));
    }
  }

  /**
   * Gets IFile from current document
   * 
   * @return IFile the IFile, null if no such file exists
   */
  private IFile getFile() {
    if (fFile == null) {
      fFile = NO_FILE;
      ITextFileBuffer buffer = FileBuffers.getTextFileBufferManager().getTextFileBuffer(
          getDocument());
      if (buffer != null && buffer.getLocation() != null) {
        IPath path = buffer.getLocation();
        if (path.segmentCount() > 1) {
          IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
          if (file.isAccessible()) {
            fFile = file;
          }
        }
      }
    }

    if (fFile != NO_FILE)
      return (IFile) fFile;
    return null;
  }
}
