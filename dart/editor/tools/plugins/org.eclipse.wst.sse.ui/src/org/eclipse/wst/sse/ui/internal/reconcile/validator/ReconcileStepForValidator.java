/*******************************************************************************
 * Copyright (c) 2001, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.reconcile.validator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilableModel;
import org.eclipse.jface.text.reconciler.IReconcileResult;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.reconcile.DocumentAdapter;
import org.eclipse.wst.sse.ui.internal.reconcile.IReconcileAnnotationKey;
import org.eclipse.wst.sse.ui.internal.reconcile.ReconcileAnnotationKey;
import org.eclipse.wst.sse.ui.internal.reconcile.StructuredReconcileStep;
import org.eclipse.wst.sse.ui.internal.reconcile.TemporaryAnnotation;
import org.eclipse.wst.validation.internal.provisional.core.IMessage;
import org.eclipse.wst.validation.internal.provisional.core.IValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * A reconcile step for an IValidator for reconcile. Used the reconcile framework to create
 * TemporaryAnnotations from the validator messages.
 * 
 * @author pavery
 */
public class ReconcileStepForValidator extends StructuredReconcileStep {

  /** debug flag */
  protected static final boolean DEBUG;
  static {
    String value = Platform.getDebugOption("org.eclipse.wst.sse.ui/debug/reconcilerjob"); //$NON-NLS-1$
    DEBUG = value != null && value.equalsIgnoreCase("true"); //$NON-NLS-1$
  }

  private final IReconcileResult[] EMPTY_RECONCILE_RESULT_SET = new IReconcileResult[0];
  private IncrementalHelper fHelper = null;
  private IncrementalReporter fReporter = null;

  /**
   * Declared scope of this validator, either ReconcileAnnotationKey.TOTAL or
   * ReconcileAnnotationKey.PARTIAL
   */
  private int fScope = -1;
  private IValidator fValidator = null;
  private final String QUICKASSISTPROCESSOR = IQuickAssistProcessor.class.getName();

  public ReconcileStepForValidator(IValidator v, int scope) {
    super();

    if (v == null)
      throw new IllegalArgumentException("validator cannot be null"); //$NON-NLS-1$

    fValidator = v;
    fScope = scope;
  }

  /**
   * Converts a map of IValidatorForReconcile to List to annotations based on those messages
   * 
   * @param messages
   * @return
   */
  // protected IReconcileResult[] createAnnotations(List messageList) {
  // List annotations = new ArrayList();
  // for (int i = 0; i < messageList.size(); i++) {
  // IMessage validationMessage = (IMessage) messageList.get(i);
  //
  // int offset = validationMessage.getOffset();
  //
  // if (offset < 0)
  // continue;
  //
  // String messageText = null;
  // try {
  // messageText =
  // validationMessage.getText(validationMessage.getClass().getClassLoader());
  // }
  // catch (Exception t) {
  // Logger.logException("exception reporting message from validator", t);
  // //$NON-NLS-1$
  // continue;
  // }
  //			
  // String type = getSeverity(validationMessage);
  // // this position seems like it would be possibly be the wrong
  // // length
  // int length = validationMessage.getLength();
  // if (length >= 0) {
  // Position p = new Position(offset, length);
  // ReconcileAnnotationKey key = createKey(getPartitionType(getDocument(),
  // offset), getScope());
  // annotations.add(new TemporaryAnnotation(p, type, messageText, key));
  // }
  // }
  //
  // return (IReconcileResult[]) annotations.toArray(new
  // IReconcileResult[annotations.size()]);
  // }
  /**
   * Converts a map of IValidatorForReconcile to List to annotations based on those messages
   * 
   * @param messages
   * @return
   */
  protected IReconcileResult[] createAnnotations(AnnotationInfo[] infos) {

    List annotations = new ArrayList();
    for (int i = 0; i < infos.length; i++) {

      AnnotationInfo info = infos[i];

      IMessage validationMessage = info.getMessage();
      int offset = validationMessage.getOffset();
      if (offset < 0)
        continue;

      String messageText = null;
      try {
        messageText = validationMessage.getText(validationMessage.getClass().getClassLoader());
      } catch (Exception t) {
        Logger.logException("exception reporting message from validator", t); //$NON-NLS-1$
        continue;
      }
      String type = getSeverity(validationMessage);
      // this position seems like it would be possibly be the wrong
      // length
      int length = validationMessage.getLength();
      if (length >= 0) {

        Position p = new Position(offset, length);
        ReconcileAnnotationKey key = createKey(getPartitionType(getDocument(), offset), getScope());

        // create an annotation w/ problem ID and fix info
        TemporaryAnnotation annotation = new TemporaryAnnotation(p, type, messageText, key);
        Object extraInfo = info.getAdditionalFixInfo();
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=170988
        // add quick fix information
        if (extraInfo == null) {
          extraInfo = validationMessage.getAttribute(QUICKASSISTPROCESSOR);
        }
        annotation.setAdditionalFixInfo(extraInfo);
        annotation.setAttributes(validationMessage.getAttributes());
        annotations.add(annotation);
      }
    }
    return (IReconcileResult[]) annotations.toArray(new IReconcileResult[annotations.size()]);
  }

