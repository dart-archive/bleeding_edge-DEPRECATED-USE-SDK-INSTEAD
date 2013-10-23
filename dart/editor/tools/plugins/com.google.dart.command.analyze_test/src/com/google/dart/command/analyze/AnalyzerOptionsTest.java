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
package com.google.dart.command.analyze;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class AnalyzerOptionsTest extends TestCase {

  public void test_default() throws Exception {
    AnalyzerOptions options = AnalyzerOptions.createFromArgs(new String[] {});
    assertFalse(options.getMachineFormat());
    assertNull("foo", options.getPackageRootPath());
    assertFalse(options.getShowPackageWarnings());
  }

  public void test_getMachineFormat() {
    AnalyzerOptions options = AnalyzerOptions.createFromArgs(new String[] {"--format=machine"});
    assertTrue(options.getMachineFormat());
  }

  public void test_getPackageRootPath_long() {
    AnalyzerOptions options = AnalyzerOptions.createFromArgs(new String[] {"--package-root", "foo"});
    assertEquals("foo", options.getPackageRootPath().getPath());
  }

  public void test_getPackageRootPath_short() {
    AnalyzerOptions options = AnalyzerOptions.createFromArgs(new String[] {"-p", "foo"});
    assertEquals("foo", options.getPackageRootPath().getPath());
  }

  public void test_getShowPackageWarnings() {
    AnalyzerOptions options = AnalyzerOptions.createFromArgs(new String[] {"--package-warnings"});
    assertTrue(options.getShowPackageWarnings());
  }

  public void test_getShowSdkWarnings() {
    AnalyzerOptions options = AnalyzerOptions.createFromArgs(new String[] {"--warnings"});
    assertTrue(options.getShowSdkWarnings());
  }

  public void test_undocumentedFlags() throws Exception {
    ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
    PrintStream writer = new PrintStream(bytesOut);
    AnalyzerOptions.printUsage(writer);
    String text = new String(bytesOut.toByteArray());
    System.out.println(text);
    assertTrue(!text.contains("--dart-sdk"));
  }
}
