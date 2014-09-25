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
package com.google.dart.engine.internal.resolver;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.Combinator;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.HideCombinator;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.NamespaceDirective;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.ShowCombinator;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.StringInterpolation;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.ast.TypeAlias;
import com.google.dart.engine.ast.UriBasedDirective;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExportElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.NamespaceCombinator;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.builder.AngularCompilationUnitBuilder;
import com.google.dart.engine.internal.builder.EnumMemberBuilder;
import com.google.dart.engine.internal.builder.PolymerCompilationUnitBuilder;
import com.google.dart.engine.internal.constant.ConstantValueComputer;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.context.PerformanceStatistics;
import com.google.dart.engine.internal.context.RecordingErrorListener;
import com.google.dart.engine.internal.element.ExportElementImpl;
import com.google.dart.engine.internal.element.HideElementCombinatorImpl;
import com.google.dart.engine.internal.element.ImportElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.PrefixElementImpl;
import com.google.dart.engine.internal.element.ShowElementCombinatorImpl;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.internal.scope.NamespaceBuilder;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.engine.utilities.general.TimeCounter.TimeCounterHandle;
import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.engine.utilities.io.UriUtilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Instances of the class {@code LibraryResolver} are used to resolve one or more mutually dependent
 * libraries within a single context.
 * 
 * @coverage dart.engine.resolver
 */
public class LibraryResolver {
  /**
   * Instances of the class {@code TypeAliasInfo} hold information about a {@link TypeAlias}.
   */
  private static class TypeAliasInfo {
    private Library library;
    private Source source;
    private FunctionTypeAlias typeAlias;

    /**
     * Initialize a newly created information holder with the given information.
     * 
     * @param library the library containing the type alias
     * @param source the source of the file containing the type alias
     * @param typeAlias the type alias being remembered
     */
    public TypeAliasInfo(Library library, Source source, FunctionTypeAlias typeAlias) {
      this.library = library;
      this.source = source;
      this.typeAlias = typeAlias;
    }
  }

  /**
   * The analysis context in which the libraries are being analyzed.
   */
  private InternalAnalysisContext analysisContext;

  /**
   * The listener to which analysis errors will be reported, this error listener is either
   * references {@link #recordingErrorListener}, or it unions the passed
   * {@link AnalysisErrorListener} with the {@link #recordingErrorListener}.
   */
  private RecordingErrorListener errorListener;

  /**
   * A source object representing the core library (dart:core).
   */
  private Source coreLibrarySource;

  /**
   * The object representing the core library.
   */
  private Library coreLibrary;

  /**
   * The object used to access the types from the core library.
   */
  private TypeProvider typeProvider;

  /**
   * A table mapping library sources to the information being maintained for those libraries.
   */
  private HashMap<Source, Library> libraryMap = new HashMap<Source, Library>();

  /**
   * A collection containing the libraries that are being resolved together.
   */
  private Set<Library> librariesInCycles;

  /**
   * Initialize a newly created library resolver to resolve libraries within the given context.
   * 
   * @param analysisContext the analysis context in which the library is being analyzed
   */
  public LibraryResolver(InternalAnalysisContext analysisContext) {
    this.analysisContext = analysisContext;
    this.errorListener = new RecordingErrorListener();
    coreLibrarySource = analysisContext.getSourceFactory().forUri(DartSdk.DART_CORE);
  }

  /**
   * Return the analysis context in which the libraries are being analyzed.
   * 
   * @return the analysis context in which the libraries are being analyzed
   */
  public InternalAnalysisContext getAnalysisContext() {
    return analysisContext;
  }

  /**
   * Return the listener to which analysis errors will be reported.
   * 
   * @return the listener to which analysis errors will be reported
   */
  public RecordingErrorListener getErrorListener() {
    return errorListener;
  }

  /**
   * Return an array containing information about all of the libraries that were resolved.
   * 
   * @return an array containing the libraries that were resolved
   */
  public Set<Library> getResolvedLibraries() {
    return librariesInCycles;
  }

