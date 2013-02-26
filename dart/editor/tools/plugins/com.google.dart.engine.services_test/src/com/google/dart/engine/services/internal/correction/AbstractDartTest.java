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

package com.google.dart.engine.services.internal.correction;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.services.status.RefactoringStatusEntry;
import com.google.dart.engine.services.status.RefactoringStatusSeverity;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.utilities.io.FileUtilities2;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.engine.utilities.source.SourceRangeFactory;

import junit.framework.TestCase;

import org.apache.commons.lang3.StringUtils;

import static org.fest.assertions.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

public class AbstractDartTest extends TestCase {
  private static AnalysisContext ANALYSIS_CONTEXT;
  private static Source SOURCE;

  /**
   * @return {@link ASTNode} which has required offset and type.
   */
  public static <E extends ASTNode> E findNode(ASTNode root, final int offset, final Class<E> clazz) {
    final AtomicReference<E> resultRef = new AtomicReference<E>();
    root.accept(new GeneralizingASTVisitor<Void>() {
      @Override
      @SuppressWarnings("unchecked")
      public Void visitNode(ASTNode node) {
        if (node.getOffset() == offset && clazz.isInstance(node)) {
          resultRef.set((E) node);
        }
        return super.visitNode(node);
      }
    });
    E result = resultRef.get();
    assertNotNull(result);
    return result;
  }

  /**
   * Function to force formatter to put every string on separate line.
   */
  public static String[] formatLines(String... lines) {
    return lines;
  }

  /**
   * @return the resolved {@link CompilationUnit} for given Dart code.
   */
  public static CompilationUnit parseUnit(String code) throws Exception {
    // initialize AnslysisContext
    if (ANALYSIS_CONTEXT == null) {
      DartSdk defaultSdk = DartSdk.getDefaultSdk();
      SourceFactory sourceFactory = new SourceFactory(new DartUriResolver(defaultSdk));
      ANALYSIS_CONTEXT = AnalysisEngine.getInstance().createAnalysisContext();
      ANALYSIS_CONTEXT.setSourceFactory(sourceFactory);
      // use single Source
      SOURCE = new FileBasedSource(sourceFactory, FileUtilities2.createFile("/Test.dart"));
    }
    // update source
    {
      ChangeSet changeSet = new ChangeSet();
      changeSet.changed(SOURCE);
      ANALYSIS_CONTEXT.changed(changeSet);
      ANALYSIS_CONTEXT.getSourceFactory().setContents(SOURCE, code);
    }
    // parse and resolve
    LibraryElement library = ANALYSIS_CONTEXT.getLibraryElement(SOURCE);
    CompilationUnit libraryUnit = ANALYSIS_CONTEXT.resolve(SOURCE, library);
    return libraryUnit;
  }

  /**
   * Asserts that given {@link RefactoringStatus} has expected severity and message.
   */
  protected static void assertRefactoringStatus(RefactoringStatus status,
      RefactoringStatusSeverity expectedSeverity, String expectedMessage) {
    assertRefactoringStatus(status, expectedSeverity, expectedMessage, null);
  }

  /**
   * Asserts that given {@link RefactoringStatus} has expected severity and message.
   */
  protected static void assertRefactoringStatus(RefactoringStatus status,
      RefactoringStatusSeverity expectedSeverity, String expectedMessage,
      SourceRange expectedContextRange) {
    assertSame(status.getMessage(), expectedSeverity, status.getSeverity());
    if (expectedSeverity != RefactoringStatusSeverity.OK) {
      RefactoringStatusEntry entry = status.getEntryWithHighestSeverity();
      assertSame(expectedSeverity, entry.getSeverity());
      assertEquals(expectedMessage, entry.getMessage());
      if (expectedContextRange != null) {
        assertEquals(expectedContextRange, entry.getContext().getRange());
      }
    }
  }

  /**
   * Asserts that given {@link RefactoringStatus} is OK.
   */
  protected static void assertRefactoringStatusOK(RefactoringStatus status) {
    assertRefactoringStatus(status, RefactoringStatusSeverity.OK, null);
  }

  protected static String makeSource(String... lines) {
    return Joiner.on("\n").join(lines);
  }

  /**
   * Prints given multi-line source in the way ready to paste back into Java test source.
   */
  protected static void printSourceLines(String source) {
    String[] lines = StringUtils.split(source, '\n');
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      line = StringUtils.replace(line, "\"", "\\\"");
      System.out.print("\"");
      System.out.print(line);
      if (i != lines.length - 1) {
        System.out.println("\",");
      } else {
        System.out.println("\"");
      }
    }
  }

  protected String testCode;
  protected Source testSource;

  protected CompilationUnit testUnit;

  protected boolean verifyNoTestUnitErrors = true;

  /**
   * @return the offset directly after given <code>search</code> string in {@link testUnit}. Fails
   *         test if not found.
   */
  protected final int findEnd(String search) {
    return findOffset(search) + search.length();
  }

  /**
   * @return the {@link SimpleIdentifier} at the given search pattern.
   */
  protected final SimpleIdentifier findIdentifier(String search) {
    return findTestNode(search, SimpleIdentifier.class);
  }

  /**
   * @return the {@link Element} of the {@link SimpleIdentifier} at the given search pattern.
   */
  @SuppressWarnings("unchecked")
  protected final <T extends Element> T findIdentifierElement(String search) {
    return (T) findIdentifier(search).getElement();
  }

  /**
   * @return the offset of given <code>search</code> string in {@link testUnit}. Fails test if not
   *         found.
   */
  protected final int findOffset(String search) {
    int offset = testCode.indexOf(search);
    assertThat(offset).describedAs(testCode).isNotEqualTo(-1);
    return offset;
  }

  /**
   * @return the {@link SourceRange} for given start/end search strings. Fails test if not found.
   */
  protected final SourceRange findRangeIdentifier(String search) {
    int start = findOffset(search);
    int end = CharMatcher.JAVA_LETTER_OR_DIGIT.negate().indexIn(testCode, start);
    return SourceRangeFactory.rangeStartEnd(start, end);
  }

  /**
   * @return the {@link SourceRange} for given start/end search strings. Fails test if not found.
   */
  protected final SourceRange findRangeStartEnd(String searchStart, String searchEnd) {
    return SourceRangeFactory.rangeStartEnd(findOffset(searchStart), findOffset(searchEnd));
  }

  /**
   * @return {@link ASTNode} which has required offset and type.
   */
  protected final <E extends ASTNode> E findTestNode(int offset, Class<E> clazz) {
    return findNode(testUnit, offset, clazz);
  }

  /**
   * @return {@link ASTNode} which starts at given text has has given type.
   */
  protected final <E extends ASTNode> E findTestNode(String search, Class<E> clazz) {
    int offset = findOffset(search);
    return findNode(testUnit, offset, clazz);
  }

  /**
   * @return the {@link CorrectionUtils} for {@link #testUnit}.
   */
  protected final CorrectionUtils getTestCorrectionUtils() throws Exception {
    return new CorrectionUtils(testUnit);
  }

  /**
   * Sets {@link #testUnit} with mocked {@link Source} which has given code.
   */
  protected final void parseTestUnit(String... lines) throws Exception {
    testCode = makeSource(lines);
    testUnit = parseUnit(testCode);
    testSource = testUnit.getElement().getSource();
    if (verifyNoTestUnitErrors) {
      assertThat(testUnit.getParsingErrors()).isEmpty();
      assertThat(testUnit.getResolutionErrors()).isEmpty();
    }
  }
}
