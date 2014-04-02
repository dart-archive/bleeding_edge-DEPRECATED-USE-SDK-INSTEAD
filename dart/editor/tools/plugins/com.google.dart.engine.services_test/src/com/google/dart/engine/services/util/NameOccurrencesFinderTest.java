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
package com.google.dart.engine.services.util;

import com.google.common.base.Joiner;
import com.google.dart.engine.ast.AstFactory;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.resolver.ResolverTestCase;
import com.google.dart.engine.source.Source;

import static org.fest.assertions.Assertions.assertThat;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NameOccurrencesFinderTest extends ResolverTestCase {
  public void test_fieldFormal_asMember() throws Exception {
    test(//
        compose(//
            "class A<T> {",
            "  T !1element;//",
            "  A({this.element});",
            "}",
            "main() {",
            "  new A<int>(!2element: 0);",
            "}"),
        "1+element;//",
        "1+element}",
        "1+element: 0",
        "2+element;//",
        "2+element}",
        "2+element: 0");
  }

  public void test_findIn_nullElement() throws Exception {
    SimpleIdentifier ident = AstFactory.identifier("test");
    Collection<AstNode> matches = NameOccurrencesFinder.findIn(ident, null);
    assertThat(matches).isEmpty();
  }

  public void test_findIn_nullIdentifier() throws Exception {
    Collection<AstNode> matches = NameOccurrencesFinder.findIn(null, null);
    assertThat(matches).isEmpty();
  }

  public void test1() throws Exception {
    test(
        compose(
            "import 'dart:html';",
            "import 'dart:math' as Math;",
            "main() {",
            "  new Sunfl!2ower();",
            "}",
            "class InputElement2 extends InputElement {",
            "  Stream<Event> get onChange2 => null;",
            "}",
            "class Sunflower!b {",
            "  Sunflower() {",
            "    PHI = (Ma!1th.sqrt(5) + 1) / 2;",
            "    CanvasElement canvas = document.query('#canvas');",
            "    xc = yc = !3MAX_D / 2;",
            "    ctx = canvas.getContext('2d');",
            "    InputElement2 slider!5 = document.query('#slider');",
            "    slider.onCh!9ange2.listen((Event e) {",
            "      seeds = int.parse(sli!6der.value);",
            "      drawFrame();",
            "    }, true);",
            "    seeds = int.parse(slider.value);",
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
        "1+Math.PI",
        // 2 constructor ref from new expr
        "2+Sunflower() {",
        "2+Sunflower {",
        "2-new Sunflower();",
        // 3 static var ref
        "3+MAX_D / 2",
        "3+MAX_D,",
        "3+MAX_D)",
        "3+MAX_D = 300",
        "3-TAU",
        // 4 static var decl
        "4+MAX_D / 2",
        "4+MAX_D,",
        "4+MAX_D)",
        "4+MAX_D = 300",
        "4-TAU",
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
        "9+onChange2",
        // 0 parameter
        "0+x, num y) {",
        "0+x, y, SEED_RADIUS, 0",
        // a field
        "a+PHI = (Math",
        "a+PHI;",
        // b class definition reference
        "b+Sunflower {",
        "b+Sunflower() {",
        "b-new Sunflower();"
    // end of tests
    );
  }

  public void test2() throws Exception {
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
            "  var w = new Map<String, !4dynamic>();",
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
        // 4 explicit dynamic ref
        "4+dynamic>",
        // 5 multiple generic type ref
        "5+String, O",
        "5+String, d",
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

  public void test3() throws Exception {
    // test refs of synthetic field
    test(
        compose(//
            "class A {",
            "  get !1x => null;",
            "  set !2x(i) => null;",
            "  var q = !3x;",
            "  t() {",
            "    !4x = q;",
            "  }",
            "}"),
        "1+x =>",
        "1+x(i)",
        "1+x;",
        "1+x = q",
        "2+x(i)",
        "2+x;",
        "2+x = q",
        "3+x(i)",
        "3+x;",
        "3+x = q",
        "4+x(i)",
        "4+x;",
        "4+x = q"
    // end
    );
  }

  public void test4() throws Exception {
    // test refs of synthetic getter & setter
    test(//
        compose(//
            "class B {",
            "  var !1x;",
            "  var a = !2x;//",
            "  t() {",
            "    !3x = a;",
            "  }",
            "}"),
        "1+x;",
        "1+x;//",
        "1+x =",
        "2+x;",
        "2+x;//",
        "2+x =",
        "3+x;",
        "3+x;//",
        "3+x ="
    //end
    );
  }

  public void test5() throws Exception {
    // test refs of synthetic member
    test(//
        compose(//
            "class A<T> {",
            "  !1mmm(T !7t) {!8t.toString();}",
            "  nnn() {}",
            "  T !4v1;",
            "}",
            "main() {",
            "  A<int> a1;",
            "  A<String> a2;",
            "  a1.!2mmm(0);",
            "  a1.mmm(1);",
            "  a1.nnn();",
            "  a2.mmm('0');",
            "  a2.!3mmm('1');",
            "  a2.nnn();",
            "  a1.!5v1;//",
            "  a2.!6v1;///",
            "}"),
        "1+mmm(T",
        "1+mmm(0)",
        "1+mmm('1')",
        "2+mmm(T",
        "2+mmm(0)",
        "2+mmm('1')",
        "3+mmm(T",
        "3+mmm(0)",
        "3+mmm('1')",
        "4+v1;",
        "4+v1;//",
        "4+v1;///",
        "5+v1;",
        "5+v1;//",
        "5+v1;///",
        "6+v1;",
        "6+v1;//",
        "6+v1;///",
        "7+t)",
        "7+t.t",
        "8+t)",
        "8+t.t"
    //end
    );
  }

  public void test6() throws Exception {
    // test refs of synthetic member
    test(//
        compose(//
            "class X {",
            "  var !1element;//",
            "  X({this.!2element});",
            "}"),
        "1+element;//",
        "1+element}",
        "1+element;//",
        "2+element}"
    //end
    );
  }

  protected CompilationUnit resolve(List<Source> sources) throws AnalysisException {
    AnalysisContext context = getAnalysisContext();
    CompilationUnit libraryUnit = null;
    for (Source source : sources) {
      LibraryElement library = resolve(source);
      libraryUnit = context.resolveCompilationUnit(source, library);
      for (CompilationUnitElement partElement : library.getParts()) {
        context.resolveCompilationUnit(partElement.getSource(), library);
      }
    }
    return libraryUnit;
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
  protected void test(String originalSource, List<Source> sources, String... results)
      throws URISyntaxException, AnalysisException {
    Collection<LocationSpec> completionTests = LocationSpec.from(originalSource, results);
    assertTrue(
        "Expected exclamation point ('!') within the source denoting the test location",
        !completionTests.isEmpty());
    sources.add(addSource(completionTests.iterator().next().source));
    CompilationUnit compilationUnit = resolve(sources);
    for (LocationSpec test : completionTests) {
      NodeLocator locator = new NodeLocator(test.testLocation);
      AstNode selectedNode = locator.searchWithin(compilationUnit);
      Collection<AstNode> matches = null;
      if (selectedNode instanceof SimpleIdentifier) {
        SimpleIdentifier ident = (SimpleIdentifier) selectedNode;
        matches = NameOccurrencesFinder.findIn(ident, compilationUnit);
        verifyLocations(test, matches);
      } else {
        fail();
      }
    }
  }

  protected void test(String originalSource, String... results) throws URISyntaxException,
      AnalysisException {
    test(originalSource, new ArrayList<Source>(), results);
  }

  private String compose(String... lines) {
    return Joiner.on("\n").join(lines);
  }

  private void verifyLocations(LocationSpec test, Collection<AstNode> matches) {
    for (String result : test.positiveResults) {
      // verify that result is present in matches
      int start = test.source.indexOf(result);
      boolean found = false;
      for (AstNode match : matches) {
        if (start == match.getOffset()) {
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
      for (AstNode match : matches) {
        if (start == match.getOffset()) {
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
