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
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

import static org.fest.assertions.Assertions.assertThat;

public class DartAutoIndentStrategyTest extends EngineTestCase {
  private static final String EOL = System.getProperty("line.separator", "\n");
  private static final String USE_SPACES = AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS;
  private static final String TAB_CHAR = DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR;

  private static void assertSmartInsertAfterNewLine(String initial, String expected) {
    int initialOffset = initial.indexOf('!');
    int expectedOffset = expected.indexOf('!');
    assertTrue("No cursor position in initial: " + initial, initialOffset != -1);
    assertTrue("No cursor position in expected: " + expected, expectedOffset != -1);
    initial = StringUtils.remove(initial, '!');
    expected = StringUtils.remove(expected, '!');
    // force "smart mode"
    DartAutoIndentStrategy strategy = new DartAutoIndentStrategy(
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
    command.text = EOL;
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

  public void test_smartIndentAfterNewLine_classBeforeMethod() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "class A {!main() {}"),
        createSource(//
            "class A {",
            "  !",
            "}main() {}"));
  }

  public void test_smartIndentAfterNewLine_method_hasClosed() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "main() {!}"),
        createSource(//
            "main() {",
            "  !",
            "}"));
  }

  public void test_smartIndentAfterNewLine_method_noClosed() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "main() {!"),
        createSource(//
            "main() {",
            "  !",
            "}"));
  }

  public void test_smartIndentAfterNewLine_wrapIntoBlock() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "main() {",
        "  if (true) {!print();",
        "}"), createSource(//
        "main() {",
        "  if (true) {",
        "    !print();",
        "  }",
        "}"));
  }

  public void test_useTabs() throws Exception {
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
}
