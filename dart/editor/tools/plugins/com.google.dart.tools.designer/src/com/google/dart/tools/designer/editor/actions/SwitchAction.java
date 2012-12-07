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
package com.google.dart.tools.designer.editor.actions;

import com.google.dart.tools.designer.editor.AbstractXmlEditor;

/**
 * This action does switching between "Source" and "Design" tabs of {@link AbstractXmlEditor}.
 * 
 * @author scheglov_ke
 * @coverage XML.editor.action
 */
public class SwitchAction extends EditorRelatedAction {
  ////////////////////////////////////////////////////////////////////////////
  //
  // IActionDelegate
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public void run() {
    AbstractXmlEditor editor = getEditor();
    if (editor != null) {
      editor.switchSourceDesign();
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Utils
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Shows "Source" page.
   */
  public static void showSource() {
    showSource(-1);
  }

  /**
   * Shows "Source" page and at given source position.
   * 
   * @param position the position to show in source, if <code>-1</code>, then ignored.
   */
  public static void showSource(int position) {
    AbstractXmlEditor editor = getActiveEditor();
    if (editor != null) {
      editor.showSource();
      if (position != -1) {
        editor.showSourcePosition(position);
      }
    }
  }
}
