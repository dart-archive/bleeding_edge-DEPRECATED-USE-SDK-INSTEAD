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

import java.util.Arrays;

public class ArrayUtils {
  public static byte[][] add(byte[][] array, byte[] newValue, int pos) {
    if (pos < 0 || pos > array.length) {
      throw new IllegalArgumentException("Invalid pos to insert an item at");
    }
    byte[][] result = new byte[array.length + 1][];
    System.arraycopy(array, 0, result, 0, pos);
    result[pos] = newValue;
    System.arraycopy(array, pos, result, pos + 1, array.length - pos);
    return result;
  }

  public static byte[][][] add(byte[][][] array, byte[][] newValue, int pos) {
    if (pos < 0 || pos > array.length) {
      throw new IllegalArgumentException("Invalid pos to insert an item at");
    }
    byte[][][] result = new byte[array.length + 1][][];
    System.arraycopy(array, 0, result, 0, pos);
    result[pos] = newValue;
    System.arraycopy(array, pos, result, pos + 1, array.length - pos);
    return result;
  }

  public static int[] add(int[] array, int newValue) {
    int[] result = new int[array.length + 1];
    System.arraycopy(array, 0, result, 0, array.length);
    result[array.length] = newValue;
    return result;
  }

  public static int[] add(int[] array, int newValue, int pos) {
    if (pos < 0 || pos > array.length) {
      throw new IllegalArgumentException("Invalid pos to insert an item at");
    }
    int[] result = new int[array.length + 1];
    System.arraycopy(array, 0, result, 0, pos);
    result[pos] = newValue;
    System.arraycopy(array, pos, result, pos + 1, array.length - pos);
    return result;
  }

  public static int[][] add(int[][] array, int[] newValue, int pos) {
    if (pos < 0 || pos > array.length) {
      throw new IllegalArgumentException("Invalid pos to insert an item at");
    }
    int[][] result = new int[array.length + 1][];
    System.arraycopy(array, 0, result, 0, pos);
    result[pos] = newValue;
    System.arraycopy(array, pos, result, pos + 1, array.length - pos);
    return result;
  }

  public static long[] add(long[] array, long newValue, int pos) {
    if (pos < 0 || pos > array.length) {
      throw new IllegalArgumentException("Invalid pos to insert an item at");
    }
    long[] result = new long[array.length + 1];
    System.arraycopy(array, 0, result, 0, pos);
    result[pos] = newValue;
    System.arraycopy(array, pos, result, pos + 1, array.length - pos);
    return result;
  }

  public static long[][] add(long[][] array, long[] newValue, int pos) {
    if (pos < 0 || pos > array.length) {
      throw new IllegalArgumentException("Invalid pos to insert an item at");
    }
    long[][] result = new long[array.length + 1][];
    System.arraycopy(array, 0, result, 0, pos);
    result[pos] = newValue;
    System.arraycopy(array, pos, result, pos + 1, array.length - pos);
    return result;
  }

  public static String[] add(String[] array, String newValue, int pos) {
    if (pos < 0 || pos > array.length) {
      throw new IllegalArgumentException("Invalid pos to insert an item at");
    }
    String[] result = new String[array.length + 1];
    System.arraycopy(array, 0, result, 0, pos);
    result[pos] = newValue;
    System.arraycopy(array, pos, result, pos + 1, array.length - pos);
    return result;
  }

  public static String[][] add(String[][] array, String[] newValue, int pos) {
    if (pos < 0 || pos > array.length) {
      throw new IllegalArgumentException("Invalid pos to insert an item at");
    }
    String[][] result = new String[array.length + 1][];
    System.arraycopy(array, 0, result, 0, pos);
    result[pos] = newValue;
    System.arraycopy(array, pos, result, pos + 1, array.length - pos);
    return result;
  }

