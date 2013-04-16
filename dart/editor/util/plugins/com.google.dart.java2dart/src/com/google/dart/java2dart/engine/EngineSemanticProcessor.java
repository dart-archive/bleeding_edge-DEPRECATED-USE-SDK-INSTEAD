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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorInitializer;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionStatement;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.ListLiteral;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NormalFormalParameter;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.VariableDeclarationStatement;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.processor.SemanticProcessor;
import com.google.dart.java2dart.util.JavaUtils;

import static com.google.dart.java2dart.util.ASTFactory.assignmentExpression;
import static com.google.dart.java2dart.util.ASTFactory.binaryExpression;
import static com.google.dart.java2dart.util.ASTFactory.block;
import static com.google.dart.java2dart.util.ASTFactory.blockFunctionBody;
import static com.google.dart.java2dart.util.ASTFactory.constructorDeclaration;
import static com.google.dart.java2dart.util.ASTFactory.expressionFunctionBody;
import static com.google.dart.java2dart.util.ASTFactory.expressionStatement;
import static com.google.dart.java2dart.util.ASTFactory.fieldDeclaration;
import static com.google.dart.java2dart.util.ASTFactory.formalParameterList;
import static com.google.dart.java2dart.util.ASTFactory.functionDeclaration;
import static com.google.dart.java2dart.util.ASTFactory.functionExpression;
import static com.google.dart.java2dart.util.ASTFactory.identifier;
import static com.google.dart.java2dart.util.ASTFactory.ifStatement;
import static com.google.dart.java2dart.util.ASTFactory.integer;
import static com.google.dart.java2dart.util.ASTFactory.methodDeclaration;
import static com.google.dart.java2dart.util.ASTFactory.methodInvocation;
import static com.google.dart.java2dart.util.ASTFactory.namedFormalParameter;
import static com.google.dart.java2dart.util.ASTFactory.nullLiteral;
import static com.google.dart.java2dart.util.ASTFactory.prefixExpression;
import static com.google.dart.java2dart.util.ASTFactory.propertyAccess;
import static com.google.dart.java2dart.util.ASTFactory.redirectingConstructorInvocation;
import static com.google.dart.java2dart.util.ASTFactory.returnStatement;
import static com.google.dart.java2dart.util.ASTFactory.simpleFormalParameter;
import static com.google.dart.java2dart.util.ASTFactory.thisExpression;
import static com.google.dart.java2dart.util.ASTFactory.typeName;
import static com.google.dart.java2dart.util.ASTFactory.variableDeclaration;
import static com.google.dart.java2dart.util.ASTFactory.variableDeclarationList;
import static com.google.dart.java2dart.util.ASTFactory.variableDeclarationStatement;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    unit.accept(new RecursiveASTVisitor<Void>() {

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
    unit.accept(new RecursiveASTVisitor<Void>() {
      @Override
      public Void visitClassDeclaration(ClassDeclaration node) {
        String className = node.getName().getName();
        for (Pair<String, String> pair : refClassFields) {
          if (pair.getLeft().equals(className)) {
            String fieldName = pair.getRight();
            String accessorName = fieldName + accessorSuffix;
            String privateFieldName = "_" + fieldName;
            node.getMembers().add(
                methodDeclaration(
                    null,
                    null,
                    Keyword.GET,
                    null,
                    identifier(accessorName),
                    null,
                    expressionFunctionBody(identifier(privateFieldName))));
            node.getMembers().add(
                methodDeclaration(
                    null,
                    null,
                    Keyword.SET,
                    null,
                    identifier(accessorName),
                    formalParameterList(simpleFormalParameter("__v")),
                    expressionFunctionBody(assignmentExpression(
                        identifier(privateFieldName),
                        TokenType.EQ,
                        identifier("__v")))));
          }
        }
        return super.visitClassDeclaration(node);
      }
    });
  }

  static void useImportPrefix(final Context context, final ASTNode rootNode,
      final String prefixName, final String[] packageNames) {
    rootNode.accept(new RecursiveASTVisitor<Void>() {
      @Override
      public Void visitPropertyAccess(PropertyAccess node) {
        super.visitPropertyAccess(node);
        Expression target = node.getTarget();
        Object binding0 = context.getNodeBinding(target);
        if (binding0 instanceof ITypeBinding) {
          ITypeBinding binding = (ITypeBinding) binding0;
          String shortName = binding.getName();
          shortName = StringUtils.substringBefore(shortName, "<");
          if (isPrefixPackage(binding)) {
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
          String shortName = binding.getName();
          shortName = StringUtils.substringBefore(shortName, "<");
          if (isPrefixPackage(binding)) {
            node.setName(identifier(prefixName, shortName));
            return null;
          }
        }
        return super.visitTypeName(node);
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
        // "Type" is declared in dart:core, so replace it
        if (JavaUtils.isTypeNamed(typeBinding, "com.google.dart.engine.type.Type")) {
          SimpleIdentifier nameNode = node.getName();
          context.renameIdentifier(nameNode, "Type2");
        }
        // done
        return null;
      }

      @Override
      public Void visitFieldDeclaration(FieldDeclaration node) {
        super.visitFieldDeclaration(node);
        ClassDeclaration parentClass = (ClassDeclaration) node.getParent();
        if (parentClass.getName().getName().equals("FileBasedSource")) {
          for (VariableDeclaration field : node.getFields().getVariables()) {
            if (field.getName().getName().equals("_UTF_8_CHARSET")) {
              parentClass.getMembers().remove(node);
            }
          }
        }
        return null;
      }

      @Override
      public Void visitMethodDeclaration(MethodDeclaration node) {
        super.visitMethodDeclaration(node);
        IMethodBinding binding = (IMethodBinding) context.getNodeBinding(node);
        String name = node.getName().getName();
        if ("accept".equals(name) && node.getParameters().getParameters().size() == 1) {
          node.setReturnType(null);
          FormalParameter formalParameter = node.getParameters().getParameters().get(0);
          ((SimpleFormalParameter) formalParameter).getType().setTypeArguments(null);
        }
        if (isMethodInClass(binding, "ensureVmIsExecutable", "com.google.dart.engine.sdk.DartSdk")) {
          node.setBody(blockFunctionBody());
          return null;
        }
        if (isMethodInClass(binding, "getContents", "com.google.dart.engine.source.FileBasedSource")) {
          SimpleIdentifier receiverIdent = node.getParameters().getParameters().get(0).getIdentifier();
          Block tryCacheBlock = block();
          {
            tryCacheBlock.getStatements().add(
                variableDeclarationStatement(variableDeclarationList(
                    null,
                    typeName("String"),
                    variableDeclaration(
                        "contents",
                        methodInvocation(
                            identifier("_contentCache"),
                            "getContents",
                            thisExpression())))));
            SimpleIdentifier contentsIdent = identifier("contents");
            Expression modificationStampExpr = methodInvocation(
                identifier("_contentCache"),
                "getModificationStamp",
                thisExpression());
            tryCacheBlock.getStatements().add(
                ifStatement(
                    binaryExpression(contentsIdent, TokenType.BANG_EQ, nullLiteral()),
                    block(
                        expressionStatement(methodInvocation(
                            receiverIdent,
                            "accept2",
                            contentsIdent,
                            modificationStampExpr)),
                        returnStatement())));
          }
          ExpressionStatement doReadStatement = expressionStatement(methodInvocation(
              receiverIdent,
              "accept2",
              methodInvocation(identifier("_file"), "readAsStringSync"),
              methodInvocation(identifier("_file"), "lastModified")));
          node.setBody(blockFunctionBody(tryCacheBlock, doReadStatement));
          return null;
        }
        return null;
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

      private boolean isMethodInClass(IMethodBinding binding, String reqName, String reqClassName) {
        return binding != null && Objects.equal(binding.getName(), reqName)
            && JavaUtils.isMethodInClass(binding, reqClassName);
      }

      private boolean isMethodInClass(MethodInvocation node, String reqName, String reqClassName) {
        Object binding = context.getNodeBinding(node);
        String name = node.getMethodName().getName();
        return Objects.equal(name, reqName) && JavaUtils.isMethodInClass(binding, reqClassName);
      }
    });
  }
}
