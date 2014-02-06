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
package com.google.dart.engine.internal.context;

import com.google.dart.engine.source.Source;

import java.util.ArrayList;

/**
 * Instances of the class {@code WorkManager} manage a list of sources that need to have analysis
 * work performed on them.
 */
public class WorkManager {
  /**
   * An array containing the various queues is priority order.
   */
  private ArrayList<Source>[] workQueues;

  /**
   * Initialize a newly created manager to have no work queued up.
   */
  @SuppressWarnings("unchecked")
  public WorkManager() {
    int queueCount = SourcePriority.values().length;
    workQueues = new ArrayList[queueCount];
    for (int i = 0; i < queueCount; i++) {
      workQueues[i] = new ArrayList<Source>();
    }
  }

  /**
   * Record that the given source needs to be analyzed. The priority level is used to control when
   * the source will be analyzed with respect to other sources.
   * 
   * @param source the source that needs to be analyzed
   * @param priority the priority level of the source
   */
  public void add(Source source, SourcePriority priority) {
    // TODO(brianwilkerson) Optimize the order of the libraries so that libraries that depend on
    // other libraries get analyzed after the other libraries.
    int queueCount = workQueues.length;
    int ordinal = priority.ordinal();
    for (int i = 0; i < queueCount; i++) {
      ArrayList<Source> queue = workQueues[i];
      if (i == ordinal) {
        if (!queue.contains(source)) {
          queue.add(source);
        }
      } else {
        queue.remove(source);
      }
    }
  }

  /**
   * Return the next source for which some analysis work needs to be done.
   * 
   * @return the next source for which some analysis work needs to be done
   */
  public Source getNextSource() {
    int queueCount = workQueues.length;
    for (int i = 0; i < queueCount; i++) {
      ArrayList<Source> queue = workQueues[i];
      if (!queue.isEmpty()) {
        return queue.get(0);
      }
    }
    return null;
  }

  /**
   * Record that the given source is fully analyzed.
   * 
   * @param source the source that is fully analyzed
   */
  public void remove(Source source) {
    int queueCount = workQueues.length;
    for (int i = 0; i < queueCount; i++) {
      workQueues[i].remove(source);
    }
  }
}
