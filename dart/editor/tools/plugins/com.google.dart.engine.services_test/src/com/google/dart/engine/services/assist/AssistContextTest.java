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

import com.google.common.base.Joiner;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.parser.ParserTestCase;
import com.google.dart.engine.source.Source;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AssistContextTest extends TestCase {
  private static String toString(String... lines) {
    return Joiner.on("\n").join(lines);
  }

  public void test_access() throws Exception {
    Source source = mock(Source.class);
    CompilationUnit compilationUnit = mock(CompilationUnit.class);
    CompilationUnitElement compilationUnitElement = mock(CompilationUnitElement.class);
    when(compilationUnit.getElement()).thenReturn(compilationUnitElement);
    when(compilationUnitElement.getSource()).thenReturn(source);
    int selectionOffset = 10;
    int selectionLength = 2;
    AssistContext context = new AssistContext(compilationUnit, selectionOffset, selectionLength);
    assertSame(source, context.getSource());
    assertSame(compilationUnit, context.getCompilationUnit());
    assertEquals(10, context.getSelectionOffset());
    assertEquals(2, context.getSelectionLength());
  }

  public void test_access_noElement() throws Exception {
    CompilationUnit compilationUnit = mock(CompilationUnit.class);
    AssistContext context = new AssistContext(compilationUnit, 0, 0);
    assertSame(null, context.getSource());
  }

  public void test_getNode() throws Exception {
    String sourceContent = toString(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  String text = '';",
        "}",
        "");
    CompilationUnit compilationUnit = ParserTestCase.parseCompilationUnit(sourceContent);
    // covering == covered
    {
      int selectionOffset = sourceContent.indexOf("tring ");
      int selectionEnd = sourceContent.indexOf("ng ");
      int selectionLength = selectionEnd - selectionOffset;
      AssistContext context = new AssistContext(compilationUnit, selectionOffset, selectionLength);
      assertSame(compilationUnit, context.getCompilationUnit());
      assertEquals("String", context.getCoveredNode().toSource());
      assertEquals("String", context.getCoveringNode().toSource());
      assertEquals(selectionOffset, context.getSelectionOffset());
      assertEquals(selectionLength, context.getSelectionLength());
    }
    // covering > covered
    {
      int selectionOffset = sourceContent.indexOf("tring ");
      int selectionEnd = sourceContent.indexOf(" = ''");
      int selectionLength = selectionEnd - selectionOffset;
      AssistContext context = new AssistContext(compilationUnit, selectionOffset, selectionLength);
      assertSame(compilationUnit, context.getCompilationUnit());
      assertEquals("String", context.getCoveredNode().toSource());
      assertEquals("String text = ''", context.getCoveringNode().toSource());
      assertEquals(selectionOffset, context.getSelectionOffset());
      assertEquals(selectionLength, context.getSelectionLength());
    }
  }
}
