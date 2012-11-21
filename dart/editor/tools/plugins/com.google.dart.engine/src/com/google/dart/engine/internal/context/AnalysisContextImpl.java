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
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.scanner.CharBufferScanner;
import com.google.dart.engine.scanner.StringScanner;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;

import java.nio.CharBuffer;
import java.util.HashMap;

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
  private HashMap<Source, CompilationUnit> parseCache = new HashMap<Source, CompilationUnit>();

  /**
   * Initialize a newly created analysis context.
   */
  public AnalysisContextImpl() {
    super();
  }

  @Override
  public Element getElement(ElementLocation location) {
    throw new UnsupportedOperationException();
  }

  @Override
  public LibraryElement getLibraryElement(Source source) {
    throw new UnsupportedOperationException();
  }

  @Override
  public SourceFactory getSourceFactory() {
    return sourceFactory;
  }

  @Override
  public CompilationUnit parse(Source source, AnalysisErrorListener errorListener)
      throws AnalysisException {
    CompilationUnit unit = parseCache.get(source);
    if (unit == null) {
      Token token = scan(source, errorListener);
      Parser parser = new Parser(source, errorListener);
      unit = parser.parseCompilationUnit(token);
      parseCache.put(source, unit);
    }
    return unit;
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
}
