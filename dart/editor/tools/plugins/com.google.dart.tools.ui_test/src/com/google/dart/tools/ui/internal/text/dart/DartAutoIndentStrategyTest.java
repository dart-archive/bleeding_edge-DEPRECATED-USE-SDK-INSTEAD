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
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.tools.core.formatter.DefaultCodeFormatterConstants;
import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.text.DartPartitions;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

import static org.fest.assertions.Assertions.assertThat;

public class DartAutoIndentStrategyTest extends EngineTestCase {
  private static final String EOL = System.getProperty("line.separator", "\n");
  private static final String USE_SPACES = AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS;
  private static final String TAB_CHAR = DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR;

  private static void assert_customizeDocumentCommand(String initial, String newText,
      String expected) {
    int initialOffset = initial.indexOf('!');
    int expectedOffset = expected.indexOf('!');
    assertTrue("No cursor position in initial: " + initial, initialOffset != -1);
    assertTrue("No cursor position in expected: " + expected, expectedOffset != -1);
    initial = StringUtils.remove(initial, '!');
    expected = StringUtils.remove(expected, '!');
    // force "smart mode"
    DartAutoIndentStrategy_NEW strategy = new DartAutoIndentStrategy_NEW(
        DartPartitions.DART_PARTITIONING,
        null) {
      @Override
      protected boolean computeSmartMode() {
        return true;
      }
    };
    // prepare document
    IDocument document = new Document(initial);
    // handle command
    DocumentCommand command = new DocumentCommand() {
    };
    command.caretOffset = -1;
    command.doit = true;
    command.offset = initialOffset;
    command.text = newText;
    strategy.customizeDocumentCommand(document, command);
    // update document
    ReflectionUtils.invokeMethod(command, "execute(org.eclipse.jface.text.IDocument)", document);
    // check new content
    String actual = document.get();
    assertEquals(expected, actual);
    // check caret offset
    int actualOffset = command.caretOffset;
    if (actualOffset == -1) {
      actualOffset = initialOffset + command.text.length();
    }
    assertThat(actualOffset).isEqualTo(expectedOffset);
  }

  private static void assertSmartInsertAfterNewLine(String initial, String expected) {
    assert_customizeDocumentCommand(initial, EOL, expected);
  }

  private static void assertSmartPaste(String initial, String newText, String expected) {
    assert_customizeDocumentCommand(initial, newText, expected);

  }

