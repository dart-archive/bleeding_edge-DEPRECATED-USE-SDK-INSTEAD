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
package com.google.dart.tools.core.model;

import com.google.common.base.Objects;

/**
 * Information about imported {@link DartLibrary}.
 */
public class DartLibraryImport {
  private final DartLibrary library;
  private final String prefix;

  public DartLibraryImport(DartLibrary library, String prefix) {
    this.library = library;
    this.prefix = prefix;
  }

  public boolean equals(DartLibrary library, String prefix) {
    return Objects.equal(this.library, library) && Objects.equal(this.prefix, prefix);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof DartLibraryImport) {
      DartLibraryImport other = (DartLibraryImport) obj;
      return Objects.equal(other.library, library) && Objects.equal(other.prefix, prefix);
    }
    return false;
  }

  /**
   * @return the imported {@link DartLibrary}, not <code>null</code>.
   */
  public DartLibrary getLibrary() {
    return library;
  }

  /**
   * @return the prefix used to import library, may be <code>null</code>.
   */
  public String getPrefix() {
    return prefix;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(library, prefix);
  }
}
