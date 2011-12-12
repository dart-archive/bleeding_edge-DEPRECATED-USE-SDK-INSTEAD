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
  public void test_FastDartPartitionScanner_blockComment() {
    ITypedRegion[] regions = partition("class X {\n/* comment */\n}\n");
    assertCount(3, regions);
    assertRegion(regions[0], "__dftl_partition_content_type", -1, -1);
    assertRegion(regions[1], DART_MULTI_LINE_COMMENT, 10, 13);
    assertRegion(regions[2], "__dftl_partition_content_type", -1, -1);
  }

  public void test_FastDartPartitionScanner_docComment() {
    ITypedRegion[] regions = partition("class X {\n/** comment */\nvar s = null;}\n");
    assertCount(3, regions);
    assertRegion(regions[0], "__dftl_partition_content_type", -1, -1);
    assertRegion(regions[1], DART_DOC, 10, 14);
    assertRegion(regions[2], "__dftl_partition_content_type", -1, -1);
  }

  public void test_FastDartPartitionScanner_endOfLineComment() {
    ITypedRegion[] regions = partition("class X {\n// comment\n}\n");
    assertCount(3, regions);
    assertRegion(regions[0], "__dftl_partition_content_type", -1, -1);
    assertRegion(regions[1], DART_SINGLE_LINE_COMMENT, 10, 10);
    assertRegion(regions[2], "__dftl_partition_content_type", -1, -1);
  }

  public void test_FastDartPartitionScanner_multilineString_double() {
    ITypedRegion[] regions = partition("class X {\nvar s=\"\"\"test\"\"\";\n}\n");
    assertCount(3, regions);
    assertRegion(regions[0], "__dftl_partition_content_type", -1, -1);
    assertRegion(regions[1], DART_MULTI_LINE_STRING, 16, 10);
    assertRegion(regions[2], "__dftl_partition_content_type", -1, -1);
  }

  public void test_FastDartPartitionScanner_multilineString_single() {
    ITypedRegion[] regions = partition("class X {\nvar s='''test''';\n}\n");
    assertCount(3, regions);
    assertRegion(regions[0], "__dftl_partition_content_type", -1, -1);
    assertRegion(regions[1], DART_MULTI_LINE_STRING, 16, 10);
    assertRegion(regions[2], "__dftl_partition_content_type", -1, -1);
  }

  public void test_FastDartPartitionScanner_nestedBraces() {
    ITypedRegion[] regions = partition("class X {\nvar s=\"xxx ${f((v) {return v;})} xxx\";\n}\n");
    assertCount(5, regions);
    assertRegion(regions[0], "__dftl_partition_content_type", -1, -1);
    assertRegion(regions[1], DART_STRING, 16, 5);
    assertRegion(regions[2], "__dftl_partition_content_type", -1, -1);
    assertRegion(regions[3], DART_STRING, 42, 5);
    assertRegion(regions[4], "__dftl_partition_content_type", -1, -1);
  }

  public void test_FastDartPartitionScanner_nestedStrings() {
    ITypedRegion[] regions = partition("class X {\nvar s=\"xxx ${yyy 'zzz' \"\"\"aaa ${bbb '''ccc''' bbb} aaa\"\"\" yyy} xxx\";\n}\n");
    assertCount(13, regions);
    assertRegion(regions[0], "__dftl_partition_content_type", -1, -1);
    assertRegion(regions[1], DART_STRING, 16, 5);
    assertRegion(regions[2], "__dftl_partition_content_type", -1, -1);
    assertRegion(regions[3], DART_STRING, 27, 5);
    assertRegion(regions[4], "__dftl_partition_content_type", -1, -1);
    assertRegion(regions[5], DART_MULTI_LINE_STRING, 33, 7);
    assertRegion(regions[6], "__dftl_partition_content_type", -1, -1);
    assertRegion(regions[7], DART_MULTI_LINE_STRING, 46, 9);
    assertRegion(regions[8], "__dftl_partition_content_type", -1, -1);
    assertRegion(regions[9], DART_MULTI_LINE_STRING, 60, 7);
    assertRegion(regions[10], "__dftl_partition_content_type", -1, -1);
    assertRegion(regions[11], DART_STRING, 72, 5);
    assertRegion(regions[12], "__dftl_partition_content_type", -1, -1);
  }

  public void test_FastDartPartitionScanner_normalString_double() {
    ITypedRegion[] regions = partition("class X {\nvar s=\"test\";\n}\n");
    assertCount(3, regions);
    assertRegion(regions[0], "__dftl_partition_content_type", -1, -1);
    assertRegion(regions[1], DART_STRING, 16, 6);
    assertRegion(regions[2], "__dftl_partition_content_type", -1, -1);
  }

  public void test_FastDartPartitionScanner_normalString_single() {
    ITypedRegion[] regions = partition("class X {\nvar s='test';\n}\n");
    assertCount(3, regions);
    assertRegion(regions[0], "__dftl_partition_content_type", -1, -1);
    assertRegion(regions[1], DART_STRING, 16, 6);
    assertRegion(regions[2], "__dftl_partition_content_type", -1, -1);
  }

  public void test_FastDartPartitionScanner_normalString_unclosed() {
    ITypedRegion[] regions = partition("class X {\nvar s='xxxyyy;\n}\n");
    assertCount(3, regions);
    assertRegion(regions[0], "__dftl_partition_content_type", -1, -1);
    assertRegion(regions[1], DART_STRING, 16, 8);
    assertRegion(regions[2], "__dftl_partition_content_type", -1, -1);
  }

  public void test_FastDartPartitionScanner_rawString_double() {
    ITypedRegion[] regions = partition("class X {\nvar s=@\"test\";\n}\n");
    assertCount(3, regions);
    assertRegion(regions[0], "__dftl_partition_content_type", -1, -1);
    assertRegion(regions[1], DART_STRING, 16, 7);
    assertRegion(regions[2], "__dftl_partition_content_type", -1, -1);
  }

  public void test_FastDartPartitionScanner_rawString_single() {
    ITypedRegion[] regions = partition("class X {\nvar s=@'test';\n}\n");
    assertCount(3, regions);
    assertRegion(regions[0], "__dftl_partition_content_type", -1, -1);
    assertRegion(regions[1], DART_STRING, 16, 7);
    assertRegion(regions[2], "__dftl_partition_content_type", -1, -1);
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

  private ITypedRegion[] partition(String source) {
    Document doc = new Document(source);
    DartTextTools tools = DartToolsPlugin.getDefault().getJavaTextTools();
    IDocumentPartitioner part = tools.createDocumentPartitioner();
    doc.setDocumentPartitioner(DartPartitions.DART_PARTITIONING, part);
    part.connect(doc);
    return part.computePartitioning(0, source.length());
  }
}
