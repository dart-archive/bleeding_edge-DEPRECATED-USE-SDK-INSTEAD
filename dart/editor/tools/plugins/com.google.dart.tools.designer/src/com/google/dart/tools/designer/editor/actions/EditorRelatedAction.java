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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.wb.internal.core.DesignerPlugin;

/**
 * Abstract superclass for actions of {@link AbstractXmlEditor}.
 * 
 * @author scheglov_ke
 * @coverage XML.editor.action
 */
public abstract class EditorRelatedAction extends Action implements IEditorActionDelegate {
  private AbstractXmlEditor m_editor;

  ////////////////////////////////////////////////////////////////////////////
  //
  // IEditorActionDelegate
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public final void setActiveEditor(IAction action, IEditorPart editor) {
    m_editor = null;
    if (editor instanceof AbstractXmlEditor) {
      m_editor = (AbstractXmlEditor) editor;
    }
    setEnabled(m_editor != null);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // IActionDelegate
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public void selectionChanged(IAction action, ISelection selection) {
  }

  @Override
  public void run(IAction action) {
    run();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Utils
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * @return the active {@link AbstractXmlEditor}.
   */
  protected final AbstractXmlEditor getEditor() {
    AbstractXmlEditor designerEditor = m_editor;
    if (designerEditor == null) {
      designerEditor = getActiveEditor();
    }
    return designerEditor;
  }

  /**
   * @return the active {@link AbstractXmlEditor}.
   */
  public static AbstractXmlEditor getActiveEditor() {
    IEditorPart editor = DesignerPlugin.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
    if (editor != null && editor instanceof AbstractXmlEditor) {
      return (AbstractXmlEditor) editor;
    }
    return null;
  }
}
