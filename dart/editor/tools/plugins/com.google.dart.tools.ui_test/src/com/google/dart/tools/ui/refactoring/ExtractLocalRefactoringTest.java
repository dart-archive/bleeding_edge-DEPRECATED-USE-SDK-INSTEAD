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
package com.google.dart.tools.ui.refactoring;

import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.tools.internal.corext.refactoring.code.ExtractLocalRefactoring;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for {@link ExtractLocalRefactoring}.
 */
public final class ExtractLocalRefactoringTest extends RefactoringTest {
  private static final IProgressMonitor pm = new NullProgressMonitor();

  private int selectionStart;
  private int selectionEnd;
  private boolean replaceAllOccurences = true;
  private ExtractLocalRefactoring refactoring;
  private RefactoringStatus refactoringStatus;

  public void test_access() throws Exception {
    setTestUnitContent();
    createRefactoring("res");
    assertEquals("Extract Local Variable", refactoring.getName());
    assertThat(refactoring.guessNames()).isEmpty();
    assertEquals(true, refactoring.replaceAllOccurrences());
  }

  public void test_bad_notPartOfFunction() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int a = 1 + 2;",
        "");
    selectionStart = findOffset("1 + 2");
    selectionEnd = findOffset(";");
    // check conditions
    createRefactoring("res");
    assertTrue(refactoringStatus.hasFatalError());
    assertEquals(
        "An expression inside of function must be selected to activate this refactoring.",
        refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL));
  }

  public void test_bad_sameVariable_after() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1 + 2;",
        "  var res;",
        "}");
    selectionStart = findOffset("1");
    selectionEnd = findOffset("2;") + 1;
    // check conditions
    createRefactoring("res");
    assert_warning_alreadyDefined();
    // use checkLocalName() - conflicting name
    {
      refactoringStatus = refactoring.checkLocalName("res");
      assert_warning_alreadyDefined();
    }
    // use checkLocalName() - unique name
    {
      refactoringStatus = refactoring.checkLocalName("uniqueName");
      assertTrue(refactoringStatus.isOK());
    }
  }

  public void test_bad_sameVariable_before() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var res;",
        "  int a = 1 + 2;",
        "}");
    selectionStart = findOffset("1");
    selectionEnd = findOffset("2;") + 1;
    // check conditions
    createRefactoring("res");
    assert_warning_alreadyDefined();
    // use checkLocalName() - conflicting name
    {
      refactoringStatus = refactoring.checkLocalName("res");
      assert_warning_alreadyDefined();
    }
    // use checkLocalName() - unique name
    {
      refactoringStatus = refactoring.checkLocalName("uniqueName");
      assertTrue(refactoringStatus.isOK());
    }
  }

  public void test_fragmentExpression_leadingNotWhitespace() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1 + 2 + 3 + 4;",
        "}");
    selectionStart = findOffset("+ 2");
    selectionEnd = findOffset("3 + ") + 1;
    // check conditions
    createRefactoring("res");
    assert_fatalError_selection();
  }

  public void test_fragmentExpression_leadingPartialSelection() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 111 + 2 + 3 + 4;",
        "}");
    selectionStart = findOffset("11 +");
    selectionEnd = findOffset("3 +") + 3;
    // check conditions
    createRefactoring("res");
    assert_fatalError_selection();
  }

  public void test_fragmentExpression_leadingWhitespace() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1 + 2 + 3 + 4;",
        "}");
    selectionStart = findOffset(" 2");
    selectionEnd = findOffset("3 + ") + 1;
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int res =  2 + 3;",
        "  int a = 1 +res + 4;",
        "}");
  }

  public void test_fragmentExpression_notAssociativeOperator() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1 - 2 - 3 - 4;",
        "}");
    selectionStart = findOffset("2");
    selectionEnd = findOffset("3 - ") + 1;
    // check conditions
    createRefactoring("res");
    assert_fatalError_selection();
  }

  public void test_fragmentExpression_OK() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1 + 2 + 3 + 4;",
        "}");
    selectionStart = findOffset("2");
    selectionEnd = findOffset("3 + ") + 1;
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int res = 2 + 3;",
        "  int a = 1 + res + 4;",
        "}");
  }

  public void test_fragmentExpression_trailingNotWhitespace() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1 + 2 + 3 + 4;",
        "}");
    selectionStart = findOffset("2");
    selectionEnd = findOffset("3 +") + 3;
    // check conditions
    createRefactoring("res");
    assert_fatalError_selection();
  }

  public void test_fragmentExpression_trailingPartialSelection() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1 + 2 + 3 + 444;",
        "}");
    selectionStart = findOffset("2");
    selectionEnd = findOffset("44;");
    // check conditions
    createRefactoring("res");
    assert_fatalError_selection();
  }

  public void test_fragmentExpression_trailingWhitespace() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1 + 2 + 3 + 4;",
        "}");
    selectionStart = findOffset("2");
    selectionEnd = findOffset("3 + ") + 2;
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int res = 2 + 3 ;",
        "  int a = 1 + res+ 4;",
        "}");
  }

  public void test_guessNames_fragmentExpression() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class TreeItem {}",
        "TreeItem getSelectedItem() => null;",
        "process(arg) {}",
        "main() {",
        "  process(111 + 222 + 333 + 444); // marker",
        "}");
    selectionStart = findOffset("222 +");
    selectionEnd = findOffset(" + 444");
    createRefactoring("res");
    // no guesses
    String[] names = refactoring.guessNames();
    assertThat(names).isEmpty();
  }

  public void test_guessNames_singleExpression() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class TreeItem {}",
        "TreeItem getSelectedItem() => null;",
        "process(arg) {}",
        "main() {",
        "  process(getSelectedItem()); // marker",
        "}");
    selectionStart = findOffset("getSelectedItem()); // marker");
    selectionEnd = findOffset("); // marker");
    createRefactoring("res");
    // check guesses
    String[] names = refactoring.guessNames();
    assertThat(names).contains("selectedItem", "item", "arg", "treeItem");
  }

  public void test_guessNames_stringPart() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var s = 'Hello Bob... welcome to Dart!';",
        "}");
    selectionStart = findOffset("Hello Bob");
    selectionEnd = findOffset("...");
    createRefactoring("res");
    // check guesses
    String[] names = refactoring.guessNames();
    assertThat(names).contains("helloBob", "bob");
  }

  public void test_occurences_disableOccurences() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int foo() => 42;",
        "main() {",
        "  int a = 1 + foo();",
        "  int b = 2 +  foo(); // marker",
        "}");
    selectionStart = findOffset("  foo();") + 2;
    selectionEnd = findOffset("; // marker");
    replaceAllOccurences = false;
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int foo() => 42;",
        "main() {",
        "  int a = 1 + foo();",
        "  int res = foo();",
        "  int b = 2 +  res; // marker",
        "}");
  }

  public void test_occurences_fragmentExpression() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int foo() => 42;",
        "main() {",
        "  int a = 1 + 2 + foo() + 3;",
        "  int b = 1 +  2 + foo() + 3; // marker",
        "}");
    selectionStart = findOffset("  2 +") + 2;
    selectionEnd = findOffset("; // marker");
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int foo() => 42;",
        "main() {",
        "  int res = 2 + foo() + 3;",
        "  int a = 1 + res;",
        "  int b = 1 +  res; // marker",
        "}");
  }

  public void test_occurences_singleExpression() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int foo() => 42;",
        "main() {",
        "  int a = 1 + foo();",
        "  int b = 2 +  foo(); // marker",
        "}");
    selectionStart = findOffset("  foo();") + 2;
    selectionEnd = findOffset("; // marker");
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int foo() => 42;",
        "main() {",
        "  int res = foo();",
        "  int a = 1 + res;",
        "  int b = 2 +  res; // marker",
        "}");
  }

  public void test_occurences_useDominator() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true) {",
        "    print(42);",
        "  } else {",
        "    print(42);",
        "  }",
        "}");
    selectionStart = findOffset("42");
    selectionEnd = findOffset("42);") + "42".length();
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int res = 42;",
        "  if (true) {",
        "    print(res);",
        "  } else {",
        "    print(res);",
        "  }",
        "}");
  }

  public void test_occurences_whenComment() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int foo() => 42;",
        "main() {",
        "  /*int a = 1 + foo();*/",
        "  int b = 2 +  foo(); // marker",
        "}");
    selectionStart = findOffset("  foo();") + 2;
    selectionEnd = findOffset("; // marker");
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int foo() => 42;",
        "main() {",
        "  /*int a = 1 + foo();*/",
        "  int res = foo();",
        "  int b = 2 +  res; // marker",
        "}");
  }

  public void test_occurences_whenSpace() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int foo(String s) => 42;",
        "main() {",
        "  int a = 1 + foo('has space');",
        "  int b = 2 +  foo('has space'); // marker",
        "}");
    selectionStart = findOffset("  foo('has space');") + 2;
    selectionEnd = findOffset("; // marker");
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int foo(String s) => 42;",
        "main() {",
        "  int res = foo('has space');",
        "  int a = 1 + res;",
        "  int b = 2 +  res; // marker",
        "}");
  }

  public void test_singleExpression() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1 + 2;",
        "}");
    selectionStart = findOffset("1");
    selectionEnd = findOffset("2;") + 1;
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int res = 1 + 2;",
        "  int a = res;",
        "}");
  }

  public void test_singleExpression_Dynamic() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {}",
        "main() {",
        "  int a = f(); // marker",
        "}");
    selectionStart = findOffset("f();");
    selectionEnd = findOffset("; // marker");
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {}",
        "main() {",
        "  var res = f();",
        "  int a = res; // marker",
        "}");
  }

  public void test_singleExpression_getter() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int get foo => 42;",
        "}",
        "main() {",
        "  A a = new A();",
        "  int b = 1 + a.foo; // marker",
        "}");
    selectionStart = findOffset("a.foo;");
    selectionEnd = findOffset("; // marker");
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int get foo => 42;",
        "}",
        "main() {",
        "  A a = new A();",
        "  int res = a.foo;",
        "  int b = 1 + res; // marker",
        "}");
  }

  public void test_singleExpression_leadingNotWhitespace() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 12 + 345; // marker",
        "}");
    selectionStart = findOffset("345") - 2;
    selectionEnd = findOffset("; // marker");
    // check conditions
    createRefactoring("res");
    assert_fatalError_selection();
  }

  public void test_singleExpression_leadingWhitespace() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1 + 2; // marker",
        "}");
    selectionStart = findOffset(" 1");
    selectionEnd = findOffset("; // marker");
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int res =  1 + 2;",
        "  int a =res; // marker",
        "}");
  }

  /**
   * We use here knowledge how exactly <code>1 + 2 + 3 + 4</code> is parsed. We know that
   * <code>1 + 2</code> will be separate and complete {@link DartBinaryExpression}, so can be
   * handled as single expression.
   */
  public void test_singleExpression_partOfBinaryExpression() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1 + 2 + 3 + 4;",
        "}");
    selectionStart = findOffset("1");
    selectionEnd = findOffset("2 + ") + 1;
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int res = 1 + 2;",
        "  int a = res + 3 + 4;",
        "}");
  }

  public void test_singleExpression_trailingComment() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 12 /*abc*/ + 345;",
        "}");
    selectionStart = findOffset("12");
    selectionEnd = findOffset("*/ + 345") + 2;
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int res = 12 /*abc*/;",
        "  int a = res + 345;",
        "}");
  }

  public void test_singleExpression_trailingNotWhitespace() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 12 + 345; // marker",
        "}");
    selectionStart = findOffset("12");
    selectionEnd = findOffset(" 345");
    // check conditions
    createRefactoring("res");
    assert_fatalError_selection();
  }

  public void test_singleExpression_trailingWhitespace() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1 + 2 ; // marker",
        "}");
    selectionStart = findOffset("1");
    selectionEnd = findOffset("; // marker");
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int res = 1 + 2 ;",
        "  int a = res; // marker",
        "}");
  }

  public void test_stringPart() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var s1 = 'aaa bbb ccc bbbb ddd';",
        "  var s2 = 'aaa bbb ccc bbbb ddd';",
        "}");
    selectionStart = findOffset("bbb");
    selectionEnd = findOffset(" ccc");
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var res = \"bbb\";",
        "  var s1 = 'aaa ${res} ccc ${res}b ddd';",
        "  var s2 = 'aaa ${res} ccc ${res}b ddd';",
        "}");
  }

  /**
   * Asserts that {@link refactoringStatus} has fatal error caused by selection.
   */
  private void assert_fatalError_selection() {
    assertTrue(refactoringStatus.hasFatalError());
    assertEquals(
        "An expression must be selected to activate this refactoring.",
        refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL));
  }

  private void assert_warning_alreadyDefined() {
    assertTrue(refactoringStatus.hasWarning());
    assertEquals(
        "A variable with name 'res' is already defined in the visible scope.",
        refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.WARNING));
  }

  /**
   * Creates refactoring and checks all conditions.
   */
  private void createRefactoring(String name) throws Exception {
    int selectionLength = selectionEnd - selectionStart;
    refactoring = new ExtractLocalRefactoring(testUnit, selectionStart, selectionLength);
    refactoring.setLocalName(name);
    refactoring.setReplaceAllOccurrences(replaceAllOccurences);
    refactoringStatus = refactoring.checkAllConditions(pm);
  }

  private void doSuccessfullRefactoring() throws Exception {
    // create refactoring
    createRefactoring("res");
    // OK status
    if (!refactoringStatus.isOK()) {
      fail(refactoringStatus.toString());
    }
    // perform change
    performRefactoringChange();
  }

  private void performRefactoringChange() throws Exception {
    ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        Change change = refactoring.createChange(pm);
        change.initializeValidationData(pm);
        new PerformChangeOperation(change).run(pm);
      }
    }, null);
  }
}
