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

package com.google.dart.java2dart.engine;

import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.ThrowExpression;
import com.google.dart.engine.ast.TypeArgumentList;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.processor.SemanticProcessor;
import com.google.dart.java2dart.util.JavaUtils;

import static com.google.dart.java2dart.util.AstFactory.identifier;
import static com.google.dart.java2dart.util.AstFactory.instanceCreationExpression;
import static com.google.dart.java2dart.util.AstFactory.nullLiteral;
import static com.google.dart.java2dart.util.AstFactory.string;
import static com.google.dart.java2dart.util.AstFactory.typeName;

import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.List;

/**
 * Rewrites {@link AnalysisException} creation to gather stack traces.
 */
public class EngineExceptionProcessor extends SemanticProcessor {
  public EngineExceptionProcessor(Context context) {
    super(context);
  }

  @Override
  public void process(CompilationUnit unit) {
    unit.accept(new RecursiveAstVisitor<Void>() {
      @Override
      public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
        List<Expression> arguments = node.getArgumentList().getArguments();
        Identifier typeName = node.getConstructorName().getType().getName();
        if (typeName.getName().equals("AnalysisException")) {
          // "log(new AnalysisException(m))" -> "log(new CaughtException(new AnalysisException(m)))"
          if (arguments.size() == 1) {
            AstNode parent = node.getParent();
            if (!(parent instanceof ThrowExpression)) {
              replaceNode(
                  parent,
                  node,
                  instanceCreationExpression(
                      Keyword.NEW,
                      typeName("CaughtException"),
                      node,
                      nullLiteral()));
            }
            return null;
          }
          // "new AnalysisException(m, e)" -> "new AnalysisException(m, new CaughtException(e, stack))"
          if (arguments.size() == 2) {
            Expression stackTraceNode = nullLiteral();
            {
              Block block = node.getAncestor(Block.class);
              if (block.getParent() instanceof CatchClause) {
                CatchClause catchClause = (CatchClause) block.getParent();
                SimpleIdentifier stackTraceIdentifier = identifier("stackTrace");
                catchClause.setStackTraceParameter(stackTraceIdentifier);
                stackTraceNode = stackTraceIdentifier;
              }
            }
            arguments.set(
                1,
                instanceCreationExpression(
                    Keyword.NEW,
                    typeName("CaughtException"),
                    arguments.get(1),
                    stackTraceNode));
          }
        }
        return super.visitInstanceCreationExpression(node);
      }

      @Override
      public Void visitSimpleIdentifier(SimpleIdentifier node) {
        ITypeBinding typeBinding = context.getNodeTypeBinding(node);
        if (JavaUtils.isTypeNamed(typeBinding, "com.google.dart.engine.context.AnalysisException")) {
          AstNode parent = node.getParent();
          if (parent instanceof ArgumentList || parent instanceof AssignmentExpression
              && ((AssignmentExpression) parent).getRightHandSide() == node) {
            Block block = node.getAncestor(Block.class);
            if (block != null && block.getParent() instanceof CatchClause) {
              CatchClause catchClause = (CatchClause) block.getParent();
              SimpleIdentifier stackTraceIdentifier = identifier("stackTrace");
              catchClause.setStackTraceParameter(stackTraceIdentifier);
              replaceNode(
                  parent,
                  node,
                  instanceCreationExpression(
                      Keyword.NEW,
                      typeName("CaughtException"),
                      node,
                      stackTraceIdentifier));
            }
          }
        }
        return super.visitSimpleIdentifier(node);
      }

      @Override
      public Void visitThrowExpression(ThrowExpression node) {
        super.visitThrowExpression(node);
        // "throw thrownException;" in AnalysisContextImpl -> "throw new AnalysisContext(thrownException)"
        Expression expression = node.getExpression();
        if (expression instanceof SimpleIdentifier
            && ((SimpleIdentifier) expression).getName().equals("thrownException")) {
          node.setExpression(instanceCreationExpression(
              Keyword.NEW,
              typeName("AnalysisException"),
              string("<rethrow>"),
              expression));
        }
        return null;
      }

      @Override
      public Void visitTypeName(TypeName node) {
        // AnalysisException -> CaughtException
        AstNode parent = node.getParent();
        if (parent instanceof VariableDeclarationList || parent instanceof MethodDeclaration
            || parent instanceof SimpleFormalParameter || parent instanceof TypeArgumentList) {
          if (node.getName() instanceof SimpleIdentifier) {
            SimpleIdentifier nameNode = (SimpleIdentifier) node.getName();
            if (nameNode.getName().equals("AnalysisException")) {
              replaceNode(nameNode, identifier("CaughtException"));
            }
          }
        }
        // done
        return super.visitTypeName(node);
      }
    });
  }
}
