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

public class SourceContainerImplTest extends TestCase {

  public void test_contains() {
    File dir = new File("/does/not/exist");
    File file1 = new File("/does/not/exist/some.dart");
    File file2 = new File("/does/not/exist/folder/some2.dart");
    File file3 = new File("/does/not/exist3/some3.dart");

    SourceFactory factory = new SourceFactory();
    SourceImpl source1 = new SourceImpl(factory, file1);
    SourceImpl source2 = new SourceImpl(factory, file2);
    SourceImpl source3 = new SourceImpl(factory, file3);

    SourceContainerImpl container = new SourceContainerImpl(dir);

    assertTrue(container.contains(source1));
    assertTrue(container.contains(source2));
    assertFalse(container.contains(source3));
  }
}
