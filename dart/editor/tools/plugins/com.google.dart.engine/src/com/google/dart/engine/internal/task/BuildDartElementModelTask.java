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
package com.google.dart.engine.internal.task;

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
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExportElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.NamespaceCombinator;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.context.PerformanceStatistics;
import com.google.dart.engine.internal.context.RecordingErrorListener;
import com.google.dart.engine.internal.context.ResolvableCompilationUnit;
import com.google.dart.engine.internal.element.ExportElementImpl;
import com.google.dart.engine.internal.element.HideElementCombinatorImpl;
import com.google.dart.engine.internal.element.ImportElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.PrefixElementImpl;
import com.google.dart.engine.internal.element.ShowElementCombinatorImpl;
import com.google.dart.engine.internal.resolver.LibraryElementBuilder;
import com.google.dart.engine.internal.resolver.ResolvableLibrary;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.internal.resolver.TypeProviderImpl;
import com.google.dart.engine.internal.resolver.TypeResolverVisitor;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.internal.scope.NamespaceBuilder;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.engine.utilities.general.TimeCounter.TimeCounterHandle;
import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Instances of the class {@code BuildDartElementModelTask} build the element models for all of the
 * libraries in a cycle.
 */
public class BuildDartElementModelTask extends AnalysisTask {
  /**
   * The library for which an element model was originally requested.
   */
  private Source targetLibrary;

  /**
   * The libraries that are part of the cycle to be resolved.
   */
  private List<ResolvableLibrary> librariesInCycle;

  /**
   * The listener to which analysis errors will be reported.
   */
  private RecordingErrorListener errorListener;

  /**
   * A source object representing the core library (dart:core).
   */
  private Source coreLibrarySource;

  /**
   * The object representing the core library.
   */
  private ResolvableLibrary coreLibrary;

  /**
   * A table mapping library sources to the information being maintained for those libraries.
   */
  private HashMap<Source, ResolvableLibrary> libraryMap = new HashMap<Source, ResolvableLibrary>();

  /**
   * Initialize a newly created task to perform analysis within the given context.
   * 
   * @param context the context in which the task is to be performed
   * @param targetLibrary the library for which an element model was originally requested
   * @param librariesInCycle the libraries that are part of the cycle to be resolved
   */
  public BuildDartElementModelTask(InternalAnalysisContext context, Source targetLibrary,
      List<ResolvableLibrary> librariesInCycle) {
    super(context);
    this.targetLibrary = targetLibrary;
    this.librariesInCycle = librariesInCycle;
    this.errorListener = new RecordingErrorListener();
    coreLibrarySource = context.getSourceFactory().forUri(DartSdk.DART_CORE);
  }

  @Override
  public <E> E accept(AnalysisTaskVisitor<E> visitor) throws AnalysisException {
    return visitor.visitBuildDartElementModelTask(this);
  }

  /**
   * Return the listener to which analysis errors were (or will be) reported.
   * 
   * @return the listener to which analysis errors were reported
   */
  public RecordingErrorListener getErrorListener() {
    return errorListener;
  }

  /**
   * Return the libraries that are part of the cycle to be resolved.
   * 
   * @return the libraries that are part of the cycle to be resolved
   */
  public List<ResolvableLibrary> getLibrariesInCycle() {
    return librariesInCycle;
  }

  /**
   * Return the library for which an element model was originally requested.
   * 
   * @return the library for which an element model was originally requested
   */
  public Source getTargetLibrary() {
    return targetLibrary;
  }

  @Override
  protected String getTaskDescription() {
    Source librarySource = librariesInCycle.get(0).getLibrarySource();
    if (librarySource == null) {
      return "build an element model for unknown library";
    }
    return "build an element model for " + librarySource.getFullName();
  }

  @Override
  protected void internalPerform() throws AnalysisException {
    InstrumentationBuilder instrumentation = Instrumentation.builder("dart.engine.BuildDartElementModel.internalPerform");
    try {
      //
      // Build the map of libraries that are known.
      //
      libraryMap = buildLibraryMap();
      coreLibrary = libraryMap.get(coreLibrarySource);
      LibraryElement coreElement = coreLibrary.getLibraryElement();
      if (coreElement == null) {
        throw new AnalysisException("Could not resolve dart:core");
      }
      instrumentation.metric("buildLibraryMap", "complete");
      //
      // Build the element models representing the libraries being resolved. This is done in three
      // steps.
      //
      // 1. Build the basic element models without making any connections between elements other than
      //    the basic parent/child relationships. This includes building the elements representing the
      //    libraries.
      //
      buildElementModels();
      instrumentation.metric("buildElementModels", "complete");
      //
      // 2. Build the elements for the import and export directives. This requires that we have the
      //    elements built for the referenced libraries, but because of the possibility of circular
      //    references needs to happen after all of the library elements have been created.
      //
      buildDirectiveModels();
      instrumentation.metric("buildDirectiveModels", "complete");
      //
      // 3. Build the rest of the type model by connecting superclasses, mixins, and interfaces. This
      //    requires that we be able to compute the names visible in the libraries being resolved,
      //    which in turn requires that we have resolved the import directives.
      //
      buildTypeHierarchies(new TypeProviderImpl(coreElement));
      instrumentation.metric("buildTypeHierarchies", "complete");
      instrumentation.metric("librariesInCycles", librariesInCycle.size());
      for (ResolvableLibrary lib : librariesInCycle) {
        instrumentation.metric(
            "librariesInCycles-CompilationUnitSources-Size",
            lib.getCompilationUnitSources().length);
      }
    } finally {
      instrumentation.log();
    }
  }

