/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.core.internal.perf;

import static com.google.dart.tools.core.utilities.general.FormattedStringBuilder.appendLong;
import static com.google.dart.tools.core.utilities.general.FormattedStringBuilder.appendText;

/**
 * Represents the result of executing a particular metric.
 * 
 * @see Performance
 * @see Metric
 */
public class Result {
  private final Metric metric;
  private final long start;
  final long elapsed;
  private final String[] comments;

  Result(Metric metric, long start, String... comments) {
    this.metric = metric;
    this.start = start;
    this.elapsed = System.currentTimeMillis() - start;
    this.comments = comments;
  }

  public Metric getMetric() {
    return metric;
  }

  public long getStart() {
    return start;
  }

  /**
   * Log the elapsed time
   */
  public void print(int depth) {
    StringBuilder line = new StringBuilder();
    for (int i = 0; i < depth; i++) {
      line.append("   ");
    }
    appendText(line, metric.name, 26 - 3 * depth);
    appendLong(line, metric.threshold, 7);
    line.append(" ms ");
    line.append(metric.threshold < elapsed ? '<' : ' ');
    appendLong(line, elapsed, 7);
    line.append(" ms");
    for (String comment : comments) {
      line.append(", ");
      line.append(comment);
    }
    System.out.println(line.toString());
  }
}
