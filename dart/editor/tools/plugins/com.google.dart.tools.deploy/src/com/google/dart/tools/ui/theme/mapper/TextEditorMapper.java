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
package com.google.dart.tools.ui.theme.mapper;

import com.google.dart.tools.ui.theme.ColorThemeSetting;

import java.util.Map;

public class TextEditorMapper extends GenericMapper {

  @Override
  public void map(Map<String, ColorThemeSetting> theme) {
    preferences.putBoolean("AbstractTextEditor.Color.Background.SystemDefault", false); // $NON-NLS-1$
    preferences.putBoolean("AbstractTextEditor.Color.Foreground.SystemDefault", false); // $NON-NLS-1$
    preferences.putBoolean("AbstractTextEditor.Color.SelectionBackground.SystemDefault", false); // $NON-NLS-1$
    preferences.putBoolean("AbstractTextEditor.Color.SelectionForeground.SystemDefault", false); // $NON-NLS-1$
    super.map(theme);
  }

}
