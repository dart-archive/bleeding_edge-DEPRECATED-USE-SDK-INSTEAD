/*******************************************************************************
 * Copyright (c) 2002, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.catalog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.common.uriresolver.internal.util.URIHelper;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalogElement;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalogEntry;
import org.eclipse.wst.xml.core.internal.catalog.provisional.IDelegateCatalog;
import org.eclipse.wst.xml.core.internal.catalog.provisional.INextCatalog;
import org.eclipse.wst.xml.core.internal.catalog.provisional.IRewriteEntry;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ISuffixEntry;

public class XMLCatalogEntryDetailsView {
  protected Text detailsText;
  protected ScrollBar verticalScroll, horizontalScroll;

  public XMLCatalogEntryDetailsView(Composite parent) {
    Color color = parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);

    detailsText = new Text(parent, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);

    GridData data = new GridData(GridData.FILL_BOTH);
    data.heightHint = 85;
    detailsText.setLayoutData(data);

    verticalScroll = detailsText.getVerticalBar();
    // verticalScroll.setVisible(false);
    horizontalScroll = detailsText.getHorizontalBar();
    detailsText.setEditable(false);
    detailsText.setBackground(color);
  }

  protected void setCatalogEntry(ICatalogEntry entry) {
    if (entry == null) {
      detailsText.setText(""); //$NON-NLS-1$
      return;
    }
    String value = getDisplayValue(entry != null ? entry.getURI() : ""); //$NON-NLS-1$
    String line0 = XMLCatalogMessages.UI_LABEL_ENTRY_ELEMENT_COLON + "\t\t"; //$NON-NLS-1$
    String line2 = XMLCatalogMessages.UI_LABEL_DETAILS_URI_COLON + "\t\t\t\t" + value; //$NON-NLS-1$
    String line1;
    if (value.startsWith("jar:file:")) { //$NON-NLS-1$
      String jarFile = URIUtils.convertURIToLocation(URIHelper.ensureURIProtocolFormat(value.substring(
          "jar:".length(), value.indexOf('!')))); //$NON-NLS-1$
      String internalFile = URIUtils.convertURIToLocation(URIHelper.ensureURIProtocolFormat("file://" + value.substring(value.indexOf('!') + 1))); //$NON-NLS-1$
      line1 = XMLCatalogMessages.UI_LABEL_DETAILS_URI_LOCATION
          + "\t\t\t" + internalFile + " " + XMLCatalogMessages.UI_LABEL_DETAILS_IN_JAR_FILE + " " + jarFile; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    } else {
      value = URIUtils.convertURIToLocation(value);
      line1 = XMLCatalogMessages.UI_LABEL_DETAILS_URI_LOCATION + "\t\t\t" + value; //$NON-NLS-1$

    }
    switch (entry.getEntryType()) {
      case ICatalogEntry.ENTRY_TYPE_PUBLIC:
        line0 += XMLCatalogMessages.UI_LABEL_PUBLIC;
        break;
      case ICatalogEntry.ENTRY_TYPE_SYSTEM:
        line0 += XMLCatalogMessages.UI_LABEL_SYSTEM;
        break;
      case ICatalogEntry.ENTRY_TYPE_URI:
        line0 += XMLCatalogMessages.UI_LABEL_URI;
        break;
    }
    value = entry != null ? getKeyTypeValue(entry) : ""; //$NON-NLS-1$
    String line3 = XMLCatalogMessages.UI_KEY_TYPE_DETAILS_COLON + "\t\t\t" + value; //$NON-NLS-1$
    value = getDisplayValue(entry != null ? entry.getKey() : ""); //$NON-NLS-1$
    String line4 = XMLCatalogMessages.UI_LABEL_DETAILS_KEY_COLON + "\t\t\t\t" + value; //$NON-NLS-1$
    String entireString = line0 + "\n" + line1 + "\n" + line2 + "\n" + line3 + "\n" + line4; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    detailsText.setText(entireString);
  }

  protected void setNextCatalog(INextCatalog nextCatalog) {
    String value = getDisplayValue(nextCatalog != null ? nextCatalog.getCatalogLocation() : ""); //$NON-NLS-1$
    String line0 = XMLCatalogMessages.UI_LABEL_ENTRY_ELEMENT_COLON
        + "\t\t" + XMLCatalogMessages.UI_LABEL_NEXT_CATALOG; //$NON-NLS-1$
    String line1 = XMLCatalogMessages.UI_LABEL_DETAILS_URI_LOCATION
        + "\t\t\t" + URIUtils.convertURIToLocation(value); //$NON-NLS-1$
    String line2 = XMLCatalogMessages.UI_LABEL_DETAILS_URI_COLON + "\t\t\t\t" + value; //$NON-NLS-1$
    String entireString = line0 + "\n" + line1 + "\n" + line2; //$NON-NLS-1$ //$NON-NLS-2$
    detailsText.setText(entireString);
  }

  protected void setSuffixEntry(ISuffixEntry element) {
    String value = getDisplayValue(element != null ? element.getURI() : ""); //$NON-NLS-1$
    String line0 = XMLCatalogMessages.UI_LABEL_ENTRY_ELEMENT_COLON
        + "\t\t" + XMLCatalogMessages.UI_LABEL_SUFFIX_ENTRY; //$NON-NLS-1$
    String line1 = XMLCatalogMessages.UI_LABEL_DETAILS_SUFFIX_COLON
        + "\t\t\t\t" + element.getSuffix(); //$NON-NLS-1$
    String line2 = XMLCatalogMessages.UI_LABEL_DETAILS_URI_LOCATION
        + "\t\t\t" + URIUtils.convertURIToLocation(value); //$NON-NLS-1$
    String line3 = XMLCatalogMessages.UI_KEY_TYPE_DETAILS_COLON + "\t\t\t"; //$NON-NLS-1$
    String uri = element.getURI();
    boolean isSchema = false;
    if (uri != null && uri.endsWith("xsd")) { //$NON-NLS-1$
      isSchema = true;
    }
    switch (element.getEntryType()) {
      case ISuffixEntry.SUFFIX_TYPE_SYSTEM:
        line3 += isSchema ? XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_XSD_SYSTEM
            : XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_DTD_SYSTEM;
        break;
      default:
      case ISuffixEntry.SUFFIX_TYPE_URI:
        line3 += isSchema ? XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_XSD_PUBLIC
            : XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_URI;
        break;
    }
    String entireString = line0 + "\n" + line1 + "\n" + line2 + "\n" + line3; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    detailsText.setText(entireString);
  }

  protected void setRewriteEntry(IRewriteEntry element) {
    String line0 = XMLCatalogMessages.UI_LABEL_ENTRY_ELEMENT_COLON
        + "\t\t" + XMLCatalogMessages.UI_LABEL_REWRITE_ENTRY; //$NON-NLS-1$
    String line1 = XMLCatalogMessages.UI_LABEL_START_STRING + "\t\t" + element.getStartString(); //$NON-NLS-1$
    String line2 = XMLCatalogMessages.UI_LABEL_REWRITE_PREFIX + "\t" + element.getRewritePrefix(); //$NON-NLS-1$
    String line3 = XMLCatalogMessages.UI_KEY_TYPE_DETAILS_COLON + "\t\t\t"; //$NON-NLS-1$
    switch (element.getEntryType()) {
      case IRewriteEntry.REWRITE_TYPE_SYSTEM:
        line3 += XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_DTD_SYSTEM;
        break;
      default:
      case IRewriteEntry.REWRITE_TYPE_URI:
        line3 += XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_URI;
        break;
    }
    String entireString = line0 + "\n" + line1 + "\n" + line2 + "\n" + line3; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    detailsText.setText(entireString);
  }

  protected void setDelegateCatalog(IDelegateCatalog element) {
    String value = getDisplayValue(element != null ? element.getCatalogLocation() : ""); //$NON-NLS-1$
    String line0 = XMLCatalogMessages.UI_LABEL_ENTRY_ELEMENT_COLON
        + "\t\t" + XMLCatalogMessages.UI_LABEL_DELEGATE_CATALOG; //$NON-NLS-1$
    String line1 = XMLCatalogMessages.UI_LABEL_START_STRING + "\t\t" + element.getStartString(); //$NON-NLS-1$
    String line2 = XMLCatalogMessages.UI_LABEL_DETAILS_URI_LOCATION
        + "\t\t\t" + URIUtils.convertURIToLocation(value); //$NON-NLS-1$
    String line3 = XMLCatalogMessages.UI_KEY_TYPE_DETAILS_COLON + "\t\t\t"; //$NON-NLS-1$
    switch (element.getEntryType()) {
      case IDelegateCatalog.DELEGATE_TYPE_PUBLIC:
        line3 += XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_DTD_PUBLIC;
        break;
      case IDelegateCatalog.DELEGATE_TYPE_SYSTEM:
        line3 += XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_DTD_SYSTEM;
        break;
      default:
      case IDelegateCatalog.DELEGATE_TYPE_URI:
        line3 += XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_URI;
        break;
    }
    String entireString = line0 + "\n" + line1 + "\n" + line2 + "\n" + line3; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    detailsText.setText(entireString);
  }

  public void setCatalogElement(ICatalogElement element) {
    // I wish we had a visitor for this kind of mess
    if (element instanceof ICatalogEntry)
      setCatalogEntry((ICatalogEntry) element);
    else if (element instanceof INextCatalog)
      setNextCatalog((INextCatalog) element);
    else if (element instanceof IDelegateCatalog)
      setDelegateCatalog((IDelegateCatalog) element);
    else if (element instanceof IRewriteEntry)
      setRewriteEntry((IRewriteEntry) element);
    else if (element instanceof ISuffixEntry)
      setSuffixEntry((ISuffixEntry) element);
    else
      setCatalogEntry(null); // Gives null text
  }

  protected String getDisplayValue(String string) {
    return string != null ? string : ""; //$NON-NLS-1$
  }

  protected String getKeyTypeValue(ICatalogEntry entry) {
    String result = null;
    if ((entry.getURI() != null) && entry.getURI().endsWith("xsd")) //$NON-NLS-1$
    {
      result = (entry.getEntryType() == ICatalogEntry.ENTRY_TYPE_URI)
          ? XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_XSD_PUBLIC
          : XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_XSD_SYSTEM;
    } else {
      switch (entry.getEntryType()) {
        case ICatalogEntry.ENTRY_TYPE_PUBLIC:
          result = XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_DTD_PUBLIC;
          break;
        case ICatalogEntry.ENTRY_TYPE_SYSTEM:
          result = XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_DTD_SYSTEM;
          break;
        default:
          result = XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_URI;
          break;
      }

    }
    return result;
  }
}
