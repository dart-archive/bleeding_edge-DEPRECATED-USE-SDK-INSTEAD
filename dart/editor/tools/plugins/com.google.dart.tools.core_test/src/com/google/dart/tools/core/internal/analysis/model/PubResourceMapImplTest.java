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

import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.utilities.io.FileUtilities2;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.mock.MockFile;
import com.google.dart.tools.core.mock.MockFolder;

import java.io.File;
import java.io.IOException;

public class PubResourceMapImplTest extends SimpleResourceMapImplTest {

  protected File packagesDir;
  protected File pkg1CanonicalDir;
  protected File pkg2CanonicalDir;
  private MockFolder packagesContainer;
  private MockFolder pkg1Container;
  private MockFolder pkg2Container;

  public void test_getResource_fromSourceInPackage() throws Exception {
    if (!setupSymlinks()) {
      return;
    }
    File file1 = new File(pkg1CanonicalDir, "file1.dart");
    File file2 = new File(pkg2CanonicalDir, "file2.dart");
    FileBasedSource source1 = new FileBasedSource(contentCache, file1);
    FileBasedSource source2 = new FileBasedSource(contentCache, file2);
    MockFile res1 = pkg1Container.addFile("file1.dart");
    MockFile res2 = pkg2Container.addFile("file2.dart");

    PubResourceMapImpl map = newTarget();
    map.getResource(source1);
    assertSame(res1, map.getResource(source1));
    assertSame(res2, map.getResource(source2));
  }

  public void test_getSource_fromPackageResource() throws Exception {
    if (!setupSymlinks()) {
      return;
    }
    File file1 = new File(pkg1CanonicalDir, "file1.dart");
    File file2 = new File(pkg2CanonicalDir, "file2.dart");
    FileBasedSource source1 = new FileBasedSource(contentCache, file1);
    FileBasedSource source2 = new FileBasedSource(contentCache, file2);
    MockFile res1 = pkg1Container.addFile("file1.dart");
    MockFile res2 = pkg2Container.addFile("file2.dart");

    PubResourceMapImpl map = newTarget();
    map.getResource(source1);
    assertEquals(source1, map.getSource(res1));
    assertEquals(source2, map.getSource(res2));
  }

  @Override
  protected PubResourceMapImpl newTarget() {
    return new PubResourceMapImpl(pubContainer, context);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    packagesDir = pubContainer.getLocation().append(DartCore.PACKAGES_DIRECTORY_NAME).toFile();
    pkg1CanonicalDir = FileUtilities2.createTempDir("pkg1").getCanonicalFile();
    pkg2CanonicalDir = FileUtilities2.createTempDir("pkg2").getCanonicalFile();
    packagesContainer = pubContainer.addFolder(DartCore.PACKAGES_DIRECTORY_NAME);
    pkg1Container = packagesContainer.addFolder("pkg1");
    pkg2Container = packagesContainer.addFolder("pkg2");
  }

  protected boolean setupSymlinks() throws IOException {
    if (!FileUtilities2.isSymLinkSupported()) {
      System.out.println("Skipping " + getClass().getSimpleName()
          + " test_getInvertedSourceContainer()");
      return false;
    }
    assertTrue(packagesDir.mkdirs());
    FileUtilities2.createSymLink(pkg1CanonicalDir, new File(packagesDir, "pkg1"));
    FileUtilities2.createSymLink(pkg2CanonicalDir, new File(packagesDir, "pkg2"));
    return true;
  }
}
