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

import com.google.common.collect.Lists;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.AsExpression;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.IntegerLiteral;
import com.google.dart.engine.ast.InterpolationElement;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.ParenthesizedExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.StringInterpolation;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.util.JavaUtils;

import static com.google.dart.java2dart.util.AstFactory.argumentList;
import static com.google.dart.java2dart.util.AstFactory.assignmentExpression;
import static com.google.dart.java2dart.util.AstFactory.binaryExpression;
import static com.google.dart.java2dart.util.AstFactory.booleanLiteral;
import static com.google.dart.java2dart.util.AstFactory.identifier;
import static com.google.dart.java2dart.util.AstFactory.instanceCreationExpression;
import static com.google.dart.java2dart.util.AstFactory.integer;
import static com.google.dart.java2dart.util.AstFactory.interpolationExpression;
import static com.google.dart.java2dart.util.AstFactory.interpolationString;
import static com.google.dart.java2dart.util.AstFactory.methodInvocation;
import static com.google.dart.java2dart.util.AstFactory.namedExpression;
import static com.google.dart.java2dart.util.AstFactory.prefixExpression;
import static com.google.dart.java2dart.util.AstFactory.propertyAccess;
import static com.google.dart.java2dart.util.AstFactory.string;
import static com.google.dart.java2dart.util.AstFactory.thisExpression;
import static com.google.dart.java2dart.util.AstFactory.typeName;
import static com.google.dart.java2dart.util.TokenFactory.token;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.List;

/**
 * {@link SemanticProcessor} for Java <code>Object</code>.
 */
public class ObjectSemanticProcessor extends SemanticProcessor {
  private static List<Expression> gatherBinaryExpressions(BinaryExpression binary) {
    List<Expression> expressions = Lists.newArrayList();
    {
      Expression left = binary.getLeftOperand();
      if (left instanceof BinaryExpression) {
        expressions.addAll(gatherBinaryExpressions((BinaryExpression) left));
      } else {
        expressions.add(left);
      }
    }
    {
      Expression right = binary.getRightOperand();
      expressions.add(right);
    }
    return expressions;
  }

  private static boolean hasStringObjects(Context context, List<Expression> expressions) {
    for (Expression expression : expressions) {
      ITypeBinding binding = context.getNodeTypeBinding(expression);
      if (JavaUtils.isTypeNamed(binding, "java.lang.String")) {
        return true;
      }
    }
    return false;
  }

  public ObjectSemanticProcessor(Context context) {
    super(context);
  }

