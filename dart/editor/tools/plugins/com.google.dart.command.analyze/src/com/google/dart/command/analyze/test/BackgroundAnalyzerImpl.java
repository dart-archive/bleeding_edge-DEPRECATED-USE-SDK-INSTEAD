/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.command.analyze.test;

import com.google.dart.command.analyze.AnalyzerImpl;
import com.google.dart.command.analyze.AnalyzerOptions;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.context.AnalysisResult;
import com.google.dart.engine.context.ChangeNotice;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorSeverity;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.general.TimeCounter.TimeCounterHandle;
import com.google.dart.engine.utilities.source.LineInfo;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scans, parses, and analyzes a library using {@link AnalysisContext#performAnalysisTask()} to
 * analyze.
 */
public class BackgroundAnalyzerImpl extends AnalyzerImpl {

  private static final String TASK_PACKAGE_NAME = "com.google.dart.engine.internal.task.";
  private final PerformanceMonitor getKindOfPM = new PerformanceMonitor("getKindOf");
  private final PerformanceMonitor getTaskPM = new PerformanceMonitor("getAnalysisTask");
  private final PerformanceMonitor performTaskPM = new PerformanceMonitor("performAnalysisTask");

  private HashMap<String, PerformanceMonitor> allMonitors = new HashMap<String, PerformanceMonitor>();
  {
    allMonitors.put(getKindOfPM.getName(), getKindOfPM);
    allMonitors.put(getTaskPM.getName(), getTaskPM);
    allMonitors.put(performTaskPM.getName(), performTaskPM);
  }

  private final ClientPerformanceTest client = new ClientPerformanceTest(getKindOfPM);

  public BackgroundAnalyzerImpl(AnalyzerOptions options) {
    super(options);
  }

  public Collection<PerformanceMonitor> getPerformanceMonitors() {
    return allMonitors.values();
  }

  @Override
  protected ErrorSeverity performAnalysis(AnalysisContext context, Source librarySource,
      File sourceFile, Map<Source, LineInfo> lineInfoMap, List<AnalysisError> errors)
      throws AnalysisException {

    ChangeSet changeSet = new ChangeSet();
    changeSet.addedSource(librarySource);
    context.applyChanges(changeSet);

    client.start(context, librarySource);
    while (true) {
      TimeCounterHandle timer = performTaskPM.start();
      AnalysisResult taskResult = context.performAnalysisTask();
      timer.stop();
      getTaskPM.recordElapsedMillis(taskResult.getGetTime());
      String taskName = taskResult.getTaskClassName();
      if (taskName != null) {
        long performTime = taskResult.getPerformTime();
        if (performTime >= 0) {
          PerformanceMonitor monitor = allMonitors.get(taskName);
          if (monitor == null) {
            String name = taskName;
            if (name.startsWith(TASK_PACKAGE_NAME)) {
              name = name.substring(TASK_PACKAGE_NAME.length());
            }
            monitor = new PerformanceMonitor(name);
            allMonitors.put(taskName, monitor);
          }
          monitor.recordElapsedMillis(performTime);
        }
      }

      ChangeNotice[] allNotices = taskResult.getChangeNotices();
      if (allNotices == null) {
        break;
      }
      for (ChangeNotice notice : allNotices) {
        Source source = notice.getSource();
        if (source != null) {
          LineInfo lineInfo = notice.getLineInfo();
          if (lineInfo != null) {
            lineInfoMap.put(source, lineInfo);
          }
        }
        AnalysisError[] newErrors = notice.getErrors();
        if (newErrors != null) {
          errors.addAll(Arrays.asList(newErrors));
        }
      }
    }
    client.stop();
    return getMaxErrorSeverity(errors);
  }
}
