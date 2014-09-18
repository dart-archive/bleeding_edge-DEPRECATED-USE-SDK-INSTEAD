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
package com.google.dart.engine.constant;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.resolver.ResolverTestCase;
import com.google.dart.engine.source.Source;

public class ConstantEvaluatorTest extends ResolverTestCase {
  public void fail_constructor() throws Exception {
    EvaluationResult result = getExpressionValue("?");
    assertTrue(result.isValid());
    DartObject value = result.getValue();
    assertEquals(null, value);
  }

  public void fail_identifier_class() throws Exception {
    EvaluationResult result = getExpressionValue("?");
    assertTrue(result.isValid());
    DartObject value = result.getValue();
    assertEquals(null, value);
  }

  public void fail_identifier_function() throws Exception {
    EvaluationResult result = getExpressionValue("?");
    assertTrue(result.isValid());
    DartObject value = result.getValue();
    assertEquals(null, value);
  }

  public void fail_identifier_static() throws Exception {
    EvaluationResult result = getExpressionValue("?");
    assertTrue(result.isValid());
    DartObject value = result.getValue();
    assertEquals(null, value);
  }

  public void fail_identifier_staticMethod() throws Exception {
    EvaluationResult result = getExpressionValue("?");
    assertTrue(result.isValid());
    DartObject value = result.getValue();
    assertEquals(null, value);
  }

  public void fail_identifier_topLevel() throws Exception {
    EvaluationResult result = getExpressionValue("?");
    assertTrue(result.isValid());
    DartObject value = result.getValue();
    assertEquals(null, value);
  }

  public void fail_identifier_typeParameter() throws Exception {
    EvaluationResult result = getExpressionValue("?");
    assertTrue(result.isValid());
    DartObject value = result.getValue();
    assertEquals(null, value);
  }

  public void fail_prefixedIdentifier_invalid() throws Exception {
    EvaluationResult result = getExpressionValue("?");
    assertTrue(result.isValid());
    DartObject value = result.getValue();
    assertEquals(null, value);
  }

  public void fail_prefixedIdentifier_valid() throws Exception {
    EvaluationResult result = getExpressionValue("?");
    assertTrue(result.isValid());
    DartObject value = result.getValue();
    assertEquals(null, value);
  }

  public void fail_propertyAccess_invalid() throws Exception {
    EvaluationResult result = getExpressionValue("?");
    assertTrue(result.isValid());
    DartObject value = result.getValue();
    assertEquals(null, value);
  }

  public void fail_propertyAccess_valid() throws Exception {
    EvaluationResult result = getExpressionValue("?");
    assertTrue(result.isValid());
    DartObject value = result.getValue();
    assertEquals(null, value);
  }

  public void fail_simpleIdentifier_invalid() throws Exception {
    EvaluationResult result = getExpressionValue("?");
    assertTrue(result.isValid());
    DartObject value = result.getValue();
    assertEquals(null, value);
  }

  public void fail_simpleIdentifier_valid() throws Exception {
    EvaluationResult result = getExpressionValue("?");
    assertTrue(result.isValid());
    DartObject value = result.getValue();
    assertEquals(null, value);
  }

  public void test_bitAnd_int_int() throws Exception {
    assertValue(74 & 42, "74 & 42");
  }

  public void test_bitNot() throws Exception {
    assertValue(~42, "~42");
  }

  public void test_bitOr_int_int() throws Exception {
    assertValue(74 | 42, "74 | 42");
  }

  public void test_bitXor_int_int() throws Exception {
    assertValue(74 ^ 42, "74 ^ 42");
  }

  public void test_divide_double_double() throws Exception {
    assertValue(3.2 / 2.3, "3.2 / 2.3");
  }

  public void test_divide_double_double_byZero() throws Exception {
    EvaluationResult result = getExpressionValue("3.2 / 0.0");
    assertTrue(result.isValid());
    DartObject value = result.getValue();
    assertEquals("double", value.getType().getName());
    assertTrue(value.getDoubleValue().isInfinite());
  }

