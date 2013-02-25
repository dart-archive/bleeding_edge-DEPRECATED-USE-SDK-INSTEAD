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

/**
 * Combines a file path and a corresponding modification stamp, as defined by
 * <code>IResource.getModificationStamp</code>.
 */
public class PathAndModStamp {
  private final String path;
  private final long modificationStamp;

  public PathAndModStamp(String path, long modificationStamp) {
    this.path = path;
    this.modificationStamp = modificationStamp;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    PathAndModStamp other = (PathAndModStamp) obj;
    if (modificationStamp != other.modificationStamp) {
      return false;
    }
    if (path == null) {
      if (other.path != null) {
        return false;
      }
    } else if (!path.equals(other.path)) {
      return false;
    }
    return true;
  }

  public long getModificationStamp() {
    return modificationStamp;
  }

  public String getPath() {
    return path;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (modificationStamp ^ (modificationStamp >>> 32));
    result = prime * result + ((path == null) ? 0 : path.hashCode());
    return result;
  }

  @Override
  public String toString() {
    return path + ":" + modificationStamp;
  }
}
