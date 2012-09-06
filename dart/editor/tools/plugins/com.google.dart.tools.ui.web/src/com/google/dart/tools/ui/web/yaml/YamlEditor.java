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
package com.google.dart.tools.ui.web.yaml;

import com.google.dart.tools.ui.web.utils.WebEditor;
import com.google.dart.tools.ui.web.yaml.model.YamlDocument;
import com.google.dart.tools.ui.web.yaml.model.YamlParser;

import org.eclipse.jface.text.IRegion;

/**
 * An editor for yaml files.
 */
public class YamlEditor extends WebEditor {

  private YamlDocument model;

  /**
   * Create a new YamlEditor.
   */
  public YamlEditor() {
    setRulerContextMenuId("#DartYamlEditorRulerContext");
    setSourceViewerConfiguration(new YamlSourceViewerConfiguration(this));
    setDocumentProvider(new YamlDocumentProvider());
  }

  public YamlDocument getModel() {
    if (model == null) {
      model = YamlParser.createEmpty();
    }

    return model;
  }

  @Override
  protected void handleDocumentModified() {
    model = null;
  }

  @Override
  protected void handleReconcilation(IRegion partition) {

  }

  @Override
  protected boolean isTabsToSpacesConversionEnabled() {
    return true;
  }

}
