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
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.source.SourceKind;

/**
 * Instances of the class {@code CompilationUnitInfo} maintain the information cached by an analysis
 * context about an individual compilation unit.
 * 
 * @coverage dart.engine
 */
public class CompilationUnitInfo extends SourceInfo {
  /**
   * The state of the cached parsed compilation unit.
   */
  private CacheState parsedUnitState = CacheState.INVALID;

  /**
   * The parsed compilation unit, or {@code null} if the parsed compilation unit is not currently
   * cached.
   */
  private CompilationUnit parsedUnit;

  /**
   * The state of the cached resolved compilation unit.
   */
  private CacheState resolvedUnitState = CacheState.INVALID;

  /**
   * The resolved compilation unit, or {@code null} if the resolved compilation unit is not
   * currently cached.
   */
  private CompilationUnit resolvedUnit;

  /**
   * The state of the cached parse errors.
   */
  private CacheState parseErrorsState = CacheState.INVALID;

  /**
   * The errors produced while scanning and parsing the compilation unit, or {@code null} if the
   * errors are not currently cached.
   */
  private AnalysisError[] parseErrors;

  /**
   * The state of the cached resolution errors.
   */
  private CacheState resolutionErrorsState = CacheState.INVALID;

  /**
   * The errors produced while resolving the compilation unit, or {@code null} if the errors are not
   * currently cached.
   */
  private AnalysisError[] resolutionErrors;

  /**
   * Initialize a newly created information holder to be empty.
   */
  public CompilationUnitInfo() {
    super();
  }

  /**
   * Remove the parsed compilation unit from the cache.
   */
  public void clearParsedUnit() {
    parsedUnit = null;
    parsedUnitState = CacheState.FLUSHED;
  }

  /**
   * Remove the parse errors from the cache.
   */
  public void clearParseErrors() {
    parseErrors = null;
    parseErrorsState = CacheState.FLUSHED;
  }

  /**
   * Remove the resolution errors from the cache.
   */
  public void clearResolutionErrors() {
    resolutionErrors = null;
    resolutionErrorsState = CacheState.FLUSHED;
  }

  /**
   * Remove the resolved compilation unit from the cache.
   */
  public void clearResolvedUnit() {
    resolvedUnit = null;
    resolvedUnitState = CacheState.FLUSHED;
  }

  @Override
  public CompilationUnitInfo copy() {
    CompilationUnitInfo copy = new CompilationUnitInfo();
    copy.copyFrom(this);
    return copy;
  }

  /**
   * Return all of the errors associated with the compilation unit.
   * 
   * @return all of the errors associated with the compilation unit
   */
  public AnalysisError[] getAllErrors() {
    if (parseErrors == null) {
      if (resolutionErrors == null) {
        return null;
      }
      return resolutionErrors;
    } else if (resolutionErrors == null) {
      return parseErrors;
    }
    int parseCount = parseErrors.length;
    int resolutionCount = resolutionErrors.length;
    AnalysisError[] errors = new AnalysisError[parseCount + resolutionCount];
    System.arraycopy(parseErrors, 0, errors, 0, parseCount);
    System.arraycopy(resolutionErrors, 0, errors, parseCount, resolutionCount);
    return errors;
  }

  @Override
  public SourceKind getKind() {
    return SourceKind.PART;
  }

  /**
   * Return the parsed compilation unit, or {@code null} if the parsed compilation unit is not
   * currently cached.
   * 
   * @return the parsed compilation unit
   */
  public CompilationUnit getParsedCompilationUnit() {
    return parsedUnit;
  }

  /**
   * Return the errors produced while scanning and parsing the compilation unit, or {@code null} if
   * the errors are not currently cached.
   * 
   * @return the errors produced while scanning and parsing the compilation unit
   */
  public AnalysisError[] getParseErrors() {
    return parseErrors;
  }

  /**
   * Return the errors produced while resolving the compilation unit, or {@code null} if the errors
   * are not currently cached.
   * 
   * @return the errors produced while resolving the compilation unit
   */
  public AnalysisError[] getResolutionErrors() {
    return resolutionErrors;
  }

  /**
   * Return the resolved compilation unit, or {@code null} if the resolved compilation unit is not
   * currently cached.
   * 
   * @return the resolved compilation unit
   */
  public CompilationUnit getResolvedCompilationUnit() {
    return resolvedUnit;
  }

