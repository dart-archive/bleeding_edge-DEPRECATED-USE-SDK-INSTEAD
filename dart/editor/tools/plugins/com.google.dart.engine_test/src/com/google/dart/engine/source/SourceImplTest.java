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
package com.google.dart.engine.source;

import junit.framework.TestCase;

import java.io.File;

public class SourceImplTest extends TestCase {
  public void test_equals_false() {
    SourceFactory factory = new SourceFactory();
    File file1 = new File("/does/not/exist1.dart");
    File file2 = new File("/does/not/exist2.dart");
    SourceImpl source1 = new SourceImpl(factory, file1);
    SourceImpl source2 = new SourceImpl(factory, file2);
    assertFalse(source1.equals(source2));
  }

  public void test_equals_true() {
    SourceFactory factory = new SourceFactory();
    File file1 = new File("/does/not/exist.dart");
    File file2 = new File("/does/not/exist.dart");
    SourceImpl source1 = new SourceImpl(factory, file1);
    SourceImpl source2 = new SourceImpl(factory, file2);
    assertTrue(source1.equals(source2));
  }

  public void test_getContainer() {
    SourceFactory factory = new SourceFactory();
    final SourceContainer[] container = {new TestSourceContainer()};
    ContainerMapper mapper = new ContainerMapper() {
      @Override
      public SourceContainer getContainerFor(Source source) {
        return container[0];
      }
    };
    factory.setContainerMapper(mapper);
    SourceImpl source = new SourceImpl(factory, new File("/does/not/exist.dart"));

    assertEquals(container[0], source.getContainer());

    container[0] = new TestSourceContainer();
    source.resetContainer();
    assertEquals(container[0], source.getContainer());
  }

  public void test_getFullName() {
    SourceFactory factory = new SourceFactory();
    String fullPath = "/does/not/exist.dart";
    File file = new File(fullPath);
    SourceImpl source = new SourceImpl(factory, file);
    assertEquals(fullPath, source.getFullName());
  }

  public void test_getShortName() {
    SourceFactory factory = new SourceFactory();
    File file = new File("/does/not/exist.dart");
    SourceImpl source = new SourceImpl(factory, file);
    assertEquals("exist.dart", source.getShortName());
  }

  public void test_hashCode() {
    SourceFactory factory = new SourceFactory();
    File file1 = new File("/does/not/exist.dart");
    File file2 = new File("/does/not/exist.dart");
    SourceImpl source1 = new SourceImpl(factory, file1);
    SourceImpl source2 = new SourceImpl(factory, file2);
    assertEquals(source1.hashCode(), source2.hashCode());
  }

  public void test_nonSystem() {
    SourceFactory factory = new SourceFactory();
    File file = new File("/does/not/exist.dart");
    SourceImpl source = new SourceImpl(factory, file);
    assertNotNull(source);
    assertEquals(file.getAbsolutePath(), source.getFullName());
    assertFalse(source.isInSystemLibrary());
  }

  public void test_resolve_absolute() {
    SourceFactory factory = new SourceFactory(new FileUriResolver());
    File file = new File("/does/not/exist1.dart");
    SourceImpl source = new SourceImpl(factory, file);
    Source result = source.resolve("file:///invalid/path.dart");
    assertEquals("/invalid/path.dart", result.getFullName());
  }

  public void test_resolve_relative() {
    SourceFactory factory = new SourceFactory(new FileUriResolver());
    File file = new File("/does/not/exist1.dart");
    SourceImpl source = new SourceImpl(factory, file);
    Source result = source.resolve("exist2.dart");
    assertNotNull(result);
    assertEquals("/does/not/exist2.dart", result.getFullName());
  }

  public void test_system() {
    SourceFactory factory = new SourceFactory();
    File file = new File("/does/not/exist.dart");
    SourceImpl source = new SourceImpl(factory, file, true);
    assertNotNull(source);
    assertEquals(file.getAbsolutePath(), source.getFullName());
    assertTrue(source.isInSystemLibrary());
  }
}