  /**
   * Resolve the library specified by the given source in the given context. The library is assumed
   * to be embedded in the given source.
   * 
   * @param librarySource the source specifying the defining compilation unit of the library to be
   *          resolved
   * @param modificationStamp the time stamp of the source from which the compilation unit was
   *          created
   * @param unit the compilation unit representing the embedded library
   * @param fullAnalysis {@code true} if a full analysis should be performed
   * @return the element representing the resolved library
   * @throws AnalysisException if the library could not be resolved for some reason
   */
  public LibraryElement resolveEmbeddedLibrary(Source librarySource, long modificationStamp,
      CompilationUnit unit, boolean fullAnalysis) throws AnalysisException {

    InstrumentationBuilder instrumentation = Instrumentation.builder("dart.engine.LibraryResolver.resolveEmbeddedLibrary");
    try {
      instrumentation.metric("fullAnalysis", fullAnalysis);
      instrumentation.data("fullName", librarySource.getFullName());
      //
      // Create the objects representing the library being resolved and the core library.
      //
      Library targetLibrary = createLibraryWithUnit(librarySource, modificationStamp, unit);
      coreLibrary = libraryMap.get(coreLibrarySource);
      if (coreLibrary == null) {
        // This will be true unless the library being analyzed is the core library.
        coreLibrary = createLibrary(coreLibrarySource);
        if (coreLibrary == null) {
          LibraryResolver2.missingCoreLibrary(analysisContext, coreLibrarySource);
        }
      }
      instrumentation.metric("createLibrary", "complete");
      //
      // Compute the set of libraries that need to be resolved together.
      //
      computeEmbeddedLibraryDependencies(targetLibrary, unit);
      librariesInCycles = computeLibrariesInCycles(targetLibrary);
      //
      // Build the element models representing the libraries being resolved. This is done in three
      // steps:
      //
      // 1. Build the basic element models without making any connections between elements other than
      //    the basic parent/child relationships. This includes building the elements representing the
      //    libraries.
      // 2. Build the elements for the import and export directives. This requires that we have the
      //    elements built for the referenced libraries, but because of the possibility of circular
      //    references needs to happen after all of the library elements have been created.
      // 3. Build the rest of the type model by connecting superclasses, mixins, and interfaces. This
      //    requires that we be able to compute the names visible in the libraries being resolved,
      //    which in turn requires that we have resolved the import directives.
      //
      buildElementModels();
      instrumentation.metric("buildElementModels", "complete");
      LibraryElement coreElement = coreLibrary.getLibraryElement();
      if (coreElement == null) {
        throw new AnalysisException("Could not resolve dart:core");
      }
      buildDirectiveModels();
      instrumentation.metric("buildDirectiveModels", "complete");
      typeProvider = new TypeProviderImpl(coreElement);
      buildTypeAliases();
      buildTypeHierarchies();
      instrumentation.metric("buildTypeHierarchies", "complete");
      //
      // Perform resolution and type analysis.
      //
      // TODO(brianwilkerson) Decide whether we want to resolve all of the libraries or whether we
      // want to only resolve the target library. The advantage to resolving everything is that we
      // have already done part of the work so we'll avoid duplicated effort. The disadvantage of
      // resolving everything is that we might do extra work that we don't really care about. Another
      // possibility is to add a parameter to this method and punt the decision to the clients.
      //
      //if (analyzeAll) {
      resolveReferencesAndTypes();
      instrumentation.metric("resolveReferencesAndTypes", "complete");
      //} else {
      //  resolveReferencesAndTypes(targetLibrary);
      //}
      performConstantEvaluation();
      instrumentation.metric("performConstantEvaluation", "complete");
      return targetLibrary.getLibraryElement();
    } finally {
      instrumentation.log();
    }
  }

