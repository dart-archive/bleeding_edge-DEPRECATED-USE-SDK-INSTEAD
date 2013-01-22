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
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.BooleanLiteral;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.IntegerLiteral;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.StringToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.util.ASTFactory;
import com.google.dart.java2dart.util.JavaUtils;

/**
 * {@link SemanticProcessor} for Java <code>Object</code>.
 */
public class ObjectSemanticProcessor extends SemanticProcessor {
  public static final SemanticProcessor INSTANCE = new ObjectSemanticProcessor();

  @Override
  public void process(final Context context, CompilationUnit unit) {
    unit.accept(new GeneralizingASTVisitor<Void>() {
      @Override
      public Void visitMethodDeclaration(MethodDeclaration node) {
        if (node.getName() instanceof SimpleIdentifier) {
          String name = ((SimpleIdentifier) node.getName()).getName();
          if (name.equals("hashCode")) {
            node.setOperatorKeyword(new KeywordToken(Keyword.GET, 0));
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
          Token period = node.getTarget() != null ? new Token(TokenType.PERIOD, 0) : null;
          replaceNode(node, new PropertyAccess(node.getTarget(), period, nameNode));
          return null;
        }
        if (isMethodInClass(node, "charAt", "java.lang.String")) {
          nameNode.setToken(ASTFactory.identifierToken("charCodeAt"));
          return null;
        }
        if (isMethodInClass(node, "format", "java.lang.String")) {
          replaceNode(node.getTarget(), ASTFactory.simpleIdentifier("JavaString"));
          return null;
        }
        if (name.equals("longValue") && node.getTarget() instanceof MethodInvocation) {
          MethodInvocation node2 = (MethodInvocation) node.getTarget();
          if (isMethodInClass(node2, "floor", "java.lang.Math")) {
            NodeList<Expression> args2 = node2.getArgumentList().getArguments();
            if (args2.size() == 1 && args2.get(0) instanceof BinaryExpression) {
              BinaryExpression binary = (BinaryExpression) args2.get(0);
              if (binary.getOperator().getType() == TokenType.SLASH) {
                replaceNode(node, new BinaryExpression(binary.getLeftOperand(), new Token(
                    TokenType.TILDE_SLASH,
                    0), binary.getRightOperand()));
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
            replaceNode(parent, new BinaryExpression(node.getTarget(), new Token(
                TokenType.BANG_EQ,
                0), args.get(0)));
          } else {
            replaceNode(node, new BinaryExpression(
                node.getTarget(),
                new Token(TokenType.EQ_EQ, 0),
                args.get(0)));
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
            replaceNode(
                node,
                new BinaryExpression(node.getTarget(), new Token(tokenType, 0), args.get(0)));
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
            replaceNode(node, new PrefixExpression(new Token(tokenType, 0), node.getTarget()));
            return null;
          }
        }
        if (isMethodInClass(node, "append", "java.lang.StringBuilder")) {
          replaceNode(nameNode, ASTFactory.simpleIdentifier("add"));
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
            Token token = value ? new KeywordToken(Keyword.TRUE, 0) : new KeywordToken(
                Keyword.FALSE,
                0);
            replaceNode(node, new BooleanLiteral(token, value));
            return null;
          }
          if (targetName.equals("Integer")) {
            int value;
            if (node.getPropertyName().getName().equals("MIN_VALUE")) {
              value = Integer.MIN_VALUE;
            } else {
              value = Integer.MAX_VALUE;
            }
            replaceNode(node, new IntegerLiteral(
                new StringToken(TokenType.INT, "" + value, 0),
                value));
            return null;
          }
        }
        return super.visitPropertyAccess(node);
      }

      @Override
      public Void visitTypeName(TypeName node) {
        if (node.getName() instanceof SimpleIdentifier) {
          SimpleIdentifier nameNode = (SimpleIdentifier) node.getName();
          String name = nameNode.getName();
          if (name.equals("StringBuilder")) {
            replaceNode(nameNode, ASTFactory.simpleIdentifier("StringBuffer"));
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
