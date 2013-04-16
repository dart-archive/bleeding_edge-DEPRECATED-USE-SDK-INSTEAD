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

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.PackageUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.utilities.io.FileUtilities;
import com.google.dart.engine.utilities.io.PrintStringWriter;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

public class LanguageAnalysisTest extends DirectoryBasedSuiteBuilder {
  public class AnalysisTestWithSource extends AnalysisTest {
    private int index;

    private String contents;

    public AnalysisTestWithSource(File sourceFile, int index, String contents) {
      super(sourceFile);
      this.index = index;
      this.contents = contents;
    }

    @Override
    public void testFile() throws Exception {
      testSingleFile(getSourceFile(), contents);
    }

    @Override
    protected String getTestName() {
      if (index >= 0) {
        return getSourceFile().getName() + ":" + index;
      }
      return getSourceFile().getName();
    }
  }

  public class ReportingTest extends TestCase {
    public ReportingTest(String methodName) {
      super(methodName);
    }

    public void reportResults() throws Exception {
      System.out.print("Analyzed ");
      System.out.print(fileCount);
      System.out.print(" files in ");
      printTime(totalTime);
      System.out.println();

      System.out.print(skippedTests);
      System.out.println(" tests were skipped");

      System.out.print(errorCount);
      System.out.println(" tests failed with unexpected errors");

      System.out.print(noErrorCount);
      System.out.println(" tests failed with no errors being generated");
    }

    private void printTime(long time) {
      if (time == 0) {
        System.out.print("0 ms");
      } else {
        System.out.print(time);
        System.out.print(" ms");
        if (time > 60000) {
          long seconds = time / 1000;
          long minutes = seconds / 60;
          seconds -= minutes * 60;
          System.out.print(" (");
          System.out.print(minutes);
          System.out.print(":");
          if (seconds < 10) {
            System.out.print("0");
          }
          System.out.print(seconds);
          System.out.print(")");
        }
      }
    }
  }

  /**
   * The URI resolver used to resolve package: URI's.
   */
  private static PackageUriResolver packageResolver;

  /**
   * An array containing the relative paths of test files that are expected to fail.
   */
  private static final String[] FAILING_TESTS = {//
  "/application_test.dart", // missing a part-of directive
      "/bit_operations_test.dart", // requires type inferencing
      "/checked_null_test.dart", // invocation of undefined function
      "/closure_in_initializer2_test.dart", // invocation of undefined function
      "/compile_time_constant_k_test.dart", // duplicated keys in literal map
      "/compile_time_constant_o_test.dart", // duplicated keys in literal map
      "/const_objects_are_immutable_test.dart", // undefined setter
      "/constructor_test.dart", // references undefined method
      "/factory_implementation_test.dart", // misuse of factory method
      "/first_class_types_literals_test.dart", // type literals don't implement +
      "/function_malformed_result_type_test.dart", // wrong number of type arguments
      "/generic_instanceof_test.dart", // missing part-of directive
      "/implicit_scope_test.dart", //test implies that some statements create their own scope
      "/import_combinators_test.dart", // missing a part-of directive
      "/lazy_static6_test.dart", // missing a part-of directive
      "/many_generic_instanceof_test.dart", // missing a part-of directive
      "/many_overridden_no_such_method_test.dart", // missing a part-of directive
      "/map_literal2_test.dart", // keys in a map must be constant
      "/multi_pass2_test.dart", // missing a part-of directive
      "/multi_pass_test.dart", // missing a part-of directive
      "/null_no_such_method_test.dart", // valid and expected warning which isn't declared by test
      "/overridden_no_such_method_test.dart", // missing a part-of directive
      "/private2_test.dart", // missing a part-of directive
      "/private3_test.dart", // missing a part-of directive
      "/private_test.dart", // missing a part-of directive
      "/super_assign_test.dart", // type propagation required
      "/super_call4_test.dart", // invocations through noSuchMethod
      "/super_getter_setter_test.dart", //invocations through noSuchMethod
      "/super_operator_index5_test.dart", //invocations through noSuchMethod
      "/super_operator_index6_test.dart", //invocations through noSuchMethod
      "/super_operator_index7_test.dart", //invocations through noSuchMethod
      "/ternary_test.dart", // no unary plus operator
      "/top_level_entry_test.dart", // missing a part-of directive
      "/top_level_multiple_files_test.dart", // missing a part-of directive
      "/top_level_non_prefixed_library_test.dart", // missing a part-of directive
      "/type_error_test.dart", // invalid assumption about the type of an assignment statement
      "/type_vm_test.dart", // StaticTypeWarningCode.NON_BOOL_CONDITION is a warning, not an error
      "/unary2_test.dart", // no unary plus operator
      "/unary_test.dart", // no unary plus operator
  };

