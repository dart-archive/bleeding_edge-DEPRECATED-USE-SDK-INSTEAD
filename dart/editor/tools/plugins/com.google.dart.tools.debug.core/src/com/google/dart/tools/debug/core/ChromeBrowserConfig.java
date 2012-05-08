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
package com.google.dart.tools.debug.core;

/**
 * This class represents a Chrome browser name and path tuple.
 */
public class ChromeBrowserConfig {
  public static ChromeBrowserConfig fromToken(String token) {
    String[] strs = token.split(" ");

    if (strs.length != 2) {
      return null;
    }

    ChromeBrowserConfig browser = new ChromeBrowserConfig();

    browser.setName(decode(strs[0]));
    browser.setPath(decode(strs[1]));

    return browser;
  }

  private static String decode(String str) {
    return str.replace("\\-", " ");
  }

  private String name;

  private String path;

  public ChromeBrowserConfig() {

  }

  public String getName() {
    return name;
  }

  public String getPath() {
    return path;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public String toString() {
    return getName();
  }

  public String toToken() {
    return encode(name) + " " + encode(path);
  }

  private String encode(String str) {
    return str.replace(" ", "\\-");
  }
}
