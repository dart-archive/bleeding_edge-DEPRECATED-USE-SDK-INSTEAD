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
package com.google.dart.tools.ui.callhierarchy;

import com.google.dart.tools.ui.internal.callhierarchy.CallLocation;
import com.google.dart.tools.ui.internal.preferences.FontPreferencePage;
import com.google.dart.tools.ui.internal.util.SWTUtil;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.actions.ActionFactory;

import java.util.ArrayList;

class LocationViewer extends TableViewer {
  private class FontPropertyChangeListener implements IPropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (getTable() != null) {
        if (FontPreferencePage.BASE_FONT_KEY.equals(event.getProperty())) {
          updateTableFont();
          refresh();
        }
      }
    }
  }

  /**
   * Creates the table control.
   * 
   * @param parent the parent composite
   * @return the table
   */
  private static Table createTable(Composite parent) {
    return new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
  }

  private final String columnHeaders[] = {
      CallHierarchyMessages.LocationViewer_ColumnIcon_header,
      CallHierarchyMessages.LocationViewer_ColumnLine_header,
      CallHierarchyMessages.LocationViewer_ColumnInfo_header};

  private ColumnLayoutData columnLayouts[] = {
      new ColumnPixelData(18, false, true), new ColumnWeightData(60), new ColumnWeightData(300)};

  private IPreferenceStore preferences;
  private IPropertyChangeListener fontPropertyChangeListener = new FontPropertyChangeListener();
  private IPropertyChangeListener propertyChangeListener = new IPropertyChangeListener() {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      doPropertyChange(event);
    }
  };

  LocationViewer(Composite parent, IPreferenceStore preferences) {
    super(createTable(parent));
    this.preferences = preferences;
    setContentProvider(new ArrayContentProvider());
    setLabelProvider(new LocationLabelProvider());
    setInput(new ArrayList<CallLocation>());

    createColumns();
    getTable().addListener(SWT.EraseItem, new Listener() {
      @Override
      public void handleEvent(Event event) {
        SWTUtil.eraseSelection(event, getTable(), getPreferences());
      }
    });
    JFaceResources.getFontRegistry().addListener(fontPropertyChangeListener);
    updateTableFont();
    preferences.addPropertyChangeListener(propertyChangeListener);
    updateColors();
  }

  public void dispose() {
    if (fontPropertyChangeListener != null) {
      JFaceResources.getFontRegistry().removeListener(fontPropertyChangeListener);
      fontPropertyChangeListener = null;
    }
    if (propertyChangeListener != null) {
      getPreferences().removePropertyChangeListener(propertyChangeListener);
      propertyChangeListener = null;
    }
  }

  protected void updateColors() {
    SWTUtil.setColors(getTable(), getPreferences());
  }

  protected void updateTableFont() {
    Font newFont = JFaceResources.getFont(FontPreferencePage.BASE_FONT_KEY);
    Font oldFont = getTable().getFont();
    Font font = SWTUtil.changeFontSize(oldFont, newFont);
    getTable().setFont(font);
  }

  void clearViewer() {
    setInput(""); //$NON-NLS-1$
  }

  /**
   * Attaches a context menu listener to the tree.
   * 
   * @param menuListener the menu listener
   * @param popupId the id of the popup
   * @param viewSite the part site
   */
  void initContextMenu(IMenuListener menuListener, String popupId, IWorkbenchPartSite viewSite) {
    MenuManager menuMgr = new MenuManager();
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(menuListener);
    Menu menu = menuMgr.createContextMenu(getControl());
    getControl().setMenu(menu);
    viewSite.registerContextMenu(popupId, menuMgr, this);
  }

  /**
   * Initializes and returns the Copy action for the location viewer.
   * 
   * @param viewSite the view site
   * @param clipboard the clipboard
   * @return the copy action
   */
  LocationCopyAction initCopyAction(final IViewSite viewSite, final Clipboard clipboard) {
    final LocationCopyAction copyAction = new LocationCopyAction(viewSite, clipboard, this);

    getTable().addFocusListener(new FocusListener() {
      IAction fViewCopyHandler;

      @Override
      public void focusGained(FocusEvent e) {
        IActionBars actionBars = viewSite.getActionBars();
        fViewCopyHandler = actionBars.getGlobalActionHandler(ActionFactory.COPY.getId());
        actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), copyAction);
        actionBars.updateActionBars();
      }

      @Override
      public void focusLost(FocusEvent e) {
        if (fViewCopyHandler != null) {
          IActionBars actionBars = viewSite.getActionBars();
          actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), fViewCopyHandler);
          actionBars.updateActionBars();
          fViewCopyHandler = null;
        }
      }
    });

    return copyAction;
  }

  private void createColumns() {
    TableLayout layout = new TableLayout();
    getTable().setLayout(layout);
    getTable().setHeaderVisible(true);
    for (int i = 0; i < columnHeaders.length; i++) {
      layout.addColumnData(columnLayouts[i]);
      TableColumn tc = new TableColumn(getTable(), SWT.NONE, i);
      tc.setResizable(columnLayouts[i].resizable);
      tc.setText(columnHeaders[i]);
    }
  }

  private void doPropertyChange(PropertyChangeEvent event) {
    updateColors();
    refresh(false);
  }

  private IPreferenceStore getPreferences() {
    return preferences;
  }
}