  public void test_divide_int_int() throws Exception {
    assertValue(1L, "3 / 2");
  }

  public void test_divide_int_int_byZero() throws Exception {
    EvaluationResult result = getExpressionValue("3 / 0");
    assertTrue(result.isValid());
  }

  public void test_equal_boolean_boolean() throws Exception {
    assertValue(false, "true == false");
  }

  public void test_equal_int_int() throws Exception {
    assertValue(false, "2 == 3");
  }

  public void test_equal_invalidLeft() throws Exception {
    EvaluationResult result = getExpressionValue("a == 3");
    assertFalse(result.isValid());
  }

  public void test_equal_invalidRight() throws Exception {
    EvaluationResult result = getExpressionValue("2 == a");
    assertFalse(result.isValid());
  }

  public void test_equal_string_string() throws Exception {
    assertValue(false, "'a' == 'b'");
  }

  public void test_greaterThan_int_int() throws Exception {
    assertValue(false, "2 > 3");
  }

  public void test_greaterThanOrEqual_int_int() throws Exception {
    assertValue(false, "2 >= 3");
  }

  public void test_leftShift_int_int() throws Exception {
    assertValue(64, "16 << 2");
  }

  public void test_lessThan_int_int() throws Exception {
    assertValue(true, "2 < 3");
  }

  public void test_lessThanOrEqual_int_int() throws Exception {
    assertValue(true, "2 <= 3");
  }

  public void test_literal_boolean_false() throws Exception {
    assertValue(false, "false");
  }

  public void test_literal_boolean_true() throws Exception {
    assertValue(true, "true");
  }

  public void test_literal_list() throws Exception {
    EvaluationResult result = getExpressionValue("const ['a', 'b', 'c']");
    assertTrue(result.isValid());
  }

  public void test_literal_map() throws Exception {
    EvaluationResult result = getExpressionValue("const {'a' : 'm', 'b' : 'n', 'c' : 'o'}");
    assertTrue(result.isValid());
  }

  public void test_literal_null() throws Exception {
    EvaluationResult result = getExpressionValue("null");
    assertTrue(result.isValid());
    DartObject value = result.getValue();
    assertTrue(value.isNull());
  }

  public void test_literal_number_double() throws Exception {
    assertValue(3.45, "3.45");
  }

  public void test_literal_number_integer() throws Exception {
    assertValue(42L, "42");
  }

  public void test_literal_string_adjacent() throws Exception {
    assertValue("abcdef", "'abc' 'def'");
  }

  public void test_literal_string_interpolation_invalid() throws Exception {
    EvaluationResult result = getExpressionValue("'a${f()}c'");
    assertFalse(result.isValid());
  }

  public void test_literal_string_interpolation_valid() throws Exception {
    assertValue("a3c", "'a${3}c'");
  }

  public void test_literal_string_simple() throws Exception {
    assertValue("abc", "'abc'");
  }

  public void test_logicalAnd() throws Exception {
    assertValue(false, "true && false");
  }

  public void test_logicalNot() throws Exception {
    assertValue(false, "!true");
  }

  public void test_logicalOr() throws Exception {
    assertValue(true, "true || false");
  }

  public void test_minus_double_double() throws Exception {
    assertValue(3.2 - 2.3, "3.2 - 2.3");
  }

  public void test_minus_int_int() throws Exception {
    assertValue(1L, "3 - 2");
  }

  public void test_negated_boolean() throws Exception {
    EvaluationResult result = getExpressionValue("-true");
    assertFalse(result.isValid());
//    AnalysisError[] errors = result.getErrors();
//    assertLength(1, errors);
  }

  public void test_negated_double() throws Exception {
    assertValue(-42.3, "-42.3");
  }

  public void test_negated_integer() throws Exception {
    assertValue(-42L, "-42");
  }

  public void test_notEqual_boolean_boolean() throws Exception {
    assertValue(true, "true != false");
  }

  public void test_notEqual_int_int() throws Exception {
    assertValue(true, "2 != 3");
  }

