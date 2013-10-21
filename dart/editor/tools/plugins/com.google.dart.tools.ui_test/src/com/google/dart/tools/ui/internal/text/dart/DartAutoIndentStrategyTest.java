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

import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;
import com.google.dart.tools.ui.text.DartPartitions;

import junit.framework.TestCase;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;

import static org.fest.assertions.Assertions.assertThat;

public class DartAutoIndentStrategyTest extends TestCase {
  private static final String EOL = System.getProperty("line.separator", "\n");

  private static void assertSmartInsertAfterNewLine(String initial, String expected) {
    initial = StringUtils.replace(initial, "\n", EOL);
    expected = StringUtils.replace(expected, "\n", EOL);
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
    command.doit = true;
    command.offset = initialOffset;
    command.text = EOL;
    strategy.customizeDocumentCommand(document, command);
    // update document
    ReflectionUtils.invokeMethod(command, "execute(org.eclipse.jface.text.IDocument)", document);
    String actual = document.get();
    assertEquals(expected, actual);
    assertThat(command.caretOffset).isEqualTo(expectedOffset);
  }

  public void test_smartIndexAfterNewLine_block_closed_betweenBraces() throws Exception {
    assertSmartInsertAfterNewLine("main() {\n  {!}\n}", "main() {\n  {\n    !\n  }\n}");
  }

  public void test_smartIndexAfterNewLine_block_noClosed() throws Exception {
    assertSmartInsertAfterNewLine("main() {\n  {!\n}", "main() {\n  {\n    !\n  }\n}");
  }

  public void test_smartIndexAfterNewLine_class_noClosed() throws Exception {
    assertSmartInsertAfterNewLine("main() {!", "main() {\n  !\n}");
  }

  public void test_smartIndexAfterNewLine_classBeforeMethod() throws Exception {
    assertSmartInsertAfterNewLine("class A {!main() {}", "class A {\n  !\n}main() {}");
  }

  public void test_smartIndexAfterNewLine_method_hasClosed() throws Exception {
    assertSmartInsertAfterNewLine("main() {!}", "main() {\n  !\n}");
  }

  public void test_smartIndexAfterNewLine_method_noClosed() throws Exception {
    assertSmartInsertAfterNewLine("main() {!", "main() {\n  !\n}");
  }

  public void test_smartIndexAfterNewLine_wrapIntoBlock() throws Exception {
    assertSmartInsertAfterNewLine(
        "main() {\n  if (true) {!print();\n}",
        "main() {\n  if (true) {\n    !print();\n  }\n}");
  }
}
