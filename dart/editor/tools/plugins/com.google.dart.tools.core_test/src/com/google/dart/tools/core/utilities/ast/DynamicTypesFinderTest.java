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
package com.google.dart.tools.core.utilities.ast;

import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.test.util.TestProject;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;

/**
 * Test for {@link DynamicTypesFinder}.
 */
public class DynamicTypesFinderTest extends TestCase {

  /**
   * Assert that the given source has the expected matches (indicating refinable types).
   * 
   * @param src the source for the test
   * @param expected expected match results
   */
  private void assertMatches(String src, String... expected) throws Exception {
    Collection<LocationSpec> locationTests = LocationSpec.from(src, expected);
    assertTrue(
        "Expected exclamation point ('!') within the source denoting the test location",
        !locationTests.isEmpty());

    TestProject testProject = new TestProject("Test");
    CompilationUnit unit = testProject.setUnitContent("Test.dart", removeLocationData(src));
    List<DartCompilationError> errors = Lists.newArrayList();
    DartUnit astRoot = DartCompilerUtilities.resolveUnit(unit, errors);
    if (!errors.isEmpty()) {
      fail("Parse/resolve errors: " + errors);
    }
    DynamicTypesFinder finder = new DynamicTypesFinder();
    finder.searchWithin(astRoot);
    Iterable<DartIdentifier> matches = finder.getMatches();

    for (LocationSpec test : locationTests) {
      try {
        verifyLocations(test, matches);
      } finally {
        testProject.dispose();
      }
    }

  }

  private String join(String... lines) {
    return Joiner.on("\n").join(lines);
  }

  private String removeLocationData(String src) {
    //source locations are flagged using !n (e.g., var !1x = 3) and this strips out the !n
    //in order to make the source parseable
    return src.replaceAll("![\\d]", "");
  }

  public void testGetterSetter() throws Exception {
    assertMatches(
        join(
            "class Foo {",
            "    String _foo;",
            "    String get !1fooBar => _foo == null ? 'none' : _foo;",
            "           set !2fooBar(String foo) => _foo = foo;",
            "}"),
        "1-fooBar",
        "2-fooBar");
  }

  public void testLocals() throws Exception {
    assertMatches(
        join(
            "class Foo {",
            "  main() {",
            "    new Foo().foo();",
            "  }",
            "  void foo(){",
            "    var !1x;",
            //"    !2x = 3;",
            "    int !3y = 3;",
            "    var !4z = 'foo';",
            "  }",
            "}"),
        "1+x",
        //"2+x",
        "3-y",
        "4-z");
  }

  private void verifyLocations(LocationSpec test, Iterable<DartIdentifier> matches) {
    for (String result : test.positiveResults) {
      // verify that result is present in matches
      int start = test.source.indexOf(result);
      boolean found = false;
      for (DartNode match : matches) {
        if (start == match.getSourceInfo().getOffset()) {
          found = true;
          break;
        }
      }
      if (!found) {
        fail("No match found for: " + result);
      }
    }
    for (String result : test.negativeResults) {
      // verify that result is not present in matches
      int start = test.source.indexOf(result);
      if (start < 0) {
        fail("Bad test result: " + result);
      }
      boolean found = false;
      for (DartNode match : matches) {
        if (start == match.getSourceInfo().getOffset()) {
          found = true;
          break;
        }
      }
      if (found) {
        fail("Invalid match found for: " + result);
      }
    }
  }
}
