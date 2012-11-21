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

import com.google.common.collect.Lists;
import com.google.dart.command.analyze.CommandLineOptions.AnalyzerOptions;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.LineInfo;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;

public class CommandLineErrorListener implements AnalysisErrorListener {
  private final AnalyzerOptions options;

  /**
   * A table mapping sources to the line information for the source.
   */
  private HashMap<Source, LineInfo> lineInfoMap = new HashMap<Source, LineInfo>();

  private final List<AnalysisError> events = Lists.newArrayList();
  int warningCount = 0;
  int errorCount = 0;

  CommandLineErrorListener(AnalyzerOptions options) {
    this.options = options;
  }

  public void clearErrors() {
    events.clear();
  }

  public boolean hasFatalErrors() {
    if (options.warningsAreFatal() && warningCount > 0) {
      return true;
    }
    return errorCount > 0;
  }

  @Override
  public void onError(AnalysisError event) {
    events.add(event);
  }

  public void printFormattedErrors(PrintStream stream) {
    CommandLineErrorFormatter formatter = new CommandLineErrorFormatter(
        stream,
        true,
        options.printErrorFormat());
    for (AnalysisError event : events) {
      formatter.format(event, lineInfoMap.get(event.getSource()));
    }
  }

  /**
   * Set the line information associated with the given source to the given information.
   * 
   * @param source the source with which the line information is associated
   * @param lineInfo the line information to be associated with the source
   */
  public void setLineInfo(Source source, LineInfo lineInfo) {
    lineInfoMap.put(source, lineInfo);
  }
}
