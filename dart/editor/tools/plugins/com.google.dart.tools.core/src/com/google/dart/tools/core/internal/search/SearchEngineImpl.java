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
package com.google.dart.tools.core.internal.search;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.indexer.exceptions.IndexTemporarilyNonOperational;
import com.google.dart.indexer.locations.Location;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.NotYetImplementedException;
import com.google.dart.tools.core.indexer.DartIndexer;
import com.google.dart.tools.core.indexer.DartIndexerResult;
import com.google.dart.tools.core.internal.indexer.contributor.DartContributor;
import com.google.dart.tools.core.internal.indexer.contributor.ElementsByCategoryContributor;
import com.google.dart.tools.core.internal.indexer.contributor.FieldAccessContributor;
import com.google.dart.tools.core.internal.indexer.contributor.MethodInvocationContributor;
import com.google.dart.tools.core.internal.indexer.contributor.TypeReferencesContributor;
import com.google.dart.tools.core.internal.indexer.location.DartElementLocation;
import com.google.dart.tools.core.internal.indexer.location.FieldLocation;
import com.google.dart.tools.core.internal.indexer.location.ReferenceKind;
import com.google.dart.tools.core.internal.indexer.location.SyntheticLocation;
import com.google.dart.tools.core.internal.model.CompilationUnitImpl;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.internal.search.listener.FilteredSearchListener;
import com.google.dart.tools.core.internal.search.listener.GatheringSearchListener;
import com.google.dart.tools.core.internal.search.listener.WrappedSearchListener;
import com.google.dart.tools.core.internal.workingcopy.DefaultWorkingCopyOwner;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.CompilationUnitElement;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartFunctionTypeAlias;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.search.MatchKind;
import com.google.dart.tools.core.search.MatchQuality;
import com.google.dart.tools.core.search.SearchEngine;
import com.google.dart.tools.core.search.SearchException;
import com.google.dart.tools.core.search.SearchFilter;
import com.google.dart.tools.core.search.SearchListener;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.core.search.SearchPattern;
import com.google.dart.tools.core.search.SearchScope;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Instances of the class <code>SearchEngineImpl</code> defines methods to perform various search
 * operations.
 */
public final class SearchEngineImpl implements SearchEngine {
  private interface SearchHelper {
    /**
     * Return the index contributor used to find matches within working copies.
     * 
     * @return the index contributor used to find matches within working copies
     */
    public DartContributor getContributor();

    /**
     * Return an array containing the target locations that form relationships that constitute a
     * match.
     * 
     * @return the target locations that form relationships that constitute a match
     * @throws DartModelException if the valid targets could not be computed
     */
    public Location[] getValidTargets() throws DartModelException;

    /**
     * Return the result of looking in the index for search results.
     * 
     * @return the result of looking in the index for search results
     * @throws IndexTemporarilyNonOperational if the index could not be searched at this time
     */
    public DartIndexerResult performIndexSearch() throws IndexTemporarilyNonOperational;
  }

  /**
   * An array containing the working copies that take precedence over their original compilation
   * units.
   */
  private CompilationUnit[] workingCopies;

  /**
   * The working copy owner used to identify the working copies that take precedence over their
   * original compilation units, or <code>null</code> if the owner of the working copies is not
   * specified.
   */
  private WorkingCopyOwner workingCopyOwner;

  /**
   * Initialize a newly created search engine.
   */
  public SearchEngineImpl() {
    super();
  }

  /**
   * Initialize a newly created search engine to find matches in the given working copies rather
   * than in the saved versions of those compilation units.
   * 
   * @param workingCopies the working copies that take precedence over their original compilation
   *          units
   */
  public SearchEngineImpl(CompilationUnit[] workingCopies) {
    this.workingCopies = workingCopies;
  }

