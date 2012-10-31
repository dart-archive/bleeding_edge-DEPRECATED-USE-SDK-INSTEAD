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

import java.util.Map;

public class ColorTheme {

  private String id;
  private String name;
  private String author;
  private String website;
  private Map<String, ColorThemeSetting> entries;

  public String getAuthor() {
    return author;
  }

  public Map<String, ColorThemeSetting> getEntries() {
    return entries;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getWebsite() {
    return website;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public void setEntries(Map<String, ColorThemeSetting> entries) {
    this.entries = entries;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setWebsite(String website) {
    this.website = website;
  }
}
