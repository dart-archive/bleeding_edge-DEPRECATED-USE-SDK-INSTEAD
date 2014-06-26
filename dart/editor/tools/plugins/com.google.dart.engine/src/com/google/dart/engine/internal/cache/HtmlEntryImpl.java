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

import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.angular.AngularComponentElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.internal.element.angular.AngularApplication;
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
   * The state of the cached resolved HTML unit.
   */
  private CacheState resolvedUnitState = CacheState.INVALID;

  /**
   * The resolved HTML unit, or {@code null} if the resolved HTML unit is not currently cached.
   */
  private HtmlUnit resolvedUnit;

  /**
   * The state of the cached parse errors.
   */
  private CacheState parseErrorsState = CacheState.INVALID;

  /**
   * The errors produced while scanning and parsing the HTML, or {@code null} if the errors are not
   * currently cached.
   */
  private AnalysisError[] parseErrors = AnalysisError.NO_ERRORS;

  /**
   * The state of the cached resolution errors.
   */
  private CacheState resolutionErrorsState = CacheState.INVALID;

  /**
   * The errors produced while resolving the HTML, or {@code null} if the errors are not currently
   * cached.
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
   * The state of the {@link #angularApplication}.
   */
  private CacheState angularApplicationState = CacheState.VALID;

  /**
   * Information about the Angular Application this unit is used in.
   */
  private AngularApplication angularApplication;

  /**
   * The state of the {@link #angularEntry}.
   */
  private CacheState angularEntryState = CacheState.INVALID;

  /**
   * Information about the Angular Application this unit is entry point for.
   */
  private AngularApplication angularEntry = null;

  /**
   * The state of the {@link #angularComponent}.
   */
  private CacheState angularComponentState = CacheState.VALID;

  /**
   * Information about the {@link AngularComponentElement} this unit is used as template for.
   */
  private AngularComponentElement angularComponent = null;

  /**
   * The state of the Angular resolution errors.
   */
  private CacheState angularErrorsState = CacheState.INVALID;

  /**
   * The hints produced while performing Angular resolution, or an empty array if the error are not
   * currently cached.
   */
  private AnalysisError[] angularErrors = AnalysisError.NO_ERRORS;

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
   * The state of the Polymer elements.
   */
  private CacheState polymerBuildErrorsState = CacheState.INVALID;

  /**
   * The hints produced while performing Polymer HTML elements building, or an empty array if the
   * error are not currently cached.
   */
  private AnalysisError[] polymerBuildErrors = AnalysisError.NO_ERRORS;

  /**
   * The state of the Polymer resolution errors.
   */
  private CacheState polymerResolutionErrorsState = CacheState.INVALID;

  /**
   * The hints produced while performing Polymer resolution, or an empty array if the error are not
   * currently cached.
   */
  private AnalysisError[] polymerResolutionErrors = AnalysisError.NO_ERRORS;

  /**
   * Initialize a newly created cache entry to be empty.
   */
  public HtmlEntryImpl() {
    super();
  }

  /**
   * Flush any AST structures being maintained by this entry.
   */
  public void flushAstStructures() {
    if (parsedUnitState == CacheState.VALID) {
      parsedUnitState = CacheState.FLUSHED;
      parsedUnit = null;
    }
    if (resolvedUnitState == CacheState.VALID) {
      resolvedUnitState = CacheState.FLUSHED;
      resolvedUnit = null;
    }
    if (angularEntryState == CacheState.VALID) {
      angularEntryState = CacheState.FLUSHED;
    }
    if (angularErrorsState == CacheState.VALID) {
      angularErrorsState = CacheState.FLUSHED;
    }
  }

  @Override
  public AnalysisError[] getAllErrors() {
    ArrayList<AnalysisError> errors = new ArrayList<AnalysisError>();
    if (parseErrors != null) {
      for (AnalysisError error : parseErrors) {
        errors.add(error);
      }
    }
    if (resolutionErrors != null) {
      for (AnalysisError error : resolutionErrors) {
        errors.add(error);
      }
    }
    if (angularErrors != null) {
      for (AnalysisError error : angularErrors) {
        errors.add(error);
      }
    }
    if (hints != null) {
      for (AnalysisError error : hints) {
        errors.add(error);
      }
    }
    if (polymerBuildErrors != null) {
      for (AnalysisError error : polymerBuildErrors) {
        errors.add(error);
      }
    }
    if (polymerResolutionErrors != null) {
      for (AnalysisError error : polymerResolutionErrors) {
        errors.add(error);
      }
    }
    if (errors.size() == 0) {
      return AnalysisError.NO_ERRORS;
    }
    return errors.toArray(new AnalysisError[errors.size()]);
  }

  @Override
  public HtmlUnit getAnyParsedUnit() {
    if (parsedUnitState == CacheState.VALID) {
//      parsedUnitAccessed = true;
      return parsedUnit;
    }
    if (resolvedUnitState == CacheState.VALID) {
//      resovledUnitAccessed = true;
      return resolvedUnit;
    }
    return null;
  }

  @Override
  public SourceKind getKind() {
    return SourceKind.HTML;
  }

  @Override
  public CacheState getState(DataDescriptor<?> descriptor) {
    if (descriptor == ANGULAR_APPLICATION) {
      return angularApplicationState;
    } else if (descriptor == ANGULAR_COMPONENT) {
      return angularComponentState;
    } else if (descriptor == ANGULAR_ENTRY) {
      return angularEntryState;
    } else if (descriptor == ANGULAR_ERRORS) {
      return angularErrorsState;
    } else if (descriptor == ELEMENT) {
      return elementState;
    } else if (descriptor == PARSE_ERRORS) {
      return parseErrorsState;
    } else if (descriptor == PARSED_UNIT) {
      return parsedUnitState;
    } else if (descriptor == RESOLVED_UNIT) {
      return resolvedUnitState;
    } else if (descriptor == REFERENCED_LIBRARIES) {
      return referencedLibrariesState;
    } else if (descriptor == RESOLUTION_ERRORS) {
      return resolutionErrorsState;
    } else if (descriptor == HINTS) {
      return hintsState;
    } else if (descriptor == POLYMER_BUILD_ERRORS) {
      return polymerBuildErrorsState;
    } else if (descriptor == POLYMER_RESOLUTION_ERRORS) {
      return polymerResolutionErrorsState;
    }
    return super.getState(descriptor);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <E> E getValue(DataDescriptor<E> descriptor) {
    if (descriptor == ANGULAR_APPLICATION) {
      return (E) angularApplication;
    } else if (descriptor == ANGULAR_COMPONENT) {
      return (E) angularComponent;
    } else if (descriptor == ANGULAR_ENTRY) {
      return (E) angularEntry;
    } else if (descriptor == ANGULAR_ERRORS) {
      return (E) angularErrors;
    } else if (descriptor == ELEMENT) {
      return (E) element;
    } else if (descriptor == PARSE_ERRORS) {
      return (E) parseErrors;
    } else if (descriptor == PARSED_UNIT) {
      return (E) parsedUnit;
    } else if (descriptor == RESOLVED_UNIT) {
      return (E) resolvedUnit;
    } else if (descriptor == REFERENCED_LIBRARIES) {
      return (E) referencedLibraries;
    } else if (descriptor == RESOLUTION_ERRORS) {
      return (E) resolutionErrors;
    } else if (descriptor == HINTS) {
      return (E) hints;
    } else if (descriptor == POLYMER_BUILD_ERRORS) {
      return (E) polymerBuildErrors;
    } else if (descriptor == POLYMER_RESOLUTION_ERRORS) {
      return (E) polymerResolutionErrors;
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
  public void invalidateAllInformation() {
    super.invalidateAllInformation();

    parseErrors = AnalysisError.NO_ERRORS;
    parseErrorsState = CacheState.INVALID;
    parsedUnit = null;
    parsedUnitState = CacheState.INVALID;
    resolvedUnit = null;
    resolvedUnitState = CacheState.INVALID;

    invalidateAllResolutionInformation(true);
  }

  /**
   * Invalidate all of the resolution information associated with the HTML file.
   * 
   * @param invalidateUris true if the cached results of converting URIs to source files should also
   *          be invalidated.
   */
  public void invalidateAllResolutionInformation(boolean invalidateUris) {
    angularEntry = null;
    angularEntryState = CacheState.INVALID;

    angularErrors = AnalysisError.NO_ERRORS;
    angularErrorsState = CacheState.INVALID;

    polymerBuildErrors = AnalysisError.NO_ERRORS;
    polymerBuildErrorsState = CacheState.INVALID;

    polymerResolutionErrors = AnalysisError.NO_ERRORS;
    polymerResolutionErrorsState = CacheState.INVALID;

    element = null;
    elementState = CacheState.INVALID;

    resolutionErrors = AnalysisError.NO_ERRORS;
    resolutionErrorsState = CacheState.INVALID;

    hints = AnalysisError.NO_ERRORS;
    hintsState = CacheState.INVALID;

    if (invalidateUris) {
      referencedLibraries = Source.EMPTY_ARRAY;
      referencedLibrariesState = CacheState.INVALID;
    }
  }

  @Override
  public void recordContentError(AnalysisException exception) {
    super.recordContentError(exception);
    recordParseError(exception);
  }

  /**
   * Record that an error was encountered while attempting to parse the source associated with this
   * entry.
   * 
   * @param exception the exception that shows where the error occurred
   */
  public void recordParseError(AnalysisException exception) {
    // If the scanning and parsing of HTML are separated, the following line can be removed.
    recordScanError(exception);

    parseErrors = AnalysisError.NO_ERRORS;
    parseErrorsState = CacheState.ERROR;

    parsedUnit = null;
    parsedUnitState = CacheState.ERROR;

    referencedLibraries = Source.EMPTY_ARRAY;
    referencedLibrariesState = CacheState.ERROR;

    recordResolutionError(exception);
  }

  /**
   * Record that an error was encountered while attempting to resolve the source associated with
   * this entry.
   * 
   * @param exception the exception that shows where the error occurred
   */
  public void recordResolutionError(AnalysisException exception) {
    setException(exception);

    angularErrors = AnalysisError.NO_ERRORS;
    angularErrorsState = CacheState.ERROR;

    resolvedUnit = null;
    resolvedUnitState = CacheState.ERROR;

    element = null;
    elementState = CacheState.ERROR;

    resolutionErrors = AnalysisError.NO_ERRORS;
    resolutionErrorsState = CacheState.ERROR;

    hints = AnalysisError.NO_ERRORS;
    hintsState = CacheState.ERROR;

    polymerBuildErrors = AnalysisError.NO_ERRORS;
    polymerBuildErrorsState = CacheState.ERROR;

    polymerResolutionErrors = AnalysisError.NO_ERRORS;
    polymerResolutionErrorsState = CacheState.ERROR;
  }

  @Override
  public void setState(DataDescriptor<?> descriptor, CacheState state) {
    if (descriptor == ANGULAR_APPLICATION) {
      angularApplication = updatedValue(state, angularApplication, null);
      angularApplicationState = state;
    } else if (descriptor == ANGULAR_COMPONENT) {
      angularComponent = updatedValue(state, angularComponent, null);
      angularComponentState = state;
    } else if (descriptor == ANGULAR_ENTRY) {
      angularEntry = updatedValue(state, angularEntry, null);
      angularEntryState = state;
    } else if (descriptor == ANGULAR_ERRORS) {
      angularErrors = updatedValue(state, angularErrors, null);
      angularErrorsState = state;
    } else if (descriptor == ELEMENT) {
      element = updatedValue(state, element, null);
      elementState = state;
    } else if (descriptor == PARSE_ERRORS) {
      parseErrors = updatedValue(state, parseErrors, null);
      parseErrorsState = state;
    } else if (descriptor == PARSED_UNIT) {
      parsedUnit = updatedValue(state, parsedUnit, null);
      parsedUnitState = state;
    } else if (descriptor == RESOLVED_UNIT) {
      resolvedUnit = updatedValue(state, resolvedUnit, null);
      resolvedUnitState = state;
    } else if (descriptor == REFERENCED_LIBRARIES) {
      referencedLibraries = updatedValue(state, referencedLibraries, Source.EMPTY_ARRAY);
      referencedLibrariesState = state;
    } else if (descriptor == RESOLUTION_ERRORS) {
      resolutionErrors = updatedValue(state, resolutionErrors, AnalysisError.NO_ERRORS);
      resolutionErrorsState = state;
    } else if (descriptor == HINTS) {
      hints = updatedValue(state, hints, AnalysisError.NO_ERRORS);
      hintsState = state;
    } else if (descriptor == POLYMER_BUILD_ERRORS) {
      polymerBuildErrors = updatedValue(state, polymerBuildErrors, null);
      polymerBuildErrorsState = state;
    } else if (descriptor == POLYMER_RESOLUTION_ERRORS) {
      polymerResolutionErrors = updatedValue(state, polymerResolutionErrors, null);
      polymerResolutionErrorsState = state;
    } else {
      super.setState(descriptor, state);
    }
  }

  @Override
  public <E> void setValue(DataDescriptor<E> descriptor, E value) {
    if (descriptor == ANGULAR_APPLICATION) {
      angularApplication = (AngularApplication) value;
      angularApplicationState = CacheState.VALID;
    } else if (descriptor == ANGULAR_COMPONENT) {
      angularComponent = (AngularComponentElement) value;
      angularComponentState = CacheState.VALID;
    } else if (descriptor == ANGULAR_ENTRY) {
      angularEntry = (AngularApplication) value;
      angularEntryState = CacheState.VALID;
    } else if (descriptor == ANGULAR_ERRORS) {
      angularErrors = (AnalysisError[]) value;
      angularErrorsState = CacheState.VALID;
    } else if (descriptor == ELEMENT) {
      element = (HtmlElement) value;
      elementState = CacheState.VALID;
    } else if (descriptor == PARSE_ERRORS) {
      parseErrors = (AnalysisError[]) value;
      parseErrorsState = CacheState.VALID;
    } else if (descriptor == PARSED_UNIT) {
      parsedUnit = (HtmlUnit) value;
      parsedUnitState = CacheState.VALID;
    } else if (descriptor == RESOLVED_UNIT) {
      resolvedUnit = (HtmlUnit) value;
      resolvedUnitState = CacheState.VALID;
    } else if (descriptor == REFERENCED_LIBRARIES) {
      referencedLibraries = value == null ? Source.EMPTY_ARRAY : (Source[]) value;
      referencedLibrariesState = CacheState.VALID;
    } else if (descriptor == RESOLUTION_ERRORS) {
      resolutionErrors = (AnalysisError[]) value;
      resolutionErrorsState = CacheState.VALID;
    } else if (descriptor == HINTS) {
      hints = (AnalysisError[]) value;
      hintsState = CacheState.VALID;
    } else if (descriptor == POLYMER_BUILD_ERRORS) {
      polymerBuildErrors = (AnalysisError[]) value;
      polymerBuildErrorsState = CacheState.VALID;
    } else if (descriptor == POLYMER_RESOLUTION_ERRORS) {
      polymerResolutionErrors = (AnalysisError[]) value;
      polymerResolutionErrorsState = CacheState.VALID;
    } else {
      super.setValue(descriptor, value);
    }
  }

  @Override
  protected void copyFrom(SourceEntryImpl entry) {
    super.copyFrom(entry);
    HtmlEntryImpl other = (HtmlEntryImpl) entry;
    angularApplicationState = other.angularApplicationState;
    angularApplication = other.angularApplication;
    angularComponentState = other.angularComponentState;
    angularComponent = other.angularComponent;
    angularEntryState = other.angularEntryState;
    angularEntry = other.angularEntry;
    angularErrorsState = other.angularErrorsState;
    angularErrors = other.angularErrors;
    parseErrorsState = other.parseErrorsState;
    parseErrors = other.parseErrors;
    parsedUnitState = other.parsedUnitState;
    parsedUnit = other.parsedUnit;
    resolvedUnitState = other.resolvedUnitState;
    resolvedUnit = other.resolvedUnit;
    referencedLibrariesState = other.referencedLibrariesState;
    referencedLibraries = other.referencedLibraries;
    resolutionErrorsState = other.resolutionErrorsState;
    resolutionErrors = other.resolutionErrors;
    elementState = other.elementState;
    element = other.element;
    hintsState = other.hintsState;
    hints = other.hints;
    polymerBuildErrorsState = other.polymerBuildErrorsState;
    polymerBuildErrors = other.polymerBuildErrors;
    polymerResolutionErrorsState = other.polymerResolutionErrorsState;
    polymerResolutionErrors = other.polymerResolutionErrors;
  }

  @Override
  protected boolean hasErrorState() {
    return super.hasErrorState() || parsedUnitState == CacheState.ERROR
        || resolvedUnitState == CacheState.ERROR || parseErrorsState == CacheState.ERROR
        || resolutionErrorsState == CacheState.ERROR
        || referencedLibrariesState == CacheState.ERROR || elementState == CacheState.ERROR
        || angularErrorsState == CacheState.ERROR || hintsState == CacheState.ERROR
        || polymerBuildErrorsState == CacheState.ERROR
        || polymerResolutionErrorsState == CacheState.ERROR;
  }

  @Override
  protected void writeOn(StringBuilder builder) {
    builder.append("Html: ");
    super.writeOn(builder);
    builder.append("; parseErrors = ");
    builder.append(parseErrorsState);
    builder.append("; parsedUnit = ");
    builder.append(parsedUnitState);
    builder.append("; resolvedUnit = ");
    builder.append(resolvedUnitState);
    builder.append("; resolutionErrors = ");
    builder.append(resolutionErrorsState);
    builder.append("; referencedLibraries = ");
    builder.append(referencedLibrariesState);
    builder.append("; element = ");
    builder.append(elementState);
    builder.append("; angularApplication = ");
    builder.append(angularApplicationState);
    builder.append("; angularComponent = ");
    builder.append(angularComponentState);
    builder.append("; angularEntry = ");
    builder.append(angularEntryState);
    builder.append("; angularErrors = ");
    builder.append(angularErrorsState);
    builder.append("; polymerBuildErrors = ");
    builder.append(polymerBuildErrorsState);
    builder.append("; polymerResolutionErrors = ");
    builder.append(polymerResolutionErrorsState);
  }
}
