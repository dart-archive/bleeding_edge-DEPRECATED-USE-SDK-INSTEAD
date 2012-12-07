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

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.wb.core.model.ObjectInfo;
import org.eclipse.wb.internal.core.editor.structure.property.IPropertiesToolBarContributor;

import java.util.List;

/**
 * {@link IPropertiesToolBarContributor} for XML.
 * 
 * @author scheglov_ke
 * @coverage XML.editor
 */
public final class XmlPropertiesToolBarContributor implements IPropertiesToolBarContributor {
  ////////////////////////////////////////////////////////////////////////////
  //
  // Instance
  //
  ////////////////////////////////////////////////////////////////////////////
  public static final IPropertiesToolBarContributor INSTANCE = new XmlPropertiesToolBarContributor();

  private XmlPropertiesToolBarContributor() {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // IPropertiesToolBarContributor
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public void contributeToolBar(IToolBarManager manager, final List<ObjectInfo> objects)
      throws Exception {
    addGotoDefinitionAction(manager, objects);
  }

  private void addGotoDefinitionAction(IToolBarManager manager, List<ObjectInfo> objects) {
    // TODO(scheglov)
//    if (objects.size() == 1 && objects.get(0) instanceof XmlObjectInfo) {
//      final XmlObjectInfo javaInfo = (XmlObjectInfo) objects.get(0);
//      IAction gotoDefinitionAction = new Action() {
//        @Override
//        public void run() {
//          int position = javaInfo.getElement().getOffset();
//          IDesignPageSite site = IDesignPageSite.Helper.getSite(javaInfo);
//          site.openSourcePosition(position);
//        }
//      };
//      gotoDefinitionAction.setImageDescriptor(DesignerPlugin.getImageDescriptor("structure/goto_definition.gif"));
//      gotoDefinitionAction.setToolTipText(Messages.ComponentsPropertiesPage_goDefinition);
//      manager.appendToGroup(GROUP_EDIT, gotoDefinitionAction);
//    }
  }
}
