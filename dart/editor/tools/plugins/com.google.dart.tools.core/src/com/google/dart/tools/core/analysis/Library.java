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

import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.ast.DartDirective;
import com.google.dart.compiler.ast.DartImportDirective;
import com.google.dart.compiler.ast.DartSourceDirective;
import com.google.dart.compiler.ast.DartStringLiteral;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.LibraryUnit;

import static com.google.dart.tools.core.analysis.AnalysisUtility.toFile;
import static com.google.dart.tools.core.analysis.AnalysisUtility.toLibrarySource;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Cached information about a Dart library used internally by the {@link AnalysisServer}.
 */
class Library {

  private static final String PREFIXES_TAG = "prefixes: ";
  private static final String DIRECTIVES_TAG = "directives: ";
  private static final int DIRECTIVES_LEN = DIRECTIVES_TAG.length();
  private static final String END_IMPORTS_TAG = "</end-imports>";
  private static final String END_SOURCES_TAG = "</end-sources>";

  /**
   * Construct a new library from the unresolved dart unit that defines the library
   */
  static Library fromDartUnit(AnalysisServer server, File libFile, LibrarySource libSource,
      Collection<DartDirective> directives) {
    HashMap<String, File> imports = new HashMap<String, File>();
    HashMap<String, File> sources = new HashMap<String, File>();
    URI base = libFile.toURI();

    // Resolve all #import and #source directives

    HashSet<String> prefixes = new HashSet<String>();
    for (DartDirective directive : directives) {
      String relPath;
      if (directive instanceof DartImportDirective) {
        DartImportDirective importDirective = (DartImportDirective) directive;
        DartStringLiteral prefix = importDirective.getPrefix();
        if (prefix != null) {
          prefixes.add(prefix.getValue());
        }
        relPath = importDirective.getLibraryUri().getValue();
        File file = server.resolvePath(base, relPath);
        if (file == null) {
          // Resolution errors reported by ResolveLibraryTask
        } else {
          imports.put(relPath, file);
        }
      } else if (directive instanceof DartSourceDirective) {
        relPath = ((DartSourceDirective) directive).getSourceUri().getValue();
        File file = server.resolvePath(base, relPath);
        if (file == null) {
          // Resolution errors reported by ResolveLibraryTask
        } else {
          sources.put(relPath, file);
        }
      }
    }

    // Import "dart:core" if it was not explicitly imported

    if (imports.get("dart:core") == null) {
      File file = server.resolvePath(base, "dart:core");
      if (file == null) {
        // Resolution errors reported by ResolveLibraryTask
      } else {
        imports.put("dart:core", file);
      }
    }

    boolean hasDirectives = directives.size() > 0;
    return new Library(libFile, libSource, prefixes, hasDirectives, imports, sources, true);
  }

  /**
   * Reload cached library information and mark the library so that listeners will not be notified
   * when the ASTs are re-parsed and re-resolved because they were already notified the first time
   * it was parsed and resolved.
   */
  static Library readCache(AnalysisServer server, File libFile, LineNumberReader reader)
      throws IOException {
    LibrarySource libSource = toLibrarySource(server, libFile);

    HashSet<String> prefixes = new HashSet<String>();
    String line = reader.readLine();
    if (line == null || !line.startsWith(PREFIXES_TAG)) {
      throw new IOException("Expected " + PREFIXES_TAG + " but found " + line);
    }
    int start = PREFIXES_TAG.length();
    int last = line.length() - 1;
    while (start < last) {
      int end = line.indexOf(',', start);
      prefixes.add(line.substring(start, end));
      start = end + 1;
    }

    line = reader.readLine();
    if (line == null || !line.startsWith(DIRECTIVES_TAG)) {
      throw new IOException("Expected " + DIRECTIVES_TAG + " but found " + line);
    }
    boolean hasDirectives = line.length() > DIRECTIVES_LEN && line.charAt(DIRECTIVES_LEN) == 't';

    HashMap<String, File> imports = new HashMap<String, File>();
    while (true) {
      line = reader.readLine();
      if (line == null) {
        throw new IOException("Expected " + END_IMPORTS_TAG + " but found EOF");
      }
      if (line.equals(END_IMPORTS_TAG)) {
        break;
      }
      String path = reader.readLine();
      if (path == null) {
        throw new IOException("Expected import path but found EOF");
      }
      imports.put(line, new File(path));
    }

    HashMap<String, File> sources = new HashMap<String, File>();
    while (true) {
      line = reader.readLine();
      if (line == null) {
        throw new IOException("Expected " + END_SOURCES_TAG + " but found EOF");
      }
      if (line.equals(END_SOURCES_TAG)) {
        break;
      }
      String path = reader.readLine();
      if (path == null) {
        throw new IOException("Expected source path but found EOF");
      }
      sources.put(line, new File(path));
    }

    return new Library(libFile, libSource, prefixes, hasDirectives, imports, sources, false);
  }

