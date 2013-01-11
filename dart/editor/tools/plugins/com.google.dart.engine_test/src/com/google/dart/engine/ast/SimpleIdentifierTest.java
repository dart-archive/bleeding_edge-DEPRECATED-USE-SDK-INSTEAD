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

public class SimpleIdentifierTest extends ParserTestCase {
  public void test_inGetterContext_assignmentParent_notPure() throws Exception {
    SimpleIdentifier identifier = identifier("field");
    // field +=
    assignmentExpression(identifier, TokenType.PLUS_EQ, null);
    assertTrue(identifier.inGetterContext());
  }

  public void test_inGetterContext_assignmentParent_pure() throws Exception {
    SimpleIdentifier identifier = identifier("field");
    // field =
    assignmentExpression(identifier, TokenType.EQ, null);
    assertFalse(identifier.inGetterContext());
  }

  public void test_inGetterContext_notAssignmentParent() throws Exception {
    SimpleIdentifier identifier = identifier("field");
    // field +
    binaryExpression(identifier, TokenType.PLUS, null);
    assertTrue(identifier.inGetterContext());
  }

  public void test_inGetterContext_whenQualified_false() throws Exception {
    SimpleIdentifier identifier = identifier("field");
    PrefixedIdentifier prefixedIdentifier = identifier("myPrefix", identifier);
    // myPrefix.field +=
    assignmentExpression(prefixedIdentifier, TokenType.PLUS_EQ, null);
    assertTrue(identifier.inGetterContext());
  }

  public void test_inGetterContext_whenQualified_true() throws Exception {
    SimpleIdentifier identifier = identifier("field");
    PrefixedIdentifier prefixedIdentifier = identifier("myPrefix", identifier);
    // myPrefix.field =
    assignmentExpression(prefixedIdentifier, TokenType.EQ, null);
    assertFalse(identifier.inGetterContext());
  }

  public void test_inSetterContext_assignmentParent_leftEq() throws Exception {
    SimpleIdentifier identifier = identifier("field");
    // field =
    assignmentExpression(identifier, TokenType.EQ, null);
    assertTrue(identifier.inSetterContext());
  }

  public void test_inSetterContext_assignmentParent_leftPlusEq() throws Exception {
    SimpleIdentifier identifier = identifier("field");
    // field +=
    assignmentExpression(identifier, TokenType.PLUS_EQ, null);
    assertTrue(identifier.inSetterContext());
  }

  public void test_inSetterContext_assignmentParent_rightEq() throws Exception {
    SimpleIdentifier identifier = identifier("field");
    // = field
    assignmentExpression(null, TokenType.EQ, identifier);
    assertFalse(identifier.inSetterContext());
  }

  public void test_inSetterContext_assignmentParent_rightPlusEq() throws Exception {
    SimpleIdentifier identifier = identifier("field");
    // += field
    assignmentExpression(null, TokenType.PLUS_EQ, identifier);
    assertFalse(identifier.inSetterContext());
  }

  public void test_inSetterContext_notInterestingParent() throws Exception {
    SimpleIdentifier identifier = identifier("field");
    binaryExpression(identifier, null, null);
    // verify
    assertFalse(identifier.inSetterContext());
  }

  public void test_inSetterContext_postfixParent() throws Exception {
    SimpleIdentifier identifier = identifier("field");
    postfixExpression(identifier, null);
    // always
    assertTrue(identifier.inSetterContext());
  }

  public void test_inSetterContext_prefixParent_bang() throws Exception {
    SimpleIdentifier identifier = identifier("field");
    // !field
    prefixExpression(TokenType.BANG, identifier);
    assertFalse(identifier.inSetterContext());
  }

  public void test_inSetterContext_prefixParent_minusMinus() throws Exception {
    SimpleIdentifier identifier = identifier("field");
    // --field
    prefixExpression(TokenType.MINUS_MINUS, identifier);
    assertTrue(identifier.inSetterContext());
  }

  public void test_inSetterContext_prefixParent_plusPlus() throws Exception {
    SimpleIdentifier identifier = identifier("field");
    // ++field
    prefixExpression(TokenType.PLUS_PLUS, identifier);
    assertTrue(identifier.inSetterContext());
  }

  public void test_inSetterContext_whenQualified_prefixParent_bang() throws Exception {
    SimpleIdentifier identifier = identifier("field");
    PrefixedIdentifier prefixedIdentifier = identifier("myPrefix", identifier);
    // !myPrefix.field
    prefixExpression(TokenType.BANG, prefixedIdentifier);
    assertFalse(identifier.inSetterContext());
  }

  public void test_inSetterContext_whenQualified_prefixParent_plusPlus() throws Exception {
    SimpleIdentifier identifier = identifier("field");
    PrefixedIdentifier prefixedIdentifier = identifier("myPrefix", identifier);
    // ++myPrefix.field
    prefixExpression(TokenType.PLUS_PLUS, prefixedIdentifier);
    assertTrue(identifier.inSetterContext());
  }
}
