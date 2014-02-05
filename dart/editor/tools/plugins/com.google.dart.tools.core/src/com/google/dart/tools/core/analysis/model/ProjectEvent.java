/*
 * Copyright (c) 2013, the Dart project authors.
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

/**
 * Event sent via {@link ProjectListener} to indicate that a project has been analyzed.
 * 
 * @coverage dart.tools.core.model
 */
public class ProjectEvent {
  private final Project project;

  public ProjectEvent(Project project) {
    this.project = project;
  }

  /**
   * Answer the project that was updated
   * 
   * @return the project (not {@code null})
   */
  public Project getProject() {
    return project;
  }

  @Override
  public String toString() {
    return "[ProjectEvent " + project.getResource().getName() + "]";
  }
}