  @Override
  public void process(CompilationUnit unit) {
    unit.accept(new GeneralizingAstVisitor<Void>() {
      @Override
      public Void visitAsExpression(AsExpression node) {
        super.visitAsExpression(node);
        Expression expression = node.getExpression();
        TypeName targetTypeName = node.getType();
        ITypeBinding expressionTypeBinding = context.getNodeTypeBinding(expression);
        ITypeBinding targetTypeBinding = context.getNodeTypeBinding(targetTypeName);
        if (JavaUtils.isTypeNamed(expressionTypeBinding, "double")) {
          if (JavaUtils.isTypeNamed(targetTypeBinding, "int")
              || JavaUtils.isTypeNamed(targetTypeBinding, "long")) {
            AstNode nodeToReplace = node;
            AstNode parent = node.getParent();
            if (parent instanceof ParenthesizedExpression) {
              nodeToReplace = parent;
            }
            replaceNode(nodeToReplace, methodInvocation(expression, identifier("toInt")));
            return null;
          }
        }
        return null;
      }

      @Override
      public Void visitAssignmentExpression(AssignmentExpression node) {
        super.visitAssignmentExpression(node);
        if (node.getOperator().getType() == TokenType.EQ) {
          Expression leftExpr = node.getLeftHandSide();
          Expression rightExpr = node.getRightHandSide();
          ITypeBinding leftBinding = context.getNodeTypeBinding(leftExpr);
          ITypeBinding rightBinding = context.getNodeTypeBinding(rightExpr);
          if (JavaUtils.isTypeNamed(leftBinding, "java.lang.CharSequence")
              && JavaUtils.isTypeNamed(rightBinding, "java.lang.String")) {
            node.setRightHandSide(instanceCreationExpression(
                Keyword.NEW,
                typeName("CharSequence"),
                rightExpr));
            return null;
          }
        }
        // Dart has no "bool |=" operator
        if (node.getOperator().getType() == TokenType.BAR_EQ) {
          Expression leftExpr = node.getLeftHandSide();
          ITypeBinding argTypeBinding = context.getNodeTypeBinding(leftExpr);
          if (JavaUtils.isTypeNamed(argTypeBinding, "boolean")) {
            Expression rightExpr = node.getRightHandSide();
            replaceNode(
                node,
                assignmentExpression(
                    leftExpr,
                    TokenType.EQ,
                    methodInvocation("javaBooleanOr", leftExpr, rightExpr)));
            return null;
          }
        }
        // Dart has no "bool &=" operator
        if (node.getOperator().getType() == TokenType.AMPERSAND_EQ) {
          Expression leftExpr = node.getLeftHandSide();
          ITypeBinding argTypeBinding = context.getNodeTypeBinding(leftExpr);
          if (JavaUtils.isTypeNamed(argTypeBinding, "boolean")) {
            Expression rightExpr = node.getRightHandSide();
            replaceNode(
                node,
                assignmentExpression(
                    leftExpr,
                    TokenType.EQ,
                    methodInvocation("javaBooleanAnd", leftExpr, rightExpr)));
            return null;
          }
        }
        // String += 'c'
        if (node.getOperator().getType() == TokenType.PLUS_EQ) {
          Expression leftExpr = node.getLeftHandSide();
          ITypeBinding argTypeBinding = context.getNodeTypeBinding(leftExpr);
          if (JavaUtils.isTypeNamed(argTypeBinding, "java.lang.String")) {
            Expression rightExpr = node.getRightHandSide();
            replaceCharWithString(rightExpr);
            return null;
          }
        }
        return null;
      }

      @Override
      public Void visitBinaryExpression(BinaryExpression node) {
        if (node.getOperator().getType() == TokenType.PLUS) {
          List<Expression> expressions = gatherBinaryExpressions(node);
          if (hasStringObjects(context, expressions)) {
            // try to rewrite each expression
            for (Expression expression : expressions) {
              expression.accept(this);
            }
            expressions = gatherBinaryExpressions(node);
            // compose interpolation using expressions
            List<InterpolationElement> elements = Lists.newArrayList();
            elements.add(interpolationString("\"", ""));
            for (Expression expression : expressions) {
              if (expression instanceof SimpleStringLiteral) {
                SimpleStringLiteral literal = (SimpleStringLiteral) expression;
                String value = literal.getValue();
                String lexeme = StringUtils.strip(literal.getLiteral().getLexeme(), "\"");
                elements.add(interpolationString(lexeme, value));
              } else if (expression instanceof IntegerLiteral
                  && JavaUtils.isTypeNamed(context.getNodeTypeBinding(expression), "char")) {
                String value = "" + (char) ((IntegerLiteral) expression).getValue().intValue();
                elements.add(interpolationString(value, value));
              } else if (JavaUtils.isTypeNamed(context.getNodeTypeBinding(expression), "char")) {
                InstanceCreationExpression newString = instanceCreationExpression(
                    Keyword.NEW,
                    typeName("String"),
                    "fromCharCode",
                    expression);
                elements.add(interpolationExpression(newString));
              } else {
                elements.add(interpolationExpression(expression));
              }
            }
            elements.add(interpolationString("\"", ""));
            StringInterpolation interpolation = string(elements);
            replaceNode(node, interpolation);
            return null;
          }
        }
        // in Java "true | false" will compute both operands
        if (node.getOperator().getType() == TokenType.BAR) {
          ITypeBinding argTypeBinding = context.getNodeTypeBinding(node.getLeftOperand());
          super.visitBinaryExpression(node);
          if (JavaUtils.isTypeNamed(argTypeBinding, "boolean")) {
            replaceNode(
                node,
                methodInvocation("javaBooleanOr", node.getLeftOperand(), node.getRightOperand()));
            return null;
          }
        }
        // in Java "true & false" will compute both operands
        if (node.getOperator().getType() == TokenType.AMPERSAND) {
          ITypeBinding argTypeBinding = context.getNodeTypeBinding(node.getLeftOperand());
          super.visitBinaryExpression(node);
          if (JavaUtils.isTypeNamed(argTypeBinding, "boolean")) {
            replaceNode(
                node,
                methodInvocation("javaBooleanAnd", node.getLeftOperand(), node.getRightOperand()));
            return null;
          }
        }
        // super
        super.visitBinaryExpression(node);
        // done
        return null;
      }

      @Override
      public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
        super.visitInstanceCreationExpression(node);
        ITypeBinding typeBinding = context.getNodeTypeBinding(node);
        IMethodBinding binding = (IMethodBinding) context.getNodeBinding(node);
        List<Expression> args = node.getArgumentList().getArguments();
        String typeSimpleName = node.getConstructorName().getType().getName().getName();
        if (JavaUtils.isTypeNamed(typeBinding, "java.lang.StringBuilder")) {
          args.clear();
          return null;
        }
        ArgumentList newArguments = fixConstructorArguments(args, binding);
        if (newArguments != null) {
          node.setArgumentList(newArguments);
        } else if (typeSimpleName.equals("int")) {
          if (args.size() == 1) {
            replaceNode(node, methodInvocation(identifier("int"), "parse", args.get(0)));
          } else {
            replaceNode(
                node,
                methodInvocation(
                    identifier("int"),
                    "parse",
                    args.get(0),
                    namedExpression("radix", args.get(1))));
          }
        }
        return null;
      }

      @Override
      public Void visitMethodDeclaration(MethodDeclaration node) {
        if (node.getName() instanceof SimpleIdentifier) {
          String name = node.getName().getName();
          if (name.equals("hashCode")) {
            node.setOperatorKeyword(token(Keyword.GET));
            node.setParameters(null);
          }
          if (name.equals("equals") && node.getParameters().getParameters().size() == 1) {
            node.setOperatorKeyword(token(Keyword.OPERATOR));
            node.setName(identifier("=="));
          }
        }
        return super.visitMethodDeclaration(node);
      }

      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        super.visitMethodInvocation(node);
        SimpleIdentifier nameNode = node.getMethodName();
        String name = nameNode.getName();
        Expression target = node.getTarget();
        NodeList<Expression> args = node.getArgumentList().getArguments();
        // System -> JavaSystem
        if (target instanceof SimpleIdentifier) {
          SimpleIdentifier targetIdentifier = (SimpleIdentifier) target;
          if (targetIdentifier.getName().equals("System")) {
            targetIdentifier.setToken(token("JavaSystem"));
          }
          if (isMethodInClass(node, "getProperty", "java.lang.System")
              || isMethodInClass(node, "getenv", "java.lang.System")) {
            targetIdentifier.setToken(token("JavaSystemIO"));
          }
        }
        //
        if (args.isEmpty()) {
          if ("hashCode".equals(name) || isMethodInClass(node, "length", "java.lang.String")
              || isMethodInClass(node, "length", "java.lang.CharSequence")
              || isMethodInClass(node, "isEmpty", "java.lang.String")
              || isMethodInClass(node, "name", "java.lang.Enum")
              || isMethodInClass(node, "ordinal", "java.lang.Enum")
              || isMethodInClass(node, "values", "java.lang.Enum")
              || isMethodInClass(node, "bitLength", "java.math.BigInteger")) {
            replaceNode(node, propertyAccess(target, nameNode));
            return null;
          }
          if ("getClass".equals(name)) {
            replaceNode(node, propertyAccess(target, "runtimeType"));
            return null;
          }
          if (isMethodInClass(node, "getName", "java.lang.Class")
              || isMethodInClass(node, "getSimpleName", "java.lang.Class")) {
            nameNode.setToken(token("toString"));
            return null;
          }
        }
        if (name.equals("equals") && args.size() == 1) {
          AstNode parent = node.getParent();
          if (target == null) {
            target = thisExpression();
          }
          if (parent instanceof PrefixExpression
              && ((PrefixExpression) parent).getOperator().getType() == TokenType.BANG) {
            replaceNode(parent, binaryExpression(target, TokenType.BANG_EQ, args.get(0)));
          } else {
            if (isMethodInClass(node, "equals", "java.util.Set")) {
              replaceNode(node, methodInvocation("javaSetEquals", target, args.get(0)));
            } else {
              replaceNode(node, binaryExpression(target, TokenType.EQ_EQ, args.get(0)));
            }
          }
          return null;
        }
        if (name.equals("clone")) {
          ITypeBinding targetBinding = context.getNodeTypeBinding(target);
          if (targetBinding.isArray()) {
            ITypeBinding componentType = targetBinding.getComponentType();
            if (componentType.isTypeVariable()) {
              componentType = componentType.getTypeBounds()[0];
            }
            replaceNode(
                node,
                instanceCreationExpression(
                    Keyword.NEW,
                    typeName("List", typeName(componentType.getName())),
                    "from",
                    target));
            return null;
          }
        }
        // prepare binding
        IMethodBinding binding = (IMethodBinding) context.getNodeBinding(node);
        // analyze invocations
        if (isMethodInClass(node, "getMessage", "java.lang.Throwable")) {
          nameNode.setToken(token("toString"));
          return null;
        }
        if (isMethodInClass(node, "getCause", "java.lang.Throwable")) {
          replaceNode(node, propertyAccess(target, "cause"));
          return null;
        }
        if (isMethodInClass(node, "printStackTrace", "java.lang.Throwable")) {
          replaceNode(node, methodInvocation("print", target));
          return null;
        }
        if (isMethodInClass(node, "isInstance", "java.lang.Class")) {
          replaceNode(node, methodInvocation("isInstanceOf", args.get(0), target));
          return null;
        }
        if (isMethodInClass(node, "charAt", "java.lang.String")) {
          nameNode.setToken(token("codeUnitAt"));
          return null;
        }
        if (isMethodInClass2(node, "replace(char,char)", "java.lang.String")) {
          nameNode.setToken(token("replaceAll"));
          replaceCharWithString(args.get(0));
          replaceCharWithString(args.get(1));
          return null;
        }
        if (isMethodInClass2(
            node,
            "replace(java.lang.CharSequence,java.lang.CharSequence)",
            "java.lang.String")) {
          nameNode.setToken(token("replaceAll"));
          return null;
        }
        if (isMethodInClass2(node, "contains(java.lang.CharSequence)", "java.lang.String")) {
          return null;
        }
        if (isMethodInClass(node, "equalsIgnoreCase", "java.lang.String")) {
          replaceNode(node, methodInvocation("javaStringEqualsIgnoreCase", target, args.get(0)));
          return null;
        }
        if (isMethodInClass(node, "regionMatches", "java.lang.String")) {
          replaceNode(
              node,
              methodInvocation(
                  "javaStringRegionMatches",
                  target,
                  args.get(0),
                  args.get(1),
                  args.get(2),
                  args.get(3)));
          return null;
        }
        if (isMethodInClass(node, "indexOf", "java.lang.String")
            || isMethodInClass(node, "lastIndexOf", "java.lang.String")) {
          replaceCharWithString(args.get(0));
          if (args.size() == 2) {
            replaceNode(
                node,
                methodInvocation(identifier("JavaString"), name, target, args.get(0), args.get(1)));
          }
          return null;
        }
        if (isMethodInClass2(node, "valueOf(char)", "java.lang.String")) {
          replaceNode(
              node,
              instanceCreationExpression(
                  Keyword.NEW,
                  typeName("String"),
                  "fromCharCode",
                  args.get(0)));
          return null;
        }
        if (isMethodInClass2(node, "concat(java.lang.String)", "java.lang.String")) {
          replaceNode(node, binaryExpression(target, TokenType.PLUS, args.get(0)));
          return null;
        }
        if (isMethodInClass(node, "print", "java.io.PrintWriter")) {
          if (binding != null && binding.getParameterTypes().length >= 1
              && binding.getParameterTypes()[0].getName().equals("char")) {
            char c = (char) ((IntegerLiteral) args.get(0)).getValue().intValue();
            replaceNode(args.get(0), string("" + c));
          }
          return null;
        }
        if (isMethodInClass2(node, "println()", "java.io.PrintWriter")) {
          nameNode.setToken(token("newLine"));
          return null;
        }
        if (isMethodInClass2(node, "println(java.lang.String)", "java.io.PrintWriter")) {
          nameNode.setToken(token("println"));
          return null;
        }
        if (isMethodInClass2(node, "startsWith(java.lang.String,int)", "java.lang.String")) {
          replaceNode(
              node,
              methodInvocation(
                  identifier("JavaString"),
                  "startsWithBefore",
                  target,
                  args.get(0),
                  args.get(1)));
          return null;
        }
        if (isMethodInClass(node, "format", "java.lang.String")) {
          replaceNode(target, identifier("JavaString"));
          return null;
        }
        if (isMethodInClass(node, "charAt", "java.lang.CharSequence")) {
          nameNode.setToken(token("codeUnitAt"));
          return null;
        }
        if (isMethodInClass(node, "subSequence", "java.lang.CharSequence")) {
          nameNode.setToken(token("substring"));
          return null;
        }
        if (isMethodInClass(node, "compile", "java.util.regex.Pattern")) {
          replaceNode(
              node,
              instanceCreationExpression(Keyword.NEW, typeName("RegExp"), args.get(0)));
          return null;
        }
        if (isMethodInClass(node, "matcher", "java.util.regex.Pattern")) {
          replaceNode(
              node,
              instanceCreationExpression(
                  Keyword.NEW,
                  typeName("JavaPatternMatcher"),
                  target,
                  args.get(0)));
          return null;
        }
        if (name.equals("longValue") && target instanceof MethodInvocation) {
          MethodInvocation node2 = (MethodInvocation) target;
          if (isMethodInClass(node2, "floor", "java.lang.Math")) {
            NodeList<Expression> args2 = node2.getArgumentList().getArguments();
            if (args2.size() == 1 && args2.get(0) instanceof BinaryExpression) {
              BinaryExpression binary = (BinaryExpression) args2.get(0);
              if (binary.getOperator().getType() == TokenType.SLASH) {
                replaceNode(
                    node,
                    binaryExpression(
                        binary.getLeftOperand(),
                        TokenType.TILDE_SLASH,
                        binary.getRightOperand()));
                return null;
              }
            }
          }
        }
        if (isMethodInClass(node, "valueOf", "java.lang.Integer")
            || isMethodInClass(node, "valueOf", "java.lang.Long")
            || isMethodInClass(node, "valueOf", "java.lang.Double")
            || isMethodInClass(node, "valueOf", "java.math.BigInteger")) {
          replaceNode(node, args.get(0));
          return null;
        }
        if (isMethodInClass(node, "parseInt", "java.lang.Integer")) {
          node.setTarget(identifier("int"));
          nameNode.setToken(token("parse"));
          if (args.size() == 2) {
            args.set(1, namedExpression("radix", args.get(1)));
          }
          return null;
        }
        if (isMethodInClass(node, "parseDouble", "java.lang.Double")) {
          node.setTarget(identifier("double"));
          nameNode.setToken(token("parse"));
          return null;
        }
        if (isMethodInClass2(node, "toString(double)", "java.lang.Double")
            || isMethodInClass2(node, "toString(int)", "java.lang.Integer")
            || isMethodInClass2(node, "toString(long)", "java.lang.Long")) {
          replaceNode(node, methodInvocation(args.get(0), "toString"));
          return null;
        }
        if (isMethodInClass2(node, "toString(int,int)", "java.lang.Integer")
            || isMethodInClass2(node, "toString(long,int)", "java.lang.Long")) {
          replaceNode(node, methodInvocation(args.get(0), "toRadixString", args.get(1)));
          return null;
        }
        if (isMethodInClass(node, "booleanValue", "java.lang.Boolean")
            || isMethodInClass(node, "doubleValue", "java.lang.Double")
            || isMethodInClass(node, "intValue", "java.lang.Integer")
            || isMethodInClass(node, "longValue", "java.lang.Long")
            || isMethodInClass(node, "intValue", "java.math.BigInteger")) {
          replaceNode(node, target);
          return null;
        }
        if (isMethodInClass(node, "intValue", "java.lang.Number")) {
          nameNode.setToken(token("toInt"));
          return null;
        }
        if (isMethodInClass(node, "doubleValue", "java.math.BigInteger")) {
          nameNode.setToken(token("toDouble"));
          return null;
        }
        {
          TokenType tokenType = null;
          if (isMethodInClass(node, "and", "java.math.BigInteger")) {
            tokenType = TokenType.AMPERSAND;
          } else if (isMethodInClass(node, "or", "java.math.BigInteger")) {
            tokenType = TokenType.BAR;
          } else if (isMethodInClass(node, "xor", "java.math.BigInteger")) {
            tokenType = TokenType.CARET;
          } else if (isMethodInClass(node, "add", "java.math.BigInteger")) {
            tokenType = TokenType.PLUS;
          } else if (isMethodInClass(node, "subtract", "java.math.BigInteger")) {
            tokenType = TokenType.MINUS;
          } else if (isMethodInClass(node, "multiply", "java.math.BigInteger")) {
            tokenType = TokenType.STAR;
          } else if (isMethodInClass(node, "divide", "java.math.BigInteger")) {
            tokenType = TokenType.TILDE_SLASH;
          } else if (isMethodInClass(node, "shiftLeft", "java.math.BigInteger")) {
            tokenType = TokenType.LT_LT;
          } else if (isMethodInClass(node, "shiftRight", "java.math.BigInteger")) {
            tokenType = TokenType.GT_GT;
          }
          if (tokenType != null) {
            replaceNode(node, binaryExpression(target, tokenType, args.get(0)));
            return null;
          }
        }
        {
          TokenType tokenType = null;
          if (isMethodInClass(node, "not", "java.math.BigInteger")) {
            tokenType = TokenType.TILDE;
          } else if (isMethodInClass(node, "negate", "java.math.BigInteger")) {
            tokenType = TokenType.MINUS;
          }
          if (tokenType != null) {
            replaceNode(node, prefixExpression(tokenType, target));
            return null;
          }
        }
        if (isMethodInClass2(node, "append(char)", "java.lang.StringBuilder")) {
          replaceNode(nameNode, identifier("appendChar"));
          return null;
        }
        if (isMethodInClass(node, "length", "java.lang.AbstractStringBuilder")) {
          replaceNode(node, propertyAccess(target, nameNode));
          return null;
        }
        if (isMethodInClass(node, "setLength", "java.lang.AbstractStringBuilder")) {
          nameNode.setToken(token("length"));
          replaceNode(
              node,
              assignmentExpression(propertyAccess(target, nameNode), TokenType.EQ, args.get(0)));
          return null;
        }
        return null;
      }

      @Override
      public Void visitPropertyAccess(PropertyAccess node) {
        Expression target = node.getTarget();
        ITypeBinding targetTypeBinding = context.getNodeTypeBinding(target);
        if (target instanceof SimpleIdentifier) {
          String targetName = ((SimpleIdentifier) target).getName();
          if (targetName.equals("Boolean")) {
            boolean value = node.getPropertyName().getName().equals("TRUE");
            replaceNode(node, booleanLiteral(value));
            return null;
          }
          if (targetName.equals("Integer")) {
            int value;
            if (node.getPropertyName().getName().equals("MIN_VALUE")) {
              value = Integer.MIN_VALUE;
            } else {
              value = Integer.MAX_VALUE;
            }
            replaceNode(node, integer(value));
            return null;
          }
          if (JavaUtils.isTypeNamed(targetTypeBinding, "java.math.BigInteger")) {
            if (node.getPropertyName().getName().equals("ZERO")) {
              replaceNode(node, integer(0));
              return null;
            }
          }
        }
        return super.visitPropertyAccess(node);
      }

      @Override
      public Void visitSuperConstructorInvocation(SuperConstructorInvocation node) {
        super.visitSuperConstructorInvocation(node);
        NodeList<Expression> args = node.getArgumentList().getArguments();
        IMethodBinding binding = (IMethodBinding) context.getNodeBinding(node);
        if (isMethodInExactClass(binding, "<init>(java.lang.Throwable)", "java.lang.Exception")) {
          node.setConstructorName(identifier("withCause"));
        }
        ArgumentList newArguments = fixConstructorArguments(args, binding);
        if (newArguments != null) {
          node.setArgumentList(newArguments);
        }
        return null;
      }

      @Override
      public Void visitTypeName(TypeName node) {
        ITypeBinding typeBinding = (ITypeBinding) context.getNodeBinding(node);
        // replace by name
        if (node.getName() instanceof SimpleIdentifier) {
          SimpleIdentifier nameNode = (SimpleIdentifier) node.getName();
          String name = nameNode.getName();
          // Exception -> JavaException
          if (JavaUtils.isTypeNamed(typeBinding, "java.lang.Exception")) {
            replaceNode(nameNode, identifier("JavaException"));
          }
          if (JavaUtils.isTypeNamed(typeBinding, "java.lang.Throwable")) {
            replaceNode(nameNode, identifier("Exception"));
          }
          if (JavaUtils.isTypeNamed(typeBinding, "java.lang.IndexOutOfBoundsException")) {
            replaceNode(nameNode, identifier("RangeError"));
          }
          if (JavaUtils.isTypeNamed(typeBinding, "java.lang.NumberFormatException")) {
            replaceNode(nameNode, identifier("FormatException"));
          }
          // StringBuilder -> JavaStringBuilder
          if (name.equals("StringBuilder")) {
            replaceNode(nameNode, identifier("JavaStringBuilder"));
          }
          if (name.equals("CharSequence")) {
            replaceNode(nameNode, identifier("String"));
          }
          // java.util.regex.*
          if (JavaUtils.isTypeNamed(typeBinding, "java.util.regex.Pattern")) {
            replaceNode(nameNode, identifier("RegExp"));
          }
          if (JavaUtils.isTypeNamed(typeBinding, "java.util.regex.Matcher")) {
            replaceNode(nameNode, identifier("JavaPatternMatcher"));
          }
          // Class<T> -> Type
          if (name.equals("Class")) {
            replaceNode(node, typeName("Type"));
          }
        }
        // done
        return super.visitTypeName(node);
      }

      @Override
      public Void visitVariableDeclaration(VariableDeclaration node) {
        super.visitVariableDeclaration(node);
        Expression initializer = node.getInitializer();
        ITypeBinding leftBinding = context.getNodeTypeBinding(node);
        ITypeBinding rightBinding = context.getNodeTypeBinding(initializer);
        if (JavaUtils.isTypeNamed(leftBinding, "java.lang.CharSequence")
            && JavaUtils.isTypeNamed(rightBinding, "java.lang.String")) {
          node.setInitializer(instanceCreationExpression(
              Keyword.NEW,
              typeName("CharSequence"),
              initializer));
          return null;
        }
        return null;
      }

      private ArgumentList fixConstructorArguments(List<Expression> args, IMethodBinding binding) {
        if (isMethodInExactClass(
            binding,
            "<init>(java.lang.Throwable)",
            "java.lang.RuntimeException")) {
          return argumentList(namedExpression("cause", args.get(0)));
        }
        if (isMethodInExactClass(binding, "<init>(java.lang.String)", "java.lang.RuntimeException")) {
          return argumentList(namedExpression("message", args.get(0)));
        }
        if (isMethodInExactClass(
            binding,
            "<init>(java.lang.String,java.lang.Throwable)",
            "java.lang.RuntimeException")) {
          return argumentList(
              namedExpression("message", args.get(0)),
              namedExpression("cause", args.get(0)));
        }
        return null;
      }

      private void replaceCharWithString(Expression x) {
        // should by 'char'
        ITypeBinding typeBinding = context.getNodeTypeBinding(x);
        if (!JavaUtils.isTypeNamed(typeBinding, "char")) {
          return;
        }
        // replace literal
        if (x instanceof IntegerLiteral) {
          IntegerLiteral literal = (IntegerLiteral) x;
          String str = String.valueOf((char) literal.getValue().intValue());
          if (str.equals("\\")) {
            str = "\\\\";
          }
          replaceNode(x, string(str));
          return;
        }
        // replace expression
        SimpleIdentifier placeholder = identifier("ph");
        InstanceCreationExpression newString = instanceCreationExpression(
            Keyword.NEW,
            typeName("String"),
            "fromCharCode",
            placeholder);
        replaceNode(x, newString);
        replaceNode(placeholder, x);
      }
    });
  }
}
