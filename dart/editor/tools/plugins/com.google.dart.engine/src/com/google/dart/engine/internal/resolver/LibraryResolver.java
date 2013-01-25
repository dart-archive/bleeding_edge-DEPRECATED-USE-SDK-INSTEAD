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

import com.google.dart.engine.ast.Combinator;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.HideCombinator;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.NamespaceDirective;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.ShowCombinator;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ExportElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.NamespaceCombinator;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.element.ExportElementImpl;
import com.google.dart.engine.internal.element.HideCombinatorImpl;
import com.google.dart.engine.internal.element.ImportElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.PrefixElementImpl;
import com.google.dart.engine.internal.element.ShowCombinatorImpl;
import com.google.dart.engine.source.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Instances of the class {@code LibraryResolver} are used to resolve one or more mutually dependent
 * libraries within a single context.
 */
public class LibraryResolver {
  /**
   * The analysis context in which the libraries are being analyzed.
   */
  private AnalysisContextImpl analysisContext;

  /**
   * The listener to which analysis errors will be reported.
   */
  private AnalysisErrorListener errorListener;

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
   * @param errorListener the listener to which analysis errors will be reported
   */
  public LibraryResolver(AnalysisContextImpl analysisContext, AnalysisErrorListener errorListener) {
    this.analysisContext = analysisContext;
    this.errorListener = errorListener;
    coreLibrarySource = analysisContext.getSourceFactory().forUri(
        LibraryElementBuilder.CORE_LIBRARY_URI);
  }

  /**
   * Return the analysis context in which the libraries are being analyzed.
   * 
   * @return the analysis context in which the libraries are being analyzed
   */
  public AnalysisContextImpl getAnalysisContext() {
    return analysisContext;
  }

