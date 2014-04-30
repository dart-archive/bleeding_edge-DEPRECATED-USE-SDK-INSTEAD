/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.engine.internal.context;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContextStatistics;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisErrorInfo;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.context.AnalysisOptions;
import com.google.dart.engine.context.AnalysisResult;
import com.google.dart.engine.context.ChangeNotice;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.internal.cache.SourceEntry;
import com.google.dart.engine.internal.element.angular.AngularApplication;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.Source.ContentReceiver;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.engine.utilities.source.LineInfo;

import java.util.List;
import java.util.Map;

public class InstrumentedAnalysisContextImplTest extends EngineTestCase {
  public void test_addSourceInfo() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public void addSourceInfo(Source source, SourceEntry info) {
            invoked[0] = true;
          }
        });
    context.addSourceInfo(null, null);
    assertTrue(invoked[0]);
  }

  public void test_applyChanges() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public void applyChanges(ChangeSet changeSet) {
            invoked[0] = true;
          }
        });
    context.applyChanges(null);
    assertTrue(invoked[0]);
  }

  public void test_computeDocumentationComment() throws AnalysisException {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public String computeDocumentationComment(Element element) throws AnalysisException {
            invoked[0] = true;
            return null;
          }
        });
    context.computeDocumentationComment(null);
    assertTrue(invoked[0]);
  }

  public void test_computeErrors() throws AnalysisException {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public AnalysisError[] computeErrors(Source source) throws AnalysisException {
            invoked[0] = true;
            return AnalysisError.NO_ERRORS;
          }
        });
    context.computeErrors(null);
    assertTrue(invoked[0]);
  }

  public void test_computeExportedLibraries() throws AnalysisException {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public Source[] computeExportedLibraries(Source source) throws AnalysisException {
            invoked[0] = true;
            return null;
          }
        });
    context.computeExportedLibraries(null);
    assertTrue(invoked[0]);
  }

  public void test_computeHtmlElement() throws AnalysisException {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public HtmlElement computeHtmlElement(Source source) throws AnalysisException {
            invoked[0] = true;
            return null;
          }
        });
    context.computeHtmlElement(null);
    assertTrue(invoked[0]);
  }

  public void test_computeImportedLibraries() throws AnalysisException {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public Source[] computeImportedLibraries(Source source) throws AnalysisException {
            invoked[0] = true;
            return null;
          }
        });
    context.computeImportedLibraries(null);
    assertTrue(invoked[0]);
  }

  public void test_computeKindOf() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public SourceKind computeKindOf(Source source) {
            invoked[0] = true;
            return null;
          }
        });
    context.computeKindOf(null);
    assertTrue(invoked[0]);
  }

  public void test_computeLibraryElement() throws AnalysisException {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public LibraryElement computeLibraryElement(Source source) throws AnalysisException {
            invoked[0] = true;
            return null;
          }
        });
    context.computeLibraryElement(null);
    assertTrue(invoked[0]);
  }

  public void test_computeLineInfo() throws AnalysisException {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public LineInfo computeLineInfo(Source source) throws AnalysisException {
            invoked[0] = true;
            return null;
          }
        });
    context.computeLineInfo(null);
    assertTrue(invoked[0]);
  }

  public void test_computeResolvableCompilationUnit() throws AnalysisException {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public ResolvableCompilationUnit computeResolvableCompilationUnit(Source source)
              throws AnalysisException {
            invoked[0] = true;
            return null;
          }
        });
    context.computeResolvableCompilationUnit(null);
    assertTrue(invoked[0]);
  }

  public void test_creation() {
    assertNotNull(new InstrumentedAnalysisContextImpl());
  }

  public void test_dispose() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public void dispose() {
            invoked[0] = true;
          }
        });
    context.dispose();
    assertTrue(invoked[0]);
  }

  public void test_exists() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public boolean exists(Source source) {
            invoked[0] = true;
            return false;
          }
        });
    context.exists(null);
    assertTrue(invoked[0]);
  }

  public void test_extractContext() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public InternalAnalysisContext extractContextInto(SourceContainer container,
              InternalAnalysisContext newContext) {
            invoked[0] = true;
            return null;
          }
        });
    context.extractContext(null);
    assertTrue(invoked[0]);
  }

  public void test_extractContextInto() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public InternalAnalysisContext extractContextInto(SourceContainer container,
              InternalAnalysisContext newContext) {
            invoked[0] = true;
            return null;
          }
        });
    context.extractContextInto(null, null);
    assertTrue(invoked[0]);
  }

  public void test_getAnalysisOptions() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public AnalysisOptions getAnalysisOptions() {
            invoked[0] = true;
            return null;
          }
        });
    context.getAnalysisOptions();
    assertTrue(invoked[0]);
  }

  public void test_getAngularApplicationWithHtml() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public AngularApplication getAngularApplicationWithHtml(Source htmlSource) {
            invoked[0] = true;
            return null;
          }
        });
    context.getAngularApplicationWithHtml(null);
    assertTrue(invoked[0]);
  }

  public void test_getCompilationUnitElement() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public CompilationUnitElement getCompilationUnitElement(Source unitSource,
              Source librarySource) {
            invoked[0] = true;
            return null;
          }
        });
    context.getCompilationUnitElement(null, null);
    assertTrue(invoked[0]);
  }

  public void test_getContents() throws Exception {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public TimestampedData<CharSequence> getContents(Source source) throws Exception {
            invoked[0] = true;
            return null;
          }
        });
    context.getContents(null);
    assertTrue(invoked[0]);
  }

  public void test_getContentsToReceiver() throws Exception {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public void getContentsToReceiver(Source source, ContentReceiver receiver)
              throws Exception {
            invoked[0] = true;
          }
        });
    context.getContentsToReceiver(null, null);
    assertTrue(invoked[0]);
  }

  public void test_getElement() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public Element getElement(ElementLocation location) {
            invoked[0] = true;
            return null;
          }
        });
    context.getElement(null);
    assertTrue(invoked[0]);
  }

  public void test_getErrors() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public AnalysisErrorInfo getErrors(Source source) {
            invoked[0] = true;
            return new AnalysisErrorInfoImpl(AnalysisError.NO_ERRORS, null);
          }
        });
    context.getErrors(null);
    assertTrue(invoked[0]);
  }

  public void test_getHtmlElement() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public HtmlElement getHtmlElement(Source source) {
            invoked[0] = true;
            return null;
          }
        });
    context.getHtmlElement(null);
    assertTrue(invoked[0]);
  }

  public void test_getHtmlFilesReferencing() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public Source[] getHtmlFilesReferencing(Source source) {
            invoked[0] = true;
            return Source.EMPTY_ARRAY;
          }
        });
    context.getHtmlFilesReferencing(null);
    assertTrue(invoked[0]);
  }

  public void test_getHtmlSources() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public Source[] getHtmlSources() {
            invoked[0] = true;
            return Source.EMPTY_ARRAY;
          }
        });
    context.getHtmlSources();
    assertTrue(invoked[0]);
  }

  public void test_getKindOf() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public SourceKind getKindOf(Source source) {
            invoked[0] = true;
            return null;
          }
        });
    context.getKindOf(null);
    assertTrue(invoked[0]);
  }

  public void test_getLaunchableClientLibrarySources() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public Source[] getLaunchableClientLibrarySources() {
            invoked[0] = true;
            return Source.EMPTY_ARRAY;
          }
        });
    context.getLaunchableClientLibrarySources();
    assertTrue(invoked[0]);
  }

  public void test_getLaunchableServerLibrarySources() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public Source[] getLaunchableServerLibrarySources() {
            invoked[0] = true;
            return Source.EMPTY_ARRAY;
          }
        });
    context.getLaunchableServerLibrarySources();
    assertTrue(invoked[0]);
  }

  public void test_getLibrariesContaining() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public Source[] getLibrariesContaining(Source source) {
            invoked[0] = true;
            return Source.EMPTY_ARRAY;
          }
        });
    context.getLibrariesContaining(null);
    assertTrue(invoked[0]);
  }

  public void test_getLibrariesDependingOn() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public Source[] getLibrariesDependingOn(Source librarySource) {
            invoked[0] = true;
            return Source.EMPTY_ARRAY;
          }
        });
    context.getLibrariesDependingOn(null);
    assertTrue(invoked[0]);
  }

  public void test_getLibrariesReferencedFromHtml() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public Source[] getLibrariesReferencedFromHtml(Source htmlSource) {
            invoked[0] = true;
            return null;
          }
        });
    context.getLibrariesReferencedFromHtml(null);
    assertTrue(invoked[0]);
  }

  public void test_getLibraryElement() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public LibraryElement getLibraryElement(Source source) {
            invoked[0] = true;
            return null;
          }
        });
    context.getLibraryElement(null);
    assertTrue(invoked[0]);
  }

  public void test_getLibrarySources() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public Source[] getLibrarySources() {
            invoked[0] = true;
            return Source.EMPTY_ARRAY;
          }
        });
    context.getLibrarySources();
    assertTrue(invoked[0]);
  }

  public void test_getLineInfo() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public LineInfo getLineInfo(Source source) {
            invoked[0] = true;
            return null;
          }
        });
    context.getLineInfo(null);
    assertTrue(invoked[0]);
  }

  public void test_getModificationStamp() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public long getModificationStamp(Source source) {
            invoked[0] = true;
            return 0L;
          }
        });
    context.getModificationStamp(null);
    assertTrue(invoked[0]);
  }

  public void test_getPublicNamespace() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public Namespace getPublicNamespace(LibraryElement library) {
            invoked[0] = true;
            return null;
          }
        });
    context.getPublicNamespace(null);
    assertTrue(invoked[0]);
  }

  public void test_getRefactoringUnsafeSources() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public Source[] getRefactoringUnsafeSources() {
            invoked[0] = true;
            return null;
          }
        });
    context.getRefactoringUnsafeSources();
    assertTrue(invoked[0]);
  }

  public void test_getResolvedCompilationUnit_element() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public CompilationUnit getResolvedCompilationUnit(Source unitSource,
              LibraryElement library) {
            invoked[0] = true;
            return null;
          }
        });
    context.getResolvedCompilationUnit(null, (LibraryElement) null);
    assertTrue(invoked[0]);
  }

  public void test_getResolvedCompilationUnit_source() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public CompilationUnit getResolvedCompilationUnit(Source unitSource, Source librarySource) {
            invoked[0] = true;
            return null;
          }
        });
    context.getResolvedCompilationUnit(null, (Source) null);
    assertTrue(invoked[0]);
  }

  public void test_getResolvedHtmlUnit() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public HtmlUnit getResolvedHtmlUnit(Source htmlSource) {
            invoked[0] = true;
            return null;
          }
        });
    context.getResolvedHtmlUnit(null);
    assertTrue(invoked[0]);
  }

  public void test_getSourceFactory() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public SourceFactory getSourceFactory() {
            invoked[0] = true;
            return null;
          }
        });
    context.getSourceFactory();
    assertTrue(invoked[0]);
  }

  public void test_getStatistics() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public AnalysisContextStatistics getStatistics() {
            invoked[0] = true;
            return null;
          }
        });
    context.getStatistics();
    assertTrue(invoked[0]);
  }

  public void test_getTypeProvider() throws AnalysisException {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public TypeProvider getTypeProvider() throws AnalysisException {
            invoked[0] = true;
            return null;
          }
        });
    context.getTypeProvider();
    assertTrue(invoked[0]);
  }

  public void test_isClientLibrary() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public boolean isClientLibrary(Source librarySource) {
            invoked[0] = true;
            return false;
          }
        });
    context.isClientLibrary(null);
    assertTrue(invoked[0]);
  }

  public void test_isDisposed() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public boolean isDisposed() {
            invoked[0] = true;
            return false;
          }
        });
    context.isDisposed();
    assertTrue(invoked[0]);
  }

  public void test_isServerLibrary() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public boolean isServerLibrary(Source librarySource) {
            invoked[0] = true;
            return false;
          }
        });
    context.isServerLibrary(null);
    assertTrue(invoked[0]);
  }

  public void test_mergeContext() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public void mergeContext(AnalysisContext context) {
            invoked[0] = true;
          }
        });
    context.mergeContext(new InstrumentedAnalysisContextImpl(new TestAnalysisContext()));
    assertTrue(invoked[0]);
  }

  public void test_parseCompilationUnit() throws AnalysisException {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public CompilationUnit parseCompilationUnit(Source source) throws AnalysisException {
            invoked[0] = true;
            return null;
          }
        });
    context.parseCompilationUnit(null);
    assertTrue(invoked[0]);
  }

  public void test_parseHtmlUnit() throws AnalysisException {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public HtmlUnit parseHtmlUnit(Source source) throws AnalysisException {
            invoked[0] = true;
            return null;
          }
        });
    context.parseHtmlUnit(null);
    assertTrue(invoked[0]);
  }

  public void test_performAnalysisTask() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public AnalysisResult performAnalysisTask() {
            invoked[0] = true;
            return new AnalysisResult(new ChangeNotice[0], 0L, null, 0L);
          }
        });
    context.performAnalysisTask();
    assertTrue(invoked[0]);
  }

  public void test_recordLibraryElements() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public void recordLibraryElements(Map<Source, LibraryElement> elementMap) {
            invoked[0] = true;
          }
        });
    context.recordLibraryElements(null);
    assertTrue(invoked[0]);
  }

  public void test_resolveCompilationUnit() throws AnalysisException {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public CompilationUnit resolveCompilationUnit(Source unitSource, Source librarySource)
              throws AnalysisException {
            invoked[0] = true;
            return null;
          }
        });
    context.resolveCompilationUnit(null, (Source) null);
    assertTrue(invoked[0]);
  }

  public void test_resolveCompilationUnit_element() throws AnalysisException {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public CompilationUnit resolveCompilationUnit(Source unitSource, LibraryElement library)
              throws AnalysisException {
            invoked[0] = true;
            return null;
          }
        });
    context.resolveCompilationUnit(null, (LibraryElement) null);
    assertTrue(invoked[0]);
  }

  public void test_resolveHtmlUnit() throws AnalysisException {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public HtmlUnit resolveHtmlUnit(Source htmlSource) throws AnalysisException {
            invoked[0] = true;
            return null;
          }
        });
    context.resolveHtmlUnit(null);
    assertTrue(invoked[0]);
  }

  public void test_setAnalysisOptions() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public void setAnalysisOptions(AnalysisOptions options) {
            invoked[0] = true;
          }
        });
    context.setAnalysisOptions(null);
    assertTrue(invoked[0]);
  }

  public void test_setAnalysisPriorityOrder() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public void setAnalysisPriorityOrder(List<Source> sources) {
            invoked[0] = true;
          }
        });
    context.setAnalysisPriorityOrder(null);
    assertTrue(invoked[0]);
  }

  public void test_setChangedContents() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public void setChangedContents(Source source, String contents, int offset, int oldLength,
              int newLength) {
            invoked[0] = true;
          }
        });
    context.setChangedContents(null, null, 0, 0, 0);
    assertTrue(invoked[0]);
  }

  public void test_setContents() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public void setContents(Source source, String contents) {
            invoked[0] = true;
          }
        });
    context.setContents(null, null);
    assertTrue(invoked[0]);
  }

  public void test_setSourceFactory() {
    final boolean[] invoked = {false};
    InstrumentedAnalysisContextImpl context = new InstrumentedAnalysisContextImpl(
        new TestAnalysisContext() {
          @Override
          public void setSourceFactory(SourceFactory factory) {
            invoked[0] = true;
          }
        });
    context.setSourceFactory(null);
    assertTrue(invoked[0]);
  }
}
