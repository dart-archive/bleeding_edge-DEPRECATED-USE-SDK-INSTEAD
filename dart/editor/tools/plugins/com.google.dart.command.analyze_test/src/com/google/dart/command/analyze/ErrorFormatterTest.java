/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.command.analyze;

import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.resolver.ResolverErrorCode;
import com.google.dart.engine.source.TestSource;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

public class ErrorFormatterTest extends TestCase {

  public void test_format_machine() throws UnsupportedEncodingException {
    AnalyzerOptions options = AnalyzerOptions.createFromArgs(new String[] {"--machine"});

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ErrorFormatter formatter = new ErrorFormatter(new PrintStream(out), options);

    AnalysisError error = new AnalysisError(
        new TestSource(),
        ResolverErrorCode.MISSING_LIBRARY_DIRECTIVE_WITH_PART);

    formatter.formatError(error);

    String actual = out.toString("UTF-8").trim();

    assertEquals("ERROR|COMPILE_TIME_ERROR|MISSING_LIBRARY_DIRECTIVE_WITH_PART|/test.dart|1|1|0|"
        + "Libraries that have parts must have a library directive", actual);
  }

  public void test_format_normal() throws UnsupportedEncodingException {
    AnalyzerOptions options = new AnalyzerOptions();

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ErrorFormatter formatter = new ErrorFormatter(new PrintStream(out), options);

    AnalysisError error = new AnalysisError(
        new TestSource(),
        ResolverErrorCode.MISSING_LIBRARY_DIRECTIVE_WITH_PART);

    formatter.formatError(error);

    String actual = out.toString("UTF-8").trim();

    assertEquals(
        "[error] Libraries that have parts must have a library directive (/test.dart:1:1)",
        actual);
  }
}
