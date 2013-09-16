/*******************************************************************************
 * Copyright (c) 2001, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.hyperlink.IHyperlinkPresenter;
import org.eclipse.jface.text.hyperlink.MultipleHyperlinkPresenter;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.quickassist.QuickAssistAssistant;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredPartitioning;
import org.eclipse.wst.sse.core.internal.text.rules.StructuredTextPartitioner;
import org.eclipse.wst.sse.ui.contentassist.StructuredContentAssistProcessor;
import org.eclipse.wst.sse.ui.internal.ExtendedConfigurationBuilder;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;
import org.eclipse.wst.sse.ui.internal.StructuredTextAnnotationHover;
import org.eclipse.wst.sse.ui.internal.contentassist.StructuredContentAssistant;
import org.eclipse.wst.sse.ui.internal.correction.CompoundQuickAssistProcessor;
import org.eclipse.wst.sse.ui.internal.derived.HTMLTextPresenter;
import org.eclipse.wst.sse.ui.internal.preferences.EditorPreferenceNames;
import org.eclipse.wst.sse.ui.internal.provisional.style.LineStyleProvider;
import org.eclipse.wst.sse.ui.internal.provisional.style.ReconcilerHighlighter;
import org.eclipse.wst.sse.ui.internal.provisional.style.StructuredPresentationReconciler;
import org.eclipse.wst.sse.ui.internal.reconcile.StructuredRegionProcessor;
import org.eclipse.wst.sse.ui.internal.rules.StructuredDocumentDamagerRepairer;
import org.eclipse.wst.sse.ui.internal.taginfo.AnnotationHoverProcessor;
import org.eclipse.wst.sse.ui.internal.taginfo.BestMatchHover;
import org.eclipse.wst.sse.ui.internal.taginfo.ProblemAnnotationHoverProcessor;
import org.eclipse.wst.sse.ui.internal.taginfo.TextHoverManager;
import org.eclipse.wst.sse.ui.internal.util.EditorUtility;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the source viewer used by StructuredTextEditor.<br />
 * Note: While ISourceViewer is passed in for each get configuration, clients should create a new
 * viewer configuration instance for each instance of source viewer as some methods return the same
 * instance of an object, regardless of the sourceviewer.
 * <p>
 * Clients should subclass and override just those methods which must be specific to their needs.
 * </p>
 * 
 * @see org.eclipse.wst.sse.ui.StructuredTextEditor
 * @see org.eclipse.wst.sse.ui.internal.StructuredTextViewer
 * @since 1.0
 */
public class StructuredTextViewerConfiguration extends TextSourceViewerConfiguration {
  /**
   * One instance per configuration because creating a second assistant that is added to a viewer
   * can cause odd key-eating by the wrong one.
   */
  private StructuredContentAssistant fContentAssistant = null;

  /*
   * One instance per configuration because it's just like content assistant
   */
  private IQuickAssistAssistant fQuickAssistant = null;
  /*
   * One instance per configuration
   */
  private IReconciler fReconciler;
  /**
   * Extended configuration provisionalConfiguration type to contribute additional auto edit
   * strategies
   */
  private final String AUTOEDITSTRATEGY = "autoeditstrategy"; //$NON-NLS-1$

  private final String CONTENT_ASSIST_SIZE = "contentassistsize";

  private ReconcilerHighlighter fHighlighter = null;

  /**
   * Creates a structured text viewer configuration.
   */
  public StructuredTextViewerConfiguration() {
    super();
    // initialize fPreferenceStore with same preference store used in
    // StructuredTextEditor
    fPreferenceStore = createCombinedPreferenceStore();
  }

  /**
   * Create a preference store that combines the source editor preferences with the base editor's
   * preferences.
   * 
   * @return IPreferenceStore
   */
  private IPreferenceStore createCombinedPreferenceStore() {
    IPreferenceStore sseEditorPrefs = SSEUIPlugin.getDefault().getPreferenceStore();
    IPreferenceStore baseEditorPrefs = EditorsUI.getPreferenceStore();
    return new ChainedPreferenceStore(new IPreferenceStore[] {sseEditorPrefs, baseEditorPrefs});
  }

