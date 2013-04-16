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

package com.google.dart.java2dart;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.AsExpression;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.BreakStatement;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.Comment;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorInitializer;
import com.google.dart.engine.ast.ContinueStatement;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.DoubleLiteral;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExtendsClause;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.FunctionBody;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.IfStatement;
import com.google.dart.engine.ast.ImplementsClause;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.IntegerLiteral;
import com.google.dart.engine.ast.Label;
import com.google.dart.engine.ast.ListLiteral;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.NullLiteral;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.SuperExpression;
import com.google.dart.engine.ast.TypeArgumentList;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.TypeParameterList;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.WhileStatement;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.StringToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.java2dart.util.ExecutionUtils;
import com.google.dart.java2dart.util.JavaUtils;
import com.google.dart.java2dart.util.RunnableEx;

import static com.google.dart.java2dart.util.ASTFactory.asExpression;
import static com.google.dart.java2dart.util.ASTFactory.assertStatement;
import static com.google.dart.java2dart.util.ASTFactory.assignmentStatement;
import static com.google.dart.java2dart.util.ASTFactory.binaryExpression;
import static com.google.dart.java2dart.util.ASTFactory.block;
import static com.google.dart.java2dart.util.ASTFactory.blockFunctionBody;
import static com.google.dart.java2dart.util.ASTFactory.booleanLiteral;
import static com.google.dart.java2dart.util.ASTFactory.breakStatement;
import static com.google.dart.java2dart.util.ASTFactory.catchClause;
import static com.google.dart.java2dart.util.ASTFactory.classDeclaration;
import static com.google.dart.java2dart.util.ASTFactory.compilationUnit;
import static com.google.dart.java2dart.util.ASTFactory.conditionalExpression;
import static com.google.dart.java2dart.util.ASTFactory.constructorDeclaration;
import static com.google.dart.java2dart.util.ASTFactory.declaredIdentifier;
import static com.google.dart.java2dart.util.ASTFactory.doStatement;
import static com.google.dart.java2dart.util.ASTFactory.doubleLiteral;
import static com.google.dart.java2dart.util.ASTFactory.emptyFunctionBody;
import static com.google.dart.java2dart.util.ASTFactory.emptyStatement;
import static com.google.dart.java2dart.util.ASTFactory.expressionFunctionBody;
import static com.google.dart.java2dart.util.ASTFactory.expressionStatement;
import static com.google.dart.java2dart.util.ASTFactory.extendsClause;
import static com.google.dart.java2dart.util.ASTFactory.fieldDeclaration;
import static com.google.dart.java2dart.util.ASTFactory.fieldFormalParameter;
import static com.google.dart.java2dart.util.ASTFactory.forEachStatement;
import static com.google.dart.java2dart.util.ASTFactory.forStatement;
import static com.google.dart.java2dart.util.ASTFactory.formalParameterList;
import static com.google.dart.java2dart.util.ASTFactory.identifier;
import static com.google.dart.java2dart.util.ASTFactory.ifStatement;
import static com.google.dart.java2dart.util.ASTFactory.implementsClause;
import static com.google.dart.java2dart.util.ASTFactory.indexExpression;
import static com.google.dart.java2dart.util.ASTFactory.instanceCreationExpression;
import static com.google.dart.java2dart.util.ASTFactory.integer;
import static com.google.dart.java2dart.util.ASTFactory.integerHex;
import static com.google.dart.java2dart.util.ASTFactory.isExpression;
import static com.google.dart.java2dart.util.ASTFactory.label;
import static com.google.dart.java2dart.util.ASTFactory.labeledStatement;
import static com.google.dart.java2dart.util.ASTFactory.listLiteral;
import static com.google.dart.java2dart.util.ASTFactory.listType;
import static com.google.dart.java2dart.util.ASTFactory.methodDeclaration;
import static com.google.dart.java2dart.util.ASTFactory.methodInvocation;
import static com.google.dart.java2dart.util.ASTFactory.nullLiteral;
import static com.google.dart.java2dart.util.ASTFactory.parenthesizedExpression;
import static com.google.dart.java2dart.util.ASTFactory.postfixExpression;
import static com.google.dart.java2dart.util.ASTFactory.prefixExpression;
import static com.google.dart.java2dart.util.ASTFactory.propertyAccess;
import static com.google.dart.java2dart.util.ASTFactory.simpleFormalParameter;
import static com.google.dart.java2dart.util.ASTFactory.string;
import static com.google.dart.java2dart.util.ASTFactory.superConstructorInvocation;
import static com.google.dart.java2dart.util.ASTFactory.thisExpression;
import static com.google.dart.java2dart.util.ASTFactory.throwExpression;
import static com.google.dart.java2dart.util.ASTFactory.tryStatement;
import static com.google.dart.java2dart.util.ASTFactory.typeName;
import static com.google.dart.java2dart.util.ASTFactory.typeParameter;
import static com.google.dart.java2dart.util.ASTFactory.typeParameterList;
import static com.google.dart.java2dart.util.ASTFactory.variableDeclaration;
import static com.google.dart.java2dart.util.ASTFactory.variableDeclarationList;
import static com.google.dart.java2dart.util.ASTFactory.variableDeclarationStatement;
import static com.google.dart.java2dart.util.ASTFactory.whileStatement;
import static com.google.dart.java2dart.util.TokenFactory.token;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Translates Java AST to Dart AST.
 */
public class SyntaxTranslator extends org.eclipse.jdt.core.dom.ASTVisitor {

  /**
   * Replaces "node" with "replacement" in parent of "node".
   */
  public static void replaceNode(ASTNode parent, ASTNode node, ASTNode replacement) {
    Class<? extends ASTNode> parentClass = parent.getClass();
    // try get/set methods
    try {
      for (Method getMethod : parentClass.getMethods()) {
        String getName = getMethod.getName();
        if (getName.startsWith("get") && getMethod.getParameterTypes().length == 0
            && getMethod.invoke(parent) == node) {
          String setName = "set" + getName.substring(3);
          Method setMethod = parentClass.getMethod(setName, getMethod.getReturnType());
          setMethod.invoke(parent, replacement);
          return;
        }
      }
    } catch (Throwable e) {
      ExecutionUtils.propagate(e);
    }
    // special cases
    if (parent instanceof ListLiteral) {
      List<Expression> elements = ((ListLiteral) parent).getElements();
      int index = elements.indexOf(node);
      if (index != -1) {
        elements.set(index, (Expression) replacement);
        return;
      }
    }
    if (parent instanceof ArgumentList) {
      List<Expression> arguments = ((ArgumentList) parent).getArguments();
      int index = arguments.indexOf(node);
      if (index != -1) {
        arguments.set(index, (Expression) replacement);
        return;
      }
    }
    if (parent instanceof TypeArgumentList) {
      List<TypeName> arguments = ((TypeArgumentList) parent).getArguments();
      int index = arguments.indexOf(node);
      if (index != -1) {
        arguments.set(index, (TypeName) replacement);
        return;
      }
    }
    // not found
    throw new UnsupportedOperationException("" + parentClass);
  }

  /**
   * Translates given Java AST into Dart AST.
   */
  public static CompilationUnit translate(Context context,
      org.eclipse.jdt.core.dom.CompilationUnit javaUnit) {
    SyntaxTranslator translator = new SyntaxTranslator(context);
    javaUnit.accept(translator);
    return (CompilationUnit) translator.result;
  }

  static Expression getPrimitiveTypeDefaultValue(String typeName) {
    if ("bool".equals(typeName)) {
      return booleanLiteral(false);
    }
    if ("int".equals(typeName)) {
      return integer(0);
    }
    if ("double".equals(typeName)) {
      return doubleLiteral(0.0);
    }
    return null;
  }

  private static org.eclipse.jdt.core.dom.MethodDeclaration getEnclosingMethod(
      org.eclipse.jdt.core.dom.ASTNode node) {
    while (node != null) {
      if (node instanceof org.eclipse.jdt.core.dom.MethodDeclaration) {
        return (org.eclipse.jdt.core.dom.MethodDeclaration) node;
      }
      node = node.getParent();
    }
    return null;
  }

  private static org.eclipse.jdt.core.dom.ITypeBinding getEnclosingTypeBinding(
      org.eclipse.jdt.core.dom.ASTNode node) {
    while (node != null) {
      if (node instanceof org.eclipse.jdt.core.dom.TypeDeclaration) {
        return ((org.eclipse.jdt.core.dom.TypeDeclaration) node).resolveBinding();
      }
      if (node instanceof org.eclipse.jdt.core.dom.AnonymousClassDeclaration) {
        return ((org.eclipse.jdt.core.dom.AnonymousClassDeclaration) node).resolveBinding();
      }
      if (node instanceof org.eclipse.jdt.core.dom.EnumDeclaration) {
        return ((org.eclipse.jdt.core.dom.EnumDeclaration) node).resolveBinding();
      }
      node = node.getParent();
    }
    return null;
  }

