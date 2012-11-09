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

import com.google.dart.tools.ui.internal.callhierarchy.CallerMethodWrapper;
import com.google.dart.tools.ui.internal.callhierarchy.MethodWrapper;
import com.google.dart.tools.ui.internal.util.SWTUtil;
import com.google.dart.tools.ui.internal.viewsupport.ColoringLabelProvider;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbenchPartSite;

class CallHierarchyViewer extends TreeViewer {

  private final CallHierarchyViewPart part;
  private final CallHierarchyContentProvider contentProvider;
  private CallerMethodWrapper constructorToExpand;
  private TreeRoot dummyRoot;

  private IPreferenceStore preferences;
  private IPropertyChangeListener propertyChangeListener = new IPropertyChangeListener() {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      doPropertyChange(event);
    }
  };

  /**
   * @param parent the parent composite
   * @param part the call hierarchy view part
   */
  CallHierarchyViewer(Composite parent, CallHierarchyViewPart part, IPreferenceStore preferences) {
    super(new Tree(parent, SWT.MULTI));
    this.part = part;
    this.preferences = preferences;
    getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
    setUseHashlookup(true);
    setAutoExpandLevel(2);
    contentProvider = new CallHierarchyContentProvider(part);
    setContentProvider(contentProvider);
    setLabelProvider(new ColoringLabelProvider(new CallHierarchyLabelProvider()));
    clearViewer();
    getTree().addListener(SWT.EraseItem, new Listener() {
      @Override
      public void handleEvent(Event event) {
        SWTUtil.eraseSelection(event, getTree(), getPreferences());
      }
    });
    preferences.addPropertyChangeListener(propertyChangeListener);
  }

  public void dispose() {
    if (propertyChangeListener != null) {
      getPreferences().removePropertyChangeListener(propertyChangeListener);
      propertyChangeListener = null;
    }
  }

  @Override
  protected Object[] getSortedChildren(Object parentElementOrTreePath) {
    Object[] sortedChildren = super.getSortedChildren(parentElementOrTreePath);
    if (parentElementOrTreePath instanceof CallerMethodWrapper) {
      CallerMethodWrapper parentWrapper = (CallerMethodWrapper) parentElementOrTreePath;
      if (parentWrapper.getExpandWithConstructors() && sortedChildren.length == 2
          && sortedChildren[0] instanceof CallerMethodWrapper) {
        setConstructorToExpand((CallerMethodWrapper) sortedChildren[0]);
      }
    }
    return sortedChildren;
  }

  @Override
  protected void handleTreeExpand(TreeEvent event) {
    super.handleTreeExpand(event);
    expandConstructorNode();
  }

  protected void updateColors() {
    SWTUtil.setColors(getTree(), getPreferences());
  }

  void addKeyListener(KeyListener keyListener) {
    getControl().addKeyListener(keyListener);
  }

  void cancelJobs() {
    if (part == null) {
      return;
    }
    contentProvider.cancelJobs(part.getCurrentMethodWrappers());
  }

  void clearViewer() {
    setInput(TreeRoot.EMPTY_ROOT);
    dummyRoot = null;
  }

  /**
   * Expands the constructor node when in expand with constructors mode.
   */
  void expandConstructorNode() {
    if (constructorToExpand != null) {
      setExpandedState(constructorToExpand, true);
      constructorToExpand = null;
    }
  }

  CallHierarchyViewPart getPart() {
    return part;
  }

  /**
   * Wraps the roots of a MethodWrapper tree in a dummy root in order to show it in the tree.
   * 
   * @param roots The visible roots of the MethodWrapper tree.
   * @param addRoots <code>true</code> if the roots need to be added to the existing roots,
   *          <code>false</code> otherwise
   * @return a new TreeRoot which is a dummy root above the specified root
   */
  TreeRoot getTreeRoot(MethodWrapper[] roots, boolean addRoots) {
    if (dummyRoot == null || !addRoots) {
      dummyRoot = new TreeRoot(roots);
    } else {
      dummyRoot.addRoots(roots);
    }
    return dummyRoot;
  }

  /**
   * Attaches a context menu listener to the tree
   * 
   * @param menuListener the menu listener
   * @param viewSite the view site
   * @param selectionProvider the selection provider
   */
  void initContextMenu(IMenuListener menuListener, IWorkbenchPartSite viewSite,
      ISelectionProvider selectionProvider) {
    MenuManager menuMgr = new MenuManager();
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(menuListener);
    Menu menu = menuMgr.createContextMenu(getTree());
    getTree().setMenu(menu);
    viewSite.registerContextMenu(menuMgr, selectionProvider);
  }

  boolean isInFocus() {
    return getControl().isFocusControl();
  }

  void setFocus() {
    getControl().setFocus();
  }

  void setMethodWrappers(MethodWrapper[] wrappers) {
    setInput(getTreeRoot(wrappers));

    setFocus();
    if (wrappers != null && wrappers.length > 0) {
      setSelection(new StructuredSelection(wrappers[0]), true);
    }
    expandConstructorNode();
  }

  private void doPropertyChange(PropertyChangeEvent event) {
    updateColors();
  }

  private IPreferenceStore getPreferences() {
    return preferences;
  }

  /**
   * Wraps the roots of a MethodWrapper tree in a dummy root in order to show it in the tree.
   * 
   * @param roots The visible roots of the MethodWrapper tree.
   * @return A new TreeRoot which is a dummy root above the specified root.
   */
  private TreeRoot getTreeRoot(MethodWrapper[] roots) {
    return getTreeRoot(roots, false);
  }

  /**
   * Sets the constructor node.
   * 
   * @param wrapper the constructor caller method wrapper
   */
  private void setConstructorToExpand(CallerMethodWrapper wrapper) {
    constructorToExpand = wrapper;
  }
}
