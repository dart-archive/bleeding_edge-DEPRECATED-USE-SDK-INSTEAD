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
package com.google.dart.engine.internal.resolver;

import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.resolver.ResolverTestCase;
import com.google.dart.engine.source.Source;

public class DeclarationMatcherTest extends ResolverTestCase {
  public void test_compilationUnitMatches_false_topLevelVariable() throws Exception {
    assertCompilationUnitMatches(false, createSource(//
        "class C {",
        "  int m(int p) {",
        "    return p + p;",
        "  }",
        "}"), createSource(//
        "const int ZERO = 0;",
        "class C {",
        "  int m(int p) {",
        "    return (p * p) + (p * p) + ZERO;",
        "  }",
        "}"));
  }

  public void test_compilationUnitMatches_true_different() throws Exception {
    assertCompilationUnitMatches(true, createSource(//
        "class C {",
        "  int m(int p) {",
        "    return p + p;",
        "  }",
        "}"), createSource(//
        "class C {",
        "  int m(int p) {",
        "    return (p * p) + (p * p);",
        "  }",
        "}"));
  }

  public void test_compilationUnitMatches_true_same() throws Exception {
    String content = createSource(//
        "class C {",
        "  int m(int p) {",
        "    return p + p;",
        "  }",
        "}");
    assertCompilationUnitMatches(true, content, content);
  }

  public void test_methodDeclarationMatches_false_localVariable() throws Exception {
    assertMethodMatches(false, createSource(//
        "class C {",
        "  int m(int p) {",
        "    return p + p;",
        "  }",
        "}"), createSource(//
        "class C {",
        "  int m(int p) {",
        "    int product = p * p;",
        "    return product + product;",
        "  }",
        "}"));
  }

  public void test_methodDeclarationMatches_false_parameter() throws Exception {
    assertMethodMatches(false, createSource(//
        "class C {",
        "  int m(int p) {",
        "    return p + p;",
        "  }",
        "}"), createSource(//
        "class C {",
        "  int m(int p, int q) {",
        "    return (p * q) + (q * p);",
        "  }",
        "}"));
  }

  public void test_methodDeclarationMatches_true_different() throws Exception {
    assertMethodMatches(true, createSource(//
        "class C {",
        "  int m(int p) {",
        "    return p + p;",
        "  }",
        "}"), createSource(//
        "class C {",
        "  int m(int p) {",
        "    return (p * p) + (p * p);",
        "  }",
        "}"));
  }

  public void test_methodDeclarationMatches_true_same() throws Exception {
    String content = createSource(//
        "class C {",
        "  int m(int p) {",
        "    return p + p;",
        "  }",
        "}");
    assertMethodMatches(true, content, content);
  }

  private void assertCompilationUnitMatches(boolean expectMatch, String oldContent,
      String newContent) throws Exception {
    Source source = addSource(oldContent);
    LibraryElement library = resolve(source);
    CompilationUnit oldUnit = resolveCompilationUnit(source, library);

    AnalysisContext context = getAnalysisContext();
    context.setContents(source, newContent);
    CompilationUnit newUnit = context.parseCompilationUnit(source);

    DeclarationMatcher matcher = new DeclarationMatcher();
    assertEquals(expectMatch, matcher.matches(newUnit, oldUnit.getElement()));
  }

  private void assertMethodMatches(boolean expectMatch, String oldContent, String newContent)
      throws Exception {
    Source source = addSource(oldContent);
    LibraryElement library = resolve(source);
    CompilationUnit oldUnit = resolveCompilationUnit(source, library);
    MethodElement element = (MethodElement) getFirstMethod(oldUnit).getElement();

    AnalysisContext context = getAnalysisContext();
    context.setContents(source, newContent);
    CompilationUnit newUnit = context.parseCompilationUnit(source);
    MethodDeclaration newMethod = getFirstMethod(newUnit);

    DeclarationMatcher matcher = new DeclarationMatcher();
    assertEquals(expectMatch, matcher.matches(newMethod, element));
  }

  private MethodDeclaration getFirstMethod(CompilationUnit unit) {
    ClassDeclaration classNode = (ClassDeclaration) unit.getDeclarations().get(0);
    return (MethodDeclaration) classNode.getMembers().get(0);
  }
}
