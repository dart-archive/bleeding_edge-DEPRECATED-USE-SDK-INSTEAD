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
package com.google.dart.engine.integration;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.IOException;

public abstract class DirectoryBasedSuiteBuilder {
  public class AnalysisTest extends TestCase {
    private File sourceFile;

    public AnalysisTest(File sourceFile) {
      super(sourceFile.getName());
      this.sourceFile = sourceFile;
    }

    public void testFile() throws Exception {
      testSingleFile(sourceFile);
    }

    @Override
    protected void runTest() throws Throwable {
      try {
        setName("testFile");
        super.runTest();
      } finally {
        setName(sourceFile.getName());
      }
    }
  }

  public TestSuite buildSuite(File directory, String suiteName) {
    TestSuite suite = new TestSuite(suiteName);
    if (directory.exists()) {
      addTestsForFilesIn(suite, directory);
    }
    return suite;
  }

  protected abstract void testSingleFile(File sourceFile) throws IOException;

  private void addTestForFile(TestSuite suite, File file) {
    suite.addTest(new AnalysisTest(file));
  }

  private void addTestsForFilesIn(TestSuite suite, File directory) {
    for (File file : directory.listFiles()) {
      if (file.isDirectory()) {
        TestSuite childSuite = new TestSuite("Tests in " + file.getName());
        addTestsForFilesIn(childSuite, file);
        if (childSuite.countTestCases() > 0) {
          suite.addTest(childSuite);
        }
      } else {
        String fileName = file.getName();
        if (fileName.endsWith(".dart") && !fileName.endsWith("_patch.dart")) {
          addTestForFile(suite, file);
        }
      }
    }
  }
}
