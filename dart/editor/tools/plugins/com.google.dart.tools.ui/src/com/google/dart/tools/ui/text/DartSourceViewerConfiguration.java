/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.text;

import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartX;
import com.google.dart.tools.ui.actions.DartEditorActionDefinitionIds;
import com.google.dart.tools.ui.internal.text.correction.DartCorrectionAssistant_NEW;
import com.google.dart.tools.ui.internal.text.correction.DartCorrectionAssistant_OLD;
import com.google.dart.tools.ui.internal.text.dart.ContentAssistProcessor;
import com.google.dart.tools.ui.internal.text.dart.DartAutoIndentStrategy_NEW;
import com.google.dart.tools.ui.internal.text.dart.DartAutoIndentStrategy_OLD;
import com.google.dart.tools.ui.internal.text.dart.DartCodeScanner;
import com.google.dart.tools.ui.internal.text.dart.DartCompletionProcessor;
import com.google.dart.tools.ui.internal.text.dart.DartDocDoubleClickStrategy;
import com.google.dart.tools.ui.internal.text.dart.DartDoubleClickSelector_NEW;
import com.google.dart.tools.ui.internal.text.dart.DartDoubleClickSelector_OLD;
import com.google.dart.tools.ui.internal.text.dart.DartReconcilingEditor;
import com.google.dart.tools.ui.internal.text.dart.DartReconcilingStrategy;
import com.google.dart.tools.ui.internal.text.dart.DartStringAutoIndentStrategy;
import com.google.dart.tools.ui.internal.text.dart.DartStringDoubleClickSelector_NEW;
import com.google.dart.tools.ui.internal.text.dart.DartStringDoubleClickSelector_OLD;
import com.google.dart.tools.ui.internal.text.dart.SmartSemicolonAutoEditStrategy;
import com.google.dart.tools.ui.internal.text.dartdoc.DartDocAutoIndentStrategy;
import com.google.dart.tools.ui.internal.text.dartdoc.DartDocScanner;
import com.google.dart.tools.ui.internal.text.dartdoc.LineDocAutoIndentStrategy;
import com.google.dart.tools.ui.internal.text.editor.DartHover;
import com.google.dart.tools.ui.internal.text.functions.AbstractDartScanner;
import com.google.dart.tools.ui.internal.text.functions.ContentAssistPreference;
import com.google.dart.tools.ui.internal.text.functions.DartCommentScanner;
import com.google.dart.tools.ui.internal.text.functions.DartElementProvider;
import com.google.dart.tools.ui.internal.text.functions.DartMultilineStringScanner;
import com.google.dart.tools.ui.internal.text.functions.DartOutlineElementProvider;
import com.google.dart.tools.ui.internal.text.functions.DartOutlineElementProvider_NEW;
import com.google.dart.tools.ui.internal.text.functions.DartOutlineInformationControl;
import com.google.dart.tools.ui.internal.text.functions.DartOutlineInformationControl_NEW;
import com.google.dart.tools.ui.internal.text.functions.DartPresentationReconciler;
import com.google.dart.tools.ui.internal.text.functions.HTMLAnnotationHover;
import com.google.dart.tools.ui.internal.text.functions.PreferencesAdapter;
import com.google.dart.tools.ui.internal.text.functions.SingleTokenDartScanner;
import com.google.dart.tools.ui.internal.typehierarchy.HierarchyInformationControl_NEW;
import com.google.dart.tools.ui.internal.typehierarchy.HierarchyInformationControl_OLD;
import com.google.dart.tools.ui.internal.util.CodeFormatterUtil;
import com.google.dart.tools.ui.text.editor.tmp.JavaScriptCore;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.internal.text.html.HTMLTextPresenter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.AbstractInformationControlManager;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.DefaultTextDoubleClickStrategy;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.hyperlink.DefaultHyperlinkPresenter;
import org.eclipse.jface.text.hyperlink.IHyperlinkPresenter;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.Arrays;
import java.util.Map;

