package com.google.dart.tools.core.analysis;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;

import static com.google.dart.tools.core.analysis.AnalysisUtility.isSdkLibrary;

import java.io.File;

/**
 * Parse and cache a Dart source file in an appropriate context if it has not already been cached.
 * If this source file does not define a library, then parse and cache the library source file as
 * well. This uses {@link ParseTask} to perform the parse in the appropriate context.
 */
public class ParseRequestTask extends Task {
  private final AnalysisServer server;
  private final File libraryFile;
  private final String relPath;
  private final File dartFile;
  private final ParseCallback callback;

  public ParseRequestTask(AnalysisServer server, File libraryFile, String relPath, File dartFile,
      ParseCallback callback) {
    this.server = server;
    this.libraryFile = libraryFile;
    this.relPath = relPath;
    this.dartFile = dartFile;
    this.callback = callback;
  }

  @Override
  public boolean canRemove(File discarded) {
    return callback == null;
  }

  @Override
  public boolean isPriority() {
    return false;
  }

  @Override
  public void perform() {
    SavedContext savedContext = server.getSavedContext();

    // SDK libraries always reside in the saved context

    if (isSdkLibrary(libraryFile)) {
      Library library = savedContext.getCachedLibrary(libraryFile);
      if (library != null) {
        DartUnit dartUnit = library.getDartUnit(dartFile);
        if (dartUnit != null) {
          notifyCaller(library, dartUnit);
          return;
        }
      }
      queueParseTask(savedContext);
      return;
    }

    // Otherwise search all contexts for cached information

    Library[] libraries;
    libraries = savedContext.getCachedLibraries(libraryFile);
    for (Library library : libraries) {
      DartUnit dartUnit = library.getDartUnit(dartFile);
      if (dartUnit != null) {
        notifyCaller(library, dartUnit);
        return;
      }
    }

    // Determine the appropriate context and queue a subtask to parse the file

    Context context = libraries.length > 0 ? libraries[0].getContext()
        : savedContext.getSuggestedContext(libraryFile);
    queueParseTask(context);
  }

  private void notifyCaller(Library library, DartUnit dartUnit) {
    if (callback != null) {
      try {
        callback.parsed(new ParseResult(dartUnit, library.getParseErrors(dartFile)));
      } catch (Throwable e) {
        DartCore.logError("Exception during parse notification", e);
      }
    }
  }

  private void queueParseTask(Context context) {
    server.queueSubTask(new ParseTask(server, context, libraryFile, relPath, dartFile));
    server.queueSubTask(this);
  }
}
