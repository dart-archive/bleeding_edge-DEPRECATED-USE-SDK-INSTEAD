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

import com.google.dart.tools.ui.internal.text.functions.DartHeuristicScanner;
import com.google.dart.tools.ui.text.DartPartitions;
import com.google.dart.tools.ui.text.DartTextTools;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;

public class DartUiTest extends TestCase {

  /**
   * Changing this source requires changing all the constants used to test the scanner, so don't do
   * it. Just add another bit of code.
   */
  private static final String source = "" // comments improve formatting...
      + "class X {\n" //
      + "  int ex() {\n" //
      + "    return 3;\n" //
      + "  }\n" //
      + "}\n" //
      + "class Y {\n" //
      + "  int ey() {\n" //
      + "  String test = 'some; string';\n" //
      + "    return 4;\n" //
      + "  }\n" //
      + "  String s = \"\"\"\n" //
      + "multi-line\n" //
      + "string\"\"\";\n" //
      + " String string = ''' another mutli\n" //
      + " line string ''';\n" + "}\n"; //
  private static final int BLOCK_START = 21;
  private static final int BLOCK_LENGTH = 19;
  private static final int X_BLOCK_BEGIN = 8;
  private static final int X_BLOCK_END = 41;
  private static final int TEXT_STMT_END = 96;

  public void testHeuristicScanner() {
    Document doc = new Document(source);
    DartTextTools tools = DartToolsPlugin.getDefault().getDartTextTools();
    IDocumentPartitioner part = tools.createDocumentPartitioner();
    doc.setDocumentPartitioner(DartPartitions.DART_PARTITIONING, part);
    part.connect(doc);
    DartHeuristicScanner scanner = new DartHeuristicScanner(doc);
    int n = scanner.findClosingPeer(X_BLOCK_BEGIN + 2, '{', '}');
    assertTrue(n == X_BLOCK_END);
    IRegion region = scanner.findSurroundingBlock(BLOCK_START + 3);
    assertTrue(region.getOffset() == BLOCK_START);
    assertTrue(region.getLength() == BLOCK_LENGTH);
    n = scanner.scanForward(source.indexOf("test"), source.length(), ';');
    // n should be at end of stmt, not within string
    assertTrue(n == TEXT_STMT_END);
  }

  public void testPartitioner() {
    Document doc = new Document(source);
    DartTextTools tools = DartToolsPlugin.getDefault().getDartTextTools();
    IDocumentPartitioner part = tools.createDocumentPartitioner();
    doc.setDocumentPartitioner(DartPartitions.DART_PARTITIONING, part);
    part.connect(doc);
    ITypedRegion[] parts = part.computePartitioning(0, source.length());

//    StringBuffer buffer = new StringBuffer();
//    for (int i = 0; i < parts.length; i++) {
//      try {
//        buffer.append("Partition type: " + parts[i].getType() + ", offset: " + parts[i].getOffset()
//            + ", length: " + parts[i].getLength());
//        buffer.append("\n");
//        buffer.append("Text:\n");
//        buffer.append(doc.get(parts[i].getOffset(), parts[i].getLength()));
//        buffer.append("\n---------------------------\n\n\n");
//      } catch (BadLocationException e) {
//        e.printStackTrace();
//      }
//    }
//    System.out.print(buffer);

    assertTrue(parts.length == 7);
    assertTrue(parts[0].getType().equals("__dftl_partition_content_type")
        && parts[0].getOffset() == 0 && parts[0].getLength() == 82);
    assertTrue(parts[1].getType().equals(DartPartitions.DART_STRING) && parts[1].getOffset() == 82
        && parts[1].getLength() == 14);
    assertTrue(parts[2].getType().equals("__dftl_partition_content_type")
        && parts[2].getOffset() == 96 && parts[2].getLength() == 33);
    assertTrue(parts[3].getType().equals(DartPartitions.DART_MULTI_LINE_STRING)
        && parts[3].getOffset() == 129 && parts[3].getLength() == 24);
    assertTrue(parts[4].getType().equals("__dftl_partition_content_type")
        && parts[4].getOffset() == 153 && parts[4].getLength() == 19);
    assertTrue(parts[5].getType().equals(DartPartitions.DART_MULTI_LINE_STRING)
        && parts[5].getOffset() == 172 && parts[5].getLength() == 34);
    assertTrue(parts[6].getType().equals("__dftl_partition_content_type")
        && parts[6].getOffset() == 206 && parts[6].getLength() == 4);
  }
}
