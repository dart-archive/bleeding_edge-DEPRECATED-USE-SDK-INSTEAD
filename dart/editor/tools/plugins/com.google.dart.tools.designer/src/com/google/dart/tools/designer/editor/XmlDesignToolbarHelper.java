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

import com.google.dart.tools.designer.editor.actions.DesignPageActions;

import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.wb.core.model.ObjectInfo;
import org.eclipse.wb.gef.core.IEditPartViewer;
import org.eclipse.wb.internal.core.editor.DesignToolbarHelper;

/**
 * Helper for managing actions on internal {@link ToolBarManager} of {@link XmlEditorPage}.
 * 
 * @author scheglov_ke
 * @coverage XML.editor
 */
public final class XmlDesignToolbarHelper extends DesignToolbarHelper {
  private DesignPageActions m_pageActions;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public XmlDesignToolbarHelper(ToolBar toolBar) {
    super(toolBar);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Access
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Initializes with {@link DesignPageActions} and {@link IEditPartViewer}.
   */
  public void initialize(DesignPageActions pageActions, IEditPartViewer viewer) {
    super.initialize(viewer);
    m_pageActions = pageActions;
  }

  /**
   * Fills {@link ToolBar} with actions.
   */
  @Override
  public void fill() {
    // TODO(scheglov)
//    {
//      m_toolBarManager.add(m_pageActions.getTestAction());
//      m_toolBarManager.add(m_pageActions.getRefreshAction());
//      m_toolBarManager.add(new Separator());
//    }
    super.fill();
    {
      m_toolBarManager.add(m_pageActions.getAssistantAction());
      m_toolBarManager.add(new Separator());
    }
    super.fill2();
  }

  @Override
  public void setRoot(ObjectInfo rootObject) {
    super.setRoot(rootObject);
    m_toolBarManager.getControl().getParent().layout();
  }
}
