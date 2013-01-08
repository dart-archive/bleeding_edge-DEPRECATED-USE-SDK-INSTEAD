/*
 * Copyright 2013 Dart project authors.
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
package com.google.dart.tools.core.analysis.model;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;

public interface ProjectManager {

  /**
   * Answer the project for the specified Eclipse resource
   * 
   * @param resource the Eclipse resource
   * @return the project (not {@code null})
   */
  Project getProject(IProject resource);

  /**
   * Answer an array containing all of the projects currently defined in the workspace
   * 
   * @return array of projects (not {@code null}, contains no {@code null})
   */
  Project[] getProjects();

  /**
   * Answer the underlying Eclipse workspace associated with this object
   * 
   * @return the Eclipse workspace (not {@code null})
   */
  IWorkspaceRoot getResource();
}