  /**
   * Resolve the library specified by the given source in the given context.
   * <p>
   * Note that because Dart allows circular imports between libraries, it is possible that more than
   * one library will need to be resolved. In such cases the error listener can receive errors from
   * multiple libraries.
   * 
   * @param librarySource the source specifying the defining compilation unit of the library to be
   *          resolved
   * @param fullAnalysis {@code true} if a full analysis should be performed
   * @return the element representing the resolved library
   * @throws AnalysisException if the library could not be resolved for some reason
   */
  public LibraryElement resolveLibrary(Source librarySource, boolean fullAnalysis)
      throws AnalysisException {
    InstrumentationBuilder instrumentation = Instrumentation.builder("dart.engine.LibraryResolver.resolveLibrary");
    try {
      instrumentation.metric("fullAnalysis", fullAnalysis);
      instrumentation.data("fullName", librarySource.getFullName());
      //
      // Create the objects representing the library being resolved and the core library.
      //
      Library targetLibrary = createLibrary(librarySource);
      coreLibrary = libraryMap.get(coreLibrarySource);
      if (coreLibrary == null) {
        // This will be true unless the library being analyzed is the core library.
        coreLibrary = createLibraryOrNull(coreLibrarySource);
        if (coreLibrary == null) {
          LibraryResolver2.missingCoreLibrary(analysisContext, coreLibrarySource);
        }
      }
      instrumentation.metric("createLibrary", "complete");
      //
      // Compute the set of libraries that need to be resolved together.
      //
      computeLibraryDependencies(targetLibrary);
      librariesInCycles = computeLibrariesInCycles(targetLibrary);
      //
      // Build the element models representing the libraries being resolved. This is done in three
      // steps:
      //
      // 1. Build the basic element models without making any connections between elements other
      //    than the basic parent/child relationships. This includes building the elements
      //    representing the libraries, but excludes members defined in enums.
      // 2. Build the elements for the import and export directives. This requires that we have the
      //    elements built for the referenced libraries, but because of the possibility of circular
      //    references needs to happen after all of the library elements have been created.
      // 3. Build the members in enum declarations.
      // 4. Build the rest of the type model by connecting superclasses, mixins, and interfaces. This
      //    requires that we be able to compute the names visible in the libraries being resolved,
      //    which in turn requires that we have resolved the import directives.
      //
      buildElementModels();
      instrumentation.metric("buildElementModels", "complete");
      LibraryElement coreElement = coreLibrary.getLibraryElement();
      if (coreElement == null) {
        throw new AnalysisException("Could not resolve dart:core");
      }
      buildDirectiveModels();
      instrumentation.metric("buildDirectiveModels", "complete");
      typeProvider = new TypeProviderImpl(coreElement);
      buildEnumMembers();
      buildTypeAliases();
      buildTypeHierarchies();
      instrumentation.metric("buildTypeHierarchies", "complete");
      //
      // Perform resolution and type analysis.
      //
      // TODO(brianwilkerson) Decide whether we want to resolve all of the libraries or whether we
      // want to only resolve the target library. The advantage to resolving everything is that we
      // have already done part of the work so we'll avoid duplicated effort. The disadvantage of
      // resolving everything is that we might do extra work that we don't really care about. Another
      // possibility is to add a parameter to this method and punt the decision to the clients.
      //
      //if (analyzeAll) {
      resolveReferencesAndTypes();
      instrumentation.metric("resolveReferencesAndTypes", "complete");
      //} else {
      //  resolveReferencesAndTypes(targetLibrary);
      //}
      performConstantEvaluation();
      instrumentation.metric("performConstantEvaluation", "complete");
      instrumentation.metric("librariesInCycles", librariesInCycles.size());
      for (Library lib : librariesInCycles) {
        instrumentation.metric(
            "librariesInCycles-CompilationUnitSources-Size",
            lib.getCompilationUnitSources().size());
      }

      return targetLibrary.getLibraryElement();
    } finally {
      instrumentation.log(15); //Log if >= than 15ms
    }
  }

  /**
   * Create an object to represent the information about the library defined by the compilation unit
   * with the given source.
   * 
   * @param librarySource the source of the library's defining compilation unit
   * @return the library object that was created
   * @throws AnalysisException if the library source is not valid
   */
  protected Library createLibrary(Source librarySource) throws AnalysisException {
    Library library = new Library(analysisContext, errorListener, librarySource);
    libraryMap.put(librarySource, library);
    return library;
  }

  /**
   * Add a dependency to the given map from the referencing library to the referenced library.
   * 
   * @param dependencyMap the map to which the dependency is to be added
   * @param referencingLibrary the library that references the referenced library
   * @param referencedLibrary the library referenced by the referencing library
   */
  private void addDependencyToMap(HashMap<Library, ArrayList<Library>> dependencyMap,
      Library referencingLibrary, Library referencedLibrary) {
    ArrayList<Library> dependentLibraries = dependencyMap.get(referencedLibrary);
    if (dependentLibraries == null) {
      dependentLibraries = new ArrayList<Library>();
      dependencyMap.put(referencedLibrary, dependentLibraries);
    }
    dependentLibraries.add(referencingLibrary);
  }

