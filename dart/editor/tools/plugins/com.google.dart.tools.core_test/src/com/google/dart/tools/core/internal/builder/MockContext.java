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
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.tools.core.internal.analysis.model.ProjectImplTest;

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

  private SourceFactory factory;
  private boolean clearResolution = false;
  private boolean discarded = false;
  private SourceContainer extractedContainer = null;
  private AnalysisContext mergedContext = null;
  private HashSet<Object> changedSources = new HashSet<Object>();
  private HashSet<Object> deletedSources = new HashSet<Object>();

  public void assertClearResolution(boolean expected) {
    ProjectImplTest.assertEquals(expected, clearResolution);
  }

  public void assertDiscarded(boolean expected) {
    ProjectImplTest.assertEquals(expected, discarded);
  }

  public void assertExtracted(IContainer expectedContainer) {
    SourceContainer expected = expectedContainer == null ? null
        : factory.forDirectory(expectedContainer.getLocation().toFile());
    ProjectImplTest.assertEquals(expected, extractedContainer);
  }

  public void assertMergedContext(AnalysisContext expectedContext) {
    ProjectImplTest.assertSame(expectedContext, mergedContext);
  }

  @Override
  public void clearResolution() {
    clearResolution = true;
  }

  @Override
  public void discard() {
    discarded = true;
  }

  @Override
  public AnalysisContext extractAnalysisContext(SourceContainer container) {
    extractedContainer = container;
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
  public LibraryElement getLibraryElement(Source source) {
    return null;
  }

  @Override
  public AnalysisError[] getSemanticErrors(Source source) throws AnalysisException {
    return new AnalysisError[] {};
  }

  @Override
  public SourceFactory getSourceFactory() {
    return factory;
  }

  @Override
  public AnalysisError[] getSyntacticErrors(Source source) throws AnalysisException {
    return new AnalysisError[] {};
  }

  @Override
  public void mergeAnalysisContext(AnalysisContext context) {
    mergedContext = context;
  }

  @Override
  public CompilationUnit parse(Source source) throws AnalysisException {
    return null;
  }

  @Override
  public CompilationUnit resolve(Source source, LibraryElement library,
      AnalysisErrorListener errorListener) throws AnalysisException {
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
    changedSources.add(source);
  }

  @Override
  public void sourceDeleted(Source source) {
    deletedSources.add(source);
  }

  @Override
  public void sourcesDeleted(SourceContainer container) {
    deletedSources.add(container);
  }

  void assertSourcesChanged(IResource... expected) {
    assertEqualContents(changedSources, expected);
  }

  void assertSourcesDeleted(IResource... expected) {
    assertEqualContents(deletedSources, expected);
  }

  private void assertEqualContents(HashSet<Object> sources, IResource[] resources) {
    if (sources.size() == resources.length) {
      boolean success = true;
      for (IResource res : resources) {
        File file = res.getLocation().toFile();
        Object expected = res.getType() == FILE ? factory.forFile(file)
            : factory.forDirectory(file);
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
