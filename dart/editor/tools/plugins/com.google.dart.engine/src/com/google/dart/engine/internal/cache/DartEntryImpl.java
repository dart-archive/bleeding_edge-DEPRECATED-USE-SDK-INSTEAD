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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.internal.context.CacheState;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.engine.utilities.source.LineInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Instances of the class {@code DartEntryImpl} implement a {@link DartEntry}.
 * 
 * @coverage dart.engine
 */
public class DartEntryImpl extends SourceEntryImpl implements DartEntry {
  /**
   * Instances of the class {@code ResolutionState} represent the information produced by resolving
   * a compilation unit as part of a specific library.
   */
  private static class ResolutionState {
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
     * The state of the cached resolution errors.
     */
    private CacheState resolutionErrorsState = CacheState.INVALID;

    /**
     * The errors produced while resolving the compilation unit, or {@code null} if the errors are
     * not currently cached.
     */
    private AnalysisError[] resolutionErrors = AnalysisError.NO_ERRORS;

    /**
     * Initialize a newly created resolution state.
     */
    public ResolutionState() {
      super();
    }

    /**
     * Set this state to be exactly like the given state.
     * 
     * @param other the state to be copied
     */
    public void copyFrom(ResolutionState other) {
      resolvedUnitState = other.resolvedUnitState;
      resolvedUnit = other.resolvedUnit;
      resolutionErrorsState = other.resolutionErrorsState;
      resolutionErrors = other.resolutionErrors;
    }

    /**
     * Invalidate all of the resolution information associated with the compilation unit.
     */
    public void invalidateAllResolutionInformation() {
      resolvedUnitState = CacheState.INVALID;
      resolvedUnit = null;
      resolutionErrorsState = CacheState.INVALID;
      resolutionErrors = AnalysisError.NO_ERRORS;
    }
  }

  /**
   * The state of the cached source kind.
   */
  private CacheState sourceKindState = CacheState.INVALID;

  /**
   * The kind of this source.
   */
  private SourceKind sourceKind = SourceKind.UNKNOWN;

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
   * The state of the cached parse errors.
   */
  private CacheState parseErrorsState = CacheState.INVALID;

  /**
   * The errors produced while scanning and parsing the compilation unit, or {@code null} if the
   * errors are not currently cached.
   */
  private AnalysisError[] parseErrors = AnalysisError.NO_ERRORS;

  /**
   * The state of the cached list of included parts.
   */
  private CacheState includedPartsState = CacheState.INVALID;

  /**
   * The list of parts included in the library, or {@code null} if the list is not currently cached.
   * The list will be empty if the Dart file is a part rather than a library.
   */
  private Source[] includedParts = Source.EMPTY_ARRAY;

  /**
   * The source for the defining compilation unit of the library that contains this unit, or
   * {@code null} if this unit is contained in multiple libraries. (If this unit is the defining
   * compilation unit for it's library, then this will be the source for this unit.)
   */
  private Source librarySource;

  /**
   * The information known as a result of resolving this compilation unit as part of the library
   * that contains this unit, or {@code null} if this unit is contained in multiple libraries.
   */
  private ResolutionState resolutionState = new ResolutionState();

  /**
   * A table mapping the sources of the libraries containing this compilation unit to the
   * information known as a result of resolving this compilation unit as part of the library, or
   * {@code null} if this unit is contained in a single library.
   */
  private HashMap<Source, ResolutionState> resolutionMap;

  /**
   * The state of the cached library element.
   */
  private CacheState elementState = CacheState.INVALID;

  /**
   * The element representing the library, or {@code null} if the element is not currently cached.
   */
  private LibraryElement element;

  /**
   * The state of the cached public namespace.
   */
  private CacheState publicNamespaceState = CacheState.INVALID;

  /**
   * The public namespace of the library, or {@code null} if the namespace is not currently cached.
   */
  private Namespace publicNamespace;

  /**
   * The state of the cached client/ server flag.
   */
  private CacheState clientServerState = CacheState.INVALID;

  /**
   * The state of the cached launchable flag.
   */
  private CacheState launchableState = CacheState.INVALID;

  /**
   * An integer holding bit masks such as {@link #LAUNCHABLE} and {@link #CLIENT_CODE}.
   */
  private int bitmask = 0;

  /**
   * Mask indicating that this library is launchable: that the file has a main method.
   */
  private static final int LAUNCHABLE = 1 << 1;

