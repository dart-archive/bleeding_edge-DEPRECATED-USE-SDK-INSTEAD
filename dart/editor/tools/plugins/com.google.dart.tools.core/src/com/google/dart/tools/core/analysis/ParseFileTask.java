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

import static com.google.dart.tools.core.analysis.AnalysisUtility.parse;

import java.io.File;
import java.util.Set;

/**
 * Parse a Dart source file and cache the result
 */
class ParseFileTask extends Task {
  private final AnalysisServer server;
  private final Context context;
  private final File libraryFile;
  private final String relPath;
  private final File dartFile;

  ParseFileTask(AnalysisServer server, Context context, File libraryFile, String relPath,
      File dartFile) {
    this.server = server;
    this.context = context;
    this.libraryFile = libraryFile;
    this.relPath = relPath;
    this.dartFile = dartFile;
  }

  @Override
  boolean isBackgroundAnalysis() {
    return true;
  }

  @Override
  void perform() {
    if (!dartFile.exists()) {
      return;
    }

    // Don't parse sourced files without first parsing the library file
    // because we need import prefixes for DartC to parse correctly

    Library library = context.getCachedLibrary(libraryFile);
    if (library == null) {
      return;
    }

    // Parse the file if it is not cached

    DartUnit unit = library.getResolvedUnit(dartFile);
    if (unit != null) {
      return;
    }
    unit = context.getUnresolvedUnit(dartFile);
    if (unit != null) {
      return;
    }
    Set<String> prefixes = library.getPrefixes();
    unit = parse(server, libraryFile, library.getLibrarySource(), relPath, prefixes);
    context.cacheUnresolvedUnit(dartFile, unit);
  }
}
