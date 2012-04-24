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
package com.google.dart.tools.core.search;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.dart.compiler.DartCompilationError;
import com.google.dart.tools.core.internal.index.impl.InMemoryIndex;
import com.google.dart.tools.core.internal.index.util.ResourceFactory;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.model.SourceRangeImpl;
import com.google.dart.tools.core.internal.search.NewSearchEngineImpl;
import com.google.dart.tools.core.internal.search.scope.LibrarySearchScope;
import com.google.dart.tools.core.internal.search.scope.WorkspaceSearchScope;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.test.util.TestProject;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;

import static com.google.dart.tools.core.test.util.MoneyProjectUtilities.getMoneyProject;

import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;

import static org.fest.assertions.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class NewSearchEngineTest extends TestCase {
  private DartLibraryImpl moneyLibrary;

  private InMemoryIndex index;

  public void indexUnit(CompilationUnit unit) throws DartModelException {
    index.indexResource(
        ResourceFactory.getResource(unit),
        unit,
        DartCompilerUtilities.resolveUnit(unit, new ArrayList<DartCompilationError>()));
  }

  @Override
  public void setUp() {
    try {
      DartProject moneyProject = getMoneyProject();
      DartLibrary[] libraries = moneyProject.getDartLibraries();
      assertNotNull(libraries);
      assertEquals(1, libraries.length);
      moneyLibrary = (DartLibraryImpl) libraries[0];

      index = InMemoryIndex.getInstance();
      index.initializeIndex();
    } catch (Exception exception) {
      fail("Could not load money project");
    }
  }

  @Override
  public void tearDown() {
    if (index != null) {
      index.shutdown();
    }
  }

  public void test_SearchEngine_searchConstructorDeclarations() throws Exception {
    SearchEngine engine = createSearchEngine();
    List<SearchMatch> matches = engine.searchConstructorDeclarations(
        new WorkspaceSearchScope(),
        SearchPatternFactory.createPrefixPattern("Simpl", true),
        null,
        new NullProgressMonitor());
    assertEquals(2, matches.size());
  }

  public void test_SearchEngine_searchImplementors() throws Exception {
    Type type = moneyLibrary.getCompilationUnit("money.dart").getType("Money");
    SearchEngine engine = createSearchEngine();
    List<SearchMatch> matches = engine.searchImplementors(
        type,
        new WorkspaceSearchScope(),
        null,
        new NullProgressMonitor());
    assertEquals(2, matches.size());
  }

  public void test_SearchEngine_searchMethodDeclarations() throws Exception {
    SearchEngine engine = createSearchEngine();
    List<SearchMatch> matches = engine.searchMethodDeclarations(
        new LibrarySearchScope(moneyLibrary),
        SearchPatternFactory.createPrefixPattern("ad", true),
        null,
        new NullProgressMonitor());
    assertEquals(6, matches.size());
  }

  public void test_SearchEngine_searchReferences_field() throws Exception {
    Type type = moneyLibrary.getCompilationUnit("simple_money.dart").getType("SimpleMoney");
    Field field = type.getField("amount");
    SearchEngine engine = createSearchEngine();
    List<SearchMatch> matches = engine.searchReferences(
        field,
        new WorkspaceSearchScope(),
        null,
        new NullProgressMonitor());
    assertEquals(4, matches.size());
  }

  public void test_SearchEngine_searchReferences_function() throws Exception {
    TestProject testProject = new TestProject("Test");
    try {
      CompilationUnit unit = testProject.setUnitContent(
          "Test.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "test() {}",
              "foo() {",
              "  test();",
              "}",
              ""));
      indexUnit(unit);
      DartFunction function = (DartFunction) unit.getChildren()[0];
      SearchEngine engine = createSearchEngine();
      List<SearchMatch> matches = engine.searchReferences(
          function,
          new WorkspaceSearchScope(),
          null,
          new NullProgressMonitor());
      assertEquals(1, matches.size());
      assertFalse(matches.get(0).isQualified());
    } finally {
      testProject.dispose();
    }
  }

  public void test_SearchEngine_searchReferences_type() throws Exception {
    Type type = moneyLibrary.getCompilationUnit("simple_money.dart").getType("SimpleMoney");
    SearchEngine engine = createSearchEngine();
    List<SearchMatch> matches = engine.searchReferences(
        type,
        new WorkspaceSearchScope(),
        null,
        new NullProgressMonitor());
    assertEquals(20, matches.size()); // I believe that this should eventually be 17.
  }

  public void test_SearchEngine_searchReferences_variable() throws Exception {
    TestProject testProject = new TestProject("Test");
    try {
      CompilationUnit unit = testProject.setUnitContent(
          "Test.dart",
          Joiner.on("\n").join(
              "int globalCount;",
              "",
              "void main() {",
              "  globalCount = 0;",
              "  print(globalCount);",
              "}",
              ""));
      indexUnit(unit);
      DartVariableDeclaration variable = (DartVariableDeclaration) unit.getChildren()[0];
      SearchEngine engine = createSearchEngine();
      List<SearchMatch> matches = engine.searchReferences(
          variable,
          new WorkspaceSearchScope(),
          null,
          new NullProgressMonitor());
      assertEquals(2, matches.size());
    } finally {
      testProject.dispose();
    }
  }

  public void test_SearchEngine_searchTypeDeclarations() throws Exception {
    SearchEngine engine = createSearchEngine();
    List<SearchMatch> matches = engine.searchTypeDeclarations(
        new WorkspaceSearchScope(),
        SearchPatternFactory.createPrefixPattern("Money", true),
        (SearchFilter) null,
        new NullProgressMonitor());
    assertEquals(1, matches.size());
    for (SearchMatch match : matches) {
      if (isType(match, "Money")) {
        return;
      }
    }
    fail("Type Money not found");
  }

  public void test_searchReferences_field() throws Exception {
    TestProject testProject = new TestProject();
    try {
      String source = Joiner.on("\n").join(
          "// filler filler filler filler filler filler filler filler filler filler",
          "class A {",
          "  var test;",
          "}",
          "class B extends A {",
          "  foo() {",
          "    test = 1;",
          "    this.test = 2;",
          "  }",
          "}",
          "bar() {",
          "  A a = new A();",
          "  a.test = 3;",
          "}",
          "");
      CompilationUnit unit = testProject.setUnitContent("Test.dart", source);
      indexUnit(unit);
      // find references
      Field field = ((Type) unit.getChildren()[0]).getField("test");
      SearchEngine engine = createSearchEngine();
      List<SearchMatch> matches = engine.searchReferences(
          field,
          new WorkspaceSearchScope(),
          null,
          new NullProgressMonitor());
      assertThat(matches).hasSize(3);
      // assert "qualified"
      Map<Integer, Boolean> expected = ImmutableMap.of(
          source.indexOf("test = 1"),
          false,
          source.indexOf("test = 2"),
          true,
          source.indexOf("test = 3"),
          true);
      for (SearchMatch match : matches) {
        int matchOffset = match.getSourceRange().getOffset();
        assertEquals(expected.get(matchOffset).booleanValue(), match.isQualified());
      }
    } finally {
      testProject.dispose();
    }
  }

  public void test_searchReferences_method() throws Exception {
    TestProject testProject = new TestProject();
    try {
      String source = Joiner.on("\n").join(
          "// filler filler filler filler filler filler filler filler filler filler",
          "class A {",
          "  test(var p) {}",
          "}",
          "class B extends A {",
          "  foo() {",
          "    test(1);",
          "    this.test(2);",
          "  }",
          "}",
          "bar() {",
          "  A a = new A();",
          "  a.test(3);",
          "}",
          "");
      CompilationUnit unit = testProject.setUnitContent("Test.dart", source);
      indexUnit(unit);
      // find references
      Method method = ((Type) unit.getChildren()[0]).getMethod("test", null);
      SearchEngine engine = createSearchEngine();
      List<SearchMatch> matches = engine.searchReferences(
          method,
          new WorkspaceSearchScope(),
          null,
          new NullProgressMonitor());
      assertThat(matches).hasSize(3);
      // assert "qualified"
      Map<Integer, Boolean> expected = ImmutableMap.of(
          source.indexOf("test(1"),
          false,
          source.indexOf("test(2"),
          true,
          source.indexOf("test(3"),
          true);
      for (SearchMatch match : matches) {
        int matchOffset = match.getSourceRange().getOffset();
        assertEquals(
            match.toString(),
            expected.get(matchOffset).booleanValue(),
            match.isQualified());
      }
    } finally {
      testProject.dispose();
    }
  }

  public void test_searchReferences_type_fromConstructor() throws Exception {
    TestProject testProject = new TestProject();
    try {
      String source = Joiner.on("\n").join(
          "// filler filler filler filler filler filler filler filler filler filler",
          "class Test {",
          "  Test() {}",
          "  Test.named() {}",
          "}",
          "");
      CompilationUnit unit = testProject.setUnitContent("Test.dart", source);
      indexUnit(unit);
      // find references
      Type type = (Type) unit.getChildren()[0];
      SearchEngine engine = createSearchEngine();
      List<SearchMatch> matches = engine.searchReferences(
          type,
          new WorkspaceSearchScope(),
          null,
          new NullProgressMonitor());
      assertThat(matches).hasSize(2);
      // validate search matches
      Collections.sort(matches, new Comparator<SearchMatch>() {
        @Override
        public int compare(SearchMatch o1, SearchMatch o2) {
          return o1.getSourceRange().getOffset() - o2.getSourceRange().getOffset();
        }
      });
      assertEquals(
          new SourceRangeImpl(source.indexOf("Test()"), 4),
          matches.get(0).getSourceRange());
      assertEquals(
          new SourceRangeImpl(source.indexOf("Test.named()"), 4),
          matches.get(1).getSourceRange());
    } finally {
      testProject.dispose();
    }
  }

  private SearchEngine createSearchEngine() {
    return new NewSearchEngineImpl(index);
  }

  private boolean isType(SearchMatch match, String typeName) {
    DartElement element = match.getElement();
    return element instanceof Type && typeName.equals(element.getElementName());
  }
}
