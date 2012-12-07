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

import com.google.common.collect.Lists;
import com.google.dart.tools.designer.editor.actions.DesignPageActions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.wb.core.model.ObjectInfo;
import org.eclipse.wb.gef.core.EditPart;
import org.eclipse.wb.gef.core.IEditPartViewer;
import org.eclipse.wb.internal.core.utils.execution.ExecutionUtils;
import org.eclipse.wb.internal.core.utils.execution.RunnableEx;
import org.eclipse.wb.internal.gef.core.ContextMenuProvider;
import org.eclipse.wb.internal.gef.core.MultiSelectionContextMenuProvider;

import java.util.List;

/**
 * Implementation of {@link ContextMenuProvider} for Designer.
 * 
 * @author scheglov_ke
 * @coverage XML.editor
 */
public final class DesignContextMenuProvider extends MultiSelectionContextMenuProvider {
  ////////////////////////////////////////////////////////////////////////////
  //
  // Groups
  //
  ////////////////////////////////////////////////////////////////////////////
  private static final String GROUP_BASE = "org.eclipse.wb.popup.group.";
  public static final String GROUP_TOP = GROUP_BASE + "top";
  public static final String GROUP_EDIT = GROUP_BASE + "edit";
  public static final String GROUP_EDIT2 = GROUP_BASE + "edit2";
  public static final String GROUP_EVENTS = GROUP_BASE + "events";
  public static final String GROUP_EVENTS2 = GROUP_BASE + "events2";
  public static final String GROUP_LAYOUT = GROUP_BASE + "layout";
  public static final String GROUP_CONSTRAINTS = GROUP_BASE + "constraints";
  public static final String GROUP_INHERITANCE = GROUP_BASE + "inheritance";
  public static final String GROUP_ADDITIONAL = GROUP_BASE + "additional";

  /**
   * Adds standard groups into given {@link IMenuManager}.
   */
  public static void addGroups(IMenuManager manager) {
    manager.add(new Separator(GROUP_TOP));
    manager.add(new Separator(GROUP_EDIT));
    manager.add(new Separator(GROUP_EDIT2));
    manager.add(new Separator(GROUP_EVENTS));
    manager.add(new Separator(GROUP_EVENTS2));
    manager.add(new Separator(GROUP_LAYOUT));
    manager.add(new Separator(GROUP_CONSTRAINTS));
    manager.add(new Separator(GROUP_INHERITANCE));
    manager.add(new Separator(GROUP_ADDITIONAL));
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Instance fields
  //
  ////////////////////////////////////////////////////////////////////////////
  private final DesignPageActions m_pageActions;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public DesignContextMenuProvider(IEditPartViewer viewer, DesignPageActions pageActions) {
    super(viewer);
    m_pageActions = pageActions;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // MultiSelectionContextMenuProvider
  //
  ////////////////////////////////////////////////////////////////////////////
  private List<ObjectInfo> m_selectedObjects;

  @Override
  protected void preprocessSelection(List<EditPart> editParts) {
    super.preprocessSelection(editParts);
    // prepare selected ObjectInfo's
    m_selectedObjects = Lists.newArrayList();
    for (EditPart editPart : editParts) {
      if (editPart.getModel() instanceof ObjectInfo) {
        m_selectedObjects.add((ObjectInfo) editPart.getModel());
      }
    }
  }

  @Override
  protected void buildContextMenu(final EditPart editPart, final IMenuManager manager) {
    addGroups(manager);
    // edit
    {
      manager.appendToGroup(GROUP_EDIT, m_pageActions.getCutAction());
      manager.appendToGroup(GROUP_EDIT, m_pageActions.getCopyAction());
      manager.appendToGroup(GROUP_EDIT, m_pageActions.getPasteAction());
      manager.appendToGroup(GROUP_EDIT, m_pageActions.getDeleteAction());
    }
    // edit2
    {
      manager.appendToGroup(GROUP_EDIT2, m_pageActions.getTestAction());
      manager.appendToGroup(GROUP_EDIT2, m_pageActions.getRefreshAction());
    }
    // send notification
    if (editPart.getModel() instanceof ObjectInfo) {
      ExecutionUtils.runLog(new RunnableEx() {
        @Override
        public void run() throws Exception {
          ObjectInfo object = (ObjectInfo) editPart.getModel();
          object.getBroadcastObject().addContextMenu(m_selectedObjects, object, manager);
        }
      });
    }
  }
}
