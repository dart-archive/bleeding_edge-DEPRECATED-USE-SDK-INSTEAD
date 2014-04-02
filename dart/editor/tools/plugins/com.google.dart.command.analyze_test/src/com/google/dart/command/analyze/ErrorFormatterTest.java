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
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.TestSource;
import com.google.dart.engine.utilities.source.LineInfo;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class ErrorFormatterTest extends TestCase {

  public void test_format_machine() throws UnsupportedEncodingException {
    AnalyzerOptions options = AnalyzerOptions.createFromArgs(new String[] {"--format=machine"});

    Source source = new TestSource();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Map<Source, LineInfo> lineInfoMap = new HashMap<Source, LineInfo>();
    lineInfoMap.put(source, new LineInfo(new int[] {0}));
    ErrorFormatter formatter = new ErrorFormatter(new PrintStream(out), options, lineInfoMap);

    AnalysisError error = new AnalysisError(
        source,
        ResolverErrorCode.MISSING_LIBRARY_DIRECTIVE_WITH_PART);

    formatter.formatError(error);

    String actual = out.toString("UTF-8").trim();

    assertEquals("ERROR|COMPILE_TIME_ERROR|MISSING_LIBRARY_DIRECTIVE_WITH_PART|/test.dart|1|1|0|"
        + "Libraries that have parts must have a library directive", actual);
  }

  public void test_format_machine_deprecated() throws UnsupportedEncodingException {
    AnalyzerOptions options = AnalyzerOptions.createFromArgs(new String[] {"--machine"});

    Source source = new TestSource();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Map<Source, LineInfo> lineInfoMap = new HashMap<Source, LineInfo>();
    lineInfoMap.put(source, new LineInfo(new int[] {0}));
    ErrorFormatter formatter = new ErrorFormatter(new PrintStream(out), options, lineInfoMap);

    AnalysisError error = new AnalysisError(
        source,
        ResolverErrorCode.MISSING_LIBRARY_DIRECTIVE_WITH_PART);

    formatter.formatError(error);

    String actual = out.toString("UTF-8").trim();

    assertEquals("ERROR|COMPILE_TIME_ERROR|MISSING_LIBRARY_DIRECTIVE_WITH_PART|/test.dart|1|1|0|"
        + "Libraries that have parts must have a library directive", actual);
  }

  public void test_format_normal() throws UnsupportedEncodingException {
    AnalyzerOptions options = new AnalyzerOptions();

    Source source = new TestSource();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Map<Source, LineInfo> lineInfoMap = new HashMap<Source, LineInfo>();
    lineInfoMap.put(source, new LineInfo(new int[] {0}));
    ErrorFormatter formatter = new ErrorFormatter(new PrintStream(out), options, lineInfoMap);

    AnalysisError error = new AnalysisError(
        source,
        ResolverErrorCode.MISSING_LIBRARY_DIRECTIVE_WITH_PART);

    formatter.formatError(error);

    String actual = out.toString("UTF-8").trim();

    assertEquals(
        "[error] Libraries that have parts must have a library directive (/test.dart, line 1, col 1)",
        actual);
  }

  public void test_format_withError() throws UnsupportedEncodingException {
    AnalyzerOptions options = new AnalyzerOptions();

    Source source = new TestSource(createFile("/test.dart"), "import 'foo.dart");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Map<Source, LineInfo> lineInfoMap = new HashMap<Source, LineInfo>();
    lineInfoMap.put(source, new LineInfo(new int[] {0}));
    ErrorFormatter formatter = new ErrorFormatter(new PrintStream(out), options, lineInfoMap);

    AnalysisError error = new AnalysisError(
        source,
        ResolverErrorCode.MISSING_LIBRARY_DIRECTIVE_WITH_PART);

    formatter.formatError(error);

    String actual = out.toString("UTF-8").trim();

    assertEquals(
        "[error] Libraries that have parts must have a library directive (/test.dart, line 1, col 1)",
        actual);
  }
}