  private String getSeverity(IMessage validationMessage) {
    String type = TemporaryAnnotation.ANNOT_INFO;
    switch (validationMessage.getSeverity()) {
      case IMessage.HIGH_SEVERITY:
        type = TemporaryAnnotation.ANNOT_ERROR;
        break;
      case IMessage.NORMAL_SEVERITY:
        type = TemporaryAnnotation.ANNOT_WARNING;
        break;
      case IMessage.LOW_SEVERITY:
        type = TemporaryAnnotation.ANNOT_WARNING;
        break;
      case IMessage.ERROR_AND_WARNING:
        type = TemporaryAnnotation.ANNOT_WARNING;
        break;
    }
    return type;
  }

  private IFile getFile() {
    IModelManager modelManager = StructuredModelManager.getModelManager();
    IStructuredModel model = null;
    IFile file = null;
    if (modelManager != null) {
      try {
        model = modelManager.getExistingModelForRead(getDocument());
        if (model != null) {
          String baseLocation = model.getBaseLocation();
          // The baseLocation may be a path on disk or relative to the
          // workspace root. Don't translate on-disk paths to
          // in-workspace resources.
          IPath basePath = new Path(baseLocation);
          if (basePath.segmentCount() > 1 && !basePath.toFile().exists()) {
            file = ResourcesPlugin.getWorkspace().getRoot().getFile(basePath);
          }
        }
      } finally {
        if (model != null) {
          model.releaseFromRead();
        }
      }
    }
    return file;
  }

  private IncrementalHelper getHelper(IProject project) {
    if (fHelper == null)
      fHelper = new IncrementalHelper(getDocument(), project);
    return fHelper;
  }

  private IncrementalReporter getReporter() {
    if (fReporter == null)
      fReporter = new IncrementalReporter(getProgressMonitor());
    return fReporter;
  }

  /**
   * If this validator is partial or total
   * 
   * @return
   */
  public int getScope() {
    return fScope;
  }

  public void initialReconcile() {
    // do nothing
  }

  protected IReconcileResult[] reconcileModel(DirtyRegion dirtyRegion, IRegion subRegion) {
    if (DEBUG)
      System.out.println("[trace reconciler] > reconciling model in VALIDATOR step w/ dirty region: [" + dirtyRegion.getText() + "]"); //$NON-NLS-1$ //$NON-NLS-2$

    IReconcileResult[] results = EMPTY_RECONCILE_RESULT_SET;
    if (dirtyRegion != null) {
      try {
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=247714
        if (fValidator instanceof ISourceValidator && getScope() == ReconcileAnnotationKey.PARTIAL) {
          results = validate(dirtyRegion, subRegion);
        } else {
          results = validate();
        }
      } catch (Exception ex) {
        Logger.logException("EXEPTION IN RECONCILE STEP FOR VALIDATOR", ex); //$NON-NLS-1$
      }
    }

    if (DEBUG)
      System.out.println("[trace reconciler] > VALIDATOR step done"); //$NON-NLS-1$

    return results;
  }

