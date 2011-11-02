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

import java.net.URI;

/**
 * The interface <code>IndexingTarget</code> defines the behavior of objects representing a single
 * target to be indexed by the indexer. These targets are placed on the {@link IndexingQueue} and
 * processed by the {@link WorkspaceIndexer}.
 */
public interface IndexingTarget {
  /**
   * Return <code>true</code> if this target exists. A non-existent target is used to signal that
   * the information associated with the target needs to be removed from the index.
   * 
   * @return <code>true</code> if this target exists
   */
  public boolean exists();

  /**
   * Return the file associated with this target.
   * 
   * @return the file associated with this target
   */
  @Deprecated
  public IFile getFile();

  /**
   * Return the indexing group containing this target.
   * 
   * @return the indexing group containing this target
   */
  public IndexingTargetGroup getGroup();

  /**
   * Return an indication of when the target was last modified. The stamp can be any monotonically
   * increasing value, such as a modification time for a file or a simple counter.
   * 
   * @return the modification stamp associated with the target
   */
  public long getModificationStamp();

  /**
   * Return the project associated with this target.
   * 
   * @return the project associated with this target
   */
  @Deprecated
  public IProject getProject();

  /**
   * Return the URI uniquely identifying this target.
   * 
   * @return the URI uniquely identifying this target
   */
  public URI getUri();
}
