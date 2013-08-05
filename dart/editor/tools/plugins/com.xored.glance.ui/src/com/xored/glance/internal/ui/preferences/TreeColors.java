/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and Implementation (Yuri Strot)
 *******************************************************************************/
package com.xored.glance.internal.ui.preferences;

import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

public class TreeColors implements IPreferenceConstants {

  public static TreeColors getDefault() {
    IPreferenceStore prefs = DartToolsPlugin.getDefault().getCombinedPreferenceStore();
    RGB c1 = createColor(
        prefs,
        AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_COLOR);
    RGB c2 = new RGB(128, 128, 128);
    String osName = System.getProperty("os.name").toLowerCase();

    if (osName.contains("windows")) {
      if (osName.contains("7") || osName.contains("8")) {
        return new TreeColors(null, null, true);
      }
    }
//    else if (osName.contains("mac")) {
//      return new TreeColors(new RGB(128, 128, 128), new RGB(255, 255, 255), false);//56, 117, 215
//    }
//    return new TreeColors(null, null, false);
    return new TreeColors(c1, c2, false);
  }

  private static RGB createColor(IPreferenceStore store, String key) {
    RGB rgb = null;
    if (store.contains(key)) {
      if (store.isDefault(key)) {
        rgb = PreferenceConverter.getDefaultColor(store, key);
      } else {
        rgb = PreferenceConverter.getColor(store, key);
      }
      return rgb;
    }
    return null;
  }

  private final boolean useNative;

  private final RGB bg;

  private final RGB fg;

  public TreeColors(RGB bg, RGB fg, boolean useNative) {
    this.bg = bg;
    this.fg = fg;
    this.useNative = useNative;
  }

  public RGB getBg() {
    return bg;
  }

  public RGB getFg() {
    return fg;
  }

  public boolean isUseNative() {
    return useNative;
  }

}
