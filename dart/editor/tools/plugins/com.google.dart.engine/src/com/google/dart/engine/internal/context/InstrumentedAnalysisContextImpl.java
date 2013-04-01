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

  /**
   * Record an exception that was thrown during analysis
   */
  private static void recordAnalysisException(InstrumentationBuilder instrumentation,
      AnalysisException e) {
    instrumentation.metric("Problem", e.getClass().toString());
    instrumentation.data("AnalysisException", e.toString());
  }

  protected String contextId = UUID.randomUUID().toString();
  protected AnalysisContext basis = new AnalysisContextImpl();

  @Override
  public void applyChanges(ChangeSet changeSet) {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-applyChanges");
    try {
      instrumentation.metric("contextId", contextId);

      basis.applyChanges(changeSet);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public AnalysisError[] computeErrors(Source source) throws AnalysisException {

    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-computeErrors");
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

    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-computeHtmlElement");
    try {
      instrumentation.metric("contextId", contextId);

      return basis.computeHtmlElement(source);
    } catch (AnalysisException e) {
      recordAnalysisException(instrumentation, e);
      throw e;
    } finally {
      instrumentation.log();
    }

  }

  @Override
  public SourceKind computeKindOf(Source source) {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-computeKindOf");
    try {
      instrumentation.metric("contextId", contextId);

      return basis.computeKindOf(source);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public LibraryElement computeLibraryElement(Source source) throws AnalysisException {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-computeLibraryElement");
    try {
      instrumentation.metric("contextId", contextId);

      return basis.computeLibraryElement(source);
    } catch (AnalysisException e) {
      recordAnalysisException(instrumentation, e);
      throw e;
    } finally {
      instrumentation.log();
    }

  }

  @Override
  public LineInfo computeLineInfo(Source source) throws AnalysisException {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-computeLineInfo");
    try {
      instrumentation.metric("contextId", contextId);

      return basis.computeLineInfo(source);
    } catch (AnalysisException e) {
      recordAnalysisException(instrumentation, e);
      throw e;
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public AnalysisContext extractContext(SourceContainer container) {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-extractContext");
    try {
      instrumentation.metric("contextId", contextId);

      return basis.extractContext(container);
    } finally {
      instrumentation.log();
    }

  }

  @Override
  public Element getElement(ElementLocation location) {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-getElement");
    try {
      instrumentation.metric("contextId", contextId);

      return basis.getElement(location);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public AnalysisError[] getErrors(Source source) {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-getErrors");
    try {
      instrumentation.metric("contextId", contextId);

      AnalysisError[] ret = basis.getErrors(source);
      instrumentation.metric("Errors-count", ret.length);
      return ret;
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public HtmlElement getHtmlElement(Source source) {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-getHtmlElement");
    try {
      instrumentation.metric("contextId", contextId);

      return basis.getHtmlElement(source);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public Source[] getHtmlFilesReferencing(Source source) {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-getHtmlFilesReferencing");
    try {
      instrumentation.metric("contextId", contextId);

      Source[] ret = basis.getHtmlFilesReferencing(source);
      instrumentation.metric("Source-count", ret.length);
      return ret;
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public Source[] getHtmlSources() {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-getHtmlSources");
    try {
      instrumentation.metric("contextId", contextId);
      Source[] ret = basis.getHtmlSources();
      instrumentation.metric("Source-count", ret.length);
      return ret;
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public SourceKind getKindOf(Source source) {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-getKindOf");
    try {
      instrumentation.metric("contextId", contextId);
      return basis.getKindOf(source);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public Source[] getLaunchableClientLibrarySources() {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-getLaunchableClientLibrarySources");
    try {
      instrumentation.metric("contextId", contextId);
      Source[] ret = basis.getLaunchableClientLibrarySources();
      instrumentation.metric("Source-count", ret.length);
      return ret;
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public Source[] getLaunchableServerLibrarySources() {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-getLaunchableServerLibrarySources");
    try {
      instrumentation.metric("contextId", contextId);
      Source[] ret = basis.getLaunchableServerLibrarySources();
      instrumentation.metric("Source-count", ret.length);
      return ret;
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public Source[] getLibrariesContaining(Source source) {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-getLibrariesContaining");
    try {
      instrumentation.metric("contextId", contextId);
      Source[] ret = basis.getLibrariesContaining(source);
      instrumentation.metric("Source-count", ret.length);
      return ret;
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public LibraryElement getLibraryElement(Source source) {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-getLibraryElement");
    try {
      instrumentation.metric("contextId", contextId);
      return basis.getLibraryElement(source);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public Source[] getLibrarySources() {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-getLibrarySources");
    try {
      instrumentation.metric("contextId", contextId);
      Source[] ret = basis.getLibrarySources();
      instrumentation.metric("Source-count", ret.length);
      return ret;
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public LineInfo getLineInfo(Source source) {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-getLineInfo");
    try {
      instrumentation.metric("contextId", contextId);
      return basis.getLineInfo(source);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public CompilationUnit getResolvedCompilationUnit(Source unitSource, LibraryElement library) {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-getResolvedCompilationUnit");
    try {
      instrumentation.metric("contextId", contextId);
      return basis.getResolvedCompilationUnit(unitSource, library);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public CompilationUnit getResolvedCompilationUnit(Source unitSource, Source librarySource) {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-getResolvedCompilationUnit");
    try {
      instrumentation.metric("contextId", contextId);
      return basis.getResolvedCompilationUnit(unitSource, librarySource);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public SourceFactory getSourceFactory() {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-getSourceFactory");
    try {
      instrumentation.metric("contextId", contextId);
      return basis.getSourceFactory();
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public boolean isClientLibrary(Source librarySource) {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-isClientLibrary");
    try {
      instrumentation.metric("contextId", contextId);
      return basis.isClientLibrary(librarySource);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public boolean isServerLibrary(Source librarySource) {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-isServerLibrary");
    try {
      instrumentation.metric("contextId", contextId);
      return basis.isServerLibrary(librarySource);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public void mergeContext(AnalysisContext context) {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-mergeContext");
    try {
      instrumentation.metric("contextId", contextId);
      basis.mergeContext(context);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public CompilationUnit parseCompilationUnit(Source source) throws AnalysisException {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-parseCompilationUnit");
    try {
      instrumentation.metric("contextId", contextId);
      return basis.parseCompilationUnit(source);
    } catch (AnalysisException e) {
      recordAnalysisException(instrumentation, e);
      throw e;
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public HtmlUnit parseHtmlUnit(Source source) throws AnalysisException {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-parseHtmlUnit");
    try {
      instrumentation.metric("contextId", contextId);
      return basis.parseHtmlUnit(source);
    } catch (AnalysisException e) {
      recordAnalysisException(instrumentation, e);
      throw e;
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public ChangeNotice[] performAnalysisTask() {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-performAnalysisTask");
    try {
      instrumentation.metric("contextId", contextId);
      ChangeNotice[] ret = basis.performAnalysisTask();
      instrumentation.metric("ChangeNotice-count", ret.length);
      return ret;
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public CompilationUnit resolveCompilationUnit(Source unitSource, LibraryElement library)
      throws AnalysisException {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-resolveCompilationUnit");
    try {
      instrumentation.metric("contextId", contextId);
      return basis.resolveCompilationUnit(unitSource, library);
    } catch (AnalysisException e) {
      recordAnalysisException(instrumentation, e);
      throw e;
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public CompilationUnit resolveCompilationUnit(Source unitSource, Source librarySource)
      throws AnalysisException {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-resolveCompilationUnit");
    try {
      instrumentation.metric("contextId", contextId);
      return basis.resolveCompilationUnit(unitSource, librarySource);
    } catch (AnalysisException e) {
      recordAnalysisException(instrumentation, e);
      throw e;
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public HtmlUnit resolveHtmlUnit(Source htmlSource) throws AnalysisException {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-resolveHtmlUnit");
    try {
      instrumentation.metric("contextId", contextId);
      return basis.resolveHtmlUnit(htmlSource);
    } catch (AnalysisException e) {
      recordAnalysisException(instrumentation, e);
      throw e;
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public void setContents(Source source, String contents) {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-setContents");
    try {
      instrumentation.metric("contextId", contextId);
      basis.setContents(source, contents);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public void setSourceFactory(SourceFactory factory) {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-setSourceFactory");
    try {
      instrumentation.metric("contextId", contextId);
      basis.setSourceFactory(factory);
    } finally {
      instrumentation.log();
    }

  }

  @Override
  @Deprecated
  public Iterable<Source> sourcesToResolve(Source[] changedSources) {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-sourcesToResolve");
    try {
      instrumentation.metric("contextId", contextId);
      return basis.sourcesToResolve(changedSources);
    } finally {
      instrumentation.log();
    }
  }

}
