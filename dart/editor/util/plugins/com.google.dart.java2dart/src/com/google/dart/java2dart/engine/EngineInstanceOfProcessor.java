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

import com.google.common.collect.Maps;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionFunctionBody;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NullLiteral;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.processor.SemanticProcessor;

import static com.google.dart.java2dart.util.AstFactory.expressionFunctionBody;
import static com.google.dart.java2dart.util.AstFactory.formalParameterList;
import static com.google.dart.java2dart.util.AstFactory.functionExpression;
import static com.google.dart.java2dart.util.AstFactory.functionExpressionInvocation;
import static com.google.dart.java2dart.util.AstFactory.identifier;
import static com.google.dart.java2dart.util.AstFactory.isExpression;
import static com.google.dart.java2dart.util.AstFactory.simpleFormalParameter;
import static com.google.dart.java2dart.util.AstFactory.typeName;
import static com.google.dart.java2dart.util.TokenFactory.token;

import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import java.util.List;
import java.util.Map;

/**
 * Replace <code>isInstanceOf(Object, Type)</code> invocations with a predicate invocation.
 */
public class EngineInstanceOfProcessor extends SemanticProcessor {
  public EngineInstanceOfProcessor(Context context) {
    super(context);
  }

  @Override
  public void process(CompilationUnit unit) {
    unit.accept(new RecursiveAstVisitor<Void>() {
      String replace_isInstanceOf_arg = null;
      Map<String, String> replaceIdentifiersMap = Maps.newHashMap();

      @Override
      public Void visitMethodDeclaration(MethodDeclaration node) {
        replaceIdentifiersMap.clear();
        FormalParameterList parameterList = node.getParameters();
        if (parameterList != null) {
          List<FormalParameter> parameters = parameterList.getParameters();
          if (isMethodInClass(node, "getAncestor", "com.google.dart.engine.ast.AstNode")
              || isMethodInClass(
                  node,
                  "getNode",
                  "com.google.dart.engine.internal.element.ElementImpl")
              && parameters.size() == 1
              || isMethodInClass(
                  node,
                  "getAncestor",
                  "com.google.dart.engine.internal.element.ElementImpl")
              || isMethodInClass(node, "getAncestor", "com.google.dart.engine.element.Element")) {
            parameterList.getParameters().set(
                0,
                simpleFormalParameter(typeName("Predicate", typeName("AstNode")), "predicate"));
            replace_isInstanceOf_arg = "node";
          }
          if (isMethodInClass(node, "getAncestor", "com.google.dart.engine.element.Element")) {
            parameterList.getParameters().set(
                0,
                simpleFormalParameter(typeName("Predicate", typeName("Element")), "predicate"));
            replace_isInstanceOf_arg = "ancestor";
          }
          if (isMethodInClass(node, "assertInstanceOf", "com.google.dart.engine.EngineTestCase")) {
            parameterList.getParameters().add(
                0,
                simpleFormalParameter(typeName("Predicate", typeName("Object")), "predicate"));
            replace_isInstanceOf_arg = "object";
          }
          if (isMethodInClass(
              node,
              "assertLocate",
              "com.google.dart.engine.ast.visitor.NodeLocatorTest") && parameters.size() == 4) {
            parameterList.getParameters().add(
                3,
                simpleFormalParameter(typeName("Predicate", typeName("AstNode")), "predicate"));
            replace_isInstanceOf_arg = "node";
          }
          if (isMethodInClass(node, "findNode", "com.google.dart.engine.EngineTestCase")
              || isMethodInClass(
                  node,
                  "assertLocate",
                  "com.google.dart.engine.ast.visitor.NodeLocatorTest") && parameters.size() == 4) {
            parameterList.getParameters().set(
                3,
                simpleFormalParameter(typeName("Predicate", typeName("AstNode")), "predicate"));
            replace_isInstanceOf_arg = "node";
          }
          if (isMethodInClass(
              node,
              "checkResolved",
              "com.google.dart.engine.internal.resolver.ResolutionVerifier")) {
            parameterList.getParameters().set(
                2,
                simpleFormalParameter(typeName("Predicate", typeName("Element")), "predicate"));
            replace_isInstanceOf_arg = "element";
            replaceIdentifiersMap.put("expectedClass", "predicate");
          }
        }
        try {
          return super.visitMethodDeclaration(node);
        } finally {
          replace_isInstanceOf_arg = null;
          replaceIdentifiersMap.clear();
        }
      }

      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        String name = node.getMethodName().getName();
        ArgumentList argumentList = node.getArgumentList();
        List<Expression> arguments = argumentList.getArguments();
        if (replace_isInstanceOf_arg != null && name.equals("isInstanceOf")) {
          replaceNode(
              node,
              functionExpressionInvocation(
                  identifier("predicate"),
                  identifier(replace_isInstanceOf_arg)));
        }
        int typeLiteralIndex = -1;
        String predicateArgumentName = "_";
        if (isMethodInClass(node, "getAncestor", "com.google.dart.engine.ast.AstNode")
            || isMethodInClass(
                node,
                "getNode",
                "com.google.dart.engine.internal.element.ElementImpl") && arguments.size() == 1) {
          typeLiteralIndex = 0;
          predicateArgumentName = "node";
        }
        if (isMethodInClass(
            node,
            "getAncestor",
            "com.google.dart.engine.internal.element.ElementImpl")
            || isMethodInClass(node, "getAncestor", "com.google.dart.engine.element.Element")) {
          typeLiteralIndex = 0;
          predicateArgumentName = "element";
        }
        if (isMethodInClass(node, "assertInstanceOf", "com.google.dart.engine.EngineTestCase")) {
          arguments.add(0, arguments.get(0));
          typeLiteralIndex = 0;
          predicateArgumentName = "obj";
        }
        if (isMethodInClass(
            node,
            "assertLocate",
            "com.google.dart.engine.ast.visitor.NodeLocatorTest") && arguments.size() == 4) {
          arguments.add(3, arguments.get(3));
          typeLiteralIndex = 3;
          predicateArgumentName = "node";
        }
        if (isMethodInClass(node, "findNode", "com.google.dart.engine.EngineTestCase")) {
          typeLiteralIndex = 3;
          predicateArgumentName = "node";
        }
        if (isMethodInClass(
            node,
            "checkResolved",
            "com.google.dart.engine.internal.resolver.ResolutionVerifier")) {
          typeLiteralIndex = 2;
          predicateArgumentName = "node";
        }
        if (typeLiteralIndex != -1) {
          Expression argument = arguments.get(typeLiteralIndex);
          IBinding argumentBinding = context.getNodeBinding(argument);
          if (argument instanceof NullLiteral) {
          } else if (argumentBinding instanceof IVariableBinding) {
            replaceNode(argumentList, argument, identifier("predicate"));
          } else {
            SimpleIdentifier typeLiteralName = (SimpleIdentifier) argument;
            ExpressionFunctionBody body = expressionFunctionBody(isExpression(
                identifier(predicateArgumentName),
                false,
                typeName(typeLiteralName)));
            body.setSemicolon(null);
            replaceNode(
                argumentList,
                argument,
                functionExpression(
                    formalParameterList(simpleFormalParameter(predicateArgumentName)),
                    body));
          }
        }
        return super.visitMethodInvocation(node);
      }

      @Override
      public Void visitSimpleIdentifier(SimpleIdentifier node) {
        String name = node.getName();
        String newName = replaceIdentifiersMap.get(name);
        if (newName != null) {
          node.setToken(token(newName));
        }
        return super.visitSimpleIdentifier(node);
      }
    });
  }
}
