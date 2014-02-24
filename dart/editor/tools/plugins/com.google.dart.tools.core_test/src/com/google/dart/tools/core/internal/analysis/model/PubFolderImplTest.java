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

import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.PackageUriResolver;
import com.google.dart.engine.utilities.io.FileUtilities2;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.pub.PubspecModel;

import static com.google.dart.tools.core.DartCore.PACKAGES_DIRECTORY_NAME;
import static com.google.dart.tools.core.pub.PubYamlUtilsTest.pubspecYamlString;

import static org.mockito.Mockito.mock;

import java.io.File;

public class PubFolderImplTest extends PubResourceMapImplTest {

  private DartSdk expectedSdk;

  public void test_getInvertedSourceContainer() throws Exception {
    if (!setupSymlinks()) {
      return;
    }
    File file1 = new File(pkg1CanonicalDir, "file1.dart");
    File file2 = new File(pkg2CanonicalDir, "file2.dart");
    FileBasedSource source1 = new FileBasedSource(file1);
    FileBasedSource source2 = new FileBasedSource(file2);

    PubFolderImpl pubFolder = newTarget();
    InvertedSourceContainer container1 = pubFolder.getInvertedSourceContainer();

    assertFalse(container1.contains(source1));
    assertFalse(container1.contains(source2));

    FileUtilities2.deleteSymLink(new File(packagesDir, "pkg2"));
    InvertedSourceContainer container2 = pubFolder.getInvertedSourceContainer();

    assertFalse(container2.contains(source1));
    assertTrue(container2.contains(source2));
  }

  public void test_getPubspec() throws Exception {
    PubFolderImpl pubFolder = newTarget();
    PubspecModel pubspec = pubFolder.getPubspec();
    assertNotNull(pubspec);
    assertSame(pubspec, pubFolder.getPubspec());
    assertEquals("myapp", pubspec.getName());
  }

  public void test_getSdk() throws Exception {
    PubFolderImpl pubFolder = newTarget();
    DartSdk sdk = pubFolder.getSdk();
    assertNotNull(sdk);
    assertSame(expectedSdk, sdk);
  }

  @Override
  protected PubFolderImpl newTarget() {
    pubContainer.getMockFile(DartCore.PUBSPEC_FILE_NAME).setContents(
        pubspecYamlString.replace("name: web_components", "name: myapp"));
    return new PubFolderImpl(pubContainer, context, expectedSdk, new PackageUriResolver(new File(
        pubContainer.toFile(),
        PACKAGES_DIRECTORY_NAME)));
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    expectedSdk = mock(DartSdk.class);
  }
}
