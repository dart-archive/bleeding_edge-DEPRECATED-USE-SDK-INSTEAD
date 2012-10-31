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
package com.google.dart.tools.core.artifact;

import com.google.dart.tools.core.analysis.AnalysisTestUtilities;
import com.google.dart.tools.core.index.NotifyCallback;
import com.google.dart.tools.core.internal.index.impl.InMemoryIndex;
import com.google.dart.tools.core.test.util.FileUtilities;

import junit.framework.TestCase;

import java.io.File;
import java.util.concurrent.CountDownLatch;

public class TestGenerateArtifacts extends TestCase {

  private static final String OUTPUT_KEY = "build.extra.artifacts";

  /**
   * Generate the index for the SDK so that it does not need to be computed at startup
   */
  public void test_generate_SDK_index() throws Exception {

    File sdkIndexFile = InMemoryIndex.getSdkIndexFile();
    sdkIndexFile.getParentFile().mkdirs();
    sdkIndexFile.delete();
    assertFalse(sdkIndexFile.exists());

    long start = System.currentTimeMillis();

    InMemoryIndex index = InMemoryIndex.getInstance();
    index.initializeIndex(false);
    AnalysisTestUtilities.waitForAnalysis();
    final CountDownLatch latch = new CountDownLatch(1);
    index.notify(new NotifyCallback() {
      @Override
      public void done() {
        latch.countDown();
      }
    });
    latch.await();
    index.writeIndexToSdk();

    long delta = System.currentTimeMillis() - start;
    System.out.println("Generated SDK index in " + delta + " ms");
    System.out.println("  generated: " + sdkIndexFile);

    // Sanity check the result
    assertTrue(sdkIndexFile.exists());
    if (sdkIndexFile.length() < 1000) {
      long actualSize = sdkIndexFile.length();
      // delete bad index file so that it does not interfere with other tests
      sdkIndexFile.delete();
      fail(sdkIndexFile.getName() + " has only " + actualSize + " bytes");
    }

    // If this is a local test execution, not a build, then don't copy the index file
    String userName = System.getProperty("user.name"); //$NON-NLS-1$
    if (getOutputName() == null && !userName.startsWith("chrome")) {
      System.out.println(">>> Skip copying " + getClass().getSimpleName());
      return;
    }

    // Copy the index file to a location that can be picked up by the build
    File baseDir = sdkIndexFile.getParentFile().getParentFile();
    String relPath = sdkIndexFile.getPath().substring(baseDir.getPath().length() + 1);
    File configDir = new File(getOutputDir(), "configuration");
    File outputFile = new File(configDir, relPath);
    outputFile.getParentFile().mkdirs();
    outputFile.delete();
    assertFalse(outputFile.exists());
    FileUtilities.copyFile(sdkIndexFile, outputFile);

    System.out.println("  copied to: " + outputFile);
  }

  /**
   * Answer the output directory as defined by the "build.extra.artifacts" system property. Fail if
   * the system property is not defined.
   */
  private File getOutputDir() {
    String outputName = getOutputName();
    if (outputName == null) {
      fail(OUTPUT_KEY + " is not defined");
    }
    File outputDir = new File(outputName);
    outputDir.mkdirs();
    return outputDir;
  }

  /**
   * Answer the output directory name as defined by the "build.extra.artifacts" system property or
   * <code>null</code> if it is undefined.
   */
  private String getOutputName() {
    return System.getProperty(OUTPUT_KEY, null);
  }
}
