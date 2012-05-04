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
package com.google.dart.tools.core.internal.builder;

import com.google.common.io.Closeables;
import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.UrlDartSource;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.generator.ApplicationGenerator;
import com.google.dart.tools.core.generator.FileGenerator;
import com.google.dart.tools.core.internal.model.CompilationUnitImpl;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.test.util.FileUtilities;
import com.google.dart.tools.core.test.util.TestUtilities;
import com.google.dart.tools.core.utilities.resource.IProjectUtilities;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.util.Random;

public class RootArtifactProviderTest extends TestCase {
  private static final String RANDOM_EXT1 = "jsx" + new Random().nextInt();
  private static final String RANDOM_EXT2 = "jsx" + new Random().nextInt();
  private static final String RANDOM_CONTENT1 = "some-random-text-" + new Random().nextFloat();
  private static final String RANDOM_CONTENT2 = "some-random-text-" + new Random().nextFloat();
  private static int appCount = 0;

  private File tempDir;
  private RootArtifactProvider provider;
  private DartLibraryImpl app;
  private CompilationUnitImpl unit1;
  private DartSource source1;
  private CompilationUnitImpl unit2;
  private DartSource source2;

  public void test_RootArtifactProvider_deleteClass() throws Exception {
    createProviderAndApp();
    writeArtifact(source1, "", RANDOM_EXT1, RANDOM_CONTENT1);
    writeArtifact(source2, "", RANDOM_EXT2, RANDOM_CONTENT2);
    assertArtifact(source1, "", RANDOM_EXT1, RANDOM_CONTENT1);
    assertArtifact(source2, "", RANDOM_EXT2, RANDOM_CONTENT2);
    unit2.getResource().delete(false, new NullProgressMonitor());
//    assertArtifact(source1, "", RANDOM_EXT1, RANDOM_CONTENT1);
    assertArtifact(source2, "", RANDOM_EXT2, null);
  }

//  public void test_RootArtifactProvider_deleteDefiningCompUnit() throws Exception {
//    createProviderAndApp();
//    writeArtifact(source1, "", RANDOM_EXT1, RANDOM_CONTENT1);
//    writeArtifact(source2, "", RANDOM_EXT2, RANDOM_CONTENT2);
//    assertArtifact(source1, "", RANDOM_EXT1, RANDOM_CONTENT1);
//    assertArtifact(source2, "", RANDOM_EXT2, RANDOM_CONTENT2);
//    unit1.getResource().delete(false, new NullProgressMonitor());
//    assertArtifact(source1, "", RANDOM_EXT1, null);
//    assertArtifact(source2, "", RANDOM_EXT2, null);
//  }

  public void test_RootArtifactProvider_deleteProject() throws Exception {
    createProviderAndApp();
    writeArtifact(source1, "", RANDOM_EXT1, RANDOM_CONTENT1);
    writeArtifact(source2, "", RANDOM_EXT2, RANDOM_CONTENT2);
    assertArtifact(source1, "", RANDOM_EXT1, RANDOM_CONTENT1);
    assertArtifact(source2, "", RANDOM_EXT2, RANDOM_CONTENT2);
    unit1.getLibrary().getDartProject().getProject().delete(true, true, new NullProgressMonitor());
    assertArtifact(source1, "", RANDOM_EXT1, null);
    assertArtifact(source2, "", RANDOM_EXT2, null);
  }

  public void test_RootArtifactProvider_getArtifactUri() throws Exception {
    createProviderAndApp();
    URI uri = provider.getArtifactUri(source1, "", RANDOM_EXT1);
    assertEquals(RANDOM_EXT1, uri.getPath().substring(uri.getPath().lastIndexOf(".") + 1));
  }

  public void test_RootArtifactProvider_readNonExistant() throws Exception {
    createProviderAndApp();
    assertArtifact(source1, "", "doesnotexist", null);
  }

  public void test_RootArtifactProvider_writeThenRead() throws Exception {
    createProviderAndApp();
    writeArtifact(source1, "", RANDOM_EXT1, RANDOM_CONTENT1);
    assertArtifact(source1, "", RANDOM_EXT1, RANDOM_CONTENT1);
  }

  protected void createProviderAndApp() throws Exception {
    provider = RootArtifactProvider.newInstanceForTesting();
    tempDir = TestUtilities.createTempDirectory();
    appCount++;

    IProject project = (IProject) IProjectUtilities.createOrOpenProject(
        tempDir,
        new NullProgressMonitor());
    ApplicationGenerator appGen = new ApplicationGenerator(project);
    appGen.setApplicationLocation(tempDir.getAbsolutePath());
    appGen.setApplicationName(getClass().getSimpleName() + appCount);
    appGen.execute(new NullProgressMonitor());
    CompilationUnitImpl unit = (CompilationUnitImpl) DartModelManager.getInstance().create(
        appGen.getFile());
    app = (DartLibraryImpl) unit.getLibrary();
    unit1 = (CompilationUnitImpl) app.getDefiningCompilationUnit();
    source1 = getSourceForUnit(unit1);

    String fileName = "MyClass" + appCount + ".dart";
    FileGenerator fileGen = new FileGenerator();
    fileGen.setLibrary(app);
    fileGen.setFileLocation(unit1.getResource().getLocation().removeLastSegments(1).toString());
    fileGen.setFileName(fileName);
    fileGen.execute(new NullProgressMonitor());
    unit2 = (CompilationUnitImpl) DartModelManager.getInstance().create(fileGen.getFile());
    source2 = getSourceForUnit(unit2);

    assertEquals(unit1.getLibrary(), unit2.getLibrary());
  }

  @Override
  protected void tearDown() throws Exception {
    if (provider != null) {
      provider.dispose();
    }
    if (app != null) {
      IProject project = app.getDartProject().getProject();
      if (project.exists()) {
        try {
          project.open(new NullProgressMonitor());
        } catch (Exception e) {
          DartCore.logInformation("Failed to reopen " + app.getDisplayName(), e);
        }
        try {
          project.delete(true, new NullProgressMonitor());
        } catch (Exception e) {
          DartCore.logInformation("Failed to delete " + app.getDisplayName(), e);
        }
      }
    }
    if (tempDir != null) {
      FileUtilities.delete(tempDir);
    }
  }

  private void assertArtifact(DartSource source, String part, String extension, String expected)
      throws IOException {
    Reader reader = provider.getArtifactReader(source, part, extension);
    if (reader != null) {
      boolean failed = true;
      try {
        char[] cbuf = new char[1000];
        int len = reader.read(cbuf);
        assertEquals(expected, new String(cbuf, 0, len));
        failed = false;
      } finally {
        Closeables.close(reader, failed);
      }
    } else {
      assertEquals(expected, null);
    }
  }

  private DartSource getSourceForUnit(CompilationUnitImpl unit) {
    File srcFile = unit.getResource().getLocation().toFile();
    LibrarySource libSrc = ((DartLibraryImpl) unit.getLibrary()).getLibrarySourceFile();
    return new UrlDartSource(srcFile, libSrc);
  }

  private void writeArtifact(DartSource source, String part, String extension, String content)
      throws IOException {
    Writer writer = provider.getArtifactWriter(source, part, extension);
    boolean failed = true;
    try {
      writer.append(content);
      failed = false;
    } finally {
      Closeables.close(writer, failed);
    }
  }
}
