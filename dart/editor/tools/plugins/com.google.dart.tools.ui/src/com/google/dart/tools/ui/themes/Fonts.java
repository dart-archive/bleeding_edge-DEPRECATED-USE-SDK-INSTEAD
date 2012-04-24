/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui.themes;

import com.google.dart.tools.ui.internal.util.SWTUtil;

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
  private static Map<Font, Font> italicMap = new HashMap<Font, Font>();

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
      FontData data = new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD);
      boldFont = SWTUtil.getFont(templateFont.getDevice(), new FontData[] {data});
      boldMap.put(templateFont, boldFont);
    }
    return boldFont;
  }

  /**
   * Get (or create) an italic version of the given font.
   * 
   * @param templateFont the font to italicize
   * @return an italicized version of the given font
   */
  public static Font getItalicFont(Font templateFont) {
    Font italicFont = italicMap.get(templateFont);
    if (italicFont == null) {
      FontData fontData = templateFont.getFontData()[0];
      FontData data = new FontData(fontData.getName(), fontData.getHeight(), SWT.ITALIC);
      italicFont = SWTUtil.getFont(templateFont.getDevice(), new FontData[] {data});
      italicMap.put(templateFont, italicFont);
    }
    return italicFont;
  }

}
