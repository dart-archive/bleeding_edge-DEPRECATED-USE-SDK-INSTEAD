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

package com.google.dart.engine.services.refactoring;

import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.services.internal.correction.AbstractDartTest;
import com.google.dart.engine.services.internal.refactoring.RenameLocalVariableRefactoringImpl;

import static com.google.dart.engine.services.refactoring.RefactoringFactory.*;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RefactoringFactoryTest extends AbstractDartTest {
  public void test_createRenameRefactoring_unknownElement() throws Exception {
    SearchEngine searchEngine = mock(SearchEngine.class);
    Element element = mock(Element.class);
    Refactoring refactoring = createRenameRefactoring(searchEngine, element);
    assertNull(refactoring);
  }

  public void test_createRenameRefactoring_VariableElement() throws Exception {
    SearchEngine searchEngine = mock(SearchEngine.class);
    CompilationUnitElement unitElement = mock(CompilationUnitElement.class);
    VariableElement element = mock(VariableElement.class);
    when(element.getAncestor(CompilationUnitElement.class)).thenReturn(unitElement);
    Refactoring refactoring = createRenameRefactoring(searchEngine, element);
    assertThat(refactoring).isInstanceOf(RenameLocalVariableRefactoringImpl.class);
  }
}
