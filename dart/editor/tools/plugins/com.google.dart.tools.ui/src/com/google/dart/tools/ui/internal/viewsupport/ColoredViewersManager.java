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
package com.google.dart.tools.ui.internal.viewsupport;

import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.internal.preferences.AppearancePreferencePage;

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("deprecation")
public class ColoredViewersManager implements IPropertyChangeListener {

  private class ManagedViewer implements DisposeListener {

    private static final String COLORED_LABEL_KEY = "coloredlabel"; //$NON-NLS-1$

    private final StructuredViewer fViewer;
    private OwnerDrawSupport fOwnerDrawSupport;

    private ManagedViewer(StructuredViewer viewer) {
      fViewer = viewer;
      fOwnerDrawSupport = null;
      fViewer.getControl().addDisposeListener(this);
      if (showColoredLabels()) {
        installOwnerDraw();
      }
    }

    public final void refresh() {
      Control control = fViewer.getControl();
      if (!control.isDisposed()) {
        if (showColoredLabels()) {
          installOwnerDraw();
        } else {
          uninstallOwnerDraw();
        }
      }
    }

    @Override
    public void widgetDisposed(DisposeEvent e) {
      uninstallColoredLabels(fViewer);
    }

    protected void installOwnerDraw() {
      if (fOwnerDrawSupport == null) {
        // not yet installed
        fOwnerDrawSupport = new OwnerDrawSupport(fViewer.getControl()) { // will install itself as listeners
          @Override
          public Color getColor(String foregroundColorName, Display display) {
            return getColorForName(foregroundColorName);
          }

          @Override
          public ColoredString getColoredLabel(Item item) {
            return getColoredLabelForView(item);
          }
        };
      }
      refreshViewer();
    }

    protected void uninstallOwnerDraw() {
      if (fOwnerDrawSupport == null) {
        return; // not installed
      }

      fOwnerDrawSupport.dispose(); // removes itself as listener
      fOwnerDrawSupport = null;
      refreshViewer();
    }

    private ColoredString getColoredLabelForView(Item item) {
      ColoredString oldLabel = (ColoredString) item.getData(COLORED_LABEL_KEY);
      String itemText = item.getText();
      if (oldLabel != null && oldLabel.getString().equals(itemText)) {
        // avoid accesses to the label provider if possible
        return oldLabel;
      }
      ColoredString newLabel = null;
      IBaseLabelProvider labelProvider = fViewer.getLabelProvider();
      if (labelProvider instanceof IRichLabelProvider) {
        newLabel = ((IRichLabelProvider) labelProvider).getRichTextLabel(item.getData());
      }
      if (newLabel == null) {
        newLabel = new ColoredString(itemText); // fallback. Should never happen.
      } else if (!newLabel.getString().equals(itemText)) {
        // the decorator manager has already queued an new update
        newLabel = ColoredDartElementLabels.decorateColoredString(
            newLabel,
            itemText,
            ColoredDartElementLabels.DECORATIONS_STYLE);
      }
      item.setData(COLORED_LABEL_KEY, newLabel); // cache the result
      return newLabel;
    }

    private void refresh(Item[] items) {
      for (int i = 0; i < items.length; i++) {
        Item item = items[i];
        item.setData(COLORED_LABEL_KEY, null);
        String text = item.getText();
        item.setText(""); //$NON-NLS-1$
        item.setText(text);
        if (item instanceof TreeItem) {
          refresh(((TreeItem) item).getItems());
        }
      }
    }

    private void refreshViewer() {
      Control control = fViewer.getControl();
      if (!control.isDisposed()) {
        if (control instanceof Tree) {
          refresh(((Tree) control).getItems());
        } else if (control instanceof Table) {
          refresh(((Table) control).getItems());
        }
      }
    }

  }

  public static final String QUALIFIER_COLOR_NAME = "com.google.dart.tools.ui.ColoredLabels.qualifier"; //$NON-NLS-1$
  public static final String DECORATIONS_COLOR_NAME = "com.google.dart.tools.ui.ColoredLabels.decorations"; //$NON-NLS-1$
  public static final String COUNTER_COLOR_NAME = "com.google.dart.tools.ui.ColoredLabels.counter"; //$NON-NLS-1$
  public static final String INHERITED_COLOR_NAME = "com.google.dart.tools.ui.ColoredLabels.inherited"; //$NON-NLS-1$
  public static final String HIGHLIGHT_BG_COLOR_NAME = "com.google.dart.tools.ui.ColoredLabels.match_highlight"; //$NON-NLS-1$
  public static final String HIGHLIGHT_WRITE_BG_COLOR_NAME = "com.google.dart.tools.ui.ColoredLabels.writeaccess_highlight"; //$NON-NLS-1$

