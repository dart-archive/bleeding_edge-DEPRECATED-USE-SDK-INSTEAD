/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation David Carver (Intalio) - bug 307323 - remove extraneous call to spell check
 * strategy
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.reconcile;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.wst.sse.core.internal.ltk.parser.RegionParser;
import org.eclipse.wst.sse.core.internal.ltk.parser.StructuredDocumentRegionHandler;
import org.eclipse.wst.sse.core.internal.ltk.parser.StructuredDocumentRegionParser;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.ui.internal.ExtendedConfigurationBuilder;
import org.eclipse.wst.sse.ui.internal.IReleasable;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;
import org.eclipse.wst.sse.ui.internal.projection.AbstractStructuredFoldingStrategy;
import org.eclipse.wst.sse.ui.internal.provisional.preferences.CommonEditorPreferenceNames;
import org.eclipse.wst.sse.ui.internal.reconcile.validator.ValidatorBuilder;
import org.eclipse.wst.sse.ui.internal.reconcile.validator.ValidatorMetaData;
import org.eclipse.wst.sse.ui.internal.reconcile.validator.ValidatorStrategy;
import org.eclipse.wst.sse.ui.internal.spelling.SpellcheckStrategy;
import org.eclipse.wst.sse.ui.reconcile.ISourceReconcilingListener;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adds to DirtyRegionProcessor Job: - IDocumentListener - ValidatorStrategy - Text viewer(dispose,
 * input changed) listeners. - default, spelling, and validator strategies - DirtyRegion processing
 * logic.
 */
public class DocumentRegionProcessor extends DirtyRegionProcessor {
  private static final boolean DEBUG_VALIDATORS = Boolean.TRUE.toString().equalsIgnoreCase(
      Platform.getDebugOption("org.eclipse.wst.sse.ui/debug/reconcilerValidators")); //$NON-NLS-1$

  /**
   * Marks the entire document dirty when a parser-level change is notified that might affect the
   * entire document
   */
  class DirtyRegionParseHandler implements StructuredDocumentRegionHandler {
    public void nodeParsed(IStructuredDocumentRegion aCoreStructuredDocumentRegion) {
    }

    public void resetNodes() {
      setEntireDocumentDirty(getDocument());
    }
  }

  private DirtyRegionParseHandler fResetHandler = new DirtyRegionParseHandler();
  /**
   * A strategy to use the defined default Spelling service.
   */
  private IReconcilingStrategy fSpellcheckStrategy;

  /**
   * The strategy that runs validators contributed via
   * <code>org.eclipse.wst.sse.ui.extensions.sourcevalidation</code> extension point
   */
  private ValidatorStrategy fValidatorStrategy;

  private ISourceReconcilingListener[] fReconcileListeners = new ISourceReconcilingListener[0];

  private IReconcilingStrategy fSemanticHighlightingStrategy;

  /**
   * The folding strategy for this processor
   */
  private AbstractStructuredFoldingStrategy fFoldingStrategy;

  private final String SSE_UI_ID = "org.eclipse.wst.sse.ui"; //$NON-NLS-1$

  /**
   * true if as you type validation is enabled, false otherwise
   */
  private boolean fValidationEnabled;

  public void addReconcilingListener(ISourceReconcilingListener listener) {
    Set listeners = new HashSet(Arrays.asList(fReconcileListeners));
    listeners.add(listener);
    fReconcileListeners = (ISourceReconcilingListener[]) listeners.toArray(new ISourceReconcilingListener[listeners.size()]);
  }

  protected void beginProcessing() {
    super.beginProcessing();
    ValidatorStrategy validatorStrategy = getValidatorStrategy();
    if (validatorStrategy != null) {
      validatorStrategy.beginProcessing();
    }
    if ((getTextViewer() instanceof ISourceViewer)) {
      for (int i = 0; i < fReconcileListeners.length; i++) {
        fReconcileListeners[i].aboutToBeReconciled();
      }
    }
  }

