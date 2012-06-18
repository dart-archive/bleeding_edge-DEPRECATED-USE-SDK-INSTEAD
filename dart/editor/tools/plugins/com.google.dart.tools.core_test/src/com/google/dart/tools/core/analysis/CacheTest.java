/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.core.analysis;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CacheTest extends TestCase {

  private StringWriter stringWriter;
  private CacheWriter cacheWriter;
  private StringReader stringReader;
  private CacheReader cacheReader;

  public void test_AnalysisCache_boolean() throws Exception {
    initWriter();
    cacheWriter.writeBoolean(true);
    cacheWriter.writeBoolean(false);
    initReader();
    assertTrue(cacheReader.readBoolean());
    assertFalse(cacheReader.readBoolean());
    assertTrue(stringReader.read() == -1);
  }

  public void test_AnalysisCache_empty() throws Exception {
    initWriter();
    initReader();
    assertTrue(stringReader.read() == -1);
  }

  public void test_AnalysisCache_filePaths() throws Exception {
    initWriter();
    ArrayList<File> expected = new ArrayList<File>();
    expected.add(new File("one"));
    expected.add(new File("two/three").getAbsoluteFile());
    String endTag = "my-end-tag";
    cacheWriter.writeFilePaths(expected, endTag);
    cacheWriter.writeString("another");
    initReader();
    ArrayList<File> actual = new ArrayList<File>();
    cacheReader.readFilePaths(actual, endTag);
    assertEquals(expected, actual);
    assertEquals("another", cacheReader.readString());
    assertTrue(stringReader.read() == -1);
  }

  public void test_AnalysisCache_string_backslash() throws Exception {
    writeAndReadStrings("\\hello\\");
  }

  public void test_AnalysisCache_string_backslash2() throws Exception {
    writeAndReadStrings("\\hel\\\nlo\\");
  }

  public void test_AnalysisCache_string_bang() throws Exception {
    writeAndReadStrings("!one");
  }

  public void test_AnalysisCache_string_bang2() throws Exception {
    writeAndReadStrings("!one!\r!two!");
  }

  public void test_AnalysisCache_string_cr() throws Exception {
    writeAndReadStrings("one\rtwo");
  }

  public void test_AnalysisCache_string_cr_newline() throws Exception {
    writeAndReadStrings("one\r\ntwo");
  }

  public void test_AnalysisCache_string_empty() throws Exception {
    writeAndReadStrings("");
  }

  public void test_AnalysisCache_string_multiple() throws Exception {
    writeAndReadStrings("one", "", "!one", "!one!\r!two!", "/comp\"plex\\", "four");
  }

  public void test_AnalysisCache_string_newline() throws Exception {
    writeAndReadStrings("one\ntwo");
  }

  public void test_AnalysisCache_string_simple() throws Exception {
    writeAndReadStrings("hello");
  }

  public void test_AnalysisCache_string_slash() throws Exception {
    writeAndReadStrings("/hello/");
  }

  public void test_AnalysisCache_stringFileMap() throws Exception {
    initWriter();
    Map<String, File> expected = new HashMap<String, File>();
    expected.put("one", new File("xOne"));
    expected.put("two", new File("something"));
    expected.put("three", new File("third"));
    String endTag = "my-end-tag";
    cacheWriter.writeStringFileMap(expected, endTag);
    cacheWriter.writeString("another");
    initReader();
    Map<String, File> actual = cacheReader.readStringFileMap(endTag);
    assertEquals(expected, actual);
    assertEquals("another", cacheReader.readString());
    assertTrue(stringReader.read() == -1);
  }

  public void test_AnalysisCache_stringSet() throws Exception {
    initWriter();
    Set<String> expected = new HashSet<String>();
    expected.add("one");
    expected.add("two");
    expected.add("three");
    String endTag = "my-end-tag";
    cacheWriter.writeStringSet(expected, endTag);
    cacheWriter.writeString("another");
    initReader();
    HashSet<String> actual = cacheReader.readStringSet(endTag);
    assertEquals(expected, actual);
    assertEquals("another", cacheReader.readString());
    assertTrue(stringReader.read() == -1);
  }

  private void initReader() throws IOException {
    stringReader = new StringReader(stringWriter.toString());
    cacheReader = new CacheReader(stringReader);
  }

  private void initWriter() {
    stringWriter = new StringWriter(200);
    cacheWriter = new CacheWriter(stringWriter);
  }

  private void writeAndReadStrings(String... strings) throws IOException {
    initWriter();
    for (String string : strings) {
      cacheWriter.writeString(string);
    }
    initReader();
    for (String string : strings) {
      assertEquals(string, cacheReader.readString());
    }
    assertTrue(stringReader.read() == -1);
  }
}
