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
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.scanner.Token;
import com.google.dart.server.NavigationRegion;
import com.google.dart.server.NavigationTarget;

import java.util.List;

/**
 * A computer for {@link NavigationRegion}s in a Dart {@link CompilationUnit}.
 * 
 * @coverage dart.server.local
 */
public class DartUnitNavigationComputer {
  private final CompilationUnit unit;
  private final List<NavigationRegion> regions = Lists.newArrayList();

  public DartUnitNavigationComputer(CompilationUnit unit) {
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
      public Void visitIndexExpression(IndexExpression node) {
        addRegionForToken(node.getRightBracket(), node.getBestElement());
        return super.visitIndexExpression(node);
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
        addRegionForNode(node, node.getBestElement());
        return super.visitSimpleIdentifier(node);
      }
    });
    return regions.toArray(new NavigationRegion[regions.size()]);
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
  private void addRegion(int offset, int length, Element element) {
    NavigationTarget target = createTarget(element);
    if (target == null) {
      return;
    }
    regions.add(new NavigationRegionImpl(offset, length, new NavigationTarget[] {target}));
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
   * Returns the {@link NavigationTarget} for the given {@link Element}, maybe {@code null} if
   * {@code null} was given.
   */
  private NavigationTarget createTarget(Element element) {
    if (element == null) {
      return null;
    }
    return new NavigationTargetImpl(
        element.getSource(),
        getElementId(element),
        element.getNameOffset(),
        element.getDisplayName().length());
  }

  private String getElementId(Element element) {
    return element.getLocation().getEncoding();
  }
}
