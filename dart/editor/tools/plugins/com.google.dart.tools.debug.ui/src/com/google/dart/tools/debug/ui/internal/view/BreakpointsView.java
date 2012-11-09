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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.functions.PreferencesAdapter;
import com.google.dart.tools.ui.internal.util.SWTUtil;

import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;

import java.util.ArrayList;
import java.util.List;

/**
 * A debugger breakpoints view.
 */
@SuppressWarnings("restriction")
public class BreakpointsView extends
    org.eclipse.debug.internal.ui.views.breakpoints.BreakpointsView {
  public static final String VIEW_ID = "com.google.dart.tools.debug.breakpointsView";

  private RemoveAllBreakpointsAction removeAllBreakpointsAction;

  ListViewer breakpointsViewer;
  TreeModelViewer treeViewer;
  private IPreferenceStore preferences;
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

    super.dispose();
  }

  @Override
  protected void configureToolBar(IToolBarManager manager) {
    removeAllBreakpointsAction = new RemoveAllBreakpointsAction();

    manager.add(removeAllBreakpointsAction);
    manager.update(true);
  }

  @Override
  protected TreeModelViewer createTreeViewer(Composite parent) {
    preferences = createCombinedPreferences();
    final TreeModelViewer treeViewer = super.createTreeViewer(parent);
    this.treeViewer = treeViewer;
    treeViewer.getTree().setBackgroundMode(SWT.INHERIT_FORCE);
    treeViewer.getTree().addListener(SWT.EraseItem, new Listener() {
      @Override
      public void handleEvent(Event event) {
        SWTUtil.eraseSelection(event, treeViewer.getTree(), getPreferences());
      }
    });
    getPreferences().addPropertyChangeListener(propertyChangeListener);
    updateColors();
    return treeViewer;
  }

  protected void updateColors() {
    SWTUtil.setColors(treeViewer.getTree(), getPreferences());
  }

  @SuppressWarnings("deprecation")
  private IPreferenceStore createCombinedPreferences() {
    List<IPreferenceStore> stores = new ArrayList<IPreferenceStore>(3);
    stores.add(DartToolsPlugin.getDefault().getPreferenceStore());
    stores.add(new PreferencesAdapter(DartCore.getPlugin().getPluginPreferences()));
    stores.add(EditorsUI.getPreferenceStore());
    return new ChainedPreferenceStore(stores.toArray(new IPreferenceStore[stores.size()]));
  }

  private void doPropertyChange(PropertyChangeEvent event) {
    updateColors();
    treeViewer.refresh(false);
  }

  private IPreferenceStore getPreferences() {
    return preferences;
  }

}
