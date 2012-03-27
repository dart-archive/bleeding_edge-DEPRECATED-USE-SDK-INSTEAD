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

import com.google.dart.compiler.UrlLibrarySource;
import com.google.dart.compiler.ast.DartUnit;

import static com.google.dart.tools.core.analysis.AnalysisUtility.parse;

import java.io.File;
import java.net.URI;

/**
 * Parse a library source file and create the associated {@link Library}
 */
class ParseLibraryFileTask extends Task {
  private final AnalysisServer server;
  private final Context context;
  private final File libraryFile;
  private final UrlLibrarySource librarySource;

  ParseLibraryFileTask(AnalysisServer server, Context context, File libraryFile) {
    this.server = server;
    this.context = context;
    this.libraryFile = libraryFile;
    URI fileUri = libraryFile.toURI();
    URI shortUri = server.getLibraryManager().getShortUri(fileUri);
    URI libUri = shortUri != null ? shortUri : fileUri;
    this.librarySource = new UrlLibrarySource(libUri, server.getLibraryManager());
  }

  @Override
  void perform() {
    if (!libraryFile.exists()) {
      return;
    }
    Library library = context.getCachedLibrary(libraryFile);
    if (library != null) {
      return;
    }
    DartUnit unit = parse(server, libraryFile, librarySource, libraryFile);
    library = Library.fromDartUnit(server, libraryFile, librarySource, unit);
    context.cacheLibrary(library);
  }
}
