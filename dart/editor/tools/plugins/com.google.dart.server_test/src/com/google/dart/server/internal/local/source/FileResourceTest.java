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
package com.google.dart.server.internal.local.source;

import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.UriKind;
import com.google.dart.engine.utilities.io.FileUtilities2;

import junit.framework.TestCase;

import java.io.File;

public class FileResourceTest extends TestCase {
  public void test_createSource() throws Exception {
    FileUtilities2.createTempDir("my");
    File file = FileUtilities2.createTempFile("my/file.dart", "");
    FileResource resource = new FileResource(file);
    Source source = resource.createSource(UriKind.FILE_URI);
    assertSame(UriKind.FILE_URI, source.getUriKind());
    assertTrue(source.exists());
  }

  public void test_equals() throws Exception {
    FileResource resourceA = new FileResource(new File("a.txt"));
    FileResource resourceA2 = new FileResource(new File("a.txt"));
    FileResource resourceB = new FileResource(new File("b.txt"));
    assertTrue(resourceA.equals(resourceA));
    assertFalse(resourceA.equals(this));
    assertTrue(resourceA.equals(resourceA2));
    assertFalse(resourceA.equals(resourceB));
  }

  public void test_exists_false() throws Exception {
    File file = new File("nofile.txt");
    FileResource resource = new FileResource(file);
    assertFalse(resource.exists());
  }

  public void test_exists_true() throws Exception {
    FileUtilities2.createTempDir("my");
    File file = FileUtilities2.createTempFile("my/file.txt", "");
    FileResource resource = new FileResource(file);
    assertTrue(resource.exists());
  }

  public void test_getChild() throws Exception {
    File dir = FileUtilities2.createTempDir("my");
    FileResource dirResource = new FileResource(dir);
    Resource child = dirResource.getChild("other/file.txt");
    assertEquals(dir.getAbsolutePath() + "/other/file.txt", child.getPath());
  }

  public void test_getPath() throws Exception {
    FileUtilities2.createTempDir("my");
    File file = FileUtilities2.createTempFile("my/file.txt", "");
    FileResource resource = new FileResource(file);
    assertEquals(file.getAbsolutePath(), resource.getPath());
  }

  public void test_hashCode() throws Exception {
    FileResource resource = new FileResource(new File("file.txt"));
    resource.hashCode();
  }

  @Override
  protected void tearDown() throws Exception {
    FileUtilities2.deleteTempDir();
  }
}
