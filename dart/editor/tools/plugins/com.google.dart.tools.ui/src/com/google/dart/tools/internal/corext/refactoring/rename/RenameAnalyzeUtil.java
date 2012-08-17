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
package com.google.dart.tools.internal.corext.refactoring.rename;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.common.SourceInfo;
import com.google.dart.compiler.resolver.TypeVariableElement;
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.tools.core.dom.NodeFinder;
import com.google.dart.tools.core.internal.model.SourceRangeImpl;
import com.google.dart.tools.core.internal.util.SourceRangeUtils;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.CompilationUnitElement;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartFunctionTypeAlias;
import com.google.dart.tools.core.model.DartImport;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartTypeParameter;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.core.search.SearchEngine;
import com.google.dart.tools.core.search.SearchEngineFactory;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.internal.corext.refactoring.Checks;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.base.DartStatusContext;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableObjectEx;
import com.google.dart.tools.ui.internal.viewsupport.BasicElementLabels;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;

import java.util.List;
import java.util.Set;

/**
 * Utilities used in various {@link RenameProcessor} implementations.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class RenameAnalyzeUtil {

  /**
   * If {@link DartElement} becomes private, then marks its usage places outside of declaring
   * {@link DartLibrary} as errors.
   */
  public static RefactoringStatus checkBecomePrivate(String oldName, String newName,
      DartElement member, List<SearchMatch> references) throws CoreException {
    RefactoringStatus result = new RefactoringStatus();
    if (!StringUtils.startsWith(oldName, "_") && StringUtils.startsWith(newName, "_")) {
      DartLibrary declarationLibrary = member.getAncestor(DartLibrary.class);
      for (SearchMatch reference : references) {
        DartLibrary referenceLibrary = reference.getElement().getAncestor(DartLibrary.class);
        if (!Objects.equal(referenceLibrary, declarationLibrary)) {
          IPath referenceLibraryPath = referenceLibrary.getDefiningCompilationUnit().getResource().getFullPath();
          String message = Messages.format(
              RefactoringCoreMessages.RenameProcessor_willBecomePrivate,
              new Object[] {
                  RenameAnalyzeUtil.getElementTypeName(member),
                  BasicElementLabels.getPathLabel(referenceLibraryPath, false),});
          result.addError(message, DartStatusContext.create(reference));
        }
      }
    }
    return result;
  }

  /**
   * Check that given {@link DartElement} is defined in workspace.
   * 
   * @param element the {@link DartElement} to check, not <code>null</code>.
   * @return the {@link RefactoringStatus} with {@link RefactoringStatus#OK} or
   *         {@link RefactoringStatus#ERROR}.
   */
  public static RefactoringStatus checkLocalElement(DartElement element) throws CoreException {
    if (!Checks.isLocal(element)) {
      String libraryName = element.getAncestor(DartLibrary.class).getElementName();
      IPath elementPath = element.getPath();
      String message = Messages.format(
          RefactoringCoreMessages.RenameProcessor_notLocal,
          new Object[] {
              RenameAnalyzeUtil.getElementTypeName(element), libraryName,
              BasicElementLabels.getPathLabel(elementPath, true)});
      return RefactoringStatus.createErrorStatus(message);
    }
    return new RefactoringStatus();
  }

  /**
   * Checks that all references have expected source, or returns fatal {@link RefactoringStatus}.
   */
  public static RefactoringStatus checkReferencesSource(List<SearchMatch> references,
      String expectedSource) throws DartModelException {
    expectedSource = expectedSource == null ? "" : expectedSource;
    for (SearchMatch reference : references) {
      DartElement element = reference.getElement();
      if (element != null) {
        CompilationUnit unit = element.getAncestor(CompilationUnit.class);
        if (unit != null) {
          SourceRange range = reference.getSourceRange();
          String referenceSource = StringUtils.substring(
              unit.getSource(),
              range.getOffset(),
              SourceRangeUtils.getEnd(range));
          if (!referenceSource.equals(expectedSource)) {
            return RefactoringStatus.createFatalErrorStatus(
                Messages.format(
                    RefactoringCoreMessages.RenameProcessor_invalidReference,
                    new Object[] {expectedSource, referenceSource}),
                DartStatusContext.create(reference));
          }
        }
      }
    }
    return new RefactoringStatus();
  }

  /**
   * Converts error message by replacing "renamed" with "created".
   */
  public static String convertRenameMessageToCreateMessage(String msg) {
    return StringUtils.replace(
        msg,
        RefactoringCoreMessages.RenameProcessor_wordRenamed,
        RefactoringCoreMessages.RenameProcessor_wordCreated);
  }

  /**
   * @return the localized name of the {@link DartElement}.
   */
  public static String getElementTypeName(DartElement element) {
    int elementType = element.getElementType();
    return getElementTypeName(elementType);
  }

  /**
   * @return the localized name of the {@link DartElement}.
   */
  public static String getElementTypeName(int elementType) {
    return Messages.format(RefactoringCoreMessages.RenameProcessor_elementTypeName, elementType);
  }

  /**
   * @return the names of all top-level elements exported from the given {@link DartLibrary}.
   */
  public static Set<String> getExportedTopLevelNames(DartLibrary library) throws CoreException {
    Set<String> names = Sets.newHashSet();
    for (CompilationUnit unit : library.getCompilationUnits()) {
      for (DartElement element : unit.getChildren()) {
        if (element instanceof CompilationUnitElement) {
          String name = element.getElementName();
          if (isPublicName(name)) {
            names.add(name);
          }
        }
      }
    }
    return names;
  }

  /**
   * @return the {@link FunctionLocalElement}s defined in the given {@link DartFunction}.
   */
  public static List<FunctionLocalElement> getFunctionLocalElements(DartFunction function)
      throws DartModelException {
    List<FunctionLocalElement> elements = Lists.newArrayList();
    for (DartElement child : function.getChildren()) {
      // local variable
      if (child instanceof DartVariableDeclaration) {
        DartVariableDeclaration localVariable = (DartVariableDeclaration) child;
        elements.add(new FunctionLocalElement(localVariable));
      }
      // local function
      if (child instanceof DartFunction) {
        DartFunction localFunction = (DartFunction) child;
        if (localFunction.isLocal()) {
          elements.add(new FunctionLocalElement(localFunction));
        }
      }
    }
    return elements;
  }

  /**
   * @return the names of all top-level elements imported by the given {@link DartImport}.
   */
  public static Set<String> getImportedTopLevelNames(DartImport imprt) throws CoreException {
    // TODO(scheglov) in soon we will need to support "show" and "hide" combinators
    DartLibrary library = imprt.getLibrary();
    return getExportedTopLevelNames(library);
  }

  /**
   * @return the references to the given {@link DartElement}, may be empty {@link List}, but not
   *         <code>null</code>.
   */
  public static List<SearchMatch> getReferences(final DartElement element, final IProgressMonitor pm)
      throws CoreException {
    List<SearchMatch> fieldReferences = ExecutionUtils.runObjectCore(new RunnableObjectEx<List<SearchMatch>>() {
      @Override
      public List<SearchMatch> runObject() throws Exception {
        SearchEngine searchEngine = SearchEngineFactory.createSearchEngine();
        if (element instanceof DartImport) {
          return searchEngine.searchReferences((DartImport) element, null, null, pm);
        }
        if (element instanceof Type) {
          return searchEngine.searchReferences((Type) element, null, null, pm);
        }
        if (element instanceof Field) {
          return searchEngine.searchReferences((Field) element, null, null, pm);
        }
        if (element instanceof Method) {
          return searchEngine.searchReferences((Method) element, null, null, pm);
        }
        if (element instanceof DartVariableDeclaration) {
          return searchEngine.searchReferences((DartVariableDeclaration) element, null, null, pm);
        }
        if (element instanceof DartFunction) {
          return searchEngine.searchReferences((DartFunction) element, null, null, pm);
        }
        if (element instanceof DartFunctionTypeAlias) {
          return searchEngine.searchReferences((DartFunctionTypeAlias) element, null, null, pm);
        }
        return Lists.newArrayList();
      }
    });
    return fieldReferences;
  }

  /**
   * @return references to the given {@link DartTypeParameter}, excluding declaration.
   */
  public static List<SourceRange> getReferences(DartTypeParameter parameter)
      throws DartModelException {
    final List<SourceRange> references = Lists.newArrayList();
    CompilationUnit unit = parameter.getCompilationUnit();
    DartUnit unitNode = DartCompilerUtilities.resolveUnit(unit);
    // prepare Node
    final SourceRange sourceRange = parameter.getNameRange();
    DartNode parameterNode = NodeFinder.perform(unitNode, sourceRange);
    if (parameterNode != null) {
      // prepare Element
      if (parameterNode.getElement() instanceof TypeVariableElement) {
        final TypeVariableElement parameterElement = (TypeVariableElement) parameterNode.getElement();
        // find references
        unitNode.accept(new ASTVisitor<Void>() {
          @Override
          public Void visitIdentifier(DartIdentifier node) {
            if (node.getElement() == parameterElement
                && node.getSourceInfo().getOffset() != sourceRange.getOffset()) {
              SourceInfo sourceInfo = node.getSourceInfo();
              int offset = sourceInfo.getOffset();
              int length = sourceInfo.getLength();
              references.add(new SourceRangeImpl(offset, length));
            }
            return null;
          }
        });
      }
    }
    // done, may be empty
    return references;
  }

  /**
   * @return all direct and indirect subtypes of the given {@link Type}.
   */
  public static List<Type> getSubTypes(final Type type) throws CoreException {
    List<Type> subTypes = Lists.newArrayList();
    // find direct references
    List<SearchMatch> matches = ExecutionUtils.runObjectCore(new RunnableObjectEx<List<SearchMatch>>() {
      @Override
      public List<SearchMatch> runObject() throws Exception {
        SearchEngine searchEngine = SearchEngineFactory.createSearchEngine();
        return searchEngine.searchSubtypes(type, null, null, null);
      }
    });
    // add references from Types, find indirect subtypes
    for (SearchMatch match : matches) {
      if (match.getElement() instanceof Type) {
        Type subType = (Type) match.getElement();
        subTypes.add(subType);
        subTypes.addAll(getSubTypes(subType));
      }
    }
    // done
    return subTypes;
  }

  /**
   * @return all direct and indirect supertypes of the given {@link Type}.
   */
  public static Set<Type> getSuperTypes(Type type) throws DartModelException {
    Set<Type> superTypes = Sets.newHashSet();
    DartLibrary library = type.getLibrary();
    if (library != null) {
      for (String superTypeName : type.getSupertypeNames()) {
        Type superType = library.findTypeInScope(superTypeName);
        if (superType != null && superTypes.add(superType)) {
          superTypes.addAll(getSuperTypes(superType));
        }
      }
    }
    return superTypes;
  }

  /**
   * @return the first top-level {@link CompilationUnitElement} in the enclosing {@link DartLibrary}
   *         or any {@link DartLibrary} imported by it, which has given name. May be
   *         <code>null</code>.
   */
  public static CompilationUnitElement getTopLevelElementNamed(DartElement reference, String name)
      throws DartModelException {
    DartLibrary library = reference.getAncestor(DartLibrary.class);
    if (library != null) {
      // search in import prefixes
      for (DartImport dartImport : library.getImports()) {
        if (Objects.equal(dartImport.getPrefix(), name)) {
          return dartImport;
        }
      }
      // search in contributing units of this library
      for (CompilationUnit unit : library.getCompilationUnitsInScope()) {
        for (DartElement element : unit.getChildren()) {
          if (element instanceof CompilationUnitElement
              && Objects.equal(element.getElementName(), name)) {
            return (CompilationUnitElement) element;
          }
        }
      }
    }
    // not found
    return null;
  }

  /**
   * @return {@link TypeMember} children of the given {@link Type};
   */
  public static List<TypeMember> getTypeMembers(Type type) throws DartModelException {
    List<TypeMember> members = Lists.newArrayList();
    for (DartElement typeChild : type.getChildren()) {
      if (typeChild instanceof Method && ((Method) typeChild).isImplicit()) {
        continue;
      }
      if (typeChild instanceof TypeMember) {
        members.add((TypeMember) typeChild);
      }
    }
    return members;
  }

  /**
   * @return <code>true</code> if given name has public privacy, i.e. does not starts with
   *         underscore.
   */
  public static boolean isPublicName(String name) {
    return !StringUtils.startsWith(name, "_");
  }

  /**
   * @return <code>true</code> if second {@link Type} is super type for first one.
   */
  public static boolean isTypeHierarchy(Type type, Type superType) throws CoreException {
    return getSuperTypes(type).contains(superType);
  }

  private RenameAnalyzeUtil() {
  }
}
