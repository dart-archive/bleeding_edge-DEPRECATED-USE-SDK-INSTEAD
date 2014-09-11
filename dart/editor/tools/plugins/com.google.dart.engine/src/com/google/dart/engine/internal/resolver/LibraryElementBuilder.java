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
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.builder.CompilationUnitBuilder;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.PropertyAccessorElementImpl;
import com.google.dart.engine.internal.element.PropertyInducingElementImpl;
import com.google.dart.engine.resolver.ResolverErrorCode;
import com.google.dart.engine.source.Source;

import java.util.ArrayList;
import java.util.HashMap;

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
  public static final String ENTRY_POINT_NAME = "main";

  /**
   * Initialize a newly created library element builder.
   * 
   * @param analysisContext the analysis context in which the element model will be built
   * @param errorListener the listener to which errors will be reported
   */
  public LibraryElementBuilder(InternalAnalysisContext analysisContext,
      AnalysisErrorListener errorListener) {
    this.analysisContext = analysisContext;
    this.errorListener = errorListener;
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
        PartDirective partDirective = (PartDirective) directive;
        StringLiteral partUri = partDirective.getUri();
        Source partSource = partDirective.getSource();
        if (analysisContext.exists(partSource)) {
          hasPartDirective = true;
          CompilationUnit partUnit = library.getAST(partSource);
          CompilationUnitElementImpl part = builder.buildCompilationUnit(partSource, partUnit);
          part.setUriOffset(partUri.getOffset());
          part.setUriEnd(partUri.getEnd());
          part.setUri(partDirective.getUriContent());
          //
          // Validate that the part contains a part-of directive with the same name as the library.
          //
          String partLibraryName = getPartLibraryName(partSource, partUnit, directivesToResolve);
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
    LibraryElementImpl libraryElement = new LibraryElementImpl(
        analysisContext.getContextFor(librarySource),
        libraryNameNode);
    libraryElement.setDefiningCompilationUnit(definingCompilationUnitElement);
    if (entryPoint != null) {
      libraryElement.setEntryPoint(entryPoint);
    }
    int sourcedUnitCount = sourcedCompilationUnits.size();
    libraryElement.setParts(sourcedCompilationUnits.toArray(new CompilationUnitElementImpl[sourcedUnitCount]));
    for (Directive directive : directivesToResolve) {
      directive.setElement(libraryElement);
    }
    library.setLibraryElement(libraryElement);
    if (sourcedUnitCount > 0) {
      patchTopLevelAccessors(libraryElement);
    }
    return libraryElement;
  }

  /**
   * Build the library element for the given library.
   * 
   * @param library the library for which an element model is to be built
   * @return the library element that was built
   * @throws AnalysisException if the analysis could not be performed
   */
  public LibraryElementImpl buildLibrary(ResolvableLibrary library) throws AnalysisException {
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
        PartDirective partDirective = (PartDirective) directive;
        StringLiteral partUri = partDirective.getUri();
        Source partSource = partDirective.getSource();
        if (analysisContext.exists(partSource)) {
          hasPartDirective = true;
          CompilationUnit partUnit = library.getAST(partSource);
          if (partUnit != null) {
            CompilationUnitElementImpl part = builder.buildCompilationUnit(partSource, partUnit);
            part.setUriOffset(partUri.getOffset());
            part.setUriEnd(partUri.getEnd());
            part.setUri(partDirective.getUriContent());
            //
            // Validate that the part contains a part-of directive with the same name as the library.
            //
            String partLibraryName = getPartLibraryName(partSource, partUnit, directivesToResolve);
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
    }

    if (hasPartDirective && libraryNameNode == null) {
      errorListener.onError(new AnalysisError(
          librarySource,
          ResolverErrorCode.MISSING_LIBRARY_DIRECTIVE_WITH_PART));
    }
    //
    // Create and populate the library element.
    //
    LibraryElementImpl libraryElement = new LibraryElementImpl(
        analysisContext.getContextFor(librarySource),
        libraryNameNode);
    libraryElement.setDefiningCompilationUnit(definingCompilationUnitElement);
    if (entryPoint != null) {
      libraryElement.setEntryPoint(entryPoint);
    }
    int sourcedUnitCount = sourcedCompilationUnits.size();
    libraryElement.setParts(sourcedCompilationUnits.toArray(new CompilationUnitElementImpl[sourcedUnitCount]));
    for (Directive directive : directivesToResolve) {
      directive.setElement(libraryElement);
    }
    library.setLibraryElement(libraryElement);
    if (sourcedUnitCount > 0) {
      patchTopLevelAccessors(libraryElement);
    }
    return libraryElement;
  }

  /**
   * Add all of the non-synthetic getters and setters defined in the given compilation unit that
   * have no corresponding accessor to one of the given collections.
   * 
   * @param getters the map to which getters are to be added
   * @param setters the list to which setters are to be added
   * @param unit the compilation unit defining the accessors that are potentially being added
   */
  private void collectAccessors(HashMap<String, PropertyAccessorElement> getters,
      ArrayList<PropertyAccessorElement> setters, CompilationUnitElement unit) {
    for (PropertyAccessorElement accessor : unit.getAccessors()) {
      if (accessor.isGetter()) {
        if (!accessor.isSynthetic() && accessor.getCorrespondingSetter() == null) {
          getters.put(accessor.getDisplayName(), accessor);
        }
      } else {
        if (!accessor.isSynthetic() && accessor.getCorrespondingGetter() == null) {
          setters.add(accessor);
        }
      }
    }
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
   * @param partSource the source representing the part
   * @param partUnit the AST structure of the part
   * @param directivesToResolve a list of directives that should be resolved to the library being
   *          built
   * @return the name of the library that the given part is declared to be a part of
   */
  private String getPartLibraryName(Source partSource, CompilationUnit partUnit,
      ArrayList<Directive> directivesToResolve) {
    for (Directive directive : partUnit.getDirectives()) {
      if (directive instanceof PartOfDirective) {
        directivesToResolve.add(directive);
        LibraryIdentifier libraryName = ((PartOfDirective) directive).getLibraryName();
        if (libraryName != null) {
          return libraryName.getName();
        }
      }
    }
    return null;
  }

  /**
   * Look through all of the compilation units defined for the given library, looking for getters
   * and setters that are defined in different compilation units but that have the same names. If
   * any are found, make sure that they have the same variable element.
   * 
   * @param libraryElement the library defining the compilation units to be processed
   */
  private void patchTopLevelAccessors(LibraryElementImpl libraryElement) {
    HashMap<String, PropertyAccessorElement> getters = new HashMap<String, PropertyAccessorElement>();
    ArrayList<PropertyAccessorElement> setters = new ArrayList<PropertyAccessorElement>();
    collectAccessors(getters, setters, libraryElement.getDefiningCompilationUnit());
    for (CompilationUnitElement unit : libraryElement.getParts()) {
      collectAccessors(getters, setters, unit);
    }
    for (PropertyAccessorElement setter : setters) {
      PropertyAccessorElement getter = getters.get(setter.getDisplayName());
      if (getter != null) {
        PropertyInducingElementImpl variable = (PropertyInducingElementImpl) getter.getVariable();
        variable.setSetter(setter);
        ((PropertyAccessorElementImpl) setter).setVariable(variable);
      }
    }
  }
}
