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

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import junit.framework.TestCase;

import java.io.File;

public class DirectoryBasedSourceContainerTest extends TestCase {

  public void test_contains() {
    File dir = createFile("/does/not/exist");
    File file1 = createFile("/does/not/exist/some.dart");
    File file2 = createFile("/does/not/exist/folder/some2.dart");
    File file3 = createFile("/does/not/exist3/some3.dart");

    SourceFactory factory = new SourceFactory();
    FileBasedSource source1 = new FileBasedSource(factory, file1);
    FileBasedSource source2 = new FileBasedSource(factory, file2);
    FileBasedSource source3 = new FileBasedSource(factory, file3);

    DirectoryBasedSourceContainer container = new DirectoryBasedSourceContainer(dir);

    assertTrue(container.contains(source1));
    assertTrue(container.contains(source2));
    assertFalse(container.contains(source3));
  }
}
