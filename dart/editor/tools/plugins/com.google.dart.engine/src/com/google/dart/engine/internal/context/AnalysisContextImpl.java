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
package com.google.dart.engine.internal.context;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.AnnotatedNode;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.Comment;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.constant.DeclaredVariables;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisContextStatistics;
import com.google.dart.engine.context.AnalysisDelta;
import com.google.dart.engine.context.AnalysisDelta.AnalysisLevel;
import com.google.dart.engine.context.AnalysisErrorInfo;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.context.AnalysisOptions;
import com.google.dart.engine.context.AnalysisResult;
import com.google.dart.engine.context.ChangeNotice;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.context.ObsoleteSourceAnalysisException;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.angular.AngularComponentElement;
import com.google.dart.engine.element.angular.AngularElement;
import com.google.dart.engine.element.angular.AngularHasTemplateElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.internal.cache.AnalysisCache;
import com.google.dart.engine.internal.cache.CachePartition;
import com.google.dart.engine.internal.cache.CacheRetentionPolicy;
import com.google.dart.engine.internal.cache.CacheState;
import com.google.dart.engine.internal.cache.DartEntry;
import com.google.dart.engine.internal.cache.DartEntryImpl;
import com.google.dart.engine.internal.cache.DataDescriptor;
import com.google.dart.engine.internal.cache.HtmlEntry;
import com.google.dart.engine.internal.cache.HtmlEntryImpl;
import com.google.dart.engine.internal.cache.RetentionPriority;
import com.google.dart.engine.internal.cache.SourceEntry;
import com.google.dart.engine.internal.cache.SourceEntryImpl;
import com.google.dart.engine.internal.cache.UniversalCachePartition;
import com.google.dart.engine.internal.element.ElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.angular.AngularApplication;
import com.google.dart.engine.internal.resolver.Library;
import com.google.dart.engine.internal.resolver.LibraryResolver;
import com.google.dart.engine.internal.resolver.LibraryResolver2;
import com.google.dart.engine.internal.resolver.ResolvableLibrary;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.internal.resolver.TypeProviderImpl;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.internal.scope.NamespaceBuilder;
import com.google.dart.engine.internal.task.AnalysisTask;
import com.google.dart.engine.internal.task.AnalysisTaskVisitor;
import com.google.dart.engine.internal.task.BuildDartElementModelTask;
import com.google.dart.engine.internal.task.GenerateDartErrorsTask;
import com.google.dart.engine.internal.task.GenerateDartHintsTask;
import com.google.dart.engine.internal.task.GetContentTask;
import com.google.dart.engine.internal.task.IncrementalAnalysisTask;
import com.google.dart.engine.internal.task.ParseDartTask;
import com.google.dart.engine.internal.task.ParseHtmlTask;
import com.google.dart.engine.internal.task.PolymerBuildHtmlTask;
import com.google.dart.engine.internal.task.PolymerResolveHtmlTask;
import com.google.dart.engine.internal.task.ResolveAngularComponentTemplateTask;
import com.google.dart.engine.internal.task.ResolveAngularEntryHtmlTask;
import com.google.dart.engine.internal.task.ResolveDartLibraryCycleTask;
import com.google.dart.engine.internal.task.ResolveDartLibraryTask;
import com.google.dart.engine.internal.task.ResolveDartUnitTask;
import com.google.dart.engine.internal.task.ResolveHtmlTask;
import com.google.dart.engine.internal.task.ScanDartTask;
import com.google.dart.engine.internal.task.WaitForAsyncTask;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.ContentCache;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.Source.ContentReceiver;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.engine.utilities.collection.DirectedGraph;
import com.google.dart.engine.utilities.collection.ListUtilities;
import com.google.dart.engine.utilities.collection.MapIterator;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.engine.utilities.source.LineInfo;
import com.google.dart.engine.utilities.translation.DartOmit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Instances of the class {@code AnalysisContextImpl} implement an {@link AnalysisContext analysis
 * context}.
 * 
 * @coverage dart.engine
 */
public class AnalysisContextImpl implements InternalAnalysisContext {
  /**
   * Instances of the class {@code AnalysisTaskResultRecorder} are used by an analysis context to
   * record the results of a task.
   */
  private class AnalysisTaskResultRecorder implements AnalysisTaskVisitor<SourceEntry> {
    @Override
    public DartEntry visitBuildDartElementModelTask(BuildDartElementModelTask task)
        throws AnalysisException {
      return recordBuildDartElementModelTask(task);
    }

    @Override
    public DartEntry visitGenerateDartErrorsTask(GenerateDartErrorsTask task)
        throws AnalysisException {
      return recordGenerateDartErrorsTask(task);
    }

    @Override
    public DartEntry visitGenerateDartHintsTask(GenerateDartHintsTask task)
        throws AnalysisException {
      return recordGenerateDartHintsTask(task);
    }

    @Override
    public SourceEntry visitGetContentTask(GetContentTask task) throws AnalysisException {
      return recordGetContentsTask(task);
    }

    @Override
    public DartEntry visitIncrementalAnalysisTask(IncrementalAnalysisTask task)
        throws AnalysisException {
      return recordIncrementalAnalysisTaskResults(task);
    }

    @Override
    public DartEntry visitParseDartTask(ParseDartTask task) throws AnalysisException {
      return recordParseDartTaskResults(task);
    }

    @Override
    public HtmlEntry visitParseHtmlTask(ParseHtmlTask task) throws AnalysisException {
      return recordParseHtmlTaskResults(task);
    }

    @Override
    public HtmlEntry visitPolymerBuildHtmlTask(PolymerBuildHtmlTask task) throws AnalysisException {
      return recordPolymerBuildHtmlTaskResults(task);
    }

    @Override
    public HtmlEntry visitPolymerResolveHtmlTask(PolymerResolveHtmlTask task)
        throws AnalysisException {
      return recordPolymerResolveHtmlTaskResults(task);
    }

    @Override
    public HtmlEntry visitResolveAngularComponentTemplateTask(
        ResolveAngularComponentTemplateTask task) throws AnalysisException {
      return recordResolveAngularComponentTemplateTaskResults(task);
    }

    @Override
    public HtmlEntry visitResolveAngularEntryHtmlTask(ResolveAngularEntryHtmlTask task)
        throws AnalysisException {
      return recordResolveAngularEntryHtmlTaskResults(task);
    }

    @Override
    public DartEntry visitResolveDartLibraryCycleTask(ResolveDartLibraryCycleTask task)
        throws AnalysisException {
      return recordResolveDartLibraryCycleTaskResults(task);
    }

    @Override
    public DartEntry visitResolveDartLibraryTask(ResolveDartLibraryTask task)
        throws AnalysisException {
      return recordResolveDartLibraryTaskResults(task);
    }

    @Override
    public DartEntry visitResolveDartUnitTask(ResolveDartUnitTask task) throws AnalysisException {
      return recordResolveDartUnitTaskResults(task);
    }

    @Override
    public HtmlEntry visitResolveHtmlTask(ResolveHtmlTask task) throws AnalysisException {
      return recordResolveHtmlTaskResults(task);
    }

    @Override
    public DartEntry visitScanDartTask(ScanDartTask task) throws AnalysisException {
      return recordScanDartTaskResults(task);
    }
  }

  private class ContextRetentionPolicy implements CacheRetentionPolicy {
    @Override
    public RetentionPriority getAstPriority(Source source, SourceEntry sourceEntry) {
      int priorityCount = priorityOrder.length;
      for (int i = 0; i < priorityCount; i++) {
        if (source.equals(priorityOrder[i])) {
          return RetentionPriority.HIGH;
        }
      }
      if (neededForResolution != null && neededForResolution.contains(source)) {
        return RetentionPriority.HIGH;
      }
      if (sourceEntry instanceof DartEntry) {
        DartEntry dartEntry = (DartEntry) sourceEntry;
        if (astIsNeeded(dartEntry)) {
          return RetentionPriority.MEDIUM;
        }
      }
      return RetentionPriority.LOW;
    }

    private boolean astIsNeeded(DartEntry dartEntry) {
      return dartEntry.hasInvalidData(DartEntry.HINTS)
          || dartEntry.hasInvalidData(DartEntry.VERIFICATION_ERRORS)
          || dartEntry.hasInvalidData(DartEntry.RESOLUTION_ERRORS);
    }
  }

  /**
   * Instances of the class {@code CycleBuilder} are used to construct a list of the libraries that
   * must be resolved together in order to resolve any one of the libraries.
   */
  private class CycleBuilder {
    /**
     * Instances of the class {@code LibraryPair} hold a library and a list of the (source, entry)
     * pairs for compilation units in the library.
     */
    private/*static*/class LibraryPair {
      /**
       * The library containing the compilation units.
       */
      public ResolvableLibrary library;

      /**
       * The (source, entry) pairs representing the compilation units in the library.
       */
      public ArrayList<SourceEntryPair> entryPairs;

      /**
       * Initialize a newly created pair.
       * 
       * @param library the library containing the compilation units
       * @param entryPairs the (source, entry) pairs representing the compilation units in the
       *          library
       */
      public LibraryPair(ResolvableLibrary library, ArrayList<SourceEntryPair> entryPairs) {
        this.library = library;
        this.entryPairs = entryPairs;
      }
    }

    /**
     * Instances of the class {@code SourceEntryPair} hold a source and the cache entry associated
     * with that source. They are used to reduce the number of times an entry must be looked up in
     * the {@link cache}.
     */
    private/*static*/class SourceEntryPair {
      /**
       * The source associated with the entry.
       */
      public Source source;

      /**
       * The entry associated with the source.
       */
      public DartEntry entry;

      /**
       * Initialize a newly created pair.
       * 
       * @param source the source associated with the entry
       * @param entry the entry associated with the source
       */
      public SourceEntryPair(Source source, DartEntry entry) {
        this.source = source;
        this.entry = entry;
      }
    }

    /**
     * A table mapping the sources of the defining compilation units of libraries to the
     * representation of the library that has the information needed to resolve the library.
     */
    private HashMap<Source, ResolvableLibrary> libraryMap = new HashMap<Source, ResolvableLibrary>();

    /**
     * The dependency graph used to compute the libraries in the cycle.
     */
    // TODO(brianwilkerson) Explore the possibility of maintaining a single dependency graph for the
    // whole context. Instead of building the graph, this class would merely determine whether the
    // graph was complete enough to find the cycle containing a given library.
    private DirectedGraph<ResolvableLibrary> dependencyGraph;

    /**
     * A list containing the libraries that are ready to be resolved.
     */
    private List<ResolvableLibrary> librariesInCycle;

    /**
     * The analysis task that needs to be performed before the cycle of libraries can be resolved,
     * or {@code null} if the libraries are ready to be resolved.
     */
    private TaskData taskData;

    /**
     * Initialize a newly created cycle builder.
     */
    public CycleBuilder() {
      super();
    }

    /**
     * Compute a list of the libraries that need to be resolved together in order to resolve the
     * given library.
     * 
     * @param librarySource the source of the library to be resolved
     * @throws AnalysisException if the core library cannot be found
     */
    public void computeCycleContaining(Source librarySource) throws AnalysisException {
      //
      // Create the object representing the library being resolved.
      //
      ResolvableLibrary targetLibrary = createLibrary(librarySource);
      //
      // Compute the set of libraries that need to be resolved together.
      //
      dependencyGraph = new DirectedGraph<ResolvableLibrary>();
      computeLibraryDependencies(targetLibrary);
      if (taskData != null) {
        return;
      }
      librariesInCycle = dependencyGraph.findCycleContaining(targetLibrary);
      //
      // Ensure that all of the data needed to resolve them has been computed.
      //
      ensureImportsAndExports();
      if (taskData != null) {
        // At least one imported library needs to be resolved before the target library.
        AnalysisTask task = taskData.getTask();
        if (task instanceof ResolveDartLibraryTask) {
          workManager.addFirst(
              ((ResolveDartLibraryTask) task).getLibrarySource(),
              SourcePriority.LIBRARY);
        }
        return;
      }
      computePartsInCycle(librarySource);
      if (taskData != null) {
        // At least one part needs to be parsed.
        return;
      }
      // All of the AST's necessary to perform a resolution of the library cycle have been
      // gathered, so it is no longer necessary to retain them in the cache.
      neededForResolution = null;
    }

    /**
     * Return a list containing the libraries that are ready to be resolved (assuming that
     * {@link #getTaskData()} returns {@code null}).
     * 
     * @return the libraries that are ready to be resolved
     */
    public List<ResolvableLibrary> getLibrariesInCycle() {
      return librariesInCycle;
    }

    /**
     * Return a representation of an analysis task that needs to be performed before the cycle of
     * libraries can be resolved, or {@code null} if the libraries are ready to be resolved.
     * 
     * @return the analysis task that needs to be performed before the cycle of libraries can be
     *         resolved
     */
    public TaskData getTaskData() {
      return taskData;
    }

    /**
     * Recursively traverse the libraries reachable from the given library, creating instances of
     * the class {@link Library} to represent them, and record the references in the library
     * objects.
     * 
     * @param library the library to be processed to find libraries that have not yet been traversed
     * @throws AnalysisException if some portion of the library graph could not be traversed
     */
    private void computeLibraryDependencies(ResolvableLibrary library) {
      Source librarySource = library.getLibrarySource();
      DartEntry dartEntry = getReadableDartEntry(librarySource);
      Source[] importedSources = getSources(librarySource, dartEntry, DartEntry.IMPORTED_LIBRARIES);
      if (taskData != null) {
        return;
      }
      Source[] exportedSources = getSources(librarySource, dartEntry, DartEntry.EXPORTED_LIBRARIES);
      if (taskData != null) {
        return;
      }
      computeLibraryDependenciesFromDirectives(library, importedSources, exportedSources);
    }

    /**
     * Recursively traverse the libraries reachable from the given library, creating instances of
     * the class {@link Library} to represent them, and record the references in the library
     * objects.
     * 
     * @param library the library to be processed to find libraries that have not yet been traversed
     * @param importedSources an array containing the sources that are imported into the given
     *          library
     * @param exportedSources an array containing the sources that are exported from the given
     *          library
     */
    private void computeLibraryDependenciesFromDirectives(ResolvableLibrary library,
        Source[] importedSources, Source[] exportedSources) {
      int importCount = importedSources.length;
      if (importCount > 0) {
        ArrayList<ResolvableLibrary> importedLibraries = new ArrayList<ResolvableLibrary>();
        boolean explicitlyImportsCore = false;
        for (int i = 0; i < importCount; i++) {
          Source importedSource = importedSources[i];
          if (importedSource.equals(coreLibrarySource)) {
            explicitlyImportsCore = true;
          }
          ResolvableLibrary importedLibrary = libraryMap.get(importedSource);
          if (importedLibrary == null) {
            importedLibrary = createLibraryOrNull(importedSource);
            if (importedLibrary != null) {
              computeLibraryDependencies(importedLibrary);
              if (taskData != null) {
                return;
              }
            }
          }
          if (importedLibrary != null) {
            importedLibraries.add(importedLibrary);
            dependencyGraph.addEdge(library, importedLibrary);
          }
        }

        library.setExplicitlyImportsCore(explicitlyImportsCore);
        if (!explicitlyImportsCore && !coreLibrarySource.equals(library.getLibrarySource())) {
          ResolvableLibrary importedLibrary = libraryMap.get(coreLibrarySource);
          if (importedLibrary == null) {
            importedLibrary = createLibraryOrNull(coreLibrarySource);
            if (importedLibrary != null) {
              computeLibraryDependencies(importedLibrary);
              if (taskData != null) {
                return;
              }
            }
          }
          if (importedLibrary != null) {
            importedLibraries.add(importedLibrary);
            dependencyGraph.addEdge(library, importedLibrary);
          }
        }

        library.setImportedLibraries(importedLibraries.toArray(new ResolvableLibrary[importedLibraries.size()]));
      } else {
        library.setExplicitlyImportsCore(false);
        ResolvableLibrary importedLibrary = libraryMap.get(coreLibrarySource);
        if (importedLibrary == null) {
          importedLibrary = createLibraryOrNull(coreLibrarySource);
          if (importedLibrary != null) {
            computeLibraryDependencies(importedLibrary);
            if (taskData != null) {
              return;
            }
          }
        }
        if (importedLibrary != null) {
          dependencyGraph.addEdge(library, importedLibrary);
          library.setImportedLibraries(new ResolvableLibrary[] {importedLibrary});
        }
      }

      int exportCount = exportedSources.length;
      if (exportCount > 0) {
        ArrayList<ResolvableLibrary> exportedLibraries = new ArrayList<ResolvableLibrary>(
            exportCount);
        for (int i = 0; i < exportCount; i++) {
          Source exportedSource = exportedSources[i];
          ResolvableLibrary exportedLibrary = libraryMap.get(exportedSource);
          if (exportedLibrary == null) {
            exportedLibrary = createLibraryOrNull(exportedSource);
            if (exportedLibrary != null) {
              computeLibraryDependencies(exportedLibrary);
              if (taskData != null) {
                return;
              }
            }
          }
          if (exportedLibrary != null) {
            exportedLibraries.add(exportedLibrary);
            dependencyGraph.addEdge(library, exportedLibrary);
          }
        }
        library.setExportedLibraries(exportedLibraries.toArray(new ResolvableLibrary[exportedLibraries.size()]));
      }
    }

    /**
     * Gather the resolvable AST structures for each of the compilation units in each of the
     * libraries in the cycle. This is done in two phases: first we ensure that we have cached an
     * AST structure for each compilation unit, then we gather them. We split the work this way
     * because getting the AST structures can change the state of the cache in such a way that we
     * would have more work to do if any compilation unit didn't have a resolvable AST structure.
     */
    private void computePartsInCycle(Source librarySource) {
      int count = librariesInCycle.size();
      ArrayList<LibraryPair> libraryData = new ArrayList<LibraryPair>(count);
      for (int i = 0; i < count; i++) {
        ResolvableLibrary library = librariesInCycle.get(i);
        libraryData.add(new LibraryPair(library, ensurePartsInLibrary(library)));
      }
      neededForResolution = gatherSources(libraryData);
      if (TRACE_PERFORM_TASK) {
        System.out.println("  preserve resolution data for " + neededForResolution.size()
            + " sources while resolving " + librarySource.getFullName());
      }
      if (taskData != null) {
        return;
      }
      for (int i = 0; i < count; i++) {
        computePartsInLibrary(libraryData.get(i));
      }
    }

    /**
     * Gather the resolvable compilation units for each of the compilation units in the specified
     * library.
     * 
     * @param libraryPair a holder containing both the library and a list of (source, entry) pairs
     *          for all of the compilation units in the library
     */
    private void computePartsInLibrary(LibraryPair libraryPair) {
      ResolvableLibrary library = libraryPair.library;
      ArrayList<SourceEntryPair> entryPairs = libraryPair.entryPairs;
      int count = entryPairs.size();
      ResolvableCompilationUnit[] units = new ResolvableCompilationUnit[count];
      for (int i = 0; i < count; i++) {
        SourceEntryPair entryPair = entryPairs.get(i);
        Source source = entryPair.source;
        DartEntryImpl dartCopy = entryPair.entry.getWritableCopy();
        units[i] = new ResolvableCompilationUnit(
            dartCopy.getModificationTime(),
            dartCopy.getResolvableCompilationUnit(),
            source);
        cache.put(source, dartCopy);
      }
      library.setResolvableCompilationUnits(units);
    }

    /**
     * Create an object to represent the information about the library defined by the compilation
     * unit with the given source.
     * 
     * @param librarySource the source of the library's defining compilation unit
     * @return the library object that was created
     */
    private ResolvableLibrary createLibrary(Source librarySource) {
      ResolvableLibrary library = new ResolvableLibrary(librarySource);
      SourceEntry sourceEntry = cache.get(librarySource);
      if (sourceEntry instanceof DartEntry) {
        LibraryElementImpl libraryElement = (LibraryElementImpl) sourceEntry.getValue(DartEntry.ELEMENT);
        if (libraryElement != null) {
          library.setLibraryElement(libraryElement);
        }
      }
      libraryMap.put(librarySource, library);
      return library;
    }

    /**
     * Create an object to represent the information about the library defined by the compilation
     * unit with the given source.
     * 
     * @param librarySource the source of the library's defining compilation unit
     * @return the library object that was created
     */
    private ResolvableLibrary createLibraryOrNull(Source librarySource) {
      if (!exists(librarySource)) {
        return null;
      }
      ResolvableLibrary library = new ResolvableLibrary(librarySource);
      SourceEntry sourceEntry = cache.get(librarySource);
      if (sourceEntry instanceof DartEntry) {
        LibraryElementImpl libraryElement = (LibraryElementImpl) sourceEntry.getValue(DartEntry.ELEMENT);
        if (libraryElement != null) {
          library.setLibraryElement(libraryElement);
        }
      }
      libraryMap.put(librarySource, library);
      return library;
    }

    /**
     * Ensure that all of the libraries that are exported by the given library (but are not
     * themselves in the cycle) have element models built for them.
     * 
     * @param library the library being tested
     */
    private void ensureExports(ResolvableLibrary library, HashSet<Source> visitedLibraries) {
      ResolvableLibrary[] dependencies = library.getExports();
      int dependencyCount = dependencies.length;
      for (int i = 0; i < dependencyCount; i++) {
        ResolvableLibrary dependency = dependencies[i];
        if (!librariesInCycle.contains(dependency)
            && visitedLibraries.add(dependency.getLibrarySource())) {
          if (dependency.getLibraryElement() == null) {
            Source dependencySource = dependency.getLibrarySource();
            workManager.addFirst(dependencySource, SourcePriority.LIBRARY);
            if (taskData == null) {
              taskData = createResolveDartLibraryTask(
                  dependencySource,
                  getReadableDartEntry(dependencySource));
              return;
            }
          } else {
            ensureExports(dependency, visitedLibraries);
            if (taskData != null) {
              return;
            }
          }
        }
      }
    }

    /**
     * Ensure that all of the libraries that are exported by the given library (but are not
     * themselves in the cycle) have element models built for them.
     * 
     * @param library the library being tested
     * @throws MissingDataException if there is at least one library being depended on that does not
     *           have an element model built for it
     */
    private void ensureImports(ResolvableLibrary library) {
      ResolvableLibrary[] dependencies = library.getImports();
      int dependencyCount = dependencies.length;
      for (int i = 0; i < dependencyCount; i++) {
        ResolvableLibrary dependency = dependencies[i];
        if (!librariesInCycle.contains(dependency) && dependency.getLibraryElement() == null) {
          Source dependencySource = dependency.getLibrarySource();
          workManager.addFirst(dependencySource, SourcePriority.LIBRARY);
          if (taskData == null) {
            taskData = createResolveDartLibraryTask(
                dependencySource,
                getReadableDartEntry(dependencySource));
            return;
          }
        }
      }
    }

    /**
     * Ensure that all of the libraries that are either imported or exported by libraries in the
     * cycle (but are not themselves in the cycle) have element models built for them.
     */
    private void ensureImportsAndExports() {
      HashSet<Source> visitedLibraries = new HashSet<Source>();
      int libraryCount = librariesInCycle.size();
      for (int i = 0; i < libraryCount; i++) {
        ResolvableLibrary library = librariesInCycle.get(i);
        ensureImports(library);
        if (taskData != null) {
          return;
        }
        ensureExports(library, visitedLibraries);
        if (taskData != null) {
          return;
        }
      }
    }

    /**
     * Ensure that there is a resolvable compilation unit available for all of the compilation units
     * in the given library.
     * 
     * @param library the library for which resolvable compilation units must be available
     * @return a list of (source, entry) pairs for all of the compilation units in the library
     */
    private ArrayList<SourceEntryPair> ensurePartsInLibrary(ResolvableLibrary library) {
      ArrayList<SourceEntryPair> pairs = new ArrayList<SourceEntryPair>();
      Source librarySource = library.getLibrarySource();
      DartEntry libraryEntry = getReadableDartEntry(librarySource);
      ensureResolvableCompilationUnit(librarySource, libraryEntry);
      pairs.add(new SourceEntryPair(librarySource, libraryEntry));
      Source[] partSources = getSources(librarySource, libraryEntry, DartEntry.INCLUDED_PARTS);
      int count = partSources.length;
      for (int i = 0; i < count; i++) {
        Source partSource = partSources[i];
        DartEntry partEntry = getReadableDartEntry(partSource);
        if (partEntry != null && partEntry.getState(DartEntry.PARSED_UNIT) != CacheState.ERROR) {
          ensureResolvableCompilationUnit(partSource, partEntry);
          pairs.add(new SourceEntryPair(partSource, partEntry));
        }
      }
      return pairs;
    }

    /**
     * Ensure that there is a resolvable compilation unit available for the given source.
     * 
     * @param source the source for which a resolvable compilation unit must be available
     * @param dartEntry the entry associated with the source
     */
    private void ensureResolvableCompilationUnit(Source source, DartEntry dartEntry) {
      // The entry will be null if the source represents a non-Dart file.
      if (dartEntry != null && !dartEntry.hasResolvableCompilationUnit()) {
        if (taskData == null) {
          taskData = createParseDartTask(source, dartEntry);
        }
      }
    }

    private HashSet<Source> gatherSources(ArrayList<LibraryPair> libraryData) {
      int libraryCount = libraryData.size();
      HashSet<Source> sources = new HashSet<Source>(libraryCount * 2);
      for (int i = 0; i < libraryCount; i++) {
        ArrayList<SourceEntryPair> entryPairs = libraryData.get(i).entryPairs;
        int entryCount = entryPairs.size();
        for (int j = 0; j < entryCount; j++) {
          sources.add(entryPairs.get(j).source);
        }
      }
      return sources;
    }

    /**
     * Return the sources described by the given descriptor.
     * 
     * @param source the source with which the sources are associated
     * @param dartEntry the entry corresponding to the source
     * @param descriptor the descriptor indicating which sources are to be returned
     * @return the sources described by the given descriptor
     */
    private Source[] getSources(Source source, DartEntry dartEntry,
        DataDescriptor<Source[]> descriptor) {
      if (dartEntry == null) {
        return Source.EMPTY_ARRAY;
      }
      CacheState exportState = dartEntry.getState(descriptor);
      if (exportState == CacheState.ERROR) {
        return Source.EMPTY_ARRAY;
      } else if (exportState != CacheState.VALID) {
        if (taskData == null) {
          taskData = createParseDartTask(source, dartEntry);
        }
        return Source.EMPTY_ARRAY;
      }
      return dartEntry.getValue(descriptor);
    }
  }

