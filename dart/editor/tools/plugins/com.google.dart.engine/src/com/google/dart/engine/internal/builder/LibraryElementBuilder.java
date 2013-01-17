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
package com.google.dart.engine.internal.builder;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.AdjacentStrings;
import com.google.dart.engine.ast.Combinator;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.HideCombinator;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.LibraryIdentifier;
import com.google.dart.engine.ast.NamespaceDirective;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.ast.ShowCombinator;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ExportElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.NamespaceCombinator;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.ExportElementImpl;
import com.google.dart.engine.internal.element.HideCombinatorImpl;
import com.google.dart.engine.internal.element.ImportElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.PrefixElementImpl;
import com.google.dart.engine.internal.element.ShowCombinatorImpl;
import com.google.dart.engine.resolver.ResolverErrorCode;
import com.google.dart.engine.source.Source;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Instances of the class {@code LibraryElementBuilder} build an element model for a single library.
 */
public class LibraryElementBuilder {
  /**
   * The analysis context in which the element model will be built.
   */
  private AnalysisContextImpl analysisContext;

  /**
   * The listener to which errors will be reported.
   */
  private AnalysisErrorListener errorListener;

  /**
   * The name of the core library.
   */
  // TODO(brianwilkerson) Decide where this should really be defined. Perhaps DartSDK?
  private static final String CORE_LIBRARY_NAME = "dart:core";

  /**
   * The name of the function used as an entry point.
   */
  private static final String ENTRY_POINT_NAME = "main";

  /**
   * Initialize a newly created library element builder.
   * 
   * @param analysisContext the analysis context in which the element model will be built
   * @param errorListener the listener to which errors will be reported
   */
  public LibraryElementBuilder(AnalysisContextImpl analysisContext,
      AnalysisErrorListener errorListener) {
    this.analysisContext = analysisContext;
    this.errorListener = errorListener;
  }

