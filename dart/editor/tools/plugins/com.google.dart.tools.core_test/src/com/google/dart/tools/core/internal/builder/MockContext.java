package com.google.dart.tools.core.internal.builder;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.constant.DeclaredVariables;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisContextStatistics;
import com.google.dart.engine.context.AnalysisDelta;
import com.google.dart.engine.context.AnalysisDelta.AnalysisLevel;
import com.google.dart.engine.context.AnalysisErrorInfo;
import com.google.dart.engine.context.AnalysisException;
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
import com.google.dart.engine.internal.context.AnalysisErrorInfoImpl;
import com.google.dart.engine.internal.context.AnalysisOptionsImpl;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.context.ResolvableCompilationUnit;
import com.google.dart.engine.internal.context.TimestampedData;
import com.google.dart.engine.internal.element.angular.AngularApplication;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.source.ContentCache;
import com.google.dart.engine.source.DirectoryBasedSourceContainer;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.Source.ContentReceiver;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.engine.utilities.source.LineInfo;
import com.google.dart.tools.core.CallList;
import com.google.dart.tools.core.CallList.Call;
import com.google.dart.tools.core.mock.MockContainer;
import com.google.dart.tools.core.mock.MockFile;

import junit.framework.TestCase;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Mock {@link AnalysisContext} that validates calls and returns Mocks rather than performing the
 * requested analysis.
 */
public class MockContext implements InternalAnalysisContext {
  private final class ChangedCall extends Call {
    private ChangedCall(AnalysisContext target, ChangeSet expected) {
      super(target, APPLY_CHANGES, expected);
    }

    @Override
    protected boolean equalArguments(Object[] otherArgs) {
      if (otherArgs.length != 1 || !(otherArgs[0] instanceof ChangeSet)) {
        return false;
      }
      ChangeSet changes = (ChangeSet) args[0];
      ChangeSet otherChanges = (ChangeSet) otherArgs[0];
      return equalArgument(changes.getAddedSources(), otherChanges.getAddedSources())
          && equalArgument(changes.getChangedSources(), otherChanges.getChangedSources())
          && equalArgument(changes.getRemovedSources(), otherChanges.getRemovedSources())
          && equalArgument(changes.getRemovedContainers(), otherChanges.getRemovedContainers());
    }

    @Override
    protected void printArguments(PrintStringWriter writer, String indent, Object[] args) {
      ChangeSet changes = (ChangeSet) args[0];
      writer.print(indent);
      writer.println("ChangeSet");
      printCollection(writer, indent, "added", changes.getAddedSources());
      printCollection(writer, indent, "changed", changes.getChangedSources());
      printCollection(writer, indent, "removed", changes.getRemovedSources());
      printCollection(writer, indent, "removedContainers", changes.getRemovedContainers());
    }

    protected void printCollection(PrintStringWriter writer, String indent, String name,
        Collection<?> collection) {
      writer.print(indent);
      writer.print("    ");
      writer.print(name);
      writer.println(": ");
      for (Object object : collection) {
        writer.print(indent);
        writer.print("        ");
        writer.println(object != null ? object.toString() : "null");
      }
    }

    private boolean equalArgument(Collection<?> list1, Collection<?> list2) {
      ArrayList<Object> copy = new ArrayList<Object>(list2);
      for (Object object : list1) {
        if (!copy.remove(object)) {
          return false;
        }
      }
      return copy.isEmpty();
    }
  }

  private AnalysisOptions options = new AnalysisOptionsImpl();

  /**
   * The set of declared variables used when computing constant values.
   */
  private DeclaredVariables declaredVariables = new DeclaredVariables();

  private static final String APPLY_CHANGES = "applyChanges";
  private static final String EXTRACT_CONTEXT = "extractContext";
  private static final String MERGE_CONTEXT = "mergeContext";
  private static final String SOURCE_CHANGED = "sourceChanged";
  private static final String SOURCE_DELETED = "sourceDeleted";