  /**
   * Instances of the class {@code TaskData} represent information about the next task to be
   * performed. Each data has an implicit associated source: the source that might need to be
   * analyzed. There are essentially three states that can be represented:
   * <ul>
   * <li>If {@link #getTask()} returns a non-{@code null} value, then that is the task that should
   * be executed to further analyze the associated source.
   * <li>Otherwise, if {@link #isBlocked()} returns {@code true}, then there is no work that can be
   * done, but analysis for the associated source is not complete.
   * <li>Otherwise, {@link #getDependentSource()} should return a source that needs to be analyzed
   * before the analysis of the associated source can be completed.
   * </ul>
   */
  private static class TaskData {
    /**
     * The task that is to be performed.
     */
    private AnalysisTask task;

    /**
     * A flag indicating whether the associated source is blocked waiting for its contents to be
     * loaded.
     */
    private boolean blocked;

    /**
     * Initialize a newly created data holder.
     * 
     * @param task the task that is to be performed
     * @param blocked {@code true} if the associated source is blocked waiting for its contents to
     *          be loaded
     */
    public TaskData(AnalysisTask task, boolean blocked) {
      this.task = task;
      this.blocked = blocked;
    }

    /**
     * Return the task that is to be performed, or {@code null} if there is no task associated with
     * the source.
     * 
     * @return the task that is to be performed
     */
    public AnalysisTask getTask() {
      return task;
    }

    /**
     * Return {@code true} if the associated source is blocked waiting for its contents to be
     * loaded.
     * 
     * @return {@code true} if the associated source is blocked waiting for its contents to be
     *         loaded
     */
    public boolean isBlocked() {
      return blocked;
    }

    @Override
    public String toString() {
      if (task == null) {
        return "blocked: " + blocked;
      }
      return task.toString();
    }
  }

  /**
   * The difference between the maximum cache size and the maximum priority order size. The priority
   * list must be capped so that it is less than the cache size. Failure to do so can result in an
   * infinite loop in performAnalysisTask() because re-caching one AST structure can cause another
   * priority source's AST structure to be flushed.
   */
  private static final int PRIORITY_ORDER_SIZE_DELTA = 4;

  /**
   * A flag indicating whether trace output should be produced as analysis tasks are performed. Used
   * for debugging.
   */
  private static final boolean TRACE_PERFORM_TASK = false;

  /**
   * The set of analysis options controlling the behavior of this context.
   */
  private AnalysisOptionsImpl options = new AnalysisOptionsImpl();

  /**
   * A flag indicating whether errors related to sources in the SDK should be generated and
   * reported.
   */
  boolean generateSdkErrors = true;

  /**
   * A flag indicating whether this context is disposed.
   */
  private boolean disposed;

  /**
   * A cache of content used to override the default content of a source.
   */
  private ContentCache contentCache = new ContentCache();

  /**
   * The source factory used to create the sources that can be analyzed in this context.
   */
  private SourceFactory sourceFactory;

  /**
   * The set of declared variables used when computing constant values.
   */
  private DeclaredVariables declaredVariables = new DeclaredVariables();

  /**
   * A source representing the core library.
   */
  private Source coreLibrarySource;

  /**
   * The partition that contains analysis results that are not shared with other contexts.
   */
  private CachePartition privatePartition;

  /**
   * A table mapping the sources known to the context to the information known about the source.
   */
  private AnalysisCache cache;

  /**
   * An array containing sources for which data should not be flushed.
   */
  private Source[] priorityOrder = Source.EMPTY_ARRAY;

  /**
   * An array containing sources whose AST structure is needed in order to resolve the next library
   * to be resolved.
   */
  private HashSet<Source> neededForResolution = null;

  /**
   * A table mapping sources to the change notices that are waiting to be returned related to that
   * source.
   */
  private HashMap<Source, ChangeNoticeImpl> pendingNotices = new HashMap<Source, ChangeNoticeImpl>();

  /**
   * A set containing information about the tasks that have been performed since the last change
   * notification. Used to detect infinite loops in {@link #performAnalysisTask()}.
   */
  private HashSet<String> recentTasks = new HashSet<String>();

  /**
   * The object used to synchronize access to all of the caches. The rules related to the use of
   * this lock object are
   * <ul>
   * <li>no analysis work is done while holding the lock, and</li>
   * <li>no analysis results can be recorded unless we have obtained the lock and validated that the
   * results are for the same version (modification time) of the source as our current cache
   * content.</li>
   * </ul>
   */
  private static Object cacheLock = new Object();

  /**
   * The object used to record the results of performing an analysis task.
   */
  private AnalysisTaskResultRecorder resultRecorder;

  /**
   * Cached information used in incremental analysis or {@code null} if none. Synchronize against
   * {@link #cacheLock} before accessing this field.
   */
  private IncrementalAnalysisCache incrementalAnalysisCache;

  /**
   * The object used to manage the list of sources that need to be analyzed.
   */
  private WorkManager workManager = new WorkManager();

  /**
   * The set of {@link AngularApplication} in this context.
   */
  private final Set<AngularApplication> angularApplications = Sets.newHashSet();

  /**
   * Initialize a newly created analysis context.
   */
  public AnalysisContextImpl() {
    super();
    resultRecorder = new AnalysisTaskResultRecorder();
    privatePartition = new UniversalCachePartition(
        AnalysisOptionsImpl.DEFAULT_CACHE_SIZE,
        new ContextRetentionPolicy());
    cache = createCacheFromSourceFactory(null);
  }

  @Override
  public void addSourceInfo(Source source, SourceEntry info) {
    // This implementation assumes that the access to the cache does not need to be synchronized
    // because no other object can have access to this context while this method is being invoked.
    cache.put(source, info);
  }

  @Override
  public void applyAnalysisDelta(AnalysisDelta delta) {
    ChangeSet changeSet = new ChangeSet();
    for (Entry<Source, AnalysisLevel> entry : delta.getAnalysisLevels().entrySet()) {
      Source source = entry.getKey();
      if (entry.getValue() == AnalysisLevel.NONE) {
        changeSet.removedSource(source);
      } else {
        changeSet.addedSource(source);
      }
    }
    applyChanges(changeSet);
  }

  @Override
  public void applyChanges(ChangeSet changeSet) {
    if (changeSet.isEmpty()) {
      return;
    }
    synchronized (cacheLock) {
      recentTasks.clear();
      //
      // First, compute the list of sources that have been removed.
      //
      ArrayList<Source> removedSources = new ArrayList<Source>(changeSet.getRemovedSources());
      for (SourceContainer container : changeSet.getRemovedContainers()) {
        addSourcesInContainer(removedSources, container);
      }
      //
      // Then determine which cached results are no longer valid.
      //
      boolean addedDartSource = false;
      for (Source source : changeSet.getAddedSources()) {
        if (sourceAvailable(source)) {
          addedDartSource = true;
        }
      }
      for (Source source : changeSet.getChangedSources()) {
        sourceChanged(source);
      }
      for (Map.Entry<Source, String> entry : changeSet.getChangedContents().entrySet()) {
        setContents(entry.getKey(), entry.getValue());
      }
      for (Map.Entry<Source, ChangeSet.ContentChange> entry : changeSet.getChangedRanges().entrySet()) {
        ChangeSet.ContentChange change = entry.getValue();
        setChangedContents(
            entry.getKey(),
            change.getContents(),
            change.getOffset(),
            change.getOldLength(),
            change.getNewLength());
      }
      for (Source source : changeSet.getDeletedSources()) {
        sourceDeleted(source);
      }
      for (Source source : removedSources) {
        sourceRemoved(source);
      }
      if (addedDartSource) {
        // TODO(brianwilkerson) This is hugely inefficient, but we need to re-analyze any libraries
        // that might have been referencing the not-yet-existing source that was just added. Longer
        // term we need to keep track of which libraries are referencing non-existing sources and
        // only re-analyze those libraries.
//        logInformation("Added Dart sources, invalidating all resolution information");
        ArrayList<Source> sourcesToInvalidate = new ArrayList<Source>();
        MapIterator<Source, SourceEntry> iterator = cache.iterator();
        while (iterator.moveNext()) {
          Source source = iterator.getKey();
          SourceEntry sourceEntry = iterator.getValue();
          if (!source.isInSystemLibrary()
              && (sourceEntry instanceof DartEntry || sourceEntry instanceof HtmlEntry)) {
            sourcesToInvalidate.add(source);
          }
        }
        int count = sourcesToInvalidate.size();
        for (int i = 0; i < count; i++) {
          Source source = sourcesToInvalidate.get(i);
          SourceEntry entry = getReadableSourceEntry(source);
          if (entry instanceof DartEntry) {
            DartEntry dartEntry = (DartEntry) entry;
            DartEntryImpl dartCopy = dartEntry.getWritableCopy();
            dartCopy.invalidateAllResolutionInformation();
            cache.put(source, dartCopy);
            SourcePriority priority = SourcePriority.UNKNOWN;
            SourceKind kind = dartCopy.getKind();
            if (kind == SourceKind.LIBRARY) {
              priority = SourcePriority.LIBRARY;
            } else if (kind == SourceKind.PART) {
              priority = SourcePriority.NORMAL_PART;
            }
            workManager.add(source, priority);
          } else if (entry instanceof HtmlEntry) {
            HtmlEntry htmlEntry = (HtmlEntry) entry;
            HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
            htmlCopy.invalidateAllResolutionInformation();
            cache.put(source, htmlCopy);
            workManager.add(source, SourcePriority.HTML);
          }
        }
      }
    }
  }

  @Override
  public String computeDocumentationComment(Element element) throws AnalysisException {
    if (element == null) {
      return null;
    }
    Source source = element.getSource();
    if (source == null) {
      return null;
    }
    CompilationUnit unit = parseCompilationUnit(source);
    if (unit == null) {
      return null;
    }
    NodeLocator locator = new NodeLocator(element.getNameOffset());
    AstNode nameNode = locator.searchWithin(unit);
    while (nameNode != null) {
      if (nameNode instanceof AnnotatedNode) {
        Comment comment = ((AnnotatedNode) nameNode).getDocumentationComment();
        if (comment == null) {
          return null;
        }
        StringBuilder builder = new StringBuilder();
        Token[] tokens = comment.getTokens();
        for (int i = 0; i < tokens.length; i++) {
          if (i > 0) {
            builder.append("\n");
          }
          builder.append(tokens[i].getLexeme());
        }
        return builder.toString();
      }
      nameNode = nameNode.getParent();
    }
    return null;
  }

  @Override
  public AnalysisError[] computeErrors(Source source) throws AnalysisException {
    boolean enableHints = options.getHint();
    SourceEntry sourceEntry = getReadableSourceEntry(source);
    if (sourceEntry instanceof DartEntry) {
      ArrayList<AnalysisError> errors = new ArrayList<AnalysisError>();
      try {
        DartEntry dartEntry = (DartEntry) sourceEntry;
        ListUtilities.addAll(errors, getDartScanData(source, dartEntry, DartEntry.SCAN_ERRORS));
        dartEntry = getReadableDartEntry(source);
        ListUtilities.addAll(errors, getDartParseData(source, dartEntry, DartEntry.PARSE_ERRORS));
        dartEntry = getReadableDartEntry(source);
        if (dartEntry.getValue(DartEntry.SOURCE_KIND) == SourceKind.LIBRARY) {
          ListUtilities.addAll(
              errors,
              getDartResolutionData(source, source, dartEntry, DartEntry.RESOLUTION_ERRORS));
          dartEntry = getReadableDartEntry(source);
          ListUtilities.addAll(
              errors,
              getDartVerificationData(source, source, dartEntry, DartEntry.VERIFICATION_ERRORS));
          if (enableHints) {
            dartEntry = getReadableDartEntry(source);
            ListUtilities.addAll(
                errors,
                getDartHintData(source, source, dartEntry, DartEntry.HINTS));
          }
        } else {
          Source[] libraries = getLibrariesContaining(source);
          for (Source librarySource : libraries) {
            ListUtilities.addAll(
                errors,
                getDartResolutionData(source, librarySource, dartEntry, DartEntry.RESOLUTION_ERRORS));
            dartEntry = getReadableDartEntry(source);
            ListUtilities.addAll(
                errors,
                getDartVerificationData(
                    source,
                    librarySource,
                    dartEntry,
                    DartEntry.VERIFICATION_ERRORS));
            if (enableHints) {
              dartEntry = getReadableDartEntry(source);
              ListUtilities.addAll(
                  errors,
                  getDartHintData(source, librarySource, dartEntry, DartEntry.HINTS));
            }
          }
        }
      } catch (ObsoleteSourceAnalysisException exception) {
        AnalysisEngine.getInstance().getLogger().logInformation(
            "Could not compute errors",
            exception);
      }
      if (errors.isEmpty()) {
        return AnalysisError.NO_ERRORS;
      }
      return errors.toArray(new AnalysisError[errors.size()]);
    } else if (sourceEntry instanceof HtmlEntry) {
      HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
      try {
        return getHtmlResolutionData(source, htmlEntry, HtmlEntry.RESOLUTION_ERRORS);
      } catch (ObsoleteSourceAnalysisException exception) {
        AnalysisEngine.getInstance().getLogger().logInformation(
            "Could not compute errors",
            exception);
      }
    }
    return AnalysisError.NO_ERRORS;
  }

  @Override
  public Source[] computeExportedLibraries(Source source) throws AnalysisException {
    return getDartParseData(source, DartEntry.EXPORTED_LIBRARIES, Source.EMPTY_ARRAY);
  }

  @Override
  public HtmlElement computeHtmlElement(Source source) throws AnalysisException {
    return getHtmlResolutionData(source, HtmlEntry.ELEMENT, null);
  }

  @Override
  public Source[] computeImportedLibraries(Source source) throws AnalysisException {
    return getDartParseData(source, DartEntry.IMPORTED_LIBRARIES, Source.EMPTY_ARRAY);
  }

  @Override
  public SourceKind computeKindOf(Source source) {
    SourceEntry sourceEntry = getReadableSourceEntry(source);
    if (sourceEntry == null) {
      return SourceKind.UNKNOWN;
    } else if (sourceEntry instanceof DartEntry) {
      try {
        return getDartParseData(source, (DartEntry) sourceEntry, DartEntry.SOURCE_KIND);
      } catch (AnalysisException exception) {
        return SourceKind.UNKNOWN;
      }
    }
    return sourceEntry.getKind();
  }

  @Override
  public LibraryElement computeLibraryElement(Source source) throws AnalysisException {
    return getDartResolutionData(source, source, DartEntry.ELEMENT, null);
  }

  @Override
  public LineInfo computeLineInfo(Source source) throws AnalysisException {
    SourceEntry sourceEntry = getReadableSourceEntry(source);
    try {
      if (sourceEntry instanceof HtmlEntry) {
        return getHtmlParseData(source, SourceEntry.LINE_INFO, null);
      } else if (sourceEntry instanceof DartEntry) {
        return getDartScanData(source, SourceEntry.LINE_INFO, null);
      }
    } catch (ObsoleteSourceAnalysisException exception) {
      AnalysisEngine.getInstance().getLogger().logInformation(
          "Could not compute " + SourceEntry.LINE_INFO.toString(),
          exception);
    }
    return null;
  }

  @Override
  public ResolvableCompilationUnit computeResolvableCompilationUnit(Source source)
      throws AnalysisException {
    synchronized (cacheLock) {
      DartEntry dartEntry = getReadableDartEntry(source);
      if (dartEntry == null) {
        throw new AnalysisException("computeResolvableCompilationUnit for non-Dart: "
            + source.getFullName());
      }
      dartEntry = cacheDartParseData(source, dartEntry, DartEntry.PARSED_UNIT);
      DartEntryImpl dartCopy = dartEntry.getWritableCopy();
      CompilationUnit unit = dartCopy.getResolvableCompilationUnit();
      if (unit == null) {
        throw new AnalysisException(
            "Internal error: computeResolvableCompilationUnit could not parse "
                + source.getFullName(),
            dartEntry.getException());
      }
      cache.put(source, dartCopy);
      return new ResolvableCompilationUnit(dartCopy.getModificationTime(), unit);
    }
  }

  @Override
  public void dispose() {
    disposed = true;
  }

  @Override
  public boolean exists(Source source) {
    if (source == null) {
      return false;
    }
    synchronized (cacheLock) {
      if (contentCache.getContents(source) != null) {
        return true;
      }
    }
    return source.exists();
  }

  @Override
  public AnalysisContext extractContext(SourceContainer container) {
    return extractContextInto(
        container,
        (InternalAnalysisContext) AnalysisEngine.getInstance().createAnalysisContext());
  }

  @Override
  public InternalAnalysisContext extractContextInto(SourceContainer container,
      InternalAnalysisContext newContext) {
    ArrayList<Source> sourcesToRemove = new ArrayList<Source>();
    synchronized (cacheLock) {
      // Move sources in the specified directory to the new context
      MapIterator<Source, SourceEntry> iterator = cache.iterator();
      while (iterator.moveNext()) {
        Source source = iterator.getKey();
        SourceEntry sourceEntry = iterator.getValue();
        if (container.contains(source)) {
          sourcesToRemove.add(source);
          newContext.addSourceInfo(source, sourceEntry.getWritableCopy());
        }
      }

      // TODO (danrubel): Either remove sources or adjust contract described in AnalysisContext.
      // Currently, callers assume that sources have been removed from this context

//      for (Source source : sourcesToRemove) {
//        // TODO(brianwilkerson) Determine whether the source should be removed (that is, whether
//        // there are no additional dependencies on the source), and if so remove all information
//        // about the source.
//        sourceMap.remove(source);
//        parseCache.remove(source);
//        publicNamespaceCache.remove(source);
//        libraryElementCache.remove(source);
//      }
    }

    return newContext;
  }

  @Override
  public AnalysisOptions getAnalysisOptions() {
    return options;
  }

  @Override
  public AngularApplication getAngularApplicationWithHtml(Source htmlSource) {
    SourceEntry sourceEntry = getReadableSourceEntryOrNull(htmlSource);
    if (sourceEntry instanceof HtmlEntry) {
      HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
      AngularApplication application = htmlEntry.getValue(HtmlEntry.ANGULAR_APPLICATION);
      if (application != null) {
        return application;
      }
      return htmlEntry.getValue(HtmlEntry.ANGULAR_ENTRY);
    }
    return null;
  }

  @Override
  public CompilationUnitElement getCompilationUnitElement(Source unitSource, Source librarySource) {
    LibraryElement libraryElement = getLibraryElement(librarySource);
    if (libraryElement != null) {
      // try defining unit
      CompilationUnitElement definingUnit = libraryElement.getDefiningCompilationUnit();
      if (definingUnit.getSource().equals(unitSource)) {
        return definingUnit;
      }
      // try parts
      for (CompilationUnitElement partUnit : libraryElement.getParts()) {
        if (partUnit.getSource().equals(unitSource)) {
          return partUnit;
        }
      }
    }
    return null;
  }

  @Override
  public TimestampedData<CharSequence> getContents(Source source) throws Exception {
    synchronized (cacheLock) {
      String contents = contentCache.getContents(source);
      if (contents != null) {
        return new TimestampedData<CharSequence>(
            contentCache.getModificationStamp(source),
            contents);
      }
    }
    return source.getContents();
  }

  @Override
  @SuppressWarnings("deprecation")
  @DartOmit
  public void getContentsToReceiver(Source source, ContentReceiver receiver) throws Exception {
    synchronized (cacheLock) {
      String contents = contentCache.getContents(source);
      if (contents != null) {
        receiver.accept(contents, contentCache.getModificationStamp(source));
        return;
      }
    }
    source.getContentsToReceiver(receiver);
  }

  @Override
  public DeclaredVariables getDeclaredVariables() {
    return declaredVariables;
  }

  @Override
  public Element getElement(ElementLocation location) {
    // TODO(brianwilkerson) This should not be a "get" method.
    try {
      String[] components = location.getComponents();
      Source source = computeSourceFromEncoding(components[0]);
      String sourceName = source.getShortName();
      if (AnalysisEngine.isDartFileName(sourceName)) {
        ElementImpl element = (ElementImpl) computeLibraryElement(source);
        for (int i = 1; i < components.length; i++) {
          if (element == null) {
            return null;
          }
          element = element.getChild(components[i]);
        }
        return element;
      }
      if (AnalysisEngine.isHtmlFileName(sourceName)) {
        return computeHtmlElement(source);
      }
    } catch (AnalysisException exception) {
    }
    return null;
  }

  @Override
  public AnalysisErrorInfo getErrors(Source source) {
    SourceEntry sourceEntry = getReadableSourceEntryOrNull(source);
    if (sourceEntry instanceof DartEntry) {
      DartEntry dartEntry = (DartEntry) sourceEntry;
      return new AnalysisErrorInfoImpl(
          dartEntry.getAllErrors(),
          dartEntry.getValue(SourceEntry.LINE_INFO));
    } else if (sourceEntry instanceof HtmlEntry) {
      HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
      return new AnalysisErrorInfoImpl(
          htmlEntry.getAllErrors(),
          htmlEntry.getValue(SourceEntry.LINE_INFO));
    }
    return new AnalysisErrorInfoImpl(AnalysisError.NO_ERRORS, null);
  }

  @Override
  public HtmlElement getHtmlElement(Source source) {
    SourceEntry sourceEntry = getReadableSourceEntryOrNull(source);
    if (sourceEntry instanceof HtmlEntry) {
      return ((HtmlEntry) sourceEntry).getValue(HtmlEntry.ELEMENT);
    }
    return null;
  }

  @Override
  public Source[] getHtmlFilesReferencing(Source source) {
    SourceKind sourceKind = getKindOf(source);
    if (sourceKind == null) {
      return Source.EMPTY_ARRAY;
    }
    synchronized (cacheLock) {
      ArrayList<Source> htmlSources = new ArrayList<Source>();
      switch (sourceKind) {
        case LIBRARY:
        default:
          MapIterator<Source, SourceEntry> iterator = cache.iterator();
          while (iterator.moveNext()) {
            SourceEntry sourceEntry = iterator.getValue();
            if (sourceEntry.getKind() == SourceKind.HTML) {
              Source[] referencedLibraries = ((HtmlEntry) sourceEntry).getValue(HtmlEntry.REFERENCED_LIBRARIES);
              if (contains(referencedLibraries, source)) {
                htmlSources.add(iterator.getKey());
              }
            }
          }
          break;
        case PART:
          Source[] librarySources = getLibrariesContaining(source);
          MapIterator<Source, SourceEntry> partIterator = cache.iterator();
          while (partIterator.moveNext()) {
            SourceEntry sourceEntry = partIterator.getValue();
            if (sourceEntry.getKind() == SourceKind.HTML) {
              Source[] referencedLibraries = ((HtmlEntry) sourceEntry).getValue(HtmlEntry.REFERENCED_LIBRARIES);
              if (containsAny(referencedLibraries, librarySources)) {
                htmlSources.add(partIterator.getKey());
              }
            }
          }
          break;
      }
      if (htmlSources.isEmpty()) {
        return Source.EMPTY_ARRAY;
      }
      return htmlSources.toArray(new Source[htmlSources.size()]);
    }
  }

  @Override
  public Source[] getHtmlSources() {
    return getSources(SourceKind.HTML);
  }

  @Override
  public SourceKind getKindOf(Source source) {
    SourceEntry sourceEntry = getReadableSourceEntryOrNull(source);
    if (sourceEntry == null) {
      return SourceKind.UNKNOWN;
    }
    return sourceEntry.getKind();
  }

  @Override
  public Source[] getLaunchableClientLibrarySources() {
    // TODO(brianwilkerson) This needs to filter out libraries that do not reference dart:html,
    // either directly or indirectly.
    ArrayList<Source> sources = new ArrayList<Source>();
    synchronized (cacheLock) {
      MapIterator<Source, SourceEntry> iterator = cache.iterator();
      while (iterator.moveNext()) {
        Source source = iterator.getKey();
        SourceEntry sourceEntry = iterator.getValue();
        if (sourceEntry.getKind() == SourceKind.LIBRARY && !source.isInSystemLibrary()) {
//          DartEntry dartEntry = (DartEntry) sourceEntry;
//          if (dartEntry.getValue(DartEntry.IS_LAUNCHABLE) && dartEntry.getValue(DartEntry.IS_CLIENT)) {
          sources.add(source);
//          }
        }
      }
    }
    return sources.toArray(new Source[sources.size()]);
  }

  @Override
  public Source[] getLaunchableServerLibrarySources() {
    // TODO(brianwilkerson) This needs to filter out libraries that reference dart:html, either
    // directly or indirectly.
    ArrayList<Source> sources = new ArrayList<Source>();
    synchronized (cacheLock) {
      MapIterator<Source, SourceEntry> iterator = cache.iterator();
      while (iterator.moveNext()) {
        Source source = iterator.getKey();
        SourceEntry sourceEntry = iterator.getValue();
        if (sourceEntry.getKind() == SourceKind.LIBRARY && !source.isInSystemLibrary()) {
//          DartEntry dartEntry = (DartEntry) sourceEntry;
//          if (dartEntry.getValue(DartEntry.IS_LAUNCHABLE) && !dartEntry.getValue(DartEntry.IS_CLIENT)) {
          sources.add(source);
//          }
        }
      }
    }
    return sources.toArray(new Source[sources.size()]);
  }

  @Override
  public Source[] getLibrariesContaining(Source source) {
    SourceEntry sourceEntry = getReadableSourceEntryOrNull(source);
    if (sourceEntry instanceof DartEntry) {
      return ((DartEntry) sourceEntry).getValue(DartEntry.CONTAINING_LIBRARIES);
    }
    return Source.EMPTY_ARRAY;
  }

  @Override
  public Source[] getLibrariesDependingOn(Source librarySource) {
    synchronized (cacheLock) {
      ArrayList<Source> dependentLibraries = new ArrayList<Source>();
      MapIterator<Source, SourceEntry> iterator = cache.iterator();
      while (iterator.moveNext()) {
        SourceEntry sourceEntry = iterator.getValue();
        if (sourceEntry.getKind() == SourceKind.LIBRARY) {
          if (contains(
              ((DartEntry) sourceEntry).getValue(DartEntry.EXPORTED_LIBRARIES),
              librarySource)) {
            dependentLibraries.add(iterator.getKey());
          }
          if (contains(
              ((DartEntry) sourceEntry).getValue(DartEntry.IMPORTED_LIBRARIES),
              librarySource)) {
            dependentLibraries.add(iterator.getKey());
          }
        }
      }
      if (dependentLibraries.isEmpty()) {
        return Source.EMPTY_ARRAY;
      }
      return dependentLibraries.toArray(new Source[dependentLibraries.size()]);
    }
  }

