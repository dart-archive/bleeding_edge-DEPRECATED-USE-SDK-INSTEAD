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
package com.google.dart.tools.ui;

import com.google.common.base.Joiner;
import com.google.dart.tools.ui.text.DartPartitions;
import com.google.dart.tools.ui.text.DartTextTools;

import junit.framework.TestCase;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITypedRegion;

public class PartitionTest extends TestCase {
  public void test_commentAfterBlock() {
    assertPartitionCount(3, Joiner.on("\n").join(//
        "class X {", //
        "  void m(v) {", //
        "    if (v < 0) {", //
        "      return;", //
        "    }", //
        "    // Comment", //
        "  }", //
        "}"));
  }

  public void test_multilineString() {
    assertPartitionCount(3, Joiner.on("\n").join(//
        "class X {", //
        "  var s='''test''';", //
        "}"));
  }

  public void test_partial() {
    String source = Joiner.on("\n").join(//
        "class TestComment {", //
        "  TestComment() {", //
        "  }", //
        "} // TestComment" //
    );
    int expectedPartitionCount = 2;
    Document doc = new Document(source);
    DartTextTools tools = DartToolsPlugin.getDefault().getDartTextTools();
    IDocumentPartitioner part = tools.createDocumentPartitioner();
    doc.setDocumentPartitioner(DartPartitions.DART_PARTITIONING, part);
    part.connect(doc);
    ITypedRegion[] parts = part.computePartitioning(0, source.length());
    assertEquals(expectedPartitionCount, parts.length);
    String typeA = part.getContentType(52);
    try {
      doc.replace(41, 0, " ");
    } catch (BadLocationException exception) {
      // ignore it
    }
    String typeB = part.getContentType(53);
    assertEquals(typeA, typeB);
  }

  private void assertPartitionCount(int expectedPartitionCount, String source) {
    Document doc = new Document(source);
    DartTextTools tools = DartToolsPlugin.getDefault().getDartTextTools();
    IDocumentPartitioner part = tools.createDocumentPartitioner();
    doc.setDocumentPartitioner(DartPartitions.DART_PARTITIONING, part);
    part.connect(doc);
    ITypedRegion[] parts = part.computePartitioning(0, source.length());
    assertEquals(expectedPartitionCount, parts.length);
  }
}
