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

import static com.google.dart.engine.ast.AstFactory.assignmentExpression;
import static com.google.dart.engine.ast.AstFactory.binaryExpression;
import static com.google.dart.engine.ast.AstFactory.catchClause;
import static com.google.dart.engine.ast.AstFactory.classDeclaration;
import static com.google.dart.engine.ast.AstFactory.classTypeAlias;
import static com.google.dart.engine.ast.AstFactory.constructorDeclaration;
import static com.google.dart.engine.ast.AstFactory.declaredIdentifier;
import static com.google.dart.engine.ast.AstFactory.emptyStatement;
import static com.google.dart.engine.ast.AstFactory.enumDeclaration;
import static com.google.dart.engine.ast.AstFactory.fieldFormalParameter;
import static com.google.dart.engine.ast.AstFactory.functionDeclaration;
import static com.google.dart.engine.ast.AstFactory.identifier;
import static com.google.dart.engine.ast.AstFactory.importDirective;
import static com.google.dart.engine.ast.AstFactory.integer;
import static com.google.dart.engine.ast.AstFactory.label;
import static com.google.dart.engine.ast.AstFactory.labeledStatement;
import static com.google.dart.engine.ast.AstFactory.list;
import static com.google.dart.engine.ast.AstFactory.methodDeclaration;
import static com.google.dart.engine.ast.AstFactory.methodInvocation;
import static com.google.dart.engine.ast.AstFactory.namedExpression;
import static com.google.dart.engine.ast.AstFactory.postfixExpression;
import static com.google.dart.engine.ast.AstFactory.prefixExpression;
import static com.google.dart.engine.ast.AstFactory.propertyAccess;
import static com.google.dart.engine.ast.AstFactory.returnStatement;
import static com.google.dart.engine.ast.AstFactory.simpleFormalParameter;
import static com.google.dart.engine.ast.AstFactory.typeAlias;
import static com.google.dart.engine.ast.AstFactory.typeName;
import static com.google.dart.engine.ast.AstFactory.typeParameter;
import static com.google.dart.engine.ast.AstFactory.variableDeclaration;

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

  public void test_inDeclarationContext_catch_exception() {
    SimpleIdentifier identifier = catchClause("e").getExceptionParameter();
    assertTrue(identifier.inDeclarationContext());
  }

  public void test_inDeclarationContext_catch_stack() {
    SimpleIdentifier identifier = catchClause("e", "s").getStackTraceParameter();
    assertTrue(identifier.inDeclarationContext());
  }

  public void test_inDeclarationContext_classDeclaration() {
    SimpleIdentifier identifier = classDeclaration(null, "C", null, null, null, null).getName();
    assertTrue(identifier.inDeclarationContext());
  }

  public void test_inDeclarationContext_classTypeAlias() {
    SimpleIdentifier identifier = classTypeAlias("C", null, null, null, null, null).getName();
    assertTrue(identifier.inDeclarationContext());
  }

  public void test_inDeclarationContext_constructorDeclaration() {
    SimpleIdentifier identifier = constructorDeclaration(identifier("C"), "c", null, null).getName();
    assertTrue(identifier.inDeclarationContext());
  }

  public void test_inDeclarationContext_declaredIdentifier() {
    DeclaredIdentifier declaredIdentifier = declaredIdentifier("v");
    SimpleIdentifier identifier = declaredIdentifier.getIdentifier();
    assertTrue(identifier.inDeclarationContext());
  }

  public void test_inDeclarationContext_enumConstantDeclaration() {
    EnumDeclaration enumDeclaration = enumDeclaration("MyEnum", "CONST");
    SimpleIdentifier identifier = enumDeclaration.getConstants().get(0).getName();
    assertTrue(identifier.inDeclarationContext());
  }

  public void test_inDeclarationContext_enumDeclaration() {
    EnumDeclaration enumDeclaration = enumDeclaration("MyEnum", "A", "B", "C");
    SimpleIdentifier identifier = enumDeclaration.getName();
    assertTrue(identifier.inDeclarationContext());
  }

  public void test_inDeclarationContext_fieldFormalParameter() {
    SimpleIdentifier identifier = fieldFormalParameter("p").getIdentifier();
    assertFalse(identifier.inDeclarationContext());
  }

  public void test_inDeclarationContext_functionDeclaration() {
    SimpleIdentifier identifier = functionDeclaration(null, null, "f", null).getName();
    assertTrue(identifier.inDeclarationContext());
  }

  public void test_inDeclarationContext_functionTypeAlias() {
    SimpleIdentifier identifier = typeAlias(null, "F", null, null).getName();
    assertTrue(identifier.inDeclarationContext());
  }

  public void test_inDeclarationContext_label_false() {
    SimpleIdentifier identifier = namedExpression("l", integer(0)).getName().getLabel();
    assertFalse(identifier.inDeclarationContext());
  }

  public void test_inDeclarationContext_label_true() {
    Label label = label("l");
    SimpleIdentifier identifier = label.getLabel();
    labeledStatement(list(label), emptyStatement());
    assertTrue(identifier.inDeclarationContext());
  }

  public void test_inDeclarationContext_methodDeclaration() {
    SimpleIdentifier identifier = identifier("m");
    methodDeclaration(null, null, null, null, identifier, null, null);
    assertTrue(identifier.inDeclarationContext());
  }

  public void test_inDeclarationContext_prefix() {
    SimpleIdentifier identifier = importDirective("uri", "pref").getPrefix();
    assertTrue(identifier.inDeclarationContext());
  }

  public void test_inDeclarationContext_simpleFormalParameter() {
    SimpleIdentifier identifier = simpleFormalParameter("p").getIdentifier();
    assertTrue(identifier.inDeclarationContext());
  }

  public void test_inDeclarationContext_typeParameter_bound() {
    TypeName bound = typeName("A");
    SimpleIdentifier identifier = (SimpleIdentifier) bound.getName();
    typeParameter("E", bound);
    assertFalse(identifier.inDeclarationContext());
  }

  public void test_inDeclarationContext_typeParameter_name() {
    SimpleIdentifier identifier = typeParameter("E").getName();
    assertTrue(identifier.inDeclarationContext());
  }

  public void test_inDeclarationContext_variableDeclaration() {
    SimpleIdentifier identifier = variableDeclaration("v").getName();
    assertTrue(identifier.inDeclarationContext());
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

  public void test_inReferenceContext() throws Exception {
    SimpleIdentifier identifier = identifier("id");
    namedExpression(label(identifier), identifier("_"));
    assertFalse(identifier.inGetterContext());
    assertFalse(identifier.inSetterContext());
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

  public void test_isQualified_inMethodInvocation_noTarget() throws Exception {
    MethodInvocation invocation = methodInvocation("test", identifier("arg0"));
    SimpleIdentifier identifier = invocation.getMethodName();
    assertFalse(identifier.isQualified());
  }

  public void test_isQualified_inMethodInvocation_withTarget() throws Exception {
    MethodInvocation invocation = methodInvocation(identifier("target"), "test", identifier("arg0"));
    SimpleIdentifier identifier = invocation.getMethodName();
    assertTrue(identifier.isQualified());
  }

  public void test_isQualified_inPrefixedIdentifier_name() throws Exception {
    SimpleIdentifier identifier = identifier("test");
    identifier("prefix", identifier);
    assertTrue(identifier.isQualified());
  }

  public void test_isQualified_inPrefixedIdentifier_prefix() throws Exception {
    SimpleIdentifier identifier = identifier("test");
    identifier(identifier, identifier("name"));
    assertFalse(identifier.isQualified());
  }

  public void test_isQualified_inPropertyAccess_name() throws Exception {
    SimpleIdentifier identifier = identifier("test");
    propertyAccess(identifier("target"), identifier);
    assertTrue(identifier.isQualified());
  }

  public void test_isQualified_inPropertyAccess_target() throws Exception {
    SimpleIdentifier identifier = identifier("test");
    propertyAccess(identifier, identifier("name"));
    assertFalse(identifier.isQualified());
  }

  public void test_isQualified_inReturnStatement() throws Exception {
    SimpleIdentifier identifier = identifier("test");
    returnStatement(identifier);
    assertFalse(identifier.isQualified());
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
      case NONE:
        // Nothing to add.
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
      case NONE:
        // Nothing to add.
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
  private AstNode topMostNode(SimpleIdentifier identifier) {
    AstNode child = identifier;
    AstNode parent = identifier.getParent();
    while (parent != null) {
      child = parent;
      parent = parent.getParent();
    }
    return child;
  }
}