  /**
   * Build the library element for the given source.
   * 
   * @param librarySource the source describing the defining compilation unit for the library
   * @return the library element that was built
   * @throws AnalysisException if the analysis could not be performed
   */
  public LibraryElement buildLibrary(Source librarySource) throws AnalysisException {
    CompilationUnitBuilder builder = new CompilationUnitBuilder(analysisContext, errorListener);
    CompilationUnit definingCompilationUnit = analysisContext.parse(librarySource, errorListener);
    CompilationUnitElementImpl definingCompilationUnitElement = builder.buildCompilationUnit(
        librarySource,
        definingCompilationUnit);
    NodeList<Directive> directives = definingCompilationUnit.getDirectives();
    LibraryIdentifier libraryNameNode = null;
    boolean hasPartDirective = false;
    boolean explicitlyImportsCore = false;
    FunctionElement entryPoint = findEntryPoint(definingCompilationUnitElement);
    ArrayList<ImportElement> imports = new ArrayList<ImportElement>();
    ArrayList<ExportElement> exports = new ArrayList<ExportElement>();
    HashMap<String, PrefixElementImpl> nameToPrefixMap = new HashMap<String, PrefixElementImpl>();
    HashMap<PrefixElementImpl, ArrayList<ImportElement>> prefixToImportMap = new HashMap<PrefixElementImpl, ArrayList<ImportElement>>();
    ArrayList<Directive> directivesToResolve = new ArrayList<Directive>();
    ArrayList<CompilationUnitElementImpl> sourcedCompilationUnits = new ArrayList<CompilationUnitElementImpl>();
    for (Directive directive : directives) {
      if (directive instanceof LibraryDirective) {
        if (libraryNameNode == null) {
          libraryNameNode = ((LibraryDirective) directive).getName();
          directivesToResolve.add(directive);
        }
      } else if (directive instanceof NamespaceDirective) {
        NamespaceDirective namespaceDirective = (NamespaceDirective) directive;
        if (directive instanceof ImportDirective) {
          String uri = getStringValue(namespaceDirective.getLibraryUri());
          if (uri != null && CORE_LIBRARY_NAME.equals(uri)) {
            explicitlyImportsCore = true;
          }
          ImportElementImpl specification = new ImportElementImpl();
          specification.setCombinators(buildCombinators(namespaceDirective));
          LibraryElement importedLibrary = getReferencedLibrary(
              librarySource,
              namespaceDirective.getLibraryUri());
          if (importedLibrary != null) {
            specification.setImportedLibrary(importedLibrary);
            directive.setElement(importedLibrary);
          }
          SimpleIdentifier prefixNode = ((ImportDirective) directive).getPrefix();
          if (prefixNode != null) {
            String prefixName = prefixNode.getName();
            PrefixElementImpl prefix = nameToPrefixMap.get(prefixName);
            if (prefix == null) {
              prefix = new PrefixElementImpl(prefixNode);
              nameToPrefixMap.put(prefixName, prefix);
            } else {
              //
              // It is a compile-time error ... if any other import directive in the current library
              // includes a prefix clause of the form 'as p'.
              //
              // TODO(brianwilkerson) Report the error
              // errorListener.onError(new AnalysisError(source, prefixNode.getOffset(), prefixNode.getLength(), ResolverErrorCode.DUPLICATE_PREFIX, prefixName));
            }
            ArrayList<ImportElement> prefixedImports = prefixToImportMap.get(prefix);
            if (prefixedImports == null) {
              prefixedImports = new ArrayList<ImportElement>();
              prefixToImportMap.put(prefix, prefixedImports);
            }
            prefixedImports.add(specification);
            specification.setPrefix(prefix);
          }
          imports.add(specification);
        } else if (directive instanceof ExportDirective) {
          ExportElementImpl specification = new ExportElementImpl();
          specification.setCombinators(buildCombinators(namespaceDirective));
          LibraryElement exportedLibrary = getReferencedLibrary(
              librarySource,
              namespaceDirective.getLibraryUri());
          if (exportedLibrary != null) {
            specification.setExportedLibrary(exportedLibrary);
            directive.setElement(exportedLibrary);
          }
          exports.add(specification);
        } else {
          AnalysisEngine.getInstance().getLogger().logError(
              "Internal error: LibraryElementBuilder does not handle instances of "
                  + directive.getClass().getName());
        }
      } else if (directive instanceof PartDirective) {
        hasPartDirective = true;
        StringLiteral partUri = ((PartDirective) directive).getPartUri();
        Source partSource = getSource(librarySource, partUri);
        if (partSource != null) {
          CompilationUnitElementImpl part = builder.buildCompilationUnit(partSource);
          //
          // Validate that the part contains a part-of directive with the same name as the library.
          //
          String partLibraryName = getPartLibraryName(partSource, directivesToResolve);
          if (partLibraryName == null) {
            errorListener.onError(new AnalysisError(
                librarySource,
                partUri.getOffset(),
                partUri.getLength(),
                ResolverErrorCode.MISSING_PART_OF_DIRECTIVE));
          } else if (libraryNameNode == null) {
            // TODO(brianwilkerson) Collect the names declared by the part. If they are all the same
            // then we can use that name as the inferred name of the library and present it in a
            // quick-fix.
            // partLibraryNames.add(partLibraryName);
          } else if (!libraryNameNode.getName().equals(partLibraryName)) {
            errorListener.onError(new AnalysisError(
                librarySource,
                partUri.getOffset(),
                partUri.getLength(),
                ResolverErrorCode.PART_WITH_WRONG_LIBRARY_NAME,
                partLibraryName));
          }
          if (entryPoint == null) {
            entryPoint = findEntryPoint(part);
          }
          directive.setElement(part);
          sourcedCompilationUnits.add(part);
        }
      }
    }
    // TODO(brianwilkerson) Uncomment the code below when the necessary pieces are implemented.
//    if (!explicitlyImportsCore && !CORE_LIBRARY_NAME.equals(librarySource.getFullName())) {
//      ImportSpecificationImpl specification = new ImportSpecificationImpl();
//      LibraryElement importedLibrary = getReferencedLibrary(librarySource, CORE_LIBRARY_NAME);
//      specification.setImportedLibrary(importedLibrary);
//      specification.setSynthetic(true);
//      imports.add(specification);
//    }

    if (hasPartDirective && libraryNameNode == null) {
      errorListener.onError(new AnalysisError(
          librarySource,
          ResolverErrorCode.MISSING_LIBRARY_DIRECTIVE_WITH_PART));
    }
    LibraryElementImpl libraryElement = new LibraryElementImpl(analysisContext, libraryNameNode);
    libraryElement.setDefiningCompilationUnit(definingCompilationUnitElement);
    if (entryPoint != null) {
      libraryElement.setEntryPoint(entryPoint);
    }
    libraryElement.setImports(imports.toArray(new ImportElement[imports.size()]));
    libraryElement.setParts(sourcedCompilationUnits.toArray(new CompilationUnitElementImpl[sourcedCompilationUnits.size()]));
    for (Directive directive : directivesToResolve) {
      directive.setElement(libraryElement);
    }

    return libraryElement;
  }