  protected void endProcessing() {
    super.endProcessing();
    ValidatorStrategy validatorStrategy = getValidatorStrategy();
    if (validatorStrategy != null) {
      validatorStrategy.endProcessing();
    }
    /* single spell-check for everything to ensure that SpellingProblem offsets are correct */
    IReconcilingStrategy spellingStrategy = getSpellcheckStrategy();
    IDocument document = getDocument();
    if (spellingStrategy != null && document != null) {
      spellingStrategy.reconcile(new Region(0, document.getLength()));
    }

    IReconcilingStrategy semanticHighlightingStrategy = getSemanticHighlightingStrategy();
    if (semanticHighlightingStrategy != null && document != null) {
      semanticHighlightingStrategy.reconcile(new Region(0, document.getLength()));
    }

    if ((getTextViewer() instanceof ISourceViewer)) {
      ISourceViewer sourceViewer = (ISourceViewer) getTextViewer();
      IAnnotationModel annotationModel = sourceViewer.getAnnotationModel();
      for (int i = 0; i < fReconcileListeners.length; i++) {
        fReconcileListeners[i].reconciled(document, annotationModel, false,
            new NullProgressMonitor());
      }
    }
  }

  public void forceReconciling() {
    super.forceReconciling();
  }

  protected String getContentType(IDocument doc) {
    if (doc == null)
      return null;

    String contentTypeId = null;

    IContentType ct = null;
    try {
      IContentDescription desc = Platform.getContentTypeManager().getDescriptionFor(
          new StringReader(doc.get()), null, IContentDescription.ALL);
      if (desc != null) {
        ct = desc.getContentType();
        if (ct != null)
          contentTypeId = ct.getId();
      }
    } catch (IOException e) {
      // just bail
    }
    return contentTypeId;
  }

  protected IReconcilingStrategy getSpellcheckStrategy() {
    if (fSpellcheckStrategy == null && getDocument() != null) {
      String contentTypeId = getContentType(getDocument());
      if (contentTypeId == null) {
        contentTypeId = IContentTypeManager.CT_TEXT;
      }
      if (getTextViewer() instanceof ISourceViewer) {
        ISourceViewer viewer = (ISourceViewer) getTextViewer();
        fSpellcheckStrategy = new SpellcheckStrategy(viewer, contentTypeId);
        fSpellcheckStrategy.setDocument(getDocument());
      }
    }
    return fSpellcheckStrategy;
  }

  /**
   * <p>
   * Get the folding strategy for this processor. Retrieved from the extended configuration builder.
   * The processor chosen is set by the plugin.
   * </p>
   * <p>
   * EX:<br />
   * <code>&lt;extension point="org.eclipse.wst.sse.ui.editorConfiguration"&gt;<br />
   *  &lt;provisionalConfiguration<br />
   * 			type="foldingstrategy"<br />
   * 			class="org.eclipse.wst.xml.ui.internal.projection.XMLFoldingStrategy"<br />
   * 			target="org.eclipse.core.runtime.xml, org.eclipse.wst.xml.core.xmlsource" /&gt;<br />
   * 	&lt;/extension&gt;</code>
   * </p>
   * <p>
   * The type must be equal to <code>AbstractFoldingStrategy.ID</code> (AKA: foldingstrategy) and
   * the class must extend
   * <code>org.eclipse.wst.sse.ui.internal.projection.AbstractFoldingStrategy</code> and the target
   * must be a structured editor content type ID
   * </p>
   * 
   * @return the requested folding strategy or null if none can be found
   */
  protected IReconcilingStrategy getFoldingStrategy() {
    if (fFoldingStrategy == null && getDocument() != null) {
      String contentTypeId = getContentType(getDocument());
      if (contentTypeId == null) {
        contentTypeId = IContentTypeManager.CT_TEXT;
      }

      ITextViewer viewer = getTextViewer();
      if (viewer instanceof ProjectionViewer) {
        ExtendedConfigurationBuilder builder = ExtendedConfigurationBuilder.getInstance();

        IContentType type = Platform.getContentTypeManager().getContentType(contentTypeId);
        while (fFoldingStrategy == null && type != null) {
          fFoldingStrategy = (AbstractStructuredFoldingStrategy) builder.getConfiguration(
              AbstractStructuredFoldingStrategy.ID, type.getId());

          type = type.getBaseType();
        }

        if (fFoldingStrategy != null) {
          fFoldingStrategy.setViewer((ProjectionViewer) viewer);
          fFoldingStrategy.setDocument(getDocument());
        }
      }
    }

    return fFoldingStrategy;
  }

