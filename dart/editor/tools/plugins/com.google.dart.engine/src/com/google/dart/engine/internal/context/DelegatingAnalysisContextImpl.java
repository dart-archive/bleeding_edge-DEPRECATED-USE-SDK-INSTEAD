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
package com.google.dart.engine.internal.context;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisErrorInfo;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.engine.utilities.source.LineInfo;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Map;

/**
 * Instances of the class {@code DelegatingAnalysisContextImpl} extend {@link AnalysisContextImpl
 * analysis context} to delegate sources to the appropriate analysis context. For instance, if the
 * source is in a system library then the analysis context from the {@link DartSdk} is used.
 * 
 * @coverage dart.engine
 */
public class DelegatingAnalysisContextImpl extends AnalysisContextImpl {

  /**
   * This references the {@link InternalAnalysisContext} held onto by the {@link DartSdk} which is
   * used (instead of this {@link AnalysisContext}) for SDK sources. This field is set when
   * #setSourceFactory(SourceFactory) is called, and references the analysis context in the
   * {@link DartUriResolver} in the {@link SourceFactory}, this analysis context assumes that there
   * will be such a resolver.
   */
  private InternalAnalysisContext sdkAnalysisContext;

  /**
   * Initialize a newly created delegating analysis context.
   */
  public DelegatingAnalysisContextImpl() {
    super();
  }

  @Override
  public void addSourceInfo(Source source, SourceInfo info) {
    if (source.isInSystemLibrary()) {
      sdkAnalysisContext.addSourceInfo(source, info);
    } else {
      super.addSourceInfo(source, info);
    }
  }

  @Override
  public AnalysisError[] computeErrors(Source source) throws AnalysisException {
    if (source.isInSystemLibrary()) {
      return sdkAnalysisContext.computeErrors(source);
    } else {
      return super.computeErrors(source);
    }
  }

  @Override
  public HtmlElement computeHtmlElement(Source source) throws AnalysisException {
    if (source.isInSystemLibrary()) {
      return sdkAnalysisContext.computeHtmlElement(source);
    } else {
      return super.computeHtmlElement(source);
    }
  }

  @Override
  public SourceKind computeKindOf(Source source) {
    if (source.isInSystemLibrary()) {
      return sdkAnalysisContext.computeKindOf(source);
    } else {
      return super.computeKindOf(source);
    }
  }

  @Override
  public LibraryElement computeLibraryElement(Source source) throws AnalysisException {
    if (source.isInSystemLibrary()) {
      return sdkAnalysisContext.computeLibraryElement(source);
    } else {
      return super.computeLibraryElement(source);
    }
  }

  @Override
  public LineInfo computeLineInfo(Source source) throws AnalysisException {
    if (source.isInSystemLibrary()) {
      return sdkAnalysisContext.computeLineInfo(source);
    } else {
      return super.computeLineInfo(source);
    }
  }

  @Override
  public CompilationUnit computeResolvableCompilationUnit(Source source) throws AnalysisException {
    if (source.isInSystemLibrary()) {
      return sdkAnalysisContext.computeResolvableCompilationUnit(source);
    } else {
      return super.computeResolvableCompilationUnit(source);
    }
  }

  @Override
  public AnalysisErrorInfo getErrors(Source source) {
    if (source.isInSystemLibrary()) {
      return sdkAnalysisContext.getErrors(source);
    } else {
      return super.getErrors(source);
    }
  }

  @Override
  public HtmlElement getHtmlElement(Source source) {
    if (source.isInSystemLibrary()) {
      return sdkAnalysisContext.getHtmlElement(source);
    } else {
      return super.getHtmlElement(source);
    }
  }

  @Override
  public Source[] getHtmlFilesReferencing(Source source) {
    if (source.isInSystemLibrary()) {
      return sdkAnalysisContext.getHtmlFilesReferencing(source);
    } else {
      return super.getHtmlFilesReferencing(source);
    }
  }

  @Override
  public SourceKind getKindOf(Source source) {
    if (source.isInSystemLibrary()) {
      return sdkAnalysisContext.getKindOf(source);
    } else {
      return super.getKindOf(source);
    }
  }

  @Override
  public Source[] getLibrariesContaining(Source source) {
    if (source.isInSystemLibrary()) {
      return sdkAnalysisContext.getLibrariesContaining(source);
    } else {
      return super.getLibrariesContaining(source);
    }
  }

  @Override
  public LibraryElement getLibraryElement(Source source) {
    if (source.isInSystemLibrary()) {
      return sdkAnalysisContext.getLibraryElement(source);
    } else {
      return super.getLibraryElement(source);
    }
  }

  @Override
  public Source[] getLibrarySources() {
    return ArrayUtils.addAll(super.getLibrarySources(), sdkAnalysisContext.getLibrarySources());
  }

  @Override
  public LineInfo getLineInfo(Source source) {
    if (source.isInSystemLibrary()) {
      return sdkAnalysisContext.getLineInfo(source);
    } else {
      return super.getLineInfo(source);
    }
  }

  @Override
  public Namespace getPublicNamespace(LibraryElement library) {
    Source source = library.getSource();
    if (source.isInSystemLibrary()) {
      return sdkAnalysisContext.getPublicNamespace(library);
    } else {
      return super.getPublicNamespace(library);
    }
  }

