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

import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceKind;

/**
 * Instances of the class {@code HtmlUnitInfo} maintain the information cached by an analysis
 * context about an individual HTML file.
 * 
 * @coverage dart.engine
 */
public class HtmlUnitInfo extends SourceInfo {
  /**
   * The state of the cached parsed (but not resolved) HTML unit.
   */
  private CacheState parsedUnitState = CacheState.INVALID;

  /**
   * The parsed HTML unit, or {@code null} if the parsed HTML unit is not currently cached.
   */
  private HtmlUnit parsedUnit;

  /**
   * The state of the cached parsed and resolved HTML unit.
   */
  private CacheState resolvedUnitState = CacheState.INVALID;

  /**
   * The resolved HTML unit, or {@code null} if the resolved HTML unit is not currently cached.
   */
  private HtmlUnit resolvedUnit;

  /**
   * The state of the cached HTML element.
   */
  private CacheState elementState = CacheState.INVALID;

  /**
   * The element representing the HTML file, or {@code null} if the element is not currently cached.
   */
  private HtmlElement element;

  /**
   * The state of the cached library sources.
   */
  private CacheState librarySourcesState = CacheState.INVALID;

  /**
   * The sources of libraries referenced by this HTML file.
   */
  private Source[] librarySources = Source.EMPTY_ARRAY;

  /**
   * Initialize a newly created information holder to be empty.
   */
  public HtmlUnitInfo() {
    super();
  }

  /**
   * Remove the parsed HTML unit from the cache.
   */
  public void clearParsedUnit() {
    parsedUnit = null;
    parsedUnitState = CacheState.FLUSHED;
  }

  /**
   * Remove the resolved HTML unit from the cache.
   */
  public void clearResolvedUnit() {
    resolvedUnit = null;
    resolvedUnitState = CacheState.FLUSHED;
  }

  @Override
  public HtmlUnitInfo copy() {
    HtmlUnitInfo copy = new HtmlUnitInfo();
    copy.copyFrom(this);
    return copy;
  }

  /**
   * Return the element representing the HTML file, or {@code null} if the element is not currently
   * cached.
   * 
   * @return the element representing the HTML file
   */
  public HtmlElement getElement() {
    return element;
  }

  @Override
  public SourceKind getKind() {
    return SourceKind.HTML;
  }

  /**
   * Return the sources of libraries referenced by this HTML file.
   * 
   * @return the sources of libraries referenced by this HTML file
   */
  public Source[] getLibrarySources() {
    return librarySources;
  }

  /**
   * Return the parsed HTML unit, or {@code null} if the parsed HTML unit is not currently cached.
   * 
   * @return the parsed HTML unit
   */
  public HtmlUnit getParsedUnit() {
    return parsedUnit;
  }

  /**
   * Return the resolved HTML unit, or {@code null} if the resolved HTML unit is not currently
   * cached.
   * 
   * @return the resolved HTML unit
   */
  public HtmlUnit getResolvedUnit() {
    return resolvedUnit;
  }

  /**
   * Return {@code true} if the HTML element needs to be recomputed.
   * 
   * @return {@code true} if the HTML element needs to be recomputed
   */
  public boolean hasInvalidElement() {
    return elementState == CacheState.INVALID;
  }

  /**
   * Return {@code true} if the parsed HTML unit needs to be recomputed.
   * 
   * @return {@code true} if the parsed HTML unit needs to be recomputed
   */
  public boolean hasInvalidParsedUnit() {
    return parsedUnitState == CacheState.INVALID;
  }

  /**
   * Return {@code true} if the resolved HTML unit needs to be recomputed.
   * 
   * @return {@code true} if the resolved HTML unit needs to be recomputed
   */
  public boolean hasInvalidResolvedUnit() {
    return resolvedUnitState == CacheState.INVALID;
  }

  /**
   * Mark the HTML element as needing to be recomputed.
   */
  public void invalidateElement() {
    elementState = CacheState.INVALID;
    element = null;
  }

  /**
   * Mark the library sources as needing to be recomputed.
   */
  public void invalidateLibrarySources() {
    librarySourcesState = CacheState.INVALID;
    librarySources = Source.EMPTY_ARRAY;
  }

  /**
   * Mark the parsed HTML unit as needing to be recomputed.
   */
  public void invalidateParsedUnit() {
    parsedUnitState = CacheState.INVALID;
    parsedUnit = null;
  }

  /**
   * Mark the resolved HTML unit as needing to be recomputed.
   */
  public void invalidateResolvedUnit() {
    invalidateElement();
    resolvedUnitState = CacheState.INVALID;
    resolvedUnit = null;
  }

  /**
   * Set the element representing the HTML file to the given element.
   * <p>
   * <b>Note:</b> Do not use this method to clear or invalidate the element. Use either
   * {@link #clearElement()} or {@link #invalidateElement()}.
   * 
   * @param element the element representing the HTML file
   */
  public void setElement(HtmlElement element) {
    this.element = element;
    elementState = CacheState.VALID;
  }

  /**
   * Set the sources of libraries referenced by this HTML file to the given sources.
   * 
   * @param sources the sources of libraries referenced by this HTML file
   */
  public void setLibrarySources(Source[] sources) {
    librarySources = sources;
    librarySourcesState = CacheState.VALID;
  }

  /**
   * Set the parsed HTML unit to the given HTML unit.
   * <p>
   * <b>Note:</b> Do not use this method to clear or invalidate the HTML unit. Use either
   * {@link #clearParsedUnit()} or {@link #invalidateParsedUnit()}.
   * 
   * @param unit the result of parsing the source as an HTML unit
   */
  public void setParsedUnit(HtmlUnit unit) {
    parsedUnit = unit;
    parsedUnitState = CacheState.VALID;
  }

  /**
   * Set the resolved HTML unit to the given HTML unit.
   * <p>
   * <b>Note:</b> Do not use this method to clear or invalidate the HTML unit. Use either
   * {@link #clearResolvedUnit()} or {@link #invalidateResolvedUnit()}.
   * 
   * @param unit the result of resolving the source as an HTML unit
   */
  public void setResolvedUnit(HtmlUnit unit) {
    resolvedUnit = unit;
    resolvedUnitState = CacheState.VALID;
  }

  @Override
  protected void copyFrom(SourceInfo info) {
    super.copyFrom(info);
    // TODO(brianwilkerson) Decide how much of this data we can safely copy.
//    HtmlUnitInfo htmlInfo = (HtmlUnitInfo) info;
//    parsedUnitState = htmlInfo.parsedUnitState;
//    parsedUnit = htmlInfo.parsedUnit;
//    resolvedUnitState = htmlInfo.resolvedUnitState;
//    resolvedUnit = htmlInfo.resolvedUnit;
//    librarySourcesState = htmlInfo.librarySourcesState;
//    librarySources = htmlInfo.librarySources;
  }
}
