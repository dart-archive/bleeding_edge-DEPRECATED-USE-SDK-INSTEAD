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
package com.google.dart.tools.ui.internal.update;

import com.google.dart.tools.core.test.util.FileUtilities;
import com.google.dart.tools.core.test.util.PluginUtilities;
import com.google.dart.tools.core.test.util.TestUtilities;
import com.google.dart.tools.update.core.internal.UpdateUtils;

import junit.framework.TestCase;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class UpdateUtilsTest extends TestCase {

  private static final NullProgressMonitor NULL_MONITOR = new NullProgressMonitor();

  private static final FileFilter NO_OVERWRITE = new FileFilter() {
    @Override
    public boolean accept(File file) {
      return false;
    }
  };

  private File tempDir;

  private static final String TEST_PLUGIN_ID = "com.google.dart.tools.ui_test";

  private static final String TEST_DATA_DIR = "test_data";

  private static final String NULL_TASK = "";

  public void testCopyPreservesPerms() throws Exception {

    File fromDir = getDataDir("testCopyPreservesPerms");
    File toDir = getToDir();

    UpdateUtils.copyDirectory(fromDir, toDir, NO_OVERWRITE, NULL_MONITOR);

    assertTrue(new File(toDir, "exec").canExecute());
    assertFalse(new File(toDir, "no_exec").canExecute());
  }

  public void testParseRevisionJSON() throws Exception {

    // {
    //   "revision" : "9826",
    //   "version"  : "0.0.1_v2012070961811",
    //   "date"     : "2012-07-09"
    // }  
    String json = new StringBuilder().append("{ \"revision\": \"9826\", ").append(
        "\"version\": \"0.0.1_v2012070961811\", ").append("\"date\": \"2012-07-09\"").append(" }").toString();

    String revision = UpdateUtils.parseRevisionNumberFromJSON(json);
    assertEquals(revision, "9826");

  }

  public void testUnzipPreservesPerms() throws Exception {

    File fromDir = getDataDir("testUnzipPreservesPerms");
    File toDir = getToDir();

    UpdateUtils.unzip(new File(fromDir, "archive.zip"), toDir, NULL_TASK, NULL_MONITOR);

    assertTrue(new File(toDir, "exec").canExecute());
    assertFalse(new File(toDir, "no_exec").canExecute());
  }

  @Override
  protected void tearDown() throws Exception {
    if (tempDir != null) {
      FileUtilities.delete(tempDir);
    }
  }

  private File getDataDir(String testName) throws MalformedURLException, IOException {
    URL pluginInstallUri = PluginUtilities.getInstallUrl(TEST_PLUGIN_ID);
    URL sourceUrl = new URL(pluginInstallUri, TEST_DATA_DIR + "/" + getClass().getSimpleName()
        + "/" + testName);
    IPath sourcePath = new Path(FileLocator.toFileURL(sourceUrl).getPath());
    File fromDir = sourcePath.toFile();
    return fromDir;
  }

  private File getToDir() {
    if (tempDir == null) {
      tempDir = TestUtilities.createTempDirectory();
    }
    return tempDir.getAbsoluteFile();
  }

}