  private final CallList calls = new CallList();
  private final ContentCache contentCache = new ContentCache();
  private SourceFactory factory = new SourceFactory();

  private Map<Source, AnalysisLevel> analysisLevels = new HashMap<Source, AnalysisDelta.AnalysisLevel>();

  @Override
  public void addSourceInfo(Source source, SourceEntry info) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void applyAnalysisDelta(AnalysisDelta delta) {
    for (Entry<Source, AnalysisLevel> entry : delta.getAnalysisLevels().entrySet()) {
      TestCase.assertNotNull(entry.getKey());
      TestCase.assertNotNull(entry.getValue());
    }
    analysisLevels.putAll(delta.getAnalysisLevels());
  }

  @Override
  public void applyChanges(ChangeSet changes) {
    calls.add(new ChangedCall(this, changes));
  }

  public void assertAnalysisLevel(MockContainer container, AnalysisLevel expected) {
    for (IResource res : container.getAllDartAndHtmlFiles()) {
      MockFile file = (MockFile) res;
      assertAnalysisLevel(file, expected);
    }
  }

  public void assertAnalysisLevel(MockFile file, AnalysisLevel expected) {
    assertAnalysisLevel(file.asSource(), expected);
  }

  public void assertAnalysisLevel(Source source, AnalysisLevel expected) {
    TestCase.assertNotNull(source);
    AnalysisLevel actual = analysisLevels.remove(source);
    if (actual != expected) {
      TestCase.failNotEquals("Analysis level for " + source + "\n  ", expected, actual);
    }
    TestCase.assertEquals(expected, actual);
  }

  public void assertChanged(ChangeSet expected) {
    calls.assertCall(new ChangedCall(this, expected));
  }

  public void assertChanged(File[] added, File[] changed, File[] removedFiles, File[] removedDirs) {
    final ChangeSet expected = new ChangeSet();
    if (added != null) {
      for (File file : added) {
        expected.addedSource(new FileBasedSource(file));
      }
    }
    if (changed != null) {
      for (File file : changed) {
        expected.changedSource(new FileBasedSource(file));
      }
    }
    if (removedFiles != null) {
      for (File file : removedFiles) {
        expected.removedSource(new FileBasedSource(file));
      }
    }
    if (removedDirs != null) {
      for (File dir : removedDirs) {
        expected.removedContainer(new DirectoryBasedSourceContainer(dir));
      }
    }
    assertChanged(expected);
  }

  public void assertChanged(IResource[] added, IResource[] changed, IResource[] removed) {
    assertChanged(
        asFiles(added),
        asFiles(changed),
        asFiles(filesOnly(removed)),
        asFiles(containersOnly(removed)));
  }

  public void assertExtracted(IContainer expectedContainer) {
    if (expectedContainer != null) {
      calls.assertCall(this, EXTRACT_CONTEXT, new DirectoryBasedSourceContainer(
          expectedContainer.getLocation().toFile()));
    } else {
      calls.assertNoCall(new Call(this, EXTRACT_CONTEXT) {
        @Override
        protected boolean equalArguments(Object[] otherArgs) {
          return true;
        }
      });
    }
  }

  public void assertMergedContext(AnalysisContext expectedContext) {
    if (expectedContext != null) {
      calls.assertCall(this, MERGE_CONTEXT, expectedContext);
    } else {
      calls.assertNoCall(new Call(this, MERGE_CONTEXT) {
        @Override
        protected boolean equalArguments(Object[] otherArgs) {
          return true;
        }
      });
    }
  }

  public void assertNoCalls() {
    calls.assertNoCalls();
    if (!analysisLevels.isEmpty()) {
      @SuppressWarnings("resource")
      PrintStringWriter writer = new PrintStringWriter();
      writer.print("Expected no more calls to updateAnalysis, but found ");
      for (Entry<Source, AnalysisLevel> entry : analysisLevels.entrySet()) {
        writer.println();
        writer.print("  ");
        writer.print(entry.getValue());
        writer.print(" : ");
        writer.print(entry.getKey());
      }
      TestCase.fail(writer.toString());
    }
  }

