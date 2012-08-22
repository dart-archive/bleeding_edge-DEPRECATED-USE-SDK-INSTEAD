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
package com.google.dart.tools.search.ui.text;

import com.google.dart.tools.core.model.DartSdkManager;

import org.eclipse.core.resources.IResource;

import java.io.File;

/**
 * A helper for calculating external root search scopes.
 */
public class ExternalRootSearchScopeHelper {

  private static final File SDK_DIR = DartSdkManager.getManager().getSdk().getDirectory();
  private static final File[] DEFAULT_ROOTS = new File[] {SDK_DIR};

  /**
   * Calculate external roots based on a given set of workspace resources.
   * 
   * @param workspaceRoots the workspace root resources
   * @return the calculated external roots
   */
  public static File[] calculateExternalRoots(IResource[] workspaceRoots) {
    return DEFAULT_ROOTS;
  }

}
