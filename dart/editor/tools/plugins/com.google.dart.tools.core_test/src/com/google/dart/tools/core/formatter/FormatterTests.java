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
package com.google.dart.tools.core.formatter;

import com.google.dart.tools.core.internal.formatter.DartCodeFormatter;
import com.google.dart.tools.core.internal.formatter.DefaultCodeFormatterOptions;
import com.google.dart.tools.core.internal.model.CompilationUnitImpl;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.model.DartModelImpl;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.internal.model.DartProjectImpl;
import com.google.dart.tools.core.internal.workingcopy.DefaultWorkingCopyOwner;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.testutil.TestFileUtil;

import junit.framework.TestCase;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FormatterTests extends TestCase {
  private static final String IN = "_in";
  private static final String OUT = "_out";

  public static String convertToIndependantLineDelimiter(String source) {
    if (source == null) {
      return "";
    }
    if (source.indexOf('\n') == -1 && source.indexOf('\r') == -1) {
      return source;
    }
    StringBuffer buffer = new StringBuffer();
    for (int i = 0, length = source.length(); i < length; i++) {
      char car = source.charAt(i);
      if (car == '\r') {
        buffer.append('\n');
        if (i < length - 1 && source.charAt(i + 1) == '\n') {
          i++; // skip \n after \r
        }
      } else {
        buffer.append(car);
      }
    }
    return buffer.toString();
  }

  public static String editedString(String original, TextEdit edit) {
    if (edit == null) {
      return original;
    }
    IDocument document = new Document(original);
    try {
      edit.apply(document, TextEdit.NONE);
      return document.get();
    } catch (MalformedTreeException e) {
      e.printStackTrace();
    } catch (BadLocationException e) {
      e.printStackTrace();
    }
    return original;
  }

  private IProject project;
  private IContainer container;

  public void testA() {
    runTest("testA", "A.dart");
  }

//  public void test001() {
//    runTest("test001", "A.dart");//$NON-NLS-1$ //$NON-NLS-2$
//  }

//  public void test002() {
//    runTest("test002", "A.dart");//$NON-NLS-1$ //$NON-NLS-2$
//  }

  /*
  public void test007() {
    runTest("test007", "A.dart");//$NON-NLS-1$ //$NON-NLS-2$
  }

  public void test003() {
    runTest("test003", "A.dart");//$NON-NLS-1$ //$NON-NLS-2$
  }

  public void test004() {
    runTest("test004", "A.dart");//$NON-NLS-1$ //$NON-NLS-2$
  }

  public void test005() {
    runTest("test005", "A.dart");//$NON-NLS-1$ //$NON-NLS-2$
  }

  public void test006() {
    runTest("test006", "A.dart");//$NON-NLS-1$ //$NON-NLS-2$
  }

  public void testCoreCrash() throws Exception {
    DartLibrary library = getLibrary("corelib.lib");
    if (library == null) {
      fail();
    }
    DefaultCodeFormatterOptions preferences = new DefaultCodeFormatterOptions(
        DefaultCodeFormatterConstants.getDartConventionsSettings());
    preferences.number_of_empty_lines_to_preserve = 0;
    DefaultCodeFormatter codeFormatter = new DefaultCodeFormatter(preferences);
    CompilationUnit[] units = library.getCompilationUnits();
    int failures = 0;
    for (CompilationUnit unit : units) {
      String source = unit.getSource();
      String name = unit.getElementName();
      if (!runLibraryTest(name, codeFormatter, source, CodeFormatter.K_COMPILATION_UNIT, 0, 0, -1,
          null)) {
        failures += 1;
      }
    }
    assertTrue("Failed " + failures + " of " + units.length + " tests (see console for specifics)",
        failures == 0);
  }
  */

  protected void assertLineEquals(String actualContents, String originalSource,
      String expectedContents) {
    String outputSource = expectedContents == null ? originalSource : expectedContents;
    assertLineEquals(actualContents, originalSource, outputSource, false);
  }

  protected void assertLineEquals(String actualContents, String originalSource,
      String expectedContents, boolean checkNull) {
    if (actualContents == null) {
      assertTrue("actualContents is null", checkNull);
      assertEquals(expectedContents, originalSource);
      return;
    }
    assertSourceEquals(
        "Different number of length",
        convertToIndependantLineDelimiter(expectedContents),
        actualContents);
  }

  protected void assertSourceEquals(String message, String expected, String actual) {
    assertSourceEquals(message, expected, actual, true/* convert line delimiter */);
  }

  protected void assertSourceEquals(String message, String expected, String actual, boolean convert) {
    if (actual == null) {
      assertEquals(message, expected, null);
      return;
    }
    if (convert) {
      actual = convertToIndependantLineDelimiter(actual);
    }
    if (!actual.equals(expected)) {
      System.out.println("Expected source in " + getName() + " should be:");
      System.out.println("|" + expected + "|");
      System.out.println(" but it is:");
      System.out.println("|" + actual + "|");
    }
    assertEquals(message, expected, actual);
  }

  private CompilationUnit getCompilationUnit(Class<?> base, String relPath)
      throws DartModelException, IOException, CoreException {
    DartLibraryImpl lib = getDartApp();
    String expected = TestFileUtil.readResource(base, relPath);
    String fileName = relPath.substring(relPath.lastIndexOf('/') + 1);
    IFile file = getOrCreateFile(fileName, expected);
    DefaultWorkingCopyOwner wcopy = DefaultWorkingCopyOwner.getInstance();
    CompilationUnitImpl unit = new CompilationUnitImpl(lib, file, wcopy);
    String actual = unit.getSource();
    assertEquals(expected, actual);
    return unit;
  }

  private CompilationUnit getCompilationUnit(String testName, String baseName)
      throws CoreException, IOException, DartModelException {
    String relPath = "testsource/" + testName + "$" + baseName;
    return getCompilationUnit(getClass(), relPath);
  }

  private IContainer getContainer() throws CoreException {
    if (container == null) {
      container = TestFileUtil.getOrCreateFolder(getProject(), "src");
    }
    return container;
  }

  private DartLibraryImpl getDartApp() throws CoreException, IOException {
    return new DartLibraryImpl(getDartProject(), getOrCreateFile("Sample.app"));
  }

  private DartModelImpl getDartModel() {
    return DartModelManager.getInstance().getDartModel();
  }

  private DartProjectImpl getDartProject() throws CoreException {
    return new DartProjectImpl(getDartModel(), getProject());
  }

  private String getIn(String compilationUnitName) {
    assertNotNull(compilationUnitName);
    int dotIndex = compilationUnitName.indexOf('.');
    assertTrue(dotIndex != -1);
    return compilationUnitName.substring(0, dotIndex) + IN
        + compilationUnitName.substring(dotIndex);
  }

//  private DartLibrary getLibrary(String libraryName) throws Exception {
//    for (DartLibrary library : DartModelManager.getInstance().getDartModel().getBundledLibraries()) {
//      // library element names are absolute paths, so just match the last segment
//      if (((DartLibraryImpl) library).getLibrarySourceFile().getUri().getPath().endsWith(
//          libraryName)) {
//        return library;
//      }
//    }
//    return null;
//  }

  private IFile getOrCreateFile(String fileName) throws CoreException, IOException {
    return getOrCreateFile(
        fileName,
        TestFileUtil.readResource(getClass(), "testsource/" + fileName));
  }

  private IFile getOrCreateFile(String fileName, String expected) throws CoreException {
    final IFile file = getContainer().getFile(new Path(fileName));
    if (file.exists()) {
      return file;
    }
    final InputStream stream = new ByteArrayInputStream(expected.getBytes());
    TestFileUtil.run(new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        file.create(stream, 0, new NullProgressMonitor());
      }
    });
    return file;
  }

  private String getOut(String compilationUnitName) {
    assertNotNull(compilationUnitName);
    int dotIndex = compilationUnitName.indexOf('.');
    assertTrue(dotIndex != -1);
    return compilationUnitName.substring(0, dotIndex) + OUT
        + compilationUnitName.substring(dotIndex);
  }

  private IProject getProject() throws CoreException {
    if (project == null) {
      project = TestFileUtil.getOrCreateDartProject(getClass().getSimpleName());
    }
    return project;
  }

  private String runFormatter(CodeFormatter codeFormatter, String source, int kind,
      int indentationLevel, int offset, int length, String lineSeparator, boolean repeat) {
    TextEdit edit = codeFormatter.format(
        kind,
        source,
        offset,
        length,
        indentationLevel,
        lineSeparator);//$NON-NLS-1$
    if (edit == null) {
      return null;
    }
    String result = editedString(source, edit);
    if (repeat && length == source.length()) {
      edit = codeFormatter.format(kind, result, 0, result.length(), indentationLevel, lineSeparator);//$NON-NLS-1$
      if (edit == null) {
        return null;
      }
      final String result2 = editedString(result, edit);
      if (!result.equals(result2)) {
        assertSourceEquals(
            "Second formatting is different from first one!",
            convertToIndependantLineDelimiter(result),
            convertToIndependantLineDelimiter(result2));
      }
    }
    try {
      getOrCreateFile("SampleOutput", result);
    } catch (Exception ex) {
      // ignore it -- this is just for comparing large output files
    }
    return result;
  }