  @Override
  public Source[] getLibrariesReferencedFromHtml(Source htmlSource) {
    SourceEntry sourceEntry = getReadableSourceEntryOrNull(htmlSource);
    if (sourceEntry instanceof HtmlEntry) {
      HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
      return htmlEntry.getValue(HtmlEntry.REFERENCED_LIBRARIES);
    }
    return Source.EMPTY_ARRAY;
  }

  @Override
  public LibraryElement getLibraryElement(Source source) {
    SourceEntry sourceEntry = getReadableSourceEntryOrNull(source);
    if (sourceEntry instanceof DartEntry) {
      return ((DartEntry) sourceEntry).getValue(DartEntry.ELEMENT);
    }
    return null;
  }

  @Override
  public Source[] getLibrarySources() {
    return getSources(SourceKind.LIBRARY);
  }

  @Override
  public LineInfo getLineInfo(Source source) {
    SourceEntry sourceEntry = getReadableSourceEntryOrNull(source);
    if (sourceEntry != null) {
      return sourceEntry.getValue(SourceEntry.LINE_INFO);
    }
    return null;
  }

  @Override
  public long getModificationStamp(Source source) {
    synchronized (cacheLock) {
      Long stamp = contentCache.getModificationStamp(source);
      if (stamp != null) {
        return stamp.longValue();
      }
    }
    return source.getModificationStamp();
  }

  @Override
  public Namespace getPublicNamespace(LibraryElement library) {
    // TODO(brianwilkerson) Rename this to not start with 'get'. Note that this is not part of the
    // API of the interface.
    Source source = library.getDefiningCompilationUnit().getSource();
    DartEntry dartEntry = getReadableDartEntry(source);
    if (dartEntry == null) {
      return null;
    }
    Namespace namespace = null;
    if (dartEntry.getValue(DartEntry.ELEMENT) == library) {
      namespace = dartEntry.getValue(DartEntry.PUBLIC_NAMESPACE);
    }
    if (namespace == null) {
      NamespaceBuilder builder = new NamespaceBuilder();
      namespace = builder.createPublicNamespaceForLibrary(library);
      synchronized (cacheLock) {
        dartEntry = getReadableDartEntry(source);
        if (dartEntry == null) {
          AnalysisEngine.getInstance().getLogger().logError(
              "Could not compute the public namespace for " + library.getSource().getFullName(),
              new AnalysisException("A Dart file became a non-Dart file: " + source.getFullName()));
          return null;
        }
        if (dartEntry.getValue(DartEntry.ELEMENT) == library) {
          DartEntryImpl dartCopy = getReadableDartEntry(source).getWritableCopy();
          dartCopy.setValue(DartEntry.PUBLIC_NAMESPACE, namespace);
          cache.put(source, dartCopy);
        }
      }
    }
    return namespace;
  }

  @Override
  public Source[] getRefactoringUnsafeSources() {
    ArrayList<Source> sources = new ArrayList<Source>();
    synchronized (cacheLock) {
      MapIterator<Source, SourceEntry> iterator = cache.iterator();
      while (iterator.moveNext()) {
        SourceEntry sourceEntry = iterator.getValue();
        if (sourceEntry instanceof DartEntry) {
          if (!((DartEntry) sourceEntry).isRefactoringSafe()) {
            sources.add(iterator.getKey());
          }
        }
      }
    }
    return sources.toArray(new Source[sources.size()]);
  }

  @Override
  public CompilationUnit getResolvedCompilationUnit(Source unitSource, LibraryElement library) {
    if (library == null) {
      return null;
    }
    return getResolvedCompilationUnit(unitSource, library.getSource());
  }

  @Override
  public CompilationUnit getResolvedCompilationUnit(Source unitSource, Source librarySource) {
    SourceEntry sourceEntry = getReadableSourceEntryOrNull(unitSource);
    if (sourceEntry instanceof DartEntry) {
      return ((DartEntry) sourceEntry).getValueInLibrary(DartEntry.RESOLVED_UNIT, librarySource);
    }
    return null;
  }

  @Override
  public HtmlUnit getResolvedHtmlUnit(Source htmlSource) {
    SourceEntry sourceEntry = getReadableSourceEntryOrNull(htmlSource);
    if (sourceEntry instanceof HtmlEntry) {
      HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
      return htmlEntry.getValue(HtmlEntry.RESOLVED_UNIT);
    }
    return null;
  }

  @Override
  public SourceFactory getSourceFactory() {
    return sourceFactory;
  }

  /**
   * Return a list of the sources that would be processed by {@link #performAnalysisTask()}. This
   * method duplicates, and must therefore be kept in sync with, {@link #getNextAnalysisTask()}.
   * This method is intended to be used for testing purposes only.
   * 
   * @return a list of the sources that would be processed by {@link #performAnalysisTask()}
   */
  @VisibleForTesting
  public List<Source> getSourcesNeedingProcessing() {
    HashSet<Source> sources = new HashSet<Source>();
    synchronized (cacheLock) {
      boolean hintsEnabled = options.getHint();
      //
      // Look for priority sources that need to be analyzed.
      //
      for (Source source : priorityOrder) {
        getSourcesNeedingProcessing(source, cache.get(source), true, hintsEnabled, sources);
      }
      //
      // Look for non-priority sources that need to be analyzed.
      //
      MapIterator<Source, SourceEntry> iterator = cache.iterator();
      while (iterator.moveNext()) {
        getSourcesNeedingProcessing(
            iterator.getKey(),
            iterator.getValue(),
            false,
            hintsEnabled,
            sources);
      }
    }
    return new ArrayList<Source>(sources);
  }

  @Override
  public AnalysisContextStatistics getStatistics() {
    boolean hintsEnabled = options.getHint();
    AnalysisContextStatisticsImpl statistics = new AnalysisContextStatisticsImpl();
    synchronized (cacheLock) {
      MapIterator<Source, SourceEntry> iterator = cache.iterator();
      while (iterator.moveNext()) {
        SourceEntry sourceEntry = iterator.getValue();
        if (sourceEntry instanceof DartEntry) {
          Source source = iterator.getKey();
          DartEntry dartEntry = (DartEntry) sourceEntry;
          SourceKind kind = dartEntry.getValue(DartEntry.SOURCE_KIND);
          // get library independent values
          statistics.putCacheItem(dartEntry, SourceEntry.LINE_INFO);
          statistics.putCacheItem(dartEntry, DartEntry.PARSE_ERRORS);
          statistics.putCacheItem(dartEntry, DartEntry.PARSED_UNIT);
          statistics.putCacheItem(dartEntry, DartEntry.SOURCE_KIND);
          if (kind == SourceKind.LIBRARY) {
            statistics.putCacheItem(dartEntry, DartEntry.ELEMENT);
            statistics.putCacheItem(dartEntry, DartEntry.EXPORTED_LIBRARIES);
            statistics.putCacheItem(dartEntry, DartEntry.IMPORTED_LIBRARIES);
            statistics.putCacheItem(dartEntry, DartEntry.INCLUDED_PARTS);
            statistics.putCacheItem(dartEntry, DartEntry.IS_CLIENT);
            statistics.putCacheItem(dartEntry, DartEntry.IS_LAUNCHABLE);
            // The public namespace isn't computed by performAnalysisTask() and therefore isn't
            // interesting.
            //statistics.putCacheItem(dartEntry, DartEntry.PUBLIC_NAMESPACE);
          }
          // get library-specific values
          Source[] librarySources = getLibrariesContaining(source);
          for (Source librarySource : librarySources) {
            statistics.putCacheItemInLibrary(dartEntry, librarySource, DartEntry.RESOLUTION_ERRORS);
            statistics.putCacheItemInLibrary(dartEntry, librarySource, DartEntry.RESOLVED_UNIT);
            if (generateSdkErrors || !source.isInSystemLibrary()) {
              statistics.putCacheItemInLibrary(
                  dartEntry,
                  librarySource,
                  DartEntry.VERIFICATION_ERRORS);
              if (hintsEnabled) {
                statistics.putCacheItemInLibrary(dartEntry, librarySource, DartEntry.HINTS);
              }
            }
          }
        } else if (sourceEntry instanceof HtmlEntry) {
          HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
          statistics.putCacheItem(htmlEntry, SourceEntry.LINE_INFO);
          statistics.putCacheItem(htmlEntry, HtmlEntry.PARSE_ERRORS);
          statistics.putCacheItem(htmlEntry, HtmlEntry.PARSED_UNIT);
          statistics.putCacheItem(htmlEntry, HtmlEntry.RESOLUTION_ERRORS);
          statistics.putCacheItem(htmlEntry, HtmlEntry.RESOLVED_UNIT);
          // We are not currently recording any hints related to HTML.
          // statistics.putCacheItem(htmlEntry, HtmlEntry.HINTS);
        }
      }
    }
    statistics.setPartitionData(cache.getPartitionData());
    return statistics;
  }

  @Override
  public TypeProvider getTypeProvider() throws AnalysisException {
    Source coreSource = getSourceFactory().forUri(DartSdk.DART_CORE);
    if (coreSource == null) {
      throw new AnalysisException("Could not create a source for dart:core");
    }
    LibraryElement coreElement = computeLibraryElement(coreSource);
    if (coreElement == null) {
      throw new AnalysisException("Could not create an element for dart:core");
    }
    return new TypeProviderImpl(coreElement);
  }

  @Override
  public boolean isClientLibrary(Source librarySource) {
    SourceEntry sourceEntry = getReadableSourceEntry(librarySource);
    if (sourceEntry instanceof DartEntry) {
      DartEntry dartEntry = (DartEntry) sourceEntry;
      return dartEntry.getValue(DartEntry.IS_CLIENT) && dartEntry.getValue(DartEntry.IS_LAUNCHABLE);
    }
    return false;
  }

  @Override
  public boolean isDisposed() {
    return disposed;
  }

  @Override
  public boolean isServerLibrary(Source librarySource) {
    SourceEntry sourceEntry = getReadableSourceEntry(librarySource);
    if (sourceEntry instanceof DartEntry) {
      DartEntry dartEntry = (DartEntry) sourceEntry;
      return !dartEntry.getValue(DartEntry.IS_CLIENT)
          && dartEntry.getValue(DartEntry.IS_LAUNCHABLE);
    }
    return false;
  }

  @Override
  public void mergeContext(AnalysisContext context) {
    if (context instanceof InstrumentedAnalysisContextImpl) {
      context = ((InstrumentedAnalysisContextImpl) context).getBasis();
    }
    if (!(context instanceof AnalysisContextImpl)) {
      return;
    }
    synchronized (cacheLock) {
      // TODO(brianwilkerson) This does not lock against the other context's cacheLock.
      MapIterator<Source, SourceEntry> iterator = cache.iterator();
      while (iterator.moveNext()) {
        Source newSource = iterator.getKey();
        SourceEntry existingEntry = getReadableSourceEntry(newSource);
        if (existingEntry == null) {
          // TODO(brianwilkerson) Decide whether we really need to copy the info.
          cache.put(newSource, iterator.getValue().getWritableCopy());
        } else {
          // TODO(brianwilkerson) Decide whether/how to merge the entries.
        }
      }
    }
  }

  @Override
  public CompilationUnit parseCompilationUnit(Source source) throws AnalysisException {
    return getDartParseData(source, DartEntry.PARSED_UNIT, null);
  }

  @Override
  public HtmlUnit parseHtmlUnit(Source source) throws AnalysisException {
    return getHtmlParseData(source, HtmlEntry.PARSED_UNIT, null);
  }

  @Override
  public AnalysisResult performAnalysisTask() {
    if (TRACE_PERFORM_TASK) {
      System.out.println("----------------------------------------");
    }
    long getStart = System.currentTimeMillis();
    AnalysisTask task = getNextAnalysisTask();
    long getEnd = System.currentTimeMillis();
    if (task == null && validateCacheConsistency()) {
      task = getNextAnalysisTask();
    }
    if (task == null) {
      return new AnalysisResult(getChangeNotices(true), getEnd - getStart, null, -1L);
    }
    String taskDescriptor = task.toString();
//    if (recentTasks.add(taskDescriptor)) {
//      logInformation("Performing task: " + taskDescriptor);
//    } else {
//      if (TRACE_PERFORM_TASK) {
//        System.out.print("* ");
//      }
//      logInformation("*** Performing repeated task: " + taskDescriptor);
//    }
    if (TRACE_PERFORM_TASK) {
      System.out.println(taskDescriptor);
    }
    long performStart = System.currentTimeMillis();
    try {
      task.perform(resultRecorder);
    } catch (ObsoleteSourceAnalysisException exception) {
      AnalysisEngine.getInstance().getLogger().logInformation(
          "Could not perform analysis task: " + taskDescriptor,
          exception);
    } catch (AnalysisException exception) {
      if (!(exception.getCause() instanceof IOException)) {
        AnalysisEngine.getInstance().getLogger().logError(
            "Internal error while performing the task: " + task,
            exception);
      }
    }
    long performEnd = System.currentTimeMillis();
    return new AnalysisResult(
        getChangeNotices(false),
        getEnd - getStart,
        task.getClass().getName(),
        performEnd - performStart);
  }

  @Override
  public void recordLibraryElements(Map<Source, LibraryElement> elementMap) {
    synchronized (cacheLock) {
      Source htmlSource = sourceFactory.forUri(DartSdk.DART_HTML);
      for (Map.Entry<Source, LibraryElement> entry : elementMap.entrySet()) {
        Source librarySource = entry.getKey();
        LibraryElement library = entry.getValue();
        //
        // Cache the element in the library's info.
        //
        DartEntry dartEntry = getReadableDartEntry(librarySource);
        if (dartEntry != null) {
          DartEntryImpl dartCopy = dartEntry.getWritableCopy();
          recordElementData(dartCopy, library, library.getSource(), htmlSource);
          dartCopy.setValue(DartEntry.SCAN_ERRORS, AnalysisError.NO_ERRORS);
          dartCopy.setValue(DartEntry.PARSE_ERRORS, AnalysisError.NO_ERRORS);
          dartCopy.setState(DartEntry.PARSED_UNIT, CacheState.FLUSHED);
          dartCopy.setValueInLibrary(
              DartEntry.BUILD_ELEMENT_ERRORS,
              librarySource,
              AnalysisError.NO_ERRORS);
          dartCopy.setValueInLibrary(
              DartEntry.RESOLUTION_ERRORS,
              librarySource,
              AnalysisError.NO_ERRORS);
          dartCopy.setStateInLibrary(DartEntry.RESOLVED_UNIT, librarySource, CacheState.FLUSHED);
          dartCopy.setValueInLibrary(
              DartEntry.VERIFICATION_ERRORS,
              librarySource,
              AnalysisError.NO_ERRORS);
          dartCopy.setValue(DartEntry.ANGULAR_ERRORS, AnalysisError.NO_ERRORS);
          dartCopy.setValueInLibrary(DartEntry.HINTS, librarySource, AnalysisError.NO_ERRORS);
          cache.put(librarySource, dartCopy);
        }
      }
    }
  }

  @Override
  public CompilationUnit resolveCompilationUnit(Source unitSource, LibraryElement library)
      throws AnalysisException {
    if (library == null) {
      return null;
    }
    return resolveCompilationUnit(unitSource, library.getSource());
  }

  @Override
  public CompilationUnit resolveCompilationUnit(Source unitSource, Source librarySource)
      throws AnalysisException {
    return getDartResolutionData(unitSource, librarySource, DartEntry.RESOLVED_UNIT, null);
  }

  @Override
  public HtmlUnit resolveHtmlUnit(Source htmlSource) throws AnalysisException {
    computeHtmlElement(htmlSource);
    return parseHtmlUnit(htmlSource);
  }

  @Override
  public void setAnalysisOptions(AnalysisOptions options) {
    synchronized (cacheLock) {
      boolean needsRecompute = this.options.getAnalyzeAngular() != options.getAnalyzeAngular()
          || this.options.getAnalyzeFunctionBodies() != options.getAnalyzeFunctionBodies()
          || this.options.getGenerateSdkErrors() != options.getGenerateSdkErrors()
          || this.options.getEnableDeferredLoading() != options.getEnableDeferredLoading()
          || this.options.getDart2jsHint() != options.getDart2jsHint()
          || (this.options.getHint() && !options.getHint())
          || this.options.getPreserveComments() != options.getPreserveComments();

      int cacheSize = options.getCacheSize();
      if (this.options.getCacheSize() != cacheSize) {
        this.options.setCacheSize(cacheSize);
        //cache.setMaxCacheSize(cacheSize);
        privatePartition.setMaxCacheSize(cacheSize);
        //
        // Cap the size of the priority list to being less than the cache size. Failure to do so can
        // result in an infinite loop in performAnalysisTask() because re-caching one AST structure
        // can cause another priority source's AST structure to be flushed.
        //
        int maxPriorityOrderSize = cacheSize - PRIORITY_ORDER_SIZE_DELTA;
        if (priorityOrder.length > maxPriorityOrderSize) {
          Source[] newPriorityOrder = new Source[maxPriorityOrderSize];
          System.arraycopy(priorityOrder, 0, newPriorityOrder, 0, maxPriorityOrderSize);
          priorityOrder = newPriorityOrder;
        }
      }
      this.options.setAnalyzeAngular(options.getAnalyzeAngular());
      this.options.setAnalyzeFunctionBodies(options.getAnalyzeFunctionBodies());
      this.options.setGenerateSdkErrors(options.getGenerateSdkErrors());
      this.options.setEnableDeferredLoading(options.getEnableDeferredLoading());
      this.options.setDart2jsHint(options.getDart2jsHint());
      this.options.setHint(options.getHint());
      this.options.setIncremental(options.getIncremental());
      this.options.setPreserveComments(options.getPreserveComments());

      generateSdkErrors = options.getGenerateSdkErrors();

      if (needsRecompute) {
        invalidateAllLocalResolutionInformation();
      }
    }
  }

  @Override
  public void setAnalysisPriorityOrder(List<Source> sources) {
    synchronized (cacheLock) {
      if (sources == null || sources.isEmpty()) {
        priorityOrder = Source.EMPTY_ARRAY;
      } else {
        while (sources.remove(null)) {
          // Nothing else to do.
        }
        if (sources.isEmpty()) {
          priorityOrder = Source.EMPTY_ARRAY;
        }
        //
        // Cap the size of the priority list to being less than the cache size. Failure to do so can
        // result in an infinite loop in performAnalysisTask() because re-caching one AST structure
        // can cause another priority source's AST structure to be flushed.
        //
        int count = Math.min(sources.size(), options.getCacheSize() - PRIORITY_ORDER_SIZE_DELTA);
        priorityOrder = new Source[count];
        for (int i = 0; i < count; i++) {
          priorityOrder[i] = sources.get(i);
        }
      }
    }
  }

  @Override
  public void setChangedContents(Source source, String contents, int offset, int oldLength,
      int newLength) {
    synchronized (cacheLock) {
      recentTasks.clear();
      String originalContents = contentCache.setContents(source, contents);
      if (contents != null) {
        if (!contents.equals(originalContents)) {
          if (options.getIncremental()) {
            incrementalAnalysisCache = IncrementalAnalysisCache.update(
                incrementalAnalysisCache,
                source,
                originalContents,
                contents,
                offset,
                oldLength,
                newLength,
                getReadableSourceEntry(source));
          }
          sourceChanged(source);
          SourceEntry sourceEntry = cache.get(source);
          if (sourceEntry != null) {
            SourceEntryImpl sourceCopy = sourceEntry.getWritableCopy();
            sourceCopy.setModificationTime(contentCache.getModificationStamp(source));
            sourceCopy.setValue(SourceEntry.CONTENT, contents);
            cache.put(source, sourceCopy);
          }
        }
      } else if (originalContents != null) {
        incrementalAnalysisCache = IncrementalAnalysisCache.clear(incrementalAnalysisCache, source);
        sourceChanged(source);
      }
    }
  }

  @Override
  public void setContents(Source source, String contents) {
    synchronized (cacheLock) {
      recentTasks.clear();
      String originalContents = contentCache.setContents(source, contents);
      if (contents != null) {
        if (!contents.equals(originalContents)) {
          incrementalAnalysisCache = IncrementalAnalysisCache.clear(
              incrementalAnalysisCache,
              source);
          sourceChanged(source);
          SourceEntry sourceEntry = cache.get(source);
          if (sourceEntry != null) {
            SourceEntryImpl sourceCopy = sourceEntry.getWritableCopy();
            sourceCopy.setModificationTime(contentCache.getModificationStamp(source));
            sourceCopy.setValue(SourceEntry.CONTENT, contents);
            cache.put(source, sourceCopy);
          }
        }
      } else if (originalContents != null) {
        incrementalAnalysisCache = IncrementalAnalysisCache.clear(incrementalAnalysisCache, source);
        sourceChanged(source);
      }
    }
  }

  @Override
  public void setSourceFactory(SourceFactory factory) {
    synchronized (cacheLock) {
      if (sourceFactory == factory) {
        return;
      } else if (factory.getContext() != null) {
        throw new IllegalStateException("Source factories cannot be shared between contexts");
      }
      if (sourceFactory != null) {
        sourceFactory.setContext(null);
      }
      factory.setContext(this);
      sourceFactory = factory;
      coreLibrarySource = sourceFactory.forUri(DartSdk.DART_CORE);

      cache = createCacheFromSourceFactory(factory);

      invalidateAllLocalResolutionInformation();
    }
  }

  /**
   * Create an analysis cache based on the given source factory.
   * 
   * @param factory the source factory containing the information needed to create the cache
   * @return the cache that was created
   */
  protected AnalysisCache createCacheFromSourceFactory(SourceFactory factory) {
    if (factory == null) {
      return new AnalysisCache(new CachePartition[] {privatePartition});
    }
    DartSdk sdk = factory.getDartSdk();
    if (sdk == null) {
      return new AnalysisCache(new CachePartition[] {privatePartition});
    }
    return new AnalysisCache(new CachePartition[] {
        AnalysisEngine.getInstance().getPartitionManager().forSdk(sdk), privatePartition});
  }

