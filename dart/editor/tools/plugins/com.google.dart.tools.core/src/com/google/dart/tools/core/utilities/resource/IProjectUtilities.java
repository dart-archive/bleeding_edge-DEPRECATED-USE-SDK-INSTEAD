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
package com.google.dart.tools.core.utilities.resource;

import com.google.dart.tools.core.refresh.DartPackagesFolderMatcher;

import org.eclipse.core.resources.FileInfoMatcherDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceFilterDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * The class <code>IProjectUtilities</code> defines utility methods used to work with projects.
 * 
 * @coverage dart.tools.core.utilities
 */
public final class IProjectUtilities {

  public static void configurePackagesFilter(IProject project) throws CoreException {
    FileInfoMatcherDescription matcher = new FileInfoMatcherDescription(
        DartPackagesFolderMatcher.MATCHER_ID,
        null);

    project.createFilter(IResourceFilterDescription.EXCLUDE_ALL
        | IResourceFilterDescription.FOLDERS | IResourceFilterDescription.FILES
        | IResourceFilterDescription.INHERITABLE, matcher, 0, new NullProgressMonitor());
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private IProjectUtilities() {
    super();
  }
}
