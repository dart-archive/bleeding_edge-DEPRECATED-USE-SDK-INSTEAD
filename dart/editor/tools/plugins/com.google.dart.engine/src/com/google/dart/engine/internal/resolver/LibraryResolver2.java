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
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.ast.TypeAlias;
import com.google.dart.engine.context.AnalysisContext;
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
import com.google.dart.engine.internal.context.ResolvableCompilationUnit;
import com.google.dart.engine.internal.element.ExportElementImpl;
import com.google.dart.engine.internal.element.HideElementCombinatorImpl;
import com.google.dart.engine.internal.element.ImportElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.PrefixElementImpl;
import com.google.dart.engine.internal.element.ShowElementCombinatorImpl;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.internal.scope.NamespaceBuilder;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.engine.utilities.general.TimeCounter.TimeCounterHandle;
import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.engine.utilities.translation.DartBlockBody;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Instances of the class {@code LibraryResolver} are used to resolve one or more mutually dependent
 * libraries within a single context.
 * 
 * @coverage dart.engine.resolver
 */
public class LibraryResolver2 {
  /**
   * Instances of the class {@code TypeAliasInfo} hold information about a {@link TypeAlias}.
   */
  private static class TypeAliasInfo {
    private ResolvableLibrary library;
    private Source source;
    private FunctionTypeAlias typeAlias;

    /**
     * Initialize a newly created information holder with the given information.
     * 
     * @param library the library containing the type alias
     * @param source the source of the file containing the type alias
     * @param typeAlias the type alias being remembered
     */
    public TypeAliasInfo(ResolvableLibrary library, Source source, FunctionTypeAlias typeAlias) {
      this.library = library;
      this.source = source;
      this.typeAlias = typeAlias;
    }
  }

