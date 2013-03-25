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
package com.google.dart.engine.ast.visitor;

import com.google.dart.engine.parser.ParserTestCase;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class ConstantEvaluatorTest extends ParserTestCase {
  public void fail_constructor() throws Exception {
    Object value = getConstantValue("?");
    assertEquals(null, value);
  }

  public void fail_identifier_class() throws Exception {
    Object value = getConstantValue("?");
    assertEquals(null, value);
  }

  public void fail_identifier_function() throws Exception {
    Object value = getConstantValue("?");
    assertEquals(null, value);
  }

  public void fail_identifier_static() throws Exception {
    Object value = getConstantValue("?");
    assertEquals(null, value);
  }

  public void fail_identifier_staticMethod() throws Exception {
    Object value = getConstantValue("?");
    assertEquals(null, value);
  }

  public void fail_identifier_topLevel() throws Exception {
    Object value = getConstantValue("?");
    assertEquals(null, value);
  }

  public void fail_identifier_typeVariable() throws Exception {
    Object value = getConstantValue("?");
    assertEquals(null, value);
  }

  public void test_binary_bitAnd() throws Exception {
    Object value = getConstantValue("74 & 42");
    assertInstanceOf(BigInteger.class, value);
    assertEquals(74 & 42, ((BigInteger) value).intValue());
  }

  public void test_binary_bitOr() throws Exception {
    Object value = getConstantValue("74 | 42");
    assertInstanceOf(BigInteger.class, value);
    assertEquals(74 | 42, ((BigInteger) value).intValue());
  }

  public void test_binary_bitXor() throws Exception {
    Object value = getConstantValue("74 ^ 42");
    assertInstanceOf(BigInteger.class, value);
    assertEquals(74 ^ 42, ((BigInteger) value).intValue());
  }

  public void test_binary_divide_double() throws Exception {
    Object value = getConstantValue("3.2 / 2.3");
    assertInstanceOf(Double.class, value);
    assertEquals(3.2 / 2.3, ((Double) value).doubleValue());
  }

  public void test_binary_divide_integer() throws Exception {
    Object value = getConstantValue("3 / 2");
    assertInstanceOf(BigInteger.class, value);
    assertEquals(1, ((BigInteger) value).intValue());
  }

  public void test_binary_equal_boolean() throws Exception {
    Object value = getConstantValue("true == false");
    assertEquals(Boolean.FALSE, value);
  }

  public void test_binary_equal_integer() throws Exception {
    Object value = getConstantValue("2 == 3");
    assertEquals(Boolean.FALSE, value);
  }

  public void test_binary_equal_invalidLeft() throws Exception {
    Object value = getConstantValue("a == 3");
    assertEquals(ConstantEvaluator.NOT_A_CONSTANT, value);
  }

  public void test_binary_equal_invalidRight() throws Exception {
    Object value = getConstantValue("2 == a");
    assertEquals(ConstantEvaluator.NOT_A_CONSTANT, value);
  }

  public void test_binary_equal_string() throws Exception {
    Object value = getConstantValue("'a' == 'b'");
    assertEquals(Boolean.FALSE, value);
  }

  public void test_binary_greaterThan() throws Exception {
    Object value = getConstantValue("2 > 3");
    assertEquals(Boolean.FALSE, value);
  }

  public void test_binary_greaterThanOrEqual() throws Exception {
    Object value = getConstantValue("2 >= 3");
    assertEquals(Boolean.FALSE, value);
  }

  public void test_binary_leftShift() throws Exception {
    Object value = getConstantValue("16 << 2");
    assertInstanceOf(BigInteger.class, value);
    assertEquals(64, ((BigInteger) value).intValue());
  }

  public void test_binary_lessThan() throws Exception {
    Object value = getConstantValue("2 < 3");
    assertEquals(Boolean.TRUE, value);
  }

  public void test_binary_lessThanOrEqual() throws Exception {
    Object value = getConstantValue("2 <= 3");
    assertEquals(Boolean.TRUE, value);
  }

  public void test_binary_logicalAnd() throws Exception {
    Object value = getConstantValue("true && false");
    assertEquals(Boolean.FALSE, value);
  }

  public void test_binary_logicalOr() throws Exception {
    Object value = getConstantValue("true || false");
    assertEquals(Boolean.TRUE, value);
  }

  public void test_binary_minus_double() throws Exception {
    Object value = getConstantValue("3.2 - 2.3");
    assertInstanceOf(Double.class, value);
    assertEquals(3.2 - 2.3, ((Double) value).doubleValue());
  }

  public void test_binary_minus_integer() throws Exception {
    Object value = getConstantValue("3 - 2");
    assertInstanceOf(BigInteger.class, value);
    assertEquals(1, ((BigInteger) value).intValue());
  }

  public void test_binary_notEqual_boolean() throws Exception {
    Object value = getConstantValue("true != false");
    assertEquals(Boolean.TRUE, value);
  }

  public void test_binary_notEqual_integer() throws Exception {
    Object value = getConstantValue("2 != 3");
    assertEquals(Boolean.TRUE, value);
  }

  public void test_binary_notEqual_invalidLeft() throws Exception {
    Object value = getConstantValue("a != 3");
    assertEquals(ConstantEvaluator.NOT_A_CONSTANT, value);
  }

  public void test_binary_notEqual_invalidRight() throws Exception {
    Object value = getConstantValue("2 != a");
    assertEquals(ConstantEvaluator.NOT_A_CONSTANT, value);
  }

  public void test_binary_notEqual_string() throws Exception {
    Object value = getConstantValue("'a' != 'b'");
    assertEquals(Boolean.TRUE, value);
  }

  public void test_binary_plus_double() throws Exception {
    Object value = getConstantValue("2.3 + 3.2");
    assertInstanceOf(Double.class, value);
    assertEquals(2.3 + 3.2, ((Double) value).doubleValue());
  }

  public void test_binary_plus_integer() throws Exception {
    Object value = getConstantValue("2 + 3");
    assertInstanceOf(BigInteger.class, value);
    assertEquals(5, ((BigInteger) value).intValue());
  }

  public void test_binary_remainder_double() throws Exception {
    Object value = getConstantValue("3.2 % 2.3");
    assertInstanceOf(Double.class, value);
    assertEquals(3.2 % 2.3, ((Double) value).doubleValue());
  }

  public void test_binary_remainder_integer() throws Exception {
    Object value = getConstantValue("8 % 3");
    assertInstanceOf(BigInteger.class, value);
    assertEquals(2, ((BigInteger) value).intValue());
  }

  public void test_binary_rightShift() throws Exception {
    Object value = getConstantValue("64 >> 2");
    assertInstanceOf(BigInteger.class, value);
    assertEquals(16, ((BigInteger) value).intValue());
  }

  public void test_binary_times_double() throws Exception {
    Object value = getConstantValue("2.3 * 3.2");
    assertInstanceOf(Double.class, value);
    assertEquals(2.3 * 3.2, ((Double) value).doubleValue());
  }

  public void test_binary_times_integer() throws Exception {
    Object value = getConstantValue("2 * 3");
    assertInstanceOf(BigInteger.class, value);
    assertEquals(6, ((BigInteger) value).intValue());
  }

  public void test_binary_truncatingDivide_double() throws Exception {
    Object value = getConstantValue("3.2 ~/ 2.3");
    assertInstanceOf(BigInteger.class, value);
    assertEquals(1, ((BigInteger) value).intValue());
  }

  public void test_binary_truncatingDivide_integer() throws Exception {
    Object value = getConstantValue("10 ~/ 3");
    assertInstanceOf(BigInteger.class, value);
    assertEquals(3, ((BigInteger) value).intValue());
  }

  public void test_literal_boolean_false() throws Exception {
    Object value = getConstantValue("false");
    assertEquals(Boolean.FALSE, value);
  }

  public void test_literal_boolean_true() throws Exception {
    Object value = getConstantValue("true");
    assertEquals(Boolean.TRUE, value);
  }

  public void test_literal_list() throws Exception {
    Object value = getConstantValue("['a', 'b', 'c']");
    assertInstanceOf(List.class, value);
    List<?> list = (List<?>) value;
    assertEquals(3, list.size());
    assertEquals("a", list.get(0));
    assertEquals("b", list.get(1));
    assertEquals("c", list.get(2));
  }

  public void test_literal_map() throws Exception {
    Object value = getConstantValue("{'a' : 'm', 'b' : 'n', 'c' : 'o'}");
    assertInstanceOf(Map.class, value);
    Map<?, ?> map = (Map<?, ?>) value;
    assertEquals(3, map.size());
    assertEquals("m", map.get("a"));
    assertEquals("n", map.get("b"));
    assertEquals("o", map.get("c"));
  }

  public void test_literal_null() throws Exception {
    Object value = getConstantValue("null");
    assertEquals(null, value);
  }

  public void test_literal_number_double() throws Exception {
    Object value = getConstantValue("3.45");
    assertInstanceOf(Double.class, value);
    assertEquals(3.45, ((Double) value).doubleValue());
  }

  public void test_literal_number_integer() throws Exception {
    Object value = getConstantValue("42");
    assertInstanceOf(BigInteger.class, value);
    assertEquals(42, ((BigInteger) value).intValue());
  }

  public void test_literal_string_adjacent() throws Exception {
    Object value = getConstantValue("'abc' 'def'");
    assertEquals("abcdef", value);
  }

  public void test_literal_string_interpolation_invalid() throws Exception {
    Object value = getConstantValue("'a${f()}c'");
    assertEquals(ConstantEvaluator.NOT_A_CONSTANT, value);
  }

  public void test_literal_string_interpolation_valid() throws Exception {
    Object value = getConstantValue("'a${3}c'");
    assertEquals("a3c", value);
  }

  public void test_literal_string_simple() throws Exception {
    Object value = getConstantValue("'abc'");
    assertEquals("abc", value);
  }

  public void test_parenthesizedExpression() throws Exception {
    Object value = getConstantValue("('a')");
    assertEquals("a", value);
  }

  public void test_unary_bitNot() throws Exception {
    Object value = getConstantValue("~42");
    assertInstanceOf(BigInteger.class, value);
    assertEquals(~42, ((BigInteger) value).intValue());
  }

  public void test_unary_logicalNot() throws Exception {
    Object value = getConstantValue("!true");
    assertEquals(Boolean.FALSE, value);
  }

  public void test_unary_negated_double() throws Exception {
    Object value = getConstantValue("-42.3");
    assertInstanceOf(Double.class, value);
    assertEquals(-42.3, ((Double) value).doubleValue());
  }

  public void test_unary_negated_integer() throws Exception {
    Object value = getConstantValue("-42");
    assertInstanceOf(BigInteger.class, value);
    assertEquals(-42, ((BigInteger) value).intValue());
  }

  private Object getConstantValue(String source) throws Exception {
    return parseExpression(source).accept(new ConstantEvaluator());
  }
}
