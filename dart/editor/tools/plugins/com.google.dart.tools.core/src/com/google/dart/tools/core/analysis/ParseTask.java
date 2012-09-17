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
package com.google.dart.tools.core.analysis;

import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.UrlLibrarySource;
import com.google.dart.compiler.ast.DartDirective;
import com.google.dart.compiler.ast.DartUnit;

import static com.google.dart.tools.core.analysis.AnalysisUtility.parse;
import static com.google.dart.tools.core.analysis.AnalysisUtility.toLibrarySource;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Parse and cache a Dart source file in a specific context if it has not already been cached. If
 * this source file does not define a library, then parse and cache the library source file as well.
 * If you do not know the context in which the parse is or should be cached, use
 * {@link ParseRequestTask} instead.
 */
public class ParseTask extends Task {
  private final AnalysisServer server;
  private final Context context;
  private final File libraryFile;
  private final String relPath;
  private final File dartFile;

  public ParseTask(AnalysisServer server, Context context, File libraryFile) {
    this(server, context, libraryFile, libraryFile.getName(), libraryFile);
  }

  public ParseTask(AnalysisServer server, Context context, File libraryFile, String relPath,
      File dartFile) {
    this.server = server;
    this.context = context;
    this.libraryFile = libraryFile;
    this.relPath = relPath;
    this.dartFile = dartFile;
  }

  @Override
  public boolean canRemove(File discarded) {
    return true;
  }

  @Override
  public boolean isPriority() {
    return false;
  }

  @Override
  public void perform() {

    // Ensure the library is built

    Library library = context.getCachedLibrary(libraryFile);
    if (library == null) {
      Set<String> prefixes = new HashSet<String>();
      ErrorListener errorListener = new ErrorListener(context);
      UrlLibrarySource librarySource = toLibrarySource(context, libraryFile);
      DartSource source = librarySource.getSourceFor(libraryFile.getName());

      DartUnit unit = parse(libraryFile, source, prefixes, errorListener);

      AnalysisEvent event = new AnalysisEvent(
          context.getApplicationDirectory(),
          libraryFile,
          errorListener.getErrors());
      event.addFileAndDartUnit(libraryFile, unit);
      server.getSavedContext().notifyParsed(event);

      List<DartDirective> directives = unit.getDirectives();
      library = Library.fromDartUnit(server, context, libraryFile, librarySource, directives);
      library.cacheDartUnit(libraryFile, unit, errorListener.getErrors());
      context.cacheLibrary(library);
    }

    // Ensure the desired file is parsed... dartFile may == libraryFile

    DartUnit unit = library.getDartUnit(dartFile);
    if (unit == null) {
      Set<String> prefixes = library.getPrefixes();
      ErrorListener errorListener = new ErrorListener(context);
      DartSource source = library.getLibrarySource().getSourceFor(relPath);

      unit = AnalysisUtility.parse(dartFile, source, prefixes, errorListener);

      library.cacheDartUnit(dartFile, unit, errorListener.getErrors());
      if (library.shouldNotify) {
        AnalysisEvent event = new AnalysisEvent(
            context.getApplicationDirectory(),
            libraryFile,
            errorListener.getErrors());
        event.addFileAndDartUnit(dartFile, unit);
        server.getSavedContext().notifyParsed(event);
      }
    }
  }
}