  /**
   * @return the {@link Method} of {@link SyntaxTranslator} to translate
   *         {@link org.eclipse.jdt.core.dom.ASTNode} of the given class.
   */
  private static Method getMostSpecificMethod(Class<?> argumentType) throws Exception {
    Method resultMethod = null;
    for (Method method : SyntaxTranslator.class.getMethods()) {
      if (method.getName().equals("visit")) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 1 && parameterTypes[0] == argumentType) {
          resultMethod = method;
          break;
        }
      }
    }
    Assert.isNotNull(resultMethod);
    return resultMethod;
  }

  private static boolean hasConstructorInvocation(org.eclipse.jdt.core.dom.ASTNode node) {
    final AtomicBoolean result = new AtomicBoolean();
    node.accept(new ASTVisitor() {
      @Override
      public boolean visit(ConstructorInvocation node) {
        result.set(true);
        return false;
      }
    });
    return result.get();
  }

  private static boolean isInEnumConstructor(org.eclipse.jdt.core.dom.ASTNode node) {
    boolean inConstructor = false;
    while (node != null) {
      if (node instanceof org.eclipse.jdt.core.dom.MethodDeclaration) {
        inConstructor = ((org.eclipse.jdt.core.dom.MethodDeclaration) node).isConstructor();
      }
      if (node instanceof org.eclipse.jdt.core.dom.EnumDeclaration) {
        return inConstructor;
      }
      node = node.getParent();
    }
    return false;
  }

  private static boolean isIntegerType(org.eclipse.jdt.core.dom.Expression expression) {
    ITypeBinding typeBinding = expression.resolveTypeBinding();
    return JavaUtils.isTypeNamed(typeBinding, "int") || JavaUtils.isTypeNamed(typeBinding, "long")
        || JavaUtils.isTypeNamed(typeBinding, "short");
  }

  /**
   * @return <code>true</code> if "subConstructor" is constructor of inner class which call
   *         "superConstructor".
   */
  private static boolean isSuperConstructor(IMethodBinding superConstructor,
      IMethodBinding subConstructor) {
    String superString = superConstructor.toString();
    String subString = subConstructor.toString();
    return superString.endsWith(subString);
  }

  private static int numberOfConstructors(org.eclipse.jdt.core.dom.ASTNode node) {
    int count = 0;
    if (node instanceof org.eclipse.jdt.core.dom.TypeDeclaration) {
      org.eclipse.jdt.core.dom.TypeDeclaration typeDecl = (org.eclipse.jdt.core.dom.TypeDeclaration) node;
      for (org.eclipse.jdt.core.dom.MethodDeclaration methodDecl : typeDecl.getMethods()) {
        if (methodDecl.isConstructor()) {
          count++;
        }
      }
      return count;
    }
    if (node instanceof EnumDeclaration) {
      EnumDeclaration enumDecl = (EnumDeclaration) node;
      for (Object child : (List<?>) enumDecl.bodyDeclarations()) {
        if (child instanceof org.eclipse.jdt.core.dom.MethodDeclaration) {
          org.eclipse.jdt.core.dom.MethodDeclaration methodDecl = (org.eclipse.jdt.core.dom.MethodDeclaration) child;
          if (methodDecl.isConstructor()) {
            count++;
          }
        }
      }
      return count;
    }
    throw new UnsupportedOperationException("not implemented: " + node.getClass());
  }

  private final Context context;

  private ASTNode result;

  private final List<CompilationUnitMember> artificialUnitDeclarations = Lists.newArrayList();

  private MethodDeclaration constructorImpl;

  private SyntaxTranslator(Context context) {
    this.context = context;
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ArrayAccess node) {
    Expression expression = translate(node.getArray());
    Expression index = translate(node.getIndex());
    return done(indexExpression(expression, index));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ArrayCreation node) {
    TypeName listType = translate(node.getType());
    TypeArgumentList typeArgs = listType.getTypeArguments();
    if (node.getInitializer() != null) {
      List<Expression> elements = translateExpressionList(node.getInitializer().expressions());
      return done(listLiteral(null, typeArgs, elements));
    } else {
      List<Expression> arguments = translateArguments(null, node.dimensions());
      // may be primitive array element
      {
        String arrayElementTypeName = typeArgs.getArguments().get(0).getName().getName();
        Expression initializer = getPrimitiveTypeDefaultValue(arrayElementTypeName);
        if (initializer != null) {
          arguments.add(initializer);
          return done(instanceCreationExpression(Keyword.NEW, listType, "filled", arguments));
        }
      }
      // non-primitive array element
      return done(instanceCreationExpression(Keyword.NEW, listType, arguments));
    }
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ArrayInitializer node) {
    List<Expression> elements = translateExpressionList(node.expressions());
    return done(listLiteral(elements));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ArrayType node) {
    TypeName elementType = translate(node.getElementType());
    int dimensions = node.getDimensions();
    return done(listType(elementType, dimensions));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.AssertStatement node) {
    return done(assertStatement(translateExpression(node.getExpression())));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.Assignment node) {
    Expression left = translate(node.getLeftHandSide());
    Expression right = translate(node.getRightHandSide());
    // operator
    TokenType tokenType = null;
    org.eclipse.jdt.core.dom.Assignment.Operator javaOperator = node.getOperator();
    if (javaOperator == org.eclipse.jdt.core.dom.Assignment.Operator.ASSIGN) {
      tokenType = TokenType.EQ;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.Assignment.Operator.PLUS_ASSIGN) {
      tokenType = TokenType.PLUS_EQ;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.Assignment.Operator.MINUS_ASSIGN) {
      tokenType = TokenType.MINUS_EQ;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.Assignment.Operator.TIMES_ASSIGN) {
      tokenType = TokenType.STAR_EQ;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.Assignment.Operator.DIVIDE_ASSIGN) {
      tokenType = TokenType.SLASH_EQ;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.Assignment.Operator.REMAINDER_ASSIGN) {
      tokenType = TokenType.PERCENT_EQ;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.Assignment.Operator.LEFT_SHIFT_ASSIGN) {
      tokenType = TokenType.LT_LT_EQ;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN
        || javaOperator == org.eclipse.jdt.core.dom.Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN) {
      tokenType = TokenType.GT_GT_EQ;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.Assignment.Operator.BIT_XOR_ASSIGN) {
      tokenType = TokenType.CARET_EQ;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.Assignment.Operator.BIT_OR_ASSIGN) {
      tokenType = TokenType.BAR_EQ;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.Assignment.Operator.BIT_AND_ASSIGN) {
      tokenType = TokenType.AMPERSAND_EQ;
    }
    Assert.isNotNull(tokenType, "No token for: " + javaOperator);
    // done
    return done(new AssignmentExpression(left, new Token(tokenType, 0), right));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.Block node) {
    List<Statement> statements = Lists.newArrayList();
    for (Iterator<?> I = node.statements().iterator(); I.hasNext();) {
      org.eclipse.jdt.core.dom.Statement javaStatement = (org.eclipse.jdt.core.dom.Statement) I.next();
      if (javaStatement instanceof org.eclipse.jdt.core.dom.SuperConstructorInvocation) {
        continue;
      }
      statements.add((Statement) translate(javaStatement));
    }
    return done(block(statements));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.BooleanLiteral node) {
    boolean value = node.booleanValue();
    return done(booleanLiteral(value));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.BreakStatement node) {
    SimpleIdentifier label = translate(node.getLabel());
    return done(breakStatement(label));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.CastExpression node) {
    Expression expression = translate(node.getExpression());
    TypeName typeName = translate(node.getType());
    AsExpression asExpression = asExpression(expression, typeName);
    return done(parenthesizedExpression(asExpression));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.CatchClause node) {
    SimpleIdentifier exceptionParameter = translateSimpleName(node.getException().getName());
    Block block = translate(node.getBody());
    // "catch (e) {}" or "on Type catch (e) {}"
    Type javaExceptionType = node.getException().getType();
    ITypeBinding javaExceptionBinding = javaExceptionType.resolveBinding();
    if (JavaUtils.isTypeNamed(javaExceptionBinding, "java.lang.Exception")) {
      return done(catchClause(null, exceptionParameter, null, block));
    } else {
      TypeName exceptionType = translate(javaExceptionType);
      return done(catchClause(exceptionType, exceptionParameter, null, block));
    }
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.CharacterLiteral node) {
    int intValue = node.charValue();
    return done(integerHex(intValue));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ClassInstanceCreation node) {
    IMethodBinding binding = node.resolveConstructorBinding();
    String signature = JavaUtils.getJdtSignature(binding);
    TypeName typeNameNode = (TypeName) translate(node.getType());
    final List<Expression> arguments = translateArguments(binding, node.arguments());
    final ClassDeclaration innerClass;
    {
      AnonymousClassDeclaration anoDeclaration = node.getAnonymousClassDeclaration();
      if (anoDeclaration != null) {
        ITypeBinding superclass = anoDeclaration.resolveBinding().getSuperclass();
        signature = superclass.getKey() + StringUtils.substringAfter(signature, ";");
        String name = typeNameNode.getName().getName().replace('.', '_');
        name = name + "_" + context.generateTechnicalAnonymousClassIndex();
        innerClass = declareInnerClass(binding, anoDeclaration, name, ArrayUtils.EMPTY_STRING_ARRAY);
        typeNameNode = typeName(name);
        // prepare enclosing type
        final ITypeBinding enclosingTypeBinding = getEnclosingTypeBinding(node);
        final SimpleIdentifier enclosingTypeRef;
        final AtomicBoolean addEnclosingTypeRef = new AtomicBoolean();
        {
          if (enclosingTypeBinding != null) {
            enclosingTypeRef = identifier(enclosingTypeBinding.getName() + "_this");
            // add enclosing class references
            innerClass.accept(new RecursiveASTVisitor<Void>() {
              @Override
              public Void visitMethodInvocation(MethodInvocation node) {
                if (node.getTarget() == null) {
                  IMethodBinding methodBinding = (IMethodBinding) context.getNodeBinding(node);
                  if (methodBinding.getDeclaringClass() == enclosingTypeBinding) {
                    addEnclosingTypeRef.set(true);
                    node.setTarget(enclosingTypeRef);
                  }
                }
                return super.visitMethodInvocation(node);
              }

              @Override
              public Void visitSimpleIdentifier(SimpleIdentifier node) {
                if (!(node.getParent() instanceof PropertyAccess)
                    && !(node.getParent() instanceof PrefixedIdentifier)) {
                  Object binding = context.getNodeBinding(node);
                  if (binding instanceof IVariableBinding) {
                    IVariableBinding variableBinding = (IVariableBinding) binding;
                    if (variableBinding.isField()
                        && variableBinding.getDeclaringClass() == enclosingTypeBinding) {
                      addEnclosingTypeRef.set(true);
                      replaceNode(node.getParent(), node, propertyAccess(enclosingTypeRef, node));
                    }
                  }
                }
                return super.visitSimpleIdentifier(node);
              }
            });
          } else {
            enclosingTypeRef = null;
          }
        }
        // declare referenced final variables XXX
        final String finalName = name;
        anoDeclaration.accept(new ASTVisitor() {
          final Set<org.eclipse.jdt.core.dom.IVariableBinding> addedParameters = Sets.newHashSet();
          final List<FormalParameter> constructorParameters = Lists.newArrayList();
          int index;

          @Override
          public void endVisit(AnonymousClassDeclaration node) {
            if (!constructorParameters.isEmpty()) {
              // add parameters to the existing "inner" constructor XXX
              for (ClassMember classMember : innerClass.getMembers()) {
                if (classMember instanceof ConstructorDeclaration) {
                  ConstructorDeclaration innerConstructor = (ConstructorDeclaration) classMember;
                  innerConstructor.getParameters().getParameters().addAll(constructorParameters);
                  return;
                }
              }
              // create new "inner" constructor
              innerClass.getMembers().add(
                  index,
                  constructorDeclaration(
                      identifier(finalName),
                      null,
                      formalParameterList(constructorParameters),
                      null));
            }
            super.endVisit(node);
          }

          @Override
          public void endVisit(SimpleName node) {
            IBinding nameBinding = node.resolveBinding();
            if (nameBinding instanceof org.eclipse.jdt.core.dom.IVariableBinding) {
              org.eclipse.jdt.core.dom.IVariableBinding variableBinding = (org.eclipse.jdt.core.dom.IVariableBinding) nameBinding;
              org.eclipse.jdt.core.dom.MethodDeclaration enclosingMethod = getEnclosingMethod(node);
              if (!variableBinding.isField() && enclosingMethod != null
                  && variableBinding.getDeclaringMethod() != enclosingMethod.resolveBinding()
                  && addedParameters.add(variableBinding)) {
                TypeName parameterTypeName = translateTypeName(variableBinding.getType());
                String parameterName = variableBinding.getName();
                SimpleIdentifier parameterNameNode = identifier(parameterName);
                innerClass.getMembers().add(
                    index++,
                    fieldDeclaration(parameterTypeName, variableDeclaration(parameterNameNode)));
                constructorParameters.add(fieldFormalParameter(null, null, parameterNameNode));
                arguments.add(parameterNameNode);
                context.putReference(parameterNameNode, variableBinding, null);
              }
            }
            super.endVisit(node);
          }

          @Override
          public boolean visit(AnonymousClassDeclaration node) {
            if (addEnclosingTypeRef.get()) {
              TypeName parameterTypeName = translateTypeName(enclosingTypeBinding);
              innerClass.getMembers().add(
                  index++,
                  fieldDeclaration(
                      false,
                      Keyword.FINAL,
                      parameterTypeName,
                      variableDeclaration(enclosingTypeRef)));
              constructorParameters.add(fieldFormalParameter(null, null, enclosingTypeRef));
              arguments.add(thisExpression());
            }
            return super.visit(node);
          }
        });
      } else {
        innerClass = null;
      }
    }
    InstanceCreationExpression creation = instanceCreationExpression(
        Keyword.NEW,
        typeNameNode,
        null,
        arguments);
    context.putNodeBinding(creation, binding);
    context.putAnonymousDeclaration(creation, innerClass);
    context.getConstructorDescription(binding).instanceCreations.add(creation);
    return done(creation);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.CompilationUnit node) {
    List<Directive> directives = Lists.newArrayList();
    List<CompilationUnitMember> declarations = Lists.newArrayList();
    for (Iterator<?> I = node.types().iterator(); I.hasNext();) {
      Object javaType = I.next();
      ClassDeclaration dartClass = translate((org.eclipse.jdt.core.dom.ASTNode) javaType);
      declarations.add(dartClass);
      declarations.addAll(artificialUnitDeclarations);
      artificialUnitDeclarations.clear();
    }
    return done(compilationUnit(directives, declarations));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ConditionalExpression node) {
    return done(conditionalExpression(
        translateExpression(node.getExpression()),
        translateExpression(node.getThenExpression()),
        translateExpression(node.getElseExpression())));
  }

  /**
   * We generate invocation of "impl" method instead of redirecting constructor invocation. The
   * reason is that in Java it is possible to have "redirecting constructor invocation" as first
   * statement of constructor and then any other statement. But in Dart redirection should be only
   * clause.
   */
  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ConstructorInvocation node) {
    IMethodBinding binding = node.resolveConstructorBinding();
    SimpleIdentifier nameNode = identifier("jtdTmp");
    context.getConstructorDescription(binding).implInvocations.add(nameNode);
    // invoke "impl"
    List<Expression> arguments = translateArguments(binding, node.arguments());
    if (isInEnumConstructor(node)) {
      arguments.add(0, identifier("___ordinal"));
      arguments.add(0, identifier("___name"));
    }
    MethodInvocation invocation = methodInvocation(nameNode, arguments);
    context.putNodeBinding(invocation, binding);
    return done(expressionStatement(invocation));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ContinueStatement node) {
    return done(new ContinueStatement(null, (SimpleIdentifier) translate(node.getLabel()), null));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.DoStatement node) {
    return done(doStatement(
        (Statement) translate(node.getBody()),
        translateExpression(node.getExpression())));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.EmptyStatement node) {
    return done(emptyStatement());
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.EnhancedForStatement node) {
    SimpleFormalParameter sfp = (SimpleFormalParameter) translate(node.getParameter());
    return done(forEachStatement(
        declaredIdentifier(sfp.getType(), sfp.getIdentifier()),
        translateExpression(node.getExpression()),
        (Statement) translate(node.getBody())));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.EnumConstantDeclaration node) {
    String fieldName = node.getName().getIdentifier();
    IMethodBinding constructorBinding = node.resolveConstructorBinding();
    // prepare enum name
    org.eclipse.jdt.core.dom.EnumDeclaration parentEnum = (org.eclipse.jdt.core.dom.EnumDeclaration) node.getParent();
    String enumTypeName = parentEnum.getName().getIdentifier();
    // may be create Dart top-level class for Java inner class
    String innerClassName = null;
    {
      AnonymousClassDeclaration anoClassDeclaration = node.getAnonymousClassDeclaration();
      if (anoClassDeclaration != null) {
        innerClassName = enumTypeName + "_" + fieldName;
        declareInnerClass(constructorBinding, anoClassDeclaration, innerClassName, new String[] {
            "String", "___name", "int", "___ordinal"});
      }
    }
    // prepare field type
    TypeName type = typeName(enumTypeName);
    // prepare field variables
    List<VariableDeclaration> variables = Lists.newArrayList();
    {
      List<Expression> argList = translateArguments(null, node.arguments());
      {
        int ordinal = parentEnum.enumConstants().indexOf(node);
        argList.add(0, integer(ordinal));
        argList.add(0, string(fieldName));
      }
      InstanceCreationExpression init;
      if (innerClassName == null) {
        init = instanceCreationExpression(Keyword.NEW, typeName(enumTypeName), argList);
        context.getConstructorDescription(constructorBinding).instanceCreations.add(init);
      } else {
        init = instanceCreationExpression(Keyword.NEW, typeName(innerClassName), argList);
      }
      variables.add(variableDeclaration(fieldName, init));
    }
    return done(fieldDeclaration(translateJavadoc(node), true, Keyword.FINAL, type, variables));
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean visit(org.eclipse.jdt.core.dom.EnumDeclaration node) {
    SimpleIdentifier name = translateSimpleName(node.getName());
    // implements
    ImplementsClause implementsClause;
    {
      List<TypeName> interfaces = Lists.newArrayList();
      // add Comparable
      interfaces.add(typeName("Comparable", typeName(name)));
      // add declared interfaces
      if (!node.superInterfaceTypes().isEmpty()) {
        for (Object javaInterface : node.superInterfaceTypes()) {
          interfaces.add((TypeName) translate((org.eclipse.jdt.core.dom.ASTNode) javaInterface));
        }
      }
      // create ImplementsClause
      implementsClause = new ImplementsClause(null, interfaces);
    }
    // members
    List<ClassMember> members = Lists.newArrayList();
    {
      boolean multipleConstructors = numberOfConstructors(node) > 1;
      // constants
      List<Expression> valuesList = Lists.newArrayList();
      for (Object javaConst : node.enumConstants()) {
        org.eclipse.jdt.core.dom.EnumConstantDeclaration javaEnumConst = (org.eclipse.jdt.core.dom.EnumConstantDeclaration) javaConst;
        members.add((FieldDeclaration) translate(javaEnumConst));
        valuesList.add(identifier(javaEnumConst.getName().getIdentifier()));
      }
      // values
      members.add(fieldDeclaration(
          true,
          Keyword.FINAL,
          listType(typeName(name), 1),
          variableDeclaration("values", listLiteral(valuesList))));
      // body declarations
      members.add(fieldDeclaration(
          false,
          multipleConstructors ? null : Keyword.FINAL,
          typeName("String"),
          variableDeclaration("__name")));
      members.add(fieldDeclaration(
          false,
          multipleConstructors ? null : Keyword.FINAL,
          typeName("int"),
          variableDeclaration("__ordinal")));
      members.add(methodDeclaration(
          null,
          typeName("int"),
          Keyword.GET,
          null,
          identifier("ordinal"),
          null,
          expressionFunctionBody(identifier("__ordinal"))));
      boolean hasConstructor = false;
      for (Iterator<?> I = node.bodyDeclarations().iterator(); I.hasNext();) {
        org.eclipse.jdt.core.dom.BodyDeclaration javaBodyDecl = (org.eclipse.jdt.core.dom.BodyDeclaration) I.next();
        constructorImpl = null;
        ClassMember member = translate(javaBodyDecl);
        members.add(member);
        if (constructorImpl != null) {
          members.add(constructorImpl);
        }
        if (javaBodyDecl instanceof org.eclipse.jdt.core.dom.MethodDeclaration) {
          if (((org.eclipse.jdt.core.dom.MethodDeclaration) javaBodyDecl).isConstructor()) {
            hasConstructor = true;
          }
        }
      }
      // add default constructor, use artificial constructor
      if (!hasConstructor) {
        org.eclipse.jdt.core.dom.MethodDeclaration ac = node.getAST().newMethodDeclaration();
        try {
          ac.setConstructor(true);
          ac.setName(node.getAST().newSimpleName(name.getName()));
          ac.setBody(node.getAST().newBlock());
          node.bodyDeclarations().add(ac);
          ConstructorDeclaration innerConstructor = translate(ac);
          members.add(innerConstructor);
          if (constructorImpl != null) {
            members.add(constructorImpl);
          }
        } finally {
          node.bodyDeclarations().remove(ac);
        }
      }
      // compareTo()
      members.add(methodDeclaration(
          typeName("int"),
          identifier("compareTo"),
          formalParameterList(simpleFormalParameter(typeName(name), "other")),
          expressionFunctionBody(binaryExpression(
              identifier("__ordinal"),
              TokenType.MINUS,
              propertyAccess(identifier("other"), identifier("__ordinal"))))));
      // toString()
      members.add(methodDeclaration(
          typeName("String"),
          identifier("toString"),
          formalParameterList(),
          expressionFunctionBody(identifier("__name"))));
    }
    return done(classDeclaration(
        translateJavadoc(node),
        name,
        null,
        null,
        implementsClause,
        members));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ExpressionStatement node) {
    Expression expression = translate(node.getExpression());
    return done(expressionStatement(expression));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.FieldAccess node) {
    PropertyAccess result = propertyAccess(
        translateExpression(node.getExpression()),
        (SimpleIdentifier) translate(node.getName()));
    context.putNodeBinding(result, node.resolveFieldBinding());
    return done(result);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.FieldDeclaration node) {
    boolean isNotPublic = !org.eclipse.jdt.core.dom.Modifier.isPublic(node.getModifiers());
    boolean isStatic = org.eclipse.jdt.core.dom.Modifier.isStatic(node.getModifiers());
    FieldDeclaration fieldDeclaration = fieldDeclaration(
        translateJavadoc(node),
        isStatic,
        translateVariableDeclarationList(false, node.getType(), node.fragments()));
    if (isNotPublic) {
      context.putPrivateClassMember(fieldDeclaration);
    }
    return done(fieldDeclaration);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ForStatement node) {
    Expression condition = translateExpression(node.getExpression());
    List<Expression> updaters = translateExpressionList(node.updaters());
    Statement body = (Statement) translate(node.getBody());
    Object javaInitializer = !node.initializers().isEmpty() ? node.initializers().get(0) : null;
    if (javaInitializer instanceof org.eclipse.jdt.core.dom.VariableDeclarationExpression) {
      org.eclipse.jdt.core.dom.VariableDeclarationExpression javaVDE = (org.eclipse.jdt.core.dom.VariableDeclarationExpression) javaInitializer;
      List<VariableDeclaration> variables = Lists.newArrayList();
      for (Iterator<?> I = javaVDE.fragments().iterator(); I.hasNext();) {
        org.eclipse.jdt.core.dom.VariableDeclarationFragment fragment = (org.eclipse.jdt.core.dom.VariableDeclarationFragment) I.next();
        variables.add((VariableDeclaration) translate(fragment));
      }
      VariableDeclarationList variableList = variableDeclarationList(
          null,
          (TypeName) translate(javaVDE.getType()),
          variables);
      return done(forStatement(variableList, condition, updaters, body));
    } else {
      Expression initializer = translate((org.eclipse.jdt.core.dom.ASTNode) javaInitializer);
      return done(forStatement(initializer, condition, updaters, body));
    }
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.IfStatement node) {
    return done(ifStatement(
        translateExpression(node.getExpression()),
        (Statement) translate(node.getThenStatement()),
        (Statement) translate(node.getElseStatement())));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.InfixExpression node) {
    Expression left = translate(node.getLeftOperand());
    Expression right = translate(node.getRightOperand());
    // operator
    TokenType tokenType = null;
    org.eclipse.jdt.core.dom.InfixExpression.Operator javaOperator = node.getOperator();
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.PLUS) {
      tokenType = TokenType.PLUS;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.MINUS) {
      tokenType = TokenType.MINUS;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.TIMES) {
      tokenType = TokenType.STAR;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.DIVIDE) {
      if (isIntegerType(node.getLeftOperand()) && isIntegerType(node.getRightOperand())) {
        tokenType = TokenType.TILDE_SLASH;
      } else {
        tokenType = TokenType.SLASH;
      }
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.REMAINDER) {
      tokenType = TokenType.PERCENT;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.LEFT_SHIFT) {
      tokenType = TokenType.LT_LT;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.RIGHT_SHIFT_SIGNED
        || javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED) {
      tokenType = TokenType.GT_GT;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.CONDITIONAL_OR) {
      tokenType = TokenType.BAR_BAR;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.CONDITIONAL_AND) {
      tokenType = TokenType.AMPERSAND_AMPERSAND;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.XOR) {
      tokenType = TokenType.CARET;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.OR) {
      tokenType = TokenType.BAR;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.AND) {
      tokenType = TokenType.AMPERSAND;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.LESS) {
      tokenType = TokenType.LT;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.GREATER) {
      tokenType = TokenType.GT;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.LESS_EQUALS) {
      tokenType = TokenType.LT_EQ;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.GREATER_EQUALS) {
      tokenType = TokenType.GT_EQ;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.EQUALS) {
      if (isNumberOrNull(left) || isNumberOrNull(right)) {
        tokenType = TokenType.EQ_EQ;
      } else {
        return done(methodInvocation("identical", left, right));
      }
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.NOT_EQUALS) {
      tokenType = TokenType.BANG_EQ;
    }
    Assert.isNotNull(tokenType, "No token for: " + javaOperator);
    // create BinaryExpression
    BinaryExpression binary = binaryExpression(left, tokenType, right);
    for (Object javaOperand : node.extendedOperands()) {
      Expression operand = translate((org.eclipse.jdt.core.dom.ASTNode) javaOperand);
      binary = binaryExpression(binary, tokenType, operand);
    }
    return done(binary);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.InstanceofExpression node) {
    return done(isExpression(
        translateExpression(node.getLeftOperand()),
        false,
        (TypeName) translate(node.getRightOperand())));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.Javadoc node) {
    StringBuilder buffer = new StringBuilder();
    {
      buffer.append("/**");
      for (Object javaTag : node.tags()) {
        buffer.append(javaTag.toString());
      }
      buffer.append("\n */\n");
    }
    StringToken commentToken = new StringToken(TokenType.STRING, buffer.toString(), 0);
    return done(Comment.createDocumentationComment(new Token[] {commentToken}));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.LabeledStatement node) {
    List<Label> labels = Lists.newArrayList();
    while (true) {
      SimpleIdentifier labelIdentifier = translate(node.getLabel());
      labels.add(label(labelIdentifier));
      if (node.getBody() instanceof org.eclipse.jdt.core.dom.LabeledStatement) {
        node = (org.eclipse.jdt.core.dom.LabeledStatement) node.getBody();
      } else {
        break;
      }
    }
    return done(labeledStatement(labels, (Statement) translate(node.getBody())));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.MethodDeclaration node) {
    boolean isNotPublic = !org.eclipse.jdt.core.dom.Modifier.isPublic(node.getModifiers());
    IMethodBinding binding = node.resolveBinding();
    boolean isEnumConstructor = node.isConstructor()
        && node.getParent() instanceof org.eclipse.jdt.core.dom.EnumDeclaration;
    // parameters
    FormalParameterList parameterList;
    {
      List<FormalParameter> parameters = Lists.newArrayList();
      if (isEnumConstructor) {
        if (numberOfConstructors(node.getParent()) > 1) {
          parameters.add(simpleFormalParameter(typeName("String"), "___name"));
          parameters.add(simpleFormalParameter(typeName("int"), "___ordinal"));
        } else {
          parameters.add(fieldFormalParameter(null, null, "__name"));
          parameters.add(fieldFormalParameter(null, null, "__ordinal"));
        }
      }
      for (Iterator<?> I = node.parameters().iterator(); I.hasNext();) {
        org.eclipse.jdt.core.dom.SingleVariableDeclaration javaParameter = (org.eclipse.jdt.core.dom.SingleVariableDeclaration) I.next();
        SimpleFormalParameter parameter = translate(javaParameter);
        parameters.add(parameter);
      }
      parameterList = formalParameterList(parameters);
    }
    // body
    FunctionBody body;
    SuperConstructorInvocation superConstructorInvocation = null;
    {
      org.eclipse.jdt.core.dom.Block javaBlock = node.getBody();
      if (javaBlock != null) {
        for (Object javaStatement : javaBlock.statements()) {
          if (javaStatement instanceof org.eclipse.jdt.core.dom.SuperConstructorInvocation) {
            superConstructorInvocation = translate((org.eclipse.jdt.core.dom.SuperConstructorInvocation) javaStatement);
          }
        }
        Block bodyBlock = (Block) translate(javaBlock);
        body = new BlockFunctionBody(bodyBlock);
        NodeList<Statement> statements = bodyBlock.getStatements();
        if (isEnumConstructor && !hasConstructorInvocation(node)) {
          if (numberOfConstructors(node.getParent()) > 1) {
            statements.add(0, assignmentStatement("__ordinal", "___ordinal"));
            statements.add(0, assignmentStatement("__name", "___name"));
          }
        }
        // convert "{ return foo; }" to "=> foo;"
        if (statements.size() == 1 && statements.get(0) instanceof ReturnStatement) {
          body = expressionFunctionBody(((ReturnStatement) statements.get(0)).getExpression());
        }
      } else {
        body = emptyFunctionBody();
      }
    }
    // constructor
    if (node.isConstructor()) {
      boolean multipleConstructors = numberOfConstructors(node.getParent()) > 1;
      List<ConstructorInitializer> initializers = Lists.newArrayList();
      if (superConstructorInvocation != null) {
        initializers.add(superConstructorInvocation);
      }
      String technicalConstructorName = context.generateTechnicalConstructorName();
      String constructorDeclName = technicalConstructorName + "_decl";
      String constructorImplName = "_" + technicalConstructorName + "_impl";
      if (multipleConstructors) {
        constructorImpl = methodDeclaration(
            null,
            identifier(constructorImplName),
            parameterList,
            body);
      }
      //
      List<Expression> implInvArgs = Lists.newArrayList();
      for (FormalParameter parameter : parameterList.getParameters()) {
        implInvArgs.add(parameter.getIdentifier());
      }
      Expression implInvocation = methodInvocation(constructorImplName, implInvArgs);
      Statement conStatement = expressionStatement(implInvocation);
      Block constructorBody = block(conStatement);
      SimpleIdentifier nameNode = identifier(constructorDeclName);
      context.getConstructorDescription(binding).declName = constructorDeclName;
      context.getConstructorDescription(binding).implName = multipleConstructors
          ? constructorImplName : constructorDeclName;
      if (multipleConstructors) {
        body = blockFunctionBody(constructorBody);
      }
      ConstructorDeclaration constructor = constructorDeclaration(
          translateJavadoc(node),
          identifier(node.getName().getIdentifier()),
          nameNode,
          parameterList,
          initializers,
          body);
      context.putConstructorBinding(constructor, binding);
      return done(constructor);
    } else {
      boolean isStatic = org.eclipse.jdt.core.dom.Modifier.isStatic(node.getModifiers());
      SimpleIdentifier dartMethodName = translateSimpleName(node.getName());
      MethodDeclaration methodDeclaration = methodDeclaration(
          translateJavadoc(node),
          isStatic,
          (TypeName) translate(node.getReturnType2()),
          dartMethodName,
          parameterList,
          body);
      context.putNodeBinding(methodDeclaration, binding);
      if (isNotPublic) {
        context.putPrivateClassMember(methodDeclaration);
      }
      return done(methodDeclaration);
    }
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.MethodInvocation node) {
    IMethodBinding binding = node.resolveMethodBinding();
    Expression target = translateExpression(node.getExpression());
    List<Expression> arguments = translateArguments(binding, node.arguments());
    // prepare invocation
    Identifier name = translate(node.getName());
    MethodInvocation invocation;
    if (name instanceof SimpleIdentifier) {
      invocation = methodInvocation(target, (SimpleIdentifier) name, arguments);
    } else {
      PrefixedIdentifier prefixedName = (PrefixedIdentifier) name;
      target = prefixedName.getPrefix();
      invocation = methodInvocation(target, prefixedName.getIdentifier(), arguments);
    }
    // done
    context.putNodeBinding(invocation, binding);
    return done(invocation);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.NullLiteral node) {
    return done(nullLiteral());
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.NumberLiteral node) {
    String token = node.getToken();
    if (token.contains(".")
        || !StringUtils.startsWithIgnoreCase(token, "0x")
        && (StringUtils.endsWithIgnoreCase(token, "F") || StringUtils.endsWithIgnoreCase(token, "D"))) {
      token = StringUtils.removeEndIgnoreCase(token, "F");
      token = StringUtils.removeEndIgnoreCase(token, "D");
      if (!token.contains(".")) {
        token += ".0";
      }
      return done(new DoubleLiteral(token(TokenType.DOUBLE, token), 0));
    } else {
      token = StringUtils.removeEndIgnoreCase(token, "L");
      return done(new IntegerLiteral(token(TokenType.INT, token), BigInteger.valueOf(0)));
    }
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ParameterizedType node) {
    List<TypeName> typeArguments;
    {
      List<?> javaTypeArguments = node.typeArguments();
      boolean hasMethodTypeVariable = false;
      for (Object _javaTypeArgument : javaTypeArguments) {
        org.eclipse.jdt.core.dom.Type javaTypeArgument = (org.eclipse.jdt.core.dom.Type) _javaTypeArgument;
        ITypeBinding binding = javaTypeArgument.resolveBinding();
        if (binding != null && binding.isTypeVariable() && binding.getDeclaringMethod() != null) {
          hasMethodTypeVariable = true;
          break;
        }
      }
      if (hasMethodTypeVariable) {
        typeArguments = null;
      } else {
        typeArguments = translateTypeNames(javaTypeArguments);
      }
    }
    ITypeBinding binding = node.resolveBinding();
    TypeName typeName = typeName(((TypeName) translate(node.getType())).getName(), typeArguments);
    context.putNodeBinding(typeName, binding);
    context.putNodeTypeBinding(typeName, binding);
    return done(typeName);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ParenthesizedExpression node) {
    Expression expression = translate(node.getExpression());
    return done(parenthesizedExpression(expression));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.PostfixExpression node) {
    Expression operand = translate(node.getOperand());
    // operator
    TokenType tokenType = null;
    org.eclipse.jdt.core.dom.PostfixExpression.Operator javaOperator = node.getOperator();
    if (javaOperator == org.eclipse.jdt.core.dom.PostfixExpression.Operator.INCREMENT) {
      tokenType = TokenType.PLUS_PLUS;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.PostfixExpression.Operator.DECREMENT) {
      tokenType = TokenType.MINUS_MINUS;
    }
    // done
    return done(postfixExpression(operand, tokenType));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.PrefixExpression node) {
    Expression operand = translate(node.getOperand());
    // operator
    TokenType tokenType = null;
    org.eclipse.jdt.core.dom.PrefixExpression.Operator javaOperator = node.getOperator();
    if (javaOperator == org.eclipse.jdt.core.dom.PrefixExpression.Operator.PLUS) {
      return done(operand);
    }
    if (javaOperator == org.eclipse.jdt.core.dom.PrefixExpression.Operator.INCREMENT) {
      tokenType = TokenType.PLUS_PLUS;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.PrefixExpression.Operator.DECREMENT) {
      tokenType = TokenType.MINUS_MINUS;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.PrefixExpression.Operator.MINUS) {
      tokenType = TokenType.MINUS;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.PrefixExpression.Operator.NOT) {
      tokenType = TokenType.BANG;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.PrefixExpression.Operator.COMPLEMENT) {
      tokenType = TokenType.TILDE;
    }
    Assert.isNotNull(tokenType, "No token for: " + javaOperator);
    // done
    return done(prefixExpression(tokenType, operand));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.PrimitiveType node) {
    String name = node.toString();
    if ("boolean".equals(name)) {
      name = "bool";
    }
    if ("byte".equals(name) || "char".equals(name) || "short".equals(name) || "long".equals(name)) {
      name = "int";
    }
    if ("float".equals(name)) {
      name = "double";
    }
    return done(typeName(name));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.QualifiedName node) {
    PropertyAccess result = propertyAccess(
        translateExpression(node.getQualifier()),
        translateSimpleName(node.getName()));
    context.putNodeBinding(result, node.resolveBinding());
    return done(result);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ReturnStatement node) {
    return done(new ReturnStatement(null, translateExpression(node.getExpression()), null));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.SimpleName node) {
    IBinding binding = node.resolveBinding();
    SimpleIdentifier result = identifier(node.getIdentifier());
    putReference(binding, result);
    // may be statically imported field, generate PropertyAccess
    {
      org.eclipse.jdt.core.dom.StructuralPropertyDescriptor locationInParent = node.getLocationInParent();
      if (binding instanceof IVariableBinding) {
        org.eclipse.jdt.core.dom.IVariableBinding variableBinding = (org.eclipse.jdt.core.dom.IVariableBinding) binding;
        org.eclipse.jdt.core.dom.ASTNode parent = node.getParent();
        if (locationInParent == org.eclipse.jdt.core.dom.EnumConstantDeclaration.ARGUMENTS_PROPERTY
            || locationInParent == org.eclipse.jdt.core.dom.ClassInstanceCreation.ARGUMENTS_PROPERTY
            || locationInParent == org.eclipse.jdt.core.dom.MethodInvocation.ARGUMENTS_PROPERTY
            || locationInParent == org.eclipse.jdt.core.dom.ConstructorInvocation.ARGUMENTS_PROPERTY
            || locationInParent == org.eclipse.jdt.core.dom.SuperConstructorInvocation.ARGUMENTS_PROPERTY
            || locationInParent == org.eclipse.jdt.core.dom.Assignment.RIGHT_HAND_SIDE_PROPERTY
            || locationInParent == org.eclipse.jdt.core.dom.SwitchCase.EXPRESSION_PROPERTY
            || parent instanceof org.eclipse.jdt.core.dom.InfixExpression
            || parent instanceof org.eclipse.jdt.core.dom.ConditionalExpression
            || parent instanceof org.eclipse.jdt.core.dom.ReturnStatement) {
          ITypeBinding declaringBinding = variableBinding.getDeclaringClass();
          ITypeBinding enclosingBinding = getEnclosingTypeBinding(node);
          if (declaringBinding != null && enclosingBinding != declaringBinding
              && org.eclipse.jdt.core.dom.Modifier.isStatic(variableBinding.getModifiers())) {
            SimpleIdentifier target = identifier(declaringBinding.getName());
            putReference(declaringBinding, target);
            return done(propertyAccess(target, result));
          }
        }
      }
    }
    // may be statically imported method, generate PrefixedIdentifier
    {
      org.eclipse.jdt.core.dom.StructuralPropertyDescriptor locationInParent = node.getLocationInParent();
      if (binding instanceof IMethodBinding) {
        IMethodBinding methodBinding = (IMethodBinding) binding;
        if (locationInParent == org.eclipse.jdt.core.dom.MethodInvocation.NAME_PROPERTY
            && ((org.eclipse.jdt.core.dom.MethodInvocation) node.getParent()).getExpression() == null) {
          ITypeBinding declaringBinding = methodBinding.getDeclaringClass();
          ITypeBinding enclosingBinding = getEnclosingTypeBinding(node);
          if (declaringBinding != null && enclosingBinding != declaringBinding
              && org.eclipse.jdt.core.dom.Modifier.isStatic(methodBinding.getModifiers())) {
            SimpleIdentifier prefix = identifier(declaringBinding.getName());
            putReference(declaringBinding, prefix);
            return done(identifier(prefix, result));
          }
        }
      }
    }
    // done
    return done(result);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.SimpleType node) {
    String name = node.getName().toString();
    ITypeBinding binding = node.resolveBinding();
    // in Dart we cannot use separate type parameters for methods, so we replace
    // them with type bounds
    if (binding != null && binding.isTypeVariable() && binding.getDeclaringMethod() != null) {
      binding = binding.getErasure();
      name = binding.getName();
    }
    // translate name
    SimpleIdentifier nameNode = identifier(name);
    putReference(binding, nameNode);
    {
      if ("Void".equals(name)) {
        nameNode = identifier("Object");
      }
      if ("Boolean".equals(name)) {
        nameNode = identifier("bool");
      }
      if ("Short".equals(name) || "Integer".equals(name) || "Long".equals(name)) {
        nameNode = identifier("int");
      }
      if ("Float".equals(name) || "Double".equals(name)) {
        nameNode = identifier("double");
      }
      if ("BigInteger".equals(name)) {
        nameNode = identifier("int");
      }
    }
    // done
    TypeName typeName = typeName(nameNode);
    context.putNodeBinding(typeName, binding);
    context.putNodeTypeBinding(typeName, binding);
    return done(typeName);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.SingleVariableDeclaration node) {
    TypeName type = (TypeName) translate(node.getType());
    type = listType(type, node.getExtraDimensions());
    if (node.isVarargs()) {
      type = listType(type, 1);
    }
    return done(simpleFormalParameter(type, translateSimpleName(node.getName())));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.StringLiteral node) {
    String tokenValue = node.getEscapedValue();
    tokenValue = StringUtils.replace(tokenValue, "$", "\\$");
    SimpleStringLiteral literal = new SimpleStringLiteral(
        token(TokenType.STRING, tokenValue),
        node.getLiteralValue());
    context.putNodeTypeBinding(literal, node.resolveTypeBinding());
    return done(literal);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.SuperConstructorInvocation node) {
    IMethodBinding binding = node.resolveConstructorBinding();
    // invoke "impl"
    List<Expression> arguments = translateArguments(binding, node.arguments());
    SuperConstructorInvocation superInvocation = superConstructorInvocation(arguments);
    context.getConstructorDescription(binding).superInvocations.add(superInvocation);
    context.putNodeBinding(superInvocation, binding);
    return done(superInvocation);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.SuperMethodInvocation node) {
    IMethodBinding binding = node.resolveMethodBinding();
    Expression target = new SuperExpression(null);
    List<Expression> arguments = translateArguments(binding, node.arguments());
    SimpleIdentifier name = translateSimpleName(node.getName());
    return done(methodInvocation(target, name, arguments));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.SwitchStatement node) {
    IfStatement mainIfStatement = null;
    IfStatement targetIfStatement = null;
    Block ifBlock = null;
    Expression ifCondition = null;
    for (Iterator<?> I = node.statements().iterator(); I.hasNext();) {
      Object javaMember = I.next();
      if (javaMember instanceof org.eclipse.jdt.core.dom.SwitchCase) {
        org.eclipse.jdt.core.dom.SwitchCase javaCase = (org.eclipse.jdt.core.dom.SwitchCase) javaMember;
        if (javaCase.getExpression() != null) {
          Expression condition = binaryExpression(
              translateExpression(node.getExpression()),
              TokenType.EQ_EQ,
              translateExpression(javaCase.getExpression()));
          if (ifCondition == null) {
            ifCondition = condition;
            ifBlock = block();
            IfStatement ifStatement = ifStatement(condition, ifBlock);
            if (mainIfStatement == null) {
              mainIfStatement = ifStatement;
            } else {
              targetIfStatement.setElseStatement(ifStatement);
            }
            targetIfStatement = ifStatement;
          } else {
            ifCondition = binaryExpression(ifCondition, TokenType.BAR_BAR, condition);
            targetIfStatement.setCondition(ifCondition);
          }
        } else {
          ifBlock = block();
          targetIfStatement.setElseStatement(ifBlock);
        }
      } else {
        ifCondition = null;
        Statement statement = translate((org.eclipse.jdt.core.dom.Statement) javaMember);
        if (!(statement instanceof BreakStatement)) {
          ifBlock.getStatements().add(statement);
        }
      }
    }
    // wrap everything into "while(true)" to handle inner "break"
    WhileStatement whileStatement = whileStatement(
        booleanLiteral(true),
        block(mainIfStatement, breakStatement()));
    return done(whileStatement);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.SynchronizedStatement node) {
    return visit(node.getBody());
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ThisExpression node) {
    return done(thisExpression());
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ThrowStatement node) {
    return done(expressionStatement(throwExpression(translateExpression(node.getExpression()))));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.TryStatement node) {
    List<CatchClause> catchClauses = Lists.newArrayList();
    for (Iterator<?> I = node.catchClauses().iterator(); I.hasNext();) {
      org.eclipse.jdt.core.dom.CatchClause javaCatch = (org.eclipse.jdt.core.dom.CatchClause) I.next();
      catchClauses.add((CatchClause) translate(javaCatch));
    }
    return done(tryStatement(
        (Block) translate(node.getBody()),
        catchClauses,
        (Block) translate(node.getFinally())));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.TypeDeclaration node) {
    ITypeBinding binding = node.resolveBinding();
    // prepare name
    SimpleIdentifier name;
    {
      name = translateSimpleName(node.getName());
      if (binding != null) {
        ITypeBinding declaringClass = binding.getDeclaringClass();
        if (declaringClass != null) {
          context.putInnerClassName(name);
          name.setToken(token(TokenType.IDENTIFIER, declaringClass.getName() + "_" + name.getName()));
        }
      }
    }
    // interface
    Token abstractToken = null;
    if (node.isInterface() || org.eclipse.jdt.core.dom.Modifier.isAbstract(node.getModifiers())) {
      abstractToken = token(Keyword.ABSTRACT);
    }
    // type parameters
    TypeParameterList typeParams = null;
    {
      List<TypeParameter> typeParameters = Lists.newArrayList();
      List<?> javaTypeParameters = node.typeParameters();
      if (!javaTypeParameters.isEmpty()) {
        for (Iterator<?> I = javaTypeParameters.iterator(); I.hasNext();) {
          org.eclipse.jdt.core.dom.TypeParameter javaTypeParameter = (org.eclipse.jdt.core.dom.TypeParameter) I.next();
          TypeParameter typeParameter = translate(javaTypeParameter);
          typeParameters.add(typeParameter);
        }
        typeParams = typeParameterList(typeParameters);
      }
    }
    // extends
    ExtendsClause extendsClause = null;
    if (node.getSuperclassType() != null) {
      TypeName superType = translate(node.getSuperclassType());
      extendsClause = extendsClause(superType);
    }
    // implements
    ImplementsClause implementsClause = null;
    if (!node.superInterfaceTypes().isEmpty()) {
      List<TypeName> interfaces = Lists.newArrayList();
      for (Object javaInterface : node.superInterfaceTypes()) {
        interfaces.add((TypeName) translate((org.eclipse.jdt.core.dom.ASTNode) javaInterface));
      }
      implementsClause = implementsClause(interfaces);
    }
    // members
    List<ClassMember> members = translateBodyDeclarations(node.bodyDeclarations());
    for (ClassMember member : members) {
      if (member instanceof ConstructorDeclaration) {
        ConstructorDeclaration constructor = (ConstructorDeclaration) member;
        constructor.setReturnType(name);
      }
    }
    //
    ClassDeclaration classDeclaration = new ClassDeclaration(
        translateJavadoc(node),
        null,
        abstractToken,
        null,
        name,
        typeParams,
        extendsClause,
        null,
        implementsClause,
        null,
        members,
        null);
    context.putNodeBinding(classDeclaration, binding);
    context.putNodeTypeBinding(classDeclaration, binding);
    context.putNodeTypeBinding(name, binding);
    return done(classDeclaration);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.TypeLiteral node) {
    org.eclipse.jdt.core.dom.Type javaType = node.getType();
    Identifier result = null;
    if (javaType instanceof org.eclipse.jdt.core.dom.SimpleType) {
      result = ((TypeName) translate(javaType)).getName();
    }
    return done(result);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.TypeParameter node) {
    SimpleIdentifier name = translateSimpleName(node.getName());
    TypeName bound = null;
    {
      List<?> typeBounds = node.typeBounds();
      if (typeBounds.size() == 1) {
        org.eclipse.jdt.core.dom.Type javaBound = (org.eclipse.jdt.core.dom.Type) typeBounds.get(0);
        bound = (TypeName) translate(javaBound);
      }
    }
    return done(typeParameter(name, bound));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.VariableDeclarationFragment node) {
    return done(variableDeclaration(
        translateSimpleName(node.getName()),
        translateExpression(node.getInitializer())));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.VariableDeclarationStatement node) {
    return done(variableDeclarationStatement(translateVariableDeclarationList(
        false,
        node.getType(),
        node.fragments())));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.WhileStatement node) {
    return done(whileStatement(
        translateExpression(node.getExpression()),
        (Statement) translate(node.getBody())));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.WildcardType node) {
    org.eclipse.jdt.core.dom.Type javaBoundType = node.getBound();
    if (javaBoundType == null) {
      return done(typeName("Object"));
    } else {
      return done(translate(javaBoundType));
    }
  }

  private ClassDeclaration declareInnerClass(IMethodBinding constructorBinding,
      AnonymousClassDeclaration anoClassDeclaration, String innerClassName,
      String[] additionalParameters) {
    ITypeBinding superTypeBinding = anoClassDeclaration.resolveBinding().getSuperclass();
    ExtendsClause extendsClause = null;
    ImplementsClause implementsClause = null;
    {
      ITypeBinding[] superInterfaces = anoClassDeclaration.resolveBinding().getInterfaces();
      if (superInterfaces.length != 0) {
        superTypeBinding = superInterfaces[0];
        TypeName superType = typeName(superInterfaces[0].getName());
        putReference(superTypeBinding, (SimpleIdentifier) superType.getName());
        implementsClause = implementsClause(superType);
      } else {
        TypeName superType = translateTypeName(superTypeBinding);
        putReference(superTypeBinding, (SimpleIdentifier) superType.getName());
        extendsClause = extendsClause(superType);
      }
    }
    ClassDeclaration innerClass = classDeclaration(
        null,
        identifier(innerClassName),
        extendsClause,
        null,
        implementsClause,
        null);
    artificialUnitDeclarations.add(innerClass);
    if (extendsClause != null) {
      List<FormalParameter> parameters = Lists.newArrayList();
      List<Expression> arguments = Lists.newArrayList();
      // find "super" constructor
      IMethodBinding superConstructor = null;
      for (IMethodBinding superMethod : superTypeBinding.getDeclaredMethods()) {
        if (superMethod.isConstructor()) {
          if (isSuperConstructor(superMethod, constructorBinding)) {
            superConstructor = superMethod;
            // additional parameters
            for (int i = 0; i < additionalParameters.length / 2; i++) {
              parameters.add(simpleFormalParameter(
                  null,
                  typeName(additionalParameters[2 * i + 0]),
                  additionalParameters[2 * i + 1]));
              arguments.add(identifier(additionalParameters[2 * i + 1]));
            }
            // "declared" parameters
            ITypeBinding[] parameterTypes = superMethod.getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
              TypeName dartParameterType = typeName(parameterTypes[i].getName());
              String parameterName = "arg" + i;
              parameters.add(simpleFormalParameter(dartParameterType, parameterName));
              arguments.add(identifier(parameterName));
            }
            // done, we found and processed "super" constructor
            break;
          }
        }
      }
      // declare "inner" constructor
      FormalParameterList parameterList = formalParameterList(parameters);
      ArgumentList argList = new ArgumentList(null, arguments, null);
      SuperConstructorInvocation superCI = new SuperConstructorInvocation(null, null, null, argList);
      context.getConstructorDescription(superConstructor).superInvocations.add(superCI);
      ConstructorDeclaration innerConstructor = constructorDeclaration(
          null,
          null,
          identifier(innerClassName),
          null,
          parameterList,
          ImmutableList.<ConstructorInitializer> of(superCI),
          emptyFunctionBody());
      innerClass.getMembers().add(innerConstructor);
    }
    for (Object javaBodyDeclaration : anoClassDeclaration.bodyDeclarations()) {
      ClassMember classMember = translate((org.eclipse.jdt.core.dom.ASTNode) javaBodyDeclaration);
      innerClass.getMembers().add(classMember);
    }
    return innerClass;
  }

  /**
   * Set {@link #result} and return <code>false</code> - we don't want normal JDT visiting.
   */
  private boolean done(ASTNode node) {
    result = node;
    return false;
  }

  private boolean isNumberOrNull(Expression expression) {
    if (expression instanceof IntegerLiteral || expression instanceof DoubleLiteral
        || expression instanceof NullLiteral) {
      return true;
    }
    ITypeBinding typeBinding = context.getNodeTypeBinding(expression);
    if (typeBinding != null) {
      String name = typeBinding.getName();
      return name.equals("char") || name.equals("short") || name.equals("int")
          || name.equals("long") || name.equals("float") || name.equals("double");
    }
    return false;
  }

  private void putReference(org.eclipse.jdt.core.dom.IBinding binding, SimpleIdentifier identifier) {
    if (binding != null) {
      binding = JavaUtils.getOriginalBinding(binding);
      context.putReference(identifier, binding, JavaUtils.getJdtSignature(binding));
    }
  }

  /**
   * Recursively translates given {@link org.eclipse.jdt.core.dom.ASTNode} to Dart {@link ASTNode}.
   * 
   * @return the corresponding Dart {@link ASTNode}, may be <code>null</code> if <code>null</code>
   *         argument was given; not <code>null</code> if argument is not <code>null</code> (if
   *         translation is not implemented, exception will be thrown).
   */
  @SuppressWarnings("unchecked")
  private <T extends ASTNode> T translate(final org.eclipse.jdt.core.dom.ASTNode node) {
    if (node == null) {
      return null;
    }
    ExecutionUtils.runRethrow(new RunnableEx() {
      @Override
      public void run() throws Exception {
        Method method = getMostSpecificMethod(node.getClass());
        try {
          method.invoke(SyntaxTranslator.this, node);
        } catch (InvocationTargetException e) {
          ExecutionUtils.propagate(e.getCause());
        }
      }
    });
    Assert.isNotNull(result, "No result for: " + node.getClass().getCanonicalName());
    T castedResult = (T) result;
    if (node instanceof org.eclipse.jdt.core.dom.Expression) {
      context.putNodeTypeBinding(
          result,
          ((org.eclipse.jdt.core.dom.Expression) node).resolveTypeBinding());
    }
    result = null;
    return castedResult;
  }

  /**
   * Translates given {@link List} of {@link org.eclipse.jdt.core.dom.Expression} to the Dart
   * {@link Expression} list.
   */
  private List<Expression> translateArguments(IMethodBinding binding, List<?> javaArguments) {
    List<Expression> arguments = translateExpressionList(javaArguments);
    // may be some of the arguments are var-args
    if (binding != null && binding.isVarargs()) {
      int numRequired = binding.getParameterTypes().length - 1;
      List<Expression> vars = Lists.newArrayList();
      for (int i = numRequired; i < arguments.size(); i++) {
        vars.add(arguments.get(i));
      }
      List<Expression> newArguments = Lists.newArrayList();
      newArguments.addAll(arguments.subList(0, numRequired));
      newArguments.add(new ListLiteral(null, null, null, vars, null));
      arguments = newArguments;
    }
    // done
    return arguments;
  }

  private List<ClassMember> translateBodyDeclarations(List<?> javaBodyDeclarations) {
    List<ClassMember> members = Lists.newArrayList();
    for (Iterator<?> I = javaBodyDeclarations.iterator(); I.hasNext();) {
      org.eclipse.jdt.core.dom.BodyDeclaration javaBodyDecl = (org.eclipse.jdt.core.dom.BodyDeclaration) I.next();
      constructorImpl = null;
      if (javaBodyDecl instanceof org.eclipse.jdt.core.dom.TypeDeclaration
          || javaBodyDecl instanceof org.eclipse.jdt.core.dom.EnumDeclaration) {
        ClassDeclaration innerClassDeclaration = translate(javaBodyDecl);
        artificialUnitDeclarations.add(innerClassDeclaration);
      } else {
        ClassMember member = translate(javaBodyDecl);
        members.add(member);
        if (constructorImpl != null) {
          members.add(constructorImpl);
        }
      }
    }
    return members;
  }

  private Expression translateExpression(Object o) {
    return (Expression) translate((org.eclipse.jdt.core.dom.ASTNode) o);
  }

  /**
   * Translates given {@link List} of {@link org.eclipse.jdt.core.dom.Expression} to the
   * {@link List} of {@link Expression}s.
   */
  private List<Expression> translateExpressionList(List<?> javaArguments) {
    List<Expression> arguments = Lists.newArrayList();
    for (Iterator<?> I = javaArguments.iterator(); I.hasNext();) {
      org.eclipse.jdt.core.dom.Expression javaArg = (org.eclipse.jdt.core.dom.Expression) I.next();
      Expression dartArg = translate(javaArg);
      arguments.add(dartArg);
    }
    return arguments;
  }

  private Comment translateJavadoc(org.eclipse.jdt.core.dom.BodyDeclaration node) {
    return (Comment) translate(node.getJavadoc());
  }

  private SimpleIdentifier translateSimpleName(org.eclipse.jdt.core.dom.SimpleName name) {
    return translate(name);
  }

  private TypeName translateTypeName(ITypeBinding binding) {
    if (binding != null) {
      if (binding.isArray()) {
        return typeName(identifier("List"), translateTypeName(binding.getComponentType()));
      }
      String name = binding.getName();
      name = StringUtils.substringBefore(name, "<");
      if (JavaUtils.isTypeNamed(binding, "java.util.ArrayList")) {
        name = "List";
      }
      if (JavaUtils.isTypeNamed(binding, "java.lang.Void")) {
        name = "Object";
      }
      if ("boolean".equals(name)) {
        return typeName("bool");
      }
      List<TypeName> arguments = Lists.newArrayList();
      for (ITypeBinding typeArgument : binding.getTypeArguments()) {
        arguments.add(translateTypeName(typeArgument));
      }
      TypeName result = typeName(identifier(name), arguments);
      context.putNodeTypeBinding(result, binding);
      context.putNodeTypeBinding(result.getName(), binding);
      putReference(binding, (SimpleIdentifier) result.getName());
      return result;
    }
    throw new IllegalArgumentException("" + binding);
  }

  /**
   * Translates given {@link List} of {@link org.eclipse.jdt.core.dom.Type} to the
   * {@link TypeArgumentList}.
   */
  private List<TypeName> translateTypeNames(List<?> javaTypes) {
    List<TypeName> typeNames = Lists.newArrayList();
    for (Iterator<?> I = javaTypes.iterator(); I.hasNext();) {
      org.eclipse.jdt.core.dom.Type javaType = (org.eclipse.jdt.core.dom.Type) I.next();
      TypeName dartType = translate(javaType);
      typeNames.add(dartType);
    }
    return typeNames;
  }

  /**
   * Translates given {@link List} of {@link org.eclipse.jdt.core.dom.VariableDeclarationFragment}
   * to the {@link VariableDeclarationList}.
   */
  private VariableDeclarationList translateVariableDeclarationList(boolean isFinal,
      org.eclipse.jdt.core.dom.Type javaType, List<?> javaVars) {
    List<VariableDeclaration> variableDeclarations = Lists.newArrayList();
    for (Iterator<?> I = javaVars.iterator(); I.hasNext();) {
      org.eclipse.jdt.core.dom.VariableDeclarationFragment javaFragment = (org.eclipse.jdt.core.dom.VariableDeclarationFragment) I.next();
      VariableDeclaration var = translate(javaFragment);
      variableDeclarations.add(var);
    }
    return variableDeclarationList(
        isFinal ? Keyword.FINAL : null,
        (TypeName) translate(javaType),
        variableDeclarations);
  }
}
