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
package com.google.dart.tools.ui.web.html;

import com.google.dart.tools.ui.internal.text.functions.DartColorManager;
import com.google.dart.tools.ui.text.DartIndiscriminateDamager;
import com.google.dart.tools.ui.web.DartWebPlugin;
import com.google.dart.tools.ui.web.css.CssContentAssistProcessor;
import com.google.dart.tools.ui.web.css.CssScanner;
import com.google.dart.tools.ui.web.utils.WebEditorReconcilingStrategy;

import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.URLHyperlinkDetector;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.HippieProposalProcessor;

/**
 * The SourceViewerConfiguration for the html editor.
 */
public class HtmlSourceViewerConfiguration extends TextSourceViewerConfiguration {
  private HtmlEditor editor;

  private RuleBasedScanner htmlScanner;
  private RuleBasedScanner cssScanner;
  private RuleBasedScanner codeScanner;
  private RuleBasedScanner templateScanner;

  private MonoReconciler reconciler;

  public HtmlSourceViewerConfiguration(HtmlEditor editor) {
    super(editor.getPreferences());

    this.editor = editor;
  }

  @Override
  public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
    if (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType)
        || HtmlEditor.HTML_BRACKET_PARTITION.equals(contentType)) {
      return new IAutoEditStrategy[] {new HtmlAutoIndentStrategy()};
    } else {
      return super.getAutoEditStrategies(sourceViewer, contentType);
    }
  }

  @Override
  public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
    return new String[] {
        IDocument.DEFAULT_CONTENT_TYPE, HtmlEditor.HTML_COMMENT_PARTITION,
        HtmlEditor.HTML_BRACKET_PARTITION, HtmlEditor.HTML_STYLE_PARTITION,
        HtmlEditor.HTML_CODE_PARTITION, HtmlEditor.HTML_TEMPLATE_PARTITION};
  }

  @Override
  public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
    ContentAssistant assistant = new ContentAssistant();

    assistant.enableAutoActivation(true);

    HtmlContentAssistProcessor htmlContentAssist = new HtmlContentAssistProcessor(editor);

    assistant.setContentAssistProcessor(htmlContentAssist, IDocument.DEFAULT_CONTENT_TYPE);

    assistant.setContentAssistProcessor(htmlContentAssist, HtmlEditor.HTML_BRACKET_PARTITION);

    assistant.setContentAssistProcessor(
        new CssContentAssistProcessor(),
        HtmlEditor.HTML_STYLE_PARTITION);

    // TODO: add support for template {{ }} content assist
    assistant.setContentAssistProcessor(
        new HippieProposalProcessor(),
        HtmlEditor.HTML_TEMPLATE_PARTITION);

    // TODO: add support for Dart content assist
//    assistant.setContentAssistProcessor(new DartCompletionProcessor(
//        editor,
//        assistant,
//        IDocument.DEFAULT_CONTENT_TYPE), HtmlEditor.HTML_CODE_PARTITION);

    return assistant;
  }

  @Override
  public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
    if (sourceViewer == null) {
      return null;
    }

    return new IHyperlinkDetector[] {new URLHyperlinkDetector(), new HtmlHyperlinkDetector(editor)};
  }

  @Override
  public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
    PresentationReconciler reconciler = new PresentationReconciler();

    DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getHtmlScanner());
    reconciler.setDamager(dr, HtmlEditor.HTML_BRACKET_PARTITION);
    reconciler.setRepairer(dr, HtmlEditor.HTML_BRACKET_PARTITION);

    DefaultDamagerRepairer cssDR = new DefaultDamagerRepairer(getCssScanner());
    reconciler.setDamager(new DartIndiscriminateDamager(), HtmlEditor.HTML_STYLE_PARTITION);
    reconciler.setRepairer(cssDR, HtmlEditor.HTML_STYLE_PARTITION);

    DefaultDamagerRepairer templateDR = new DefaultDamagerRepairer(getTemplateScanner());
    reconciler.setDamager(templateDR, HtmlEditor.HTML_TEMPLATE_PARTITION);
    reconciler.setRepairer(templateDR, HtmlEditor.HTML_TEMPLATE_PARTITION);

    DefaultDamagerRepairer codeDR = new DefaultDamagerRepairer(getCodeScanner());
    reconciler.setDamager(new DartIndiscriminateDamager(), HtmlEditor.HTML_CODE_PARTITION);
    reconciler.setRepairer(codeDR, HtmlEditor.HTML_CODE_PARTITION);

    HtmlDamagerRepairer ndr = new HtmlDamagerRepairer(new TextAttribute(
        DartWebPlugin.getPlugin().getEditorColor(DartWebPlugin.COLOR_COMMENTS)));
    reconciler.setDamager(ndr, HtmlEditor.HTML_COMMENT_PARTITION);
    reconciler.setRepairer(ndr, HtmlEditor.HTML_COMMENT_PARTITION);

    return reconciler;
  }

  @Override
  public IReconciler getReconciler(ISourceViewer sourceViewer) {
    if (reconciler == null && sourceViewer != null) {
      reconciler = new MonoReconciler(new WebEditorReconcilingStrategy(editor), false);
      reconciler.setDelay(500);
    }

    return reconciler;
  }

  /**
   * Returns the visual width of the tab character. This implementation always returns 2.
   * 
   * @param sourceViewer the source viewer to be configured by this configuration
   * @return the tab width
   */
  @Override
  public int getTabWidth(ISourceViewer sourceViewer) {
    return 2; // TODO Read from preferences so users can change it
  }

  protected RuleBasedScanner getCodeScanner() {
    if (codeScanner == null) {
      // TODO: We'll need the ability to have several different types of Dart partitions
      // inside html files.
      codeScanner = new HtmlDartScanner(new DartColorManager(), editor.getPreferences());
    }

    return codeScanner;
  }

  protected RuleBasedScanner getCssScanner() {
    if (cssScanner == null) {
      cssScanner = new CssScanner();
      cssScanner.setDefaultReturnToken(new Token(new TextAttribute(null)));
    }

    return cssScanner;
  }

  protected RuleBasedScanner getHtmlScanner() {
    if (htmlScanner == null) {
      htmlScanner = new HtmlTagScanner();
      htmlScanner.setDefaultReturnToken(new Token(new TextAttribute(null)));
    }

    return htmlScanner;
  }

  protected RuleBasedScanner getTemplateScanner() {
    if (templateScanner == null) {
      // TODO(devoncarew): upgrade this to a better scanner (when we know more about the format)
      templateScanner = new RuleBasedScanner();
      IToken codeToken = new Token(new TextAttribute(DartWebPlugin.getPlugin().getEditorColor(
          DartWebPlugin.COLOR_STATIC_FIELD), null, SWT.ITALIC));

      templateScanner.setDefaultReturnToken(codeToken);
    }

    return templateScanner;
  }

}
