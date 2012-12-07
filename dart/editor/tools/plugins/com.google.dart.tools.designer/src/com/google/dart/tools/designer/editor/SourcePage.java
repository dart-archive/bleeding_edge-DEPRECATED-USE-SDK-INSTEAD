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
package com.google.dart.tools.designer.editor;

import com.google.dart.tools.designer.DartDesignerPlugin;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * "Source" page of {@link AbstractXmlEditor}.
 * 
 * @author scheglov_ke
 * @coverage XML.editor
 */
public final class SourcePage extends XmlEditorPage {
  private final Control m_control;
  private final ITextEditor m_xmlEditor;

  public SourcePage(ITextEditor xmlEditor, Control control) {
    m_xmlEditor = xmlEditor;
    m_control = control;
  }

  @Override
  public Control createControl(Composite parent) {
    return null;
  }

  @Override
  public Control getControl() {
    return m_control;
  }

  @Override
  public Image getImage() {
    return DartDesignerPlugin.getImage("editor_page_xml.png");
  }

  @Override
  public String getName() {
    return "Source";
  }

  /**
   * @return the underlying HTML editor.
   */
  public ITextEditor getXmlEditor() {
    return m_xmlEditor;
  }
}
