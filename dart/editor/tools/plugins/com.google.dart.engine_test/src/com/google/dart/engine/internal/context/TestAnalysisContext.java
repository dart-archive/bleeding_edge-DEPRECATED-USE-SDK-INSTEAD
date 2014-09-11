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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.constant.DeclaredVariables;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisContextStatistics;
import com.google.dart.engine.context.AnalysisDelta;
import com.google.dart.engine.context.AnalysisErrorInfo;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.context.AnalysisListener;
import com.google.dart.engine.context.AnalysisOptions;
import com.google.dart.engine.context.AnalysisResult;
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

import static junit.framework.Assert.fail;

import java.util.List;
import java.util.Map;

/**
 * Instances of the class {@code TestAnalysisContext} implement an analysis context in which every
 * method will cause a test to fail when invoked.
 */
public class TestAnalysisContext implements InternalAnalysisContext {
  @Override
  public void addListener(AnalysisListener listener) {
    fail("Unexpected invocation of addListener");
  }

  @Override
  public void addSourceInfo(Source source, SourceEntry info) {
    fail("Unexpected invocation of addSourceInfo");
  }

  @Override
  public void applyAnalysisDelta(AnalysisDelta delta) {
    fail("Unexpected invocation of applyAnalysisDelta");
  }

  @Override
  public void applyChanges(ChangeSet changeSet) {
    fail("Unexpected invocation of applyChanges");
  }

  @Override
  public String computeDocumentationComment(Element element) throws AnalysisException {
    fail("Unexpected invocation of computeDocumentationComment");
    return null;
  }

  @Override
  public AnalysisError[] computeErrors(Source source) throws AnalysisException {
    fail("Unexpected invocation of computeErrors");
    return null;
  }

  @Override
  public Source[] computeExportedLibraries(Source source) throws AnalysisException {
    fail("Unexpected invocation of computeExportedLibraries");
    return null;
  }

  @Override
  public HtmlElement computeHtmlElement(Source source) throws AnalysisException {
    fail("Unexpected invocation of computeHtmlElement");
    return null;
  }

  @Override
  public Source[] computeImportedLibraries(Source source) throws AnalysisException {
    fail("Unexpected invocation of computeImportedLibraries");
    return null;
  }

  @Override
  public SourceKind computeKindOf(Source source) {
    fail("Unexpected invocation of computeKindOf");
    return null;
  }

  @Override
  public LibraryElement computeLibraryElement(Source source) throws AnalysisException {
    fail("Unexpected invocation of computeLibraryElement");
    return null;
  }

  @Override
  public LineInfo computeLineInfo(Source source) throws AnalysisException {
    fail("Unexpected invocation of computeLineInfo");
    return null;
  }

  @Override
  public ResolvableCompilationUnit computeResolvableCompilationUnit(Source source)
      throws AnalysisException {
    fail("Unexpected invocation of computeResolvableCompilationUnit");
    return null;
  }

  @Override
  public void dispose() {
    fail("Unexpected invocation of dispose");
  }

  @Override
  public boolean exists(Source source) {
    fail("Unexpected invocation of exists");
    return false;
  }

  @Override
  public AnalysisContext extractContext(SourceContainer container) {
    fail("Unexpected invocation of extractContext");
    return null;
  }

  @Override
  public InternalAnalysisContext extractContextInto(SourceContainer container,
      InternalAnalysisContext newContext) {
    fail("Unexpected invocation of extractContextInto");
    return null;
  }

  @Override
  public AnalysisOptions getAnalysisOptions() {
    fail("Unexpected invocation of getAnalysisOptions");
    return null;
  }

  @Override
  public AngularApplication getAngularApplicationWithHtml(Source htmlSource) {
    fail("Unexpected invocation of getAngularApplicationWithHtml");
    return null;
  }

  @Override
  public CompilationUnitElement getCompilationUnitElement(Source unitSource, Source librarySource) {
    fail("Unexpected invocation of getCompilationUnitElement");
    return null;
  }

  @Override
  public TimestampedData<CharSequence> getContents(Source source) throws Exception {
    fail("Unexpected invocation of getContents");
    return null;
  }

  @Override
  @Deprecated
  public void getContentsToReceiver(Source source, ContentReceiver receiver) throws Exception {
    fail("Unexpected invocation of getContentsToReceiver");
  }

  @Override
  public InternalAnalysisContext getContextFor(Source source) {
    fail("Unexpected invocation of getContextFor");
    return null;
  }

  @Override
  public DeclaredVariables getDeclaredVariables() {
    fail("Unexpected invocation of getDeclaredVariables");
    return null;
  }

  @Override
  public Element getElement(ElementLocation location) {
    fail("Unexpected invocation of getElement");
    return null;
  }

  @Override
  public AnalysisErrorInfo getErrors(Source source) {
    fail("Unexpected invocation of getErrors");
    return null;
  }

  @Override
  public HtmlElement getHtmlElement(Source source) {
    fail("Unexpected invocation of getHtmlElement");
    return null;
  }

  @Override
  public Source[] getHtmlFilesReferencing(Source source) {
    fail("Unexpected invocation of getHtmlFilesReferencing");
    return null;
  }

  @Override
  public Source[] getHtmlSources() {
    fail("Unexpected invocation of getHtmlSources");
    return null;
  }

  @Override
  public SourceKind getKindOf(Source source) {
    fail("Unexpected invocation of getKindOf");
    return null;
  }

