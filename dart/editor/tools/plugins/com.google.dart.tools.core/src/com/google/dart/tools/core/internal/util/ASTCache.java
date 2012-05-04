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
package com.google.dart.tools.core.internal.util;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.LibraryUnit;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;

import org.eclipse.core.resources.IResource;

import java.util.HashMap;

/**
 * Instances of the class <code>ASTCache</code> maintain a cache of AST structures associated with
 * compilation units.
 */
public class ASTCache {
  /**
   * Instances of the class <code>CacheEntry</code> contain the information being cached about a
   * single compilation unit.
   */
  private static class CacheEntry {
    /**
     * The modification stamp of the compilation unit at the time the AST structure was created.
     */
    private long modificationStamp;

    /**
     * The AST structure that was created from the compilation unit.
     */
    private DartUnit ast;

    /**
     * Return <code>true</code> if the AST structure maintained by this entry needs to be
     * recomputed. The AST structure needs to be recreated if it does not exist, or if the
     * compilation unit has been modified since the AST structure was last created.
     * 
     * @param compilationUnit the compilation unit associated with the entry
     * @return <code>true</code> if the AST structure maintained by this entry needs to be
     *         recomputed
     */
    public boolean isStale(CompilationUnit compilationUnit) {
      return ast == null || modificationStamp != compilationUnit.getModificationStamp();
    }
  }

  /**
   * A table mapping compilation units to the cache entries associated with those units.
   */
  private HashMap<CompilationUnit, CacheEntry> entryMap = new HashMap<CompilationUnit, CacheEntry>();

  /**
   * The number of milliseconds that have been spent parsing source code since the last time that
   * the time was requested.
   */
  private long timeSpentParsing = 0;

  /**
   * Flush the contents of the cache.
   */
  public void flush() {
    entryMap.clear();
  }

  /**
   * Return the number of milliseconds that have been spent parsing source code since the last time
   * that the time was requested using this method.
   * 
   * @return the number of milliseconds that have been spent parsing source code
   */
  public long getAndResetTimeSpentParsing() {
    long result = timeSpentParsing;
    timeSpentParsing = 0;
    return result;
  }

  /**
   * Return the AST structure corresponding to the contents of the given compilation unit.
   * 
   * @param compilationUnit the compilation unit whose AST structure is to be returned
   * @return the AST structure corresponding to the contents of the given compilation unit
   */
  public DartUnit getAST(CompilationUnit compilationUnit) {
    CacheEntry entry = getOrCreateCacheEntry(compilationUnit);
    if (entry.isStale(compilationUnit)) {
      entry.ast = null;
      DartLibraryImpl library = (DartLibraryImpl) compilationUnit.getLibrary();
      if (library == null) {
        try {
          long startParsing = System.currentTimeMillis();
          entry.ast = DartCompilerUtilities.resolveUnit(compilationUnit);
          long endParsing = System.currentTimeMillis();
          timeSpentParsing += (endParsing - startParsing);
        } catch (DartModelException exception) {
          DartCore.logError(
              "Could not parse compilation unit " + compilationUnit.getElementName(),
              exception);
        }
      } else {
        try {
          long startParsing = System.currentTimeMillis();
          LibraryUnit libraryUnit = DartCompilerUtilities.resolveLibrary(library, true, null);
          long endParsing = System.currentTimeMillis();
          timeSpentParsing += (endParsing - startParsing);
          if (libraryUnit != null) {
            for (CompilationUnit unitInLibrary : library.getCompilationUnits()) {
              IResource resource = unitInLibrary.getResource();
              if (resource != null && resource.getLocationURI() != null) {
                CacheEntry entryInLibrary = getOrCreateCacheEntry(unitInLibrary);
                entryInLibrary.ast = libraryUnit.getUnit(resource.getLocationURI().toString());
                if (entryInLibrary.ast == null) {
                  //TODO ugly hack to work around the fact that the compiler appends the file name to the file URI
                  entryInLibrary.ast = libraryUnit.getUnit(library.getCorrespondingResource().getLocationURI().toString()
                      + "/" + resource.getName());
                }
              }
            }
          }
        } catch (DartModelException exception) {
          DartCore.logError(
              "Could not parse library " + library.getResource().getLocation(),
              exception);
        }
      }
    }
    if (entry.ast != null && entry.ast.isDiet()) {
      entry.ast = null;
    }
    return entry.ast;
  }

  /**
   * Return the cache entry associated with the given compilation unit.
   * 
   * @param compilationUnit the compilation unit whose cache entry is to be returned
   * @return the cache entry associated with the given compilation unit
   */
  private CacheEntry getOrCreateCacheEntry(CompilationUnit compilationUnit) {
    CacheEntry entry = entryMap.get(compilationUnit);
    if (entry == null) {
      entry = new CacheEntry();
      entry.modificationStamp = compilationUnit.getModificationStamp();
      entryMap.put(compilationUnit, entry);
    }
    return entry;
  }
}
