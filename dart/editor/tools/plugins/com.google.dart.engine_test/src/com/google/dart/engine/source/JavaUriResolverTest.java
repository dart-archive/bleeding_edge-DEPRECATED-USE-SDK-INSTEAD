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

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.utilities.io.FileUtilities2;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import java.io.File;
import java.net.URI;

public class JavaUriResolverTest extends EngineTestCase {
  public void test_isJavaUri_java_scheme() throws Exception {
    URI uri = new URI("java:android.activity.Context");
    assertTrue(JavaUriResolver.isJavaUri(uri));
  }

  public void test_isJavaUri_null_scheme() throws Exception {
    URI uri = new URI("Context");
    assertNull(uri.getScheme());
    assertFalse(JavaUriResolver.isJavaUri(uri));
  }

  public void test_isJavaUri_package_scheme() throws Exception {
    URI uri = new URI("package:path/path.dart");
    assertFalse(JavaUriResolver.isJavaUri(uri));
  }

  @SuppressWarnings("deprecation")
  public void test_resolve_java() throws Exception {
    // rootA
    File rootA = FileUtilities2.createTempDir("rootA");
    File libA = new File(rootA, "libA");
    libA.mkdirs();
    File fileA = new File(libA, "ClassA.dart");
    Files.write("library libA.ClassA;", fileA, Charsets.UTF_8);
    // rootB
    File rootB = FileUtilities2.createTempDir("rootB");
    File libB = new File(rootA, "libB");
    libB.mkdirs();
    File fileB = new File(libB, "ClassB.dart");
    Files.write("library libB.ClassB;", fileB, Charsets.UTF_8);
    // prepare resolver
    UriResolver resolver = new JavaUriResolver(rootA, rootB);
    // fileA
    {
      Source result = resolver.resolveAbsolute(new URI("java:libA.ClassA"));
      assertEquals(fileA, new File(result.getFullName()));
      assertSame(UriKind.JAVA_URI, result.getUriKind());
    }
    // fileB
    {
      Source result = resolver.resolveAbsolute(new URI("java:libB.ClassB"));
      assertEquals(fileB, new File(result.getFullName()));
      assertSame(UriKind.JAVA_URI, result.getUriKind());
    }
  }

  public void test_resolve_nonExisting() throws Exception {
    File rootA = createFile("/rootA");
    File rootB = createFile("/rootB");
    JavaUriResolver resolver = new JavaUriResolver(rootA, rootB);
    Source result = resolver.resolveAbsolute(new URI("java:no.such.Class"));
    assertInstanceOf(NonExistingSource.class, result);
  }

  public void test_resolve_package() throws Exception {
    File rootA = createFile("/rootA");
    File rootB = createFile("/rootB");
    JavaUriResolver resolver = new JavaUriResolver(rootA, rootB);
    Source result = resolver.resolveAbsolute(new URI("package:lib.Class"));
    assertNull(result);
  }

  @Override
  protected void tearDown() throws Exception {
    FileUtilities2.deleteTempDir();
  }
}
