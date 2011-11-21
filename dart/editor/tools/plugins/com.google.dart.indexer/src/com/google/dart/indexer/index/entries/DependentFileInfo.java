/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.indexer.index.entries;

import com.google.dart.indexer.locations.Location;
import com.google.dart.indexer.source.IndexableSource;

import org.eclipse.core.resources.IFile;

import java.util.Set;

public class DependentFileInfo implements DependentEntity {
  @Deprecated
  private IFile file;
  private IndexableSource source;

  @Deprecated
  public DependentFileInfo(IFile file) {
    if (file == null) {
      throw new NullPointerException("file is null");
    }
    this.file = file;
  }

  public DependentFileInfo(IndexableSource source) {
    if (source == null) {
      throw new NullPointerException("source is null");
    }
    this.source = source;
  }

  @Deprecated
  public IFile getFile() {
    return file;
  }

  public IndexableSource getSource() {
    return source;
  }

  @Override
  @Deprecated
  public boolean isStale(IFile staleFile, Set<Location> staleLocations) {
    return file.equals(staleFile);
  }

  @Override
  public boolean isStale(IndexableSource staleSource, Set<Location> staleLocations) {
    return source.equals(staleSource);
  }

  @Override
  public String toString(boolean showLayers) {
    if (source != null) {
      return "source " + source.getUri();
    }
    return "file " + file.getFullPath();
  }
}
