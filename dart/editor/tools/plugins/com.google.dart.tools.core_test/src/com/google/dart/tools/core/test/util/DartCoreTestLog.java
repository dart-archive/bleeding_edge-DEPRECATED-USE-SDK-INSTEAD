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
package com.google.dart.tools.core.test.util;

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.osgi.framework.Bundle;

import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

/**
 * Used during test execution to intercept errors, warnings, and information that would normally be
 * logged to the Eclipse plugin log.
 */
public class DartCoreTestLog implements ILog {

  /**
   * The single instance of the receiver
   */
  private static final DartCoreTestLog LOG = new DartCoreTestLog();

  /**
   * Answer the single instance of the receiver
   */
  public static DartCoreTestLog getLog() {
    return LOG;
  }

  /**
   * A collection of log entries
   */
  private ArrayList<IStatus> content = new ArrayList<IStatus>();

  private DartCoreTestLog() {
  }

  @Override
  public void addLogListener(ILogListener listener) {
    // ignored
  }

  /**
   * Assert that the log is empty. This discards any current log entries so that future calls to
   * this method are not affected.
   */
  public void assertEmpty() {
    assertEntries();
  }

  /**
   * Assert that the log has entries with the specified severities in the specified order. This
   * discards any current log entries so that future calls to this method are not affected.
   */
  public void assertEntries(int... severities) {
    if (hasEntries(severities)) {
      content.clear();
      return;
    }
    StringWriter sw = new StringWriter(1000);
    PrintWriter pw = new PrintWriter(sw);
    pw.println("Expected " + severities.length + " log entries, but found " + content.size());
    if (severities.length > 0) {
      pw.println("  expected:");
      for (int severity : severities) {
        pw.println("    Status " + severityToString(severity));
      }
    }
    if (content.size() > 0) {
      ILog eclipseLog = DartCore.getPlugin().getLog();
      pw.println("  found:");
      for (IStatus status : content) {
        pw.println("    " + status);
        eclipseLog.log(status);
      }
      content.clear();
    }
    fail(sw.toString().trim());
  }

  @Override
  public Bundle getBundle() {
    return DartCore.getPlugin().getBundle();
  }

  @Override
  public void log(IStatus status) {
    content.add(status);
  }

  @Override
  public void removeLogListener(ILogListener listener) {
    // ignored
  }

  /**
   * Add a log listener that asserts no log entries
   */
  public void setUp() {
    DartCore.setPluginLog(this);
  }

  /**
   * Remove the log listener and dump any entries to the log
   */
  public void tearDown() {
    if (content.size() > 0) {
      ILog eclipseLog = DartCore.getPlugin().getLog();
      for (IStatus status : content) {
        eclipseLog.log(status);
      }
      content.clear();
    }
    DartCore.setPluginLog(null);
  }

  private boolean hasEntries(int... severities) {
    if (severities.length != content.size()) {
      return false;
    }
    int index = 0;
    for (int severity : severities) {
      if (severity != content.get(index++).getSeverity()) {
        return false;
      }
    }
    return true;
  }

  private String severityToString(int severity) {
    if (severity == IStatus.OK) {
      return "OK";
    }
    if (severity == IStatus.ERROR) {
      return "ERROR";
    }
    if (severity == IStatus.WARNING) {
      return "WARNING";
    }
    if (severity == IStatus.INFO) {
      return "INFO";
    }
    if (severity == IStatus.CANCEL) {
      return "CANCEL";
    }
    return "SEVERITY[" + severity + "]";
  }
}
