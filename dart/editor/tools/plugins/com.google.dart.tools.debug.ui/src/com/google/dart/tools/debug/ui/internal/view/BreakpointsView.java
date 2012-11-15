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

package com.google.dart.tools.debug.ui.internal.view;

import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.preferences.FontPreferencePage;
import com.google.dart.tools.ui.internal.util.SWTUtil;

import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.debug.internal.ui.views.variables.details.DetailPaneProxy;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IActionBars;

/**
 * A debugger breakpoints view.
 */
@SuppressWarnings("restriction")
public class BreakpointsView extends
    org.eclipse.debug.internal.ui.views.breakpoints.BreakpointsView {
  private class FontPropertyChangeListener implements IPropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (getViewer() != null) {
        if (FontPreferencePage.BASE_FONT_KEY.equals(event.getProperty())) {
          updateTreeFont();
          getViewer().refresh();
        }
      }
    }
  }

  public static final String VIEW_ID = "com.google.dart.tools.debug.breakpointsView";

  private RemoveAllBreakpointsAction removeAllBreakpointsAction;

  ListViewer breakpointsViewer;
  private TreeModelViewer treeViewer;
  private IPreferenceStore preferences;
  private IPropertyChangeListener fontPropertyChangeListener = new FontPropertyChangeListener();
  private IPropertyChangeListener propertyChangeListener = new IPropertyChangeListener() {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      doPropertyChange(event);
    }
  };

  public BreakpointsView() {

  }

  @Override
  public Viewer createViewer(Composite parent) {
    Viewer viewer = super.createViewer(parent);

    IActionBars actionBars = getViewSite().getActionBars();

    actionBars.getMenuManager().removeAll();

    return viewer;
  }

  @Override
  public void dispose() {
    if (removeAllBreakpointsAction != null) {
      removeAllBreakpointsAction.dispose();
    }
    if (propertyChangeListener != null) {
      getPreferences().removePropertyChangeListener(propertyChangeListener);
      propertyChangeListener = null;
    }
    if (propertyChangeListener != null) {
      getPreferences().removePropertyChangeListener(propertyChangeListener);
      propertyChangeListener = null;
    }

    super.dispose();
  }

  @Override
  public void refreshDetailPaneContents() {
    super.refreshDetailPaneContents();
    SWTUtil.setColors(getDetails(), getPreferences());
  }

  @Override
  protected void configureToolBar(IToolBarManager manager) {
    removeAllBreakpointsAction = new RemoveAllBreakpointsAction();

    manager.add(removeAllBreakpointsAction);
    manager.update(true);
  }

  @Override
  protected TreeModelViewer createTreeViewer(Composite parent) {
    preferences = DartToolsPlugin.getDefault().getCombinedPreferenceStore();
    final TreeModelViewer treeViewer = super.createTreeViewer(parent);
    this.treeViewer = treeViewer;
    treeViewer.getTree().setBackgroundMode(SWT.INHERIT_FORCE);
    treeViewer.getTree().addListener(SWT.EraseItem, new Listener() {
      @Override
      public void handleEvent(Event event) {
        SWTUtil.eraseSelection(event, treeViewer.getTree(), getPreferences());
      }
    });
    JFaceResources.getFontRegistry().addListener(fontPropertyChangeListener);
    updateTreeFont();
    getPreferences().addPropertyChangeListener(propertyChangeListener);
    updateColors();
    return treeViewer;
  }

  protected void updateColors() {
    SWTUtil.setColors(treeViewer.getTree(), getPreferences());
  }

  protected void updateTreeFont() {
    Font newFont = JFaceResources.getFont(FontPreferencePage.BASE_FONT_KEY);
    Font oldFont = treeViewer.getTree().getFont();
    Font font = SWTUtil.changeFontSize(oldFont, newFont);
    treeViewer.getTree().setFont(font);
  }

  private void doPropertyChange(PropertyChangeEvent event) {
    updateColors();
    treeViewer.refresh(false);
  }

  private Composite getDetails() {
    // Warning: fragile code!
    DetailPaneProxy detailProxy = ReflectionUtils.getFieldObject(this, "fDetailPane");
    Composite text = ReflectionUtils.getFieldObject(detailProxy, "fCurrentControl");
    return text;
  }

  private IPreferenceStore getPreferences() {
    return preferences;
  }

}
