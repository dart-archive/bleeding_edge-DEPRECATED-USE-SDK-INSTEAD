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
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.AdjacentStrings;
import com.google.dart.engine.ast.Combinator;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.HideCombinator;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.NamespaceDirective;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.ShowCombinator;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.ImportCombinator;
import com.google.dart.engine.element.ImportSpecification;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.HideCombinatorImpl;
import com.google.dart.engine.internal.element.ImportSpecificationImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.PrefixElementImpl;
import com.google.dart.engine.internal.element.ShowCombinatorImpl;
import com.google.dart.engine.provider.CompilationUnitProvider;
import com.google.dart.engine.resolver.ResolverErrorCode;
import com.google.dart.engine.source.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Instances of the class {@code LibraryElementBuilder} build an element model for a single library.
 */
public class LibraryElementBuilder {
  /**
   * The provider used to access the compilation unit associated with a given source.
   */
  private CompilationUnitProvider provider;

  /**
   * The listener to which errors will be reported.
   */
  private AnalysisErrorListener errorListener;

  /**
   * A table mapping the identifiers of declared elements to the element that was declared.
   */
  private HashMap<ASTNode, Element> declaredElementMap = new HashMap<ASTNode, Element>();

  /**
   * The name of the function used as an entry point.
   */
  private static final String ENTRY_POINT_NAME = "main";

  /**
   * Initialize a newly created library element builder.
   * 
   * @param provider the provider used to access the compilation unit associated with a given source
   * @param errorListener the listener to which errors will be reported
   */
  public LibraryElementBuilder(CompilationUnitProvider provider, AnalysisErrorListener errorListener) {
    this.provider = provider;
    this.errorListener = errorListener;
  }

  /**
   * Build the library element for the given source.
   * 
   * @param librarySource the source describing the defining compilation unit for the library
   * @return the library element that was built
   */
  public LibraryElement buildLibrary(Source librarySource) {
    CompilationUnitBuilder builder = new CompilationUnitBuilder(provider, declaredElementMap);
    CompilationUnit definingCompilationUnit = provider.getCompilationUnit(librarySource);
    CompilationUnitElementImpl definingCompilationUnitElement = builder.buildCompilationUnit(librarySource);
    NodeList<Directive> directives = definingCompilationUnit.getDirectives();
    SimpleIdentifier libraryNameNode = null;
    boolean hasPartDirective = false;
    FunctionElement entryPoint = findEntryPoint(definingCompilationUnitElement);
    ArrayList<ImportSpecification> imports = new ArrayList<ImportSpecification>();
    HashMap<String, PrefixElementImpl> nameToPrefixMap = new HashMap<String, PrefixElementImpl>();
    HashMap<PrefixElementImpl, ArrayList<ImportSpecification>> prefixToImportMap = new HashMap<PrefixElementImpl, ArrayList<ImportSpecification>>();
    ArrayList<CompilationUnitElementImpl> sourcedCompilationUnits = new ArrayList<CompilationUnitElementImpl>();
    for (Directive directive : directives) {
      if (directive instanceof LibraryDirective) {
        if (libraryNameNode == null) {
          libraryNameNode = ((LibraryDirective) directive).getName();
        }
      } else if (directive instanceof NamespaceDirective) {
        NamespaceDirective namespaceDirective = (NamespaceDirective) directive;
        ImportSpecificationImpl specification = new ImportSpecificationImpl();
        specification.setCombinators(buildCombinators(namespaceDirective));
        Source source = getSource(librarySource, namespaceDirective.getLibraryUri());
        if (source != null) {
          // TODO(brianwilkerson) This needs to go through the analysis context so that we can take
          // advantage of cached results.
          LibraryElementBuilder libraryBuilder = new LibraryElementBuilder(provider, errorListener);
          // TODO(brianwilkerson) Check to see that the imported library has a library directive and
          // report an error if it does not.
          // TODO(brianwilkerson) This needs to be a handle to the built library rather than a
          // direct reference.
          specification.setImportedLibrary(libraryBuilder.buildLibrary(source));
        }
        if (directive instanceof ImportDirective) {
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
            ArrayList<ImportSpecification> prefixedImports = prefixToImportMap.get(prefix);
            if (prefixedImports == null) {
              prefixedImports = new ArrayList<ImportSpecification>();
              prefixToImportMap.put(prefix, prefixedImports);
            }
            prefixedImports.add(specification);
            specification.setPrefix(prefix);
          }
        } else if (directive instanceof ExportDirective) {
          specification.setExported(true);
        } else {
          AnalysisEngine.getInstance().getLogger().logError(
              "Internal error: LibraryElementBuilder does not handle instances of "
                  + directive.getClass().getName());
        }
        imports.add(specification);
      } else if (directive instanceof PartDirective) {
        hasPartDirective = true;
        StringLiteral partUri = ((PartDirective) directive).getPartUri();
        Source source = getSource(librarySource, partUri);
        if (source != null) {
          CompilationUnitElementImpl part = builder.buildCompilationUnit(source);
          // TODO(brianwilkerson) Validate that the part contains a part-of directive with the same
          // name as the library. If there is no libraryNameNode, look to see whether all of the
          // parts use the same name so that we can use the intended name in a quick-fix.
          //
          // String partLibraryName = ...;
          // if (partLibraryName == null) {
          //   errorListener.onError(new AnalysisError(librarySource, partUri.getOffset, partUri.getLength(), ResolverErrorCode.MISSING_PART_OF_DIRECTIVE));
          // } else if (libraryNameNode == null) {
          //   partLibraryNames.add(partLibraryName);
          // } else if (!libraryNameNode.getName().equals(partLibraryName)) {
          //   errorListener.onError(new AnalysisError(librarySource, partUri.getOffset, partUri.getLength(), ResolverErrorCode.PART_WITH_WRONG_LIBRARY_NAME, partLibraryName));
          // }
          if (entryPoint == null) {
            entryPoint = findEntryPoint(part);
          }
          declaredElementMap.put(partUri, part);
          sourcedCompilationUnits.add(part);
        }
      }
    }

