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

import com.google.dart.tools.ui.web.utils.WebEditor;
import com.google.dart.tools.ui.web.xml.model.XmlDocument;
import com.google.dart.tools.ui.web.xml.model.XmlParser;

import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * An editor for xml files.
 */
public class XmlEditor extends WebEditor {
  private XmlContentOutlinePage outlinePage;
  private XmlDocument model;

  public XmlEditor() {
    setRulerContextMenuId("#DartXmlEditorRulerContext");
    setSourceViewerConfiguration(new XmlSourceViewerConfiguration(this));
    setDocumentProvider(new XmlDocumentProvider());
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Object getAdapter(Class required) {
    if (IContentOutlinePage.class.equals(required)) {
      if (outlinePage == null) {
        outlinePage = new XmlContentOutlinePage(this);
      }

      return outlinePage;
    }

    return super.getAdapter(required);
  }

  protected XmlDocument getModel() {
    if (model == null) {
      //model = new XmlParser(getDocument()).parse();
      model = XmlParser.createEmpty();
    }

    return model;
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
