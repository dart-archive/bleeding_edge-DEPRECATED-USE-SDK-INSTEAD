/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.core.internal.analysis.model;

import com.google.dart.engine.source.DirectoryBasedSourceContainer;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.SourceContainer;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import junit.framework.TestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link CompositeSourceContainer}
 */
public class CompositeSourceContainerTest extends TestCase {

  public void test_contains() {
    File dir1 = createFile("/does/not/exist");
    File file1 = createFile("/does/not/exist/some.dart");
    File file2 = createFile("/does/not/exist/folder/some2.dart");

    File dir2 = createFile("/dir2/folder");
    File file3 = createFile("/dir2/folder/some3.dart");
    File file4 = createFile("/dir2/folder2/some4.dart");

    FileBasedSource source1 = new FileBasedSource(file1);
    FileBasedSource source2 = new FileBasedSource(file2);
    FileBasedSource source3 = new FileBasedSource(file3);
    FileBasedSource source4 = new FileBasedSource(file4);

    List<SourceContainer> containers = new ArrayList<SourceContainer>();
    containers.add(new DirectoryBasedSourceContainer(dir1));
    containers.add(new DirectoryBasedSourceContainer(dir2));

    CompositeSourceContainer container = new CompositeSourceContainer(containers);
    assertTrue(container.contains(source1));
    assertTrue(container.contains(source2));
    assertTrue(container.contains(source3));
    assertFalse(container.contains(source4));

    InvertedSourceContainer invertedSourceContainer = new InvertedSourceContainer(container);
    assertFalse(invertedSourceContainer.contains(source1));
    assertFalse(invertedSourceContainer.contains(source2));
    assertFalse(invertedSourceContainer.contains(source3));
    assertTrue(invertedSourceContainer.contains(source4));

  }

}
