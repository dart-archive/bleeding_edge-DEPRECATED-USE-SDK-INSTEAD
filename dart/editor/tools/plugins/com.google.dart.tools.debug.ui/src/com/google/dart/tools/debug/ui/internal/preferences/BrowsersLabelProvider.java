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
package com.google.dart.tools.debug.ui.internal.preferences;

import com.google.dart.tools.debug.core.ChromeBrowserConfig;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;

/**
 * A label provider for the ChromeBrowserConfig class.
 * 
 * @see ChromeBrowserConfig
 */
class BrowsersLabelProvider extends DelegatingStyledCellLabelProvider implements ILabelProvider {

  private static class StyledLabelProvider extends LabelProvider implements
      DelegatingStyledCellLabelProvider.IStyledLabelProvider {

    public StyledLabelProvider() {

    }

    @Override
    public Image getImage(Object element) {
      if (element instanceof ChromeBrowserConfig) {
        return DartDebugUIPlugin.getImage("chromium_16.png");
      }

      return super.getImage(element);
    }

    @Override
    public StyledString getStyledText(Object element) {
      String mainText = getMainText(element);
      String secondaryText = getSecondaryText(element);

      StyledString str = new StyledString();

      str.append(mainText);

      if (secondaryText != null) {
        str.append(secondaryText, StyledString.DECORATIONS_STYLER);
      }

      return str;
    }

    private String getMainText(Object element) {
      if (element instanceof ChromeBrowserConfig) {
        ChromeBrowserConfig browser = (ChromeBrowserConfig) element;

        return browser.getName();
      }

      return super.getText(element);
    }

    private String getSecondaryText(Object element) {
      if (element instanceof ChromeBrowserConfig) {
        ChromeBrowserConfig browser = (ChromeBrowserConfig) element;

        return " - " + browser.getPath();
      }

      return null;
    }
  }

  /**
   * Create a new ChromeBrowserConfig.
   */
  public BrowsersLabelProvider() {
    super(new StyledLabelProvider());
  }

  @Override
  public Image getImage(Object element) {
    StyledLabelProvider styledProvider = (StyledLabelProvider) getStyledStringProvider();

    return styledProvider.getImage(element);
  }

  @Override
  public String getText(Object element) {
    StyledLabelProvider styledProvider = (StyledLabelProvider) getStyledStringProvider();

    return styledProvider.getMainText(element);
  }

}