  /**
   * Return {@code true} if the parsed compilation unit needs to be recomputed.
   * 
   * @return {@code true} if the parsed compilation unit needs to be recomputed
   */
  public boolean hasInvalidParsedUnit() {
    return parsedUnitState == CacheState.INVALID;
  }

  /**
   * Return {@code true} if the parse errors needs to be recomputed.
   * 
   * @return {@code true} if the parse errors needs to be recomputed
   */
  public boolean hasInvalidParseErrors() {
    return parseErrorsState == CacheState.INVALID;
  }

  /**
   * Return {@code true} if the resolution errors needs to be recomputed.
   * 
   * @return {@code true} if the resolution errors needs to be recomputed
   */
  public boolean hasInvalidResolutionErrors() {
    return resolutionErrorsState == CacheState.INVALID;
  }

  /**
   * Return {@code true} if the resolved compilation unit needs to be recomputed.
   * 
   * @return {@code true} if the resolved compilation unit needs to be recomputed
   */
  public boolean hasInvalidResolvedUnit() {
    return resolvedUnitState == CacheState.INVALID;
  }

  /**
   * Mark the parsed compilation unit as needing to be recomputed.
   */
  public void invalidateParsedUnit() {
    parsedUnitState = CacheState.INVALID;
    parsedUnit = null;
  }

  /**
   * Mark the parse errors as needing to be recomputed.
   */
  public void invalidateParseErrors() {
    parseErrorsState = CacheState.INVALID;
    parseErrors = null;
  }

  /**
   * Mark the resolution errors as needing to be recomputed.
   */
  public void invalidateResolutionErrors() {
    resolutionErrorsState = CacheState.INVALID;
    resolutionErrors = null;
  }

  /**
   * Mark the resolved compilation unit as needing to be recomputed.
   */
  public void invalidateResolvedUnit() {
    resolvedUnitState = CacheState.INVALID;
    resolvedUnit = null;
  }

  /**
   * Set the parsed compilation unit to the given compilation unit.
   * <p>
   * <b>Note:</b> Do not use this method to clear or invalidate the parsed compilation unit. Use
   * either {@link #clear} or {@link #invalidate}.
   * 
   * @param unit the parsed compilation unit
   */
  public void setParsedCompilationUnit(CompilationUnit unit) {
    parsedUnit = unit;
    parsedUnitState = CacheState.VALID;
  }

  /**
   * Set the errors produced while scanning and parsing the compilation unit to the given errors.
   * <p>
   * <b>Note:</b> Do not use this method to clear or invalidate the parse errors. Use either
   * {@link #clear} or {@link #invalidate}.
   * 
   * @param errors the errors produced while scanning and parsing the compilation unit
   */
  public void setParseErrors(AnalysisError[] errors) {
    parseErrors = errors;
    parseErrorsState = CacheState.VALID;
  }

  /**
   * Set the errors produced while resolving the compilation unit to the given errors.
   * <p>
   * <b>Note:</b> Do not use this method to clear or invalidate the resolution errors. Use either
   * {@link #clear} or {@link #invalidate}.
   * 
   * @param errors the errors produced while resolving the compilation unit
   */
  public void setResolutionErrors(AnalysisError[] errors) {
    resolutionErrors = errors;
    resolutionErrorsState = CacheState.VALID;
  }

  /**
   * Set the resolved compilation unit to the given compilation unit.
   * <p>
   * <b>Note:</b> Do not use this method to clear or invalidate the resolved compilation unit. Use
   * either {@link #clear} or {@link #invalidate}.
   * 
   * @param unit the resolved compilation unit
   */
  public void setResolvedCompilationUnit(CompilationUnit unit) {
    resolvedUnit = unit;
    resolvedUnitState = CacheState.VALID;
  }

  @Override
  protected void copyFrom(SourceInfo info) {
    super.copyFrom(info);
    // TODO(brianwilkerson) Decide how much of this data we can safely copy.
//    parsedUnitState = info.parsedUnitState;
//    parsedUnit = info.parsedUnit;
//    resolvedUnitState = info.resolvedUnitState;
//    resolvedUnit = info.resolvedUnit;
//    parseErrorsState = info.parseErrorsState;
//    parseErrors = info.parseErrors;
//    resolutionErrorsState = info.resolutionErrorsState;
//    resolutionErrors = info.resolutionErrors;
//    librarySources = new ArrayList<Source>(info.librarySources);
  }
}
