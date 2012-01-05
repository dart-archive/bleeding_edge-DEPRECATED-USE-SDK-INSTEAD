/*
 * Copyright 2011, the Dart project authors.
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
package com.google.dart.tools.ui.internal.text.functions;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.text.DartPartitions;
import com.google.dart.tools.ui.text.DartTextTools;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITypedRegion;

public class FastDartPartitionScannerTest extends TestCase implements DartPartitions {
  /**
   * The default partition content type, used to represent "plain" code.
   */
  private static final String DEFAULT_TYPE = "__dftl_partition_content_type";

  public void test_FastDartPartitionScanner_blockComment() {
    // class X { /* comment */ } 
    assertPartitions( //
        "class X {\n", DEFAULT_TYPE, //
        "/* comment */", DART_MULTI_LINE_COMMENT, //
        "\n}\n", DEFAULT_TYPE //
    );
  }

  public void test_FastDartPartitionScanner_blockComment_inInterpolation() {
    // class X { var s="xxx ${yyy /* comment */ + zzz} xxx"; }
    assertPartitions( //
        "class X {\nvar s=", DEFAULT_TYPE, //
        "\"xxx ", DART_STRING, //
        "${yyy ", DEFAULT_TYPE, //
        "/* comment */", DART_MULTI_LINE_COMMENT, //
        " + zzz}", DEFAULT_TYPE, //
        " xxx\"", DART_STRING, //
        ";\n}\n", DEFAULT_TYPE //
    );
  }

  public void test_FastDartPartitionScanner_blockComment_nested() {
    // class X { /* a/one /* comment /* nested */ inside */ another */ }
    assertPartitions( //
        "class X {\n", DEFAULT_TYPE, //
        "/* a/one /* comment /* nested */ inside */ another */", DART_MULTI_LINE_COMMENT, //
        "\n}\n", DEFAULT_TYPE //
    );
  }

  public void test_FastDartPartitionScanner_blockComment_nested_unclosed() {
    // class X { /* a/one /* comment /* nested */ inside */ }
    assertPartitions( //
        "class X {\n", DEFAULT_TYPE, //
        "/* a/one /* comment /* nested */ inside */\n}\n", DART_MULTI_LINE_COMMENT //
    );
  }

  public void test_FastDartPartitionScanner_blockComment_unclosed() {
    // class X { /* comment
    assertPartitions( //
        "class X {\n", DEFAULT_TYPE, //
        "/* comment", DART_MULTI_LINE_COMMENT //
    );
  }

  public void test_FastDartPartitionScanner_docComment() {
    // class X { /** comment */ var s=null; } 
    assertPartitions( //
        "class X {\n", DEFAULT_TYPE, //
        "/** comment */", DART_DOC, //
        "\nvar s = null;}\n", DEFAULT_TYPE //
    );
  }

  public void test_FastDartPartitionScanner_docComment_unfinishedToFinished() {
    assertPartitionsAfter("/** *\nvoid main() {}", //
        "/** */", DART_DOC, //
        "void main() {}", DEFAULT_TYPE //
    );
  }

  public void test_FastDartPartitionScanner_endOfLineComment() {
    // class X { // comment } 
    assertPartitions( //
        "class X {\n", DEFAULT_TYPE, //
        "// comment\n", DART_SINGLE_LINE_COMMENT, //
        "}\n", DEFAULT_TYPE //
    );
  }

  public void test_FastDartPartitionScanner_endOfLineComment_inInterpolation() {
    // class X { var s="xxx ${yyy // comment + zzz} xxx"; }
    assertPartitions( //
        "class X {\nvar s=", DEFAULT_TYPE, //
        "\"xxx ", DART_STRING, //
        "${yyy ", DEFAULT_TYPE, //
        "// comment\n", DART_SINGLE_LINE_COMMENT, //
        " + zzz}", DEFAULT_TYPE, //
        " xxx\"", DART_STRING, //
        ";\n}\n", DEFAULT_TYPE //
    );
  }

  public void test_FastDartPartitionScanner_multilineString_double() {
    // class X { var s="""test"""; } 
    assertPartitions( //
        "class X {\nvar s=", DEFAULT_TYPE, //
        "\"\"\"test\"\"\"", DART_MULTI_LINE_STRING, //
        ";\n}\n", DEFAULT_TYPE //
    );
  }

  public void test_FastDartPartitionScanner_multilineString_single() {
    // class X { var s='''test'''; } 
    assertPartitions( //
        "class X {\nvar s=", DEFAULT_TYPE, //
        "'''test'''", DART_MULTI_LINE_STRING, //
        ";\n}\n", DEFAULT_TYPE //
    );
  }

  public void test_FastDartPartitionScanner_nestedBraces() {
    // class X { var s="xxx ${f((v) {return v;})} xxx"; } 
    assertPartitions( //
        "class X {\nvar s=", DEFAULT_TYPE, //
        "\"xxx ", DART_STRING, //
        "${f((v) {return v;})}", DEFAULT_TYPE, //
        " xxx\"", DART_STRING, //
        ";\n}\n", DEFAULT_TYPE //
    );
  }

  public void test_FastDartPartitionScanner_nestedStrings() {
    // class X { var s="xxx ${yyy 'zzz' """aaa ${bbb '''ccc''' bbb} aaa""" yyy} xxx"; } 
    assertPartitions( //
        "class X {\nvar s=", DEFAULT_TYPE, //
        "\"xxx ", DART_STRING, //
        "${yyy ", DEFAULT_TYPE, //
        "'zzz'", DART_STRING, //
        " ", DEFAULT_TYPE, //
        "\"\"\"aaa ", DART_MULTI_LINE_STRING, //
        "${bbb ", DEFAULT_TYPE, //
        "'''ccc'''", DART_MULTI_LINE_STRING, //
        " bbb}", DEFAULT_TYPE, //
        " aaa\"\"\"", DART_MULTI_LINE_STRING, //
        " yyy}", DEFAULT_TYPE, //
        " xxx\"", DART_STRING, //
        ";\n}\n", DEFAULT_TYPE //
    );
  }

  public void test_FastDartPartitionScanner_nestedStrings_with_comments() {
    // class X { var s="xxx ${yyy 'zzz' /* comment */ """aaa ${bbb '''ccc'''/* comment/*nested*/*/ bbb} aaa""" yyy} xxx"; } 
    assertPartitions( //
        "class X {\nvar s=", DEFAULT_TYPE, //
        "\"xxx ", DART_STRING, //
        "${yyy ", DEFAULT_TYPE, //
        "'zzz'", DART_STRING, //
        " ", DEFAULT_TYPE, //
        "/* comment */", DART_MULTI_LINE_COMMENT, //
        " ", DEFAULT_TYPE, //
        "\"\"\"aaa ", DART_MULTI_LINE_STRING, //
        "${bbb ", DEFAULT_TYPE, //
        "'''ccc'''", DART_MULTI_LINE_STRING, //
        "/* comment/*nested*/*/", DART_MULTI_LINE_COMMENT, //
        " bbb}", DEFAULT_TYPE, //
        " aaa\"\"\"", DART_MULTI_LINE_STRING, //
        " yyy}", DEFAULT_TYPE, //
        " xxx\"", DART_STRING, //
        ";\n}\n", DEFAULT_TYPE //
    );
  }

  public void test_FastDartPartitionScanner_normalString_atEOF() {
    // class X { var s='
    assertPartitions( //
        "class X {\nvar s=", DEFAULT_TYPE, //
        "'", DART_STRING //
    );
  }

  public void test_FastDartPartitionScanner_normalString_backToBackInterpolations() {
    // class X { return newName(id, '${prefix}${prefixes[prefix]++}'); } 
    assertPartitions( //
        "class X {\nreturn newName(id, ", DEFAULT_TYPE, //
        "'", DART_STRING, //
        "${prefix}${prefixes[prefix]++}", DEFAULT_TYPE, //
        "'", DART_STRING, //
        ");\n}\n", DEFAULT_TYPE //
    );
  }

  public void test_FastDartPartitionScanner_normalString_double() {
    // class X { var s="test"; } 
    assertPartitions( //
        "class X {\nvar s=", DEFAULT_TYPE, //
        "\"test\"", DART_STRING, //
        ";\n}\n", DEFAULT_TYPE //
    );
  }

  public void test_FastDartPartitionScanner_normalString_single() {
    // class X { var s='test'; } 
    assertPartitions( //
        "class X {\nvar s=", DEFAULT_TYPE, //
        "'test'", DART_STRING, //
        ";\n}\n", DEFAULT_TYPE //
    );
  }

  public void test_FastDartPartitionScanner_normalString_unclosed() {
    // class X { var s='xxxyyy; } 
    assertPartitions( //
        "class X {\nvar s=", DEFAULT_TYPE, //
        "'xxxyyy;", DART_STRING, //
        "\n}\n", DEFAULT_TYPE //
    );
  }

  public void test_FastDartPartitionScanner_normalString_withEscapes() {
    // class X { var s=' \$$foo '; } 
    assertPartitions( //
        "class X {\nvar s=", DEFAULT_TYPE, //
        "' \\$", DART_STRING, //
        "$foo", DEFAULT_TYPE, //
        " '", DART_STRING, //
        ";\n}\n", DEFAULT_TYPE //
    );
  }

  public void test_FastDartPartitionScanner_rawString_double() {
    // class X { var s=@"test"; } 
    assertPartitions( //
        "class X {\nvar s=", DEFAULT_TYPE, //
        "@\"test\"", DART_STRING, //
        ";\n}\n", DEFAULT_TYPE //
    );
  }

  public void test_FastDartPartitionScanner_rawString_single() {
    // class X { var s=@'test'; } 
    assertPartitions( //
        "class X {\nvar s=", DEFAULT_TYPE, //
        "@'test'", DART_STRING, //
        ";\n}\n", DEFAULT_TYPE //
    );
  }

  /**
   * Assert that the given array of regions contains the expected number of elements.
   * 
   * @param expectedCount the expected number of elements
   * @param regions the array being tested
   */
  private void assertCount(int expectedCount, ITypedRegion[] regions) {
    assertEquals("wrong number of partitions:", expectedCount, regions.length);
  }

  /**
   * Given an array containing pairs of strings of the form
   * 
   * <pre>
   *   [codeSnippet1, partitionType1, codeSnippet2, partitionType2, ..., codeSnippetN, partitionTypeN]
   * </pre>
   * 
   * where each code snippet should be a single partition of the given type, compose the snippets
   * into a single source string, partition it, and compare the results to see whether they have the
   * expected type and position.
   * 
   * @param strings an array containing the (snippet, partition type) pairs
   */
  private void assertPartitions(String... strings) {
    int stringCount = strings.length;
    assertTrue(stringCount % 2 == 0);
    int expectedCount = stringCount / 2;
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < expectedCount; i++) {
      builder.append(strings[i * 2]);
    }
    ITypedRegion[] regions = partition(builder.toString());
    assertCount(expectedCount, regions);
    int start = 0;
    for (int i = 0; i < expectedCount; i++) {
      int length = strings[i * 2].length();
      assertRegion(regions[i], strings[(i * 2) + 1], start, length);
      start += length;
    }
  }

  /**
   * Given an array containing pairs of strings of the form
   * 
   * <pre>
   *   [codeSnippet1, partitionType1, codeSnippet2, partitionType2, ..., codeSnippetN, partitionTypeN]
   * </pre>
   * 
   * where each code snippet should be a single partition of the given type, compose the snippets
   * into a single source string, partition it, and compare the results to see whether they have the
   * expected type and position.
   * 
   * @param previousContent the content of the document prior to the composed source being tested
   * @param strings an array containing the (snippet, partition type) pairs
   */
  private void assertPartitionsAfter(String previousContent, String... strings) {
    Document doc = new Document(previousContent);
    DartTextTools tools = DartToolsPlugin.getDefault().getJavaTextTools();
    IDocumentPartitioner part = tools.createDocumentPartitioner();
    doc.setDocumentPartitioner(DartPartitions.DART_PARTITIONING, part);
    part.connect(doc);
    part.computePartitioning(0, previousContent.length());

    int stringCount = strings.length;
    assertTrue(stringCount % 2 == 0);
    int expectedCount = stringCount / 2;
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < expectedCount; i++) {
      builder.append(strings[i * 2]);
    }
    String source = builder.toString();
    doc.set(source);
    ITypedRegion[] regions = part.computePartitioning(0, source.length());
    assertCount(expectedCount, regions);
    int start = 0;
    for (int i = 0; i < expectedCount; i++) {
      int length = strings[i * 2].length();
      assertRegion(regions[i], strings[(i * 2) + 1], start, length);
      start += length;
    }
  }

  /**
   * Assert that the given region has the given type. If the offset and/or length is positive,
   * additionally assert that it has the given offset and length.
   * 
   * @param region the region being tested
   * @param type the expected type of the region
   * @param offset the expected offset of the region, or -1 if the offset is to be ignored
   * @param length the expected length of the region, or -1 if the length is to be ignored
   */
  private void assertRegion(ITypedRegion region, String type, int offset, int length) {
    assertEquals("wrong type:", type, region.getType());
    if (offset >= 0) {
      assertEquals("wrong offset:", offset, region.getOffset());
    }
    if (length >= 0) {
      assertEquals("wrong length:", length, region.getLength());
    }
  }

  /**
   * Create partitions for the given source string.
   * 
   * @param source the source string to be partitioned
   * @return the partitions that were created
   */
  private ITypedRegion[] partition(String source) {
    Document doc = new Document(source);
    DartTextTools tools = DartToolsPlugin.getDefault().getJavaTextTools();
    IDocumentPartitioner part = tools.createDocumentPartitioner();
    doc.setDocumentPartitioner(DartPartitions.DART_PARTITIONING, part);
    part.connect(doc);
    return part.computePartitioning(0, source.length());
  }
}
