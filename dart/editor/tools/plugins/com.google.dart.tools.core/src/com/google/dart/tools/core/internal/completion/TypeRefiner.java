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
package com.google.dart.tools.core.internal.completion;

import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartBlock;
import com.google.dart.compiler.ast.DartDeclaration;
import com.google.dart.compiler.ast.DartDoWhileStatement;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartForInStatement;
import com.google.dart.compiler.ast.DartForStatement;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartIfStatement;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartStatement;
import com.google.dart.compiler.ast.DartTypeExpression;
import com.google.dart.compiler.ast.DartUnaryExpression;
import com.google.dart.compiler.ast.DartVariable;
import com.google.dart.compiler.ast.DartVariableStatement;
import com.google.dart.compiler.ast.DartWhileStatement;
import com.google.dart.compiler.parser.Token;
import com.google.dart.compiler.resolver.CoreTypeProvider;
import com.google.dart.compiler.resolver.Element;
import com.google.dart.compiler.type.Type;
import com.google.dart.compiler.type.TypeKind;
import com.google.dart.compiler.type.Types;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Optimistic-SSA type analyzer. Attempt to refine the type of a variable by assuming it is defined
 * only once. If the analysis discovers another assignment then quit trying and return the base
 * type. Otherwise, if the variable is found in an is-statement to have a more specific type than
 * the base type, return the more specific type. If the variable is declared final or const but its
 * type is dynamic then return the type of the initialization value.
 * <p>
 * Loops are assumed to contain an assignment to the variable being analyzed. This is clearly
 * sub-optimal, but proper analysis requires conversion to SSA form or computation of the dominator
 * tree, neither of which is readily available.
 * <p>
 * Analysis begins at the identifier whose type is to be refined and walks up the AST, ending at the
 * declaration of the element containing the identifier reference.
 */
public class TypeRefiner extends ASTVisitor<Void> {

  private static class AssignmentFinder extends ASTVisitor<Void> {
    private DartExpression target;
    private Set<DartNode> visitedNodes = new HashSet<DartNode>();
    private int assignmentCount = 0;

    AssignmentFinder(DartExpression target) {
      this.target = target;
    }

    @Override
    public Void visitBinaryExpression(DartBinaryExpression node) {
      if (!visitedNodes.contains(node)) {
        visitedNodes.add(node);
        Token operator = node.getOperator();
        if (operator.isAssignmentOperator()) {
          DartExpression lhs = node.getArg1();
          if (isAssignmentTo(lhs, target)) {
            assignmentCount += 1;
          }
        }
        node.visitChildren(this);
      }
      return null;
    }

    @Override
    public Void visitNode(DartNode node) {
      if (!visitedNodes.contains(node)) {
        visitedNodes.add(node);
        node.visitChildren(this);
      }
      return null;
    }

    @Override
    public Void visitUnaryExpression(DartUnaryExpression node) {
      if (!visitedNodes.contains(node)) {
        visitedNodes.add(node);
        Token operator = node.getOperator();
        if (operator.isCountOperator()) {
          // For the purposes of type refinement we can probably ignore count operators since they
          // do not change the type. Something to consider...
          DartExpression lhs = node.getArg();
          if (isAssignmentTo(lhs, target)) {
            assignmentCount += 1;
          }
        }
        node.visitChildren(this);
      }
      return null;
    }

    void findAssignmentsFrom(DartNode root) {
      if (visitedNodes.contains(root)) {
        return;
      }
      root.accept(this);
    }

    int getAssignmentCount() {
      return assignmentCount;
    }

    private boolean isAssignmentTo(DartExpression lhs, DartExpression ident) {
      return isSameElement(lhs.getElement(), ident.getElement());
    }
  }

  /**
   * Attempt to refine the give <code>type</code> of the node <code>ident</code>.
   * 
   * @param ident node whose type is to be refined
   * @param type initial type of ident
   * @return <code>type</code> or a more specific instance of Type, if found
   */
  public static Type refineType(DartIdentifier ident, Type type, CoreTypeProvider typeProvider) {
    return new TypeRefiner(ident, type, typeProvider).analyze();
  }

  private static boolean isSameElement(Element e1, Element e2) {
    if (e1 != null) {
      return e1.equals(e2);
    }
    return e1 == e2;
  }

