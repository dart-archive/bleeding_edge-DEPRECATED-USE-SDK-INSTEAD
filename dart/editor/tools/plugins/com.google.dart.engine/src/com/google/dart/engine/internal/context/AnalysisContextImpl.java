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
package com.google.dart.engine.internal.context;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.internal.resolver.LibraryResolver;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.internal.scope.NamespaceBuilder;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.scanner.CharBufferScanner;
import com.google.dart.engine.scanner.StringScanner;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.engine.utilities.source.LineInfo;

import java.io.File;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Instances of the class {@code AnalysisContextImpl} implement an {@link AnalysisContext analysis
 * context}.
 */
public class AnalysisContextImpl implements AnalysisContext {
  /**
   * Instances of the class {@code ScanResult} represent the results of scanning a source.
   */
  private static class ScanResult {
    /**
     * The first token in the token stream.
     */
    private Token token;

    /**
     * The line start information that was produced.
     */
    private int[] lineStarts;

    /**
     * Initialize a newly created result object to be empty.
     */
    private ScanResult() {
      super();
    }
  }

  /**
   * The source factory used to create the sources that can be analyzed in this context.
   */
  private SourceFactory sourceFactory;

  /**
   * A cache mapping sources to the compilation units that were produced for the contents of the
   * source.
   */
  // TODO(brianwilkerson) Replace this with a real cache.
  private HashMap<Source, CompilationUnit> parseCache = new HashMap<Source, CompilationUnit>();

  /**
   * A cache mapping sources (of the defining compilation units of libraries) to the library
   * elements for those libraries.
   */
  // TODO(brianwilkerson) Replace this with a real cache.
  private HashMap<Source, LibraryElement> libraryElementCache = new HashMap<Source, LibraryElement>();

  /**
   * A cache mapping sources (of the defining compilation units of libraries) to the public
   * namespace for that library.
   */
  // TODO(brianwilkerson) Replace this with a real cache.
  private HashMap<Source, Namespace> publicNamespaceCache = new HashMap<Source, Namespace>();

  /**
   * A cache of the available sources of interest to the client. Sources are added to this
   * collection via {@link #sourceAvailable(Source)} and removed from this collection via
   * {@link #sourceDeleted(Source)} and {@link #directoryDeleted(File)}
   */
  private final HashSet<Source> availableSources = new HashSet<Source>();

  /**
   * The object used to synchronize access to all of the caches.
   */
  private Object cacheLock = new Object();

  /**
   * The suffix used by sources that contain Dart.
   */
  private static final String DART_SUFFIX = ".dart";

  /**
   * The suffix used by sources that contain HTML.
   */
  private static final String HTML_SUFFIX = ".html";

  /**
   * Initialize a newly created analysis context.
   */
  public AnalysisContextImpl() {
    super();
  }

  @Override
  public void clearResolution() {
    synchronized (cacheLock) {
      // TODO (danrubel): Optimize to only discard resolution information
      parseCache.clear();
      libraryElementCache.clear();
      publicNamespaceCache.clear();
    }
  }

  @Override
  public void discard() {
    synchronized (cacheLock) {
      // TODO (danrubel): Optimize to recache the token stream and/or ASTs in a global context
      parseCache.clear();
      libraryElementCache.clear();
      publicNamespaceCache.clear();
      availableSources.clear();
    }
  }

  @Override
  public AnalysisContext extractAnalysisContext(SourceContainer container) {
    AnalysisContext newContext = AnalysisEngine.getInstance().createAnalysisContext();

    synchronized (cacheLock) {
      // Move sources in the specified directory to the new context
      Iterator<Source> iter = availableSources.iterator();
      while (iter.hasNext()) {
        Source source = iter.next();
        if (container.contains(source)) {
          iter.remove();
          newContext.sourceAvailable(source);
        }
      }

      // TODO (danrubel): Copy cached ASTs without resolution into the new context
      // because the file content has not changed, but the resolution has changed.
    }

    return newContext;
  }

  @Override
  public Collection<Source> getAvailableSources() {
    synchronized (cacheLock) {
      return new ArrayList<Source>(availableSources);
    }
  }

  @Override
  public Element getElement(ElementLocation location) {
    throw new UnsupportedOperationException();
//    String[] components = ((ElementLocationImpl) location).getComponents();
//    Source librarySource = findSource(components[0]);
//    ElementImpl element = (ElementImpl) getLibraryElement(librarySource);
//    for (int i = 1; i < components.length; i++) {
//      if (element == null) {
//        return null;
//      }
//      element = element.getChild(components[i]);
//    }
//    return element;
  }

