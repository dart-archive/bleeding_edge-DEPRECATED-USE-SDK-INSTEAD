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
package com.google.dart.command.analyze;

import com.google.dart.command.analyze.CommandLineOptions.AnalyzerOptions;
import com.google.dart.command.metrics.AnalysisMetrics;

import java.io.File;

/**
 * A configuration for the Dart Analyzer specifying which phases will be executed.
 */
public class CommandLineAnalysisConfiguration implements AnalysisConfiguration {

  private final AnalysisMetrics compilerMetrics;

  private final File dartSdkDirectory;

  private final boolean developerModeChecks;

  private final String jvmMetricOptions;

  private final String platformName;

  private final boolean suppressSdkWarnings;

  /**
   * A new instance with the specified {@link AnalyzerOptions}.
   */
  public CommandLineAnalysisConfiguration(AnalyzerOptions analyzerOptions) {
    this.developerModeChecks = analyzerOptions.developerModeChecks();
    this.dartSdkDirectory = analyzerOptions.dartSdkPath();
    this.jvmMetricOptions = analyzerOptions.jvmMetricOptions();
    this.platformName = analyzerOptions.platformName();
    if (analyzerOptions.showMetrics()) {
      this.compilerMetrics = new AnalysisMetrics();
    } else {
      this.compilerMetrics = null;
    }
    this.suppressSdkWarnings = analyzerOptions.suppressSdkWarnings();
  }

  @Override
  public AnalysisMetrics analyzerMetrics() {
    return compilerMetrics;
  }

  @Override
  public File dartSdkDirectory() {
    return dartSdkDirectory;
  }

  @Override
  public boolean developerModeChecks() {
    return developerModeChecks;
  }

  @Override
  public String jvmMetricOptions() {
    return jvmMetricOptions;
  }

  @Override
  public String platformName() {
    return platformName;
  }

  @Override
  public boolean showSdkWarnings() {
    return suppressSdkWarnings;
  }
}
