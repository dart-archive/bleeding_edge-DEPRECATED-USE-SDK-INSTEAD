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

import java.io.File;
import java.io.IOException;

/**
 * Scans, Parses, and analyzes a library.
 */
public class Analyzer {

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
   * Treats the <code>sourceFile</code> as the top level library and analyzes the unit for warnings
   * and errors.
   * 
   * @param sourceFile file to analyze
   * @param config configuration for this analysis pass
   * @param listener error listener
   * @return <code> true</code> on success, <code>false</code> on failure.
   */
  public String analyze(File sourceFile, AnalysisConfiguration config,
      AnalysisErrorListener listener) throws IOException {
    TraceEvent logEvent = Tracer.canTrace() ? Tracer.start(DartEventType.ANALYZE_TOP_LEVEL_LIBRARY,
        "src", sourceFile.toString()) : null;
    try {
      // TODO(zundel): Start scanning, parsing, and analyzing
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
