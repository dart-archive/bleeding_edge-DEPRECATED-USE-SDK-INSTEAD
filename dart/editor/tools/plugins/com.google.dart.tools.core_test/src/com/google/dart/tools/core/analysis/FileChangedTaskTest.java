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
package com.google.dart.tools.core.analysis;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.AbstractDartCoreTest;
import com.google.dart.tools.core.test.util.FileUtilities;
import com.google.dart.tools.core.test.util.TestUtilities;

import java.io.File;

public class FileChangedTaskTest extends AbstractDartCoreTest {

  private static final long FIVE_MINUTES_MS = 300000;

  private static File tempDir;
  private static File libraryFile;
  private static File dartFile;

  /**
   * Called once prior to executing the first test in this class
   */
  public static void setUpOnce() throws Exception {
    tempDir = TestUtilities.createTempDirectory();
    File moneyDir = new File(tempDir, "money");
    TestUtilities.copyPluginRelativeContent("Money", moneyDir);
    libraryFile = new File(moneyDir, "money.dart");
    assertTrue(libraryFile.exists());
    dartFile = new File(moneyDir, "simple_money.dart");
    assertTrue(dartFile.exists());
  }

  /**
   * Called once after executing the last test in this class
   */
  public static void tearDownOnce() {
    FileUtilities.delete(tempDir);
    tempDir = null;
  }

  private AnalysisServerAdapter server;
  private Context savedContext;

  public void test_changed() {
    DartUnit dartUnit1 = savedContext.parse(libraryFile, libraryFile, FIVE_MINUTES_MS);
    assertNotNull(dartUnit1);
    DartUnit dartUnit2 = savedContext.parse(libraryFile, libraryFile, FIVE_MINUTES_MS);
    assertSame(dartUnit1, dartUnit2);

    server.changed(libraryFile);
    dartUnit2 = savedContext.parse(libraryFile, libraryFile, FIVE_MINUTES_MS);
    assertSame(dartUnit1, dartUnit2);

    libraryFile.setLastModified(System.currentTimeMillis() + 1000);
    server.changed(libraryFile);
    dartUnit2 = savedContext.parse(libraryFile, libraryFile, FIVE_MINUTES_MS);
    assertNotSame(dartUnit1, dartUnit2);
  }

  @Override
  protected void setUp() throws Exception {
    server = new AnalysisServerAdapter();
    savedContext = server.getSavedContext();
    server.start();
  }

  @Override
  protected void tearDown() throws Exception {
    server.stop();
  }
}