  public void test_afterConditional_withInvocation() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "main() {",
        "  var v = true ?",
        "      ''.length() : 0;!",
        "}"), createSource(//
        "main() {",
        "  var v = true ?",
        "      ''.length() : 0;",
        "  !",
        "}"));
  }

  public void test_afterFor_withBody() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "main() {",
        "  for (var x = 0; x < 10; x++) print(x);!",
        "}"), createSource(//
        "main() {",
        "  for (var x = 0; x < 10; x++) print(x);",
        "  !",
        "}"));
  }

  public void test_afterForIn_noBody() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "main() {",
        "  for (var x in [1, 2, 3])!",
        "}"), createSource(//
        "main() {",
        "  for (var x in [1, 2, 3])",
        "    !",
        "}"));
  }

  public void test_afterForIn_withBody() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "main() {",
        "  for (var x in [1, 2, 3]) print(x);!",
        "}"), createSource(//
        "main() {",
        "  for (var x in [1, 2, 3]) print(x);",
        "  !",
        "}"));
  }

  public void test_inMapLiteral_inList() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "main() {",
        "  [{'A': 0,",
        "           'B': 0},",
        "   {'A': 1,! 'B': 1}];",
        "}"), createSource(//
        "main() {",
        "  [{'A': 0,",
        "           'B': 0},",
        "   {'A': 1,",
        "     !'B': 1}];",
        "}"));
  }

  public void test_smartIndentAfterNewLine_afterClassPrologue() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "class A {!",
        "main() {}"), createSource(//
        "class A {",
        "  !",
        "}",
        "main() {}"));
  }

  public void test_smartIndentAfterNewLine_afterClassPrologue_withExtends() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "class A extends Object implements B, C, D {!",
        "main() {}"), createSource(//
        "class A extends Object implements B, C, D {",
        "  !",
        "}",
        "main() {}"));
  }

  public void test_smartIndentAfterNewLine_afterLBrace_beforeAnd() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "main() {",
        "  if (true) {! && false) {",
        "    print(42);",
        "  }",
        "}"), createSource(//
        "main() {",
        "  if (true) {",
        "    !&& false) {",
        "    print(42);",
        "  }",
        "}"));
  }

  public void test_smartIndentAfterNewLine_afterMethod() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "class A {",
        "  m();!",
        "}"), createSource(//
        "class A {",
        "  m();",
        "  !",
        "}"));
  }

  public void test_smartIndentAfterNewLine_block_closed_betweenBraces() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "main() {",
        "  {!}",
        "}"), createSource(//
        "main() {",
        "  {",
        "    !",
        "  }",
        "}"));
  }

  public void test_smartIndentAfterNewLine_block_noClosed() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "main() {",
        "  {!",
        "}"), createSource(//
        "main() {",
        "  {",
        "    !",
        "  }",
        "}"));
  }

  /**
   * <p>
   * https://code.google.com/p/dart/issues/detail?id=16899
   */
  public void test_smartIndentAfterNewLine_blockAfterWrappedArgumentList() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "main() {",
        "  f(",
        "      111,",
        "      222).then(() {!",
        "  });",
        "}"), createSource(//
        "main() {",
        "  f(",
        "      111,",
        "      222).then(() {",
        "    !",
        "  });",
        "}"));
  }

  public void test_smartIndentAfterNewLine_class_noClosed() throws Exception {
    assertSmartInsertAfterNewLine("class A {!", createSource(//
        "class A {",
        "  !",
        "}").trim());
  }

  public void test_smartIndentAfterNewLine_class_noClosed_leadingEOLC() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "// this comment used to cause a problem",
        "class A {!",
        ""), createSource(//
        "// this comment used to cause a problem",
        "class A {",
        "  !",
        "}",
        ""));
  }

  public void test_smartIndentAfterNewLine_closure_closed() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "main() {",
        "  print((_) {!});",
        "}"), createSource(//
        "main() {",
        "  print((_) {",
        "    !",
        "  });",
        "}"));
  }

  public void test_smartIndentAfterNewLine_closure_notClosed_beforeComma() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "main() {",
        "  print((_) {!, 42);",
        "}"), createSource(//
        "main() {",
        "  print((_) {",
        "    !",
        "  }, 42);",
        "}"));
  }

  public void test_smartIndentAfterNewLine_closure_notClosed_beforeRBracket() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "main() {",
        "  print([(_) {!]);",
        "}"), createSource(//
        "main() {",
        "  print([(_) {",
        "    !",
        "  }]);",
        "}"));
  }

  public void test_smartIndentAfterNewLine_closure_notClosed_beforeRParen() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "main() {",
        "  print((_) {!);",
        "}"), createSource(//
        "main() {",
        "  print((_) {",
        "    !",
        "  });",
        "}"));
  }

  public void test_smartIndentAfterNewLine_function_hasClosed() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "main() {!}"),
        createSource(//
            "main() {",
            "  !",
            "}"));
  }

  public void test_smartIndentAfterNewLine_function_hasClosed_hasStatement() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "main() {!",
        "  print(0);",
        "}"), createSource(//
        "main() {",
        "  !",
        "  print(0);",
        "}"));
  }

  /**
   * We don't wrap the expression into {} as per discussion in
   * https://code.google.com/p/dart/issues/detail?id=17046
   */
  public void test_smartIndentAfterNewLine_function_hasExpression_sameLine() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "main() {!42;"),
        createSource(//
            "main() {",
            "  !42;"));
  }

  public void test_smartIndentAfterNewLine_function_noClosed() throws Exception {
    assertSmartInsertAfterNewLine("main() {!", createSource(//
        "main() {",
        "  !",
        "}").trim());
  }

  public void test_smartIndentAfterNewLine_method_hasClosed() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "class A {",
        "  m() {!}",
        "}"), createSource(//
        "class A {",
        "  m() {",
        "    !",
        "  }",
        "}"));
  }

  /**
   * We don't wrap the statement into {} as per discussion in
   * https://code.google.com/p/dart/issues/detail?id=17046
   */
  public void test_smartIndentAfterNewLine_wrapIntoBlock() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "main() {",
        "  if (true) {!print();",
        "}"), createSource(//
        "main() {",
        "  if (true) {",
        "    !print();",
        "}"));
  }

  public void test_smartPaste_cascade_afterCascade() throws Exception {
    assertSmartPaste(createSource(//
        "main() {",
        "  a",
        "      ..b = 100",
        "!      ..c()",
        "      ..d();",
        "}"), "..x()" + EOL, createSource(//
        "main() {",
        "  a",
        "      ..b = 100",
        "      ..x()",
        "!      ..c()",
        "      ..d();",
        "}"));
  }

  public void test_smartPaste_cascade_afterTarget() throws Exception {
    assertSmartPaste(createSource(//
        "main() {",
        "  a",
        "!      ..b = 100",
        "      ..c()",
        "      ..d();",
        "}"), "..x()" + EOL, createSource(//
        "main() {",
        "  a",
        "      ..x()",
        "!      ..b = 100",
        "      ..c()",
        "      ..d();",
        "}"));
  }

  /**
   * We should keep original difference between first and following lines.
   */
  public void test_smartPaste_cascade_complete_onlyAssignments() throws Exception {
    assertSmartPaste(createSource(//
        "main() {",
        "!}"), createSource(//
        "    aaa",
        "       ..bbb = 1",
        "      ..ccc = 2;"), createSource(//
        "main() {",
        "  aaa",
        "     ..bbb = 1",
        "    ..ccc = 2;",
        "!}"));
  }

  /**
   * We should keep original difference between first and following lines.
   */
  public void test_smartPaste_cascade_complete_withInvocation() throws Exception {
    assertSmartPaste(createSource(//
        "main() {",
        "!}"), createSource(//
        "    aaa",
        "       ..bbb()",
        "      ..ccc = 2;"), createSource(//
        "main() {",
        "  aaa",
        "     ..bbb()",
        "    ..ccc = 2;",
        "!}"));
  }

  public void test_smartPaste_multiLineString_doubleQuote() throws Exception {
    assertSmartPaste(createSource(//
        "main() {",
        "  var a = !;",
        "}"), "\"\"\"" + EOL + "000" + EOL + "  111" + EOL + "\"\"\"", createSource(//
        "main() {",
        "  var a = \"\"\"",
        "000",
        "  111",
        "\"\"\"!;",
        "}"));
  }

  public void test_smartPaste_multiLineString_singleQuote() throws Exception {
    assertSmartPaste(createSource(//
        "main() {",
        "  var a = !;",
        "}"), "'''" + EOL + "000" + EOL + "  111" + EOL + "'''", createSource(//
        "main() {",
        "  var a = '''",
        "000",
        "  111",
        "'''!;",
        "}"));
  }

  public void test_smartPaste_preventNegativeIndent() throws Exception {
    assertSmartPaste(createSource(//
        "main() {",
        "!}"), createSource(//
        "    var a;",
        "var b;"), createSource(//
        "main() {",
        "  var a;",
        "var b;",
        "!}"));
  }

  public void test_useTabs() throws Exception {
    // We have 1000 and one listener for preferences that expect that we run them in UI.
    // Just make them happy...
    Display.getDefault().syncExec(new Runnable() {
      @Override
      public void run() {
        IPreferenceStore preferenceStore = DartToolsPlugin.getDefault().getPreferenceStore();
        try {
          preferenceStore.setValue(USE_SPACES, false);
          preferenceStore.setValue(TAB_CHAR, "tab");
          assertSmartInsertAfterNewLine(createSource(//
              "main() {!",
              "}"), createSource(//
              "main() {",
              "\t!",
              "}"));
        } finally {
          preferenceStore.setToDefault(USE_SPACES);
          preferenceStore.setToDefault(TAB_CHAR);
        }
      }
    });
  }
}
