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
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.internal.context.AnalysisOptionsImpl;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.utilities.io.FileUtilities2;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.engine.utilities.source.SourceRangeFactory;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import junit.framework.TestCase;

import org.apache.commons.lang3.StringUtils;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class AbstractDartTest extends TestCase {
  protected final static String EOL = System.getProperty("line.separator", "\n");
  protected final static String EOL2 = EOL + EOL;

  protected static final DartSdk defaultSdk = DirectoryBasedDartSdk.getDefaultSdk();
  protected static final SourceFactory sourceFactory = new SourceFactory(new DartUriResolver(
      defaultSdk), new FileUriResolver());
  protected static AnalysisContext analysisContext;

  /**
   * @return {@link AstNode} which has required offset and type.
   */
  public static <E extends AstNode> E findNode(AstNode root, final int offset, final Class<E> clazz) {
    final AtomicReference<E> resultRef = new AtomicReference<E>();
    root.accept(new GeneralizingAstVisitor<Void>() {
      @Override
      @SuppressWarnings("unchecked")
      public Void visitNode(AstNode node) {
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
   * @return the {@link String} content of the given {@link Source}.
   */
  public static String getSourceContent(Source source) throws Exception {
    return analysisContext.getContents(source).getData().toString();
  }

  /**
   * @return the resolved {@link CompilationUnit} for given source.
   */
  public static CompilationUnit parseUnit(Source source) throws Exception {
    // parse and resolve
    LibraryElement library = analysisContext.computeLibraryElement(source);
    CompilationUnit libraryUnit = analysisContext.resolveCompilationUnit(source, library);
    return libraryUnit;
  }

  /**
   * @return the resolved {@link CompilationUnit} for given Dart code.
   */
  public static CompilationUnit parseUnit(String path, String code) throws Exception {
    ensureAnalysisContext();
    // configure Source
    Source source = new FileBasedSource(FileUtilities2.createFile(path));
    ChangeSet changeSet = new ChangeSet();
    changeSet.addedSource(source);
    analysisContext.applyChanges(changeSet);
    analysisContext.setContents(source, code);
    // parse and resolve
    LibraryElement library = analysisContext.computeLibraryElement(source);
    CompilationUnit libraryUnit = analysisContext.resolveCompilationUnit(source, library);
    return libraryUnit;
  }

  protected static void disableContextHints() {
    ensureAnalysisContext();
    AnalysisOptionsImpl options = new AnalysisOptionsImpl(analysisContext.getAnalysisOptions());
    options.setHint(false);
    analysisContext.setAnalysisOptions(options);
  }

  protected static void enableContextHints() {
    ensureAnalysisContext();
    AnalysisOptionsImpl options = new AnalysisOptionsImpl(analysisContext.getAnalysisOptions());
    options.setHint(true);
    analysisContext.setAnalysisOptions(options);
  }

  /**
   * Ensure that {@link #analysisContext} is initialized.
   */
  protected static void ensureAnalysisContext() {
    if (analysisContext == null) {
      analysisContext = AnalysisEngine.getInstance().createAnalysisContext();
      analysisContext.setSourceFactory(sourceFactory);
      AnalysisOptionsImpl analysisOptionsImpl = new AnalysisOptionsImpl();
      analysisOptionsImpl.setHint(false);
      analysisContext.setAnalysisOptions(analysisOptionsImpl);
    }
  }

  /**
   * @return the offset of given <code>search</code> string in the given code. Fails test if not
   *         found.
   */
  protected static int findOffset(String code, String search) {
    int offset = code.indexOf(search);
    assertThat(offset).describedAs(code).isNotEqualTo(-1);
    return offset;
  }

  /**
   * @return the {@link SourceRange} for given start/end search strings. Fails test if not found.
   */
  protected static SourceRange findRangeIdentifier(String code, String search) {
    int start = findOffset(code, search);
    int end = CharMatcher.JAVA_LETTER_OR_DIGIT.negate().indexIn(code, start);
    return SourceRangeFactory.rangeStartEnd(start, end);
  }

  protected static String makeSource(String... lines) {
    return Joiner.on(EOL).join(lines);
  }

  /**
   * Prints given multi-line source in the way ready to paste back into Java test source.
   */
  protected static void printSourceLines(String source) {
    String[] lines = StringUtils.splitByWholeSeparatorPreserveAllTokens(source, EOL);
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
    return s.replace(EOL, "\n");
  }

  private final Set<Source> sourceWithSetContent = Sets.newHashSet();

  protected boolean verifyNoTestUnitErrors = true;
  protected String testCode;
  protected Source testSource;
  protected CompilationUnit testUnit;
  protected CompilationUnitElement testUnitElement;
  protected LibraryElement testLibraryElement;

  /**
   * Add a source file to the content provider.
   * 
   * @param contents the contents to be returned by the content provider for the specified file
   * @return the source object representing the added file
   */
  protected Source addSource(String contents) {
    return addSource("/test.dart", contents);
  }

  /**
   * Add a source file to the content provider. The file path should be absolute.
   * 
   * @param filePath the path of the file being added
   * @param contents the contents to be returned by the content provider for the specified file
   * @return the source object representing the added file
   */
  protected Source addSource(String filePath, String contents) {
    ensureAnalysisContext();
    Source source = new FileBasedSource(createFile(filePath));
    // add Source to the context
    ChangeSet changeSet = new ChangeSet();
    changeSet.addedSource(source);
    analysisContext.applyChanges(changeSet);
    analysisContext.setContents(source, contents);
    // remember Source to remove from the context later
    sourceWithSetContent.add(source);
    // done
    return source;
  }

  /**
   * @return the {@link Element} if there is {@link SimpleIdentifier} at position of "search", not
   *         {@code null} or fails.
   */
  @SuppressWarnings("unchecked")
  protected final <T extends Element> T findElement(String search) {
    Element element = findSimpleIdentifier(search).getBestElement();
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
    return (T) findIdentifier(search).getBestElement();
  }

  /**
   * @return {@link AstNode} form {@link #testUnit} which has required offset and type.
   */
  protected final <E extends AstNode> E findNode(int offset, Class<E> clazz) {
    return findNode(testUnit, offset, clazz);
  }

  /**
   * @return {@link AstNode} from {@link #testUnit} which starts at given text has has given type.
   */
  protected final <E extends AstNode> E findNode(String search, Class<E> clazz) {
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

  protected AnalysisContext getAnalysisContext() {
    return analysisContext;
  }

  /**
   * Sets {@link #testUnit} with mocked {@link Source} which has given code.
   */
  protected final void parseTestUnit(Source source) throws Exception {
    testUnit = parseUnit(source);
    initTestFields(testUnit);
  }

  protected final void parseTestUnit(Source libSource, Source unitSource) throws Exception {
    CompilationUnit resolvedUnit = analysisContext.resolveCompilationUnit(unitSource, libSource);
    initTestFields(resolvedUnit);
  }

  /**
   * Sets {@link #testUnit} with mocked {@link Source} which has given code.
   */
  protected final void parseTestUnit(String... lines) throws Exception {
    String code = makeSource(lines);
    testUnit = parseUnit("/Test.dart", code);
    initTestFields(testUnit);
  }

  /**
   * Sets {@link #testUnit} with mocked {@link Source} which has given code.
   */
  protected final void parseTestUnits(Source... sources) throws Exception {
    Source librarySource = sources[0];
    testSource = sources[1];
    testCode = analysisContext.getContents(testSource).getData().toString();
    // fill AnalysisContext
    {
      ChangeSet changeSet = new ChangeSet();
      for (Source source : sources) {
        changeSet.addedSource(source);
      }
      analysisContext.applyChanges(changeSet);
    }
    //
    testLibraryElement = analysisContext.computeLibraryElement(librarySource);
    testUnit = analysisContext.resolveCompilationUnit(testSource, testLibraryElement);
    testUnitElement = testUnit.getElement();
    if (verifyNoTestUnitErrors) {
      assertThat(analysisContext.getErrors(testUnitElement.getSource()).getErrors()).describedAs(
          testCode).isEmpty();
    }
  }

  /**
   * Configures {@link SourceFactory} to use given content for file at given path.
   * 
   * @return the {@link Source} which corresponds given path.
   */
  protected final Source setFileContent(String path, String content) {
    ensureAnalysisContext();
    FileBasedSource source = new FileBasedSource(createFile("/" + path));
    sourceWithSetContent.add(source);
    analysisContext.setContents(source, content);
    return source;
  }

  @Override
  protected void tearDown() throws Exception {
    // reset SourceFactory
    for (Source source : sourceWithSetContent) {
      analysisContext.setContents(source, null);
    }
    // reset AnalysisContext
    if (analysisContext != null) {
      ChangeSet changeSet = new ChangeSet();
      if (testSource != null) {
        changeSet.removedSource(testSource);
      }
      for (Source source : sourceWithSetContent) {
        changeSet.removedSource(source);
      }
      analysisContext.applyChanges(changeSet);
    }
    // clear fields
    testCode = null;
    testSource = null;
    testUnit = null;
    testUnitElement = null;
    testLibraryElement = null;
    // continue
    super.tearDown();
  }

  private void initTestFields(CompilationUnit resolvedUnit) throws Exception {
    testUnit = resolvedUnit;
    testUnitElement = testUnit.getElement();
    testLibraryElement = testUnitElement.getEnclosingElement();
    if (verifyNoTestUnitErrors) {
      assertThat(analysisContext.getErrors(testUnitElement.getSource()).getErrors()).describedAs(
          testCode).isEmpty();
    }
    testSource = testUnitElement.getSource();
    testCode = getSourceContent(testSource);
  }
}
