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

import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartFieldDefinition;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartPropertyAccess;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.internal.corext.refactoring.code.ExtractUtils;

import java.util.List;

/**
 * Test for {@link ExtractUtils}.
 * <p>
 * TODO(scheglov) 1) variables - {@link DartIdentifier} 2) DartNewExpression 3)
 * {@link DartPropertyAccess}
 */
public final class ExtractUtilsTest extends AbstractDartTest {
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

  public void test_getType_null() throws Exception {
    assertSame(null, ExtractUtils.getTypeSource(null));
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
   * Asserts that value of the top-level field "x" in given source has the given type name.
   */
  private String assertType(String expectedTypeName, String[] lines) throws Exception {
    setTestUnitContent(lines);
    DartUnit unit = DartCompilerUtilities.resolveUnit(testUnit);
    for (DartNode topLevelNode : unit.getTopLevelNodes()) {
      if (topLevelNode instanceof DartFieldDefinition) {
        DartFieldDefinition fieldDefinition = (DartFieldDefinition) topLevelNode;
        List<DartField> fields = fieldDefinition.getFields();
        if (fields.size() == 1 && fields.get(0).getName().getName().equals("x")) {
          DartExpression expression = fields.get(0).getValue();
          String type = ExtractUtils.getTypeSource(expression);
          if (expectedTypeName != null) {
            assertNotNull(type);
            assertEquals(expectedTypeName, type);
          } else {
            assertNull(type);
          }
          return type;
        }
      }
    }
    fail("Field 'x' not found");
    return null;
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
}