  /**
   * Mask indicating that the library is client code: that the library depends on the html library.
   * If the library is not "client code", then it is referenced as "server code".
   */
  private static final int CLIENT_CODE = 1 << 2;

  /**
   * Initialize a newly created cache entry to be empty.
   */
  public DartEntryImpl() {
    super();
  }

  /**
   * Record the fact that we are about to parse the compilation unit by marking the results of
   * parsing as being in progress.
   */
  public void aboutToParse() {
    setState(LINE_INFO, CacheState.IN_PROCESS);
    parsedUnitState = CacheState.IN_PROCESS;
    parseErrorsState = CacheState.IN_PROCESS;
  }

  @Override
  public AnalysisError[] getAllErrors() {
    ArrayList<AnalysisError> errors = new ArrayList<AnalysisError>();
    for (AnalysisError error : parseErrors) {
      errors.add(error);
    }
    if (resolutionMap == null) {
      for (AnalysisError error : resolutionState.resolutionErrors) {
        errors.add(error);
      }
    } else {
      for (ResolutionState state : resolutionMap.values()) {
        for (AnalysisError error : state.resolutionErrors) {
          errors.add(error);
        }
      }
    }
    if (errors.size() == 0) {
      return AnalysisError.NO_ERRORS;
    }
    return errors.toArray(new AnalysisError[errors.size()]);
  }

  @Override
  public CompilationUnit getAnyParsedCompilationUnit() {
    if (parsedUnitState == CacheState.VALID) {
      return parsedUnit;
    }
    if (resolutionMap == null) {
      return resolutionState.resolvedUnit;
    } else {
      for (ResolutionState state : resolutionMap.values()) {
        if (state.resolvedUnitState == CacheState.VALID) {
          return state.resolvedUnit;
        }
      }
    }
    return null;
  }

  @Override
  public CompilationUnit getAnyResolvedCompilationUnit() {
    for (ResolutionState state : resolutionMap.values()) {
      if (state.resolvedUnitState == CacheState.VALID) {
        return state.resolvedUnit;
      }
    }
    return null;
  }

  @Override
  public SourceKind getKind() {
    return sourceKind;
  }

  @Override
  public CacheState getState(DataDescriptor<?> descriptor) {
    if (descriptor == ELEMENT) {
      return elementState;
    } else if (descriptor == INCLUDED_PARTS) {
      return includedPartsState;
    } else if (descriptor == IS_CLIENT) {
      return clientServerState;
    } else if (descriptor == IS_LAUNCHABLE) {
      return launchableState;
    } else if (descriptor == PARSE_ERRORS) {
      return parseErrorsState;
    } else if (descriptor == PARSED_UNIT) {
      return parsedUnitState;
    } else if (descriptor == PUBLIC_NAMESPACE) {
      return publicNamespaceState;
    } else if (descriptor == SOURCE_KIND) {
      return sourceKindState;
    } else {
      return super.getState(descriptor);
    }
  }

