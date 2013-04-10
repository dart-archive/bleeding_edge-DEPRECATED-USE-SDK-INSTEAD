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

package com.google.dart.engine.internal.index;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.ExtendsClause;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.ImplementsClause;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.UriBasedDirective;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.WithClause;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.ExportElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FieldFormalParameterElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LabelElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.element.visitor.GeneralizingElementVisitor;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.collection.IntStack;

import java.util.LinkedList;
import java.util.List;

/**
 * Visits resolved AST and adds relationships into {@link IndexStore}.
 * 
 * @coverage dart.engine.index
 */
public class IndexContributor extends GeneralizingASTVisitor<Void> {

  /**
   * @return the {@link Location} representing location of the {@link Element}.
   */
  @VisibleForTesting
  public static Location createElementLocation(Element element) {
    if (element != null) {
      int offset = element.getNameOffset();
      int length = element.getName().length();
      String prefix = null;
      return new Location(element, offset, length, prefix);
    }
    return null;
  }

  /**
   * @return the library import prefix name, may be {@code null}.
   */
  @VisibleForTesting
  static String getLibraryImportPrefix(ASTNode node) {
    // "prefix.Type" or "prefix.topLevelVariable"
    if (node.getParent() instanceof PrefixedIdentifier) {
      PrefixedIdentifier propertyAccess = (PrefixedIdentifier) node.getParent();
      SimpleIdentifier qualifier = propertyAccess.getPrefix();
      if (qualifier instanceof SimpleIdentifier && qualifier.getElement() instanceof LibraryElement) {
        return qualifier.getName();
      }
    }
    // no prefix
    return null;
  }

  /**
   * @return {@code true} if given "node" is part of {@link PrefixedIdentifier} "prefix.node".
   */
  private static boolean isIdentifierInPrefixedIdentifier(SimpleIdentifier node) {
    ASTNode parent = node.getParent();
    return parent instanceof PrefixedIdentifier
        && ((PrefixedIdentifier) parent).getIdentifier() == node;
  }

  /**
   * @return {@code true} if given {@link SimpleIdentifier} is "name" part of prefixed identifier or
   *         method invocation.
   */
  private static boolean isQualified(SimpleIdentifier node) {
    ASTNode parent = node.getParent();
    if (parent instanceof PrefixedIdentifier) {
      return ((PrefixedIdentifier) parent).getIdentifier() == node;
    }
    if (parent instanceof PropertyAccess) {
      return ((PropertyAccess) parent).getPropertyName() == node;
    }
    if (parent instanceof MethodInvocation) {
      MethodInvocation invocation = (MethodInvocation) parent;
      return invocation.getRealTarget() != null && invocation.getMethodName() == node;
    }
    return false;
  }

  private final IndexStore store;

  private LibraryElement libraryElement;

  /**
   * A stack whose top element (the element with the largest index) is an element representing the
   * inner-most enclosing scope.
   */
  private LinkedList<Element> elementStack = Lists.newLinkedList();

  /**
   * A stack containing one value for each name scope that has been entered, where the values are a
   * count of the number of unnamed functions that have been found within that scope. These counts
   * are used to synthesize a name for those functions. The innermost scope is at the top of the
   * stack.
   */
  private IntStack unnamedFunctionCount = new IntStack();

  public IndexContributor(IndexStore store) {
    this.store = store;
  }

  /**
   * @return the inner-most enclosing {@link Element}, may be {@code null}.
   */
  @VisibleForTesting
  public Element peekElement() {
    for (Element element : elementStack) {
      if (element != null) {
        return element;
      }
    }
    return null;
  }

