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

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.internal.correction.AbstractDartTest;
import com.google.dart.engine.services.internal.refactoring.ConvertMethodToGetterRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.ExtractLocalRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.ExtractMethodRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.InlineLocalRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.InlineMethodRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.RenameClassMemberRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.RenameConstructorRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.RenameLocalRefactoringImpl;
import com.google.dart.engine.services.internal.refactoring.RenameUnitMemberRefactoringImpl;

import static com.google.dart.engine.services.refactoring.RefactoringFactory.createConvertMethodToGetterRefactoring;
import static com.google.dart.engine.services.refactoring.RefactoringFactory.createExtractLocalRefactoring;
import static com.google.dart.engine.services.refactoring.RefactoringFactory.createExtractMethodRefactoring;
import static com.google.dart.engine.services.refactoring.RefactoringFactory.createInlineLocalRefactoring;
import static com.google.dart.engine.services.refactoring.RefactoringFactory.createInlineMethodRefactoring;
import static com.google.dart.engine.services.refactoring.RefactoringFactory.createRenameRefactoring;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RefactoringFactoryTest extends AbstractDartTest {
  private final SearchEngine searchEngine = mock(SearchEngine.class);
  private final CompilationUnitElement enclosingUnit = mock(CompilationUnitElement.class);
  private final ClassElement enclosingClass = mock(ClassElement.class);
  private final ExecutableElement enclosingMethod = mock(ExecutableElement.class);

  public void test_createConvertMethodToGetterRefactoring() throws Exception {
    MethodElement element = mock(MethodElement.class);
    ConvertMethodToGetterRefactoring refactoring = createConvertMethodToGetterRefactoring(
        searchEngine,
        element);
    assertThat(refactoring).isInstanceOf(ConvertMethodToGetterRefactoringImpl.class);
  }

  public void test_createExtractLocalRefactoring() throws Exception {
    parseTestUnit("");
    AssistContext context = new AssistContext(searchEngine, testUnit, 0, 0);
    ExtractLocalRefactoring refactoring = createExtractLocalRefactoring(context);
    assertThat(refactoring).isInstanceOf(ExtractLocalRefactoringImpl.class);
  }

  public void test_createExtractMethodRefactoring() throws Exception {
    parseTestUnit("");
    AssistContext context = new AssistContext(searchEngine, testUnit, 0, 0);
    ExtractMethodRefactoring refactoring = createExtractMethodRefactoring(context);
    assertThat(refactoring).isInstanceOf(ExtractMethodRefactoringImpl.class);
  }

  public void test_createInlineLocalRefactoring() throws Exception {
    parseTestUnit("");
    AssistContext context = new AssistContext(searchEngine, testUnit, 0, 0);
    InlineLocalRefactoring refactoring = createInlineLocalRefactoring(context);
    assertThat(refactoring).isInstanceOf(InlineLocalRefactoringImpl.class);
  }

  public void test_createInlineMethodRefactoring() throws Exception {
    parseTestUnit("");
    AssistContext context = new AssistContext(searchEngine, testUnit, 0, 0);
    InlineMethodRefactoring refactoring = createInlineMethodRefactoring(context);
    assertThat(refactoring).isInstanceOf(InlineMethodRefactoringImpl.class);
  }

  public void test_createRenameRefactoring_classMember_FieldElement() throws Exception {
    FieldElement element = mock(FieldElement.class);
    when(element.getEnclosingElement()).thenReturn(enclosingClass);
    // create refactoring
    Refactoring refactoring = createRenameRefactoring(searchEngine, element);
    assertThat(refactoring).isInstanceOf(RenameClassMemberRefactoringImpl.class);
  }

  public void test_createRenameRefactoring_classMember_MethodElement() throws Exception {
    MethodElement element = mock(MethodElement.class);
    when(element.getEnclosingElement()).thenReturn(enclosingClass);
    // create refactoring
    Refactoring refactoring = createRenameRefactoring(searchEngine, element);
    assertThat(refactoring).isInstanceOf(RenameClassMemberRefactoringImpl.class);
  }

  public void test_createRenameRefactoring_ConstructorElement() throws Exception {
    ConstructorElement element = mock(ConstructorElement.class);
    when(element.getEnclosingElement()).thenReturn(enclosingClass);
    // create refactoring
    Refactoring refactoring = createRenameRefactoring(searchEngine, element);
    assertThat(refactoring).isInstanceOf(RenameConstructorRefactoringImpl.class);
  }

  public void test_createRenameRefactoring_local_FunctionElement() throws Exception {
    FunctionElement element = mock(FunctionElement.class);
    when(element.getEnclosingElement()).thenReturn(enclosingMethod);
    // create refactoring
    Refactoring refactoring = createRenameRefactoring(searchEngine, element);
    assertThat(refactoring).isInstanceOf(RenameLocalRefactoringImpl.class);
  }

  public void test_createRenameRefactoring_local_LocalVariableElement() throws Exception {
    LocalVariableElement element = mock(LocalVariableElement.class);
    when(element.getEnclosingElement()).thenReturn(enclosingMethod);
    // create refactoring
    Refactoring refactoring = createRenameRefactoring(searchEngine, element);
    assertThat(refactoring).isInstanceOf(RenameLocalRefactoringImpl.class);
  }

  public void test_createRenameRefactoring_local_ParameterElement() throws Exception {
    ParameterElement element = mock(ParameterElement.class);
    when(element.getEnclosingElement()).thenReturn(enclosingMethod);
    // create refactoring
    Refactoring refactoring = createRenameRefactoring(searchEngine, element);
    assertThat(refactoring).isInstanceOf(RenameLocalRefactoringImpl.class);
  }

  public void test_createRenameRefactoring_unitMember_ClassElement() throws Exception {
    ClassElement element = mock(ClassElement.class);
    when(element.getEnclosingElement()).thenReturn(enclosingUnit);
    // create refactoring
    Refactoring refactoring = createRenameRefactoring(searchEngine, element);
    assertThat(refactoring).isInstanceOf(RenameUnitMemberRefactoringImpl.class);
  }

  public void test_createRenameRefactoring_unitMember_FunctionElement() throws Exception {
    FunctionElement element = mock(FunctionElement.class);
    when(element.getEnclosingElement()).thenReturn(enclosingUnit);
    // create refactoring
    Refactoring refactoring = createRenameRefactoring(searchEngine, element);
    assertThat(refactoring).isInstanceOf(RenameUnitMemberRefactoringImpl.class);
  }

  public void test_createRenameRefactoring_unknownElement() throws Exception {
    Element element = mock(Element.class);
    Refactoring refactoring = createRenameRefactoring(searchEngine, element);
    assertNull(refactoring);
  }
}