  @Override
  public AnalysisError[] getErrors(Source source) throws AnalysisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public HtmlElement getHtmlElement(Source source) {
    // TODO(brianwilkerson) Implement this.
    throw new UnsupportedOperationException();
  }

  @Override
  public SourceKind getKnownKindOf(Source source) {
    if (source.getFullName().endsWith(HTML_SUFFIX)) {
      return SourceKind.HTML;
    }
    if (!source.getFullName().endsWith(DART_SUFFIX)) {
      return SourceKind.UNKNOWN;
    }
    synchronized (cacheLock) {
      if (libraryElementCache.containsKey(source)) {
        return SourceKind.LIBRARY;
      }
      CompilationUnit unit = parseCache.get(source);
      if (unit != null && hasPartOfDirective(unit)) {
        return SourceKind.PART;
      }
    }
    return null;
  }

  @Override
  public LibraryElement getLibraryElement(Source source) {
    synchronized (cacheLock) {
      LibraryElement element = libraryElementCache.get(source);
      if (element == null) {
        LibraryResolver resolver = new LibraryResolver(this);
        try {
          element = resolver.resolveLibrary(source, true);
          // TODO(brianwilkerson) Cache the errors that were recorded by the listener.
        } catch (AnalysisException exception) {
          AnalysisEngine.getInstance().getLogger().logError(
              "Could not resolve the library " + source.getFullName(),
              exception);
        }
      }
      return element;
    }
  }

  /**
   * Return the element model corresponding to the library defined by the given source, or
   * {@code null} if the element model does not yet exist.
   * 
   * @param source the source defining the library whose element model is to be returned
   * @return the element model corresponding to the library defined by the given source
   */
  @Override
  public LibraryElement getLibraryElementOrNull(Source source) {
    synchronized (cacheLock) {
      return libraryElementCache.get(source);
    }
  }

  @Override
  public SourceKind getOrComputeKindOf(Source source) {
    SourceKind kind = getKnownKindOf(source);
    if (kind != null) {
      return kind;
    }
    try {
      if (hasPartOfDirective(parse(source))) {
        return SourceKind.PART;
      }
    } catch (AnalysisException exception) {
      return SourceKind.UNKNOWN;
    }
    return SourceKind.LIBRARY;
  }

  @Override
  public AnalysisError[] getParsingErrors(Source source) throws AnalysisException {
    throw new UnsupportedOperationException();
  }

  /**
   * Return a namespace containing mappings for all of the public names defined by the given
   * library.
   * 
   * @param library the library whose public namespace is to be returned
   * @return the public namespace of the given library
   */
  public Namespace getPublicNamespace(LibraryElement library) {
    Source source = library.getDefiningCompilationUnit().getSource();
    synchronized (cacheLock) {
      Namespace namespace = publicNamespaceCache.get(source);
      if (namespace == null) {
        NamespaceBuilder builder = new NamespaceBuilder();
        namespace = builder.createPublicNamespace(library);
        publicNamespaceCache.put(source, namespace);
      }
      return namespace;
    }
  }

  /**
   * Return a namespace containing mappings for all of the public names defined by the library
   * defined by the given source.
   * 
   * @param source the source defining the library whose public namespace is to be returned
   * @return the public namespace corresponding to the library defined by the given source
   */
  public Namespace getPublicNamespace(Source source) {
    synchronized (cacheLock) {
      Namespace namespace = publicNamespaceCache.get(source);
      if (namespace == null) {
        LibraryElement library = getLibraryElement(source);
        if (library == null) {
          return null;
        }
        NamespaceBuilder builder = new NamespaceBuilder();
        namespace = builder.createPublicNamespace(library);
        publicNamespaceCache.put(source, namespace);
      }
      return namespace;
    }
  }

  @Override
  public AnalysisError[] getResolutionErrors(Source source) throws AnalysisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public SourceFactory getSourceFactory() {
    return sourceFactory;
  }

  @Override
  public void mergeAnalysisContext(AnalysisContext context) {
    synchronized (cacheLock) {
      // Move sources in the specified context into the receiver
      availableSources.addAll(context.getAvailableSources());

      // TODO (danrubel): Copy ASTs without resolution information from the old context
      // into this context because the file content has not changed, but the resolution has changed.
    }
  }

  @Override
  public CompilationUnit parse(Source source) throws AnalysisException {
    synchronized (cacheLock) {
      CompilationUnit unit = parseCache.get(source);
      if (unit == null) {
        RecordingErrorListener errorListener = new RecordingErrorListener();
        ScanResult scanResult = internalScan(source, errorListener);
        Parser parser = new Parser(source, errorListener);
        unit = parser.parseCompilationUnit(scanResult.token);
        unit.setParsingErrors(errorListener.getErrors(source));
        unit.setLineInfo(new LineInfo(scanResult.lineStarts));
        parseCache.put(source, unit);
      }
      return unit;
    }
  }