//  private boolean runLibraryTest(String testName, CodeFormatter codeFormatter, String source,
//      int kind, int indentationLevel, int offset, int length, String lineSeparator) {
//    try {
//      assertNotNull(source);
//      String result;
//      if (length == -1) {
//        result = runFormatter(
//            codeFormatter,
//            source,
//            kind,
//            indentationLevel,
//            offset,
//            source.length(),
//            lineSeparator,
//            true);
//      } else {
//        result = runFormatter(
//            codeFormatter,
//            source,
//            kind,
//            indentationLevel,
//            offset,
//            length,
//            lineSeparator,
//            true);
//      }
//      if (result == null) {
//        System.err.println("Test Failed: " + testName);
//        return false;
//      }
//    } catch (Throwable t) {
//      System.err.println("Test failed: " + testName);
//      return false;
//    }
//    return true;
//  }

  private void runTest(CodeFormatter codeFormatter, String testName, String compilationUnitName,
      int kind, int indentationLevel) {
    runTest(codeFormatter, testName, compilationUnitName, kind, indentationLevel, false, 0, -1);
  }

  private void runTest(CodeFormatter codeFormatter, String testName, String compilationUnitName,
      int kind, int indentationLevel, boolean checkNull, int offset, int length) {
    runTest(
        codeFormatter,
        testName,
        compilationUnitName,
        kind,
        indentationLevel,
        checkNull,
        offset,
        length,
        null);
  }

  private void runTest(CodeFormatter codeFormatter, String testName, String compilationUnitName,
      int kind, int indentationLevel, boolean checkNull, int offset, int length,
      String lineSeparator) {
    try {
      CompilationUnit sourceUnit = getCompilationUnit(testName, getIn(compilationUnitName));
      String source = sourceUnit.getSource();
      assertNotNull(source);
      CompilationUnit outputUnit = getCompilationUnit(testName, getOut(compilationUnitName));
      assertNotNull(outputUnit);
      String result;
      if (length == -1) {
        result = runFormatter(
            codeFormatter,
            source,
            kind,
            indentationLevel,
            offset,
            source.length(),
            lineSeparator,
            true);
      } else {
        result = runFormatter(
            codeFormatter,
            source,
            kind,
            indentationLevel,
            offset,
            length,
            lineSeparator,
            true);
      }
      assertLineEquals(result, source, outputUnit.getSource(), checkNull);
    } catch (DartModelException e) {
      e.printStackTrace();
      assertTrue(false);
    } catch (CoreException e) {
      e.printStackTrace();
      assertTrue(false);
    } catch (IOException e) {
      e.printStackTrace();
      assertTrue(false);
    }
  }

  private void runTest(String testName, String compilationUnitName) {
    DefaultCodeFormatterOptions preferences = new DefaultCodeFormatterOptions(
        DefaultCodeFormatterConstants.getDartConventionsSettings());
    preferences.number_of_empty_lines_to_preserve = 0;
    preferences.blank_lines_before_imports = 0;
    preferences.blank_lines_after_imports = 0;
    DartCodeFormatter codeFormatter = new DartCodeFormatter(preferences);
//    DefaultCodeFormatter codeFormatter = new DefaultCodeFormatter(preferences);
    runTest(codeFormatter, testName, compilationUnitName, CodeFormatter.K_COMPILATION_UNIT, 0);
  }

}
