/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.themes;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;

import java.util.HashMap;
import java.util.Map;

/**
 * A point of entry for application-managed {@link Font}s.
 */
public class Fonts {

  private static Map<Font, Font> boldMap = new HashMap<Font, Font>();

  /**
   * Get (or create) a bold version of the given font.
   * 
   * @param templateFont the font to embolden
   * @return a bolded version of the given font
   */
  public static Font getBoldFont(Font templateFont) {
    Font boldFont = boldMap.get(templateFont);
    if (boldFont == null) {
      FontData fontData = templateFont.getFontData()[0];
      boldFont = new Font(templateFont.getDevice(), new FontData(fontData.getName(),
          fontData.getHeight(), SWT.BOLD));
      boldMap.put(templateFont, boldFont);
    }
    return boldFont;
  }
}
