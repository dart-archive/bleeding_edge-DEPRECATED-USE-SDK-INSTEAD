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

import com.google.dart.tools.ui.web.css.model.CssDocument;
import com.google.dart.tools.ui.web.css.model.CssParser;
import com.google.dart.tools.ui.web.utils.WebEditor;

import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * An editor for CSS files.
 */
public class CssEditor extends WebEditor {
  private CssContentOutlinePage outlinePage;
  private CssDocument model;

  /**
   * Create a new CssEditor.
   */
  public CssEditor() {
    setRulerContextMenuId("#DartCssEditorRulerContext");
    setSourceViewerConfiguration(new CssSourceViewerConfiguration(this));
    setDocumentProvider(new CssDocumentProvider());
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Object getAdapter(Class required) {
    if (IContentOutlinePage.class.equals(required)) {
      if (outlinePage == null) {
        outlinePage = new CssContentOutlinePage(this);
      }

      return outlinePage;
    }

    return super.getAdapter(required);
  }

  public CssDocument getModel() {
    if (model == null) {
      //model = new CssParser(getDocument()).parse();
      model = CssParser.createEmpty();
    }

    return model;
  }

  @Override
  protected void configureSourceViewerDecorationSupport(SourceViewerDecorationSupport support) {
    ICharacterPairMatcher matcher = new DefaultCharacterPairMatcher(
        new char[] {'{', '}'},
        IDocumentExtension3.DEFAULT_PARTITIONING);
    support.setCharacterPairMatcher(matcher);
    support.setMatchingCharacterPainterPreferenceKeys(MATCHING_BRACKETS, MATCHING_BRACKETS_COLOR);

    super.configureSourceViewerDecorationSupport(support);
  }

  @Override
  protected void handleDocumentModified() {
    model = null;
  }

  @Override
  protected void handleReconcilation(IRegion partition) {
    if (outlinePage != null) {
      outlinePage.handleEditorReconcilation();
    }
  }

}