  private DartIdentifier ident;
  private Type type;
  private Type refinedType;
  private DartNode immediateChild;
  private AssignmentFinder assignmentFinder;

  private Types types;

  private TypeRefiner(DartIdentifier ident, Type type, CoreTypeProvider typeProvider) {
    this.ident = ident;
    this.type = type;
    this.refinedType = type;
    this.assignmentFinder = new AssignmentFinder(ident);
    this.types = Types.getInstance(typeProvider);
  }

  @Override
  public Void visitBlock(DartBlock node) {
    if (!visitStatements(node.getStatements())) {
      return bailout();
    }
    return visitStatement(node);
  }

  @Override
  public Void visitDeclaration(DartDeclaration<?> node) {
    // end of analysis
    return null;
  }

  @Override
  public Void visitDoWhileStatement(DartDoWhileStatement node) {
    return bailout();
  }

  @Override
  public Void visitForInStatement(DartForInStatement node) {
    if (immediateChild == node.getIterable() || immediateChild == node.getVariableStatement()) {
      return visitStatement(node);
    }
    return bailout();
  }

  @Override
  public Void visitForStatement(DartForStatement node) {
    if (immediateChild == node.getInit()) {
      return visitStatement(node);
    }
    return bailout();
  }

  @Override
  public Void visitIfStatement(DartIfStatement node) {
    // Look for is-clause and refine type; complex booleans are not examined
    DartExpression expr = node.getCondition();
    if (expr instanceof DartBinaryExpression) {
      DartBinaryExpression bexp = (DartBinaryExpression) expr;
      DartExpression arg1 = bexp.getArg1();
      if (isSameElement(arg1.getElement(), ident.getElement())) {
        DartExpression arg2 = bexp.getArg2();
        if (arg2 instanceof DartTypeExpression) {
          DartTypeExpression texp = (DartTypeExpression) arg2;
          refinedType = mostSpecificType(refinedType, texp.getTypeNode().getType());
          return null;
        }
      }
    }
    return visitStatement(node);
  }

  @Override
  public Void visitNode(DartNode node) {
    immediateChild = node;
    if (node.getParent() != null) {
      node.getParent().accept(this);
    }
    return null;
  }

  @Override
  public Void visitVariable(DartVariable node) {
    // analysis continues with parent node, unlike other declarations
    return visitNode(node);
  }

  @Override
  public Void visitVariableStatement(DartVariableStatement node) {
    DartNode child = immediateChild;
    // Check each decl; if matches target then refine type
    for (DartVariable var : node.getVariables()) {
      if (var.getName().getName().equals(ident.getElement().getOriginalName())) {
        if (var.getValue() != null) {
          refinedType = var.getValue().getType();
        }
        return null;
      }
      if (var == child) {
        break;
      }
    }
    return super.visitVariableStatement(node);
  }

  @Override
  public Void visitWhileStatement(DartWhileStatement node) {
    if (immediateChild == node.getCondition()) {
      return visitStatement(node);
    }
    return bailout();
  }

  private Type analyze() {
    ident.accept(this);
    if (getAssignmentCountFrom(ident) > 1) {
      return type;
    }
    return refinedType;
  }

  private Void bailout() {
    refinedType = type;
    return null;
  }

  private int getAssignmentCountFrom(DartNode root) {
    assignmentFinder.findAssignmentsFrom(root);
    // gets count of all assignments to ident, not just those from root
    return assignmentFinder.getAssignmentCount();
  }

  private Type mostSpecificType(Type a, Type b) {
    if (a.getKind().equals(TypeKind.DYNAMIC)) {
      return b;
    }
    if (types.isSubtype(a, b)) {
      return a;
    }
    if (types.isSubtype(b, a)) {
      return b;
    }
    return b;
  }

  private boolean visitStatements(List<DartStatement> statements) {
    if (statements == null) {
      return true;
    }
    DartNode child = immediateChild;
    for (DartStatement stmt : statements) {
      if (stmt == child) {
        break;
      }
      if (getAssignmentCountFrom(stmt) > 0) {
        return false;
      }
      stmt.accept(this);
    }
    return true;
  }
}