  private final File libraryFile;
  private final LibrarySource librarySource;
  private final Set<String> prefixes;
  private final boolean hasDirectives;
  private final HashMap<String, File> imports;
  private final HashMap<String, File> sources;
  private final HashMap<File, DartUnit> resolvedUnits;

  /**
   * Flag indicating if listeners should be notified when the library is parsed and resolved. This
   * is <code>false</code> when a library is reloaded from a cache file so that listeners will not
   * be notified because they were already notified when the library was first parsed and resolved.
   */
  public final boolean shouldNotify;

  private LibraryUnit libraryUnit;

  private Library(File libraryFile, LibrarySource librarySource, Set<String> prefixes,
      boolean hasDirectives, HashMap<String, File> imports, HashMap<String, File> sources,
      boolean shouldNotify) {
    this.libraryFile = libraryFile;
    this.librarySource = librarySource;
    this.prefixes = prefixes;
    this.hasDirectives = hasDirectives;
    this.imports = imports;
    this.sources = sources;
    this.shouldNotify = shouldNotify;
    this.resolvedUnits = new HashMap<File, DartUnit>();
  }

  void cacheLibraryUnit(AnalysisServer server, LibraryUnit libUnit) {
    this.libraryUnit = libUnit;
    for (DartUnit dartUnit : libUnit.getUnits()) {
      File file = toFile(server, dartUnit.getSourceInfo().getSource().getUri());
      if (file != null) {
        resolvedUnits.put(file, dartUnit);
      }
    }
  }

  File getFile() {
    return libraryFile;
  }

  Collection<File> getImportedFiles() {
    return imports.values();
  }

  LibrarySource getLibrarySource() {
    return librarySource;
  }

  LibraryUnit getLibraryUnit() {
    return libraryUnit;
  }

  Set<String> getPrefixes() {
    return prefixes;
  }

  Set<Entry<String, File>> getRelativeSourcePathsAndFiles() {
    return sources.entrySet();
  }

  DartUnit getResolvedUnit(File file) {
    return resolvedUnits.get(file);
  }

  HashMap<File, DartUnit> getResolvedUnits() {
    return resolvedUnits;
  }

  Collection<File> getSourceFiles() {
    return sources.values();
  }

  boolean hasBeenResolved() {
    return libraryUnit != null || !shouldNotify;
  }

  boolean hasDirectives() {
    return hasDirectives;
  }

  /**
   * Write information about the cached library, but do not write the AST structures themselves.
   */
  void writeCache(PrintWriter writer) {
    writer.print(PREFIXES_TAG);
    for (String eachPrefix : prefixes) {
      writer.print(eachPrefix);
      writer.print(',');
    }
    writer.println();
    writer.print(DIRECTIVES_TAG);
    writer.println(hasDirectives);
    for (Entry<String, File> entry : imports.entrySet()) {
      writer.println(entry.getKey());
      writer.println(entry.getValue().getPath());
    }
    writer.println(END_IMPORTS_TAG);
    for (Entry<String, File> entry : sources.entrySet()) {
      writer.println(entry.getKey());
      writer.println(entry.getValue().getPath());
    }
    writer.println(END_SOURCES_TAG);
  }
}
