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
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.engine.utilities.ast.AstCloner;
import com.google.dart.engine.utilities.collection.BooleanArray;
import com.google.dart.engine.utilities.collection.ListUtilities;

import java.util.ArrayList;

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
     * The next resolution state or {@code null} if none.
     */
    private ResolutionState nextState;

    /**
     * The source for the defining compilation unit of the library that contains this unit. If this
     * unit is the defining compilation unit for it's library, then this will be the source for this
     * unit.
     */
    private Source librarySource;

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
     * The errors produced while resolving the compilation unit, or an empty array if the errors are
     * not currently cached.
     */
    private AnalysisError[] resolutionErrors = AnalysisError.NO_ERRORS;

    /**
     * The state of the cached verification errors.
     */
    private CacheState verificationErrorsState = CacheState.INVALID;

    /**
     * The errors produced while verifying the compilation unit, or an empty array if the errors are
     * not currently cached.
     */
    private AnalysisError[] verificationErrors = AnalysisError.NO_ERRORS;

    /**
     * The state of the cached hints.
     */
    private CacheState hintsState = CacheState.INVALID;

    /**
     * The hints produced while auditing the compilation unit, or an empty array if the hints are
     * not currently cached.
     */
    private AnalysisError[] hints = AnalysisError.NO_ERRORS;

    /**
     * Initialize a newly created resolution state.
     */
    public ResolutionState() {
      super();
    }

    /**
     * Set this state to be exactly like the given state, recursively copying the next state as
     * necessary.
     * 
     * @param other the state to be copied
     */
    public void copyFrom(ResolutionState other) {
      librarySource = other.librarySource;

      resolvedUnitState = other.resolvedUnitState;
      resolvedUnit = other.resolvedUnit;

      resolutionErrorsState = other.resolutionErrorsState;
      resolutionErrors = other.resolutionErrors;

      verificationErrorsState = other.verificationErrorsState;
      verificationErrors = other.verificationErrors;

      hintsState = other.hintsState;
      hints = other.hints;

      if (other.nextState != null) {
        nextState = new ResolutionState();
        nextState.copyFrom(other.nextState);
      }
    }

    /**
     * Flush any AST structures being maintained by this state.
     */
    public void flushAstStructures() {
      if (resolvedUnitState == CacheState.VALID) {
        resolvedUnitState = CacheState.FLUSHED;
        resolvedUnit = null;
      }
      if (nextState != null) {
        nextState.flushAstStructures();
      }
    }

    public boolean hasErrorState() {
      return resolvedUnitState == CacheState.ERROR || resolutionErrorsState == CacheState.ERROR
          || verificationErrorsState == CacheState.ERROR || hintsState == CacheState.ERROR
          || (nextState != null && nextState.hasErrorState());
    }

    /**
     * Invalidate all of the resolution information associated with the compilation unit.
     */
    public void invalidateAllResolutionInformation() {
      nextState = null;
      librarySource = null;

      resolvedUnitState = CacheState.INVALID;
      resolvedUnit = null;

      resolutionErrorsState = CacheState.INVALID;
      resolutionErrors = AnalysisError.NO_ERRORS;

      verificationErrorsState = CacheState.INVALID;
      verificationErrors = AnalysisError.NO_ERRORS;

      hintsState = CacheState.INVALID;
      hints = AnalysisError.NO_ERRORS;
    }

    /**
     * Record that an error occurred while attempting to scan or parse the entry represented by this
     * entry. This will set the state of all resolution-based information as being in error, but
     * will not change the state of any parse results.
     */
    public void recordResolutionError() {
      resolvedUnitState = CacheState.ERROR;
      resolvedUnit = null;

      resolutionErrorsState = CacheState.ERROR;
      resolutionErrors = AnalysisError.NO_ERRORS;

      verificationErrorsState = CacheState.ERROR;
      verificationErrors = AnalysisError.NO_ERRORS;

      hintsState = CacheState.ERROR;
      hints = AnalysisError.NO_ERRORS;

      if (nextState != null) {
        nextState.recordResolutionError();
      }
    }

    /**
     * Record that an in-process parse has stopped without recording results because the results
     * were invalidated before they could be recorded.
     */
    public void recordResolutionNotInProcess() {
      if (resolvedUnitState == CacheState.IN_PROCESS) {
        resolvedUnitState = CacheState.INVALID;
      }
      if (resolutionErrorsState == CacheState.IN_PROCESS) {
        resolutionErrorsState = CacheState.INVALID;
      }
      if (verificationErrorsState == CacheState.IN_PROCESS) {
        verificationErrorsState = CacheState.INVALID;
      }
      if (hintsState == CacheState.IN_PROCESS) {
        hintsState = CacheState.INVALID;
      }
      if (nextState != null) {
        nextState.recordResolutionNotInProcess();
      }
    }

    /**
     * Write a textual representation of this state to the given builder. The result will only be
     * used for debugging purposes.
     * 
     * @param builder the builder to which the text should be written
     */
    protected void writeOn(StringBuilder builder) {
      if (librarySource != null) {
        builder.append("; resolvedUnit = ");
        builder.append(resolvedUnitState);
        builder.append("; resolutionErrors = ");
        builder.append(resolutionErrorsState);
        builder.append("; verificationErrors = ");
        builder.append(verificationErrorsState);
        builder.append("; hints = ");
        builder.append(hintsState);
        if (nextState != null) {
          nextState.writeOn(builder);
        }
      }
    }
  }

  /**
   * The state of the cached token stream.
   */
  private CacheState tokenStreamState = CacheState.INVALID;

  /**
   * The head of the token stream, or {@code null} if the token stream is not currently cached.
   */
  private Token tokenStream;

  /**
   * The state of the cached scan errors.
   */
  private CacheState scanErrorsState = CacheState.INVALID;

  /**
   * The errors produced while scanning the compilation unit, or {@code null} if the errors are not
   * currently cached.
   */
  private AnalysisError[] scanErrors = AnalysisError.NO_ERRORS;

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
   * A flag indicating whether the parsed AST structure has been accessed since it was set. This is
   * used to determine whether the structure needs to be copied before it is resolved.
   */
  private boolean parsedUnitAccessed = false;

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
   * The errors produced while parsing the compilation unit, or {@code null} if the errors are not
   * currently cached.
   */
  private AnalysisError[] parseErrors = AnalysisError.NO_ERRORS;

  /**
   * The state of the cached list of imported libraries.
   */
  private CacheState importedLibrariesState = CacheState.INVALID;

  /**
   * The list of libraries imported by the library, or an empty array if the list is not currently
   * cached. The list will be empty if the Dart file is a part rather than a library.
   */
  private Source[] importedLibraries = Source.EMPTY_ARRAY;

  /**
   * The state of the cached list of exported libraries.
   */
  private CacheState exportedLibrariesState = CacheState.INVALID;

  /**
   * The list of libraries exported by the library, or an empty array if the list is not currently
   * cached. The list will be empty if the Dart file is a part rather than a library.
   */
  private Source[] exportedLibraries = Source.EMPTY_ARRAY;

  /**
   * The state of the cached list of included parts.
   */
  private CacheState includedPartsState = CacheState.INVALID;

  /**
   * The list of parts included in the library, or an empty array if the list is not currently
   * cached. The list will be empty if the Dart file is a part rather than a library.
   */
  private Source[] includedParts = Source.EMPTY_ARRAY;

  /**
   * The list of libraries that contain this compilation unit. The list will be empty if there are
   * no known libraries that contain this compilation unit.
   */
  private ArrayList<Source> containingLibraries = new ArrayList<Source>();

  /**
   * The information known as a result of resolving this compilation unit as part of the library
   * that contains this unit. This field will never be {@code null}.
   */
  private ResolutionState resolutionState = new ResolutionState();

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
   * The error produced while performing Angular resolution, or an empty array if there are no
   * errors if the error are not currently cached.
   */
  private AnalysisError[] angularErrors = AnalysisError.NO_ERRORS;

  /**
   * The index of the bit in the {@link #bitmask} indicating that this library is launchable: that
   * the file has a main method.
   */
  private static final int LAUNCHABLE_INDEX = 1;

  /**
   * The index of the bit in the {@link #bitmask} indicating that the library is client code: that
   * the library depends on the html library. If the library is not "client code", then it is
   * referred to as "server code".
   */
  private static final int CLIENT_CODE_INDEX = 2;

  /**
   * Initialize a newly created cache entry to be empty.
   */
  public DartEntryImpl() {
    super();
  }

  /**
   * Add the given library to the list of libraries that contain this part. This method should only
   * be invoked on entries that represent a part.
   * 
   * @param librarySource the source of the library to be added
   */
  public void addContainingLibrary(Source librarySource) {
    containingLibraries.add(librarySource);
  }

  /**
   * Flush any AST structures being maintained by this entry.
   */
  public void flushAstStructures() {
    if (tokenStreamState == CacheState.VALID) {
      tokenStreamState = CacheState.FLUSHED;
      tokenStream = null;
    }
    if (parsedUnitState == CacheState.VALID) {
      parsedUnitState = CacheState.FLUSHED;
      parsedUnitAccessed = false;
      parsedUnit = null;
    }
    resolutionState.flushAstStructures();
  }

  @Override
  public AnalysisError[] getAllErrors() {
    ArrayList<AnalysisError> errors = new ArrayList<AnalysisError>();
    ListUtilities.addAll(errors, scanErrors);
    ListUtilities.addAll(errors, parseErrors);
    ResolutionState state = resolutionState;
    while (state != null) {
      ListUtilities.addAll(errors, state.resolutionErrors);
      ListUtilities.addAll(errors, state.verificationErrors);
      ListUtilities.addAll(errors, state.hints);
      state = state.nextState;
    }
    ListUtilities.addAll(errors, angularErrors);
    if (errors.size() == 0) {
      return AnalysisError.NO_ERRORS;
    }
    return errors.toArray(new AnalysisError[errors.size()]);
  }

  @Override
  public CompilationUnit getAnyParsedCompilationUnit() {
    if (parsedUnitState == CacheState.VALID) {
      parsedUnitAccessed = true;
      return parsedUnit;
    }
    return getAnyResolvedCompilationUnit();
  }

  @Override
  public CompilationUnit getAnyResolvedCompilationUnit() {
    ResolutionState state = resolutionState;
    while (state != null) {
      if (state.resolvedUnitState == CacheState.VALID) {
        return state.resolvedUnit;
      }
      state = state.nextState;
    };
    return null;
  }

  @Override
  public SourceKind getKind() {
    return sourceKind;
  }

  /**
   * Answer an array of library sources containing the receiver's source.
   */
  public Source[] getLibrariesContaining() {
    ResolutionState state = resolutionState;
    ArrayList<Source> result = new ArrayList<Source>();
    while (state != null) {
      if (state.librarySource != null) {
        result.add(state.librarySource);
      }
      state = state.nextState;
    }
    return result.toArray(new Source[result.size()]);
  }

  /**
   * Return a compilation unit that has not been accessed by any other client and can therefore
   * safely be modified by the reconciler.
   * 
   * @return a compilation unit that can be modified by the reconciler
   */
  public CompilationUnit getResolvableCompilationUnit() {
    if (parsedUnitState == CacheState.VALID) {
      if (parsedUnitAccessed) {
        return (CompilationUnit) parsedUnit.accept(new AstCloner());
      }
      CompilationUnit unit = parsedUnit;
      parsedUnitState = CacheState.FLUSHED;
      parsedUnitAccessed = false;
      parsedUnit = null;
      return unit;
    }
    ResolutionState state = resolutionState;
    while (state != null) {
      if (state.resolvedUnitState == CacheState.VALID) {
        return (CompilationUnit) state.resolvedUnit.accept(new AstCloner());
      }
      state = state.nextState;
    };
    return null;
  }

  @Override
  public CacheState getState(DataDescriptor<?> descriptor) {
    if (descriptor == ELEMENT) {
      return elementState;
    } else if (descriptor == EXPORTED_LIBRARIES) {
      return exportedLibrariesState;
    } else if (descriptor == IMPORTED_LIBRARIES) {
      return importedLibrariesState;
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
    } else if (descriptor == SCAN_ERRORS) {
      return scanErrorsState;
    } else if (descriptor == SOURCE_KIND) {
      return sourceKindState;
    } else if (descriptor == TOKEN_STREAM) {
      return tokenStreamState;
    } else {
      return super.getState(descriptor);
    }
  }

  @Override
  public CacheState getStateInLibrary(DataDescriptor<?> descriptor, Source librarySource) {
    ResolutionState state = resolutionState;
    while (state != null) {
      if (librarySource.equals(state.librarySource)) {
        if (descriptor == RESOLUTION_ERRORS) {
          return state.resolutionErrorsState;
        } else if (descriptor == RESOLVED_UNIT) {
          return state.resolvedUnitState;
        } else if (descriptor == VERIFICATION_ERRORS) {
          return state.verificationErrorsState;
        } else if (descriptor == HINTS) {
          return state.hintsState;
        } else {
          throw new IllegalArgumentException("Invalid descriptor: " + descriptor);
        }
      }
      state = state.nextState;
    };
    if (descriptor == RESOLUTION_ERRORS || descriptor == RESOLVED_UNIT
        || descriptor == VERIFICATION_ERRORS || descriptor == HINTS) {
      return CacheState.INVALID;
    } else {
      throw new IllegalArgumentException("Invalid descriptor: " + descriptor);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <E> E getValue(DataDescriptor<E> descriptor) {
    if (descriptor == ANGULAR_ERRORS) {
      return (E) angularErrors;
    } else if (descriptor == CONTAINING_LIBRARIES) {
      return (E) containingLibraries.toArray(new Source[containingLibraries.size()]);
    } else if (descriptor == ELEMENT) {
      return (E) element;
    } else if (descriptor == EXPORTED_LIBRARIES) {
      return (E) exportedLibraries;
    } else if (descriptor == IMPORTED_LIBRARIES) {
      return (E) importedLibraries;
    } else if (descriptor == INCLUDED_PARTS) {
      return (E) includedParts;
    } else if (descriptor == IS_CLIENT) {
      return (E) (Boolean) BooleanArray.get(bitmask, CLIENT_CODE_INDEX);
    } else if (descriptor == IS_LAUNCHABLE) {
      return (E) (Boolean) BooleanArray.get(bitmask, LAUNCHABLE_INDEX);
    } else if (descriptor == PARSE_ERRORS) {
      return (E) parseErrors;
    } else if (descriptor == PARSED_UNIT) {
      parsedUnitAccessed = true;
      return (E) parsedUnit;
    } else if (descriptor == PUBLIC_NAMESPACE) {
      return (E) publicNamespace;
    } else if (descriptor == SCAN_ERRORS) {
      return (E) scanErrors;
    } else if (descriptor == SOURCE_KIND) {
      return (E) sourceKind;
    } else if (descriptor == TOKEN_STREAM) {
      return (E) tokenStream;
    }
    return super.getValue(descriptor);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <E> E getValueInLibrary(DataDescriptor<E> descriptor, Source librarySource) {
    ResolutionState state = resolutionState;
    while (state != null) {
      if (librarySource.equals(state.librarySource)) {
        if (descriptor == RESOLUTION_ERRORS) {
          return (E) state.resolutionErrors;
        } else if (descriptor == RESOLVED_UNIT) {
          return (E) state.resolvedUnit;
        } else if (descriptor == VERIFICATION_ERRORS) {
          return (E) state.verificationErrors;
        } else if (descriptor == HINTS) {
          return (E) state.hints;
        } else {
          throw new IllegalArgumentException("Invalid descriptor: " + descriptor);
        }
      }
      state = state.nextState;
    };
    if (descriptor == RESOLUTION_ERRORS || descriptor == VERIFICATION_ERRORS || descriptor == HINTS) {
      return (E) AnalysisError.NO_ERRORS;
    } else if (descriptor == RESOLVED_UNIT) {
      return null;
    } else {
      throw new IllegalArgumentException("Invalid descriptor: " + descriptor);
    }
  }

  @Override
  public DartEntryImpl getWritableCopy() {
    DartEntryImpl copy = new DartEntryImpl();
    copy.copyFrom(this);
    return copy;
  }

  @Override
  public boolean hasInvalidData(DataDescriptor<?> descriptor) {
    if (descriptor == ELEMENT) {
      return elementState == CacheState.INVALID;
    } else if (descriptor == EXPORTED_LIBRARIES) {
      return exportedLibrariesState == CacheState.INVALID;
    } else if (descriptor == IMPORTED_LIBRARIES) {
      return importedLibrariesState == CacheState.INVALID;
    } else if (descriptor == INCLUDED_PARTS) {
      return includedPartsState == CacheState.INVALID;
    } else if (descriptor == IS_CLIENT) {
      return clientServerState == CacheState.INVALID;
    } else if (descriptor == IS_LAUNCHABLE) {
      return launchableState == CacheState.INVALID;
    } else if (descriptor == PARSE_ERRORS) {
      return parseErrorsState == CacheState.INVALID;
    } else if (descriptor == PARSED_UNIT) {
      return parsedUnitState == CacheState.INVALID;
    } else if (descriptor == PUBLIC_NAMESPACE) {
      return publicNamespaceState == CacheState.INVALID;
    } else if (descriptor == SCAN_ERRORS) {
      return scanErrorsState == CacheState.INVALID;
    } else if (descriptor == SOURCE_KIND) {
      return sourceKindState == CacheState.INVALID;
    } else if (descriptor == TOKEN_STREAM) {
      return tokenStreamState == CacheState.INVALID;
    } else if (descriptor == RESOLUTION_ERRORS || descriptor == RESOLVED_UNIT
        || descriptor == VERIFICATION_ERRORS || descriptor == HINTS) {
      ResolutionState state = resolutionState;
      while (state != null) {
        if (descriptor == RESOLUTION_ERRORS) {
          return state.resolutionErrorsState == CacheState.INVALID;
        } else if (descriptor == RESOLVED_UNIT) {
          return state.resolvedUnitState == CacheState.INVALID;
        } else if (descriptor == VERIFICATION_ERRORS) {
          return state.verificationErrorsState == CacheState.INVALID;
        } else if (descriptor == HINTS) {
          return state.hintsState == CacheState.INVALID;
        }
      }
      return false;
    } else {
      return super.getState(descriptor) == CacheState.INVALID;
    }
  }

  @Override
  public void invalidateAllInformation() {
    super.invalidateAllInformation();

    scanErrors = AnalysisError.NO_ERRORS;
    scanErrorsState = CacheState.INVALID;

    tokenStream = null;
    tokenStreamState = CacheState.INVALID;

    sourceKind = SourceKind.UNKNOWN;
    sourceKindState = CacheState.INVALID;

    parseErrors = AnalysisError.NO_ERRORS;
    parseErrorsState = CacheState.INVALID;

    parsedUnit = null;
    parsedUnitAccessed = false;
    parsedUnitState = CacheState.INVALID;

    discardCachedResolutionInformation();
  }

  /**
   * Invalidate all of the resolution information associated with the compilation unit.
   */
  public void invalidateAllResolutionInformation() {
    if (parsedUnitState == CacheState.FLUSHED) {
      ResolutionState state = resolutionState;
      while (state != null) {
        if (state.resolvedUnitState == CacheState.VALID) {
          parsedUnit = state.resolvedUnit;
          parsedUnitAccessed = true;
          parsedUnitState = CacheState.VALID;
          break;
        }
        state = state.nextState;
      }
    }
    discardCachedResolutionInformation();
  }

  @Override
  public boolean isRefactoringSafe() {
    ResolutionState state = resolutionState;
    while (state != null) {
      CacheState resolvedState = state.resolvedUnitState;
      if (resolvedState != CacheState.VALID && resolvedState != CacheState.FLUSHED) {
        return false;
      }
      state = state.nextState;
    }
    return true;
  }

  @Override
  public void recordContentError() {
    super.recordContentError();
    recordScanError();
  }

  /**
   * Record that an error occurred while attempting to resolve the directives in the source
   * represented by this entry.
   */
  public void recordDependencyError() {
    exportedLibraries = Source.EMPTY_ARRAY;
    exportedLibrariesState = CacheState.ERROR;

    importedLibraries = Source.EMPTY_ARRAY;
    importedLibrariesState = CacheState.ERROR;

    includedParts = Source.EMPTY_ARRAY;
    includedPartsState = CacheState.ERROR;
  }

  /**
   * Record that the information related to resolving dependencies for the associated source is
   * about to be computed by the current thread.
   */
  public void recordDependencyInProcess() {
    if (exportedLibrariesState != CacheState.VALID) {
      exportedLibrariesState = CacheState.IN_PROCESS;
    }
    if (importedLibrariesState != CacheState.VALID) {
      importedLibrariesState = CacheState.IN_PROCESS;
    }
    if (includedPartsState != CacheState.VALID) {
      includedPartsState = CacheState.IN_PROCESS;
    }
  }

  /**
   * Record that an in-process dependency resolution has stopped without recording results because
   * the results were invalidated before they could be recorded.
   */
  public void recordDependencyNotInProcess() {
    if (exportedLibrariesState == CacheState.IN_PROCESS) {
      exportedLibrariesState = CacheState.INVALID;
    }
    if (importedLibrariesState == CacheState.IN_PROCESS) {
      importedLibrariesState = CacheState.INVALID;
    }
    if (includedPartsState == CacheState.IN_PROCESS) {
      includedPartsState = CacheState.INVALID;
    }
  }

  /**
   * Record that an error occurred while attempting to scan or parse the entry represented by this
   * entry. This will set the state of all information, including any resolution-based information,
   * as being in error.
   */
  public void recordParseError() {
    sourceKind = SourceKind.UNKNOWN;
    sourceKindState = CacheState.ERROR;

    parseErrors = AnalysisError.NO_ERRORS;
    parseErrorsState = CacheState.ERROR;

    parsedUnit = null;
    parsedUnitAccessed = false;
    parsedUnitState = CacheState.ERROR;

    recordDependencyError();
    recordResolutionError();
  }

  /**
   * Record that the parse-related information for the associated source is about to be computed by
   * the current thread.
   */
  public void recordParseInProcess() {
    if (sourceKindState != CacheState.VALID) {
      sourceKindState = CacheState.IN_PROCESS;
    }
    if (parseErrorsState != CacheState.VALID) {
      parseErrorsState = CacheState.IN_PROCESS;
    }
    if (parsedUnitState != CacheState.VALID) {
      parsedUnitState = CacheState.IN_PROCESS;
    }
  }

  /**
   * Record that an in-process parse has stopped without recording results because the results were
   * invalidated before they could be recorded.
   */
  public void recordParseNotInProcess() {
    if (getState(LINE_INFO) == CacheState.IN_PROCESS) {
      setState(LINE_INFO, CacheState.INVALID);
    }
    if (sourceKindState == CacheState.IN_PROCESS) {
      sourceKindState = CacheState.INVALID;
    }
    if (parseErrorsState == CacheState.IN_PROCESS) {
      parseErrorsState = CacheState.INVALID;
    }
    if (parsedUnitState == CacheState.IN_PROCESS) {
      parsedUnitState = CacheState.INVALID;
    }
  }

  /**
   * Record that an error occurred while attempting to scan or parse the entry represented by this
   * entry. This will set the state of all resolution-based information as being in error, but will
   * not change the state of any parse results.
   */
  public void recordResolutionError() {
    element = null;
    elementState = CacheState.ERROR;

    bitmask = 0;
    clientServerState = CacheState.ERROR;
    launchableState = CacheState.ERROR;

    publicNamespace = null;
    publicNamespaceState = CacheState.ERROR;

    resolutionState.recordResolutionError();
  }

  /**
   * Record that an in-process parse has stopped without recording results because the results were
   * invalidated before they could be recorded.
   */
  public void recordResolutionNotInProcess() {
    if (elementState == CacheState.IN_PROCESS) {
      elementState = CacheState.INVALID;
    }
    if (clientServerState == CacheState.IN_PROCESS) {
      clientServerState = CacheState.INVALID;
    }
    if (launchableState == CacheState.IN_PROCESS) {
      launchableState = CacheState.INVALID;
    }
    if (publicNamespaceState == CacheState.IN_PROCESS) {
      publicNamespaceState = CacheState.INVALID;
    }
    resolutionState.recordResolutionNotInProcess();
  }

  /**
   * Record that an error occurred while attempting to scan or parse the entry represented by this
   * entry. This will set the state of all information, including any resolution-based information,
   * as being in error.
   */
  public void recordScanError() {
    setState(LINE_INFO, CacheState.ERROR);

    scanErrors = AnalysisError.NO_ERRORS;
    scanErrorsState = CacheState.ERROR;

    tokenStream = null;
    tokenStreamState = CacheState.ERROR;

    recordParseError();
  }

  /**
   * Record that the scan-related information for the associated source is about to be computed by
   * the current thread.
   */
  public void recordScanInProcess() {
    if (getState(LINE_INFO) != CacheState.VALID) {
      setState(LINE_INFO, CacheState.IN_PROCESS);
    }
    if (scanErrorsState != CacheState.VALID) {
      scanErrorsState = CacheState.IN_PROCESS;
    }
    if (tokenStreamState != CacheState.VALID) {
      tokenStreamState = CacheState.IN_PROCESS;
    }
  }

  /**
   * Record that an in-process scan has stopped without recording results because the results were
   * invalidated before they could be recorded.
   */
  public void recordScanNotInProcess() {
    if (getState(LINE_INFO) == CacheState.IN_PROCESS) {
      setState(LINE_INFO, CacheState.INVALID);
    }
    if (scanErrorsState == CacheState.IN_PROCESS) {
      scanErrorsState = CacheState.INVALID;
    }
    if (tokenStreamState == CacheState.IN_PROCESS) {
      tokenStreamState = CacheState.INVALID;
    }
  }

  /**
   * Remove the given library from the list of libraries that contain this part. This method should
   * only be invoked on entries that represent a part.
   * 
   * @param librarySource the source of the library to be removed
   */
  public void removeContainingLibrary(Source librarySource) {
    containingLibraries.remove(librarySource);
  }

  /**
   * Remove any resolution information associated with this compilation unit being part of the given
   * library, presumably because it is no longer part of the library.
   * 
   * @param librarySource the source of the defining compilation unit of the library that used to
   *          contain this part but no longer does
   */
  public void removeResolution(Source librarySource) {
    if (librarySource != null) {
      if (librarySource.equals(resolutionState.librarySource)) {
        if (resolutionState.nextState == null) {
          resolutionState.invalidateAllResolutionInformation();
        } else {
          resolutionState = resolutionState.nextState;
        }
      } else {
        ResolutionState priorState = resolutionState;
        ResolutionState state = resolutionState.nextState;
        while (state != null) {
          if (librarySource.equals(state.librarySource)) {
            priorState.nextState = state.nextState;
            break;
          }
          priorState = state;
          state = state.nextState;
        }
      }
    }
  }

  /**
   * Set the list of libraries that contain this compilation unit to contain only the given source.
   * This method should only be invoked on entries that represent a library.
   * 
   * @param librarySource the source of the single library that the list should contain
   */
  public void setContainingLibrary(Source librarySource) {
    containingLibraries.clear();
    containingLibraries.add(librarySource);
  }

  @Override
  public void setState(DataDescriptor<?> descriptor, CacheState state) {
    if (descriptor == ELEMENT) {
      element = updatedValue(state, element, null);
      elementState = state;
    } else if (descriptor == EXPORTED_LIBRARIES) {
      exportedLibraries = updatedValue(state, exportedLibraries, Source.EMPTY_ARRAY);
      exportedLibrariesState = state;
    } else if (descriptor == IMPORTED_LIBRARIES) {
      importedLibraries = updatedValue(state, importedLibraries, Source.EMPTY_ARRAY);
      importedLibrariesState = state;
    } else if (descriptor == INCLUDED_PARTS) {
      includedParts = updatedValue(state, includedParts, Source.EMPTY_ARRAY);
      includedPartsState = state;
    } else if (descriptor == IS_CLIENT) {
      bitmask = updatedValueOfFlag(state, bitmask, CLIENT_CODE_INDEX);
      clientServerState = state;
    } else if (descriptor == IS_LAUNCHABLE) {
      bitmask = updatedValueOfFlag(state, bitmask, LAUNCHABLE_INDEX);
      launchableState = state;
    } else if (descriptor == PARSE_ERRORS) {
      parseErrors = updatedValue(state, parseErrors, AnalysisError.NO_ERRORS);
      parseErrorsState = state;
    } else if (descriptor == PARSED_UNIT) {
      CompilationUnit newUnit = updatedValue(state, parsedUnit, null);
      if (newUnit != parsedUnit) {
        parsedUnitAccessed = false;
      }
      parsedUnit = newUnit;
      parsedUnitState = state;
    } else if (descriptor == PUBLIC_NAMESPACE) {
      publicNamespace = updatedValue(state, publicNamespace, null);
      publicNamespaceState = state;
    } else if (descriptor == SCAN_ERRORS) {
      scanErrors = updatedValue(state, scanErrors, AnalysisError.NO_ERRORS);
      scanErrorsState = state;
    } else if (descriptor == SOURCE_KIND) {
      sourceKind = updatedValue(state, sourceKind, SourceKind.UNKNOWN);
      sourceKindState = state;
    } else if (descriptor == TOKEN_STREAM) {
      tokenStream = updatedValue(state, tokenStream, null);
      tokenStreamState = state;
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
   * @param cacheState the new state of the data represented by the given descriptor
   */
  public void setStateInLibrary(DataDescriptor<?> descriptor, Source librarySource,
      CacheState cacheState) {
    ResolutionState state = getOrCreateResolutionState(librarySource);
    if (descriptor == RESOLUTION_ERRORS) {
      state.resolutionErrors = updatedValue(
          cacheState,
          state.resolutionErrors,
          AnalysisError.NO_ERRORS);
      state.resolutionErrorsState = cacheState;
    } else if (descriptor == RESOLVED_UNIT) {
      state.resolvedUnit = updatedValue(cacheState, state.resolvedUnit, null);
      state.resolvedUnitState = cacheState;
    } else if (descriptor == VERIFICATION_ERRORS) {
      state.verificationErrors = updatedValue(
          cacheState,
          state.verificationErrors,
          AnalysisError.NO_ERRORS);
      state.verificationErrorsState = cacheState;
    } else if (descriptor == HINTS) {
      state.hints = updatedValue(cacheState, state.hints, AnalysisError.NO_ERRORS);
      state.hintsState = cacheState;
    } else {
      throw new IllegalArgumentException("Invalid descriptor: " + descriptor);
    }
  }

  @Override
  public <E> void setValue(DataDescriptor<E> descriptor, E value) {
    if (descriptor == ANGULAR_ERRORS) {
      angularErrors = value == null ? AnalysisError.NO_ERRORS : (AnalysisError[]) value;
    } else if (descriptor == ELEMENT) {
      element = (LibraryElement) value;
      elementState = CacheState.VALID;
    } else if (descriptor == EXPORTED_LIBRARIES) {
      exportedLibraries = value == null ? Source.EMPTY_ARRAY : (Source[]) value;
      exportedLibrariesState = CacheState.VALID;
    } else if (descriptor == IMPORTED_LIBRARIES) {
      importedLibraries = value == null ? Source.EMPTY_ARRAY : (Source[]) value;
      importedLibrariesState = CacheState.VALID;
    } else if (descriptor == INCLUDED_PARTS) {
      includedParts = value == null ? Source.EMPTY_ARRAY : (Source[]) value;
      includedPartsState = CacheState.VALID;
    } else if (descriptor == IS_CLIENT) {
      bitmask = BooleanArray.set(bitmask, CLIENT_CODE_INDEX, ((Boolean) value).booleanValue());
      clientServerState = CacheState.VALID;
    } else if (descriptor == IS_LAUNCHABLE) {
      bitmask = BooleanArray.set(bitmask, LAUNCHABLE_INDEX, ((Boolean) value).booleanValue());
      launchableState = CacheState.VALID;
    } else if (descriptor == PARSE_ERRORS) {
      parseErrors = value == null ? AnalysisError.NO_ERRORS : (AnalysisError[]) value;
      parseErrorsState = CacheState.VALID;
    } else if (descriptor == PARSED_UNIT) {
      parsedUnit = (CompilationUnit) value;
      parsedUnitAccessed = false;
      parsedUnitState = CacheState.VALID;
    } else if (descriptor == PUBLIC_NAMESPACE) {
      publicNamespace = (Namespace) value;
      publicNamespaceState = CacheState.VALID;
    } else if (descriptor == SCAN_ERRORS) {
      scanErrors = value == null ? AnalysisError.NO_ERRORS : (AnalysisError[]) value;
      scanErrorsState = CacheState.VALID;
    } else if (descriptor == SOURCE_KIND) {
      sourceKind = (SourceKind) value;
      sourceKindState = CacheState.VALID;
    } else if (descriptor == TOKEN_STREAM) {
      tokenStream = (Token) value;
      tokenStreamState = CacheState.VALID;
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
  public <E> void setValueInLibrary(DataDescriptor<E> descriptor, Source librarySource, E value) {
    ResolutionState state = getOrCreateResolutionState(librarySource);
    if (descriptor == RESOLUTION_ERRORS) {
      state.resolutionErrors = value == null ? AnalysisError.NO_ERRORS : (AnalysisError[]) value;
      state.resolutionErrorsState = CacheState.VALID;
    } else if (descriptor == RESOLVED_UNIT) {
      state.resolvedUnit = (CompilationUnit) value;
      state.resolvedUnitState = CacheState.VALID;
    } else if (descriptor == VERIFICATION_ERRORS) {
      state.verificationErrors = value == null ? AnalysisError.NO_ERRORS : (AnalysisError[]) value;
      state.verificationErrorsState = CacheState.VALID;
    } else if (descriptor == HINTS) {
      state.hints = value == null ? AnalysisError.NO_ERRORS : (AnalysisError[]) value;
      state.hintsState = CacheState.VALID;
    }
  }

  @Override
  protected void copyFrom(SourceEntryImpl entry) {
    super.copyFrom(entry);
    DartEntryImpl other = (DartEntryImpl) entry;
    scanErrorsState = other.scanErrorsState;
    scanErrors = other.scanErrors;
    tokenStreamState = other.tokenStreamState;
    tokenStream = other.tokenStream;
    sourceKindState = other.sourceKindState;
    sourceKind = other.sourceKind;
    parsedUnitState = other.parsedUnitState;
    parsedUnit = other.parsedUnit;
    parsedUnitAccessed = other.parsedUnitAccessed;
    parseErrorsState = other.parseErrorsState;
    parseErrors = other.parseErrors;
    includedPartsState = other.includedPartsState;
    includedParts = other.includedParts;
    exportedLibrariesState = other.exportedLibrariesState;
    exportedLibraries = other.exportedLibraries;
    importedLibrariesState = other.importedLibrariesState;
    importedLibraries = other.importedLibraries;
    containingLibraries = new ArrayList<Source>(other.containingLibraries);
    resolutionState.copyFrom(other.resolutionState);
    elementState = other.elementState;
    element = other.element;
    publicNamespaceState = other.publicNamespaceState;
    publicNamespace = other.publicNamespace;
    clientServerState = other.clientServerState;
    launchableState = other.launchableState;
    bitmask = other.bitmask;
    angularErrors = other.angularErrors;
  }

  @Override
  protected boolean hasErrorState() {
    return super.hasErrorState() || scanErrorsState == CacheState.ERROR
        || tokenStreamState == CacheState.ERROR || sourceKindState == CacheState.ERROR
        || parsedUnitState == CacheState.ERROR || parseErrorsState == CacheState.ERROR
        || importedLibrariesState == CacheState.ERROR || exportedLibrariesState == CacheState.ERROR
        || includedPartsState == CacheState.ERROR || elementState == CacheState.ERROR
        || publicNamespaceState == CacheState.ERROR || clientServerState == CacheState.ERROR
        || launchableState == CacheState.ERROR || resolutionState.hasErrorState();
  }

  @Override
  protected void writeOn(StringBuilder builder) {
    builder.append("Dart: ");
    super.writeOn(builder);
    builder.append("; tokenStream = ");
    builder.append(tokenStreamState);
    builder.append("; scanErrors = ");
    builder.append(scanErrorsState);
    builder.append("; sourceKind = ");
    builder.append(sourceKindState);
    builder.append("; parsedUnit = ");
    builder.append(parsedUnitState);
    builder.append(" (");
    builder.append(parsedUnitAccessed ? "T" : "F");
    builder.append("); parseErrors = ");
    builder.append(parseErrorsState);
    builder.append("; exportedLibraries = ");
    builder.append(exportedLibrariesState);
    builder.append("; importedLibraries = ");
    builder.append(importedLibrariesState);
    builder.append("; includedParts = ");
    builder.append(includedPartsState);
    builder.append("; element = ");
    builder.append(elementState);
    builder.append("; publicNamespace = ");
    builder.append(publicNamespaceState);
    builder.append("; clientServer = ");
    builder.append(clientServerState);
    builder.append("; launchable = ");
    builder.append(launchableState);
    builder.append("; angularElements = ");
    resolutionState.writeOn(builder);
  }

  /**
   * Invalidate all of the resolution information associated with the compilation unit.
   */
  private void discardCachedResolutionInformation() {
    element = null;
    elementState = CacheState.INVALID;

    includedParts = Source.EMPTY_ARRAY;
    includedPartsState = CacheState.INVALID;

    exportedLibraries = Source.EMPTY_ARRAY;
    exportedLibrariesState = CacheState.INVALID;

    importedLibraries = Source.EMPTY_ARRAY;
    importedLibrariesState = CacheState.INVALID;

    bitmask = 0;
    clientServerState = CacheState.INVALID;
    launchableState = CacheState.INVALID;

    publicNamespace = null;
    publicNamespaceState = CacheState.INVALID;

    resolutionState.invalidateAllResolutionInformation();
  }

  /**
   * Return a resolution state for the specified library, creating one as necessary.
   * 
   * @param librarySource the library source (not {@code null})
   * @return the resolution state (not {@code null})
   */
  private ResolutionState getOrCreateResolutionState(Source librarySource) {
    ResolutionState state = resolutionState;
    if (state.librarySource == null) {
      state.librarySource = librarySource;
      return state;
    }
    while (!state.librarySource.equals(librarySource)) {
      if (state.nextState == null) {
        ResolutionState newState = new ResolutionState();
        newState.librarySource = librarySource;
        state.nextState = newState;
        return newState;
      }
      state = state.nextState;
    }
    return state;
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
  private int updatedValueOfFlag(CacheState state, int currentValue, int bitIndex) {
    if (state == CacheState.VALID) {
      throw new IllegalArgumentException("Use setValue() to set the state to VALID");
    } else if (state == CacheState.IN_PROCESS) {
      //
      // We can leave the current value in the cache for any 'get' methods to access.
      //
      return currentValue;
    }
    return BooleanArray.set(currentValue, bitIndex, false);
  }
}