    if (hasPartDirective && libraryNameNode == null) {
      errorListener.onError(new AnalysisError(
          librarySource,
          ResolverErrorCode.MISSING_LIBRARY_DIRECTIVE_WITH_PART));
    }
    LibraryElementImpl libraryElement = new LibraryElementImpl(libraryNameNode);
    libraryElement.setDefiningCompilationUnit(definingCompilationUnitElement);
    if (entryPoint != null) {
      libraryElement.setEntryPoint(entryPoint);
    }
    libraryElement.setImports(imports.toArray(new ImportSpecification[imports.size()]));
    libraryElement.setParts(sourcedCompilationUnits.toArray(new CompilationUnitElementImpl[sourcedCompilationUnits.size()]));

    return libraryElement;
  }

  /**
   * Return a table mapping the identifiers of declared elements to the element that was declared.
   * 
   * @return a table mapping the identifiers of declared elements to the element that was declared
   */
  public Map<ASTNode, Element> getDeclaredElementMap() {
    return declaredElementMap;
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
  private ImportCombinator[] buildCombinators(NamespaceDirective directive) {
    ArrayList<ImportCombinator> combinators = new ArrayList<ImportCombinator>();
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
    return combinators.toArray(new ImportCombinator[combinators.size()]);
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
   * Return the result of resolving the given URI against the URI of the library, or {@code null} if
   * the URI is not valid. If the URI is not valid, report the error.
   * 
   * @param librarySource the source defining the URI against which the given URI will be resolved
   * @param partUri the URI to be resolved
   * @return the result of resolving the given URI against the URI of the library
   */
  private Source getSource(Source librarySource, StringLiteral partUri) {
    String uri = getStringValue(partUri);
    if (uri == null) {
      errorListener.onError(new AnalysisError(
          librarySource,
          partUri.getOffset(),
          partUri.getLength(),
          ResolverErrorCode.INVALID_URI));
      return null;
    }
    return librarySource.resolve(uri);
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
