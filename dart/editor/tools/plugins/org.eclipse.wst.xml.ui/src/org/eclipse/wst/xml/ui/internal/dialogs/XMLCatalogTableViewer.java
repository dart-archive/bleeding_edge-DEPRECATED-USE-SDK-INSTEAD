/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/

package org.eclipse.wst.xml.ui.internal.dialogs;

import com.ibm.icu.text.Collator;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.wst.common.uriresolver.internal.util.URIHelper;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalogEntry;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImageHelper;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImages;

public class XMLCatalogTableViewer extends TableViewer {

  public class CatalogEntryContentProvider implements IStructuredContentProvider {

    public void dispose() {
    }

    public Object[] getElements(Object element) {
      Object[] array = getXMLCatalogEntries().toArray();
      Comparator comparator = new Comparator() {
        public int compare(Object o1, Object o2) {
          int result = 0;
          if ((o1 instanceof ICatalogEntry) && (o2 instanceof ICatalogEntry)) {
            ICatalogEntry mappingInfo1 = (ICatalogEntry) o1;
            ICatalogEntry mappingInfo2 = (ICatalogEntry) o2;
            result = Collator.getInstance().compare(mappingInfo1.getKey(), mappingInfo2.getKey());
          }
          return result;
        }
      };
      Arrays.sort(array, comparator);
      return array;
    }

    public void inputChanged(Viewer viewer, Object old, Object newobj) {
    }

    public boolean isDeleted(Object object) {
      return false;
    }
  }

  public class CatalogEntryLabelProvider extends LabelProvider implements ITableLabelProvider {

    public Image getColumnImage(Object object, int columnIndex) {
      Image result = null;
      if (columnIndex == 0) {
        Image base = null;
        if (object instanceof ICatalogEntry) {
          ICatalogEntry catalogEntry = (ICatalogEntry) object;
          String uri = catalogEntry.getURI();
          if (uri.endsWith("dtd")) { //$NON-NLS-1$
            base = dtdFileImage;
          } else if (uri.endsWith("xsd")) { //$NON-NLS-1$
            base = xsdFileImage;
          } else {
            base = unknownFileImage;
          }

          if (base != null) {
            if (URIHelper.isReadableURI(uri, false)) {
              result = base;
            } else {
              // TODO... SSE port
              result = base;// imageFactory.createCompositeImage(base,
              // errorImage,
              // ImageFactory.BOTTOM_LEFT);
            }
          }
        }
      }
      return result;
    }

    public String getColumnText(Object object, int columnIndex) {
      String result = null;
      if (object instanceof ICatalogEntry) {
        ICatalogEntry catalogEntry = (ICatalogEntry) object;
        result = columnIndex == 0 ? catalogEntry.getKey() : catalogEntry.getURI();
        result = URIHelper.removePlatformResourceProtocol(result);
      }
      return result != null ? result : ""; //$NON-NLS-1$
    }
  }

  class XMLCatalogTableViewerFilter extends ViewerFilter {
    protected String[] extensions;

    public XMLCatalogTableViewerFilter(String[] extensions) {
      this.extensions = extensions;
    }

    public boolean isFilterProperty(Object element, Object property) {
      return false;
    }

    public boolean select(Viewer viewer, Object parent, Object element) {
      boolean result = false;
      if (element instanceof ICatalogEntry) {
        ICatalogEntry catalogEntry = (ICatalogEntry) element;
        for (int i = 0; i < extensions.length; i++) {
          if (catalogEntry.getURI().endsWith(extensions[i])) {
            result = true;
            break;
          }
        }
      }
      return result;
    }
  }

  protected static Image dtdFileImage = XMLEditorPluginImageHelper.getInstance().getImage(
      XMLEditorPluginImages.IMG_OBJ_DTDFILE);

  protected static String ERROR_STATE_KEY = "errorstatekey"; //$NON-NLS-1$
  protected static Image errorImage = XMLEditorPluginImageHelper.getInstance().getImage(
      XMLEditorPluginImages.IMG_OVR_ERROR);

  protected static Image unknownFileImage = XMLEditorPluginImageHelper.getInstance().getImage(
      XMLEditorPluginImages.IMG_OBJ_TXTEXT);
  protected static Image xsdFileImage = XMLEditorPluginImageHelper.getInstance().getImage(
      XMLEditorPluginImages.IMG_OBJ_XSDFILE);

  // protected ImageFactory imageFactory = new ImageFactory();

  public XMLCatalogTableViewer(Composite parent, String[] columnProperties) {
    super(parent, SWT.FULL_SELECTION | SWT.BORDER);

    Table table = getTable();
    table.setLinesVisible(true);
    table.setHeaderVisible(true);
    table.setLinesVisible(true);

    TableLayout layout = new TableLayout();
    for (int i = 0; i < columnProperties.length; i++) {
      TableColumn column = new TableColumn(table, i);
      column.setText(columnProperties[i]);
      column.setAlignment(SWT.LEFT);
      layout.addColumnData(new ColumnWeightData(50, true));
    }
    table.setLayout(layout);
    table.setLinesVisible(false);

    setColumnProperties(columnProperties);

    setContentProvider(new CatalogEntryContentProvider());
    setLabelProvider(new CatalogEntryLabelProvider());
  }

  public Collection getXMLCatalogEntries() {
    return null;
  }

  public void menuAboutToShow(IMenuManager menuManager) {
    Action action = new Action("hello") { //$NON-NLS-1$
      public void run() {
        System.out.println("run!"); //$NON-NLS-1$
      }
    };
    menuManager.add(action);
  }

  public void setFilterExtensions(String[] extensions) {
    resetFilters();
    addFilter(new XMLCatalogTableViewerFilter(extensions));
  }
}