  @Override
  public Void visitClassDeclaration(ClassDeclaration node) {
    ClassElement element = node.getElement();
    enterScope(element);
    try {
      {
        Location location = createElementLocation(element);
        recordRelationship(libraryElement, IndexConstants.DEFINES_CLASS, location);
        recordRelationship(IndexConstants.UNIVERSE, IndexConstants.DEFINES_CLASS, location);
      }
      {
        ExtendsClause extendsClause = node.getExtendsClause();
        if (extendsClause != null) {
          TypeName superclassNode = extendsClause.getSuperclass();
          recordSuperType(superclassNode, IndexConstants.IS_EXTENDED_BY);
        }
      }
      {
        WithClause withClause = node.getWithClause();
        if (withClause != null) {
          for (TypeName mixinNode : withClause.getMixinTypes()) {
            recordSuperType(mixinNode, IndexConstants.IS_MIXED_IN_BY);
          }
        }
      }
      {
        ImplementsClause implementsClause = node.getImplementsClause();
        if (implementsClause != null) {
          for (TypeName interfaceNode : implementsClause.getInterfaces()) {
            recordSuperType(interfaceNode, IndexConstants.IS_IMPLEMENTED_BY);
          }
        }
      }
      return super.visitClassDeclaration(node);
    } finally {
      exitScope();
    }
  }

  @Override
  public Void visitClassTypeAlias(ClassTypeAlias node) {
    ClassElement element = node.getElement();
    enterScope(element);
    try {
      {
        Location location = createElementLocation(element);
        recordRelationship(libraryElement, IndexConstants.DEFINES_CLASS_ALIAS, location);
        recordRelationship(IndexConstants.UNIVERSE, IndexConstants.DEFINES_CLASS_ALIAS, location);
      }
      {
        TypeName superclassNode = node.getSuperclass();
        if (superclassNode != null) {
          recordSuperType(superclassNode, IndexConstants.IS_EXTENDED_BY);
        }
      }
      {
        WithClause withClause = node.getWithClause();
        if (withClause != null) {
          for (TypeName mixinNode : withClause.getMixinTypes()) {
            recordSuperType(mixinNode, IndexConstants.IS_MIXED_IN_BY);
          }
        }
      }
      {
        ImplementsClause implementsClause = node.getImplementsClause();
        if (implementsClause != null) {
          for (TypeName interfaceNode : implementsClause.getInterfaces()) {
            recordSuperType(interfaceNode, IndexConstants.IS_IMPLEMENTED_BY);
          }
        }
      }
      return super.visitClassTypeAlias(node);
    } finally {
      exitScope();
    }
  }

  @Override
  public Void visitCompilationUnit(CompilationUnit node) {
    CompilationUnitElement unitElement = node.getElement();
    if (unitElement != null) {
      elementStack.add(unitElement);
      libraryElement = unitElement.getEnclosingElement();
      if (libraryElement != null) {
        recordUnitElements(unitElement);
        return super.visitCompilationUnit(node);
      }
    }
    return null;
  }

  @Override
  public Void visitConstructorDeclaration(ConstructorDeclaration node) {
    ConstructorElement element = node.getElement();
    // define
    {
      Location location;
      if (node.getName() != null) {
        int start = node.getPeriod().getOffset();
        int end = node.getName().getEnd();
        location = createLocation(start, end - start, null);
      } else {
        int start = node.getReturnType().getEnd();
        location = createLocation(start, 0, null);
      }
      recordRelationship(element, IndexConstants.IS_DEFINED_BY, location);
    }
    // visit children
    enterScope(element);
    try {
      return super.visitConstructorDeclaration(node);
    } finally {
      exitScope();
    }
  }

  @Override
  public Void visitConstructorName(ConstructorName node) {
    ConstructorElement element = node.getElement();
    Location location;
    if (node.getName() != null) {
      int start = node.getPeriod().getOffset();
      int end = node.getName().getEnd();
      location = createLocation(start, end - start, null);
    } else {
      int start = node.getType().getEnd();
      location = createLocation(start, 0, null);
    }
    recordRelationship(element, IndexConstants.IS_REFERENCED_BY, location);
    return super.visitConstructorName(node);
  }

