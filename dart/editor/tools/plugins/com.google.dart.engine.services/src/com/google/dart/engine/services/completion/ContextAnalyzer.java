package com.google.dart.engine.services.completion;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.ArgumentDefinitionTest;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.Declaration;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.DoStatement;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ForEachStatement;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SwitchStatement;
import com.google.dart.engine.ast.TypeArgumentList;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.WhileStatement;
import com.google.dart.engine.ast.WithClause;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;

/**
 * @coverage com.google.dart.engine.services.completion
 */
class ContextAnalyzer extends GeneralizingASTVisitor<Void> {
  CompletionState state;
  ASTNode completionNode;
  ASTNode child;
  boolean inTypeName;
  boolean inIdentifier;
  boolean inExpression;

  ContextAnalyzer(CompletionState state, ASTNode completionNode) {
    this.state = state;
    this.completionNode = completionNode;
  }

  @Override
  public Void visitArgumentDefinitionTest(ArgumentDefinitionTest node) {
    state.requiresOptionalArgument();
    return super.visitArgumentDefinitionTest(node);
  }

  @Override
  public Void visitCatchClause(CatchClause node) {
    if (node.getExceptionType() == child) {
      state.prohibitsLiterals();
    }
    return null;
  }

  @Override
  public Void visitDirective(Directive node) {
    state.prohibitsLiterals();
    return super.visitDirective(node);
  }

  @Override
  public Void visitDoStatement(DoStatement node) {
    if (child == node.getCondition()) {
      state.includesLiterals();
    }
    return super.visitDoStatement(node);
  }

  @Override
  public Void visitExpression(Expression node) {
    inExpression = true;
    state.includesLiterals();
    return super.visitExpression(node);
  }

  @Override
  public Void visitForEachStatement(ForEachStatement node) {
    if (child == node.getIterator()) {
      state.includesLiterals();
    }
    return super.visitForEachStatement(node);
  }

  @Override
  public Void visitFunctionExpression(FunctionExpression node) {
    if (node.getParent() instanceof Declaration) {
      // Function expressions that are part of a declaration are not to be treated as expressions.
      return visitNode(node);
    } else {
      return visitExpression(node);
    }
  }

  @Override
  public Void visitFunctionTypeAlias(FunctionTypeAlias node) {
    if (inTypeName || node.getReturnType() == null) {
      // This may be an incomplete class type alias
      state.includesUndefinedDeclarationTypes();
    }
    return super.visitFunctionTypeAlias(node);
  }

  @Override
  public Void visitIdentifier(Identifier node) {
    // Identifiers cannot safely be generalized to expressions, so just walk up one level.
    // LibraryIdentifier is never an expression. PrefixedIdentifier may be an expression, but
    // not in a catch-clause or a declaration. SimpleIdentifier may be an expression, but not
    // in a constructor name, label, or where PrefixedIdentifier is not.
    return visitNode(node);
  }

  @Override
  public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
    state.requiresConst(node.isConst());
    state.mustBeInstantiableType();
    return super.visitInstanceCreationExpression(node);
  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    state.sourceDeclarationIsStatic(node.isStatic());
    if (child == node.getReturnType()) {
      state.includesUndefinedDeclarationTypes();
    }
    return super.visitMethodDeclaration(node);
  }

  @Override
  public Void visitNode(ASTNode node) {
    // Walk UP the tree, not down.
    ASTNode parent = node.getParent();
    if (parent != null) {
      child = node;
      parent.accept(this);
    }
    return null;
  }

  @Override
  public Void visitPrefixedIdentifier(PrefixedIdentifier node) {
    if (node == completionNode || node.getIdentifier() == completionNode) {
      Element element = node.getPrefix().getElement();
      if (!(element instanceof ClassElement)) {
        state.prohibitsStaticReferences();
      } else {
        state.prohibitsInstanceReferences();
      }
    }
    return super.visitPrefixedIdentifier(node);
  }

  @Override
  public Void visitSimpleFormalParameter(SimpleFormalParameter node) {
    state.includesUndefinedTypes();
    return super.visitSimpleFormalParameter(node);
  }

  @Override
  public Void visitSimpleIdentifier(SimpleIdentifier node) {
    inIdentifier = true;
    return super.visitSimpleIdentifier(node);
  }

  @Override
  public Void visitSwitchStatement(SwitchStatement node) {
    if (child == node.getExpression()) {
      state.includesLiterals();
    }
    return super.visitSwitchStatement(node);
  }

  @Override
  public Void visitTypeArgumentList(TypeArgumentList node) {
    state.prohibitsUndefinedTypes();
    return super.visitTypeArgumentList(node);
  }

  @Override
  public Void visitTypeName(TypeName node) {
    inTypeName = true;
    return super.visitTypeName(node);
  }

  @Override
  public Void visitVariableDeclaration(VariableDeclaration node) {
    if (node.getName() == completionNode) {
      state.prohibitsLiterals();
    }
    return super.visitVariableDeclaration(node);
  }

  @Override
  public Void visitVariableDeclarationList(VariableDeclarationList node) {
    state.includesUndefinedDeclarationTypes();
    return super.visitVariableDeclarationList(node);
  }

  @Override
  public Void visitWhileStatement(WhileStatement node) {
    if (child == node.getCondition()) {
      state.includesLiterals();
    }
    return super.visitWhileStatement(node);
  }

  @Override
  public Void visitWithClause(WithClause node) {
    state.mustBeMixin();
    return super.visitWithClause(node);
  }
}
