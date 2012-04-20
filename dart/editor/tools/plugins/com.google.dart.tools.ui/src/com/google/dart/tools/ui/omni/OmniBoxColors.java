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

package com.google.dart.tools.ui.omni;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.themes.ColorUtil;

/**
 * Point of entry for all Search box colors.
 */
public class OmniBoxColors {

  private static final LocalResourceManager resourceManager = new LocalResourceManager(
      JFaceResources.getResources());

  public static final Color SEARCHBOX_TEXT_COLOR = getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);

  public static final Color WATERMARK_TEXT_COLOR = getSystemColor(SWT.COLOR_DARK_GRAY);

  public static final Color SEARCH_ENTRY_HEADER_TEXT = getSystemColor(SWT.COLOR_GRAY);

  public static final Color SEARCH_ENTRY_ITEM_TEXT = resourceManager.createColor(ColorUtil.blend(
      getSystemColor(SWT.COLOR_WIDGET_BACKGROUND).getRGB(),
      getSystemColor(SWT.COLOR_WIDGET_FOREGROUND).getRGB()));

  public static final Color SEARCH_RESULT_BACKGROUND = getSystemColor(getSearchResultBackgroundColorId());

  private static int getSearchResultBackgroundColorId() {
    //the info background is uglier on Mac/Windows but plays nicer with Linux themes (like Radiance)
    return Util.isLinux() ? SWT.COLOR_INFO_BACKGROUND : SWT.COLOR_LIST_BACKGROUND;
  }

  private static Color getSystemColor(int id) {
    return Display.getDefault().getSystemColor(id);
  }

}