  /**
   * Build the element model representing the combinators declared by the given directive.
   * 
   * @param directive the directive that declares the combinators
   * @return an array containing the import combinators that were built
   */
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
  private void buildDirectiveModels() throws AnalysisException {
    AnalysisContext analysisContext = getContext();
    for (ResolvableLibrary library : librariesInCycle) {
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
          if (importedSource != null && analysisContext.exists(importedSource)) {
            // The imported source will be null if the URI in the import directive was invalid.
            ResolvableLibrary importedLibrary = libraryMap.get(importedSource);
            if (importedLibrary != null) {
              ImportElementImpl importElement = new ImportElementImpl(directive.getOffset());
              StringLiteral uriLiteral = importDirective.getUri();
              if (uriLiteral != null) {
                importElement.setUriOffset(uriLiteral.getOffset());
                importElement.setUriEnd(uriLiteral.getEnd());
              }
              importElement.setUri(uriContent);
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
          if (exportedSource != null && analysisContext.exists(exportedSource)) {
            // The exported source will be null if the URI in the export directive was invalid.
            ResolvableLibrary exportedLibrary = libraryMap.get(exportedSource);
            if (exportedLibrary != null) {
              ExportElementImpl exportElement = new ExportElementImpl();
              StringLiteral uriLiteral = exportDirective.getUri();
              if (uriLiteral != null) {
                exportElement.setUriOffset(uriLiteral.getOffset());
                exportElement.setUriEnd(uriLiteral.getEnd());
              }
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
    for (ResolvableLibrary library : librariesInCycle) {
      LibraryElementBuilder builder = new LibraryElementBuilder(getContext(), errorListener);
      LibraryElementImpl libraryElement = builder.buildLibrary(library);
      library.setLibraryElement(libraryElement);
    }
  }

  /**
   * Build a table mapping library sources to the resolvable libraries representing those libraries.
   * 
   * @return the map that was built
   */
  private HashMap<Source, ResolvableLibrary> buildLibraryMap() {
    HashMap<Source, ResolvableLibrary> libraryMap = new HashMap<Source, ResolvableLibrary>();
    int libraryCount = librariesInCycle.size();
    for (int i = 0; i < libraryCount; i++) {
      ResolvableLibrary library = librariesInCycle.get(i);
      library.setErrorListener(errorListener);
      libraryMap.put(library.getLibrarySource(), library);
      ResolvableLibrary[] dependencies = library.getImportsAndExports();
      int dependencyCount = dependencies.length;
      for (int j = 0; j < dependencyCount; j++) {
        ResolvableLibrary dependency = dependencies[j];
        //dependency.setErrorListener(errorListener);
        libraryMap.put(dependency.getLibrarySource(), dependency);
      }
    }
    return libraryMap;
  }

  /**
   * Resolve the type hierarchy across all of the types declared in the libraries in the current
   * cycle.
   * 
   * @throws AnalysisException if any of the type hierarchies could not be resolved
   */
  private void buildTypeHierarchies(TypeProvider typeProvider) throws AnalysisException {
    TimeCounterHandle timeCounter = PerformanceStatistics.resolve.start();
    try {
      for (ResolvableLibrary library : librariesInCycle) {
        for (ResolvableCompilationUnit unit : library.getResolvableCompilationUnits()) {
          Source source = unit.getSource();
          CompilationUnit ast = unit.getCompilationUnit();
          TypeResolverVisitor visitor = new TypeResolverVisitor(library, source, typeProvider);
          ast.accept(visitor);
        }
      }
    } finally {
      timeCounter.stop();
    }
  }

  /**
   * Return an array containing the lexical identifiers associated with the nodes in the given list.
   * 
   * @param names the AST nodes representing the identifiers
   * @return the lexical identifiers associated with the nodes in the list
   */
  private String[] getIdentifiers(NodeList<SimpleIdentifier> names) {
    int count = names.size();
    String[] identifiers = new String[count];
    for (int i = 0; i < count; i++) {
      identifiers[i] = names.get(i).getName();
    }
    return identifiers;
  }
}
