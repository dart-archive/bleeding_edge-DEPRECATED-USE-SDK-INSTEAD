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

package com.google.dart.java2dart.engine;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorInitializer;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NormalFormalParameter;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.processor.SemanticProcessor;
import com.google.dart.java2dart.util.JavaUtils;

import static com.google.dart.java2dart.util.ASTFactory.binaryExpression;
import static com.google.dart.java2dart.util.ASTFactory.blockFunctionBody;
import static com.google.dart.java2dart.util.ASTFactory.constructorDeclaration;
import static com.google.dart.java2dart.util.ASTFactory.expressionStatement;
import static com.google.dart.java2dart.util.ASTFactory.formalParameterList;
import static com.google.dart.java2dart.util.ASTFactory.functionDeclaration;
import static com.google.dart.java2dart.util.ASTFactory.functionExpression;
import static com.google.dart.java2dart.util.ASTFactory.identifier;
import static com.google.dart.java2dart.util.ASTFactory.methodInvocation;
import static com.google.dart.java2dart.util.ASTFactory.namedFormalParameter;
import static com.google.dart.java2dart.util.ASTFactory.redirectingConstructorInvocation;
import static com.google.dart.java2dart.util.ASTFactory.typeName;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * {@link SemanticProcessor} for Engine.
 */
public class EngineSemanticProcessor extends SemanticProcessor {
  public static final SemanticProcessor INSTANCE = new EngineSemanticProcessor();

  /**
   * Adds "main" function with given {@link Statement}s.
   */
  public static void addMain(CompilationUnit unit, List<Statement> statements) {
    unit.getDeclarations().add(
        functionDeclaration(
            null,
            null,
            "main",
            functionExpression(formalParameterList(), blockFunctionBody(statements))));
  }

  /**
   * Gather all <code>TestSuite.addTestSuite</code> into "main" function.
   */
  public static boolean gatherTestSuites(final List<Statement> mainStatements,
      CompilationUnitMember node) {
    if (node instanceof ClassDeclaration) {
      ClassDeclaration classDeclaration = (ClassDeclaration) node;
      if (classDeclaration.getName().getName().equals("TestAll")) {
        node.accept(new RecursiveASTVisitor<Void>() {
          @Override
          public Void visitMethodInvocation(MethodInvocation node) {
            if (node.getMethodName().getName().equals("addTestSuite")) {
              mainStatements.add(expressionStatement(methodInvocation(
                  node.getArgumentList().getArguments().get(0),
                  "dartSuite")));
            }
            return super.visitMethodInvocation(node);
          }
        });
        return true;
      }
    }
    return false;
  }

  /**
   * Update {@link ASTNode} translated classes to add constructors with all named parameters and
   * rename "full" constructors with all required positional parameters.
   */
  public static void generateConstructorWithNamedParametersInAST(final Context context,
      CompilationUnit unit) {
    unit.accept(new RecursiveASTVisitor<Void>() {
      @Override
      public Void visitClassDeclaration(ClassDeclaration node) {
        ITypeBinding binding = context.getNodeTypeBinding(node);
        if (JavaUtils.isSubtype(binding, "com.google.dart.engine.ast.ASTNode")) {
          List<ClassMember> members = node.getMembers();
          for (int i = 0; i < members.size(); i++) {
            ClassMember member = members.get(i);
            if (member instanceof ConstructorDeclaration) {
              ConstructorDeclaration constructor = (ConstructorDeclaration) member;
              // prepare names, rename "full"
              String optionalName;
              String fullName;
              if (constructor.getName() == null) {
                fullName = "full";
                context.renameConstructor(constructor, fullName);
                optionalName = null;
              } else {
                optionalName = constructor.getName().getName();
                fullName = optionalName + "_full";
                context.renameConstructor(constructor, fullName);
              }
              // add constructor with optional names parameters
              List<FormalParameter> namedParameters = Lists.newArrayList();
              List<Expression> fullConstructorArguments = Lists.newArrayList();
              for (FormalParameter parameter : constructor.getParameters().getParameters()) {
                NormalFormalParameter normalParameter = (NormalFormalParameter) parameter;
                namedParameters.add(namedFormalParameter(normalParameter, null));
                fullConstructorArguments.add(normalParameter.getIdentifier());
              }
              ConstructorInitializer fullConstructorInvocation = redirectingConstructorInvocation(
                  fullName,
                  fullConstructorArguments);
              members.add(
                  ++i,
                  constructorDeclaration(
                      constructor.getDocumentationComment(),
                      constructor.getReturnType(),
                      identifier(optionalName),
                      formalParameterList(namedParameters),
                      Lists.newArrayList(fullConstructorInvocation),
                      null));
            }
          }
        }
        return null;
      }
    });
  }

