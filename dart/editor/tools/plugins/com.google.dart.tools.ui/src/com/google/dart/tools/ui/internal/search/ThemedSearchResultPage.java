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
package com.google.dart.tools.ui.internal.search;

import com.google.dart.tools.search.ui.text.AbstractTextSearchViewPage;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.preferences.FontPreferencePage;
import com.google.dart.tools.ui.internal.util.SWTUtil;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * Abstract search results page that handles updates for font and color changes.
 */
abstract public class ThemedSearchResultPage extends AbstractTextSearchViewPage {
  private class FontPropertyChangeListener implements IPropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (getViewer() != null) {
        if (FontPreferencePage.BASE_FONT_KEY.equals(event.getProperty())) {
          if (getViewer() instanceof TreeViewer) {
            updateTreeFont();
          } else {
            updateTableFont();
          }
          getViewer().refresh();
        }
      }
    }
  }

  private IPreferenceStore preferences;
  private IPropertyChangeListener fontPropertyChangeListener = new FontPropertyChangeListener();
  private IPropertyChangeListener propertyChangeListener = new IPropertyChangeListener() {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      doPropertyChange(event);
    }
  };

  protected ThemedSearchResultPage() {
    preferences = DartToolsPlugin.getDefault().getCombinedPreferenceStore();
  }

  protected ThemedSearchResultPage(int supportedLayouts) {
    super(supportedLayouts);
    preferences = DartToolsPlugin.getDefault().getCombinedPreferenceStore();
  }

  @Override
  public void dispose() {
    super.dispose();
    if (propertyChangeListener != null) {
      getPreferences().removePropertyChangeListener(propertyChangeListener);
      propertyChangeListener = null;
    }
    if (fontPropertyChangeListener != null) {
      JFaceResources.getFontRegistry().removeListener(fontPropertyChangeListener);
      fontPropertyChangeListener = null;
    }
  }

  @Override
  protected void configureTableViewer(final TableViewer viewer) {
    viewer.getTable().setBackgroundMode(SWT.INHERIT_FORCE);
    viewer.getTable().addListener(SWT.EraseItem, new Listener() {
      @Override
      public void handleEvent(Event event) {
        SWTUtil.eraseSelection(event, viewer.getTable(), getPreferences());
      }
    });
    JFaceResources.getFontRegistry().addListener(fontPropertyChangeListener);
    updateTableFont();
    getPreferences().addPropertyChangeListener(propertyChangeListener);
    updateColors();
  }

  @Override
  protected void configureTreeViewer(final TreeViewer viewer) {
    viewer.getTree().setBackgroundMode(SWT.INHERIT_FORCE);
    viewer.getTree().addListener(SWT.EraseItem, new Listener() {
      @Override
      public void handleEvent(Event event) {
        SWTUtil.eraseSelection(event, viewer.getTree(), getPreferences());
      }
    });
    JFaceResources.getFontRegistry().addListener(fontPropertyChangeListener);
    updateTreeFont();
    getPreferences().addPropertyChangeListener(propertyChangeListener);
    updateColors();
  }

  protected void updateColors() {
    StructuredViewer viewer = getViewer();
    if (viewer instanceof TableViewer) {
      SWTUtil.setColors(((TableViewer) viewer).getTable(), getPreferences());
    } else {
      SWTUtil.setColors(((TreeViewer) viewer).getTree(), getPreferences());
    }
  }

  protected void updateTableFont() {
    Font newFont = JFaceResources.getFont(FontPreferencePage.BASE_FONT_KEY);
    TableViewer treeViewer = (TableViewer) getViewer();
    Font oldFont = treeViewer.getTable().getFont();
    Font font = SWTUtil.changeFontSize(oldFont, newFont);
    treeViewer.getTable().setFont(font);
  }

  protected void updateTreeFont() {
    Font newFont = JFaceResources.getFont(FontPreferencePage.BASE_FONT_KEY);
    TreeViewer treeViewer = (TreeViewer) getViewer();
    Font oldFont = treeViewer.getTree().getFont();
    Font font = SWTUtil.changeFontSize(oldFont, newFont);
    treeViewer.getTree().setFont(font);
  }

  private void doPropertyChange(PropertyChangeEvent event) {
    updateColors();
    getViewer().refresh(false);
  }

  private IPreferenceStore getPreferences() {
    return preferences;
  }

}
