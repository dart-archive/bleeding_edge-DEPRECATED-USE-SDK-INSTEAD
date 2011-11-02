/*
 * Copyright 2011, the Dart project authors.
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
package com.google.dart.indexer.workspace.index;

import org.eclipse.core.resources.IProject;

import java.util.HashMap;

/**
 * Instances of the class <code>ResourceIndexingTargetGroup</code> implement an indexing target
 * group appropriate for {@link ResourceIndexingTarget resource indexing targets}.
 */
public class ResourceIndexingTargetGroup implements IndexingTargetGroup {
  /**
   * A table mapping projects to the groups that correspond to them.
   */
  private static final HashMap<IProject, ResourceIndexingTargetGroup> ProjectMap = new HashMap<IProject, ResourceIndexingTargetGroup>();

  /**
   * Return the group representing resources in the given project.
   * 
   * @param project the project corresponding to the group to be returned
   * @return the group representing resources in the project
   */
  public static ResourceIndexingTargetGroup getGroupFor(IProject project) {
    ResourceIndexingTargetGroup group = ProjectMap.get(project);
    if (group == null) {
      group = new ResourceIndexingTargetGroup();
      ProjectMap.put(project, group);
    }
    return group;
  }
}