  /**
   * Record the results produced by performing a {@link ResolveDartLibraryCycleTask}. If the results
   * were computed from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  protected DartEntry recordResolveDartLibraryCycleTaskResults(ResolveDartLibraryCycleTask task)
      throws AnalysisException {
    LibraryResolver2 resolver = task.getLibraryResolver();
    AnalysisException thrownException = task.getException();
    DartEntry unitEntry = null;
    Source unitSource = task.getUnitSource();
    if (resolver != null) {
      //
      // The resolver should only be null if an exception was thrown before (or while) it was
      // being created.
      //
      List<ResolvableLibrary> resolvedLibraries = resolver.getResolvedLibraries();
      if (resolvedLibraries == null) {
        //
        // The resolved libraries should only be null if an exception was thrown during resolution.
        //
        unitEntry = getReadableDartEntry(unitSource);
        if (unitEntry == null) {
          throw new AnalysisException("A Dart file became a non-Dart file: "
              + unitSource.getFullName());
        }
        DartEntryImpl dartCopy = unitEntry.getWritableCopy();
        if (thrownException == null) {
          dartCopy.recordResolutionError(new AnalysisException(
              "In recordResolveDartLibraryCycleTaskResults, resolvedLibraries was null and there was no thrown exception"));
        } else {
          dartCopy.recordResolutionError(thrownException);
        }
        cache.put(unitSource, dartCopy);
        cache.remove(unitSource);
        if (thrownException != null) {
          throw thrownException;
        }
        return dartCopy;
      }
      synchronized (cacheLock) {
        if (allModificationTimesMatch(resolvedLibraries)) {
          Source htmlSource = getSourceFactory().forUri(DartSdk.DART_HTML);
          RecordingErrorListener errorListener = resolver.getErrorListener();
          for (ResolvableLibrary library : resolvedLibraries) {
            Source librarySource = library.getLibrarySource();
            for (Source source : library.getCompilationUnitSources()) {
              CompilationUnit unit = library.getAST(source);
              AnalysisError[] errors = errorListener.getErrorsForSource(source);
              LineInfo lineInfo = getLineInfo(source);
              DartEntryImpl dartCopy = (DartEntryImpl) cache.get(source).getWritableCopy();
              if (thrownException == null) {
                dartCopy.setState(DartEntry.PARSED_UNIT, CacheState.FLUSHED);
                dartCopy.setValueInLibrary(DartEntry.RESOLVED_UNIT, librarySource, unit);
                dartCopy.setValueInLibrary(DartEntry.RESOLUTION_ERRORS, librarySource, errors);
                if (source.equals(librarySource)) {
                  recordElementData(
                      dartCopy,
                      library.getLibraryElement(),
                      librarySource,
                      htmlSource);
                }
                cache.storedAst(source);
              } else {
                dartCopy.recordResolutionErrorInLibrary(librarySource, thrownException);
                cache.remove(source);
              }
              cache.put(source, dartCopy);
              if (!source.equals(librarySource)) {
                workManager.add(source, SourcePriority.PRIORITY_PART);
              }
              if (source.equals(unitSource)) {
                unitEntry = dartCopy;
              }

              ChangeNoticeImpl notice = getNotice(source);
              notice.setCompilationUnit(unit);
              notice.setErrors(dartCopy.getAllErrors(), lineInfo);
            }
          }
        } else {
          @SuppressWarnings("resource")
          PrintStringWriter writer = new PrintStringWriter();
          writer.println("Library resolution results discarded for");
          for (ResolvableLibrary library : resolvedLibraries) {
            for (Source source : library.getCompilationUnitSources()) {
              DartEntry dartEntry = getReadableDartEntry(source);
              if (dartEntry != null) {
                long resultTime = library.getModificationTime(source);
                writer.println("  " + debuggingString(source) + "; sourceTime = "
                    + getModificationStamp(source) + ", resultTime = " + resultTime
                    + ", cacheTime = " + dartEntry.getModificationTime());
                DartEntryImpl dartCopy = dartEntry.getWritableCopy();
                if (thrownException == null || resultTime >= 0L) {
                  //
                  // The analysis was performed on out-of-date sources. Mark the cache so that the
                  // sources will be re-analyzed using the up-to-date sources.
                  //
                  dartCopy.recordResolutionNotInProcess();
                } else {
                  //
                  // We could not determine whether the sources were up-to-date or out-of-date. Mark
                  // the cache so that we won't attempt to re-analyze the sources until there's a
                  // good chance that we'll be able to do so without error.
                  //
                  dartCopy.recordResolutionError(thrownException);
                  cache.remove(source);
                }
                cache.put(source, dartCopy);
                if (source.equals(unitSource)) {
                  unitEntry = dartCopy;
                }
              } else {
                writer.println("  " + debuggingString(source) + "; sourceTime = "
                    + getModificationStamp(source) + ", no entry");
              }
            }
          }
          logInformation(writer.toString());
        }
      }
    }
    if (thrownException != null) {
      throw thrownException;
    }
    if (unitEntry == null) {
      unitEntry = getReadableDartEntry(unitSource);
      if (unitEntry == null) {
        throw new AnalysisException("A Dart file became a non-Dart file: "
            + unitSource.getFullName());
      }
    }
    return unitEntry;
  }

  /*
   * Record the results produced by performing a {@link ResolveDartLibraryTask}. If the results were
   * computed from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  protected DartEntry recordResolveDartLibraryTaskResults(ResolveDartLibraryTask task)
      throws AnalysisException {
    LibraryResolver resolver = task.getLibraryResolver();
    AnalysisException thrownException = task.getException();
    DartEntry unitEntry = null;
    Source unitSource = task.getUnitSource();
    if (resolver != null) {
      //
      // The resolver should only be null if an exception was thrown before (or while) it was
      // being created.
      //
      Set<Library> resolvedLibraries = resolver.getResolvedLibraries();
      if (resolvedLibraries == null) {
        //
        // The resolved libraries should only be null if an exception was thrown during resolution.
        //
        unitEntry = getReadableDartEntry(unitSource);
        if (unitEntry == null) {
          throw new AnalysisException("A Dart file became a non-Dart file: "
              + unitSource.getFullName());
        }
        DartEntryImpl dartCopy = unitEntry.getWritableCopy();
        if (thrownException == null) {
          dartCopy.recordResolutionError(new AnalysisException(
              "In recordResolveDartLibraryTaskResults, resolvedLibraries was null and there was no thrown exception"));
        } else {
          dartCopy.recordResolutionError(thrownException);
        }
        cache.put(unitSource, dartCopy);
        cache.remove(unitSource);
        if (thrownException != null) {
          throw thrownException;
        }
        return dartCopy;
      }
      synchronized (cacheLock) {
        if (allModificationTimesMatch(resolvedLibraries)) {
          Source htmlSource = getSourceFactory().forUri(DartSdk.DART_HTML);
          RecordingErrorListener errorListener = resolver.getErrorListener();
          for (Library library : resolvedLibraries) {
            Source librarySource = library.getLibrarySource();
            for (Source source : library.getCompilationUnitSources()) {
              CompilationUnit unit = library.getAST(source);
              AnalysisError[] errors = errorListener.getErrorsForSource(source);
              LineInfo lineInfo = getLineInfo(source);
              DartEntry dartEntry = (DartEntry) cache.get(source);
              long sourceTime = getModificationStamp(source);
              if (dartEntry.getModificationTime() != sourceTime) {
                // The source has changed without the context being notified. Simulate notification.
                sourceChanged(source);
                dartEntry = getReadableDartEntry(source);
                if (dartEntry == null) {
                  throw new AnalysisException("A Dart file became a non-Dart file: "
                      + source.getFullName());
                }
              }
              DartEntryImpl dartCopy = dartEntry.getWritableCopy();
              if (thrownException == null) {
                dartCopy.setValue(SourceEntry.LINE_INFO, lineInfo);
                dartCopy.setState(DartEntry.PARSED_UNIT, CacheState.FLUSHED);
                dartCopy.setValueInLibrary(DartEntry.RESOLVED_UNIT, librarySource, unit);
                dartCopy.setValueInLibrary(DartEntry.RESOLUTION_ERRORS, librarySource, errors);
                if (source.equals(librarySource)) {
                  recordElementData(
                      dartCopy,
                      library.getLibraryElement(),
                      librarySource,
                      htmlSource);
                }
                cache.storedAst(source);
              } else {
                dartCopy.recordResolutionErrorInLibrary(librarySource, thrownException);
                cache.remove(source);
              }
              cache.put(source, dartCopy);
              if (!source.equals(librarySource)) {
                workManager.add(source, SourcePriority.PRIORITY_PART);
              }
              if (source.equals(unitSource)) {
                unitEntry = dartCopy;
              }

              ChangeNoticeImpl notice = getNotice(source);
              notice.setCompilationUnit(unit);
              notice.setErrors(dartCopy.getAllErrors(), lineInfo);
            }
          }
        } else {
          @SuppressWarnings("resource")
          PrintStringWriter writer = new PrintStringWriter();
          writer.println("Library resolution results discarded for");
          for (Library library : resolvedLibraries) {
            for (Source source : library.getCompilationUnitSources()) {
              DartEntry dartEntry = getReadableDartEntry(source);
              if (dartEntry != null) {
                long resultTime = library.getModificationTime(source);
                writer.println("  " + debuggingString(source) + "; sourceTime = "
                    + getModificationStamp(source) + ", resultTime = " + resultTime
                    + ", cacheTime = " + dartEntry.getModificationTime());
                DartEntryImpl dartCopy = dartEntry.getWritableCopy();
                if (thrownException == null || resultTime >= 0L) {
                  //
                  // The analysis was performed on out-of-date sources. Mark the cache so that the
                  // sources will be re-analyzed using the up-to-date sources.
                  //
                  dartCopy.recordResolutionNotInProcess();
                } else {
                  //
                  // We could not determine whether the sources were up-to-date or out-of-date. Mark
                  // the cache so that we won't attempt to re-analyze the sources until there's a
                  // good chance that we'll be able to do so without error.
                  //
                  dartCopy.recordResolutionError(thrownException);
                  cache.remove(source);
                }
                cache.put(source, dartCopy);
                if (source.equals(unitSource)) {
                  unitEntry = dartCopy;
                }
              } else {
                writer.println("  " + debuggingString(source) + "; sourceTime = "
                    + getModificationStamp(source) + ", no entry");
              }
            }
          }
          logInformation(writer.toString());
        }
      }
    }
    if (thrownException != null) {
      throw thrownException;
    }
    if (unitEntry == null) {
      unitEntry = getReadableDartEntry(unitSource);
      if (unitEntry == null) {
        throw new AnalysisException("A Dart file became a non-Dart file: "
            + unitSource.getFullName());
      }
    }
    return unitEntry;
  }

  /**
   * Record that we have accessed the AST structure associated with the given source. At the moment,
   * there is no differentiation between the parsed and resolved forms of the AST.
   * 
   * @param source the source whose AST structure was accessed
   */
  private void accessedAst(Source source) {
    synchronized (cacheLock) {
      cache.accessedAst(source);
    }
  }

  /**
   * Add all of the sources contained in the given source container to the given list of sources.
   * <p>
   * Note: This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param sources the list to which sources are to be added
   * @param container the source container containing the sources to be added to the list
   */
  private void addSourcesInContainer(ArrayList<Source> sources, SourceContainer container) {
    MapIterator<Source, SourceEntry> iterator = cache.iterator();
    while (iterator.moveNext()) {
      Source source = iterator.getKey();
      if (container.contains(source)) {
        sources.add(source);
      }
    }
  }

  /**
   * Return {@code true} if the modification times of the sources used by the given library resolver
   * to resolve one or more libraries are consistent with the modification times in the cache.
   * 
   * @param resolver the library resolver used to resolve one or more libraries
   * @return {@code true} if we should record the results of the resolution
   * @throws AnalysisException if any of the modification times could not be determined (this should
   *           not happen)
   */
  private boolean allModificationTimesMatch(List<ResolvableLibrary> resolvedLibraries)
      throws AnalysisException {
    boolean allTimesMatch = true;
    for (ResolvableLibrary library : resolvedLibraries) {
      for (Source source : library.getCompilationUnitSources()) {
        DartEntry dartEntry = getReadableDartEntry(source);
        if (dartEntry == null) {
          // This shouldn't be possible because we should never have performed the task if the
          // source didn't represent a Dart file, but check to be safe.
          throw new AnalysisException(
              "Internal error: attempting to resolve non-Dart file as a Dart file: "
                  + source.getFullName());
        }
        long sourceTime = getModificationStamp(source);
        long resultTime = library.getModificationTime(source);
        if (sourceTime != resultTime) {
          // The source has changed without the context being notified. Simulate notification.
          sourceChanged(source);
          allTimesMatch = false;
        }
      }
    }
    return allTimesMatch;
  }

  /**
   * Return {@code true} if the modification times of the sources used by the given library resolver
   * to resolve one or more libraries are consistent with the modification times in the cache.
   * 
   * @param resolver the library resolver used to resolve one or more libraries
   * @return {@code true} if we should record the results of the resolution
   * @throws AnalysisException if any of the modification times could not be determined (this should
   *           not happen)
   */
  private boolean allModificationTimesMatch(Set<Library> resolvedLibraries)
      throws AnalysisException {
    boolean allTimesMatch = true;
    for (Library library : resolvedLibraries) {
      for (Source source : library.getCompilationUnitSources()) {
        DartEntry dartEntry = getReadableDartEntry(source);
        if (dartEntry == null) {
          // This shouldn't be possible because we should never have performed the task if the
          // source didn't represent a Dart file, but check to be safe.
          throw new AnalysisException(
              "Internal error: attempting to resolve non-Dart file as a Dart file: "
                  + source.getFullName());
        }
        long sourceTime = getModificationStamp(source);
        long resultTime = library.getModificationTime(source);
        if (sourceTime != resultTime) {
          // The source has changed without the context being notified. Simulate notification.
          sourceChanged(source);
          allTimesMatch = false;
        }
      }
    }
    return allTimesMatch;
  }

  /**
   * Given a source for a Dart file and the library that contains it, return a cache entry in which
   * the state of the data represented by the given descriptor is either {@link CacheState#VALID} or
   * {@link CacheState#ERROR}. This method assumes that the data can be produced by generating hints
   * for the library if the data is not already cached.
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
   * 
   * @param unitSource the source representing the Dart file
   * @param librarySource the source representing the library containing the Dart file
   * @param dartEntry the cache entry associated with the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return a cache entry containing the required data
   * @throws AnalysisException if data could not be returned because the source could not be parsed
   */
  private DartEntry cacheDartHintData(Source unitSource, Source librarySource, DartEntry dartEntry,
      DataDescriptor<?> descriptor) throws AnalysisException {
    //
    // Check to see whether we already have the information being requested.
    //
    CacheState state = dartEntry.getStateInLibrary(descriptor, librarySource);
    while (state != CacheState.ERROR && state != CacheState.VALID) {
      //
      // If not, compute the information. Unless the modification date of the source continues to
      // change, this loop will eventually terminate.
      //
      DartEntry libraryEntry = getReadableDartEntry(librarySource);
      libraryEntry = cacheDartResolutionData(
          librarySource,
          librarySource,
          libraryEntry,
          DartEntry.ELEMENT);
      LibraryElement libraryElement = libraryEntry.getValue(DartEntry.ELEMENT);
      CompilationUnitElement definingUnit = libraryElement.getDefiningCompilationUnit();
      CompilationUnitElement[] parts = libraryElement.getParts();
      @SuppressWarnings("unchecked")
      TimestampedData<CompilationUnit>[] units = new TimestampedData[parts.length + 1];
      units[0] = getResolvedUnit(definingUnit, librarySource);
      if (units[0] == null) {
        Source source = definingUnit.getSource();
        units[0] = new TimestampedData<CompilationUnit>(
            getModificationStamp(source),
            resolveCompilationUnit(source, libraryElement));
      }
      for (int i = 0; i < parts.length; i++) {
        units[i + 1] = getResolvedUnit(parts[i], librarySource);
        if (units[i + 1] == null) {
          Source source = parts[i].getSource();
          units[i + 1] = new TimestampedData<CompilationUnit>(
              getModificationStamp(source),
              resolveCompilationUnit(source, libraryElement));
        }
      }
      dartEntry = (DartEntry) new GenerateDartHintsTask(
          this,
          units,
          getLibraryElement(librarySource)).perform(resultRecorder);
      state = dartEntry.getStateInLibrary(descriptor, librarySource);
    }
    return dartEntry;
  }

  /**
   * Given a source for a Dart file, return a cache entry in which the state of the data represented
   * by the given descriptor is either {@link CacheState#VALID} or {@link CacheState#ERROR}. This
   * method assumes that the data can be produced by parsing the source if it is not already cached.
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
   * 
   * @param source the source representing the Dart file
   * @param dartEntry the cache entry associated with the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return a cache entry containing the required data
   * @throws AnalysisException if data could not be returned because the source could not be parsed
   */
  private DartEntry cacheDartParseData(Source source, DartEntry dartEntry,
      DataDescriptor<?> descriptor) throws AnalysisException {
    if (descriptor == DartEntry.PARSED_UNIT) {
      if (dartEntry.hasResolvableCompilationUnit()) {
        return dartEntry;
      }
    }
    //
    // Check to see whether we already have the information being requested.
    //
    CacheState state = dartEntry.getState(descriptor);
    while (state != CacheState.ERROR && state != CacheState.VALID) {
      //
      // If not, compute the information. Unless the modification date of the source continues to
      // change, this loop will eventually terminate.
      //
      dartEntry = cacheDartScanData(source, dartEntry, DartEntry.TOKEN_STREAM);
      dartEntry = (DartEntry) new ParseDartTask(
          this,
          source,
          dartEntry.getModificationTime(),
          dartEntry.getValue(DartEntry.TOKEN_STREAM),
          dartEntry.getValue(SourceEntry.LINE_INFO)).perform(resultRecorder);
      state = dartEntry.getState(descriptor);
    }
    return dartEntry;
  }

  /**
   * Given a source for a Dart file and the library that contains it, return a cache entry in which
   * the state of the data represented by the given descriptor is either {@link CacheState#VALID} or
   * {@link CacheState#ERROR}. This method assumes that the data can be produced by resolving the
   * source in the context of the library if it is not already cached.
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
   * 
   * @param unitSource the source representing the Dart file
   * @param librarySource the source representing the library containing the Dart file
   * @param dartEntry the cache entry associated with the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return a cache entry containing the required data
   * @throws AnalysisException if data could not be returned because the source could not be parsed
   */
  private DartEntry cacheDartResolutionData(Source unitSource, Source librarySource,
      DartEntry dartEntry, DataDescriptor<?> descriptor) throws AnalysisException {
    //
    // Check to see whether we already have the information being requested.
    //
    CacheState state = (descriptor == DartEntry.ELEMENT) ? dartEntry.getState(descriptor)
        : dartEntry.getStateInLibrary(descriptor, librarySource);
    while (state != CacheState.ERROR && state != CacheState.VALID) {
      //
      // If not, compute the information. Unless the modification date of the source continues to
      // change, this loop will eventually terminate.
      //
      // TODO(brianwilkerson) As an optimization, if we already have the element model for the
      // library we can use ResolveDartUnitTask to produce the resolved AST structure much faster.
      dartEntry = (DartEntry) new ResolveDartLibraryTask(this, unitSource, librarySource).perform(resultRecorder);
      state = (descriptor == DartEntry.ELEMENT) ? dartEntry.getState(descriptor)
          : dartEntry.getStateInLibrary(descriptor, librarySource);
    }
    return dartEntry;
  }

  /**
   * Given a source for a Dart file, return a cache entry in which the state of the data represented
   * by the given descriptor is either {@link CacheState#VALID} or {@link CacheState#ERROR}. This
   * method assumes that the data can be produced by scanning the source if it is not already
   * cached.
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
   * 
   * @param source the source representing the Dart file
   * @param dartEntry the cache entry associated with the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return a cache entry containing the required data
   * @throws AnalysisException if data could not be returned because the source could not be scanned
   */
  private DartEntry cacheDartScanData(Source source, DartEntry dartEntry,
      DataDescriptor<?> descriptor) throws AnalysisException {
    //
    // Check to see whether we already have the information being requested.
    //
    CacheState state = dartEntry.getState(descriptor);
    while (state != CacheState.ERROR && state != CacheState.VALID) {
      //
      // If not, compute the information. Unless the modification date of the source continues to
      // change, this loop will eventually terminate.
      //
      try {
        if (dartEntry.getState(SourceEntry.CONTENT) != CacheState.VALID) {
          dartEntry = (DartEntry) new GetContentTask(this, source).perform(resultRecorder);
        }
        dartEntry = (DartEntry) new ScanDartTask(
            this,
            source,
            dartEntry.getModificationTime(),
            dartEntry.getValue(SourceEntry.CONTENT)).perform(resultRecorder);
      } catch (AnalysisException exception) {
        throw exception;
      } catch (Exception exception) {
        throw new AnalysisException("Exception", exception);
      }
      state = dartEntry.getState(descriptor);
    }
    return dartEntry;
  }

  /**
   * Given a source for a Dart file and the library that contains it, return a cache entry in which
   * the state of the data represented by the given descriptor is either {@link CacheState#VALID} or
   * {@link CacheState#ERROR}. This method assumes that the data can be produced by verifying the
   * source in the given library if the data is not already cached.
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
   * 
   * @param unitSource the source representing the Dart file
   * @param librarySource the source representing the library containing the Dart file
   * @param dartEntry the cache entry associated with the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return a cache entry containing the required data
   * @throws AnalysisException if data could not be returned because the source could not be parsed
   */
  private DartEntry cacheDartVerificationData(Source unitSource, Source librarySource,
      DartEntry dartEntry, DataDescriptor<?> descriptor) throws AnalysisException {
    //
    // Check to see whether we already have the information being requested.
    //
    CacheState state = dartEntry.getStateInLibrary(descriptor, librarySource);
    while (state != CacheState.ERROR && state != CacheState.VALID) {
      //
      // If not, compute the information. Unless the modification date of the source continues to
      // change, this loop will eventually terminate.
      //
      LibraryElement library = computeLibraryElement(librarySource);
      CompilationUnit unit = resolveCompilationUnit(unitSource, library);
      if (unit == null) {
        throw new AnalysisException("Could not resolve compilation unit "
            + unitSource.getFullName() + " in " + librarySource.getFullName());
      }
      dartEntry = (DartEntry) new GenerateDartErrorsTask(
          this,
          unitSource,
          dartEntry.getModificationTime(),
          unit,
          library).perform(resultRecorder);
      state = dartEntry.getStateInLibrary(descriptor, librarySource);
    }
    return dartEntry;
  }

  /**
   * Given a source for an HTML file, return a cache entry in which all of the data represented by
   * the state of the given descriptors is either {@link CacheState#VALID} or
   * {@link CacheState#ERROR}. This method assumes that the data can be produced by parsing the
   * source if it is not already cached.
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
   * 
   * @param source the source representing the HTML file
   * @param htmlEntry the cache entry associated with the HTML file
   * @param descriptor the descriptor representing the data to be returned
   * @return a cache entry containing the required data
   * @throws AnalysisException if data could not be returned because the source could not be
   *           resolved
   */
  private HtmlEntry cacheHtmlParseData(Source source, HtmlEntry htmlEntry,
      DataDescriptor<?> descriptor) throws AnalysisException {
    if (descriptor == HtmlEntry.PARSED_UNIT) {
      HtmlUnit unit = htmlEntry.getAnyParsedUnit();
      if (unit != null) {
        return htmlEntry;
      }
    }
    //
    // Check to see whether we already have the information being requested.
    //
    CacheState state = htmlEntry.getState(descriptor);
    while (state != CacheState.ERROR && state != CacheState.VALID) {
      //
      // If not, compute the information. Unless the modification date of the source continues to
      // change, this loop will eventually terminate.
      //
      try {
        if (htmlEntry.getState(SourceEntry.CONTENT) != CacheState.VALID) {
          htmlEntry = (HtmlEntry) new GetContentTask(this, source).perform(resultRecorder);
        }
        htmlEntry = (HtmlEntry) new ParseHtmlTask(
            this,
            source,
            htmlEntry.getModificationTime(),
            htmlEntry.getValue(SourceEntry.CONTENT)).perform(resultRecorder);
      } catch (AnalysisException exception) {
        throw exception;
      } catch (Exception exception) {
        throw new AnalysisException("Exception", exception);
      }
      state = htmlEntry.getState(descriptor);
    }
    return htmlEntry;
  }

  /**
   * Given a source for an HTML file, return a cache entry in which the state of the data
   * represented by the given descriptor is either {@link CacheState#VALID} or
   * {@link CacheState#ERROR}. This method assumes that the data can be produced by resolving the
   * source if it is not already cached.
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
   * 
   * @param source the source representing the HTML file
   * @param dartEntry the cache entry associated with the HTML file
   * @param descriptor the descriptor representing the data to be returned
   * @return a cache entry containing the required data
   * @throws AnalysisException if data could not be returned because the source could not be
   *           resolved
   */
  private HtmlEntry cacheHtmlResolutionData(Source source, HtmlEntry htmlEntry,
      DataDescriptor<?> descriptor) throws AnalysisException {
    //
    // Check to see whether we already have the information being requested.
    //
    CacheState state = htmlEntry.getState(descriptor);
    while (state != CacheState.ERROR && state != CacheState.VALID) {
      //
      // If not, compute the information. Unless the modification date of the source continues to
      // change, this loop will eventually terminate.
      //
      htmlEntry = cacheHtmlParseData(source, htmlEntry, HtmlEntry.PARSED_UNIT);
      htmlEntry = (HtmlEntry) new ResolveHtmlTask(
          this,
          source,
          htmlEntry.getModificationTime(),
          htmlEntry.getValue(HtmlEntry.PARSED_UNIT)).perform(resultRecorder);
      state = htmlEntry.getState(descriptor);
    }
    return htmlEntry;
  }

  /**
   * Compute the transitive closure of all libraries that depend on the given library by adding such
   * libraries to the given collection.
   * 
   * @param library the library on which the other libraries depend
   * @param librariesToInvalidate the libraries that depend on the given library
   */
  private void computeAllLibrariesDependingOn(Source library, HashSet<Source> librariesToInvalidate) {
    if (librariesToInvalidate.add(library)) {
      for (Source dependentLibrary : getLibrariesDependingOn(library)) {
        computeAllLibrariesDependingOn(dependentLibrary, librariesToInvalidate);
      }
    }
  }

  /**
   * Given the encoded form of a source, use the source factory to reconstitute the original source.
   * 
   * @param encoding the encoded form of a source
   * @return the source represented by the encoding
   */
  private Source computeSourceFromEncoding(String encoding) {
    synchronized (cacheLock) {
      return sourceFactory.fromEncoding(encoding);
    }
  }

