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
import com.google.common.collect.Lists;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ExtendsClause;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.ImplementsClause;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementProxy;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.LibraryElement;
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
public class IndexContributor extends RecursiveASTVisitor<Void> {

  /**
   * @return the {@link Location} representing location of the {@link Element}.
   */
  @VisibleForTesting
  static Location createElementLocation(Element element) {
    if (element != null) {
      int offset = element.getNameOffset();
      int length = element.getName().length();
      String prefix = null;
      return new Location(new ElementProxy(element), offset, length, prefix);
    }
    return null;
  }

  /**
   * @return <code>true</code> if given {@link SimpleIdentifier} is "name" part of prefixed
   *         identifier or method invocation.
   */
  private static boolean isQualified(SimpleIdentifier node) {
    if (node.getParent() instanceof PrefixedIdentifier) {
      return ((PrefixedIdentifier) node.getParent()).getIdentifier() == node;
    }
    if (node.getParent() instanceof MethodInvocation) {
      return ((MethodInvocation) node.getParent()).getMethodName() == node;
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

  @Override
  public Void visitClassDeclaration(ClassDeclaration node) {
    // TODO(scheglov) mixins
    ClassElement element = node.getElement();
    enterScope(element);
    try {
      recordRelationship(
          libraryElement,
          IndexConstants.DEFINES_CLASS,
          createElementLocation(element));
      {
        ExtendsClause extendsClause = node.getExtendsClause();
        if (extendsClause != null) {
          TypeName superclassNode = extendsClause.getSuperclass();
          recordSuperType(element, superclassNode, IndexConstants.IS_EXTENDED_BY);
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
    Location location = createElementLocation(element);
    recordRelationship(libraryElement, IndexConstants.DEFINES_FUNCTION, location);
    return super.visitFunctionDeclaration(node);
  }

  @Override
  public Void visitFunctionTypeAlias(FunctionTypeAlias node) {
    Element element = node.getElement();
    Location location = createElementLocation(element);
    recordRelationship(libraryElement, IndexConstants.DEFINES_FUNCTION_TYPE, location);
    return super.visitFunctionTypeAlias(node);
  }

  @Override
  public Void visitSimpleIdentifier(SimpleIdentifier node) {
    Element element = node.getElement();
    if (element instanceof FieldElement) {
      FieldElement fieldElement = (FieldElement) element;
      Location location = createLocation(node);
      if (node.inGetterContext()) {
        if (isQualified(node)) {
          recordRelationship(fieldElement, IndexConstants.IS_ACCESSED_BY_QUALIFIED, location);
        } else {
          recordRelationship(fieldElement, IndexConstants.IS_ACCESSED_BY_UNQUALIFIED, location);
        }
      } else {
        // TODO(scheglov)
        throw new UnsupportedOperationException();
      }
    }
    return super.visitSimpleIdentifier(node);
  }

  @Override
  public Void visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
    VariableDeclarationList variables = node.getVariables();
    for (VariableDeclaration variableDeclaration : variables.getVariables()) {
      Element element = variableDeclaration.getElement();
      Location location = createElementLocation(element);
      recordRelationship(libraryElement, IndexConstants.DEFINES_VARIABLE, location);
    }
    return super.visitTopLevelVariableDeclaration(node);
  }

  /**
   * Enter a new scope represented by the given {@link Element}.
   */
  @VisibleForTesting
  void enterScope(Element element) {
    elementStack.addFirst(element);
    unnamedFunctionCount.push(0);
  }

//  /**
//   * @return the {@link Location} representing the location of the name of the given class.
//   */
//  private Location createNameLocation(ClassDeclaration node) {
//    return createNameLocation(node.getName());
//  }

  /**
   * @return the inner-most enclosing {@link Element}, may be <code>null</code>.
   */
  @VisibleForTesting
  Element peekElement() {
    for (Element element : elementStack) {
      if (element != null) {
        return element;
      }
    }
    return null;
  }

  /**
   * @return the {@link Location} representing location of the {@link ASTNode}.
   */
  private Location createLocation(ASTNode node) {
    // TODO(scheglov) get actual prefix
    String prefix = null;
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
    return new Location(new ElementProxy(element), offset, length, prefix);
  }

  /**
   * Exit the current scope.
   */
  private void exitScope() {
    elementStack.removeFirst();
    unnamedFunctionCount.pop();
  }

  /**
   * Record the given relationship between the given {@link Element} and {@link Location}.
   */
  private void recordRelationship(Element element, Relationship relationship, Location location) {
    if (element != null && location != null) {
      store.recordRelationship(new ElementProxy(element), relationship, location);
    }
  }

  /**
   * Records extends/implements relationships between given {@link ClassElement} and {@link Type} of
   * "superNode".
   */
  private void recordSuperType(ClassElement element, TypeName superNode, Relationship relationship) {
    if (element != null) {
      Type superType = superNode.getType();
      if (superType != null) {
        Element superElement = superType.getElement();
        recordRelationship(superElement, relationship, createLocation(superNode));
      }
    }
  }

}
