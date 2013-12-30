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
import com.google.dart.tools.ui.internal.util.SWTUtil;

import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.internal.ui.elements.adapters.DefaultBreakpointsViewInput;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.debug.internal.ui.views.variables.details.DetailPaneProxy;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * A debugger breakpoints view.
 */
@SuppressWarnings("restriction")
public class BreakpointsView extends
    org.eclipse.debug.internal.ui.views.breakpoints.BreakpointsView {
  public static final String VIEW_ID = "com.google.dart.tools.debug.breakpointsView";

  private static final String ACTION_GOTO_MARKER = "GotoMarker";

  private RemoveBreakpointAction removeBreakpointAction;
  private RemoveAllBreakpointsAction removeAllBreakpointsAction;

  private TreeModelViewer treeViewer;
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
    if (removeBreakpointAction != null) {
      removeBreakpointAction.dispose();
    }

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
  public void refreshDetailPaneContents() {
    super.refreshDetailPaneContents();
    SWTUtil.setColors(getDetails(), getPreferences());
  }

  @Override
  protected void configureToolBar(IToolBarManager manager) {
    manager.add(removeBreakpointAction);

    removeAllBreakpointsAction = new RemoveAllBreakpointsAction();
    manager.add(removeAllBreakpointsAction);

    manager.update(true);
  }

  @Override
  protected void contextActivated(ISelection selection) {
    IPresentationContext presentationContext = getTreeModelViewer().getPresentationContext();

    if (selection == null || selection.isEmpty()) {
      Object input = new DefaultBreakpointsViewInput(presentationContext);
      super.contextActivated(new StructuredSelection(input));
    } else {
      if (selection instanceof TreeSelection) {
        if (((TreeSelection) selection).getFirstElement() instanceof DebugElement) {
          super.contextActivated(new StructuredSelection(new DefaultBreakpointsViewInput(
              presentationContext)));
        } else {
          super.contextActivated(selection);
        }
      } else {
        super.contextActivated(selection);
      }
    }

    if (isAvailable() && isVisible()) {
      updateAction("ContentAssist");
    }
  }

  @Override
  protected void createActions() {
    super.createActions();

    removeBreakpointAction = new RemoveBreakpointAction(treeViewer);
    setAction(REMOVE_ACTION, removeBreakpointAction);
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
    SWTUtil.bindJFaceResourcesFontToControl(treeViewer.getTree());
    getPreferences().addPropertyChangeListener(propertyChangeListener);
    updateColors();
    return treeViewer;
  }

  @Override
  protected void fillContextMenu(IMenuManager menu) {
    updateObjects();

    menu.add(getAction(ACTION_GOTO_MARKER));
    menu.add(new Separator());
    menu.add(removeBreakpointAction);
    menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
  }

  @Override
  protected void setViewerInput(Object context) {
    if (context != null && context instanceof DebugElement) {
      return;
    }
    super.setViewerInput(context);
  }

  protected void updateColors() {
    SWTUtil.setColors(treeViewer.getTree(), getPreferences());
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
