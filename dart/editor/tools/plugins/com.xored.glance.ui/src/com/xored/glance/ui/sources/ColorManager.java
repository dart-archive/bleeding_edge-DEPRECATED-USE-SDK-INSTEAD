/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and Implementation (Yuri Strot)
 *******************************************************************************/
package com.xored.glance.ui.sources;

import com.google.dart.tools.ui.DartToolsPlugin;

import com.xored.glance.internal.ui.GlancePlugin;
import com.xored.glance.internal.ui.preferences.IPreferenceConstants;
import com.xored.glance.internal.ui.preferences.TreeColors;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import static org.eclipse.jface.preference.PreferenceConverter.getColor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Yuri Strot
 */
public class ColorManager implements IPropertyChangeListener, IPreferenceConstants {

  public static final String ANNOTATION_ID = "com.xored.glance.ui.highlight";
  public static final String ANNOTATION_SELECTED_ID = "com.xored.glance.ui.select";

  private static ColorManager INSTANCE;

  public static ColorManager getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new ColorManager();
    }
    return INSTANCE;
  }

  public static IPreferenceStore getStore() {
//		return ((AbstractUIPlugin) Platform.getPlugin("org.eclipse.ui.editors")).getPreferenceStore();
    return DartToolsPlugin.getDefault().getCombinedPreferenceStore();
  }

  public static Color lighten(Color color, int delta) {
    int r = ensureColor(color.getRed() + delta);
    int g = ensureColor(color.getGreen() + delta);
    int b = ensureColor(color.getBlue() + delta);
    return new Color(color.getDevice(), r, g, b);
  }

  private static int ensureColor(int value) {
    return value > 255 ? 255 : value;
  }

  private Color selection;

  private Color highlight;

  private Color treeBg;

  private Color treeFg;

  private boolean useNative;

  private List<Color> toDispose = new ArrayList<Color>();

  private ColorManager() {
    getStore().addPropertyChangeListener(this);
    GlancePlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
    updateColors();
  }

  public Color getBackgroundColor() {
    return selection;
  }

  public Color getSelectedBackgroundColor() {
    return highlight;
  }

  public Color getTreeSelectionBg() {
    return treeBg;
  }

  public Color getTreeSelectionFg() {
    return treeFg;
  }

  public boolean isUseNative() {
    return useNative;
  }

  @Override
  public void propertyChange(final PropertyChangeEvent event) {
    if (COLOR_HIGHLIGHT.equals(event.getProperty()) || COLOR_SELECTION.equals(event.getProperty())) {
      updateColors();
    }
  }

  private void updateColors() {
    for (Color color : toDispose) {
      color.dispose();
    }
    toDispose = new ArrayList<Color>();

    final Display display = PlatformUI.getWorkbench().getDisplay();
    final IPreferenceStore store = getStore();

    selection = new Color(display, getColor(store, COLOR_HIGHLIGHT));
    highlight = new Color(display, getColor(store, COLOR_SELECTION));

    TreeColors colors = TreeColors.getDefault();
    useNative = colors.isUseNative();
    if (colors.getBg() != null) {
      treeBg = new Color(display, colors.getBg());
      toDispose.add(treeBg);
    } else {
      treeBg = display.getSystemColor(SWT.COLOR_LIST_SELECTION);
    }

    if (colors.getFg() != null) {
      treeFg = new Color(display, colors.getFg());
      toDispose.add(treeFg);
    } else {
      treeFg = display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);
    }

    toDispose.add(selection);
    toDispose.add(highlight);
  }
}
