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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.utilities.io.PrintStringWriter;

import junit.framework.Assert;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public abstract class DirectoryBasedSuiteBuilder {
  public class AnalysisTest extends TestCase {
    private File sourceFile;

    public AnalysisTest(File sourceFile) {
      super(sourceFile.getName());
      this.sourceFile = sourceFile;
    }

    public File getSourceFile() {
      return sourceFile;
    }

    public void testFile() throws Exception {
      testSingleFile(sourceFile);
    }

    protected String getTestName() {
      return sourceFile.getName();
    }

    @Override
    protected void runTest() throws Throwable {
      try {
        setName("testFile");
        super.runTest();
      } finally {
        setName(getTestName());
      }
    }
  }

  /**
   * The number of tests that failed because there were errors generated when there were not
   * expected to be any errors.
   */
  protected int errorCount = 0;

  /**
   * The number of tests that failed because there were no errors generated when errors were
   * expected.
   */
  protected int noErrorCount = 0;

  public TestSuite buildSuite(File directory, String suiteName) {
    TestSuite suite = new TestSuite(suiteName);
    if (directory.exists()) {
      addTestsForFilesIn(suite, directory);
    }
    return suite;
  }

  /**
   * Add the errors reported for the given compilation unit to the given list of errors.
   * 
   * @param errorList the list to which the errors are to be added
   * @param element the compilation unit whose errors are to be added
   * @throws AnalysisException if the errors could not be determined
   */
  protected void addErrors(ArrayList<AnalysisError> errorList, CompilationUnitElement element)
      throws AnalysisException {
    LibraryElement library = element.getLibrary();
    AnalysisContext context = library.getContext();
    CompilationUnit unit = context.resolveCompilationUnit(element.getSource(), library);
    AnalysisError[] errors = unit.getErrors();
    if (errors == null) {
      Assert.fail("The compilation unit \"" + element.getSource().getFullName()
          + "\" was not resolved");
    }
    for (AnalysisError error : errors) {
      errorList.add(error);
    }
  }

  protected void addTestForFile(TestSuite suite, File file) {
    suite.addTest(new AnalysisTest(file));
  }

  /**
   * Assert that the errors in the error list match the expected behavior of the test.
   * 
   * @param errorExpected {@code true} if the test indicates that errors should be produced
   * @param expectedToFail {@code true} if the outcome is expected to be inverted from normal
   * @param errorList the list of errors that were produced for the files that were analyzed
   */
  protected void assertErrors(boolean errorExpected, boolean expectedToFail,
      ArrayList<AnalysisError> errorList) {
    Collections.sort(errorList, AnalysisError.ERROR_CODE_COMPARATOR);
    if (errorExpected) {
      if (expectedToFail) {
        if (errorList.size() > 0) {
          Assert.fail("Expected to fail with no errors, but passed with errors");
        }
      } else {
        if (errorList.size() <= 0) {
          noErrorCount++;
          Assert.fail("Expected errors, found none");
        }
      }
    } else {
      if (expectedToFail) {
        if (errorList.size() <= 0) {
          Assert.fail("Expected to fail with errors, but passed with no errors");
        }
      } else {
        if (errorList.size() > 0) {
          errorCount++;
          PrintStringWriter writer = new PrintStringWriter();
          writer.print("Expected 0 errors, found ");
          writer.print(errorList.size());
          writer.print(":");
          for (AnalysisError error : errorList) {
            ErrorCode code = error.getErrorCode();
            int offset = error.getOffset();
            writer.println();
            writer.printf(
                "  %s (%d..%d) \"%s\"",
                code.getClass().getSimpleName() + "." + code,
                offset,
                offset + error.getLength(),
                error.getMessage());
          }
          Assert.fail(writer.toString());
        }
      }
    }
  }

  protected abstract void testSingleFile(File sourceFile) throws Exception;

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