  public String toString() {
    StringBuffer debugString = new StringBuffer("ValidatorStep: "); //$NON-NLS-1$
    if (fValidator != null)
      debugString.append(fValidator.toString());
    return debugString.toString();
  }

  protected IReconcileResult[] validate() {
    IReconcileResult[] results = EMPTY_RECONCILE_RESULT_SET;

    IFile file = getFile();
    IncrementalReporter reporter = null;

    try {
      IncrementalHelper helper = getHelper(file != null ? file.getProject() : null);

      if (file != null && file.isAccessible()) {
        helper.setURI(file.getFullPath().toString());
      } else {
        String uri = getURI();
        if (uri != null) {
          helper.setURI(uri);
        }
      }

      reporter = getReporter();
      fValidator.validate(helper, reporter);

      // results = createAnnotations(reporter.getMessages());
      results = createAnnotations(reporter.getAnnotationInfo());
      reporter.removeAllMessages(fValidator);

    } catch (Exception e) {
      Logger.logException(e);
    } finally {
      fValidator.cleanup(reporter);
    }
    return results;
  }

  /**
   * @return
   */
  private String getURI() {
    IStructuredModel model = null;
    try {
      model = StructuredModelManager.getModelManager().getExistingModelForRead(getDocument());
      if (model != null && !(IModelManager.UNMANAGED_MODEL.equals(model.getBaseLocation()))) {
        return model.getBaseLocation();
      }
    } finally {
      if (model != null) {
        model.releaseFromRead();
      }
    }
    return null;
  }

  public void setInputModel(IReconcilableModel inputModel) {
    if (inputModel instanceof DocumentAdapter) {
      IDocument document = ((DocumentAdapter) inputModel).getDocument();
      if (document != null) {
        if (fValidator instanceof ISourceValidator) {
          // notify that document connecting
          ((ISourceValidator) fValidator).connect(document);
        }
      }
    }
    super.setInputModel(inputModel);
  }

  public void release() {
    if (fValidator instanceof ISourceValidator) {
      IDocument document = getDocument();
      if (document != null) {
        // notify that document disconnecting
        ((ISourceValidator) fValidator).disconnect(document);
      }
      fValidator.cleanup(getReporter());
    }
    super.release();
  }

  protected IReconcileResult[] validate(DirtyRegion dirtyRegion, IRegion subRegion) {
    IReconcileResult[] results = EMPTY_RECONCILE_RESULT_SET;

    IFile file = getFile();

    try {
      IncrementalHelper helper = getHelper(file != null ? file.getProject() : null);
      /*
       * Setting the URI isn't necessary for all source validators, we can still continue without it
       */
      if (file != null && file.exists()) {
        helper.setURI(file.getFullPath().toString());
      }

      if (fValidator instanceof ISourceValidator) {
        IncrementalReporter reporter = getReporter();
        if (getScope() == IReconcileAnnotationKey.PARTIAL)
          ((ISourceValidator) fValidator).validate(dirtyRegion, helper, reporter);
        else
          ((ISourceValidator) fValidator).validate(new Region(0, getDocument().getLength()),
              helper, reporter);
        /*
         * call IValidator.cleanup() during release() because this validator might be called again
         * on a different region
         */
        // results = createAnnotations(reporter.getMessages());
        results = createAnnotations(reporter.getAnnotationInfo());
        reporter.removeAllMessages(fValidator);
      }
    } catch (Exception e) {
      Logger.logException(e);
    }
    return results;
  }
}
