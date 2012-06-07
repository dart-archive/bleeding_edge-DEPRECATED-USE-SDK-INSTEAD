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

import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;

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
  private final ParseCallback callback;

  ParseFileTask(AnalysisServer server, Context context, File libraryFile, String relPath,
      File dartFile, ParseCallback callback) {
    this.server = server;
    this.context = context;
    this.libraryFile = libraryFile;
    this.relPath = relPath;
    this.dartFile = dartFile;
    this.callback = callback;
  }

  @Override
  boolean isBackgroundAnalysis() {
    return true;
  }

  @Override
  boolean isPriority() {
    return false;
  }

  @Override
  void perform() {

    // Don't parse sourced files without first parsing the library file
    // because we need import prefixes for DartC to parse correctly

    Library library = context.getCachedLibrary(libraryFile);
    if (library != null) {
      parse(library);
      return;
    }

    server.queueSubTask(new ParseLibraryFileTask(server, context, libraryFile, new ParseCallback() {
      @Override
      public void parsed(DartUnit unit) {
        Library library = context.getCachedLibrary(libraryFile);
        if (library != null) {
          parse(library);
          return;
        }
        throw new RuntimeException("Failed to parse library: " + libraryFile);
      }
    }));
  }

  private void parse(Library library) {
    // Parse the file if it is not cached

    DartUnit unit = context.getCachedUnit(library, dartFile);
    if (unit == null) {
      Set<String> prefixes = library.getPrefixes();
      ErrorListener errorListener = new ErrorListener(server);
      DartSource source = library.getLibrarySource().getSourceFor(relPath);

      unit = AnalysisUtility.parse(dartFile, source, prefixes, errorListener);
      context.cacheUnresolvedUnit(dartFile, unit);
      if (library.shouldNotify) {
        AnalysisEvent event = new AnalysisEvent(libraryFile, errorListener.getErrors());
        event.addFileAndDartUnit(dartFile, unit);
        event.notifyParsed(context);
      }
    }

    // Notify the caller

    if (callback != null) {
      try {
        callback.parsed(unit);
      } catch (Throwable e) {
        DartCore.logError("Exception during parse notification", e);
      }
    }
  }
}
