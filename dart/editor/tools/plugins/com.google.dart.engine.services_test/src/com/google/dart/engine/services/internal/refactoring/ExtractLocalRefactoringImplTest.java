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
package com.google.dart.engine.services.internal.refactoring;

import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.refactoring.ExtractLocalRefactoring;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.services.status.RefactoringStatusSeverity;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for {@link ExtractLocalRefactoringImpl}.
 */
public class ExtractLocalRefactoringImplTest extends RefactoringImplTest {
  private ExtractLocalRefactoringImpl refactoring;
  private int selectionStart = -1;
  private int selectionEnd = -1;
  private String localName = "res";
  private boolean replaceAllOccurences = true;
  private RefactoringStatus refactoringStatus;

  public void test_bad_assignmentLeftHandSize() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f(p) {",
        "  var v = 0;",
        "  v = 1;",
        "}");
    // create refactoring
    selectionStart = findOffset("v = 1");
    selectionEnd = findOffset(" = 1;");
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Cannot extract the left-hand side of an assignment.");
  }

  public void test_bad_methodName_reference() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  main();",
        "}",
        "");
    selectionStart = findOffset("main();");
    selectionEnd = selectionStart + "main".length();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Cannot extract a single method name.");
  }

  public void test_bad_nameOfProperty_prefixedIdentifier() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f(p) {",
        "  p.value = 1;",
        "  print(p.value); // marker",
        "}",
        "print(x) {}");
    // create refactoring
    selectionStart = findOffset("value);");
    selectionEnd = findOffset("); // marker");
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Can not extract name part of a property access.");
  }

  public void test_bad_nameOfProperty_propertyAccess() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f(p) {",
        "  foo().value = 1;",
        "  print(foo().value); // marker",
        "}",
        "foo() {}",
        "print(x) {}");
    // create refactoring
    selectionStart = findOffset("value);");
    selectionEnd = findOffset("); // marker");
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Can not extract name part of a property access.");
  }

  public void test_bad_namePartOfDeclaration_variable() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int vvv = 0;",
        "}",
        "");
    selectionStart = findOffset("vvv =");
    selectionEnd = selectionStart + "vvv".length();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Cannot extract the name part of a declaration.");
  }

  public void test_bad_stringSelection_leadingQuote() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var vvv = 'abc';",
        "}",
        "");
    setSelectionString("'a");
    selectionEnd--;
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Cannot extract only leading or trailing quote of string literal.");
  }

  public void test_bad_stringSelection_trailingQuote() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var vvv = 'abc';",
        "}",
        "");
    setSelectionString("c'");
    selectionStart++;
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Cannot extract only leading or trailing quote of string literal.");
  }

  public void test_checkFinalConditions_sameVariable_after() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1 + 2;",
        "  var res;",
        "}");
    // create refactoring
    setSelectionString("1 + 2");
    createRefactoring();
    // conflicting name
    {
      refactoring.setLocalName("res");
      refactoringStatus = refactoring.checkFinalConditions(pm);
      assert_warning_alreadyDefined();
    }
    // unique name
    {
      refactoring.setLocalName("uniqueName");
      refactoringStatus = refactoring.checkFinalConditions(pm);
      assertRefactoringStatusOK(refactoringStatus);
    }
  }

  public void test_checkFinalConditions_sameVariable_before() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res;",
        "  int a = 1 + 2;",
        "}");
    // create refactoring
    setSelectionString("1 + 2");
    createRefactoring();
    // conflicting name
    {
      refactoring.setLocalName("res");
      refactoringStatus = refactoring.checkAllConditions(pm);
      assert_warning_alreadyDefined();
    }
    // unique name
    {
      refactoring.setLocalName("uniqueName");
      refactoringStatus = refactoring.checkAllConditions(pm);
      assertRefactoringStatusOK(refactoringStatus);
    }
  }

  public void test_checkFinalConditions_variableName_warning() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1 + 2;",
        "}");
    // create refactoring
    setSelectionString("1 + 2");
    localName = "Res";
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.WARNING,
        "Variable name should start with a lowercase letter.");
  }

  public void test_checkInitialConditions_notPartOfFunction() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int a = 1 + 2;",
        "");
    // create refactoring
    selectionStart = findOffset("1 + 2");
    selectionEnd = findOffset(";");
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Expression inside of function must be selected to activate this refactoring.");
  }

  public void test_checkLocalName() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1 + 2;",
        "}");
    // create refactoring
    setSelectionString("1 + 2");
    createRefactoring();
    // null
    assertRefactoringStatus(
        refactoring.checkLocalName(null),
        RefactoringStatusSeverity.ERROR,
        "Variable name must not be null.");
    // empty
    assertRefactoringStatus(
        refactoring.checkLocalName(""),
        RefactoringStatusSeverity.ERROR,
        "Variable name must not be empty.");
    // warning
    assertRefactoringStatus(
        refactoring.checkLocalName("Res"),
        RefactoringStatusSeverity.WARNING,
        "Variable name should start with a lowercase letter.");
    // OK
    assertRefactoringStatusOK(refactoring.checkLocalName("res"));
  }

  public void test_completeStatementExpression() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  Object o;",
        "  o.toString();",
        "}");
    // create refactoring
    setSelectionString("o.toString()");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  Object o;",
        "  var res = o.toString();",
        "}");
  }

  public void test_const_argument_inConstInstanceCreation() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  const A(int a, int b);",
        "}",
        "main() {",
        "  const A(1, 2);",
        "}");
    // create refactoring
    setSelectionString("1");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  const A(int a, int b);",
        "}",
        "main() {",
        "  const res = 1;",
        "  const A(res, 2);",
        "}");
  }

  public void test_const_element_inBinaryExpression() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  const [1 + 2, 3];",
        "}");
    // create refactoring
    setSelectionString("1");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  const res = 1;",
        "  const [res + 2, 3];",
        "}");
  }

  public void test_const_element_inConditionalExpression() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main(bool b) {",
        "  const [b ? 1 : 2, 3];",
        "}");
    // create refactoring
    setSelectionString("1");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main(bool b) {",
        "  const res = 1;",
        "  const [b ? res : 2, 3];",
        "}");
  }

  public void test_const_element_inConstListLiteral() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  const [1, 2];",
        "}");
    // create refactoring
    setSelectionString("1");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  const res = 1;",
        "  const [res, 2];",
        "}");
  }

  public void test_const_element_inParenthesis() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  const [(1), 2];",
        "}");
    // create refactoring
    setSelectionString("1");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  const res = 1;",
        "  const [(res), 2];",
        "}");
  }

  public void test_const_element_inPrefixExpression() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  const [!true, 3];",
        "}");
    // create refactoring
    setSelectionString("true");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  const res = true;",
        "  const [!res, 3];",
        "}");
  }

  public void test_const_key_inConstMapLiteral() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  const {1: 2};",
        "}");
    // create refactoring
    setSelectionString("1");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  const res = 1;",
        "  const {res: 2};",
        "}");
  }

  public void test_const_value_inConstMapLiteral() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  const {1: 2};",
        "}");
    // create refactoring
    setSelectionString("2");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  const res = 2;",
        "  const {1: res};",
        "}");
  }

  public void test_fragmentExpression_leadingNotWhitespace() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1 + 2 + 3 + 4;",
        "}");
    // create refactoring
    setSelectionString("+ 2");
    createRefactoring();
    // check conditions
    assert_fatalError_selection();
  }

  public void test_fragmentExpression_leadingPartialSelection() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 111 + 2 + 3 + 4;",
        "}");
    // create refactoring
    setSelectionString("11 + 2");
    createRefactoring();
    // check conditions
    assert_fatalError_selection();
  }

  public void test_fragmentExpression_leadingWhitespace() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1 + 2 + 3 + 4;",
        "}");
    // create refactoring
    setSelectionString(" 2 + 3");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res =  2 + 3;",
        "  int a = 1 +res + 4;",
        "}");
  }

  public void test_fragmentExpression_notAssociativeOperator() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1 - 2 - 3 - 4;",
        "}");
    // create refactoring
    setSelectionString("2 - 3");
    createRefactoring();
    // check conditions
    assert_fatalError_selection();
  }

  public void test_fragmentExpression_OK() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1 + 2 + 3 + 4;",
        "}");
    // create refactoring
    setSelectionString("2 + 3");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res = 2 + 3;",
        "  int a = 1 + res + 4;",
        "}");
  }

  public void test_fragmentExpression_trailingNotWhitespace() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1 + 2 + 3 + 4;",
        "}");
    // create refactoring
    setSelectionString("2 + 3 +");
    createRefactoring();
    // check conditions
    assert_fatalError_selection();
  }

  public void test_fragmentExpression_trailingPartialSelection() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1 + 2 + 3 + 444;",
        "}");
    // create refactoring
    setSelectionString("2 + 3 + 44");
    createRefactoring();
    // check conditions
    assert_fatalError_selection();
  }

  public void test_fragmentExpression_trailingWhitespace() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1 + 2 + 3 + 4;",
        "}");
    // create refactoring
    setSelectionString("2 + 3 ");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res = 2 + 3 ;",
        "  int a = 1 + res+ 4;",
        "}");
  }

  public void test_getRefactoringName() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(1 + 2);",
        "}");
    // create refactoring
    setSelectionString("1 + 2");
    createRefactoring();
    // access
    assertEquals("Extract Local Variable", refactoring.getRefactoringName());
  }

  public void test_guessNames_fragmentExpression() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class TreeItem {}",
        "TreeItem getSelectedItem() => null;",
        "process(arg) {}",
        "main() {",
        "  process(111 + 222 + 333 + 444); // marker",
        "}");
    // create refactoring
    setSelectionString("222 + 333");
    createRefactoring();
    // no guesses
    String[] names = refactoring.guessNames();
    assertThat(names).isEmpty();
  }

  public void test_guessNames_singleExpression() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class TreeItem {}",
        "TreeItem getSelectedItem() => null;",
        "process(arg) {}",
        "main() {",
        "  process(getSelectedItem()); // marker",
        "}");
    // create refactoring
    selectionStart = findOffset("getSelectedItem()); // marker");
    selectionEnd = findOffset("); // marker");
    createRefactoring();
    // check guesses
    String[] names = refactoring.guessNames();
    assertThat(names).contains("selectedItem", "item", "arg", "treeItem");
  }

  public void test_guessNames_stringPart() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var s = 'Hello Bob... welcome to Dart!';",
        "}");
    // create refactoring
    setSelectionString("Hello Bob");
    createRefactoring();
    // check guesses
    String[] names = refactoring.guessNames();
    assertThat(names).contains("helloBob", "bob");
  }

  public void test_hasSeveralOccurrences_false() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1 + 2;",
        "}");
    // create refactoring
    setSelectionString("1 + 2");
    createRefactoring();
    assertFalse(refactoring.hasSeveralOccurrences());
  }

  public void test_hasSeveralOccurrences_true() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1 + 2;",
        "  int b = 1 + 2;",
        "}");
    // create refactoring
    setSelectionString("1 + 2");
    createRefactoring();
    assertTrue(refactoring.hasSeveralOccurrences());
  }

  public void test_occurences_disableOccurences() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int foo() => 42;",
        "main() {",
        "  int a = 1 + foo();",
        "  int b = 2 +  foo(); // marker",
        "}");
    // create refactoring
    selectionStart = findOffset("  foo();") + 2;
    selectionEnd = findOffset("; // marker");
    replaceAllOccurences = false;
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int foo() => 42;",
        "main() {",
        "  int a = 1 + foo();",
        "  var res = foo();",
        "  int b = 2 +  res; // marker",
        "}");
  }

  public void test_occurences_fragmentExpression() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int foo() => 42;",
        "main() {",
        "  int a = 11 + 2 + foo() + 3;",
        "  int b = 12 +  2 + foo() + 3; // marker",
        "}");
    // create refactoring
    selectionStart = findOffset("  2 +") + 2;
    selectionEnd = findOffset("; // marker");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int foo() => 42;",
        "main() {",
        "  var res = 2 + foo() + 3;",
        "  int a = 11 + res;",
        "  int b = 12 +  res; // marker",
        "}");
  }

  public void test_occurences_ignore_assignmentLeftHandSize() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v = 1;",
        "  v = 2;",
        "  print(() {v = 2;});",
        "  print(1 + (() {v = 2; return 3;})());",
        "  print(v); // marker",
        "}",
        "print(x) {}");
    // create refactoring
    selectionStart = findOffset("v);");
    selectionEnd = findOffset("); // marker");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v = 1;",
        "  v = 2;",
        "  print(() {v = 2;});",
        "  print(1 + (() {v = 2; return 3;})());",
        "  var res = v;",
        "  print(res); // marker",
        "}",
        "print(x) {}");
  }

  public void test_occurences_ignore_nameOfVariableDeclariton() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v = 1;",
        "  print(v); // marker",
        "}",
        "print(x) {}");
    // create refactoring
    selectionStart = findOffset("v);");
    selectionEnd = findOffset("); // marker");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v = 1;",
        "  var res = v;",
        "  print(res); // marker",
        "}",
        "print(x) {}");
  }

  public void test_occurences_singleExpression() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int foo() => 42;",
        "main() {",
        "  int a = 1 + foo();",
        "  int b = 2 +  foo(); // marker",
        "}");
    // create refactoring
    selectionStart = findOffset("  foo();") + 2;
    selectionEnd = findOffset("; // marker");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int foo() => 42;",
        "main() {",
        "  var res = foo();",
        "  int a = 1 + res;",
        "  int b = 2 +  res; // marker",
        "}");
  }

  public void test_occurences_useDominator() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true) {",
        "    print(42);",
        "  } else {",
        "    print(42);",
        "  }",
        "}");
    // create refactoring
    selectionStart = findOffset("42");
    selectionEnd = findOffset("42);") + "42".length();
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res = 42;",
        "  if (true) {",
        "    print(res);",
        "  } else {",
        "    print(res);",
        "  }",
        "}");
  }

  public void test_occurences_whenComment() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int foo() => 42;",
        "main() {",
        "  /*int a = 1 + foo();*/",
        "  int b = 2 +  foo(); // marker",
        "}");
    // create refactoring
    selectionStart = findOffset("  foo();") + 2;
    selectionEnd = findOffset("; // marker");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int foo() => 42;",
        "main() {",
        "  /*int a = 1 + foo();*/",
        "  var res = foo();",
        "  int b = 2 +  res; // marker",
        "}");
  }

  public void test_occurences_whenSpace() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int foo(String s) => 42;",
        "main() {",
        "  int a = 1 + foo('has space');",
        "  int b = 2 +  foo('has space'); // marker",
        "}");
    // create refactoring
    selectionStart = findOffset("  foo('has space');") + 2;
    selectionEnd = findOffset("; // marker");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int foo(String s) => 42;",
        "main() {",
        "  var res = foo('has space');",
        "  int a = 1 + res;",
        "  int b = 2 +  res; // marker",
        "}");
  }

  public void test_singleExpression() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(1 + 2);",
        "}");
    // create refactoring
    setSelectionString("1 + 2");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res = 1 + 2;",
        "  print(res);",
        "}");
  }

  public void test_singleExpression_getter() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int get foo => 42;",
        "}",
        "main() {",
        "  A a = new A();",
        "  int b = 1 + a.foo; // marker",
        "}");
    // create refactoring
    selectionStart = findOffset("a.foo;");
    selectionEnd = findOffset("; // marker");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int get foo => 42;",
        "}",
        "main() {",
        "  A a = new A();",
        "  var res = a.foo;",
        "  int b = 1 + res; // marker",
        "}");
  }

  public void test_singleExpression_inMethod() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  main() {",
        "    print(1 + 2);",
        "  }",
        "}");
    // create refactoring
    setSelectionString("1 + 2");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  main() {",
        "    var res = 1 + 2;",
        "    print(res);",
        "  }",
        "}");
  }

  public void test_singleExpression_leadingNotWhitespace() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 12 + 345; // marker",
        "}");
    // create refactoring
    setSelectionString("+ 345");
    createRefactoring();
    // check conditions
    assert_fatalError_selection();
  }

  public void test_singleExpression_leadingWhitespace() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1 + 2; // marker",
        "}");
    // create refactoring
    setSelectionString(" 1 + 2");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res =  1 + 2;",
        "  int a =res; // marker",
        "}");
  }

  /**
   * We use here knowledge how exactly <code>1 + 2 + 3 + 4</code> is parsed. We know that
   * <code>1 + 2</code> will be separate and complete {@link DartBinaryExpression}, so can be
   * handled as single expression.
   */
  public void test_singleExpression_partOfBinaryExpression() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1 + 2 + 3 + 4;",
        "}");
    // create refactoring
    setSelectionString("1 + 2");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res = 1 + 2;",
        "  int a = res + 3 + 4;",
        "}");
  }

  public void test_singleExpression_trailingComment() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 12 /*abc*/ + 345;",
        "}");
    // create refactoring
    setSelectionString("12 /*abc*/");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res = 12 /*abc*/;",
        "  int a = res + 345;",
        "}");
  }

  public void test_singleExpression_trailingNotWhitespace() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 12 + 345; // marker",
        "}");
    // create refactoring
    setSelectionString("12 +");
    createRefactoring();
    // check conditions
    assert_fatalError_selection();
  }

  public void test_singleExpression_trailingWhitespace() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1 + 2 ; // marker",
        "}");
    // create refactoring
    setSelectionString("1 + 2 ");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res = 1 + 2 ;",
        "  int a = res; // marker",
        "}");
  }

  public void test_stringLiteral_part() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print('abcdefgh');",
        "}");
    // create refactoring
    setSelectionString("cde");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res = 'cde';",
        "  print('ab${res}fgh');",
        "}");
  }

  public void test_stringLiteral_whole() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print('abc');",
        "}");
    // create refactoring
    setSelectionString("'abc'");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res = 'abc';",
        "  print(res);",
        "}");
  }

  /**
   * Checks that all conditions are <code>OK</code> and applying {@link Change} to the
   * {@link #testUnit} is same source as given lines.
   */
  protected final void assertSuccessfulRefactoring(String... lines) throws Exception {
    assertRefactoringStatus(refactoringStatus, RefactoringStatusSeverity.OK, null);
    Change change = refactoring.createChange(pm);
    assertTestChangeResult(change, makeSource(lines));
  }

  @Override
  protected void tearDown() throws Exception {
    refactoring = null;
    refactoringStatus = null;
    super.tearDown();
  }

  /**
   * Asserts that {@link refactoringStatus} has fatal error caused by selection.
   */
  private void assert_fatalError_selection() throws Exception {
    RefactoringStatus status = refactoring.checkInitialConditions(pm);
    assertRefactoringStatus(
        status,
        RefactoringStatusSeverity.FATAL,
        "Expression must be selected to activate this refactoring.");
  }

  private void assert_warning_alreadyDefined() {
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.WARNING,
        "A variable with name 'res' is already defined in the visible scope.");
  }

  /**
   * Creates {@link ExtractLocalRefactoring} in {@link #refactoring}.
   */
  private void createRefactoring() throws Exception {
    int selectionLength = selectionEnd - selectionStart;
    AssistContext context = new AssistContext(
        searchEngine,
        analysisContext,
        null,
        testSource,
        testUnit,
        selectionStart,
        selectionLength);
    refactoring = new ExtractLocalRefactoringImpl(context);
    refactoring.setLocalName(localName);
    refactoring.setReplaceAllOccurrences(replaceAllOccurences);
    // prepare status
    refactoringStatus = refactoring.checkAllConditions(pm);
  }

  /**
   * Prints result of {@link #refactoring} in the way ready to parse into test expectations.
   */
  @SuppressWarnings("unused")
  private void printRefactoringResultSource() throws Exception {
    printRefactoringTestSourceResult(getAnalysisContext(), refactoring);
  }

  /**
   * Sets selection to the start of the first occurrence of the given string.
   */
  private void setSelectionString(String pattern) {
    selectionStart = findOffset(pattern);
    selectionEnd = findEnd(pattern);
  }
}