  /**
   * Return the listener to which analysis errors will be reported.
   * 
   * @return the listener to which analysis errors will be reported
   */
  public AnalysisErrorListener getErrorListener() {
    return errorListener;
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
    //
    // Create the objects representing the library being resolved and the core library.
    //
    Library targetLibrary = createLibrary(librarySource);
    coreLibrary = libraryMap.get(coreLibrarySource);
    if (coreLibrary == null) {
      // This will be true unless the library being analyzed is the core library.
      coreLibrary = createLibrary(coreLibrarySource);
    }
    //
    // Compute the set of libraries that need to be resolved together.
    //
    computeLibraryDependencies(targetLibrary);
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
    //    step only depends on the first step and could be done in parallel with the second step.
    //
    buildElementModels();
    buildDirectiveModels();
    typeProvider = new TypeProviderImpl(coreLibrary.getLibraryElement());
    buildTypeHierarchies();
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
    //} else {
    //  resolveReferencesAndTypes(targetLibrary);
    //}
    if (fullAnalysis) {
      //
      // TODO(brianwilkerson) Run additional analyses, such as constant expression analysis.
      //
    }
    recordLibraryElements();
    return targetLibrary.getLibraryElement();
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
        HideCombinatorImpl hide = new HideCombinatorImpl();
        hide.setHiddenNames(getIdentifiers(((HideCombinator) combinator).getHiddenNames()));
        combinators.add(hide);
      } else {
        ShowCombinatorImpl show = new ShowCombinatorImpl();
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
          Library importedLibrary = library.getImport(importDirective);
          ImportElementImpl importElement = new ImportElementImpl();
          importElement.setCombinators(buildCombinators(importDirective));
          LibraryElement importedLibraryElement = importedLibrary.getLibraryElement();
          if (importedLibraryElement != null) {
            importElement.setImportedLibrary(importedLibraryElement);
            directive.setElement(importedLibraryElement);
          }
          SimpleIdentifier prefixNode = ((ImportDirective) directive).getPrefix();
          if (prefixNode != null) {
            String prefixName = prefixNode.getName();
            PrefixElementImpl prefix = nameToPrefixMap.get(prefixName);
            if (prefix == null) {
              prefix = new PrefixElementImpl(prefixNode);
              nameToPrefixMap.put(prefixName, prefix);
            }
            importElement.setPrefix(prefix);
          }
          imports.add(importElement);
        } else if (directive instanceof ExportDirective) {
          ExportDirective exportDirective = (ExportDirective) directive;
          ExportElementImpl exportElement = new ExportElementImpl();
          exportElement.setCombinators(buildCombinators(exportDirective));
          LibraryElement exportedLibrary = library.getExport(exportDirective).getLibraryElement();
          if (exportedLibrary != null) {
            exportElement.setExportedLibrary(exportedLibrary);
            directive.setElement(exportedLibrary);
          }
          exports.add(exportElement);
        }
      }
      Source librarySource = library.getLibrarySource();
      if (!library.getExplicitlyImportsCore() && !coreLibrarySource.equals(librarySource)) {
        ImportElementImpl importElement = new ImportElementImpl();
        importElement.setImportedLibrary(coreLibrary.getLibraryElement());
        importElement.setSynthetic(true);
        imports.add(importElement);
      }
      LibraryElementImpl libraryElement = library.getLibraryElement();
      libraryElement.setImports(imports.toArray(new ImportElement[imports.size()]));
      libraryElement.setExports(exports.toArray(new ExportElement[exports.size()]));
    }
  }

  /**
   * Build element models for all of the libraries in the current cycle.
   * 
   * @throws AnalysisException if any of the element models cannot be built
   */
  private void buildElementModels() throws AnalysisException {
    for (Library library : librariesInCycles) {
      LibraryElementBuilder builder = new LibraryElementBuilder(this);
      LibraryElementImpl libraryElement = builder.buildLibrary(library);
      library.setLibraryElement(libraryElement);
    }
  }

  /**
   * Resolve the type hierarchy across all of the types declared in the libraries in the current
   * cycle.
   * 
   * @throws AnalysisException if any of the type hierarchies could not be resolved
   */
  private void buildTypeHierarchies() throws AnalysisException {
    for (Library library : librariesInCycles) {
      for (Source source : library.getCompilationUnitSources()) {
        TypeResolverVisitor visitor = new TypeResolverVisitor(library, source, typeProvider);
        library.getAST(source).accept(visitor);
      }
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
   * @param library the library to be processed to find libaries that have not yet been traversed
   * @throws AnalysisException if some portion of the library graph could not be traversed
   */
  private void computeLibraryDependencies(Library library) throws AnalysisException {
    boolean explicitlyImportsCore = false;
    CompilationUnit unit = library.getDefiningCompilationUnit();
    for (Directive directive : unit.getDirectives()) {
      if (directive instanceof ImportDirective) {
        ImportDirective importDirective = (ImportDirective) directive;
        Source importedSource = library.getSource(importDirective.getLibraryUri());
        if (importedSource.equals(coreLibrarySource)) {
          explicitlyImportsCore = true;
        }
        Library importedLibrary = libraryMap.get(importedSource);
        if (importedLibrary == null) {
          importedLibrary = createLibrary(importedSource);
          computeLibraryDependencies(importedLibrary);
        }
        library.addImport(importDirective, importedLibrary);
      } else if (directive instanceof ExportDirective) {
        ExportDirective exportDirective = (ExportDirective) directive;
        Source exportedSource = library.getSource(exportDirective.getLibraryUri());
        Library exportedLibrary = libraryMap.get(exportedSource);
        if (exportedLibrary == null) {
          exportedLibrary = createLibrary(exportedSource);
          computeLibraryDependencies(exportedLibrary);
        }
        library.addExport(exportDirective, exportedLibrary);
      }
    }
    library.setExplicitlyImportsCore(explicitlyImportsCore);
    if (!explicitlyImportsCore && !coreLibrarySource.equals(library.getLibrarySource())) {
      Library importedLibrary = libraryMap.get(coreLibrarySource);
      if (importedLibrary == null) {
        importedLibrary = createLibrary(coreLibrarySource);
        computeLibraryDependencies(importedLibrary);
      }
    }
  }

  /**
   * Create an object to represent the information about the library defined by the compilation unit
   * with the given source.
   * 
   * @param librarySource the source of the library's defining compilation unit
   * @return the library object that was created
   */
  private Library createLibrary(Source librarySource) {
    Library library = new Library(analysisContext, errorListener, librarySource);
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
   * As the final step in the process, record the resolved element models with the analysis context.
   */
  private void recordLibraryElements() {
    HashMap<Source, LibraryElement> elementMap = new HashMap<Source, LibraryElement>();
    for (Library library : librariesInCycles) {
      elementMap.put(library.getLibrarySource(), library.getLibraryElement());
    }
    analysisContext.recordLibraryElements(elementMap);
  }

  /**
   * Resolve the identifiers and perform type analysis in the libraries in the current cycle.
   * 
   * @throws AnalysisException if any of the identifiers could not be resolved or if any of the
   *           libraries could not have their types analyzed
   */
  private void resolveReferencesAndTypes() throws AnalysisException {
    for (Library library : librariesInCycles) {
      resolveReferencesAndTypes(library);
    }
  }

  /**
   * Resolve the identifiers and perform type analysis in the given library.
   * 
   * @param library the library to be resolved
   * @throws AnalysisException if any of the identifiers could not be resolved or if the types in
   *           the library cannot be analyzed
   */
  private void resolveReferencesAndTypes(Library library) throws AnalysisException {
    for (Source source : library.getCompilationUnitSources()) {
      ResolverVisitor visitor = new ResolverVisitor(library, source, typeProvider);
      library.getAST(source).accept(visitor);
    }
  }
}