  /**
   * Append the value of the given string literal to the given string builder.
   * 
   * @param builder the builder to which the string's value is to be appended
   * @param literal the string literal whose value is to be appended to the builder
   * @throws IllegalArgumentException if the string is not a constant string without any string
   *           interpolation
   */
  private void appendStringValue(StringBuilder builder, StringLiteral literal)
      throws IllegalArgumentException {
    if (literal instanceof SimpleStringLiteral) {
      builder.append(((SimpleStringLiteral) literal).getValue());
    } else if (literal instanceof AdjacentStrings) {
      for (StringLiteral stringLiteral : ((AdjacentStrings) literal).getStrings()) {
        appendStringValue(builder, stringLiteral);
      }
    } else {
      throw new IllegalArgumentException();
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
   * Search the top-level functions defined in the given compilation unit for the entry point.
   * 
   * @param element the compilation unit to be searched
   * @return the entry point that was found, or {@code null} if the compilation unit does not define
   *         an entry point
   */
  private FunctionElement findEntryPoint(CompilationUnitElementImpl element) {
    for (FunctionElement function : element.getFunctions()) {
      if (function.getName().equals(ENTRY_POINT_NAME)) {
        return function;
      }
    }
    return null;
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

  /**
   * Return the name of the library that the given part is declared to be a part of, or {@code null}
   * if the part does not contain a part-of directive.
   * 
   * @param partSource the source representing the part
   * @param directivesToResolve a list of directives that should be resolved to the library being
   *          built
   * @return the name of the library that the given part is declared to be a part of
   */
  private String getPartLibraryName(Source partSource, ArrayList<Directive> directivesToResolve) {
    try {
      CompilationUnit partUnit = analysisContext.parse(partSource, errorListener);
      for (Directive directive : partUnit.getDirectives()) {
        if (directive instanceof PartOfDirective) {
          directivesToResolve.add(directive);
          LibraryIdentifier libraryName = ((PartOfDirective) directive).getLibraryName();
          if (libraryName != null) {
            return libraryName.getName();
          }
        }
      }
    } catch (AnalysisException exception) {
      // Fall through to return null.
    }
    return null;
  }

  /**
   * Return the element model for a library that is being referenced by the library whose element
   * model is being built.
   * 
   * @param librarySource the source for the library being built
   * @param referencedSource the source for the library that was referenced
   * @return the element model for the referenced library
   */
  private LibraryElement getReferencedLibrary(Source librarySource, Source referencedSource) {
    if (referencedSource == null) {
      return null;
    }
    // TODO(brianwilkerson) This needs to go through the analysis context so that we can take
    // advantage of cached results. In addition, we need to have a way to build a library element
    // without building the whole element model for that library so that we can handle circular
    // dependencies.
    LibraryElementBuilder libraryBuilder = new LibraryElementBuilder(analysisContext, errorListener);
    // TODO(brianwilkerson) Check to see that the referenced library has a library directive and
    // report an error if it does not.
    try {
      // TODO(brianwilkerson) This needs to return a handle to the built library rather than a
      // direct reference.
      return libraryBuilder.buildLibrary(referencedSource);
    } catch (AnalysisException exception) {
      // Even if we are unable to build the referenced library we still want to try to continue to
      // build the referencing library.
      return null;
    }
  }

  /**
   * Return the element model for a library that is being referenced by the library whose element
   * model is being built.
   * 
   * @param librarySource the source for the library being built
   * @param libraryUri the URI of the referenced library
   * @return the element model for the referenced library
   */
  private LibraryElement getReferencedLibrary(Source librarySource, String libraryUri) {
    return getReferencedLibrary(librarySource, getSource(librarySource, libraryUri, 0, 0));
  }

  /**
   * Return the element model for a library that is being referenced by the library whose element
   * model is being built.
   * 
   * @param librarySource the source for the library being built
   * @param libraryUri the URI of the referenced library
   * @return the element model for the referenced library
   */
  private LibraryElement getReferencedLibrary(Source librarySource, StringLiteral libraryUri) {
    return getReferencedLibrary(librarySource, getSource(librarySource, libraryUri));
  }

  /**
   * Return the result of resolving the given URI against the URI of the library, or {@code null} if
   * the URI is not valid. If the URI is not valid, report the error.
   * 
   * @param librarySource the source defining the URI against which the given URI will be resolved
   * @param partUri the URI to be resolved
   * @return the result of resolving the given URI against the URI of the library
   */
  private Source getSource(Source librarySource, String uri, int uriOffset, int uriLength) {
    if (uri == null) {
      errorListener.onError(new AnalysisError(
          librarySource,
          uriOffset,
          uriLength,
          ResolverErrorCode.INVALID_URI));
      return null;
    }
    return librarySource.resolve(uri);
  }

  /**
   * Return the result of resolving the given URI against the URI of the library, or {@code null} if
   * the URI is not valid. If the URI is not valid, report the error.
   * 
   * @param librarySource the source defining the URI against which the given URI will be resolved
   * @param partUri the URI to be resolved
   * @return the result of resolving the given URI against the URI of the library
   */
  private Source getSource(Source librarySource, StringLiteral partUri) {
    return getSource(
        librarySource,
        getStringValue(partUri),
        partUri.getOffset(),
        partUri.getLength());
  }

  /**
   * Return the value of the given string literal, or {@code null} if the string is not a constant
   * string without any string interpolation.
   * 
   * @param literal the string literal whose value is to be returned
   * @return the value of the given string literal
   */
  private String getStringValue(StringLiteral literal) {
    StringBuilder builder = new StringBuilder();
    try {
      appendStringValue(builder, literal);
    } catch (IllegalArgumentException exception) {
      return null;
    }
    return builder.toString();
  }
}
