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

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.IntegerLiteral;
import com.google.dart.engine.ast.InterpolationElement;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.StringInterpolation;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.util.JavaUtils;

import static com.google.dart.java2dart.util.ASTFactory.binaryExpression;
import static com.google.dart.java2dart.util.ASTFactory.booleanLiteral;
import static com.google.dart.java2dart.util.ASTFactory.identifier;
import static com.google.dart.java2dart.util.ASTFactory.integer;
import static com.google.dart.java2dart.util.ASTFactory.interpolationExpression;
import static com.google.dart.java2dart.util.ASTFactory.interpolationString;
import static com.google.dart.java2dart.util.ASTFactory.methodInvocation;
import static com.google.dart.java2dart.util.ASTFactory.namedExpression;
import static com.google.dart.java2dart.util.ASTFactory.prefixExpression;
import static com.google.dart.java2dart.util.ASTFactory.propertyAccess;
import static com.google.dart.java2dart.util.ASTFactory.simpleIdentifier;
import static com.google.dart.java2dart.util.ASTFactory.string;
import static com.google.dart.java2dart.util.ASTFactory.typeName;
import static com.google.dart.java2dart.util.TokenFactory.token;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.List;

/**
 * {@link SemanticProcessor} for Java <code>Object</code>.
 */
public class ObjectSemanticProcessor extends SemanticProcessor {
  public static final SemanticProcessor INSTANCE = new ObjectSemanticProcessor();

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

