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

package com.google.dart.engine.services.assist;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.services.internal.correction.AbstractDartTest;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.SourceRange;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AssistContextTest extends AbstractDartTest {
  private final SearchEngine searchEngine = mock(SearchEngine.class);

  public void test_access() throws Exception {
    Source source = mock(Source.class);
    CompilationUnit compilationUnit = mock(CompilationUnit.class);
    CompilationUnitElement compilationUnitElement = mock(CompilationUnitElement.class);
    when(compilationUnit.getElement()).thenReturn(compilationUnitElement);
    when(compilationUnitElement.getSource()).thenReturn(source);
    int selectionOffset = 10;
    int selectionLength = 2;
    AssistContext context = new AssistContext(
        searchEngine,
        compilationUnit,
        selectionOffset,
        selectionLength);
    assertSame(searchEngine, context.getSearchEngine());
    assertSame(source, context.getSource());
    assertSame(compilationUnit, context.getCompilationUnit());
    assertEquals(10, context.getSelectionOffset());
    assertEquals(2, context.getSelectionLength());
    assertEquals(new SourceRange(10, 2), context.getSelectionRange());
  }

  public void test_access_noElement() throws Exception {
    CompilationUnit compilationUnit = mock(CompilationUnit.class);
    AssistContext context = new AssistContext(searchEngine, compilationUnit, 0, 0);
    assertSame(null, context.getSource());
  }

  public void test_getCoveredElement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  String text = '';",
        "}",
        "");
    //
    int selectionOffset = testCode.indexOf("tring ");
    int selectionLength = testCode.indexOf("ng ") - selectionOffset;
    AssistContext context = new AssistContext(
        searchEngine,
        testUnit,
        selectionOffset,
        selectionLength);
    ClassElement coveredElement = (ClassElement) context.getCoveredElement();
    assertNotNull(coveredElement);
    assertEquals("String", coveredElement.getName());
  }

  public void test_getCoveredElement_null() throws Exception {
    AssistContext context = new AssistContext(searchEngine, null, 0, 0);
    assertNull(context.getCoveredElement());
  }

  public void test_getNode() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  String text = '';",
        "}",
        "");
    // covering == covered
    {
      int selectionOffset = testCode.indexOf("tring ");
      int selectionEnd = testCode.indexOf("ng ");
      int selectionLength = selectionEnd - selectionOffset;
      AssistContext context = new AssistContext(
          searchEngine,
          testUnit,
          selectionOffset,
          selectionLength);
      assertSame(testUnit, context.getCompilationUnit());
      assertEquals("String", context.getCoveredNode().toSource());
      assertEquals("String", context.getCoveringNode().toSource());
      assertEquals(selectionOffset, context.getSelectionOffset());
      assertEquals(selectionLength, context.getSelectionLength());
    }
    // covering > covered
    {
      int selectionOffset = testCode.indexOf("tring ");
      int selectionEnd = testCode.indexOf(" = ''");
      int selectionLength = selectionEnd - selectionOffset;
      AssistContext context = new AssistContext(
          searchEngine,
          testUnit,
          selectionOffset,
          selectionLength);
      assertSame(testUnit, context.getCompilationUnit());
      assertEquals("String", context.getCoveredNode().toSource());
      assertEquals("String text = ''", context.getCoveringNode().toSource());
      assertEquals(selectionOffset, context.getSelectionOffset());
      assertEquals(selectionLength, context.getSelectionLength());
    }
  }

  public void test_new_SourceRange() throws Exception {
    CompilationUnit compilationUnit = mock(CompilationUnit.class);
    AssistContext context = new AssistContext(searchEngine, compilationUnit, new SourceRange(10, 2));
    assertSame(compilationUnit, context.getCompilationUnit());
    assertEquals(10, context.getSelectionOffset());
    assertEquals(2, context.getSelectionLength());
    assertEquals(new SourceRange(10, 2), context.getSelectionRange());
  }
}
