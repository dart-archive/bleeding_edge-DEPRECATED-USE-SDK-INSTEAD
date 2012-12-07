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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.wb.internal.core.editor.errors.WarningComposite;

/**
 * Implementation for XML.
 * 
 * @author scheglov_ke
 * @coverage XML.editor
 */
public final class XmlWarningComposite extends WarningComposite {
  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public XmlWarningComposite(Composite parent, int style) {
    super(parent, style);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Operations
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  protected void doRefresh() {
    // TODO(scheglov)
//    new RefreshAction().run();
  }

  @Override
  protected void doShowSource(int sourcePosition) {
    // TODO(scheglov)
//    SwitchAction.showSource(sourcePosition);
  }
}