  @Override
  public Namespace getPublicNamespace(Source source) throws AnalysisException {
    if (source.isInSystemLibrary()) {
      return sdkAnalysisContext.getPublicNamespace(source);
    } else {
      return super.getPublicNamespace(source);
    }
  }

  @Override
  public CompilationUnit getResolvedCompilationUnit(Source unitSource, LibraryElement library) {
    if (unitSource.isInSystemLibrary()) {
      return sdkAnalysisContext.getResolvedCompilationUnit(unitSource, library);
    } else {
      return super.getResolvedCompilationUnit(unitSource, library);
    }
  }

  @Override
  public CompilationUnit getResolvedCompilationUnit(Source unitSource, Source librarySource) {
    if (unitSource.isInSystemLibrary()) {
      return sdkAnalysisContext.getResolvedCompilationUnit(unitSource, librarySource);
    } else {
      return super.getResolvedCompilationUnit(unitSource, librarySource);
    }
  }

  @Override
  public boolean isClientLibrary(Source librarySource) {
    if (librarySource.isInSystemLibrary()) {
      return sdkAnalysisContext.isClientLibrary(librarySource);
    } else {
      return super.isClientLibrary(librarySource);
    }
  }

  @Override
  public boolean isServerLibrary(Source librarySource) {
    if (librarySource.isInSystemLibrary()) {
      return sdkAnalysisContext.isServerLibrary(librarySource);
    } else {
      return super.isServerLibrary(librarySource);
    }
  }

  @Override
  public CompilationUnit parseCompilationUnit(Source source) throws AnalysisException {
    if (source.isInSystemLibrary()) {
      return sdkAnalysisContext.parseCompilationUnit(source);
    } else {
      return super.parseCompilationUnit(source);
    }
  }

  @Override
  public HtmlUnit parseHtmlUnit(Source source) throws AnalysisException {
    if (source.isInSystemLibrary()) {
      return sdkAnalysisContext.parseHtmlUnit(source);
    } else {
      return super.parseHtmlUnit(source);
    }
  }

//  @Override
//  public ChangeNotice[] performAnalysisTask() {
//    ChangeNotice[] sdkNotices = sdkAnalysisContext.performAnalysisTask();
//    if (sdkNotices != null) {
//      return sdkNotices;
//    }
//    return super.performAnalysisTask();
//  }

  @Override
  public void recordLibraryElements(Map<Source, LibraryElement> elementMap) {
    if (elementMap.isEmpty()) {
      return;
    }
    // TODO(jwren) we are making the assumption here that the elementMap will have sources from only
    // one library, while this is true with our use of the Analysis Engine, it is not required by
    // the API, revisit to fix cases where the elementMap can have sources both in the sdk and other
    // libraries
    Source source = elementMap.keySet().iterator().next();
    if (source.isInSystemLibrary()) {
      sdkAnalysisContext.recordLibraryElements(elementMap);
    } else {
      super.recordLibraryElements(elementMap);
    }
  }

  @Override
  public void recordResolutionErrors(Source source, AnalysisError[] errors, LineInfo lineInfo) {
    if (source.isInSystemLibrary()) {
      sdkAnalysisContext.recordResolutionErrors(source, errors, lineInfo);
    } else {
      super.recordResolutionErrors(source, errors, lineInfo);
    }
  }

  @Override
  public void recordResolvedCompilationUnit(Source source, CompilationUnit unit) {
    if (source.isInSystemLibrary()) {
      sdkAnalysisContext.recordResolvedCompilationUnit(source, unit);
    } else {
      super.recordResolvedCompilationUnit(source, unit);
    }
  }

  @Override
  public CompilationUnit resolveCompilationUnit(Source source, LibraryElement library)
      throws AnalysisException {
    if (source.isInSystemLibrary()) {
      return sdkAnalysisContext.resolveCompilationUnit(source, library);
    } else {
      return super.resolveCompilationUnit(source, library);
    }
  }

  @Override
  public CompilationUnit resolveCompilationUnit(Source unitSource, Source librarySource)
      throws AnalysisException {
    if (unitSource.isInSystemLibrary()) {
      return sdkAnalysisContext.resolveCompilationUnit(unitSource, librarySource);
    } else {
      return super.resolveCompilationUnit(unitSource, librarySource);
    }
  }

  @Override
  public HtmlUnit resolveHtmlUnit(Source unitSource) throws AnalysisException {
    if (unitSource.isInSystemLibrary()) {
      return sdkAnalysisContext.resolveHtmlUnit(unitSource);
    } else {
      return super.resolveHtmlUnit(unitSource);
    }
  }

  @Override
  public void setContents(Source source, String contents) {
    if (source.isInSystemLibrary()) {
      sdkAnalysisContext.setContents(source, contents);
    } else {
      super.setContents(source, contents);
    }
  }

  @Override
  public void setSourceFactory(SourceFactory factory) {
    super.setSourceFactory(factory);
    DartSdk sdk = factory.getDartSdk();
    if (sdk != null) {
      sdkAnalysisContext = (InternalAnalysisContext) sdk.getContext();
    } else {
      throw new IllegalStateException(
          "SourceFactorys provided to DelegatingAnalysisContextImpls must have a DartSdk associated with the provided SourceFactory.");
    }
  }
}