  @Override
  public Source[] getLaunchableClientLibrarySources() {
    fail("Unexpected invocation of getLaunchableClientLibrarySources");
    return null;
  }

  @Override
  public Source[] getLaunchableServerLibrarySources() {
    fail("Unexpected invocation of getLaunchableServerLibrarySources");
    return null;
  }

  @Override
  public Source[] getLibrariesContaining(Source source) {
    fail("Unexpected invocation of getLibrariesContaining");
    return null;
  }

  @Override
  public Source[] getLibrariesDependingOn(Source librarySource) {
    fail("Unexpected invocation of getLibrariesDependingOn");
    return null;
  }

  @Override
  public Source[] getLibrariesReferencedFromHtml(Source htmlSource) {
    fail("Unexpected invocation of getLibrariesReferencedFromHtml");
    return null;
  }

  @Override
  public LibraryElement getLibraryElement(Source source) {
    fail("Unexpected invocation of getLibraryElement");
    return null;
  }

  @Override
  public Source[] getLibrarySources() {
    fail("Unexpected invocation of getLibrarySources");
    return null;
  }

  @Override
  public LineInfo getLineInfo(Source source) {
    fail("Unexpected invocation of getLineInfo");
    return null;
  }

  @Override
  public long getModificationStamp(Source source) {
    fail("Unexpected invocation of getModificationStamp");
    return 0;
  }

  @Override
  public Source[] getPrioritySources() {
    fail("Unexpected invocation of getPrioritySources");
    return null;
  }

  @Override
  public Namespace getPublicNamespace(LibraryElement library) {
    fail("Unexpected invocation of getPublicNamespace");
    return null;
  }

  @Override
  public Source[] getRefactoringUnsafeSources() {
    fail("Unexpected invocation of getRefactoringUnsafeSources");
    return null;
  }

  @Override
  public CompilationUnit getResolvedCompilationUnit(Source unitSource, LibraryElement library) {
    fail("Unexpected invocation of getResolvedCompilationUnit");
    return null;
  }

  @Override
  public CompilationUnit getResolvedCompilationUnit(Source unitSource, Source librarySource) {
    fail("Unexpected invocation of getResolvedCompilationUnit");
    return null;
  }

  @Override
  public HtmlUnit getResolvedHtmlUnit(Source htmlSource) {
    fail("Unexpected invocation of getResolvedHtmlUnit");
    return null;
  }

  @Override
  public SourceFactory getSourceFactory() {
    fail("Unexpected invocation of getSourceFactory");
    return null;
  }

  @Override
  public AnalysisContextStatistics getStatistics() {
    fail("Unexpected invocation of getStatistics");
    return null;
  }

  @Override
  public TypeProvider getTypeProvider() throws AnalysisException {
    fail("Unexpected invocation of getTypeProvider");
    return null;
  }

  @Override
  public boolean isClientLibrary(Source librarySource) {
    fail("Unexpected invocation of isClientLibrary");
    return false;
  }

  @Override
  public boolean isDisposed() {
    fail("Unexpected invocation of isDisposed");
    return false;
  }

  @Override
  public boolean isServerLibrary(Source librarySource) {
    fail("Unexpected invocation of isServerLibrary");
    return false;
  }

  @Override
  public void mergeContext(AnalysisContext context) {
    fail("Unexpected invocation of mergeContext");
  }

  @Override
  public CompilationUnit parseCompilationUnit(Source source) throws AnalysisException {
    fail("Unexpected invocation of parseCompilationUnit");
    return null;
  }

  @Override
  public HtmlUnit parseHtmlUnit(Source source) throws AnalysisException {
    fail("Unexpected invocation of parseHtmlUnit");
    return null;
  }

  @Override
  public AnalysisResult performAnalysisTask() {
    fail("Unexpected invocation of performAnalysisTask");
    return null;
  }

  @Override
  public void recordLibraryElements(Map<Source, LibraryElement> elementMap) {
    fail("Unexpected invocation of recordLibraryElements");
  }

  @Override
  public void removeListener(AnalysisListener listener) {
    fail("Unexpected invocation of removeListener");
  }

  @Override
  public CompilationUnit resolveCompilationUnit(Source unitSource, LibraryElement library)
      throws AnalysisException {
    fail("Unexpected invocation of resolveCompilationUnit");
    return null;
  }

  @Override
  public CompilationUnit resolveCompilationUnit(Source unitSource, Source librarySource)
      throws AnalysisException {
    fail("Unexpected invocation of resolveCompilationUnit");
    return null;
  }

  @Override
  public HtmlUnit resolveHtmlUnit(Source htmlSource) throws AnalysisException {
    fail("Unexpected invocation of resolveHtmlUnit");
    return null;
  }

  @Override
  public void setAnalysisOptions(AnalysisOptions options) {
    fail("Unexpected invocation of setAnalysisOptions");
  }

  @Override
  public void setAnalysisPriorityOrder(List<Source> sources) {
    fail("Unexpected invocation of setAnalysisPriorityOrder");
  }

  @Override
  public void setChangedContents(Source source, String contents, int offset, int oldLength,
      int newLength) {
    fail("Unexpected invocation of setChangedContents");
  }

  @Override
  public void setContents(Source source, String contents) {
    fail("Unexpected invocation of setContents");
  }

  @Override
  public void setSourceFactory(SourceFactory factory) {
    fail("Unexpected invocation of setSourceFactory");
  }
}
