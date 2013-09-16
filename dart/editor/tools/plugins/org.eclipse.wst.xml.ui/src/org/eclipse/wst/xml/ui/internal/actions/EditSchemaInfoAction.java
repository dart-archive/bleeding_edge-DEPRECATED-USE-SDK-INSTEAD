/*******************************************************************************
 * Copyright (c) 2001, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.actions;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.xml.core.internal.contentmodel.util.DOMNamespaceInfoManager;
import org.eclipse.wst.xml.core.internal.contentmodel.util.NamespaceInfo;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;
import org.eclipse.wst.xml.ui.internal.dialogs.EditSchemaInfoDialog;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * EditDoctypeAction
 */
public class EditSchemaInfoAction extends NodeAction {
  protected AbstractNodeActionManager manager;
  protected DOMNamespaceInfoManager namespaceInfoManager = new DOMNamespaceInfoManager();
  protected Node node;
  protected String resourceLocation;
  protected String title;

  public EditSchemaInfoAction(AbstractNodeActionManager manager, Node node,
      String resourceLocation, String title) {
    this.manager = manager;
    this.node = node;
    setText(title);
    this.resourceLocation = resourceLocation;
    this.title = title;
  }

  protected Map createPrefixMapping(List oldList, List newList) {
    Map map = new Hashtable();

    Hashtable oldURIToPrefixTable = new Hashtable();
    for (Iterator i = oldList.iterator(); i.hasNext();) {
      NamespaceInfo oldInfo = (NamespaceInfo) i.next();
      oldURIToPrefixTable.put(oldInfo.uri, oldInfo);
    }

    for (Iterator i = newList.iterator(); i.hasNext();) {
      NamespaceInfo newInfo = (NamespaceInfo) i.next();
      NamespaceInfo oldInfo = (NamespaceInfo) oldURIToPrefixTable.get(newInfo.uri != null
          ? newInfo.uri : ""); //$NON-NLS-1$

      // if oldInfo is non null ... there's a matching URI in the old
      // set
      // we can use its prefix to detemine out mapping
      //
      // if oldInfo is null ... we use the 'oldCopy' we stashed away
      // assuming that the user changed the URI and the prefix
      if (oldInfo == null) {
        oldInfo = (NamespaceInfo) newInfo.getProperty("oldCopy"); //$NON-NLS-1$
      }

      if (oldInfo != null) {
        String newPrefix = newInfo.prefix != null ? newInfo.prefix : ""; //$NON-NLS-1$
        String oldPrefix = oldInfo.prefix != null ? oldInfo.prefix : ""; //$NON-NLS-1$
        if (!oldPrefix.equals(newPrefix)) {
          map.put(oldPrefix, newPrefix);
        }
      }
    }
    return map;
  }

  public Element getElement(Node node) {
    Element result = null;
    if (node.getNodeType() == Node.ELEMENT_NODE) {
      result = (Element) node;
    } else if (node.getNodeType() == Node.DOCUMENT_NODE) {
      result = getRootElement((Document) node);
    }
    return result;
  }

  public Element getRootElement(Document document) {
    Element rootElement = null;
    NodeList nodeList = document.getChildNodes();
    int nodeListLength = nodeList.getLength();
    for (int i = 0; i < nodeListLength; i++) {
      Node childNode = nodeList.item(i);
      if (childNode.getNodeType() == Node.ELEMENT_NODE) {
        rootElement = (Element) childNode;
        break;
      }
    }
    return rootElement;
  }

  public String getUndoDescription() {
    return title;
  }

  public void run() {
    Shell shell = XMLUIPlugin.getInstance().getWorkbench().getActiveWorkbenchWindow().getShell();
    if (validateEdit(manager.getModel(), shell)) {
      manager.beginNodeAction(this);

      // todo... change constructor to take an element
      Element element = getElement(node);
      if (element != null) {
        EditSchemaInfoDialog dialog = new EditSchemaInfoDialog(shell, new Path(resourceLocation));

        List namespaceInfoList = namespaceInfoManager.getNamespaceInfoList(element);
        List oldNamespaceInfoList = NamespaceInfo.cloneNamespaceInfoList(namespaceInfoList);

        // here we store a copy of the old info for each NamespaceInfo
        // this info will be used in createPrefixMapping() to figure out
        // how to update the document
        // in response to these changes
        for (Iterator i = namespaceInfoList.iterator(); i.hasNext();) {
          NamespaceInfo info = (NamespaceInfo) i.next();
          NamespaceInfo oldCopy = new NamespaceInfo(info);
          info.setProperty("oldCopy", oldCopy); //$NON-NLS-1$
        }

        dialog.setNamespaceInfoList(namespaceInfoList);
        dialog.create();
        // dialog.getShell().setSize(500, 300);
        dialog.getShell().setText(XMLUIMessages._UI_MENU_EDIT_SCHEMA_INFORMATION_TITLE);
        dialog.setBlockOnOpen(true);
        dialog.open();

        if (dialog.getReturnCode() == Window.OK) {
          List newInfoList = dialog.getNamespaceInfoList();
          namespaceInfoManager.removeNamespaceInfo(element);
          namespaceInfoManager.addNamespaceInfo(element, newInfoList, false);

          // see if we need to rename any prefixes
          Map prefixMapping = createPrefixMapping(oldNamespaceInfoList, namespaceInfoList);
          if (prefixMapping.size() > 0) {
            try {
              manager.getModel().aboutToChangeModel();
              ReplacePrefixAction replacePrefixAction = new ReplacePrefixAction(manager, element,
                  prefixMapping);
              replacePrefixAction.run();
            } finally {
              manager.getModel().changedModel();
            }
          }
        }
      }
      manager.endNodeAction(this);
    }
  }
}
