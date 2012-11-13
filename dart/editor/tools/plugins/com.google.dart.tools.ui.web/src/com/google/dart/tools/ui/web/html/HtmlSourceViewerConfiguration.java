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

import com.google.dart.tools.ui.web.DartWebPlugin;
import com.google.dart.tools.ui.web.css.CssContentAssistProcessor;
import com.google.dart.tools.ui.web.css.CssScanner;
import com.google.dart.tools.ui.web.utils.WebEditorReconcilingStrategy;

import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;

/**
 * The SourceViewerConfiguration for the html editor.
 */
public class HtmlSourceViewerConfiguration extends SourceViewerConfiguration {
  private HtmlEditor editor;

  private RuleBasedScanner htmlScanner;
  private RuleBasedScanner cssScanner;
  private RuleBasedScanner codeScanner;
  private RuleBasedScanner templateScanner;

  private MonoReconciler reconciler;

  public HtmlSourceViewerConfiguration(HtmlEditor editor) {
    this.editor = editor;
  }

  @Override
  public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
    if (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType)) {
      return new IAutoEditStrategy[] {new HtmlAutoIndentStrategy()};
    } else {
      return super.getAutoEditStrategies(sourceViewer, contentType);
    }
  }

  @Override
  public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
    return new String[] {
        IDocument.DEFAULT_CONTENT_TYPE, HtmlEditor.HTML_COMMENT_PARTITION,
        HtmlEditor.HTML_STYLE_PARTITION, HtmlEditor.HTML_CODE_PARTITION,
        HtmlEditor.HTML_TEMPLATE_PARTITION};
  }

  @Override
  public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
    ContentAssistant assistant = new ContentAssistant();

    assistant.enableAutoActivation(true);

    assistant.setContentAssistProcessor(
        new HtmlContentAssistProcessor(),
        IDocument.DEFAULT_CONTENT_TYPE);
    assistant.setContentAssistProcessor(
        new CssContentAssistProcessor(),
        HtmlEditor.HTML_STYLE_PARTITION);
    // TODO: add support for template {{ }} content assist
//  assistant.setContentAssistProcessor(
//      template assist processor,
//      HtmlEditor.HTML_TEMPLATE_PARTITION);
    // TODO: add support for Dart content assist
//    assistant.setContentAssistProcessor(
//        dart content assist processor,
//        HtmlEditor.HTML_CODE_PARTITION);

    return assistant;
  }

  @Override
  public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
    PresentationReconciler reconciler = new PresentationReconciler();

    DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getHtmlScanner());
    reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
    reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

    DefaultDamagerRepairer cssDR = new DefaultDamagerRepairer(getCssScanner());
    reconciler.setDamager(cssDR, HtmlEditor.HTML_STYLE_PARTITION);
    reconciler.setRepairer(cssDR, HtmlEditor.HTML_STYLE_PARTITION);

    DefaultDamagerRepairer templateDR = new DefaultDamagerRepairer(getTemplateScanner());
    reconciler.setDamager(templateDR, HtmlEditor.HTML_TEMPLATE_PARTITION);
    reconciler.setRepairer(templateDR, HtmlEditor.HTML_TEMPLATE_PARTITION);

    DefaultDamagerRepairer codeDR = new DefaultDamagerRepairer(getCodeScanner());
    reconciler.setDamager(codeDR, HtmlEditor.HTML_CODE_PARTITION);
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
      // TODO(devoncarew): this RuleBasedScanner is just a place-holder until we can delegate
      // through to the DartCodeScanner class.
      codeScanner = new RuleBasedScanner();
      IToken codeToken = new Token(new TextAttribute(DartWebPlugin.getPlugin().getEditorColor(
          DartWebPlugin.COLOR_STATIC_FIELD)));
      codeScanner.setDefaultReturnToken(codeToken);

      // TODO: this needs to also handle comments and semantic highlighting
      // TODO: look at FastDartPartitionScanner
      // We'll need the ability to have several different types of Dart partitions
      // inside html files.
      //  codeScanner = new DartCodeScanner(new DartColorManager(), editor.getPreferences());
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
