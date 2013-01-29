/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.tools.debug.core.sourcemaps;

import com.google.common.primitives.Ints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A class to convert to and from base64 vlq encoded strings.
 * <p>
 * <code>"AAgBC" <==> {0, 0, 16, 1}</code>
 */
public class VlqDecoder {
  // A Base64 VLQ digit can represent 5 bits, so it is base-32.
  private static final int VLQ_BASE_SHIFT = 5;
  private static final int VLQ_BASE = 1 << VLQ_BASE_SHIFT;

  // A mask of bits for a VLQ digit (11111), 31 decimal.
  private static final int VLQ_BASE_MASK = VLQ_BASE - 1;

  // The continuation bit is the 6th bit.
  private static final int VLQ_CONTINUATION_BIT = VLQ_BASE;

  /**
   * A map used to convert integer values in the range 0-63 to their base64 values.
   */
  private static final String BASE64_MAP = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
      + "abcdefghijklmnopqrstuvwxyz" + "0123456789+/";

  /**
   * A map used to convert base64 character into integer values.
   */
  private static final int[] BASE64_DECODE_MAP = new int[256];

  static {
    Arrays.fill(BASE64_DECODE_MAP, -1);

    for (int i = 0; i < BASE64_MAP.length(); i++) {
      BASE64_DECODE_MAP[BASE64_MAP.charAt(i)] = i;
    }
  }

  /**
   * Convert from a Base64 VLQ string sequence to the corresponding sequence of ints.
   * 
   * @param str
   * @return
   */
  public static int[] decode(String str) {
    List<Integer> results = new ArrayList<Integer>();
    int i = 0;
    int strLen = str.length();

    while (i < strLen) {
      int result = 0;
      boolean continuation;
      int shift = 0;

      do {
        char c = str.charAt(i++);
        int digit = fromBase64(c);
        continuation = (digit & VLQ_CONTINUATION_BIT) != 0;
        digit &= VLQ_BASE_MASK;
        result = result + (digit << shift);
        shift = shift + VLQ_BASE_SHIFT;
      } while (continuation);

      results.add(fromVLQSigned(result));
    }

    return Ints.toArray(results);
  }

  /**
   * Encode the given sequence of ints to a Base64 VLQ encoded string.
   * 
   * @param values
   * @return
   */
  public static String encode(int[] values) {
    StringBuilder builder = new StringBuilder();

    for (int value : values) {
      value = toVLQSigned(value);
      do {
        int digit = value & VLQ_BASE_MASK;
        value >>>= VLQ_BASE_SHIFT;
        if (value > 0) {
          digit |= VLQ_CONTINUATION_BIT;
        }
        builder.append(toBase64(digit));
      } while (value > 0);
    }

    return builder.toString();
  }

  private static int fromBase64(char c) {
    return BASE64_DECODE_MAP[c];
  }

  /**
   * Converts to a two-complement value from a value where the sign bit is is placed in the least
   * significant bit. For example, as decimals: 2 (10 binary) becomes 1, 3 (11 binary) becomes -1 4
   * (100 binary) becomes 2, 5 (101 binary) becomes -2
   */
  private static int fromVLQSigned(int value) {
    boolean negate = (value & 1) == 1;
    value = value >> 1;
    return negate ? -value : value;
  }

  private static char toBase64(int value) {
    return BASE64_MAP.charAt(value);
  }

  /**
   * Converts from a two-complement value to a value where the sign bit is is placed in the least
   * significant bit. For example, as decimals: 1 becomes 2 (10 binary), -1 becomes 3 (11 binary) 2
   * becomes 4 (100 binary), -2 becomes 5 (101 binary)
   */
  private static int toVLQSigned(int value) {
    if (value < 0) {
      return ((-value) << 1) + 1;
    } else {
      return (value << 1) + 0;
    }
  }

  private VlqDecoder() {

  }

}