  @Override
  public CacheState getState(DataDescriptor<?> descriptor, Source librarySource) {
    if (descriptor == RESOLUTION_ERRORS) {
      if (resolutionMap == null) {
        return resolutionState.resolutionErrorsState;
      } else {
        ResolutionState state = resolutionMap.get(librarySource);
        if (state != null) {
          return state.resolutionErrorsState;
        }
      }
    } else if (descriptor == RESOLVED_UNIT) {
      if (resolutionMap == null) {
        return resolutionState.resolvedUnitState;
      } else {
        ResolutionState state = resolutionMap.get(librarySource);
        if (state != null) {
          return state.resolvedUnitState;
        }
      }
    } else {
      throw new IllegalArgumentException("Invalid descriptor: " + descriptor);
    }
    return CacheState.INVALID;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <E> E getValue(DataDescriptor<E> descriptor) {
    if (descriptor == ELEMENT) {
      return (E) element;
    } else if (descriptor == INCLUDED_PARTS) {
      return (E) includedParts;
    } else if (descriptor == IS_CLIENT) {
      return (E) ((bitmask & CLIENT_CODE) != 0 ? Boolean.TRUE : Boolean.FALSE);
    } else if (descriptor == IS_LAUNCHABLE) {
      return (E) ((bitmask & LAUNCHABLE) != 0 ? Boolean.TRUE : Boolean.FALSE);
    } else if (descriptor == PARSE_ERRORS) {
      return (E) parseErrors;
    } else if (descriptor == PARSED_UNIT) {
      return (E) parsedUnit;
    } else if (descriptor == PUBLIC_NAMESPACE) {
      return (E) publicNamespace;
    } else if (descriptor == SOURCE_KIND) {
      return (E) sourceKind;
    }
    return super.getValue(descriptor);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <E> E getValue(DataDescriptor<E> descriptor, Source librarySource) {
    if (descriptor == RESOLUTION_ERRORS) {
      if (resolutionMap == null) {
        if (librarySource.equals(this.librarySource)) {
          return (E) resolutionState.resolutionErrors;
        } else {
          return (E) AnalysisError.NO_ERRORS;
        }
      } else {
        ResolutionState state = resolutionMap.get(librarySource);
        if (state != null) {
          return (E) state.resolutionErrors;
        }
      }
    } else if (descriptor == RESOLVED_UNIT) {
      if (resolutionMap == null) {
        if (librarySource.equals(this.librarySource)) {
          return (E) resolutionState.resolvedUnit;
        } else {
          return null;
        }
      } else {
        ResolutionState state = resolutionMap.get(librarySource);
        if (state != null) {
          return (E) state.resolvedUnit;
        }
      }
    } else {
      throw new IllegalArgumentException("Invalid descriptor: " + descriptor);
    }
    return null;
  }

  @Override
  public DartEntryImpl getWritableCopy() {
    DartEntryImpl copy = new DartEntryImpl();
    copy.copyFrom(this);
    return copy;
  }

  /**
   * Invalidate all of the resolution information associated with the compilation unit.
   */
  public void invalidateAllResolutionInformation() {
    if (resolutionMap == null) {
      resolutionState.invalidateAllResolutionInformation();
    } else {
      for (ResolutionState state : resolutionMap.values()) {
        state.invalidateAllResolutionInformation();
      }
    }
    elementState = CacheState.INVALID;
    element = null;
    publicNamespaceState = CacheState.INVALID;
    publicNamespace = null;
    launchableState = CacheState.INVALID;
    clientServerState = CacheState.INVALID;
    bitmask = 0;
  }

  /**
   * Remove any resolution information associated with this compilation unit being part of the given
   * library, presumably because it is no longer part of the library.
   * 
   * @param librarySource the source of the defining compilation unit of the library that used to
   *          contain this part but no longer does
   */
  public void removeResolution(Source librarySource) {
    if (resolutionMap == null) {
      if (librarySource.equals(this.librarySource)) {
        resolutionState.invalidateAllResolutionInformation();
        this.librarySource = null;
      }
    } else {
      resolutionMap.remove(librarySource);
      if (resolutionMap.size() == 1) {
        //
        // We now have resolution state for a single library. Convert from the multi-library form to
        // the single library form.
        //
        Map.Entry<Source, ResolutionState> entry = resolutionMap.entrySet().iterator().next();
        this.librarySource = entry.getKey();
        resolutionState = entry.getValue();
        resolutionMap = null;
      }
    }
  }

  /**
   * Set the results of parsing the compilation unit at the given time to the given values.
   * 
   * @param modificationStamp the earliest time at which the source was last modified before the
   *          parsing was started
   * @param lineInfo the line information resulting from parsing the compilation unit
   * @param unit the AST structure resulting from parsing the compilation unit
   * @param errors the parse errors resulting from parsing the compilation unit
   */
  public void setParseResults(long modificationStamp, LineInfo lineInfo, CompilationUnit unit,
      AnalysisError[] errors) {
    if (getState(LINE_INFO) != CacheState.VALID) {
      setValue(LINE_INFO, lineInfo);
    }
    if (parsedUnitState != CacheState.VALID) {
      parsedUnit = unit;
    }
    if (parseErrorsState != CacheState.VALID) {
      parseErrors = errors == null ? AnalysisError.NO_ERRORS : errors;
    }
  }

  @Override
  public void setState(DataDescriptor<?> descriptor, CacheState state) {
    if (descriptor == ELEMENT) {
      element = updatedValue(state, element, null);
      elementState = state;
    } else if (descriptor == INCLUDED_PARTS) {
      includedParts = updatedValue(state, includedParts, Source.EMPTY_ARRAY);
      includedPartsState = state;
    } else if (descriptor == IS_CLIENT) {
      bitmask = updatedValue(state, bitmask, CLIENT_CODE);
      clientServerState = state;
    } else if (descriptor == IS_LAUNCHABLE) {
      bitmask = updatedValue(state, bitmask, LAUNCHABLE);
      launchableState = state;
    } else if (descriptor == PARSE_ERRORS) {
      parseErrors = updatedValue(state, parseErrors, AnalysisError.NO_ERRORS);
      parseErrorsState = state;
    } else if (descriptor == PARSED_UNIT) {
      parsedUnit = updatedValue(state, parsedUnit, null);
      parsedUnitState = state;
    } else if (descriptor == PUBLIC_NAMESPACE) {
      publicNamespace = updatedValue(state, publicNamespace, null);
      publicNamespaceState = state;
    } else if (descriptor == SOURCE_KIND) {
      sourceKind = updatedValue(state, sourceKind, SourceKind.UNKNOWN);
      sourceKindState = state;
    } else {
      super.setState(descriptor, state);
    }
  }

  /**
   * Set the state of the data represented by the given descriptor in the context of the given
   * library to the given state.
   * 
   * @param descriptor the descriptor representing the data whose state is to be set
   * @param librarySource the source of the defining compilation unit of the library that is the
   *          context for the data
   * @param state the new state of the data represented by the given descriptor
   */
  public void setState(DataDescriptor<?> descriptor, Source librarySource, CacheState state) {
    if (resolutionMap == null && !this.librarySource.equals(librarySource)) {
      //
      // We now have resolution state for a second library. Convert from the single library form to
      // the multi-library form.
      //
      resolutionMap = new HashMap<Source, ResolutionState>();
      resolutionMap.put(this.librarySource, resolutionState);
      this.librarySource = null;
      resolutionState = null;
    }
    if (descriptor == RESOLUTION_ERRORS) {
      if (resolutionMap == null) {
        resolutionState.resolutionErrors = updatedValue(
            state,
            resolutionState.resolutionErrors,
            AnalysisError.NO_ERRORS);
        resolutionState.resolutionErrorsState = state;
      } else {
        ResolutionState resolutionState = resolutionMap.get(librarySource);
        if (resolutionState == null) {
          resolutionState = new ResolutionState();
          resolutionMap.put(librarySource, resolutionState);
        }
        resolutionState.resolutionErrors = updatedValue(
            state,
            resolutionState.resolutionErrors,
            AnalysisError.NO_ERRORS);
        resolutionState.resolutionErrorsState = state;
      }
    } else if (descriptor == RESOLVED_UNIT) {
      if (resolutionMap == null) {
        resolutionState.resolvedUnit = updatedValue(state, resolutionState.resolvedUnit, null);
        resolutionState.resolvedUnitState = state;
      } else {
        ResolutionState resolutionState = resolutionMap.get(librarySource);
        if (resolutionState == null) {
          resolutionState = new ResolutionState();
          resolutionMap.put(librarySource, resolutionState);
        }
        resolutionState.resolvedUnit = updatedValue(state, resolutionState.resolvedUnit, null);
        resolutionState.resolvedUnitState = state;
      }
    } else {
      throw new IllegalArgumentException("Invalid descriptor: " + descriptor);
    }
  }

  @Override
  public <E> void setValue(DataDescriptor<E> descriptor, E value) {
    if (descriptor == ELEMENT) {
      element = (LibraryElement) value;
      elementState = CacheState.VALID;
    } else if (descriptor == INCLUDED_PARTS) {
      includedParts = value == null ? Source.EMPTY_ARRAY : (Source[]) value;
      includedPartsState = CacheState.VALID;
    } else if (descriptor == IS_CLIENT) {
      if (((Boolean) value).booleanValue()) {
        bitmask |= CLIENT_CODE;
      } else {
        bitmask &= ~CLIENT_CODE;
      }
      clientServerState = CacheState.VALID;
    } else if (descriptor == IS_LAUNCHABLE) {
      if (((Boolean) value).booleanValue()) {
        bitmask |= LAUNCHABLE;
      } else {
        bitmask &= ~LAUNCHABLE;
      }
      launchableState = CacheState.VALID;
    } else if (descriptor == PARSE_ERRORS) {
      parseErrors = value == null ? AnalysisError.NO_ERRORS : (AnalysisError[]) value;
      parseErrorsState = CacheState.VALID;
    } else if (descriptor == PARSED_UNIT) {
      parsedUnit = (CompilationUnit) value;
      parsedUnitState = CacheState.VALID;
    } else if (descriptor == PUBLIC_NAMESPACE) {
      publicNamespace = (Namespace) value;
      publicNamespaceState = CacheState.VALID;
    } else if (descriptor == SOURCE_KIND) {
      sourceKind = (SourceKind) value;
      sourceKindState = CacheState.VALID;
    } else {
      super.setValue(descriptor, value);
    }
  }

  /**
   * Set the value of the data represented by the given descriptor in the context of the given
   * library to the given value, and set the state of that data to {@link CacheState#VALID}.
   * 
   * @param descriptor the descriptor representing which data is to have its value set
   * @param librarySource the source of the defining compilation unit of the library that is the
   *          context for the data
   * @param value the new value of the data represented by the given descriptor and library
   */
  public <E> void setValue(DataDescriptor<E> descriptor, Source librarySource, E value) {
    if (resolutionMap == null) {
      if (this.librarySource == null) {
        //
        // This is the first library to have resolution state.
        //
        this.librarySource = librarySource;
      } else if (!librarySource.equals(this.librarySource)) {
        //
        // We now have resolution state for a second library. Convert from the single library form to
        // the multi-library form.
        //
        resolutionMap = new HashMap<Source, ResolutionState>();
        resolutionMap.put(this.librarySource, resolutionState);
        this.librarySource = null;
        resolutionState = null;
      }
    }
    if (descriptor == RESOLUTION_ERRORS) {
      if (resolutionMap == null) {
        resolutionState.resolutionErrors = value == null ? AnalysisError.NO_ERRORS
            : (AnalysisError[]) value;
        resolutionState.resolutionErrorsState = CacheState.VALID;
      } else {
        ResolutionState state = resolutionMap.get(librarySource);
        if (state != null) {
          state.resolutionErrors = value == null ? AnalysisError.NO_ERRORS
              : (AnalysisError[]) value;
          state.resolutionErrorsState = CacheState.VALID;
        }
      }
    } else if (descriptor == RESOLVED_UNIT) {
      if (resolutionMap == null) {
        resolutionState.resolvedUnit = (CompilationUnit) value;
        resolutionState.resolvedUnitState = CacheState.VALID;
      } else {
        ResolutionState state = resolutionMap.get(librarySource);
        if (state != null) {
          state.resolvedUnit = (CompilationUnit) value;
          state.resolvedUnitState = CacheState.VALID;
        }
      }
    }
  }

  @Override
  protected void copyFrom(SourceEntryImpl entry) {
    super.copyFrom(entry);
    DartEntryImpl other = (DartEntryImpl) entry;
    sourceKindState = other.sourceKindState;
    sourceKind = other.sourceKind;
    parsedUnitState = other.parsedUnitState;
    parsedUnit = other.parsedUnit;
    parseErrorsState = other.parseErrorsState;
    parseErrors = other.parseErrors;
    includedPartsState = other.includedPartsState;
    includedParts = other.includedParts;
    if (other.resolutionMap == null) {
      librarySource = other.librarySource;
      ResolutionState newState = new ResolutionState();
      newState.copyFrom(resolutionState);
      resolutionState = newState;
    } else {
      resolutionMap = new HashMap<Source, ResolutionState>();
      for (Map.Entry<Source, ResolutionState> mapEntry : other.resolutionMap.entrySet()) {
        ResolutionState newState = new ResolutionState();
        newState.copyFrom(mapEntry.getValue());
        resolutionMap.put(mapEntry.getKey(), newState);
      }
    }
    elementState = other.elementState;
    element = other.element;
    publicNamespaceState = other.publicNamespaceState;
    publicNamespace = other.publicNamespace;
    clientServerState = other.clientServerState;
    launchableState = other.launchableState;
    bitmask = other.bitmask;
  }

  /**
   * Given that one of the flags is being transitioned to the given state, return the value of the
   * flags that should be kept in the cache.
   * 
   * @param state the state to which the data is being transitioned
   * @param currentValue the value of the flags before the transition
   * @param bitMask the mask used to access the bit whose state is being set
   * @return the value of the data that should be kept in the cache
   */
  private int updatedValue(CacheState state, int currentValue, int bitMask) {
    if (state == CacheState.VALID) {
      throw new IllegalArgumentException("Use setValue() to set the state to VALID");
    } else if (state == CacheState.IN_PROCESS) {
      //
      // We can leave the current value in the cache for any 'get' methods to access.
      //
      return currentValue;
    }
    return currentValue &= ~bitMask;
  }
}
