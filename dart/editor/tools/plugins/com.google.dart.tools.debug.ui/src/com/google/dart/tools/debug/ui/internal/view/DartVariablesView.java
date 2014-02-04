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

import com.google.dart.tools.debug.core.util.IDartDebugVariable;
import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.util.SWTUtil;

import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.debug.internal.ui.views.variables.AvailableLogicalStructuresAction;
import org.eclipse.debug.internal.ui.views.variables.VariablesView;
import org.eclipse.debug.internal.ui.views.variables.details.AvailableDetailPanesAction;
import org.eclipse.debug.internal.ui.views.variables.details.DetailPaneProxy;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * This custom subclass of the debugger VariablesView allows us to customize the actions that the
 * view exposes, and override some visibility behavior that is necessary to embed this view in other
 * views.
 */
@SuppressWarnings("restriction")
public class DartVariablesView extends VariablesView {
  private boolean visible;

  private TreeModelViewer treeViewer;
  private IPreferenceStore preferences;
  private IPropertyChangeListener propertyChangeListener = new IPropertyChangeListener() {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      doPropertyChange(event);
    }
  };

  /**
   * Create a new DartVariablesView instance.
   */
  public DartVariablesView() {

  }

  @Override
  public void becomesHidden() {
    visible = false;

    super.becomesHidden();
  }

  @Override
  public void becomesVisible() {
    visible = true;

    super.becomesVisible();
  }

  @Override
  public TreeModelViewer createViewer(Composite parent) {
    preferences = DartToolsPlugin.getDefault().getCombinedPreferenceStore();
    final TreeModelViewer treeViewer = (TreeModelViewer) super.createViewer(parent);
    this.treeViewer = treeViewer;
    treeViewer.getTree().setBackgroundMode(SWT.INHERIT_FORCE);
    treeViewer.getTree().addListener(SWT.EraseItem, new Listener() {
      @Override
      public void handleEvent(Event event) {
        SWTUtil.eraseSelection(event, treeViewer.getTree(), getPreferences());
      }
    });
    SWTUtil.bindJFaceResourcesFontToControl(treeViewer.getTree());
    updateColors();
    return treeViewer;
  }

  @Override
  public void dispose() {
    if (propertyChangeListener != null) {
      getPreferences().removePropertyChangeListener(propertyChangeListener);
      propertyChangeListener = null;
    }

    super.dispose();
  }

  @Override
  public boolean isVisible() {
    return visible;
  }

  @Override
  public void refreshDetailPaneContents() {
    super.refreshDetailPaneContents();
    SWTUtil.setColors(getDetailsText(), getPreferences());
    getDetailsText().setEditable(false);
  }

  @Override
  public void setSelectionProvider(ISelectionProvider provider) {
    // Overridden to do nothing. This prevents a nasty class cast exception.
    // See https://code.google.com/p/dart/issues/detail?id=2008.

  }

  @Override
  public void viewerUpdatesComplete() {
    super.viewerUpdatesComplete();

    // If the first element is an exception, select it.
    // Because the viewer uses a lazy content provider, there is no happy, clean
    // way of getting the contents of the viewer.
    TreeModelViewer viewer = (TreeModelViewer) getViewer();

    Tree tree = viewer.getTree();

    int itemCount = tree.getItemCount();

    if (itemCount > 0) {
      TreeItem item = tree.getItem(0);

      Object data = item.getData();

      if (data instanceof IDartDebugVariable) {
        IDartDebugVariable var = (IDartDebugVariable) data;

        if (var.isThrownException()) {
          viewer.setSelection(new StructuredSelection(var));
        }
      }
    }
  }

  @Override
  protected void configureToolBar(IToolBarManager tbm) {
//    tbm.add(new Separator(this.getClass().getName()));
//    tbm.add(new Separator(IDebugUIConstants.RENDER_GROUP));
//
//    //tbm.add(getAction("ShowTypeNames")); //$NON-NLS-1$
//    tbm.add(getAction("ToggleContentProviders")); //$NON-NLS-1$
//    //tbm.add(getAction("CollapseAll")); //$NON-NLS-1$
  }

  @Override
  protected void fillContextMenu(IMenuManager menu) {
    menu.add(new Separator(IDebugUIConstants.EMPTY_VARIABLE_GROUP));
    menu.add(new Separator(IDebugUIConstants.VARIABLE_GROUP));
    menu.add(new Separator());
    IAction action = new AvailableLogicalStructuresAction(this);
    if (action.isEnabled()) {
      menu.add(action);
    }
    action = new AvailableDetailPanesAction(this);
    if (isDetailPaneVisible() && action.isEnabled()) {
      menu.add(action);
    }
    menu.add(new Separator(IDebugUIConstants.EMPTY_RENDER_GROUP));
    menu.add(new Separator(IDebugUIConstants.EMPTY_NAVIGATION_GROUP));
    menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
  }

  @Override
  protected int[] getLastSashWeights() {
    return new int[] {12, 4};
  }

  protected void updateColors() {
    IPreferenceStore prefs = getPreferences();
    StyledText detailsText = getDetailsText();
    SWTUtil.setColors(treeViewer.getTree(), prefs);
    SWTUtil.setColors(detailsText, prefs);
  }

  private void doPropertyChange(PropertyChangeEvent event) {
    updateColors();
    treeViewer.refresh(false);
  }

  private StyledText getDetailsText() {
    // Warning: fragile code!
    DetailPaneProxy detailProxy = ReflectionUtils.getFieldObject(this, "fDetailPane");
    StyledText text = ReflectionUtils.getFieldObject(detailProxy, "fCurrentControl");
    return text;
  }

  private IPreferenceStore getPreferences() {
    return preferences;
  }
}
