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
package com.google.dart.tools.core.internal.indexer.task;

import com.google.dart.indexer.workspace.index.IndexingTargetGroup;
import com.google.dart.tools.core.model.DartLibrary;

import java.util.HashMap;

/**
 * Instances of the class <code>LibraryIndexingTargetGroup</code> implement an indexing target group
 * appropriate for {@link CompilationUnitIndexingTarget compilation unit indexing targets}.
 */
public class LibraryIndexingTargetGroup implements IndexingTargetGroup {
  /**
   * A table mapping libraries to the groups that correspond to them.
   */
  private static final HashMap<DartLibrary, LibraryIndexingTargetGroup> LibraryMap = new HashMap<DartLibrary, LibraryIndexingTargetGroup>();

  /**
   * Return the group representing compilation units in the given library.
   * 
   * @param library the library corresponding to the group to be returned
   * @return the group representing compilation units in the library
   */
  public static LibraryIndexingTargetGroup getGroupFor(DartLibrary library) {
    LibraryIndexingTargetGroup group = LibraryMap.get(library);
    if (group == null) {
      group = new LibraryIndexingTargetGroup();
      LibraryMap.put(library, group);
    }
    return group;
  }
}
