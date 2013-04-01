package com.google.dart.engine.internal.context;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.context.ChangeNotice;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.engine.utilities.source.LineInfo;

import java.util.UUID;

/**
 * Instances of the class {@code AnalysisContextImpl} implement an {@link AnalysisContext analysis
 * context}.
 * 
 * @coverage dart.engine
 */
public class InstrumentedAnalysisContextImpl implements AnalysisContext {

  protected String contextId = UUID.randomUUID().toString();

  protected AnalysisContext basis = new AnalysisContextImpl();

  @Override
  public void applyChanges(ChangeSet changeSet) {
    basis.applyChanges(changeSet);

  }

  @Override
  public AnalysisError[] computeErrors(Source source) throws AnalysisException {

    InstrumentationBuilder instrumentation = Instrumentation.builder("ComputeErrors");

    try {
      instrumentation.metric("contextId", contextId);

      AnalysisError[] errors = basis.computeErrors(source);
      instrumentation.metric("Errors-count", errors.length);

      return errors;

    } finally {
      instrumentation.log();
    }
  }

  @Override
  public HtmlElement computeHtmlElement(Source source) throws AnalysisException {
    return basis.computeHtmlElement(source);
  }

  @Override
  public SourceKind computeKindOf(Source source) {
    return basis.computeKindOf(source);
  }

  @Override
  public LibraryElement computeLibraryElement(Source source) throws AnalysisException {
    return basis.computeLibraryElement(source);
  }

  @Override
  public LineInfo computeLineInfo(Source source) throws AnalysisException {
    return basis.computeLineInfo(source);
  }

  @Override
  public AnalysisContext extractContext(SourceContainer container) {
    return basis.extractContext(container);
  }

  @Override
  public Element getElement(ElementLocation location) {
    return basis.getElement(location);
  }

  @Override
  public AnalysisError[] getErrors(Source source) {
    return basis.getErrors(source);
  }

  @Override
  public HtmlElement getHtmlElement(Source source) {
    return basis.getHtmlElement(source);
  }

  @Override
  public Source[] getHtmlFilesReferencing(Source source) {
    return basis.getHtmlFilesReferencing(source);
  }

  @Override
  public Source[] getHtmlSources() {
    return basis.getHtmlSources();
  }

  @Override
  public SourceKind getKindOf(Source source) {
    return basis.getKindOf(source);
  }

  @Override
  public Source[] getLaunchableClientLibrarySources() {
    return basis.getLaunchableClientLibrarySources();
  }

  @Override
  public Source[] getLaunchableServerLibrarySources() {
    return basis.getLaunchableServerLibrarySources();
  }

  @Override
  public Source[] getLibrariesContaining(Source source) {
    return basis.getLibrariesContaining(source);
  }

  @Override
  public LibraryElement getLibraryElement(Source source) {
    return basis.getLibraryElement(source);
  }

  @Override
  public Source[] getLibrarySources() {
    return basis.getLibrarySources();
  }

  @Override
  public LineInfo getLineInfo(Source source) {
    return basis.getLineInfo(source);
  }

  @Override
  public CompilationUnit getResolvedCompilationUnit(Source unitSource, LibraryElement library) {
    return basis.getResolvedCompilationUnit(unitSource, library);
  }

  @Override
  public CompilationUnit getResolvedCompilationUnit(Source unitSource, Source librarySource) {
    return basis.getResolvedCompilationUnit(unitSource, librarySource);
  }

  @Override
  public SourceFactory getSourceFactory() {
    return basis.getSourceFactory();
  }

  @Override
  public boolean isClientLibrary(Source librarySource) {
    return basis.isClientLibrary(librarySource);
  }

  @Override
  public boolean isServerLibrary(Source librarySource) {
    return basis.isServerLibrary(librarySource);
  }

  @Override
  public void mergeContext(AnalysisContext context) {
    basis.mergeContext(context);
  }

  @Override
  public CompilationUnit parseCompilationUnit(Source source) throws AnalysisException {
    return basis.parseCompilationUnit(source);
  }

  @Override
  public HtmlUnit parseHtmlUnit(Source source) throws AnalysisException {
    return basis.parseHtmlUnit(source);
  }

  @Override
  public ChangeNotice[] performAnalysisTask() {
    return basis.performAnalysisTask();
  }

  @Override
  public CompilationUnit resolveCompilationUnit(Source unitSource, LibraryElement library)
      throws AnalysisException {
    return basis.resolveCompilationUnit(unitSource, library);
  }

  @Override
  public CompilationUnit resolveCompilationUnit(Source unitSource, Source librarySource)
      throws AnalysisException {
    return basis.resolveCompilationUnit(unitSource, librarySource);
  }

  @Override
  public HtmlUnit resolveHtmlUnit(Source htmlSource) throws AnalysisException {
    return basis.resolveHtmlUnit(htmlSource);
  }

  @Override
  public void setContents(Source source, String contents) {
    basis.setContents(source, contents);
  }

  @Override
  public void setSourceFactory(SourceFactory factory) {
    basis.setSourceFactory(factory);

  }

  @Override
  @Deprecated
  public Iterable<Source> sourcesToResolve(Source[] changedSources) {
    return basis.sourcesToResolve(changedSources);
  }

}
