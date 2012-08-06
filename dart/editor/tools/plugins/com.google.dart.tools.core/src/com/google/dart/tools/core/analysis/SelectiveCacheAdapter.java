package com.google.dart.tools.core.analysis;

import com.google.dart.compiler.DartCompiler.SelectiveCache;
import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.LibraryUnit;
import com.google.dart.tools.core.DartCore;

import static com.google.dart.tools.core.analysis.AnalysisUtility.toFile;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

final class SelectiveCacheAdapter extends SelectiveCache {

  private final AnalysisServer server;
  private final Context context;
  private final HashMap<File, HashSet<File>> parsedFiles;

  SelectiveCacheAdapter(AnalysisServer server, Context context) {
    this.server = server;
    this.context = context;
    this.parsedFiles = new HashMap<File, HashSet<File>>();
  }

  @Override
  public Map<URI, LibraryUnit> getResolvedLibraries() {
    return context.getResolvedLibraries();
  }

  @Override
  public DartUnit getUnresolvedDartUnit(DartSource dartSrc) {
    File libraryFile = toFile(server, dartSrc.getLibrary().getUri());
    File dartFile = toFile(server, dartSrc.getUri());
    if (libraryFile == null || dartFile == null) {
      return null;
    }
    Library library = context.getCachedLibrary(libraryFile);
    if (library != null) {

      // Sanity check... should not be requesting unresolved units for resolved libraries
      if (library.getLibraryUnit() != null) {
        DartCore.logError("Requesting unresolved dart unit: " + dartFile
            + "\n  but library is already resolved: " + libraryFile);
        return null;
      }

      DartUnit dartUnit = library.getDartUnit(dartFile);
      if (dartUnit != null) {
        return dartUnit;
      }
    }

    // If the desired unit is not cached, record the fact that it was requested
    // so that later we can notify others that it was parsed as part of the resolution process

    HashSet<File> files = parsedFiles.get(libraryFile);
    if (files == null) {
      files = new HashSet<File>();
      parsedFiles.put(libraryFile, files);
    }
    files.add(dartFile);
    return null;
  }

  /**
   * Answer the files in the specified files that were parsed during the process of resolution or
   * <code>null</code> if none
   */
  HashSet<File> getFilesParsedInLibrary(File libraryFile) {
    return parsedFiles.get(libraryFile);
  }
}
