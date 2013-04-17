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

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.ContentCache;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.utilities.io.FileUtilities2;
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.core.internal.builder.TestProjects;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.mock.MockWorkspaceRoot;
import com.google.dart.tools.core.pub.PubspecModel;

import static com.google.dart.tools.core.DartCore.PUBSPEC_FILE_NAME;
import static com.google.dart.tools.core.pub.PubYamlUtilsTest.pubspecYamlString;

import junit.framework.TestCase;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;

public class PubFolderImplTest extends TestCase {

  private IContainer container;
  private AnalysisContext context;
  private PubFolder pubFolder;
  private DartSdk expectedSdk;

  public void test_getContext() {
    assertSame(context, pubFolder.getContext());
    verifyNoMoreInteractions(container, context);
  }

  public void test_getInvertedSourceContainer() throws Exception {
    if (!FileUtilities2.isSymLinkSupported()) {
      System.out.println("Skipping " + getClass().getSimpleName()
          + " test_getInvertedSourceContainer()");
      return;
    }

    MockWorkspaceRoot rootContainer = new MockWorkspaceRoot();
    MockProject projectContainer = TestProjects.newPubProject3(rootContainer);

    File projDir = FileUtilities2.createTempDir(projectContainer.getName());
    File packagesDir = new File(projDir, "packages");
    assertTrue(packagesDir.mkdir());
    File pkg1Dir = FileUtilities2.createTempDir("pkg1").getCanonicalFile();
    File file1 = new File(pkg1Dir, "file1.dart");
    File pkg2Dir = FileUtilities2.createTempDir("pkg2").getCanonicalFile();
    File file2 = new File(pkg2Dir, "file2.dart");

    FileUtilities2.createSymLink(pkg1Dir, new File(packagesDir, "pkg1"));
    FileUtilities2.createSymLink(pkg2Dir, new File(packagesDir, "pkg2"));

    ContentCache contentCache = new ContentCache();
    FileBasedSource source1 = new FileBasedSource(contentCache, file1);
    FileBasedSource source2 = new FileBasedSource(contentCache, file2);

    projectContainer.setLocation(new Path(projDir.getAbsolutePath()));

    PubFolder pFolder = new PubFolderImpl(projectContainer, context, expectedSdk);
    InvertedSourceContainer container = pFolder.getInvertedSourceContainer();

    assertFalse(container.contains(source1));
    assertFalse(container.contains(source2));

    FileUtilities2.deleteSymLink(new File(packagesDir, "pkg2"));
    InvertedSourceContainer container2 = pFolder.getInvertedSourceContainer();

    assertFalse(container2.contains(source1));
    assertTrue(container2.contains(source2));

  }

  public void test_getPubspec() throws Exception {
    IFile pubFile = mock(IFile.class);
    when(container.getFile(new Path(PUBSPEC_FILE_NAME))).thenReturn(pubFile);
    when(pubFile.getContents()).thenReturn(new ByteArrayInputStream(pubspecYamlString.getBytes()));
    when(pubFile.getCharset()).thenReturn("UTF-8");

    PubspecModel pubspec = pubFolder.getPubspec();
    assertNotNull(pubspec);
    assertSame(pubspec, pubFolder.getPubspec());
    assertEquals("web_components", pubspec.getName());
  }

  public void test_getResource() {
    assertSame(container, pubFolder.getResource());
    verifyNoMoreInteractions(container, context);
  }

  public void test_getSdk() throws Exception {
    final DartSdk sdk = pubFolder.getSdk();
    assertNotNull(sdk);
    assertSame(expectedSdk, sdk);
  }

  @Override
  protected void setUp() throws Exception {
    container = mock(IContainer.class);
    context = mock(AnalysisContext.class);
    expectedSdk = mock(DartSdk.class);
    pubFolder = new PubFolderImpl(container, context, expectedSdk);
  }

  @Override
  protected void tearDown() throws Exception {
    FileUtilities2.deleteTempDir();
  }

}
