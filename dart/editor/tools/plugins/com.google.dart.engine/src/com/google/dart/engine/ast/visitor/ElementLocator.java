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
package com.google.dart.engine.ast.visitor;

import com.google.dart.engine.ast.Annotation;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.LibraryIdentifier;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.ast.UriBasedDirective;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.internal.builder.AngularCompilationUnitBuilder;
import com.google.dart.engine.internal.builder.PolymerCompilationUnitBuilder;

/**
 * Instances of the class {@code ElementLocator} locate the {@link Element Dart model element}
 * associated with a given {@link AstNode AST node}.
 * 
 * @coverage dart.engine.ast
 */
public class ElementLocator {
  /**
   * Visitor that maps nodes to elements.
   */
  private static final class ElementMapper extends GeneralizingAstVisitor<Element> {
    @Override
    public Element visitAnnotation(Annotation node) {
      return node.getElement();
    }

    @Override
    public Element visitAssignmentExpression(AssignmentExpression node) {
      return node.getBestElement();
    }

    @Override
    public Element visitBinaryExpression(BinaryExpression node) {
      return node.getBestElement();
    }

    @Override
    public Element visitClassDeclaration(ClassDeclaration node) {
      return node.getElement();
    }

    @Override
    public Element visitCompilationUnit(CompilationUnit node) {
      return node.getElement();
    }

    @Override
    public Element visitConstructorDeclaration(ConstructorDeclaration node) {
      return node.getElement();
    }

    @Override
    public Element visitFunctionDeclaration(FunctionDeclaration node) {
      return node.getElement();
    }

    @Override
    public Element visitIdentifier(Identifier node) {
      AstNode parent = node.getParent();
      // Type name in Annotation
      if (parent instanceof Annotation) {
        Annotation annotation = (Annotation) parent;
        if (annotation.getName() == node && annotation.getConstructorName() == null) {
          return annotation.getElement();
        }
      }
      // Extra work to map Constructor Declarations to their associated Constructor Elements
      if (parent instanceof ConstructorDeclaration) {
        ConstructorDeclaration decl = (ConstructorDeclaration) parent;
        Identifier returnType = decl.getReturnType();
        if (returnType == node) {
          SimpleIdentifier name = decl.getName();
          if (name != null) {
            return name.getBestElement();
          }
          Element element = node.getBestElement();
          if (element instanceof ClassElement) {
            return ((ClassElement) element).getUnnamedConstructor();
          }
        }
      }
      if (parent instanceof LibraryIdentifier) {
        AstNode grandParent = ((LibraryIdentifier) parent).getParent();
        if (grandParent instanceof PartOfDirective) {
          Element element = ((PartOfDirective) grandParent).getElement();
          if (element instanceof LibraryElement) {
            return ((LibraryElement) element).getDefiningCompilationUnit();
          }
        }
      }
      return node.getBestElement();
    }

    @Override
    public Element visitImportDirective(ImportDirective node) {
      return node.getElement();
    }

    @Override
    public Element visitIndexExpression(IndexExpression node) {
      return node.getBestElement();
    }

    @Override
    public Element visitInstanceCreationExpression(InstanceCreationExpression node) {
      return node.getStaticElement();
    }

    @Override
    public Element visitLibraryDirective(LibraryDirective node) {
      return node.getElement();
    }

    @Override
    public Element visitMethodDeclaration(MethodDeclaration node) {
      return node.getElement();
    }

    @Override
    public Element visitMethodInvocation(MethodInvocation node) {
      return node.getMethodName().getBestElement();
    }

    @Override
    public Element visitPostfixExpression(PostfixExpression node) {
      return node.getBestElement();
    }

    @Override
    public Element visitPrefixedIdentifier(PrefixedIdentifier node) {
      return node.getBestElement();
    }

    @Override
    public Element visitPrefixExpression(PrefixExpression node) {
      return node.getBestElement();
    }

    @Override
    public Element visitStringLiteral(StringLiteral node) {
      AstNode parent = node.getParent();
      if (parent instanceof UriBasedDirective) {
        return ((UriBasedDirective) parent).getUriElement();
      }
      return null;
    }

    @Override
    public Element visitVariableDeclaration(VariableDeclaration node) {
      return node.getElement();
    }
  }

  /**
   * Locate the {@link Element Dart model element} associated with the given {@link AstNode AST
   * node}.
   * 
   * @param node the node (not {@code null})
   * @return the associated element, or {@code null} if none is found
   */
  public static Element locate(AstNode node) {
    ElementMapper mapper = new ElementMapper();
    return node.accept(mapper);
  }

  /**
   * Locate the {@link Element Dart model element} associated with the given {@link AstNode AST
   * node} and offset.
   * 
   * @param node the node (not {@code null})
   * @param offset the offset relative to source
   * @return the associated element, or {@code null} if none is found
   */
  public static Element locateWithOffset(AstNode node, int offset) {
    if (node == null) {
      return null;
    }
    // try to get Element from node
    {
      Element nodeElement = locate(node);
      if (nodeElement != null) {
        return nodeElement;
      }
    }
    // try to get Angular specific Element
    {
      Element element = AngularCompilationUnitBuilder.getElement(node, offset);
      if (element != null) {
        return element;
      }
    }
    // try to get Polymer specific Element
    {
      Element element = PolymerCompilationUnitBuilder.getElement(node, offset);
      if (element != null) {
        return element;
      }
    }
    // no Element
    return null;
  }

  /**
   * Clients should use {@link #locate(AstNode)} or {@link #locateWithOffset(AstNode, int)}.
   */
  private ElementLocator() {
  }

}
