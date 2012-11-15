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
import com.google.dart.tools.ui.internal.preferences.FontPreferencePage;
import com.google.dart.tools.ui.internal.util.SWTUtil;

import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.debug.internal.ui.views.variables.VariablesView;
import org.eclipse.debug.internal.ui.views.variables.details.DetailPaneProxy;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * This custom subclass of the debugger VariablesView allows us to customize the actions that the
 * view exposes, and override some visibility behavior that is necessary to embed this view in other
 * views.
 */
@SuppressWarnings("restriction")
public class DartVariablesView extends VariablesView {

  private class FontPropertyChangeListener implements IPropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (treeViewer != null) {
        if (FontPreferencePage.BASE_FONT_KEY.equals(event.getProperty())) {
          updateTreeFont();
          treeViewer.refresh();
        }
      }
    }
  }

  private boolean visible;
  private TreeModelViewer treeViewer;
  private IPreferenceStore preferences;
  private IPropertyChangeListener fontPropertyChangeListener = new FontPropertyChangeListener();
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
    JFaceResources.getFontRegistry().addListener(fontPropertyChangeListener);
    updateTreeFont();
    getPreferences().addPropertyChangeListener(propertyChangeListener);
    updateColors();
    return treeViewer;
  }

  @Override
  public void dispose() {
    if (fontPropertyChangeListener != null) {
      JFaceResources.getFontRegistry().removeListener(fontPropertyChangeListener);
      fontPropertyChangeListener = null;
    }
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
  protected int[] getLastSashWeights() {
    return new int[] {8, 2};
  }

  protected void updateColors() {
    IPreferenceStore prefs = getPreferences();
    StyledText detailsText = getDetailsText();
    SWTUtil.setColors(treeViewer.getTree(), prefs);
    SWTUtil.setColors(detailsText, prefs);
  }

  protected void updateTreeFont() {
    Font newFont = JFaceResources.getFont(FontPreferencePage.BASE_FONT_KEY);
    Font oldFont = treeViewer.getTree().getFont();
    Font font = SWTUtil.changeFontSize(oldFont, newFont);
    treeViewer.getTree().setFont(font);
    StyledText detailsText = getDetailsText();
    detailsText.setFont(font);
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
