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
package com.google.dart.engine.internal.cache;

import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.internal.context.CacheState;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceKind;

/**
 * Instances of the class {@code HtmlEntryImpl} implement an {@link HtmlEntry}.
 * 
 * @coverage dart.engine
 */
public class HtmlEntryImpl extends SourceEntryImpl implements HtmlEntry {
  /**
   * The state of the cached parsed (but not resolved) HTML unit.
   */
  private CacheState parsedUnitState = CacheState.INVALID;

  /**
   * The parsed HTML unit, or {@code null} if the parsed HTML unit is not currently cached.
   */
  private HtmlUnit parsedUnit;

  /**
   * The state of the cached list of referenced libraries.
   */
  private CacheState referencedLibrariesState = CacheState.INVALID;

  /**
   * The list of libraries referenced in the HTML, or {@code null} if the list is not currently
   * cached. Note that this list does not include libraries defined directly within the HTML file.
   */
  private Source[] referencedLibraries = Source.EMPTY_ARRAY;

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
   * Initialize a newly created cache entry to be empty.
   */
  public HtmlEntryImpl() {
    super();
  }

  @Override
  public SourceKind getKind() {
    return SourceKind.HTML;
  }

  @Override
  public CacheState getState(DataDescriptor<?> descriptor) {
    if (descriptor == ELEMENT) {
      return elementState;
    } else if (descriptor == PARSED_UNIT) {
      return parsedUnitState;
    } else if (descriptor == REFERENCED_LIBRARIES) {
      return referencedLibrariesState;
    } else if (descriptor == RESOLVED_UNIT) {
      return resolvedUnitState;
    }
    return super.getState(descriptor);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <E> E getValue(DataDescriptor<E> descriptor) {
    if (descriptor == ELEMENT) {
      return (E) element;
    } else if (descriptor == PARSED_UNIT) {
      return (E) parsedUnit;
    } else if (descriptor == REFERENCED_LIBRARIES) {
      return (E) referencedLibraries;
    } else if (descriptor == RESOLVED_UNIT) {
      return (E) resolvedUnit;
    }
    return super.getValue(descriptor);
  }

  @Override
  public HtmlEntryImpl getWritableCopy() {
    HtmlEntryImpl copy = new HtmlEntryImpl();
    copy.copyFrom(this);
    return copy;
  }

  @Override
  public void setState(DataDescriptor<?> descriptor, CacheState state) {
    if (descriptor == ELEMENT) {
      element = updatedValue(state, element, null);
      elementState = state;
    } else if (descriptor == PARSED_UNIT) {
      parsedUnit = updatedValue(state, parsedUnit, null);
      parsedUnitState = state;
    } else if (descriptor == REFERENCED_LIBRARIES) {
      referencedLibraries = updatedValue(state, referencedLibraries, Source.EMPTY_ARRAY);
      referencedLibrariesState = state;
    } else if (descriptor == RESOLVED_UNIT) {
      resolvedUnit = updatedValue(state, resolvedUnit, null);
      resolvedUnitState = state;
    } else {
      super.setState(descriptor, state);
    }
  }

  @Override
  public <E> void setValue(DataDescriptor<E> descriptor, E value) {
    if (descriptor == ELEMENT) {
      element = (HtmlElement) value;
      elementState = CacheState.VALID;
    } else if (descriptor == PARSED_UNIT) {
      parsedUnit = (HtmlUnit) value;
      parsedUnitState = CacheState.VALID;
    } else if (descriptor == REFERENCED_LIBRARIES) {
      referencedLibraries = value == null ? Source.EMPTY_ARRAY : (Source[]) value;
      referencedLibrariesState = CacheState.VALID;
    } else if (descriptor == RESOLVED_UNIT) {
      resolvedUnit = (HtmlUnit) value;
      resolvedUnitState = CacheState.VALID;
    } else {
      super.setValue(descriptor, value);
    }
  }

  @Override
  protected void copyFrom(SourceEntryImpl entry) {
    super.copyFrom(entry);
    HtmlEntryImpl other = (HtmlEntryImpl) entry;
    parsedUnitState = other.parsedUnitState;
    parsedUnit = other.parsedUnit;
    referencedLibrariesState = other.referencedLibrariesState;
    referencedLibraries = other.referencedLibraries;
    resolvedUnitState = other.resolvedUnitState;
    resolvedUnit = other.resolvedUnit;
    elementState = other.elementState;
    element = other.element;
  }
}
