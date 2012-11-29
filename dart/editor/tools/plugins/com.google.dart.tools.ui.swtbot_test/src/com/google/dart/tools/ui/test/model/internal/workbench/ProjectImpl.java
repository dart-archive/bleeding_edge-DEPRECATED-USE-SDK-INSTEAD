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
package com.google.dart.tools.ui.test.model.internal.workbench;

import com.google.dart.tools.ui.test.model.Workspace.Project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

/**
 * Project model element implementation.
 */
public class ProjectImpl implements Project {

  private final IProject project;

  /**
   * Create an instance for the given {@link IProject}.
   */
  public ProjectImpl(IProject project) {
    this.project = project;
  }

  @Override
  public void delete() throws CoreException {
    project.delete(true, null);
  }

  @Override
  public String getName() {
    return project.getName();
  }

}
