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
package com.google.dart.engine.resolver;

import com.google.dart.engine.error.StaticTypeWarningCode;
import com.google.dart.engine.internal.context.AnalysisOptionsImpl;
import com.google.dart.engine.source.Source;

/**
 * The class {@code StrictModeTest} contains tests to ensure that the correct errors and warnings
 * are reported when the analysis engine is run in strict mode.
 */
public class StrictModeTest extends ResolverTestCase {
  public void fail_for() throws Exception {
    Source source = addSource(createSource(//
        "int f(List<int> list) {",
        "  num sum = 0;",
        "  for (num i = 0; i < list.length; i++) {",
        "    sum += list[i];",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_OPERATOR);
  }

  @Override
  public void setUp() {
    super.setUp();
    AnalysisOptionsImpl options = new AnalysisOptionsImpl();
    options.setStrictMode(true);
    options.setHint(false);
    getAnalysisContext().setAnalysisOptions(options);
  }

  public void test_assert_is() throws Exception {
    Source source = addSource(createSource(//
        "int f(num n) {",
        "  assert (n is int);",
        "  return n & 0x0F;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_OPERATOR);
  }

  public void test_conditional_and_is() throws Exception {
    Source source = addSource(createSource(//
        "int f(num n) {",
        "  return (n is int && n > 0) ? n & 0x0F : 0;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_OPERATOR);
  }

  public void test_conditional_is() throws Exception {
    Source source = addSource(createSource(//
        "int f(num n) {",
        "  return (n is int) ? n & 0x0F : 0;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_OPERATOR);
  }

  public void test_conditional_isNot() throws Exception {
    Source source = addSource(createSource(//
        "int f(num n) {",
        "  return (n is! int) ? 0 : n & 0x0F;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_OPERATOR);
  }

  public void test_conditional_or_is() throws Exception {
    Source source = addSource(createSource(//
        "int f(num n) {",
        "  return (n is! int || n < 0) ? 0 : n & 0x0F;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_OPERATOR);
  }

  public void test_forEach() throws Exception {
    Source source = addSource(createSource(//
        "int f(List<int> list) {",
        "  num sum = 0;",
        "  for (num n in list) {",
        "    sum += n & 0x0F;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_OPERATOR);
  }

  public void test_if_and_is() throws Exception {
    Source source = addSource(createSource(//
        "int f(num n) {",
        "  if (n is int && n > 0) {",
        "    return n & 0x0F;",
        "  }",
        "  return 0;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_OPERATOR);
  }

  public void test_if_is() throws Exception {
    Source source = addSource(createSource(//
        "int f(num n) {",
        "  if (n is int) {",
        "    return n & 0x0F;",
        "  }",
        "  return 0;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_OPERATOR);
  }

  public void test_if_isNot() throws Exception {
    Source source = addSource(createSource(//
        "int f(num n) {",
        "  if (n is! int) {",
        "    return 0;",
        "  } else {",
        "    return n & 0x0F;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_OPERATOR);
  }

  public void test_if_isNot_abrupt() throws Exception {
    Source source = addSource(createSource(//
        "int f(num n) {",
        "  if (n is! int) {",
        "    return 0;",
        "  }",
        "  return n & 0x0F;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_OPERATOR);
  }

  public void test_if_or_is() throws Exception {
    Source source = addSource(createSource(//
        "int f(num n) {",
        "  if (n is! int || n < 0) {",
        "    return 0;",
        "  } else {",
        "    return n & 0x0F;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_OPERATOR);
  }

  public void test_localVar() throws Exception {
    Source source = addSource(createSource(//
        "int f() {",
        "  num n = 1234;",
        "  return n & 0x0F;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_OPERATOR);
  }
}
