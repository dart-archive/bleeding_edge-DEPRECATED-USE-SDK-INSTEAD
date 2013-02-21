package com.google.dart.tools.core.internal.builder;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.DirectoryBasedSourceContainer;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.tools.core.CallList;
import com.google.dart.tools.core.CallList.Call;

import static junit.framework.Assert.fail;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;

import static org.eclipse.core.resources.IResource.FILE;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * Mock {@link AnalysisContext} that validates calls and returns Mocks rather than performing the
 * requested analysis.
 */
public class MockContext implements AnalysisContext {

  private static final String CLEAR_RESOLUTION = "clearResolution";
  private static final String DISCARDED = "discarded";
  private static final String EXTRACT_ANALYSIS_CONTEXT = "extractAnalysisContext";
  private static final String MERGE_ANALYSIS_CONTEXT = "mergeAnalysisContext";
  private static final String SOURCE_CHANGED = "sourceChanged";
  private static final String SOURCE_DELETED = "sourceDeleted";

  private final CallList calls = new CallList();
  private SourceFactory factory;

  public void assertClearResolution(boolean expected) {
    calls.assertExpectedCall(expected, this, CLEAR_RESOLUTION);
  }

  public void assertDiscarded(boolean expected) {
    calls.assertExpectedCall(expected, this, DISCARDED);
  }

  public void assertExtracted(IContainer expectedContainer) {
    if (expectedContainer != null) {
      calls.assertCall(this, EXTRACT_ANALYSIS_CONTEXT, new DirectoryBasedSourceContainer(
          expectedContainer.getLocation().toFile()));
    } else {
      calls.assertNoCall(new Call(this, EXTRACT_ANALYSIS_CONTEXT) {
        @Override
        protected boolean equalArguments(Object[] otherArgs) {
          return true;
        }
      });
    }
  }

  public void assertMergedContext(AnalysisContext expectedContext) {
    if (expectedContext != null) {
      calls.assertCall(this, MERGE_ANALYSIS_CONTEXT, expectedContext);
    } else {
      calls.assertNoCall(new Call(this, MERGE_ANALYSIS_CONTEXT) {
        @Override
        protected boolean equalArguments(Object[] otherArgs) {
          return true;
        }
      });
    }
  }

  public void assertNoCalls() {
    calls.assertNoCalls();
  }

  public void assertSourcesChanged(IResource... expected) {
    for (IResource resource : expected) {
      Source source = new FileBasedSource(factory, resource.getLocation().toFile());
      calls.assertCall(this, SOURCE_CHANGED, source);
    }
  }

  public void assertSourcesDeleted(IResource... expected) {
    for (IResource resource : expected) {
      File file = resource.getLocation().toFile();
      if (resource.getType() == IResource.FILE) {
        Source source = new FileBasedSource(factory, file);
        calls.assertCall(this, SOURCE_DELETED, source);
      } else {
        SourceContainer sourceContainer = new DirectoryBasedSourceContainer(file);
        calls.assertCall(this, SOURCE_DELETED, sourceContainer);
      }
    }
  }

  @Override
  public void clearResolution() {
    calls.add(this, CLEAR_RESOLUTION);
  }

  @Override
  public void discard() {
    calls.add(this, DISCARDED);
  }

  @Override
  public AnalysisContext extractAnalysisContext(SourceContainer container) {
    calls.add(this, EXTRACT_ANALYSIS_CONTEXT, container);
    return new MockContext();
  }

  @Override
  public Collection<Source> getAvailableSources() {
    return null;
  }

  @Override
  public Element getElement(ElementLocation location) {
    return null;
  }

  @Override
  public AnalysisError[] getErrors(Source source) throws AnalysisException {
    return null;
  }

  @Override
  public HtmlElement getHtmlElement(Source source) {
    return null;
  }

  @Override
  public SourceKind getKnownKindOf(Source source) {
    throw new UnsupportedOperationException();
  }

  @Override
  public LibraryElement getLibraryElement(Source source) {
    return null;
  }

  @Override
  public LibraryElement getLibraryElementOrNull(Source source) {
    return null;
  }

  @Override
  public SourceKind getOrComputeKindOf(Source source) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AnalysisError[] getParsingErrors(Source source) throws AnalysisException {
    return new AnalysisError[] {};
  }

  @Override
  public AnalysisError[] getResolutionErrors(Source source) throws AnalysisException {
    return new AnalysisError[] {};
  }

  @Override
  public SourceFactory getSourceFactory() {
    return factory;
  }

  @Override
  public void mergeAnalysisContext(AnalysisContext context) {
    calls.add(this, MERGE_ANALYSIS_CONTEXT, context);
  }

  @Override
  public CompilationUnit parse(Source source) throws AnalysisException {
    return null;
  }

  @Override
  public CompilationUnit resolve(Source source, LibraryElement library) throws AnalysisException {
    return null;
  }

  @Override
  public Token scan(Source source, AnalysisErrorListener errorListener) throws AnalysisException {
    return null;
  }

  @Override
  public void setSourceFactory(SourceFactory sourceFactory) {
    factory = sourceFactory;
  }

  @Override
  public void sourceAvailable(Source source) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void sourceChanged(Source source) {
    calls.add(this, SOURCE_CHANGED, source);
  }

  @Override
  public void sourceDeleted(Source source) {
    calls.add(this, SOURCE_DELETED, source);
  }

  @Override
  public void sourcesDeleted(SourceContainer container) {
    calls.add(this, SOURCE_DELETED, container);
  }

  @Override
  public Iterable<Source> sourcesToResolve(Source[] changedSources) {
    throw new UnsupportedOperationException();
  }

  private void assertEqualContents(HashSet<Object> sources, IResource[] resources) {
    if (sources.size() == resources.length) {
      boolean success = true;
      for (IResource res : resources) {
        File file = res.getLocation().toFile();
        Object expected = res.getType() == FILE ? new FileBasedSource(factory, file)
            : new DirectoryBasedSourceContainer(file);
        if (!sources.contains(expected)) {
          success = false;
          break;
        }
      }
      if (success) {
        return;
      }
    }
    PrintStringWriter msg = new PrintStringWriter();
    msg.println("Expected:");
    for (String string : sort(getPaths(resources))) {
      msg.println(string);
    }
    msg.println("Actual:");
    for (String string : sort(getPaths(sources))) {
      msg.println(string);
    }
    fail(msg.toString().trim());
  }

  private ArrayList<String> getPaths(HashSet<Object> sources) {
    ArrayList<String> result = new ArrayList<String>();
    for (Object object : sources) {
      if (object instanceof Source) {
        Source source = (Source) object;
        result.add(source.getFullName());
      } else if (object instanceof SourceContainer) {
        SourceContainer container = (SourceContainer) object;
        result.add(container.toString());
      } else {
        throw new RuntimeException("Unexpected: " + object);
      }
    }
    return result;
  }

  private ArrayList<String> getPaths(IResource[] resources) {
    ArrayList<String> result = new ArrayList<String>();
    for (IResource resource : resources) {
      result.add(resource.getLocation().toOSString());
    }
    return result;
  }

  private ArrayList<String> sort(ArrayList<String> paths) {
    Collections.sort(paths);
    return paths;
  }
}
