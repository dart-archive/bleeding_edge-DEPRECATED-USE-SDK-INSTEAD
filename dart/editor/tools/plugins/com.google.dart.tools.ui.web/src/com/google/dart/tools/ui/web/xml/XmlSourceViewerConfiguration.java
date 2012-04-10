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
package com.google.dart.tools.ui.web.xml;

import com.google.dart.tools.ui.web.DartWebPlugin;
import com.google.dart.tools.ui.web.utils.WebEditorReconcilingStrategy;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

/**
 * The SourceViewerConfiguration for the xml editor.
 */
public class XmlSourceViewerConfiguration extends SourceViewerConfiguration {
  private XmlEditor editor;
  private XmlTagScanner tagScanner;
  private XmlScanner scanner;
  private MonoReconciler reconciler;

  public XmlSourceViewerConfiguration(XmlEditor editor) {
    this.editor = editor;
  }

  @Override
  public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
    return new String[] {
        IDocument.DEFAULT_CONTENT_TYPE, XmlPartitionScanner.XML_COMMENT,
        XmlPartitionScanner.XML_TAG};
  }

  @Override
  public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
    PresentationReconciler reconciler = new PresentationReconciler();

    DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getXMLTagScanner());
    reconciler.setDamager(dr, XmlPartitionScanner.XML_TAG);
    reconciler.setRepairer(dr, XmlPartitionScanner.XML_TAG);

    dr = new DefaultDamagerRepairer(getXMLScanner());
    reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
    reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

    XmlDamagerRepairer ndr = new XmlDamagerRepairer(new TextAttribute(
        DartWebPlugin.getPlugin().getEditorColor(DartWebPlugin.COLOR_COMMENTS)));
    reconciler.setDamager(ndr, XmlPartitionScanner.XML_COMMENT);
    reconciler.setRepairer(ndr, XmlPartitionScanner.XML_COMMENT);

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

  protected XmlScanner getXMLScanner() {
    if (scanner == null) {
      scanner = new XmlScanner();
      scanner.setDefaultReturnToken(new Token(new TextAttribute(null)));
    }
    return scanner;
  }

  protected XmlTagScanner getXMLTagScanner() {
    if (tagScanner == null) {
      tagScanner = new XmlTagScanner();
      tagScanner.setDefaultReturnToken(new Token(new TextAttribute(null)));
    }
    return tagScanner;
  }

}
