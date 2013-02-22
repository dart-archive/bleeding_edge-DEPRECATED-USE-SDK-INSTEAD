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
package com.google.dart.engine.ast;

import com.google.dart.engine.parser.ParserTestCase;
import com.google.dart.engine.scanner.TokenType;

import static com.google.dart.engine.ast.ASTFactory.assignmentExpression;
import static com.google.dart.engine.ast.ASTFactory.binaryExpression;
import static com.google.dart.engine.ast.ASTFactory.identifier;
import static com.google.dart.engine.ast.ASTFactory.postfixExpression;
import static com.google.dart.engine.ast.ASTFactory.prefixExpression;
import static com.google.dart.engine.ast.ASTFactory.propertyAccess;

public class SimpleIdentifierTest extends ParserTestCase {
  private enum AssignmentKind {
    BINARY,
    COMPOUND_LEFT,
    COMPOUND_RIGHT,
    POSTFIX_INC,
    PREFIX_DEC,
    PREFIX_INC,
    PREFIX_NOT,
    SIMPLE_LEFT,
    SIMPLE_RIGHT,
    NONE;
  }

  private enum WrapperKind {
    PREFIXED_LEFT,
    PREFIXED_RIGHT,
    PROPERTY_LEFT,
    PROPERTY_RIGHT,
    NONE;
  }

  public void test_inGetterContext() throws Exception {
    for (WrapperKind wrapper : WrapperKind.values()) {
      for (AssignmentKind assignment : AssignmentKind.values()) {
        SimpleIdentifier identifier = createIdentifier(wrapper, assignment);
        if (assignment == AssignmentKind.SIMPLE_LEFT && wrapper != WrapperKind.PREFIXED_LEFT
            && wrapper != WrapperKind.PROPERTY_LEFT) {
          if (identifier.inGetterContext()) {
            fail("Expected " + topMostNode(identifier).toSource() + " to be false");
          }
        } else {
          if (!identifier.inGetterContext()) {
            fail("Expected " + topMostNode(identifier).toSource() + " to be true");
          }
        }
      }
    }
  }

  public void test_inSetterContext() throws Exception {
    for (WrapperKind wrapper : WrapperKind.values()) {
      for (AssignmentKind assignment : AssignmentKind.values()) {
        SimpleIdentifier identifier = createIdentifier(wrapper, assignment);
        if (wrapper == WrapperKind.PREFIXED_LEFT || wrapper == WrapperKind.PROPERTY_LEFT
            || assignment == AssignmentKind.BINARY || assignment == AssignmentKind.COMPOUND_RIGHT
            || assignment == AssignmentKind.PREFIX_NOT || assignment == AssignmentKind.SIMPLE_RIGHT
            || assignment == AssignmentKind.NONE) {
          if (identifier.inSetterContext()) {
            fail("Expected " + topMostNode(identifier).toSource() + " to be false");
          }
        } else {
          if (!identifier.inSetterContext()) {
            fail("Expected " + topMostNode(identifier).toSource() + " to be true");
          }
        }
      }
    }
  }

  private SimpleIdentifier createIdentifier(WrapperKind wrapper, AssignmentKind assignment) {
    SimpleIdentifier identifier = identifier("a");
    Expression expression = identifier;
    switch (wrapper) {
      case PREFIXED_LEFT:
        expression = identifier(identifier, identifier("_"));
        break;
      case PREFIXED_RIGHT:
        expression = identifier(identifier("_"), identifier);
        break;
      case PROPERTY_LEFT:
        expression = propertyAccess(expression, "_");
        break;
      case PROPERTY_RIGHT:
        expression = propertyAccess(identifier("_"), identifier);
        break;
    }
    switch (assignment) {
      case BINARY:
        binaryExpression(expression, TokenType.PLUS, identifier("_"));
        break;
      case COMPOUND_LEFT:
        assignmentExpression(expression, TokenType.PLUS_EQ, identifier("_"));
        break;
      case COMPOUND_RIGHT:
        assignmentExpression(identifier("_"), TokenType.PLUS_EQ, expression);
        break;
      case POSTFIX_INC:
        postfixExpression(expression, TokenType.PLUS_PLUS);
        break;
      case PREFIX_DEC:
        prefixExpression(TokenType.MINUS_MINUS, expression);
        break;
      case PREFIX_INC:
        prefixExpression(TokenType.PLUS_PLUS, expression);
        break;
      case PREFIX_NOT:
        prefixExpression(TokenType.BANG, expression);
        break;
      case SIMPLE_LEFT:
        assignmentExpression(expression, TokenType.EQ, identifier("_"));
        break;
      case SIMPLE_RIGHT:
        assignmentExpression(identifier("_"), TokenType.EQ, expression);
        break;
    }
    return identifier;
  }

  /**
   * Return the top-most node in the AST structure containing the given identifier.
   * 
   * @param identifier the identifier in the AST structure being traversed
   * @return the root of the AST structure containing the identifier
   */
  private ASTNode topMostNode(SimpleIdentifier identifier) {
    ASTNode child = identifier;
    ASTNode parent = identifier.getParent();
    while (parent != null) {
      child = parent;
      parent = parent.getParent();
    }
    return child;
  }
}