  /**
   * Generates "invokeParserMethodImpl" and supporting Dart code.
   */
  public static void replaceReflectionMethods(final Context context, final PrintWriter pw,
      ASTNode node) {
    node.accept(new RecursiveASTVisitor<Void>() {
      @Override
      public Void visitClassDeclaration(ClassDeclaration node) {
        List<ClassMember> members = Lists.newArrayList(node.getMembers());
        // visit s usually
        for (ClassMember classMember : members) {
          classMember.accept(this);
        }
        // use Parser, fill method lookup table
        if (node.getName().getName().equals("Parser")) {
          Set<String> usedMethodSignatures = Sets.newHashSet();
          pw.println();
          pw.print("Map<String, MethodTrampoline> _methodTable_Parser = <String, MethodTrampoline> {");
          for (ClassMember classMember : members) {
            Object binding = context.getNodeBinding(classMember);
            if (classMember instanceof MethodDeclaration && binding instanceof IMethodBinding) {
              MethodDeclaration method = (MethodDeclaration) classMember;
              IMethodBinding methodBinding = (IMethodBinding) binding;
              if (method.getPropertyKeyword() == null) {
                int parameterCount = methodBinding.getParameterTypes().length;
                String methodSignature = methodBinding.getName() + "_" + parameterCount;
                // don't add method more than once and hope that it is never called
                if (usedMethodSignatures.contains(methodSignature)) {
                  continue;
                } else {
                  usedMethodSignatures.add(methodSignature);
                }
                // generate map entry to method
                pw.println();
                pw.print("  '");
                pw.print(methodSignature);
                pw.print("': ");
                pw.print("new MethodTrampoline(");
                pw.print(parameterCount);
                pw.print(", (Parser target");
                for (int i = 0; i < parameterCount; i++) {
                  pw.print(", arg");
                  pw.print(i);
                }
                pw.print(") => target.");
                pw.print(method.getName().getName());
                pw.print("(");
                for (int i = 0; i < parameterCount; i++) {
                  if (i != 0) {
                    pw.print(", ");
                  }
                  pw.print("arg");
                  pw.print(i);
                }
                pw.print(")),");
              }
            }
          }
          pw.println("};");
          pw.println();
        }
        return null;
      }

      @Override
      public Void visitMethodDeclaration(MethodDeclaration node) {
        String name = node.getName().getName();
        if (name.equals("findParserMethod")) {
          removeMethod(node);
        }
        if (name.equals("invokeParserMethodImpl")) {
          removeMethod(node);
          String source = toSource(
              "Object invokeParserMethodImpl(Parser parser, String methodName, List<Object> objects, Token tokenStream) {",
              "  parser.currentToken = tokenStream;",
              "  MethodTrampoline method = _methodTable_Parser['${methodName}_${objects.length}'];",
              "  return method.invoke(parser, objects);",
              "}");
          pw.print("\n");
          pw.print(source);
          pw.print("\n");
        }
        return super.visitMethodDeclaration(node);
      }

      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        String name = node.getMethodName().getName();
        if (name.equals("invokeParserMethodImpl")) {
          node.setTarget(null);
          return null;
        }
        return super.visitMethodInvocation(node);
      }

      private void removeMethod(MethodDeclaration node) {
        ((ClassDeclaration) node.getParent()).getMembers().remove(node);
      }
    });
  }

  private static String toSource(String... lines) {
    return Joiner.on("\n").join(lines);
  }

  @Override
  public void process(final Context context, final CompilationUnit unit) {
    List<CompilationUnitMember> declarations = unit.getDeclarations();
    // remove NodeList, it is declared in enginelib.dart
    for (Iterator<CompilationUnitMember> iter = declarations.iterator(); iter.hasNext();) {
      CompilationUnitMember member = iter.next();
      if (member instanceof ClassDeclaration) {
        ClassDeclaration classDeclaration = (ClassDeclaration) member;
        String name = classDeclaration.getName().getName();
        if (name.equals("NodeList")) {
          iter.remove();
        }
      }
    }
    // process nodes
    unit.accept(new GeneralizingASTVisitor<Void>() {
      @Override
      public Void visitClassDeclaration(ClassDeclaration node) {
        ITypeBinding typeBinding = context.getNodeTypeBinding(node);
        // "Type" is declared in dart:core, so replace it
        if (JavaUtils.isTypeNamed(typeBinding, "com.google.dart.engine.type.Type")) {
          SimpleIdentifier nameNode = node.getName();
          context.renameIdentifier(nameNode, "Type2");
        }
        return super.visitClassDeclaration(node);
      }

      @Override
      public Void visitMethodDeclaration(MethodDeclaration node) {
        String name = node.getName().getName();
        if ("accept".equals(name) && node.getParameters().getParameters().size() == 1) {
          node.setReturnType(null);
          FormalParameter formalParameter = node.getParameters().getParameters().get(0);
          ((SimpleFormalParameter) formalParameter).getType().setTypeArguments(null);
        }
        return super.visitMethodDeclaration(node);
      }

      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        List<Expression> args = node.getArgumentList().getArguments();
        if (isMethodInClass(node, "toArray", "com.google.dart.engine.utilities.collection.IntList")) {
          replaceNode(node, node.getTarget());
          return null;
        }
        if (isMethodInClass(
            node,
            "equals",
            "com.google.dart.engine.utilities.general.ObjectUtilities")) {
          replaceNode(node, binaryExpression(args.get(0), TokenType.EQ_EQ, args.get(1)));
          return null;
        }
        return super.visitMethodInvocation(node);
      }

      @Override
      public Void visitTypeName(TypeName node) {
        if (node.getName() instanceof SimpleIdentifier) {
          SimpleIdentifier nameNode = (SimpleIdentifier) node.getName();
          String name = nameNode.getName();
          if ("IntList".equals(name)) {
            replaceNode(node, typeName("List", typeName("int")));
            return null;
          }
        }
        return super.visitTypeName(node);
      }

      private boolean isMethodInClass(MethodInvocation node, String reqName, String reqClassName) {
        String name = node.getMethodName().getName();
        return Objects.equal(name, reqName)
            && JavaUtils.isMethodInClass(context.getNodeBinding(node), reqClassName);
      }
    });
  }
}
