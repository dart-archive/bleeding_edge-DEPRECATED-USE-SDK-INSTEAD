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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.resolver.scope.Namespace;
import com.google.dart.engine.resolver.scope.NamespaceBuilder;
import com.google.dart.engine.scanner.CharBufferScanner;
import com.google.dart.engine.scanner.StringScanner;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.SourceFactory;

import java.io.File;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.List;

/**
 * Instances of the class {@code AnalysisContextImpl} implement an {@link AnalysisContext analysis
 * context}.
 */
public class AnalysisContextImpl implements AnalysisContext {
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
  public void clearResolution() {
    // TODO (danrubel): Optimize to only discard resolution information
    discard();
  }

  @Override
  public void discard() {
    // TODO (danrubel): Optimize to recache the token stream and/or ASTs in a global context
    parseCache.clear();
    libraryElementCache.clear();
    publicNamespaceCache.clear();
  }

  @Override
  public void filesDeleted(File file) {
    // TODO (danrubel): Optimize to remove only the specified files
    discard();
  }

  @Override
  public List<SourceContainer> getDependedOnContainers(SourceContainer container) {
    throw new UnsupportedOperationException();
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
  public LibraryElement getLibraryElement(Source source) {
    throw new UnsupportedOperationException();
//    synchronized (cacheLock) {
//      LibraryElement element = libraryElementCache.get(source);
//      if (element == null) {
//        // TODO(brianwilkerson) Build the library element.
//        element = ...;
//        libraryElementCache.put(source, element);
//      }
//      return element;
//    }
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
  public CompilationUnit parse(Source source, AnalysisErrorListener errorListener)
      throws AnalysisException {
    synchronized (cacheLock) {
      CompilationUnit unit = parseCache.get(source);
      if (unit == null) {
        Token token = scan(source, errorListener);
        Parser parser = new Parser(source, errorListener);
        unit = parser.parseCompilationUnit(token);
        parseCache.put(source, unit);
      }
      return unit;
    }
  }

  @Override
  public CompilationUnit resolve(Source source, LibraryElement library,
      AnalysisErrorListener errorListener) throws AnalysisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Token scan(final Source source, final AnalysisErrorListener errorListener)
      throws AnalysisException {
    final Token[] tokens = new Token[1];
    Source.ContentReceiver receiver = new Source.ContentReceiver() {
      @Override
      public void accept(CharBuffer contents) {
        CharBufferScanner scanner = new CharBufferScanner(source, contents, errorListener);
        tokens[0] = scanner.tokenize();
      }

      @Override
      public void accept(String contents) {
        StringScanner scanner = new StringScanner(source, contents, errorListener);
        tokens[0] = scanner.tokenize();
      }
    };
    try {
      source.getContents(receiver);
    } catch (Exception exception) {
    }
    return tokens[0];
  }

  @Override
  public void setSourceFactory(SourceFactory sourceFactory) {
    this.sourceFactory = sourceFactory;
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
    sourceChanged(source);
  }
}
