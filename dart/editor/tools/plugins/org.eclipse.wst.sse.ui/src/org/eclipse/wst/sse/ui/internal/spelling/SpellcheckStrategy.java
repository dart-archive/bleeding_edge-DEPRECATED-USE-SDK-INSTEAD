/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.spelling;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcileStep;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.IAnnotationModelExtension2;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingContext;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;
import org.eclipse.ui.texteditor.spelling.SpellingService;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.parser.ForeignRegion;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionCollection;
import org.eclipse.wst.sse.core.utils.StringUtils;
import org.eclipse.wst.sse.ui.internal.ExtendedConfigurationBuilder;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.reconcile.ReconcileAnnotationKey;
import org.eclipse.wst.sse.ui.internal.reconcile.StructuredReconcileStep;
import org.eclipse.wst.sse.ui.internal.reconcile.StructuredTextReconcilingStrategy;
import org.eclipse.wst.sse.ui.internal.reconcile.TemporaryAnnotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A reconciling strategy that queries the SpellingService using its default engine. Results are
 * show as temporary annotations.
 * 
 * @since 1.1
 */
public class SpellcheckStrategy extends StructuredTextReconcilingStrategy {

  class SpellCheckPreferenceListener implements IPropertyChangeListener {
    private boolean isInterestingProperty(Object property) {
      return SpellingService.PREFERENCE_SPELLING_ENABLED.equals(property)
          || SpellingService.PREFERENCE_SPELLING_ENGINE.equals(property);
    }

    public void propertyChange(PropertyChangeEvent event) {
      if (isInterestingProperty(event.getProperty())) {
        if (event.getOldValue() == null || event.getNewValue() == null
            || !event.getNewValue().equals(event.getOldValue())) {
          reconcile();
        }
      }
    }
  }

  private class SpellingProblemCollector implements ISpellingProblemCollector {
    List annotations = new ArrayList();

    public void accept(SpellingProblem problem) {
      if (isInterestingProblem(problem)) {
        TemporaryAnnotation annotation = new TemporaryAnnotation(new Position(problem.getOffset(),
            problem.getLength()), SpellingAnnotation.TYPE, problem.getMessage(),
            fReconcileAnnotationKey);

        SpellingQuickAssistProcessor quickAssistProcessor = new SpellingQuickAssistProcessor();
        quickAssistProcessor.setSpellingProblem(problem);
        annotation.setAdditionalFixInfo(quickAssistProcessor);
        annotations.add(annotation);
        if (_DEBUG_SPELLING_PROBLEMS) {
          Logger.log(Logger.INFO, problem.getMessage());
        }
      }
    }

    public void beginCollecting() {
    }

    void clear() {
      annotations.clear();
    }

    public void endCollecting() {
    }

    Annotation[] getAnnotations() {
      return (Annotation[]) annotations.toArray(new Annotation[annotations.size()]);
    }
  }

  private static final boolean _DEBUG_SPELLING = Boolean.valueOf(
      Platform.getDebugOption("org.eclipse.wst.sse.ui/debug/reconcilerSpelling")).booleanValue(); //$NON-NLS-1$
  private static final boolean _DEBUG_SPELLING_PROBLEMS = Boolean.valueOf(
      Platform.getDebugOption("org.eclipse.wst.sse.ui/debug/reconcilerSpelling/showProblems")).booleanValue(); //$NON-NLS-1$

  private static final String EXTENDED_BUILDER_TYPE_CONTEXTS = "spellingregions"; //$NON-NLS-1$
  private static final String KEY_CONTENT_TYPE = "org.eclipse.wst.sse.ui.temp.spelling"; //$NON-NLS-1$

  private String fContentTypeId = null;

  private SpellingProblemCollector fProblemCollector = new SpellingProblemCollector();

  IStructuredModel structuredModel = null;

  /*
   * Keying our Temporary Annotations based on the partition doesn't help this strategy to only
   * remove its own TemporaryAnnotations since it's possibly run on all partitions. Instead, set the
   * key to use an arbitrary partition type that we can check for using our own implementation of
   * getAnnotationsToRemove(DirtyRegion).
   */
  ReconcileAnnotationKey fReconcileAnnotationKey;

  private IPropertyChangeListener fSpellCheckPreferenceListener;

  private SpellingContext fSpellingContext;

  private String[] fSupportedTextRegionContexts;
  private IReconcileStep fSpellingStep = new StructuredReconcileStep() {
  };