  /**
   * Given a library that is part of a cycle that includes the root library, add to the given set of
   * libraries all of the libraries reachable from the root library that are also included in the
   * cycle.
   * 
   * @param library the library to be added to the collection of libraries in cycles
   * @param librariesInCycle a collection of the libraries that are in the cycle
   * @param dependencyMap a table mapping libraries to the collection of libraries from which those
   *          libraries are referenced
   */
  private void addLibrariesInCycle(Library library, Set<Library> librariesInCycle,
      HashMap<Library, ArrayList<Library>> dependencyMap) {
    if (librariesInCycle.add(library)) {
      ArrayList<Library> dependentLibraries = dependencyMap.get(library);
      if (dependentLibraries != null) {
        for (Library dependentLibrary : dependentLibraries) {
          addLibrariesInCycle(dependentLibrary, librariesInCycle, dependencyMap);
        }
      }
    }
  }

  /**
   * Add the given library, and all libraries reachable from it that have not already been visited,
   * to the given dependency map.
   * 
   * @param library the library currently being added to the dependency map
   * @param dependencyMap the dependency map being computed
   * @param visitedLibraries the libraries that have already been visited, used to prevent infinite
   *          recursion
   */
  private void addToDependencyMap(Library library,
      HashMap<Library, ArrayList<Library>> dependencyMap, Set<Library> visitedLibraries) {
    if (visitedLibraries.add(library)) {
      for (Library referencedLibrary : library.getImportsAndExports()) {
        addDependencyToMap(dependencyMap, library, referencedLibrary);
        addToDependencyMap(referencedLibrary, dependencyMap, visitedLibraries);
      }
      if (!library.getExplicitlyImportsCore() && library != coreLibrary) {
        addDependencyToMap(dependencyMap, library, coreLibrary);
      }
    }
  }

  /**
   * Build the element model representing the combinators declared by the given directive.
   * 
   * @param directive the directive that declares the combinators
   * @return an array containing the import combinators that were built
   */
  // TODO(brianwilkerson) Move with buildDirectiveModels().
  private NamespaceCombinator[] buildCombinators(NamespaceDirective directive) {
    ArrayList<NamespaceCombinator> combinators = new ArrayList<NamespaceCombinator>();
    for (Combinator combinator : directive.getCombinators()) {
      if (combinator instanceof HideCombinator) {
        HideElementCombinatorImpl hide = new HideElementCombinatorImpl();
        hide.setHiddenNames(getIdentifiers(((HideCombinator) combinator).getHiddenNames()));
        combinators.add(hide);
      } else {
        ShowElementCombinatorImpl show = new ShowElementCombinatorImpl();
        show.setOffset(combinator.getOffset());
        show.setEnd(combinator.getEnd());
        show.setShownNames(getIdentifiers(((ShowCombinator) combinator).getShownNames()));
        combinators.add(show);
      }
    }
    return combinators.toArray(new NamespaceCombinator[combinators.size()]);
  }

