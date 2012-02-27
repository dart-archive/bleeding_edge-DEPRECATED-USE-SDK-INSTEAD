/*
 * Copyright 2012 Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this libraryFile
 * except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.core.analysis;

import com.google.dart.compiler.UrlLibrarySource;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.LibraryUnit;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;

/**
 * Cached information about a Dart library used internally by the {@link AnalysisServer}.
 */
class Library {
  private final File libraryFile;
  private final UrlLibrarySource librarySource;
  private final HashMap<String, File> imports;
  private final HashMap<String, File> sources;
  private final HashMap<File, DartUnit> unitCache;
  private LibraryUnit libraryUnit;

  Library(File libraryFile, UrlLibrarySource librarySource, DartUnit libraryUnit,
      HashMap<String, File> imports, HashMap<String, File> sources) {
    this.libraryFile = libraryFile;
    this.librarySource = librarySource;
    this.imports = imports;
    this.sources = sources;
    this.unitCache = new HashMap<File, DartUnit>();
    this.unitCache.put(libraryFile, libraryUnit);
  }

  void cacheLibraryUnit(LibraryUnit unit) {
    this.libraryUnit = unit;
  }

  void cacheUnit(File file, DartUnit unit) {
    unitCache.put(file, unit);
  }

  DartUnit getCachedUnit(File file) {
    return unitCache.get(file);
  }

  HashMap<File, DartUnit> getCachedUnits() {
    return unitCache;
  }

  File getFile() {
    return libraryFile;
  }

  Collection<File> getImportedFiles() {
    return imports.values();
  }

  UrlLibrarySource getLibrarySource() {
    return librarySource;
  }

  LibraryUnit getLibraryUnit() {
    return libraryUnit;
  }

  Collection<File> getSourceFiles() {
    return sources.values();
  }
}
