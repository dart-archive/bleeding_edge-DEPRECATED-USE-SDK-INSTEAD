package com.google.dart.engine.internal.context;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContentStatistics;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisErrorInfo;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.context.AnalysisOptions;
import com.google.dart.engine.context.AnalysisResult;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.internal.cache.SourceEntry;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.engine.utilities.source.LineInfo;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Instances of the class {@code InstrumentedAnalysisContextImpl} implement an
 * {@link AnalysisContext analysis context} by recording instrumentation data and delegating to
 * another analysis context to do the non-instrumentation work.
 * 
 * @coverage dart.engine
 */
public class InstrumentedAnalysisContextImpl implements InternalAnalysisContext {
  /**
   * Record an exception that was thrown during analysis.
   * 
   * @param instrumentation the instrumentation builder being used to record the exception
   * @param exception the exception being reported
   */
  private static void recordAnalysisException(InstrumentationBuilder instrumentation,
      AnalysisException exception) {
    instrumentation.record(exception);
  }

  /**
   * The unique identifier used to identify this analysis context in the instrumentation data.
   */
  private final String contextId = UUID.randomUUID().toString();

  /**
   * The analysis context to which all of the non-instrumentation work is delegated.
   */
  private final InternalAnalysisContext basis;

  /**
   * Create a new {@link InstrumentedAnalysisContextImpl} which wraps a new
   * {@link AnalysisContextImpl} as the basis context.
   */
  public InstrumentedAnalysisContextImpl() {
    this(new DelegatingAnalysisContextImpl());
  }

  /**
   * Create a new {@link InstrumentedAnalysisContextImpl} with a specified basis context, aka the
   * context to wrap and instrument.
   * 
   * @param context some {@link InstrumentedAnalysisContext} to wrap and instrument
   */
  public InstrumentedAnalysisContextImpl(InternalAnalysisContext context) {
    basis = context;
  }

