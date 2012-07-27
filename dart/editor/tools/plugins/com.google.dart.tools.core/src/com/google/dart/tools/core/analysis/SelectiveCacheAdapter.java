package com.google.dart.tools.core.analysis;

import com.google.dart.compiler.DartCompiler.SelectiveCache;
import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.SystemLibraryManager;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.LibraryUnit;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;

import java.net.URI;
import java.util.HashSet;
import java.util.Map;

final class SelectiveCacheAdapter extends SelectiveCache {
  private final Context context;
  private final HashSet<URI> parsedUnitURIs;

  SelectiveCacheAdapter(Context context) {
    this.context = context;
    this.parsedUnitURIs = new HashSet<URI>();
  }

  @Override
  public Map<URI, LibraryUnit> getResolvedLibraries() {
    return context.getResolvedLibraries();
  }

  @Override
  public DartUnit getUnresolvedDartUnit(DartSource dartSrc) {

    // Remove the parsed unit from the map if present
    // so that it will not be consumed a 2nd time if it is sourced by multiple libraries

    URI srcUri = dartSrc.getUri();
    DartUnit dartUnit = context.getUnresolvedUnits().remove(srcUri);
    if (dartUnit != null) {
      return dartUnit;
    }
    SystemLibraryManager libraryManager = SystemLibraryManagerProvider.getSystemLibraryManager();
    URI fileUri = libraryManager.resolveDartUri(srcUri);
    dartUnit = context.getUnresolvedUnits().remove(fileUri);
    if (dartUnit != null) {
      return dartUnit;
    }

    // If the desired unit is not cached, record the fact that it was requested
    // so that later we can notify others that it was parsed as part of this process

    parsedUnitURIs.add(srcUri);
    if (fileUri != null) {
      parsedUnitURIs.add(fileUri);
    }
    return null;
  }

  /**
   * Answer the URIs for the files that were parsed during the resolution process
   * 
   * @return a collection of URIs (not <code>null</code>, contains no <code>null</code>s)
   */
  HashSet<URI> getParsedUnitURIs() {
    return parsedUnitURIs;
  }
}
