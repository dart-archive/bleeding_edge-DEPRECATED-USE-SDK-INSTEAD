/*
 * Copyright 2011 Dart project authors.
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
package com.google.dart.tools.core.internal.builder;

import com.google.common.io.CharStreams;
import com.google.dart.compiler.util.DartSourceString;
import com.google.dart.tools.core.test.util.FileOperation;
import com.google.dart.tools.core.test.util.TestUtilities;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class CachingArtifactProviderTest extends TestCase {

  public void test_CachingArtifactProvider_clearCachedArtifacts() throws IOException {
    CachingArtifactProvider provider = newProvider();
    assertEquals(0, provider.getCacheSize());

    writeArtifact1(provider);
    assertEquals(1, provider.getCacheSize());
    readArtifact1(provider);

    provider.clearCachedArtifacts();
    assertEquals(0, provider.getCacheSize());
    assertNull(provider.getArtifactReader(getSource1(), "", "js"));
  }

  public void test_CachingArtifactProvider_remove1() throws Exception {
    CachingArtifactProvider provider = newProvider();
    writeArtifact1(provider);
    writeArtifact1a(provider);
    writeArtifact2(provider);

    provider.removeArtifactsFor(getSource1());
    assertEquals(1, provider.getCacheSize());
    readArtifact2(provider);
  }

  public void test_CachingArtifactProvider_remove2() throws Exception {
    CachingArtifactProvider provider = newProvider();
    writeArtifact1(provider);
    writeArtifact1a(provider);
    writeArtifact2(provider);

    provider.removeArtifactsFor(getSource2());
    assertEquals(2, provider.getCacheSize());
    readArtifact1(provider);
    readArtifact1a(provider);
  }

  public void test_CachingArtifactProvider_saveAndLoad() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {

      @Override
      public void run(File tempDir) throws Exception {
        test_CachingArtifactProvider_saveAndLoad(tempDir);
      }
    });
  }

  public void test_CachingArtifactProvider_saveAndLoad(File tempDir) throws IOException {
    File cacheFile = new File(tempDir, "artifacts.zip");
    long startTime = System.currentTimeMillis();

    // Populate and save artifacts
    CachingArtifactProvider provider = newProvider();
    writeArtifact1(provider);
    writeArtifact1a(provider);
    writeArtifact2(provider);

    final int expectedArtifactCount = 3;
    assertEquals(expectedArtifactCount, provider.getCacheSize());
    int saveCount = provider.saveCachedArtifacts(cacheFile);
    assertEquals(expectedArtifactCount, saveCount);
    long lastModified1 = provider.getArtifactLastModified(getSource1(), getSource1(), "js");
    assertTrue(lastModified1 >= startTime);
    assertTrue(lastModified1 <= System.currentTimeMillis());

    // Load and validate artifacts
    provider = newProvider();
    int loadCount = provider.loadCachedArtifacts(cacheFile);
    assertEquals(expectedArtifactCount, loadCount);
    assertEquals(expectedArtifactCount, provider.getCacheSize());
    readArtifact1(provider);
    readArtifact1a(provider);
    readArtifact2(provider);
    assertEquals(lastModified1, provider.getArtifactLastModified(getSource1(), getSource1(), "js"));
  }

  public void test_CachingArtifactProvider_writeAndRead() throws Exception {
    CachingArtifactProvider provider = newProvider();

    writeArtifact1(provider);
    readArtifact1(provider);

    writeArtifact1a(provider);
    assertEquals(2, provider.getCacheSize());
    readArtifact1(provider);
    readArtifact1a(provider);

    writeArtifact2(provider);
    readArtifact1(provider);
    readArtifact1a(provider);
    readArtifact2(provider);
  }

  private String getArtifact1() {
    return "a;klsdjf this is artifact 1 a;sdkljf";
  }

  private String getArtifact1a() {
    return "a;wewerdsf this is artifact 1a a;vwevzezs";
  }

  private String getArtifact2() {
    return "a;8769867 this is artifact 2 a;vnuef";
  }

  private DartSourceString getSource1() {
    return new DartSourceString("foo", "class foo { }");
  }

  private DartSourceString getSource2() {
    return new DartSourceString("bar", "class bar { }");
  }

  private CachingArtifactProvider newProvider() {
    return new CachingArtifactProvider() {
    };
  }

  private void readArtifact(CachingArtifactProvider provider, DartSourceString source, String part,
      String extension, String artifact) throws IOException {
    Reader reader = provider.getArtifactReader(source, part, extension);
    if (reader == null) {
      fail("Expected reader for source: " + source.getName() + " part: " + part + " extension: "
          + extension);
    }
    String actual = CharStreams.toString(reader);
    assertEquals(artifact, actual);
  }

  private void readArtifact1(CachingArtifactProvider provider) throws IOException {
    readArtifact(provider, getSource1(), "", "js", getArtifact1());
  }

  private void readArtifact1a(CachingArtifactProvider provider) throws IOException {
    readArtifact(provider, getSource1(), "a", "js", getArtifact1a());
  }

  private void readArtifact2(CachingArtifactProvider provider) throws IOException {
    readArtifact(provider, getSource2(), "boo", "js", getArtifact2());
  }

  private void writeArtifact(CachingArtifactProvider provider, DartSourceString source,
      String part, String extension, String artifact) throws IOException {
    Writer writer = provider.getArtifactWriter(source, part, extension);
    writer.append(artifact);
    writer.close();
  }

  private void writeArtifact1(CachingArtifactProvider provider) throws IOException {
    writeArtifact(provider, getSource1(), "", "js", getArtifact1());
  }

  private void writeArtifact1a(CachingArtifactProvider provider) throws IOException {
    writeArtifact(provider, getSource1(), "a", "js", getArtifact1a());
  }

  private void writeArtifact2(CachingArtifactProvider provider) throws IOException {
    writeArtifact(provider, getSource2(), "boo", "js", getArtifact2());
  }
}
