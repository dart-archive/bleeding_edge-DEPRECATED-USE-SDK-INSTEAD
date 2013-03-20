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
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.parser.HtmlParseResult;
import com.google.dart.engine.html.parser.HtmlParser;
import com.google.dart.engine.html.scanner.HtmlScanResult;
import com.google.dart.engine.html.scanner.HtmlScanner;
import com.google.dart.engine.internal.builder.HtmlUnitBuilder;
import com.google.dart.engine.internal.element.ElementImpl;
import com.google.dart.engine.internal.element.ElementLocationImpl;
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
   * A table mapping the sources known to the context to the information known about the source.
   */
  private final HashMap<Source, SourceInfo> sourceMap = new HashMap<Source, SourceInfo>();

  /**
   * A cache mapping sources to the html parse results that were produced for the contents of the
   * source.
   */
  // TODO (brianwilkerson): Remove this after removing parseHtml(Source).
  private HashMap<Source, HtmlParseResult> htmlParseCache = new HashMap<Source, HtmlParseResult>();

  /**
   * A table mapping sources to the change notices that are waiting to be returned related to that
   * source.
   */
  private HashMap<Source, ChangeNoticeImpl> pendingNotices = new HashMap<Source, ChangeNoticeImpl>();

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
      // Then determine which cached results are no longer valid.
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
  public AnalysisError[] computeErrors(Source source) throws AnalysisException {
    synchronized (cacheLock) {
      CompilationUnitInfo info = getCompilationUnitInfo(source);
      if (info == null) {
        return AnalysisError.NO_ERRORS;
      }
      if (info.hasInvalidParseErrors()) {
        parseCompilationUnit(source);
      }
      if (info.hasInvalidResolutionErrors()) {
        // TODO(brianwilkerson) Decide whether to resolve the source against all libraries or
        // whether to add a librarySource parameter to this method.
        // resolveCompilationUnit(source);
      }
      return info.getAllErrors();
    }
  }

  //@Override
  public HtmlElement computeHtmlElement(Source source) throws AnalysisException {
    if (!AnalysisEngine.isHtmlFileName(source.getShortName())) {
      return null;
    }
    synchronized (cacheLock) {
      HtmlUnitInfo htmlUnitInfo = getHtmlUnitInfo(source);
      if (htmlUnitInfo == null) {
        return null;
      }
      HtmlElement element = htmlUnitInfo.getElement();
      if (element == null) {
        HtmlUnitBuilder builder = new HtmlUnitBuilder(this);
        element = builder.buildHtmlElement(source, parseHtmlUnit(source));
        htmlUnitInfo.setElement(element);
      }
      return element;
    }
  }

  @Override
  public SourceKind computeKindOf(Source source) {
    synchronized (cacheLock) {
      SourceInfo sourceInfo = getSourceInfo(source);
      if (sourceInfo == null) {
        return SourceKind.UNKNOWN;
      } else if (sourceInfo instanceof DartInfo) {
        sourceInfo = internalComputeKindOf(source, sourceInfo);
        sourceMap.put(source, sourceInfo);
      }
      return sourceInfo.getKind();
    }
  }

  @Override
  public LibraryElement computeLibraryElement(Source source) throws AnalysisException {
    if (!AnalysisEngine.isDartFileName(source.getShortName())) {
      return null;
    }
    synchronized (cacheLock) {
      LibraryInfo libraryInfo = getLibraryInfo(source);
      if (libraryInfo == null) {
        return null;
      }
      LibraryElement element = libraryInfo.getElement();
      if (element == null) {
        if (computeKindOf(source) != SourceKind.LIBRARY) {
          return null;
        }
        LibraryResolver resolver = new LibraryResolver(this);
        try {
          element = resolver.resolveLibrary(source, true);
          if (element != null) {
            libraryInfo.setElement(element);
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
          newContext.sourceMap.put(source, entry.getValue().copy());
        }
      }

      // TODO (danrubel): Either remove sources or adjust contract described in AnalysisContext.
      // Currently, callers assume that sources have been removed from this context

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
    String[] components = ((ElementLocationImpl) location).getComponents();
    ElementImpl element;
    synchronized (cacheLock) {
      Source librarySource = sourceFactory.fromEncoding(components[0]);
      try {
        element = (ElementImpl) computeLibraryElement(librarySource);
      } catch (AnalysisException exception) {
        return null;
      }
    }
    for (int i = 1; i < components.length; i++) {
      if (element == null) {
        return null;
      }
      element = element.getChild(components[i]);
    }
    return element;
  }

  @Override
  public AnalysisError[] getErrors(Source source) {
    synchronized (cacheLock) {
      CompilationUnitInfo info = getCompilationUnitInfo(source);
      if (info == null) {
        return AnalysisError.NO_ERRORS;
      }
      return info.getAllErrors();
    }
  }

  @Override
  public HtmlElement getHtmlElement(Source source) {
    if (!AnalysisEngine.isHtmlFileName(source.getShortName())) {
      return null;
    }
    synchronized (cacheLock) {
      HtmlUnitInfo info = getHtmlUnitInfo(source);
      if (info == null) {
        return null;
      }
      return info.getElement();
    }
  }

  @Override
  public Source[] getHtmlSources() {
    return getSources(SourceKind.HTML);
  }

  //@Override
  public SourceKind getKindOf(Source source) {
    synchronized (cacheLock) {
      SourceInfo sourceInfo = getSourceInfo(source);
      if (sourceInfo == null) {
        return SourceKind.UNKNOWN;
      }
      return sourceInfo.getKind();
    }
  }

  @Override
  public SourceKind getKnownKindOf(Source source) {
    return getKindOf(source);
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
      CompilationUnitInfo compilationUnitInfo = getCompilationUnitInfo(source);
      if (compilationUnitInfo == null) {
        return Source.EMPTY_ARRAY;
      }
      return compilationUnitInfo.getLibrarySources();
    }
  }

  @Override
  public LibraryElement getLibraryElement(Source source) {
    try {
      return computeLibraryElement(source);
    } catch (AnalysisException exception) {
      return null;
    }
  }

  @Override
  public LibraryElement getLibraryElementOrNull(Source source) {
    synchronized (cacheLock) {
      LibraryInfo libraryInfo = getLibraryInfo(source);
      if (libraryInfo == null) {
        return null;
      }
      return libraryInfo.getElement();
    }
  }

  @Override
  public Source[] getLibrarySources() {
    return getSources(SourceKind.LIBRARY);
  }

  @Override
  public LineInfo getLineInfo(Source source) {
    synchronized (cacheLock) {
      SourceInfo sourceInfo = getSourceInfo(source);
      if (sourceInfo == null) {
        return null;
      }
      LineInfo lineInfo = sourceInfo.getLineInfo();
      if (lineInfo == null) {
        try {
          parse(source);
          lineInfo = sourceInfo.getLineInfo();
        } catch (AnalysisException exception) {
          AnalysisEngine.getInstance().getLogger().logError(
              "Could not parse " + source.getFullName(),
              exception);
        }
      }
      return lineInfo;
    }
  }

  @Override
  public SourceKind getOrComputeKindOf(Source source) {
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
    // TODO(brianwilkerson) Rename this to not start with 'get'. Note that this is not part of the
    // API of the interface.
    Source source = library.getDefiningCompilationUnit().getSource();
    synchronized (cacheLock) {
      LibraryInfo libraryInfo = getLibraryInfo(source);
      if (libraryInfo == null) {
        return null;
      }
      Namespace namespace = libraryInfo.getPublicNamespace();
      if (namespace == null) {
        NamespaceBuilder builder = new NamespaceBuilder();
        namespace = builder.createPublicNamespace(library);
        libraryInfo.setPublicNamespace(namespace);
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
    // TODO(brianwilkerson) Rename this to not start with 'get'. Note that this is not part of the
    // API of the interface.
    synchronized (cacheLock) {
      LibraryInfo libraryInfo = getLibraryInfo(source);
      if (libraryInfo == null) {
        return null;
      }
      Namespace namespace = libraryInfo.getPublicNamespace();
      if (namespace == null) {
        LibraryElement library = getLibraryElement(source);
        if (library == null) {
          return null;
        }
        NamespaceBuilder builder = new NamespaceBuilder();
        namespace = builder.createPublicNamespace(library);
        libraryInfo.setPublicNamespace(namespace);
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
        SourceInfo existingInfo = getSourceInfo(newSource);
        if (existingInfo == null) {
          // TODO(brianwilkerson) Decide whether we really need to copy the info.
          sourceMap.put(newSource, entry.getValue().copy());
        } else {
          // TODO(brianwilkerson) Decide whether/how to merge the info's.
        }
      }
    }
  }

  @Override
  public CompilationUnit parse(Source source) throws AnalysisException {
    return parseCompilationUnit(source);
  }

  @Override
  public CompilationUnit parseCompilationUnit(Source source) throws AnalysisException {
    synchronized (cacheLock) {
      CompilationUnitInfo compilationUnitInfo = getCompilationUnitInfo(source);
      if (compilationUnitInfo == null) {
        return null;
      }
      CompilationUnit unit = compilationUnitInfo.getResolvedCompilationUnit();
      if (unit == null) {
        unit = compilationUnitInfo.getParsedCompilationUnit();
        if (unit == null) {
          RecordingErrorListener errorListener = new RecordingErrorListener();
          ScanResult scanResult = internalScan(source, errorListener);
          Parser parser = new Parser(source, errorListener);
          unit = parser.parseCompilationUnit(scanResult.token);
          LineInfo lineInfo = new LineInfo(scanResult.lineStarts);
          AnalysisError[] errors = errorListener.getErrors(source);
          unit.setParsingErrors(errors);
          unit.setLineInfo(lineInfo);
          compilationUnitInfo.setLineInfo(lineInfo);
          compilationUnitInfo.setParsedCompilationUnit(unit);
          compilationUnitInfo.setParseErrors(errors);
        }
      }
      return unit;
    }
  }

  @Override
  public HtmlParseResult parseHtml(Source source) throws AnalysisException {
    synchronized (cacheLock) {
      SourceInfo sourceInfo = getSourceInfo(source);
      if (sourceInfo == null) {
        return null;
      }
      HtmlParseResult result = htmlParseCache.get(source);
      if (result == null) {
        result = new HtmlParser(source).parse(scanHtml(source));
        LineInfo lineInfo = new LineInfo(result.getLineStarts());
        htmlParseCache.put(source, result);
        sourceInfo.setLineInfo(lineInfo);
      }
      return result;
    }
  }

  @Override
  public HtmlUnit parseHtmlUnit(Source source) throws AnalysisException {
    synchronized (cacheLock) {
      HtmlUnitInfo htmlUnitInfo = getHtmlUnitInfo(source);
      if (htmlUnitInfo == null) {
        return null;
      }
      HtmlUnit unit = htmlUnitInfo.getResolvedUnit();
      if (unit == null) {
        unit = htmlUnitInfo.getParsedUnit();
        if (unit == null) {
          HtmlParseResult result = new HtmlParser(source).parse(scanHtml(source));
          unit = result.getHtmlUnit();
          htmlUnitInfo.setLineInfo(new LineInfo(result.getLineStarts()));
          htmlUnitInfo.setParsedUnit(unit);
        }
      }
      return unit;
    }
  }

  @Override
  public ChangeNotice[] performAnalysisTask() {
    synchronized (cacheLock) {
      if (!performSingleAnalysisTask()) {
        return null;
      }
      ChangeNotice[] notices = pendingNotices.values().toArray(
          new ChangeNotice[pendingNotices.size()]);
      pendingNotices.clear();
      return notices;
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
      for (Map.Entry<Source, LibraryElement> entry : elementMap.entrySet()) {
        LibraryInfo libraryInfo = getLibraryInfo(entry.getKey());
        if (libraryInfo != null) {
          libraryInfo.setElement(entry.getValue());
        }
      }
    }
  }

  /**
   * Give the resolution errors and line info associated with the given source, add the information
   * to the cache.
   * 
   * @param source the source with which the information is associated
   * @param errors the resolution errors associated with the source
   * @param lineInfo the line information associated with the source
   */
  public void recordResolutionErrors(Source source, AnalysisError[] errors, LineInfo lineInfo) {
    synchronized (cacheLock) {
      CompilationUnitInfo compilationUnitInfo = getCompilationUnitInfo(source);
      if (compilationUnitInfo != null) {
        compilationUnitInfo.setLineInfo(lineInfo);
        compilationUnitInfo.setResolutionErrors(errors);
      }
      getNotice(source).setErrors(compilationUnitInfo.getAllErrors(), lineInfo);
    }
  }

  /**
   * Give the resolved compilation unit associated with the given source, add the unit to the cache.
   * 
   * @param source the source with which the unit is associated
   * @param unit the compilation unit associated with the source
   */
  public void recordResolvedCompilationUnit(Source source, CompilationUnit unit) {
    synchronized (cacheLock) {
      CompilationUnitInfo compilationUnitInfo = getCompilationUnitInfo(source);
      if (compilationUnitInfo != null) {
        compilationUnitInfo.setResolvedCompilationUnit(unit);
        getNotice(source).setCompilationUnit(unit);
      }
    }
  }

  @Override
  public CompilationUnit resolve(Source source, LibraryElement library) throws AnalysisException {
    if (library == null) {
      return null;
    }
    return resolveCompilationUnit(source, library.getSource());
  }

  @Override
  public CompilationUnit resolveCompilationUnit(Source librarySource, Source unitSource)
      throws AnalysisException {
    synchronized (cacheLock) {
      CompilationUnitInfo compilationUnitInfo = getCompilationUnitInfo(unitSource);
      if (compilationUnitInfo == null) {
        return null;
      }
      // TODO(brianwilkerson) This doesn't ensure that the compilation unit was resolved in the
      // context of the specified library.
      CompilationUnit unit = compilationUnitInfo.getResolvedCompilationUnit();
      if (unit == null) {
        computeLibraryElement(librarySource);
        unit = compilationUnitInfo.getResolvedCompilationUnit();
      }
      return unit;
    }
  }

  @Override
  public HtmlUnit resolveHtmlUnit(Source unitSource) throws AnalysisException {
    synchronized (cacheLock) {
      HtmlUnitInfo htmlUnitInfo = getHtmlUnitInfo(unitSource);
      if (htmlUnitInfo == null) {
        return null;
      }
      HtmlUnit unit = htmlUnitInfo.getResolvedUnit();
      if (unit == null) {
        computeHtmlElement(unitSource);
        unit = htmlUnitInfo.getResolvedUnit();
      }
      return unit;
    }
  }

  @Override
  public void setSourceFactory(SourceFactory factory) {
    if (sourceFactory == factory) {
      return;
    } else if (factory.getContext() != null) {
      throw new IllegalStateException("Source factories cannot be shared between contexts");
    }
    synchronized (cacheLock) {
      if (sourceFactory != null) {
        sourceFactory.setContext(null);
      }
      factory.setContext(this);
      sourceFactory = factory;
      for (SourceInfo sourceInfo : sourceMap.values()) {
        if (sourceInfo instanceof HtmlUnitInfo) {
          ((HtmlUnitInfo) sourceInfo).invalidateResolvedUnit();
        } else if (sourceInfo instanceof CompilationUnitInfo) {
          CompilationUnitInfo compilationUnitInfo = (CompilationUnitInfo) sourceInfo;
          compilationUnitInfo.invalidateResolvedUnit();
          compilationUnitInfo.invalidateResolutionErrors();
          if (sourceInfo instanceof LibraryInfo) {
            LibraryInfo libraryInfo = (LibraryInfo) sourceInfo;
            libraryInfo.invalidateElement();
            libraryInfo.invalidatePublicNamespace();
          }
        }
      }
    }
  }

  @Override
  public Iterable<Source> sourcesToResolve(Source[] changedSources) {
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

  /**
   * Return the compilation unit information associated with the given source, or {@code null} if
   * the source is not known to this context. This method should be used to access the compilation
   * unit information rather than accessing the compilation unit map directly because sources in the
   * SDK are implicitly part of every analysis context and are therefore only added to the map when
   * first accessed.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source for which information is being sought
   * @return the compilation unit information associated with the given source
   */
  private CompilationUnitInfo getCompilationUnitInfo(Source source) {
    SourceInfo sourceInfo = getSourceInfo(source);
    if (sourceInfo == null) {
      sourceInfo = new CompilationUnitInfo();
      sourceMap.put(source, sourceInfo);
      return (CompilationUnitInfo) sourceInfo;
    } else if (sourceInfo instanceof CompilationUnitInfo) {
      return (CompilationUnitInfo) sourceInfo;
    } else if (sourceInfo instanceof DartInfo) {
      sourceInfo = internalComputeKindOf(source, sourceInfo);
      if (sourceInfo instanceof CompilationUnitInfo) {
        sourceMap.put(source, sourceInfo);
        return (CompilationUnitInfo) sourceInfo;
      }
    }
    return null;
  }

  /**
   * Return the HTML unit information associated with the given source, or {@code null} if the
   * source is not known to this context. This method should be used to access the HTML unit
   * information rather than accessing the HTML unit map directly because sources in the SDK are
   * implicitly part of every analysis context and are therefore only added to the map when first
   * accessed.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source for which information is being sought
   * @return the HTML unit information associated with the given source
   */
  private HtmlUnitInfo getHtmlUnitInfo(Source source) {
    SourceInfo sourceInfo = getSourceInfo(source);
    if (sourceInfo == null) {
      sourceInfo = new HtmlUnitInfo();
      sourceMap.put(source, sourceInfo);
      return (HtmlUnitInfo) sourceInfo;
    } else if (sourceInfo instanceof HtmlUnitInfo) {
      return (HtmlUnitInfo) sourceInfo;
    }
    return null;
  }

  /**
   * Return the library information associated with the given source, or {@code null} if the source
   * is not known to this context. This method should be used to access the library information
   * rather than accessing the library map directly because sources in the SDK are implicitly part
   * of every analysis context and are therefore only added to the map when first accessed.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source for which information is being sought
   * @return the library information associated with the given source
   */
  private LibraryInfo getLibraryInfo(Source source) {
    SourceInfo sourceInfo = getSourceInfo(source);
    if (sourceInfo == null) {
      sourceInfo = new LibraryInfo();
      sourceMap.put(source, sourceInfo);
      return (LibraryInfo) sourceInfo;
    } else if (sourceInfo instanceof LibraryInfo) {
      return (LibraryInfo) sourceInfo;
    } else if (sourceInfo instanceof DartInfo) {
      sourceInfo = internalComputeKindOf(source, sourceInfo);
      if (sourceInfo instanceof LibraryInfo) {
        sourceMap.put(source, sourceInfo);
        return (LibraryInfo) sourceInfo;
      }
    }
    return null;
  }

  /**
   * Return a change notice for the given source, creating one if one does not already exist.
   * 
   * @param source the source for which changes are being reported
   * @return a change notice for the given source
   */
  private ChangeNoticeImpl getNotice(Source source) {
    ChangeNoticeImpl notice = pendingNotices.get(source);
    if (notice == null) {
      notice = new ChangeNoticeImpl(source);
      pendingNotices.put(source, notice);
    }
    return notice;
  }

  /**
   * Return the source information associated with the given source, or {@code null} if the source
   * is not known to this context. This method should be used to access the source information
   * rather than accessing the source map directly because sources in the SDK are implicitly part of
   * every analysis context and are therefore only added to the map when first accessed.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source for which information is being sought
   * @return the source information associated with the given source
   */
  private SourceInfo getSourceInfo(Source source) {
    SourceInfo sourceInfo = sourceMap.get(source);
    if (sourceInfo == null) {
      sourceInfo = DartInfo.getInstance();
      sourceMap.put(source, sourceInfo);
    }
    return sourceInfo;
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

  private SourceInfo internalComputeKindOf(Source source, SourceInfo info) {
    try {
      RecordingErrorListener errorListener = new RecordingErrorListener();
      ScanResult scanResult = internalScan(source, errorListener);
      Parser parser = new Parser(source, errorListener);
      CompilationUnit unit = parser.parseCompilationUnit(scanResult.token);
      LineInfo lineInfo = new LineInfo(scanResult.lineStarts);
      AnalysisError[] errors = errorListener.getErrors(source);
      unit.setParsingErrors(errors);
      unit.setLineInfo(lineInfo);
      CompilationUnitInfo sourceInfo;
      if (hasPartOfDirective(unit)) {
        sourceInfo = new CompilationUnitInfo();
      } else {
        sourceInfo = new LibraryInfo();
      }
      sourceInfo.setLineInfo(lineInfo);
      sourceInfo.setParsedCompilationUnit(unit);
      sourceInfo.setParseErrors(errors);
      return sourceInfo;
    } catch (AnalysisException exception) {
      return info;
    }
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

  /**
   * Perform a single analysis task.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @return {@code true} if work was done and their might be {@link #pendingNotices} for the
   *         client, or {@code false} if no more work needs to be done.
   */
  private boolean performSingleAnalysisTask() {
    //
    // Look a source whose kind is not known.
    //
    for (Map.Entry<Source, SourceInfo> entry : sourceMap.entrySet()) {
      SourceInfo sourceInfo = entry.getValue();
      if (sourceInfo == DartInfo.getInstance() || sourceInfo.getKind() == null) {
        entry.setValue(internalComputeKindOf(entry.getKey(), sourceInfo));
        return true;
      }
    }
    //
    // Look a source that needs to be parsed.
    //
    for (Map.Entry<Source, SourceInfo> entry : sourceMap.entrySet()) {
      SourceInfo sourceInfo = entry.getValue();
      if (sourceInfo instanceof CompilationUnitInfo) {
        CompilationUnitInfo unitInfo = (CompilationUnitInfo) sourceInfo;
        if (unitInfo.hasInvalidParsedUnit()) {
          try {
            parseCompilationUnit(entry.getKey());
          } catch (AnalysisException exception) {
            unitInfo.setParsedCompilationUnit(null);
            AnalysisEngine.getInstance().getLogger().logError(
                "Could not parse " + entry.getKey().getFullName(),
                exception);
          }
          return true;
        }
      } else if (sourceInfo instanceof HtmlUnitInfo) {
        HtmlUnitInfo unitInfo = (HtmlUnitInfo) sourceInfo;
        if (unitInfo.hasInvalidParsedUnit()) {
          try {
            parseHtmlUnit(entry.getKey());
          } catch (AnalysisException exception) {
            unitInfo.setParsedUnit(null);
            AnalysisEngine.getInstance().getLogger().logError(
                "Could not parse " + entry.getKey().getFullName(),
                exception);
          }
          return true;
        }
      }
    }
    //
    // Look for a library that needs to be resolved.
    //
    for (Map.Entry<Source, SourceInfo> entry : sourceMap.entrySet()) {
      SourceInfo sourceInfo = entry.getValue();
      if (sourceInfo instanceof LibraryInfo) {
        LibraryInfo libraryInfo = (LibraryInfo) sourceInfo;
        if (libraryInfo.hasInvalidElement()) {
          try {
            computeLibraryElement(entry.getKey());
          } catch (AnalysisException exception) {
            libraryInfo.setElement(null);
            AnalysisEngine.getInstance().getLogger().logError(
                "Could not compute the library element for " + entry.getKey().getFullName(),
                exception);
          }
          return true;
        }
      }
    }
    return false;
  }

  private HtmlScanResult scanHtml(Source source) throws AnalysisException {
    HtmlScanner scanner = new HtmlScanner(source);
    try {
      source.getContents(scanner);
    } catch (Exception exception) {
      throw new AnalysisException(exception);
    }
    return scanner.getResult();
  }

  /**
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source that has been added
   */
  private void sourceAvailable(Source source) {
    SourceInfo existingInfo = sourceMap.get(source);
    if (existingInfo == null) {
      String name = source.getShortName();
      if (AnalysisEngine.isHtmlFileName(name)) {
        sourceMap.put(source, new HtmlUnitInfo());
      } else if (AnalysisEngine.isDartFileName(name)) {
        sourceMap.put(source, DartInfo.getInstance());
      }
    }
  }

  /**
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source that has been changed
   */
  private void sourceChanged(Source source) {
    SourceInfo sourceInfo = sourceMap.get(source);
    if (sourceInfo instanceof HtmlUnitInfo) {
      HtmlUnitInfo htmlUnitInfo = (HtmlUnitInfo) sourceInfo;
      htmlUnitInfo.invalidateLineInfo();
      htmlUnitInfo.invalidateParsedUnit();
      htmlUnitInfo.invalidateResolvedUnit();
    } else if (sourceInfo instanceof CompilationUnitInfo) {
      CompilationUnitInfo compilationUnitInfo = (CompilationUnitInfo) sourceInfo;
      for (Source librarySource : compilationUnitInfo.getLibrarySources()) {
        // TODO(brianwilkerson) This could be optimized. There's no need to flush these caches if
        // the public namespace hasn't changed, which will be a fairly common case.
        LibraryInfo libraryInfo = getLibraryInfo(librarySource);
        if (libraryInfo != null) {
          libraryInfo.invalidateElement();
          libraryInfo.invalidatePublicNamespace();
        }
      }
      sourceMap.put(source, DartInfo.getInstance());
    }
  }

  /**
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source that has been deleted
   */
  private void sourceRemoved(Source source) {
    // TODO(brianwilkerson) Determine whether the source should be removed (that is, whether
    // there are no additional dependencies on the source), and if so remove all information
    // about the source.
    CompilationUnitInfo compilationUnitInfo = getCompilationUnitInfo(source);
    if (compilationUnitInfo != null) {
      for (Source librarySource : compilationUnitInfo.getLibrarySources()) {
        // TODO(brianwilkerson) This could be optimized. There's no need to flush these caches if
        // the public namespace hasn't changed, which will be a fairly common case.
        LibraryInfo libraryInfo = getLibraryInfo(librarySource);
        if (libraryInfo != null) {
          libraryInfo.invalidateElement();
          libraryInfo.invalidatePublicNamespace();
        }
      }
    }
    sourceMap.remove(source);
  }
}
