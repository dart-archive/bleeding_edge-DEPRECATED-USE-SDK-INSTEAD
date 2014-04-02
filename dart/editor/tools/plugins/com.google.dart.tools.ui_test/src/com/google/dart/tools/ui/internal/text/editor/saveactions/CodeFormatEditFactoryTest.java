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
package com.google.dart.tools.ui.internal.text.editor.saveactions;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MultiTextEdit;

public class CodeFormatEditFactoryTest extends TestCase {

  static final String EOL = System.getProperty("line.separator");

  public void test_removeTrailingWhitespace_none() throws Exception {
    String original = "class NoTrailingWhitespace {" + EOL //
        + "  void foo() { }" + EOL //
        + "}";
    IDocument doc = new Document(original);
    MultiTextEdit edit = CodeFormatEditFactory.removeTrailingWhitespace(doc);
    assertEquals(0, edit.getChildrenSize());
    edit.apply(doc);
    assertEquals(original, doc.get());
  }

  public void test_removeTrailingWhitespace_trailingBlankLine() throws Exception {
    String original = "class NoTrailingWhitespace {" + EOL //
        + "  void foo() { }" + EOL //
        + "}" + EOL;
    IDocument doc = new Document(original);
    MultiTextEdit edit = CodeFormatEditFactory.removeTrailingWhitespace(doc);
    assertEquals(0, edit.getChildrenSize());
    edit.apply(doc);
    assertEquals(original, doc.get());
  }

  public void test_removeTrailingWhitespace_trailingBlankLine_CR_only() throws Exception {
    String original = "class NoTrailingWhitespace {" + EOL //
        + "  void foo() { }" + EOL //
        + "}\r";
    IDocument doc = new Document(original);
    MultiTextEdit edit = CodeFormatEditFactory.removeTrailingWhitespace(doc);
    assertEquals(0, edit.getChildrenSize());
    edit.apply(doc);
    assertEquals(original, doc.get());
  }

  public void test_removeTrailingWhitespace_trailingBlankLine_NL_only() throws Exception {
    String original = "class NoTrailingWhitespace {\n" //
        + "  void foo() { }" + EOL //
        + "}\n";
    IDocument doc = new Document(original);
    MultiTextEdit edit = CodeFormatEditFactory.removeTrailingWhitespace(doc);
    assertEquals(0, edit.getChildrenSize());
    edit.apply(doc);
    assertEquals(original, doc.get());
  }

  public void test_removeTrailingWhitespace_trailingWhitespace() throws Exception {
    String original = "class NoTrailingWhitespace {" + EOL //
        + "  void foo() { }   " + EOL //
        + "}   " + EOL //
        + "  " + EOL;
    IDocument doc = new Document(original);
    MultiTextEdit edit = CodeFormatEditFactory.removeTrailingWhitespace(doc);
    assertEquals(3, edit.getChildrenSize());
    edit.apply(doc);
    String expected = "class NoTrailingWhitespace {" + EOL //
        + "  void foo() { }" + EOL //
        + "}" + EOL //
        + EOL;
    assertEquals(expected, doc.get());
  }
}
