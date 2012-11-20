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
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.internal.element.ElementLocationImpl;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.SourceImpl;

import junit.framework.TestCase;

import java.io.File;

public class AnalysisContextImplTest extends TestCase {
  public void fail_getElement_location() {
    AnalysisContextImpl context = new AnalysisContextImpl();
    ElementLocation location = new ElementLocationImpl("dart:core;Object");
    Element element = context.getElement(location);
    assertNotNull(element);
    assertEquals(location, element.getLocation());
  }

  public void fail_getLibraryElement_source() {
    AnalysisContextImpl context = new AnalysisContextImpl();
    SourceFactory sourceFactory = new SourceFactory();
    context.setSourceFactory(sourceFactory);
    Source source = new SourceImpl(sourceFactory, new File("/does/not/exist.dart"));
    Element element = context.getLibraryElement(source);
    assertNotNull(element);
  }

  public void fail_resolve() throws Exception {
    AnalysisContextImpl context = new AnalysisContextImpl();
    SourceFactory sourceFactory = new SourceFactory();
    context.setSourceFactory(sourceFactory);
    Source source = new SourceImpl(sourceFactory, new File("/does/not/exist.dart")) {
      @Override
      public void getContents(ContentReceiver receiver) throws Exception {
        receiver.accept("library lib;");
      }
    };
    GatheringErrorListener listener = new GatheringErrorListener();
    CompilationUnit compilationUnit = context.resolve(source, null, listener);
    assertNotNull(compilationUnit);
  }

  public void test_creation() {
    assertNotNull(new AnalysisContextImpl());
  }

  public void test_parse() throws Exception {
    AnalysisContextImpl context = new AnalysisContextImpl();
    SourceFactory sourceFactory = new SourceFactory();
    context.setSourceFactory(sourceFactory);
    Source source = new SourceImpl(sourceFactory, new File("/does/not/exist.dart")) {
      @Override
      public void getContents(ContentReceiver receiver) throws Exception {
        receiver.accept("library lib;");
      }
    };
    GatheringErrorListener listener = new GatheringErrorListener();
    CompilationUnit compilationUnit = context.parse(source, listener);
    assertNotNull(compilationUnit);
  }

  public void test_scan() throws Exception {
    AnalysisContextImpl context = new AnalysisContextImpl();
    SourceFactory sourceFactory = new SourceFactory();
    context.setSourceFactory(sourceFactory);
    Source source = new SourceImpl(sourceFactory, new File("/does/not/exist.dart")) {
      @Override
      public void getContents(ContentReceiver receiver) throws Exception {
        receiver.accept("library lib;");
      }
    };
    GatheringErrorListener listener = new GatheringErrorListener();
    Token token = context.scan(source, listener);
    assertNotNull(token);
  }

  public void test_setSourceFactory() {
    AnalysisContextImpl context = new AnalysisContextImpl();
    SourceFactory sourceFactory = new SourceFactory();
    context.setSourceFactory(sourceFactory);
    assertEquals(sourceFactory, context.getSourceFactory());
  }
}
