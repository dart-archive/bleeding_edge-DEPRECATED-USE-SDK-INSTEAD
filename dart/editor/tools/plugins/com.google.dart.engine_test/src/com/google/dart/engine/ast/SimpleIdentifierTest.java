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
package com.google.dart.engine.ast;

import com.google.dart.engine.parser.ParserTestCase;
import com.google.dart.engine.scanner.TokenType;

public class SimpleIdentifierTest extends ParserTestCase {
  public void test_inGetterContext_isAssignmentParent_isPureAssignment() throws Exception {
    SimpleIdentifier identifier = ASTFactory.identifier("field");
    ASTFactory.assignmentExpression(identifier, TokenType.EQ, null);
    // verify
    assertEquals(false, identifier.inGetterContext());
  }

  public void test_inGetterContext_isAssignmentParent_notPureAssignment() throws Exception {
    SimpleIdentifier identifier = ASTFactory.identifier("field");
    ASTFactory.assignmentExpression(identifier, TokenType.PLUS_EQ, null);
    // verify
    assertEquals(true, identifier.inGetterContext());
  }

  public void test_inGetterContext_notAssignmentParent() throws Exception {
    SimpleIdentifier identifier = ASTFactory.identifier("field");
    ASTFactory.binaryExpression(identifier, null, null);
    // verify
    assertEquals(true, identifier.inGetterContext());
  }

  public void test_inSetterContext_assignmentParent() throws Exception {
    SimpleIdentifier identifier = ASTFactory.identifier("field");
    // =
    {
      ASTFactory.assignmentExpression(identifier, TokenType.EQ, null);
      assertEquals(true, identifier.inSetterContext());
    }
    // =
    {
      ASTFactory.assignmentExpression(null, TokenType.EQ, identifier);
      assertEquals(false, identifier.inSetterContext());
    }
    // +=
    {
      ASTFactory.assignmentExpression(identifier, TokenType.PLUS_EQ, null);
      assertEquals(true, identifier.inSetterContext());
    }
    // +=
    {
      ASTFactory.assignmentExpression(null, TokenType.PLUS_EQ, identifier);
      assertEquals(false, identifier.inSetterContext());
    }
  }

  public void test_inSetterContext_notInterestingParent() throws Exception {
    SimpleIdentifier identifier = ASTFactory.identifier("field");
    ASTFactory.binaryExpression(identifier, null, null);
    // verify
    assertEquals(false, identifier.inSetterContext());
  }

  public void test_inSetterContext_postfixParent() throws Exception {
    SimpleIdentifier identifier = ASTFactory.identifier("field");
    ASTFactory.postfixExpression(identifier, null);
    // always
    assertEquals(true, identifier.inSetterContext());
  }

  public void test_inSetterContext_prefixParent() throws Exception {
    SimpleIdentifier identifier = ASTFactory.identifier("field");
    // ++
    {
      ASTFactory.prefixExpression(TokenType.PLUS_PLUS, identifier);
      assertEquals(true, identifier.inSetterContext());
    }
    // --
    {
      ASTFactory.prefixExpression(TokenType.MINUS_MINUS, identifier);
      assertEquals(true, identifier.inSetterContext());
    }
    // !
    {
      ASTFactory.prefixExpression(TokenType.BANG, identifier);
      assertEquals(false, identifier.inSetterContext());
    }
  }
}
