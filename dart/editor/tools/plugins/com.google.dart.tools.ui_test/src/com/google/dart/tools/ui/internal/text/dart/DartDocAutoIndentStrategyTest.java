/*
 * Copyright (c) 2014, the Dart project authors.
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
import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.dartdoc.DartDocAutoIndentStrategy;
import com.google.dart.tools.ui.text.DartPartitions;
import com.google.dart.tools.ui.text.DartTextTools;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;

import static org.fest.assertions.Assertions.assertThat;

public class DartDocAutoIndentStrategyTest extends EngineTestCase {
  private static final String EOL = System.getProperty("line.separator", "\n");

  private static void assert_customizeDocumentCommand(String initial, String newText,
      String expected) {
    int initialOffset = initial.indexOf('!');
    int expectedOffset = expected.indexOf('!');
    assertTrue("No cursor position in initial: " + initial, initialOffset != -1);
    assertTrue("No cursor position in expected: " + expected, expectedOffset != -1);
    initial = StringUtils.remove(initial, '!');
    expected = StringUtils.remove(expected, '!');
    // prepare document
    IDocument document = new Document(initial);
    {
      DartTextTools tools = DartToolsPlugin.getDefault().getDartTextTools();
      tools.setupDartDocumentPartitioner(document, DartPartitions.DART_PARTITIONING);
    }
    // handle command
    DocumentCommand command = new DocumentCommand() {
    };
    command.caretOffset = -1;
    command.doit = true;
    command.offset = initialOffset;
    command.text = newText;
    // execute command
    DartDocAutoIndentStrategy strategy = new DartDocAutoIndentStrategy(
        DartPartitions.DART_PARTITIONING);
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

  public void test_afterStart_beforeEnd_hasEOL_beforeEOF() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "/**!*/",
        ""), createSource(//
        "/**",
        " * !",
        " */",
        ""));
  }

  public void test_afterStart_beforeEnd_noEOL_beforeEOF() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "/**!*/"),
        createSource(//
            "/**",
            " * !",
            " */"));
  }

  public void test_inEmptyLine() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "/**",
        "!",
        " */",
        ""), createSource(//
        "/**",
        "",
        " * !",
        " */",
        ""));
  }

  public void test_inMiddleLine() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "/**",
        " * ab!cd",
        " */",
        ""), createSource(//
        "/**",
        " * ab",
        " * !cd",
        " */",
        ""));
  }

  public void test_newComment_method() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "class A {",
        "  /**!",
        "  foo() {}",
        "}"), createSource(//
        "class A {",
        "  /**",
        "   * !",
        "   */",
        "  foo() {}",
        "}"));
  }

  public void test_newComment_topLevel() throws Exception {
    assertSmartInsertAfterNewLine(createSource(//
        "/**!",
        "main() {",
        "}"), createSource(//
        "/**",
        " * !",
        " */",
        "main() {",
        "}"));
  }
}
