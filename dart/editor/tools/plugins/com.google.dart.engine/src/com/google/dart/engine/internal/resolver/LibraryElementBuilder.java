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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.LibraryIdentifier;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.builder.CompilationUnitBuilder;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.resolver.ResolverErrorCode;
import com.google.dart.engine.source.Source;

import java.util.ArrayList;

/**
 * Instances of the class {@code LibraryElementBuilder} build an element model for a single library.
 * 
 * @coverage dart.engine.resolver
 */
public class LibraryElementBuilder {
  /**
   * The analysis context in which the element model will be built.
   */
  private InternalAnalysisContext analysisContext;

  /**
   * The listener to which errors will be reported.
   */
  private AnalysisErrorListener errorListener;

  /**
   * The name of the function used as an entry point.
   */
  private static final String ENTRY_POINT_NAME = "main";

  /**
   * Initialize a newly created library element builder.
   * 
   * @param resolver the resolver for which the element model is being built
   */
  public LibraryElementBuilder(LibraryResolver resolver) {
    this.analysisContext = resolver.getAnalysisContext();
    this.errorListener = resolver.getErrorListener();
  }

  /**
   * Build the library element for the given library.
   * 
   * @param library the library for which an element model is to be built
   * @return the library element that was built
   * @throws AnalysisException if the analysis could not be performed
   */
  public LibraryElementImpl buildLibrary(Library library) throws AnalysisException {
    CompilationUnitBuilder builder = new CompilationUnitBuilder();
    Source librarySource = library.getLibrarySource();
    CompilationUnit definingCompilationUnit = library.getDefiningCompilationUnit();
    CompilationUnitElementImpl definingCompilationUnitElement = builder.buildCompilationUnit(
        librarySource,
        definingCompilationUnit);
    NodeList<Directive> directives = definingCompilationUnit.getDirectives();
    LibraryIdentifier libraryNameNode = null;
    boolean hasPartDirective = false;
    FunctionElement entryPoint = findEntryPoint(definingCompilationUnitElement);
    ArrayList<Directive> directivesToResolve = new ArrayList<Directive>();
    ArrayList<CompilationUnitElementImpl> sourcedCompilationUnits = new ArrayList<CompilationUnitElementImpl>();
    for (Directive directive : directives) {
      //
      // We do not build the elements representing the import and export directives at this point.
      // That is not done until we get to LibraryResolver.buildDirectiveModels() because we need the
      // LibraryElements for the referenced libraries, which might not exist at this point (due to
      // the possibility of circular references).
      //
      if (directive instanceof LibraryDirective) {
        if (libraryNameNode == null) {
          libraryNameNode = ((LibraryDirective) directive).getName();
          directivesToResolve.add(directive);
        }
      } else if (directive instanceof PartDirective) {
        hasPartDirective = true;
        StringLiteral partUri = ((PartDirective) directive).getUri();
        Source partSource = library.getSource(partUri);
        if (partSource != null && partSource.exists()) {
          CompilationUnitElementImpl part = builder.buildCompilationUnit(
              partSource,
              library.getAST(partSource));
          //
          // Validate that the part contains a part-of directive with the same name as the library.
          //
          String partLibraryName = getPartLibraryName(library, partSource, directivesToResolve);
          if (partLibraryName == null) {
            errorListener.onError(new AnalysisError(
                librarySource,
                partUri.getOffset(),
                partUri.getLength(),
                CompileTimeErrorCode.PART_OF_NON_PART,
                partUri.toSource()));
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
                StaticWarningCode.PART_OF_DIFFERENT_LIBRARY,
                libraryNameNode.getName(),
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

    if (hasPartDirective && libraryNameNode == null) {
      errorListener.onError(new AnalysisError(
          librarySource,
          ResolverErrorCode.MISSING_LIBRARY_DIRECTIVE_WITH_PART));
    }
    //
    // Create and populate the library element.
    //
    LibraryElementImpl libraryElement = new LibraryElementImpl(analysisContext, libraryNameNode);
    libraryElement.setDefiningCompilationUnit(definingCompilationUnitElement);
    if (entryPoint != null) {
      libraryElement.setEntryPoint(entryPoint);
    }
    libraryElement.setParts(sourcedCompilationUnits.toArray(new CompilationUnitElementImpl[sourcedCompilationUnits.size()]));
    for (Directive directive : directivesToResolve) {
      directive.setElement(libraryElement);
    }
    library.setLibraryElement(libraryElement);
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
   * Return the name of the library that the given part is declared to be a part of, or {@code null}
   * if the part does not contain a part-of directive.
   * 
   * @param library the library containing the part
   * @param partSource the source representing the part
   * @param directivesToResolve a list of directives that should be resolved to the library being
   *          built
   * @return the name of the library that the given part is declared to be a part of
   */
  private String getPartLibraryName(Library library, Source partSource,
      ArrayList<Directive> directivesToResolve) {
    try {
      CompilationUnit partUnit = library.getAST(partSource);
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
}
