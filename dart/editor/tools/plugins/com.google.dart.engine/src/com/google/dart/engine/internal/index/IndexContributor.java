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
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.ExtendsClause;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.ImplementsClause;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.Label;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NamedExpression;
import com.google.dart.engine.ast.NamespaceDirective;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.WithClause;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LabelElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.TypeAliasElement;
import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.collection.IntStack;

import java.util.LinkedList;

/**
 * Visits resolved AST and adds relationships into {@link IndexStore}.
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
   * @return the library import prefix name, may be <code>null</code>.
   */
  @VisibleForTesting
  static String getLibraryImportPrefix(ASTNode node) {
    // "prefix.topLevelFunction()" looks as method invocation
    if (node.getParent() instanceof MethodInvocation) {
      MethodInvocation invocation = (MethodInvocation) node.getParent();
      if (invocation.getRealTarget() instanceof SimpleIdentifier) {
        SimpleIdentifier target = (SimpleIdentifier) invocation.getRealTarget();
        if (target != null && target.getElement() instanceof LibraryElement) {
          return target.getName();
        }
      }
    }
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
   * @return the name node of the given {@link ASTNode}.
   */
  private static Identifier getNameNode(ASTNode node) {
    if (node instanceof ClassDeclaration) {
      return ((ClassDeclaration) node).getName();
    }
    if (node instanceof ClassTypeAlias) {
      return ((ClassTypeAlias) node).getName();
    }
    if (node instanceof FunctionTypeAlias) {
      return ((FunctionTypeAlias) node).getName();
    }
    if (node instanceof FunctionDeclaration) {
      return ((FunctionDeclaration) node).getName();
    }
    if (node instanceof MethodDeclaration) {
      return ((MethodDeclaration) node).getName();
    }
    if (node instanceof VariableDeclaration) {
      return ((VariableDeclaration) node).getName();
    }
    if (node instanceof SimpleFormalParameter) {
      return ((SimpleFormalParameter) node).getIdentifier();
    }
    return null;
  }

  private static boolean isIdentifierInPrefixedIdentifier(SimpleIdentifier node) {
    ASTNode parent = node.getParent();
    return parent instanceof PrefixedIdentifier
        && ((PrefixedIdentifier) parent).getIdentifier() == node;
  }

  /**
   * @return <code>true</code> if the given identifier is the name in a declaration
   */
  private static boolean isNameInDeclaration(SimpleIdentifier node) {
    ASTNode parent = node.getParent();
    return getNameNode(parent) == node;
  }

  /**
   * @return <code>true</code> if given {@link SimpleIdentifier} is "name" part of prefixed
   *         identifier or method invocation.
   */
  private static boolean isQualified(SimpleIdentifier node) {
    ASTNode parent = node.getParent();
    if (parent instanceof PrefixedIdentifier) {
      return ((PrefixedIdentifier) parent).getIdentifier() == node;
    }
    if (parent instanceof PropertyAccess) {
      return ((PropertyAccess) parent).getPropertyName() == node;
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
   * @return the inner-most enclosing {@link Element}, may be <code>null</code>.
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
          recordSuperType(element, superclassNode, IndexConstants.IS_EXTENDED_BY);
        }
      }
      {
        WithClause withClause = node.getWithClause();
        if (withClause != null) {
          for (TypeName mixinNode : withClause.getMixinTypes()) {
            recordSuperType(element, mixinNode, IndexConstants.IS_MIXED_IN_BY);
          }
        }
      }
      {
        ImplementsClause implementsClause = node.getImplementsClause();
        if (implementsClause != null) {
          for (TypeName interfaceNode : implementsClause.getInterfaces()) {
            recordSuperType(element, interfaceNode, IndexConstants.IS_IMPLEMENTED_BY);
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
          recordSuperType(element, superclassNode, IndexConstants.IS_EXTENDED_BY);
        }
      }
      {
        WithClause withClause = node.getWithClause();
        if (withClause != null) {
          for (TypeName mixinNode : withClause.getMixinTypes()) {
            recordSuperType(element, mixinNode, IndexConstants.IS_MIXED_IN_BY);
          }
        }
      }
      {
        ImplementsClause implementsClause = node.getImplementsClause();
        if (implementsClause != null) {
          for (TypeName interfaceNode : implementsClause.getInterfaces()) {
            recordSuperType(element, interfaceNode, IndexConstants.IS_IMPLEMENTED_BY);
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
        return super.visitCompilationUnit(node);
      }
    }
    return null;
  }

  @Override
  public Void visitFunctionDeclaration(FunctionDeclaration node) {
    Element element = node.getElement();
    enterScope(element);
    try {
      Location location = createElementLocation(element);
      recordRelationship(libraryElement, IndexConstants.DEFINES_FUNCTION, location);
      recordRelationship(IndexConstants.UNIVERSE, IndexConstants.DEFINES_FUNCTION, location);
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
  public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
    ConstructorName name = node.getConstructorName();
    Location location = createLocation(name);
    recordRelationship(name.getElement(), IndexConstants.IS_INVOKED_BY_UNQUALIFIED, location);
    return super.visitInstanceCreationExpression(node);
  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    MethodElement element = node.getElement();
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
    Location location = createLocation(name);
    Relationship relationship;
    if (node.getTarget() != null) {
      relationship = IndexConstants.IS_INVOKED_BY_QUALIFIED;
    } else {
      relationship = IndexConstants.IS_INVOKED_BY_UNQUALIFIED;
    }
    recordRelationship(name.getElement(), relationship, location);
    return super.visitMethodInvocation(node);
  }

  @Override
  public Void visitNamedExpression(NamedExpression node) {
    SimpleIdentifier name = node.getName().getLabel();
    Location location = createLocation(name);
    recordRelationship(name.getElement(), IndexConstants.IS_REFERENCED_BY, location);
    return super.visitNamedExpression(node);
  }

  @Override
  public Void visitNamespaceDirective(NamespaceDirective node) {
    Element element = node.getElement();
    Location location = createLocation(node.getLibraryUri());
    recordRelationship(element, IndexConstants.IS_REFERENCED_BY, location);
    return super.visitNamespaceDirective(node);
  }

  @Override
  public Void visitPartDirective(PartDirective node) {
    Element element = node.getElement();
    Location location = createLocation(node.getPartUri());
    recordRelationship(element, IndexConstants.IS_REFERENCED_BY, location);
    return super.visitPartDirective(node);
  }

  @Override
  public Void visitSimpleIdentifier(SimpleIdentifier node) {
    Element nameElement = new NameElementImpl(node.getName());
    Location location = createLocation(node);
    // name in declaration
    if (isNameInDeclaration(node)) {
      recordRelationship(nameElement, IndexConstants.IS_DEFINED_BY, location);
      return null;
    }
    // name is referenced
    recordRelationship(nameElement, IndexConstants.IS_REFERENCED_BY, location);
    if (isAlreadyHandledName(node)) {
      return null;
    }
    // record specific relations
    Element element = node.getElement();
    if (element instanceof ClassElement || element instanceof TypeAliasElement
        || element instanceof TypeVariableElement || element instanceof LabelElement
        || element instanceof ImportElement) {
      recordRelationship(element, IndexConstants.IS_REFERENCED_BY, location);
    } else if (element instanceof FieldElement || element instanceof ParameterElement
        || element instanceof VariableElement || element instanceof FunctionElement
        || element instanceof MethodElement) {
      if (node.inGetterContext()) {
        if (isQualified(node)) {
          recordRelationship(element, IndexConstants.IS_ACCESSED_BY_QUALIFIED, location);
        } else {
          recordRelationship(element, IndexConstants.IS_ACCESSED_BY_UNQUALIFIED, location);
        }
      } else {
        if (isQualified(node)) {
          recordRelationship(element, IndexConstants.IS_MODIFIED_BY_QUALIFIED, location);
        } else {
          recordRelationship(element, IndexConstants.IS_MODIFIED_BY_UNQUALIFIED, location);
        }
      }
    }
    recordImportElementReferenceWithoutPrefix(node);
    return super.visitSimpleIdentifier(node);
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
   * @param prefix the import prefix of top-level element, may be <code>null</code>
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
   * @return <code>true</code> if given node already indexed as more interesting reference, so it
   *         should not be indexed again.
   */
  private boolean isAlreadyHandledName(SimpleIdentifier node) {
    ASTNode parent = node.getParent();
    if (parent instanceof MethodInvocation) {
      return ((MethodInvocation) parent).getMethodName() == node;
    }
    if (parent instanceof Label) {
      Label label = (Label) parent;
      if (label.getLabel() == node && label.getParent() instanceof NamedExpression) {
        return true;
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
    if (element != null && element.getEnclosingElement() instanceof LibraryElement
        && !isIdentifierInPrefixedIdentifier(node)) {
      LibraryElement importLibraryElement = (LibraryElement) element.getEnclosingElement();
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
  private void recordSuperType(ClassElement element, TypeName superNode, Relationship relationship) {
    if (element != null) {
      Element superElement = superNode.getName().getElement();
      if (superElement != null) {
        recordRelationship(superElement, relationship, createLocation(superNode));
      }
    }
  }

}
