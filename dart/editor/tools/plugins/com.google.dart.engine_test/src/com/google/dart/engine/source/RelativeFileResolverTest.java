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
package com.google.dart.engine.source;

import com.google.dart.engine.utilities.io.FileUtilities2;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import junit.framework.TestCase;

import java.io.File;
import java.net.URI;

public class RelativeFileResolverTest extends TestCase {

  public void test_creation() {
    File root = createFile("/does/not/exist");
    File directory = createFile("/does/not/exist/relative");
    assertNotNull(new RelativeFileUriResolver(root, directory));
  }

  public void test_resolve_file() throws Exception {
    File root = FileUtilities2.createTempDir("/does/not/exist");
    File directory = FileUtilities2.createTempDir("/does/not/exist/relative");

    File testFile = new File(directory, "exist.dart");
    testFile.createNewFile();

    URI uri = new URI(
        "file",
        null,
        root.toURI().getPath() + File.separator + "exist.dart",
        null,
        null);
    UriResolver resolver = new RelativeFileUriResolver(root, directory);
    Source result = resolver.resolveAbsolute(uri);
    assertNotNull(result);
    assertEquals(testFile.getAbsolutePath(), result.getFullName());
  }

  public void test_resolve_nonFile() throws Exception {
    File root = createFile("/does/not/exist");
    File directory = createFile("/does/not/exist/relative");
    UriResolver resolver = new RelativeFileUriResolver(root, directory);
    Source result = resolver.resolveAbsolute(new URI("dart:core"));
    assertNull(result);
  }

  @Override
  protected void tearDown() throws Exception {
    FileUtilities2.deleteTempDir();
  }
}