  /**
   * Build a JUnit test suite that will analyze all of the tests in the language test suite.
   * 
   * @return the test suite that was built
   */
  public static Test suite() {
    String svnRootName = System.getProperty("svnRoot");
    if (svnRootName != null) {
      File svnRoot = new File(svnRootName);
      File packageDirectory = new File(svnRoot, "pkg");
      File testDirectory = new File(svnRoot, "tests/language");
      if (packageDirectory.exists() && testDirectory.exists()) {
        packageResolver = new PackageUriResolver(packageDirectory) {
          @Override
          protected File getCanonicalFile(File packagesDirectory, String pkgName, String relPath) {
            File pkgDir = new File(packagesDirectory, pkgName);
            try {
              pkgDir = pkgDir.getCanonicalFile();
            } catch (IOException e) {
              AnalysisEngine.getInstance().getLogger().logError("Canonical failed: " + pkgDir, e);
            }
            File file = new File(pkgDir, relPath.replace('/', File.separatorChar));
            if (!file.exists()) {
              file = new File(new File(pkgDir, "lib"), relPath.replace('/', File.separatorChar));
            }
            return file;
          }
        };
        LanguageAnalysisTest tester = new LanguageAnalysisTest();
        TestSuite suite = tester.buildSuite(testDirectory, "Analyze language files");
        suite.addTest(tester.new ReportingTest("reportResults"));
        return suite;
      }
    }
    return new TestSuite("Analyze language files (no tests: directory not found)");
  }

  /**
   * Return {@code true} if the given file should be skipped because it is on the list of files to
   * be skipped.
   * 
   * @param file the file being tested
   * @return {@code true} if the file should be skipped
   */
  private static boolean expectedToFail(File file) {
    String fullPath = file.getAbsolutePath();
    for (String relativePath : FAILING_TESTS) {
      if (fullPath.endsWith(relativePath)) {
        return true;
      }
    }
    return false;
  }

  private long fileCount = 0L;

  private long totalTime = 0L;

  private int skippedTests = 0;

  @Override
  protected void addTestForFile(TestSuite suite, File file) {
    if (file.getName().endsWith("_test.dart")) {
      //
      // Determine how many tests to create from this one file.
      //
      try {
        String contents = FileUtilities.getContents(file);
        if (contents.indexOf("///") < 0) {
          suite.addTest(new AnalysisTestWithSource(file, -1, contents));
          return;
        }
        String[] lines = toLines(contents);
        int count = getTestCount(lines);
        for (int i = 0; i < count; i++) {
          String testSource = getTestSource(i, lines);
          suite.addTest(new AnalysisTestWithSource(file, i, testSource));
        }
      } catch (IOException exception) {
        suite.addTest(new TestSuite("Analyze " + file.getAbsolutePath() + " (could not read file)"));
      }
    }
  }

  @Override
  protected void testSingleFile(File sourceFile) throws IOException {
    // This method should never be called.
    throw new InternalError("Wrong test method invoked for file " + sourceFile.getAbsolutePath());
  }