  @Override
  public Void visitExportDirective(ExportDirective node) {
    ExportElement element = (ExportElement) node.getElement();
    if (element != null) {
      LibraryElement expLibrary = element.getExportedLibrary();
      recordLibraryReference(node, expLibrary);
    }
    return super.visitExportDirective(node);
  }

  @Override
  public Void visitFunctionDeclaration(FunctionDeclaration node) {
    Element element = node.getElement();
    Location location = createElementLocation(element);
    recordRelationship(libraryElement, IndexConstants.DEFINES_FUNCTION, location);
    recordRelationship(IndexConstants.UNIVERSE, IndexConstants.DEFINES_FUNCTION, location);
    enterScope(element);
    try {
      return super.visitFunctionDeclaration(node);
    } finally {
      exitScope();
    }
  }

  @Override
  public Void visitFunctionTypeAlias(FunctionTypeAlias node) {
    Element element = node.getElement();
    Location location = createElementLocation(element);
    recordRelationship(libraryElement, IndexConstants.DEFINES_FUNCTION_TYPE, location);
    recordRelationship(IndexConstants.UNIVERSE, IndexConstants.DEFINES_FUNCTION_TYPE, location);
    return super.visitFunctionTypeAlias(node);
  }

  @Override
  public Void visitImportDirective(ImportDirective node) {
    ImportElement element = (ImportElement) node.getElement();
    if (element != null) {
      LibraryElement impLibrary = element.getImportedLibrary();
      recordLibraryReference(node, impLibrary);
    }
    return super.visitImportDirective(node);
  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    ExecutableElement element = node.getElement();
    enterScope(element);
    try {
      return super.visitMethodDeclaration(node);
    } finally {
      exitScope();
    }
  }

  @Override
  public Void visitMethodInvocation(MethodInvocation node) {
    SimpleIdentifier name = node.getMethodName();
    Element element = name.getElement();
    if (element instanceof MethodElement) {
      Location location = createLocation(name);
      Relationship relationship;
      if (node.getTarget() != null) {
        relationship = IndexConstants.IS_INVOKED_BY_QUALIFIED;
      } else {
        relationship = IndexConstants.IS_INVOKED_BY_UNQUALIFIED;
      }
      recordRelationship(element, relationship, location);
    }
    if (element instanceof FunctionElement) {
      Location location = createLocation(name);
      recordRelationship(element, IndexConstants.IS_INVOKED_BY, location);
    }
    return super.visitMethodInvocation(node);
  }

  @Override
  public Void visitPartDirective(PartDirective node) {
    Element element = node.getElement();
    Location location = createLocation(node.getUri());
    recordRelationship(element, IndexConstants.IS_REFERENCED_BY, location);
    return super.visitPartDirective(node);
  }

  @Override
  public Void visitSimpleIdentifier(SimpleIdentifier node) {
    Element nameElement = new NameElementImpl(node.getName());
    Location location = createLocation(node);
    // name in declaration
    if (node.inDeclarationContext()) {
      recordRelationship(nameElement, IndexConstants.IS_DEFINED_BY, location);
      return null;
    }
    // prepare information
    Element element = node.getElement();
    // qualified name reference
    recordQualifiedMemberReference(node, element, nameElement, location);
    // stop if already handled
    if (isAlreadyHandledName(node)) {
      return null;
    }
    // record specific relations
    if (element instanceof ClassElement || element instanceof FunctionTypeAliasElement
        || element instanceof TypeVariableElement || element instanceof LabelElement
        || element instanceof FunctionElement) {
      recordRelationship(element, IndexConstants.IS_REFERENCED_BY, location);
    } else if (element instanceof FieldFormalParameterElement) {
      FieldFormalParameterElement fieldParameter = (FieldFormalParameterElement) element;
      FieldElement field = fieldParameter.getField();
      recordRelationship(field, IndexConstants.IS_REFERENCED_BY_QUALIFIED, location);
    } else if (element instanceof PrefixElement) {
      // TODO(scheglov) record ImportElement
//      recordRelationship(element.get, IndexConstants.IS_REFERENCED_BY, location);
    } else if (element instanceof PropertyAccessorElement || element instanceof MethodElement) {
      if (isQualified(node)) {
        recordRelationship(element, IndexConstants.IS_REFERENCED_BY_QUALIFIED, location);
      } else {
        recordRelationship(element, IndexConstants.IS_REFERENCED_BY_UNQUALIFIED, location);
      }
    } else if (element instanceof ParameterElement || element instanceof LocalVariableElement) {
      boolean inGetterContext = node.inGetterContext();
      boolean inSetterContext = node.inSetterContext();
      if (inGetterContext && inSetterContext) {
        recordRelationship(element, IndexConstants.IS_READ_WRITTEN_BY, location);
      } else if (inGetterContext) {
        recordRelationship(element, IndexConstants.IS_READ_BY, location);
      } else if (inSetterContext) {
        recordRelationship(element, IndexConstants.IS_WRITTEN_BY, location);
      } else {
        recordRelationship(element, IndexConstants.IS_REFERENCED_BY, location);
      }
    }
    recordImportElementReferenceWithoutPrefix(node);
    return super.visitSimpleIdentifier(node);
  }

