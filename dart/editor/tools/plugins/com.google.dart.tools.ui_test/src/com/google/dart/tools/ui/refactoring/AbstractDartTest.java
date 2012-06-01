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
import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.test.util.TestProject;

import junit.framework.TestCase;

import org.eclipse.swt.widgets.Display;

import static org.fest.assertions.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract base for any Dart test which uses {@link TestProject}.
 */
public abstract class AbstractDartTest extends TestCase {
  /**
   * @return {@link DartNode} which has required offset and type.
   */
  public static <E extends DartNode> E findNode(
      DartNode root,
      final int offset,
      final Class<E> clazz) {
    final AtomicReference<E> result = new AtomicReference<E>();
    root.accept(new ASTVisitor<Void>() {
      @Override
      @SuppressWarnings("unchecked")
      public Void visitNode(DartNode node) {
        if (node.getSourceInfo().getOffset() == offset && clazz.isInstance(node)) {
          result.set((E) node);
        }
        return super.visitNode(node);
      }
    });
    return result.get();
  }

  /**
   * Waits given number of milliseconds and runs events loop every 1 millisecond. At least one
   * events loop will be executed.
   */
  public static void waitEventLoop(int time) {
    waitEventLoop(time, 0);
  }

  /**
   * Waits given number of milliseconds and runs events loop every <code>sleepMillis</code>
   * milliseconds. At least one events loop will be executed.
   */
  public static void waitEventLoop(int time, long sleepMillis) {
    long start = System.currentTimeMillis();
    do {
      try {
        Thread.sleep(sleepMillis);
      } catch (Throwable e) {
      }
      while (Display.getCurrent().readAndDispatch()) {
        // do nothing
      }
    } while (System.currentTimeMillis() - start < time);
  }

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
    int offset = findOffset(unit, search);
    DartElement[] elements = unit.codeSelect(offset, 0);
    assertThat(elements).hasSize(1);
    return (T) elements[0];
  }

  /**
   * @return the offset of given <code>search</code> string. Fails test if not found.
   */
  protected static int findOffset(CompilationUnit unit, String search) throws Exception {
    String source = unit.getSource();
    int offset = source.indexOf(search);
    assertThat(offset).describedAs(source).isNotEqualTo(-1);
    return offset;
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
   * @return the offset of given <code>search</code> string in {@link testUnit}. Fails test if not
   *         found.
   */
  protected final int findOffset(String search) throws Exception {
    return findOffset(testUnit, search);
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
  }

  @Override
  protected void tearDown() throws Exception {
    testProject.dispose();
    testProject = null;
    testUnit = null;
  }

}