/**
 * Configuration for a source viewer which shows Dart code.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
@SuppressWarnings("restriction")
public class DartSourceViewerConfiguration extends TextSourceViewerConfiguration {

  /**
   * Creates and returns a preference store which combines the preference stores from the text tools
   * and which is read-only.
   * 
   * @param javaTextTools the Dart text tools
   * @return the combined read-only preference store
   */
  @SuppressWarnings("unused")
  private static final IPreferenceStore createPreferenceStore(DartTextTools javaTextTools) {
    Assert.isNotNull(javaTextTools);
    IPreferenceStore generalTextStore = EditorsUI.getPreferenceStore();
    if (javaTextTools.getCorePreferenceStore() == null) {
      return new ChainedPreferenceStore(new IPreferenceStore[] {
          javaTextTools.getPreferenceStore(), generalTextStore});
    }

    return new ChainedPreferenceStore(new IPreferenceStore[] {
        javaTextTools.getPreferenceStore(),
        new PreferencesAdapter(javaTextTools.getCorePreferenceStore()), generalTextStore});
  }

  private static IInformationControlCreator getHierarchyPresenterControlCreator(
      ISourceViewer sourceViewer) {
    return new IInformationControlCreator() {
      @Override
      public IInformationControl createInformationControl(Shell parent) {
        int shellStyle = SWT.RESIZE;
        int treeStyle = SWT.V_SCROLL | SWT.H_SCROLL;
        if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
          return new HierarchyInformationControl_NEW(parent, shellStyle, treeStyle);
        } else {
          return new HierarchyInformationControl_OLD(parent, shellStyle, treeStyle);
        }
      }
    };
  }

  private DartTextTools fJavaTextTools;
  private ITextEditor fTextEditor;
  /**
   * The document partitioning.
   */
  private String fDocumentPartitioning;
  /**
   * The Dart source code scanner.
   */
  private AbstractDartScanner fCodeScanner;

  /**
   * The Dart multi-line comment scanner.
   */
  private AbstractDartScanner fMultilineCommentScanner;
  /**
   * The Dart single-line comment scanner.
   */
  private AbstractDartScanner fSinglelineCommentScanner;
  /**
   * The Dart string scanner.
   */
  private AbstractDartScanner fStringScanner;
  /**
   * The Dart multi-line scanner
   */
  private AbstractDartScanner fMultilineStringScanner;

  /**
   * The Doc scanner.
   */
  private AbstractDartScanner fJavaDocScanner;

  /**
   * The color manager.
   */
  private IColorManager fColorManager;

  /**
   * The double click strategy.
   */
  private DartDoubleClickSelector_OLD fJavaDoubleClickSelector_OLD;

  /**
   * The double click strategy.
   */
  private DartDoubleClickSelector_NEW fJavaDoubleClickSelector_NEW;

  /**
   * Creates a new Dart source viewer configuration for viewers in the given editor using the given
   * preference store, the color manager and the specified document partitioning.
   * <p>
   * Creates a Dart source viewer configuration in the new setup without text tools. Clients are
   * allowed to call
   * {@link DartSourceViewerConfiguration#handlePropertyChangeEvent(PropertyChangeEvent)} on the
   * resulting Dart source viewer configuration.
   * </p>
   * 
   * @param colorManager the color manager
   * @param preferenceStore the preference store, can be read-only
   * @param editor the editor in which the configured viewer(s) will reside, or <code>null</code> if
   *          none
   * @param partitioning the document partitioning for this configuration, or <code>null</code> for
   *          the default partitioning
   */
  public DartSourceViewerConfiguration(IColorManager colorManager,
      IPreferenceStore preferenceStore, ITextEditor editor, String partitioning) {
    super(preferenceStore);
    fColorManager = colorManager;
    fTextEditor = editor;
    fDocumentPartitioning = partitioning;
    initializeScanners();
  }

  /**
   * Determines whether the preference change encoded by the given event changes the behavior of one
   * of its contained components.
   * 
   * @param event the event to be investigated
   * @return <code>true</code> if event causes a behavioral change
   */
  public boolean affectsTextPresentation(PropertyChangeEvent event) {
    return fCodeScanner.affectsBehavior(event) || fMultilineCommentScanner.affectsBehavior(event)
        || fSinglelineCommentScanner.affectsBehavior(event)
        || fStringScanner.affectsBehavior(event) || fJavaDocScanner.affectsBehavior(event)
        || fMultilineStringScanner.affectsBehavior(event);
  }

  @Override
  public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
    return new HTMLAnnotationHover() {
      @Override
      protected boolean isIncluded(Annotation annotation) {
        return isShowInVerticalRuler(annotation);
      }
    };
  }

  @Override
  public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
    DartX.todo("indent");
    String partitioning = getConfiguredDocumentPartitioning(sourceViewer);
    if (DartPartitions.DART_DOC.equals(contentType)
        || DartPartitions.DART_MULTI_LINE_COMMENT.equals(contentType)) {
      return new IAutoEditStrategy[] {new DartDocAutoIndentStrategy(partitioning)};
    } else if (DartPartitions.DART_SINGLE_LINE_DOC.equals(contentType)) {
      return new IAutoEditStrategy[] {new LineDocAutoIndentStrategy(partitioning)};
    } else if (DartPartitions.DART_MULTI_LINE_STRING.equals(contentType)) {
      return new IAutoEditStrategy[] {
          new SmartSemicolonAutoEditStrategy(partitioning),
          new DartStringAutoIndentStrategy(partitioning)};
    } else if (DartPartitions.DART_STRING.equals(contentType)) {
      return new IAutoEditStrategy[] {
          new SmartSemicolonAutoEditStrategy(partitioning),
          new DartStringAutoIndentStrategy(partitioning)};
    } else if (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType)) {
      if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
        return new IAutoEditStrategy[] {
            new SmartSemicolonAutoEditStrategy(partitioning),
            new DartAutoIndentStrategy_NEW(partitioning, sourceViewer)};
      } else {
        return new IAutoEditStrategy[] {
            new SmartSemicolonAutoEditStrategy(partitioning),
            new DartAutoIndentStrategy_OLD(partitioning, sourceViewer)};
      }
    } else {
      return new IAutoEditStrategy[] {new DartAutoIndentStrategy_OLD(partitioning, sourceViewer)};
    }
  }

  @Override
  public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
    return new String[] {
        IDocument.DEFAULT_CONTENT_TYPE, DartPartitions.DART_DOC,
        DartPartitions.DART_MULTI_LINE_COMMENT, DartPartitions.DART_SINGLE_LINE_COMMENT,
        DartPartitions.DART_SINGLE_LINE_DOC, DartPartitions.DART_STRING,
        DartPartitions.DART_MULTI_LINE_STRING};
  }

  @Override
  public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
    if (fDocumentPartitioning != null) {
      return fDocumentPartitioning;
    }
    return super.getConfiguredDocumentPartitioning(sourceViewer);
  }

  @Override
  public int[] getConfiguredTextHoverStateMasks(ISourceViewer sourceViewer, String contentType) {
    return new int[] {0};
//    return super.getConfiguredTextHoverStateMasks(sourceViewer, contentType);
    // JavaEditorTextHoverDescriptor[] hoverDescs =
    // DartToolsPlugin.getDefault().getJavaEditorTextHoverDescriptors();
    // int stateMasks[] = new int[hoverDescs.length];
    // int stateMasksLength = 0;
    // for (int i = 0; i < hoverDescs.length; i++) {
    // if (hoverDescs[i].isEnabled()) {
    // int j = 0;
    // int stateMask = hoverDescs[i].getStateMask();
    // while (j < stateMasksLength) {
    // if (stateMasks[j] == stateMask)
    // break;
    // j++;
    // }
    // if (j == stateMasksLength)
    // stateMasks[stateMasksLength++] = stateMask;
    // }
    // }
    // if (stateMasksLength == hoverDescs.length)
    // return stateMasks;
    //
    // int[] shortenedStateMasks = new int[stateMasksLength];
    // System.arraycopy(stateMasks, 0, shortenedStateMasks, 0,
    // stateMasksLength);
    // return shortenedStateMasks;
  }

  @Override
  public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {

    if (getEditor() != null) {

      ContentAssistant assistant = new DartEditorContentAssistant(sourceViewer);
      assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

      assistant.setRestoreCompletionProposalSize(getSettings("completion_proposal_size")); //$NON-NLS-1$

      IContentAssistProcessor javaProcessor = new DartCompletionProcessor(
          getEditor(),
          assistant,
          IDocument.DEFAULT_CONTENT_TYPE);
      assistant.setContentAssistProcessor(javaProcessor, IDocument.DEFAULT_CONTENT_TYPE);

      ContentAssistProcessor singleLineProcessor = new DartCompletionProcessor(
          getEditor(),
          assistant,
          DartPartitions.DART_SINGLE_LINE_COMMENT);
      assistant.setContentAssistProcessor(
          singleLineProcessor,
          DartPartitions.DART_SINGLE_LINE_COMMENT);
      // TODO temporary, see docProcessor below
      assistant.setContentAssistProcessor(singleLineProcessor, DartPartitions.DART_SINGLE_LINE_DOC);

      ContentAssistProcessor stringProcessor = new DartCompletionProcessor(
          getEditor(),
          assistant,
          DartPartitions.DART_STRING);
      assistant.setContentAssistProcessor(stringProcessor, DartPartitions.DART_STRING);
      assistant.setContentAssistProcessor(stringProcessor, DartPartitions.DART_MULTI_LINE_STRING);

      ContentAssistProcessor multiLineProcessor = new DartCompletionProcessor(
          getEditor(),
          assistant,
          DartPartitions.DART_MULTI_LINE_COMMENT);
      assistant.setContentAssistProcessor(
          multiLineProcessor,
          DartPartitions.DART_MULTI_LINE_COMMENT);
      // TODO temporary, see docProcessor below
      assistant.setContentAssistProcessor(multiLineProcessor, DartPartitions.DART_DOC);

      // TODO Code completion in doc comments
//      ContentAssistProcessor docProcessor = new DartDocCompletionProcessor(getEditor(), assistant);
//      assistant.setContentAssistProcessor(docProcessor, DartPartitions.DART_DOC);
//      assistant.setContentAssistProcessor(docProcessor, DartPartitions.DART_SINGLE_LINE_DOC);

      ContentAssistPreference.configure(assistant, fPreferenceStore);

      assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
      assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));

      return assistant;
    }

    return null;
  }

  @Override
  public IContentFormatter getContentFormatter(ISourceViewer sourceViewer) {
//    final MultiPassContentFormatter formatter = new MultiPassContentFormatter(
//        getConfiguredDocumentPartitioning(sourceViewer),
//        IDocument.DEFAULT_CONTENT_TYPE);
//
//    formatter.setMasterStrategy(new DartFormattingStrategy());
//    formatter.setSlaveStrategy(new CommentFormattingStrategy(), DartPartitions.DART_DOC);
//    formatter.setSlaveStrategy(
//        new CommentFormattingStrategy(),
//        DartPartitions.DART_SINGLE_LINE_COMMENT);
//    formatter.setSlaveStrategy(new CommentFormattingStrategy(), DartPartitions.DART_SINGLE_LINE_DOC);
//    formatter.setSlaveStrategy(
//        new CommentFormattingStrategy(),
//        DartPartitions.DART_MULTI_LINE_COMMENT);
//
//    return formatter;

    return null;
  }

  @Override
  public String[] getDefaultPrefixes(ISourceViewer sourceViewer, String contentType) {
    if (DartPartitions.DART_SINGLE_LINE_DOC.equals(contentType)) {
      return new String[] {"///", ""}; //$NON-NLS-1$ //$NON-NLS-2$
    }
    return new String[] {"//", ""}; //$NON-NLS-1$ //$NON-NLS-2$
  }

  @Override
  public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer,
      String contentType) {
    if (DartPartitions.DART_DOC.equals(contentType)
        || DartPartitions.DART_SINGLE_LINE_DOC.equals(contentType)) {
      return new DartDocDoubleClickStrategy();
    }
    if (DartPartitions.DART_MULTI_LINE_COMMENT.equals(contentType)
        || DartPartitions.DART_SINGLE_LINE_COMMENT.equals(contentType)) {
      return new DefaultTextDoubleClickStrategy();
    } else if (DartPartitions.DART_STRING.equals(contentType)
        || DartPartitions.DART_MULTI_LINE_STRING.equals(contentType)) {
      if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
        return new DartStringDoubleClickSelector_NEW();
      } else {
        return new DartStringDoubleClickSelector_OLD();
      }
    }
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      if (fJavaDoubleClickSelector_NEW == null) {
        fJavaDoubleClickSelector_NEW = new DartDoubleClickSelector_NEW();
        fJavaDoubleClickSelector_NEW.setSourceVersion(fPreferenceStore.getString(JavaScriptCore.COMPILER_SOURCE));
      }
      return fJavaDoubleClickSelector_NEW;
    } else {
      if (fJavaDoubleClickSelector_OLD == null) {
        fJavaDoubleClickSelector_OLD = new DartDoubleClickSelector_OLD();
        fJavaDoubleClickSelector_OLD.setSourceVersion(fPreferenceStore.getString(JavaScriptCore.COMPILER_SOURCE));
      }
      return fJavaDoubleClickSelector_OLD;
    }
  }

  /**
   * Returns the hierarchy presenter which will determine and shown type hierarchy information
   * requested for the current cursor position.
   * 
   * @param sourceViewer the source viewer to be configured by this configuration
   * @param doCodeResolve a boolean which specifies whether code resolve should be used to compute
   *          the Dart element
   * @return an information presenter
   */
  public IInformationPresenter getHierarchyPresenter(ISourceViewer sourceViewer,
      boolean doCodeResolve) {
    IInformationControlCreator hierarchyPresenterControlCreator = getHierarchyPresenterControlCreator(sourceViewer);
    InformationPresenter presenter = new InformationPresenter(hierarchyPresenterControlCreator);
    presenter.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
    presenter.setAnchor(AbstractInformationControlManager.ANCHOR_GLOBAL);
    IInformationProvider provider = new DartElementProvider(getEditor(), doCodeResolve);
    presenter.setInformationProvider(provider, IDocument.DEFAULT_CONTENT_TYPE);
    presenter.setInformationProvider(provider, DartPartitions.DART_DOC);
    presenter.setInformationProvider(provider, DartPartitions.DART_MULTI_LINE_COMMENT);
    presenter.setInformationProvider(provider, DartPartitions.DART_SINGLE_LINE_COMMENT);
    presenter.setInformationProvider(provider, DartPartitions.DART_STRING);
    presenter.setInformationProvider(provider, DartPartitions.DART_MULTI_LINE_STRING);
    presenter.setSizeConstraints(50, 20, true, false);
    return presenter;
  }

  /**
   * Returns the hyperlink presenter for the given source viewer. This implementation always returns
   * the {@link DefaultHyperlinkPresenter}.
   * 
   * @param sourceViewer the source viewer to be configured by this configuration
   * @return the hyperlink presenter or <code>null</code> if no hyperlink support should be
   *         installed
   * @since 3.1
   */
  @Override
  public IHyperlinkPresenter getHyperlinkPresenter(ISourceViewer sourceViewer) {
    return new DefaultHyperlinkPresenter(fColorManager.getColor("dart_doc_link"));
  }

  @Override
  public String[] getIndentPrefixes(ISourceViewer sourceViewer, String contentType) {
    return super.getIndentPrefixes(sourceViewer, contentType);
    // DartProject project = getProject();
    // final int tabWidth = CodeFormatterUtil.getTabWidth(project);
    // final int indentWidth = CodeFormatterUtil.getIndentWidth(project);
    // boolean allowTabs = tabWidth <= indentWidth;
    //
    // String indentMode;
    // if (project == null)
    // indentMode =
    // JavaScriptCore.getOption(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR);
    // else
    // indentMode = project.getOption(
    // DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, true);
    //
    // boolean useSpaces = JavaScriptCore.SPACE.equals(indentMode)
    // || DefaultCodeFormatterConstants.MIXED.equals(indentMode);
    //
    // Assert.isLegal(allowTabs || useSpaces);
    //
    // if (!allowTabs) {
    // char[] spaces = new char[indentWidth];
    // Arrays.fill(spaces, ' ');
    //      return new String[]{new String(spaces), ""}; //$NON-NLS-1$
    // } else if (!useSpaces)
    // return getIndentPrefixesForTab(tabWidth);
    // else
    // return getIndentPrefixesForSpaces(tabWidth);
  }

  @Override
  public IInformationControlCreator getInformationControlCreator(ISourceViewer sourceViewer) {
    return new IInformationControlCreator() {
      @Override
      public IInformationControl createInformationControl(Shell parent) {
        return new DefaultInformationControl(parent, new HTMLTextPresenter(true));
      }
    };
  }

  @Override
  public IInformationPresenter getInformationPresenter(ISourceViewer sourceViewer) {
    return super.getInformationPresenter(sourceViewer);
    // InformationPresenter presenter = new InformationPresenter(
    // getInformationPresenterControlCreator(sourceViewer));
    // presenter.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
    //
    // // Register information provider
    // IInformationProvider provider = new JavaInformationProvider(getEditor());
    // String[] contentTypes = getConfiguredContentTypes(sourceViewer);
    // for (int i = 0; i < contentTypes.length; i++)
    // presenter.setInformationProvider(provider, contentTypes[i]);
    //
    // presenter.setSizeConstraints(60, 10, true, true);
    // return presenter;
  }

  /**
   * Returns the outline presenter which will determine and shown information requested for the
   * current cursor position.
   * 
   * @param sourceViewer the source viewer to be configured by this configuration
   * @param doCodeResolve a boolean which specifies whether code resolve should be used to compute
   *          the Dart element
   * @return an information presenter
   */
  @SuppressWarnings("deprecation")
  public IInformationPresenter getOutlinePresenter(ISourceViewer sourceViewer, boolean doCodeResolve) {
    InformationPresenter presenter;
    if (doCodeResolve) {
      presenter = new InformationPresenter(getOutlinePresenterControlCreator(
          sourceViewer,
          DartEditorActionDefinitionIds.OPEN_STRUCTURE));
    } else {
      presenter = new InformationPresenter(getOutlinePresenterControlCreator(
          sourceViewer,
          DartEditorActionDefinitionIds.SHOW_OUTLINE));
    }
    presenter.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
    presenter.setAnchor(AbstractInformationControlManager.ANCHOR_GLOBAL);
    IInformationProvider provider;
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      provider = new DartOutlineElementProvider_NEW(getEditor());
    } else {
      provider = new DartOutlineElementProvider(getEditor());
    }
    presenter.setInformationProvider(provider, IDocument.DEFAULT_CONTENT_TYPE);
    presenter.setInformationProvider(provider, DartPartitions.DART_DOC);
    presenter.setInformationProvider(provider, DartPartitions.DART_MULTI_LINE_COMMENT);
    presenter.setInformationProvider(provider, DartPartitions.DART_SINGLE_LINE_COMMENT);
    presenter.setInformationProvider(provider, DartPartitions.DART_SINGLE_LINE_DOC);
    presenter.setInformationProvider(provider, DartPartitions.DART_STRING);
    presenter.setInformationProvider(provider, DartPartitions.DART_MULTI_LINE_STRING);
    presenter.setInformationProvider(provider, DartPartitions.JAVA_CHARACTER);
    presenter.setSizeConstraints(50, 20, true, false);
    return presenter;
  }

  @Override
  public IAnnotationHover getOverviewRulerAnnotationHover(ISourceViewer sourceViewer) {
    return new HTMLAnnotationHover() {
      @Override
      protected boolean isIncluded(Annotation annotation) {
        return isShowInOverviewRuler(annotation);
      }
    };
  }

  @Override
  public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {

    PresentationReconciler reconciler = new DartPresentationReconciler();
    reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

    DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getCodeScanner());
    reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
    reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

    dr = new DefaultDamagerRepairer(getJavaDocScanner());
    reconciler.setDamager(new DartIndiscriminateDamager(), DartPartitions.DART_DOC);
    reconciler.setRepairer(dr, DartPartitions.DART_DOC);
    reconciler.setDamager(new DartIndiscriminateDamager(), DartPartitions.DART_SINGLE_LINE_DOC);
    reconciler.setRepairer(dr, DartPartitions.DART_SINGLE_LINE_DOC);

    dr = new DefaultDamagerRepairer(getMultilineCommentScanner());
    reconciler.setDamager(dr, DartPartitions.DART_MULTI_LINE_COMMENT);
    reconciler.setRepairer(dr, DartPartitions.DART_MULTI_LINE_COMMENT);

    dr = new DefaultDamagerRepairer(getSinglelineCommentScanner());
    reconciler.setDamager(dr, DartPartitions.DART_SINGLE_LINE_COMMENT);
    reconciler.setRepairer(dr, DartPartitions.DART_SINGLE_LINE_COMMENT);

    dr = new DefaultDamagerRepairer(getStringScanner());
    reconciler.setDamager(dr, DartPartitions.DART_STRING);
    reconciler.setRepairer(dr, DartPartitions.DART_STRING);

    dr = new DefaultDamagerRepairer(getMultilineStringScanner());
    reconciler.setDamager(dr, DartPartitions.DART_MULTI_LINE_STRING);
    reconciler.setRepairer(dr, DartPartitions.DART_MULTI_LINE_STRING);

    return reconciler;
  }

  @Override
  public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
    ITextEditor editor = getEditor();
    if (editor != null) {
      if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
        DartCorrectionAssistant_NEW assistant = new DartCorrectionAssistant_NEW(editor);
        assistant.setRestoreCompletionProposalSize(getSettings("quick_assist_proposal_size")); //$NON-NLS-1$
        return assistant;
      } else {
        DartCorrectionAssistant_OLD assistant = new DartCorrectionAssistant_OLD(editor);
        assistant.setRestoreCompletionProposalSize(getSettings("quick_assist_proposal_size")); //$NON-NLS-1$
        return assistant;
      }
    }
    return null;
  }

  @Override
  public IReconciler getReconciler(ISourceViewer sourceViewer) {
    DartX.todo("spelling");
    final ITextEditor editor = getEditor();
    if (editor instanceof DartReconcilingEditor) {
      DartReconcilingEditor dartEditor = (DartReconcilingEditor) editor;
      IReconcilingStrategy strategy = new DartReconcilingStrategy(dartEditor);
      MonoReconciler reconciler = new MonoReconciler(strategy, true);
      reconciler.setIsIncrementalReconciler(true);
      reconciler.setIsAllowedToModifyDocument(false);
      reconciler.setProgressMonitor(new NullProgressMonitor());
      reconciler.setDelay(500);
      return reconciler;
    }
    return null;
  }

  @Override
  public int getTabWidth(ISourceViewer sourceViewer) {
    return CodeFormatterUtil.getTabWidth();
  }

  @Override
  public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
    return getTextHover(sourceViewer, contentType, ITextViewerExtension2.DEFAULT_HOVER_STATE_MASK);
  }

  @Override
  public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType, int stateMask) {
    return new DartHover(fTextEditor, sourceViewer, this);
    // JavaEditorTextHoverDescriptor[] hoverDescs =
    // DartToolsPlugin.getDefault().getJavaEditorTextHoverDescriptors();
    // int i = 0;
    // while (i < hoverDescs.length) {
    // if (hoverDescs[i].isEnabled()
    // && hoverDescs[i].getStateMask() == stateMask)
    // return new JavaEditorTextHoverProxy(hoverDescs[i], getEditor());
    // i++;
    // }
    //
    // return null;
  }

  /**
   * Adapts the behavior of the contained components to the change encoded in the given event.
   * <p>
   * Clients are not allowed to call this method if the old setup with text tools is in use.
   * </p>
   * 
   * @param event the event to which to adapt
   * @see DartSourceViewerConfiguration#JavaSourceViewerConfiguration(IColorManager,
   *      IPreferenceStore, ITextEditor, String)
   */
  public void handlePropertyChangeEvent(PropertyChangeEvent event) {
    Assert.isTrue(isNewSetup());
    if (fCodeScanner.affectsBehavior(event)) {
      fCodeScanner.adaptToPreferenceChange(event);
    }
    if (fMultilineCommentScanner.affectsBehavior(event)) {
      fMultilineCommentScanner.adaptToPreferenceChange(event);
    }
    if (fSinglelineCommentScanner.affectsBehavior(event)) {
      fSinglelineCommentScanner.adaptToPreferenceChange(event);
    }
    if (fStringScanner.affectsBehavior(event)) {
      fStringScanner.adaptToPreferenceChange(event);
    }
    if (fMultilineStringScanner.affectsBehavior(event)) {
      fMultilineStringScanner.adaptToPreferenceChange(event);
    }
    if (fJavaDocScanner.affectsBehavior(event)) {
      fJavaDocScanner.adaptToPreferenceChange(event);
    }
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      if (fJavaDoubleClickSelector_NEW != null
          && JavaScriptCore.COMPILER_SOURCE.equals(event.getProperty())) {
        if (event.getNewValue() instanceof String) {
          fJavaDoubleClickSelector_NEW.setSourceVersion((String) event.getNewValue());
        }
      }
    } else {
      if (fJavaDoubleClickSelector_OLD != null
          && JavaScriptCore.COMPILER_SOURCE.equals(event.getProperty())) {
        if (event.getNewValue() instanceof String) {
          fJavaDoubleClickSelector_OLD.setSourceVersion((String) event.getNewValue());
        }
      }
    }
  }

  /**
   * Made public for visibility outside of the package.
   */
  @Override
  public boolean isShownInText(Annotation annotation) {
    return super.isShownInText(annotation);
  }

  /**
   * Returns the Dart source code scanner for this configuration.
   * 
   * @return the Dart source code scanner
   */
  protected RuleBasedScanner getCodeScanner() {
    return fCodeScanner;
  }

  /**
   * Returns the color manager for this configuration.
   * 
   * @return the color manager
   */
  protected IColorManager getColorManager() {
    return fColorManager;
  }

  /**
   * Returns the editor in which the configured viewer(s) will reside.
   * 
   * @return the enclosing editor
   */
  protected ITextEditor getEditor() {
    return fTextEditor;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  protected Map getHyperlinkDetectorTargets(ISourceViewer sourceViewer) {
    Map targets = super.getHyperlinkDetectorTargets(sourceViewer);
    targets.put("com.google.dart.tools.ui.dartCode", fTextEditor); //$NON-NLS-1$
    return targets;
  }

  /**
   * Returns the JavaDoc scanner for this configuration.
   * 
   * @return the JavaDoc scanner
   */
  protected RuleBasedScanner getJavaDocScanner() {
    return fJavaDocScanner;
  }

  /**
   * Returns the Dart multi-line comment scanner for this configuration.
   * 
   * @return the Dart multi-line comment scanner
   */
  protected RuleBasedScanner getMultilineCommentScanner() {
    return fMultilineCommentScanner;
  }

  /**
   * Returns the Dart multi-line string scanner for this configuration.
   * 
   * @return the Dart multi-line string scanner
   */
  protected RuleBasedScanner getMultilineStringScanner() {
    return fMultilineStringScanner;
  }

  /**
   * Returns the Dart single-line comment scanner for this configuration.
   * 
   * @return the Dart single-line comment scanner
   */
  protected RuleBasedScanner getSinglelineCommentScanner() {
    return fSinglelineCommentScanner;
  }

  /**
   * Returns the Dart string scanner for this configuration.
   * 
   * @return the Dart string scanner
   */
  protected RuleBasedScanner getStringScanner() {
    return fStringScanner;
  }

  /**
   * Computes and returns the indent prefixes for space indentation and the given
   * <code>tabWidth</code>.
   * 
   * @param tabWidth the display tab width
   * @return the indent prefixes
   * @see #getIndentPrefixes(ISourceViewer, String)
   */
  @SuppressWarnings("unused")
  private String[] getIndentPrefixesForSpaces(int tabWidth) {
    String[] indentPrefixes = new String[tabWidth + 2];
    indentPrefixes[0] = getStringWithSpaces(tabWidth);

    for (int i = 0; i < tabWidth; i++) {
      String spaces = getStringWithSpaces(i);
      if (i < tabWidth) {
        indentPrefixes[i + 1] = spaces + '\t';
      } else {
        indentPrefixes[i + 1] = new String(spaces);
      }
    }

    indentPrefixes[tabWidth + 1] = ""; //$NON-NLS-1$

    return indentPrefixes;
  }

  /**
   * Returns the information presenter control creator. The creator is a factory creating the
   * presenter controls for the given source viewer. This implementation always returns a creator
   * for <code>DefaultInformationControl</code> instances.
   * 
   * @param sourceViewer the source viewer to be configured by this configuration
   * @return an information control creator
   */
  @SuppressWarnings("unused")
  private IInformationControlCreator getInformationPresenterControlCreator(
      ISourceViewer sourceViewer) {
    return new IInformationControlCreator() {
      @SuppressWarnings("deprecation")
      @Override
      public IInformationControl createInformationControl(Shell parent) {
        int shellStyle = SWT.RESIZE | SWT.TOOL;
        int style = SWT.V_SCROLL | SWT.H_SCROLL;
        return new DefaultInformationControl(
            parent,
            shellStyle,
            style,
            new HTMLTextPresenter(false));
      }
    };
  }

  /**
   * Returns the outline presenter control creator. The creator is a factory creating outline
   * presenter controls for the given source viewer. This implementation always returns a creator
   * for <code>DartOutlineInformationControl</code> instances.
   * 
   * @param sourceViewer the source viewer to be configured by this configuration
   * @param commandId the ID of the command that opens this control
   * @return an information control creator
   */
  private IInformationControlCreator getOutlinePresenterControlCreator(ISourceViewer sourceViewer,
      final String commandId) {
    return new IInformationControlCreator() {
      @Override
      public IInformationControl createInformationControl(Shell parent) {
        int shellStyle = SWT.RESIZE;
        if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
          return new DartOutlineInformationControl_NEW(parent, shellStyle, fTextEditor);
        } else {
          return new DartOutlineInformationControl(parent, shellStyle, fTextEditor);
        }
      }
    };
  }

  /**
   * Returns the settings for the given section.
   * 
   * @param sectionName the section name
   * @return the settings
   */
  private IDialogSettings getSettings(String sectionName) {
    IDialogSettings settings = DartToolsPlugin.getDefault().getDialogSettings().getSection(
        sectionName);
    if (settings == null) {
      settings = DartToolsPlugin.getDefault().getDialogSettings().addNewSection(sectionName);
    }

    return settings;
  }

  /**
   * Creates and returns a String with <code>count</code> spaces.
   * 
   * @param count the space count
   * @return the string with the spaces
   */
  private String getStringWithSpaces(int count) {
    char[] spaceChars = new char[count];
    Arrays.fill(spaceChars, ' ');
    return new String(spaceChars);
  }

  /**
   * Initializes the scanners.
   */
  private void initializeScanners() {
    Assert.isTrue(isNewSetup());
    fCodeScanner = new DartCodeScanner(getColorManager(), fPreferenceStore);
    fMultilineCommentScanner = new DartCommentScanner(
        getColorManager(),
        fPreferenceStore,
        IDartColorConstants.JAVA_MULTI_LINE_COMMENT);
    fSinglelineCommentScanner = new DartCommentScanner(
        getColorManager(),
        fPreferenceStore,
        IDartColorConstants.JAVA_SINGLE_LINE_COMMENT);
    fStringScanner = new SingleTokenDartScanner(
        getColorManager(),
        fPreferenceStore,
        IDartColorConstants.JAVA_STRING);
    fMultilineStringScanner = new DartMultilineStringScanner(
        getColorManager(),
        fPreferenceStore,
        IDartColorConstants.DART_MULTI_LINE_STRING);
    fJavaDocScanner = new DartDocScanner(getColorManager(), fPreferenceStore);
  }

  /**
   * @return <code>true</code> iff the new setup without text tools is in use.
   */
  private boolean isNewSetup() {
    return fJavaTextTools == null;
  }

}
