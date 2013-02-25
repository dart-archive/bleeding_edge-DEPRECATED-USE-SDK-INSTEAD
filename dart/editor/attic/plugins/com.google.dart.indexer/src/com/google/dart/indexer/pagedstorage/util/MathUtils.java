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
 * This is a utility class with mathematical helper functions.
 */
public class MathUtils {
  /**
   * Check if a value is a power of two.
   * 
   * @param len the value to check
   * @throws RuntimeException if it is not a power of two
   */
  public static void checkPowerOf2(int len) {
    if ((len & (len - 1)) != 0 && len > 0) {
      throw new AssertionError("not a power of 2: " + len);
    }
  }

  /**
   * Get the value that is equal or higher than this value, and that is a power of two.
   * 
   * @param x the original value
   * @return the next power of two value
   */
  public static int nextPowerOf2(int x) {
    long i = 1;
    while (i < x && i < (Integer.MAX_VALUE / 2)) {
      i += i;
    }
    return (int) i;
  }

  /**
   * Round the value up to the next block size. The block size must be a power of two. As an
   * example, using the block size of 8, the following rounding operations are done: 0 stays 0;
   * values 1..8 results in 8, 9..16 results in 16, and so on.
   * 
   * @param x the value to be rounded
   * @param blockSizePowerOf2 the block size
   * @return the rounded value
   */
  public static int roundUp(int x, int blockSizePowerOf2) {
    return (x + blockSizePowerOf2 - 1) & (-blockSizePowerOf2);
  }

  /**
   * Round the value up to the next block size. The block size must be a power of two. As an
   * example, using the block size of 8, the following rounding operations are done: 0 stays 0;
   * values 1..8 results in 8, 9..16 results in 16, and so on.
   * 
   * @param x the value to be rounded
   * @param blockSizePowerOf2 the block size
   * @return the rounded value
   */
  public static long roundUpLong(long x, long blockSizePowerOf2) {
    return (x + blockSizePowerOf2 - 1) & (-blockSizePowerOf2);
  }

  private MathUtils() {
    // utility class
  }
}
