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

import com.google.common.base.Joiner;
import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;

import junit.framework.TestCase;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.concurrent.atomic.AtomicReference;

public class AbstractDartTest extends TestCase {
  private static AnalysisContext ANALYSIS_CONTEXT;
  private static Source SOURCE;

  /**
   * @return {@link ASTNode} which has required offset and type.
   */
  public static <E extends ASTNode> E findNode(ASTNode root, final int offset, final Class<E> clazz) {
    final AtomicReference<E> result = new AtomicReference<E>();
    root.accept(new GeneralizingASTVisitor<Void>() {
      @Override
      @SuppressWarnings("unchecked")
      public Void visitNode(ASTNode node) {
        if (node.getOffset() == offset && clazz.isInstance(node)) {
          result.set((E) node);
        }
        return super.visitNode(node);
      }
    });
    return result.get();
  }

  /**
   * @return the {@link CompilationUnit} with mocked {@link Source} which has given code.
   */
  public static CompilationUnit parseUnit(final String code) throws Exception {
    // initialize AnslysisContext
    if (ANALYSIS_CONTEXT == null) {
      DartSdk defaultSdk = DartSdk.getDefaultSdk();
      ANALYSIS_CONTEXT = AnalysisEngine.getInstance().createAnalysisContext();;
      ANALYSIS_CONTEXT.setSourceFactory(new SourceFactory(new DartUriResolver(defaultSdk)));
      // use single Source
      SOURCE = mock(Source.class);
    }
    ANALYSIS_CONTEXT.sourceChanged(SOURCE);
    // mock Source content
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        ((Source.ContentReceiver) invocation.getArguments()[0]).accept(code);
        return null;
      }
    }).when(SOURCE).getContents(any(Source.ContentReceiver.class));
    //
    LibraryElement library = ANALYSIS_CONTEXT.getLibraryElement(SOURCE);
    //
    return ANALYSIS_CONTEXT.resolve(SOURCE, library);
    // parse
//    CompilationUnit compilationUnit = ParserTestCase.parseCompilationUnit(code);
//    // set CompilationUnitElement mock with Source mock
//    {
//      CompilationUnitElement element = mock(CompilationUnitElement.class);
//      when(element.getSource()).thenReturn(source);
//      compilationUnit.setElement(element);
//    }
//    new CompilationUnitBuilder(null, null).buildCompilationUnit(source, compilationUnit);
    // done
//    return compilationUnit;
  }

  protected static String makeSource(String... lines) {
    return Joiner.on("\n").join(lines);
  }

  protected String testCode;
  protected Source testSource;
  protected CompilationUnit testUnit;

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
  }

}