  /**
   * Every library now has a corresponding {@link LibraryElement}, so it is now possible to resolve
   * the import and export directives.
   * 
   * @throws AnalysisException if the defining compilation unit for any of the libraries could not
   *           be accessed
   */
  // TODO(brianwilkerson) The body of this method probably wants to be moved into a separate class.
  private void buildDirectiveModels() throws AnalysisException {
    for (Library library : librariesInCycles) {
      HashMap<String, PrefixElementImpl> nameToPrefixMap = new HashMap<String, PrefixElementImpl>();
      ArrayList<ImportElement> imports = new ArrayList<ImportElement>();
      ArrayList<ExportElement> exports = new ArrayList<ExportElement>();
      for (Directive directive : library.getDefiningCompilationUnit().getDirectives()) {
        if (directive instanceof ImportDirective) {
          ImportDirective importDirective = (ImportDirective) directive;
          String uriContent = importDirective.getUriContent();
          if (DartUriResolver.isDartExtUri(uriContent)) {
            library.getLibraryElement().setHasExtUri(true);
          }
          Source importedSource = importDirective.getSource();
          if (importedSource != null) {
            // The imported source will be null if the URI in the import directive was invalid.
            Library importedLibrary = libraryMap.get(importedSource);
            if (importedLibrary != null) {
              ImportElementImpl importElement = new ImportElementImpl(directive.getOffset());
              StringLiteral uriLiteral = importDirective.getUri();
              importElement.setUriOffset(uriLiteral.getOffset());
              importElement.setUriEnd(uriLiteral.getEnd());
              importElement.setUri(uriContent);
              importElement.setDeferred(importDirective.getDeferredToken() != null);
              importElement.setCombinators(buildCombinators(importDirective));
              LibraryElement importedLibraryElement = importedLibrary.getLibraryElement();
              if (importedLibraryElement != null) {
                importElement.setImportedLibrary(importedLibraryElement);
              }
              SimpleIdentifier prefixNode = ((ImportDirective) directive).getPrefix();
              if (prefixNode != null) {
                importElement.setPrefixOffset(prefixNode.getOffset());
                String prefixName = prefixNode.getName();
                PrefixElementImpl prefix = nameToPrefixMap.get(prefixName);
                if (prefix == null) {
                  prefix = new PrefixElementImpl(prefixNode);
                  nameToPrefixMap.put(prefixName, prefix);
                }
                importElement.setPrefix(prefix);
                prefixNode.setStaticElement(prefix);
              }
              directive.setElement(importElement);
              imports.add(importElement);

              if (analysisContext.computeKindOf(importedSource) != SourceKind.LIBRARY) {
                ErrorCode errorCode = importElement.isDeferred()
                    ? StaticWarningCode.IMPORT_OF_NON_LIBRARY
                    : CompileTimeErrorCode.IMPORT_OF_NON_LIBRARY;
                errorListener.onError(new AnalysisError(
                    library.getLibrarySource(),
                    uriLiteral.getOffset(),
                    uriLiteral.getLength(),
                    errorCode,
                    uriLiteral.toSource()));
              }
            }
          }
        } else if (directive instanceof ExportDirective) {
          ExportDirective exportDirective = (ExportDirective) directive;
          Source exportedSource = exportDirective.getSource();
          if (exportedSource != null) {
            // The exported source will be null if the URI in the export directive was invalid.
            Library exportedLibrary = libraryMap.get(exportedSource);
            if (exportedLibrary != null) {
              ExportElementImpl exportElement = new ExportElementImpl();
              StringLiteral uriLiteral = exportDirective.getUri();
              exportElement.setUriOffset(uriLiteral.getOffset());
              exportElement.setUriEnd(uriLiteral.getEnd());
              exportElement.setUri(exportDirective.getUriContent());
              exportElement.setCombinators(buildCombinators(exportDirective));
              LibraryElement exportedLibraryElement = exportedLibrary.getLibraryElement();
              if (exportedLibraryElement != null) {
                exportElement.setExportedLibrary(exportedLibraryElement);
              }
              directive.setElement(exportElement);
              exports.add(exportElement);

              if (analysisContext.computeKindOf(exportedSource) != SourceKind.LIBRARY) {
                errorListener.onError(new AnalysisError(
                    library.getLibrarySource(),
                    uriLiteral.getOffset(),
                    uriLiteral.getLength(),
                    CompileTimeErrorCode.EXPORT_OF_NON_LIBRARY,
                    uriLiteral.toSource()));
              }
            }
          }
        }
      }
      Source librarySource = library.getLibrarySource();
      if (!library.getExplicitlyImportsCore() && !coreLibrarySource.equals(librarySource)) {
        ImportElementImpl importElement = new ImportElementImpl(-1);
        importElement.setImportedLibrary(coreLibrary.getLibraryElement());
        importElement.setSynthetic(true);
        imports.add(importElement);
      }
      LibraryElementImpl libraryElement = library.getLibraryElement();
      libraryElement.setImports(imports.toArray(new ImportElement[imports.size()]));
      libraryElement.setExports(exports.toArray(new ExportElement[exports.size()]));
      if (libraryElement.getEntryPoint() == null) {
        Namespace namespace = new NamespaceBuilder().createExportNamespaceForLibrary(libraryElement);
        Element element = namespace.get(LibraryElementBuilder.ENTRY_POINT_NAME);
        if (element instanceof FunctionElement) {
          libraryElement.setEntryPoint((FunctionElement) element);
        }
      }
    }
  }

  /**
   * Build element models for all of the libraries in the current cycle.
   * 
   * @throws AnalysisException if any of the element models cannot be built
   */
  private void buildElementModels() throws AnalysisException {
    for (Library library : librariesInCycles) {
      LibraryElementBuilder builder = new LibraryElementBuilder(
          getAnalysisContext(),
          getErrorListener());
      LibraryElementImpl libraryElement = builder.buildLibrary(library);
      library.setLibraryElement(libraryElement);
    }
  }