  public void test_notEqual_invalidLeft() throws Exception {
    EvaluationResult result = getExpressionValue("a != 3");
    assertFalse(result.isValid());
  }

  public void test_notEqual_invalidRight() throws Exception {
    EvaluationResult result = getExpressionValue("2 != a");
    assertFalse(result.isValid());
  }

  public void test_notEqual_string_string() throws Exception {
    assertValue(true, "'a' != 'b'");
  }

  public void test_parenthesizedExpression() throws Exception {
    assertValue("a", "('a')");
  }

  public void test_plus_double_double() throws Exception {
    assertValue(2.3 + 3.2, "2.3 + 3.2");
  }

  public void test_plus_int_int() throws Exception {
    assertValue(5L, "2 + 3");
  }

  public void test_plus_string_string() throws Exception {
    assertValue("ab", "'a' + 'b'");
  }

  public void test_remainder_double_double() throws Exception {
    assertValue(3.2 % 2.3, "3.2 % 2.3");
  }

  public void test_remainder_int_int() throws Exception {
    assertValue(2L, "8 % 3");
  }

  public void test_rightShift() throws Exception {
    assertValue(16L, "64 >> 2");
  }

  public void test_stringLength_complex() throws Exception {
    assertValue(6L, "('qwe' + 'rty').length");
  }

  public void test_stringLength_simple() throws Exception {
    assertValue(6L, "'Dvorak'.length");
  }

  public void test_times_double_double() throws Exception {
    assertValue(2.3 * 3.2, "2.3 * 3.2");
  }

  public void test_times_int_int() throws Exception {
    assertValue(6L, "2 * 3");
  }

  public void test_truncatingDivide_double_double() throws Exception {
    assertValue(1L, "3.2 ~/ 2.3");
  }

  public void test_truncatingDivide_int_int() throws Exception {
    assertValue(3L, "10 ~/ 3");
  }

  private void assertValue(boolean expectedValue, String contents) throws Exception {
    EvaluationResult result = getExpressionValue(contents);
    DartObject value = result.getValue();
    assertEquals("bool", value.getType().getName());
    assertEquals(expectedValue, value.getBoolValue().booleanValue());
  }

  private void assertValue(double expectedValue, String contents) throws Exception {
    EvaluationResult result = getExpressionValue(contents);
    assertTrue(result.isValid());
    DartObject value = result.getValue();
    assertEquals("double", value.getType().getName());
    assertEquals(expectedValue, value.getDoubleValue().doubleValue(), 0.0);
  }

  private void assertValue(long expectedValue, String contents) throws Exception {
    EvaluationResult result = getExpressionValue(contents);
    assertTrue(result.isValid());
    DartObject value = result.getValue();
    assertEquals("int", value.getType().getName());
    assertEquals(expectedValue, value.getIntValue().longValue());
  }

  private void assertValue(String expectedValue, String contents) throws Exception {
    EvaluationResult result = getExpressionValue(contents);
    DartObject value = result.getValue();
    assertEquals("String", value.getType().getName());
    assertEquals(expectedValue, value.getStringValue());
  }

  private EvaluationResult getExpressionValue(String contents) throws Exception {
    Source source = addSource("var x = " + contents + ";");
    LibraryElement library = resolve(source);
    CompilationUnit unit = getAnalysisContext().resolveCompilationUnit(source, library);
    assertNotNull(unit);
    NodeList<CompilationUnitMember> declarations = unit.getDeclarations();
    assertSizeOfList(1, declarations);
    CompilationUnitMember declaration = declarations.get(0);
    assertInstanceOf(TopLevelVariableDeclaration.class, declaration);
    NodeList<VariableDeclaration> variables = ((TopLevelVariableDeclaration) declaration).getVariables().getVariables();
    assertSizeOfList(1, variables);
    ConstantEvaluator evaluator = new ConstantEvaluator(
        source,
        ((AnalysisContextImpl) getAnalysisContext()).getTypeProvider());
    return evaluator.evaluate(variables.get(0).getInitializer());
  }
}
