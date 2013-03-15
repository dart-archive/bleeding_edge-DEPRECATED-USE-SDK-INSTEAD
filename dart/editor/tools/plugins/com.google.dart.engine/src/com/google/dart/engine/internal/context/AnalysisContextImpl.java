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
import com.google.dart.engine.context.ChangeNotice;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.html.parser.HtmlParseResult;
import com.google.dart.engine.html.parser.HtmlParser;
import com.google.dart.engine.html.scanner.HtmlScanResult;
import com.google.dart.engine.html.scanner.HtmlScanner;
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

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Instances of the class {@code AnalysisContextImpl} implement an {@link AnalysisContext analysis
 * context}.
 * 
 * @coverage dart.engine
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
   * A table mapping sources known to the context to the information known about the source.
   */
  private final HashMap<Source, SourceInfo> sourceMap = new HashMap<Source, SourceInfo>();

  /**
   * A cache mapping sources to the compilation units that were produced for the contents of the
   * source.
   */
  // TODO(brianwilkerson) Replace this with a real cache.
  private HashMap<Source, CompilationUnit> parseCache = new HashMap<Source, CompilationUnit>();

  /**
   * A cache mapping sources to the html parse results that were produced for the contents of the
   * source.
   */
  // TODO (danrubel): Replace this with a real cache.
  private HashMap<Source, HtmlParseResult> htmlParseCache = new HashMap<Source, HtmlParseResult>();

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
   * The object used to synchronize access to all of the caches.
   */
  private Object cacheLock = new Object();

  /**
   * Initialize a newly created analysis context.
   */
  public AnalysisContextImpl() {
    super();
  }

  @Override
  public void applyChanges(ChangeSet changeSet) {
    if (changeSet.isEmpty()) {
      return;
    }
    synchronized (cacheLock) {
      //
      // First, update the contents of the sources while computing lists of sources that have been
      // added, changed or removed.
      //
      ArrayList<Source> addedSources = new ArrayList<Source>();
      for (Map.Entry<Source, String> entry : changeSet.getAddedWithContent().entrySet()) {
        Source source = entry.getKey();
        sourceFactory.setContents(source, entry.getValue());
        addedSources.add(source);
      }
      ArrayList<Source> changedSources = new ArrayList<Source>();
      for (Map.Entry<Source, String> entry : changeSet.getChangedWithContent().entrySet()) {
        Source source = entry.getKey();
        sourceFactory.setContents(source, entry.getValue());
        changedSources.add(source);
      }
      ArrayList<Source> removedSources = new ArrayList<Source>(changeSet.getRemoved());
      for (SourceContainer container : changeSet.getRemovedContainers()) {
        addSourcesInContainer(removedSources, container);
      }
      //
      // Then determine which cached results are no longer valid and what the new structure of the
      // sources is.
      // TODO(brianwilkerson) The code below is incomplete.
      //
      for (Source source : addedSources) {
        sourceAvailable(source);
      }
      for (Source source : changedSources) {
        sourceChanged(source);
      }
      for (Source source : removedSources) {
        sourceRemoved(source);
      }
    }
  }

  @Override
  public void clearResolution() {
    synchronized (cacheLock) {
      // TODO (danrubel): Optimize to only discard resolution information
      parseCache.clear();
      htmlParseCache.clear();
      libraryElementCache.clear();
      publicNamespaceCache.clear();
    }
  }

  @Override
  public void discard() {
    synchronized (cacheLock) {
      // TODO (danrubel): Optimize to recache the token stream and/or ASTs in a global context
      sourceMap.clear();
      parseCache.clear();
      htmlParseCache.clear();
      libraryElementCache.clear();
      publicNamespaceCache.clear();
    }
  }

  @Override
  public AnalysisContext extractContext(SourceContainer container) {
    AnalysisContextImpl newContext = (AnalysisContextImpl) AnalysisEngine.getInstance().createAnalysisContext();
    ArrayList<Source> sourcesToRemove = new ArrayList<Source>();
    synchronized (cacheLock) {
      // Move sources in the specified directory to the new context
      for (Map.Entry<Source, SourceInfo> entry : sourceMap.entrySet()) {
        Source source = entry.getKey();
        if (container.contains(source)) {
          sourcesToRemove.add(source);
          newContext.sourceMap.put(source, new SourceInfo(entry.getValue()));
        }
      }
//      for (Source source : sourcesToRemove) {
//        // TODO(brianwilkerson) Determine whether the source should be removed (that is, whether
//        // there are no additional dependencies on the source), and if so remove all information
//        // about the source.
//        sourceMap.remove(source);
//        parseCache.remove(source);
//        publicNamespaceCache.remove(source);
//        libraryElementCache.remove(source);
//      }
    }

    return newContext;
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
    if (!AnalysisEngine.isHtmlFileName(source.getShortName())) {
      return null;
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public Source[] getHtmlSources() {
    return getSources(SourceKind.HTML);
  }

  @Override
  public SourceKind getKnownKindOf(Source source) {
    String name = source.getShortName();
    if (AnalysisEngine.isHtmlFileName(name)) {
      return SourceKind.HTML;
    }
    if (!AnalysisEngine.isDartFileName(name)) {
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
  public Source[] getLaunchableClientLibrarySources() {
    // TODO(brianwilkerson) Implement this
    return getLibrarySources();
  }

  @Override
  public Source[] getLaunchableServerLibrarySources() {
    // TODO(brianwilkerson) Implement this
    return getLibrarySources();
  }

  @Override
  public Source[] getLibrariesContaining(Source source) {
    synchronized (cacheLock) {
      SourceInfo info = sourceMap.get(source);
      if (info == null) {
        return Source.EMPTY_ARRAY;
      }
      return info.getLibrarySources();
    }
  }

  @Override
  public LibraryElement getLibraryElement(Source source) {
    if (!AnalysisEngine.isDartFileName(source.getShortName())) {
      return null;
    }
    synchronized (cacheLock) {
      LibraryElement element = libraryElementCache.get(source);
      if (element == null) {
        if (getOrComputeKindOf(source) != SourceKind.LIBRARY) {
          return null;
        }
        LibraryResolver resolver = new LibraryResolver(this);
        try {
          element = resolver.resolveLibrary(source, true);
          if (element != null) {
            libraryElementCache.put(source, element);
          }
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
  public Source[] getLibrarySources() {
    return getSources(SourceKind.LIBRARY);
  }

  @Override
  public SourceKind getOrComputeKindOf(Source source) {
    SourceKind kind = getKnownKindOf(source);
    if (kind != null) {
      return kind;
    }
    return computeKindOf(source);
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
  public SourceFactory getSourceFactory() {
    return sourceFactory;
  }

  @Override
  public void mergeContext(AnalysisContext context) {
    synchronized (cacheLock) {
      for (Map.Entry<Source, SourceInfo> entry : ((AnalysisContextImpl) context).sourceMap.entrySet()) {
        Source newSource = entry.getKey();
        SourceInfo existingInfo = sourceMap.get(newSource);
        if (existingInfo == null) {
          // TODO(brianwilkerson) Decide whether we really need to copy the info.
          sourceMap.put(newSource, new SourceInfo(entry.getValue()));
        } else {
          // TODO(brianwilkerson) Decide whether/how to merge the info's.
        }
      }
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

  @Override
  public HtmlParseResult parseHtml(Source source) throws AnalysisException {
    synchronized (cacheLock) {
      HtmlParseResult result = htmlParseCache.get(source);
      if (result == null) {
        result = new HtmlParser(source).parse(scanHtml(source));
        htmlParseCache.put(source, result);
      }
      return result;
    }
  }

  @Override
  public ChangeNotice[] performAnalysisTask() {
    return ChangeNotice.EMPTY_ARRAY;
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
  public void setSourceFactory(SourceFactory factory) {
    if (sourceFactory == factory) {
      return;
    } else if (factory.getContext() != null) {
      throw new IllegalStateException("Source factories cannot be shared between contexts");
    } else if (sourceFactory != null) {
      sourceFactory.setContext(null);
    }
    factory.setContext(this);
    sourceFactory = factory;
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
   * Add all of the sources contained in the given source container to the given list of sources.
   * <p>
   * Note: This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param sources the list to which sources are to be added
   * @param container the source container containing the sources to be added to the list
   */
  private void addSourcesInContainer(ArrayList<Source> sources, SourceContainer container) {
    for (Source source : sourceMap.keySet()) {
      if (container.contains(source)) {
        sources.add(source);
      }
    }
  }

  private SourceKind computeKindOf(Source source) {
    try {
      if (hasPartOfDirective(parse(source))) {
        return SourceKind.PART;
      }
    } catch (AnalysisException exception) {
      return SourceKind.UNKNOWN;
    }
    return SourceKind.LIBRARY;
  }

  /**
   * Return an array containing all of the sources known to this context that have the given kind.
   * 
   * @param kind the kind of sources to be returned
   * @return all of the sources known to this context that have the given kind
   */
  private Source[] getSources(SourceKind kind) {
    ArrayList<Source> sources = new ArrayList<Source>();
    synchronized (cacheLock) {
      for (Map.Entry<Source, SourceInfo> entry : sourceMap.entrySet()) {
        if (entry.getValue().getKind() == kind) {
          sources.add(entry.getKey());
        }
      }
    }
    return sources.toArray(new Source[sources.size()]);
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

  private HtmlScanResult scanHtml(final Source source) throws AnalysisException {
    HtmlScanner scanner = new HtmlScanner(source);
    try {
      source.getContents(scanner);
    } catch (Exception exception) {
      throw new AnalysisException(exception);
    }
    return scanner.getResult();
  }

  /**
   * Note: This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source that has been added
   */
  private void sourceAvailable(Source source) {
    SourceInfo existingInfo = sourceMap.get(source);
    if (existingInfo == null) {
      SourceKind kind = computeKindOf(source);
      sourceMap.put(source, new SourceInfo(source, kind));
    }
  }

  /**
   * Note: This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source that has been changed
   */
  private void sourceChanged(Source source) {
    SourceInfo info = sourceMap.get(source);
    if (info == null) {
      // TODO(brianwilkerson) Figure out how to report this error.
      return;
    }
    parseCache.remove(source);
    htmlParseCache.remove(source);
    // TODO(brianwilkerson) Remove the two lines below once we are recording the library source.
    libraryElementCache.remove(source);
    publicNamespaceCache.remove(source);
    SourceKind oldKind = info.getKind();
    SourceKind newKind = computeKindOf(source);
    if (newKind != oldKind) {
      info.setKind(newKind);
    }
    for (Source librarySource : info.getLibrarySources()) {
      // TODO(brianwilkerson) This could be optimized. There's no need to flush these caches if the
      // public namespace hasn't changed, which will be a fairly common case.
      libraryElementCache.remove(librarySource);
      publicNamespaceCache.remove(librarySource);
    }
  }

  /**
   * Note: This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source that has been deleted
   */
  private void sourceRemoved(Source source) {
    // TODO(brianwilkerson) Determine whether the source should be removed (that is, whether
    // there are no additional dependencies on the source), and if so remove all information
    // about the source.
    SourceInfo info = sourceMap.get(source);
    if (info == null) {
      // TODO(brianwilkerson) Figure out how to report this error.
      return;
    }
    parseCache.remove(source);
    // TODO(brianwilkerson) Remove the two lines below once we are recording the library source.
    libraryElementCache.remove(source);
    publicNamespaceCache.remove(source);
    for (Source librarySource : info.getLibrarySources()) {
      // TODO(brianwilkerson) This could be optimized. There's no need to flush these caches if the
      // public namespace hasn't changed, which will be a fairly common case.
      libraryElementCache.remove(librarySource);
      publicNamespaceCache.remove(librarySource);
    }
    sourceMap.remove(source);
  }
}