  public SpellcheckStrategy(ISourceViewer viewer, String contentTypeId) {
    super(viewer);
    fContentTypeId = contentTypeId;

    fSpellingContext = new SpellingContext();
    IContentType contentType = Platform.getContentTypeManager().getContentType(fContentTypeId);
    fSpellingContext.setContentType(contentType);
    fReconcileAnnotationKey = new ReconcileAnnotationKey(fSpellingStep, KEY_CONTENT_TYPE,
        ReconcileAnnotationKey.PARTIAL);

    /**
     * Inherit spelling region rules
     */
    List contexts = new ArrayList();
    IContentType testType = contentType;
    while (testType != null) {
      String[] textRegionContexts = ExtendedConfigurationBuilder.getInstance().getDefinitions(
          EXTENDED_BUILDER_TYPE_CONTEXTS, testType.getId());
      for (int j = 0; j < textRegionContexts.length; j++) {
        contexts.addAll(Arrays.asList(StringUtils.unpack(textRegionContexts[j])));
      }
      testType = testType.getBaseType();
    }
    fSupportedTextRegionContexts = (String[]) contexts.toArray(new String[contexts.size()]);

    fSpellCheckPreferenceListener = new SpellCheckPreferenceListener();
  }

  protected boolean containsStep(IReconcileStep step) {
    return fSpellingStep.equals(step);
  }

  public void createReconcileSteps() {

  }

  private TemporaryAnnotation[] getSpellingAnnotationsToRemove(IRegion region) {
    List toRemove = new ArrayList();
    IAnnotationModel annotationModel = getAnnotationModel();
    // can be null when closing the editor
    if (annotationModel != null) {
      Iterator i = null;
      boolean annotationOverlaps = false;
      if (annotationModel instanceof IAnnotationModelExtension2) {
        i = ((IAnnotationModelExtension2) annotationModel).getAnnotationIterator(
            region.getOffset(), region.getLength(), true, true);
        annotationOverlaps = true;
      } else {
        i = annotationModel.getAnnotationIterator();
      }

      while (i.hasNext()) {
        Object obj = i.next();
        if (!(obj instanceof TemporaryAnnotation))
          continue;

        TemporaryAnnotation annotation = (TemporaryAnnotation) obj;
        ReconcileAnnotationKey key = (ReconcileAnnotationKey) annotation.getKey();

        // then if this strategy knows how to add/remove this
        // partition type
        if (key != null && key.equals(fReconcileAnnotationKey)) {
          if (key.getScope() == ReconcileAnnotationKey.PARTIAL
              && (annotationOverlaps || annotation.getPosition().overlapsWith(region.getOffset(),
                  region.getLength()))) {
            toRemove.add(annotation);
          } else if (key.getScope() == ReconcileAnnotationKey.TOTAL) {
            toRemove.add(annotation);
          }
        }
      }
    }

    return (TemporaryAnnotation[]) toRemove.toArray(new TemporaryAnnotation[toRemove.size()]);
  }

  /**
   * Judge whether a spelling problem is "interesting". Accept any regions that are explicitly
   * allowed, and since valid prose areas are rarely in a complicated document region, accept any
   * document region with more than one text region and reject any document regions containing
   * foreign text regions.
   * 
   * @param problem a SpellingProblem
   * @return whether the collector should accept the given SpellingProblem
   */
  protected boolean isInterestingProblem(SpellingProblem problem) {
    IDocument document = getDocument();
    if (document instanceof IStructuredDocument) {
      /*
       * If the error is in a read-only section, ignore it. The user won't be able to correct it.
       */
      if (((IStructuredDocument) document).containsReadOnly(problem.getOffset(),
          problem.getLength()))
        return false;

      IStructuredDocumentRegion documentRegion = ((IStructuredDocument) document).getRegionAtCharacterOffset(problem.getOffset());
      if (documentRegion != null) {
        ITextRegion textRegion = documentRegion.getRegionAtCharacterOffset(problem.getOffset());
        //if the region is not null, and is a supported context and is not a collection of regions,
        //	and it should be spell-checked, then spell check it.
        if (textRegion != null && isSupportedContext(textRegion.getType())
            && !(textRegion instanceof ITextRegionCollection)
            && shouldSpellcheck(problem.getOffset())) {
          return true;
        }
        if (documentRegion.getFirstRegion() instanceof ForeignRegion)
          return false;
//				[192572] Simple regions were being spellchecked just for the sake of them being simple
//				if (documentRegion.getRegions().size() == 1)
//					return true;
        return false;
      }
    }
    return true;
  }

  private boolean isSupportedContext(String type) {
    boolean isSupported = false;
    if (fSupportedTextRegionContexts.length > 0) {
      for (int i = 0; i < fSupportedTextRegionContexts.length; i++) {
        if (type.equals(fSupportedTextRegionContexts[i])) {
          isSupported = true;
          break;
        }
      }
    } else {
      isSupported = true;
    }
    return isSupported;
  }

  public void reconcile() {
    IDocument document = getDocument();
    if (document != null) {
      IAnnotationModel annotationModel = getAnnotationModel();
      if (annotationModel != null) {
        IRegion documentRegion = new Region(0, document.getLength());
        spellCheck(documentRegion, documentRegion, annotationModel);
      }
    }
  }

