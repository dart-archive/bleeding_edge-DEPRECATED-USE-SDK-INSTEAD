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
package com.google.dart.tools.ui.web.css;

import com.google.dart.tools.ui.web.DartWebPlugin;
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
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

/**
 * The SourceViewerConfiguration for the css editor.
 */
public class CssSourceViewerConfiguration extends SourceViewerConfiguration {
  private CssEditor editor;
  private CssScanner scanner;
  private MonoReconciler reconciler;

  public CssSourceViewerConfiguration(CssEditor editor) {
    this.editor = editor;
  }

  @Override
  public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
    if (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType)) {
      return new IAutoEditStrategy[] {new CssAutoIndentStrategy()};
    } else {
      return super.getAutoEditStrategies(sourceViewer, contentType);
    }
  }

  @Override
  public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
    return new String[] {IDocument.DEFAULT_CONTENT_TYPE, CssPartitionScanner.CSS_COMMENT};
  }

  @Override
  public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
    ContentAssistant assistant = new ContentAssistant();

    assistant.setContentAssistProcessor(
        new CssContentAssistProcessor(),
        IDocument.DEFAULT_CONTENT_TYPE);

    return assistant;
  }

  @Override
  public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
    PresentationReconciler reconciler = new PresentationReconciler();

    DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getScanner());
    reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
    reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

    CssDamagerRepairer ndr = new CssDamagerRepairer(new TextAttribute(
        DartWebPlugin.getPlugin().getEditorColor(DartWebPlugin.COLOR_COMMENTS)));
    reconciler.setDamager(ndr, CssPartitionScanner.CSS_COMMENT);
    reconciler.setRepairer(ndr, CssPartitionScanner.CSS_COMMENT);

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

  protected CssScanner getScanner() {
    if (scanner == null) {
      scanner = new CssScanner();
      scanner.setDefaultReturnToken(new Token(new TextAttribute(null)));
    }
    return scanner;
  }

}