  /**
   * Return {@code true} if the given array of sources contains the given source.
   * 
   * @param sources the sources being searched
   * @param targetSource the source being searched for
   * @return {@code true} if the given source is in the array
   */
  private boolean contains(Source[] sources, Source targetSource) {
    for (Source source : sources) {
      if (source.equals(targetSource)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return {@code true} if the given array of sources contains any of the given target sources.
   * 
   * @param sources the sources being searched
   * @param targetSources the sources being searched for
   * @return {@code true} if any of the given target sources are in the array
   */
  private boolean containsAny(Source[] sources, Source[] targetSources) {
    for (Source targetSource : targetSources) {
      if (contains(sources, targetSource)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Create a {@link BuildDartElementModelTask} for the given source, marking the built unit as
   * being in-process.
   * 
   * @param source the source for the library whose element model is to be built
   * @param dartEntry the entry for the source
   * @return task data representing the created task
   */
  private TaskData createBuildDartElementModelTask(Source source, DartEntry dartEntry) {
    try {
      CycleBuilder builder = new CycleBuilder();
      builder.computeCycleContaining(source);
      TaskData taskData = builder.getTaskData();
      if (taskData != null) {
        return taskData;
      }
      DartEntryImpl dartCopy = dartEntry.getWritableCopy();
      dartCopy.setStateInLibrary(DartEntry.BUILT_UNIT, source, CacheState.IN_PROCESS);
      cache.put(source, dartCopy);
      return new TaskData(
          new BuildDartElementModelTask(this, source, builder.getLibrariesInCycle()),
          false);
    } catch (AnalysisException exception) {
      DartEntryImpl dartCopy = dartEntry.getWritableCopy();
      dartCopy.recordBuildElementErrorInLibrary(source, exception);
      cache.put(source, dartCopy);
      AnalysisEngine.getInstance().getLogger().logError(
          "Internal error trying to compute the next analysis task",
          exception);
    }
    return new TaskData(null, false);
  }

  /**
   * Create a {@link GenerateDartErrorsTask} for the given source, marking the verification errors
   * as being in-process. The compilation unit and the library can be the same if the compilation
   * unit is the defining compilation unit of the library.
   * 
   * @param unitSource the source for the compilation unit to be verified
   * @param unitEntry the entry for the compilation unit
   * @param librarySource the source for the library containing the compilation unit
   * @param libraryEntry the entry for the library
   * @return task data representing the created task
   */
  private TaskData createGenerateDartErrorsTask(Source unitSource, DartEntry unitEntry,
      Source librarySource, DartEntry libraryEntry) {
    if (unitEntry.getStateInLibrary(DartEntry.RESOLVED_UNIT, librarySource) != CacheState.VALID
        || libraryEntry.getState(DartEntry.ELEMENT) != CacheState.VALID) {
      return createResolveDartLibraryTask(librarySource, libraryEntry);
    }
    CompilationUnit unit = unitEntry.getValueInLibrary(DartEntry.RESOLVED_UNIT, librarySource);
    if (unit == null) {
      AnalysisException exception = new AnalysisException(
          "Entry has VALID state for RESOLVED_UNIT but null value for " + unitSource.getFullName()
              + " in " + librarySource.getFullName());
      AnalysisEngine.getInstance().getLogger().logInformation(exception.getMessage(), exception);
      DartEntryImpl dartCopy = unitEntry.getWritableCopy();
      dartCopy.recordResolutionError(exception);
      cache.put(unitSource, dartCopy);
      return new TaskData(null, false);
    }
    LibraryElement libraryElement = libraryEntry.getValue(DartEntry.ELEMENT);
    DartEntryImpl dartCopy = unitEntry.getWritableCopy();
    dartCopy.setStateInLibrary(DartEntry.VERIFICATION_ERRORS, librarySource, CacheState.IN_PROCESS);
    cache.put(unitSource, dartCopy);
    return new TaskData(new GenerateDartErrorsTask(
        this,
        unitSource,
        dartCopy.getModificationTime(),
        unit,
        libraryElement), false);
  }

  /**
   * Create a {@link GenerateDartHintsTask} for the given source, marking the hints as being
   * in-process.
   * 
   * @param source the source whose content is to be verified
   * @param dartEntry the entry for the source
   * @param librarySource the source for the library containing the source
   * @param libraryEntry the entry for the library
   * @return task data representing the created task
   */
  private TaskData createGenerateDartHintsTask(Source source, DartEntry dartEntry,
      Source librarySource, DartEntry libraryEntry) {
    if (libraryEntry.getState(DartEntry.ELEMENT) != CacheState.VALID) {
      return createResolveDartLibraryTask(librarySource, libraryEntry);
    }
    LibraryElement libraryElement = libraryEntry.getValue(DartEntry.ELEMENT);
    CompilationUnitElement definingUnit = libraryElement.getDefiningCompilationUnit();
    CompilationUnitElement[] parts = libraryElement.getParts();
    @SuppressWarnings("unchecked")
    TimestampedData<CompilationUnit>[] units = new TimestampedData[parts.length + 1];
    units[0] = getResolvedUnit(definingUnit, librarySource);
    if (units[0] == null) {
      // TODO(brianwilkerson) We should return a ResolveDartUnitTask (unless there are multiple ASTs
      // that need to be resolved.
      return createResolveDartLibraryTask(librarySource, libraryEntry);
    }
    for (int i = 0; i < parts.length; i++) {
      units[i + 1] = getResolvedUnit(parts[i], librarySource);
      if (units[i + 1] == null) {
        // TODO(brianwilkerson) We should return a ResolveDartUnitTask (unless there are multiple
        // ASTs that need to be resolved.
        return createResolveDartLibraryTask(librarySource, libraryEntry);
      }
    }

    DartEntryImpl dartCopy = dartEntry.getWritableCopy();
    dartCopy.setStateInLibrary(DartEntry.HINTS, librarySource, CacheState.IN_PROCESS);
    cache.put(source, dartCopy);
    return new TaskData(new GenerateDartHintsTask(this, units, libraryElement), false);
  }

  /**
   * Create a {@link GetContentTask} for the given source, marking the content as being in-process.
   * 
   * @param source the source whose content is to be accessed
   * @param sourceEntry the entry for the source
   * @return task data representing the created task
   */
  private TaskData createGetContentTask(Source source, SourceEntry sourceEntry) {
    SourceEntryImpl sourceCopy = sourceEntry.getWritableCopy();
    sourceCopy.setState(SourceEntry.CONTENT, CacheState.IN_PROCESS);
    cache.put(source, sourceCopy);
    return new TaskData(new GetContentTask(this, source), false);
  }

  /**
   * Create a {@link ParseDartTask} for the given source, marking the parse errors as being
   * in-process.
   * 
   * @param source the source whose content is to be parsed
   * @param dartEntry the entry for the source
   * @return task data representing the created task
   */
  private TaskData createParseDartTask(Source source, DartEntry dartEntry) {
    if (dartEntry.getState(DartEntry.TOKEN_STREAM) != CacheState.VALID
        || dartEntry.getState(SourceEntry.LINE_INFO) != CacheState.VALID) {
      return createScanDartTask(source, dartEntry);
    }
    Token tokenStream = dartEntry.getValue(DartEntry.TOKEN_STREAM);
    DartEntryImpl dartCopy = dartEntry.getWritableCopy();
    dartCopy.setState(DartEntry.TOKEN_STREAM, CacheState.FLUSHED);
    dartCopy.setState(DartEntry.PARSE_ERRORS, CacheState.IN_PROCESS);
    cache.put(source, dartCopy);
    return new TaskData(new ParseDartTask(
        this,
        source,
        dartCopy.getModificationTime(),
        tokenStream,
        dartEntry.getValue(SourceEntry.LINE_INFO)), false);
  }

  /**
   * Create a {@link ParseHtmlTask} for the given source, marking the parse errors as being
   * in-process.
   * 
   * @param source the source whose content is to be parsed
   * @param htmlEntry the entry for the source
   * @return task data representing the created task
   */
  private TaskData createParseHtmlTask(Source source, HtmlEntry htmlEntry) {
    if (htmlEntry.getState(SourceEntry.CONTENT) != CacheState.VALID) {
      return createGetContentTask(source, htmlEntry);
    }
    CharSequence content = htmlEntry.getValue(SourceEntry.CONTENT);
    HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
    htmlCopy.setState(SourceEntry.CONTENT, CacheState.FLUSHED);
    htmlCopy.setState(HtmlEntry.PARSE_ERRORS, CacheState.IN_PROCESS);
    cache.put(source, htmlCopy);
    return new TaskData(
        new ParseHtmlTask(this, source, htmlCopy.getModificationTime(), content),
        false);
  }

  /**
   * Create a {@link PolymerBuildHtmlTask} for the given source, marking the Polymer elements as
   * being in-process.
   * 
   * @param source the source whose content is to be processed
   * @param htmlEntry the entry for the source
   * @return task data representing the created task
   */
  private TaskData createPolymerBuildHtmlTask(Source source, HtmlEntry htmlEntry) {
    if (htmlEntry.getState(HtmlEntry.RESOLVED_UNIT) != CacheState.VALID) {
      return createResolveHtmlTask(source, htmlEntry);
    }
    HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
    htmlCopy.setState(HtmlEntry.POLYMER_BUILD_ERRORS, CacheState.IN_PROCESS);
    cache.put(source, htmlCopy);
    return new TaskData(new PolymerBuildHtmlTask(
        this,
        source,
        htmlCopy.getModificationTime(),
        htmlEntry.getValue(SourceEntry.LINE_INFO),
        htmlCopy.getValue(HtmlEntry.RESOLVED_UNIT)), false);
  }

  /**
   * Create a {@link PolymerResolveHtmlTask} for the given source, marking the Polymer errors as
   * being in-process.
   * 
   * @param source the source whose content is to be resolved
   * @param htmlEntry the entry for the source
   * @return task data representing the created task
   */
  private TaskData createPolymerResolveHtmlTask(Source source, HtmlEntry htmlEntry) {
    if (htmlEntry.getState(HtmlEntry.RESOLVED_UNIT) != CacheState.VALID) {
      return createResolveHtmlTask(source, htmlEntry);
    }
    HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
    htmlCopy.setState(HtmlEntry.POLYMER_RESOLUTION_ERRORS, CacheState.IN_PROCESS);
    cache.put(source, htmlCopy);
    return new TaskData(new PolymerResolveHtmlTask(
        this,
        source,
        htmlCopy.getModificationTime(),
        htmlEntry.getValue(SourceEntry.LINE_INFO),
        htmlCopy.getValue(HtmlEntry.RESOLVED_UNIT)), false);
  }

  /**
   * Create a {@link ResolveAngularComponentTemplateTask} for the given source, marking the angular
   * errors as being in-process.
   * 
   * @param source the source whose content is to be resolved
   * @param htmlEntry the entry for the source
   * @return task data representing the created task
   */
  private TaskData createResolveAngularComponentTemplateTask(Source source, HtmlEntry htmlEntry) {
    if (htmlEntry.getState(HtmlEntry.RESOLVED_UNIT) != CacheState.VALID) {
      return createResolveHtmlTask(source, htmlEntry);
    }
    AngularApplication application = htmlEntry.getValue(HtmlEntry.ANGULAR_APPLICATION);
    AngularComponentElement component = htmlEntry.getValue(HtmlEntry.ANGULAR_COMPONENT);
    HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
    htmlCopy.setState(HtmlEntry.ANGULAR_ERRORS, CacheState.IN_PROCESS);
    cache.put(source, htmlCopy);
    return new TaskData(new ResolveAngularComponentTemplateTask(
        this,
        source,
        htmlCopy.getModificationTime(),
        htmlCopy.getValue(HtmlEntry.RESOLVED_UNIT),
        component,
        application), false);
  }

  /**
   * Create a {@link ResolveAngularEntryHtmlTask} for the given source, marking the angular entry as
   * being in-process.
   * 
   * @param source the source whose content is to be resolved
   * @param htmlEntry the entry for the source
   * @return task data representing the created task
   */
  private TaskData createResolveAngularEntryHtmlTask(Source source, HtmlEntry htmlEntry) {
    if (htmlEntry.getState(HtmlEntry.RESOLVED_UNIT) != CacheState.VALID) {
      return createResolveHtmlTask(source, htmlEntry);
    }
    HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
    htmlCopy.setState(HtmlEntry.ANGULAR_ENTRY, CacheState.IN_PROCESS);
    cache.put(source, htmlCopy);
    return new TaskData(new ResolveAngularEntryHtmlTask(
        this,
        source,
        htmlCopy.getModificationTime(),
        htmlCopy.getValue(HtmlEntry.RESOLVED_UNIT)), false);
  }

  /**
   * Create a {@link ResolveDartLibraryTask} for the given source, marking ? as being in-process.
   * 
   * @param source the source whose content is to be resolved
   * @param dartEntry the entry for the source
   * @return task data representing the created task
   */
  private TaskData createResolveDartLibraryTask(Source source, DartEntry dartEntry) {
    try {
      CycleBuilder builder = new CycleBuilder();
      builder.computeCycleContaining(source);
      TaskData taskData = builder.getTaskData();
      if (taskData != null) {
        return taskData;
      }
      return new TaskData(new ResolveDartLibraryCycleTask(
          this,
          source,
          source,
          builder.getLibrariesInCycle()), false);
    } catch (AnalysisException exception) {
      DartEntryImpl dartCopy = dartEntry.getWritableCopy();
      dartCopy.recordResolutionError(exception);
      cache.put(source, dartCopy);
      AnalysisEngine.getInstance().getLogger().logError(
          "Internal error trying to create a ResolveDartLibraryTask",
          exception);
    }
    return new TaskData(null, false);
  }

  /**
   * Create a {@link ResolveHtmlTask} for the given source, marking the resolved unit as being
   * in-process.
   * 
   * @param source the source whose content is to be resolved
   * @param htmlEntry the entry for the source
   * @return task data representing the created task
   */
  private TaskData createResolveHtmlTask(Source source, HtmlEntry htmlEntry) {
    if (htmlEntry.getState(HtmlEntry.PARSED_UNIT) != CacheState.VALID) {
      return createParseHtmlTask(source, htmlEntry);
    }
    HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
    htmlCopy.setState(HtmlEntry.RESOLVED_UNIT, CacheState.IN_PROCESS);
    cache.put(source, htmlCopy);
    return new TaskData(new ResolveHtmlTask(
        this,
        source,
        htmlCopy.getModificationTime(),
        htmlCopy.getValue(HtmlEntry.PARSED_UNIT)), false);
  }

  /**
   * Create a {@link ScanDartTask} for the given source, marking the scan errors as being
   * in-process.
   * 
   * @param source the source whose content is to be scanned
   * @param dartEntry the entry for the source
   * @return task data representing the created task
   */
  private TaskData createScanDartTask(Source source, DartEntry dartEntry) {
    if (dartEntry.getState(SourceEntry.CONTENT) != CacheState.VALID) {
      return createGetContentTask(source, dartEntry);
    }
    CharSequence content = dartEntry.getValue(SourceEntry.CONTENT);
    DartEntryImpl dartCopy = dartEntry.getWritableCopy();
    dartCopy.setState(SourceEntry.CONTENT, CacheState.FLUSHED);
    dartCopy.setState(DartEntry.SCAN_ERRORS, CacheState.IN_PROCESS);
    cache.put(source, dartCopy);
    return new TaskData(
        new ScanDartTask(this, source, dartCopy.getModificationTime(), content),
        false);
  }

  /**
   * Create a source information object suitable for the given source. Return the source information
   * object that was created, or {@code null} if the source should not be tracked by this context.
   * 
   * @param source the source for which an information object is being created
   * @param explicitlyAdded {@code true} if the source was explicitly added to the context
   * @return the source information object that was created
   */
  private SourceEntry createSourceEntry(Source source, boolean explicitlyAdded) {
    String name = source.getShortName();
    if (AnalysisEngine.isHtmlFileName(name)) {
      HtmlEntryImpl htmlEntry = new HtmlEntryImpl();
      htmlEntry.setModificationTime(getModificationStamp(source));
      htmlEntry.setExplicitlyAdded(explicitlyAdded);
      cache.put(source, htmlEntry);
      return htmlEntry;
    } else {
      DartEntryImpl dartEntry = new DartEntryImpl();
      dartEntry.setModificationTime(getModificationStamp(source));
      dartEntry.setExplicitlyAdded(explicitlyAdded);
      cache.put(source, dartEntry);
      return dartEntry;
    }
  }

  /**
   * Return a string with debugging information about the given source (the full name and
   * modification stamp of the source).
   * 
   * @param source the source for which a debugging string is to be produced
   * @return debugging information about the given source
   */
  private String debuggingString(Source source) {
    return "'" + source.getFullName() + "' [" + getModificationStamp(source) + "]";
  }

  /**
   * Return an array containing all of the change notices that are waiting to be returned. If there
   * are no notices, then return either {@code null} or an empty array, depending on the value of
   * the argument.
   * 
   * @param nullIfEmpty {@code true} if {@code null} should be returned when there are no notices
   * @return the change notices that are waiting to be returned
   */
  private ChangeNotice[] getChangeNotices(boolean nullIfEmpty) {
    synchronized (cacheLock) {
      if (pendingNotices.isEmpty()) {
        if (nullIfEmpty) {
          return null;
        }
        return ChangeNoticeImpl.EMPTY_ARRAY;
      }
      ChangeNotice[] notices = pendingNotices.values().toArray(
          new ChangeNotice[pendingNotices.size()]);
      pendingNotices.clear();
      return notices;
    }
  }

  /**
   * Given a source for a Dart file and the library that contains it, return the data represented by
   * the given descriptor that is associated with that source. This method assumes that the data can
   * be produced by generating hints for the library if it is not already cached.
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
   * 
   * @param unitSource the source representing the Dart file
   * @param librarySource the source representing the library containing the Dart file
   * @param dartEntry the entry representing the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be
   *           resolved
   */
  private <E> E getDartHintData(Source unitSource, Source librarySource, DartEntry dartEntry,
      DataDescriptor<E> descriptor) throws AnalysisException {
    dartEntry = cacheDartHintData(unitSource, librarySource, dartEntry, descriptor);
    if (descriptor == DartEntry.ELEMENT) {
      return dartEntry.getValue(descriptor);
    }
    return dartEntry.getValueInLibrary(descriptor, librarySource);
  }

  /**
   * Given a source for a Dart file, return the data represented by the given descriptor that is
   * associated with that source. This method assumes that the data can be produced by parsing the
   * source if it is not already cached.
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
   * 
   * @param source the source representing the Dart file
   * @param dartEntry the cache entry associated with the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be parsed
   */
  @SuppressWarnings("unchecked")
  private <E> E getDartParseData(Source source, DartEntry dartEntry, DataDescriptor<E> descriptor)
      throws AnalysisException {
    dartEntry = cacheDartParseData(source, dartEntry, descriptor);
    if (descriptor == DartEntry.PARSED_UNIT) {
      accessedAst(source);
      return (E) dartEntry.getAnyParsedCompilationUnit();
    }
    return dartEntry.getValue(descriptor);
  }

  /**
   * Given a source for a Dart file, return the data represented by the given descriptor that is
   * associated with that source, or the given default value if the source is not a Dart file. This
   * method assumes that the data can be produced by parsing the source if it is not already cached.
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
   * 
   * @param source the source representing the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @param defaultValue the value to be returned if the source is not a Dart file
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be parsed
   */
  private <E> E getDartParseData(Source source, DataDescriptor<E> descriptor, E defaultValue)
      throws AnalysisException {
    DartEntry dartEntry = getReadableDartEntry(source);
    if (dartEntry == null) {
      return defaultValue;
    }
    try {
      return getDartParseData(source, dartEntry, descriptor);
    } catch (ObsoleteSourceAnalysisException exception) {
      AnalysisEngine.getInstance().getLogger().logInformation(
          "Could not compute " + descriptor.toString(),
          exception);
      return defaultValue;
    }
  }

  /**
   * Given a source for a Dart file and the library that contains it, return the data represented by
   * the given descriptor that is associated with that source. This method assumes that the data can
   * be produced by resolving the source in the context of the library if it is not already cached.
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
   * 
   * @param unitSource the source representing the Dart file
   * @param librarySource the source representing the library containing the Dart file
   * @param dartEntry the entry representing the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be
   *           resolved
   */
  private <E> E getDartResolutionData(Source unitSource, Source librarySource, DartEntry dartEntry,
      DataDescriptor<E> descriptor) throws AnalysisException {
    dartEntry = cacheDartResolutionData(unitSource, librarySource, dartEntry, descriptor);
    if (descriptor == DartEntry.ELEMENT) {
      return dartEntry.getValue(descriptor);
    } else if (descriptor == DartEntry.RESOLVED_UNIT) {
      accessedAst(unitSource);
    }
    return dartEntry.getValueInLibrary(descriptor, librarySource);
  }

  /**
   * Given a source for a Dart file and the library that contains it, return the data represented by
   * the given descriptor that is associated with that source, or the given default value if the
   * source is not a Dart file. This method assumes that the data can be produced by resolving the
   * source in the context of the library if it is not already cached.
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
   * 
   * @param unitSource the source representing the Dart file
   * @param librarySource the source representing the library containing the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @param defaultValue the value to be returned if the source is not a Dart file
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be
   *           resolved
   */
  private <E> E getDartResolutionData(Source unitSource, Source librarySource,
      DataDescriptor<E> descriptor, E defaultValue) throws AnalysisException {
    DartEntry dartEntry = getReadableDartEntry(unitSource);
    if (dartEntry == null) {
      return defaultValue;
    }
    try {
      return getDartResolutionData(unitSource, librarySource, dartEntry, descriptor);
    } catch (ObsoleteSourceAnalysisException exception) {
      AnalysisEngine.getInstance().getLogger().logInformation(
          "Could not compute " + descriptor.toString(),
          exception);
      return defaultValue;
    }
  }

  /**
   * Given a source for a Dart file, return the data represented by the given descriptor that is
   * associated with that source. This method assumes that the data can be produced by scanning the
   * source if it is not already cached.
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
   * 
   * @param source the source representing the Dart file
   * @param dartEntry the cache entry associated with the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be scanned
   */
  private <E> E getDartScanData(Source source, DartEntry dartEntry, DataDescriptor<E> descriptor)
      throws AnalysisException {
    dartEntry = cacheDartScanData(source, dartEntry, descriptor);
    return dartEntry.getValue(descriptor);
  }

  /**
   * Given a source for a Dart file, return the data represented by the given descriptor that is
   * associated with that source, or the given default value if the source is not a Dart file. This
   * method assumes that the data can be produced by scanning the source if it is not already
   * cached.
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
   * 
   * @param source the source representing the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @param defaultValue the value to be returned if the source is not a Dart file
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be scanned
   */
  private <E> E getDartScanData(Source source, DataDescriptor<E> descriptor, E defaultValue)
      throws AnalysisException {
    DartEntry dartEntry = getReadableDartEntry(source);
    if (dartEntry == null) {
      return defaultValue;
    }
    try {
      return getDartScanData(source, dartEntry, descriptor);
    } catch (ObsoleteSourceAnalysisException exception) {
      AnalysisEngine.getInstance().getLogger().logInformation(
          "Could not compute " + descriptor.toString(),
          exception);
      return defaultValue;
    }
  }

  /**
   * Given a source for a Dart file and the library that contains it, return the data represented by
   * the given descriptor that is associated with that source. This method assumes that the data can
   * be produced by verifying the source within the given library if it is not already cached.
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
   * 
   * @param unitSource the source representing the Dart file
   * @param librarySource the source representing the library containing the Dart file
   * @param dartEntry the entry representing the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be
   *           resolved
   */
  private <E> E getDartVerificationData(Source unitSource, Source librarySource,
      DartEntry dartEntry, DataDescriptor<E> descriptor) throws AnalysisException {
    dartEntry = cacheDartVerificationData(unitSource, librarySource, dartEntry, descriptor);
    return dartEntry.getValueInLibrary(descriptor, librarySource);
  }

  /**
   * Given a source for an HTML file, return the data represented by the given descriptor that is
   * associated with that source, or the given default value if the source is not an HTML file. This
   * method assumes that the data can be produced by parsing the source if it is not already cached.
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
   * 
   * @param source the source representing the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @param defaultValue the value to be returned if the source is not an HTML file
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be parsed
   */
  @SuppressWarnings("unchecked")
  private <E> E getHtmlParseData(Source source, DataDescriptor<E> descriptor, E defaultValue)
      throws AnalysisException {
    HtmlEntry htmlEntry = getReadableHtmlEntry(source);
    if (htmlEntry == null) {
      return defaultValue;
    }
    htmlEntry = cacheHtmlParseData(source, htmlEntry, descriptor);
    if (descriptor == HtmlEntry.PARSED_UNIT) {
      accessedAst(source);
      return (E) htmlEntry.getAnyParsedUnit();
    }
    return htmlEntry.getValue(descriptor);
  }

  /**
   * Given a source for an HTML file, return the data represented by the given descriptor that is
   * associated with that source, or the given default value if the source is not an HTML file. This
   * method assumes that the data can be produced by resolving the source if it is not already
   * cached.
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
   * 
   * @param source the source representing the HTML file
   * @param descriptor the descriptor representing the data to be returned
   * @param defaultValue the value to be returned if the source is not an HTML file
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be
   *           resolved
   */
  private <E> E getHtmlResolutionData(Source source, DataDescriptor<E> descriptor, E defaultValue)
      throws AnalysisException {
    HtmlEntry htmlEntry = getReadableHtmlEntry(source);
    if (htmlEntry == null) {
      return defaultValue;
    }
    try {
      return getHtmlResolutionData(source, htmlEntry, descriptor);
    } catch (ObsoleteSourceAnalysisException exception) {
      AnalysisEngine.getInstance().getLogger().logInformation(
          "Could not compute " + descriptor.toString(),
          exception);
      return defaultValue;
    }
  }

  /**
   * Given a source for an HTML file, return the data represented by the given descriptor that is
   * associated with that source. This method assumes that the data can be produced by resolving the
   * source if it is not already cached.
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
   * 
   * @param source the source representing the HTML file
   * @param htmlEntry the entry representing the HTML file
   * @param descriptor the descriptor representing the data to be returned
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be
   *           resolved
   */
  private <E> E getHtmlResolutionData(Source source, HtmlEntry htmlEntry,
      DataDescriptor<E> descriptor) throws AnalysisException {
    htmlEntry = cacheHtmlResolutionData(source, htmlEntry, descriptor);
    if (descriptor == HtmlEntry.RESOLVED_UNIT) {
      accessedAst(source);
    }
    return htmlEntry.getValue(descriptor);
  }

  /**
   * Look through the cache for a task that needs to be performed. Return the task that was found,
   * or {@code null} if there is no more work to be done.
   * 
   * @return the next task that needs to be performed
   */
  private AnalysisTask getNextAnalysisTask() {
    synchronized (cacheLock) {
      boolean hintsEnabled = options.getHint();
      boolean hasBlockedTask = false;
      //
      // Look for incremental analysis
      //
      if (incrementalAnalysisCache != null && incrementalAnalysisCache.hasWork()) {
        AnalysisTask task = new IncrementalAnalysisTask(this, incrementalAnalysisCache);
        incrementalAnalysisCache = null;
        return task;
      }
      //
      // Look for a priority source that needs to be analyzed.
      //
      int priorityCount = priorityOrder.length;
      for (int i = 0; i < priorityCount; i++) {
        Source source = priorityOrder[i];
        TaskData taskData = getNextAnalysisTaskForSource(
            source,
            cache.get(source),
            true,
            hintsEnabled);
        AnalysisTask task = taskData.getTask();
        if (task != null) {
          return task;
        } else if (taskData.isBlocked()) {
          hasBlockedTask = true;
        }
      }
      if (neededForResolution != null) {
        ArrayList<Source> sourcesToRemove = new ArrayList<Source>();
        for (Source source : neededForResolution) {
          SourceEntry sourceEntry = cache.get(source);
          if (sourceEntry instanceof DartEntry) {
            DartEntry dartEntry = (DartEntry) sourceEntry;
            if (!dartEntry.hasResolvableCompilationUnit()) {
              if (dartEntry.getState(DartEntry.PARSED_UNIT) == CacheState.ERROR) {
                sourcesToRemove.add(source);
              } else {
                TaskData taskData = createParseDartTask(source, dartEntry);
                AnalysisTask task = taskData.getTask();
                if (task != null) {
                  return task;
                } else if (taskData.isBlocked()) {
                  hasBlockedTask = true;
                }
              }
            }
          }
        }
        int count = sourcesToRemove.size();
        for (int i = 0; i < count; i++) {
          neededForResolution.remove(sourcesToRemove.get(i));
        }
      }
      //
      // Look for a non-priority source that needs to be analyzed.
      //
      ArrayList<Source> sourcesToRemove = new ArrayList<Source>();
      WorkManager.WorkIterator sources = workManager.iterator();
      while (sources.hasNext()) {
        Source source = sources.next();
        TaskData taskData = getNextAnalysisTaskForSource(
            source,
            cache.get(source),
            false,
            hintsEnabled);
        AnalysisTask task = taskData.getTask();
        if (task != null) {
          int count = sourcesToRemove.size();
          for (int i = 0; i < count; i++) {
            workManager.remove(sourcesToRemove.get(i));
          }
          return task;
        } else if (taskData.isBlocked()) {
          hasBlockedTask = true;
        } else {
          sourcesToRemove.add(source);
        }
      }
      int count = sourcesToRemove.size();
      for (int i = 0; i < count; i++) {
        workManager.remove(sourcesToRemove.get(i));
      }
//      //
//      // Look for a non-priority source that needs to be analyzed and was missed by the loop above.
//      //
//      for (Map.Entry<Source, SourceEntry> entry : cache.entrySet()) {
//        source = entry.getKey();
//        TaskData taskData = getNextAnalysisTaskForSource(source, entry.getValue(), false, hintsEnabled);
//        AnalysisTask task = taskData.getTask();
//        if (task != null) {
//          System.out.println("Failed to analyze " + source.getFullName());
//          return task;
//        }
//      }
      if (hasBlockedTask) {
        // All of the analysis work is blocked waiting for an asynchronous task to complete.
        return WaitForAsyncTask.getInstance();
      }
      return null;
    }
  }

  /**
   * Look at the given source to see whether a task needs to be performed related to it. Return the
   * task that should be performed, or {@code null} if there is no more work to be done for the
   * source.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source to be checked
   * @param sourceEntry the cache entry associated with the source
   * @param isPriority {@code true} if the source is a priority source
   * @param hintsEnabled {@code true} if hints are currently enabled
   * @return the next task that needs to be performed for the given source
   */
  private TaskData getNextAnalysisTaskForSource(Source source, SourceEntry sourceEntry,
      boolean isPriority, boolean hintsEnabled) {
    if (sourceEntry == null) {
      return new TaskData(null, false);
    }

    CacheState contentState = sourceEntry.getState(SourceEntry.CONTENT);
    if (contentState == CacheState.INVALID) {
      return createGetContentTask(source, sourceEntry);
    } else if (contentState == CacheState.IN_PROCESS) {
      // We are already in the process of getting the content. There's nothing else we can do with
      // this source until that's complete.
      return new TaskData(null, true);
    } else if (contentState == CacheState.ERROR) {
      // We have done all of the analysis we can for this source because we cannot get its content.
      return new TaskData(null, false);
    }

    if (sourceEntry instanceof DartEntry) {
      DartEntry dartEntry = (DartEntry) sourceEntry;

      CacheState scanErrorsState = dartEntry.getState(DartEntry.SCAN_ERRORS);
      if (scanErrorsState == CacheState.INVALID
          || (isPriority && scanErrorsState == CacheState.FLUSHED)) {
        return createScanDartTask(source, dartEntry);
      }

      CacheState parseErrorsState = dartEntry.getState(DartEntry.PARSE_ERRORS);
      if (parseErrorsState == CacheState.INVALID
          || (isPriority && parseErrorsState == CacheState.FLUSHED)) {
        return createParseDartTask(source, dartEntry);
      }
      if (isPriority && parseErrorsState != CacheState.ERROR) {
        if (!dartEntry.hasResolvableCompilationUnit()) {
          return createParseDartTask(source, dartEntry);
        }
      }

      SourceKind kind = dartEntry.getValue(DartEntry.SOURCE_KIND);
      if (kind == SourceKind.UNKNOWN) {
        return createParseDartTask(source, dartEntry);
      } else if (kind == SourceKind.LIBRARY) {
        CacheState elementState = dartEntry.getState(DartEntry.ELEMENT);
        if (elementState == CacheState.INVALID) {
          return createResolveDartLibraryTask(source, dartEntry);
        }
      }

      Source[] librariesContaining = dartEntry.getValue(DartEntry.CONTAINING_LIBRARIES);
      for (Source librarySource : librariesContaining) {
        SourceEntry librarySourceEntry = cache.get(librarySource);
        if (librarySourceEntry instanceof DartEntry) {
          DartEntry libraryEntry = (DartEntry) librarySourceEntry;
          CacheState elementState = libraryEntry.getState(DartEntry.ELEMENT);
          if (elementState == CacheState.INVALID
              || (isPriority && elementState == CacheState.FLUSHED)) {
            //return createResolveDartLibraryTask(librarySource, (DartEntry) libraryEntry);
            DartEntryImpl libraryCopy = libraryEntry.getWritableCopy();
            libraryCopy.setState(DartEntry.ELEMENT, CacheState.IN_PROCESS);
            cache.put(librarySource, libraryCopy);
            return new TaskData(new ResolveDartLibraryTask(this, source, librarySource), false);
          }
          CacheState resolvedUnitState = dartEntry.getStateInLibrary(
              DartEntry.RESOLVED_UNIT,
              librarySource);
          if (resolvedUnitState == CacheState.INVALID
              || (isPriority && resolvedUnitState == CacheState.FLUSHED)) {
            //
            // The commented out lines below are an optimization that doesn't quite work yet. The
            // problem is that if the source was not resolved because it wasn't part of any library,
            // then there won't be any elements in the element model that we can use to resolve it.
            //
            //LibraryElement libraryElement = libraryEntry.getValue(DartEntry.ELEMENT);
            //if (libraryElement != null) {
            //  return new ResolveDartUnitTask(this, source, libraryElement);
            //}
            // Possibly replace with: return createResolveDartLibraryTask(librarySource, (DartEntry) libraryEntry);
            DartEntryImpl dartCopy = dartEntry.getWritableCopy();
            dartCopy.setStateInLibrary(
                DartEntry.RESOLVED_UNIT,
                librarySource,
                CacheState.IN_PROCESS);
            cache.put(source, dartCopy);
            return new TaskData(new ResolveDartLibraryTask(this, source, librarySource), false);
          }
          if (generateSdkErrors || !source.isInSystemLibrary()) {
            CacheState verificationErrorsState = dartEntry.getStateInLibrary(
                DartEntry.VERIFICATION_ERRORS,
                librarySource);
            if (verificationErrorsState == CacheState.INVALID
                || (isPriority && verificationErrorsState == CacheState.FLUSHED)) {
              return createGenerateDartErrorsTask(source, dartEntry, librarySource, libraryEntry);
            }
            if (hintsEnabled) {
              CacheState hintsState = dartEntry.getStateInLibrary(DartEntry.HINTS, librarySource);
              if (hintsState == CacheState.INVALID
                  || (isPriority && hintsState == CacheState.FLUSHED)) {
                return createGenerateDartHintsTask(source, dartEntry, librarySource, libraryEntry);
              }
            }
          }
        }
      }
    } else if (sourceEntry instanceof HtmlEntry) {
      HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;

      CacheState parseErrorsState = htmlEntry.getState(HtmlEntry.PARSE_ERRORS);
      if (parseErrorsState == CacheState.INVALID
          || (isPriority && parseErrorsState == CacheState.FLUSHED)) {
        return createParseHtmlTask(source, htmlEntry);
      }
      if (isPriority && parseErrorsState != CacheState.ERROR) {
        HtmlUnit parsedUnit = htmlEntry.getAnyParsedUnit();
        if (parsedUnit == null) {
          return createParseHtmlTask(source, htmlEntry);
        }
      }

      CacheState resolvedUnitState = htmlEntry.getState(HtmlEntry.RESOLVED_UNIT);
      if (resolvedUnitState == CacheState.INVALID
          || (isPriority && resolvedUnitState == CacheState.FLUSHED)) {
        return createResolveHtmlTask(source, htmlEntry);
      }
      //
      // Angular support
      //
      if (options.getAnalyzeAngular()) {
        // Try to resolve the HTML as an Angular entry point.
        CacheState angularEntryState = htmlEntry.getState(HtmlEntry.ANGULAR_ENTRY);
        if (angularEntryState == CacheState.INVALID
            || (isPriority && angularEntryState == CacheState.FLUSHED)) {
          return createResolveAngularEntryHtmlTask(source, htmlEntry);
        }
        // Try to resolve the HTML as an Angular application part.
        CacheState angularErrorsState = htmlEntry.getState(HtmlEntry.ANGULAR_ERRORS);
        if (angularErrorsState == CacheState.INVALID
            || (isPriority && angularErrorsState == CacheState.FLUSHED)) {
          return createResolveAngularComponentTemplateTask(source, htmlEntry);
        }
      }
      //
      // Polymer support
      //
      if (options.getAnalyzePolymer()) {
        // Build elements.
        CacheState polymerBuildErrorsState = htmlEntry.getState(HtmlEntry.POLYMER_BUILD_ERRORS);
        if (polymerBuildErrorsState == CacheState.INVALID
            || (isPriority && polymerBuildErrorsState == CacheState.FLUSHED)) {
          return createPolymerBuildHtmlTask(source, htmlEntry);
        }
        // Resolve references.
        CacheState polymerResolutionErrorsState = htmlEntry.getState(HtmlEntry.POLYMER_RESOLUTION_ERRORS);
        if (polymerResolutionErrorsState == CacheState.INVALID
            || (isPriority && polymerResolutionErrorsState == CacheState.FLUSHED)) {
          return createPolymerResolveHtmlTask(source, htmlEntry);
        }
      }
    }
    return new TaskData(null, false);
  }

  /**
   * Return a change notice for the given source, creating one if one does not already exist.
   * 
   * @param source the source for which changes are being reported
   * @return a change notice for the given source
   */
  private ChangeNoticeImpl getNotice(Source source) {
    ChangeNoticeImpl notice = pendingNotices.get(source);
    if (notice == null) {
      notice = new ChangeNoticeImpl(source);
      pendingNotices.put(source, notice);
    }
    return notice;
  }

  /**
   * Return the cache entry associated with the given source, or {@code null} if the source is not a
   * Dart file.
   * 
   * @param source the source for which a cache entry is being sought
   * @return the source cache entry associated with the given source
   */
  private DartEntry getReadableDartEntry(Source source) {
    synchronized (cacheLock) {
      SourceEntry sourceEntry = cache.get(source);
      if (sourceEntry == null) {
        sourceEntry = createSourceEntry(source, false);
      }
      if (sourceEntry instanceof DartEntry) {
        return (DartEntry) sourceEntry;
      }
      return null;
    }
  }

  /**
   * Return the cache entry associated with the given source, or {@code null} if the source is not
   * an HTML file.
   * 
   * @param source the source for which a cache entry is being sought
   * @return the source cache entry associated with the given source
   */
  private HtmlEntry getReadableHtmlEntry(Source source) {
    synchronized (cacheLock) {
      SourceEntry sourceEntry = cache.get(source);
      if (sourceEntry == null) {
        sourceEntry = createSourceEntry(source, false);
      }
      if (sourceEntry instanceof HtmlEntry) {
        return (HtmlEntry) sourceEntry;
      }
      return null;
    }
  }

  /**
   * Return the cache entry associated with the given source, creating it if necessary.
   * 
   * @param source the source for which a cache entry is being sought
   * @return the source cache entry associated with the given source
   */
  private SourceEntry getReadableSourceEntry(Source source) {
    synchronized (cacheLock) {
      SourceEntry sourceEntry = cache.get(source);
      if (sourceEntry == null) {
        sourceEntry = createSourceEntry(source, false);
      }
      return sourceEntry;
    }
  }

  /**
   * Return the cache entry associated with the given source, or {@code null} if there is no entry
   * associated with the source.
   * 
   * @param source the source for which a cache entry is being sought
   * @return the source cache entry associated with the given source
   */
  private SourceEntry getReadableSourceEntryOrNull(Source source) {
    synchronized (cacheLock) {
      return cache.get(source);
    }
  }

  /**
   * Return a resolved compilation unit corresponding to the given element in the given library, or
   * {@code null} if the information is not cached.
   * 
   * @param element the element representing the compilation unit
   * @param librarySource the source representing the library containing the unit
   * @return the specified resolved compilation unit
   */
  private TimestampedData<CompilationUnit> getResolvedUnit(CompilationUnitElement element,
      Source librarySource) {
    SourceEntry sourceEntry = cache.get(element.getSource());
    if (sourceEntry instanceof DartEntry) {
      DartEntry dartEntry = (DartEntry) sourceEntry;
      if (dartEntry.getStateInLibrary(DartEntry.RESOLVED_UNIT, librarySource) == CacheState.VALID) {
        return new TimestampedData<CompilationUnit>(
            dartEntry.getModificationTime(),
            dartEntry.getValueInLibrary(DartEntry.RESOLVED_UNIT, librarySource));
      }
    }
    return null;
  }

  /**
   * Return an array containing all of the sources known to this context that have the given kind.
   * 
   * @param kind the kind of sources to be returned
   * @return all of the sources known to this context that have the given kind
   */
  private Source[] getSources(SourceKind kind) {
    ArrayList<Source> sources = new ArrayList<Source>();
    synchronized (cacheLock) {
      MapIterator<Source, SourceEntry> iterator = cache.iterator();
      while (iterator.moveNext()) {
        if (iterator.getValue().getKind() == kind) {
          sources.add(iterator.getKey());
        }
      }
    }
    return sources.toArray(new Source[sources.size()]);
  }

  /**
   * Look at the given source to see whether a task needs to be performed related to it. If so, add
   * the source to the set of sources that need to be processed. This method duplicates, and must
   * therefore be kept in sync with,
   * {@link #getNextAnalysisTask(Source, SourceEntry, boolean, boolean)}. This method is intended to
   * be used for testing purposes only.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source to be checked
   * @param sourceEntry the cache entry associated with the source
   * @param isPriority {@code true} if the source is a priority source
   * @param hintsEnabled {@code true} if hints are currently enabled
   * @param sources the set to which sources should be added
   */
  private void getSourcesNeedingProcessing(Source source, SourceEntry sourceEntry,
      boolean isPriority, boolean hintsEnabled, HashSet<Source> sources) {
    if (sourceEntry instanceof DartEntry) {
      DartEntry dartEntry = (DartEntry) sourceEntry;
      CacheState scanErrorsState = dartEntry.getState(DartEntry.SCAN_ERRORS);
      if (scanErrorsState == CacheState.INVALID
          || (isPriority && scanErrorsState == CacheState.FLUSHED)) {
        sources.add(source);
        return;
      }
      CacheState parseErrorsState = dartEntry.getState(DartEntry.PARSE_ERRORS);
      if (parseErrorsState == CacheState.INVALID
          || (isPriority && parseErrorsState == CacheState.FLUSHED)) {
        sources.add(source);
        return;
      }
      if (isPriority) {
        if (!dartEntry.hasResolvableCompilationUnit()) {
          sources.add(source);
          return;
        }
      }
      for (Source librarySource : getLibrariesContaining(source)) {
        SourceEntry libraryEntry = cache.get(librarySource);
        if (libraryEntry instanceof DartEntry) {
          CacheState elementState = libraryEntry.getState(DartEntry.ELEMENT);
          if (elementState == CacheState.INVALID
              || (isPriority && elementState == CacheState.FLUSHED)) {
            sources.add(source);
            return;
          }
          CacheState resolvedUnitState = dartEntry.getStateInLibrary(
              DartEntry.RESOLVED_UNIT,
              librarySource);
          if (resolvedUnitState == CacheState.INVALID
              || (isPriority && resolvedUnitState == CacheState.FLUSHED)) {
            LibraryElement libraryElement = libraryEntry.getValue(DartEntry.ELEMENT);
            if (libraryElement != null) {
              sources.add(source);
              return;
            }
          }
          CacheState verificationErrorsState = dartEntry.getStateInLibrary(
              DartEntry.VERIFICATION_ERRORS,
              librarySource);
          if (verificationErrorsState == CacheState.INVALID
              || (isPriority && verificationErrorsState == CacheState.FLUSHED)) {
            LibraryElement libraryElement = libraryEntry.getValue(DartEntry.ELEMENT);
            if (libraryElement != null) {
              sources.add(source);
              return;
            }
          }
          if (hintsEnabled) {
            CacheState hintsState = dartEntry.getStateInLibrary(DartEntry.HINTS, librarySource);
            if (hintsState == CacheState.INVALID
                || (isPriority && hintsState == CacheState.FLUSHED)) {
              LibraryElement libraryElement = libraryEntry.getValue(DartEntry.ELEMENT);
              if (libraryElement != null) {
                sources.add(source);
                return;
              }
            }
          }
        }
      }
    } else if (sourceEntry instanceof HtmlEntry) {
      HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
      CacheState parsedUnitState = htmlEntry.getState(HtmlEntry.PARSED_UNIT);
      if (parsedUnitState == CacheState.INVALID
          || (isPriority && parsedUnitState == CacheState.FLUSHED)) {
        sources.add(source);
        return;
      }
      CacheState resolvedUnitState = htmlEntry.getState(HtmlEntry.RESOLVED_UNIT);
      if (resolvedUnitState == CacheState.INVALID
          || (isPriority && resolvedUnitState == CacheState.FLUSHED)) {
        sources.add(source);
        return;
      }
      // Angular
      if (options.getAnalyzeAngular()) {
        CacheState angularErrorsState = htmlEntry.getState(HtmlEntry.ANGULAR_ERRORS);
        if (angularErrorsState == CacheState.INVALID
            || (isPriority && angularErrorsState == CacheState.FLUSHED)) {
          AngularApplication entryInfo = htmlEntry.getValue(HtmlEntry.ANGULAR_ENTRY);
          if (entryInfo != null) {
            sources.add(source);
            return;
          }
          AngularApplication applicationInfo = htmlEntry.getValue(HtmlEntry.ANGULAR_APPLICATION);
          if (applicationInfo != null) {
            AngularComponentElement component = htmlEntry.getValue(HtmlEntry.ANGULAR_COMPONENT);
            if (component != null) {
              sources.add(source);
              return;
            }
          }
        }
      }
      // Polymer
      if (options.getAnalyzePolymer()) {
        // Elements building.
        CacheState polymerBuildErrorsState = htmlEntry.getState(HtmlEntry.POLYMER_BUILD_ERRORS);
        if (polymerBuildErrorsState == CacheState.INVALID
            || (isPriority && polymerBuildErrorsState == CacheState.FLUSHED)) {
          sources.add(source);
        }
        // Resolution.
        CacheState polymerResolutionErrorsState = htmlEntry.getState(HtmlEntry.POLYMER_RESOLUTION_ERRORS);
        if (polymerResolutionErrorsState == CacheState.INVALID
            || (isPriority && polymerResolutionErrorsState == CacheState.FLUSHED)) {
          sources.add(source);
        }
      }
    }
  }

  /**
   * Invalidate all of the resolution results computed by this context.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   */
  private void invalidateAllLocalResolutionInformation() {
    HashMap<Source, Source[]> oldPartMap = new HashMap<Source, Source[]>();
    MapIterator<Source, SourceEntry> iterator = privatePartition.iterator();
    while (iterator.moveNext()) {
      Source source = iterator.getKey();
      SourceEntry sourceEntry = iterator.getValue();
      if (sourceEntry instanceof HtmlEntry) {
        HtmlEntryImpl htmlCopy = ((HtmlEntry) sourceEntry).getWritableCopy();
        htmlCopy.invalidateAllResolutionInformation();
        iterator.setValue(htmlCopy);
      } else if (sourceEntry instanceof DartEntry) {
        DartEntry dartEntry = (DartEntry) sourceEntry;
        oldPartMap.put(source, dartEntry.getValue(DartEntry.INCLUDED_PARTS));
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        dartCopy.invalidateAllResolutionInformation();
        iterator.setValue(dartCopy);
        workManager.add(source, SourcePriority.UNKNOWN);
      }
    }
    removeFromPartsUsingMap(oldPartMap);
  }

  /**
   * In response to a change to Angular entry point {@link HtmlElement}, invalidate any results that
   * depend on it.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * <p>
   * <b>Note:</b> Any cache entries that were accessed before this method was invoked must be
   * re-accessed after this method returns.
   * 
   * @param entryCopy the {@link HtmlEntryImpl} of the (maybe) Angular entry point being invalidated
   */
  private void invalidateAngularResolution(HtmlEntryImpl entryCopy) {
    AngularApplication application = entryCopy.getValue(HtmlEntry.ANGULAR_ENTRY);
    if (application == null) {
      return;
    }
    angularApplications.remove(application);
    // invalidate Entry
    entryCopy.setState(HtmlEntry.ANGULAR_ENTRY, CacheState.INVALID);
    // reset HTML sources
    AngularElement[] oldAngularElements = application.getElements();
    for (AngularElement angularElement : oldAngularElements) {
      if (angularElement instanceof AngularHasTemplateElement) {
        AngularHasTemplateElement hasTemplate = (AngularHasTemplateElement) angularElement;
        Source templateSource = hasTemplate.getTemplateSource();
        if (templateSource != null) {
          HtmlEntry htmlEntry = getReadableHtmlEntry(templateSource);
          HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
          htmlCopy.setValue(HtmlEntry.ANGULAR_APPLICATION, null);
          htmlCopy.setValue(HtmlEntry.ANGULAR_COMPONENT, null);
          htmlCopy.setState(HtmlEntry.ANGULAR_ERRORS, CacheState.INVALID);
          cache.put(templateSource, htmlCopy);
          workManager.add(templateSource, SourcePriority.HTML);
        }
      }
    }
    // reset Dart sources
    Source[] oldElementSources = application.getElementSources();
    for (Source elementSource : oldElementSources) {
      DartEntry dartEntry = getReadableDartEntry(elementSource);
      DartEntryImpl dartCopy = dartEntry.getWritableCopy();
      dartCopy.setValue(DartEntry.ANGULAR_ERRORS, AnalysisError.NO_ERRORS);
      cache.put(elementSource, dartCopy);
      // notify about (disappeared) Angular errors
      ChangeNoticeImpl notice = getNotice(elementSource);
      notice.setErrors(dartCopy.getAllErrors(), dartEntry.getValue(SourceEntry.LINE_INFO));
    }
  }

  /**
   * In response to a change to at least one of the compilation units in the given library,
   * invalidate any results that are dependent on the result of resolving that library.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * <p>
   * <b>Note:</b> Any cache entries that were accessed before this method was invoked must be
   * re-accessed after this method returns.
   * 
   * @param librarySource the source of the library being invalidated
   */
  private void invalidateLibraryResolution(Source librarySource) {
    // TODO(brianwilkerson) This could be optimized. There's no need to flush all of these entries
    // if the public namespace hasn't changed, which will be a fairly common case. The question is
    // whether we can afford the time to compute the namespace to look for differences.
    DartEntry libraryEntry = getReadableDartEntry(librarySource);
    if (libraryEntry != null) {
      Source[] includedParts = libraryEntry.getValue(DartEntry.INCLUDED_PARTS);
      DartEntryImpl libraryCopy = libraryEntry.getWritableCopy();
      libraryCopy.invalidateAllResolutionInformation();
      cache.put(librarySource, libraryCopy);
      workManager.add(librarySource, SourcePriority.LIBRARY);
      for (Source partSource : includedParts) {
        SourceEntry partEntry = cache.get(partSource);
        if (partEntry instanceof DartEntry) {
          DartEntryImpl partCopy = ((DartEntry) partEntry).getWritableCopy();
          partCopy.invalidateAllResolutionInformation();
          cache.put(partSource, partCopy);
        }
      }
    }
    // invalidate Angular applications
    List<AngularApplication> angularApplicationsCopy = Lists.newArrayList(angularApplications);
    for (AngularApplication application : angularApplicationsCopy) {
      if (application.dependsOn(librarySource)) {
        Source entryPointSource = application.getEntryPoint();
        HtmlEntry entry = getReadableHtmlEntry(entryPointSource);
        HtmlEntryImpl entryCopy = entry.getWritableCopy();
        invalidateAngularResolution(entryCopy);
        cache.put(entryPointSource, entryCopy);
        workManager.add(entryPointSource, SourcePriority.HTML);
      }
    }
  }

  /**
   * Return {@code true} if this library is, or depends on, dart:html.
   * 
   * @param library the library being tested
   * @param visitedLibraries a collection of the libraries that have been visited, used to prevent
   *          infinite recursion
   * @return {@code true} if this library is, or depends on, dart:html
   */
  private boolean isClient(LibraryElement library, Source htmlSource,
      HashSet<LibraryElement> visitedLibraries) {
    if (visitedLibraries.contains(library)) {
      return false;
    }
    if (library.getSource().equals(htmlSource)) {
      return true;
    }
    visitedLibraries.add(library);
    for (LibraryElement imported : library.getImportedLibraries()) {
      if (isClient(imported, htmlSource, visitedLibraries)) {
        return true;
      }
    }
    for (LibraryElement exported : library.getExportedLibraries()) {
      if (isClient(exported, htmlSource, visitedLibraries)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Log the given debugging information.
   * 
   * @param message the message to be added to the log
   */
  private void logInformation(String message) {
    AnalysisEngine.getInstance().getLogger().logInformation(message);
  }

  /**
   * Log the given debugging information.
   * 
   * @param message the message to be added to the log
   * @param exception the exception to be included in the log entry
   */
  private void logInformation(String message, Throwable exception) {
    if (exception == null) {
      AnalysisEngine.getInstance().getLogger().logInformation(message);
    } else {
      AnalysisEngine.getInstance().getLogger().logInformation(message, exception);
    }
  }

  /**
   * Updates {@link HtmlEntry}s that correspond to the previously known and new Angular application
   * information.
   */
  private void recordAngularEntryPoint(HtmlEntryImpl entry, ResolveAngularEntryHtmlTask task)
      throws AnalysisException {
    AngularApplication application = task.getApplication();
    if (application != null) {
      angularApplications.add(application);
      // if this is an entry point, then we already resolved it
      entry.setValue(HtmlEntry.ANGULAR_ERRORS, task.getEntryErrors());
      // schedule HTML templates analysis
      AngularElement[] newAngularElements = application.getElements();
      for (AngularElement angularElement : newAngularElements) {
        if (angularElement instanceof AngularHasTemplateElement) {
          AngularHasTemplateElement hasTemplate = (AngularHasTemplateElement) angularElement;
          Source templateSource = hasTemplate.getTemplateSource();
          if (templateSource != null) {
            HtmlEntry htmlEntry = getReadableHtmlEntry(templateSource);
            HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
            htmlCopy.setValue(HtmlEntry.ANGULAR_APPLICATION, application);
            if (hasTemplate instanceof AngularComponentElement) {
              AngularComponentElement component = (AngularComponentElement) hasTemplate;
              htmlCopy.setValue(HtmlEntry.ANGULAR_COMPONENT, component);
            }
            htmlCopy.setState(HtmlEntry.ANGULAR_ERRORS, CacheState.INVALID);
            cache.put(templateSource, htmlCopy);
            workManager.add(templateSource, SourcePriority.HTML);
          }
        }
      }
      // update Dart sources errors
      Source[] newElementSources = application.getElementSources();
      for (Source elementSource : newElementSources) {
        DartEntry dartEntry = getReadableDartEntry(elementSource);
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        dartCopy.setValue(DartEntry.ANGULAR_ERRORS, task.getErrors(elementSource));
        cache.put(elementSource, dartCopy);
        // notify about Dart errors
        ChangeNoticeImpl notice = getNotice(elementSource);
        notice.setErrors(dartCopy.getAllErrors(), computeLineInfo(elementSource));
      }
    }
    // remember Angular entry point
    entry.setValue(HtmlEntry.ANGULAR_ENTRY, application);
  }

  /**
   * Record the results produced by performing a {@link BuildDartElementModelTask}. If the results
   * were computed from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @return an entry containing the recorded results
   * @throws AnalysisException if the results could not be recorded
   */
  private DartEntry recordBuildDartElementModelTask(BuildDartElementModelTask task)
      throws AnalysisException {
    Source targetLibrary = task.getTargetLibrary();
    List<ResolvableLibrary> builtLibraries = task.getLibrariesInCycle();
    AnalysisException thrownException = task.getException();
    DartEntry targetEntry = null;
    synchronized (cacheLock) {
      if (allModificationTimesMatch(builtLibraries)) {
        Source htmlSource = getSourceFactory().forUri(DartSdk.DART_HTML);
        RecordingErrorListener errorListener = task.getErrorListener();
        for (ResolvableLibrary library : builtLibraries) {
          Source librarySource = library.getLibrarySource();
          for (Source source : library.getCompilationUnitSources()) {
            CompilationUnit unit = library.getAST(source);
            AnalysisError[] errors = errorListener.getErrorsForSource(source);
            LineInfo lineInfo = getLineInfo(source);
            DartEntryImpl dartCopy = (DartEntryImpl) cache.get(source).getWritableCopy();
            if (thrownException == null) {
              dartCopy.setValueInLibrary(DartEntry.BUILD_ELEMENT_ERRORS, librarySource, errors);
              dartCopy.setValueInLibrary(DartEntry.BUILT_UNIT, librarySource, unit);
              if (source.equals(librarySource)) {
                LibraryElementImpl libraryElement = library.getLibraryElement();
                dartCopy.setValue(DartEntry.ELEMENT, libraryElement);
                dartCopy.setValue(DartEntry.IS_LAUNCHABLE, libraryElement.getEntryPoint() != null);
                dartCopy.setValue(
                    DartEntry.IS_CLIENT,
                    isClient(libraryElement, htmlSource, new HashSet<LibraryElement>()));
              }
            } else {
              dartCopy.recordBuildElementErrorInLibrary(librarySource, thrownException);
              cache.remove(source);
            }
            cache.put(source, dartCopy);
            if (!source.equals(librarySource)) {
              workManager.add(librarySource, SourcePriority.PRIORITY_PART);
            }
            if (source.equals(targetLibrary)) {
              targetEntry = dartCopy;
            }

            ChangeNoticeImpl notice = getNotice(source);
            notice.setCompilationUnit(unit);
            notice.setErrors(dartCopy.getAllErrors(), lineInfo);
          }
        }
      } else {
        @SuppressWarnings("resource")
        PrintStringWriter writer = new PrintStringWriter();
        writer.println("Build element model results discarded for");
        for (ResolvableLibrary library : builtLibraries) {
          Source librarySource = library.getLibrarySource();
          for (Source source : library.getCompilationUnitSources()) {
            DartEntry dartEntry = getReadableDartEntry(source);
            if (dartEntry != null) {
              long resultTime = library.getModificationTime(source);
              writer.println("  " + debuggingString(source) + "; sourceTime = "
                  + getModificationStamp(source) + ", resultTime = " + resultTime
                  + ", cacheTime = " + dartEntry.getModificationTime());
              DartEntryImpl dartCopy = dartEntry.getWritableCopy();
              if (thrownException == null || resultTime >= 0L) {
                //
                // The analysis was performed on out-of-date sources. Mark the cache so that the
                // sources will be re-analyzed using the up-to-date sources.
                //
                dartCopy.recordBuildElementNotInProcess();
              } else {
                //
                // We could not determine whether the sources were up-to-date or out-of-date. Mark
                // the cache so that we won't attempt to re-analyze the sources until there's a
                // good chance that we'll be able to do so without error.
                //
                dartCopy.recordBuildElementErrorInLibrary(librarySource, thrownException);
                cache.remove(source);
              }
              cache.put(source, dartCopy);
              if (source.equals(targetLibrary)) {
                targetEntry = dartCopy;
              }
            } else {
              writer.println("  " + debuggingString(source) + "; sourceTime = "
                  + getModificationStamp(source) + ", no entry");
            }
          }
        }
        logInformation(writer.toString());
      }
    }
    if (thrownException != null) {
      throw thrownException;
    }
    if (targetEntry == null) {
      targetEntry = getReadableDartEntry(targetLibrary);
      if (targetEntry == null) {
        throw new AnalysisException("A Dart file became a non-Dart file: "
            + targetLibrary.getFullName());
      }
    }
    return targetEntry;
  }

  /**
   * Given a cache entry and a library element, record the library element and other information
   * gleaned from the element in the cache entry.
   * 
   * @param dartCopy the cache entry in which data is to be recorded
   * @param library the library element used to record information
   * @param librarySource the source for the library used to record information
   * @param htmlSource the source for the HTML library
   */
  private void recordElementData(DartEntryImpl dartCopy, LibraryElement library,
      Source librarySource, Source htmlSource) {
    dartCopy.setValue(DartEntry.ELEMENT, library);
    dartCopy.setValue(DartEntry.IS_LAUNCHABLE, library.getEntryPoint() != null);
    dartCopy.setValue(
        DartEntry.IS_CLIENT,
        isClient(library, htmlSource, new HashSet<LibraryElement>()));
  }

  /**
   * Record the results produced by performing a {@link GenerateDartErrorsTask}. If the results were
   * computed from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  private DartEntry recordGenerateDartErrorsTask(GenerateDartErrorsTask task)
      throws AnalysisException {
    Source source = task.getSource();
    Source librarySource = task.getLibraryElement().getSource();
    AnalysisException thrownException = task.getException();
    DartEntry dartEntry = null;
    synchronized (cacheLock) {
      SourceEntry sourceEntry = cache.get(source);
      if (sourceEntry == null) {
        throw new ObsoleteSourceAnalysisException(source);
      } else if (!(sourceEntry instanceof DartEntry)) {
        // This shouldn't be possible because we should never have performed the task if the source
        // didn't represent a Dart file, but check to be safe.
        throw new AnalysisException(
            "Internal error: attempting to verify non-Dart file as a Dart file: "
                + source.getFullName());
      }
      dartEntry = (DartEntry) sourceEntry;
      long sourceTime = getModificationStamp(source);
      long resultTime = task.getModificationTime();
      if (sourceTime == resultTime) {
        if (dartEntry.getModificationTime() != sourceTime) {
          // The source has changed without the context being notified. Simulate notification.
          sourceChanged(source);
          dartEntry = getReadableDartEntry(source);
          if (dartEntry == null) {
            throw new AnalysisException("A Dart file became a non-Dart file: "
                + source.getFullName());
          }
        }
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        if (thrownException == null) {
          dartCopy.setValueInLibrary(DartEntry.VERIFICATION_ERRORS, librarySource, task.getErrors());
          ChangeNoticeImpl notice = getNotice(source);
          notice.setErrors(dartCopy.getAllErrors(), dartCopy.getValue(SourceEntry.LINE_INFO));
        } else {
          dartCopy.recordVerificationErrorInLibrary(librarySource, thrownException);
        }
        cache.put(source, dartCopy);
        dartEntry = dartCopy;
      } else {
        logInformation("Generated errors discarded for " + debuggingString(source)
            + "; sourceTime = " + sourceTime + ", resultTime = " + resultTime + ", cacheTime = "
            + dartEntry.getModificationTime(), thrownException);
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        if (thrownException == null || resultTime >= 0L) {
          //
          // The analysis was performed on out-of-date sources. Mark the cache so that the source
          // will be re-verified using the up-to-date sources.
          //
//          dartCopy.setState(DartEntry.VERIFICATION_ERRORS, librarySource, CacheState.INVALID);
          removeFromParts(source, dartEntry);
          dartCopy.invalidateAllInformation();
          dartCopy.setModificationTime(sourceTime);
          cache.removedAst(source);
          workManager.add(source, SourcePriority.UNKNOWN);
        } else {
          //
          // We could not determine whether the sources were up-to-date or out-of-date. Mark the
          // cache so that we won't attempt to re-verify the source until there's a good chance
          // that we'll be able to do so without error.
          //
          dartCopy.recordVerificationErrorInLibrary(librarySource, thrownException);
        }
        cache.put(source, dartCopy);
        dartEntry = dartCopy;
      }
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return dartEntry;
  }

  /**
   * Record the results produced by performing a {@link GenerateDartHintsTask}. If the results were
   * computed from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  private DartEntry recordGenerateDartHintsTask(GenerateDartHintsTask task)
      throws AnalysisException {
    Source librarySource = task.getLibraryElement().getSource();
    AnalysisException thrownException = task.getException();
    DartEntry libraryEntry = null;
    HashMap<Source, TimestampedData<AnalysisError[]>> hintMap = task.getHintMap();
    if (hintMap == null) {
      synchronized (cacheLock) {
        // We don't have any information about which sources to mark as invalid other than the library
        // source.
        SourceEntry sourceEntry = cache.get(librarySource);
        if (sourceEntry == null) {
          throw new ObsoleteSourceAnalysisException(librarySource);
        } else if (!(sourceEntry instanceof DartEntry)) {
          // This shouldn't be possible because we should never have performed the task if the source
          // didn't represent a Dart file, but check to be safe.
          throw new AnalysisException(
              "Internal error: attempting to generate hints for non-Dart file as a Dart file: "
                  + librarySource.getFullName());
        }
        if (thrownException == null) {
          thrownException = new AnalysisException(
              "GenerateDartHintsTask returned a null hint map without throwing an exception: "
                  + librarySource.getFullName());
        }
        DartEntryImpl dartCopy = ((DartEntry) sourceEntry).getWritableCopy();
        dartCopy.recordHintErrorInLibrary(librarySource, thrownException);
        cache.put(librarySource, dartCopy);
      }
      throw thrownException;
    }
    for (Map.Entry<Source, TimestampedData<AnalysisError[]>> entry : hintMap.entrySet()) {
      Source unitSource = entry.getKey();
      TimestampedData<AnalysisError[]> results = entry.getValue();
      synchronized (cacheLock) {
        SourceEntry sourceEntry = cache.get(unitSource);
        if (!(sourceEntry instanceof DartEntry)) {
          // This shouldn't be possible because we should never have performed the task if the source
          // didn't represent a Dart file, but check to be safe.
          throw new AnalysisException(
              "Internal error: attempting to parse non-Dart file as a Dart file: "
                  + unitSource.getFullName());
        }
        DartEntry dartEntry = (DartEntry) sourceEntry;
        if (unitSource.equals(librarySource)) {
          libraryEntry = dartEntry;
        }
        long sourceTime = getModificationStamp(unitSource);
        long resultTime = results.getModificationTime();
        if (sourceTime == resultTime) {
          if (dartEntry.getModificationTime() != sourceTime) {
            // The source has changed without the context being notified. Simulate notification.
            sourceChanged(unitSource);
            dartEntry = getReadableDartEntry(unitSource);
            if (dartEntry == null) {
              throw new AnalysisException("A Dart file became a non-Dart file: "
                  + unitSource.getFullName());
            }
          }
          DartEntryImpl dartCopy = dartEntry.getWritableCopy();
          if (thrownException == null) {
            dartCopy.setValueInLibrary(DartEntry.HINTS, librarySource, results.getData());
            ChangeNoticeImpl notice = getNotice(unitSource);
            notice.setErrors(dartCopy.getAllErrors(), dartCopy.getValue(SourceEntry.LINE_INFO));
          } else {
            dartCopy.recordHintErrorInLibrary(librarySource, thrownException);
          }
          cache.put(unitSource, dartCopy);
          dartEntry = dartCopy;
        } else {
          logInformation("Generated hints discarded for " + debuggingString(unitSource)
              + "; sourceTime = " + sourceTime + ", resultTime = " + resultTime + ", cacheTime = "
              + dartEntry.getModificationTime(), thrownException);
          if (dartEntry.getStateInLibrary(DartEntry.HINTS, librarySource) == CacheState.IN_PROCESS) {
            DartEntryImpl dartCopy = dartEntry.getWritableCopy();
            if (thrownException == null || resultTime >= 0L) {
              //
              // The analysis was performed on out-of-date sources. Mark the cache so that the sources
              // will be re-analyzed using the up-to-date sources.
              //
//              dartCopy.setState(DartEntry.HINTS, librarySource, CacheState.INVALID);
              removeFromParts(unitSource, dartEntry);
              dartCopy.invalidateAllInformation();
              dartCopy.setModificationTime(sourceTime);
              cache.removedAst(unitSource);
              workManager.add(unitSource, SourcePriority.UNKNOWN);
            } else {
              //
              // We could not determine whether the sources were up-to-date or out-of-date. Mark the
              // cache so that we won't attempt to re-analyze the sources until there's a good chance
              // that we'll be able to do so without error.
              //
              dartCopy.recordHintErrorInLibrary(librarySource, thrownException);
            }
            cache.put(unitSource, dartCopy);
            dartEntry = dartCopy;
          }
        }
      }
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return libraryEntry;
  }

  /**
   * Record the results produced by performing a {@link GetContentTask}.
   * 
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  private SourceEntry recordGetContentsTask(GetContentTask task) throws AnalysisException {
    if (!task.isComplete()) {
      return null;
    }
    Source source = task.getSource();
    AnalysisException thrownException = task.getException();
    SourceEntry sourceEntry = null;
    synchronized (cacheLock) {
      sourceEntry = cache.get(source);
      if (sourceEntry == null) {
        throw new ObsoleteSourceAnalysisException(source);
      }
      SourceEntryImpl sourceCopy = sourceEntry.getWritableCopy();
      if (thrownException == null) {
        sourceCopy.setModificationTime(task.getModificationTime());
        sourceCopy.setValue(SourceEntry.CONTENT, task.getContent());
      } else {
        sourceCopy.recordContentError(thrownException);
        workManager.remove(source);
      }
      cache.put(source, sourceCopy);
      sourceEntry = sourceCopy;
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return sourceEntry;
  }

  /**
   * Record the results produced by performing a {@link IncrementalAnalysisTask}.
   * 
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  private DartEntry recordIncrementalAnalysisTaskResults(IncrementalAnalysisTask task)
      throws AnalysisException {
    synchronized (cacheLock) {
      CompilationUnit unit = task.getCompilationUnit();
      if (unit != null) {
        ChangeNoticeImpl notice = getNotice(task.getSource());
        notice.setCompilationUnit(unit);
        incrementalAnalysisCache = IncrementalAnalysisCache.cacheResult(task.getCache(), unit);
      }
    }
    return null;
  }

  /**
   * Record the results produced by performing a {@link ParseDartTask}. If the results were computed
   * from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  private DartEntry recordParseDartTaskResults(ParseDartTask task) throws AnalysisException {
    Source source = task.getSource();
    AnalysisException thrownException = task.getException();
    DartEntry dartEntry = null;
    synchronized (cacheLock) {
      SourceEntry sourceEntry = cache.get(source);
      if (sourceEntry == null) {
        throw new ObsoleteSourceAnalysisException(source);
      } else if (!(sourceEntry instanceof DartEntry)) {
        // This shouldn't be possible because we should never have performed the task if the source
        // didn't represent a Dart file, but check to be safe.
        throw new AnalysisException(
            "Internal error: attempting to parse non-Dart file as a Dart file: "
                + source.getFullName());
      }
      dartEntry = (DartEntry) sourceEntry;
      long sourceTime = getModificationStamp(source);
      long resultTime = task.getModificationTime();
      if (sourceTime == resultTime) {
        if (dartEntry.getModificationTime() != sourceTime) {
          // The source has changed without the context being notified. Simulate notification.
          sourceChanged(source);
          dartEntry = getReadableDartEntry(source);
          if (dartEntry == null) {
            throw new AnalysisException("A Dart file became a non-Dart file: "
                + source.getFullName());
          }
        }
        removeFromParts(source, dartEntry);
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        if (thrownException == null) {
          if (task.hasNonPartOfDirective()) {
            dartCopy.setValue(DartEntry.SOURCE_KIND, SourceKind.LIBRARY);
            dartCopy.setContainingLibrary(source);
            workManager.add(source, SourcePriority.LIBRARY);
          } else if (task.hasPartOfDirective()) {
            dartCopy.setValue(DartEntry.SOURCE_KIND, SourceKind.PART);
            dartCopy.removeContainingLibrary(source);
            workManager.add(source, SourcePriority.NORMAL_PART);
          } else {
            // The file contains no directives.
            List<Source> containingLibraries = dartCopy.getContainingLibraries();
            if (containingLibraries.size() > 1
                || (containingLibraries.size() == 1 && !containingLibraries.get(0).equals(source))) {
              dartCopy.setValue(DartEntry.SOURCE_KIND, SourceKind.PART);
              dartCopy.removeContainingLibrary(source);
              workManager.add(source, SourcePriority.NORMAL_PART);
            } else {
              dartCopy.setValue(DartEntry.SOURCE_KIND, SourceKind.LIBRARY);
              dartCopy.setContainingLibrary(source);
              workManager.add(source, SourcePriority.LIBRARY);
            }
          }
          Source[] newParts = task.getIncludedSources();
          for (int i = 0; i < newParts.length; i++) {
            Source partSource = newParts[i];
            DartEntry partEntry = getReadableDartEntry(partSource);
            if (partEntry != null && partEntry != dartEntry) {
              DartEntryImpl partCopy = partEntry.getWritableCopy();
              // TODO(brianwilkerson) Change the kind of the "part" if it was marked as a library
              // and it has no directives.
              partCopy.addContainingLibrary(source);
              cache.put(partSource, partCopy);
            }
          }
          dartCopy.setValue(DartEntry.PARSED_UNIT, task.getCompilationUnit());
          dartCopy.setValue(DartEntry.PARSE_ERRORS, task.getErrors());
          dartCopy.setValue(DartEntry.EXPORTED_LIBRARIES, task.getExportedSources());
          dartCopy.setValue(DartEntry.IMPORTED_LIBRARIES, task.getImportedSources());
          dartCopy.setValue(DartEntry.INCLUDED_PARTS, newParts);
          cache.storedAst(source);

          ChangeNoticeImpl notice = getNotice(source);
          notice.setErrors(dartCopy.getAllErrors(), task.getLineInfo());

          // Verify that the incrementally parsed and resolved unit in the incremental cache
          // is structurally equivalent to the fully parsed unit
          incrementalAnalysisCache = IncrementalAnalysisCache.verifyStructure(
              incrementalAnalysisCache,
              source,
              task.getCompilationUnit());
        } else {
          removeFromParts(source, dartEntry);
          dartCopy.recordParseError(thrownException);
          cache.removedAst(source);
        }
        cache.put(source, dartCopy);
        dartEntry = dartCopy;
      } else {
        logInformation(
            "Parse results discarded for " + debuggingString(source) + "; sourceTime = "
                + sourceTime + ", resultTime = " + resultTime + ", cacheTime = "
                + dartEntry.getModificationTime(),
            thrownException);
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        if (thrownException == null || resultTime >= 0L) {
          //
          // The analysis was performed on out-of-date sources. Mark the cache so that the sources
          // will be re-analyzed using the up-to-date sources.
          //
//          dartCopy.recordParseNotInProcess();
          removeFromParts(source, dartEntry);
          dartCopy.invalidateAllInformation();
          dartCopy.setModificationTime(sourceTime);
          cache.removedAst(source);
          workManager.add(source, SourcePriority.UNKNOWN);
        } else {
          //
          // We could not determine whether the sources were up-to-date or out-of-date. Mark the
          // cache so that we won't attempt to re-analyze the sources until there's a good chance
          // that we'll be able to do so without error.
          //
          dartCopy.recordParseError(thrownException);
        }
        cache.put(source, dartCopy);
        dartEntry = dartCopy;
      }
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return dartEntry;
  }

  /**
   * Record the results produced by performing a {@link ParseHtmlTask}. If the results were computed
   * from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  private HtmlEntry recordParseHtmlTaskResults(ParseHtmlTask task) throws AnalysisException {
    Source source = task.getSource();
    AnalysisException thrownException = task.getException();
    HtmlEntry htmlEntry = null;
    synchronized (cacheLock) {
      SourceEntry sourceEntry = cache.get(source);
      if (sourceEntry == null) {
        throw new ObsoleteSourceAnalysisException(source);
      } else if (!(sourceEntry instanceof HtmlEntry)) {
        // This shouldn't be possible because we should never have performed the task if the source
        // didn't represent an HTML file, but check to be safe.
        throw new AnalysisException(
            "Internal error: attempting to parse non-HTML file as a HTML file: "
                + source.getFullName());
      }
      htmlEntry = (HtmlEntry) sourceEntry;
      long sourceTime = getModificationStamp(source);
      long resultTime = task.getModificationTime();
      if (sourceTime == resultTime) {
        if (htmlEntry.getModificationTime() != sourceTime) {
          // The source has changed without the context being notified. Simulate notification.
          sourceChanged(source);
          htmlEntry = getReadableHtmlEntry(source);
          if (htmlEntry == null) {
            throw new AnalysisException("An HTML file became a non-HTML file: "
                + source.getFullName());
          }
        }
        HtmlEntryImpl htmlCopy = ((HtmlEntry) sourceEntry).getWritableCopy();
        if (thrownException == null) {
          LineInfo lineInfo = task.getLineInfo();
          HtmlUnit unit = task.getHtmlUnit();
          htmlCopy.setValue(SourceEntry.LINE_INFO, lineInfo);
          htmlCopy.setValue(HtmlEntry.PARSED_UNIT, unit);
          htmlCopy.setValue(HtmlEntry.PARSE_ERRORS, task.getErrors());
          htmlCopy.setValue(HtmlEntry.REFERENCED_LIBRARIES, task.getReferencedLibraries());
          cache.storedAst(source);

          ChangeNoticeImpl notice = getNotice(source);
          notice.setErrors(htmlCopy.getAllErrors(), lineInfo);
        } else {
          htmlCopy.recordParseError(thrownException);
          cache.removedAst(source);
        }
        cache.put(source, htmlCopy);
        htmlEntry = htmlCopy;
      } else {
        logInformation(
            "Parse results discarded for " + debuggingString(source) + "; sourceTime = "
                + sourceTime + ", resultTime = " + resultTime + ", cacheTime = "
                + htmlEntry.getModificationTime(),
            thrownException);
        HtmlEntryImpl htmlCopy = ((HtmlEntry) sourceEntry).getWritableCopy();
        if (thrownException == null || resultTime >= 0L) {
          //
          // The analysis was performed on out-of-date sources. Mark the cache so that the sources
          // will be re-analyzed using the up-to-date sources.
          //
//          if (htmlCopy.getState(SourceEntry.LINE_INFO) == CacheState.IN_PROCESS) {
//            htmlCopy.setState(SourceEntry.LINE_INFO, CacheState.INVALID);
//          }
//          if (htmlCopy.getState(HtmlEntry.PARSED_UNIT) == CacheState.IN_PROCESS) {
//            htmlCopy.setState(HtmlEntry.PARSED_UNIT, CacheState.INVALID);
//          }
//          if (htmlCopy.getState(HtmlEntry.REFERENCED_LIBRARIES) == CacheState.IN_PROCESS) {
//            htmlCopy.setState(HtmlEntry.REFERENCED_LIBRARIES, CacheState.INVALID);
//          }
          htmlCopy.invalidateAllInformation();
          htmlCopy.setModificationTime(sourceTime);
          cache.removedAst(source);
        } else {
          //
          // We could not determine whether the sources were up-to-date or out-of-date. Mark the
          // cache so that we won't attempt to re-analyze the sources until there's a good chance
          // that we'll be able to do so without error.
          //
          htmlCopy.recordParseError(thrownException);
        }
        cache.put(source, htmlCopy);
        htmlEntry = htmlCopy;
      }
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return htmlEntry;
  }

  /**
   * Record the results produced by performing a {@link PolymerBuildHtmlTask}. If the results were
   * computed from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @throws AnalysisException if the results could not be recorded
   */
  private HtmlEntry recordPolymerBuildHtmlTaskResults(PolymerBuildHtmlTask task)
      throws AnalysisException {
    Source source = task.getSource();
    AnalysisException thrownException = task.getException();
    HtmlEntry htmlEntry = null;
    synchronized (cacheLock) {
      SourceEntry sourceEntry = cache.get(source);
      if (sourceEntry == null) {
        throw new ObsoleteSourceAnalysisException(source);
      } else if (!(sourceEntry instanceof HtmlEntry)) {
        // This shouldn't be possible because we should never have performed the task if the source
        // didn't represent an HTML file, but check to be safe.
        throw new AnalysisException(
            "Internal error: attempting to resolve non-HTML file as an HTML file: "
                + source.getFullName());
      }
      htmlEntry = (HtmlEntry) sourceEntry;
      long sourceTime = getModificationStamp(source);
      long resultTime = task.getModificationTime();
      if (sourceTime == resultTime) {
        if (htmlEntry.getModificationTime() != sourceTime) {
          // The source has changed without the context being notified. Simulate notification.
          sourceChanged(source);
          htmlEntry = getReadableHtmlEntry(source);
          if (htmlEntry == null) {
            throw new AnalysisException("An HTML file became a non-HTML file: "
                + source.getFullName());
          }
        }
        HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
        if (thrownException == null) {
          htmlCopy.setValue(HtmlEntry.POLYMER_BUILD_ERRORS, task.getErrors());
          // notify about errors
          ChangeNoticeImpl notice = getNotice(source);
          notice.setErrors(htmlCopy.getAllErrors(), htmlCopy.getValue(SourceEntry.LINE_INFO));
        } else {
          htmlCopy.recordResolutionError(thrownException);
        }
        cache.put(source, htmlCopy);
        htmlEntry = htmlCopy;
      } else {
        HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
        if (thrownException == null || resultTime >= 0L) {
          //
          // The analysis was performed on out-of-date sources. Mark the cache so that the sources
          // will be re-analyzed using the up-to-date sources.
          //
          htmlCopy.invalidateAllInformation();
          htmlCopy.setModificationTime(sourceTime);
          cache.removedAst(source);
        } else {
          //
          // We could not determine whether the sources were up-to-date or out-of-date. Mark the
          // cache so that we won't attempt to re-analyze the sources until there's a good chance
          // that we'll be able to do so without error.
          //
          htmlCopy.recordResolutionError(thrownException);
        }
        cache.put(source, htmlCopy);
        htmlEntry = htmlCopy;
      }
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return htmlEntry;
  }

  /**
   * Record the results produced by performing a {@link PolymerResolveHtmlTask}. If the results were
   * computed from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @throws AnalysisException if the results could not be recorded
   */
  private HtmlEntry recordPolymerResolveHtmlTaskResults(PolymerResolveHtmlTask task)
      throws AnalysisException {
    Source source = task.getSource();
    AnalysisException thrownException = task.getException();
    HtmlEntry htmlEntry = null;
    synchronized (cacheLock) {
      SourceEntry sourceEntry = cache.get(source);
      if (sourceEntry == null) {
        throw new ObsoleteSourceAnalysisException(source);
      } else if (!(sourceEntry instanceof HtmlEntry)) {
        // This shouldn't be possible because we should never have performed the task if the source
        // didn't represent an HTML file, but check to be safe.
        throw new AnalysisException(
            "Internal error: attempting to resolve non-HTML file as an HTML file: "
                + source.getFullName());
      }
      htmlEntry = (HtmlEntry) sourceEntry;
      long sourceTime = getModificationStamp(source);
      long resultTime = task.getModificationTime();
      if (sourceTime == resultTime) {
        if (htmlEntry.getModificationTime() != sourceTime) {
          // The source has changed without the context being notified. Simulate notification.
          sourceChanged(source);
          htmlEntry = getReadableHtmlEntry(source);
          if (htmlEntry == null) {
            throw new AnalysisException("An HTML file became a non-HTML file: "
                + source.getFullName());
          }
        }
        HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
        if (thrownException == null) {
          htmlCopy.setValue(HtmlEntry.POLYMER_RESOLUTION_ERRORS, task.getErrors());
          // notify about errors
          ChangeNoticeImpl notice = getNotice(source);
          notice.setErrors(htmlCopy.getAllErrors(), htmlCopy.getValue(SourceEntry.LINE_INFO));
        } else {
          htmlCopy.recordResolutionError(thrownException);
        }
        cache.put(source, htmlCopy);
        htmlEntry = htmlCopy;
      } else {
        HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
        if (thrownException == null || resultTime >= 0L) {
          //
          // The analysis was performed on out-of-date sources. Mark the cache so that the sources
          // will be re-analyzed using the up-to-date sources.
          //
          htmlCopy.invalidateAllInformation();
          htmlCopy.setModificationTime(sourceTime);
          cache.removedAst(source);
        } else {
          //
          // We could not determine whether the sources were up-to-date or out-of-date. Mark the
          // cache so that we won't attempt to re-analyze the sources until there's a good chance
          // that we'll be able to do so without error.
          //
          htmlCopy.recordResolutionError(thrownException);
        }
        cache.put(source, htmlCopy);
        htmlEntry = htmlCopy;
      }
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return htmlEntry;
  }

  /**
   * Record the results produced by performing a {@link ResolveAngularComponentTemplateTask}. If the
   * results were computed from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @throws AnalysisException if the results could not be recorded
   */
  private HtmlEntry recordResolveAngularComponentTemplateTaskResults(
      ResolveAngularComponentTemplateTask task) throws AnalysisException {
    Source source = task.getSource();
    AnalysisException thrownException = task.getException();
    HtmlEntry htmlEntry = null;
    synchronized (cacheLock) {
      SourceEntry sourceEntry = cache.get(source);
      if (sourceEntry == null) {
        throw new ObsoleteSourceAnalysisException(source);
      } else if (!(sourceEntry instanceof HtmlEntry)) {
        // This shouldn't be possible because we should never have performed the task if the source
        // didn't represent an HTML file, but check to be safe.
        throw new AnalysisException(
            "Internal error: attempting to resolve non-HTML file as an HTML file: "
                + source.getFullName());
      }
      htmlEntry = (HtmlEntry) sourceEntry;
      long sourceTime = getModificationStamp(source);
      long resultTime = task.getModificationTime();
      if (sourceTime == resultTime) {
        if (htmlEntry.getModificationTime() != sourceTime) {
          // The source has changed without the context being notified. Simulate notification.
          sourceChanged(source);
          htmlEntry = getReadableHtmlEntry(source);
          if (htmlEntry == null) {
            throw new AnalysisException("An HTML file became a non-HTML file: "
                + source.getFullName());
          }
        }
        HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
        if (thrownException == null) {
          htmlCopy.setValue(HtmlEntry.ANGULAR_ERRORS, task.getResolutionErrors());
          // notify about errors
          ChangeNoticeImpl notice = getNotice(source);
          notice.setHtmlUnit(task.getResolvedUnit());
          notice.setErrors(htmlCopy.getAllErrors(), htmlCopy.getValue(SourceEntry.LINE_INFO));
        } else {
          htmlCopy.recordResolutionError(thrownException);
        }
        cache.put(source, htmlCopy);
        htmlEntry = htmlCopy;
      } else {
        HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
        if (thrownException == null || resultTime >= 0L) {
          //
          // The analysis was performed on out-of-date sources. Mark the cache so that the sources
          // will be re-analyzed using the up-to-date sources.
          //
//          if (htmlCopy.getState(HtmlEntry.ANGULAR_ERRORS) == CacheState.IN_PROCESS) {
//            htmlCopy.setState(HtmlEntry.ANGULAR_ERRORS, CacheState.INVALID);
//          }
//          if (htmlCopy.getState(HtmlEntry.ELEMENT) == CacheState.IN_PROCESS) {
//            htmlCopy.setState(HtmlEntry.ELEMENT, CacheState.INVALID);
//          }
//          if (htmlCopy.getState(HtmlEntry.RESOLUTION_ERRORS) == CacheState.IN_PROCESS) {
//            htmlCopy.setState(HtmlEntry.RESOLUTION_ERRORS, CacheState.INVALID);
//          }
          htmlCopy.invalidateAllInformation();
          htmlCopy.setModificationTime(sourceTime);
          cache.removedAst(source);
        } else {
          //
          // We could not determine whether the sources were up-to-date or out-of-date. Mark the
          // cache so that we won't attempt to re-analyze the sources until there's a good chance
          // that we'll be able to do so without error.
          //
          htmlCopy.recordResolutionError(thrownException);
        }
        cache.put(source, htmlCopy);
        htmlEntry = htmlCopy;
      }
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return htmlEntry;
  }

  /**
   * Record the results produced by performing a {@link ResolveAngularEntryHtmlTask}. If the results
   * were computed from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @throws AnalysisException if the results could not be recorded
   */
  private HtmlEntry recordResolveAngularEntryHtmlTaskResults(ResolveAngularEntryHtmlTask task)
      throws AnalysisException {
    Source source = task.getSource();
    AnalysisException thrownException = task.getException();
    HtmlEntry htmlEntry = null;
    synchronized (cacheLock) {
      SourceEntry sourceEntry = cache.get(source);
      if (sourceEntry == null) {
        throw new ObsoleteSourceAnalysisException(source);
      } else if (!(sourceEntry instanceof HtmlEntry)) {
        // This shouldn't be possible because we should never have performed the task if the source
        // didn't represent an HTML file, but check to be safe.
        throw new AnalysisException(
            "Internal error: attempting to resolve non-HTML file as an HTML file: "
                + source.getFullName());
      }
      htmlEntry = (HtmlEntry) sourceEntry;
      long sourceTime = getModificationStamp(source);
      long resultTime = task.getModificationTime();
      if (sourceTime == resultTime) {
        if (htmlEntry.getModificationTime() != sourceTime) {
          // The source has changed without the context being notified. Simulate notification.
          sourceChanged(source);
          htmlEntry = getReadableHtmlEntry(source);
          if (htmlEntry == null) {
            throw new AnalysisException("An HTML file became a non-HTML file: "
                + source.getFullName());
          }
        }
        HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
        if (thrownException == null) {
          htmlCopy.setValue(HtmlEntry.RESOLVED_UNIT, task.getResolvedUnit());
          recordAngularEntryPoint(htmlCopy, task);
          cache.storedAst(source);

          ChangeNoticeImpl notice = getNotice(source);
          notice.setHtmlUnit(task.getResolvedUnit());
          notice.setErrors(htmlCopy.getAllErrors(), htmlCopy.getValue(SourceEntry.LINE_INFO));
        } else {
          htmlCopy.recordResolutionError(thrownException);
        }
        cache.put(source, htmlCopy);
        htmlEntry = htmlCopy;
      } else {
        HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
        if (thrownException == null || resultTime >= 0L) {
          //
          // The analysis was performed on out-of-date sources. Mark the cache so that the sources
          // will be re-analyzed using the up-to-date sources.
          //
//          if (htmlCopy.getState(HtmlEntry.ANGULAR_ERRORS) == CacheState.IN_PROCESS) {
//            htmlCopy.setState(HtmlEntry.ANGULAR_ERRORS, CacheState.INVALID);
//          }
//          if (htmlCopy.getState(HtmlEntry.ELEMENT) == CacheState.IN_PROCESS) {
//            htmlCopy.setState(HtmlEntry.ELEMENT, CacheState.INVALID);
//          }
//          if (htmlCopy.getState(HtmlEntry.RESOLUTION_ERRORS) == CacheState.IN_PROCESS) {
//            htmlCopy.setState(HtmlEntry.RESOLUTION_ERRORS, CacheState.INVALID);
//          }
          htmlCopy.invalidateAllInformation();
          htmlCopy.setModificationTime(sourceTime);
          cache.removedAst(source);
        } else {
          //
          // We could not determine whether the sources were up-to-date or out-of-date. Mark the
          // cache so that we won't attempt to re-analyze the sources until there's a good chance
          // that we'll be able to do so without error.
          //
          htmlCopy.recordResolutionError(thrownException);
        }
        cache.put(source, htmlCopy);
        htmlEntry = htmlCopy;
      }
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return htmlEntry;
  }

  /**
   * Record the results produced by performing a {@link ResolveDartUnitTask}. If the results were
   * computed from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  private DartEntry recordResolveDartUnitTaskResults(ResolveDartUnitTask task)
      throws AnalysisException {
    Source unitSource = task.getSource();
    Source librarySource = task.getLibrarySource();
    AnalysisException thrownException = task.getException();
    DartEntry dartEntry = null;
    synchronized (cacheLock) {
      SourceEntry sourceEntry = cache.get(unitSource);
      if (sourceEntry == null) {
        throw new ObsoleteSourceAnalysisException(unitSource);
      } else if (!(sourceEntry instanceof DartEntry)) {
        // This shouldn't be possible because we should never have performed the task if the source
        // didn't represent a Dart file, but check to be safe.
        throw new AnalysisException(
            "Internal error: attempting to resolve non-Dart file as a Dart file: "
                + unitSource.getFullName());
      }
      dartEntry = (DartEntry) sourceEntry;
      long sourceTime = getModificationStamp(unitSource);
      long resultTime = task.getModificationTime();
      if (sourceTime == resultTime) {
        if (dartEntry.getModificationTime() != sourceTime) {
          // The source has changed without the context being notified. Simulate notification.
          sourceChanged(unitSource);
          dartEntry = getReadableDartEntry(unitSource);
          if (dartEntry == null) {
            throw new AnalysisException("A Dart file became a non-Dart file: "
                + unitSource.getFullName());
          }
        }
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        if (thrownException == null) {
          dartCopy.setValueInLibrary(DartEntry.RESOLVED_UNIT, librarySource, task.getResolvedUnit());
          cache.storedAst(unitSource);
        } else {
          dartCopy.recordResolutionErrorInLibrary(librarySource, thrownException);
          cache.removedAst(unitSource);
        }

        cache.put(unitSource, dartCopy);
        dartEntry = dartCopy;
      } else {
        logInformation("Resolution results discarded for " + debuggingString(unitSource)
            + "; sourceTime = " + sourceTime + ", resultTime = " + resultTime + ", cacheTime = "
            + dartEntry.getModificationTime(), thrownException);
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        if (thrownException == null || resultTime >= 0L) {
          //
          // The analysis was performed on out-of-date sources. Mark the cache so that the sources
          // will be re-analyzed using the up-to-date sources.
          //
//          if (dartCopy.getState(DartEntry.RESOLVED_UNIT) == CacheState.IN_PROCESS) {
//            dartCopy.setState(DartEntry.RESOLVED_UNIT, librarySource, CacheState.INVALID);
//          }
          removeFromParts(unitSource, dartEntry);
          dartCopy.invalidateAllInformation();
          dartCopy.setModificationTime(sourceTime);
          cache.removedAst(unitSource);
          workManager.add(unitSource, SourcePriority.UNKNOWN);
        } else {
          //
          // We could not determine whether the sources were up-to-date or out-of-date. Mark the
          // cache so that we won't attempt to re-analyze the sources until there's a good chance
          // that we'll be able to do so without error.
          //
          dartCopy.recordResolutionErrorInLibrary(librarySource, thrownException);
        }
        cache.put(unitSource, dartCopy);
        dartEntry = dartCopy;
      }
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return dartEntry;
  }

  /**
   * Record the results produced by performing a {@link ResolveHtmlTask}. If the results were
   * computed from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  private HtmlEntry recordResolveHtmlTaskResults(ResolveHtmlTask task) throws AnalysisException {
    Source source = task.getSource();
    AnalysisException thrownException = task.getException();
    HtmlEntry htmlEntry = null;
    synchronized (cacheLock) {
      SourceEntry sourceEntry = cache.get(source);
      if (sourceEntry == null) {
        throw new ObsoleteSourceAnalysisException(source);
      } else if (!(sourceEntry instanceof HtmlEntry)) {
        // This shouldn't be possible because we should never have performed the task if the source
        // didn't represent an HTML file, but check to be safe.
        throw new AnalysisException(
            "Internal error: attempting to resolve non-HTML file as an HTML file: "
                + source.getFullName());
      }
      htmlEntry = (HtmlEntry) sourceEntry;
      long sourceTime = getModificationStamp(source);
      long resultTime = task.getModificationTime();
      if (sourceTime == resultTime) {
        if (htmlEntry.getModificationTime() != sourceTime) {
          // The source has changed without the context being notified. Simulate notification.
          sourceChanged(source);
          htmlEntry = getReadableHtmlEntry(source);
          if (htmlEntry == null) {
            throw new AnalysisException("An HTML file became a non-HTML file: "
                + source.getFullName());
          }
        }
        HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
        if (thrownException == null) {
          htmlCopy.setState(HtmlEntry.PARSED_UNIT, CacheState.FLUSHED);
          htmlCopy.setValue(HtmlEntry.RESOLVED_UNIT, task.getResolvedUnit());
          htmlCopy.setValue(HtmlEntry.ELEMENT, task.getElement());
          htmlCopy.setValue(HtmlEntry.RESOLUTION_ERRORS, task.getResolutionErrors());
          cache.storedAst(source);

          ChangeNoticeImpl notice = getNotice(source);
          notice.setHtmlUnit(task.getResolvedUnit());
          notice.setErrors(htmlCopy.getAllErrors(), htmlCopy.getValue(SourceEntry.LINE_INFO));
        } else {
          htmlCopy.recordResolutionError(thrownException);
          cache.removedAst(source);
        }
        cache.put(source, htmlCopy);
        htmlEntry = htmlCopy;
      } else {
        logInformation("Resolution results discarded for " + debuggingString(source)
            + "; sourceTime = " + sourceTime + ", resultTime = " + resultTime + ", cacheTime = "
            + htmlEntry.getModificationTime(), thrownException);
        HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
        if (thrownException == null || resultTime >= 0L) {
          //
          // The analysis was performed on out-of-date sources. Mark the cache so that the sources
          // will be re-analyzed using the up-to-date sources.
          //
//          if (htmlCopy.getState(HtmlEntry.ELEMENT) == CacheState.IN_PROCESS) {
//            htmlCopy.setState(HtmlEntry.ELEMENT, CacheState.INVALID);
//          }
//          if (htmlCopy.getState(HtmlEntry.RESOLUTION_ERRORS) == CacheState.IN_PROCESS) {
//            htmlCopy.setState(HtmlEntry.RESOLUTION_ERRORS, CacheState.INVALID);
//          }
          htmlCopy.invalidateAllInformation();
          htmlCopy.setModificationTime(sourceTime);
          cache.removedAst(source);
        } else {
          //
          // We could not determine whether the sources were up-to-date or out-of-date. Mark the
          // cache so that we won't attempt to re-analyze the sources until there's a good chance
          // that we'll be able to do so without error.
          //
          htmlCopy.recordResolutionError(thrownException);
        }
        cache.put(source, htmlCopy);
        htmlEntry = htmlCopy;
      }
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return htmlEntry;
  }

  /**
   * Record the results produced by performing a {@link ScanDartTask}. If the results were computed
   * from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  private DartEntry recordScanDartTaskResults(ScanDartTask task) throws AnalysisException {
    Source source = task.getSource();
    AnalysisException thrownException = task.getException();
    DartEntry dartEntry = null;
    synchronized (cacheLock) {
      SourceEntry sourceEntry = cache.get(source);
      if (sourceEntry == null) {
        throw new ObsoleteSourceAnalysisException(source);
      } else if (!(sourceEntry instanceof DartEntry)) {
        // This shouldn't be possible because we should never have performed the task if the source
        // didn't represent a Dart file, but check to be safe.
        throw new AnalysisException(
            "Internal error: attempting to parse non-Dart file as a Dart file: "
                + source.getFullName());
      }
      dartEntry = (DartEntry) sourceEntry;
      long sourceTime = getModificationStamp(source);
      long resultTime = task.getModificationTime();
      if (sourceTime == resultTime) {
        if (dartEntry.getModificationTime() != sourceTime) {
          // The source has changed without the context being notified. Simulate notification.
          sourceChanged(source);
          dartEntry = getReadableDartEntry(source);
          if (dartEntry == null) {
            throw new AnalysisException("A Dart file became a non-Dart file: "
                + source.getFullName());
          }
        }
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        if (thrownException == null) {
          LineInfo lineInfo = task.getLineInfo();
          dartCopy.setValue(SourceEntry.LINE_INFO, lineInfo);
          dartCopy.setValue(DartEntry.TOKEN_STREAM, task.getTokenStream());
          dartCopy.setValue(DartEntry.SCAN_ERRORS, task.getErrors());
          cache.storedAst(source);
          workManager.add(source, SourcePriority.NORMAL_PART);

          ChangeNoticeImpl notice = getNotice(source);
          notice.setErrors(dartEntry.getAllErrors(), lineInfo);
        } else {
          removeFromParts(source, dartEntry);
          dartCopy.recordScanError(thrownException);
          cache.removedAst(source);
        }
        cache.put(source, dartCopy);
        dartEntry = dartCopy;
      } else {
        logInformation(
            "Scan results discarded for " + debuggingString(source) + "; sourceTime = "
                + sourceTime + ", resultTime = " + resultTime + ", cacheTime = "
                + dartEntry.getModificationTime(),
            thrownException);
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        if (thrownException == null || resultTime >= 0L) {
          //
          // The analysis was performed on out-of-date sources. Mark the cache so that the sources
          // will be re-analyzed using the up-to-date sources.
          //
//          dartCopy.recordScanNotInProcess();
          removeFromParts(source, dartEntry);
          dartCopy.invalidateAllInformation();
          dartCopy.setModificationTime(sourceTime);
          cache.removedAst(source);
          workManager.add(source, SourcePriority.UNKNOWN);
        } else {
          //
          // We could not determine whether the sources were up-to-date or out-of-date. Mark the
          // cache so that we won't attempt to re-analyze the sources until there's a good chance
          // that we'll be able to do so without error.
          //
          dartCopy.recordScanError(thrownException);
        }
        cache.put(source, dartCopy);
        dartEntry = dartCopy;
      }
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return dartEntry;
  }

  /**
   * Remove the given library from the list of containing libraries for all of the parts referenced
   * by the given entry.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param librarySource the library to be removed
   * @param dartEntry the entry containing the list of included parts
   */
  private void removeFromParts(Source librarySource, DartEntry dartEntry) {
    Source[] oldParts = dartEntry.getValue(DartEntry.INCLUDED_PARTS);
    for (int i = 0; i < oldParts.length; i++) {
      Source partSource = oldParts[i];
      DartEntry partEntry = getReadableDartEntry(partSource);
      if (partEntry != null && partEntry != dartEntry) {
        DartEntryImpl partCopy = partEntry.getWritableCopy();
        partCopy.removeContainingLibrary(librarySource);
        if (partCopy.getContainingLibraries().size() == 0 && !exists(partSource)) {
          cache.remove(partSource);
        } else {
          cache.put(partSource, partCopy);
        }
      }
    }
  }

  /**
   * Remove the given libraries that are keys in the given map from the list of containing libraries
   * for each of the parts in the corresponding value.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param oldPartMap the table containing the parts associated with each library
   */
  private void removeFromPartsUsingMap(HashMap<Source, Source[]> oldPartMap) {
    for (Map.Entry<Source, Source[]> entry : oldPartMap.entrySet()) {
      Source librarySource = entry.getKey();
      Source[] oldParts = entry.getValue();
      for (int i = 0; i < oldParts.length; i++) {
        Source partSource = oldParts[i];
        if (!partSource.equals(librarySource)) {
          DartEntry partEntry = getReadableDartEntry(partSource);
          if (partEntry != null) {
            DartEntryImpl partCopy = partEntry.getWritableCopy();
            partCopy.removeContainingLibrary(librarySource);
            if (partCopy.getContainingLibraries().size() == 0 && !exists(partSource)) {
              cache.remove(partSource);
            } else {
              cache.put(partSource, partCopy);
            }
          }
        }
      }
    }
  }

  /**
   * Remove the given source from the priority order if it is in the list.
   * 
   * @param source the source to be removed
   */
  private void removeFromPriorityOrder(Source source) {
    int count = priorityOrder.length;
    ArrayList<Source> newOrder = new ArrayList<Source>(count);
    for (int i = 0; i < count; i++) {
      if (!priorityOrder[i].equals(source)) {
        newOrder.add(priorityOrder[i]);
      }
    }
    if (newOrder.size() < count) {
      setAnalysisPriorityOrder(newOrder);
    }
  }

  /**
   * Create an entry for the newly added source. Return {@code true} if the new source is a Dart
   * file.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source that has been added
   * @return {@code true} if the new source is a Dart file
   */
  private boolean sourceAvailable(Source source) {
    SourceEntry sourceEntry = cache.get(source);
    if (sourceEntry == null) {
      sourceEntry = createSourceEntry(source, true);
    } else {
      sourceChanged(source);
      sourceEntry = cache.get(source);
    }
    if (sourceEntry instanceof HtmlEntry) {
      workManager.add(source, SourcePriority.HTML);
    } else {
      workManager.add(source, SourcePriority.UNKNOWN);
    }
    return sourceEntry instanceof DartEntry;
  }

  /**
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source that has been changed
   */
  private void sourceChanged(Source source) {
    SourceEntry sourceEntry = cache.get(source);
    if (sourceEntry == null || sourceEntry.getModificationTime() == getModificationStamp(source)) {
      // Either we have removed this source, in which case we don't care that it is changed, or we
      // have already invalidated the cache and don't need to invalidate it again.
      return;
    }
    if (sourceEntry instanceof HtmlEntry) {
      HtmlEntryImpl htmlCopy = ((HtmlEntry) sourceEntry).getWritableCopy();
      htmlCopy.setModificationTime(getModificationStamp(source));
      invalidateAngularResolution(htmlCopy);
      htmlCopy.invalidateAllInformation();
      cache.put(source, htmlCopy);
      cache.removedAst(source);
      workManager.add(source, SourcePriority.HTML);
    } else if (sourceEntry instanceof DartEntry) {
      Source[] containingLibraries = getLibrariesContaining(source);
      HashSet<Source> librariesToInvalidate = new HashSet<Source>();
      for (Source containingLibrary : containingLibraries) {
        computeAllLibrariesDependingOn(containingLibrary, librariesToInvalidate);
      }

      for (Source library : librariesToInvalidate) {
        invalidateLibraryResolution(library);
      }

      removeFromParts(source, ((DartEntry) cache.get(source)));
      DartEntryImpl dartCopy = ((DartEntry) cache.get(source)).getWritableCopy();
      dartCopy.setModificationTime(getModificationStamp(source));
      dartCopy.invalidateAllInformation();
      cache.put(source, dartCopy);
      cache.removedAst(source);
      workManager.add(source, SourcePriority.UNKNOWN);
    }
  }

  /**
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source that has been deleted
   */
  private void sourceDeleted(Source source) {
    SourceEntry sourceEntry = cache.get(source);
    if (sourceEntry instanceof HtmlEntry) {
      HtmlEntryImpl htmlCopy = ((HtmlEntry) sourceEntry).getWritableCopy();
      invalidateAngularResolution(htmlCopy);
      htmlCopy.recordContentError(new AnalysisException("This source was marked as being deleted"));
      cache.put(source, htmlCopy);
    } else if (sourceEntry instanceof DartEntry) {
      HashSet<Source> libraries = new HashSet<Source>();
      for (Source librarySource : getLibrariesContaining(source)) {
        libraries.add(librarySource);
        for (Source dependentLibrary : getLibrariesDependingOn(librarySource)) {
          libraries.add(dependentLibrary);
        }
      }
      for (Source librarySource : libraries) {
        invalidateLibraryResolution(librarySource);
      }
      DartEntryImpl dartCopy = ((DartEntry) sourceEntry).getWritableCopy();
      dartCopy.recordContentError(new AnalysisException("This source was marked as being deleted"));
      cache.put(source, dartCopy);
    }
    workManager.remove(source);
    removeFromPriorityOrder(source);
  }

  /**
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source that has been removed
   */
  private void sourceRemoved(Source source) {
    SourceEntry sourceEntry = cache.get(source);
    if (sourceEntry instanceof HtmlEntry) {
      HtmlEntryImpl htmlCopy = ((HtmlEntry) sourceEntry).getWritableCopy();
      invalidateAngularResolution(htmlCopy);
    } else if (sourceEntry instanceof DartEntry) {
      HashSet<Source> libraries = new HashSet<Source>();
      for (Source librarySource : getLibrariesContaining(source)) {
        libraries.add(librarySource);
        for (Source dependentLibrary : getLibrariesDependingOn(librarySource)) {
          libraries.add(dependentLibrary);
        }
      }
      for (Source librarySource : libraries) {
        invalidateLibraryResolution(librarySource);
      }
    }
    cache.remove(source);
    workManager.remove(source);
    removeFromPriorityOrder(source);
  }

  /**
   * Check the cache for any invalid entries (entries whose modification time does not match the
   * modification time of the source associated with the entry). Invalid entries will be marked as
   * invalid so that the source will be re-analyzed.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @return {@code true} if at least one entry was invalid
   */
  private boolean validateCacheConsistency() {
    long consistencyCheckStart = System.nanoTime();
    ArrayList<Source> changedSources = new ArrayList<Source>();
    ArrayList<Source> missingSources = new ArrayList<Source>();
    synchronized (cacheLock) {
      MapIterator<Source, SourceEntry> iterator = cache.iterator();
      while (iterator.moveNext()) {
        Source source = iterator.getKey();
        SourceEntry sourceEntry = iterator.getValue();
        long sourceTime = getModificationStamp(source);
        if (sourceTime != sourceEntry.getModificationTime()) {
          changedSources.add(source);
        }
        if (sourceEntry.getException() != null) {
          if (!exists(source)) {
            missingSources.add(source);
          }
        }
      }
      int count = changedSources.size();
      for (int i = 0; i < count; i++) {
        sourceChanged(changedSources.get(i));
      }
    }
    long consistencyCheckEnd = System.nanoTime();
    if (changedSources.size() > 0 || missingSources.size() > 0) {
      @SuppressWarnings("resource")
      PrintStringWriter writer = new PrintStringWriter();
      writer.print("Consistency check took ");
      writer.print((consistencyCheckEnd - consistencyCheckStart) / 1000000.0);
      writer.println(" ms and found");
      writer.print("  ");
      writer.print(changedSources.size());
      writer.println(" inconsistent entries");
      writer.print("  ");
      writer.print(missingSources.size());
      writer.println(" missing sources");
      for (Source source : missingSources) {
        writer.print("    ");
        writer.println(source.getFullName());
      }
      logInformation(writer.toString());
    }
    return changedSources.size() > 0;
  }
}
