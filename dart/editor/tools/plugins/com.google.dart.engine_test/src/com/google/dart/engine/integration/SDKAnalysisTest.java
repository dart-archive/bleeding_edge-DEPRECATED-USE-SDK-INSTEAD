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
package com.google.dart.engine.integration;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.cmdline.Analyzer;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.parser.ASTValidator;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.scanner.CharBufferScanner;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;

import junit.framework.Test;

import java.io.File;
import java.io.IOException;
import java.nio.CharBuffer;

public class SDKAnalysisTest extends DirectoryBasedSuiteBuilder {
  /**
   * Build a JUnit test suite that will analyze all of the files in the SDK.
   * 
   * @return the test suite that was built
   */
  public static Test suite() {
    File directory = DartSdk.getDefaultSdkDirectory();
    return new SDKAnalysisTest().buildSuite(directory, "Analyze SDK files");
  }

  @Override
  protected void testSingleFile(File sourceFile) throws IOException {
    //
    // Scan the file.
    //
    CharBuffer buffer = Analyzer.getBufferFromFile(sourceFile);
    Source source = new SourceFactory().forFile(sourceFile);
    GatheringErrorListener listener = new GatheringErrorListener();
    CharBufferScanner scanner = new CharBufferScanner(source, buffer, listener);
    Token token = scanner.tokenize();
    //
    // Parse the file.
    //
    Parser parser = new Parser(source, listener);
    CompilationUnit unit = parser.parseCompilationUnit(token);
    // Uncomment the lines below to stop reporting failures for files containing directives.
//    if (listener.hasError(ParserErrorCode.UNEXPECTED_TOKEN)) {
//      return;
//    }
    listener.assertNoErrors();
    //
    // Validate that the AST structure was built correctly.
    //
    ASTValidator validator = new ASTValidator();
    unit.accept(validator);
    validator.assertValid();
  }
}