  private static boolean hasStringLiteral(List<Expression> expressions) {
    for (Expression expression : expressions) {
      if (expression instanceof SimpleStringLiteral) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void process(final Context context, CompilationUnit unit) {
    unit.accept(new GeneralizingASTVisitor<Void>() {
      @Override
      public Void visitBinaryExpression(BinaryExpression node) {
        if (node.getOperator().getType() == TokenType.PLUS) {
          List<Expression> expressions = gatherBinaryExpressions(node);
          if (hasStringLiteral(expressions)) {
            List<InterpolationElement> elements = Lists.newArrayList();
            elements.add(interpolationString("\"", ""));
            for (Expression expression : expressions) {
              if (expression instanceof SimpleStringLiteral) {
                String value = ((SimpleStringLiteral) expression).getValue();
                elements.add(interpolationString(value, value));
              } else {
                elements.add(interpolationExpression(expression));
              }
            }
            elements.add(interpolationString("\"", ""));
            StringInterpolation interpolation = string(elements);
            replaceNode(node, interpolation);
          }
        }
        return super.visitBinaryExpression(node);
      }

      @Override
      public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
        List<Expression> args = node.getArgumentList().getArguments();
        if (node.getConstructorName().getType().getName().getName().equals("int")) {
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
        return super.visitInstanceCreationExpression(node);
      }

      @Override
      public Void visitMethodDeclaration(MethodDeclaration node) {
        if (node.getName() instanceof SimpleIdentifier) {
          String name = ((SimpleIdentifier) node.getName()).getName();
          if (name.equals("hashCode")) {
            node.setOperatorKeyword(token(Keyword.GET));
            node.setParameters(null);
          }
        }
        return super.visitMethodDeclaration(node);
      }

      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        super.visitMethodInvocation(node);
        SimpleIdentifier nameNode = node.getMethodName();
        String name = nameNode.getName();
        NodeList<Expression> args = node.getArgumentList().getArguments();
        if ("hashCode".equals(name) || isMethodInClass(node, "length", "java.lang.String")
            || isMethodInClass(node, "values", "java.lang.Enum")) {
          replaceNode(node, propertyAccess(node.getTarget(), nameNode));
          return null;
        }
        if (isMethodInClass(node, "charAt", "java.lang.String")) {
          nameNode.setToken(token("charCodeAt"));
          return null;
        }
        if (isMethodInClass(node, "indexOf", "java.lang.String")) {
          IMethodBinding binding = (IMethodBinding) context.getNodeBinding(node);
          if (binding != null && binding.getParameterTypes().length >= 1
              && binding.getParameterTypes()[0].getName().equals("int")) {
            char c = (char) ((IntegerLiteral) args.get(0)).getValue().intValue();
            replaceNode(args.get(0), string("" + c));
          }
          return null;
        }
        if (isMethodInClass(node, "print", "java.io.PrintWriter")) {
          IMethodBinding binding = (IMethodBinding) context.getNodeBinding(node);
          if (binding != null && binding.getParameterTypes().length >= 1
              && binding.getParameterTypes()[0].getName().equals("char")) {
            char c = (char) ((IntegerLiteral) args.get(0)).getValue().intValue();
            replaceNode(args.get(0), string("" + c));
          }
          return null;
        }
        if (isMethodInClass(node, "format", "java.lang.String")) {
          replaceNode(node.getTarget(), simpleIdentifier("JavaString"));
          return null;
        }
        if (name.equals("longValue") && node.getTarget() instanceof MethodInvocation) {
          MethodInvocation node2 = (MethodInvocation) node.getTarget();
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
            || isMethodInClass(node, "valueOf", "java.lang.Double")
            || isMethodInClass(node, "valueOf", "java.math.BigInteger")) {
          replaceNode(node, args.get(0));
          return null;
        }
        if (isMethodInClass(node, "parseDouble", "java.lang.Double")) {
          replaceNode(node, methodInvocation(identifier("double"), "parse", args.get(0)));
          return null;
        }
        if (isMethodInClass(node, "toString", "java.lang.Integer")) {
          replaceNode(node, methodInvocation(args.get(0), "toString"));
          return null;
        }
        if (isMethodInClass(node, "booleanValue", "java.lang.Boolean")
            || isMethodInClass(node, "doubleValue", "java.lang.Double")
            || isMethodInClass(node, "intValue", "java.math.BigInteger")) {
          replaceNode(node, node.getTarget());
          return null;
        }
        if (isMethodInClass(node, "equals", "java.lang.Double")
            || isMethodInClass(node, "equals", "java.math.BigInteger")
            || isMethodInClass(node, "equals", "java.lang.String")) {
          ASTNode parent = node.getParent();
          if (parent instanceof PrefixExpression
              && ((PrefixExpression) parent).getOperator().getType() == TokenType.BANG) {
            replaceNode(parent, binaryExpression(node.getTarget(), TokenType.BANG_EQ, args.get(0)));
          } else {
            replaceNode(node, binaryExpression(node.getTarget(), TokenType.EQ_EQ, args.get(0)));
          }
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
            tokenType = TokenType.SLASH;
          } else if (isMethodInClass(node, "shiftLeft", "java.math.BigInteger")) {
            tokenType = TokenType.LT_LT;
          } else if (isMethodInClass(node, "shiftRight", "java.math.BigInteger")) {
            tokenType = TokenType.GT_GT;
          }
          if (tokenType != null) {
            replaceNode(node, binaryExpression(node.getTarget(), tokenType, args.get(0)));
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
            replaceNode(node, prefixExpression(tokenType, node.getTarget()));
            return null;
          }
        }
        if (isMethodInClass(node, "append", "java.lang.StringBuilder")) {
          replaceNode(nameNode, simpleIdentifier("add"));
          return null;
        }
        return null;
      }

      @Override
      public Void visitPropertyAccess(PropertyAccess node) {
        if (node.getTarget() instanceof SimpleIdentifier) {
          String targetName = ((SimpleIdentifier) node.getTarget()).getName();
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
        }
        return super.visitPropertyAccess(node);
      }

      @Override
      public Void visitTypeName(TypeName node) {
        // in Dart we cannot use separate type parameters for methods, so we replace
        // them with type bounds
        if (getAncestor(node, MethodDeclaration.class) != null) {
          Object binding = context.getNodeBinding(node);
          if (binding instanceof ITypeBinding) {
            ITypeBinding typeBinding = (ITypeBinding) binding;
            if (typeBinding.isTypeVariable() && typeBinding.getDeclaringMethod() != null) {
              replaceNode(node, typeName(typeBinding.getErasure().getName()));
            }
          }
        }
        // StringBuilder -> StringBuffer
        if (node.getName() instanceof SimpleIdentifier) {
          SimpleIdentifier nameNode = (SimpleIdentifier) node.getName();
          String name = nameNode.getName();
          if (name.equals("StringBuilder")) {
            replaceNode(nameNode, simpleIdentifier("StringBuffer"));
          }
        }
        // done
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