  /**
   * Enable or disable as you type validation. Typically set by a user preference
   * 
   * @param enable true to enable as you type validation, false to disable
   */
  public void setValidatorStrategyEnabled(boolean enable) {
    fValidationEnabled = enable;
  }

  /**
   * @return Returns the ValidatorStrategy.
   */
  protected ValidatorStrategy getValidatorStrategy() {
    ValidatorStrategy validatorStrategy = null;
    if (fValidatorStrategy == null && fValidationEnabled) {
      if (getTextViewer() instanceof ISourceViewer) {
        ISourceViewer viewer = (ISourceViewer) getTextViewer();
        String contentTypeId = null;

        IDocument doc = viewer.getDocument();
        contentTypeId = getContentType(doc);

        if (contentTypeId != null) {
          validatorStrategy = new ValidatorStrategy(viewer, contentTypeId);
          ValidatorBuilder vBuilder = new ValidatorBuilder();
          ValidatorMetaData[] vmds = vBuilder.getValidatorMetaData(SSE_UI_ID);
          List enabledValidators = new ArrayList(1);
          /* if any "must" handle this content type, just add them */
          boolean foundSpecificContentTypeValidators = false;
          for (int i = 0; i < vmds.length; i++) {
            if (vmds[i].mustHandleContentType(contentTypeId)) {
              if (DEBUG_VALIDATORS)
                Logger.log(Logger.INFO,
                    contentTypeId + " using specific validator " + vmds[i].getValidatorId()); //$NON-NLS-1$
              foundSpecificContentTypeValidators = true;
              enabledValidators.add(vmds[i]);
            }
          }
          if (!foundSpecificContentTypeValidators) {
            for (int i = 0; i < vmds.length; i++) {
              if (vmds[i].canHandleContentType(contentTypeId)) {
                if (DEBUG_VALIDATORS)
                  Logger.log(Logger.INFO, contentTypeId
                      + " using inherited(?) validator " + vmds[i].getValidatorId()); //$NON-NLS-1$
                enabledValidators.add(vmds[i]);
              }
            }
          }
          for (int i = 0; i < enabledValidators.size(); i++) {
            validatorStrategy.addValidatorMetaData((ValidatorMetaData) enabledValidators.get(i));
          }
        }
      }
      fValidatorStrategy = validatorStrategy;
    } else if (fValidatorStrategy != null && fValidationEnabled) {
      validatorStrategy = fValidatorStrategy;
    }
    return validatorStrategy;
  }

  public void setSemanticHighlightingStrategy(IReconcilingStrategy semanticHighlightingStrategy) {
    fSemanticHighlightingStrategy = semanticHighlightingStrategy;
    fSemanticHighlightingStrategy.setDocument(getDocument());
  }

  protected IReconcilingStrategy getSemanticHighlightingStrategy() {
    return fSemanticHighlightingStrategy;
  }

  /**
   * @param dirtyRegion
   */
  protected void process(DirtyRegion dirtyRegion) {
    if (!isInstalled() || isInRewriteSession() || dirtyRegion == null || getDocument() == null)
      return;

    super.process(dirtyRegion);

    ITypedRegion[] partitions = computePartitioning(dirtyRegion);

    // call the validator strategy once for each effected partition
    DirtyRegion dirty = null;
    for (int i = 0; i < partitions.length; i++) {
      dirty = createDirtyRegion(partitions[i], DirtyRegion.INSERT);

      // [source]validator (extension) for this partition
      if (getValidatorStrategy() != null) {
        getValidatorStrategy().reconcile(partitions[i], dirty);
      }
    }

    /*
     * if there is a folding strategy then reconcile it for the entire dirty region. NOTE: the
     * folding strategy does not care about the sub regions.
     */
    if (getFoldingStrategy() != null) {
      getFoldingStrategy().reconcile(dirtyRegion, null);
    }
  }

