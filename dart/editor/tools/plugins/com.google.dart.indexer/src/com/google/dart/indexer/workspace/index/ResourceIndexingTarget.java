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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

/**
 * Instances of the class <code>ResourceIndexingTarget</code> implement an indexing target
 * representing an {@link IFile}.
 */
public class ResourceIndexingTarget implements IndexingTarget {
  /**
   * The file to be indexed.
   */
  private IFile file;

  /**
   * Initialize a newly created target to represent the given file.
   * 
   * @param file the file to be indexed
   */
  public ResourceIndexingTarget(IFile file) {
    this.file = file;
  }

  @Override
  public IFile getFile() {
    return file;
  }

  @Override
  public IProject getProject() {
    return file.getProject();
  }

  @Override
  public String toString() {
    return getFile().toString();
  }
}
