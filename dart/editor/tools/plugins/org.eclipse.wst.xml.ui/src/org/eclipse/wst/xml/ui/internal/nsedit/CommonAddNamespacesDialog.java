/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.nsedit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.xml.core.internal.XMLCorePlugin;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalog;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalogEntry;
import org.eclipse.wst.xml.core.internal.catalog.provisional.INextCatalog;
import org.eclipse.wst.xml.core.internal.contentmodel.util.NamespaceInfo;

public class CommonAddNamespacesDialog extends Dialog {
  protected CommonAddNamespacesControl addNamespacesControl;
  protected List existingNamespaces;
  protected List namespaceInfoList;
  protected Button okButton;
  protected HashMap preferredPrefixTable = new HashMap();
  protected IPath resourceLocation;
  protected String title;

  public CommonAddNamespacesDialog(Shell parentShell, String title, IPath resourceLocation,
      List existingNamespaces) {
    super(parentShell);
    this.resourceLocation = resourceLocation;
    setShellStyle(getShellStyle() | SWT.RESIZE);
    this.title = title;
    this.existingNamespaces = existingNamespaces;
    preferredPrefixTable.put("http://schemas.xmlsoap.org/wsdl/", "wsdl"); //$NON-NLS-1$ //$NON-NLS-2$
    preferredPrefixTable.put("http://schemas.xmlsoap.org/wsdl/soap/", "soap"); //$NON-NLS-1$ //$NON-NLS-2$
    preferredPrefixTable.put("http://schemas.xmlsoap.org/wsdl/http/", "http"); //$NON-NLS-1$ //$NON-NLS-2$
    preferredPrefixTable.put("http://schemas.xmlsoap.org/wsdl/mime/", "mime"); //$NON-NLS-1$ //$NON-NLS-2$
    preferredPrefixTable.put("http://schemas.xmlsoap.org/soap/encoding/", "soapenc"); //$NON-NLS-1$ //$NON-NLS-2$
    preferredPrefixTable.put("http://schemas.xmlsoap.org/soap/envelope/", "soapenv"); //$NON-NLS-1$ //$NON-NLS-2$
    preferredPrefixTable.put("http://www.w3.org/2001/XMLSchema-instance", "xsi"); //$NON-NLS-1$ //$NON-NLS-2$
    preferredPrefixTable.put("http://www.w3.org/2001/XMLSchema", "xsd"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  protected void addBuiltInNamespaces(List list) {
    String xsiNamespace = "http://www.w3.org/2001/XMLSchema-instance"; //$NON-NLS-1$
    String xsdNamespace = "http://www.w3.org/2001/XMLSchema"; //$NON-NLS-1$
    if (!isAlreadyDeclared(xsiNamespace)) {
      list.add(new NamespaceInfo("http://www.w3.org/2001/XMLSchema-instance", "xsi", null)); //$NON-NLS-1$ //$NON-NLS-2$
    }
    if (!isAlreadyDeclared(xsdNamespace)) {
      list.add(new NamespaceInfo("http://www.w3.org/2001/XMLSchema", "xsd", null)); //$NON-NLS-1$ //$NON-NLS-2$
    }
  }

  protected void addCatalogMapToList(ICatalog catalog, List list) {
    ICatalogEntry[] entries = catalog.getCatalogEntries();
    for (int i = 0; i < entries.length; i++) {
      ICatalogEntry entry = entries[i];
      if ((entry.getEntryType() == ICatalogEntry.ENTRY_TYPE_PUBLIC)
          && entry.getURI().endsWith(".xsd")) { //$NON-NLS-1$
        if (!isAlreadyDeclared(entry.getKey())) {
          NamespaceInfo namespaceInfo = new NamespaceInfo(entry.getKey(), "xx", null); //$NON-NLS-1$
          list.add(namespaceInfo);
        }
      }
    }
  }

  protected void buttonPressed(int buttonId) {
    if (buttonId == IDialogConstants.OK_ID) {
      namespaceInfoList = addNamespacesControl.getNamespaceInfoList();
    }
    super.buttonPressed(buttonId);
  }

  public void computeAddablePrefixes(List addableList, List exisitingList) {
    HashMap map = new HashMap();
    for (Iterator i = exisitingList.iterator(); i.hasNext();) {
      NamespaceInfo info = (NamespaceInfo) i.next();
      if (info.prefix != null) {
        map.put(info.prefix, info);
      }
    }
    for (Iterator i = addableList.iterator(); i.hasNext();) {
      NamespaceInfo info = (NamespaceInfo) i.next();
      if (info.uri != null) {
        String prefix = (String) preferredPrefixTable.get(info.uri);
        info.prefix = getUniquePrefix(map, prefix, info.uri);
        map.put(info.prefix, info);
      }
    }
  }

  public int createAndOpen() {
    create();
    getShell().setText(title);
    Rectangle r = getShell().getBounds();
    getShell().setBounds(r.x + 80, r.y + 80, r.width, r.height);
    setBlockOnOpen(true);
    return open();
  }

  protected void createButtonsForButtonBar(Composite parent) {
    okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
  }

  protected Control createContents(Composite parent) {
    Control control = super.createContents(parent);
    return control;
  }

  protected Control createDialogArea(Composite parent) {
    Composite dialogArea = (Composite) super.createDialogArea(parent);
    addNamespacesControl = new CommonAddNamespacesControl(dialogArea, SWT.NONE, resourceLocation);
    List list = new ArrayList();

    addBuiltInNamespaces(list);
    ICatalog defaultCatalog = XMLCorePlugin.getDefault().getDefaultXMLCatalog();
    INextCatalog[] nextCatalogs = defaultCatalog.getNextCatalogs();
    for (int i = 0; i < nextCatalogs.length; i++) {
      INextCatalog catalog = nextCatalogs[i];
      ICatalog referencedCatalog = catalog.getReferencedCatalog();
      if (referencedCatalog != null) {
        if (XMLCorePlugin.USER_CATALOG_ID.equals(referencedCatalog.getId())) {
          ICatalog userCatalog = referencedCatalog;
          addCatalogMapToList(userCatalog, list);

        } else if (XMLCorePlugin.SYSTEM_CATALOG_ID.equals(referencedCatalog.getId())) {
          ICatalog systemCatalog = referencedCatalog;
          addCatalogMapToList(systemCatalog, list);
        }
      }
    }

    computeAddablePrefixes(list, existingNamespaces);

    addNamespacesControl.setNamespaceInfoList(list);
    return dialogArea;
  }

  public List getNamespaceInfoList() {
    return namespaceInfoList;
  }

  protected String getPreferredPrefix(String namespaceURI) {
    return (String) preferredPrefixTable.get(namespaceURI);
  }

  private String getUniquePrefix(HashMap prefixMap, String prefix, String uri) {
    if (prefix == null) {
      int lastIndex = uri.lastIndexOf('/');
      if (lastIndex == uri.length() - 1) {
        uri = uri.substring(0, lastIndex);
        lastIndex = uri.lastIndexOf('/');
      }
      prefix = uri.substring(lastIndex + 1);
      if ((prefix.length() > 20) || (prefix.indexOf(':') != -1)) {
        prefix = null;
      }
    }
    if (prefix == null) {
      prefix = "p"; //$NON-NLS-1$
    }
    if (prefixMap.get(prefix) != null) {
      String base = prefix;
      for (int count = 0; prefixMap.get(prefix) != null; count++) {
        prefix = base + count;
      }
    }
    return prefix;
  }

  protected boolean isAlreadyDeclared(String namespaceURI) {
    boolean result = false;
    for (Iterator i = existingNamespaces.iterator(); i.hasNext();) {
      NamespaceInfo namespaceInfo = (NamespaceInfo) i.next();
      if (namespaceURI.equals(namespaceInfo.uri)) {
        result = true;
        break;
      }
    }
    return result;
  }
}