  /**
   * Build the members in enum declarations. This cannot be done while building the rest of the
   * element model because it depends on being able to access core types, which cannot happen until
   * the rest of the element model has been built (when resolving the core library).
   * 
   * @throws AnalysisException if any of the enum members could not be built
   */
  private void buildEnumMembers() throws AnalysisException {
    TimeCounterHandle timeCounter = PerformanceStatistics.resolve.start();
    try {
      for (Library library : librariesInCycles) {
        for (Source source : library.getCompilationUnitSources()) {
          EnumMemberBuilder builder = new EnumMemberBuilder(typeProvider);
          library.getAST(source).accept(builder);
        }
      }
    } finally {
      timeCounter.stop();
    }
  }

  /**
   * Resolve the types referenced by function type aliases across all of the function type aliases
   * defined in the current cycle.
   * 
   * @throws AnalysisException if any of the function type aliases could not be resolved
   */
  private void buildTypeAliases() throws AnalysisException {
    TimeCounterHandle timeCounter = PerformanceStatistics.resolve.start();
    try {
      List<TypeAliasInfo> typeAliases = new ArrayList<TypeAliasInfo>();
      for (Library library : librariesInCycles) {
        for (Source source : library.getCompilationUnitSources()) {
          CompilationUnit ast = library.getAST(source);
          for (CompilationUnitMember member : ast.getDeclarations()) {
            if (member instanceof FunctionTypeAlias) {
              typeAliases.add(new TypeAliasInfo(library, source, (FunctionTypeAlias) member));
            }
          }
        }
      }
      // TODO(brianwilkerson) We need to sort the type aliases such that all aliases referenced by
      // an alias T are resolved before we resolve T.
      for (TypeAliasInfo info : typeAliases) {
        TypeResolverVisitor visitor = new TypeResolverVisitor(
            info.library,
            info.source,
            typeProvider);
        info.typeAlias.accept(visitor);
      }
    } finally {
      timeCounter.stop();
    }
  }

  /**
   * Resolve the type hierarchy across all of the types declared in the libraries in the current
   * cycle.
   * 
   * @throws AnalysisException if any of the type hierarchies could not be resolved
   */
  private void buildTypeHierarchies() throws AnalysisException {
    TimeCounterHandle timeCounter = PerformanceStatistics.resolve.start();
    try {
      for (Library library : librariesInCycles) {
        for (Source source : library.getCompilationUnitSources()) {
          TypeResolverVisitor visitor = new TypeResolverVisitor(library, source, typeProvider);
          library.getAST(source).accept(visitor);
        }
      }
    } finally {
      timeCounter.stop();
    }
  }

  /**
   * Compute a dependency map of libraries reachable from the given library. A dependency map is a
   * table that maps individual libraries to a list of the libraries that either import or export
   * those libraries.
   * <p>
   * This map is used to compute all of the libraries involved in a cycle that include the root
   * library. Given that we only add libraries that are reachable from the root library, when we
   * work backward we are guaranteed to only get libraries in the cycle.
   * 
   * @param library the library currently being added to the dependency map
   */
  private HashMap<Library, ArrayList<Library>> computeDependencyMap(Library library) {
    HashMap<Library, ArrayList<Library>> dependencyMap = new HashMap<Library, ArrayList<Library>>();
    addToDependencyMap(library, dependencyMap, new HashSet<Library>());
    return dependencyMap;
  }

  /**
   * Recursively traverse the libraries reachable from the given library, creating instances of the
   * class {@link Library} to represent them, and record the references in the library objects.
   * 
   * @param library the library to be processed to find libraries that have not yet been traversed
   * @throws AnalysisException if some portion of the library graph could not be traversed
   */
  private void computeEmbeddedLibraryDependencies(Library library, CompilationUnit unit)
      throws AnalysisException {
    Source librarySource = library.getLibrarySource();
    HashSet<Source> exportedSources = new HashSet<Source>();
    HashSet<Source> importedSources = new HashSet<Source>();
    for (Directive directive : unit.getDirectives()) {
      if (directive instanceof ExportDirective) {
        Source exportSource = resolveSource(librarySource, (ExportDirective) directive);
        if (exportSource != null) {
          exportedSources.add(exportSource);
        }
      } else if (directive instanceof ImportDirective) {
        Source importSource = resolveSource(librarySource, (ImportDirective) directive);
        if (importSource != null) {
          importedSources.add(importSource);
        }
      }
    }
    computeLibraryDependenciesFromDirectives(
        library,
        importedSources.toArray(new Source[importedSources.size()]),
        exportedSources.toArray(new Source[exportedSources.size()]));
  }

