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

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.resolver.Element;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.test.util.TestProject;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;

import junit.framework.TestCase;

import java.util.Collection;
import java.util.List;

public class NameOccurrencesFinderTest extends TestCase {

  public void test2() throws Exception {
    /*
    #import('../x/x.dart', prefix: 'x');
    class extra {
      f() {
        x.x u;
        var y = new x.x.x();
      }
    }
    */
    test(
        compose(
            "var y = 0;",
            "class S {",
            "  S();",
            "  S.s(x);",
            "}",
            "int tf!6(var xx) => xx * xx + 42 - !7tf(xx-1);",
            "class R extends S {",
            "  R.a() : this.!1b();",
            "  R.b() : super.s!2(1);",
            "  var q = new Map<St!5ring, Object!3>();",
            "  var w = new Map<String, !4Dynamic>();",
            "  var zz = t!8f(y);",
            "  var z2 = tf((xx!9) => 11);",
            "}"),
        // 1 redirect constructor
        "1+b();",
        "1+b() ",
        // 2 super constructor
        "2+s(x);",
        "2+s(1);",
        // 3 terminal generic type ref
        "3+Object>",
        // 4 explicit Dynamic ref
        "4+Dynamic>",
        // 5 multiple generic type ref
        "5+String, O",
        "5+String, D",
        // 6 top-level function def
        "6+tf(var",
        "6+tf(xx",
        "6+tf(y",
        // 7 top-level function ref in top-level func
        "7+tf(var",
        "7+tf(xx",
        "7+tf(y",
        // 8 top-level function def in class method
        "8+tf(var",
        "8+tf(xx",
        "8+tf(y",
        // not finding parameter
        "9-xx) =>"
    // end of tests
    );
  }

  public void xtest1() throws Exception {
    // TODO(brianwilkerson) Math has been removed. Enable with a different class or remove this test.
    test(
        compose(
            "#import('dart:html');",
            "main() {",
            "  new Sunfl!2ower();",
            "}",
            "class Sunflower!b {",
            "  Sunflower() {",
            "    PHI = (Ma!1th.sqrt(5) + 1) / 2;",
            "    CanvasElement canvas = document.query('#canvas');",
            "    xc = yc = !3MAX_D / 2;",
            "    ctx = canvas.getContext('2d');",
            "    InputElement slider!5 = document.query('#slider');",
            "    slider.o!9n.change.add((Event e) {",
            "      seeds = Math.parseInt(sli!6der.value);",
            "      drawFrame();",
            "    }, true);",
            "    seeds = Math.parseInt(slider.value);",
            "    drawFrame();",
            "  }",
            "  // Draw the complete figure for the current number of seeds.",
            "  void drawFrame() {",
            "    ctx.clearRect(0, 0, MAX_D, MAX_D);",
            "    for (var i=0; i<seeds; i++) {",
            "      var theta = i * TAU / PHI;",
            "      var r = Math.sqrt(i) * SCALE_FACTOR;",
            "      var x = xc + r * Math.cos(theta);",
            "      var y = yc - r * Math.sin(theta);",
            "      draw!7Seed(x,y);",
            "    }",
            "  }",
            "  // Draw a small circle representing a seed centered at (x,y).",
            "  void !8drawSeed(num x!0, num y) {",
            "    ctx.beginPath();",
            "    ctx.lineWidth = 2;",
            "    ctx.fillStyle = ORANGE;",
            "    ctx.strokeStyle = ORANGE;",
            "    ctx.arc(x, y, SEED_RADIUS, 0, TAU, false);",
            "    ctx.fill();",
            "    ctx.closePath();",
            "    ctx.stroke();",
            "  }",
            "  CanvasRenderingContext2D ctx;",
            "  num xc, yc;",
            "  num seeds = 0;",
            "  static final SEED_RADIUS = 2;",
            "  static final SCALE_FACTOR = 4;",
            "  static final TAU = Math.PI * 2;",
            "  var PHI!a;",
            "  static final MAX_D!4 = 300;",
            "  static final String ORANGE = 'orange';",
            "}"),
        // 1 class ref
        "1+Math.sqrt(5)",
        "1+Math.parseInt",
        "1+Math.PI",
        // 2 constructor ref from new expr
        "2+Sunflower() {",
        "2-Sunflower {",
        "2-new Sunflower();",
        // 3 static var ref
        "3+MAX_D / 2",
        "3+MAX_D,",
        "3+MAX_D)",
        "3+MAX_D = 300",
        // 4 static var decl
        "4+MAX_D / 2",
        "4+MAX_D,",
        "4+MAX_D)",
        "4+MAX_D = 300",
        // 5 local var decl
        "5+slider =",
        "5+slider.on",
        "5+slider.value",
        // 6 local var ref
        "6+slider =",
        "6+slider.on",
        "6+slider.value",
        // 7 unqualified method invocation
        "7+drawSeed(x,y);",
        "7+drawSeed(num x, num y)",
        // 8 method decl
        "8+drawSeed(num x, num y)",
        "8+drawSeed(x,y);",
        // 9 property access getter
        "9+on.change",
        // 0 parameter
        "0+x, num y) {",
        "0+x, y, SEED_RADIUS, 0",
        // a field
        "a+PHI = (Math",
        "a+PHI;",
        // b class definition reference
        "b+Sunflower {",
        "b-Sunflower() {",
        "b-new Sunflower();"
    // end of tests
    );
  }

  private String compose(String... lines) {
    return Joiner.on("\n").join(lines);
  }

  /**
   * Run a set of location tests on the given <code>originalSource</code>. The source string has
   * test locations embedded in it, which are identified by '!X' where X is a single character. Each
   * X is matched to positive or negative results in the array of <code>validationStrings</code>.
   * Validation strings contain the name of a prediction with a two character prefix. The first
   * character of the prefix corresponds to an X in the <code>originalSource</code>. The second
   * character is either a '+' or a '-' indicating whether the string is a positive or negative
   * result.
   * 
   * @param originalSource The source for a completion test that contains completion points
   * @param validationStrings The positive and negative predictions
   */
  private void test(String originalSource, String... results) throws Exception {
    Collection<LocationSpec> locationTests = LocationSpec.from(originalSource, results);
    assertTrue(
        "Expected exclamation point ('!') within the source denoting the test location",
        !locationTests.isEmpty());
    for (LocationSpec test : locationTests) {
      TestProject testProject = new TestProject("Test");
      try {
        CompilationUnit unit = testProject.setUnitContent("Test.dart", test.source);
        DartElementLocator locator = new DartElementLocator(
            unit,
            test.testLocation,
            test.testLocation,
            true);
        List<DartCompilationError> errors = Lists.newArrayList();
        DartUnit astRoot = DartCompilerUtilities.resolveUnit(unit, errors);
        locator.searchWithin(astRoot);
        Element selectedNode = locator.getResolvedElement();
        NameOccurrencesFinder finder = new NameOccurrencesFinder(selectedNode);
        finder.searchWithin(astRoot);
        List<DartNode> matches = finder.getMatches();
        verifyLocations(test, matches);
      } finally {
        testProject.dispose();
      }
    }
  }

  private void verifyLocations(LocationSpec test, List<DartNode> matches) {
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