  public void assertSourcesChanged(IResource... expected) {
    for (IResource resource : expected) {
      Source source = new FileBasedSource(resource.getLocation().toFile());
      calls.assertCall(this, SOURCE_CHANGED, source);
    }
  }

  public void assertSourcesDeleted(IResource... expected) {
    for (IResource resource : expected) {
      File file = resource.getLocation().toFile();
      if (resource.getType() == IResource.FILE) {
        Source source = new FileBasedSource(file);
        calls.assertCall(this, SOURCE_DELETED, source);
      } else {
        SourceContainer sourceContainer = new DirectoryBasedSourceContainer(file);
        calls.assertCall(this, SOURCE_DELETED, sourceContainer);
      }
    }
  }

  public void clearCalls() {
    calls.clear();
    analysisLevels.clear();
  }

  @Override
  public String computeDocumentationComment(Element element) throws AnalysisException {
    return null;
  }

  @Override
  public AnalysisError[] computeErrors(Source source) throws AnalysisException {
    return AnalysisError.NO_ERRORS;
  }

  @Override
  public Source[] computeExportedLibraries(Source source) throws AnalysisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public HtmlElement computeHtmlElement(Source source) throws AnalysisException {
    return null;
  }

  @Override
  public Source[] computeImportedLibraries(Source source) throws AnalysisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public SourceKind computeKindOf(Source source) {
    return SourceKind.UNKNOWN;
  }

  @Override
  public LibraryElement computeLibraryElement(Source source) throws AnalysisException {
    return null;
  }

  @Override
  public LineInfo computeLineInfo(Source source) throws AnalysisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResolvableCompilationUnit computeResolvableCompilationUnit(Source source)
      throws AnalysisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void dispose() {
  }

  @Override
  public boolean exists(Source source) {
    if (source == null) {
      return false;
    }
    if (contentCache.getContents(source) != null) {
      return true;
    }
    return source.exists();
  }

  @Override
  public AnalysisContext extractContext(SourceContainer container) {
    calls.add(this, EXTRACT_CONTEXT, container);
    return new MockContext();
  }

  @Override
  public InternalAnalysisContext extractContextInto(SourceContainer container,
      InternalAnalysisContext newContext) {
    return newContext;
  }

  @Override
  public AnalysisOptions getAnalysisOptions() {
    return options;
  }

  @Override
  public AngularApplication getAngularApplicationWithHtml(Source htmlSource) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompilationUnitElement getCompilationUnitElement(Source unitSource, Source librarySource) {
    return null;
  }

  @Override
  public TimestampedData<CharSequence> getContents(Source source) throws Exception {
    String contents = contentCache.getContents(source);
    if (contents != null) {
      return new TimestampedData<CharSequence>(contentCache.getModificationStamp(source), contents);
    }
    return source.getContents();
  }

  @Override
  @SuppressWarnings("deprecation")
  public void getContentsToReceiver(Source source, ContentReceiver receiver) throws Exception {
    String contents = contentCache.getContents(source);
    if (contents != null) {
      receiver.accept(contents, contentCache.getModificationStamp(source));
    } else {
      source.getContentsToReceiver(receiver);
    }
  }

  @Override
  public DeclaredVariables getDeclaredVariables() {
    return declaredVariables;
  }

  @Override
  public Element getElement(ElementLocation location) {
    return null;
  }

  @Override
  public AnalysisErrorInfo getErrors(Source source) {
    return new AnalysisErrorInfoImpl(AnalysisError.NO_ERRORS, null);
  }

  @Override
  public HtmlElement getHtmlElement(Source source) {
    return null;
  }

  @Override
  public Source[] getHtmlFilesReferencing(Source source) {
    return Source.EMPTY_ARRAY;
  }

  @Override
  public Source[] getHtmlSources() {
    return Source.EMPTY_ARRAY;
  }

  @Override
  public SourceKind getKindOf(Source source) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Source[] getLaunchableClientLibrarySources() {
    return Source.EMPTY_ARRAY;
  }

