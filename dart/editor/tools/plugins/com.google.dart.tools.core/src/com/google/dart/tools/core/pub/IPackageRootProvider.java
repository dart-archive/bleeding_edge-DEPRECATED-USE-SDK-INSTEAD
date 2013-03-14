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

package com.google.dart.tools.core.pub;

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.resources.IProject;

import java.io.File;

/**
 * This interface is able to return the appropriate package-root for a given IProject.
 */
public interface IPackageRootProvider {

  public static final IPackageRootProvider DEFAULT = new IPackageRootProvider() {
    @Override
    public File getPackageRoot(IProject project) {
      return DartCore.getPlugin().getPackageRoot(project);
    }
  };

  /**
   * Given an IProject, return the appropriate java.io.File representing a package root. This can be
   * null if a package root should not be used.
   * 
   * @param project the IProject
   * @return the File for the package root, or null if none
   */
  public File getPackageRoot(IProject project);

}
