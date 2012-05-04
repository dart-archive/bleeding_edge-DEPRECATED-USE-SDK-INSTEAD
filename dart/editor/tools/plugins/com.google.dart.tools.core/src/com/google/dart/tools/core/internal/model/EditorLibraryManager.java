/*
 * Copyright 2012 Dart project authors.
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

import com.google.dart.compiler.SystemLibraryManager;

import java.io.File;

/**
 * Specialized {@link SystemLibraryManager} for accessing libraries bundled with the editor.
 * 
 * @see SystemLibraryManagerProvider#getSystemLibraryManager()
 * @see SystemLibraryManagerProvider#getDartCLibraryManager()
 * @see SystemLibraryManagerProvider#getAnyLibraryManager()
 */
public class EditorLibraryManager extends SystemLibraryManager {

  public EditorLibraryManager(File sdkPath, String platformName) {
    super(sdkPath, platformName);
  }

}
