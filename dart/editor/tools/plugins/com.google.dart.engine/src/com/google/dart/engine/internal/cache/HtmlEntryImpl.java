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
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceKind;

import java.util.ArrayList;

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
   * The state of the cached resolution errors.
   */
  private CacheState resolutionErrorsState = CacheState.INVALID;

  /**
   * The errors produced while resolving the compilation unit, or {@code null} if the errors are not
   * currently cached.
   */
  private AnalysisError[] resolutionErrors = AnalysisError.NO_ERRORS;

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
   * The state of the cached HTML element.
   */
  private CacheState elementState = CacheState.INVALID;

  /**
   * The element representing the HTML file, or {@code null} if the element is not currently cached.
   */
  private HtmlElement element;

  /**
   * The state of the cached hints.
   */
  private CacheState hintsState = CacheState.INVALID;

  /**
   * The hints produced while auditing the compilation unit, or an empty array if the hints are not
   * currently cached.
   */
  private AnalysisError[] hints = AnalysisError.NO_ERRORS;

  /**
   * Initialize a newly created cache entry to be empty.
   */
  public HtmlEntryImpl() {
    super();
  }

  @Override
  public AnalysisError[] getAllErrors() {
    ArrayList<AnalysisError> errors = new ArrayList<AnalysisError>();
//    for (AnalysisError error : parseErrors) {
//      errors.add(error);
//    }
    for (AnalysisError error : resolutionErrors) {
      errors.add(error);
    }
    for (AnalysisError error : hints) {
      errors.add(error);
    }
    if (errors.size() == 0) {
      return AnalysisError.NO_ERRORS;
    }
    return errors.toArray(new AnalysisError[errors.size()]);
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
    } else if (descriptor == RESOLUTION_ERRORS) {
      return resolutionErrorsState;
    } else if (descriptor == HINTS) {
      return hintsState;
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
    } else if (descriptor == RESOLUTION_ERRORS) {
      return (E) resolutionErrors;
    } else if (descriptor == HINTS) {
      return (E) hints;
    }
    return super.getValue(descriptor);
  }

  @Override
  public HtmlEntryImpl getWritableCopy() {
    HtmlEntryImpl copy = new HtmlEntryImpl();
    copy.copyFrom(this);
    return copy;
  }

  /**
   * Invalidate all of the information associated with the HTML file.
   */
  public void invalidateAllInformation() {
    setState(LINE_INFO, CacheState.INVALID);

    parsedUnit = null;
    parsedUnitState = CacheState.INVALID;

    referencedLibraries = Source.EMPTY_ARRAY;
    referencedLibrariesState = CacheState.INVALID;

    invalidateAllResolutionInformation();
  }

  /**
   * Invalidate all of the resolution information associated with the HTML file.
   */
  public void invalidateAllResolutionInformation() {
    element = null;
    elementState = CacheState.INVALID;

    resolutionErrors = AnalysisError.NO_ERRORS;
    resolutionErrorsState = CacheState.INVALID;

    hints = AnalysisError.NO_ERRORS;
    hintsState = CacheState.INVALID;
  }

  /**
   * Record that an error was encountered while attempting to parse the source associated with this
   * entry.
   */
  public void recordParseError() {
    setState(SourceEntry.LINE_INFO, CacheState.ERROR);
    setState(HtmlEntry.PARSED_UNIT, CacheState.ERROR);
    setState(HtmlEntry.REFERENCED_LIBRARIES, CacheState.ERROR);
    recordResolutionError();
  }

  /**
   * Record that an error was encountered while attempting to resolve the source associated with
   * this entry.
   */
  public void recordResolutionError() {
    setState(HtmlEntry.ELEMENT, CacheState.ERROR);
    setState(HtmlEntry.RESOLUTION_ERRORS, CacheState.ERROR);
    setState(HtmlEntry.HINTS, CacheState.ERROR);
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
    } else if (descriptor == RESOLUTION_ERRORS) {
      resolutionErrors = updatedValue(state, resolutionErrors, AnalysisError.NO_ERRORS);
      resolutionErrorsState = state;
    } else if (descriptor == HINTS) {
      hints = updatedValue(state, hints, AnalysisError.NO_ERRORS);
      hintsState = state;
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
    } else if (descriptor == RESOLUTION_ERRORS) {
      resolutionErrors = (AnalysisError[]) value;
      resolutionErrorsState = CacheState.VALID;
    } else if (descriptor == HINTS) {
      hints = (AnalysisError[]) value;
      hintsState = CacheState.VALID;
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
    resolutionErrors = other.resolutionErrors;
    resolutionErrorsState = other.resolutionErrorsState;
    elementState = other.elementState;
    element = other.element;
    hints = other.hints;
    hintsState = other.hintsState;
  }

  @Override
  protected void writeOn(StringBuilder builder) {
    builder.append("Html: ");
    super.writeOn(builder);
    builder.append("; parsedUnit = ");
    builder.append(parsedUnitState);
    builder.append("; resolutionErrors = ");
    builder.append(resolutionErrorsState);
    builder.append("; referencedLibraries = ");
    builder.append(referencedLibrariesState);
    builder.append("; element = ");
    builder.append(elementState);
  }
}
