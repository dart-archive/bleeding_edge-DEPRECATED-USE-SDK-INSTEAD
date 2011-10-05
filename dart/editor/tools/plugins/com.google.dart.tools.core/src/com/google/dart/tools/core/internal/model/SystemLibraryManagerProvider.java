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
package com.google.dart.tools.core.internal.model;

import com.google.dart.compiler.SystemLibrary;
import com.google.dart.compiler.SystemLibraryManager;

/**
 * The class <code>SystemLibraryManagerProvider</code> manages the {@link SystemLibraryManager
 * system library managers} used by the tools.
 */
public class SystemLibraryManagerProvider {
  /**
   * The single system library manager currently being managed.
   */
  private static SystemLibraryManager MANAGER;

  /**
   * Return the single system library manager currently being managed.
   * 
   * @return the single system library manager currently being managed
   */
  public static SystemLibraryManager getSystemLibraryManager() {
    if (MANAGER == null) {
      MANAGER = new SystemLibraryManager() {
        @Override
        protected SystemLibrary[] getDefaultLibraries() {
          return BundledSystemLibraryManager.getAllLibraries();
        }
      };
    }
    return MANAGER;
  }
}
