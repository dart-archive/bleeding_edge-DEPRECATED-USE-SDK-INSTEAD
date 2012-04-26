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
package com.google.dart.tools.core.internal.model.delta;

import com.google.common.base.Objects;

/**
 * Information about imported library.
 */
public final class CachedLibraryImport {
  private final String path;
  private final String prefix;

  public CachedLibraryImport(String path, String prefix) {
    this.path = path;
    this.prefix = prefix;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof CachedLibraryImport) {
      CachedLibraryImport other = (CachedLibraryImport) obj;
      return Objects.equal(other.path, path) && Objects.equal(other.prefix, prefix);
    }
    return false;
  }

  /**
   * @return the path of the imported library, not <code>null</code>.
   */
  public String getPath() {
    return path;
  }

  /**
   * @return the prefix of the imported library, may be <code>null</code>.
   */
  public String getPrefix() {
    return prefix;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(path, prefix);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this).add("path", path).add("prefix", prefix).toString();
  }
}