  /**
   * Return a collection containing all of the libraries reachable from the given library that are
   * contained in a cycle that includes the given library.
   * 
   * @param library the library that must be included in any cycles whose members are to be returned
   * @return all of the libraries referenced by the given library that have a circular reference
   *         back to the given library
   */
  private Set<Library> computeLibrariesInCycles(Library library) {
    HashMap<Library, ArrayList<Library>> dependencyMap = computeDependencyMap(library);
    Set<Library> librariesInCycle = new HashSet<Library>();
    addLibrariesInCycle(library, librariesInCycle, dependencyMap);
    return librariesInCycle;
  }

  /**
   * Recursively traverse the libraries reachable from the given library, creating instances of the
   * class {@link Library} to represent them, and record the references in the library objects.
   * 
   * @param library the library to be processed to find libraries that have not yet been traversed
   * @throws AnalysisException if some portion of the library graph could not be traversed
   */
  private void computeLibraryDependencies(Library library) throws AnalysisException {
    Source librarySource = library.getLibrarySource();
    computeLibraryDependenciesFromDirectives(
        library,
        analysisContext.computeImportedLibraries(librarySource),
        analysisContext.computeExportedLibraries(librarySource));
  }

  /**
   * Recursively traverse the libraries reachable from the given library, creating instances of the
   * class {@link Library} to represent them, and record the references in the library objects.
   * 
   * @param library the library to be processed to find libraries that have not yet been traversed
   * @param importedSources an array containing the sources that are imported into the given library
   * @param exportedSources an array containing the sources that are exported from the given library
   * @throws AnalysisException if some portion of the library graph could not be traversed
   */
  private void computeLibraryDependenciesFromDirectives(Library library, Source[] importedSources,
      Source[] exportedSources) throws AnalysisException {
    ArrayList<Library> importedLibraries = new ArrayList<Library>();
    boolean explicitlyImportsCore = false;
    for (Source importedSource : importedSources) {
      if (importedSource.equals(coreLibrarySource)) {
        explicitlyImportsCore = true;
      }
      Library importedLibrary = libraryMap.get(importedSource);
      if (importedLibrary == null) {
        importedLibrary = createLibraryOrNull(importedSource);
        if (importedLibrary != null) {
          computeLibraryDependencies(importedLibrary);
        }
      }
      if (importedLibrary != null) {
        importedLibraries.add(importedLibrary);
      }
    }
    library.setImportedLibraries(importedLibraries.toArray(new Library[importedLibraries.size()]));

    ArrayList<Library> exportedLibraries = new ArrayList<Library>();
    for (Source exportedSource : exportedSources) {
      Library exportedLibrary = libraryMap.get(exportedSource);
      if (exportedLibrary == null) {
        exportedLibrary = createLibraryOrNull(exportedSource);
        if (exportedLibrary != null) {
          computeLibraryDependencies(exportedLibrary);
        }
      }
      if (exportedLibrary != null) {
        exportedLibraries.add(exportedLibrary);
      }
    }
    library.setExportedLibraries(exportedLibraries.toArray(new Library[exportedLibraries.size()]));

    library.setExplicitlyImportsCore(explicitlyImportsCore);
    if (!explicitlyImportsCore && !coreLibrarySource.equals(library.getLibrarySource())) {
      Library importedLibrary = libraryMap.get(coreLibrarySource);
      if (importedLibrary == null) {
        importedLibrary = createLibraryOrNull(coreLibrarySource);
        if (importedLibrary != null) {
          computeLibraryDependencies(importedLibrary);
        }
      }
    }
  }

  /**
   * Create an object to represent the information about the library defined by the compilation unit
   * with the given source. Return the library object that was created, or {@code null} if the
   * source is not valid.
   * 
   * @param librarySource the source of the library's defining compilation unit
   * @return the library object that was created
   */
  private Library createLibraryOrNull(Source librarySource) {
    if (!analysisContext.exists(librarySource)) {
      return null;
    }
    Library library = new Library(analysisContext, errorListener, librarySource);
    libraryMap.put(librarySource, library);
    return library;
  }

