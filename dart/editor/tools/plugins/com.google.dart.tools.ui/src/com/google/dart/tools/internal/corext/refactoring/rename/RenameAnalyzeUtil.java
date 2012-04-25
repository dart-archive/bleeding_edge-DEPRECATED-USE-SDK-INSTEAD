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
import com.google.dart.tools.core.dom.NodeFinder;
import com.google.dart.tools.core.internal.model.SourceRangeImpl;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.CompilationUnitElement;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartFunctionTypeAlias;
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
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableObjectEx;

import org.eclipse.core.runtime.CoreException;
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
   * @return the localized name of the {@link DartElement}.
   */
  public static String getElementTypeName(DartElement element) {
    return Messages.format(
        RefactoringCoreMessages.RenameRefactoring_elementTypeName,
        element.getElementType());
  }

  /**
   * @return the references to the given {@link DartElement}, may be empty {@link List}, but not
   *         <code>null</code>.
   */
  public static List<SearchMatch> getReferences(final DartElement element) throws CoreException {
    List<SearchMatch> fieldReferences = ExecutionUtils.runObjectCore(new RunnableObjectEx<List<SearchMatch>>() {
      @Override
      public List<SearchMatch> runObject() throws Exception {
        SearchEngine searchEngine = SearchEngineFactory.createSearchEngine();
        if (element instanceof Type) {
          return searchEngine.searchReferences((Type) element, null, null, null);
        }
        if (element instanceof Field) {
          return searchEngine.searchReferences((Field) element, null, null, null);
        }
        if (element instanceof Method) {
          return searchEngine.searchReferences((Method) element, null, null, null);
        }
        if (element instanceof DartVariableDeclaration) {
          return searchEngine.searchReferences((DartVariableDeclaration) element, null, null, null);
        }
        if (element instanceof DartFunction) {
          return searchEngine.searchReferences((DartFunction) element, null, null, null);
        }
        if (element instanceof DartFunctionTypeAlias) {
          return searchEngine.searchReferences((DartFunctionTypeAlias) element, null, null, null);
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
  public static Set<Type> getSuperTypes(Type type) throws CoreException {
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
  public static CompilationUnitElement getTopLevelElementNamed(
      Set<DartLibrary> visitedLibraries,
      DartElement reference,
      String name) throws DartModelException {
    DartLibrary library = reference.getAncestor(DartLibrary.class);
    if (library != null && !visitedLibraries.contains(library)) {
      visitedLibraries.add(library);
      // search in units of this library
      for (CompilationUnit unit : library.getCompilationUnits()) {
        for (DartElement element : unit.getChildren()) {
          if (element instanceof CompilationUnitElement
              && Objects.equal(element.getElementName(), name)) {
            return (CompilationUnitElement) element;
          }
        }
      }
      // search in imported libraries
      for (DartLibrary importedLibrary : library.getImportedLibraries()) {
        CompilationUnitElement element = getTopLevelElementNamed(
            visitedLibraries,
            importedLibrary,
            name);
        if (element != null) {
          return element;
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
      if (typeChild instanceof TypeMember) {
        members.add((TypeMember) typeChild);
      }
    }
    return members;
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