  /**
   * Initialize a newly created search engine to find matches in working copies with the given
   * working copy owner rather than in the saved versions of those compilation units.
   * 
   * @param workingCopyOwner the working copy owner used to identify the working copies that take
   *          precedence over their original compilation units
   */
  public SearchEngineImpl(WorkingCopyOwner workingCopyOwner) {
    this.workingCopyOwner = workingCopyOwner;
  }

  @Override
  public List<SearchMatch> searchConstructorDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, IProgressMonitor monitor) throws SearchException {
    DartCore.notYetImplemented();
    throw new NotYetImplementedException();
  }

  @Override
  public void searchConstructorDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, SearchListener listener, IProgressMonitor monitor)
      throws SearchException {
    SearchListener typeListener = new WrappedSearchListener(applyFilter(filter, listener)) {
      @Override
      public void matchFound(SearchMatch match) {
        MatchQuality quality = match.getQuality();
        DartElement element = match.getElement();
        if (element instanceof Type) {
          try {
            for (Method method : ((Type) element).getMethods()) {
              if (method.isConstructor()) {
                propagateMatch(new SearchMatch(quality, method, method.getNameRange()));
              }
            }
          } catch (DartModelException exception) {
            DartCore.logError("Could not get methods defined for type " + element.getElementName(),
                exception);
          }
        }
      }
    };
    searchTypeDeclarations(scope, pattern, typeListener, monitor);
  }

  @Override
  public List<SearchMatch> searchImplementors(Type type, SearchScope scope, SearchFilter filter,
      IProgressMonitor monitor) throws SearchException {
    DartCore.notYetImplemented();
    throw new NotYetImplementedException();
  }

  @Override
  public void searchImplementors(Type type, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    DartCore.notYetImplemented();
  }

  @Override
  public List<SearchMatch> searchReferences(DartFunction function, SearchScope scope,
      SearchFilter filter, IProgressMonitor monitor) throws SearchException {
    DartCore.notYetImplemented();
    throw new NotYetImplementedException();
  }

  @Override
  public void searchReferences(final DartFunction function, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    SearchHelper helper = new SearchHelper() {
      @Override
      public DartContributor getContributor() {
        return new MethodInvocationContributor();
      }

      @Override
      public Location[] getValidTargets() {
        DartCore.notYetImplemented();
        return null;
      }

      @Override
      public DartIndexerResult performIndexSearch() throws IndexTemporarilyNonOperational {
        return DartIndexer.getReferences(function);
      }
    };
    performSearch(helper, scope, null, applyFilter(filter, listener), monitor);
  }

  @Override
  public List<SearchMatch> searchReferences(DartFunctionTypeAlias alias, SearchScope scope,
      SearchFilter filter, IProgressMonitor monitor) throws SearchException {
    DartCore.notYetImplemented();
    throw new NotYetImplementedException();
  }

  @Override
  public void searchReferences(final DartFunctionTypeAlias alias, SearchScope scope,
      SearchFilter filter, SearchListener listener, IProgressMonitor monitor)
      throws SearchException {
    SearchHelper helper = new SearchHelper() {
      @Override
      public DartContributor getContributor() {
        return new TypeReferencesContributor();
      }

      @Override
      public Location[] getValidTargets() {
        DartCore.notYetImplemented();
        return null;
      }

      @Override
      public DartIndexerResult performIndexSearch() throws IndexTemporarilyNonOperational {
        return DartIndexer.getReferences(alias);
      }
    };
    performSearch(helper, scope, null, applyFilter(filter, listener), monitor);
  }

  @Override
  public List<SearchMatch> searchReferences(Field field, SearchScope scope, SearchFilter filter,
      IProgressMonitor monitor) throws SearchException {
    DartCore.notYetImplemented();
    throw new NotYetImplementedException();
  }

  @Override
  public void searchReferences(final Field field, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    SearchHelper helper = new SearchHelper() {
      @Override
      public DartContributor getContributor() {
        return new FieldAccessContributor();
      }

      @Override
      public Location[] getValidTargets() throws DartModelException {
        return new Location[] {new FieldLocation(field, field.getNameRange())};
      }

      @Override
      public DartIndexerResult performIndexSearch() throws IndexTemporarilyNonOperational {
        return DartIndexer.getReferences(field);
      }
    };
    performSearch(helper, scope, null, applyFilter(filter, listener), monitor);
  };

  public void searchReferences(Method method, SearchListener listener, IProgressMonitor monitor) {
    DartCore.notYetImplemented();
  }

  @Override
  public List<SearchMatch> searchReferences(Method method, SearchScope scope, SearchFilter filter,
      IProgressMonitor monitor) throws SearchException {
    DartCore.notYetImplemented();
    throw new NotYetImplementedException();
  };

  @Override
  public void searchReferences(final Method method, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    SearchHelper helper = new SearchHelper() {
      @Override
      public DartContributor getContributor() {
        return new MethodInvocationContributor();
      }

      @Override
      public Location[] getValidTargets() {
        DartCore.notYetImplemented();
        return null;
      }

      @Override
      public DartIndexerResult performIndexSearch() throws IndexTemporarilyNonOperational {
        return DartIndexer.getReferences(method);
      }
    };
    performSearch(helper, scope, null, applyFilter(filter, listener), monitor);
  };

  @Override
  public List<SearchMatch> searchReferences(Type type, SearchScope scope, SearchFilter filter,
      IProgressMonitor monitor) throws SearchException {
    DartCore.notYetImplemented();
    throw new NotYetImplementedException();
  }

  @Override
  public void searchReferences(final Type type, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    SearchHelper helper = new SearchHelper() {
      @Override
      public DartContributor getContributor() {
        return new TypeReferencesContributor();
      }

      @Override
      public Location[] getValidTargets() {
        DartCore.notYetImplemented();
        return null;
      }

      @Override
      public DartIndexerResult performIndexSearch() throws IndexTemporarilyNonOperational {
        return DartIndexer.getReferences(type);
      }
    };
    performSearch(helper, scope, null, applyFilter(filter, listener), monitor);
  };

  @Override
  public List<SearchMatch> searchSubtypes(Type type, SearchScope scope, SearchFilter filter,
      IProgressMonitor monitor) throws SearchException {
    DartCore.notYetImplemented();
    throw new NotYetImplementedException();
  };

  @Override
  public void searchSubtypes(Type type, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    DartCore.notYetImplemented();
    throw new NotYetImplementedException();
  }

  @Override
  public List<SearchMatch> searchSupertypes(Type type, SearchScope scope, SearchFilter filter,
      IProgressMonitor monitor) throws SearchException {
    DartCore.notYetImplemented();
    throw new NotYetImplementedException();
  };

  @Override
  public void searchSupertypes(Type type, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    DartCore.notYetImplemented();
    throw new NotYetImplementedException();
  }

  @Override
  public List<SearchMatch> searchTypeDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, IProgressMonitor monitor) throws SearchException {
    GatheringSearchListener listener = new GatheringSearchListener();
    searchTypeDeclarations(scope, pattern, filter, listener, monitor);
    return listener.getMatches();
  };

  @Override
  public void searchTypeDeclarations(SearchScope scope, SearchPattern pattern, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    SearchHelper helper = new SearchHelper() {
      @Override
      public DartContributor getContributor() {
        return new ElementsByCategoryContributor();
      }

      @Override
      public Location[] getValidTargets() {
        return new Location[] {
            SyntheticLocation.ALL_CLASSES, SyntheticLocation.ALL_FUNCTION_TYPE_ALIASES,
            SyntheticLocation.ALL_INTERFACES};
      }

      @Override
      public DartIndexerResult performIndexSearch() throws IndexTemporarilyNonOperational {
        return DartIndexer.getAllTypes();
      }
    };
    performSearch(helper, scope, pattern, applyFilter(filter, listener), monitor);
  };

  @Override
  public void searchTypeDeclarations(SearchScope scope, SearchPattern pattern,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    searchTypeDeclarations(scope, pattern, null, listener, monitor);
  }

  /**
   * Apply the given filter to the given listener.
   * 
   * @param filter the filter to be used before passing matches on to the listener, or
   *          <code>null</code> if all matches should be passed on
   * @param listener the listener that will only be given matches that pass the filter
   * @return a search listener that will pass to the given listener any matches that pass the given
   *         filter
   */
  private SearchListener applyFilter(SearchFilter filter, SearchListener listener) {
    if (filter == null) {
      return listener;
    }
    return new FilteredSearchListener(filter, listener);
  }

  /**
   * Return the list of working copies used by this search engine.
   * 
   * @return the list of working copies used by this search engine
   */
  private CompilationUnit[] getWorkingCopies() {
    CompilationUnit[] copies;
    if (workingCopies != null) {
      if (workingCopyOwner == null) {
        copies = DartModelManager.getInstance().getWorkingCopies(
            DefaultWorkingCopyOwner.getInstance(), false);
        if (copies == null) {
          copies = workingCopies;
        } else {
          HashMap<IPath, CompilationUnit> pathToCUs = new HashMap<IPath, CompilationUnit>();
          for (int i = 0, length = copies.length; i < length; i++) {
            CompilationUnit unit = copies[i];
            pathToCUs.put(unit.getPath(), unit);
          }
          for (int i = 0, length = workingCopies.length; i < length; i++) {
            CompilationUnit unit = workingCopies[i];
            pathToCUs.put(unit.getPath(), unit);
          }
          int length = pathToCUs.size();
          copies = new CompilationUnit[length];
          pathToCUs.values().toArray(copies);
        }
      } else {
        copies = workingCopies;
      }
    } else if (workingCopyOwner != null) {
      copies = DartModelManager.getInstance().getWorkingCopies(workingCopyOwner, true);
    } else {
      copies = DartModelManager.getInstance().getWorkingCopies(
          DefaultWorkingCopyOwner.getInstance(), false);
    }
    if (copies == null) {
      return new CompilationUnit[0];
    }
    // filter out primary working copies that are saved
    CompilationUnit[] result = null;
    int length = copies.length;
    int index = 0;
    for (int i = 0; i < length; i++) {
      CompilationUnitImpl copy = (CompilationUnitImpl) copies[i];
      try {
        if (!copy.isPrimary() || copy.hasUnsavedChanges() || copy.hasResourceChanged()) {
          if (result == null) {
            result = new CompilationUnit[length];
          }
          result[index++] = copy;
        }
      } catch (DartModelException exception) {
        // copy doesn't exist: ignore
      }
    }
    if (index != length && result != null) {
      CompilationUnit[] trimmed = new CompilationUnit[index];
      System.arraycopy(result, 0, trimmed, 0, index);
      return trimmed;
    }
    return result;
  }

  /**
   * Return <code>true</code> if the given element is contained in a compilation unit that is
   * equivalent to one of the compilation units in the given array of working copies.
   * 
   * @param element the element being tested
   * @param currentWorkingCopies the working copies being tested against
   * @return <code>true</code> if the given element is contained in a superseded compilation unit
   */
  private boolean isSuperseded(DartElement element, CompilationUnit[] currentWorkingCopies) {
    if (element instanceof CompilationUnitElement) {
      CompilationUnit unit = ((CompilationUnitElement) element).getCompilationUnit();
      if (unit.isWorkingCopy()) {
        return false;
      }
      String unitHandle = unit.getHandleIdentifier();
      for (CompilationUnit workingCopy : currentWorkingCopies) {
        if (workingCopy.getPrimary().getHandleIdentifier().equals(unitHandle)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Map the given location reference kind into the corresponding match kind.
   * 
   * @param referenceKind the reference kind being mapped
   * @return the corresponding match kind
   */
  private MatchKind map(ReferenceKind referenceKind) {
    if (referenceKind == null) {
      return MatchKind.NOT_A_REFERENCE;
    }
    switch (referenceKind) {
      case FIELD_READ:
        return MatchKind.FIELD_READ;
      case FIELD_WRITE:
        return MatchKind.FIELD_WRITE;
      case FUNCTION_EXECUTION:
        return MatchKind.FUNCTION_EXECUTION;
      case METHOD_INVOCATION:
        return MatchKind.METHOD_INVOCATION;
    }
    return MatchKind.NOT_A_REFERENCE;
  }

  /**
   * Use the given helper object to search for all of the results that are defined in the given
   * scope, and match the given pattern.
   * 
   * @param helper an object used to encode the information that is specific to each individual
   *          search operation
   * @param scope the scope containing the results to be returned
   * @param pattern the pattern used to determine which results are to be returned, or
   *          <code>null</code> if all results are to be returned
   * @param listener the listener that will be notified when matches are found
   * @param monitor the progress monitor to use for reporting progress to the user. It is the
   *          caller's responsibility to call done() on the given monitor. Accepts <code>null</code>
   *          , indicating that no progress should be reported and that the operation cannot be
   *          canceled.
   * @throws SearchException if the results could not be computed
   */
  private void performSearch(SearchHelper helper, SearchScope scope, SearchPattern pattern,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    CompilationUnit[] currentWorkingCopies = getWorkingCopies();
    // TODO(brianwilkerson) Figure out how best to report progress.
    SubMonitor progress = SubMonitor.convert(monitor, "Searching...",
        Math.max(100, (currentWorkingCopies.length + 1) * 2));
    //
    // Search the index for matches.
    //
    if (progress.isCanceled()) {
      throw new OperationCanceledException();
    }
    DartIndexerResult result;

    try {
      result = helper.performIndexSearch();
    } catch (IndexTemporarilyNonOperational ex) {
      throw new SearchException(ex);
    }

    List<Location> locations = toList(result.getResult());
    progress.worked(1);
    //
    // Then search the working copies for more possible matches.
    //
    try {
      SearchLayerUpdater layerUpdater = new SearchLayerUpdater(locations, helper.getValidTargets());
      DartContributor contributor = helper.getContributor();
      for (CompilationUnit workingCopy : currentWorkingCopies) {
        if (progress.isCanceled()) {
          throw new OperationCanceledException();
        }
        contributor.initialize(workingCopy, layerUpdater);
        try {
          DartUnit ast = DartCompilerUtilities.parseUnit(workingCopy);
          ast.accept(contributor);
        } catch (DartModelException exception) {
          DartCore.logError(
              "Could not parse " + workingCopy.getResource().getLocation().toString(), exception);
        }
        progress.worked(1);
      }
    } catch (DartModelException exception) {
      DartCore.logInformation("Could not search working copies for matches", exception);
    }
    //
    // Filter the matches and report them to the listener.
    //
    progress.setWorkRemaining(locations.size());
    for (Location location : locations) {
      if (progress.isCanceled()) {
        throw new OperationCanceledException();
      }
      if (location instanceof DartElementLocation) {
        DartElementLocation dartLocation = (DartElementLocation) location;
        DartElement element = DartIndexer.unpackElementOrNull(location);
        if (element != null && scope.encloses(element)
            && !isSuperseded(element, currentWorkingCopies)) {
          MatchQuality quality = MatchQuality.EXACT;
          if (pattern != null) {
            quality = pattern.matches(element);
          }
          if (quality != null) {
            listener.matchFound(new SearchMatch(quality, map(dartLocation.getReferenceKind()),
                element, dartLocation.getSourceRange()));
          }
        }
      }
      progress.worked(1);
    }
  }

  /**
   * Return a list containing all of the locations in the given array.
   * 
   * @param locations the locations to be included in the list
   * @return a list containing all of the locations in the given array
   */
  private List<Location> toList(Location[] locations) {
    List<Location> result = new ArrayList<Location>(locations.length + 20);
    for (Location location : locations) {
      result.add(location);
    }
    return result;
  }
}
