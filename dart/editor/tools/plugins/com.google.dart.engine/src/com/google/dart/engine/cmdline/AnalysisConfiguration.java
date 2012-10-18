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

import com.google.dart.engine.metrics.AnalysisMetrics;

import java.io.File;

/**
 * A configuration for the Dart analyzer.
 */
public interface AnalysisConfiguration {

  /**
   * Returns the {@link AnalysisMetrics} instance or {@code null} if metrics should not be
   * recorded.
   * 
   * @return the metrics instance, {@code null} if metrics should not be recorded
   */
  AnalysisMetrics analyzerMetrics();

  /**
   * Returns the directory where the Dart SDK is stored.
   */
  File dartSdkDirectory();

  /**
   * Indicates whether developer-mode runtime checks are needed.
   * 
   * @return true if developer-mode checks should be inserted, false if not
   */
  boolean developerModeChecks();

  /**
   * Returns a comma-separated string list of options for displaying JVM metrics. Returns
   * {@code null} if JVM metrics are not enabled.
   */
  String jvmMetricOptions();

  /**
   * Returns the name of the platform (used for finding system libraries).
   */
  String platformName();

  /**
   * Returns {@code true} if warnings in the SDK should not be forwarded to the error listener.
   */
  boolean showSdkWarnings();
}
