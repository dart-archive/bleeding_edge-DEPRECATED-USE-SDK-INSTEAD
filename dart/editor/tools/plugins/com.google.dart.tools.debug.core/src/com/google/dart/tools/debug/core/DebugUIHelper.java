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
package com.google.dart.tools.debug.core;

import java.util.List;

/**
 * An interface to allow the user to choose which tab in Chrome to debug.
 */
public interface DebugUIHelper {

  /**
   * Show the given error message to the user.
   * 
   * @param title
   * @param message
   */
  public void displayError(String title, String message);

  /**
   * Returns a token for the current platform.
   * <p>
   * Examples: "win32", "motif", "gtk", "photon", "carbon", "cocoa", "wpf"
   * 
   * @return the current platform
   */
  public String getPlatform();

  /**
   * Return the user's selection given a set of browser tabs.
   * 
   * @param availableTabs
   * @return
   */
  public int select(List<String> availableTabs);

}
