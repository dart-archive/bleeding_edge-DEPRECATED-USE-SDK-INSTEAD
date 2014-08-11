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

package com.google.dart.java2dart.processor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.util.JavaUtils;

import static com.google.dart.java2dart.util.AstFactory.blockFunctionBody;
import static com.google.dart.java2dart.util.AstFactory.expressionStatement;
import static com.google.dart.java2dart.util.AstFactory.extendsClause;
import static com.google.dart.java2dart.util.AstFactory.formalParameterList;
import static com.google.dart.java2dart.util.AstFactory.functionExpression;
import static com.google.dart.java2dart.util.AstFactory.identifier;
import static com.google.dart.java2dart.util.AstFactory.instanceCreationExpression;
import static com.google.dart.java2dart.util.AstFactory.methodDeclaration;
import static com.google.dart.java2dart.util.AstFactory.methodInvocation;
import static com.google.dart.java2dart.util.AstFactory.propertyAccess;
import static com.google.dart.java2dart.util.AstFactory.string;
import static com.google.dart.java2dart.util.AstFactory.typeName;
import static com.google.dart.java2dart.util.AstFactory.variableDeclaration;
import static com.google.dart.java2dart.util.AstFactory.variableDeclarationList;
import static com.google.dart.java2dart.util.AstFactory.variableDeclarationStatement;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;

import java.util.Collections;
import java.util.List;

/**
 * {@link SemanticProcessor} for Java <code>Object</code>.
 */
public class JUnitSemanticProcessor extends SemanticProcessor {
  /**
   * @return the name of all "test" methods in the hierarchy.
   */
  private static List<String> getTestMethodNames(org.eclipse.jdt.core.dom.ITypeBinding binding) {
    if (binding == null) {
      return ImmutableList.<String> of();
    }
    List<String> testMethods = Lists.newArrayList();
    for (org.eclipse.jdt.core.dom.IMethodBinding method : binding.getDeclaredMethods()) {
      String methodName = method.getName();
      if (Modifier.isPublic(method.getModifiers()) && methodName.startsWith("test")) {
        testMethods.add(methodName);
      }
    }
    testMethods.addAll(getTestMethodNames(binding.getSuperclass()));
    return testMethods;
  }

  public JUnitSemanticProcessor(Context context) {
    super(context);
  }

  @Override
  public void process(CompilationUnit unit) {
    unit.accept(new GeneralizingAstVisitor<Void>() {
      @Override
      public Void visitClassDeclaration(ClassDeclaration node) {
        ITypeBinding typeBinding = context.getNodeTypeBinding(node);
        if (JavaUtils.isSubtype(typeBinding, "junit.framework.TestCase")) {
          // replace extends clause
          if (JavaUtils.isTypeNamed(typeBinding.getSuperclass(), "junit.framework.TestCase")) {
            node.setExtendsClause(extendsClause(typeName("JUnitTestCase")));
          }
          // generate "dartSuite"
          if (!node.isAbstract()) {
            generateDartSuite(node);
          }
        }
        return super.visitClassDeclaration(node);
      }

      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        super.visitMethodInvocation(node);
        SimpleIdentifier nameNode = node.getMethodName();
        String name = nameNode.getName();
        List<Expression> args = node.getArgumentList().getArguments();
        if (isJUnitAssertMethod(node)) {
          IMethodBinding binding = (IMethodBinding) context.getNodeBinding(node);
          if (JavaUtils.isTypeNamed(binding.getDeclaringClass(), "junit.framework.Assert")) {
            node.setTarget(identifier("JUnitTestCase"));
          }
          if (isMethodInClass2(
              node,
              "assertTrue(java.lang.String,boolean)",
              "junit.framework.Assert")) {
            node.setMethodName(identifier("assertTrueMsg"));
          }
          if (isMethodInClass2(
              node,
              "assertFalse(java.lang.String,boolean)",
              "junit.framework.Assert")) {
            node.setMethodName(identifier("assertFalseMsg"));
          }
          if (isMethodInClass2(
              node,
              "assertNull(java.lang.String,java.lang.Object)",
              "junit.framework.Assert")) {
            node.setMethodName(identifier("assertNullMsg"));
          }
          if (isMethodInClass2(
              node,
              "assertNotNull(java.lang.String,java.lang.Object)",
              "junit.framework.Assert")) {
            node.setMethodName(identifier("assertNotNullMsg"));
          }
          if (name.equals("assertEquals")) {
            if (args.size() == 3) {
              node.setMethodName(identifier("assertEqualsMsg"));
            }
          }
          if (name.equals("assertSame")) {
            if (args.size() == 3) {
              node.setMethodName(identifier("assertSameMsg"));
            }
          }
        }
        return null;
      }

      private void generateDartSuite(ClassDeclaration node) {
        ITypeBinding typeBinding = context.getNodeTypeBinding(node);
        TypeName suiteTypeName = typeName(node.getName());
        SimpleIdentifier testInstanceIdentifier = identifier("__test");
        Statement testStatementInstance = variableDeclarationStatement(variableDeclarationList(
            Keyword.FINAL,
            (TypeName) null,
            variableDeclaration(
                testInstanceIdentifier,
                instanceCreationExpression(Keyword.NEW, suiteTypeName))));
        // prepare "test" invocations
        List<Statement> testInvocationStatements = Lists.newArrayList();
        {
          List<String> testMethodNames = getTestMethodNames(typeBinding);
          Collections.sort(testMethodNames);
          for (String methodName : testMethodNames) {
            List<Statement> testStatements = Lists.newArrayList();
            testStatements.add(testStatementInstance);
            testStatements.add(expressionStatement(methodInvocation(
                "runJUnitTest",
                testInstanceIdentifier,
                propertyAccess(testInstanceIdentifier, methodName))));
            MethodInvocation testInvocation = methodInvocation(
                "_ut.test",
                string(methodName),
                functionExpression(formalParameterList(), blockFunctionBody(testStatements)));
            testInvocationStatements.add(expressionStatement(testInvocation));
          }
        }
        // add "dartSuite" method
        MethodInvocation groupInvocation = methodInvocation(
            "_ut.group",
            string(node.getName().getName()),
            functionExpression(formalParameterList(), blockFunctionBody(testInvocationStatements)));
        node.getMembers().add(
            methodDeclaration(
                null,
                true,
                null,
                identifier("dartSuite"),
                formalParameterList(),
                blockFunctionBody(expressionStatement(groupInvocation))));
      }

      private boolean isJUnitAssertMethod(MethodInvocation node) {
        return JavaUtils.isMethodInClass(context.getNodeBinding(node), "junit.framework.Assert");
      }
    });
  }
}