  protected void testSingleFile(File sourceFile, String contents) throws Exception {
    //
    // Uncomment the lines below to stop reporting failures for files containing directives or
    // interface declarations.
    //
    if (contents.indexOf("#library") >= 0 || contents.indexOf("#import") >= 0
        || contents.indexOf("#source") >= 0 || contents.indexOf("interface") >= 0
        || contents.indexOf("===") >= 0 || contents.indexOf("!==") >= 0) {
      skippedTests++;
      return;
    }
    //
    // Determine whether the test is expected to pass or fail.
    //
    boolean errorExpected = sourceFile.getName().endsWith("_negative_test.dart")
        || contents.indexOf("compile-time error") > 0
        || contents.indexOf("static type warning") > 0 || contents.indexOf("static warning") > 0;
    // Uncomment the lines below to stop reporting failures for files that are expected to contain
    // errors.
//    if (errorExpected) {
//      skippedTests++;
//      return;
//    }
    //
    // Create the analysis context in which the file will be analyzed.
    //
    DartSdk sdk = DirectoryBasedDartSdk.getDefaultSdk();
    Assert.assertNotNull(packageResolver);
    SourceFactory sourceFactory = new SourceFactory(
        new DartUriResolver(sdk),
        new FileUriResolver(),
        packageResolver);
    AnalysisContext context = AnalysisEngine.getInstance().createAnalysisContext();
    context.setSourceFactory(sourceFactory);
    //
    // Analyze the file.
    //
    Source source = new FileBasedSource(sourceFactory.getContentCache(), sourceFile);
    sourceFactory.setContents(source, contents);
    long startTime = System.currentTimeMillis();
    LibraryElement library = context.computeLibraryElement(source);
    long endTime = System.currentTimeMillis();
    if (library == null) {
      Assert.fail("Could not analyze " + sourceFile.getAbsolutePath());
    }
    //
    // Gather statistics.
    //
    fileCount++;
    totalTime += (endTime - startTime);
    //
    // Validate the results.
    //
    ElementStructureVerifier elementVerifier = new ElementStructureVerifier();
    library.accept(elementVerifier);
    elementVerifier.assertValid();

    ArrayList<AnalysisError> errorList = new ArrayList<AnalysisError>();
    addErrors(errorList, library.getDefiningCompilationUnit());
    for (CompilationUnitElement part : library.getParts()) {
      addErrors(errorList, part);
    }
    assertErrors(errorExpected, expectedToFail(sourceFile), errorList);
  }

  private int getTestCount(String[] lines) {
    int maxIndex = 0;
    for (int i = 0; i < lines.length; i++) {
      int testIndex = parseTestIndex(lines[i]);
      maxIndex = Math.max(maxIndex, testIndex);
    }
    return maxIndex + 1;
  }

  private String getTestSource(int testIndex, String[] lines) {
    PrintStringWriter writer = new PrintStringWriter();
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      int index = parseTestIndex(line);
      if (index < 0 || index == testIndex) {
        writer.println(line);
      }
    }
    return writer.toString();
  }

  private int parseTestIndex(String line) {
    int index = line.indexOf("///");
    if (index < 0) {
      return -1;
    }
    int length = line.length();
    while (index < length && Character.isWhitespace(line.charAt(index))) {
      index++;
    }
    int testIndex = 0;
    while (index < length && Character.isDigit(line.charAt(index))) {
      testIndex = (testIndex * 10) + Character.digit(line.charAt(index++), 10);
    }
    return testIndex;
  }

  private String[] toLines(String source) {
    ArrayList<String> lines = new ArrayList<String>();
    BufferedReader reader = new BufferedReader(new StringReader(source));
    String line;
    try {
      line = reader.readLine();
      while (line != null) {
        lines.add(line);
        line = reader.readLine();
      }
    } catch (IOException exception) {
      // This cannot happen because we are reading from a string, not from an external source.
    }
    return lines.toArray(new String[lines.size()]);
  }
}
