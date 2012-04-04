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
package com.google.dart.tools.ui.refactoring;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.test.util.TestProject;
import com.google.dart.tools.ui.internal.refactoring.UserInteractions;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.swt.widgets.Shell;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

/**
 * Abstract test for any refactoring.
 */
public abstract class RefactoringTest extends TestCase {
  /**
   * Asserts that {@link CompilationUnit} has expected content.
   */
  protected static void assertUnitContent(CompilationUnit unit, String... lines) throws Exception {
    assertEquals(makeSource(lines), unit.getSource());
  }

  /**
   * Attempts to find {@link DartElement} at the position of the <code>search</code> string. If
   * position not found, fails the test.
   */
  @SuppressWarnings("unchecked")
  protected static <T extends DartElement> T findElement(CompilationUnit unit, String search)
      throws Exception {
    int index = unit.getSource().indexOf(search);
    assertThat(index).isNotEqualTo(-1);
    DartElement[] elements = unit.codeSelect(index, 0);
    assertThat(elements).hasSize(1);
    return (T) elements[0];
  }

  /**
   * Creates source for given lines, that can be used later in
   * {@link #setUnitContent(String, String)}.
   */
  protected static String getLinesForSource(Iterable<String> lines) {
    StringBuffer buffer = new StringBuffer();
    // lines
    for (String line : lines) {
      buffer.append('"');
      buffer.append(line);
      buffer.append('"');
      buffer.append(",\n");
    }
    // end
    if (buffer.length() > 0) {
      buffer.setLength(buffer.length() - 2);
    }
    return buffer.toString();
  }

  protected static String makeSource(String... lines) {
    return Joiner.on("\n").join(lines);
  }

  /**
   * Prints lines of code to insert into {@link #assertUnitContent(String...)}.
   */
  protected static void printUnitLinesSource(CompilationUnit unit) throws Exception {
    String source = unit.getSource();
    Iterable<String> lines = Splitter.on('\n').split(source);
    System.out.println(getLinesForSource(lines));
  }

  protected final List<String> openInformationMessages = Lists.newArrayList();

  protected final List<String> showStatusMessages = Lists.newArrayList();

  protected boolean showStatusCancel;

  protected TestProject testProject;

  protected CompilationUnit testUnit;

  /**
   * Asserts that <code>Test.dart</code> has expected content.
   */
  protected final void assertTestUnitContent(String... lines) throws Exception {

    assertUnitContent(testUnit, lines);
  }

  /**
   * Attempts to find {@link DartElement} at the position of the <code>search</code> string. If
   * position not found, fails the test.
   */
  protected final <T extends DartElement> T findElement(String search) throws Exception {
    return findElement(testUnit, search);
  }

  /**
   * Prints result of {@link #getEditorLinesSource(AstEditor)} .
   */
  protected final void printTestUnitLinesSource() throws Exception {
    printUnitLinesSource(testUnit);
  }

  /**
   * Sets content of <code>Test.dart</code> unit.
   */
  protected final CompilationUnit setTestUnitContent(String... lines) throws Exception {
    testUnit = setUnitContent("Test.dart", lines);
    return testUnit;
  }

  /**
   * Sets content of the unit with given path.
   */
  protected final CompilationUnit setUnitContent(String path, String... lines) throws Exception {
    return testProject.setUnitContent(path, makeSource(lines));
  }

  @Override
  protected void setUp() throws Exception {
    testProject = new TestProject();
    UserInteractions.openInformation = new UserInteractions.OpenInformation() {
      @Override
      public void open(Shell parent, String title, String message) {
        openInformationMessages.add(message);
      }
    };
    UserInteractions.showStatusDialog = new UserInteractions.ShowStatusDialog() {
      @Override
      public boolean open(RefactoringStatus status, Shell parent, String windowTitle) {
        showStatusMessages.add(status.getMessageMatchingSeverity(IStatus.INFO));
        return showStatusCancel;
      }
    };
    showStatusCancel = true;
  }

  @Override
  protected void tearDown() throws Exception {
    UserInteractions.reset();
    testProject.dispose();
    testProject = null;
  }

}