  public void removeReconcilingListener(ISourceReconcilingListener listener) {
    Set listeners = new HashSet(Arrays.asList(fReconcileListeners));
    listeners.remove(listener);
    fReconcileListeners = (ISourceReconcilingListener[]) listeners.toArray(new ISourceReconcilingListener[listeners.size()]);
  }

  public void setDocument(IDocument doc) {
    if (getDocument() instanceof IStructuredDocument) {
      RegionParser parser = ((IStructuredDocument) getDocument()).getParser();
      if (parser instanceof StructuredDocumentRegionParser) {
        ((StructuredDocumentRegionParser) parser).removeStructuredDocumentRegionHandler(fResetHandler);
      }
    }

    super.setDocument(doc);

    IReconcilingStrategy validatorStrategy = getValidatorStrategy();
    if (validatorStrategy != null) {
      validatorStrategy.setDocument(doc);
    }
    if (fSemanticHighlightingStrategy != null) {
      fSemanticHighlightingStrategy.setDocument(doc);
    }

    fSpellcheckStrategy = null;
    if (fFoldingStrategy != null) {
      fFoldingStrategy.uninstall();
    }
    fFoldingStrategy = null;

    if (getDocument() instanceof IStructuredDocument) {
      RegionParser parser = ((IStructuredDocument) doc).getParser();
      if (parser instanceof StructuredDocumentRegionParser) {
        ((StructuredDocumentRegionParser) parser).addStructuredDocumentRegionHandler(fResetHandler);
      }
    }
  }

  protected void setEntireDocumentDirty(IDocument document) {
    super.setEntireDocumentDirty(document);

    // make the entire document dirty
    // this also happens on a "save as"
    if (document != null && isInstalled() && fLastPartitions != null && document.getLength() == 0) {
      /**
       * https://bugs.eclipse.org/bugs/show_bug.cgi?id=199053 Process the strategies for the last
       * known-good partitions.
       */
      for (int i = 0; i < fLastPartitions.length; i++) {
        ValidatorStrategy validatorStrategy = getValidatorStrategy();
        if (validatorStrategy != null) {
          validatorStrategy.reconcile(fLastPartitions[i],
              createDirtyRegion(fLastPartitions[i], DirtyRegion.REMOVE));
        }
      }
      IReconcilingStrategy spellingStrategy = getSpellcheckStrategy();
      if (spellingStrategy != null) {
        spellingStrategy.reconcile(new Region(0, document.getLength()));
      }

      //if there is a folding strategy then reconcile it
      if (getFoldingStrategy() != null) {
        getFoldingStrategy().reconcile(new Region(0, document.getLength()));
      }
    }
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.reconcile.DirtyRegionProcessor#install(org.eclipse.jface.text.ITextViewer)
   */
  public void install(ITextViewer textViewer) {
    super.install(textViewer);

    //determine if validation is enabled
    this.fValidationEnabled = SSEUIPlugin.getInstance().getPreferenceStore().getBoolean(
        CommonEditorPreferenceNames.EVALUATE_TEMPORARY_PROBLEMS);
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.reconcile.DirtyRegionProcessor#uninstall()
   */
  public void uninstall() {
    if (isInstalled()) {

      IReconcilingStrategy validatorStrategy = getValidatorStrategy();

      if (validatorStrategy != null) {
        if (validatorStrategy instanceof IReleasable)
          ((IReleasable) validatorStrategy).release();
      }

      if (fSpellcheckStrategy != null) {
        fSpellcheckStrategy.setDocument(null);
        fSpellcheckStrategy = null;
      }

      fReconcileListeners = new ISourceReconcilingListener[0];

      if (getDocument() instanceof IStructuredDocument) {
        RegionParser parser = ((IStructuredDocument) getDocument()).getParser();
        if (parser instanceof StructuredDocumentRegionParser) {
          ((StructuredDocumentRegionParser) parser).removeStructuredDocumentRegionHandler(fResetHandler);
        }
      }
    }
    super.uninstall();
  }
}
