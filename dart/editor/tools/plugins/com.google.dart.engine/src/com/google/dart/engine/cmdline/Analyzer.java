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
package com.google.dart.engine.cmdline;

import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.metrics.AnalysisMetrics;
import com.google.dart.engine.metrics.DartEventType;
import com.google.dart.engine.metrics.JvmMetrics;
import com.google.dart.engine.metrics.Tracer;
import com.google.dart.engine.metrics.Tracer.TraceEvent;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.scanner.CharBufferScanner;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.utilities.source.LineInfo;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

/**
 * Scans, Parses, and analyzes a library.
 */
public class Analyzer {
  /**
   * The character set used to decode bytes into characters.
   */
  private static final Charset utf8Charset = Charset.forName("UTF-8");

  /**
   * Return a character buffer containing the contents of the file represented by the given source,
   * or {@code null} if the given source does not represent a file.
   * 
   * @param source the source representing the file whose contents are being requested
   * @return the contents of the given source file represented as a character buffer
   * @throws IOException if the contents of the file cannot be read
   */
  public static CharBuffer getBufferFromFile(File sourceFile) throws IOException {
    RandomAccessFile file = new RandomAccessFile(sourceFile.getAbsolutePath(), "r");
    FileChannel channel = null;
    ByteBuffer byteBuffer = null;
    try {
      channel = file.getChannel();
      long size = channel.size();
      if (size > Integer.MAX_VALUE) {
        throw new IllegalStateException("File is too long to be read");
      }
      int length = (int) size;
      byte[] bytes = new byte[length];
      byteBuffer = ByteBuffer.wrap(bytes);
      byteBuffer.position(0);
      byteBuffer.limit(length);
      channel.read(byteBuffer);
    } finally {
      if (channel != null) {
        try {
          channel.close();
        } catch (IOException exception) {
          // Ignored
        }
      }
    }
    byteBuffer.position(0);
    return utf8Charset.decode(byteBuffer);
  }

  private static void maybeShowMetrics(AnalysisConfiguration config) {
    AnalysisMetrics analyzerMetrics = config.analyzerMetrics();
    if (analyzerMetrics != null) {
      analyzerMetrics.write(System.out);
    }
    JvmMetrics.maybeWriteJvmMetrics(System.out, config.jvmMetricOptions());
  }

  public Analyzer() {
  }

  /**
   * Treats the {@code sourceFile} as the top level library and analyzes the unit for warnings
   * and errors.
   * 
   * @param sourceFile file to analyze
   * @param config configuration for this analysis pass
   * @param listener error listener
   * @return {@code  true} on success, {@code false} on failure.
   */
  public String analyze(File sourceFile, AnalysisConfiguration config,
      AnalysisErrorListener listener) throws IOException {
    TraceEvent logEvent = Tracer.canTrace() ? Tracer.start(
        DartEventType.ANALYZE_TOP_LEVEL_LIBRARY,
        "src",
        sourceFile.toString()) : null;
    try {
      // TODO(zundel): Start scanning, parsing, and analyzing
      CharBuffer buffer = getBufferFromFile(sourceFile);
      Source source = new SourceFactory().forFile(sourceFile);
      CharBufferScanner scanner = new CharBufferScanner(source, buffer, listener);
      Token token = scanner.tokenize();
      ((CommandLineErrorListener) listener).setLineInfo(
          source,
          new LineInfo(scanner.getLineStarts()));
      Parser parser = new Parser(source, listener);
      parser.parseCompilationUnit(token);
      System.err.println("Not Implemented");
    } finally {
      Tracer.end(logEvent);
    }

    logEvent = Tracer.canTrace() ? Tracer.start(DartEventType.WRITE_METRICS) : null;
    try {
      maybeShowMetrics(config);
    } finally {
      Tracer.end(logEvent);
    }
    return null;
  }
}
