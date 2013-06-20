/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and Implementation (Yuri Strot)
 *******************************************************************************/
package com.xored.glance.internal.ui.preferences;

import org.eclipse.swt.graphics.RGB;

public class TreeColors implements IPreferenceConstants {

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

  public static TreeColors getDefault() {
    String osName = System.getProperty("os.name").toLowerCase();

    if (osName.contains("windows")) {
      if (osName.contains("7")) {
        return new TreeColors(null, null, true);
      }
    } else if (osName.contains("mac")) {
      return new TreeColors(new RGB(56, 117, 215), new RGB(255, 255, 255), false);
    }
    return new TreeColors(null, null, false);
  }

  private final boolean useNative;
  private final RGB bg;
  private final RGB fg;

}
