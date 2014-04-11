/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.internal.context;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.internal.cache.DartEntry;
import com.google.dart.engine.internal.cache.DartEntryImpl;
import com.google.dart.engine.internal.cache.SourceEntry;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.ast.AstComparator;

/**
 * Instances of the class {@code IncrementalAnalysisCache} hold information used to perform
 * incremental analysis.
 * 
 * @see AnalysisContextImpl#setChangedContents(Source, String, int, int, int)
 */
public class IncrementalAnalysisCache {

  /**
   * Determine if the incremental analysis result can be cached for the next incremental analysis.
   * 
   * @param cache the prior incremental analysis cache
   * @param unit the incrementally updated compilation unit
   * @return the cache used for incremental analysis or {@code null} if incremental analysis results
   *         cannot be cached for the next incremental analysis
   */
  public static IncrementalAnalysisCache cacheResult(IncrementalAnalysisCache cache,
      CompilationUnit unit) {
    if (cache != null && unit != null) {
      return new IncrementalAnalysisCache(
          cache.librarySource,
          cache.source,
          unit,
          cache.newContents,
          cache.newContents,
          0,
          0,
          0);
    }
    return null;
  }

  /**
   * Determine if the cache should be cleared.
   * 
   * @param cache the prior cache or {@code null} if none
   * @param source the source being updated (not {@code null})
   * @return the cache used for incremental analysis or {@code null} if incremental analysis cannot
   *         be performed
   */
  public static IncrementalAnalysisCache clear(IncrementalAnalysisCache cache, Source source) {
    if (cache == null || cache.getSource().equals(source)) {
      return null;
    }
    return cache;
  }

  /**
   * Determine if incremental analysis can be performed from the given information.
   * 
   * @param cache the prior cache or {@code null} if none
   * @param source the source being updated (not {@code null})
   * @param oldContents the original source contents prior to this update (may be {@code null})
   * @param newContents the new contents after this incremental change (not {@code null})
   * @param offset the offset at which the change occurred
   * @param oldLength the length of the text being replaced
   * @param newLength the length of the replacement text
   * @param sourceEntry the cached entry for the given source or {@code null} if none
   * @return the cache used for incremental analysis or {@code null} if incremental analysis cannot
   *         be performed
   */
  public static IncrementalAnalysisCache update(IncrementalAnalysisCache cache, Source source,
      String oldContents, String newContents, int offset, int oldLength, int newLength,
      SourceEntry sourceEntry) {

    // Determine the cache resolved unit
    Source librarySource = null;
    CompilationUnit unit = null;
    if (sourceEntry instanceof DartEntryImpl) {
      DartEntryImpl dartEntry = (DartEntryImpl) sourceEntry;
      Source[] librarySources = dartEntry.getLibrariesContaining();
      if (librarySources.length == 1) {
        librarySource = librarySources[0];
        if (librarySource != null) {
          unit = dartEntry.getValueInLibrary(DartEntry.RESOLVED_UNIT, librarySource);
        }
      }
    }

    // Create a new cache if there is not an existing cache or the source is different
    // or a new resolved compilation unit is available
    if (cache == null || !cache.getSource().equals(source) || unit != null) {
      if (unit == null) {
        return null;
      }
      if (oldContents == null) {
        if (oldLength != 0) {
          return null;
        }
        oldContents = newContents.substring(0, offset) + newContents.substring(offset + newLength);
      }
      return new IncrementalAnalysisCache(
          librarySource,
          source,
          unit,
          oldContents,
          newContents,
          offset,
          oldLength,
          newLength);
    }

    // Update the existing cache if the change is contiguous
    if (cache.oldLength == 0 && cache.newLength == 0) {
      cache.offset = offset;
      cache.oldLength = oldLength;
      cache.newLength = newLength;
    } else {
      if (cache.offset > offset || offset > cache.offset + cache.newLength) {
        return null;
      }
      cache.newLength += newLength - oldLength;
    }
    cache.newContents = newContents;
    return cache;
  }

  /**
   * Verify that the incrementally parsed and resolved unit in the incremental cache is structurally
   * equivalent to the fully parsed unit.
   * 
   * @param cache the prior cache or {@code null} if none
   * @param source the source of the compilation unit that was parsed (not {@code null})
   * @param unit the compilation unit that was just parsed
   * @return the cache used for incremental analysis or {@code null} if incremental analysis results
   *         cannot be cached for the next incremental analysis
   */
  public static IncrementalAnalysisCache verifyStructure(IncrementalAnalysisCache cache,
      Source source, CompilationUnit unit) {
    if (cache != null && unit != null && cache.source.equals(source)) {
      if (!AstComparator.equalNodes(cache.resolvedUnit, unit)) {
        return null;
      }
    }
    return cache;
  }

  private final Source librarySource;
  private final Source source;
  private final String oldContents;
  private final CompilationUnit resolvedUnit;

  private String newContents;
  private int offset;
  private int oldLength;
  private int newLength;

  public IncrementalAnalysisCache(Source librarySource, Source source,
      CompilationUnit resolvedUnit, String oldContents, String newContents, int offset,
      int oldLength, int newLength) {
    this.librarySource = librarySource;
    this.source = source;
    this.resolvedUnit = resolvedUnit;
    this.oldContents = oldContents;
    this.newContents = newContents;
    this.offset = offset;
    this.oldLength = oldLength;
    this.newLength = newLength;
  }

  /**
   * Answer the library source for the incremental analysis to be performed
   * 
   * @return the source (not {@code null})
   */
  public Source getLibrarySource() {
    return librarySource;
  }

  /**
   * Return the current contents for the receiver's source.
   * 
   * @return the contents (not {@code null})
   */
  public String getNewContents() {
    return newContents;
  }

  /**
   * Return the number of characters in the replacement text.
   * 
   * @return the replacement length (zero or greater)
   */
  public int getNewLength() {
    return newLength;
  }

  /**
   * Return the character position of the first changed character.
   * 
   * @return the offset (zero or greater)
   */
  public int getOffset() {
    return offset;
  }

  /**
   * Return the original contents for the receiver's source.
   * 
   * @return the contents (not {@code null})
   */
  public String getOldContents() {
    return oldContents;
  }

  /**
   * Return the number of characters that were replaced.
   * 
   * @return the replaced length (zero or greater)
   */
  public int getOldLength() {
    return oldLength;
  }

  /**
   * Return the resolved compilation unit to be used for incremental analysis
   * 
   * @return the resolved unit (not {@code null})
   */
  public CompilationUnit getResolvedUnit() {
    return resolvedUnit;
  }

  /**
   * Return the source for which incremental analysis is to be performed
   * 
   * @return the source (not {@code null})
   */
  public Source getSource() {
    return source;
  }

  /**
   * Determine if the cache contains source changes that need to be analyzed
   * 
   * @return {@code true} if the cache contains changes to be analyzed, else {@code false}
   */
  public boolean hasWork() {
    return oldLength > 0 || newLength > 0;
  }
}