  /**
   * Create an object to represent the information about the library defined by the compilation unit
   * with the given source.
   * 
   * @param librarySource the source of the library's defining compilation unit
   * @param modificationStamp the modification time of the source from which the compilation unit
   *          was created
   * @param unit the compilation unit that defines the library
   * @return the library object that was created
   * @throws AnalysisException if the library source is not valid
   */
  private Library createLibraryWithUnit(Source librarySource, long modificationStamp,
      CompilationUnit unit) throws AnalysisException {
    Library library = new Library(analysisContext, errorListener, librarySource);
    library.setDefiningCompilationUnit(modificationStamp, unit);
    libraryMap.put(librarySource, library);
    return library;
  }

  /**
   * Return an array containing the lexical identifiers associated with the nodes in the given list.
   * 
   * @param names the AST nodes representing the identifiers
   * @return the lexical identifiers associated with the nodes in the list
   */
  // TODO(brianwilkerson) Move with buildDirectiveModels().
  private String[] getIdentifiers(NodeList<SimpleIdentifier> names) {
    int count = names.size();
    String[] identifiers = new String[count];
    for (int i = 0; i < count; i++) {
      identifiers[i] = names.get(i).getName();
    }
    return identifiers;
  }

  /**
   * Compute a value for all of the constants in the libraries being analyzed.
   */
  private void performConstantEvaluation() {
    TimeCounterHandle timeCounter = PerformanceStatistics.resolve.start();
    try {
      ConstantValueComputer computer = new ConstantValueComputer(
          typeProvider,
          analysisContext.getDeclaredVariables());
      for (Library library : librariesInCycles) {
        for (Source source : library.getCompilationUnitSources()) {
          try {
            CompilationUnit unit = library.getAST(source);
            if (unit != null) {
              computer.add(unit);
            }
          } catch (AnalysisException exception) {
            AnalysisEngine.getInstance().getLogger().logError(
                "Internal Error: Could not access AST for " + source.getFullName()
                    + " during constant evaluation",
                exception);
          }
        }
      }
      computer.computeValues();
    } finally {
      timeCounter.stop();
    }
  }

  /**
   * Resolve the identifiers and perform type analysis in the libraries in the current cycle.
   * 
   * @throws AnalysisException if any of the identifiers could not be resolved or if any of the
   *           libraries could not have their types analyzed
   */
  private void resolveReferencesAndTypes() throws AnalysisException {
    for (Library library : librariesInCycles) {
      resolveReferencesAndTypesInLibrary(library);
    }
  }

  /**
   * Resolve the identifiers and perform type analysis in the given library.
   * 
   * @param library the library to be resolved
   * @throws AnalysisException if any of the identifiers could not be resolved or if the types in
   *           the library cannot be analyzed
   */
  private void resolveReferencesAndTypesInLibrary(Library library) throws AnalysisException {
    TimeCounterHandle timeCounter = PerformanceStatistics.resolve.start();
    try {
      for (Source source : library.getCompilationUnitSources()) {
        CompilationUnit ast = library.getAST(source);
        ast.accept(new VariableResolverVisitor(library, source, typeProvider));
        ResolverVisitor visitor = new ResolverVisitor(library, source, typeProvider);
        ast.accept(visitor);
      }
    } finally {
      timeCounter.stop();
    }
    // Angular
    timeCounter = PerformanceStatistics.angular.start();
    try {
      for (Source source : library.getCompilationUnitSources()) {
        CompilationUnit ast = library.getAST(source);
        new AngularCompilationUnitBuilder(errorListener, source, ast).build();
      }
    } finally {
      timeCounter.stop();
    }
    // Polymer
    timeCounter = PerformanceStatistics.polymer.start();
    try {
      for (Source source : library.getCompilationUnitSources()) {
        CompilationUnit ast = library.getAST(source);
        new PolymerCompilationUnitBuilder(ast).build();
      }
    } finally {
      timeCounter.stop();
    }
  }

  /**
   * Return the result of resolving the URI of the given URI-based directive against the URI of the
   * given library, or {@code null} if the URI is not valid.
   * 
   * @param librarySource the source representing the library containing the directive
   * @param directive the directive which URI should be resolved
   * @return the result of resolving the URI against the URI of the library
   */
  private Source resolveSource(Source librarySource, UriBasedDirective directive) {
    StringLiteral uriLiteral = directive.getUri();
    if (uriLiteral instanceof StringInterpolation) {
      return null;
    }
    String uriContent = uriLiteral.getStringValue().trim();
    if (uriContent == null || uriContent.isEmpty()) {
      return null;
    }
    uriContent = UriUtilities.encode(uriContent);
    return analysisContext.getSourceFactory().resolveUri(librarySource, uriContent);
  }
}