  @Override
  public Void visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    ConstructorElement element = node.getElement();
    Location location;
    if (node.getConstructorName() != null) {
      int start = node.getPeriod().getOffset();
      int end = node.getConstructorName().getEnd();
      location = createLocation(start, end - start, null);
    } else {
      int start = node.getKeyword().getEnd();
      location = createLocation(start, 0, null);
    }
    recordRelationship(element, IndexConstants.IS_REFERENCED_BY, location);
    return super.visitSuperConstructorInvocation(node);
  }

  @Override
  public Void visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
    VariableDeclarationList variables = node.getVariables();
    for (VariableDeclaration variableDeclaration : variables.getVariables()) {
      Element element = variableDeclaration.getElement();
      Location location = createElementLocation(element);
      recordRelationship(libraryElement, IndexConstants.DEFINES_VARIABLE, location);
      recordRelationship(IndexConstants.UNIVERSE, IndexConstants.DEFINES_VARIABLE, location);
    }
    return super.visitTopLevelVariableDeclaration(node);
  }

  @Override
  public Void visitTypeParameter(TypeParameter node) {
    TypeVariableElement element = node.getElement();
    enterScope(element);
    try {
      return super.visitTypeParameter(node);
    } finally {
      exitScope();
    }
  }

  @Override
  public Void visitVariableDeclaration(VariableDeclaration node) {
    VariableElement element = node.getElement();
    enterScope(element);
    try {
      return super.visitVariableDeclaration(node);
    } finally {
      exitScope();
    }
  }

  /**
   * Enter a new scope represented by the given {@link Element}.
   */
  @VisibleForTesting
  void enterScope(Element element) {
    elementStack.addFirst(element);
    unnamedFunctionCount.push(0);
  }

  /**
   * @return the {@link Location} representing location of the {@link ASTNode}.
   */
  private Location createLocation(ASTNode node) {
    String prefix = getLibraryImportPrefix(node);
    return createLocation(node.getOffset(), node.getLength(), prefix);
  }

  /**
   * @param offset the offset of the location within {@link Source}
   * @param length the length of the location
   * @param prefix the import prefix of top-level element, may be {@code null}
   * @return the {@link Location} representing the given offset and length within the inner-most
   *         {@link Element}.
   */
  private Location createLocation(int offset, int length, String prefix) {
    Element element = peekElement();
    return new Location(element, offset, length, prefix);
  }

  /**
   * Exit the current scope.
   */
  private void exitScope() {
    elementStack.removeFirst();
    unnamedFunctionCount.pop();
  }

  /**
   * @return {@code true} if given node already indexed as more interesting reference, so it should
   *         not be indexed again.
   */
  private boolean isAlreadyHandledName(SimpleIdentifier node) {
    ASTNode parent = node.getParent();
    if (parent instanceof MethodInvocation) {
      Element element = node.getElement();
      if (element instanceof MethodElement || element instanceof FunctionElement) {
        return ((MethodInvocation) parent).getMethodName() == node;
      }
    }
    return false;
  }

  /**
   * Records {@link ImportElement} reference if given {@link SimpleIdentifier} references some
   * top-level element and not qualified with import prefix.
   */
  private void recordImportElementReferenceWithoutPrefix(SimpleIdentifier node) {
    Element element = node.getElement();
    if (element != null && element.getEnclosingElement() instanceof CompilationUnitElement
        && !isIdentifierInPrefixedIdentifier(node)) {
      LibraryElement importLibraryElement = element.getLibrary();
      for (ImportElement importElement : libraryElement.getImports()) {
        if (importElement.getPrefix() == null
            && Objects.equal(importElement.getImportedLibrary(), importLibraryElement)) {
          Location location = createLocation(node.getOffset(), 0, null);
          recordRelationship(importElement, IndexConstants.IS_REFERENCED_BY, location);
          break;
        }
      }
    }
  }

  /**
   * Records reference to the given {@link LibraryElement} and its defining
   * {@link CompilationUnitElement}.
   */
  private void recordLibraryReference(UriBasedDirective node, LibraryElement library) {
    if (library != null) {
      Location location = createLocation(node.getUri());
      recordRelationship(library, IndexConstants.IS_REFERENCED_BY, location);
      recordRelationship(
          library.getDefiningCompilationUnit(),
          IndexConstants.IS_REFERENCED_BY,
          location);
    }
  }

  /**
   * Records reference if the given {@link SimpleIdentifier} looks like a qualified property access
   * or method invocation.
   */
  private void recordQualifiedMemberReference(SimpleIdentifier node, Element element,
      Element nameElement, Location location) {
    if (isQualified(node)) {
      Relationship relationship = element != null
          ? IndexConstants.IS_REFERENCED_BY_QUALIFIED_RESOLVED
          : IndexConstants.IS_REFERENCED_BY_QUALIFIED_UNRESOLVED;
      recordRelationship(nameElement, relationship, location);
    }
  }

  /**
   * Record the given relationship between the given {@link Element} and {@link Location}.
   */
  private void recordRelationship(Element element, Relationship relationship, Location location) {
    if (element != null && location != null) {
      store.recordRelationship(element, relationship, location);
    }
  }

  /**
   * Records extends/implements relationships between given {@link ClassElement} and {@link Type} of
   * "superNode".
   */
  private void recordSuperType(TypeName superNode, Relationship relationship) {
    if (superNode != null) {
      Identifier superName = superNode.getName();
      if (superName != null) {
        Element superElement = superName.getElement();
        recordRelationship(superElement, relationship, createLocation(superNode));
      }
    }
  }

  /**
   * Remembers {@link Element}s declared in the {@link Source} of the given
   * {@link CompilationUnitElement}.
   */
  private void recordUnitElements(final CompilationUnitElement unitElement) {
    final List<Element> unitElements = Lists.newArrayList();
    // add elements of unit itself
    unitElement.accept(new GeneralizingElementVisitor<Void>() {
      @Override
      public Void visitElement(Element element) {
        unitElements.add(element);
        return super.visitElement(element);
      }
    });
    // add also children of LibraryElement
    if (libraryElement.getDefiningCompilationUnit() == unitElement) {
      libraryElement.accept(new GeneralizingElementVisitor<Void>() {
        @Override
        public Void visitElement(Element element) {
          // don't visit units
          if (element instanceof CompilationUnitElement) {
            return null;
          }
          // visit LibraryElement children
          unitElements.add(element);
          return super.visitElement(element);
        }
      });
    }
    // do record
    AnalysisContext context = unitElement.getContext();
    Source source = unitElement.getSource();
    store.recordSourceElements(context, source, unitElements);
  }
}
