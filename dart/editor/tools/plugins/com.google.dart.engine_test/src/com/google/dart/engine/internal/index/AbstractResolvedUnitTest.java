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
package com.google.dart.engine.internal.index;

import com.google.common.collect.Sets;
import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.EngineTestCase;
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
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.utilities.io.FileUtilities2;

import static org.fest.assertions.Assertions.assertThat;

import java.io.File;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract base for testing resolved {@link CompilationUnit}s.
 */
public class AbstractResolvedUnitTest extends EngineTestCase {
  private static final DartSdk defaultSdk = DartSdk.getDefaultSdk();
  private static final SourceFactory sourceFactory = new SourceFactory(new DartUriResolver(
      defaultSdk));
  private static AnalysisContext ANALYSIS_CONTEXT;
  private static Source SOURCE;

  /**
   * @return the resolved {@link CompilationUnit} for given Dart code.
   */
  public static CompilationUnit parseUnit(String code) throws Exception {
    // initialize AnslysisContext
    if (ANALYSIS_CONTEXT == null) {
      ANALYSIS_CONTEXT = AnalysisEngine.getInstance().createAnalysisContext();
      ANALYSIS_CONTEXT.setSourceFactory(sourceFactory);
      // use single Source
      SOURCE = new FileBasedSource(sourceFactory, FileUtilities2.createFile("/Test.dart"));
      {
        ChangeSet changeSet = new ChangeSet();
        changeSet.added(SOURCE);
        ANALYSIS_CONTEXT.applyChanges(changeSet);
      }
    }
    // update source
    {
      ChangeSet changeSet = new ChangeSet();
      changeSet.changed(SOURCE);
      ANALYSIS_CONTEXT.applyChanges(changeSet);
      ANALYSIS_CONTEXT.getSourceFactory().setContents(SOURCE, code);
    }
    // parse and resolve
    LibraryElement library = ANALYSIS_CONTEXT.getLibraryElement(SOURCE);
    CompilationUnit libraryUnit = ANALYSIS_CONTEXT.resolve(SOURCE, library);
    return libraryUnit;
  }

  private final Set<Source> sourceWithSetContent = Sets.newHashSet();

  protected String testCode;
  protected Source testSource;
  protected CompilationUnit testUnit;
  protected CompilationUnitElement unitElement;
  protected LibraryElement libraryElement;

  protected boolean verifyNoTestUnitErrors = true;

  /**
   * Find node in {@link #testUnit} parsed form {@link #testCode}.
   */
  protected final <T extends ASTNode> T findNode(final Class<T> clazz, String pattern) {
    final int index = getOffset(pattern);
    final AtomicReference<T> result = new AtomicReference<T>();
    testUnit.accept(new GeneralizingASTVisitor<Void>() {
      @Override
      @SuppressWarnings("unchecked")
      public Void visitNode(ASTNode node) {
        if (node.getOffset() <= index && index < node.getOffset() + node.getLength()
            && clazz.isInstance(node)) {
          result.set((T) node);
        }
        return super.visitNode(node);
      }
    });
    return result.get();
  }

  protected final SimpleIdentifier findSimpleIdentifier(String pattern) {
    return findNode(SimpleIdentifier.class, pattern);
  }

  /**
   * @return the {@link Element} if {@link SimpleIdentifier} at position of "pattern", not
   *         <code>null</code> or fails.
   */
  @SuppressWarnings("unchecked")
  protected final <T extends Element> T getElement(String pattern) {
    Element element = findSimpleIdentifier(pattern).getElement();
    assertNotNull(element);
    return (T) element;
  }

  /**
   * @return the existing offset of the given "pattern" in {@link #testCode}.
   */
  protected final int getOffset(String pattern) {
    int offset = testCode.indexOf(pattern);
    assertThat(offset).describedAs(testCode).isNotEqualTo(-1);
    return offset;
  }

  /**
   * Sets {@link #testUnit} with mocked {@link Source} which has given code.
   */
  protected final void parseTestUnit(String... lines) throws Exception {
    testCode = createSource(lines);
    testUnit = parseUnit(testCode);
    testSource = testUnit.getElement().getSource();
    // TODO(scheglov) restore once "duplicate" fixed in resolver
//    if (verifyNoTestUnitErrors) {
//      assertThat(testUnit.getParsingErrors()).isEmpty();
//      assertThat(testUnit.getResolutionErrors()).isEmpty();
//    }
    unitElement = testUnit.getElement();
    libraryElement = unitElement.getEnclosingElement();
  }

  /**
   * Configures {@link SourceFactory} to use given content for file at given path.
   */
  protected final void setFileContent(String path, String content) {
    String absolutePath = "/" + path;
    FileBasedSource setSource = new FileBasedSource(sourceFactory, new File(absolutePath));
    sourceWithSetContent.add(setSource);
    sourceFactory.setContents(setSource, content);
  }

  @Override
  protected void tearDown() throws Exception {
    for (Source source : sourceWithSetContent) {
      sourceFactory.setContents(source, null);
    }
    super.tearDown();
  }
}
