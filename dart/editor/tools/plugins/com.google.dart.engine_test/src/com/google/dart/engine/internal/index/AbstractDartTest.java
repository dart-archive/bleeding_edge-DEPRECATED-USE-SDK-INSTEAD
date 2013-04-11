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

package com.google.dart.engine.internal.index;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
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

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class AbstractDartTest extends TestCase {
  protected final static String lineSeparator = System.getProperty("line.separator", "\n");

  private static final DartSdk defaultSdk = DirectoryBasedDartSdk.getDefaultSdk();
  private static final SourceFactory sourceFactory = new SourceFactory(new DartUriResolver(
      defaultSdk));
  private static AnalysisContext ANALYSIS_CONTEXT;

  /**
   * @return {@link ASTNode} which has required offset and type.
   */
  public static <E extends ASTNode> E findNode(ASTNode root, final int offset, final Class<E> clazz) {
    final AtomicReference<E> resultRef = new AtomicReference<E>();
    root.accept(new GeneralizingASTVisitor<Void>() {
      @Override
      @SuppressWarnings("unchecked")
      public Void visitNode(ASTNode node) {
        if (node.getOffset() <= offset && offset < node.getEnd() && clazz.isInstance(node)) {
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
  public static CompilationUnit parseUnit(String path, String code) throws Exception {
    // initialize AnslysisContext
    if (ANALYSIS_CONTEXT == null) {
      ANALYSIS_CONTEXT = AnalysisEngine.getInstance().createAnalysisContext();
      ANALYSIS_CONTEXT.setSourceFactory(sourceFactory);
    }
    // configure Source
    Source source = new FileBasedSource(
        sourceFactory.getContentCache(),
        FileUtilities2.createFile(path));
    {
      sourceFactory.setContents(source, "");
      ChangeSet changeSet = new ChangeSet();
      changeSet.added(source);
      ANALYSIS_CONTEXT.applyChanges(changeSet);
    }
    // update Source
    ANALYSIS_CONTEXT.setContents(source, code);
    // parse and resolve
    LibraryElement library = ANALYSIS_CONTEXT.computeLibraryElement(source);
    CompilationUnit libraryUnit = ANALYSIS_CONTEXT.resolveCompilationUnit(source, library);
    return libraryUnit;
  }

  protected static String makeSource(String... lines) {
    return Joiner.on(lineSeparator).join(lines);
  }

  /**
   * Prints given multi-line source in the way ready to paste back into Java test source.
   */
  protected static void printSourceLines(String source) {
    String[] lines = StringUtils.splitByWholeSeparatorPreserveAllTokens(source, lineSeparator);
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

  /**
   * @return {@link String} with system line separator converted to Unix <code>\n</code>.
   */
  protected static String toUnixEol(String s) {
    return s.replace(lineSeparator, "\n");
  }

  private final Set<Source> sourceWithSetContent = Sets.newHashSet();
  protected boolean verifyNoTestUnitErrors = true;
  protected String testCode;
  protected Source testSource;
  protected CompilationUnit testUnit;
  protected CompilationUnitElement testUnitElement;
  protected LibraryElement testLibraryElement;

  /**
   * @return the {@link Element} if there is {@link SimpleIdentifier} at position of "search", not
   *         {@code null} or fails.
   */
  @SuppressWarnings("unchecked")
  protected final <T extends Element> T findElement(String search) {
    Element element = findSimpleIdentifier(search).getElement();
    assertNotNull(element);
    return (T) element;
  }

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
    return findNode(search, SimpleIdentifier.class);
  }

  /**
   * @return the {@link Element} of the {@link SimpleIdentifier} at the given search pattern.
   */
  @SuppressWarnings("unchecked")
  protected final <T extends Element> T findIdentifierElement(String search) {
    return (T) findIdentifier(search).getElement();
  }

  /**
   * @return {@link ASTNode} form {@link #testUnit} which has required offset and type.
   */
  protected final <E extends ASTNode> E findNode(int offset, Class<E> clazz) {
    return findNode(testUnit, offset, clazz);
  }

  /**
   * @return {@link ASTNode} from {@link #testUnit} which starts at given text has has given type.
   */
  protected final <E extends ASTNode> E findNode(String search, Class<E> clazz) {
    int offset = findOffset(search);
    return findNode(testUnit, offset, clazz);
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
   * @return the {@link SourceRange} for given sub-string. Fails test if not found.
   */
  protected final SourceRange findRange(String search) {
    int start = findOffset(search);
    return SourceRangeFactory.rangeStartLength(start, search.length());
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
   * @return the first {@link SimpleIdentifier} which starts at position of given string.
   */
  protected final SimpleIdentifier findSimpleIdentifier(String pattern) {
    return findNode(pattern, SimpleIdentifier.class);
  }

  /**
   * Sets {@link #testUnit} with mocked {@link Source} which has given code.
   */
  protected final void parseTestUnit(String... lines) throws Exception {
    testCode = makeSource(lines);
    testUnit = parseUnit("/Test.dart", testCode);
    testSource = testUnit.getElement().getSource();
    testUnitElement = testUnit.getElement();
    testLibraryElement = testUnitElement.getEnclosingElement();
    if (verifyNoTestUnitErrors) {
      assertThat(testUnit.getParsingErrors()).isEmpty();
      assertThat(testUnit.getResolutionErrors()).isEmpty();
    }
  }

  /**
   * Configures {@link SourceFactory} to use given content for file at given path.
   * 
   * @return the {@link Source} which corresponds given path.
   */
  protected final Source setFileContent(String path, String content) {
    FileBasedSource source = new FileBasedSource(
        sourceFactory.getContentCache(),
        FileUtilities2.createFile("/" + path));
    sourceWithSetContent.add(source);
    sourceFactory.setContents(source, content);
    return source;
  }

  @Override
  protected void tearDown() throws Exception {
    for (Source source : sourceWithSetContent) {
      sourceFactory.setContents(source, null);
    }
    super.tearDown();
  }
}