  @Override
  public Source[] getLaunchableServerLibrarySources() {
    return Source.EMPTY_ARRAY;
  }

  @Override
  public Source[] getLibrariesContaining(Source source) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Source[] getLibrariesDependingOn(Source librarySource) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Source[] getLibrariesReferencedFromHtml(Source htmlSource) {
    throw new UnsupportedOperationException();
  }

  @Override
  public LibraryElement getLibraryElement(Source source) {
    return null;
  }

  @Override
  public Source[] getLibrarySources() {
    return Source.EMPTY_ARRAY;
  }

  @Override
  public LineInfo getLineInfo(Source source) {
    return null;
  }

  @Override
  public long getModificationStamp(Source source) {
    Long stamp = contentCache.getModificationStamp(source);
    if (stamp != null) {
      return stamp.longValue();
    }
    return source.getModificationStamp();
  }

  @Override
  public Source[] getPrioritySources() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Namespace getPublicNamespace(LibraryElement library) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Source[] getRefactoringUnsafeSources() {
    return Source.EMPTY_ARRAY;
  }

  @Override
  public CompilationUnit getResolvedCompilationUnit(Source unitSource, LibraryElement library) {
    return null;
  }

  @Override
  public CompilationUnit getResolvedCompilationUnit(Source unitSource, Source librarySource) {
    return null;
  }

  @Override
  public HtmlUnit getResolvedHtmlUnit(Source htmlSource) {
    return null;
  }

  @Override
  public SourceFactory getSourceFactory() {
    return factory;
  }

  @Override
  public AnalysisContextStatistics getStatistics() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TypeProvider getTypeProvider() throws AnalysisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isClientLibrary(Source librarySource) {
    return false;
  }

  @Override
  public boolean isDisposed() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isServerLibrary(Source librarySource) {
    return false;
  }

  @Override
  public void mergeContext(AnalysisContext context) {
    calls.add(this, MERGE_CONTEXT, context);
  }

  @Override
  public CompilationUnit parseCompilationUnit(Source source) throws AnalysisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public HtmlUnit parseHtmlUnit(Source source) throws AnalysisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public AnalysisResult performAnalysisTask() {
    // indicate no analysis to be performed
    return new AnalysisResult(null, 0L, null, -1L);
  }

  @Override
  public void recordLibraryElements(Map<Source, LibraryElement> elementMap) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompilationUnit resolveCompilationUnit(Source source, LibraryElement library)
      throws AnalysisException {
    return null;
  }

  @Override
  public CompilationUnit resolveCompilationUnit(Source librarySource, Source unitSource)
      throws AnalysisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public HtmlUnit resolveHtmlUnit(Source htmlSource) throws AnalysisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setAnalysisOptions(AnalysisOptions options) {
    this.options = options;
  }

  @Override
  public void setAnalysisPriorityOrder(List<Source> sources) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setChangedContents(Source source, String contents, int offset, int oldLength,
      int newLength) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setContents(Source source, String contents) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setSourceFactory(SourceFactory sourceFactory) {
    factory = sourceFactory;
  }

  private File[] asFiles(IResource[] resources) {
    if (resources == null) {
      return null;
    }
    File[] files = new File[resources.length];
    for (int i = 0; i < resources.length; i++) {
      files[i] = resources[i].getLocation().toFile();
    }
    return files;
  }

  private IResource[] containersOnly(IResource[] resources) {
    if (resources == null) {
      return null;
    }
    ArrayList<IResource> result = new ArrayList<IResource>();
    for (IResource res : resources) {
      if (res instanceof IContainer) {
        result.add(res);
      }
    }
    return result.toArray(new IResource[result.size()]);
  }

  private IResource[] filesOnly(IResource[] resources) {
    if (resources == null) {
      return null;
    }
    ArrayList<IResource> result = new ArrayList<IResource>();
    for (IResource res : resources) {
      if (!(res instanceof IContainer)) {
        result.add(res);
      }
    }
    return result.toArray(new IResource[result.size()]);
  }
}
