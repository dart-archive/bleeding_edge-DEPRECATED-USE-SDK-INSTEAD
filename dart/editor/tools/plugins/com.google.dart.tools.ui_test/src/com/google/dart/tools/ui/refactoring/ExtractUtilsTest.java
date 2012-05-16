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
package com.google.dart.tools.ui.refactoring;

import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartFieldDefinition;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.DartVariableStatement;
import com.google.dart.tools.core.internal.model.SourceRangeImpl;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.internal.corext.refactoring.code.ExtractUtils;

import java.util.List;

/**
 * Test for {@link ExtractUtils}.
 */
public final class ExtractUtilsTest extends AbstractDartTest {
  /**
   * Test for {@link ExtractUtils#covers(SourceRange, DartNode)}.
   */
  public void test_covers_SourceRange_DartNode() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "}",
        "");
    DartUnit unitNode = DartCompilerUtilities.resolveUnit(testUnit);
    assertFalse(ExtractUtils.covers(new SourceRangeImpl(0, 1), unitNode));
    assertTrue(ExtractUtils.covers(new SourceRangeImpl(0, 1000), unitNode));
  }

  /**
   * Test for {@link ExtractUtils#getEndOfLine()}.
   */
  public void test_getEndOfLine_default() throws Exception {
    setTestUnitContent("");
    ExtractUtils utils = new ExtractUtils(testUnit);
    assertEquals(ExtractUtils.DEFAULT_END_OF_LINE, utils.getEndOfLine());
  }

  /**
   * Test for {@link ExtractUtils#getEndOfLine()}.
   */
  public void test_getEndOfLine_unix() throws Exception {
    setTestUnitContent("aaa\nbbb\nccc");
    ExtractUtils utils = new ExtractUtils(testUnit);
    assertEquals("\n", utils.getEndOfLine());
  }

  /**
   * Test for {@link ExtractUtils#getEndOfLine()}.
   */
  public void test_getEndOfLine_windows() throws Exception {
    setTestUnitContent("aaa\r\nbbb\r\nccc");
    ExtractUtils utils = new ExtractUtils(testUnit);
    assertEquals("\r\n", utils.getEndOfLine());
  }

  /**
   * Test for {@link ExtractUtils#getNodePrefix(DartNode)}.
   */
  public void test_getNodePrefix_block_noPrefix() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "var a;",
        "}",
        "");
    assert_getNodePrefix("var a;", "");
  }

  /**
   * Test for {@link ExtractUtils#getNodePrefix(DartNode)}.
   */
  public void test_getNodePrefix_block_withPrefix() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var a;",
        "}",
        "");
    assert_getNodePrefix("var a;", "  ");
  }

  /**
   * Test for {@link ExtractUtils#getNodePrefix(DartNode)}.
   */
  public void test_getNodePrefix_noPrefix() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var b;var a;",
        "}",
        "");
    assert_getNodePrefix("var a;", "");
  }

  /**
   * Test for {@link ExtractUtils#getText(int, int)}.
   */
  public void test_getText() throws Exception {
    setTestUnitContent("// 0123456789");
    ExtractUtils utils = new ExtractUtils(testUnit);
    assertEquals("0123", utils.getText(3, 4));
  }

  public void test_getType_binaryExpression_ADD_double_double() throws Exception {
    assertTypeSimple("double", "1.0 + 2.0");
  }

  public void test_getType_binaryExpression_ADD_double_int() throws Exception {
    assertTypeSimple("double", "1.0 + 2");
  }

  public void test_getType_binaryExpression_ADD_int_double() throws Exception {
    assertTypeSimple("double", "1 + 2.0");
  }

  public void test_getType_binaryExpression_ADD_int_int() throws Exception {
    assertTypeSimple("int", "1 + 2");
  }

  public void test_getType_binaryExpression_AND() throws Exception {
    assertTypeSimple("bool", "true && false");
  }

  public void test_getType_binaryExpression_BIT_AND() throws Exception {
    assertTypeSimple("int", "1 & 2");
  }

  public void test_getType_binaryExpression_BIT_OR() throws Exception {
    assertTypeSimple("int", "1 | 2");
  }

  public void test_getType_binaryExpression_BIT_XOR() throws Exception {
    assertTypeSimple("int", "1 ^ 2");
  }

  public void test_getType_binaryExpression_DIV_int_int() throws Exception {
    assertTypeSimple("int", "1 / 2");
  }

  public void test_getType_binaryExpression_EQ() throws Exception {
    assertTypeSimple("bool", "1 == 2");
  }

  public void test_getType_binaryExpression_EQ_STRICT() throws Exception {
    assertTypeSimple("bool", "1 === 2");
  }

  public void test_getType_binaryExpression_GT() throws Exception {
    assertTypeSimple("bool", "1 > 2");
  }

  public void test_getType_binaryExpression_GTE() throws Exception {
    assertTypeSimple("bool", "1 >= 2");
  }

  public void test_getType_binaryExpression_LT() throws Exception {
    assertTypeSimple("bool", "1 < 2");
  }

  public void test_getType_binaryExpression_LTE() throws Exception {
    assertTypeSimple("bool", "1 <= 2");
  }

  public void test_getType_binaryExpression_MOD() throws Exception {
    assertTypeSimple("int", "1 % 2");
  }

  public void test_getType_binaryExpression_MUL_int_int() throws Exception {
    assertTypeSimple("int", "1 * 2");
  }

  public void test_getType_binaryExpression_NE() throws Exception {
    assertTypeSimple("bool", "1 != 2");
  }

  public void test_getType_binaryExpression_NE_STRICT() throws Exception {
    assertTypeSimple("bool", "1 !== 2");
  }

  public void test_getType_binaryExpression_OR() throws Exception {
    assertTypeSimple("bool", "true || false");
  }

  public void test_getType_binaryExpression_SAR() throws Exception {
    assertTypeSimple("int", "1 >> 2");
  }

  public void test_getType_binaryExpression_SHL() throws Exception {
    assertTypeSimple("int", "1 << 2");
  }

  public void test_getType_binaryExpression_SUB_int_int() throws Exception {
    assertTypeSimple("int", "1 - 2");
  }

  public void test_getType_binaryExpression_SUB_String_int() throws Exception {
    assertTypeSimple(null, "'1' - 2");
  }

  public void test_getType_binaryExpression_unknownArg1() throws Exception {
    assertType(null, new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "var x = unknown + 2;",
        ""});
  }

  public void test_getType_binaryExpression_unknownArg2() throws Exception {
    assertType(null, new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "var x = 1 + unknown;",
        ""});
  }

  public void test_getType_literal_bool_false() throws Exception {
    assertTypeSimple("bool", "false");
  }

  public void test_getType_literal_bool_true() throws Exception {
    assertTypeSimple("bool", "true");
  }

  public void test_getType_literal_double() throws Exception {
    assertTypeSimple("double", "1.0");
  }

  public void test_getType_literal_int() throws Exception {
    assertTypeSimple("int", "1");
  }

  public void test_getType_literal_String() throws Exception {
    assertTypeSimple("String", "'abc'");
  }

  public void test_getType_methodInvocation_int() throws Exception {
    assert_getType_methodInvocation("int");
  }

  public void test_getType_methodInvocation_List() throws Exception {
    assert_getType_methodInvocation("List<String>");
  }

  public void test_getType_methodInvocation_unknownTarget() throws Exception {
    assertType(null, new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "var x = unknown.m();",
        ""});
  }

  public void test_getType_newExpression() throws Exception {
    assertType("MyClass", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "class MyClass {",
        "}",
        "var x = new MyClass();",
        ""});
  }

  public void test_getType_newExpression_factoryConstructor() throws Exception {
    assertType("MyInterface", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "interface MyInterface factory MyFactory {",
        "}",
        "class MyFactory {",
        "}",
        "var x = new MyInterface();",
        ""});
  }

  public void test_getType_newExpression_unknownConstructor() throws Exception {
    assertType(null, new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "class MyClass {",
        "}",
        "var x = new MyClass.unknown();",
        ""});
  }

  public void test_getType_newExpression_unknownType() throws Exception {
    assertType(null, new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "var x = new Unknown();",
        ""});
  }

  public void test_getType_newExpression_withTypeArguments() throws Exception {
    assertType("MyClass<String>", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "class MyClass<T> {",
        "}",
        "var x = new MyClass<String>();",
        ""});
  }

  public void test_getType_null() throws Exception {
    assertSame(null, ExtractUtils.getTypeSource(null));
  }

  public void test_getType_propertyAccess() throws Exception {
    assertType("int", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "class MyClass {",
        "  int prop;",
        "}",
        "var x = new MyClass().prop;",
        ""});
  }

  public void test_getType_typedLiteral_List_noTypeArgument() throws Exception {
    assertTypeSimple("List", "[1, 2, 3]");
  }

  public void test_getType_typedLiteral_List_withTypeArgument() throws Exception {
    assertTypeSimple("List<int>", "<int>[1, 2, 3]");
  }

  public void test_getType_typedLiteral_Map_noTypeArgument() throws Exception {
    assertTypeSimple("Map<String, Dynamic>", "{'a' : 1, 'b' : 2, 'c' : 3}");
  }

  public void test_getType_typedLiteral_Map_withTypeArgument() throws Exception {
    assertTypeSimple("Map<String, int>", "<int>{'a' : 1, 'b' : 2, 'c' : 3}");
  }

  public void test_getType_unaryExpression_BIT_NOT() throws Exception {
    assertTypeSimple("int", "~1");
  }

  public void test_getType_unaryExpression_NOT() throws Exception {
    assertTypeSimple("bool", "!true");
  }

  public void test_getType_unaryExpression_SUB_double() throws Exception {
    assertTypeSimple("double", "-(1.0)");
  }

  public void test_getType_unaryExpression_SUB_int() throws Exception {
    assertTypeSimple("int", "-(1)");
  }

  public void test_getType_unaryExpression_unknownArg() throws Exception {
    assertType(null, new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "var x = -unknown;",
        ""});
  }

  public void test_getType_unqualifiedInvocation_int() throws Exception {
    assert_getType_unqualifiedInvocation("int");
  }

  public void test_getType_unqualifiedInvocation_List_noTypeArgument() throws Exception {
    assert_getType_unqualifiedInvocation("List");
  }

  public void test_getType_unqualifiedInvocation_List_withTypeArgument() throws Exception {
    assert_getType_unqualifiedInvocation("List<String>");
  }

  public void test_getType_unqualifiedInvocation_Map_noTypeArgument() throws Exception {
    assert_getType_unqualifiedInvocation("Map");
  }

  public void test_getType_unqualifiedInvocation_Map_withTypeArgument() throws Exception {
    assert_getType_unqualifiedInvocation("Map<int, String>");
  }

  public void test_getType_unqualifiedInvocation_Map_withTypeArgument2() throws Exception {
    assert_getType_unqualifiedInvocation("Map<int, Dynamic>");
  }

  public void test_getType_unqualifiedInvocation_String() throws Exception {
    assert_getType_unqualifiedInvocation("String");
  }

  public void test_getType_variable() throws Exception {
    assertType("int", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "int v = 1;",
        "var x = v;",
        ""});
  }

  public void test_getType_variable_unknown() throws Exception {
    assertType(null, new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "var x = unknown;",
        ""});
  }

  public void test_getType_variable_withTypeArguments() throws Exception {
    assertType("List<String>", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "List<String> v = null;",
        "var x = v;",
        ""});
  }

  /**
   * Test for {@link ExtractUtils#isAssociative(SourceRange, DartNode)}.
   */
  public void test_isAssociative_false_literals() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var x_SUB = 1 - 2;",
        "var x_DIV = 1 / 2;",
        "var x_SHR = 1 << 2;",
        "var x_SAR = 1 >> 2;",
        "var x_MOD = 1 % 2;",
        "");
    assert_isAssociative(false);
  }

  /**
   * Test for {@link ExtractUtils#isAssociative(SourceRange, DartNode)}.
   */
  public void test_isAssociative_false_notAssociativeArgs() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var x_1 = 1 / 2 + 3;",
        "var x_2 = 1 + 2 / 3;",
        "");
    assert_isAssociative(false);
  }

  /**
   * Test for {@link ExtractUtils#isAssociative(SourceRange, DartNode)}.
   */
  public void test_isAssociative_true_literals() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var x_ADD = 1 + 2;",
        "var x_MUL = 1 * 2;",
        "var x_BIT_XOR = 1 ^ 2;",
        "var x_BIT_OR = 1 | 2;",
        "var x_BIT_AND = 1 & 2;",
        "var x_OR = true || false;",
        "var x_AND = true && false;",
        "");
    assert_isAssociative(true);
  }

  /**
   * Test for {@link ExtractUtils#isAssociative(SourceRange, DartNode)}.
   */
  public void test_isAssociative_true_notBinaryExpressionArgs() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "  int foo() => 42;",
        "}",
        "var x_1 = Math.random() + 2;",
        "var x_2 = 1 + Math.random();",
        "var x_3 = Math.random() + Math.random();",
        "var x_4 = new A().foo() + 2;",
        "");
    assert_isAssociative(true);
  }

  /**
   * Asserts that {@link ExtractUtils#getNodePrefix(DartNode)} in {@link #testUnit} has expected
   * prefix.
   */
  private void assert_getNodePrefix(String nodePattern, String expectedPrefix) throws Exception {
    // prepare AST
    ExtractUtils utils = new ExtractUtils(testUnit);
    // find node
    int nodeOffset = findOffset(nodePattern);
    DartVariableStatement node = findNode(
        utils.getUnitNode(),
        nodeOffset,
        DartVariableStatement.class);
    assertNotNull(node);
    // assert prefix
    assertEquals(expectedPrefix, utils.getNodePrefix(node));
  }

  private void assert_getType_methodInvocation(String returnType) throws Exception {
    assertType(returnType, new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  " + returnType + " m() {}",
        "}",
        "var x = new A().m();",
        ""});
  }

  private void assert_getType_unqualifiedInvocation(String returnType) throws Exception {
    assertType(returnType, new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        returnType + " f() {}",
        "var x = f();",
        ""});
  }

  /**
   * Asserts that each top-level variable has associative (or not) {@link DartBinaryExpression}
   * value.
   */
  private void assert_isAssociative(boolean expected) throws DartModelException {
    DartUnit unit = DartCompilerUtilities.resolveUnit(testUnit);
    for (DartNode topLevelNode : unit.getTopLevelNodes()) {
      if (topLevelNode instanceof DartFieldDefinition) {
        DartFieldDefinition fieldDefinition = (DartFieldDefinition) topLevelNode;
        List<DartField> fields = fieldDefinition.getFields();
        if (fields.size() == 1) {
          DartBinaryExpression expression = (DartBinaryExpression) fields.get(0).getValue();
          assertEquals(expected, ExtractUtils.isAssociative(expression));
        }
      }
    }
  }

  /**
   * Asserts that value of the top-level field "x" in given source has the given type name.
   */
  private String assertType(String expectedTypeName, String[] lines) throws Exception {
    DartExpression expression = getMarkerVariableExpression(lines);
    String type = ExtractUtils.getTypeSource(expression);
    if (expectedTypeName != null) {
      assertNotNull(type);
      assertEquals(expectedTypeName, type);
    } else {
      assertNull(type);
    }
    return type;
  }

  /**
   * Simplified call to {@link #assertType(String, String[])} for single {@link DartExpression}
   * without dependencies.
   */
  private void assertTypeSimple(String expectedType, String expression) throws Exception {
    assertType(expectedType, new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "var x = " + expression + ";",
        ""});
  }

  /**
   * @return the {@link DartExpression} of top-level variable "x" in the given source.
   */
  private DartExpression getMarkerVariableExpression(String... lines) throws Exception {
    setTestUnitContent(lines);
    DartUnit unit = DartCompilerUtilities.resolveUnit(testUnit);
    for (DartNode topLevelNode : unit.getTopLevelNodes()) {
      if (topLevelNode instanceof DartFieldDefinition) {
        DartFieldDefinition fieldDefinition = (DartFieldDefinition) topLevelNode;
        List<DartField> fields = fieldDefinition.getFields();
        if (fields.size() == 1 && fields.get(0).getName().getName().equals("x")) {
          return fields.get(0).getValue();
        }
      }
    }
    fail("Field 'x' not found");
    return null;
  }
}
