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

package com.google.dart.tools.ui.web.json;

import com.google.dart.tools.ui.web.utils.WebEditor;

import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;

/**
 * An editor for json files.
 */
public class JsonEditor extends WebEditor {

  /**
   * Create a new JsonEditor.
   */
  public JsonEditor() {
    setRulerContextMenuId("#DartJSONEditorRulerContext");
    setSourceViewerConfiguration(new JsonSourceViewerConfiguration(this));
    setDocumentProvider(new JsonDocumentProvider());
  }

  @Override
  protected void configureSourceViewerDecorationSupport(SourceViewerDecorationSupport support) {
    ICharacterPairMatcher matcher = new DefaultCharacterPairMatcher(
        new char[] {'{', '}', '[', ']'},
        IDocumentExtension3.DEFAULT_PARTITIONING);
    support.setCharacterPairMatcher(matcher);
    support.setMatchingCharacterPainterPreferenceKeys(MATCHING_BRACKETS, MATCHING_BRACKETS_COLOR);

    super.configureSourceViewerDecorationSupport(support);
  }

  @Override
  protected void handleDocumentModified() {

  }

  @Override
  protected void handleReconcilation(IRegion partition) {

  }

  protected boolean isManifestEditor() {
    IEditorInput input = getEditorInput();

    if (input != null) {
      return "manifest.json".equals(input.getName());
    } else {
      return false;
    }
  }

  @Override
  protected boolean isTabsToSpacesConversionEnabled() {
    return true;
  }

}
