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

/**
 * Parse a Dart source file and cache the result
 */
class ParseFileTask extends Task {
  private final AnalysisServer server;
  private final Library library;
  private final File file;

  ParseFileTask(AnalysisServer server, Library library, File file) {
    this.server = server;
    this.library = library;
    this.file = file;
  }

  @Override
  void perform() {
    DartUnit unit = parse(server, library.getFile(), library.getLibrarySource(), file);
    library.cacheUnit(file, unit);
  }
}
