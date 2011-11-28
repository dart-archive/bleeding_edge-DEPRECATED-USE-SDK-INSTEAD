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

import com.google.dart.indexer.source.IndexableSource;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import java.net.URI;

/**
 * Instances of the class <code>ResourceIndexingTarget</code> implement an indexing target
 * representing an {@link IFile}.
 */
public class ResourceIndexingTarget extends IndexableSource implements IndexingTarget {
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
  public IndexableSource asSource() {
    return this;
  }

  @Override
  public boolean exists() {
    return file.exists();
  }

  @Override
  @Deprecated
  public IFile getFile() {
    return file;
  }

  @Override
  public String getFileExtension() {
    String fileName = file.getName();
    int index = fileName.lastIndexOf('.');
    if (index < 0) {
      return "";
    }
    return fileName.substring(index + 1);
  }

  @Override
  public IndexingTargetGroup getGroup() {
    return ResourceIndexingTargetGroup.getGroupFor(file.getProject());
  }

  @Override
  public long getModificationStamp() {
    return file.getModificationStamp();
  }

  @Override
  @Deprecated
  public IProject getProject() {
    return file.getProject();
  }

  @Override
  public URI getUri() {
    return file.getLocationURI();
  }

  @Override
  public String toString() {
    return getFile().toString();
  }
}
