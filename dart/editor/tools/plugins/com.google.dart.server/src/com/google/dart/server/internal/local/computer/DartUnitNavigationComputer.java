/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.server.internal.local.computer;

import com.google.common.collect.Lists;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExportElement;
import com.google.dart.engine.element.FieldFormalParameterElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.scanner.Token;
import com.google.dart.server.NavigationRegion;

import java.util.List;

/**
 * A computer for {@link NavigationRegion}s in a Dart {@link CompilationUnit}.
 * 
 * @coverage dart.server.local
 */
public class DartUnitNavigationComputer {
  private final String contextId;
  private final CompilationUnit unit;
  private final List<NavigationRegion> regions = Lists.newArrayList();

  public DartUnitNavigationComputer(String contextId, CompilationUnit unit) {
    this.contextId = contextId;
    this.unit = unit;
  }

  /**
   * Returns the computed {@link NavigationRegion}s, not {@code null}.
   */
  public NavigationRegion[] compute() {
    unit.accept(new RecursiveAstVisitor<Void>() {
      @Override
      public Void visitAssignmentExpression(AssignmentExpression node) {
        addRegionForToken(node.getOperator(), node.getBestElement());
        return super.visitAssignmentExpression(node);
      }

      @Override
      public Void visitBinaryExpression(BinaryExpression node) {
        addRegionForToken(node.getOperator(), node.getBestElement());
        return super.visitBinaryExpression(node);
      }

      @Override
      public Void visitConstructorDeclaration(ConstructorDeclaration node) {
        // associate constructor with "T" or "T.name"
        {
          AstNode firstNode = node.getReturnType();
          AstNode lastNode = node.getName();
          if (lastNode == null) {
            lastNode = firstNode;
          }
          if (firstNode != null && lastNode != null) {
            addRegion_nodeStart_nodeEnd(firstNode, lastNode, node.getElement());
          }
        }
        return super.visitConstructorDeclaration(node);
      }

      @Override
      public Void visitExportDirective(ExportDirective node) {
        ExportElement exportElement = node.getElement();
        if (exportElement != null) {
          Element element = exportElement.getExportedLibrary();
          addRegion_tokenStart_nodeEnd(node.getKeyword(), node.getUri(), element);
        }
        return super.visitExportDirective(node);
      }

      @Override
      public Void visitImportDirective(ImportDirective node) {
        ImportElement importElement = node.getElement();
        if (importElement != null) {
          Element element = importElement.getImportedLibrary();
          addRegion_tokenStart_nodeEnd(node.getKeyword(), node.getUri(), element);
        }
        return super.visitImportDirective(node);
      }

      @Override
      public Void visitIndexExpression(IndexExpression node) {
        addRegionForToken(node.getRightBracket(), node.getBestElement());
        return super.visitIndexExpression(node);
      }

      @Override
      public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
        addRegion_nodeStart_nodeStart(node, node.getArgumentList(), node.getStaticElement());
        return super.visitInstanceCreationExpression(node);
      }

      @Override
      public Void visitPartDirective(PartDirective node) {
        addRegion_tokenStart_nodeEnd(node.getKeyword(), node.getUri(), node.getElement());
        return super.visitPartDirective(node);
      }

      @Override
      public Void visitPartOfDirective(PartOfDirective node) {
        addRegion_tokenStart_nodeEnd(node.getKeyword(), node.getLibraryName(), node.getElement());
        return super.visitPartOfDirective(node);
      }

      @Override
      public Void visitPostfixExpression(PostfixExpression node) {
        addRegionForToken(node.getOperator(), node.getBestElement());
        return super.visitPostfixExpression(node);
      }

      @Override
      public Void visitPrefixExpression(PrefixExpression node) {
        addRegionForToken(node.getOperator(), node.getBestElement());
        return super.visitPrefixExpression(node);
      }

      @Override
      public Void visitSimpleIdentifier(SimpleIdentifier node) {
        if (node.getParent() instanceof ConstructorDeclaration) {
          // we have already recorded a region for this constructor name
        } else {
          addRegionForNode(node, node.getBestElement());
        }
        return super.visitSimpleIdentifier(node);
      }
    });
    return regions.toArray(new NavigationRegion[regions.size()]);
  }

  /**
   * If the given {@link Element} is not {@code null}, then creates a corresponding
   * {@link NavigationRegion}.
   */
  private void addRegion(int offset, int length, Element element) {
    com.google.dart.server.Element target = createTarget(element);
    if (target == null) {
      return;
    }
    throw new IllegalStateException("Not yet implemented: API has changed in NavigationRegionImpl");
    // API has changed in NavigationRegionImpl
//    regions.add(new NavigationRegionImpl(
//        offset,
//        length,
//        new com.google.dart.server.Element[] {target}));
  }

  private void addRegion_nodeStart_nodeEnd(AstNode a, AstNode b, Element element) {
    int offset = a.getOffset();
    int length = b.getEnd() - offset;
    addRegion(offset, length, element);
  }

  private void addRegion_nodeStart_nodeStart(AstNode a, AstNode b, Element element) {
    int offset = a.getOffset();
    int length = b.getOffset() - offset;
    addRegion(offset, length, element);
  }

  private void addRegion_tokenStart_nodeEnd(Token a, AstNode b, Element element) {
    int offset = a.getOffset();
    int length = b.getEnd() - offset;
    addRegion(offset, length, element);
  }

  /**
   * If the given {@link Element} is not {@code null}, then creates a corresponding
   * {@link NavigationRegion}.
   */
  private void addRegionForNode(AstNode node, Element element) {
    int offset = node.getOffset();
    int length = node.getLength();
    addRegion(offset, length, element);
  }

  /**
   * If the given {@link Element} is not {@code null}, then creates a corresponding
   * {@link NavigationRegion}.
   */
  private void addRegionForToken(Token token, Element element) {
    int offset = token.getOffset();
    int length = token.getLength();
    addRegion(offset, length, element);
  }

  /**
   * Returns the {@link com.google.dart.server.Element} for the given {@link Element}, maybe
   * {@code null} if {@code null} was given.
   */
  private com.google.dart.server.Element createTarget(Element element) {
    if (element == null) {
      return null;
    }
    if (element instanceof FieldFormalParameterElement) {
      element = ((FieldFormalParameterElement) element).getField();
    }
    // TODO (jwren) Element API has changed
//    return ElementImpl.create(contextId, element);
    return null;
  }
}