  @Override
  public void addSourceInfo(Source source, SourceEntry info) {
    basis.addSourceInfo(source, info);
  }

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
  public String computeDocumentationComment(Element element) throws AnalysisException {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-computeDocumentationComment");
    try {
      instrumentation.metric("contextId", contextId);
      return basis.computeDocumentationComment(element);
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
  public Source[] computeExportedLibraries(Source source) throws AnalysisException {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-computeExportedLibraries");
    try {
      instrumentation.metric("contextId", contextId);
      return basis.computeExportedLibraries(source);
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
  public Source[] computeImportedLibraries(Source source) throws AnalysisException {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-computeImportedLibraries");
    try {
      instrumentation.metric("contextId", contextId);
      return basis.computeImportedLibraries(source);
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
  public ResolvableCompilationUnit computeResolvableCompilationUnit(Source source)
      throws AnalysisException {
    return basis.computeResolvableCompilationUnit(source);
  }

  @Override
  public ResolvableHtmlUnit computeResolvableHtmlUnit(Source source) throws AnalysisException {
    return basis.computeResolvableHtmlUnit(source);
  }

  @Override
  public AnalysisContext extractContext(SourceContainer container) {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-extractContext");
    try {
      instrumentation.metric("contextId", contextId);
      InstrumentedAnalysisContextImpl newContext = new InstrumentedAnalysisContextImpl();
      basis.extractContextInto(container, newContext.basis);
      return newContext;
    } finally {
      instrumentation.log();
    }

  }

  @Override
  public InternalAnalysisContext extractContextInto(SourceContainer container,
      InternalAnalysisContext newContext) {
    return basis.extractContextInto(container, newContext);
  }

  @Override
  public AnalysisOptions getAnalysisOptions() {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-getAnalysisOptions");
    try {
      instrumentation.metric("contextId", contextId);
      return basis.getAnalysisOptions();
    } finally {
      instrumentation.log();
    }
  }

  /**
   * @return the underlying {@link AnalysisContext}.
   */
  public AnalysisContext getBasis() {
    return basis;
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
  public AnalysisErrorInfo getErrors(Source source) {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-getErrors");
    try {
      instrumentation.metric("contextId", contextId);
      AnalysisErrorInfo ret = basis.getErrors(source);
      if (ret != null) {
        instrumentation.metric("Errors-count", ret.getErrors().length);
      }
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
      if (ret != null) {
        instrumentation.metric("Source-count", ret.length);
      }
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
      if (ret != null) {
        instrumentation.metric("Source-count", ret.length);
      }
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
      if (ret != null) {
        instrumentation.metric("Source-count", ret.length);
      }
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
      if (ret != null) {
        instrumentation.metric("Source-count", ret.length);
      }
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
      if (ret != null) {
        instrumentation.metric("Source-count", ret.length);
      }
      return ret;
    } finally {
      instrumentation.log(2); //Log if 1ms
    }
  }

  @Override
  public Source[] getLibrariesDependingOn(Source librarySource) {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-getLibrariesDependingOn");
    try {
      instrumentation.metric("contextId", contextId);
      Source[] ret = basis.getLibrariesDependingOn(librarySource);
      if (ret != null) {
        instrumentation.metric("Source-count", ret.length);
      }
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
      if (ret != null) {
        instrumentation.metric("Source-count", ret.length);
      }
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
  public Namespace getPublicNamespace(LibraryElement library) {
    return basis.getPublicNamespace(library);
  }

  @Override
  public Namespace getPublicNamespace(Source source) throws AnalysisException {
    return basis.getPublicNamespace(source);
  }

  @Override
  public Source[] getRefactoringUnsafeSources() {
    return basis.getRefactoringUnsafeSources();
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
      instrumentation.log(2); //Log if over 1ms
    }
  }

  @Override
  public SourceFactory getSourceFactory() {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-getSourceFactory");
    try {
      instrumentation.metric("contextId", contextId);
      return basis.getSourceFactory();
    } finally {
      instrumentation.log(2); //Log if over 1ms
    }
  }

  @Override
  public AnalysisContentStatistics getStatistics() {
    return basis.getStatistics();
  }

  @Override
  public TimestampedData<CompilationUnit> internalResolveCompilationUnit(Source unitSource,
      LibraryElement libraryElement) throws AnalysisException {
    return basis.internalResolveCompilationUnit(unitSource, libraryElement);
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
      if (context instanceof InstrumentedAnalysisContextImpl) {
        context = ((InstrumentedAnalysisContextImpl) context).basis;
      }
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
  public AnalysisResult performAnalysisTask() {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-performAnalysisTask");
    try {
      instrumentation.metric("contextId", contextId);
      AnalysisResult result = basis.performAnalysisTask();
      if (result.getChangeNotices() != null) {
        instrumentation.metric("ChangeNotice-count", result.getChangeNotices().length);
      }
      return result;
    } finally {
      instrumentation.log(2); //Log if over 1ms
    }
  }

  @Override
  public void recordLibraryElements(Map<Source, LibraryElement> elementMap) {
    basis.recordLibraryElements(elementMap);
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
  public void setAnalysisOptions(AnalysisOptions options) {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-setAnalysisOptions");
    try {
      instrumentation.metric("contextId", contextId);
      basis.setAnalysisOptions(options);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public void setAnalysisPriorityOrder(List<Source> sources) {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-setAnalysisPriorityOrder");
    try {
      instrumentation.metric("contextId", contextId);
      basis.setAnalysisPriorityOrder(sources);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public void setChangedContents(Source source, String contents, int offset, int oldLength,
      int newLength) {
    InstrumentationBuilder instrumentation = Instrumentation.builder("Analysis-setChangedContents");
    try {
      instrumentation.metric("contextId", contextId);
      basis.setChangedContents(source, contents, offset, oldLength, newLength);
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
