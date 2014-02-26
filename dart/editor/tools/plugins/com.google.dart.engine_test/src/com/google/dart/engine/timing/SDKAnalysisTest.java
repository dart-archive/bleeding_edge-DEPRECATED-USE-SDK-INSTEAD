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
package com.google.dart.engine.timing;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.scanner.CharSequenceReader;
import com.google.dart.engine.scanner.Scanner;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;

import junit.framework.TestCase;

public class SDKAnalysisTest extends TestCase {
  public void test_parseTimeAnalysis() throws AnalysisException {
    DartSdk sdk = DirectoryBasedDartSdk.getDefaultSdk();
    SourceFactory sourceFactory = new SourceFactory(new DartUriResolver(sdk), new FileUriResolver());
    AnalysisContext context = AnalysisEngine.getInstance().createAnalysisContext();
    context.setSourceFactory(sourceFactory);

    long startTime = System.currentTimeMillis();
    //Number of iterations to test
    int iterations = 1000;

    for (int i = 0; i < iterations; i++) {
      for (String dartUri : sdk.getUris()) {
        Source source = sourceFactory.forUri(dartUri);

        internalParse(context, source, AnalysisErrorListener.NULL_LISTENER);

      }
    }
    long endTime = System.currentTimeMillis();

    System.out.println("Iterations: " + String.valueOf(iterations) + " Parse completed in "
        + String.valueOf(endTime - startTime));

    //LukeChurch: As of 2013-04-12
    //impact of instrumentation is 0.3ms to tokenize the SDK, representing 0.6% of the total time.

  }

  /**
   * Test for observing the impact of instrumentation on the scanner this can only be run manually
   * by enabling or disabling the instrumentation code and re-running this test to observe the
   * change
   * 
   * @throws AnalysisException
   */
  public void test_scanTimeAnalysis() throws AnalysisException {
    DartSdk sdk = DirectoryBasedDartSdk.getDefaultSdk();
    SourceFactory sourceFactory = new SourceFactory(new DartUriResolver(sdk), new FileUriResolver());
    AnalysisContext context = AnalysisEngine.getInstance().createAnalysisContext();
    context.setSourceFactory(sourceFactory);

    long startTime = System.currentTimeMillis();
    //Number of iterations to test
    int iterations = 1000;

    for (int i = 0; i < iterations; i++) {
      for (String dartUri : sdk.getUris()) {
        Source source = sourceFactory.forUri(dartUri);
        internalScan(context, source, AnalysisErrorListener.NULL_LISTENER);
      }
    }
    long endTime = System.currentTimeMillis();

    System.out.println("Iterations: " + String.valueOf(iterations) + " Scan completed in "
        + String.valueOf(endTime - startTime));

    //LukeChurch: As of 2013-04-12
    //impact of instrumentation is 0.3ms to tokenize the SDK, representing 0.6% of the total time.

  }

  private void internalParse(AnalysisContext context, final Source source,
      final AnalysisErrorListener errorListener) throws AnalysisException {
    try {
      Scanner scanner = new Scanner(source, new CharSequenceReader(
          context.getContents(source).getData()), errorListener);
      Token token = scanner.tokenize();
      Parser parser = new Parser(source, AnalysisErrorListener.NULL_LISTENER);
      parser.parseCompilationUnit(token);
    } catch (Exception exception) {
      throw new AnalysisException(exception);
    }
  }

  private void internalScan(AnalysisContext context, final Source source,
      final AnalysisErrorListener errorListener) throws AnalysisException {
    try {
      Scanner scanner = new Scanner(source, new CharSequenceReader(
          context.getContents(source).getData()), errorListener);
      scanner.tokenize();
    } catch (Exception exception) {
      throw new AnalysisException(exception);
    }
  }
}
