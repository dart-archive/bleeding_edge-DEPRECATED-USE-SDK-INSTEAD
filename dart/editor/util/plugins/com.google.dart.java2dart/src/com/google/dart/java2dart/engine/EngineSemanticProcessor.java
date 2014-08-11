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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionStatement;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.ListLiteral;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.VariableDeclarationStatement;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.processor.SemanticProcessor;
import com.google.dart.java2dart.util.JavaUtils;

import static com.google.dart.java2dart.util.AstFactory.assignmentExpression;
import static com.google.dart.java2dart.util.AstFactory.binaryExpression;
import static com.google.dart.java2dart.util.AstFactory.blockFunctionBody;
import static com.google.dart.java2dart.util.AstFactory.expressionFunctionBody;
import static com.google.dart.java2dart.util.AstFactory.expressionStatement;
import static com.google.dart.java2dart.util.AstFactory.fieldDeclaration;
import static com.google.dart.java2dart.util.AstFactory.formalParameterList;
import static com.google.dart.java2dart.util.AstFactory.functionDeclaration;
import static com.google.dart.java2dart.util.AstFactory.functionExpression;
import static com.google.dart.java2dart.util.AstFactory.identifier;
import static com.google.dart.java2dart.util.AstFactory.integer;
import static com.google.dart.java2dart.util.AstFactory.methodDeclaration;
import static com.google.dart.java2dart.util.AstFactory.methodInvocation;
import static com.google.dart.java2dart.util.AstFactory.parenthesizedExpression;
import static com.google.dart.java2dart.util.AstFactory.prefixExpression;
import static com.google.dart.java2dart.util.AstFactory.propertyAccess;
import static com.google.dart.java2dart.util.AstFactory.simpleFormalParameter;
import static com.google.dart.java2dart.util.AstFactory.typeName;
import static com.google.dart.java2dart.util.AstFactory.variableDeclaration;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link SemanticProcessor} for Engine.
 */
public class EngineSemanticProcessor extends SemanticProcessor {
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
        node.accept(new RecursiveAstVisitor<Void>() {
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
   * Generates "invokeParserMethodImpl" and supporting Dart code.
   */
  public static void replaceReflection_generateParserTable(final Context context,
      final PrintWriter pw, AstNode node) {
    node.accept(new RecursiveAstVisitor<Void>() {
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
          pw.print("Map<String, MethodTrampoline> methodTable_Parser = <String, MethodTrampoline> {");
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
        if (name.equals("_findParserMethod")) {
          removeMethod(node);
        }
        if (name.equals("invokeParserMethodImpl")) {
          removeMethod(node);
          String source = toSource(
              "Object invokeParserMethodImpl(Parser parser, String methodName, List<Object> objects, Token tokenStream) {",
              "  parser.currentToken = tokenStream;",
              "  MethodTrampoline method = methodTable_Parser['${methodName}_${objects.length}'];",
              "  return method.invoke(parser, objects);",
              "}");
          pw.print("\n");
          pw.print(source);
          pw.print("\n");
        }
        return super.visitMethodDeclaration(node);
      }

      private void removeMethod(MethodDeclaration node) {
        ((ClassDeclaration) node.getParent()).getMembers().remove(node);
      }
    });
  }

  /**
   * Rewrites "invokeParserMethodImpl" invocation.
   */
  public static void replaceReflection_invokeParserMethodImpl(AstNode node) {
    node.accept(new RecursiveAstVisitor<Void>() {

      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        String name = node.getMethodName().getName();
        if (name.equals("invokeParserMethodImpl")) {
          node.setTarget(null);
          return null;
        }
        return super.visitMethodInvocation(node);
      }
    });
  }

