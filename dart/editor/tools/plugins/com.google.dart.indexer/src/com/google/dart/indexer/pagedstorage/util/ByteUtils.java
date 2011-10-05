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

public class ByteUtils {
  /**
   * Compare the contents of two byte arrays. If the content or length of the first array is smaller
   * than the second array, -1 is returned. If the content or length of the second array is smaller
   * than the first array, 1 is returned. If the contents and lengths are the same, 0 is returned.
   * 
   * @param data1 the first byte array (must not be null)
   * @param data2 the second byte array (must not be null)
   * @return the result of the comparison (-1, 1 or 0)
   */
  public static int compareNotNull(byte[] data1, byte[] data2) {
    int len = Math.min(data1.length, data2.length);
    for (int i = 0; i < len; i++) {
      byte b = data1[i];
      byte b2 = data2[i];
      if (b != b2) {
        return b > b2 ? 1 : -1;
      }
    }
    int c = data1.length - data2.length;
    return c == 0 ? 0 : (c < 0 ? -1 : 1);
  }

  private ByteUtils() {
    // utility class
  }
}
