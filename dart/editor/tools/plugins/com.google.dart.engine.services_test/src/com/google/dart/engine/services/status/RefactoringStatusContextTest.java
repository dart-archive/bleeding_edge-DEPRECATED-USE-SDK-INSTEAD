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

package com.google.dart.engine.services.status;

import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.services.internal.correction.AbstractDartTest;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.engine.utilities.source.SourceRangeFactory;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RefactoringStatusContextTest extends AbstractDartTest {

  public void test_new_ASTNode() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {}",
        "");
    SimpleIdentifier node = findNode("main() {}", SimpleIdentifier.class);
    RefactoringStatusContext context = RefactoringStatusContext.create(node);
    // access
    assertSame(testUnit.getElement().getContext(), context.getContext());
    assertSame(testUnit.getElement().getSource(), context.getSource());
    assertEquals(SourceRangeFactory.rangeNode(node), context.getRange());
  }

  public void test_new_CompilationUnit_SourceRange() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {}",
        "");
    SourceRange range = new SourceRange(10, 20);
    RefactoringStatusContext context = RefactoringStatusContext.create(testUnit, range);
    // access
    assertSame(testUnit.getElement().getContext(), context.getContext());
    assertSame(testUnit.getElement().getSource(), context.getSource());
    assertEquals(range, context.getRange());
    // toString()
    {
      String str = context.toString();
      assertThat(str).contains("range=" + range);
      assertThat(str).startsWith("RefactoringStatusContext{source=");
    }
  }

  public void test_new_Element() throws Exception {
    AnalysisContext analysisContext = mock(AnalysisContext.class);
    Source source = mock(Source.class);
    Element element = mock(Element.class);
    when(element.getContext()).thenReturn(analysisContext);
    when(element.getSource()).thenReturn(source);
    when(element.getNameOffset()).thenReturn(12);
    when(element.getName()).thenReturn("test");
    RefactoringStatusContext context = RefactoringStatusContext.create(element);
    // access
    assertSame(analysisContext, context.getContext());
    assertSame(source, context.getSource());
    assertEquals(new SourceRange(12, 4), context.getRange());
  }

  public void test_new_Element_SourceRange() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {}",
        "");
    AnalysisContext analysisContext = mock(AnalysisContext.class);
    Source source = mock(Source.class);
    Element element = mock(Element.class);
    when(element.getContext()).thenReturn(analysisContext);
    when(element.getSource()).thenReturn(source);
    SourceRange range = new SourceRange(10, 20);
    RefactoringStatusContext context = RefactoringStatusContext.create(element, range);
    // access
    assertSame(analysisContext, context.getContext());
    assertSame(source, context.getSource());
    assertEquals(range, context.getRange());
  }

  public void test_new_SearchMatch() throws Exception {
    AnalysisContext analysisContext = mock(AnalysisContext.class);
    Source source = mock(Source.class);
    Element element = mock(Element.class);
    when(element.getContext()).thenReturn(analysisContext);
    when(element.getSource()).thenReturn(source);
    SearchMatch match = mock(SearchMatch.class);
    when(match.getElement()).thenReturn(element);
    when(match.getSourceRange()).thenReturn(new SourceRange(12, 4));
    RefactoringStatusContext context = RefactoringStatusContext.create(match);
    // access
    assertSame(analysisContext, context.getContext());
    assertSame(source, context.getSource());
    assertEquals(new SourceRange(12, 4), context.getRange());
  }
}
