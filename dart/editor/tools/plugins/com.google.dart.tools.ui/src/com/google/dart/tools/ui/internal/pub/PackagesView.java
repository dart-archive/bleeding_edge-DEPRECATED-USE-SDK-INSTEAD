/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.ui.internal.pub;

import com.google.dart.tools.core.pub.IPubPackageListener;
import com.google.dart.tools.core.pub.PubPackageManager;
import com.google.dart.tools.core.pub.PubPackageObject;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.util.ExternalBrowserUtil;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

import java.util.ArrayList;
import java.util.List;

/**
 * A view that shows the packages available on pub with the descriptions and latest versions. Users
 * can create a copy of the package to explore the package or can view package documentation on pub.
 */
public class PackagesView extends ViewPart {

  class PackagesComparator extends ViewerComparator {

    private static final int DESCENDING = 1;
    private int direction;

    public PackagesComparator() {
      direction = DESCENDING;
    }

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
      if (e1 instanceof PubPackageObject) {
        PubPackageObject t1 = (PubPackageObject) e1;
        PubPackageObject t2 = (PubPackageObject) e2;
        int result = t1.getName().compareTo(t2.getName());
        if (direction == DESCENDING) {
          result *= -1;
        }
        return result;
      }
      return -1;
    }

    public void setColumn(int column) {
      if (column == 0) {
        direction = -1 * direction;
      } else {
        direction = DESCENDING;
      }
    }

  }

  class PackagesFilter extends ViewerFilter {

    private String searchString;

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
      if (searchString == null || searchString.length() == 0) {
        return true;
      }

      if (((PubPackageObject) element).getName().matches(searchString)
          || ((PubPackageObject) element).getDescription().matches(searchString)) {
        return true;
      }
      return false;
    }

    public void setSearchText(String s) {
      this.searchString = "(?i).*" + s + ".*";
    }

  }

  class PackagesLabelProvider extends StyledCellLabelProvider {

    @Override
    public String getToolTipText(Object element) {
      if (element instanceof PubPackageObject) {
        return ((PubPackageObject) element).getDescription();
      }
      return null;
    }

    @Override
    public void update(ViewerCell cell) {
      Object element = cell.getElement();

      if (element instanceof PubPackageObject) {
        String string = "";
        StyledString styledString = new StyledString();
        switch (cell.getColumnIndex()) {
          case 0:
            string = ((PubPackageObject) element).getName();
            styledString = new StyledString(string, boldStyler);
            break;
          case 1:
            string = ((PubPackageObject) element).getDescription();
            styledString = new StyledString(string, italicStyler);
            break;
          case 2:
            string = ((PubPackageObject) element).getVersion();
            styledString = new StyledString(string);
            break;
        }

        cell.setText(styledString.toString());
        cell.setStyleRanges(styledString.getStyleRanges());

        if (cell.getColumnIndex() == 0) {
          cell.setImage(DartToolsPlugin.getImage("icons/full/obj16/package_obj.gif"));
        }

      } else {
        if (element instanceof String) {
          cell.setText((String) element);
        } else {
          cell.setText("Unknown element"); //$NON-NLS-1$
        }
      }
      super.update(cell);
    }
  }

  class PubPackageListener implements IPubPackageListener {
    @Override
    public void pubPackagesChanged(final List<PubPackageObject> packages) {
      Display.getDefault().syncExec(new Runnable() {

        @Override
        public void run() {
          pubPackages = packages;
          tableViewer.setInput(pubPackages);
          tableViewer.refresh();
        }
      });

    }
  }

  public static String ID = "com.google.dart.tools.ui.view.packages";

  private static Styler italicStyler;

  private static Styler boldStyler;

  private static FontData[] getModifiedFontData(FontData[] originalData, int additionalStyle) {
    FontData[] styleData = new FontData[originalData.length];
    for (int i = 0; i < styleData.length; i++) {
      FontData base = originalData[i];
      styleData[i] = new FontData(base.getName(), base.getHeight(), base.getStyle()
          | additionalStyle);
    }
    return styleData;
  }

  private PackagesFilter filter;
  private TableViewer tableViewer;
  private Text filterText;
  private Font boldFont;
  private Font italicFont;

  private List<PubPackageObject> pubPackages;

  private PubPackageListener packageListener = new PubPackageListener();

  private PackagesComparator comparator;;

  @Override
  public void createPartControl(Composite parent) {

    Composite client = new Composite(parent, SWT.NONE);
    GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(client);
    GridLayoutFactory.swtDefaults().spacing(10, 5).numColumns(1).applyTo(client);

    Composite filterComp = new Composite(client, SWT.NONE);
    GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.FILL).grab(false, false).applyTo(filterComp);
    GridLayoutFactory.swtDefaults().numColumns(2).applyTo(filterComp);

    Label filterLabel = new Label(filterComp, SWT.NONE);
    filterLabel.setText("Search by name or description: ");
    GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.FILL).grab(false, false).applyTo(filterLabel);

    filterText = new Text(filterComp, SWT.BORDER | SWT.SEARCH);
    GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).hint(200, SWT.DEFAULT).applyTo(
        filterText);
    filterText.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent ke) {
        filter.setSearchText(filterText.getText());
        tableViewer.refresh();
      }
    });

    createTable(client);

    PubPackageManager.getInstance().addListener(packageListener);
  }

  @Override
  public void dispose() {
    super.dispose();
    boldFont.dispose();
    italicFont.dispose();
    PubPackageManager.getInstance().removeListener(packageListener);
  }

  @Override
  public void setFocus() {

  }

  private TableViewerColumn createColumnsAndMenu(TableColumnLayout tableLayout) {
    String[] titles = {"Name", "Description", "Version"};
    final TableViewerColumn nameColumn = createTableViewerColumn(titles[0], 0);
    TableViewerColumn descColumn = createTableViewerColumn(titles[1], 1);
    TableViewerColumn versionColumn = createTableViewerColumn(titles[2], 2);

    nameColumn.getColumn().pack();
    descColumn.getColumn().pack();
    versionColumn.getColumn().pack();
    int stylesheetWidth = nameColumn.getColumn().getWidth();
    int conceptWidth = descColumn.getColumn().getWidth();
    tableLayout.setColumnData(nameColumn.getColumn(), new ColumnWeightData(20, stylesheetWidth));
    tableLayout.setColumnData(descColumn.getColumn(), new ColumnWeightData(73, conceptWidth));
    tableLayout.setColumnData(versionColumn.getColumn(), new ColumnWeightData(7));
    nameColumn.getColumn().addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        comparator.setColumn(0);
        int dir = tableViewer.getTable().getSortDirection();
        dir = dir == SWT.DOWN ? SWT.UP : SWT.DOWN;
        tableViewer.getTable().setSortDirection(dir);
        tableViewer.getTable().setSortColumn(nameColumn.getColumn());
        tableViewer.refresh();
      }
    });

    // define the menu and assign to the table
    Menu headerMenu = new Menu(tableViewer.getTable());
    tableViewer.getTable().setMenu(headerMenu);
    createImportMenuItem(headerMenu, nameColumn.getColumn());
    createBrowseMenuItem(headerMenu, nameColumn.getColumn());
    return nameColumn;
  }

  private void createFontStyles() {
    boldFont = getBoldFont(tableViewer.getTable().getFont().getFontData());
    boldStyler = new Styler() {
      @Override
      public void applyStyles(TextStyle textStyle) {
        textStyle.font = boldFont;
      }
    };

    italicFont = getItalicFont(tableViewer.getTable().getFont().getFontData());
    italicStyler = new Styler() {
      @Override
      public void applyStyles(TextStyle textStyle) {
        textStyle.font = italicFont;
      }
    };
  }

  private void createImportMenuItem(Menu parent, TableColumn column) {
    final MenuItem itemName = new MenuItem(parent, SWT.PUSH);
    itemName.setText("New application from package");
    itemName.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        PubPackageObject selection = (PubPackageObject) ((IStructuredSelection) tableViewer.getSelection()).getFirstElement();
        if (selection != null) {
          AddPackageAction action = new AddPackageAction(
              getSite(),
              selection.getName(),
              selection.getVersion());
          action.run();
        }
      }
    });
  }

  private void createBrowseMenuItem(Menu parent, final TableColumn column) {
    final MenuItem itemName = new MenuItem(parent, SWT.PUSH);
    itemName.setText("Browse docs on pub.dartlang.org");
    itemName.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        PubPackageObject selection = (PubPackageObject) ((IStructuredSelection) tableViewer.getSelection()).getFirstElement();
        if (selection != null) {
          ExternalBrowserUtil.openInExternalBrowser("http://pub.dartlang.org/packages/"
              + selection.getName());
        }
      }
    });
  }

  private void createTable(Composite client) {

    Composite tableComposite = new Composite(client, SWT.NONE);
    GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, true).applyTo(
        tableComposite);
    TableColumnLayout tableLayout = new TableColumnLayout();
    tableComposite.setLayout(tableLayout);

    tableViewer = new TableViewer(tableComposite, SWT.HIDE_SELECTION | SWT.FULL_SELECTION
        | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.WRAP);
    final Table table = tableViewer.getTable();
    table.setLinesVisible(true);
    table.setHeaderVisible(true);

    createFontStyles();

    final TableViewerColumn nameColumn = createColumnsAndMenu(tableLayout);

    tableViewer.setContentProvider(new ArrayContentProvider());
    tableViewer.setLabelProvider(new PackagesLabelProvider());
    tableViewer.getTable().setSortColumn(nameColumn.getColumn());
    tableViewer.getTable().setSortDirection(SWT.DOWN);
    comparator = new PackagesComparator();
    comparator.setColumn(0);
    tableViewer.setComparator(comparator);

    filter = new PackagesFilter();
    tableViewer.addFilter(filter);
    ColumnViewerToolTipSupport.enableFor(tableViewer, ToolTip.NO_RECREATE);

    pubPackages = PubPackageManager.getInstance().getPubPackages();
    if (pubPackages.size() > 0) {
      tableViewer.setInput(pubPackages);
    } else {
      List<String> strings = new ArrayList<String>();
      for (int i = 0; i < 25; i++) {
        strings.add("Populating data ...");
      }
      tableViewer.setInput(strings);
    }

  }

  private TableViewerColumn createTableViewerColumn(String title, final int colNumber) {
    final TableViewerColumn viewerColumn = new TableViewerColumn(tableViewer, SWT.H_SCROLL
        | SWT.V_SCROLL | SWT.WRAP);
    final TableColumn column = viewerColumn.getColumn();
    column.setText(title);
    column.setResizable(true);
    return viewerColumn;
  }

  private Font getBoldFont(FontData[] fontData) {
    FontData[] boldFontData = getModifiedFontData(fontData, SWT.BOLD);

    Font boldFont = new Font(Display.getCurrent(), boldFontData);
    return boldFont;
  }

  private Font getItalicFont(FontData[] fontData) {
    FontData[] iFontData = getModifiedFontData(fontData, SWT.ITALIC);

    Font italicFont = new Font(Display.getCurrent(), iFontData);
    return italicFont;
  }

}
