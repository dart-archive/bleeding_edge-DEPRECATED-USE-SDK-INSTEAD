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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.ImportHideCombinator;
import com.google.dart.engine.ast.ImportShowCombinator;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.ImportCombinator;
import com.google.dart.engine.element.ImportSpecification;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.HideCombinatorImpl;
import com.google.dart.engine.internal.element.ImportSpecificationImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.PrefixElementImpl;
import com.google.dart.engine.internal.element.ShowCombinatorImpl;
import com.google.dart.engine.source.Source;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Instances of the class {@code LibraryElementBuilder} build an element model for a single library.
 */
public class LibraryElementBuilder {
  /**
   * The name of the function used as an entry point.
   */
  private static final String ENTRY_POINT_NAME = "main";

  /**
   * Initialize a newly created library element builder.
   */
  public LibraryElementBuilder() {
    super();
  }

  /**
   * Build the library element for the given source.
   * 
   * @param librarySource the source describing the defining compilation unit for the library
   * @return the library element that was built
   */
  public LibraryElement buildLibrary(Source librarySource) {
    CompilationUnitBuilder builder = new CompilationUnitBuilder();
    CompilationUnit definingCompilationUnit = getCompilationUnit(librarySource);
    CompilationUnitElementImpl definingCompilationUnitElement = builder.buildCompilationUnit(librarySource);
    NodeList<Directive> directives = definingCompilationUnit.getDirectives();
    String libraryName = null;
    FunctionElement entryPoint = findEntryPoint(definingCompilationUnitElement);
    ArrayList<ImportSpecification> imports = new ArrayList<ImportSpecification>();
    HashMap<String, PrefixElementImpl> nameToPrefixMap = new HashMap<String, PrefixElementImpl>();
    HashMap<PrefixElementImpl, ArrayList<ImportSpecification>> prefixToImportMap = new HashMap<PrefixElementImpl, ArrayList<ImportSpecification>>();
    ArrayList<CompilationUnitElementImpl> sourcedCompilationUnits = new ArrayList<CompilationUnitElementImpl>();
    for (Directive directive : directives) {
      if (directive instanceof LibraryDirective) {
        if (libraryName == null) {
          libraryName = ((LibraryDirective) directive).getName().getName();
        }
      } else if (directive instanceof ImportDirective) {
        ImportDirective importDirective = (ImportDirective) directive;
        Source source = getSource(librarySource, importDirective.getLibraryUri());
        ImportSpecificationImpl specification = new ImportSpecificationImpl();
        ArrayList<ImportCombinator> combinators = new ArrayList<ImportCombinator>();
        for (com.google.dart.engine.ast.ImportCombinator combinator : importDirective.getCombinators()) {
          if (combinator instanceof ImportHideCombinator) {
            HideCombinatorImpl hide = new HideCombinatorImpl();
            hide.setHiddenNames(getIdentifiers(((ImportHideCombinator) combinator).getHiddenNames()));
            combinators.add(hide);
          } else {
            ShowCombinatorImpl show = new ShowCombinatorImpl();
            show.setShownNames(getIdentifiers(((ImportShowCombinator) combinator).getShownNames()));
            combinators.add(show);
          }
        }
        specification.setCombinators(combinators.toArray(new ImportCombinator[combinators.size()]));
        specification.setExported(importDirective.getExportToken() != null);
        specification.setImportedLibrary(buildLibrary(source));
        if (importDirective.getPrefix() != null) {
          String prefixName = importDirective.getPrefix().getName();
          PrefixElementImpl prefix = nameToPrefixMap.get(prefixName);
          if (prefix == null) {
            prefix = new PrefixElementImpl(prefixName);
            nameToPrefixMap.put(prefixName, prefix);
          }
          ArrayList<ImportSpecification> prefixedImports = prefixToImportMap.get(prefix);
          if (prefixedImports == null) {
            prefixedImports = new ArrayList<ImportSpecification>();
            prefixToImportMap.put(prefix, prefixedImports);
          }
          prefixedImports.add(specification);
          specification.setPrefix(prefix);
        }
        imports.add(specification);
      } else if (directive instanceof PartDirective) {
        Source source = getSource(librarySource, ((PartDirective) directive).getPartUri());
        CompilationUnitElementImpl part = builder.buildCompilationUnit(source);
        if (entryPoint == null) {
          entryPoint = findEntryPoint(part);
        }
        sourcedCompilationUnits.add(part);
      }
    }

    LibraryElementImpl libraryElement = new LibraryElementImpl(libraryName);
    libraryElement.setDefiningCompilationUnit(definingCompilationUnitElement);
    libraryElement.setEntryPoint(entryPoint);
    libraryElement.setImports(imports.toArray(new ImportSpecification[imports.size()]));
    libraryElement.setSourcedCompilationUnits(sourcedCompilationUnits.toArray(new CompilationUnitElementImpl[sourcedCompilationUnits.size()]));
    return libraryElement;
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
   * Return the AST structure for the compilation unit with the given source.
   * 
   * @param source the source describing the compilation unit
   * @return the AST structure for the compilation unit with the given source
   */
  private CompilationUnit getCompilationUnit(Source source) {
    // TODO(brianwilkerson) Implement this.
    return null;
  }

  /**
   * Return an array containing the lexical identifiers associated with the nodes in the given list.
   * 
   * @param names the AST nodes representing the identifiers
   * @return the lexical identifiers associated with the nodes in the list
   */
  private String[] getIdentifiers(NodeList<Identifier> names) {
    int count = names.size();
    String[] identifiers = new String[count];
    for (int i = 0; i < count; i++) {
      identifiers[i] = names.get(i).getName();
    }
    return identifiers;
  }

  /**
   * Return the result of resolving the given URI against the URI of the library.
   * 
   * @param librarySource the source defining the URI against which the given URI will be resolved
   * @param partUri the URI to be resolved
   * @return the result of resolving the given URI against the URI of the library
   */
  private Source getSource(Source librarySource, StringLiteral partUri) {
    // TODO(brianwilkerson) Implement this. We need a way to get the string from the string literal.
    // return librarySource.resolve(partUri.getStringValue());
    return null;
  }
}
