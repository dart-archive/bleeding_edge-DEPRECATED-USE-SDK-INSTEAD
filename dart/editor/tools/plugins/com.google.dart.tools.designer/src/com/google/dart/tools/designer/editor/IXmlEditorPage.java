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

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Interface for page in {@link AbstractXmlEditor}.
 * 
 * @author scheglov_ke
 * @coverage XML.editor
 */
public interface IXmlEditorPage {
  ////////////////////////////////////////////////////////////////////////////
  //
  // Life cycle
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Initialize this page for given {@link AbstractXmlEditor}.
   */
  void initialize(AbstractXmlEditor editor);

  /**
   * Disposes this page.
   */
  void dispose();

  /**
   * Notifies when this page become active or inactive.
   */
  void setActive(boolean active);

  ////////////////////////////////////////////////////////////////////////////
  //
  // Page
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * @return the index of this page into multi-page editor.
   */
  int getPageIndex();

  /**
   * Sets index of this page into multi-page editor.
   */
  void setPageIndex(int index);

  ////////////////////////////////////////////////////////////////////////////
  //
  // GUI
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Creates the {@link Control} for this page.
   */
  Control createControl(Composite parent);

  /**
   * @return the {@link Control} of this page.
   */
  Control getControl();

  ////////////////////////////////////////////////////////////////////////////
  //
  // Presentation
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * @return the display name for this page.
   */
  String getName();

  /**
   * @return the display {@link Image} image for this page.
   */
  Image getImage();
}
