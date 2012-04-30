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
  public void test_SourceImpl_nonSystem() {
    SourceFactory factory = new SourceFactory();
    File file = new File("/does/not/exist.dart");
    SourceImpl source = new SourceImpl(factory, file);
    assertNotNull(source);
    assertEquals(file, source.getFile());
    assertFalse(source.isInSystemLibrary());
  }

  public void test_SourceImpl_system() {
    SourceFactory factory = new SourceFactory();
    File file = new File("/does/not/exist.dart");
    SourceImpl source = new SourceImpl(factory, file, true);
    assertNotNull(source);
    assertEquals(file, source.getFile());
    assertTrue(source.isInSystemLibrary());
  }
}
