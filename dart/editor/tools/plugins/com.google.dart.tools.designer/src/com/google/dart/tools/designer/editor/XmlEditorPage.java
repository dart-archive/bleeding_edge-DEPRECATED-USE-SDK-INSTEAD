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

/**
 * Abstract {@link IXmlEditorPage}.
 * 
 * @author scheglov_ke
 * @coverage XML.editor
 */
public abstract class XmlEditorPage implements IXmlEditorPage {
  protected AbstractXmlEditor m_editor;
  protected boolean m_active;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Life cycle
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public void initialize(AbstractXmlEditor editor) {
    m_editor = editor;
  }

  @Override
  public void dispose() {
  }

  @Override
  public void setActive(boolean active) {
    m_active = active;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Page
  //
  ////////////////////////////////////////////////////////////////////////////
  private int m_pageIndex;

  @Override
  public int getPageIndex() {
    return m_pageIndex;
  }

  @Override
  public void setPageIndex(int index) {
    m_pageIndex = index;
  }
}
