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
package com.google.dart.java2dart.processor;

import org.apache.commons.lang3.StringUtils;

import java.io.File;

/**
 * Test for {@link JUnitSemanticProcessor}.
 */
public class JUnitSemanticProcessorTest extends SemanticProcessorTest {
  public void test_Assert() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import junit.framework.Assert;",
        "import junit.framework.TestCase;",
        "public class Test extends TestCase {",
        "  public void test_x() {",
        "    fail(\"msg\");",
        "    Assert.fail(\"msg\");",
        "  }",
        "}");
    JUnitSemanticProcessor.INSTANCE.process(context, unit);
    String resultSource = getFormattedSource(unit);
    resultSource = StringUtils.substringBefore(resultSource, "static dartSuite() {");
    assertEquals(toString(//
        "class Test extends JUnitTestCase {",
        "  void test_x() {",
        "    JUnitTestCase.fail(\"msg\");",
        "    JUnitTestCase.fail(\"msg\");",
        "  }",
        "  "), resultSource);
  }

  public void test_assertTrueFalse() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import junit.framework.TestCase;",
        "public class Test extends TestCase {",
        "  public void test_assertTrue() {",
        "    boolean v = true;",
        "    assertTrue(v);",
        "    assertTrue(\"msg\", v);",
        "  }",
        "  public void test_assertFalse() {",
        "    boolean v = false;",
        "    assertFalse(v);",
        "    assertFalse(\"msg\", v);",
        "  }",
        "}");
    JUnitSemanticProcessor.INSTANCE.process(context, unit);
    assertFormattedSource(
        "class Test extends JUnitTestCase {",
        "  void test_assertTrue() {",
        "    bool v = true;",
        "    JUnitTestCase.assertTrue(v);",
        "    JUnitTestCase.assertTrueMsg(\"msg\", v);",
        "  }",
        "  void test_assertFalse() {",
        "    bool v = false;",
        "    JUnitTestCase.assertFalse(v);",
        "    JUnitTestCase.assertFalseMsg(\"msg\", v);",
        "  }",
        "  static dartSuite() {",
        "    _ut.group('Test', () {",
        "      _ut.test('test_assertFalse', () {",
        "        final __test = new Test();",
        "        runJUnitTest(__test, __test.test_assertFalse);",
        "      });",
        "      _ut.test('test_assertTrue', () {",
        "        final __test = new Test();",
        "        runJUnitTest(__test, __test.test_assertTrue);",
        "      });",
        "    });",
        "  }",
        "}");
  }

  public void test_assertX() throws Exception {
    translateSingleFile(
        "// filler filler filler filler filler filler filler filler filler filler",
        "package test;",
        "import junit.framework.TestCase;",
        "public class Test extends TestCase {",
        "  public void test_x() {",
        "    Object v;",
        "    assertNull(v);",
        "    assertNotNull(v);",
        "    assertEquals(1, v);",
        "    assertEquals(\"msg\", 1, v);",
        "    assertSame(2, v);",
        "    assertNotSame(3, v);",
        "  }",
        "}");
    JUnitSemanticProcessor.INSTANCE.process(context, unit);
    String resultSource = getFormattedSource(unit);
    resultSource = StringUtils.substringBefore(resultSource, "static dartSuite() {");
    assertEquals(
        toString(
            "class Test extends JUnitTestCase {",
            "  void test_x() {",
            "    Object v;",
            "    JUnitTestCase.assertNull(v);",
            "    JUnitTestCase.assertNotNull(v);",
            "    JUnitTestCase.assertEquals(1, v);",
            "    JUnitTestCase.assertEqualsMsg(\"msg\", 1, v);",
            "    JUnitTestCase.assertSame(2, v);",
            "    JUnitTestCase.assertNotSame(3, v);",
            "  }",
            "  "),
        resultSource);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context.addClasspathFile(new File("../../../../third_party/junit/v4_8_2/junit.jar"));
  }
}
