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

import com.google.dart.compiler.PackageLibraryManager;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.LibraryUnit;
import com.google.dart.tools.core.AbstractDartCoreTest;
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;
import com.google.dart.tools.core.test.util.FileUtilities;
import com.google.dart.tools.core.test.util.TestUtilities;

import java.io.File;

public class ContextTest extends AbstractDartCoreTest {

  private static final long FIVE_MINUTES_MS = 300000;

  private static File tempDir;
  private static File libraryFile;
  private static File dartFile;

  /**
   * Called once prior to executing the first test in this class
   */
  public static void setUpOnce() throws Exception {
    tempDir = TestUtilities.createTempDirectory();
    TestUtilities.copyPluginRelativeContent("Money", tempDir);
    libraryFile = new File(tempDir, "money.dart");
    assertTrue(libraryFile.exists());
    dartFile = new File(tempDir, "simple_money.dart");
    assertTrue(dartFile.exists());
  }

  /**
   * Called once after executing the last test in this class
   */
  public static void tearDownOnce() {
    FileUtilities.delete(tempDir);
    tempDir = null;
  }

  private AnalysisServer server;
  private SavedContext context;
  private Listener listener;

  public void test_parse_library() throws Exception {
    ParseResult result1 = context.parse(libraryFile, libraryFile, FIVE_MINUTES_MS);
    DartUnit dartUnit = result1.getDartUnit();
    assertEquals("Money", dartUnit.getTopDeclarationNames().iterator().next());
    assertEquals(0, result1.getParseErrors().length);
    assertParsed(1, 0);

    listener.reset();
    ParseResult result2 = context.parse(libraryFile, libraryFile, FIVE_MINUTES_MS);
    assertSame(dartUnit, result2.getDartUnit());
    assertParsed(0, 0);
  }

  public void test_parse_libraryAndSourceDoNotExist() throws Exception {
    File doesNotExist = new File(tempDir, "doesNotExist.dart");
    assertFalse(doesNotExist.exists());
    File doesNotExist2 = new File(tempDir, "doesNotExist2.dart");
    assertFalse(doesNotExist2.exists());
    ParseResult result = context.parse(doesNotExist, doesNotExist2, FIVE_MINUTES_MS);
    assertEquals(0, result.getDartUnit().getTopDeclarationNames().size());
    assertEquals(1, result.getParseErrors().length);
    assertParsed(2, 2);
  }

  public void test_parse_libraryDoesNotExist() throws Exception {
    File doesNotExist = new File(tempDir, "doesNotExist.dart");
    assertFalse(doesNotExist.exists());
    ParseResult result = context.parse(doesNotExist, doesNotExist, FIVE_MINUTES_MS);
    assertEquals(0, result.getDartUnit().getTopDeclarationNames().size());
    assertEquals(1, result.getParseErrors().length);
    assertParsed(1, 1);
  }

  public void test_parse_source() throws Exception {
    ParseResult result1 = context.parse(libraryFile, dartFile, FIVE_MINUTES_MS);
    DartUnit dartUnit = result1.getDartUnit();
    assertEquals("SimpleMoney", dartUnit.getTopDeclarationNames().iterator().next());
    assertParsed(2, 0);

    listener.reset();
    ParseResult result2 = context.parse(libraryFile, dartFile, FIVE_MINUTES_MS);
    assertSame(dartUnit, result2.getDartUnit());
    assertEquals(0, result2.getParseErrors().length);
    assertParsed(0, 0);

    listener.reset();
    ParseResult result3 = context.parse(libraryFile, libraryFile, FIVE_MINUTES_MS);
    dartUnit = result3.getDartUnit();
    assertEquals("Money", dartUnit.getTopDeclarationNames().iterator().next());
    assertEquals(0, result3.getParseErrors().length);
    assertParsed(0, 0);
  }

  public void test_parse_sourceDoesNotExist() throws Exception {
    File doesNotExist = new File(tempDir, "doesNotExist.dart");
    assertFalse(doesNotExist.exists());
    ParseResult result = context.parse(libraryFile, doesNotExist, FIVE_MINUTES_MS);
    DartUnit dartUnit = result.getDartUnit();
    assertEquals(0, dartUnit.getTopDeclarationNames().size());
    assertParsed(2, 1);
  }

  public void test_resolve() throws Exception {
    LibraryUnit libraryUnit = context.resolve(libraryFile, FIVE_MINUTES_MS);
    assertEquals("Money", libraryUnit.getName());
    assertTrue(listener.getParsedCount() > 10);
    listener.assertResolved(libraryFile);
    listener.assertNoErrors();
    listener.assertNoDuplicates();
    listener.assertNoDiscards();
    listener.reset();
    libraryUnit = context.resolve(libraryFile, FIVE_MINUTES_MS);
    assertEquals("Money", libraryUnit.getName());
    listener.assertParsedCount(0);
    listener.assertResolvedCount(0);
    listener.assertNoErrors();
    listener.assertNoDuplicates();
    listener.assertNoDiscards();
  }

  public void test_resolve_doesNotExist() throws Exception {
    File doesNotExist = new File(tempDir, "doesNotExist.dart");
    assertFalse(doesNotExist.exists());
    LibraryUnit libraryUnit = context.resolve(doesNotExist, FIVE_MINUTES_MS);
    assertNotNull(libraryUnit);
    listener.assertParsed(doesNotExist, doesNotExist);
    listener.assertResolved(doesNotExist);
    assertTrue(listener.getErrorCount() > 0);
    listener.assertNoDuplicates();
    listener.assertNoDiscards();
  }

  @Override
  protected void setUp() throws Exception {
    PackageLibraryManager libraryManager = PackageLibraryManagerProvider.getAnyLibraryManager();
    server = new AnalysisServer(libraryManager);
    context = server.getSavedContext();
    listener = new Listener(server);
    server.start();
  }

  @Override
  protected void tearDown() throws Exception {
    server.stop();
  }

  private void assertParsed(int expectedParseCount, int expectedErrorCount) {
    listener.assertParsedCount(expectedParseCount);
    listener.assertResolvedCount(0);
    listener.assertErrorCount(expectedErrorCount);
    listener.assertNoDuplicates();
    listener.assertNoDiscards();
  }
}
