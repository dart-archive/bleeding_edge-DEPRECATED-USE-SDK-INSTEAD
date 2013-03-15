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

package com.google.dart.engine.services.internal.correction;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.utilities.source.SourceRange;

import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartEnd;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class SelectionAnalyzerTest extends AbstractDartTest {

  /**
   * We need this NoOp subclass of {@link SelectionAnalyzer} to make its protected methods available
   * during running tests in OSGi.
   */
  public static class SelectionAnalyzer extends
      com.google.dart.engine.services.internal.correction.SelectionAnalyzer {
    public SelectionAnalyzer(SourceRange selection) {
      super(selection);
    }

    @Override
    protected void handleSelectionEndsIn(ASTNode node) {
      super.handleSelectionEndsIn(node);
    }

    @Override
    protected void handleSelectionStartsIn(ASTNode node) {
      super.handleSelectionStartsIn(node);
    }

    @Override
    protected void reset() {
      super.reset();
    }
  }

  public void test_handleSelectionEndsIn() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() { // marker",
        "}",
        "");
    FunctionDeclaration main = findNode("main", FunctionDeclaration.class);
    // analyze selection
    SourceRange selection = rangeStartEnd(0, findOffset("// marker"));
    SelectionAnalyzer analyzer = new SelectionAnalyzer(selection);
    SelectionAnalyzer analyzerSpy = spy(analyzer);
    testUnit.accept(analyzerSpy);
    verify(analyzerSpy).handleSelectionEndsIn(main);
  }

  public void test_handleSelectionStartsIn() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var a;",
        "  var b;",
        "}",
        "");
    Statement statementA = findNode("var a", Statement.class);
    Statement statementB = findNode("var b", Statement.class);
    // analyze selection
    SourceRange selection = rangeStartEnd(findOffset("ar a"), statementB);
    SelectionAnalyzer analyzer = new SelectionAnalyzer(selection);
    SelectionAnalyzer analyzerSpy = spy(analyzer);
    testUnit.accept(analyzerSpy);
    verify(analyzerSpy).handleSelectionStartsIn(statementA);
  }

  public void test_hasSelectedNode() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() { // marker",
        "  var a;",
        "  var b;",
        "  var c;",
        "}",
        "");
    Block block = findNode("{ // marker", Block.class);
    Statement statementA = findNode("var a", Statement.class);
    Statement statementB = findNode("var b", Statement.class);
    // analyze selection
    SourceRange selection = rangeStartEnd(findOffset("  var a"), findOffset("  var c"));
    SelectionAnalyzer analyzer = new SelectionAnalyzer(selection);
    testUnit.accept(analyzer);
    assertSame(block, analyzer.getCoveringNode());
    assertTrue(analyzer.hasSelectedNodes());
    assertSame(statementA, analyzer.getFirstSelectedNode());
    assertSame(statementB, analyzer.getLastSelectedNode());
    assertEquals(rangeStartEnd(statementA, statementB), analyzer.getSelectedNodeRange());
    assertThat(analyzer.getSelectedNodes()).containsExactly(statementA, statementB);
    // reset
    analyzer.reset();
    assertFalse(analyzer.hasSelectedNodes());
  }

  public void test_noSelectedNode() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {}",
        "");
    // analyze selection
    SourceRange selection = rangeStartEnd(0, 0);
    SelectionAnalyzer analyzer = new SelectionAnalyzer(selection);
    // no selected nodes
    assertFalse(analyzer.hasSelectedNodes());
    assertSame(null, analyzer.getFirstSelectedNode());
    assertSame(null, analyzer.getLastSelectedNode());
    assertThat(analyzer.getSelectedNodes()).isEmpty();
    assertEquals(null, analyzer.getSelectedNodeRange());
  }
}
