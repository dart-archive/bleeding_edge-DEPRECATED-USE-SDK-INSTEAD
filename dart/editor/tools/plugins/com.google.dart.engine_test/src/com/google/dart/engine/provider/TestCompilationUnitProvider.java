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
package com.google.dart.engine.provider;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.scanner.StringScanner;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.Source;

import junit.framework.Assert;

import java.util.HashMap;

/**
 * Instances of the class {@code TestCompilationUnitProvider} implement a compilation unit provider
 * that can be used within tests.
 */
public class TestCompilationUnitProvider implements CompilationUnitProvider {
  /**
   * The content provider used to get the contents of the sources.
   */
  private TestSourceContentProvider contentProvider;

  /**
   * The error listener to which errors are reported while parsing the compilation units provided by
   * this provider.
   */
  private AnalysisErrorListener errorListener;

  /**
   * A table mapping sources to the compilation units resulting from parsing the contents of those
   * sources.
   */
  private HashMap<Source, CompilationUnit> sourceMap = new HashMap<Source, CompilationUnit>();

  /**
   * Initialize a newly create compilation unit provider.
   * 
   * @param contentProvider the content provider used to get the contents of the sources
   * @param errorListener the error listener to which errors are reported while parsing the
   *          compilation units provided by this provider
   */
  public TestCompilationUnitProvider(TestSourceContentProvider contentProvider,
      AnalysisErrorListener errorListener) {
    this.contentProvider = contentProvider;
  }

  @Override
  public CompilationUnit getCompilationUnit(Source source) {
    CompilationUnit unit = sourceMap.get(source);
    if (unit == null) {
      String contents = contentProvider.getSource(source);
      if (contents == null) {
        Assert.fail("Could not get contents for " + source.getFullName());
      }
      //
      // Scan the file.
      //
      StringScanner scanner = new StringScanner(source, contents, errorListener);
      Token token = scanner.tokenize();
      //
      // Parse the file.
      //
      Parser parser = new Parser(source, errorListener);
      unit = parser.parseCompilationUnit(token);
      sourceMap.put(source, unit);
    }
    return unit;
  }
}
