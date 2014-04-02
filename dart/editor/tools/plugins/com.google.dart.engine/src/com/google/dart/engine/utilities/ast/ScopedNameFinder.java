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
package com.google.dart.engine.utilities.ast;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.Declaration;
import com.google.dart.engine.ast.DeclaredIdentifier;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.ForEachStatement;
import com.google.dart.engine.ast.ForStatement;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionDeclarationStatement;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.SwitchMember;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.TypeAlias;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationStatement;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Traverse the AST from initial child node to successive parents, building a collection of local
 * variable and parameter names visible to the initial child node. In case of name shadowing, the
 * first name seen is the most specific one so names are not redefined.
 * <p>
 * Completion test code coverage is 95%. The two basic blocks that are not executed cannot be
 * executed. They are included for future reference.
 * 
 * @coverage com.google.dart.engine.services.completion
 */
public class ScopedNameFinder extends GeneralizingAstVisitor<Void> {

  private Declaration declarationNode;
  private AstNode immediateChild;
  private Map<String, SimpleIdentifier> locals = new HashMap<String, SimpleIdentifier>();
  private int position;
  private boolean referenceIsWithinLocalFunction;

  public ScopedNameFinder(int position) {
    this.position = position;
  }

  public Declaration getDeclaration() {
    return declarationNode;
  }

  public Map<String, SimpleIdentifier> getLocals() {
    return locals;
  }

  @Override
  public Void visitBlock(Block node) {
    checkStatements(node.getStatements());
    return super.visitBlock(node);
  }

  @Override
  public Void visitCatchClause(CatchClause node) {
    addToScope(node.getExceptionParameter());
    addToScope(node.getStackTraceParameter());
    return super.visitCatchClause(node);
  }

  @Override
  public Void visitConstructorDeclaration(ConstructorDeclaration node) {
    if (immediateChild != node.getParameters()) {
      addParameters(node.getParameters().getParameters());
    }
    declarationNode = node;
    return null;
  }

  @Override
  public Void visitFieldDeclaration(FieldDeclaration node) {
    declarationNode = node;
    return null;
  }

  @Override
  public Void visitForEachStatement(ForEachStatement node) {
    DeclaredIdentifier loopVariable = node.getLoopVariable();
    if (loopVariable != null) {
      addToScope(loopVariable.getIdentifier());
    }
    return super.visitForEachStatement(node);
  }

  @Override
  public Void visitForStatement(ForStatement node) {
    if (immediateChild != node.getVariables() && node.getVariables() != null) {
      addVariables(node.getVariables().getVariables());
    }
    return super.visitForStatement(node);
  }

  @Override
  public Void visitFunctionDeclaration(FunctionDeclaration node) {
    if (!(node.getParent() instanceof FunctionDeclarationStatement)) {
      declarationNode = node;
      return null;
    }
    return super.visitFunctionDeclaration(node);
  }

  @Override
  public Void visitFunctionDeclarationStatement(FunctionDeclarationStatement node) {
    referenceIsWithinLocalFunction = true;
    return super.visitFunctionDeclarationStatement(node);
  }

  @Override
  public Void visitFunctionExpression(FunctionExpression node) {
    if (node.getParameters() != null && immediateChild != node.getParameters()) {
      addParameters(node.getParameters().getParameters());
    }
    return super.visitFunctionExpression(node);
  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    declarationNode = node;
    if (node.getParameters() == null) {
      return null;
    }
    if (immediateChild != node.getParameters()) {
      addParameters(node.getParameters().getParameters());
    }
    return null;
  }

  @Override
  public Void visitNode(AstNode node) {
    immediateChild = node;
    AstNode parent = node.getParent();
    if (parent != null) {
      parent.accept(this);
    }
    return null;
  }

  @Override
  public Void visitSwitchMember(SwitchMember node) {
    checkStatements(node.getStatements());
    return super.visitSwitchMember(node);
  }

  @Override
  public Void visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
    declarationNode = node;
    return null;
  }

  @Override
  public Void visitTypeAlias(TypeAlias node) {
    declarationNode = node; // not reached
    return null;
  }

  private void addParameters(NodeList<FormalParameter> vars) {
    for (FormalParameter var : vars) {
      addToScope(var.getIdentifier());
    }
  }

  private void addToScope(SimpleIdentifier identifier) {
    if (identifier != null && isInRange(identifier)) {
      String name = identifier.getName();
      if (!locals.containsKey(name)) {
        locals.put(name, identifier);
      }
    }
  }

  private void addVariables(NodeList<VariableDeclaration> vars) {
    for (VariableDeclaration var : vars) {
      addToScope(var.getName());
    }
  }

  /**
   * Some statements define names that are visible downstream. There aren't many of these.
   * 
   * @param statements the list of statements to check for name definitions
   */
  private void checkStatements(List<Statement> statements) {
    for (Statement stmt : statements) {
      if (stmt == immediateChild) {
        return;
      }
      if (stmt instanceof VariableDeclarationStatement) {
        addVariables(((VariableDeclarationStatement) stmt).getVariables().getVariables());
      } else if (stmt instanceof FunctionDeclarationStatement && !referenceIsWithinLocalFunction) {
        addToScope(((FunctionDeclarationStatement) stmt).getFunctionDeclaration().getName());
      }
    }
  }

  private boolean isInRange(AstNode node) {
    if (position < 0) {
      // if source position is not set then all nodes are in range
      return true; // not reached
    }
    return node.getEnd() < position;
  }

}
