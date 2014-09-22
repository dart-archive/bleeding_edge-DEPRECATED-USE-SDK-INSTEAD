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
package com.google.dart.engine.internal.object;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.internal.resolver.TestTypeProvider;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.utilities.translation.DartBlockBody;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class DartObjectImplTest extends EngineTestCase {
  TypeProvider typeProvider = new TestTypeProvider();

  public void fail_add_knownString_knownString() throws EvaluationException {
    fail("New constant semantics are not yet enabled");
    assertAdd(stringValue("ab"), stringValue("a"), stringValue("b"));
  }

  public void fail_add_knownString_unknownString() throws EvaluationException {
    fail("New constant semantics are not yet enabled");
    assertAdd(stringValue(null), stringValue("a"), stringValue(null));
  }

  public void fail_add_unknownString_knownString() throws EvaluationException {
    fail("New constant semantics are not yet enabled");
    assertAdd(stringValue(null), stringValue(null), stringValue("b"));
  }

  public void fail_add_unknownString_unknownString() throws EvaluationException {
    fail("New constant semantics are not yet enabled");
    assertAdd(stringValue(null), stringValue(null), stringValue(null));
  }

  public void test_add_knownDouble_knownDouble() throws EvaluationException {
    assertAdd(doubleValue(3.0), doubleValue(1.0), doubleValue(2.0));
  }

  public void test_add_knownDouble_knownInt() throws EvaluationException {
    assertAdd(doubleValue(3.0), doubleValue(1.0), intValue(2));
  }

  public void test_add_knownDouble_unknownDouble() throws EvaluationException {
    assertAdd(doubleValue(null), doubleValue(1.0), doubleValue(null));
  }

  public void test_add_knownDouble_unknownInt() throws EvaluationException {
    assertAdd(doubleValue(null), doubleValue(1.0), intValue(null));
  }

  public void test_add_knownInt_knownInt() throws EvaluationException {
    assertAdd(intValue(3), intValue(1), intValue(2));
  }

  public void test_add_knownInt_knownString() throws EvaluationException {
    assertAdd(null, intValue(1), stringValue("2"));
  }

  public void test_add_knownInt_unknownDouble() throws EvaluationException {
    assertAdd(doubleValue(null), intValue(1), doubleValue(null));
  }

  public void test_add_knownInt_unknownInt() throws EvaluationException {
    assertAdd(intValue(null), intValue(1), intValue(null));
  }

  public void test_add_knownString_knownInt() throws EvaluationException {
    assertAdd(null, stringValue("1"), intValue(2));
  }

  public void test_add_unknownDouble_knownDouble() throws EvaluationException {
    assertAdd(doubleValue(null), doubleValue(null), doubleValue(2.0));
  }

  public void test_add_unknownDouble_knownInt() throws EvaluationException {
    assertAdd(doubleValue(null), doubleValue(null), intValue(2));
  }

  public void test_add_unknownInt_knownDouble() throws EvaluationException {
    assertAdd(doubleValue(null), intValue(null), doubleValue(2.0));
  }

  public void test_add_unknownInt_knownInt() throws EvaluationException {
    assertAdd(intValue(null), intValue(null), intValue(2));
  }

  public void test_bitAnd_knownInt_knownInt() throws EvaluationException {
    assertBitAnd(intValue(2), intValue(6), intValue(3));
  }

  public void test_bitAnd_knownInt_knownString() throws EvaluationException {
    assertBitAnd(null, intValue(6), stringValue("3"));
  }

  public void test_bitAnd_knownInt_unknownInt() throws EvaluationException {
    assertBitAnd(intValue(null), intValue(6), intValue(null));
  }

  public void test_bitAnd_knownString_knownInt() throws EvaluationException {
    assertBitAnd(null, stringValue("6"), intValue(3));
  }

  public void test_bitAnd_unknownInt_knownInt() throws EvaluationException {
    assertBitAnd(intValue(null), intValue(null), intValue(3));
  }

  public void test_bitAnd_unknownInt_unknownInt() throws EvaluationException {
    assertBitAnd(intValue(null), intValue(null), intValue(null));
  }

  public void test_bitNot_knownInt() throws EvaluationException {
    assertBitNot(intValue(-4), intValue(3));
  }

  public void test_bitNot_knownString() throws EvaluationException {
    assertBitNot(null, stringValue("6"));
  }

  public void test_bitNot_unknownInt() throws EvaluationException {
    assertBitNot(intValue(null), intValue(null));
  }

  public void test_bitOr_knownInt_knownInt() throws EvaluationException {
    assertBitOr(intValue(7), intValue(6), intValue(3));
  }

  public void test_bitOr_knownInt_knownString() throws EvaluationException {
    assertBitOr(null, intValue(6), stringValue("3"));
  }

  public void test_bitOr_knownInt_unknownInt() throws EvaluationException {
    assertBitOr(intValue(null), intValue(6), intValue(null));
  }

  public void test_bitOr_knownString_knownInt() throws EvaluationException {
    assertBitOr(null, stringValue("6"), intValue(3));
  }

  public void test_bitOr_unknownInt_knownInt() throws EvaluationException {
    assertBitOr(intValue(null), intValue(null), intValue(3));
  }

  public void test_bitOr_unknownInt_unknownInt() throws EvaluationException {
    assertBitOr(intValue(null), intValue(null), intValue(null));
  }

  public void test_bitXor_knownInt_knownInt() throws EvaluationException {
    assertBitXor(intValue(5), intValue(6), intValue(3));
  }

  public void test_bitXor_knownInt_knownString() throws EvaluationException {
    assertBitXor(null, intValue(6), stringValue("3"));
  }

  public void test_bitXor_knownInt_unknownInt() throws EvaluationException {
    assertBitXor(intValue(null), intValue(6), intValue(null));
  }

  public void test_bitXor_knownString_knownInt() throws EvaluationException {
    assertBitXor(null, stringValue("6"), intValue(3));
  }

  public void test_bitXor_unknownInt_knownInt() throws EvaluationException {
    assertBitXor(intValue(null), intValue(null), intValue(3));
  }

  public void test_bitXor_unknownInt_unknownInt() throws EvaluationException {
    assertBitXor(intValue(null), intValue(null), intValue(null));
  }

  public void test_concatenate_knownInt_knownString() throws EvaluationException {
    assertConcatenate(null, intValue(2), stringValue("def"));
  }

  public void test_concatenate_knownString_knownInt() throws EvaluationException {
    assertConcatenate(null, stringValue("abc"), intValue(3));
  }

  public void test_concatenate_knownString_knownString() throws EvaluationException {
    assertConcatenate(stringValue("abcdef"), stringValue("abc"), stringValue("def"));
  }

  public void test_concatenate_knownString_unknownString() throws EvaluationException {
    assertConcatenate(stringValue(null), stringValue("abc"), stringValue(null));
  }

  public void test_concatenate_unknownString_knownString() throws EvaluationException {
    assertConcatenate(stringValue(null), stringValue(null), stringValue("def"));
  }

  public void test_divide_knownDouble_knownDouble() throws EvaluationException {
    assertDivide(doubleValue(3.0), doubleValue(6.0), doubleValue(2.0));
  }

  public void test_divide_knownDouble_knownInt() throws EvaluationException {
    assertDivide(doubleValue(3.0), doubleValue(6.0), intValue(2));
  }

  public void test_divide_knownDouble_unknownDouble() throws EvaluationException {
    assertDivide(doubleValue(null), doubleValue(6.0), doubleValue(null));
  }

  public void test_divide_knownDouble_unknownInt() throws EvaluationException {
    assertDivide(doubleValue(null), doubleValue(6.0), intValue(null));
  }

  public void test_divide_knownInt_knownInt() throws EvaluationException {
    assertDivide(intValue(3), intValue(6), intValue(2));
  }

  public void test_divide_knownInt_knownString() throws EvaluationException {
    assertDivide(null, intValue(6), stringValue("2"));
  }

  public void test_divide_knownInt_unknownDouble() throws EvaluationException {
    assertDivide(doubleValue(null), intValue(6), doubleValue(null));
  }

  public void test_divide_knownInt_unknownInt() throws EvaluationException {
    assertDivide(intValue(null), intValue(6), intValue(null));
  }

  public void test_divide_knownString_knownInt() throws EvaluationException {
    assertDivide(null, stringValue("6"), intValue(2));
  }

  public void test_divide_unknownDouble_knownDouble() throws EvaluationException {
    assertDivide(doubleValue(null), doubleValue(null), doubleValue(2.0));
  }

  public void test_divide_unknownDouble_knownInt() throws EvaluationException {
    assertDivide(doubleValue(null), doubleValue(null), intValue(2));
  }

  public void test_divide_unknownInt_knownDouble() throws EvaluationException {
    assertDivide(doubleValue(null), intValue(null), doubleValue(2.0));
  }

  public void test_divide_unknownInt_knownInt() throws EvaluationException {
    assertDivide(intValue(null), intValue(null), intValue(2));
  }

  public void test_equalEqual_bool_false() throws EvaluationException {
    assertEqualEqual(boolValue(false), boolValue(false), boolValue(true));
  }

  public void test_equalEqual_bool_true() throws EvaluationException {
    assertEqualEqual(boolValue(true), boolValue(true), boolValue(true));
  }

  public void test_equalEqual_bool_unknown() throws EvaluationException {
    assertEqualEqual(boolValue(null), boolValue(null), boolValue(false));
  }

  public void test_equalEqual_double_false() throws EvaluationException {
    assertEqualEqual(boolValue(false), doubleValue(2.0), doubleValue(4.0));
  }

  public void test_equalEqual_double_true() throws EvaluationException {
    assertEqualEqual(boolValue(true), doubleValue(2.0), doubleValue(2.0));
  }

  public void test_equalEqual_double_unknown() throws EvaluationException {
    assertEqualEqual(boolValue(null), doubleValue(1.0), doubleValue(null));
  }

  public void test_equalEqual_int_false() throws EvaluationException {
    assertEqualEqual(boolValue(false), intValue(-5), intValue(5));
  }

  public void test_equalEqual_int_true() throws EvaluationException {
    assertEqualEqual(boolValue(true), intValue(5), intValue(5));
  }

  public void test_equalEqual_int_unknown() throws EvaluationException {
    assertEqualEqual(boolValue(null), intValue(null), intValue(3));
  }

  public void test_equalEqual_list_empty() throws EvaluationException {
    assertEqualEqual(null, listValue(), listValue());
  }

  public void test_equalEqual_list_false() throws EvaluationException {
    assertEqualEqual(null, listValue(), listValue());
  }

  public void test_equalEqual_map_empty() throws EvaluationException {
    assertEqualEqual(null, mapValue(), mapValue());
  }

  public void test_equalEqual_map_false() throws EvaluationException {
    assertEqualEqual(null, mapValue(), mapValue());
  }

  public void test_equalEqual_null() throws EvaluationException {
    assertEqualEqual(boolValue(true), nullValue(), nullValue());
  }

  public void test_equalEqual_string_false() throws EvaluationException {
    assertEqualEqual(boolValue(false), stringValue("abc"), stringValue("def"));
  }

  public void test_equalEqual_string_true() throws EvaluationException {
    assertEqualEqual(boolValue(true), stringValue("abc"), stringValue("abc"));
  }

  public void test_equalEqual_string_unknown() throws EvaluationException {
    assertEqualEqual(boolValue(null), stringValue(null), stringValue("def"));
  }

  public void test_equals_list_false_differentSizes() throws EvaluationException {
    assertFalse(listValue(boolValue(true)).equals(listValue(boolValue(true), boolValue(false))));
  }

  public void test_equals_list_false_sameSize() throws EvaluationException {
    assertFalse(listValue(boolValue(true)).equals(listValue(boolValue(false))));
  }

  public void test_equals_list_true_empty() throws EvaluationException {
    assertEquals(listValue(), listValue());
  }

  public void test_equals_list_true_nonEmpty() throws EvaluationException {
    assertEquals(listValue(boolValue(true)), listValue(boolValue(true)));
  }

  public void test_equals_map_true_empty() throws EvaluationException {
    assertEquals(mapValue(), mapValue());
  }

  public void test_equals_symbol_false() throws EvaluationException {
    assertFalse(symbolValue("a").equals(symbolValue("b")));
  }

  public void test_equals_symbol_true() throws EvaluationException {
    assertEquals(symbolValue("a"), symbolValue("a"));
  }

  public void test_getValue_bool_false() {
    assertEquals(Boolean.FALSE, boolValue(false).getValue());
  }

  public void test_getValue_bool_true() {
    assertEquals(Boolean.TRUE, boolValue(true).getValue());
  }

  public void test_getValue_bool_unknown() {
    assertNull(boolValue(null).getValue());
  }

  public void test_getValue_double_known() {
    double value = 2.3;
    assertEquals(value, doubleValue(value).getValue());
  }

  public void test_getValue_double_unknown() {
    assertNull(doubleValue(null).getValue());
  }

  public void test_getValue_int_known() {
    int value = 23;
    assertEquals(BigInteger.valueOf(value), intValue(value).getValue());
  }

  public void test_getValue_int_unknown() {
    assertNull(intValue(null).getValue());
  }

  public void test_getValue_list_empty() {
    Object result = listValue().getValue();
    assertInstanceOfObjectArray(result);
    Object[] array = (Object[]) result;
    assertLength(0, array);
  }

  public void test_getValue_list_valid() {
    Object result = listValue(intValue(23)).getValue();
    assertInstanceOfObjectArray(result);
    Object[] array = (Object[]) result;
    assertLength(1, array);
  }

  @SuppressWarnings("rawtypes")
  public void test_getValue_map_empty() {
    Object result = mapValue().getValue();
    assertInstanceOf(Map.class, result);
    Map map = (Map) result;
    assertSizeOfMap(0, map);
  }

  @SuppressWarnings("rawtypes")
  public void test_getValue_map_valid() {
    Object result = mapValue(stringValue("key"), stringValue("value")).getValue();
    assertInstanceOf(Map.class, result);
    Map map = (Map) result;
    assertSizeOfMap(1, map);
  }

  public void test_getValue_null() {
    assertNull(nullValue().getValue());
  }

  public void test_getValue_string_known() {
    String value = "twenty-three";
    assertEquals(value, stringValue(value).getValue());
  }

  public void test_getValue_string_unknown() {
    assertNull(stringValue(null).getValue());
  }

  public void test_greaterThan_knownDouble_knownDouble_false() throws EvaluationException {
    assertGreaterThan(boolValue(false), doubleValue(1.0), doubleValue(2.0));
  }

  public void test_greaterThan_knownDouble_knownDouble_true() throws EvaluationException {
    assertGreaterThan(boolValue(true), doubleValue(2.0), doubleValue(1.0));
  }

  public void test_greaterThan_knownDouble_knownInt_false() throws EvaluationException {
    assertGreaterThan(boolValue(false), doubleValue(1.0), intValue(2));
  }

  public void test_greaterThan_knownDouble_knownInt_true() throws EvaluationException {
    assertGreaterThan(boolValue(true), doubleValue(2.0), intValue(1));
  }

  public void test_greaterThan_knownDouble_unknownDouble() throws EvaluationException {
    assertGreaterThan(boolValue(null), doubleValue(1.0), doubleValue(null));
  }

  public void test_greaterThan_knownDouble_unknownInt() throws EvaluationException {
    assertGreaterThan(boolValue(null), doubleValue(1.0), intValue(null));
  }

  public void test_greaterThan_knownInt_knownInt_false() throws EvaluationException {
    assertGreaterThan(boolValue(false), intValue(1), intValue(2));
  }

  public void test_greaterThan_knownInt_knownInt_true() throws EvaluationException {
    assertGreaterThan(boolValue(true), intValue(2), intValue(1));
  }

  public void test_greaterThan_knownInt_knownString() throws EvaluationException {
    assertGreaterThan(null, intValue(1), stringValue("2"));
  }

  public void test_greaterThan_knownInt_unknownDouble() throws EvaluationException {
    assertGreaterThan(boolValue(null), intValue(1), doubleValue(null));
  }

  public void test_greaterThan_knownInt_unknownInt() throws EvaluationException {
    assertGreaterThan(boolValue(null), intValue(1), intValue(null));
  }

  public void test_greaterThan_knownString_knownInt() throws EvaluationException {
    assertGreaterThan(null, stringValue("1"), intValue(2));
  }

  public void test_greaterThan_unknownDouble_knownDouble() throws EvaluationException {
    assertGreaterThan(boolValue(null), doubleValue(null), doubleValue(2.0));
  }

  public void test_greaterThan_unknownDouble_knownInt() throws EvaluationException {
    assertGreaterThan(boolValue(null), doubleValue(null), intValue(2));
  }

  public void test_greaterThan_unknownInt_knownDouble() throws EvaluationException {
    assertGreaterThan(boolValue(null), intValue(null), doubleValue(2.0));
  }

  public void test_greaterThan_unknownInt_knownInt() throws EvaluationException {
    assertGreaterThan(boolValue(null), intValue(null), intValue(2));
  }

  public void test_greaterThanOrEqual_knownDouble_knownDouble_false() throws EvaluationException {
    assertGreaterThanOrEqual(boolValue(false), doubleValue(1.0), doubleValue(2.0));
  }

  public void test_greaterThanOrEqual_knownDouble_knownDouble_true() throws EvaluationException {
    assertGreaterThanOrEqual(boolValue(true), doubleValue(2.0), doubleValue(1.0));
  }

  public void test_greaterThanOrEqual_knownDouble_knownInt_false() throws EvaluationException {
    assertGreaterThanOrEqual(boolValue(false), doubleValue(1.0), intValue(2));
  }

  public void test_greaterThanOrEqual_knownDouble_knownInt_true() throws EvaluationException {
    assertGreaterThanOrEqual(boolValue(true), doubleValue(2.0), intValue(1));
  }

  public void test_greaterThanOrEqual_knownDouble_unknownDouble() throws EvaluationException {
    assertGreaterThanOrEqual(boolValue(null), doubleValue(1.0), doubleValue(null));
  }

  public void test_greaterThanOrEqual_knownDouble_unknownInt() throws EvaluationException {
    assertGreaterThanOrEqual(boolValue(null), doubleValue(1.0), intValue(null));
  }

  public void test_greaterThanOrEqual_knownInt_knownInt_false() throws EvaluationException {
    assertGreaterThanOrEqual(boolValue(false), intValue(1), intValue(2));
  }

  public void test_greaterThanOrEqual_knownInt_knownInt_true() throws EvaluationException {
    assertGreaterThanOrEqual(boolValue(true), intValue(2), intValue(2));
  }

  public void test_greaterThanOrEqual_knownInt_knownString() throws EvaluationException {
    assertGreaterThanOrEqual(null, intValue(1), stringValue("2"));
  }

  public void test_greaterThanOrEqual_knownInt_unknownDouble() throws EvaluationException {
    assertGreaterThanOrEqual(boolValue(null), intValue(1), doubleValue(null));
  }

  public void test_greaterThanOrEqual_knownInt_unknownInt() throws EvaluationException {
    assertGreaterThanOrEqual(boolValue(null), intValue(1), intValue(null));
  }

  public void test_greaterThanOrEqual_knownString_knownInt() throws EvaluationException {
    assertGreaterThanOrEqual(null, stringValue("1"), intValue(2));
  }

  public void test_greaterThanOrEqual_unknownDouble_knownDouble() throws EvaluationException {
    assertGreaterThanOrEqual(boolValue(null), doubleValue(null), doubleValue(2.0));
  }

  public void test_greaterThanOrEqual_unknownDouble_knownInt() throws EvaluationException {
    assertGreaterThanOrEqual(boolValue(null), doubleValue(null), intValue(2));
  }

  public void test_greaterThanOrEqual_unknownInt_knownDouble() throws EvaluationException {
    assertGreaterThanOrEqual(boolValue(null), intValue(null), doubleValue(2.0));
  }

  public void test_greaterThanOrEqual_unknownInt_knownInt() throws EvaluationException {
    assertGreaterThanOrEqual(boolValue(null), intValue(null), intValue(2));
  }

  public void test_hasExactValue_bool_false() {
    assertTrue(boolValue(false).hasExactValue());
  }

  public void test_hasExactValue_bool_true() {
    assertTrue(boolValue(true).hasExactValue());
  }

  public void test_hasExactValue_bool_unknown() {
    assertTrue(boolValue(null).hasExactValue());
  }

  public void test_hasExactValue_double_known() {
    assertTrue(doubleValue(2.3).hasExactValue());
  }

  public void test_hasExactValue_double_unknown() {
    assertTrue(doubleValue(null).hasExactValue());
  }

  public void test_hasExactValue_dynamic() {
    assertFalse(dynamicValue().hasExactValue());
  }

  public void test_hasExactValue_int_known() {
    assertTrue(intValue(23).hasExactValue());
  }

  public void test_hasExactValue_int_unknown() {
    assertTrue(intValue(null).hasExactValue());
  }

  public void test_hasExactValue_list_empty() {
    assertTrue(listValue().hasExactValue());
  }

  public void test_hasExactValue_list_invalid() {
    assertFalse(dynamicValue().hasExactValue());
  }

  public void test_hasExactValue_list_valid() {
    assertTrue(listValue(intValue(23)).hasExactValue());
  }

  public void test_hasExactValue_map_empty() {
    assertTrue(mapValue().hasExactValue());
  }

  public void test_hasExactValue_map_invalidKey() {
    assertFalse(mapValue(dynamicValue(), stringValue("value")).hasExactValue());
  }

  public void test_hasExactValue_map_invalidValue() {
    assertFalse(mapValue(stringValue("key"), dynamicValue()).hasExactValue());
  }

  public void test_hasExactValue_map_valid() {
    assertTrue(mapValue(stringValue("key"), stringValue("value")).hasExactValue());
  }

  public void test_hasExactValue_null() {
    assertTrue(nullValue().hasExactValue());
  }

  public void test_hasExactValue_num() {
    assertFalse(numValue().hasExactValue());
  }

  public void test_hasExactValue_string_known() {
    assertTrue(stringValue("twenty-three").hasExactValue());
  }

  public void test_hasExactValue_string_unknown() {
    assertTrue(stringValue(null).hasExactValue());
  }

  public void test_integerDivide_knownDouble_knownDouble() throws EvaluationException {
    assertIntegerDivide(intValue(3), doubleValue(6.0), doubleValue(2.0));
  }

  public void test_integerDivide_knownDouble_knownInt() throws EvaluationException {
    assertIntegerDivide(intValue(3), doubleValue(6.0), intValue(2));
  }

  public void test_integerDivide_knownDouble_unknownDouble() throws EvaluationException {
    assertIntegerDivide(intValue(null), doubleValue(6.0), doubleValue(null));
  }

  public void test_integerDivide_knownDouble_unknownInt() throws EvaluationException {
    assertIntegerDivide(intValue(null), doubleValue(6.0), intValue(null));
  }

  public void test_integerDivide_knownInt_knownInt() throws EvaluationException {
    assertIntegerDivide(intValue(3), intValue(6), intValue(2));
  }

  public void test_integerDivide_knownInt_knownString() throws EvaluationException {
    assertIntegerDivide(null, intValue(6), stringValue("2"));
  }

  public void test_integerDivide_knownInt_unknownDouble() throws EvaluationException {
    assertIntegerDivide(intValue(null), intValue(6), doubleValue(null));
  }

  public void test_integerDivide_knownInt_unknownInt() throws EvaluationException {
    assertIntegerDivide(intValue(null), intValue(6), intValue(null));
  }

  public void test_integerDivide_knownString_knownInt() throws EvaluationException {
    assertIntegerDivide(null, stringValue("6"), intValue(2));
  }

  public void test_integerDivide_unknownDouble_knownDouble() throws EvaluationException {
    assertIntegerDivide(intValue(null), doubleValue(null), doubleValue(2.0));
  }

  public void test_integerDivide_unknownDouble_knownInt() throws EvaluationException {
    assertIntegerDivide(intValue(null), doubleValue(null), intValue(2));
  }

  public void test_integerDivide_unknownInt_knownDouble() throws EvaluationException {
    assertIntegerDivide(intValue(null), intValue(null), doubleValue(2.0));
  }

  public void test_integerDivide_unknownInt_knownInt() throws EvaluationException {
    assertIntegerDivide(intValue(null), intValue(null), intValue(2));
  }

  public void test_isBoolNumStringOrNull_bool_false() {
    assertTrue(boolValue(false).isBoolNumStringOrNull());
  }

  public void test_isBoolNumStringOrNull_bool_true() {
    assertTrue(boolValue(true).isBoolNumStringOrNull());
  }

  public void test_isBoolNumStringOrNull_bool_unknown() {
    assertTrue(boolValue(null).isBoolNumStringOrNull());
  }

  public void test_isBoolNumStringOrNull_double_known() {
    assertTrue(doubleValue(2.3).isBoolNumStringOrNull());
  }

  public void test_isBoolNumStringOrNull_double_unknown() {
    assertTrue(doubleValue(null).isBoolNumStringOrNull());
  }

  public void test_isBoolNumStringOrNull_dynamic() {
    assertTrue(dynamicValue().isBoolNumStringOrNull());
  }

  public void test_isBoolNumStringOrNull_int_known() {
    assertTrue(intValue(23).isBoolNumStringOrNull());
  }

  public void test_isBoolNumStringOrNull_int_unknown() {
    assertTrue(intValue(null).isBoolNumStringOrNull());
  }

  public void test_isBoolNumStringOrNull_list() {
    assertFalse(listValue().isBoolNumStringOrNull());
  }

  public void test_isBoolNumStringOrNull_null() {
    assertTrue(nullValue().isBoolNumStringOrNull());
  }

  public void test_isBoolNumStringOrNull_num() {
    assertTrue(numValue().isBoolNumStringOrNull());
  }

  public void test_isBoolNumStringOrNull_string_known() {
    assertTrue(stringValue("twenty-three").isBoolNumStringOrNull());
  }

  public void test_isBoolNumStringOrNull_string_unknown() {
    assertTrue(stringValue(null).isBoolNumStringOrNull());
  }

  public void test_lessThan_knownDouble_knownDouble_false() throws EvaluationException {
    assertLessThan(boolValue(false), doubleValue(2.0), doubleValue(1.0));
  }

  public void test_lessThan_knownDouble_knownDouble_true() throws EvaluationException {
    assertLessThan(boolValue(true), doubleValue(1.0), doubleValue(2.0));
  }

  public void test_lessThan_knownDouble_knownInt_false() throws EvaluationException {
    assertLessThan(boolValue(false), doubleValue(2.0), intValue(1));
  }

  public void test_lessThan_knownDouble_knownInt_true() throws EvaluationException {
    assertLessThan(boolValue(true), doubleValue(1.0), intValue(2));
  }

  public void test_lessThan_knownDouble_unknownDouble() throws EvaluationException {
    assertLessThan(boolValue(null), doubleValue(1.0), doubleValue(null));
  }

  public void test_lessThan_knownDouble_unknownInt() throws EvaluationException {
    assertLessThan(boolValue(null), doubleValue(1.0), intValue(null));
  }

  public void test_lessThan_knownInt_knownInt_false() throws EvaluationException {
    assertLessThan(boolValue(false), intValue(2), intValue(1));
  }

  public void test_lessThan_knownInt_knownInt_true() throws EvaluationException {
    assertLessThan(boolValue(true), intValue(1), intValue(2));
  }

  public void test_lessThan_knownInt_knownString() throws EvaluationException {
    assertLessThan(null, intValue(1), stringValue("2"));
  }

  public void test_lessThan_knownInt_unknownDouble() throws EvaluationException {
    assertLessThan(boolValue(null), intValue(1), doubleValue(null));
  }

  public void test_lessThan_knownInt_unknownInt() throws EvaluationException {
    assertLessThan(boolValue(null), intValue(1), intValue(null));
  }

  public void test_lessThan_knownString_knownInt() throws EvaluationException {
    assertLessThan(null, stringValue("1"), intValue(2));
  }

  public void test_lessThan_unknownDouble_knownDouble() throws EvaluationException {
    assertLessThan(boolValue(null), doubleValue(null), doubleValue(2.0));
  }

  public void test_lessThan_unknownDouble_knownInt() throws EvaluationException {
    assertLessThan(boolValue(null), doubleValue(null), intValue(2));
  }

  public void test_lessThan_unknownInt_knownDouble() throws EvaluationException {
    assertLessThan(boolValue(null), intValue(null), doubleValue(2.0));
  }

  public void test_lessThan_unknownInt_knownInt() throws EvaluationException {
    assertLessThan(boolValue(null), intValue(null), intValue(2));
  }

  public void test_lessThanOrEqual_knownDouble_knownDouble_false() throws EvaluationException {
    assertLessThanOrEqual(boolValue(false), doubleValue(2.0), doubleValue(1.0));
  }

  public void test_lessThanOrEqual_knownDouble_knownDouble_true() throws EvaluationException {
    assertLessThanOrEqual(boolValue(true), doubleValue(1.0), doubleValue(2.0));
  }

  public void test_lessThanOrEqual_knownDouble_knownInt_false() throws EvaluationException {
    assertLessThanOrEqual(boolValue(false), doubleValue(2.0), intValue(1));
  }

  public void test_lessThanOrEqual_knownDouble_knownInt_true() throws EvaluationException {
    assertLessThanOrEqual(boolValue(true), doubleValue(1.0), intValue(2));
  }

  public void test_lessThanOrEqual_knownDouble_unknownDouble() throws EvaluationException {
    assertLessThanOrEqual(boolValue(null), doubleValue(1.0), doubleValue(null));
  }

  public void test_lessThanOrEqual_knownDouble_unknownInt() throws EvaluationException {
    assertLessThanOrEqual(boolValue(null), doubleValue(1.0), intValue(null));
  }

  public void test_lessThanOrEqual_knownInt_knownInt_false() throws EvaluationException {
    assertLessThanOrEqual(boolValue(false), intValue(2), intValue(1));
  }

  public void test_lessThanOrEqual_knownInt_knownInt_true() throws EvaluationException {
    assertLessThanOrEqual(boolValue(true), intValue(1), intValue(2));
  }

  public void test_lessThanOrEqual_knownInt_knownString() throws EvaluationException {
    assertLessThanOrEqual(null, intValue(1), stringValue("2"));
  }

  public void test_lessThanOrEqual_knownInt_unknownDouble() throws EvaluationException {
    assertLessThanOrEqual(boolValue(null), intValue(1), doubleValue(null));
  }

  public void test_lessThanOrEqual_knownInt_unknownInt() throws EvaluationException {
    assertLessThanOrEqual(boolValue(null), intValue(1), intValue(null));
  }

  public void test_lessThanOrEqual_knownString_knownInt() throws EvaluationException {
    assertLessThanOrEqual(null, stringValue("1"), intValue(2));
  }

  public void test_lessThanOrEqual_unknownDouble_knownDouble() throws EvaluationException {
    assertLessThanOrEqual(boolValue(null), doubleValue(null), doubleValue(2.0));
  }

  public void test_lessThanOrEqual_unknownDouble_knownInt() throws EvaluationException {
    assertLessThanOrEqual(boolValue(null), doubleValue(null), intValue(2));
  }

  public void test_lessThanOrEqual_unknownInt_knownDouble() throws EvaluationException {
    assertLessThanOrEqual(boolValue(null), intValue(null), doubleValue(2.0));
  }

  public void test_lessThanOrEqual_unknownInt_knownInt() throws EvaluationException {
    assertLessThanOrEqual(boolValue(null), intValue(null), intValue(2));
  }

  public void test_logicalAnd_false_false() throws EvaluationException {
    assertLogicalAnd(boolValue(false), boolValue(false), boolValue(false));
  }

  public void test_logicalAnd_false_null() throws EvaluationException {
    try {
      assertLogicalAnd(boolValue(false), boolValue(false), nullValue());
      fail("Expected EvaluationException");
    } catch (EvaluationException exception) {
      // Expected
    }
  }

  public void test_logicalAnd_false_string() throws EvaluationException {
    try {
      assertLogicalAnd(boolValue(false), boolValue(false), stringValue("false"));
      fail("Expected EvaluationException");
    } catch (EvaluationException exception) {
      // Expected
    }
  }

  public void test_logicalAnd_false_true() throws EvaluationException {
    assertLogicalAnd(boolValue(false), boolValue(false), boolValue(true));
  }

  public void test_logicalAnd_null_false() throws EvaluationException {
    try {
      assertLogicalAnd(boolValue(false), nullValue(), boolValue(false));
      fail("Expected EvaluationException");
    } catch (EvaluationException exception) {
      // Expected
    }
  }

  public void test_logicalAnd_null_true() throws EvaluationException {
    try {
      assertLogicalAnd(boolValue(false), nullValue(), boolValue(true));
      fail("Expected EvaluationException");
    } catch (EvaluationException exception) {
      // Expected
    }
  }

  public void test_logicalAnd_string_false() throws EvaluationException {
    try {
      assertLogicalAnd(boolValue(false), stringValue("true"), boolValue(false));
      fail("Expected EvaluationException");
    } catch (EvaluationException exception) {
      // Expected
    }
  }

  public void test_logicalAnd_string_true() throws EvaluationException {
    try {
      assertLogicalAnd(boolValue(false), stringValue("false"), boolValue(true));
      fail("Expected EvaluationException");
    } catch (EvaluationException exception) {
      // Expected
    }
  }

  public void test_logicalAnd_true_false() throws EvaluationException {
    assertLogicalAnd(boolValue(false), boolValue(true), boolValue(false));
  }

  public void test_logicalAnd_true_null() throws EvaluationException {
    assertLogicalAnd(null, boolValue(true), nullValue());
  }

  public void test_logicalAnd_true_string() throws EvaluationException {
    try {
      assertLogicalAnd(boolValue(false), boolValue(true), stringValue("true"));
      fail("Expected EvaluationException");
    } catch (EvaluationException exception) {
      // Expected
    }
  }

  public void test_logicalAnd_true_true() throws EvaluationException {
    assertLogicalAnd(boolValue(true), boolValue(true), boolValue(true));
  }

  public void test_logicalNot_false() throws EvaluationException {
    assertLogicalNot(boolValue(true), boolValue(false));
  }

  public void test_logicalNot_null() throws EvaluationException {
    assertLogicalNot(null, nullValue());
  }

  public void test_logicalNot_string() throws EvaluationException {
    try {
      assertLogicalNot(boolValue(true), stringValue(null));
      fail("Expected EvaluationException");
    } catch (EvaluationException exception) {
      // Expected
    }
  }

  public void test_logicalNot_true() throws EvaluationException {
    assertLogicalNot(boolValue(false), boolValue(true));
  }

  public void test_logicalNot_unknown() throws EvaluationException {
    assertLogicalNot(boolValue(null), boolValue(null));
  }

  public void test_logicalOr_false_false() throws EvaluationException {
    assertLogicalOr(boolValue(false), boolValue(false), boolValue(false));
  }

  public void test_logicalOr_false_null() throws EvaluationException {
    assertLogicalOr(null, boolValue(false), nullValue());
  }

  public void test_logicalOr_false_string() throws EvaluationException {
    try {
      assertLogicalOr(boolValue(false), boolValue(false), stringValue("false"));
      fail("Expected EvaluationException");
    } catch (EvaluationException exception) {
      // Expected
    }
  }

  public void test_logicalOr_false_true() throws EvaluationException {
    assertLogicalOr(boolValue(true), boolValue(false), boolValue(true));
  }

  public void test_logicalOr_null_false() throws EvaluationException {
    try {
      assertLogicalOr(boolValue(false), nullValue(), boolValue(false));
      fail("Expected EvaluationException");
    } catch (EvaluationException exception) {
      // Expected
    }
  }

  public void test_logicalOr_null_true() throws EvaluationException {
    try {
      assertLogicalOr(boolValue(true), nullValue(), boolValue(true));
      fail("Expected EvaluationException");
    } catch (EvaluationException exception) {
      // Expected
    }
  }

  public void test_logicalOr_string_false() throws EvaluationException {
    try {
      assertLogicalOr(boolValue(false), stringValue("true"), boolValue(false));
      fail("Expected EvaluationException");
    } catch (EvaluationException exception) {
      // Expected
    }
  }

  public void test_logicalOr_string_true() throws EvaluationException {
    try {
      assertLogicalOr(boolValue(true), stringValue("false"), boolValue(true));
      fail("Expected EvaluationException");
    } catch (EvaluationException exception) {
      // Expected
    }
  }

  public void test_logicalOr_true_false() throws EvaluationException {
    assertLogicalOr(boolValue(true), boolValue(true), boolValue(false));
  }

  public void test_logicalOr_true_null() throws EvaluationException {
    try {
      assertLogicalOr(boolValue(true), boolValue(true), nullValue());
      fail("Expected EvaluationException");
    } catch (EvaluationException exception) {
      // Expected
    }
  }

  public void test_logicalOr_true_string() throws EvaluationException {
    try {
      assertLogicalOr(boolValue(true), boolValue(true), stringValue("true"));
      fail("Expected EvaluationException");
    } catch (EvaluationException exception) {
      // Expected
    }
  }

  public void test_logicalOr_true_true() throws EvaluationException {
    assertLogicalOr(boolValue(true), boolValue(true), boolValue(true));
  }

  public void test_minus_knownDouble_knownDouble() throws EvaluationException {
    assertMinus(doubleValue(1.0), doubleValue(4.0), doubleValue(3.0));
  }

  public void test_minus_knownDouble_knownInt() throws EvaluationException {
    assertMinus(doubleValue(1.0), doubleValue(4.0), intValue(3));
  }

  public void test_minus_knownDouble_unknownDouble() throws EvaluationException {
    assertMinus(doubleValue(null), doubleValue(4.0), doubleValue(null));
  }

  public void test_minus_knownDouble_unknownInt() throws EvaluationException {
    assertMinus(doubleValue(null), doubleValue(4.0), intValue(null));
  }

  public void test_minus_knownInt_knownInt() throws EvaluationException {
    assertMinus(intValue(1), intValue(4), intValue(3));
  }

  public void test_minus_knownInt_knownString() throws EvaluationException {
    assertMinus(null, intValue(4), stringValue("3"));
  }

  public void test_minus_knownInt_unknownDouble() throws EvaluationException {
    assertMinus(doubleValue(null), intValue(4), doubleValue(null));
  }

  public void test_minus_knownInt_unknownInt() throws EvaluationException {
    assertMinus(intValue(null), intValue(4), intValue(null));
  }

  public void test_minus_knownString_knownInt() throws EvaluationException {
    assertMinus(null, stringValue("4"), intValue(3));
  }

  public void test_minus_unknownDouble_knownDouble() throws EvaluationException {
    assertMinus(doubleValue(null), doubleValue(null), doubleValue(3.0));
  }

  public void test_minus_unknownDouble_knownInt() throws EvaluationException {
    assertMinus(doubleValue(null), doubleValue(null), intValue(3));
  }

  public void test_minus_unknownInt_knownDouble() throws EvaluationException {
    assertMinus(doubleValue(null), intValue(null), doubleValue(3.0));
  }

  public void test_minus_unknownInt_knownInt() throws EvaluationException {
    assertMinus(intValue(null), intValue(null), intValue(3));
  }

  public void test_negated_double_known() throws EvaluationException {
    assertNegated(doubleValue(2.0), doubleValue(-2.0));
  }

  public void test_negated_double_unknown() throws EvaluationException {
    assertNegated(doubleValue(null), doubleValue(null));
  }

  public void test_negated_int_known() throws EvaluationException {
    assertNegated(intValue(-3), intValue(3));
  }

  public void test_negated_int_unknown() throws EvaluationException {
    assertNegated(intValue(null), intValue(null));
  }

  public void test_negated_string() throws EvaluationException {
    assertNegated(null, stringValue(null));
  }

  public void test_notEqual_bool_false() throws EvaluationException {
    assertNotEqual(boolValue(false), boolValue(true), boolValue(true));
  }

  public void test_notEqual_bool_true() throws EvaluationException {
    assertNotEqual(boolValue(true), boolValue(false), boolValue(true));
  }

  public void test_notEqual_bool_unknown() throws EvaluationException {
    assertNotEqual(boolValue(null), boolValue(null), boolValue(false));
  }

  public void test_notEqual_double_false() throws EvaluationException {
    assertNotEqual(boolValue(false), doubleValue(2.0), doubleValue(2.0));
  }

  public void test_notEqual_double_true() throws EvaluationException {
    assertNotEqual(boolValue(true), doubleValue(2.0), doubleValue(4.0));
  }

  public void test_notEqual_double_unknown() throws EvaluationException {
    assertNotEqual(boolValue(null), doubleValue(1.0), doubleValue(null));
  }

  public void test_notEqual_int_false() throws EvaluationException {
    assertNotEqual(boolValue(false), intValue(5), intValue(5));
  }

  public void test_notEqual_int_true() throws EvaluationException {
    assertNotEqual(boolValue(true), intValue(-5), intValue(5));
  }

  public void test_notEqual_int_unknown() throws EvaluationException {
    assertNotEqual(boolValue(null), intValue(null), intValue(3));
  }

  public void test_notEqual_null() throws EvaluationException {
    assertNotEqual(boolValue(false), nullValue(), nullValue());
  }

  public void test_notEqual_string_false() throws EvaluationException {
    assertNotEqual(boolValue(false), stringValue("abc"), stringValue("abc"));
  }

  public void test_notEqual_string_true() throws EvaluationException {
    assertNotEqual(boolValue(true), stringValue("abc"), stringValue("def"));
  }

  public void test_notEqual_string_unknown() throws EvaluationException {
    assertNotEqual(boolValue(null), stringValue(null), stringValue("def"));
  }

  public void test_performToString_bool_false() throws EvaluationException {
    assertPerformToString(stringValue("false"), boolValue(false));
  }

  public void test_performToString_bool_true() throws EvaluationException {
    assertPerformToString(stringValue("true"), boolValue(true));
  }

  public void test_performToString_bool_unknown() throws EvaluationException {
    assertPerformToString(stringValue(null), boolValue(null));
  }

  public void test_performToString_double_known() throws EvaluationException {
    assertPerformToString(stringValue("2.0"), doubleValue(2.0));
  }

  public void test_performToString_double_unknown() throws EvaluationException {
    assertPerformToString(stringValue(null), doubleValue(null));
  }

  public void test_performToString_int_known() throws EvaluationException {
    assertPerformToString(stringValue("5"), intValue(5));
  }

  public void test_performToString_int_unknown() throws EvaluationException {
    assertPerformToString(stringValue(null), intValue(null));
  }

  public void test_performToString_null() throws EvaluationException {
    assertPerformToString(stringValue("null"), nullValue());
  }

  public void test_performToString_string_known() throws EvaluationException {
    assertPerformToString(stringValue("abc"), stringValue("abc"));
  }

  public void test_performToString_string_unknown() throws EvaluationException {
    assertPerformToString(stringValue(null), stringValue(null));
  }

  public void test_remainder_knownDouble_knownDouble() throws EvaluationException {
    assertRemainder(doubleValue(1.0), doubleValue(7.0), doubleValue(2.0));
  }

  public void test_remainder_knownDouble_knownInt() throws EvaluationException {
    assertRemainder(doubleValue(1.0), doubleValue(7.0), intValue(2));
  }

  public void test_remainder_knownDouble_unknownDouble() throws EvaluationException {
    assertRemainder(doubleValue(null), doubleValue(7.0), doubleValue(null));
  }

  public void test_remainder_knownDouble_unknownInt() throws EvaluationException {
    assertRemainder(doubleValue(null), doubleValue(6.0), intValue(null));
  }

  public void test_remainder_knownInt_knownInt() throws EvaluationException {
    assertRemainder(intValue(1), intValue(7), intValue(2));
  }

  public void test_remainder_knownInt_knownString() throws EvaluationException {
    assertRemainder(null, intValue(7), stringValue("2"));
  }

  public void test_remainder_knownInt_unknownDouble() throws EvaluationException {
    assertRemainder(doubleValue(null), intValue(7), doubleValue(null));
  }

  public void test_remainder_knownInt_unknownInt() throws EvaluationException {
    assertRemainder(intValue(null), intValue(7), intValue(null));
  }

  public void test_remainder_knownString_knownInt() throws EvaluationException {
    assertRemainder(null, stringValue("7"), intValue(2));
  }

  public void test_remainder_unknownDouble_knownDouble() throws EvaluationException {
    assertRemainder(doubleValue(null), doubleValue(null), doubleValue(2.0));
  }

  public void test_remainder_unknownDouble_knownInt() throws EvaluationException {
    assertRemainder(doubleValue(null), doubleValue(null), intValue(2));
  }

  public void test_remainder_unknownInt_knownDouble() throws EvaluationException {
    assertRemainder(doubleValue(null), intValue(null), doubleValue(2.0));
  }

  public void test_remainder_unknownInt_knownInt() throws EvaluationException {
    assertRemainder(intValue(null), intValue(null), intValue(2));
  }

  public void test_shiftLeft_knownInt_knownInt() throws EvaluationException {
    assertShiftLeft(intValue(48), intValue(6), intValue(3));
  }

  public void test_shiftLeft_knownInt_knownString() throws EvaluationException {
    assertShiftLeft(null, intValue(6), stringValue(null));
  }

  public void test_shiftLeft_knownInt_tooLarge() throws EvaluationException {
    assertShiftLeft(intValue(null), intValue(6), new DartObjectImpl(
        typeProvider.getIntType(),
        new IntState(BigInteger.valueOf(Long.MAX_VALUE))));
  }

  public void test_shiftLeft_knownInt_unknownInt() throws EvaluationException {
    assertShiftLeft(intValue(null), intValue(6), intValue(null));
  }

  public void test_shiftLeft_knownString_knownInt() throws EvaluationException {
    assertShiftLeft(null, stringValue(null), intValue(3));
  }

  public void test_shiftLeft_unknownInt_knownInt() throws EvaluationException {
    assertShiftLeft(intValue(null), intValue(null), intValue(3));
  }

  public void test_shiftLeft_unknownInt_unknownInt() throws EvaluationException {
    assertShiftLeft(intValue(null), intValue(null), intValue(null));
  }

  public void test_shiftRight_knownInt_knownInt() throws EvaluationException {
    assertShiftRight(intValue(6), intValue(48), intValue(3));
  }

  public void test_shiftRight_knownInt_knownString() throws EvaluationException {
    assertShiftRight(null, intValue(48), stringValue(null));
  }

  public void test_shiftRight_knownInt_tooLarge() throws EvaluationException {
    assertShiftRight(intValue(null), intValue(48), new DartObjectImpl(
        typeProvider.getIntType(),
        new IntState(BigInteger.valueOf(Long.MAX_VALUE))));
  }

  public void test_shiftRight_knownInt_unknownInt() throws EvaluationException {
    assertShiftRight(intValue(null), intValue(48), intValue(null));
  }

  public void test_shiftRight_knownString_knownInt() throws EvaluationException {
    assertShiftRight(null, stringValue(null), intValue(3));
  }

  public void test_shiftRight_unknownInt_knownInt() throws EvaluationException {
    assertShiftRight(intValue(null), intValue(null), intValue(3));
  }

  public void test_shiftRight_unknownInt_unknownInt() throws EvaluationException {
    assertShiftRight(intValue(null), intValue(null), intValue(null));
  }

  public void test_stringLength_int() throws EvaluationException {
    try {
      assertStringLength(intValue(null), intValue(0));
      fail("Expected EvaluationException");
    } catch (EvaluationException exception) {
      // Expected
    }
  }

  public void test_stringLength_knownString() throws EvaluationException {
    assertStringLength(intValue(3), stringValue("abc"));
  }

  public void test_stringLength_unknownString() throws EvaluationException {
    assertStringLength(intValue(null), stringValue(null));
  }

  public void test_times_knownDouble_knownDouble() throws EvaluationException {
    assertTimes(doubleValue(6.0), doubleValue(2.0), doubleValue(3.0));
  }

  public void test_times_knownDouble_knownInt() throws EvaluationException {
    assertTimes(doubleValue(6.0), doubleValue(2.0), intValue(3));
  }

  public void test_times_knownDouble_unknownDouble() throws EvaluationException {
    assertTimes(doubleValue(null), doubleValue(2.0), doubleValue(null));
  }

  public void test_times_knownDouble_unknownInt() throws EvaluationException {
    assertTimes(doubleValue(null), doubleValue(2.0), intValue(null));
  }

  public void test_times_knownInt_knownInt() throws EvaluationException {
    assertTimes(intValue(6), intValue(2), intValue(3));
  }

  public void test_times_knownInt_knownString() throws EvaluationException {
    assertTimes(null, intValue(2), stringValue("3"));
  }

  public void test_times_knownInt_unknownDouble() throws EvaluationException {
    assertTimes(doubleValue(null), intValue(2), doubleValue(null));
  }

  public void test_times_knownInt_unknownInt() throws EvaluationException {
    assertTimes(intValue(null), intValue(2), intValue(null));
  }

  public void test_times_knownString_knownInt() throws EvaluationException {
    assertTimes(null, stringValue("2"), intValue(3));
  }

  public void test_times_unknownDouble_knownDouble() throws EvaluationException {
    assertTimes(doubleValue(null), doubleValue(null), doubleValue(3.0));
  }

  public void test_times_unknownDouble_knownInt() throws EvaluationException {
    assertTimes(doubleValue(null), doubleValue(null), intValue(3));
  }

  public void test_times_unknownInt_knownDouble() throws EvaluationException {
    assertTimes(doubleValue(null), intValue(null), doubleValue(3.0));
  }

  public void test_times_unknownInt_knownInt() throws EvaluationException {
    assertTimes(intValue(null), intValue(null), intValue(3));
  }

  /**
   * Assert that the result of adding the left and right operands is the expected value, or that the
   * operation throws an exception if the expected value is {@code null}.
   * 
   * @param expected the expected result of the operation
   * @param leftOperand the left operand to the operation
   * @param rightOperand the left operand to the operation
   * @throws EvaluationException if the result is an exception when it should not be
   */
  private void assertAdd(DartObjectImpl expected, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) throws EvaluationException {
    if (expected == null) {
      try {
        leftOperand.add(typeProvider, rightOperand);
        fail("Expected an EvaluationException");
      } catch (EvaluationException exception) {
        // Expected
      }
    } else {
      DartObjectImpl result = leftOperand.add(typeProvider, rightOperand);
      assertNotNull(result);
      assertEquals(expected, result);
    }
  }

  /**
   * Assert that the result of bit-anding the left and right operands is the expected value, or that
   * the operation throws an exception if the expected value is {@code null}.
   * 
   * @param expected the expected result of the operation
   * @param leftOperand the left operand to the operation
   * @param rightOperand the left operand to the operation
   * @throws EvaluationException if the result is an exception when it should not be
   */
  private void assertBitAnd(DartObjectImpl expected, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) throws EvaluationException {
    if (expected == null) {
      try {
        leftOperand.bitAnd(typeProvider, rightOperand);
        fail("Expected an EvaluationException");
      } catch (EvaluationException exception) {
        // Expected
      }
    } else {
      DartObjectImpl result = leftOperand.bitAnd(typeProvider, rightOperand);
      assertNotNull(result);
      assertEquals(expected, result);
    }
  }

  /**
   * Assert that the bit-not of the operand is the expected value, or that the operation throws an
   * exception if the expected value is {@code null}.
   * 
   * @param expected the expected result of the operation
   * @param operand the operand to the operation
   * @throws EvaluationException if the result is an exception when it should not be
   */
  private void assertBitNot(DartObjectImpl expected, DartObjectImpl operand)
      throws EvaluationException {
    if (expected == null) {
      try {
        operand.bitNot(typeProvider);
        fail("Expected an EvaluationException");
      } catch (EvaluationException exception) {
        // Expected
      }
    } else {
      DartObjectImpl result = operand.bitNot(typeProvider);
      assertNotNull(result);
      assertEquals(expected, result);
    }
  }

  /**
   * Assert that the result of bit-oring the left and right operands is the expected value, or that
   * the operation throws an exception if the expected value is {@code null}.
   * 
   * @param expected the expected result of the operation
   * @param leftOperand the left operand to the operation
   * @param rightOperand the left operand to the operation
   * @throws EvaluationException if the result is an exception when it should not be
   */
  private void assertBitOr(DartObjectImpl expected, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) throws EvaluationException {
    if (expected == null) {
      try {
        leftOperand.bitOr(typeProvider, rightOperand);
        fail("Expected an EvaluationException");
      } catch (EvaluationException exception) {
        // Expected
      }
    } else {
      DartObjectImpl result = leftOperand.bitOr(typeProvider, rightOperand);
      assertNotNull(result);
      assertEquals(expected, result);
    }
  }

  /**
   * Assert that the result of bit-xoring the left and right operands is the expected value, or that
   * the operation throws an exception if the expected value is {@code null}.
   * 
   * @param expected the expected result of the operation
   * @param leftOperand the left operand to the operation
   * @param rightOperand the left operand to the operation
   * @throws EvaluationException if the result is an exception when it should not be
   */
  private void assertBitXor(DartObjectImpl expected, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) throws EvaluationException {
    if (expected == null) {
      try {
        leftOperand.bitXor(typeProvider, rightOperand);
        fail("Expected an EvaluationException");
      } catch (EvaluationException exception) {
        // Expected
      }
    } else {
      DartObjectImpl result = leftOperand.bitXor(typeProvider, rightOperand);
      assertNotNull(result);
      assertEquals(expected, result);
    }
  }

  /**
   * Assert that the result of concatenating the left and right operands is the expected value, or
   * that the operation throws an exception if the expected value is {@code null}.
   * 
   * @param expected the expected result of the operation
   * @param leftOperand the left operand to the operation
   * @param rightOperand the left operand to the operation
   * @throws EvaluationException if the result is an exception when it should not be
   */
  private void assertConcatenate(DartObjectImpl expected, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) throws EvaluationException {
    if (expected == null) {
      try {
        leftOperand.concatenate(typeProvider, rightOperand);
        fail("Expected an EvaluationException");
      } catch (EvaluationException exception) {
        // Expected
      }
    } else {
      DartObjectImpl result = leftOperand.concatenate(typeProvider, rightOperand);
      assertNotNull(result);
      assertEquals(expected, result);
    }
  }

  /**
   * Assert that the result of dividing the left and right operands is the expected value, or that
   * the operation throws an exception if the expected value is {@code null}.
   * 
   * @param expected the expected result of the operation
   * @param leftOperand the left operand to the operation
   * @param rightOperand the left operand to the operation
   * @throws EvaluationException if the result is an exception when it should not be
   */
  private void assertDivide(DartObjectImpl expected, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) throws EvaluationException {
    if (expected == null) {
      try {
        leftOperand.divide(typeProvider, rightOperand);
        fail("Expected an EvaluationException");
      } catch (EvaluationException exception) {
        // Expected
      }
    } else {
      DartObjectImpl result = leftOperand.divide(typeProvider, rightOperand);
      assertNotNull(result);
      assertEquals(expected, result);
    }
  }

  /**
   * Assert that the result of comparing the left and right operands for equality is the expected
   * value, or that the operation throws an exception if the expected value is {@code null}.
   * 
   * @param expected the expected result of the operation
   * @param leftOperand the left operand to the operation
   * @param rightOperand the left operand to the operation
   * @throws EvaluationException if the result is an exception when it should not be
   */
  private void assertEqualEqual(DartObjectImpl expected, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) throws EvaluationException {
    if (expected == null) {
      try {
        leftOperand.equalEqual(typeProvider, rightOperand);
        fail("Expected an EvaluationException");
      } catch (EvaluationException exception) {
        // Expected
      }
    } else {
      DartObjectImpl result = leftOperand.equalEqual(typeProvider, rightOperand);
      assertNotNull(result);
      assertEquals(expected, result);
    }
  }

  /**
   * Assert that the result of comparing the left and right operands is the expected value, or that
   * the operation throws an exception if the expected value is {@code null}.
   * 
   * @param expected the expected result of the operation
   * @param leftOperand the left operand to the operation
   * @param rightOperand the left operand to the operation
   * @throws EvaluationException if the result is an exception when it should not be
   */
  private void assertGreaterThan(DartObjectImpl expected, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) throws EvaluationException {
    if (expected == null) {
      try {
        leftOperand.greaterThan(typeProvider, rightOperand);
        fail("Expected an EvaluationException");
      } catch (EvaluationException exception) {
        // Expected
      }
    } else {
      DartObjectImpl result = leftOperand.greaterThan(typeProvider, rightOperand);
      assertNotNull(result);
      assertEquals(expected, result);
    }
  }

  /**
   * Assert that the result of comparing the left and right operands is the expected value, or that
   * the operation throws an exception if the expected value is {@code null}.
   * 
   * @param expected the expected result of the operation
   * @param leftOperand the left operand to the operation
   * @param rightOperand the left operand to the operation
   * @throws EvaluationException if the result is an exception when it should not be
   */
  private void assertGreaterThanOrEqual(DartObjectImpl expected, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) throws EvaluationException {
    if (expected == null) {
      try {
        leftOperand.greaterThanOrEqual(typeProvider, rightOperand);
        fail("Expected an EvaluationException");
      } catch (EvaluationException exception) {
        // Expected
      }
    } else {
      DartObjectImpl result = leftOperand.greaterThanOrEqual(typeProvider, rightOperand);
      assertNotNull(result);
      assertEquals(expected, result);
    }
  }

  @DartBlockBody({"// TODO(scheglov) implement"})
  private void assertInstanceOfObjectArray(Object result) {
    assertInstanceOf(Object[].class, result);
  }

  /**
   * Assert that the result of dividing the left and right operands as integers is the expected
   * value, or that the operation throws an exception if the expected value is {@code null}.
   * 
   * @param expected the expected result of the operation
   * @param leftOperand the left operand to the operation
   * @param rightOperand the left operand to the operation
   * @throws EvaluationException if the result is an exception when it should not be
   */
  private void assertIntegerDivide(DartObjectImpl expected, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) throws EvaluationException {
    if (expected == null) {
      try {
        leftOperand.integerDivide(typeProvider, rightOperand);
        fail("Expected an EvaluationException");
      } catch (EvaluationException exception) {
        // Expected
      }
    } else {
      DartObjectImpl result = leftOperand.integerDivide(typeProvider, rightOperand);
      assertNotNull(result);
      assertEquals(expected, result);
    }
  }

  /**
   * Assert that the result of comparing the left and right operands is the expected value, or that
   * the operation throws an exception if the expected value is {@code null}.
   * 
   * @param expected the expected result of the operation
   * @param leftOperand the left operand to the operation
   * @param rightOperand the left operand to the operation
   * @throws EvaluationException if the result is an exception when it should not be
   */
  private void assertLessThan(DartObjectImpl expected, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) throws EvaluationException {
    if (expected == null) {
      try {
        leftOperand.lessThan(typeProvider, rightOperand);
        fail("Expected an EvaluationException");
      } catch (EvaluationException exception) {
        // Expected
      }
    } else {
      DartObjectImpl result = leftOperand.lessThan(typeProvider, rightOperand);
      assertNotNull(result);
      assertEquals(expected, result);
    }
  }

  /**
   * Assert that the result of comparing the left and right operands is the expected value, or that
   * the operation throws an exception if the expected value is {@code null}.
   * 
   * @param expected the expected result of the operation
   * @param leftOperand the left operand to the operation
   * @param rightOperand the left operand to the operation
   * @throws EvaluationException if the result is an exception when it should not be
   */
  private void assertLessThanOrEqual(DartObjectImpl expected, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) throws EvaluationException {
    if (expected == null) {
      try {
        leftOperand.lessThanOrEqual(typeProvider, rightOperand);
        fail("Expected an EvaluationException");
      } catch (EvaluationException exception) {
        // Expected
      }
    } else {
      DartObjectImpl result = leftOperand.lessThanOrEqual(typeProvider, rightOperand);
      assertNotNull(result);
      assertEquals(expected, result);
    }
  }

  /**
   * Assert that the result of logical-anding the left and right operands is the expected value, or
   * that the operation throws an exception if the expected value is {@code null}.
   * 
   * @param expected the expected result of the operation
   * @param leftOperand the left operand to the operation
   * @param rightOperand the left operand to the operation
   * @throws EvaluationException if the result is an exception when it should not be
   */
  private void assertLogicalAnd(DartObjectImpl expected, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) throws EvaluationException {
    if (expected == null) {
      try {
        leftOperand.logicalAnd(typeProvider, rightOperand);
        fail("Expected an EvaluationException");
      } catch (EvaluationException exception) {
        // Expected
      }
    } else {
      DartObjectImpl result = leftOperand.logicalAnd(typeProvider, rightOperand);
      assertNotNull(result);
      assertEquals(expected, result);
    }
  }

  /**
   * Assert that the logical-not of the operand is the expected value, or that the operation throws
   * an exception if the expected value is {@code null}.
   * 
   * @param expected the expected result of the operation
   * @param operand the operand to the operation
   * @throws EvaluationException if the result is an exception when it should not be
   */
  private void assertLogicalNot(DartObjectImpl expected, DartObjectImpl operand)
      throws EvaluationException {
    if (expected == null) {
      try {
        operand.logicalNot(typeProvider);
        fail("Expected an EvaluationException");
      } catch (EvaluationException exception) {
        // Expected
      }
    } else {
      DartObjectImpl result = operand.logicalNot(typeProvider);
      assertNotNull(result);
      assertEquals(expected, result);
    }
  }

  /**
   * Assert that the result of logical-oring the left and right operands is the expected value, or
   * that the operation throws an exception if the expected value is {@code null}.
   * 
   * @param expected the expected result of the operation
   * @param leftOperand the left operand to the operation
   * @param rightOperand the left operand to the operation
   * @throws EvaluationException if the result is an exception when it should not be
   */
  private void assertLogicalOr(DartObjectImpl expected, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) throws EvaluationException {
    if (expected == null) {
      try {
        leftOperand.logicalOr(typeProvider, rightOperand);
        fail("Expected an EvaluationException");
      } catch (EvaluationException exception) {
        // Expected
      }
    } else {
      DartObjectImpl result = leftOperand.logicalOr(typeProvider, rightOperand);
      assertNotNull(result);
      assertEquals(expected, result);
    }
  }

  /**
   * Assert that the result of subtracting the left and right operands is the expected value, or
   * that the operation throws an exception if the expected value is {@code null}.
   * 
   * @param expected the expected result of the operation
   * @param leftOperand the left operand to the operation
   * @param rightOperand the left operand to the operation
   * @throws EvaluationException if the result is an exception when it should not be
   */
  private void assertMinus(DartObjectImpl expected, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) throws EvaluationException {
    if (expected == null) {
      try {
        leftOperand.minus(typeProvider, rightOperand);
        fail("Expected an EvaluationException");
      } catch (EvaluationException exception) {
        // Expected
      }
    } else {
      DartObjectImpl result = leftOperand.minus(typeProvider, rightOperand);
      assertNotNull(result);
      assertEquals(expected, result);
    }
  }

  /**
   * Assert that the negation of the operand is the expected value, or that the operation throws an
   * exception if the expected value is {@code null}.
   * 
   * @param expected the expected result of the operation
   * @param operand the operand to the operation
   * @throws EvaluationException if the result is an exception when it should not be
   */
  private void assertNegated(DartObjectImpl expected, DartObjectImpl operand)
      throws EvaluationException {
    if (expected == null) {
      try {
        operand.negated(typeProvider);
        fail("Expected an EvaluationException");
      } catch (EvaluationException exception) {
        // Expected
      }
    } else {
      DartObjectImpl result = operand.negated(typeProvider);
      assertNotNull(result);
      assertEquals(expected, result);
    }
  }

  /**
   * Assert that the result of comparing the left and right operands for inequality is the expected
   * value, or that the operation throws an exception if the expected value is {@code null}.
   * 
   * @param expected the expected result of the operation
   * @param leftOperand the left operand to the operation
   * @param rightOperand the left operand to the operation
   * @throws EvaluationException if the result is an exception when it should not be
   */
  private void assertNotEqual(DartObjectImpl expected, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) throws EvaluationException {
    if (expected == null) {
      try {
        leftOperand.notEqual(typeProvider, rightOperand);
        fail("Expected an EvaluationException");
      } catch (EvaluationException exception) {
        // Expected
      }
    } else {
      DartObjectImpl result = leftOperand.notEqual(typeProvider, rightOperand);
      assertNotNull(result);
      assertEquals(expected, result);
    }
  }

  /**
   * Assert that converting the operand to a string is the expected value, or that the operation
   * throws an exception if the expected value is {@code null}.
   * 
   * @param expected the expected result of the operation
   * @param operand the operand to the operation
   * @throws EvaluationException if the result is an exception when it should not be
   */
  private void assertPerformToString(DartObjectImpl expected, DartObjectImpl operand)
      throws EvaluationException {
    if (expected == null) {
      try {
        operand.performToString(typeProvider);
        fail("Expected an EvaluationException");
      } catch (EvaluationException exception) {
        // Expected
      }
    } else {
      DartObjectImpl result = operand.performToString(typeProvider);
      assertNotNull(result);
      assertEquals(expected, result);
    }
  }

  /**
   * Assert that the result of taking the remainder of the left and right operands is the expected
   * value, or that the operation throws an exception if the expected value is {@code null}.
   * 
   * @param expected the expected result of the operation
   * @param leftOperand the left operand to the operation
   * @param rightOperand the left operand to the operation
   * @throws EvaluationException if the result is an exception when it should not be
   */
  private void assertRemainder(DartObjectImpl expected, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) throws EvaluationException {
    if (expected == null) {
      try {
        leftOperand.remainder(typeProvider, rightOperand);
        fail("Expected an EvaluationException");
      } catch (EvaluationException exception) {
        // Expected
      }
    } else {
      DartObjectImpl result = leftOperand.remainder(typeProvider, rightOperand);
      assertNotNull(result);
      assertEquals(expected, result);
    }
  }

  /**
   * Assert that the result of multiplying the left and right operands is the expected value, or
   * that the operation throws an exception if the expected value is {@code null}.
   * 
   * @param expected the expected result of the operation
   * @param leftOperand the left operand to the operation
   * @param rightOperand the left operand to the operation
   * @throws EvaluationException if the result is an exception when it should not be
   */
  private void assertShiftLeft(DartObjectImpl expected, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) throws EvaluationException {
    if (expected == null) {
      try {
        leftOperand.shiftLeft(typeProvider, rightOperand);
        fail("Expected an EvaluationException");
      } catch (EvaluationException exception) {
        // Expected
      }
    } else {
      DartObjectImpl result = leftOperand.shiftLeft(typeProvider, rightOperand);
      assertNotNull(result);
      assertEquals(expected, result);
    }
  }

  /**
   * Assert that the result of multiplying the left and right operands is the expected value, or
   * that the operation throws an exception if the expected value is {@code null}.
   * 
   * @param expected the expected result of the operation
   * @param leftOperand the left operand to the operation
   * @param rightOperand the left operand to the operation
   * @throws EvaluationException if the result is an exception when it should not be
   */
  private void assertShiftRight(DartObjectImpl expected, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) throws EvaluationException {
    if (expected == null) {
      try {
        leftOperand.shiftRight(typeProvider, rightOperand);
        fail("Expected an EvaluationException");
      } catch (EvaluationException exception) {
        // Expected
      }
    } else {
      DartObjectImpl result = leftOperand.shiftRight(typeProvider, rightOperand);
      assertNotNull(result);
      assertEquals(expected, result);
    }
  }

  /**
   * Assert that the length of the operand is the expected value, or that the operation throws an
   * exception if the expected value is {@code null}.
   * 
   * @param expected the expected result of the operation
   * @param operand the operand to the operation
   * @throws EvaluationException if the result is an exception when it should not be
   */
  private void assertStringLength(DartObjectImpl expected, DartObjectImpl operand)
      throws EvaluationException {
    if (expected == null) {
      try {
        operand.stringLength(typeProvider);
        fail("Expected an EvaluationException");
      } catch (EvaluationException exception) {
        // Expected
      }
    } else {
      DartObjectImpl result = operand.stringLength(typeProvider);
      assertNotNull(result);
      assertEquals(expected, result);
    }
  }

  /**
   * Assert that the result of multiplying the left and right operands is the expected value, or
   * that the operation throws an exception if the expected value is {@code null}.
   * 
   * @param expected the expected result of the operation
   * @param leftOperand the left operand to the operation
   * @param rightOperand the left operand to the operation
   * @throws EvaluationException if the result is an exception when it should not be
   */
  private void assertTimes(DartObjectImpl expected, DartObjectImpl leftOperand,
      DartObjectImpl rightOperand) throws EvaluationException {
    if (expected == null) {
      try {
        leftOperand.times(typeProvider, rightOperand);
        fail("Expected an EvaluationException");
      } catch (EvaluationException exception) {
        // Expected
      }
    } else {
      DartObjectImpl result = leftOperand.times(typeProvider, rightOperand);
      assertNotNull(result);
      assertEquals(expected, result);
    }
  }

  private DartObjectImpl boolValue(Boolean value) {
    if (value == null) {
      return new DartObjectImpl(typeProvider.getBoolType(), BoolState.UNKNOWN_VALUE);
    } else if (value == Boolean.FALSE) {
      return new DartObjectImpl(typeProvider.getBoolType(), BoolState.FALSE_STATE);
    } else if (value == Boolean.TRUE) {
      return new DartObjectImpl(typeProvider.getBoolType(), BoolState.TRUE_STATE);
    }
    fail("Invalid boolean value used in test");
    return null;
  }

  private DartObjectImpl doubleValue(Double value) {
    if (value == null) {
      return new DartObjectImpl(typeProvider.getDoubleType(), DoubleState.UNKNOWN_VALUE);
    } else {
      return new DartObjectImpl(typeProvider.getDoubleType(), new DoubleState(value));
    }
  }

  private DartObjectImpl dynamicValue() {
    return new DartObjectImpl(typeProvider.getNullType(), DynamicState.DYNAMIC_STATE);
  }

  private DartObjectImpl intValue(Integer value) {
    if (value == null) {
      return new DartObjectImpl(typeProvider.getIntType(), IntState.UNKNOWN_VALUE);
    } else {
      return new DartObjectImpl(typeProvider.getIntType(), new IntState(
          BigInteger.valueOf(value.longValue())));
    }
  }

  private DartObjectImpl listValue(DartObjectImpl... elements) {
    return new DartObjectImpl(typeProvider.getListType(), new ListState(elements));
  }

  private DartObjectImpl mapValue(DartObjectImpl... keyElementPairs) {
    HashMap<DartObjectImpl, DartObjectImpl> map = new HashMap<DartObjectImpl, DartObjectImpl>();
    int count = keyElementPairs.length;
    for (int i = 0; i < count;) {
      map.put(keyElementPairs[i++], keyElementPairs[i++]);
    }
    return new DartObjectImpl(typeProvider.getMapType(), new MapState(map));
  }

  private DartObjectImpl nullValue() {
    return new DartObjectImpl(typeProvider.getNullType(), NullState.NULL_STATE);
  }

  private DartObjectImpl numValue() {
    return new DartObjectImpl(typeProvider.getNullType(), NumState.UNKNOWN_VALUE);
  }

  private DartObjectImpl stringValue(String value) {
    if (value == null) {
      return new DartObjectImpl(typeProvider.getStringType(), StringState.UNKNOWN_VALUE);
    } else {
      return new DartObjectImpl(typeProvider.getStringType(), new StringState(value));
    }
  }

  private DartObjectImpl symbolValue(String value) {
    return new DartObjectImpl(typeProvider.getSymbolType(), new SymbolState(value));
  }
}