  public static int[] addSorted(int[] array, int[] newValues) {
    if (newValues.length == 0) {
      return array;
    }

    int[] interim = new int[array.length + newValues.length];
    System.arraycopy(array, 0, interim, 0, array.length);
    System.arraycopy(newValues, 0, interim, array.length, newValues.length);
    Arrays.sort(interim);

    int count = 1;
    for (int i = 1; i < interim.length; i++) {
      if (interim[i] != interim[i - 1]) {
        ++count;
      }
    }

    if (count == interim.length) {
      return interim;
    }

    int[] result = new int[count];
    int index = 0;
    result[index++] = interim[0];
    for (int i = 1; i < interim.length; i++) {
      if (interim[i] != interim[i - 1]) {
        result[index++] = interim[i];
      }
    }
    if (index != count) {
      throw new AssertionError("addSorted internal error");
    }

    return result;
  }

  public static byte[][] remove(byte[][] array, int pos) {
    if (pos < 0 || pos >= array.length) {
      throw new IllegalArgumentException("Invalid pos to remove an item at");
    }
    byte[][] result = new byte[array.length - 1][];
    System.arraycopy(array, 0, result, 0, pos);
    if (pos < array.length - 1) {
      System.arraycopy(array, pos + 1, result, pos, array.length - pos - 1);
    }
    return result;
  }

  public static byte[][][] remove(byte[][][] array, int pos) {
    if (pos < 0 || pos >= array.length) {
      throw new IllegalArgumentException("Invalid pos to remove an item at");
    }
    byte[][][] result = new byte[array.length - 1][][];
    System.arraycopy(array, 0, result, 0, pos);
    if (pos < array.length - 1) {
      System.arraycopy(array, pos + 1, result, pos, array.length - pos - 1);
    }
    return result;
  }

  public static int[] remove(int[] array, int pos) {
    if (pos < 0 || pos >= array.length) {
      throw new IllegalArgumentException("Invalid pos to remove an item at");
    }
    int[] result = new int[array.length - 1];
    System.arraycopy(array, 0, result, 0, pos);
    if (pos < array.length - 1) {
      System.arraycopy(array, pos + 1, result, pos, array.length - pos - 1);
    }
    return result;
  }

  public static int[][] remove(int[][] array, int pos) {
    if (pos < 0 || pos >= array.length) {
      throw new IllegalArgumentException("Invalid pos to remove an item at");
    }
    int[][] result = new int[array.length - 1][];
    System.arraycopy(array, 0, result, 0, pos);
    if (pos < array.length - 1) {
      System.arraycopy(array, pos + 1, result, pos, array.length - pos - 1);
    }
    return result;
  }

  public static long[] remove(long[] array, int pos) {
    if (pos < 0 || pos >= array.length) {
      throw new IllegalArgumentException("Invalid pos to remove an item at");
    }
    long[] result = new long[array.length - 1];
    System.arraycopy(array, 0, result, 0, pos);
    if (pos < array.length - 1) {
      System.arraycopy(array, pos + 1, result, pos, array.length - pos - 1);
    }
    return result;
  }

  public static long[][] remove(long[][] array, int pos) {
    if (pos < 0 || pos >= array.length) {
      throw new IllegalArgumentException("Invalid pos to remove an item at");
    }
    long[][] result = new long[array.length - 1][];
    System.arraycopy(array, 0, result, 0, pos);
    if (pos < array.length - 1) {
      System.arraycopy(array, pos + 1, result, pos, array.length - pos - 1);
    }
    return result;
  }

  public static String[] remove(String[] array, int pos) {
    if (pos < 0 || pos >= array.length) {
      throw new IllegalArgumentException("Invalid pos to remove an item at");
    }
    String[] result = new String[array.length - 1];
    System.arraycopy(array, 0, result, 0, pos);
    if (pos < array.length - 1) {
      System.arraycopy(array, pos + 1, result, pos, array.length - pos - 1);
    }
    return result;
  }

  public static String[][] remove(String[][] array, int pos) {
    if (pos < 0 || pos >= array.length) {
      throw new IllegalArgumentException("Invalid pos to remove an item at");
    }
    String[][] result = new String[array.length - 1][];
    System.arraycopy(array, 0, result, 0, pos);
    if (pos < array.length - 1) {
      System.arraycopy(array, pos + 1, result, pos, array.length - pos - 1);
    }
    return result;
  }

  public static int[] truncate(int[] array, int maxLen) {
    if (array.length < maxLen) {
      return array;
    }
    int[] result = new int[maxLen];
    System.arraycopy(array, 0, result, 0, maxLen);
    return result;
  }
}
