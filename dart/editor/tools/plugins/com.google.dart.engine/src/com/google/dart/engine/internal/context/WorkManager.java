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
import java.util.NoSuchElementException;

/**
 * Instances of the class {@code WorkManager} manage a list of sources that need to have analysis
 * work performed on them.
 */
public class WorkManager {
  /**
   * Instances of the class {@code WorkIterator} implement an iterator that returns the sources in a
   * work manager in the order in which they are to be analyzed.
   */
  public class WorkIterator {
    /**
     * The index of the work queue through which we are currently iterating.
     */
    private int queueIndex = 0;

    /**
     * The index of the next element of the work queue to be returned.
     */
    private int index = -1;

    /**
     * Initialize a newly created iterator to be ready to return the first element in the iteration.
     */
    public WorkIterator() {
      advance();
    }

    /**
     * Return {@code true} if there is another {@link Source} available for processing.
     * 
     * @return {@code true} if there is another {@link Source} available for processing
     */
    public boolean hasNext() {
      return queueIndex < workQueues.length;
    }

    /**
     * Return the next {@link Source} available for processing and advance so that the returned
     * source will not be returned again.
     * 
     * @return the next {@link Source} available for processing
     */
    public Source next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      Source source = workQueues[queueIndex].get(index);
      advance();
      return source;
    }

    /**
     * Increment the {@link #index} and {@link #queueIndex} so that they are either indicating the
     * next source to be returned or are indicating that there are no more sources to be returned.
     */
    private void advance() {
      index++;
      if (index >= workQueues[queueIndex].size()) {
        index = 0;
        queueIndex++;
        while (queueIndex < workQueues.length && workQueues[queueIndex].isEmpty()) {
          queueIndex++;
        }
      }
    }
  }

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
   * the source will be analyzed with respect to other sources. If the source was previously added
   * then it's priority is updated. If it was previously added with the same priority then it's
   * position in the queue is unchanged.
   * 
   * @param source the source that needs to be analyzed
   * @param priority the priority level of the source
   */
  public void add(Source source, SourcePriority priority) {
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
   * Record that the given source needs to be analyzed. The priority level is used to control when
   * the source will be analyzed with respect to other sources. If the source was previously added
   * then it's priority is updated. In either case, it will be analyzed before other sources of the
   * same priority.
   * 
   * @param source the source that needs to be analyzed
   * @param priority the priority level of the source
   */
  public void addFirst(Source source, SourcePriority priority) {
    int queueCount = workQueues.length;
    int ordinal = priority.ordinal();
    for (int i = 0; i < queueCount; i++) {
      ArrayList<Source> queue = workQueues[i];
      if (i == ordinal) {
        queue.remove(source);
        queue.add(0, source);
      } else {
        queue.remove(source);
      }
    }
  }

  /**
   * Return an iterator that can be used to access the sources to be analyzed in the order in which
   * they should be analyzed.
   * <p>
   * <b>Note:</b> As with other iterators, no sources can be added or removed from this work manager
   * while the iterator is being used. Unlike some implementations, however, the iterator will not
   * detect when this requirement has been violated; it might work correctly, it might return the
   * wrong source, or it might throw an exception.
   * 
   * @return an iterator that can be used to access the next source to be analyzed
   */
  public WorkIterator iterator() {
    return new WorkIterator();
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

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    SourcePriority[] priorities = SourcePriority.values();
    boolean needsSeparator = false;
    int queueCount = workQueues.length;
    for (int i = 0; i < queueCount; i++) {
      ArrayList<Source> queue = workQueues[i];
      if (!queue.isEmpty()) {
        if (needsSeparator) {
          builder.append("; ");
        }
        builder.append(priorities[i]);
        builder.append(": ");
        int queueSize = queue.size();
        for (int j = 0; j < queueSize; j++) {
          if (j > 0) {
            builder.append(", ");
          }
          builder.append(queue.get(j).getFullName());
        }
        needsSeparator = true;
      }
    }
    return builder.toString();
  }
}
