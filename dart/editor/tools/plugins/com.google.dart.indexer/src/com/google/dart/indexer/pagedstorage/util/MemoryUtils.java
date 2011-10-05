/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.indexer.pagedstorage.util;

/**
 * This is a utility class with functions to measure the free and used memory.
 */
public class MemoryUtils {
  public static final int[] EMPTY_INTS = new int[0];

  private static long lastGC;
  private static final int GC_DELAY = 50;
  private static final int MAX_GC = 8;

  public static int getMemoryFree() {
    collectGarbage();
    Runtime rt = Runtime.getRuntime();
    long mem = rt.freeMemory();
    return (int) (mem >> 10);
  }

  /**
   * Get the used memory in KB.
   * 
   * @return the used memory
   */
  public static int getMemoryUsed() {
    collectGarbage();
    Runtime rt = Runtime.getRuntime();
    long mem = rt.totalMemory() - rt.freeMemory();
    return (int) (mem >> 10);
  }

  public static int[] newInts(int len) {
    if (len == 0) {
      return EMPTY_INTS;
    }
    return new int[len];
  }

  private static synchronized void collectGarbage() {
    Runtime runtime = Runtime.getRuntime();
    long total = runtime.totalMemory();
    long time = System.currentTimeMillis();
    if (lastGC + GC_DELAY < time) {
      for (int i = 0; i < MAX_GC; i++) {
        runtime.gc();
        long now = runtime.totalMemory();
        if (now == total) {
          lastGC = System.currentTimeMillis();
          break;
        }
        total = now;
      }
    }
  }

  private MemoryUtils() {
    // utility class
  }
}