  /**
   * Note: Clients cannot override this method because this method returns a specially configured
   * Annotation Hover for the StructuredTextViewer (non-Javadoc)
   * 
   * @see org.eclipse.ui.editors.text.TextSourceViewerConfiguration#getAnnotationHover(org.eclipse.jface.text.source.ISourceViewer)
   */
  final public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
    return new StructuredTextAnnotationHover() {
      protected boolean isIncluded(Annotation annotation) {
        return isShowInVerticalRuler(annotation);
      }
    };
  }

  /**
   * Get color for the preference key. Assumes fPreferenceStore is not null.
   * 
   * @param key
   * @return Color for preference key or null if none found
   */
  private Color getColor(String key) {
    RGB rgb = PreferenceConverter.getColor(fPreferenceStore, key);
    return EditorUtility.getColor(rgb);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.text.source.SourceViewerConfiguration#getAutoEditStrategies(org.eclipse.jface
   * .text.source.ISourceViewer, java.lang.String)
   */
  public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
    List allStrategies = new ArrayList(0);

    IAutoEditStrategy[] superStrategies = super.getAutoEditStrategies(sourceViewer, contentType);
    for (int i = 0; i < superStrategies.length; i++) {
      allStrategies.add(superStrategies[i]);
    }

    // add auto edit strategies contributed by clients
    List extendedAutoEdits = ExtendedConfigurationBuilder.getInstance().getConfigurations(
        AUTOEDITSTRATEGY, contentType);
    if (!extendedAutoEdits.isEmpty()) {
      allStrategies.addAll(extendedAutoEdits);
    }

    return (IAutoEditStrategy[]) allStrategies.toArray(new IAutoEditStrategy[allStrategies.size()]);
  }

  /**
   * Returns the configured partitioning for the given source viewer. The partitioning is used when
   * the querying content types from the source viewer's input document.<br />
   * Note: Clients cannot override this method at this time.
   * 
   * @param sourceViewer the source viewer to be configured by this configuration
   * @return the configured partitioning
   * @see #getConfiguredContentTypes(ISourceViewer)
   */
  final public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
    /*
     * This implementation returns default structured text partitioning
     */
    return IStructuredPartitioning.DEFAULT_STRUCTURED_PARTITIONING;
  }

  public int[] getConfiguredTextHoverStateMasks(ISourceViewer sourceViewer, String contentType) {
    /*
     * This implementation returns configured text hover state masks for StructuredTextViewers
     */
    TextHoverManager.TextHoverDescriptor[] hoverDescs = SSEUIPlugin.getDefault().getTextHoverManager().getTextHovers();
    int stateMasks[] = new int[hoverDescs.length];
    int stateMasksLength = 0;
    for (int i = 0; i < hoverDescs.length; i++) {
      if (hoverDescs[i].isEnabled()) {
        int j = 0;
        int stateMask = computeStateMask(hoverDescs[i].getModifierString());
        while (j < stateMasksLength) {
          if (stateMasks[j] == stateMask)
            break;
          j++;
        }
        if (j == stateMasksLength)
          stateMasks[stateMasksLength++] = stateMask;
      }
    }
    if (stateMasksLength == hoverDescs.length)
      return stateMasks;

    int[] shortenedStateMasks = new int[stateMasksLength];
    System.arraycopy(stateMasks, 0, shortenedStateMasks, 0, stateMasksLength);
    return shortenedStateMasks;
  }

  /**
   * Returns the content assistant ready to be used with the given source viewer.<br />
   * Note: The same instance of IContentAssistant is returned regardless of the source viewer passed
   * in.
   * <p>
   * Clients should generally not override this method. Instead, clients wanting to add their own
   * processors should override <code>getContentAssistProcessors(ISourceViewer, String)</code>
   * </p>
   * 
   * @param sourceViewer the source viewer to be configured by this configuration
   * @return a content assistant
   * @see #getContentAssistProcessors(ISourceViewer, String)
   */
  public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
    /*
     * Note: This method was made final so that StructuredContentAssist is always used and content
     * assist extension point always works.
     */
    if (fContentAssistant == null) {
      fContentAssistant = new StructuredContentAssistant();

      // content assistant configurations
      fContentAssistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
      fContentAssistant.enableAutoActivation(true);
      fContentAssistant.setProposalPopupOrientation(IContentAssistant.PROPOSAL_OVERLAY);
      fContentAssistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
      fContentAssistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));
      fContentAssistant.enableColoredLabels(PlatformUI.getPreferenceStore().getBoolean(
          IWorkbenchPreferenceConstants.USE_COLORED_LABELS));

      // set content assist preferences
      if (fPreferenceStore != null) {
        int delay = fPreferenceStore.getInt(EditorPreferenceNames.CODEASSIST_AUTOACTIVATION_DELAY);
        fContentAssistant.setAutoActivationDelay(delay);

        Color color = getColor(EditorPreferenceNames.CODEASSIST_PROPOSALS_BACKGROUND);
        fContentAssistant.setProposalSelectorBackground(color);

        color = getColor(EditorPreferenceNames.CODEASSIST_PROPOSALS_FOREGROUND);
        fContentAssistant.setProposalSelectorForeground(color);

        color = getColor(EditorPreferenceNames.CODEASSIST_PARAMETERS_BACKGROUND);
        fContentAssistant.setContextInformationPopupBackground(color);
        fContentAssistant.setContextSelectorBackground(color);

        color = getColor(EditorPreferenceNames.CODEASSIST_PARAMETERS_FOREGROUND);
        fContentAssistant.setContextInformationPopupForeground(color);
        fContentAssistant.setContextSelectorForeground(color);
      }

      // add content assist processors for each partition type
      String[] types = getConfiguredContentTypes(sourceViewer);
      for (int i = 0; i < types.length; i++) {
        String type = types[i];

        // add all content assist processors for current partiton type
        IContentAssistProcessor[] processors = getContentAssistProcessors(sourceViewer, type);
        if (processors != null) {
          for (int j = 0; j < processors.length; j++) {
            fContentAssistant.setContentAssistProcessor(processors[j], type);
          }
        }
      }
      IDialogSettings dialogSettings = SSEUIPlugin.getInstance().getDialogSettings();
      if (dialogSettings != null) {
        IDialogSettings section = dialogSettings.getSection(CONTENT_ASSIST_SIZE);
        if (section == null) {
          section = dialogSettings.addNewSection(CONTENT_ASSIST_SIZE);
        }
        fContentAssistant.setRestoreCompletionProposalSize(section);
      }
    }
    return fContentAssistant;
  }

  /**
   * <p>
   * Returns a {@link StructuredContentAssistProcessor} which can be contributed to through the
   * <tt>org.eclipse.wst.sse.ui.completionProposal</tt> extension point.
   * </p>
   * <p>
   * If an extender of this class overrides this method and does not include an implementation of a
   * {@link StructuredContentAssistProcessor} in their returned processors then all of the
   * contributions by the aforementioned extension point will be left out.
   * </p>
   * 
   * @param sourceViewer the source viewer to be configured by this configuration
   * @param partitionType the partition type for which the content assist processors are applicable
   * @return IContentAssistProcessors or null if should not be supported
   */
  protected IContentAssistProcessor[] getContentAssistProcessors(ISourceViewer sourceViewer,
      String partitionType) {
    IContentAssistProcessor processor = new StructuredContentAssistProcessor(fContentAssistant,
        partitionType, sourceViewer, null);
    return new IContentAssistProcessor[] {processor};
  }

  /**
   * Returns the content formatter ready to be used with the given source viewer.
   * <p>
   * It is not recommended that clients override this method as it may become <code>final</code> in
   * the future and replaced by an extensible framework.
   * </p>
   * 
   * @param sourceViewer the source viewer to be configured by this configuration
   * @return a content formatter or <code>null</code> if formatting should not be supported
   */
  public IContentFormatter getContentFormatter(ISourceViewer sourceViewer) {
    // try to use the StructuredTextMultiPassContentFormatter so that it
    // picks up any additional formatting strategies contributed via the
    // editorConfiguration extension point
    IContentFormatter formatter = null;
    if (sourceViewer != null) {
      IDocument document = sourceViewer.getDocument();
      if (document instanceof IDocumentExtension3) {
        String partitioning = getConfiguredDocumentPartitioning(sourceViewer);
        IDocumentPartitioner partitioner = ((IDocumentExtension3) document).getDocumentPartitioner(partitioning);
        if (partitioner instanceof StructuredTextPartitioner) {
          String defaultPartitionType = ((StructuredTextPartitioner) partitioner).getDefaultPartitionType();
          formatter = new StructuredTextMultiPassContentFormatter(partitioning,
              defaultPartitionType);
        }
      }
    }

    return formatter;
  }

  /**
   * Returns the double-click strategy ready to be used in this viewer when double clicking onto
   * text of the given content type. Note that if clients want to contribute their own doubleclick
   * strategy, they should use <code>org.eclipse.wst.sse.ui.editorConfiguration</code> extension
   * point's <code>doubleClickStrategy</code> element instead of overriding this method. If clients
   * do override this method, please remember to call <code>super.getDoubleClickStrategy()</code>.
   * 
   * @param sourceViewer the source viewer to be configured by this configuration
   * @param contentType the content type for which the strategy is applicable
   * @return a double-click strategy or <code>null</code> if double clicking should not be supported
   */
  public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer,
      String contentType) {
    ITextDoubleClickStrategy strategy = null;
    Object extendedStrategy = ExtendedConfigurationBuilder.getInstance().getConfiguration(
        ExtendedConfigurationBuilder.DOUBLECLICKSTRATEGY, contentType);
    if (extendedStrategy instanceof ITextDoubleClickStrategy) {
      strategy = (ITextDoubleClickStrategy) extendedStrategy;
    } else {
      strategy = super.getDoubleClickStrategy(sourceViewer, contentType);
    }
    return strategy;
  }

  /**
   * Returns the hyperlink presenter for the given source viewer.<br />
   * Note: Clients cannot override this method, and although it's no longer necessary, it must
   * remain for binary compatibility.
   * 
   * @param sourceViewer the source viewer to be configured by this configuration
   * @return a hyperlink presenter specially configured for StructuredTextViewer
   */
  final public IHyperlinkPresenter getHyperlinkPresenter(ISourceViewer sourceViewer) {
    if (fPreferenceStore == null) {
      return super.getHyperlinkPresenter(sourceViewer);
    }
    return new MultipleHyperlinkPresenter(fPreferenceStore);
  }

  /**
   * Returns the information control creator. The creator is a factory creating information controls
   * for the given source viewer.<br />
   * Note: Clients cannot override this method at this time.
   * 
   * @param sourceViewer the source viewer to be configured by this configuration
   * @return the information control creator
   */
  final public IInformationControlCreator getInformationControlCreator(ISourceViewer sourceViewer) {
    // used by hover help
    return new IInformationControlCreator() {
      public IInformationControl createInformationControl(Shell parent) {
        return new DefaultInformationControl(parent, new HTMLTextPresenter(true));
      }
    };
  }

  /**
   * Returns the information presenter ready to be used with the given source viewer.
   * <p>
   * Clients cannot override this method. Instead, clients wanting to add their own information
   * providers should override <code>getInformationProvider(ISourceViewer, String)</code>
   * </p>
   * 
   * @param sourceViewer the source viewer to be configured by this configuration
   * @return a content assistant
   * @see #getInformationProvider(ISourceViewer, String)
   */
  final public IInformationPresenter getInformationPresenter(ISourceViewer sourceViewer) {
    InformationPresenter presenter = new InformationPresenter(
        getInformationPresenterControlCreator(sourceViewer));

    // information presenter configurations
    presenter.setSizeConstraints(60, 10, true, true);
    presenter.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

    // add information providers for each partition type
    String[] types = getConfiguredContentTypes(sourceViewer);
    for (int i = 0; i < types.length; i++) {
      String type = types[i];

      IInformationProvider provider = getInformationProvider(sourceViewer, type);
      if (provider != null) {
        presenter.setInformationProvider(provider, type);
      }
    }

    return presenter;
  }

  /**
   * Returns the information provider that will be used for information presentation in the given
   * source viewer and for the given partition type.
   * 
   * @param sourceViewer the source viewer to be configured by this configuration
   * @param partitionType the partition type for which the information provider is applicable
   * @return IInformationProvider or null if should not be supported
   * @deprecated instead of overriding this method to provide documentation information, adopters
   *             should use the <code>documentationTextHover</code> element in the
   *             <code>org.eclipse.wst.sse.ui.editorConfiguration</code> extension point
   */
  protected IInformationProvider getInformationProvider(ISourceViewer sourceViewer,
      String partitionType) {
    ITextHover bestMatchHover = new BestMatchHover(partitionType);
    return new TextHoverInformationProvider(bestMatchHover);
  }

  /**
   * Returns the information presenter control creator. The creator is a factory creating the
   * presenter controls for the given source viewer.
   * 
   * @param sourceViewer the source viewer to be configured by this configuration
   * @return an information control creator
   */
  private IInformationControlCreator getInformationPresenterControlCreator(
      ISourceViewer sourceViewer) {
    return new IInformationControlCreator() {
      public IInformationControl createInformationControl(Shell parent) {
        int shellStyle = SWT.RESIZE | SWT.TOOL;
        int style = SWT.V_SCROLL | SWT.H_SCROLL;
        return new DefaultInformationControl(parent, shellStyle, style,
            new HTMLTextPresenter(false));
      }
    };
  }

  /**
   * Returns the line style providers that will be used for syntax highlighting in the given source
   * viewer.
   * <p>
   * Not fully API since return type LineStyleProvider is not API.
   * </p>
   * 
   * @param sourceViewer the source viewer to be configured by this configuration
   * @param partitionType the partition type for which the lineStyleProviders are applicable
   * @return LineStyleProvders or null if should not be supported
   */
  public LineStyleProvider[] getLineStyleProviders(ISourceViewer sourceViewer, String partitionType) {
    return null;
  }

  /**
   * See <code>getLineStyleProviders(ISourceViewer, String)</code> for alternative way to provide
   * highlighting information.
   * 
   * @param sourceViewer the source viewer to be configured by this configuration
   * @return always returns null
   * @see #getLineStyleProviders(ISourceViewer, String)
   */
  public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
    StructuredPresentationReconciler reconciler = new StructuredPresentationReconciler();
    reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

    String[] contentTypes = getConfiguredContentTypes(sourceViewer);

    if (contentTypes != null) {
      StructuredDocumentDamagerRepairer dr = null;

      for (int i = 0; i < contentTypes.length; i++) {
        if (fHighlighter != null) {
          LineStyleProvider provider = fHighlighter.getProvider(contentTypes[i]);
          if (provider == null)
            continue;

          dr = new StructuredDocumentDamagerRepairer(provider);
          dr.setDocument(sourceViewer.getDocument());
          reconciler.setDamager(dr, contentTypes[i]);
          reconciler.setRepairer(dr, contentTypes[i]);
        }
      }
    }

    return reconciler;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.ui.editors.text.TextSourceViewerConfiguration#getOverviewRulerAnnotationHover(org
   * .eclipse.jface.text.source.ISourceViewer)
   */
  public IAnnotationHover getOverviewRulerAnnotationHover(ISourceViewer sourceViewer) {
    return new StructuredTextAnnotationHover(true) {
      protected boolean isIncluded(Annotation annotation) {
        return isShowInOverviewRuler(annotation);
      }
    };
  }

  /*
   * @see
   * org.eclipse.jface.text.source.SourceViewerConfiguration#getQuickAssistAssistant(org.eclipse
   * .jface.text.source.ISourceViewer)
   */
  public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
    if (fQuickAssistant == null) {
      IQuickAssistAssistant assistant = new QuickAssistAssistant();
      assistant.setQuickAssistProcessor(new CompoundQuickAssistProcessor());
      assistant.setInformationControlCreator(getQuickAssistAssistantInformationControlCreator());

      // Waiting for color preferences, see:
      // https://bugs.eclipse.org/bugs/show_bug.cgi?id=133731
      // set content assist preferences
      if (fPreferenceStore != null) {
        Color color = getColor(EditorPreferenceNames.CODEASSIST_PROPOSALS_BACKGROUND);
        assistant.setProposalSelectorBackground(color);

        color = getColor(EditorPreferenceNames.CODEASSIST_PROPOSALS_FOREGROUND);
        assistant.setProposalSelectorForeground(color);
      }
      fQuickAssistant = assistant;
    }
    return fQuickAssistant;
  }

  /**
   * Returns the information control creator for the quick assist assistant.
   * 
   * @return the information control creator
   */
  private IInformationControlCreator getQuickAssistAssistantInformationControlCreator() {
    return new IInformationControlCreator() {
      public IInformationControl createInformationControl(Shell parent) {
        return new DefaultInformationControl(parent, new HTMLTextPresenter(true));
      }
    };
  }

  /**
   * Returns the reconciler ready to be used with the given source viewer.<br />
   * Note: The same instance of IReconciler is returned regardless of the source viewer passed in.
   * <p>
   * Clients cannot override this method. Instead, clients wanting to add their own reconciling
   * strategy should use the <code>org.eclipse.wst.sse.ui.extensions.sourcevalidation</code>
   * extension point.
   * </p>
   * 
   * @param sourceViewer the source viewer to be configured by this configuration
   * @return a reconciler
   */
  final public IReconciler getReconciler(ISourceViewer sourceViewer) {
    IReconciler reconciler = null;

    if (sourceViewer != null) {
      //Only create reconciler if sourceViewer is present
      if (fReconciler == null && sourceViewer != null) {
        StructuredRegionProcessor newReconciler = new StructuredRegionProcessor();

        // reconciler configurations
        newReconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

        fReconciler = newReconciler;
      }
      reconciler = fReconciler;
    }

    return reconciler;
  }

  /**
   * @since 2.0
   * @param treeViewer
   * @return a label provider providing the status line contents
   */
  public ILabelProvider getStatusLineLabelProvider(ISourceViewer sourceViewer) {
    return null;
  }

  /**
   * Create documentation hovers based on hovers contributed via
   * <code>org.eclipse.wst.sse.ui.editorConfiguration</code> extension point
   * 
   * @param partitionType
   * @return
   */
  private ITextHover[] createDocumentationHovers(String partitionType) {
    List extendedTextHover = ExtendedConfigurationBuilder.getInstance().getConfigurations(
        ExtendedConfigurationBuilder.DOCUMENTATIONTEXTHOVER, partitionType);
    ITextHover[] hovers = (ITextHover[]) extendedTextHover.toArray(new ITextHover[extendedTextHover.size()]);
    return hovers;
  }

  public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType, int stateMask) {
    ITextHover textHover = null;

    /*
     * Returns a default problem, annotation, and best match hover depending on stateMask
     */
    TextHoverManager.TextHoverDescriptor[] hoverDescs = SSEUIPlugin.getDefault().getTextHoverManager().getTextHovers();
    int i = 0;
    while (i < hoverDescs.length && textHover == null) {
      if (hoverDescs[i].isEnabled()
          && computeStateMask(hoverDescs[i].getModifierString()) == stateMask) {
        String hoverType = hoverDescs[i].getId();
        if (TextHoverManager.PROBLEM_HOVER.equalsIgnoreCase(hoverType))
          textHover = new ProblemAnnotationHoverProcessor();
        else if (TextHoverManager.ANNOTATION_HOVER.equalsIgnoreCase(hoverType))
          textHover = new AnnotationHoverProcessor();
        else if (TextHoverManager.COMBINATION_HOVER.equalsIgnoreCase(hoverType))
          textHover = new BestMatchHover(contentType);
        else if (TextHoverManager.DOCUMENTATION_HOVER.equalsIgnoreCase(hoverType)) {
          ITextHover[] hovers = createDocumentationHovers(contentType);
          if (hovers.length > 0) {
            textHover = hovers[0];
          }
        }
      }
      i++;
    }
    return textHover;
  }

  /**
   * Returns the undo manager for the given source viewer.<br />
   * Note: Clients cannot override this method because this method returns a specially configured
   * undo manager for the StructuredTextViewer.
   * 
   * @param sourceViewer the source viewer to be configured by this configuration
   * @return an undo manager specially configured for StructuredTextViewer
   */
  final public IUndoManager getUndoManager(ISourceViewer sourceViewer) {
    /*
     * This implementation returns an UndoManager that is used exclusively in StructuredTextViewer
     */
    return new StructuredTextViewerUndoManager();
  }

  public void setHighlighter(ReconcilerHighlighter highlighter) {
    fHighlighter = highlighter;
  }

  /**
   * @return the associated content assistnat
   */
  protected StructuredContentAssistant getContentAssistant() {
    return this.fContentAssistant;
  }
}