  private static ColoredViewersManager fgInstance = new ColoredViewersManager();

  public static void install(ColoringLabelProvider labelProvider) {
    fgInstance.installColoredLabels(labelProvider);
  }

  public static void install(StructuredViewer viewer) {
    fgInstance.installColoredLabels(viewer);
  }

  public static boolean showColoredLabels() {
    String preference = PreferenceConstants.getPreference(
        AppearancePreferencePage.PREF_COLORED_LABELS);
    return preference != null && Boolean.valueOf(preference).booleanValue();
  }

  public static void uninstall(ColoringLabelProvider labelProvider) {
    fgInstance.uninstallColoredLabels(labelProvider);
  }

  private final Map<StructuredViewer, ManagedViewer> fManagedViewers;
  private Set<ColoringLabelProvider> fManagedLabelProviders;
  private final ColorRegistry fColorRegisty;

  public ColoredViewersManager() {
    fManagedViewers = new HashMap<StructuredViewer, ManagedViewer>();
    fManagedLabelProviders = new HashSet<ColoringLabelProvider>();
    fColorRegisty = JFaceResources.getColorRegistry();
  }

  public Color getColorForName(String symbolicName) {
    return fColorRegisty.get(symbolicName);
  }

  public void installColoredLabels(ColoringLabelProvider labelProvider) {
    if (fManagedLabelProviders.contains(labelProvider)) {
      return;
    }

    if (fManagedLabelProviders.isEmpty()) {
      // first lp installed
      PlatformUI.getPreferenceStore().addPropertyChangeListener(this);
      JFaceResources.getColorRegistry().addListener(this);
    }
    fManagedLabelProviders.add(labelProvider);
  }

  public void installColoredLabels(StructuredViewer viewer) {
    if (fManagedViewers.containsKey(viewer)) {
      return; // already installed
    }
    if (fManagedViewers.isEmpty()) {
      // first viewer installed
      PreferenceConstants.getPreferenceStore().addPropertyChangeListener(this);
      fColorRegisty.addListener(this);
    }
    fManagedViewers.put(viewer, new ManagedViewer(viewer));
  }

  @Override
  public void propertyChange(PropertyChangeEvent event) {
    String property = event.getProperty();
    if (property.equals(QUALIFIER_COLOR_NAME) || property.equals(COUNTER_COLOR_NAME)
        || property.equals(DECORATIONS_COLOR_NAME)
        || property.equals(AppearancePreferencePage.PREF_COLORED_LABELS)
        || property.equals(JFacePreferences.QUALIFIER_COLOR)
        || property.equals(JFacePreferences.COUNTER_COLOR)
        || property.equals(JFacePreferences.DECORATIONS_COLOR)
        || property.equals(HIGHLIGHT_BG_COLOR_NAME)
        || property.equals(HIGHLIGHT_WRITE_BG_COLOR_NAME) || property.equals(INHERITED_COLOR_NAME)
        || property.equals(IWorkbenchPreferenceConstants.USE_COLORED_LABELS)) {
      Display.getDefault().asyncExec(new Runnable() {
        @Override
        public void run() {
          refreshAllViewers();
        }
      });
    }
  }

  public void uninstallColoredLabels(ColoringLabelProvider labelProvider) {
    if (!fManagedLabelProviders.remove(labelProvider)) {
      return; // not installed
    }

    if (fManagedLabelProviders.isEmpty()) {
      PlatformUI.getPreferenceStore().removePropertyChangeListener(this);
      JFaceResources.getColorRegistry().removeListener(this);
      // last viewer uninstalled
    }
  }

  public void uninstallColoredLabels(StructuredViewer viewer) {
    ManagedViewer mv = fManagedViewers.remove(viewer);
    if (mv == null) {
      return; // not installed
    }

    if (fManagedViewers.isEmpty()) {
      PreferenceConstants.getPreferenceStore().removePropertyChangeListener(this);
      fColorRegisty.removeListener(this);
      // last viewer uninstalled
    }
  }

  protected final void refreshAllViewers() {
    for (Iterator<ManagedViewer> iterator = fManagedViewers.values().iterator(); iterator.hasNext();) {
      ManagedViewer viewer = iterator.next();
      viewer.refresh();
    }
    for (Iterator<ColoringLabelProvider> iterator = fManagedLabelProviders.iterator(); iterator.hasNext();) {
      ColoringLabelProvider lp = iterator.next();
      lp.update();
    }
  }

}