  /**
   * Find code like:
   * 
   * <pre>
     *   Field scopeField = visitor.getClass().getSuperclass().getDeclaredField("nameScope");
     *   scopeField.setAccessible(true);
     *   Scope outerScope = (Scope) scopeField.get(visitor);
     * </pre>
   * 
   * and replaces it with direct calling of generated public accessors.
   */
  static void rewriteReflectionFieldsWithDirect(final Context context, CompilationUnit unit) {
    final Map<String, String> varToField = Maps.newHashMap();
    final Map<String, String> varToMethod = Maps.newHashMap();
    final Set<Pair<String, String>> refClassFields = Sets.newHashSet();
    final String accessorSuffix = "_J2DAccessor";
    unit.accept(new RecursiveAstVisitor<Void>() {

      @Override
      public Void visitBlock(Block node) {
        List<Statement> statements = ImmutableList.copyOf(node.getStatements());
        for (Statement statement : statements) {
          statement.accept(this);
        }
        return null;
      }

      @Override
      public Void visitExpressionStatement(ExpressionStatement node) {
        if (node.getExpression() instanceof MethodInvocation) {
          MethodInvocation invocation = (MethodInvocation) node.getExpression();
          if (JavaUtils.isMethod(
              context.getNodeBinding(invocation),
              "java.lang.reflect.AccessibleObject",
              "setAccessible")) {
            ((Block) node.getParent()).getStatements().remove(node);
            return null;
          }
        }
        return super.visitExpressionStatement(node);
      }

      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        List<Expression> arguments = node.getArgumentList().getArguments();
        if (JavaUtils.isMethod(context.getNodeBinding(node), "java.lang.reflect.Field", "get")) {
          Expression target = arguments.get(0);
          String varName = ((SimpleIdentifier) node.getTarget()).getName();
          String fieldName = varToField.get(varName);
          String accessorName = fieldName + accessorSuffix;
          SemanticProcessor.replaceNode(node, propertyAccess(target, identifier(accessorName)));
          return null;
        }
        if (JavaUtils.isMethod(context.getNodeBinding(node), "java.lang.reflect.Field", "set")) {
          Expression target = arguments.get(0);
          String varName = ((SimpleIdentifier) node.getTarget()).getName();
          String fieldName = varToField.get(varName);
          String accessorName = fieldName + accessorSuffix;
          SemanticProcessor.replaceNode(
              node,
              assignmentExpression(
                  propertyAccess(target, identifier(accessorName)),
                  TokenType.EQ,
                  arguments.get(1)));
          return null;
        }
        if (JavaUtils.isMethod(context.getNodeBinding(node), "java.lang.reflect.Method", "invoke")) {
          Expression target = arguments.get(0);
          if (node.getTarget() instanceof MethodInvocation) {
            System.out.println(node.getTarget());
          }
          String varName = ((SimpleIdentifier) node.getTarget()).getName();
          String methodName = varToMethod.get(varName);
          List<Expression> methodArgs;
          if (arguments.size() == 1) {
            methodArgs = Lists.newArrayList();
          } else if (arguments.size() == 2 && arguments.get(1) instanceof ListLiteral) {
            methodArgs = ((ListLiteral) arguments.get(1)).getElements();
          } else {
            methodArgs = Lists.newArrayList(arguments);
            methodArgs.remove(0);
          }
          if (methodName != null) {
            SemanticProcessor.replaceNode(node, methodInvocation(target, methodName, methodArgs));
          }
          return null;
        }
        return super.visitMethodInvocation(node);
      }

      @Override
      public Void visitVariableDeclarationStatement(VariableDeclarationStatement node) {
        super.visitVariableDeclarationStatement(node);
        VariableDeclarationList variableList = node.getVariables();
        ITypeBinding typeBinding = context.getNodeTypeBinding(variableList.getType());
        List<VariableDeclaration> variables = variableList.getVariables();
        if (JavaUtils.isTypeNamed(typeBinding, "java.lang.reflect.Field") && variables.size() == 1) {
          VariableDeclaration variable = variables.get(0);
          if (variable.getInitializer() instanceof MethodInvocation) {
            MethodInvocation initializer = (MethodInvocation) variable.getInitializer();
            if (JavaUtils.isMethod(
                context.getNodeBinding(initializer),
                "java.lang.Class",
                "getDeclaredField")) {
              Expression getFieldArgument = initializer.getArgumentList().getArguments().get(0);
              String varName = variable.getName().getName();
              String fieldName = ((SimpleStringLiteral) getFieldArgument).getValue();
              varToField.put(varName, fieldName);
              ((Block) node.getParent()).getStatements().remove(node);
              // add (Class, Field) pair to generate accessor later
              addClassFieldPair(initializer.getTarget(), fieldName);
            }
          }
        }
        if (JavaUtils.isTypeNamed(typeBinding, "java.lang.reflect.Method") && variables.size() == 1) {
          VariableDeclaration variable = variables.get(0);
          if (variable.getInitializer() instanceof MethodInvocation) {
            MethodInvocation initializer = (MethodInvocation) variable.getInitializer();
            if (JavaUtils.isMethod(
                context.getNodeBinding(initializer),
                "java.lang.Class",
                "getDeclaredMethod")) {
              Expression getMethodArgument = initializer.getArgumentList().getArguments().get(0);
              String varName = variable.getName().getName();
              String methodName = ((SimpleStringLiteral) getMethodArgument).getValue();
              varToMethod.put(varName, methodName);
              ((Block) node.getParent()).getStatements().remove(node);
            }
          }
        }
        return null;
      }

      private void addClassFieldPair(Expression target, String fieldName) {
        while (target instanceof MethodInvocation) {
          target = ((MethodInvocation) target).getTarget();
        }
        // we expect: object.runtimeType
        if (target instanceof PropertyAccess) {
          Expression classTarget = ((PropertyAccess) target).getTarget();
          ITypeBinding classTargetBinding = context.getNodeTypeBinding(classTarget);
          String className = classTargetBinding.getName();
          refClassFields.add(Pair.of(className, fieldName));
        }
      }
    });
    // generate private field accessors
    unit.accept(new RecursiveAstVisitor<Void>() {
      @Override
      public Void visitClassDeclaration(ClassDeclaration node) {
        String className = node.getName().getName();
        for (Pair<String, String> pair : refClassFields) {
          if (pair.getLeft().equals(className)) {
            String fieldName = pair.getRight();
            String accessorName = fieldName + accessorSuffix;
            String privatePropertyName;
            if ("elementResolver".equals(fieldName) || "thisType".equals(fieldName)
                || "typeAnalyzer".equals(fieldName) || "labelScope".equals(fieldName)
                || "nameScope".equals(fieldName) || "enclosingClass".equals(fieldName)) {
              privatePropertyName = "_" + fieldName;
            } else {
              privatePropertyName = fieldName;
            }
            node.getMembers().add(
                methodDeclaration(
                    null,
                    null,
                    Keyword.GET,
                    null,
                    identifier(accessorName),
                    null,
                    expressionFunctionBody(identifier(privatePropertyName))));
            node.getMembers().add(
                methodDeclaration(
                    null,
                    null,
                    Keyword.SET,
                    null,
                    identifier(accessorName),
                    formalParameterList(simpleFormalParameter("__v")),
                    expressionFunctionBody(assignmentExpression(
                        identifier(privatePropertyName),
                        TokenType.EQ,
                        identifier("__v")))));
          }
        }
        return super.visitClassDeclaration(node);
      }
    });
  }

  static void useImportPrefix(final Context context, final AstNode rootNode,
      final String prefixName, final String[] packageNames, final boolean exactPackage) {
    rootNode.accept(new RecursiveAstVisitor<Void>() {
      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        super.visitMethodInvocation(node);
        Expression target = node.getTarget();
        Object binding0 = context.getNodeBinding(target);
        if (binding0 instanceof ITypeBinding) {
          ITypeBinding binding = (ITypeBinding) binding0;
          String shortName = binding.getName();
          shortName = StringUtils.substringBefore(shortName, "<");
          if (shouldRewrite(binding)) {
            SemanticProcessor.replaceNode(target, identifier(prefixName, shortName));
            return null;
          }
        }
        return null;
      }

      @Override
      public Void visitPropertyAccess(PropertyAccess node) {
        super.visitPropertyAccess(node);
        Expression target = node.getTarget();
        Object binding0 = context.getNodeBinding(target);
        if (binding0 instanceof ITypeBinding) {
          ITypeBinding binding = (ITypeBinding) binding0;
          String shortName = binding.getName();
          shortName = StringUtils.substringBefore(shortName, "<");
          if (shouldRewrite(binding)) {
            SemanticProcessor.replaceNode(target, identifier(prefixName, shortName));
            return null;
          }
        }
        return null;
      }

      @Override
      public Void visitTypeName(TypeName node) {
        ITypeBinding binding = context.getNodeTypeBinding(node);
        if (binding != null) {
          if (shouldRewrite(binding)) {
            String shortName = node.getName().getName();
            if (shortName.indexOf('.') != -1) {
              shortName = StringUtils.substringAfterLast(shortName, ".");
            }
            shortName = StringUtils.substringBefore(shortName, "<");
            node.setName(identifier(prefixName, shortName));
            return null;
          }
        }
        return super.visitTypeName(node);
      }

      private boolean isInPackage(ITypeBinding binding) {
        String typeName = binding.getQualifiedName();
        for (String packageName : packageNames) {
          if (typeName.equals(packageName + binding.getName())) {
            return true;
          }
        }
        return false;
      }

      private boolean isPrefixPackage(ITypeBinding binding) {
        String typeName = binding.getQualifiedName();
        for (String packageName : packageNames) {
          if (typeName.startsWith(packageName)) {
            return true;
          }
        }
        return false;
      }

      private boolean shouldRewrite(ITypeBinding binding) {
        if (exactPackage) {
          return isInPackage(binding);
        }
        return isPrefixPackage(binding);
      }
    });
  }

  private static String toSource(String... lines) {
    return Joiner.on("\n").join(lines);
  }

  public EngineSemanticProcessor(Context context) {
    super(context);
  }

  @Override
  public void process(final CompilationUnit unit) {
    unit.accept(new GeneralizingAstVisitor<Void>() {
      @Override
      public Void visitClassDeclaration(ClassDeclaration node) {
        // visit copy of members (we modify them)
        for (ClassMember member : Lists.newArrayList(node.getMembers())) {
          member.accept(this);
        }
        ITypeBinding typeBinding = context.getNodeTypeBinding(node);
        // hashCode is broken on Dart VM. So, we generate it using different way.
        // https://code.google.com/p/dart/issues/detail?id=5746
        if (JavaUtils.isTypeNamed(typeBinding, "com.google.dart.engine.ast.ASTNode")) {
          node.getMembers().add(
              fieldDeclaration(
                  true,
                  typeName("int"),
                  variableDeclaration("_hashCodeGenerator", integer(0))));
          node.getMembers().add(
              fieldDeclaration(
                  false,
                  Keyword.FINAL,
                  typeName("int"),
                  variableDeclaration(
                      "hashCode",
                      prefixExpression(TokenType.PLUS_PLUS, identifier("_hashCodeGenerator")))));
        }
        // done
        return null;
      }

      @Override
      public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
        super.visitInstanceCreationExpression(node);
        Object binding = context.getNodeBinding(node);
        if (binding instanceof IMethodBinding) {
          IMethodBinding methodBinding = (IMethodBinding) binding;
          // new IntList(5) -> new List()
          if (isMethodInClass2(
              methodBinding,
              "<init>(int)",
              "com.google.dart.engine.utilities.collection.IntList")) {
            node.getArgumentList().getArguments().clear();
            return null;
          }
        }
        // done
        return null;
      }

      @Override
      public Void visitMethodDeclaration(MethodDeclaration node) {
        super.visitMethodDeclaration(node);
        String name = node.getName().getName();
        NodeList<FormalParameter> parameters = getParameters(node);
        if ("accept".equals(name) && parameters.size() == 1) {
          node.setReturnType(null);
          FormalParameter formalParameter = parameters.get(0);
          ((SimpleFormalParameter) formalParameter).getType().setTypeArguments(null);
        }
        return null;
      }

      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        AstNode parent = node.getParent();
        List<Expression> args = node.getArgumentList().getArguments();
        if (isMethodInClass(node, "toArray", "com.google.dart.engine.utilities.collection.IntList")) {
          replaceNode(node, node.getTarget());
          return null;
        }
        if (isMethodInClass(
            node,
            "equals",
            "com.google.dart.engine.utilities.general.ObjectUtilities")) {
          Expression arg0 = args.get(0);
          Expression arg1 = args.get(1);
          if (parent instanceof PrefixExpression
              && ((PrefixExpression) parent).getOperator().getType() == TokenType.BANG) {
            replaceNode(
                parent,
                parenthesizedExpression(binaryExpression(arg0, TokenType.BANG_EQ, arg1)));
          } else {
            replaceNode(
                node,
                parenthesizedExpression(binaryExpression(arg0, TokenType.EQ_EQ, arg1)));
          }
          return null;
        }
        if (isMethodInClass(
            node,
            "getContents",
            "com.google.dart.engine.utilities.io.FileUtilities")) {
          replaceNode(node, methodInvocation(args.get(0), "readAsStringSync"));
          return null;
        }
        if (isMethodInClass(
            node,
            "getExtension",
            "com.google.dart.engine.utilities.io.FileUtilities")) {
          replaceNode(node.getTarget(), identifier("FileNameUtilities"));
          return null;
        }
        if (isMethodInClass(node, "encode", "com.google.dart.engine.utilities.io.UriUtilities")) {
          replaceNode(node, methodInvocation(identifier("Uri"), "encodeFull", args.get(0)));
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

      private NodeList<FormalParameter> getParameters(MethodDeclaration node) {
        if (node.getParameters() == null) {
          return new NodeList<FormalParameter>(null);
        }
        return node.getParameters().getParameters();
      }
    });
  }
}
