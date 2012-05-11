/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.core.analysis;

import com.google.dart.compiler.ast.DartUnit;

import java.io.File;
import java.util.Collection;

/**
 * Event sent via {@link ParseLibraryFileCallback} providing asynchronous results.
 */
public class ParseLibraryFileEvent {

  private final Library library;
  private final DartUnit unit;

  /**
   * Construct a new instance
   * 
   * @param library the library (not <code>null</code>)
   */
  ParseLibraryFileEvent(Library library, DartUnit unit) {
    this.library = library;
    this.unit = unit;
  }

  /**
   * A collection of files that the parsed file imports, including the implicitly imported dart:core
   * library file. Will be empty if the unit could not be parsed (e.g. library file does not exist).
   * 
   * @return the imported files (not <code>null</code>, contains no <code>null</code>s)
   */
  public Collection<File> getImportedFiles() {
    return library.getImportedFiles();
  }

  /**
   * A collection of files that the parsed file sources.
   * 
   * @return the sourced files (not <code>null</code>, contains no <code>null</code>s)
   */
  public Collection<File> getSourcedFiles() {
    return library.getSourceFiles();
  }

  /**
   * The {@link DartUnit} resulting from parsing the library file. The resulting unit may or may not
   * be resolved.
   * 
   * @return the dart unit or <code>null</code> if the unit could not be parsed (e.g. library file
   *         does not exist)
   */
  public DartUnit getUnit() {
    return unit;
  }
}