  /**
   * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse.jface.text.reconciler.DirtyRegion,
   *      org.eclipse.jface.text.IRegion)
   */
  public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
    if (isCanceled())
      return;

    IAnnotationModel annotationModel = getAnnotationModel();

    IDocument document = getDocument();
    if (document != null) {
      long time0 = 0;
      if (_DEBUG_SPELLING) {
        time0 = System.currentTimeMillis();
      }
      /**
       * Apparently the default spelling engine has noticeable overhead when called multiple times
       * in rapid succession. It's faster to check the entire dirty region at once since we know
       * that we're not differentiating by partition.
       * https://bugs.eclipse.org/bugs/show_bug.cgi?id=192530
       */
      if (_DEBUG_SPELLING) {
        Logger.log(Logger.INFO,
            "Spell Checking [" + dirtyRegion.getOffset() + ":" + dirtyRegion.getLength() + "] : "
                + (System.currentTimeMillis() - time0));
      }
      if (annotationModel != null) {
        spellCheck(dirtyRegion, dirtyRegion, annotationModel);
      }
    }
  }

  private void spellCheck(IRegion dirtyRegion, IRegion regionToBeChecked,
      IAnnotationModel annotationModel) {
    if (annotationModel == null)
      return;

    TemporaryAnnotation[] annotationsToRemove;
    Annotation[] annotationsToAdd;
    annotationsToRemove = getSpellingAnnotationsToRemove(regionToBeChecked);

    if (_DEBUG_SPELLING_PROBLEMS) {
      Logger.log(
          Logger.INFO,
          "Spell checking [" + regionToBeChecked.getOffset() + "-" + (regionToBeChecked.getOffset() + regionToBeChecked.getLength()) + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
    if (getDocument() != null) {
      try {
        EditorsUI.getSpellingService().check(getDocument(), new IRegion[] {regionToBeChecked},
            fSpellingContext, fProblemCollector, null);
      } finally {
        // corresponding "get" is in #shouldSpellCheck(int) 
        if (structuredModel != null) {
          structuredModel.releaseFromRead();
          structuredModel = null;
        }
      }
    }
    annotationsToAdd = fProblemCollector.getAnnotations();
    fProblemCollector.clear();

    if (annotationModel instanceof IAnnotationModelExtension) {
      IAnnotationModelExtension modelExtension = (IAnnotationModelExtension) annotationModel;
      Map annotationsToAddMap = new HashMap();
      for (int i = 0; i < annotationsToAdd.length; i++) {
        annotationsToAddMap.put(annotationsToAdd[i],
            ((TemporaryAnnotation) annotationsToAdd[i]).getPosition());
      }
      modelExtension.replaceAnnotations(annotationsToRemove, annotationsToAddMap);
    }

    else {
      for (int j = 0; j < annotationsToAdd.length; j++) {
        annotationModel.addAnnotation(annotationsToAdd[j],
            ((TemporaryAnnotation) annotationsToAdd[j]).getPosition());
      }
      for (int j = 0; j < annotationsToRemove.length; j++) {
        annotationModel.removeAnnotation(annotationsToRemove[j]);
      }
    }
  }

  /**
   * @param partition
   * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse.jface.text.IRegion)
   */

  public void reconcile(IRegion partition) {
    IDocument document = getDocument();
    if (document != null) {
      IAnnotationModel annotationModel = getAnnotationModel();
      if (annotationModel != null) {
        spellCheck(partition, partition, annotationModel);
      }
    }
  }

  public void setDocument(IDocument document) {
    if (getDocument() != null) {
      EditorsUI.getPreferenceStore().removePropertyChangeListener(fSpellCheckPreferenceListener);
    }

    super.setDocument(document);

    if (getDocument() != null) {
      EditorsUI.getPreferenceStore().addPropertyChangeListener(fSpellCheckPreferenceListener);
    }
  }

  /**
   * Decides if the given offset should be spell-checked using an <code>IAdapterFactory</code>
   * 
   * @param offset Decide if this offset should be spell-checked
   * @return <code>true</code> if the given <code>offset</code> should be spell-checked,
   *         <code>false</code> otherwise.
   */
  private boolean shouldSpellcheck(int offset) {
    boolean decision = true;

    if (structuredModel == null)
      structuredModel = StructuredModelManager.getModelManager().getExistingModelForRead(
          getDocument());

    if (structuredModel != null) {
      /*
       * use an an adapter factory to get a spell-check decision maker, and ask it if the offset
       * should be spell-checked. It is done this way so content type specific decisions can be made
       * without this plugin being aware of any content type specifics.
       */
      ISpellcheckDelegate delegate = (ISpellcheckDelegate) Platform.getAdapterManager().getAdapter(
          structuredModel, ISpellcheckDelegate.class);
      if (delegate != null) {
        decision = delegate.shouldSpellcheck(offset, structuredModel);
      }
    }

    return decision;
  }
}
