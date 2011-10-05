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

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

/**
 * A centralized access point for custom application colors.
 */
public class Colors {

  private static Color BUTTON_RED = null;

  /**
   * Get a red suitable for a primary button background.
   */
  public static Color getButtonRed() {
    if (BUTTON_RED == null) {
      BUTTON_RED = new Color(Display.getCurrent(), 228, 98, 73);
    }
    return BUTTON_RED;
  }

}