  /**
   * Report that the core library could not be resolved in the given analysis context and throw an
   * exception.
   * 
   * @param analysisContext the analysis context in which the failure occurred
   * @param coreLibrarySource the source representing the core library
   * @throws AnalysisException always
   */
  @DartBlockBody({"throw new AnalysisException(\"Could not resolve dart:core\");"})
  public static void missingCoreLibrary(AnalysisContext analysisContext, Source coreLibrarySource)
      throws AnalysisException {
    InstrumentationBuilder instrumentation = Instrumentation.builder("ErrorNoCoreLibrary");
    try {
      DartSdk sdk = analysisContext.getSourceFactory().getDartSdk();
      if (sdk == null) {
        instrumentation.data("sdkPath", "--null--");
      } else if (sdk instanceof DirectoryBasedDartSdk) {
        File directory = ((DirectoryBasedDartSdk) sdk).getDirectory();
        if (directory == null) {
          instrumentation.data("sdkDirectoryIsNull", true);
        } else {
          instrumentation.data("sdkDirectoryIsNull", false);
          instrumentation.data("sdkPath", directory.getAbsolutePath());
          instrumentation.data("sdkDirectoryExists", directory.exists());
        }
      } else {
        instrumentation.data("sdkPath", "--unknown--");
      }
      instrumentation.data("coreLibraryPath", coreLibrarySource.getFullName());
    } finally {
      instrumentation.log();
    }
    throw new AnalysisException("Could not resolve dart:core");
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
  private ResolvableLibrary coreLibrary;

  /**
   * The object used to access the types from the core library.
   */
  private TypeProvider typeProvider;

  /**
   * A table mapping library sources to the information being maintained for those libraries.
   */
  private HashMap<Source, ResolvableLibrary> libraryMap = new HashMap<Source, ResolvableLibrary>();

  /**
   * A collection containing the libraries that are being resolved together.
   */
  private List<ResolvableLibrary> librariesInCycle;

  /**
   * Initialize a newly created library resolver to resolve libraries within the given context.
   * 
   * @param analysisContext the analysis context in which the library is being analyzed
   */
  public LibraryResolver2(InternalAnalysisContext analysisContext) {
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
  public List<ResolvableLibrary> getResolvedLibraries() {
    return librariesInCycle;
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
  public LibraryElement resolveLibrary(Source librarySource,
      List<ResolvableLibrary> librariesInCycle) throws AnalysisException {
    InstrumentationBuilder instrumentation = Instrumentation.builder("dart.engine.LibraryResolver.resolveLibrary");
    try {
      instrumentation.data("fullName", librarySource.getFullName());
      //
      // Build the map of libraries that are known.
      //
      this.librariesInCycle = librariesInCycle;
      libraryMap = buildLibraryMap();
      ResolvableLibrary targetLibrary = libraryMap.get(librarySource);
      coreLibrary = libraryMap.get(coreLibrarySource);
      instrumentation.metric("buildLibraryMap", "complete");
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
        missingCoreLibrary(analysisContext, coreLibrarySource);
      }
      buildDirectiveModels();
      instrumentation.metric("buildDirectiveModels", "complete");
      typeProvider = new TypeProviderImpl(coreElement);
      buildEnumMembers();
      buildTypeAliases();
      buildTypeHierarchies();
      buildImplicitConstructors();
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
      instrumentation.metric("librariesInCycles", librariesInCycle.size());
      for (ResolvableLibrary lib : librariesInCycle) {
        instrumentation.metric(
            "librariesInCycles-CompilationUnitSources-Size",
            lib.getCompilationUnitSources().length);
      }

      return targetLibrary.getLibraryElement();
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
      for (ResolvableLibrary library : librariesInCycle) {
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
   * Finish steps that the {@link #buildTypeHierarchies()} could not perform, see
   * {@link ImplicitConstructorBuilder}.
   * 
   * @throws AnalysisException if any of the type hierarchies could not be resolved
   */
  private void buildImplicitConstructors() throws AnalysisException {
    TimeCounterHandle timeCounter = PerformanceStatistics.resolve.start();
    try {
      for (ResolvableLibrary library : librariesInCycle) {
        for (ResolvableCompilationUnit unit : library.getResolvableCompilationUnits()) {
          Source source = unit.getSource();
          CompilationUnit ast = unit.getCompilationUnit();
          ImplicitConstructorBuilder visitor = new ImplicitConstructorBuilder(
              library,
              source,
              typeProvider);
          ast.accept(visitor);
        }
      }
    } finally {
      timeCounter.stop();
    }
  }

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
   * Resolve the types referenced by function type aliases across all of the function type aliases
   * defined in the current cycle.
   * 
   * @throws AnalysisException if any of the function type aliases could not be resolved
   */
  private void buildTypeAliases() throws AnalysisException {
    TimeCounterHandle timeCounter = PerformanceStatistics.resolve.start();
    try {
      List<TypeAliasInfo> typeAliases = new ArrayList<TypeAliasInfo>();
      for (ResolvableLibrary library : librariesInCycle) {
        for (ResolvableCompilationUnit unit : library.getResolvableCompilationUnits()) {
          for (CompilationUnitMember member : unit.getCompilationUnit().getDeclarations()) {
            if (member instanceof FunctionTypeAlias) {
              typeAliases.add(new TypeAliasInfo(
                  library,
                  unit.getSource(),
                  (FunctionTypeAlias) member));
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
      for (ResolvableLibrary library : librariesInCycle) {
        for (ResolvableCompilationUnit unit : library.getResolvableCompilationUnits()) {
          CompilationUnit ast = unit.getCompilationUnit();
          if (ast != null) {
            computer.add(ast);
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
    for (ResolvableLibrary library : librariesInCycle) {
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
  private void resolveReferencesAndTypesInLibrary(ResolvableLibrary library)
      throws AnalysisException {
    TimeCounterHandle timeCounter = PerformanceStatistics.resolve.start();
    try {
      for (ResolvableCompilationUnit unit : library.getResolvableCompilationUnits()) {
        Source source = unit.getSource();
        CompilationUnit ast = unit.getCompilationUnit();
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
      for (ResolvableCompilationUnit unit : library.getResolvableCompilationUnits()) {
        Source source = unit.getSource();
        CompilationUnit ast = unit.getCompilationUnit();
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
}
