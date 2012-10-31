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
package com.google.dart.tools.ui.theme;

public class ColorThemeSetting {

  private Color color;
  private Boolean boldEnabled;
  private Boolean italicEnabled;
  private Boolean underlineEnabled;
  private Boolean strikethroughEnabled;

  public ColorThemeSetting(String color) {
    this.color = new Color(color);
  }

  public Color getColor() {
    return color;
  }

  public Boolean isBoldEnabled() {
    return boldEnabled;
  }

  public Boolean isItalicEnabled() {
    return italicEnabled;
  }

  public Boolean isStrikethroughEnabled() {
    return strikethroughEnabled;
  }

  public Boolean isUnderlineEnabled() {
    return underlineEnabled;
  }

  public void setBoldEnabled(Boolean boldEnabled) {
    this.boldEnabled = boldEnabled;
  }

  public void setItalicEnabled(Boolean italicEnabled) {
    this.italicEnabled = italicEnabled;
  }

  public void setStrikethroughEnabled(Boolean strikethroughEnabled) {
    this.strikethroughEnabled = strikethroughEnabled;
  }

  public void setUnderlineEnabled(Boolean underlineEnabled) {
    this.underlineEnabled = underlineEnabled;
  }

}