  // TODO (danrubel): Either remove this method 
  // or ensure that the unit's syntax errors are cached in the unit itself
  public CompilationUnit parse(Source source, AnalysisErrorListener errorListener)
      throws AnalysisException {
    synchronized (cacheLock) {
      CompilationUnit unit = parseCache.get(source);
      if (unit == null) {
        ScanResult scanResult = internalScan(source, errorListener);
        Parser parser = new Parser(source, errorListener);
        unit = parser.parseCompilationUnit(scanResult.token);
        unit.setLineInfo(new LineInfo(scanResult.lineStarts));
        parseCache.put(source, unit);
      }
      return unit;
    }
  }

  /**
   * Given a table mapping the source for the libraries represented by the corresponding elements to
   * the elements representing the libraries, record those mappings.
   * 
   * @param elementMap a table mapping the source for the libraries represented by the elements to
   *          the elements representing the libraries
   */
  public void recordLibraryElements(Map<Source, LibraryElement> elementMap) {
    synchronized (cacheLock) {
      libraryElementCache.putAll(elementMap);
    }
  }

  @Override
  public CompilationUnit resolve(Source source, LibraryElement library) throws AnalysisException {
    // TODO (jwren/brianwilkerson/danrubel) not implemented correctly, this method works only if
    // the queried source has been previously resolved and happens to be in the parseCache. This was
    // included for testing purposes.
    return parse(source);
  }

  @Override
  public Token scan(final Source source, final AnalysisErrorListener errorListener)
      throws AnalysisException {
    ScanResult result = internalScan(source, errorListener);
    return result.token;
  }

  @Override
  public void setSourceFactory(SourceFactory sourceFactory) {
    this.sourceFactory = sourceFactory;
  }

  @Override
  public void sourceAvailable(Source source) {
    synchronized (cacheLock) {
      availableSources.add(source);
    }
  }

  @Override
  public void sourceChanged(Source source) {
    synchronized (cacheLock) {
      parseCache.remove(source);
      libraryElementCache.remove(source);
      publicNamespaceCache.remove(source);
    }
  }

  @Override
  public void sourceDeleted(Source source) {
    synchronized (cacheLock) {
      availableSources.remove(source);
      sourceChanged(source);
    }
  }

  @Override
  public void sourcesDeleted(SourceContainer container) {
    synchronized (cacheLock) {
      // TODO (danrubel): Optimize to remove only the specified files
      parseCache.clear();
      libraryElementCache.clear();
      publicNamespaceCache.clear();

      // Remove deleted sources from the available sources collection
      Iterator<Source> iter = availableSources.iterator();
      while (iter.hasNext()) {
        if (container.contains(iter.next())) {
          iter.remove();
        }
      }
    }
  }

  @Override
  public Iterable<Source> sourcesToResolve(Source[] changedSources) {
    // TODO(keertip): revisit to include dependent libraries that are not in changed sources but
    // have to be re-resolved.
    List<Source> librarySources = new ArrayList<Source>();
    for (Source source : changedSources) {
      if (getOrComputeKindOf(source) == SourceKind.LIBRARY) {
        librarySources.add(source);
      }
    }
    return librarySources;
  }

  /**
   * Return {@code true} if the given compilation unit has a part-of directive.
   * 
   * @param unit the compilation unit being tested
   * @return {@code true} if the compilation unit has a part-of directive
   */
  private boolean hasPartOfDirective(CompilationUnit unit) {
    for (Directive directive : unit.getDirectives()) {
      if (directive instanceof PartOfDirective) {
        return true;
      }
    }
    return false;
  }

  private ScanResult internalScan(final Source source, final AnalysisErrorListener errorListener)
      throws AnalysisException {
    final ScanResult result = new ScanResult();
    Source.ContentReceiver receiver = new Source.ContentReceiver() {
      @Override
      public void accept(CharBuffer contents) {
        CharBufferScanner scanner = new CharBufferScanner(source, contents, errorListener);
        result.token = scanner.tokenize();
        result.lineStarts = scanner.getLineStarts();
      }

      @Override
      public void accept(String contents) {
        StringScanner scanner = new StringScanner(source, contents, errorListener);
        result.token = scanner.tokenize();
        result.lineStarts = scanner.getLineStarts();
      }
    };
    try {
      source.getContents(receiver);
    } catch (Exception exception) {
      throw new AnalysisException(exception);
    }
    return result;
  }
}
